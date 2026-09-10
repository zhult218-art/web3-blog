package com.web3.aiproxy.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * 类名：ProxyTokenUtils
 * 所属模块：ai-proxy-service（中转站）
 * 职责：sk- 令牌的生成与哈希工具。
 *       令牌格式：sk- + 8 位可见前缀 + 32 位随机密钥（合计 sk- 后 40 位）；
 *       数据库只存 SHA-256(salt + 完整令牌) 哈希与前 8 位前缀，完整令牌仅创建时展示一次。
 */
public final class ProxyTokenUtils {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    private ProxyTokenUtils() {
    }

    /** 生成完整令牌：sk-xxxxxxxx(前缀8位)+32位密钥 */
    public static String generate() {
        return "sk-" + randomAlphabet(8) + randomAlphabet(32);
    }

    /** 从完整令牌截取 8 位可见前缀（不含 sk-） */
    public static String prefixOf(String fullToken) {
        if (fullToken == null || fullToken.length() < 11) return "";
        return fullToken.substring(3, 11);
    }

    /** SHA-256(salt + token) 十六进制哈希 */
    public static String hash(String salt, String fullToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((salt + fullToken).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static String randomAlphabet(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        return sb.toString();
    }
}
