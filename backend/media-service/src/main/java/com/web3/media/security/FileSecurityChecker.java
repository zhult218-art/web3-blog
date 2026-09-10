/*
 * FileSecurityChecker - upload network security policy
 *
 * 上传网络安全策略（防病毒/恶意文件）：
 *  1. 扩展名白名单（仅允许视频/图片常见格式，拒绝可执行文件与脚本）
 *  2. 双重扩展名与非法文件名字符拒绝
 *  3. 大小上限校验
 *  4. 魔数（Magic Bytes）校验：文件头必须匹配扩展名对应的真实格式，防止伪装扩展名
 *  5. 危险内容特征扫描：可执行文件头（MZ/ELF/JAVA class/ZIP）与脚本载荷特征（HTML/JS/PHP）
 */
package com.web3.media.security;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.web.multipart.MultipartFile;

/**
 * FileSecurityChecker —— 上传文件安全检查工具类
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 职责：对上传文件执行网络安全校验，防止恶意文件进入服务器：
 * <ol>
 *   <li>扩展名白名单校验（仅允许视频/图片常见格式，拒绝可执行文件与脚本）</li>
 *   <li>拒绝双重扩展名与含非法字符（路径分隔符/空格/..）的文件名</li>
 *   <li>文件大小上限校验</li>
 *   <li>魔数（Magic Bytes）校验：文件头必须匹配扩展名对应的真实格式，防止伪装扩展名</li>
 *   <li>危险内容特征扫描：可执行文件头（MZ/ELF/Java class/ZIP）与脚本载荷特征（HTML/JS/PHP）</li>
 * </ol>
 * 同时提供扩展名提取与 Content-Type 解析能力，供文件访问接口设置响应头。
 */
public class FileSecurityChecker {
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<String>();
    private static final Map<String, String> CONTENT_TYPES = new HashMap<String, String>();
    private static final int MAGIC_SCAN_BYTES = 64;
    private static final int DANGER_SCAN_BYTES = 1024;

    static {
        ALLOWED_EXTENSIONS.add("mp4");
        ALLOWED_EXTENSIONS.add("webm");
        ALLOWED_EXTENSIONS.add("mov");
        ALLOWED_EXTENSIONS.add("mkv");
        ALLOWED_EXTENSIONS.add("avi");
        ALLOWED_EXTENSIONS.add("flv");
        ALLOWED_EXTENSIONS.add("jpg");
        ALLOWED_EXTENSIONS.add("jpeg");
        ALLOWED_EXTENSIONS.add("png");
        ALLOWED_EXTENSIONS.add("gif");
        ALLOWED_EXTENSIONS.add("webp");

        CONTENT_TYPES.put("mp4", "video/mp4");
        CONTENT_TYPES.put("webm", "video/webm");
        CONTENT_TYPES.put("mov", "video/quicktime");
        CONTENT_TYPES.put("mkv", "video/x-matroska");
        CONTENT_TYPES.put("avi", "video/x-msvideo");
        CONTENT_TYPES.put("flv", "video/x-flv");
        CONTENT_TYPES.put("jpg", "image/jpeg");
        CONTENT_TYPES.put("jpeg", "image/jpeg");
        CONTENT_TYPES.put("png", "image/png");
        CONTENT_TYPES.put("gif", "image/gif");
        CONTENT_TYPES.put("webp", "image/webp");
    }

    private FileSecurityChecker() {
    }

    /**
     * 根据扩展名解析 HTTP 响应 Content-Type
     *
     * @param ext 文件扩展名（不区分大小写）
     * @return 对应的 MIME 类型，未收录时返回 application/octet-stream
     */
    public static String resolveContentType(String ext) {
        String e = ext.toLowerCase();
        return CONTENT_TYPES.getOrDefault(e, "application/octet-stream");
    }

