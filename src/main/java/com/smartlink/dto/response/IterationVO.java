package com.smartlink.dto.response;

import lombok.Data;

import java.util.Map;

/**
 * 迭代记录视图对象
 *
 * @author smartlink
 */
@Data
public class IterationVO {

    /** 主键ID */
    private String id;

    /** 来源会话ID */
    private String sourceSessionId;

    /** 会话摘要 */
    private String sessionSummary;

    /** 提取问题 */
    private String extractedQuestion;

    /** 提取答案 */
    private String extractedAnswer;

    /** 已有问答ID */
    private String existingQaId;

    /** 对比结果(已解析) */
    private Map<String, Object> comparison;

    /** 创建时间 */
    private String createdAt;

    /** 状态 */
    private String status;
}
