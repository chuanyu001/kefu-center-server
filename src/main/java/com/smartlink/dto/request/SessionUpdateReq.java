package com.smartlink.dto.request;

import lombok.Data;

/**
 * 工作表更新请求 — 更新可编辑的业务字段
 *
 * @author smartlink
 */
@Data
public class SessionUpdateReq {

    /** ICCID */
    private String iccid;

    /** 咨询场景 */
    private String consultationScenario;

    /** 问题类型 */
    private String problemType;

    /** 临时解决措施 */
    private String temporarySolution;

    /** 特殊备注 */
    private String specialNotes;
}
