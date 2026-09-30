package com.web3.resource.security;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 类名：FileSecurityChecker
 * 所属模块：resource-service（资源服务）
 * 职责：对上传的资源文件执行安全校验，防止恶意文件（可执行文件/脚本/伪装文件）进入服务器：
 *       1. 扩展名白名单校验（允许文档/图片/音视频/压缩包/代码文本，拒绝可执行文件与脚本）
 *       2. 文件名合法性校验（拒绝路径分隔符、..、空格等）
 *       3. 文件大小上限校验
 *       4. 魔数校验：对已知二进制格式（图片/PDF/ZIP/音视频）校验文件头，防止扩展名伪装
 *       5. 危险内容特征扫描：可执行文件头（MZ/ELF/Java class）与脚本载荷特征
 */
public class FileSecurityChecker {

    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            // 文档
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md", "csv", "rtf",
            // 图片
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "ico",
            // 视频
            "mp4", "webm", "mov", "mkv", "avi", "flv", "wmv",
            // 音频
            "mp3", "wav", "flac", "aac", "ogg", "m4a",
            // 压缩包
            "zip", "rar", "7z", "tar", "gz", "bz2",
            // 代码 / 配置 / 数据
            "py", "java", "c", "cpp", "h", "go", "rs", "rb", "php",
            "json", "xml", "yaml", "yml", "html", "css", "ts", "sql", "log", "ini", "conf", "properties", "toml"
    ));

    private static final Set<String> DANGEROUS_EXTENSIONS = new HashSet<>(Arrays.asList(
            "exe", "dll", "bat", "cmd", "sh", "ps1", "vbs", "js", "jse", "wsf", "wsh",
            "jar", "class", "msi", "app", "deb", "rpm", "apk", "dmg", "iso", "scr", "com"
    ));

    private static final int MAGIC_SCAN_BYTES = 64;
    private static final int DANGER_SCAN_BYTES = 1024;

    private FileSecurityChecker() {
    }

    /**
     * 提取文件扩展名（小写，不含点）
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
     *
     * @param file    上传文件
     * @param maxSize 允许的最大字节数
     * @throws IllegalArgumentException 任一安全校验不通过时抛出
     * @throws IOException              读取文件内容失败
     */
    public static void validate(MultipartFile file, long maxSize) throws IllegalArgumentException, IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("文件超过大小上限（最大 " + (maxSize / 1048576L) + "MB）");
        }

        String original = file.getOriginalFilename();
        if (original == null || original.isEmpty()) {
            throw new IllegalArgumentException("文件名不合法");
        }
        String lower = original.toLowerCase();
        if (lower.indexOf("\\") >= 0 || lower.indexOf("/") >= 0 || lower.indexOf("..") >= 0 || lower.indexOf(' ') >= 0) {
            throw new IllegalArgumentException("文件名含有非法字符");
        }

        String ext = extractExtension(original);
        if (ext == null) {
            throw new IllegalArgumentException("不支持的文件类型：无扩展名");
        }
        if (DANGEROUS_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("禁止上传可执行文件或脚本：." + ext);
        }
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("不支持的文件类型：." + ext);
        }

        byte[] head = readHead(file, MAGIC_SCAN_BYTES);
        if (isDangerousExecutable(head)) {
            throw new IllegalArgumentException("检测到可执行文件头，可能是伪装的恶意文件，已拒绝上传");
        }

        if (hasKnownMagic(ext)) {
            if (!matchMagic(head, ext)) {
                throw new IllegalArgumentException("文件格式与扩展名不匹配，可能是伪装的恶意文件");
            }
        }

        byte[] dangerZone = readHead(file, DANGER_SCAN_BYTES);
        if (containsDangerPattern(dangerZone, ext)) {
            throw new IllegalArgumentException("检测到恶意代码组件，已拒绝上传");
        }
    }

    /**
     * 判断该扩展名是否有已知的魔数需要校验（文本类文件跳过魔数校验）
     */
    private static boolean hasKnownMagic(String ext) {
        switch (ext) {
            case "jpg": case "jpeg": case "png": case "gif": case "webp": case "bmp": case "ico":
            case "mp4": case "mov": case "webm": case "mkv": case "avi": case "flv": case "wmv":
            case "mp3": case "wav": case "flac":
            case "pdf": case "zip": case "rar": case "7z": case "gz":
                return true;
            default:
                return false;
        }
    }

    /**
     * 校验文件头是否为可执行文件（MZ/ELF/Java class），无论扩展名是什么都拦截
     */
    private static boolean isDangerousExecutable(byte[] head) {
        if (head.length == 0) {
            return false;
        }
        // MZ (Windows PE/DOS executable)
        if (startsWith(head, 0, new byte[]{0x4D, 0x5A})) {
            return true;
        }
        // ELF (Linux/Unix executable)
        if (startsWith(head, 0, new byte[]{0x7F, 0x45, 0x4C, 0x46})) {
            return true;
        }
        // Java class file
        if (startsWith(head, 0, new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE})) {
            return true;
        }
        return false;
    }

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

    private static boolean matchMagic(byte[] head, String ext) {
        if (head.length == 0) {
            return false;
        }
        switch (ext) {
            case "jpg":
            case "jpeg":
                return startsWith(head, 0, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
            case "png":
                return startsWith(head, 0, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
            case "gif":
                return startsWith(head, 0, new byte[]{0x47, 0x49, 0x46, 0x38});
            case "webp":
                return startsWith(head, 0, new byte[]{0x52, 0x49, 0x46, 0x46})
                        && startsWith(head, 8, new byte[]{0x57, 0x45, 0x42, 0x50});
            case "bmp":
                return startsWith(head, 0, new byte[]{0x42, 0x4D});
            case "ico":
                return startsWith(head, 0, new byte[]{0x00, 0x00, 0x01, 0x00});
            case "mp4":
            case "mov":
                return startsWith(head, 4, new byte[]{0x66, 0x74, 0x79, 0x70});
            case "webm":
            case "mkv":
                return startsWith(head, 0, new byte[]{(byte) 0x1A, (byte) 0x45, (byte) 0xDF, (byte) 0xA3});
            case "avi":
                return startsWith(head, 0, new byte[]{0x52, 0x49, 0x46, 0x46})
                        && startsWith(head, 8, new byte[]{0x41, 0x56, 0x49, 0x20});
            case "flv":
                return startsWith(head, 0, new byte[]{0x46, 0x4C, 0x56});
            case "wmv":
                return startsWith(head, 0, new byte[]{0x30, 0x26, (byte) 0xB2, 0x75, (byte) 0x8E, 0x66, (byte) 0xCF, 0x11});
            case "mp3":
                return startsWith(head, 0, new byte[]{(byte) 0xFF, (byte) 0xFB})
                        || startsWith(head, 0, new byte[]{(byte) 0xFF, (byte) 0xF3})
                        || startsWith(head, 0, new byte[]{(byte) 0xFF, (byte) 0xF2})
                        || startsWith(head, 0, new byte[]{0x49, 0x44, 0x33});
            case "wav":
                return startsWith(head, 0, new byte[]{0x52, 0x49, 0x46, 0x46})
                        && startsWith(head, 8, new byte[]{0x57, 0x41, 0x56, 0x45});
            case "flac":
                return startsWith(head, 0, new byte[]{0x66, 0x4C, 0x61, 0x43});
            case "pdf":
                return startsWith(head, 0, new byte[]{0x25, 0x50, 0x44, 0x46});
            case "zip":
            case "docx":
            case "xlsx":
            case "pptx":
            case "jar":
                return startsWith(head, 0, new byte[]{0x50, 0x4B});
            case "rar":
                return startsWith(head, 0, new byte[]{0x52, 0x61, 0x72, 0x21});
            case "7z":
                return startsWith(head, 0, new byte[]{0x37, 0x7A, (byte) 0xBC, (byte) 0xAF, 0x27, 0x1C});
            case "gz":
            case "tar":
                return startsWith(head, 0, new byte[]{0x1F, (byte) 0x8B});
            default:
                return true;
        }
    }

    /**
     * 危险内容特征扫描：对非代码类文本文件扫描脚本载荷特征
     */
    private static boolean containsDangerPattern(byte[] data, String ext) {
        if (data.length == 0) {
            return false;
        }
        // 代码类文件本身允许包含这些特征，跳过
        if (isCodeExtension(ext)) {
            return false;
        }
        String text = new String(data, java.nio.charset.StandardCharsets.ISO_8859_1);
        String lower = text.toLowerCase();
        return lower.contains("<!doctype html")
                || lower.contains("<script")
                || lower.contains("<iframe")
                || lower.contains("<?php")
                || lower.contains("javascript:")
                || lower.contains("<applet")
                || lower.contains("<object")
                || lower.contains("<embed")
                || lower.contains("onload=")
                || lower.contains("onerror=");
    }

    private static boolean isCodeExtension(String ext) {
        return Arrays.asList("html", "css", "js", "json", "xml", "yaml", "yml", "sql", "log", "ini", "conf", "properties", "md", "txt", "csv").contains(ext);
    }
}
