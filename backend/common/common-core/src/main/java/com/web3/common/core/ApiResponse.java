/*
 * Decompiled with CFR 0.152.
 */
package com.web3.common.core;

/**
 * 类名：ApiResponse
 * 所属模块：common-core（通用核心模块）
 * 职责：统一 API 响应包装体，所有微服务 Controller 接口统一返回该结构。
 * 结构：code=状态码（200 成功 / 400 参数错误 / 401 未登录 / 403 无权限 / 409 冲突 / 500 服务器错误），message=提示信息，data=业务数据。
 * 用法：通过静态工厂方法 ok() / fail() 快速构造响应对象。
 */
public class ApiResponse<T> {
    private Integer code;
    private String message;
    private T data;

    /**
     * 成功响应：携带业务数据。
     *
     * @param data 业务数据（可为 null）
     * @return code=200、message=success、data=业务数据的响应对象
     */
    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> response = new ApiResponse<T>();
        response.setCode(200);
        response.setMessage("success");
        response.setData(data);
        return response;
    }

    /**
     * 成功响应：无业务数据（如删除成功等操作类接口）。
     *
     * @return code=200、message=success 的响应对象
     */
    public static <T> ApiResponse<T> ok() {
        ApiResponse<T> response = new ApiResponse<T>();
        response.setCode(200);
        response.setMessage("success");
        return response;
    }

    /**
     * 失败响应：使用默认 500 状态码。
     *
     * @param message 错误提示信息
     * @return code=500、message=提示信息的响应对象
     */
    public static <T> ApiResponse<T> fail(String message) {
        ApiResponse<T> response = new ApiResponse<T>();
        response.setCode(500);
        response.setMessage(message);
        return response;
    }

    /**
     * 失败响应：自定义状态码。
     *
     * @param code    业务状态码（如 400/401/403/409）
     * @param message 错误提示信息
     * @return code、message 均自定义的响应对象
     */
    public static <T> ApiResponse<T> fail(int code, String message) {
        ApiResponse<T> response = new ApiResponse<T>();
        response.setCode(code);
        response.setMessage(message);
        return response;
    }

    public Integer getCode() {
        return this.code;
    }

    public String getMessage() {
        return this.message;
    }

    public T getData() {
        return this.data;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setData(T data) {
        this.data = data;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ApiResponse)) {
            return false;
        }
        ApiResponse other = (ApiResponse)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Integer this$code = this.getCode();
        Integer other$code = other.getCode();
        if (this$code == null ? other$code != null : !((Object)this$code).equals(other$code)) {
            return false;
        }
        String this$message = this.getMessage();
        String other$message = other.getMessage();
        if (this$message == null ? other$message != null : !this$message.equals(other$message)) {
            return false;
        }
        Object this$data = this.getData();
        Object other$data = other.getData();
        return this$data == null ? other$data == null : this$data.equals(other$data);
    }

    protected boolean canEqual(Object other) {
        return other instanceof ApiResponse;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $code = this.getCode();
        result = result * 59 + ($code == null ? 43 : ((Object)$code).hashCode());
        String $message = this.getMessage();
        result = result * 59 + ($message == null ? 43 : $message.hashCode());
        T $data = this.getData();
        result = result * 59 + ($data == null ? 43 : $data.hashCode());
        return result;
    }

    public String toString() {
        return "ApiResponse(code=" + this.getCode() + ", message=" + this.getMessage() + ", data=" + this.getData() + ")";
    }
}
