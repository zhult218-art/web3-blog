package com.web3.aiproxy.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.web3.aiproxy.config.AiProxyProperties;
import com.web3.aiproxy.entity.ProxyChannel;
import com.web3.aiproxy.entity.ProxyModel;
import com.web3.aiproxy.entity.ProxyRequestLog;
import com.web3.aiproxy.entity.ProxyToken;
import com.web3.aiproxy.mapper.ProxyModelMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 类名：ProxyEngineService
 * 所属模块：ai-proxy-service（中转站）
 * 职责：中转主引擎 —— 编排一次 OpenAI 兼容请求的完整生命周期：
 *       令牌鉴权(Redis 缓存) → IP/模型白名单 → 额度预检 → RPM 滑窗限流
 *       → 渠道路由(优先级+策略，失败降级) → 上游转发(流式/非流式) → usage 计量
 *       → 分组×模型倍率计费与单语句原子扣减 → 异步后置处理(日志/渠道统计/日汇总/告警)。
 *       未携带 sk- 令牌或无可用渠道时回退兜底直连地址（不计费，保持旧行为）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProxyEngineService {

    private final TokenAuthService tokenAuthService;
    private final ChannelRouterService channelRouter;
    private final BillingService billingService;
    private final RequestLogService requestLogService;
    private final UserGroupService userGroupService;
    private final UpstreamForwarder forwarder;
    private final AiProxyProperties properties;
    private final ObjectMapper objectMapper;
    private final ProxyModelMapper modelMapper;

    /** 非流式与流式的统一入口 */
    public ResponseEntity<?> chatCompletions(String body, String authorization, String clientIp) {
        long startMs = System.currentTimeMillis();
        String requestId = newRequestId();

        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (Exception e) {
            return openAiError(400, "invalid_request_error", "invalid JSON body");
        }
        String model = root.path("model").asText(null);
        boolean stream = root.path("stream").asBoolean(false);

        // ---------- 兜底直连：未带 sk- 令牌（保留旧反代行为，不计费） ----------
        if (!isSkToken(authorization)) {
            return legacyForward(requestId, null, null, model, stream, startMs,
                    () -> forwarder.forwardBlocking(properties.getFallbackBaseUrl(), null, authorization, body));
        }

        // ---------- 令牌鉴权与预检 ----------
        String fullToken = authorization.substring(7).trim();
        ProxyToken token = null;
        try {
            token = tokenAuthService.authenticate(fullToken);
            tokenAuthService.checkIpWhitelist(token, clientIp);
            if (!StringUtils.hasText(model)) {
                throw new ProxyException(400, "invalid_request_error", "model is required");
            }
            tokenAuthService.checkModelWhitelist(token, model);
            tokenAuthService.checkQuotaEnough(token);
            tokenAuthService.checkRateLimit(token);
        } catch (ProxyException pe) {
            writeLog(requestId, token, null, model, stream, 0, 0, 0L,
                    latency(startMs), pe.getStatus(), pe.getMessage(), clientIp);
            return openAiError(pe.getStatus(), pe.getType(), pe.getMessage());
        }

        List<ProxyChannel> candidates = channelRouter.candidates(model);

        // ---------- 有令牌但无启用渠道：兜底直连透传，不计费 ----------
        if (candidates.isEmpty()) {
            return legacyForward(requestId, token, fullToken, model, stream, startMs,
                    () -> forwarder.forwardBlocking(properties.getFallbackBaseUrl(), null,
                            "Bearer " + fullToken, body));
        }

        ProxyModel modelRate = modelMapper.selectOne(Wrappers.<ProxyModel>lambdaQuery()
                .eq(ProxyModel::getModelName, model).eq(ProxyModel::getStatus, 1));
        BigDecimal groupRate = userGroupService.rateOf(token.getGroupName());

        // ---------- 流式 SSE ----------
        if (stream) {
            return buildStreamResponse(candidates, token, model, modelRate, groupRate,
                    requestId, clientIp, body, startMs);
        }

        // ---------- 非流式：候选依次降级 ----------
        Exception lastFailure = null;
        for (ProxyChannel ch : candidates) {
            String apiKey;
            try {
                apiKey = decryptKey(ch);
            } catch (Exception e) {
                lastFailure = e;
                continue;
            }
            try {
                ResponseEntity<byte[]> resp = forwarder.forwardBlocking(ch.getBaseUrl(), apiKey, null, body);
                int sc = resp.getStatusCode().value();
                if (sc >= 200 && sc < 300) {
                    return finishSuccess(requestId, token, model, modelRate, groupRate, ch,
                            startMs, resp.getBody(), clientIp);
                }
                if (sc < 500) { // 上游 4xx 属调用方问题：记账后原样透传
                    finishFailure(requestId, token, model, ch, startMs, sc,
                            truncate(bodyOf(resp)), clientIp);
                    return ResponseEntity.status(sc).contentType(MediaType.APPLICATION_JSON).body(resp.getBody());
                }
                lastFailure = new UpstreamForwarder.UpstreamStatusException(sc, bodyOf(resp));
            } catch (org.springframework.web.reactive.function.client.WebClientResponseException wcre) {
                int sc = wcre.getStatusCode().value();
                if (sc >= 200 && sc < 300) {
                    byte[] bytes = wcre.getResponseBodyAsByteArray();
                    return finishSuccess(requestId, token, model, modelRate, groupRate, ch,
                            startMs, bytes, clientIp);
                }
                if (sc < 500) {
                    finishFailure(requestId, token, model, ch, startMs, sc,
                            truncate(wcre.getResponseBodyAsString()), clientIp);
                    return openAiError(sc, "upstream_client_error", "upstream rejected request");
                }
                lastFailure = wcre;
            } catch (Exception e) {
                lastFailure = e;
            }
        }
        finishFailure(requestId, token, model, candidates.get(0), startMs, 503,
                lastFailure == null ? "all channels failed" : lastFailure.getMessage(), clientIp);
        return openAiError(HttpStatus.SERVICE_UNAVAILABLE.value(), "upstream_error", "upstream error");
    }

    /** 流式响应：先发 event-stream 头；写线程内做候选降级、字节透传、收尾计费 */
    private ResponseEntity<StreamingResponseBody> buildStreamResponse(List<ProxyChannel> candidates,
                                                                      ProxyToken token, String model,
                                                                      ProxyModel modelRate, BigDecimal groupRate,
                                                                      String requestId,
                                                                      String clientIp, String bodyJson,
                                                                      long startMs) {
        StreamingResponseBody srb = out -> {
            SseUsageCollector collector = new SseUsageCollector();
            ProxyChannel usedChannel = null;
            boolean ok = false;
            Exception lastFailure = null;
            for (ProxyChannel ch : candidates) {
                String apiKey;
                try {
                    apiKey = decryptKey(ch);
                } catch (Exception e) {
                    lastFailure = e;
                    continue;
                }
                try {
                    forwarder.forwardStreaming(ch.getBaseUrl(), apiKey, bodyJson, out, collector);
                    usedChannel = ch;
                    ok = true;
                    break;
                } catch (UpstreamForwarder.UpstreamStatusException use) {
                    lastFailure = use;
                    if (use.getStatusCode() < 500) break;
                } catch (Exception e) {
                    lastFailure = e;
                }
            }
            int pt = collector.getPromptTokens();
            int ct = collector.getCompletionTokens();
            long quotaUsed = ok ? billingService.computeQuota(modelRate, groupRate, pt, ct) : 0L;
            if (ok) billingService.deduct(token.getId(), token.getName(), token.getQuota(), quotaUsed);
            writeLog(requestId, token, usedChannel, model, true,
                    pt, ct, quotaUsed, latency(startMs), ok ? 200 : 503,
                    ok ? null : (lastFailure == null ? "all channels failed" : lastFailure.getMessage()),
                    clientIp);
        };
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .header("Cache-Control", "no-cache")
                .header("X-Accel-Buffering", "no")
                .body(srb);
    }

    /** 成功收尾：提取 usage → 分组×模型倍率计费 → 原子扣减 → 日志 */
    private ResponseEntity<byte[]> finishSuccess(String requestId, ProxyToken token, String model,
                                                 ProxyModel modelRate, BigDecimal groupRate,
                                                 ProxyChannel channel, long startMs,
                                                 byte[] respBody, String clientIp) {
        String json = respBody == null ? "" : new String(respBody, StandardCharsets.UTF_8);
        int prompt;
        int completion;
        try {
            JsonNode usage = objectMapper.readTree(json).path("usage");
            prompt = usage.path("prompt_tokens").asInt(Math.max(1, json.length() / 12));
            completion = usage.path("completion_tokens").asInt(Math.max(1, json.length() / 6));
        } catch (Exception e) {
            prompt = Math.max(1, json.length() / 12);
            completion = Math.max(1, json.length() / 6);
        }
        long quotaUsed = billingService.computeQuota(modelRate, groupRate, prompt, completion);
        billingService.deduct(token.getId(), token.getName(), token.getQuota(), quotaUsed);
        writeLog(requestId, token, channel, model, false,
                prompt, completion, quotaUsed, latency(startMs), 200, null, clientIp);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                .body(respBody == null ? new byte[0] : respBody);
    }

    private void finishFailure(String requestId, ProxyToken token, String model, ProxyChannel channel,
                               long startMs, int statusCode, String errorMsg, String clientIp) {
        writeLog(requestId, token, channel, model, false,
                0, 0, 0L, latency(startMs), statusCode, errorMsg, clientIp);
    }

    /** GET /v1/models：启用渠道声明模型 ∪ 启用倍率表模型，再按令牌白名单过滤 */
    public ResponseEntity<?> listModels(String authorization) {
        ProxyToken token = null;
        try {
            if (!isSkToken(authorization)) throw ProxyException.unauthorized("Invalid token");
            token = tokenAuthService.authenticate(authorization.substring(7).trim());
            Set<String> models = new LinkedHashSet<>();
            for (ProxyChannel ch : channelRouter.listEnabled()) {
                models.addAll(tokenAuthService.parseArray(ch.getModels()));
            }
            modelMapper.selectList(Wrappers.<ProxyModel>lambdaQuery().eq(ProxyModel::getStatus, 1))
                    .forEach(m -> models.add(m.getModelName()));
            List<String> whitelist = tokenAuthService.parseArray(token.getModelLimit());
            if (!whitelist.isEmpty()) models.retainAll(whitelist);

            StringBuilder sb = new StringBuilder("{\"object\":\"list\",\"data\":[");
            boolean first = true;
            for (String m : models) {
                if (!first) sb.append(',');
                sb.append("{\"id\":\"").append(m)
                        .append("\",\"object\":\"model\",\"created\":1700000000,\"owned_by\":\"web3-proxy\"}");
                first = false;
            }
            sb.append("]}");
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                    .body(sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (ProxyException pe) {
            writeLog(null, token, null, null, false, 0, 0, 0L, 0, pe.getStatus(),
                    pe.getMessage(), null);
            return openAiError(pe.getStatus(), pe.getType(), pe.getMessage());
        }
    }

    /** GET /v1/models/{model} */
    public ResponseEntity<?> modelDetail(String authorization, String model) {
        try {
            if (!isSkToken(authorization)) throw ProxyException.unauthorized("Invalid token");
            ProxyToken token = tokenAuthService.authenticate(authorization.substring(7).trim());
            List<String> whitelist = tokenAuthService.parseArray(token.getModelLimit());
            if (!whitelist.isEmpty() && !whitelist.contains(model)) {
                throw ProxyException.forbidden("model not allowed");
            }
            boolean exists = !channelRouter.candidates(model).isEmpty()
                    || modelMapper.selectCount(Wrappers.<ProxyModel>lambdaQuery()
                    .eq(ProxyModel::getModelName, model).eq(ProxyModel::getStatus, 1)) > 0;
            if (!exists) return openAiError(404, "not_found_error", "model not found");
            String json = "{\"id\":\"" + model
                    + "\",\"object\":\"model\",\"created\":1700000000,\"owned_by\":\"web3-proxy\"}";
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                    .body(json.getBytes(StandardCharsets.UTF_8));
        } catch (ProxyException pe) {
            return openAiError(pe.getStatus(), pe.getType(), pe.getMessage());
        }
    }

    // ---------------- 私有工具 ----------------

    private ResponseEntity<?> legacyForward(String requestId, ProxyToken token, String fullToken,
                                            String model, boolean stream,
                                            long startMs, BlockingCall call) {
        try {
            ResponseEntity<byte[]> resp = call.call();
            writeLog(requestId, token, null, model, stream, 0, 0, 0L,
                    latency(startMs), resp.getStatusCode().value(), null, null);
            return ResponseEntity.status(resp.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(resp.getBody());
        } catch (Exception e) {
            writeLog(requestId, token, null, model, stream, 0, 0, 0L,
                    latency(startMs), 503, "fallback unavailable", null);
            return openAiError(503, "upstream_error", "upstream error");
        }
    }

    @FunctionalInterface
    interface BlockingCall {
        ResponseEntity<byte[]> call() throws Exception;
    }

    private boolean isSkToken(String authorization) {
        return StringUtils.hasText(authorization) && authorization.trim().startsWith("Bearer sk-");
    }

    private String decryptKey(ProxyChannel ch) {
        return com.web3.aiproxy.util.AesGcmUtils.decrypt(
                properties.getChannelKeySecret(), ch.getApiKeyCipher());
    }

    private void writeLog(String requestId, ProxyToken token, ProxyChannel channel, String model,
                          boolean stream, int pt, int ct, long quotaCost, int latency, int status,
                          String err, String ip) {
        ProxyRequestLog entry = new ProxyRequestLog();
        entry.setRequestId(requestId != null ? requestId : newRequestId());
        entry.setTokenId(token != null ? token.getId() : null);
        entry.setTokenName(token != null ? token.getName() : null);
        entry.setChannelId(channel != null ? channel.getId() : null);
        entry.setChannelName(channel != null ? channel.getName() : null);
        // model_name 列 NOT NULL：鉴权失败等早期失败请求统一落 unknown
        entry.setModelName(StringUtils.hasText(model) ? model : "unknown");
        entry.setIsStream(stream ? 1 : 0);
        entry.setPromptTokens(pt);
        entry.setCompletionTokens(ct);
        entry.setQuotaCost(quotaCost);
        entry.setLatencyMs(latency);
        entry.setStatusCode(status);
        entry.setFallback(isSkTokenFallback(entry) ? 1 : 0);
        entry.setErrorMsg(err == null ? null : truncate(err));
        entry.setClientIp(ip);
        entry.setCreatedAt(java.time.LocalDateTime.now());
        requestLogService.saveAsync(entry);
    }

    /** 无令牌或无渠道的透传请求标记为兜底流量（channel_id 为空即兜底） */
    private boolean isSkTokenFallback(ProxyRequestLog entry) {
        return entry.getChannelId() == null && entry.getStatusCode() != null && entry.getStatusCode() > 0;
    }

    private ResponseEntity<byte[]> openAiError(int status, String type, String message) {
        String safeType = type == null ? "api_error" : type.replace("\"", "'");
        String safeMsg = message == null ? "" : message.replace("\"", "'").replace("\n", " ");
        String json = "{\"error\":{\"message\":\"" + safeMsg + "\",\"type\":\"" + safeType + "\"}}";
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON)
                .body(json.getBytes(StandardCharsets.UTF_8));
    }

    private String bodyOf(ResponseEntity<byte[]> resp) {
        byte[] b = resp.getBody();
        return b == null ? "" : new String(b, StandardCharsets.UTF_8);
    }

    private String truncate(String s) {
        if (s == null) return null;
        return s.length() > 900 ? s.substring(0, 900) : s;
    }

    private int latency(long startMs) {
        return (int) Math.max(0, System.currentTimeMillis() - startMs);
    }

    private String newRequestId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
