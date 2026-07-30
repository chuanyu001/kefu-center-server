package com.smartlink.controller;

import com.smartlink.service.AiProxyService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiProxyService aiProxyService;

    /** 有界线程池：核心2，最大8，队列16，超出由调用线程执行 */
    private final ExecutorService pool = new ThreadPoolExecutor(
            2, 8, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(16),
            new ThreadPoolExecutor.CallerRunsPolicy());

    @Data
    public static class AiChatRequest {
        private List<Map<String, Object>> messages;
        private String model;
        private Boolean allowCode;
        private Boolean wantFile;
        private Integer maxRounds;
        private String endUserId;
    }

    @PostMapping(value = "/chat", produces = "application/x-ndjson;charset=UTF-8")
    public ResponseBodyEmitter chat(@RequestBody AiChatRequest req, HttpServletResponse resp) {
        resp.setHeader("X-Accel-Buffering", "no");
        resp.setHeader("Cache-Control", "no-cache");
        resp.setContentType("application/x-ndjson;charset=UTF-8");

        ResponseBodyEmitter emitter = new ResponseBodyEmitter(300000L);

        if (req.getMessages() == null || req.getMessages().isEmpty()) {
            try { emitter.send("{\"t\":\"e\",\"v\":\"messages 不能为空\"}\n".getBytes()); }
            catch (IOException ignore) { }
            return emitter;
        }

        pool.submit(() -> {
            try {
                String fullAnswer = aiProxyService.chatStream(
                        req.getMessages(), req.getModel(),
                        req.getAllowCode(), req.getWantFile(), req.getMaxRounds(),
                        req.getEndUserId(),
                        line -> {
                            try { emitter.send((line + "\n").getBytes()); }
                            catch (IOException e) { throw new RuntimeException("client-gone"); }
                        });

                log.info("[AiChat] 完成 endUserId={} len={}", req.getEndUserId(),
                        fullAnswer != null ? fullAnswer.length() : 0);
            } catch (Exception e) {
                if (!"client-gone".equals(e.getMessage())) {
                    log.warn("[AiChat] 失败", e);
                    try { emitter.send("{\"t\":\"e\",\"v\":\"服务异常\"}\n".getBytes()); }
                    catch (IOException ignore) { }
                }
            }
        });

        return emitter;
    }
}
