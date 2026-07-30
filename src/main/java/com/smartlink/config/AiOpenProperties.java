package com.smartlink.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 鱼快 AI 开放接口配置
 * 前缀: ai.open
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.open")
public class AiOpenProperties {

    /** 流式对话接口, 如 http://11.127.20.32:30271/pockettools/ai/open/chat */
    private String chatUrl;

    /** 同步对话接口, 如 http://11.127.20.32:30271/pockettools/ai/open/chat/sync */
    private String syncUrl;

    /** X-Api-Key 鉴权密钥（同事单独发放） */
    private String apiKey;

    /** HTTP 连接超时(毫秒) */
    private int connectTimeout = 10000;

    /** HTTP 读取超时(毫秒), 流式需要较长 */
    private int readTimeout = 300000;
}
