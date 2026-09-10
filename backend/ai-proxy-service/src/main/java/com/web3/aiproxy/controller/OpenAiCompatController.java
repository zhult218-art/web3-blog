package com.web3.aiproxy.controller;

import com.web3.aiproxy.service.ProxyEngineService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 类名：OpenAiCompatController
 * 所属模块：ai-proxy-service（中转站）
 * 职责：对外 OpenAI 兼容端点 —— 用户应用把 base_url 指向本服务、api_key 换成 sk- 令牌即可调用：
 *       POST /v1/chat/completions（支持 stream SSE）、GET /v1/models、GET /v1/models/{model}。
 *       错误响应统一 OpenAI 格式 {"error":{"message":..,"type":..}}。
 */
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class OpenAiCompatController {

    private final ProxyEngineService engineService;

    /** 对话补全：JSON 透传 / stream=true 时 SSE 流式透传 */
    @PostMapping(value = "/chat/completions", consumes = "application/json")
    public ResponseEntity<?> chatCompletions(@RequestBody String body,
                                             @RequestHeader(value = "Authorization", required = false) String authorization,
                                             HttpServletRequest request) {
        return engineService.chatCompletions(body, authorization, resolveIp(request));
    }

    /** 模型清单：按令牌白名单过滤 */
    @GetMapping("/models")
    public ResponseEntity<?> models(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return engineService.listModels(authorization);
    }

    /** 单模型详情 */
    @GetMapping("/models/{model}")
    public ResponseEntity<?> modelDetail(@PathVariable String model,
                                         @RequestHeader(value = "Authorization", required = false) String authorization) {
        return engineService.modelDetail(authorization, model);
    }

    /** 客户端真实 IP：优先 X-Forwarded-For 首段 */
    private String resolveIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
