package com.smartlink.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlink.config.AiOpenProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * AI 代理层：转发到火山方舟标准端 (GLM-5.2)
 * 把 OpenAI 兼容的 SSE 流转换成前端使用的 NDJSON 协议 ({"t":"c",...} 等)。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiProxyService {

    private final AiOpenProperties props;
    private final KnowledgeService knowledgeService;
    private final ObjectMapper mapper = new ObjectMapper();
    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");

    public void chatStream(List<Map<String, Object>> messages, String model,
                           Boolean allowCode, Boolean wantFile, Integer maxRounds,
                           String endUserId, Consumer<String> lineCallback) {
        // 注入知识库检索内容（若有），使模型基于内部知识回答
        List<Map<String, Object>> finalMessages = augmentWithKnowledge(messages);

        // 构建 OpenAI 兼容请求体
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("messages", finalMessages);
        body.put("stream", true);
        // 前端传的 pro/lite 是业务代号，映射到真实模型名；直接传裸模型名则透传
        if (model == null || model.isEmpty() || "pro".equals(model) || "lite".equals(model)) {
            body.put("model", props.getModel());
        } else {
            body.put("model", model);
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(props.getConnectTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(props.getReadTimeout(), TimeUnit.MILLISECONDS)
                .build();

        String bodyStr;
        try {
            bodyStr = mapper.writeValueAsString(body);
        } catch (Exception e) {
            lineCallback.accept("{\"t\":\"e\",\"v\":\"请求序列化失败\"}");
            return;
        }

        Request request = new Request.Builder()
                .url(props.getChatUrl())
                .post(RequestBody.create(JSON_TYPE, bodyStr))
                .addHeader("Authorization", "Bearer " + props.getApiKey())
                .build();

        StringBuilder fullText = new StringBuilder();

        try (Response resp = client.newCall(request).execute()) {
            if (!resp.isSuccessful()) {
                String errBody = resp.body() != null ? resp.body().string() : "";
                log.warn("[AiProxy] 火山方舟标准端返回 HTTP {}: {}", resp.code(), errBody.substring(0, Math.min(200, errBody.length())));
                lineCallback.accept("{\"t\":\"e\",\"v\":\"AI服务请求失败（HTTP " + resp.code() + "）\"}");
                return;
            }

            // 解析 OpenAI SSE：data: {...} 行，delta.content 是正文增量
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resp.body().byteStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) continue;
                String data = line.substring(5).trim();
                if (data.isEmpty() || "[DONE]".equals(data)) continue;

                try {
                    Map<String, Object> obj = mapper.readValue(data,
                            new TypeReference<Map<String, Object>>() {});
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) obj.get("choices");
                    if (choices == null || choices.isEmpty()) continue;

                    Object deltaObj = choices.get(0).get("delta");
                    if (!(deltaObj instanceof Map)) continue;
                    @SuppressWarnings("unchecked")
                    Map<String, Object> delta = (Map<String, Object>) deltaObj;

                    // 思考过程增量 (reasoning_content)
                    Object reasoning = delta.get("reasoning_content");
                    if (reasoning != null && !reasoning.toString().isEmpty()) {
                        lineCallback.accept("{\"t\":\"r\",\"v\":" + mapper.writeValueAsString(reasoning.toString()) + "}");
                    }

                    // 正文增量
                    Object content = delta.get("content");
                    if (content != null && !content.toString().isEmpty()) {
                        fullText.append(content.toString());
                        lineCallback.accept("{\"t\":\"c\",\"v\":" + mapper.writeValueAsString(content.toString()) + "}");
                    }
                } catch (Exception ignore) {
                    // 单行解析失败不中断整个流
                }
            }

            lineCallback.accept("{\"t\":\"done\",\"v\":" + mapper.writeValueAsString(fullText.toString()) + "}");
        } catch (Exception e) {
            log.warn("[AiProxy] 调用火山方舟标准端失败", e);
            lineCallback.accept("{\"t\":\"e\",\"v\":\"AI服务连接失败：" + e.getMessage() + "\"}");
        }
    }

    /**
     * 用知识库检索结果增强消息：抽取最后一条用户消息 → 检索 document_chunks →
     * 在消息最前插入一段“知识库参考”系统消息，让模型基于内部知识回答。
     */
    private List<Map<String, Object>> augmentWithKnowledge(List<Map<String, Object>> messages) {
        String lastUser = null;
        for (int i = messages.size() - 1; i >= 0; i--) {
            Map<String, Object> m = messages.get(i);
            if ("user".equals(m.get("role"))) {
                Object c = m.get("content");
                if (c != null && !c.toString().trim().isEmpty()) {
                    lastUser = c.toString();
                    break;
                }
            }
        }
        if (lastUser == null) {
            return messages;
        }

        List<KnowledgeService.Retrieved> chunks = knowledgeService.retrieve(lastUser);
        if (chunks.isEmpty()) {
            return messages;
        }

        StringBuilder kb = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            KnowledgeService.Retrieved r = chunks.get(i);
            kb.append("\n【参考").append(i + 1).append("】文档《").append(r.docTitle).append("》");
            if (r.section != null && !r.section.isEmpty()) {
                kb.append("（").append(r.section).append("）");
            }
            kb.append("\n").append(r.text).append("\n");
        }

        String grounding = "以下是知识库中与当前问题相关的检索内容。请优先依据这些内容给出准确、可执行的回答，"
                + "并在回答中标注所依据的文档名称（如《xxx》）；若检索内容不足以回答，请明确说明，不要编造。"
                + kb.toString();

        List<Map<String, Object>> augmented = new ArrayList<>(messages.size() + 1);
        Map<String, Object> sys = new LinkedHashMap<>();
        sys.put("role", "system");
        sys.put("content", grounding);
        augmented.add(sys);
        augmented.addAll(messages);
        return augmented;
    }
}
