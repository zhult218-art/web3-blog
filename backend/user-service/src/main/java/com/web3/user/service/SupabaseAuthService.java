package com.web3.user.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.web3.user.config.SupabaseProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * SupabaseAuthService
 * (zh) Supabase 免密登录（Magic Link）校验服务。
 *
 * 流程：用户在前端通过 Supabase 邮箱魔法链接完成认证拿到 access_token，
 * 前端把该 token 交给本服务；本服务调用
 *     GET {supabase.url}/auth/v1/user   (Authorization: Bearer {token})
 * 由 Supabase 服务端校验 token 有效性并返回该用户的 email。
 * 验证通过后 Controller 复用 userService.emailLogin(email)（自动注册 + 签发应用 JWT）。
 *
 * 说明：这里不自行解析 JWT/JWKS，而是交给 Supabase 校验，简单且不引入额外依赖。
 */
@Service
public class SupabaseAuthService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseAuthService.class);

    private final SupabaseProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;

    public SupabaseAuthService(SupabaseProperties props) {
        this.props = props;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(props.getTimeoutMs()))
                .build();
    }

    /**
     * 用用户的 Supabase access_token 换取邮箱（由 Supabase 服务端校验 token）。
     *
     * @param accessToken 用户在前端拿到的 Supabase 访问令牌
     * @return 校验通过时返回邮箱；token 非法/网络异常时抛出 RuntimeException
     */
    public String resolveEmail(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException("缺少 Supabase 访问令牌");
        }
        String url = stripTrailingSlash(props.getUrl()) + "/auth/v1/user";
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(props.getTimeoutMs()))
                    .header("apikey", props.getPublishableKey())
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                log.warn("[SupabaseAuthService] token 校验失败 http={} body={}", resp.statusCode(), safePreview(resp.body()));
                throw new IllegalStateException("Supabase 令牌校验失败");
            }
            JsonNode node = objectMapper.readTree(resp.body());
            String email = node.path("email").asText(null);
            if (email == null || email.isBlank()) {
                throw new IllegalStateException("Supabase 用户缺少邮箱");
            }
            return email;
        } catch (Exception e) {
            if (e instanceof IllegalStateException) throw (IllegalStateException) e;
            log.error("[SupabaseAuthService] 调用 Supabase 校验失败", e);
            throw new IllegalStateException("Supabase 校验服务暂不可用");
        }
    }

    private String stripTrailingSlash(String s) {
        s = s == null ? "" : s.trim();
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private String safePreview(String body) {
        if (body == null) return "";
        return body.length() > 200 ? body.substring(0, 200) : body;
    }
}
