package com.web3.aiproxy.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 类名：AesGcmUtils
 * 所属模块：ai-proxy-service（中转站）
 * 职责：渠道上游 Key 的 AES-GCM 加解密工具（加密存储，管理端仅展示脱敏后缀）。
 */
public final class AesGcmUtils {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;

    private AesGcmUtils() {
    }

    /** 由配置口令派生 256 位 AES 密钥（SHA-256 摘要作 key material） */
    private static SecretKeySpec deriveKey(String secret) {
        try {
            byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                    .digest(secret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("AES key derivation failed", e);
        }
    }

    /** 明文 → Base64(iv + cipherText) */
    public static String encrypt(String secret, String plainText) {
        try {
            byte[] iv = new byte[IV_LEN];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, deriveKey(secret), new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv).put(encrypted);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("AES encrypt failed", e);
        }
    }

    /** Base64(iv + cipherText) → 明文 */
    public static String decrypt(String secret, String encoded) {
        try {
            byte[] all = Base64.getDecoder().decode(encoded);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(secret),
                    new GCMParameterSpec(TAG_BITS, all, 0, IV_LEN));
            byte[] plain = cipher.doFinal(all, IV_LEN, all.length - IV_LEN);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("AES decrypt failed", e);
        }
    }

    /** 脱敏展示：仅保留末 4 位 */
    public static String mask(String decryptedKey) {
        if (decryptedKey == null || decryptedKey.length() <= 4) return "****";
        return "sk-****" + decryptedKey.substring(decryptedKey.length() - 4);
    }
}
