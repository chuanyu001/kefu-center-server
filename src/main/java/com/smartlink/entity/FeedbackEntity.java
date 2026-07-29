package com.smartlink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 反馈实体
 *
 * @author smartlink
 */
@Data
@TableName("feedback")
public class FeedbackEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 客户名称 */
    private String customerName;

    /** 关联报告ID */
    private String reportId;

    /** 反馈内容 */
    private String content;

    /** 反馈时间 */
    private LocalDateTime feedbackTime;

    /** 状态 */
    private String status;

    /** 备注 */
    private String remark;
}
