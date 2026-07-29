package com.smartlink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 问答对实体
 *
 * @author smartlink
 */
@Data
@TableName("qa_pairs")
public class QaPairEntity {

    @TableId(type = IdType.ASSIGN_ID)
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

    /** 标签(JSON) */
    private String tags;

    /** 状态 */
    private String status;

    /** 置信度 */
    private BigDecimal confidence;

    /** 多维数据(JSON) */
    private String multiDimensional;

    /** 外部同步状态: NOT_SYNCED / SYNCED */
    private String syncStatus;

    /** 同步到外部QA库的时间 */
    private LocalDateTime syncedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