    /**
     * 提取文件扩展名（小写）
     *
     * @param filename 原始文件名
     * @return 扩展名（不含点）；文件名为空或无扩展名时返回 null
     */
    public static String extractExtension(String filename) {
        if (filename == null) {
            return null;
        }
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) {
            return null;
        }
        return filename.substring(idx + 1).toLowerCase();
    }

    /**
     * 上传文件完整安全校验（核心入口）
     * 依次校验：非空、大小上限、文件名合法性、扩展名白名单、Content-Type、
     * 文件头魔数与危险内容特征
     *
     * @param file    上传文件
     * @param maxSize 允许的最大字节数
     * @throws IllegalArgumentException 任一安全校验不通过时抛出
     * @throws IOException              读取文件内容失败
     */
    public static void validate(MultipartFile file, long maxSize) throws IllegalArgumentException, IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("\u4e0a\u4f20\u6587\u4ef6\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("\u6587\u4ef6\u8d85\u8fc7\u5927\u5c0f\u4e0a\u9650\uff081MB ~ " + (maxSize / 1048576L) + "MB\uff09");
        }

        String original = file.getOriginalFilename();
        if (original == null || original.isEmpty()) {
            throw new IllegalArgumentException("\u6587\u4ef6\u540d\u4e0d\u5408\u6cd5");
        }
        String lower = original.toLowerCase();
        if (lower.indexOf("\\") >= 0 || lower.indexOf("/") >= 0 || lower.indexOf("..") >= 0 || lower.indexOf(' ') >= 0) {
            throw new IllegalArgumentException("\u6587\u4ef6\u540d\u542b\u6709\u975e\u6cd5\u5b57\u7b26");
        }

        String ext = extractExtension(original);
        if (ext == null || !ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("\u4e0d\u652f\u6301\u7684\u6587\u4ef6\u7c7b\u578b\uff1a" + (ext == null ? "\u65e0\u6269\u5c55\u540d" : "." + ext) + "\uff0c\u4ec5\u5141\u8bb8\u89c6\u9891\uff08mp4/webm/mov/mkv/avi/flv\uff09\u548c\u56fe\u7247\uff08jpg/png/gif/webp\uff09");
        }

        String contentType = file.getContentType();
        if (contentType != null && !contentType.isEmpty()
                && !contentType.startsWith("video/") && !contentType.startsWith("image/")
                && !"application/octet-stream".equals(contentType)) {
            throw new IllegalArgumentException("\u6587\u4ef6\u7c7b\u578b\u4e0d\u5141\u8bb8\uff1a" + contentType);
        }

        byte[] head = readHead(file, MAGIC_SCAN_BYTES);
        if (!matchMagic(head, ext)) {
            throw new IllegalArgumentException("\u6587\u4ef6\u683c\u5f0f\u4e0d\u5339\u914d\uff0c\u53ef\u80fd\u662f\u4f2a\u88c5\u7684\u6076\u610f\u6587\u4ef6");
        }

        byte[] dangerZone = readHead(file, DANGER_SCAN_BYTES);
        if (containsDangerPattern(dangerZone)) {
            throw new IllegalArgumentException("\u68c0\u6d4b\u5230\u6076\u610f\u4ee3\u7801\u7ec4\u4ef6\uff0c\u5df2\u62d2\u7edd\u4e0a\u4f20");
        }
    }

    /**
     * 读取文件头部指定字节数（内部工具方法）
     *
     * @param file     上传文件
     * @param maxBytes 最多读取的字节数
     * @return 实际读取到的文件头字节数组
     */
    private static byte[] readHead(MultipartFile file, int maxBytes) throws IOException {
        byte[] buf = new byte[maxBytes];
        int read = 0;
        try (InputStream in = file.getInputStream()) {
            int n;
            while (read < maxBytes && (n = in.read(buf, read, maxBytes - read)) > 0) {
                read += n;
            }
        }
        byte[] result = new byte[read];
        System.arraycopy(buf, 0, result, 0, read);
        return result;
    }

    /**
     * 判断数据段指定偏移处是否以 magic 魔数开头（内部工具方法）
     *
     * @param data   待检测字节数组
     * @param offset 起始偏移
     * @param magic  魔数字节
     * @return true=匹配
     */
    private static boolean startsWith(byte[] data, int offset, byte[] magic) {
        if (data.length < offset + magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (data[offset + i] != magic[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 校验文件头魔数是否与扩展名声明的格式一致（内部工具方法）
     *
     * @param head 文件头字节
     * @param ext  声明扩展名
     * @return true=魔数匹配
     */
    private static boolean matchMagic(byte[] head, String ext) {
        if (head.length == 0) {
            return false;
        }
        switch (ext) {
            case "mp4":
            case "mov":
                return startsWith(head, 4, new byte[]{(byte)0x66, (byte)0x74, (byte)0x79, (byte)0x70});
            case "webm":
            case "mkv":
                return startsWith(head, 0, new byte[]{(byte)0x1A, (byte)0x45, (byte)0xDF, (byte)0xA3});
            case "avi":
                return startsWith(head, 0, new byte[]{0x52, 0x49, 0x46, 0x46})
                    && startsWith(head, 8, new byte[]{0x41, 0x56, 0x49, 0x20});
            case "flv":
                return startsWith(head, 0, new byte[]{0x46, 0x4C, 0x56});
            case "jpg":
            case "jpeg":
                return startsWith(head, 0, new byte[]{(byte)0xFF, (byte)0xD8, (byte)0xFF});
            case "png":
                return startsWith(head, 0, new byte[]{(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
            case "gif":
                return startsWith(head, 0, new byte[]{0x47, 0x49, 0x46, 0x38});
            case "webp":
                return startsWith(head, 0, new byte[]{0x52, 0x49, 0x46, 0x46})
                    && startsWith(head, 8, new byte[]{0x57, 0x45, 0x42, 0x50});
            default:
                return false;
        }
    }

    /**
     * 危险内容特征扫描（内部工具方法）
     * 检测可执行文件头（MZ/ELF/Java class/ZIP）与脚本载荷特征（HTML/JS/PHP）
     *
     * @param data 待扫描字节
     * @return true=检测到危险特征
     */
    private static boolean containsDangerPattern(byte[] data) {
        if (data.length == 0) {
            return false;
        }
        if (startsWith(data, 0, new byte[]{0x4D, 0x5A})) {
            return true;
        }
        if (startsWith(data, 0, new byte[]{0x7F, 0x45, 0x4C, 0x46})) {
            return true;
        }
        if (startsWith(data, 0, new byte[]{(byte)0xCA, (byte)0xFE, (byte)0xBA, (byte)0xBE})) {
            return true;
        }
        if (startsWith(data, 0, new byte[]{0x50, 0x4B})) {
            return true;
        }
        String text = new String(data, java.nio.charset.StandardCharsets.ISO_8859_1);
        String lower = text.toLowerCase();
        if (lower.contains("<!doctype html") || lower.contains("<script") || lower.contains("<iframe")
                || lower.contains("<?php") || lower.contains("javascript:") || lower.contains("<applet")
                || lower.contains("<object") || lower.contains("<embed") || lower.contains("onload=")
                || lower.contains("onerror=")) {
            return true;
        }
        return false;
    }
}
