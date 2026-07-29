package com.smartlink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 迭代记录实体
 *
 * @author smartlink
 */
@Data
@TableName("iterations")
public class IterationEntity {

    @TableId(type = IdType.ASSIGN_ID)
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

    /** 对比结果(JSON) */
    private String comparison;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 状态 */
    private String status;
}
