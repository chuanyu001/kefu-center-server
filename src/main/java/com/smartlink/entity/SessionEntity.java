package com.smartlink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sessions")
public class SessionEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String customerName;
    private String customerPhone;
    private String vin;
    private String workRecordType;
    private String sessionTime;
    private String agentName;
    private String iccid;
    private String carModel;
    private String fuelType;
    private String terminalNumber;
    private String simCard;
    private String manufacturer;
    private String recorderModel;
    private String recorderDeviceId;
    private String antennaPosition;
    private String noPositionReason;
    private String noPositionIssue;
    private String antennaDamaged;

    private Integer qiyuTicketStatus;
    private String qiyuTicketCategory;
    private String consultationScenario;
    private String problemType;
    private String temporarySolution;
    private String specialNotes;

    private String chatMessages;
    private String modificationHistory;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
