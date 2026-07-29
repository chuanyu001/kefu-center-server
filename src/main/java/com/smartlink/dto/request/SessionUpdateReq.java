package com.smartlink.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 工作表更新请求
 *
 * @author smartlink
 */
@Data
public class SessionUpdateReq {

    /** ICCID(存储在formData中的字段) */
    private String iccid;

    /** 导出状态 */
    @NotBlank(message = "导出状态不能为空")
    private String exportStatus;
}
