package com.example.mgis.common.result;

import lombok.Data;

@Data
public class Result<T> {
    private int code;
    private String msg;
    private T data;

    // 成功：带数据 + 默认提示
    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(ResultCode.SUCCESS.getCode());
        r.setMsg(ResultCode.SUCCESS.getMsg());
        r.setData(data);
        return r;
    }

    // 成功：带数据 + 自定义提示
    public static <T> Result<T> success(T data, String msg) {
        Result<T> r = new Result<>();
        r.setCode(ResultCode.SUCCESS.getCode());
        r.setMsg(msg);
        r.setData(data);
        return r;
    }

    // 成功：无数据 默认提示
    public static <T> Result<T> success() {
        return success(null);
    }

    // 失败：默认提示
    public static <T> Result<T> fail() {
        Result<T> r = new Result<>();
        r.setCode(ResultCode.FAILED.getCode());
        r.setMsg(ResultCode.FAILED.getMsg());
        return r;
    }

    // 失败：自定义提示
    public static <T> Result<T> fail(String msg) {
        Result<T> r = new Result<>();
        r.setCode(ResultCode.FAILED.getCode());
        r.setMsg(msg);
        return r;
    }
}