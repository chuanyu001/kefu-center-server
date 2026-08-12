package com.smartlink.common;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {

    private List<T> records;
    private long total;
    private int page;
    private int pageSize;

    private PageResult() {}

    private PageResult(List<T> records, long total, int page, int pageSize) {
        this.records = records;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
    }

    public static <T> PageResult<T> of(List<T> records, long total, int page, int pageSize) {
        return new PageResult<>(records, total, page, pageSize);
    }
}
