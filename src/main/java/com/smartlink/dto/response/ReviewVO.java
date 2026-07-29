package com.smartlink.dto.response;

import lombok.Data;

/**
 * 审核视图对象
 *
 * @author smartlink
 */
@Data
public class ReviewVO {

    /** 主键ID */
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
    private String submittedAt;

    /** 审核状态 */
    private String status;

    /** 审核人 */
    private String reviewer;

    /** 审核意见 */
    private String reviewComment;
}
