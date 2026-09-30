package com.web3.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * 类名：CorsConfig
 * 所属模块：gateway（API 网关服务）
 * 职责：显式收紧 CORS 策略，仅放行白名单来源（生产域名 + 本地开发域名），
 *       杜绝 allowedOrigin=* 通配导致的凭据泄露风险。
 * <p>
 * 架构说明：web3-blog 前端通过 Vite 代理同源访问 API，正常链路不触发 CORS；
 * 本配置作为纵深防御，覆盖直连网关的跨域请求（如外部集成、直接 API 调用）。
 * <p>
 * 关键配置：
 * <ul>
 *   <li>allowedOrigins：生产域名（SakuraFrp 隧道）+ 本地 Vite 开发服务器</li>
 *   <li>allowedMethods：GET/POST/PUT/DELETE/OPTIONS</li>
 *   <li>allowCredentials=true：允许携带 Cookie/JWT</li>
 *   <li>maxAge=3600：预检结果缓存 1 小时，减少 OPTIONS 请求</li>
 * </ul>
 */
@Configuration
public class CorsConfig {

    @Value("${gateway.cors.allowed-origins:https://frp-six.com:44598,http://localhost:5173,http://127.0.0.1:5173}")
    private List<String> allowedOrigins;

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 显式白名单，杜绝通配 *
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // 允许的标准请求头
        config.setAllowedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "X-Requested-With",
                "X-Blog-Uid", "X-Internal-Key", "Accept", "Origin"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        // 暴露给前端 JS 可读的响应头
        config.setExposedHeaders(Arrays.asList("Retry-After", "Content-Range", "Accept-Ranges"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
