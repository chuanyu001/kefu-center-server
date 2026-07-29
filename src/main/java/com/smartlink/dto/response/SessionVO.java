package com.smartlink.dto.response;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 工作表视图对象 — 和 sessions 表列一一对应
 */
@Data
public class SessionVO {

    private String id;
    private String customerName;
    private String customerPhone;
    private String vin;
    private String workRecordType;
    private String sessionTime;
    private String agentName;

    // 业务字段
    private String iccid;
    private String carModel;
    private String fuelType;
    private String terminalNumber;
    private String simCard;
    private String manufacturer;
    private String recorderModel;
    private String consultationScenario;
    private String problemType;
    private String temporarySolution;
    private String specialNotes;

    // 参考数据（JSON解析后）
    private List<Map<String, Object>> chatMessages;
    private List<Map<String, Object>> modificationHistory;

    private String createdAt;
    private String updatedAt;
}
