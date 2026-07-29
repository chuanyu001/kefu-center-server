package com.smartlink.dto.response;

import lombok.Data;

import java.util.Map;

/**
 * 问答对视图对象
 *
 * @author smartlink
 */
@Data
public class QaPairVO {

    /** 主键ID */
    private String id;

    /** 问题 */
    private String question;

    /** 答案 */
    private String answer;

    /** 答案摘要 */
    private String answerBrief;

    /** 来源文档ID */
    private String sourceDocId;

    /** 分类 */
    private String category;

    /** 标签(已解析) */
    private Object tags;

    /** 状态 */
    private String status;

    /** 置信度 */
    private Double confidence;

    /** 多维数据(已解析) */
    private Map<String, Object> multiDimensional;

    /** 外部同步状态: NOT_SYNCED / SYNCED */
    private String syncStatus;

    /** 同步到外部QA库的时间 */
    private String syncedAt;

    /** 创建时间 */
    private String createdAt;

    /** 更新时间 */
    private String updatedAt;
}
