package com.web3.common.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.web.multipart.MultipartFile;

/**
 * FileSecurityChecker —— 上传文件安全检查工具类（共享模块）
 * <p>
 * 所属模块：common-core（公共服务）。供 media-service / resource-service / forum-service 共用。
 * <p>
 * 职责：对上传文件执行网络安全校验，防止恶意文件进入服务器：
 * <ol>
 *   <li>扩展名白名单校验（拒绝可执行文件与脚本）</li>
 *   <li>拒绝双重扩展名与含非法字符（路径分隔符/空格/..）的文件名</li>
 *   <li>文件大小上限校验</li>
 *   <li>魔数（Magic Bytes）校验：文件头必须匹配扩展名对应的真实格式，防止伪装扩展名</li>
 *   <li>危险内容特征扫描：可执行文件头（MZ/ELF/Java class/ZIP）与脚本载荷特征（HTML/JS/PHP）</li>
 * </ol>
 * 同时提供扩展名提取与 Content-Type 解析能力。
 */
public class FileSecurityChecker {
    /** 默认允许的扩展名（视频+图片），供 media/resource 服务使用 */
    private static final Set<String> DEFAULT_ALLOWED_EXTENSIONS;
    /** 仅图片扩展名，供聊天图片上传等场景使用 */
    public static final Set<String> IMAGE_EXTENSIONS;
    private static final Map<String, String> CONTENT_TYPES = new HashMap<>();
    private static final int MAGIC_SCAN_BYTES = 64;
    private static final int DANGER_SCAN_BYTES = 1024;

    static {
        Set<String> d = new HashSet<>();
        d.add("mp4"); d.add("webm"); d.add("mov"); d.add("mkv"); d.add("avi"); d.add("flv");
        d.add("jpg"); d.add("jpeg"); d.add("png"); d.add("gif"); d.add("webp");
        DEFAULT_ALLOWED_EXTENSIONS = Collections.unmodifiableSet(d);

        Set<String> img = new HashSet<>();
        img.add("jpg"); img.add("jpeg"); img.add("png"); img.add("gif"); img.add("webp");
        img.add("bmp"); img.add("avif");
        IMAGE_EXTENSIONS = Collections.unmodifiableSet(img);

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
        CONTENT_TYPES.put("bmp", "image/bmp");
        CONTENT_TYPES.put("avif", "image/avif");
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
        if (ext == null) return "application/octet-stream";
        return CONTENT_TYPES.getOrDefault(ext.toLowerCase(), "application/octet-stream");
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
     * 上传文件完整安全校验（默认视频+图片白名单）
     *
     * @param file    上传文件
     * @param maxSize 允许的最大字节数
     * @throws IllegalArgumentException 任一安全校验不通过时抛出
     * @throws IOException              读取文件内容失败
     */
    public static void validate(MultipartFile file, long maxSize) throws IllegalArgumentException, IOException {
        validate(file, maxSize, DEFAULT_ALLOWED_EXTENSIONS);
    }

    /**
     * 上传文件完整安全校验（自定义扩展名白名单）
     * <p>依次校验：非空、大小上限、文件名合法性、扩展名白名单、Content-Type、
     * 文件头魔数与危险内容特征。
     *
     * @param file              上传文件
     * @param maxSize          允许的最大字节数
     * @param allowedExtensions 允许的扩展名集合（小写）
     * @throws IllegalArgumentException 任一安全校验不通过时抛出
     * @throws IOException              读取文件内容失败
     */
    public static void validate(MultipartFile file, long maxSize, Set<String> allowedExtensions)
            throws IllegalArgumentException, IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("文件超过大小上限（1MB ~ " + (maxSize / 1048576L) + "MB）");
        }

        String original = file.getOriginalFilename();
        if (original == null || original.isEmpty()) {
            throw new IllegalArgumentException("文件名不合法");
        }
        String lower = original.toLowerCase();
        if (lower.indexOf("\\") >= 0 || lower.indexOf("/") >= 0
                || lower.indexOf("..") >= 0 || lower.indexOf(' ') >= 0) {
            throw new IllegalArgumentException("文件名含有非法字符");
        }
        // 双重扩展名检测：如 file.jpg.exe
        int firstDot = lower.indexOf('.');
        int lastDot = lower.lastIndexOf('.');
        if (firstDot != lastDot) {
            throw new IllegalArgumentException("拒绝双重扩展名文件：" + original);
        }

