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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiProxyService {

    private final AiOpenProperties props;
    private final ObjectMapper mapper = new ObjectMapper();
    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");

    public String chatStream(List<Map<String, Object>> messages, String model,
                              Boolean allowCode, Boolean wantFile, Integer maxRounds,
                              String endUserId, Consumer<String> lineCallback) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("messages", messages);
        if (model != null && !model.isEmpty()) body.put("model", model);
        if (allowCode != null) body.put("allowCode", allowCode);
        if (wantFile != null) body.put("wantFile", wantFile);
        if (maxRounds != null && maxRounds > 0) body.put("maxRounds", maxRounds);
        if (endUserId != null && !endUserId.isEmpty()) body.put("endUserId", endUserId);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(props.getConnectTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(props.getReadTimeout(), TimeUnit.MILLISECONDS)
                .build();

        String bodyStr;
        try { bodyStr = mapper.writeValueAsString(body); }
        catch (Exception e) { lineCallback.accept("{\"t\":\"e\",\"v\":\"序列化失败\"}"); return ""; }

        Request request = new Request.Builder()
                .url(props.getChatUrl())
                .post(RequestBody.create(JSON_TYPE, bodyStr))
                .addHeader("X-Api-Key", props.getApiKey())
                .build();

        StringBuilder answer = new StringBuilder();

        try (Response resp = client.newCall(request).execute()) {
            if (!resp.isSuccessful()) {
                lineCallback.accept("{\"t\":\"e\",\"v\":\"AI服务异常 " + resp.code() + "\"}");
                return "";
            }
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resp.body().byteStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                lineCallback.accept(line);
                try {
                    Map<String, Object> obj = mapper.readValue(line,
                            new TypeReference<Map<String, Object>>() {});
                    if ("c".equals(obj.get("t"))) {
                        answer.append(obj.get("v"));
                    }
                } catch (Exception ignore) { /* ping */ }
            }
        } catch (Exception e) {
            log.warn("[AiProxy] 调用AI网关失败", e);
            lineCallback.accept("{\"t\":\"e\",\"v\":\"AI服务连接失败\"}");
        }
        return answer.toString();
    }

    public String chatSync(List<Map<String, Object>> messages, String endUserId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("messages", messages);
        if (endUserId != null && !endUserId.isEmpty()) body.put("endUserId", endUserId);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(props.getConnectTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(120000, TimeUnit.MILLISECONDS)
                .build();

        String bodyStr;
        try { bodyStr = mapper.writeValueAsString(body); }
        catch (Exception e) { return null; }

        Request request = new Request.Builder()
                .url(props.getSyncUrl())
                .post(RequestBody.create(JSON_TYPE, bodyStr))
                .addHeader("X-Api-Key", props.getApiKey())
                .build();

        try (Response resp = client.newCall(request).execute()) {
            if (!resp.isSuccessful()) return null;
            Map<String, Object> result = mapper.readValue(resp.body().string(),
                    new TypeReference<Map<String, Object>>() {});
            if (Boolean.TRUE.equals(result.get("ok"))) {
                return (String) result.get("answer");
            }
        } catch (Exception e) {
            log.warn("[AiProxy] sync error", e);
        }
        return null;
    }
}
