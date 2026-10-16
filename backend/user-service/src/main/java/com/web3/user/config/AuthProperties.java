package com.web3.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AuthProperties
 * (zh) 登录/认证相关配置项（application.yml 中 app.auth 前缀）：
 *      captcha-required 图形验证码开关；mail-enabled/sms-enabled 决定邮箱/短信验证码
 *      真发还是返回 devCode（开发模式）；google.* 为 OAuth 客户端配置；
 *      frontend-url 为 OAuth 回调重定向的前端地址。
 */
@Component
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    /** 登录/注册/发送验证码前是否必须通过图形验证码 */
    private boolean captchaRequired = true;

    /** 邮箱 SMTP 是否已配置可用（false 时验证码走开发模式：日志输出并回传 devCode） */
    private boolean mailEnabled = false;

    /** 短信通道是否已配置可用（false 时验证码走开发模式） */
    private boolean smsEnabled = false;

    /**
     * 邮箱/手机验证码登录遇到未知账号时是否自动注册。
     * 默认 false（关闭）：未注册账号验证码登录返回"未注册"错误，防止任意账号接管。
     * 设为 true 时恢复自动注册行为（需配合真实 SMTP/SMS 网关，避免 devCode 泄露链路）。
     */
    private boolean autoRegister = false;

    /** 前端站点地址（Google 回调 302 重定向目标 {frontend-url}/login?access_token=...） */
    private String frontendUrl = "http://localhost:5173";

    private final Google google = new Google();

    public boolean isCaptchaRequired() { return captchaRequired; }
    public void setCaptchaRequired(boolean v) { this.captchaRequired = v; }

    public boolean isMailEnabled() { return mailEnabled; }
    public void setMailEnabled(boolean v) { this.mailEnabled = v; }

    public boolean isSmsEnabled() { return smsEnabled; }
    public void setSmsEnabled(boolean v) { this.smsEnabled = v; }

    public boolean isAutoRegister() { return autoRegister; }
    public void setAutoRegister(boolean v) { this.autoRegister = v; }

    public String getFrontendUrl() { return frontendUrl; }
    public void setFrontendUrl(String v) { this.frontendUrl = v; }

    public Google getGoogle() { return google; }

    public static class Google {
        /** 是否启用 Google 授权登录（未配置 client-id 时前端隐藏入口） */
        private boolean enabled = false;
        /**
         * 是否允许 dev mock 登录（POST /user/oauth/google/dev-login）。
         * 安全默认关闭：该接口可用任意邮箱直接登录/接管已有账号，仅限本地调试显式开启
         * （环境变量 GOOGLE_DEV_LOGIN_ENABLED=true，且必须同时未启用真实 OAuth）。
         */
        private boolean devLoginEnabled = false;
        private String clientId = "";
        private String clientSecret = "";
        /** 必须与 Google Cloud Console 中配置的回调一致，如 https://api.example.com/user/oauth/google/callback */
        private String redirectUri = "";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean v) { this.enabled = v; }
        public boolean isDevLoginEnabled() { return devLoginEnabled; }
        public void setDevLoginEnabled(boolean v) { this.devLoginEnabled = v; }
        public String getClientId() { return clientId; }
        public void setClientId(String v) { this.clientId = v; }
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String v) { this.clientSecret = v; }
        public String getRedirectUri() { return redirectUri; }
        public void setRedirectUri(String v) { this.redirectUri = v; }
    }
}