        String ext = extractExtension(original);
        if (ext == null || !allowedExtensions.contains(ext)) {
            throw new IllegalArgumentException("不支持的文件类型："
                    + (ext == null ? "无扩展名" : "." + ext)
                    + "，仅允许：" + allowedExtensions);
        }

        String contentType = file.getContentType();
        if (contentType != null && !contentType.isEmpty()
                && !contentType.startsWith("video/") && !contentType.startsWith("image/")
                && !"application/octet-stream".equals(contentType)) {
            throw new IllegalArgumentException("文件类型不允许：" + contentType);
        }

        byte[] head = readHead(file, MAGIC_SCAN_BYTES);
        if (!matchMagic(head, ext)) {
            throw new IllegalArgumentException("文件格式不匹配，可能是伪装的恶意文件");
        }

        byte[] dangerZone = readHead(file, DANGER_SCAN_BYTES);
        if (containsDangerPattern(dangerZone)) {
            throw new IllegalArgumentException("检测到恶意代码组件，已拒绝上传");
        }
    }

    /** 读取文件头部指定字节数 */
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

    /** 判断数据段指定偏移处是否以 magic 魔数开头 */
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

    /** 校验文件头魔数是否与扩展名声明的格式一致 */
    private static boolean matchMagic(byte[] head, String ext) {
        if (head.length == 0) {
            return false;
        }
        switch (ext) {
            case "mp4":
            case "mov":
                return startsWith(head, 4, new byte[]{0x66, 0x74, 0x79, 0x70});
            case "webm":
            case "mkv":
                return startsWith(head, 0, new byte[]{(byte) 0x1A, 0x45, (byte) 0xDF, (byte) 0xA3});
            case "avi":
                return startsWith(head, 0, new byte[]{0x52, 0x49, 0x46, 0x46})
                        && startsWith(head, 8, new byte[]{0x41, 0x56, 0x49, 0x20});
            case "flv":
                return startsWith(head, 0, new byte[]{0x46, 0x4C, 0x56});
            case "jpg":
            case "jpeg":
                return startsWith(head, 0, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
            case "png":
                return startsWith(head, 0, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, (byte) 0x1A, 0x0A});
            case "gif":
                return startsWith(head, 0, new byte[]{0x47, 0x49, 0x46, 0x38});
            case "webp":
                return startsWith(head, 0, new byte[]{0x52, 0x49, 0x46, 0x46})
                        && startsWith(head, 8, new byte[]{0x57, 0x45, 0x42, 0x50});
            case "bmp":
                return startsWith(head, 0, new byte[]{0x42, 0x4D});
            case "avif":
                // AVIF: ftyp box with brand 'avif' or 'avis'
                return startsWith(head, 4, new byte[]{0x61, 0x76, 0x69, 0x66})
                        || startsWith(head, 4, new byte[]{0x61, 0x76, 0x69, 0x73});
            default:
                return false;
        }
    }

    /**
     * 危险内容特征扫描：检测可执行文件头（MZ/ELF/Java class/ZIP）与脚本载荷特征（HTML/JS/PHP）
     */
    private static boolean containsDangerPattern(byte[] data) {
        if (data.length == 0) {
            return false;
        }
        if (startsWith(data, 0, new byte[]{0x4D, 0x5A})) return true;             // Windows PE (MZ)
        if (startsWith(data, 0, new byte[]{0x7F, 0x45, 0x4C, 0x46})) return true; // ELF
        if (startsWith(data, 0, new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE})) return true; // Java class
        if (startsWith(data, 0, new byte[]{0x50, 0x4B})) return true;             // ZIP/Office 宏文档
        String text = new String(data, StandardCharsets.ISO_8859_1);
        String lower = text.toLowerCase();
        if (lower.contains("<!doctype html") || lower.contains("<script")
                || lower.contains("<iframe") || lower.contains("<?php")
                || lower.contains("javascript:") || lower.contains("<applet")
                || lower.contains("<object") || lower.contains("<embed")
                || lower.contains("onload=") || lower.contains("onerror=")) {
            return true;
        }
        return false;
    }
}
