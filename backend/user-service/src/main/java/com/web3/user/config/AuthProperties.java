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

    /** 前端站点地址（Google 回调 302 重定向目标 {frontend-url}/login?access_token=...） */
    private String frontendUrl = "http://localhost:5173";

    private final Google google = new Google();

    public boolean isCaptchaRequired() { return captchaRequired; }
    public void setCaptchaRequired(boolean v) { this.captchaRequired = v; }

    public boolean isMailEnabled() { return mailEnabled; }
    public void setMailEnabled(boolean v) { this.mailEnabled = v; }

    public boolean isSmsEnabled() { return smsEnabled; }
    public void setSmsEnabled(boolean v) { this.smsEnabled = v; }

    public String getFrontendUrl() { return frontendUrl; }
    public void setFrontendUrl(String v) { this.frontendUrl = v; }

    public Google getGoogle() { return google; }

    public static class Google {
        /** 是否启用 Google 授权登录（未配置 client-id 时前端隐藏入口） */
        private boolean enabled = false;
        private String clientId = "";
        private String clientSecret = "";
        /** 必须与 Google Cloud Console 中配置的回调一致，如 https://api.example.com/user/oauth/google/callback */
        private String redirectUri = "";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean v) { this.enabled = v; }
        public String getClientId() { return clientId; }
        public void setClientId(String v) { this.clientId = v; }
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String v) { this.clientSecret = v; }
        public String getRedirectUri() { return redirectUri; }
        public void setRedirectUri(String v) { this.redirectUri = v; }
    }
}
