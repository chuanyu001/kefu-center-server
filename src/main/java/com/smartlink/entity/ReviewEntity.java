package com.smartlink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审核实体
 *
 * @author smartlink
 */
@Data
@TableName("reviews")
public class ReviewEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 审核类型 */
    private String type;

    /** 审核目标ID */
    private String targetId;

    /** 标题 */
    private String title;

    /** 提交人 */
    private String submitter;

    /** 提交时间 */
    private LocalDateTime submittedAt;

    /** 审核状态 */
    private String status;

    /** 审核人 */
    private String reviewer;

    /** 审核意见 */
    private String reviewComment;
}
