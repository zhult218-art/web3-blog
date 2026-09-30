package com.web3.aiproxy.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.web3.aiproxy.service.AgentFlowService;
import com.web3.aiproxy.service.AgentWorkflowService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * AgentController
 * (zh) Agent 聊天编排入口 + 多步技能工作流（Workflow）入口。
 * POST /agent/chat { "messages": [...] }          —— 自由对话（SSE 流式）
 * GET  /agent/workflows                            —— 技能列表
 * POST /agent/workflow/match { "message":"..." }   —— 关键词匹配技能
 * POST /agent/workflow/execute { "slug","answers" }—— 执行技能（SSE 产出产物）
 * 前端无需携带 sk- 令牌；本服务用应用级系统令牌调用中转引擎并计费。
 */
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AgentWorkflowService agentWorkflowService;
    private final AgentFlowService agentFlowService;

    @PostMapping(value = "/chat", consumes = "application/json")
    public ResponseEntity<StreamingResponseBody> chat(@RequestBody String rawBody, HttpServletRequest request) {
        String messagesJson = "[]";
        try {
            JsonNode root = MAPPER.readTree(rawBody);
            JsonNode messages = root.path("messages");
            messagesJson = messages.isArray() ? messages.toString() : "[]";
        } catch (Exception e) {
            // 忽略，使用空消息
        }
        return agentWorkflowService.chat(messagesJson, resolveIp(request));
    }

    @GetMapping("/workflows")
    public String workflows() {
        return agentFlowService.listJson();
    }

    @PostMapping("/workflow/match")
    public String match(@RequestBody Map<String, String> body) {
        return agentFlowService.matchJson(body == null ? null : body.get("message"));
    }

    @PostMapping("/workflow/execute")
    public ResponseEntity<StreamingResponseBody> execute(@RequestBody Map<String, String> body,
                                                         HttpServletRequest request) {
        String slug = body == null ? null : body.get("slug");
        String answers = body == null ? null : body.get("answers");
        return agentFlowService.execute(slug, answers, resolveIp(request));
    }

    @GetMapping("/workflow/execute")
    public ResponseEntity<StreamingResponseBody> executeGet(@RequestParam(value = "slug", required = false) String slug,
                                                            @RequestParam(value = "answers", required = false) String answers,
                                                            HttpServletRequest request) {
        return agentFlowService.execute(slug, answers, resolveIp(request));
    }

    private String resolveIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
