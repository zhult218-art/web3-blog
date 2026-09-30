package com.web3.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 网关全局限流过滤器（DDoS 防护核心）
 * <p>
 * 基于 Redis 令牌桶算法，对每个客户端 IP 做双级限流：
 * 1) 全局维度：每 IP 每分钟 N 请求（防单 IP 洪泛）
 * 2) 路径维度：每 IP 对同一路径前缀每分钟 M 请求（防针对性刷接口）
 * <p>
 * 令牌桶通过 Lua 脚本原子操作（取令牌+填充+过期），避免并发竞态。
 * 白名单：本机回环（127.0.0.0/8）+ 可配置内网段，保证服务间调用不受限。
 * 超限返回 429 Too Many Requests + Retry-After 头。
 */
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

  private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

  /** 令牌桶 Lua 脚本：原子取令牌 + 按时间填充 + 设过期
   * KEYS[1]=桶 key, ARGV[1]=容量, ARGV[2]=填充速率(令牌/秒), ARGV[3]=当前时间戳(ms), ARGV[4]=请求令牌数(=1)
   * 返回 {allowed(0/1), remaining} */
  private static final String TOKEN_BUCKET_LUA =
      "local key = KEYS[1] " +
      "local capacity = tonumber(ARGV[1]) " +
      "local refill_rate = tonumber(ARGV[2]) " +
      "local now = tonumber(ARGV[3]) " +
      "local requested = tonumber(ARGV[4]) " +
      "local bucket = redis.call('HMGET', key, 'tokens', 'last') " +
      "local tokens = tonumber(bucket[1]) or capacity " +
      "local last = tonumber(bucket[2]) or now " +
      "local delta = math.max(0, now - last) " +
      "local refilled = tokens + (delta / 1000.0) * refill_rate " +
      "tokens = math.min(capacity, refilled) " +
      "local allowed = 0 " +
      "if tokens >= requested then tokens = tokens - requested; allowed = 1 end " +
      "redis.call('HMSET', key, 'tokens', tokens, 'last', now) " +
      "redis.call('EXPIRE', key, math.ceil(capacity / refill_rate) + 10) " +
      "return {allowed, tokens}";

  private final DefaultRedisScript<List> script;
  private final ReactiveStringRedisTemplate redis;

  @Value("${gateway.ratelimit.global-capacity:120}")
  private int globalCapacity;
  @Value("${gateway.ratelimit.global-refill:2}")
  private double globalRefill;
  @Value("${gateway.ratelimit.path-capacity:60}")
  private int pathCapacity;
  @Value("${gateway.ratelimit.path-refill:1}")
  private double pathRefill;
  @Value("${gateway.ratelimit.whitelist-ips:127.0.0.1,0:0:0:0:0:0:0:1}")
  private Set<String> whitelistIps;

  /** 可信代理 IP 集合：仅这些来源转发的 XFF 才被解析，杜绝任意客户端伪造 XFF 绕过限流 */
  @Value("${gateway.trusted-proxies:127.0.0.1,0:0:0:0:0:0:0:1}")
  private Set<String> trustedProxies;

  public RateLimitFilter(ReactiveStringRedisTemplate redis) {
    this.redis = redis;
    this.script = new DefaultRedisScript<>();
    this.script.setScriptText(TOKEN_BUCKET_LUA);
    this.script.setResultType(List.class);
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    ServerHttpRequest req = exchange.getRequest();
    String ip = resolveClientIp(req);
    // 白名单：本机/内网放行，避免服务间互调和健康探测被误杀
    if (isWhitelisted(ip)) {
      return chain.filter(exchange);
    }
    String path = normalizePath(req.getPath().value());
    long now = System.currentTimeMillis();
    String globalKey = "rl:" + ip + ":global";
    String pathKey = "rl:" + ip + ":" + path;

    // 双级限流：全局桶 + 路径桶，任一超限即拒绝
    return checkBucket(globalKey, globalCapacity, globalRefill, now)
        .flatMap(globalResult -> {
          if (!allowed(globalResult)) {
            return reject(exchange, globalKey);
          }
          return checkBucket(pathKey, pathCapacity, pathRefill, now)
              .flatMap(pathResult -> {
                if (!allowed(pathResult)) {
                  return reject(exchange, pathKey);
                }
                return chain.filter(exchange);
              });
        });
  }

  /** 调用 Lua 脚本检查令牌桶 */
  private Mono<List> checkBucket(String key, int capacity, double refill, long now) {
    return redis.execute(script, Collections.singletonList(key),
        Arrays.asList(String.valueOf(capacity), String.valueOf(refill),
            String.valueOf(now), "1"))
        .next()
        .map(obj -> (List) obj)
        .onErrorResume(e -> {
          // Redis 故障时降级放行，避免限流器自身成为单点故障
          log.warn("[rate-limit] redis error, fail-open for {}", key, e);
          return Mono.just(Arrays.asList(1L, (long) capacity));
        });
  }

  private boolean allowed(List result) {
    if (result == null || result.isEmpty()) return true;
    Object o = result.get(0);
    if (o instanceof Number) return ((Number) o).intValue() == 1;
    return !"0".equals(String.valueOf(o));
  }

  /** 限流触发：返回 429 + Retry-After 头 */
  private Mono<Void> reject(ServerWebExchange exchange, String bucketKey) {
    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
    exchange.getResponse().getHeaders().add("Retry-After", "10");
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    String body = "{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}";
    DataBuffer buf = exchange.getResponse().bufferFactory()
        .wrap(body.getBytes(StandardCharsets.UTF_8));
    log.warn("[rate-limit] rejected ip={} bucket={}", resolveClientIp(exchange.getRequest()), bucketKey);
    return exchange.getResponse().writeWith(Mono.just(buf));
  }

  /** 提取客户端真实 IP：仅当直连来源为可信代理时才解析 XFF，从右向左取第一个非可信 IP；
   * 否则直接用 remoteAddress。杜绝攻击者伪造 XFF 绕过 IP 限流。
   * <p>XFF 链格式：client, proxy1, proxy2...，最右侧是最近一跳代理，从右向左逆推 */
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
      // 整条 XFF 链都是可信代理（罕见，说明链路都是内部代理），返回链尾
      String tail = parts[parts.length - 1].trim();
      if (!tail.isEmpty()) return tail;
    }
    // 可信代理转发但无 XFF，退回 X-Real-IP，再兜底 TCP 对端
    String xri = req.getHeaders().getFirst("X-Real-IP");
    if (xri != null && !xri.isEmpty()) return xri.trim();
    return remoteIp;
  }

  /** 判断 IP 是否为可信代理（仅这些来源转发的 XFF 才被信任） */
  private boolean isTrustedProxy(String ip) {
    if (ip == null) return false;
    if (trustedProxies.contains(ip)) return true;
    return ip.startsWith("127.") || ip.startsWith("10.")
        || ip.startsWith("192.168.") || ip.equals("0:0:0:0:0:0:0:1");
  }

  private boolean isWhitelisted(String ip) {
    if (ip == null) return false;
    return whitelistIps.contains(ip) || ip.startsWith("127.") || ip.startsWith("192.168.")
        || ip.startsWith("10.") || ip.equals("0:0:0:0:0:0:0:1");
  }

  /** 路径归一化：取前两级路径段做限流维度，避免每个具体 ID 单独建桶 */
  private String normalizePath(String fullPath) {
    if (fullPath == null || fullPath.isEmpty()) return "root";
    String[] parts = fullPath.split("/");
    if (parts.length <= 2) return "root";
    if (parts.length >= 3) return parts[1] + "/" + parts[2];
    return parts[1];
  }

  /** 在认证过滤器之前执行：未认证流量先被限流挡掉，节省 JWT 解析开销 */
  @Override
  public int getOrder() {
    // 紧邻 AUTHENTICATION 之前（-100），早于业务但晚于极高优先级过滤器
    return -100;
  }
}
