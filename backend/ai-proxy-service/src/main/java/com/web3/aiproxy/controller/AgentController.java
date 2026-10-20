package com.web3.aiproxy.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.web3.aiproxy.service.AgentFlowService;
import com.web3.aiproxy.service.AgentWorkflowService;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * AgentController
 * (zh) Agent 聊天编排入口 + 多步技能工作流（Workflow）入口。
 * POST /agent/chat { "messages": [...] }          —— 自由对话（SSE 流式，需登录）
 * GET  /agent/workflows                            —— 技能列表（公开）
 * GET  /agent/models                               —— 可用模型列表（公开，供会话台模型选择器）
 * POST /agent/workflow/match { "message":"..." }   —— 关键词匹配技能（公开）
 * POST /agent/workflow/execute { "slug","answers" }—— 执行技能（SSE 产出产物，需登录）
 * 前端无需携带 sk- 令牌；本服务用应用级系统令牌调用中转引擎并计费，
 * 故 chat/execute 必须登录（防匿名刷计费接口），workflows/match 为静态数据保持公开。
 */
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private static final Logger log = LoggerFactory.getLogger(AgentController.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AgentWorkflowService agentWorkflowService;
    private final AgentFlowService agentFlowService;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    @PostMapping(value = "/chat", consumes = "application/json")
    public ResponseEntity<StreamingResponseBody> chat(@RequestBody String rawBody, HttpServletRequest request,
                                                      @RequestHeader(value = "Authorization", required = false) String auth) {
        // 计费接口：必须登录（匿名可刷运营商账单）
        AuthUtils.requireUserId(auth, jwtUtil, jwtProperties);
        String messagesJson = "[]";
        String model = null;
        try {
            JsonNode root = MAPPER.readTree(rawBody);
            JsonNode messages = root.path("messages");
            messagesJson = messages.isArray() ? messages.toString() : "[]";
            // 会话台模型选择器传来的模型名；缺失/非法时为 null，由服务层回退默认模型
            model = root.path("model").asText(null);
        } catch (Exception e) {
            // 忽略，使用空消息
            log.debug("[agent] parse request body for messages skipped: {}", e.getMessage());
        }
        return agentWorkflowService.chat(messagesJson, model, resolveIp(request));
    }

    @GetMapping("/workflows")
    public String workflows() {
        return agentFlowService.listJson();
    }

    /** 可用模型列表（供会话台模型选择器；公开只读，不回传密钥，不涉及计费） */
    @GetMapping("/models")
    public String models() {
        return agentWorkflowService.listModels();
    }

    @PostMapping("/workflow/match")
    public String match(@RequestBody Map<String, String> body) {
        return agentFlowService.matchJson(body == null ? null : body.get("message"));
    }

    @PostMapping("/workflow/execute")
    public ResponseEntity<StreamingResponseBody> execute(@RequestBody Map<String, String> body,
                                                         HttpServletRequest request,
                                                         @RequestHeader(value = "Authorization", required = false) String auth) {
        // 计费接口：必须登录
        AuthUtils.requireUserId(auth, jwtUtil, jwtProperties);
        String slug = body == null ? null : body.get("slug");
        String answers = body == null ? null : body.get("answers");
        return agentFlowService.execute(slug, answers, resolveIp(request));
    }

    @GetMapping("/workflow/execute")
    public ResponseEntity<StreamingResponseBody> executeGet(@RequestParam(value = "slug", required = false) String slug,
                                                            @RequestParam(value = "answers", required = false) String answers,
                                                            HttpServletRequest request,
                                                            @RequestHeader(value = "Authorization", required = false) String auth) {
        // 计费接口：必须登录
        AuthUtils.requireUserId(auth, jwtUtil, jwtProperties);
        return agentFlowService.execute(slug, answers, resolveIp(request));
    }

    private String resolveIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
