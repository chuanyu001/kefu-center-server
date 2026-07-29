package com.smartlink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工作表实体
 *
 * @author smartlink
 */
@Data
@TableName("sessions")
public class SessionEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 客户名称 */
    private String customerName;

    /** 客户电话 */
    private String customerPhone;

    /** 车辆VIN码 */
    private String vin;

    /** 工单类型 */
    private String workRecordType;

    /** 导出状态 */
    private String exportStatus;

    /** 会话时间 */
    private String sessionTime;

    /** 坐席名称 */
    private String agentName;

    /** 填写状态 */
    private String fillStatus;

    /** AI置信度 */
    private BigDecimal aiConfidence;

    /** 表单数据(JSON) */
    private String formData;

    /** 会话消息(JSON) */
    private String messages;

    /** 修改历史(JSON) */
    private String modificationHistory;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
