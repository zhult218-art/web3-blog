package com.web3.user.controller;

import com.web3.common.core.ApiResponse;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import com.web3.user.service.GuildService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/guild")
public class GuildController {
    private final GuildService guildService;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    public GuildController(GuildService guildService, JwtUtil jwtUtil, JwtProperties jwtProperties) {
        this.guildService = guildService;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
    }

    /** GET /guild/list — 公会列表 */
    @GetMapping("/list")
    public ApiResponse<?> listGuilds() {
        return guildService.listGuilds();
    }

    /** GET /guild/{id} — 公会详情 */
    @GetMapping("/{id}")
    public ApiResponse<?> getGuild(@PathVariable Long id) {
        return guildService.getGuild(id);
    }

    /** GET /guild/my — 当前用户的公会状态 */
    @GetMapping("/my")
    public ApiResponse<?> myGuildStatus(@RequestHeader("Authorization") String auth) {
        Long userId = AuthUtils.requireUserId(auth, jwtUtil, jwtProperties);
        return guildService.getMyGuildStatus(userId);
    }

    /** POST /guild/apply — 申请加入公会 */
    @PostMapping("/apply")
    public ApiResponse<?> applyGuild(@RequestHeader("Authorization") String auth,
                                     @RequestBody Map<String, Object> body) {
        Long userId = AuthUtils.requireUserId(auth, jwtUtil, jwtProperties);
        Long guildId = Long.valueOf(body.get("guildId").toString());
        String message = body.getOrDefault("message", "").toString();
        return guildService.applyGuild(userId, guildId, message);
    }

    /** GET /guild/{guildId}/pending — 待审核申请（仅会长） */
    @GetMapping("/{guildId}/pending")
    public ApiResponse<?> getPending(@RequestHeader("Authorization") String auth,
                                     @PathVariable Long guildId) {
        Long userId = AuthUtils.requireUserId(auth, jwtUtil, jwtProperties);
        return guildService.getPendingMembers(userId, guildId);
    }

    /** GET /guild/{guildId}/members — 已通过成员列表 */
    @GetMapping("/{guildId}/members")
    public ApiResponse<?> getMembers(@PathVariable Long guildId) {
        return guildService.getApprovedMembers(guildId);
    }

    /** POST /guild/approve — 审批申请 */
    @PostMapping("/approve")
    public ApiResponse<?> approve(@RequestHeader("Authorization") String auth,
                                  @RequestBody Map<String, Object> body) {
        Long userId = AuthUtils.requireUserId(auth, jwtUtil, jwtProperties);
        Long memberId = Long.valueOf(body.get("memberId").toString());
        boolean approve = Boolean.TRUE.equals(body.get("approve"));
        return guildService.approveMember(userId, memberId, approve);
    }

    /** POST /guild/create — 创建公会 */
    @PostMapping("/create")
    public ApiResponse<?> createGuild(@RequestHeader("Authorization") String auth,
                                      @RequestBody Map<String, String> body) {
        Long userId = AuthUtils.requireUserId(auth, jwtUtil, jwtProperties);
        String name = body.getOrDefault("name", "").toString().trim();
        String description = body.getOrDefault("description", "");
        if (name.isEmpty()) return ApiResponse.fail(400, "公会名称不能为空");
        return guildService.createGuild(userId, name, description);
    }

    /** POST /guild/leave — 退出公会 */
    @PostMapping("/leave")
    public ApiResponse<?> leaveGuild(@RequestHeader("Authorization") String auth) {
        Long userId = AuthUtils.requireUserId(auth, jwtUtil, jwtProperties);
        return guildService.leaveGuild(userId);
    }
}
