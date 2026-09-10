package com.web3.user.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.web3.user.config.AuthProperties;
import com.web3.user.entity.User;
import com.web3.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * GoogleOAuthService
 *
 * 处理 Google OAuth2 授权码登录全流程：
 * 1. 前端获取授权 URL → 用户跳转 Google 登录
 * 2. Google 回调带 code → 此服务换取 access_token → 获取用户信息 → 查找或自动创建用户 → 签发 JWT → 重定向前端
 *
 * 开发模式：google.enabled=false 时，GET /user/oauth/google/url 直接返回空 URL，
 *          前端隐藏 Google 按钮；不走回调流程。
 */
@Service
public class GoogleOAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthService.class);
    private static final String OAUTH_STATE_PREFIX = "oauth:state:";
    private static final int STATE_TTL_SECONDS = 600;
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final AuthProperties props;
    private final UserMapper userMapper;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public GoogleOAuthService(AuthProperties props, UserMapper userMapper,
                              StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.props = props;
        this.userMapper = userMapper;
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    /**
     * 构造 Google 授权重定向 URL（含随机 state 防 CSRF）
     *
     * @return 授权 URL；google.enabled=false 时返回 null
     */
    public String buildAuthorizeUrl() {
        AuthProperties.Google g = props.getGoogle();
        if (!g.isEnabled() || g.getClientId().isBlank()) return null;
        String state = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set(OAUTH_STATE_PREFIX + state, state, STATE_TTL_SECONDS, TimeUnit.SECONDS);
        String scope = URLEncoder.encode("openid email profile", StandardCharsets.UTF_8);
        String redirect = URLEncoder.encode(g.getRedirectUri(), StandardCharsets.UTF_8);
        String clientId = URLEncoder.encode(g.getClientId(), StandardCharsets.UTF_8);
        return "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirect
                + "&response_type=code"
                + "&scope=" + scope
                + "&state=" + state
                + "&prompt=select_account";
    }

    /**
     * 处理 Google 回调，返回 (accessToken) 或抛异常
     */
    public String exchangeCodeForToken(String code, String state) {
        if (state == null || !Boolean.TRUE.equals(redis.hasKey(OAUTH_STATE_PREFIX + state))) {
            throw new RuntimeException("授权 state 无效或已过期，请重新登录");
        }
        redis.delete(OAUTH_STATE_PREFIX + state);

        AuthProperties.Google g = props.getGoogle();
        try {
            String body = "code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                    + "&client_id=" + URLEncoder.encode(g.getClientId(), StandardCharsets.UTF_8)
                    + "&client_secret=" + URLEncoder.encode(g.getClientSecret(), StandardCharsets.UTF_8)
                    + "&redirect_uri=" + URLEncoder.encode(g.getRedirectUri(), StandardCharsets.UTF_8)
                    + "&grant_type=authorization_code";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(GOOGLE_TOKEN_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(resp.body());
            if (json.has("error")) {
                throw new RuntimeException(json.get("error_description").asText("换取 access_token 失败"));
            }
            return json.get("access_token").asText();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("换取 Google access_token 网络异常", e);
        }
    }

    /**
     * 用 access_token 拉取 Google 用户信息，返回 { sub, email, name, picture }
     */
    public Map<String, String> fetchUserInfo(String accessToken) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(GOOGLE_USERINFO_URL))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET().build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(resp.body());
            Map<String, String> info = new HashMap<>();
            info.put("sub", json.path("sub").asText(""));
            info.put("email", json.path("email").asText(""));
            info.put("name", json.path("name").asText(""));
            info.put("picture", json.path("picture").asText(""));
            return info;
        } catch (Exception e) {
            throw new RuntimeException("获取 Google 用户信息失败", e);
        }
    }

    /**
     * 查找或自动创建 Google 登录用户：优先 google_id → 其次 email 绑定 → 最后自动注册
     */
    public User findOrCreateGoogleUser(Map<String, String> googleUser) {
        String sub = googleUser.get("sub");
        String email = googleUser.get("email");

        // 1. 按 google_id 查找
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getGoogleId, sub));
        if (user != null) return user;

        // 2. 按 email 绑定（已有邮箱账号直接关联 google_id）
        if (email != null && !email.isBlank()) {
            user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
            if (user != null) {
                user.setGoogleId(sub);
                userMapper.updateById(user);
                return user;
            }
        }

        // 3. 自动注册
        user = new User();
        String baseUsername = "g_" + (email != null ? email.split("@")[0] : sub.substring(0, 8));
        // 确保用户名唯一
        String username = baseUsername;
        int suffix = 0;
        while (userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username)) != null) {
            suffix++;
            username = baseUsername + suffix;
        }
        user.setUsername(username);
        user.setNickname(googleUser.getOrDefault("name", username));
        user.setEmail(email);
        user.setGoogleId(sub);
        user.setAvatar(googleUser.get("picture"));
        user.setPassword(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        user.setRole("USER");
        userMapper.insert(user);
        log.info("[GoogleOAuth] auto-created user: id={}, username={}", user.getId(), username);
        return user;
    }
}
