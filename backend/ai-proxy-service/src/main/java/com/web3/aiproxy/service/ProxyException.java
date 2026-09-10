package com.web3.aiproxy.service;

import lombok.Getter;

/**
 * 类名：ProxyException
 * 所属模块：ai-proxy-service（中转站）
 * 职责：中转链路业务异常 —— 携带 HTTP 状态码与 OpenAI 风格错误类型，
 *       由引擎统一转换为 {"error":{"message":...,"type":...}} 格式响应。
 */
@Getter
public class ProxyException extends RuntimeException {

    private final int status;
    private final String type;

    public ProxyException(int status, String type, String message) {
        super(message);
        this.status = status;
        this.type = type;
    }

    /** 无效/停用/过期令牌 → 401 */
    public static ProxyException unauthorized(String message) {
        return new ProxyException(401, "invalid_authentication", message);
    }

    /** 白名单不匹配/额度不足 → 403 */
    public static ProxyException forbidden(String message) {
        return new ProxyException(403, "insufficient_quota", message);
    }

    /** 上游不可用 → 503，不外泄上游细节 */
    public static ProxyException upstream() {
        return new ProxyException(503, "upstream_error", "upstream error");
    }
}
