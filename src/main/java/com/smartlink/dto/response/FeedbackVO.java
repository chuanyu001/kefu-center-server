package com.smartlink.dto.response;

import lombok.Data;

/**
 * 反馈视图对象
 *
 * @author smartlink
 */
@Data
public class FeedbackVO {

    /** 主键ID */
    private String id;

    /** 客户名称 */
    private String customerName;

    /** 关联报告ID */
    private String reportId;

    /** 反馈内容 */
    private String content;

    /** 反馈时间 */
    private String feedbackTime;

    /** 状态 */
    private String status;

    /** 备注 */
    private String remark;
}
