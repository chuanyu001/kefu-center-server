package com.smartlink.dto.response;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 工作表视图对象
 *
 * @author smartlink
 */
@Data
public class SessionVO {

    /** 主键ID */
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

    /** 坐席名称 */
    private String agentName;

    /** 填写状态 */
    private String fillStatus;

    /** AI置信度 */
    private Double aiConfidence;

    /** 表单数据(已解析) */
    private Map<String, Object> formData;

    /** 会话消息(已解析) */
    private List<Map<String, Object>> messages;

    /** 修改历史(已解析) */
    private List<Map<String, Object>> modificationHistory;

    /** 会话时间 */
    private String sessionTime;
}
