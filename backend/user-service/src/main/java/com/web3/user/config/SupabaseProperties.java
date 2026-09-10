package com.web3.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SupabaseProperties
 * (zh) Supabase 免密登录（Magic Link）相关配置项（application.yml 中 supabase 前缀）：
 *      url             Supabase 项目地址
 *      publishable-key 前端公钥（用于调用 GET /auth/v1/user 校验用户令牌）
 *      timeout-ms      HTTP 调用超时（毫秒）
 */
@Component
@ConfigurationProperties(prefix = "supabase")
public class SupabaseProperties {

    private String url = "";
    private String publishableKey = "";
    private int timeoutMs = 5000;

    public String getUrl() { return url; }
    public void setUrl(String v) { this.url = v; }

    public String getPublishableKey() { return publishableKey; }
    public void setPublishableKey(String v) { this.publishableKey = v; }

    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int v) { this.timeoutMs = v; }
}
