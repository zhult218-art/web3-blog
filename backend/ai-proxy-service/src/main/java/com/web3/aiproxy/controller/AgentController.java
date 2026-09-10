package com.web3.aiproxy.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.web3.aiproxy.service.AgentWorkflowService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AgentController
 * (zh) Agent 聊天编排入口。
 * POST /agent/chat { "messages": [{"role":"user","content":"..."}, ...] } （SSE 流式）
 * 前端无需携带 sk- 令牌；本服务用应用级系统令牌调用中转引擎并计费。
 */
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AgentWorkflowService agentWorkflowService;

    @PostMapping(value = "/chat", consumes = "application/json")
    public ResponseEntity<?> chat(@RequestBody String rawBody, HttpServletRequest request) {
        String messagesJson = "[]";
        try {
            JsonNode root = MAPPER.readTree(rawBody);
            JsonNode messages = root.path("messages");
            messagesJson = messages.isArray() ? messages.toString() : "[]";
        } catch (Exception e) {
            // 忽略，使用空消息
        }
        return (ResponseEntity<?>) agentWorkflowService.chat(messagesJson, resolveIp(request));
    }

    private String resolveIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
