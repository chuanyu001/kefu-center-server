package com.smartlink.dto.request;

import lombok.Data;

/**
 * 工作表查询请求
 *
 * @author smartlink
 */
@Data
public class SessionQueryReq {

    /** 关键词搜索(客户名称、VIN、电话) */
    private String keyword;

    /** 工单类型 */
    private String workRecordType;

    /** 导出状态 */
    private String exportStatus;

    /** 页码，默认1 */
    private Integer page = 1;

    /** 每页大小，默认10 */
    private Integer pageSize = 10;
}
