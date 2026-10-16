package com.web3.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * IP 黑名单过滤器（GlobalFilter，介于安全头与限流之间执行）
 * <p>
 * 命中黑名单的 IP 立即返回 403，不进入限流/认证/路由链路，节省资源。
 * 黑名单存储在 Redis Set `ip:blocklist`，由 admin-service 管理 API 维护。
 * <p>Redis 故障 fail-open：避免黑名单自身成为单点故障导致全站不可用。
 */
@Component
public class IpBlocklistFilter implements GlobalFilter, Ordered {

  private static final Logger log = LoggerFactory.getLogger(IpBlocklistFilter.class);
  private static final String BLOCKLIST_KEY = "ip:blocklist";

  private final ReactiveStringRedisTemplate redis;

  /** 可信代理 IP 集合：仅这些来源转发的 XFF 才被解析，杜绝任意客户端伪造 XFF 绕过黑名单 */
  @Value("${gateway.trusted-proxies:127.0.0.1,0:0:0:0:0:0:0:1}")
  private Set<String> trustedProxies;

  public IpBlocklistFilter(ReactiveStringRedisTemplate redis) {
    this.redis = redis;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String ip = resolveClientIp(exchange.getRequest());
    if (ip == null || ip.equals("unknown")) {
      return chain.filter(exchange);
    }
    // 查询 Redis Set 是否包含该 IP
    return redis.opsForSet().isMember(BLOCKLIST_KEY, ip)
        .onErrorResume(e -> {
          // Redis 故障 fail-open：避免黑名单故障导致全站不可用
          log.warn("[ip-blocklist] redis error, fail-open for {}", ip, e);
          return Mono.just(Boolean.FALSE);
        })
        .flatMap(blocked -> {
          if (Boolean.TRUE.equals(blocked)) {
            log.warn("[ip-blocklist] blocked ip={}", ip);
            return reject(exchange, ip);
          }
          return chain.filter(exchange);
        });
  }

  /** 拒绝访问：返回 403 + JSON 提示 */
  private Mono<Void> reject(ServerWebExchange exchange, String ip) {
    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    String body = "{\"code\":403,\"message\":\"IP 已被加入黑名单，如有疑问请联系管理员\"}";
    DataBuffer buf = exchange.getResponse().bufferFactory()
        .wrap(body.getBytes(StandardCharsets.UTF_8));
    return exchange.getResponse().writeWith(Mono.just(buf));
  }

  /**
   * 提取客户端真实 IP（与 RateLimitFilter 相同的可信代理解析逻辑）。
   * <p>仅当直连来源是可信代理时才解析 XFF，从右向左取第一个非可信 IP；
   * 否则直接使用 TCP 对端地址，杜绝攻击者伪造 XFF 绕过黑名单。
   */
  private String resolveClientIp(ServerHttpRequest req) {
    InetSocketAddress remote = req.getRemoteAddress();
    String remoteIp = remote != null ? remote.getAddress().getHostAddress() : "unknown";
    // 直连来源不是可信代理 → 无条件信任 TCP 对端地址，忽略所有 XFF 头
    if (!isTrustedProxy(remoteIp)) {
      return remoteIp;
    }
    // 直连来源是可信代理 → 解析 XFF，从右向左取第一个非可信代理 IP
    String xff = req.getHeaders().getFirst("X-Forwarded-For");
    if (xff != null && !xff.isEmpty()) {
      String[] parts = xff.split(",");
      for (int i = parts.length - 1; i >= 0; i--) {
        String ip = parts[i].trim();
        if (!ip.isEmpty() && !isTrustedProxy(ip)) {
          return ip;
        }
      }
      String tail = parts[parts.length - 1].trim();
      if (!tail.isEmpty()) return tail;
    }
    String xri = req.getHeaders().getFirst("X-Real-IP");
    if (xri != null && !xri.isEmpty()) return xri.trim();
    return remoteIp;
  }

  /** 判断 IP 是否为可信代理（与 RateLimitFilter 一致） */
  private boolean isTrustedProxy(String ip) {
    if (ip == null) return false;
    if (trustedProxies.contains(ip)) return true;
    return ip.startsWith("127.") || ip.startsWith("10.")
        || ip.startsWith("192.168.") || ip.equals("0:0:0:0:0:0:0:1");
  }

  /** 介于安全头（-200）与限流（-100）之间 */
  @Override
  public int getOrder() {
    return -150;
  }
}
