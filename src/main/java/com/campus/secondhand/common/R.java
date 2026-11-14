package com.campus.secondhand.common;

import java.io.Serializable;

/**
 * 统一响应结果
 *
 * @param <T> 业务数据类型
 */
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务状态码：200 成功 */
    private int code;
    /** 提示信息 */
    private String msg;
    /** 业务数据 */
    private T data;
    /** 服务器时间戳，便于前端排查 */
    private long timestamp;

    public R() {
        this.timestamp = System.currentTimeMillis();
    }

    public R(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> R<T> ok() {
        return new R<>(200, "操作成功", null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(200, "操作成功", data);
    }

    public static <T> R<T> ok(String msg, T data) {
        return new R<>(200, msg, data);
    }

    public static <T> R<T> fail(String msg) {
        return new R<>(500, msg, null);
    }

    public static <T> R<T> fail(int code, String msg) {
        return new R<>(code, msg, null);
    }

    public static <T> R<T> unauthorized(String msg) {
        return new R<>(401, msg, null);
    }

    public static <T> R<T> forbidden(String msg) {
        return new R<>(403, msg, null);
    }

    public boolean isSuccess() {
        return this.code == 200;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}