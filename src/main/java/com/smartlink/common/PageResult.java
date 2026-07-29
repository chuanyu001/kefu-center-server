package com.smartlink.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 分页响应包装类
 *
 * @param <T> 数据类型
 * @author smartlink
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    /** 记录列表 */
    private List<T> records;

    /** 总条数 */
    private long total;

    /** 当前页码 */
    private long page;

    /** 每页大小 */
    private long pageSize;

    /**
     * 创建空的分页结果
     */
    public static <T> PageResult<T> empty() {
        return new PageResult<>(Collections.emptyList(), 0, 1, 10);
    }

    /**
     * 创建分页结果
     */
    public static <T> PageResult<T> of(List<T> records, long total, long page, long pageSize) {
        return new PageResult<>(records, total, page, pageSize);
    }
}
