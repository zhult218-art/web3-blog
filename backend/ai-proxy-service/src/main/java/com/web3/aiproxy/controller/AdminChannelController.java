package com.web3.aiproxy.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.web3.aiproxy.config.AiProxyProperties;
import com.web3.aiproxy.entity.ProxyChannel;
import com.web3.aiproxy.mapper.ProxyChannelMapper;
import com.web3.aiproxy.service.ChannelRouterService;
import com.web3.aiproxy.service.UpstreamForwarder;
import com.web3.aiproxy.util.AesGcmUtils;
import com.web3.common.core.ApiResponse;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 类名：AdminChannelController
 * 所属模块：ai-proxy-service（中转站）
 * 职责：渠道管理接口（需管理员 JWT）—— 渠道 CRUD、启停、连通测试；
 *       上游 Key AES-GCM 加密入库（apiSecret 明文仅入参），列表仅回脱敏形式；
 *       任何变更即时失效 Redis 渠道缓存。
 */
@RestController
@RequestMapping("/admin/ai/channels")
@RequiredArgsConstructor
public class AdminChannelController {

    private final ProxyChannelMapper channelMapper;
    private final AiProxyProperties properties;
    private final UpstreamForwarder forwarder;
    private final ChannelRouterService channelRouter;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    @GetMapping
    public ApiResponse<List<ProxyChannel>> list(@RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        List<ProxyChannel> channels = channelMapper.selectList(
                Wrappers.<ProxyChannel>lambdaQuery().orderByDesc(ProxyChannel::getPriority)
                        .orderByDesc(ProxyChannel::getId));
        channels.forEach(this::maskKey);
        return ApiResponse.ok(channels);
    }

    /** 创建渠道：apiSecret 为明文入参 → AES 加密入库，永不回传 */
    @PostMapping
    public ApiResponse<ProxyChannel> create(@RequestBody ProxyChannel body,
                                            @RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        if (StringUtils.hasText(body.getApiSecret())) {
            body.setApiKeyCipher(AesGcmUtils.encrypt(properties.getChannelKeySecret(), body.getApiSecret().trim()));
            try {
                body.setMaskedKey(AesGcmUtils.mask(body.getApiSecret().trim()));
            } catch (Exception ignored) {
            }
        }
        body.setApiSecret(null);
        body.setId(null);
        if (body.getStatus() == null) body.setStatus(1);
        if (body.getWeight() == null) body.setWeight(1);
        if (body.getPriority() == null) body.setPriority(0);
        if (body.getType() == null) body.setType(1);
        body.setFailCount(0);
        body.setSuccessCount(0L);
        body.setAutoDisabled(0);
        body.setCreatedAt(LocalDateTime.now());
        body.setUpdatedAt(LocalDateTime.now());
        channelMapper.insert(body);
        channelRouter.evictCache();
        ProxyChannel view = maskKey(channelMapper.selectById(body.getId()));
        return ApiResponse.ok(view);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                    @RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        ProxyChannel exist = channelMapper.selectById(id);
        if (exist == null) return ApiResponse.fail(404, "channel not found");
        copyIfPresent(body, "name", v -> exist.setName((String) v));
        copyIfPresent(body, "baseUrl", v -> exist.setBaseUrl((String) v));
        copyIfPresent(body, "models", v -> exist.setModels(v instanceof List<?> l ? toJson(l) : (String) v));
        copyIfPresent(body, "strategy", v -> exist.setStrategy((String) v));
        copyIfPresent(body, "weight", v -> exist.setWeight(asInt(v)));
        copyIfPresent(body, "priority", v -> exist.setPriority(asInt(v)));
        copyIfPresent(body, "status", v -> exist.setStatus(asInt(v)));
        copyIfPresent(body, "balance", v -> exist.setBalance(v == null ? null : new java.math.BigDecimal(String.valueOf(v))));
        copyIfPresent(body, "config", v -> exist.setConfig(v instanceof Map<?, ?> m ? toJsonMap(m) : (String) v));
        copyIfPresent(body, "remark", v -> exist.setRemark((String) v));
        // 传了新明文 Key 才轮换加密
        Object newKey = body.get("apiSecret");
        if (StringUtils.hasText((String) newKey)) {
            String plain = ((String) newKey).trim();
            exist.setApiKeyCipher(AesGcmUtils.encrypt(properties.getChannelKeySecret(), plain));
        }
        exist.setUpdatedAt(LocalDateTime.now());
        channelMapper.updateById(exist);
        channelRouter.evictCache();
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id,
                                    @RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        channelMapper.deleteById(id);
        channelRouter.evictCache();
        return ApiResponse.ok();
    }

    /** 渠道连通测试：GET {base}/models 最小探针 */
    @PostMapping("/{id}/test")
    public ApiResponse<Map<String, Object>> test(@PathVariable Long id,
                                                 @RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        ProxyChannel ch = channelMapper.selectById(id);
        if (ch == null) return ApiResponse.fail(404, "channel not found");
        Map<String, Object> result = new HashMap<>();
        try {
            String key = AesGcmUtils.decrypt(properties.getChannelKeySecret(), ch.getApiKeyCipher());
            ResponseEntity<byte[]> resp = forwarder.probeModels(stripTrailingSlash(ch.getBaseUrl()), key);
            int sc = resp.getStatusCode().value();
            result.put("ok", sc >= 200 && sc < 300);
            result.put("statusCode", sc);
            result.put("message", sc >= 200 && sc < 300 ? "连通正常" : "上游返回 " + sc);
        } catch (Exception e) {
            result.put("ok", false);
            result.put("statusCode", 0);
            result.put("message", "连接失败：" + e.getClass().getSimpleName());
        }
        return ApiResponse.ok(result);
    }

    // ---------------- 工具 ----------------

    private ProxyChannel maskKey(ProxyChannel ch) {
        try {
            if (StringUtils.hasText(ch.getApiKeyCipher())) {
                String plain = AesGcmUtils.decrypt(properties.getChannelKeySecret(), ch.getApiKeyCipher());
                ch.setMaskedKey(AesGcmUtils.mask(plain));
            } else {
                ch.setMaskedKey("(未配置)");
            }
        } catch (Exception e) {
            ch.setMaskedKey("(解密失败)");
        }
        ch.setApiKeyCipher(null); // 永不回传密文/明文
        ch.setApiSecret(null);
        return ch;
    }

    private void copyIfPresent(Map<String, Object> body, String key, java.util.function.Consumer<Object> setter) {
        if (body.containsKey(key)) setter.accept(body.get(key));
    }

    private Integer asInt(Object v) {
        return v == null ? null : Integer.parseInt(String.valueOf(v));
    }

    private String toJson(List<?> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(String.valueOf(list.get(i)).replace("\"", "")).append('"');
        }
        return sb.append(']').toString();
    }

    private String toJsonMap(Map<?, ?> map) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(map);
        } catch (Exception e) {
            return null;
        }
    }

    private String stripTrailingSlash(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
