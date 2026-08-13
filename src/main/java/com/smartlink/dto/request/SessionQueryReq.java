package com.smartlink.dto.request;

import lombok.Data;

@Data
public class SessionQueryReq {

    private String keyword;
    private String workRecordType;
    private String exportStatus;

    private Integer page = 1;
    private Integer pageSize = 10;
}
