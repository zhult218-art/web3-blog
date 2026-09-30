package com.web3.forum.controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;

/**
 * 聊天图片上传：POST /chat/upload (multipart/form-data, field=file)
 * 权限：需未被收回 chat 权限（互动类，默认拥有，管理员可收回）
 * 返回 { url: "/uploads/chat/xxx.jpg" }
 */
@RestController
@RequestMapping("/chat")
public class ChatUploadController {

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    public ChatUploadController(JwtUtil jwtUtil, JwtProperties jwtProperties) {
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
    }

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    private static final Set<String> ALLOWED = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "avif");

    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file,
                                      @RequestHeader(value = "Authorization", required = false) String authorization) throws IOException {
        AuthUtils.requirePerm(authorization, this.jwtUtil, this.jwtProperties, "chat", true);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件为空");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        String ext = "jpg";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) ext = original.substring(dot + 1);
        // 根据 contentType 兜底
        String ct = file.getContentType() == null ? "" : file.getContentType();
        if (!ALLOWED.contains(ext)) {
            if (ct.contains("png")) ext = "png";
            else if (ct.contains("gif")) ext = "gif";
            else if (ct.contains("webp")) ext = "webp";
            else throw new IllegalArgumentException("仅支持图片文件");
        }

        String date = LocalDate.now().toString();
        // 必须转绝对路径：MultipartFile.transferTo 对相对路径会按 Tomcat
        // multipart 临时目录解析（而非 JVM 工作目录），导致 FileNotFoundException
        Path dir = Paths.get(uploadDir, "chat", date).toAbsolutePath();
        Files.createDirectories(dir);
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path target = dir.resolve(filename);
        // 流式落盘：不依赖 Part.write 的相对路径规则
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("url", "/uploads/chat/" + date + "/" + filename);
        return result;
    }
}
