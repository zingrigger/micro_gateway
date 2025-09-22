package com.bosyon.gateway.response;

import java.util.HashMap;
import java.util.Map;

public class Return<T> {

    private int code;
    private String message;
    private T data;

    private Return() {
    }

    private Return(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Return<T> success() {
        return new Return<T>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getDepict(), null);
    }

    public static <T> Return<T> success(String message) {
        return new Return<T>(ResultCode.SUCCESS.getCode(), message, null);
    }

    public static <T> Return<Map<String, T>> success(String key, T t) {
        Map<String, T> map = new HashMap<>();
        map.put(key, t);
        return new Return<Map<String, T>>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getDepict(), map);
    }

    public static <T> Return<T> success(T data) {
        return new Return<T>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getDepict(), data);
    }

    public static <T> Return<T> failed() {
        return new Return<T>(ResultCode.FAILED.getCode(), ResultCode.FAILED.getDepict(), null);
    }

    public static <T> Return<T> failed(String message) {
        return new Return<T>(ResultCode.FAILED.getCode(), message, null);
    }
    
    public static <T> Return<T> failed(int code, String message) {
    	return new Return<T>(code, message, null);
    }

    public static <T> Return<T> failed(T t) {
        return new Return<T>(ResultCode.FAILED.getCode(), ResultCode.FAILED.getDepict(), t);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
