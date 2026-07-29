package com.smartlink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作表实体
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

    /** 工作表类型: fleet_register / after_sales */
    private String workRecordType;

    /** 会话时间 */
    private String sessionTime;

    /** 受理客服 */
    private String agentName;

    // ── 业务字段（AI自动填充）──

    /** ICCID（车队登记表手动输入） */
    private String iccid;

    /** 车型 */
    private String carModel;

    /** 燃料类型: 柴油 / LNG / 纯电动 */
    private String fuelType;

    /** T-Box终端号 */
    private String terminalNumber;

    /** SIM卡号 */
    private String simCard;

    /** 厂家 */
    private String manufacturer;

    /** 记录仪型号 */
    private String recorderModel;

    /** 咨询场景 */
    private String consultationScenario;

    /** 问题类型 */
    private String problemType;

    /** 临时解决措施 */
    private String temporarySolution;

    /** 特殊备注 */
    private String specialNotes;

    // ── 参考数据（JSON）──

    /** 原始会话记录 */
    private String chatMessages;

    /** 人工修改记录 */
    private String modificationHistory;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
