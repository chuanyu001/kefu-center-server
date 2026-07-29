package com.smartlink.dto.request;

import lombok.Data;

/**
 * 问答对查询请求
 *
 * @author smartlink
 */
@Data
public class QaQueryReq {

    /** 关键词搜索 */
    private String keyword;

    /** 分类 */
    private String category;

    /** 状态 */
    private String status;

    /** 页码，默认1 */
    private Integer page = 1;

    /** 每页大小，默认10 */
    private Integer pageSize = 10;
}
