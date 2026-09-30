package com.web3.admin.controller;

import com.web3.common.core.ApiResponse;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 类名：IpBlocklistController
 * 所属模块：admin-service（管理后台服务）
 * 职责：维护网关 IP 黑名单（Redis Set `ip:blocklist`），管理员可增删查封禁 IP。
 * 关键注解：{@code @RestController}；{@code @RequestMapping("/admin/ip-blocklist")}。
 * 权限：所有接口均需管理员令牌，{@link AuthUtils#requireAdmin} 校验。
 */
@RestController
@RequestMapping("/admin/ip-blocklist")
public class IpBlocklistController {

  private static final String BLOCKLIST_KEY = "ip:blocklist";

  @Autowired
  private StringRedisTemplate redis;

  @Autowired
  private JwtUtil jwtUtil;

  @Autowired
  private JwtProperties jwtProperties;

  /**
   * 前端调用路由：POST /admin/ip-blocklist/{ip}（需管理员权限）。
   * 职责：将 IP 加入黑名单，命中即 403。
   *
   * @param ip  要封禁的 IP 地址
   * @param auth Authorization 请求头（Bearer 令牌），非管理员返回 403
   * @return code=200 时 data 为当前黑名单大小
   */
  @PostMapping("/{ip}")
  public ApiResponse<Long> block(@PathVariable String ip,
                                  @RequestHeader(value = "Authorization", required = false) String auth) {
    AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
    Long size = redis.opsForSet().add(BLOCKLIST_KEY, ip);
    return ApiResponse.ok(size == null ? 0L : size);
  }

  /**
   * 前端调用路由：DELETE /admin/ip-blocklist/{ip}（需管理员权限）。
   * 职责：将 IP 从黑名单移除。
   *
   * @param ip  要解封的 IP 地址
   * @param auth Authorization 请求头（Bearer 令牌），非管理员返回 403
   * @return code=200 时 data 为当前黑名单大小
   */
  @DeleteMapping("/{ip}")
  public ApiResponse<Long> unblock(@PathVariable String ip,
                                   @RequestHeader(value = "Authorization", required = false) String auth) {
    AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
    Long size = redis.opsForSet().remove(BLOCKLIST_KEY, ip);
    return ApiResponse.ok(size == null ? 0L : size);
  }

  /**
   * 前端调用路由：GET /admin/ip-blocklist（需管理员权限）。
   * 职责：列出当前所有封禁 IP。
   *
   * @param auth Authorization 请求头（Bearer 令牌），非管理员返回 403
   * @return code=200 时 data 为 IP 字符串列表
   */
  @GetMapping
  public ApiResponse<List<String>> list(@RequestHeader(value = "Authorization", required = false) String auth) {
    AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
    Set<String> members = redis.opsForSet().members(BLOCKLIST_KEY);
    if (members == null) {
      return ApiResponse.ok(Collections.emptyList());
    }
    return ApiResponse.ok(new ArrayList<>(members));
  }
}
