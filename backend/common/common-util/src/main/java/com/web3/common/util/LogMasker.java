package com.web3.common.util;

/**
 * LogMasker —— 日志脱敏工具
 * <p>
 * 所属模块：common-util（公共工具）。供所有服务在日志中输出敏感字段时调用。
 * <p>
 * 职责：对手机号 / 邮箱 / 令牌 / 验证码 / 密码 等敏感字段做掩码处理，
 * 只保留必要的前后片段，避免敏感信息落盘到日志文件。
 * <ul>
 *   <li>maskPhone("13812345678") → "138****5678"</li>
 *   <li>maskEmail("abc@example.com") → "a***@example.com"</li>
 *   <li>maskToken("Bearer eyJhbGc...") → "Bearer eyJ***"</li>
 *   <li>maskCode("123456") → "1***56"</li>
 *   <li>maskPassword(...) → "***"</li>
 * </ul>
 */
public final class LogMasker {

    private LogMasker() {
    }

    /**
     * 手机号脱敏：保留前 3 后 4，中间 4 位用 * 替换。
     * <p>非 11 位或为 null 时原样返回（不阻断业务日志）。
     *
     * @param phone 原始手机号
     * @return 脱敏后字符串
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return maskSensitive(phone);
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /**
     * 邮箱脱敏：@ 前保留首字符，其余用 * 替换。
     *
     * @param email 原始邮箱
     * @return 脱敏后字符串
     */
    public static String maskEmail(String email) {
        if (email == null) return null;
        int at = email.indexOf('@');
        if (at <= 0) return maskSensitive(email);
        return email.charAt(0) + "***" + email.substring(at);
    }

    /**
     * 令牌脱敏：保留前 8 字符（含 Bearer 前缀），其余用 *** 替换。
     *
     * @param token 原始令牌（如 "Bearer eyJhbGciOi..."）
     * @return 脱敏后字符串
     */
    public static String maskToken(String token) {
        if (token == null) return null;
        if (token.length() <= 8) return "***";
        return token.substring(0, 8) + "***";
    }

    /**
     * 验证码脱敏：保留首末各 1 位，中间用 * 替换。
     *
     * @param code 原始验证码
     * @return 脱敏后字符串
     */
    public static String maskCode(String code) {
        if (code == null) return null;
        if (code.length() <= 2) return "*";
        return code.charAt(0) + "***" + code.charAt(code.length() - 1);
    }

    /**
     * 密码脱敏：统一返回 ***，不暴露任何片段。
     */
    public static String maskPassword(String password) {
        return "***";
    }

    /**
     * 通用敏感字段兜底：非空且长度 > 4 时保留首尾各 1 字符，中间用 *** 替换；
     * 否则返回 ***。
     */
    public static String maskSensitive(String value) {
        if (value == null) return null;
        if (value.length() <= 4) return "***";
        return value.charAt(0) + "***" + value.charAt(value.length() - 1);
    }
}
