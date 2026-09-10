/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.web3.common.core.ApiResponse
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.web3.jarvis.controller;

import com.web3.common.core.ApiResponse;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import com.web3.jarvis.service.JarvisService;
import com.web3.jarvis.vo.VoiceCommandVO;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Jarvis 语音助手控制器(jarvis-service 模块)。
 *
 * <p>负责对外暴露语音指令列表、指令识别、会话创建与会话查询接口,
 * 所有接口统一以 {@code /jarvis} 为前缀(由 {@code @RestController}
 * 与 {@code @RequestMapping("/jarvis")} 声明)。指令列表接口要求管理员
 * 身份;识别与会话接口在携带 Authorization 时校验并解析用户 ID。</p>
 */
@RestController
@RequestMapping(value={"/jarvis"})
public class JarvisController {
    private final JarvisService jarvisService;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    public JarvisController(JarvisService jarvisService, JwtUtil jwtUtil, JwtProperties jwtProperties) {
        this.jarvisService = jarvisService;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 查询全部语音指令(前端路由 GET /jarvis/commands),仅管理员可访问。
     *
     * @param auth 登录令牌(请求头),需具备管理员角色
     * @return 启用状态下的语音指令 VO 列表(按排序号升序)
     */
    @GetMapping(value={"/commands"})
    public ApiResponse<List<VoiceCommandVO>> getCommands(@RequestHeader(value="Authorization", required=false) String auth) {
        AuthUtils.requireAdmin(auth, this.jwtUtil, this.jwtProperties);
        return ApiResponse.ok(this.jarvisService.getAllCommands());
    }

    /**
     * 识别语音指令(前端路由 POST /jarvis/recognize):
     * 将文本指令与指令库匹配,匹配成功后发布 MQ 消息并返回语音应答。
     *
     * @param body 请求体,含 text(识别文本)与 sessionId(会话 ID,必填)
     * @param auth 登录令牌(可选,携带时解析用户 ID)
     * @return 含 sessionId、response(应答文本)、action、targetUrl、commandKey 的 Map;
     *         sessionId 缺失时返回 code=400
     */
    @PostMapping(value={"/recognize"})
    public ApiResponse<Map<String, Object>> recognize(@RequestBody Map<String, String> body, @RequestHeader(value="Authorization", required=false) String auth) {
        Long userId = null;
        if (auth != null && !auth.isBlank()) {
            userId = AuthUtils.requireUserId(auth, this.jwtUtil, this.jwtProperties);
        }
        String text = body.get("text");
        String sessionId = body.get("sessionId");
        if (sessionId == null || sessionId.isEmpty()) {
            return ApiResponse.fail((int)400, (String)"sessionId is required");
        }
        Map<String, Object> result = this.jarvisService.handleCommand(text, sessionId, userId);
        result.put("sessionId", sessionId);
        return ApiResponse.ok(result);
    }

    /**
     * 创建新的语音会话(前端路由 POST /jarvis/session),返回生成的 sessionId。
     *
     * @param auth 登录令牌(可选,携带时关联用户 ID)
     * @return 含 sessionId 的 Map
     */
    @PostMapping(value={"/session"})
    public ApiResponse<Map<String, String>> createSession(@RequestHeader(value="Authorization", required=false) String auth) {
        Long userId = null;
        if (auth != null && !auth.isBlank()) {
            userId = AuthUtils.requireUserId(auth, this.jwtUtil, this.jwtProperties);
        }
        return ApiResponse.ok(Map.of("sessionId", this.jarvisService.createSession(userId).getSessionId()));
    }

    /**
     * 查询会话详情(前端路由 GET /jarvis/session/{sessionId}),需登录用户。
     *
     * @param sessionId 会话 ID(路径参数)
     * @param auth      登录令牌(请求头)
     * @return 会话实体(VoiceSession)
     */
    @GetMapping(value={"/session/{sessionId}"})
    public ApiResponse<Object> getSession(@PathVariable String sessionId, @RequestHeader(value="Authorization") String auth) {
        AuthUtils.requireUserId(auth, this.jwtUtil, this.jwtProperties);
        return ApiResponse.ok((Object)this.jarvisService.getSession(sessionId));
    }
}
