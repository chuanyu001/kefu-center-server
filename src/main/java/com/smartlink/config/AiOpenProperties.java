package com.smartlink.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 火山方舟 AgentPlan 大模型接口配置
 * 前缀: ai.open
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.open")
public class AiOpenProperties {

    /** OpenAI 兼容对话接口: https://ark.cn-beijing.volces.com/api/v3/chat/completions */
    private String chatUrl = "https://ark.cn-beijing.volces.com/api/v3/chat/completions";

    /** AgentPlan API Key (ark-xxx)，通过环境变量 ARK_API_KEY 注入 */
    private String apiKey;

    /** 默认模型 */
    private String model = "glm-5-2-260617";

    /** HTTP 连接超时(毫秒) */
    private int connectTimeout = 10000;

    /** HTTP 读取超时(毫秒), 流式需要较长 */
    private int readTimeout = 300000;
}
