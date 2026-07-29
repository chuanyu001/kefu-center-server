package com.smartlink.common;

import lombok.Data;

/**
 * 通用响应包装类
 *
 * @param <T> 数据类型
 * @author smartlink
 */
@Data
public class Result<T> {

    /** 状态码：200成功，其他为失败 */
    private int code;

    /** 提示信息 */
    private String message;

    /** 响应数据 */
    private T data;

    private Result() {}

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功响应
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /**
     * 成功响应，自定义消息
     */
    public static <T> Result<T> ok(T data, String message) {
        return new Result<>(200, message, data);
    }

    /**
     * 失败响应
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }

    /**
     * 失败响应，自定义状态码
     */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 参数错误
     */
    public static <T> Result<T> badRequest(String message) {
        return new Result<>(400, message, null);
    }

    /**
     * 未找到
     */
    public static <T> Result<T> notFound(String message) {
        return new Result<>(404, message, null);
    }
}
