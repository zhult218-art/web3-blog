package com.web3.forum.controller;

import com.web3.common.core.FileSecurityChecker;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 聊天图片上传：POST /chat/upload (multipart/form-data, field=file)
 * 权限：需未被收回 chat 权限（互动类，默认拥有，管理员可收回）
 * 安全：调用 {@link FileSecurityChecker#validate} 做扩展名白名单 + 魔数 + 危险内容扫描，
 * 防止伪装扩展名的恶意文件（可执行/脚本）进入服务器。
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

    /** 聊天图片大小上限 5MB */
    private static final long MAX_SIZE = 5_000_000L;

    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file,
                                      @RequestHeader(value = "Authorization", required = false) String authorization) throws IOException {
        AuthUtils.requirePerm(authorization, this.jwtUtil, this.jwtProperties, "chat", true);
        // 复用共享 FileSecurityChecker：扩展名白名单 + 魔数 + 危险内容扫描
        FileSecurityChecker.validate(file, MAX_SIZE, FileSecurityChecker.IMAGE_EXTENSIONS);
        // validate 已确认扩展名合法，直接提取
        String ext = FileSecurityChecker.extractExtension(file.getOriginalFilename());
        if (ext == null) ext = "jpg";

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

