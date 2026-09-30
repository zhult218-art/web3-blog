package com.web3.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 安全响应头过滤器（GlobalFilter，最早执行）
 * <p>
 * 在限流/认证/路由之前为所有响应注入一组安全响应头，
 * 提升浏览器侧安全基线，缓解 XSS / 点击劫持 / MIME 嗅探 / 降级攻击 / 隐私泄露。
 * <p>
 * 与 marketingdashboard 同源 iframe 兼容：X-Frame-Options=SAMEORIGIN 不阻断同源嵌入。
 */
@Component
public class SecurityHeadersFilter implements GlobalFilter, Ordered {

  // Content-Security-Policy：限制资源加载来源
  // - default-src 'self'：默认只加载同源资源
  // - script/style 'unsafe-inline'：Vite 构建 + Vue scoped CSS 需要
  // - img/media blob:：封面/音频流式播放需要
  // - connect-src wss:：聊天 WebSocket
  // - frame-src 'self'：同源 iframe 嵌入（市场驾驶舱）
  private static final String CSP =
      "default-src 'self'; " +
      "script-src 'self' 'unsafe-inline'; " +
      "style-src 'self' 'unsafe-inline'; " +
      "img-src 'self' data: https: blob:; " +
      "media-src 'self' https: blob:; " +
      "font-src 'self' data:; " +
      "connect-src 'self' https: wss:; " +
      "frame-src 'self'; " +
      "object-src 'none'; " +
      "base-uri 'self'; " +
      "form-action 'self'";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    ServerHttpResponse resp = exchange.getResponse();
    resp.getHeaders().add("Content-Security-Policy", CSP);
    // 同源 iframe 仍可加载（marketingdashboard 同源嵌入），跨站被拦
    resp.getHeaders().add("X-Frame-Options", "SAMEORIGIN");
    // 禁止 MIME 嗅探：浏览器严格按声明的 Content-Type 处理响应
    resp.getHeaders().add("X-Content-Type-Options", "nosniff");
    // HTTPS 场景强制 HSTS（仅对 https 请求生效，http 不下发）
    if ("https".equalsIgnoreCase(exchange.getRequest().getURI().getScheme())) {
      resp.getHeaders().add("Strict-Transport-Security",
          "max-age=31536000; includeSubDomains");
    }
    // 仅向同源/目标源泄露 Referer，避免跨站完整 URL 泄露
    resp.getHeaders().add("Referrer-Policy", "strict-origin-when-cross-origin");
    // 禁用不需要的浏览器能力，最小化权限暴露面
    resp.getHeaders().add("Permissions-Policy",
        "geolocation=(), microphone=(self), camera=(), payment=(), usb=()");
    return chain.filter(exchange);
  }

  /** 最早执行：早于限流（-100），确保所有响应（含 429 拒绝）都带安全头 */
  @Override
  public int getOrder() {
    return -200;
  }
}
