package com.web3.forum.controller;

import com.web3.common.core.ApiResponse;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import com.web3.forum.entity.ChatMessage;
import com.web3.forum.entity.ChatRoomMember;
import com.web3.forum.service.ChatService;
import com.web3.forum.service.ChatSwitchService;
import io.jsonwebtoken.Claims;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 群聊 REST 接口：房间列表、加入申请、审批、消息历史、成员列表、聊天开关
 * 实时消息走 Socket.IO 网关（node-service/chat-gateway，端口 3010），本服务不再承载任何 WebSocket
 */
@RestController
@RequestMapping("/chat")
@lombok.extern.slf4j.Slf4j
public class ChatController {

    private final ChatService chatService;
    private final ChatSwitchService chatSwitchService;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    /** 在线状态查询（Socket.IO 网关 HTTP 接口），网关不可用时降级为全部离线 */
    private static final java.net.http.HttpClient ONLINE_HTTP = java.net.http.HttpClient.newBuilder()
            // 显式禁用代理：网关在本机回环地址，走系统代理必然失败
            .proxy(java.net.ProxySelector.of(null))
            // 强制 HTTP/1.1：默认会先做 h2c 升级探测，而 Node 的 http server 不支持，
            // 结果是 "HTTP/1.1 header parser received no bytes" 直接超时
            .version(java.net.http.HttpClient.Version.HTTP_1_1)
            .connectTimeout(java.time.Duration.ofMillis(500)).build();
    private static final com.fasterxml.jackson.databind.ObjectMapper ONLINE_MAPPER = new com.fasterxml.jackson.databind.ObjectMapper();

    public ChatController(ChatService chatService, ChatSwitchService chatSwitchService,
                          JwtUtil jwtUtil, JwtProperties jwtProperties) {
        this.chatService = chatService;
        this.chatSwitchService = chatSwitchService;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
    }

    private Long requireUserId(String auth) {
        return AuthUtils.requireUserId(auth, jwtUtil, jwtProperties);
    }

    /** 群列表（登录用户自动入群，myStatus 恒为 approved） */
    @GetMapping("/rooms")
    public ApiResponse<List<ChatService.RoomVO>> listRooms(@RequestHeader(value = "Authorization", required = false) String auth) {
        Long uid = null;
        String username = null, nickname = null;
        try {
            uid = requireUserId(auth);
            String token = AuthUtils.resolveToken(auth, jwtProperties);
            Claims claims = jwtUtil.parseClaims(token);
            username = claims.get("username", String.class);
            nickname = claims.get("nickname", String.class);
            if (nickname == null) nickname = username;
        } catch (Exception e) {
            log.debug("[chat] JWT parse failed for /rooms, treating as anonymous: {}", e.getMessage());
        }
        return ApiResponse.ok(chatService.listRoomsWithStatus(uid, username, nickname));
    }

    /** 加入群（注册用户直接入群，无需审批） */
    @PostMapping("/rooms/{roomId}/join")
    public ApiResponse<ChatRoomMember> joinRoom(@PathVariable Long roomId,
                                                 @RequestHeader("Authorization") String auth) {
        Long uid = requireUserId(auth);
        String token = AuthUtils.resolveToken(auth, jwtProperties);
        Claims claims = jwtUtil.parseClaims(token);
        String username = claims.get("username", String.class);
        String nickname = claims.get("nickname", String.class);
        if (nickname == null) nickname = username;
        return ApiResponse.ok(chatService.ensureMembership(roomId, uid, username, nickname));
    }

    /** 退出群 */
    @PostMapping("/rooms/{roomId}/leave")
    public ApiResponse<Void> leaveRoom(@PathVariable Long roomId,
                                        @RequestHeader("Authorization") String auth) {
        Long uid = requireUserId(auth);
        chatService.leaveRoom(roomId, uid);
        return ApiResponse.ok();
    }

    /** 查询自己在某群的状态 */
    @GetMapping("/rooms/{roomId}/my-status")
    public ApiResponse<ChatRoomMember> myStatus(@PathVariable Long roomId,
                                                 @RequestHeader("Authorization") String auth) {
        Long uid = requireUserId(auth);
        return ApiResponse.ok(chatService.getMemberStatus(roomId, uid));
    }

    /** 群成员列表：全部注册用户（固定公开群），带在线标记与头像
     *  ?refresh=1 时强制刷新用户目录缓存（用户在个人中心改完头像后调用，立即生效） */
    @GetMapping("/rooms/{roomId}/members")
    public ApiResponse<List<ChatService.MemberVO>> members(@PathVariable Long roomId,
                                                            @RequestParam(value = "refresh", required = false) String refresh) {
        boolean force = "1".equals(refresh) || "true".equalsIgnoreCase(refresh);
        return ApiResponse.ok(chatService.listAllMembersWithStatus(roomId, this.onlineUserIds(roomId), force));
    }

    /**
     * 从 Socket.IO 聊天网关拉取房间在线用户 ID（GET /online/{roomId}）。
     * 网关未启动/超时一律返回空集合：成员列表仍可用，只是全部显示离线。
     */
    private java.util.Set<Long> onlineUserIds(Long roomId) {
        String base = System.getenv().getOrDefault("CHAT_GATEWAY_URL", "http://127.0.0.1:3010");
        try {
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(base + "/online/" + roomId))
                    .timeout(java.time.Duration.ofMillis(800))
                    .GET().build();
            java.net.http.HttpResponse<String> res = ONLINE_HTTP.send(req,
                    java.net.http.HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) return java.util.Set.of();
            com.fasterxml.jackson.databind.JsonNode ids = ONLINE_MAPPER.readTree(res.body()).get("userIds");
            java.util.Set<Long> set = new java.util.HashSet<>();
            if (ids != null && ids.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode n : ids) {
                    // 网关以字符串返回雪花 ID；用 asText 转再 parse 才有精度保证
                    String s = n.asText();
                    if (s != null && !s.isEmpty()) set.add(Long.parseLong(s));
                }
            }
            return set;
        } catch (Exception e) {
            log.warn("[chat] 查询网关在线成员失败（房间 {}，网关 {}）：{}", roomId, base, e.toString());
            return java.util.Set.of();
        }
    }

    /** 历史消息 */
    @GetMapping("/rooms/{roomId}/messages")
    public ApiResponse<List<ChatMessage>> messages(@PathVariable Long roomId,
                                                    @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.ok(chatService.listMessages(roomId, limit));
    }

    // ========== 管理员审批 ==========

    /** 待审批申请列表 */
    @GetMapping("/applications")
    public ApiResponse<List<ChatRoomMember>> applications(@RequestHeader("Authorization") String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        return ApiResponse.ok(chatService.listPendingApplications());
    }

    /** 审批通过 */
    @PostMapping("/applications/{id}/approve")
    public ApiResponse<ChatRoomMember> approve(@PathVariable Long id,
                                                @RequestHeader("Authorization") String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        return ApiResponse.ok(chatService.approveApplication(id));
    }

    /** 审批拒绝 */
    @PostMapping("/applications/{id}/reject")
    public ApiResponse<ChatRoomMember> reject(@PathVariable Long id,
                                                @RequestHeader("Authorization") String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        return ApiResponse.ok(chatService.rejectApplication(id));
    }

    // ========== 聊天开关（管理员可一键关闭/恢复群聊） ==========

    /** 查询聊天开关状态（公开：前端据此显隐聊天入口） */
    @GetMapping("/enabled")
    public ApiResponse<Map<String, Object>> chatEnabled() {
        return ApiResponse.ok(Map.of("enabled", chatSwitchService.isEnabled()));
    }

    /** 设置聊天开关（仅管理员）：body { "enabled": true/false } */
    @PutMapping("/enabled")
    public ApiResponse<Map<String, Object>> setChatEnabled(@RequestBody Map<String, Object> body,
                                                           @RequestHeader("Authorization") String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        boolean value = Boolean.TRUE.equals(body.get("enabled"))
                || "true".equals(String.valueOf(body.get("enabled")))
                || "1".equals(String.valueOf(body.get("enabled")));
        chatSwitchService.setEnabled(value);
        return ApiResponse.ok(Map.of("enabled", chatSwitchService.isEnabled()));
    }

    // ========== 私聊 ==========

    /**
     * 获取或创建与指定用户的私聊房间。
     * 返回 { id: roomId, name: 对方昵称, peer: {userId, username, nickname, avatar} }
     */
    @PostMapping("/private/{targetUserId}")
    public ApiResponse<java.util.Map<String, Object>> createPrivateChat(@PathVariable Long targetUserId,
                                                                         @RequestHeader("Authorization") String auth) {
        Long uid = requireUserId(auth);
        if (targetUserId.equals(uid)) return ApiResponse.fail(400, "不能和自己私聊");
        String token = AuthUtils.resolveToken(auth, jwtProperties);
        Claims claims = jwtUtil.parseClaims(token);
        String myName = claims.get("username", String.class);
        String myNick = claims.get("nickname", String.class);
        if (myNick == null) myNick = myName;
        // 拉取对方资料：优先 user-service 目录（INTERNAL_API_KEY 配置时可用），
        // 否则从 chat_room_member 表兜底查（每个曾连过群聊的用户都有冗余记录）
        java.util.Map<String, Object> peer = chatService.directoryUser(targetUserId);
        String peerName = myName, peerNick = myNick, peerAvatar = null;
        if (peer != null) {
            peerName = (String) peer.getOrDefault("username", myName);
            peerNick = (String) peer.getOrDefault("nickname", peerName);
            peerAvatar = (String) peer.get("avatar");
        } else {
            // 兜底：从任意群成员记录里取对方最新冗余资料
            com.web3.forum.entity.ChatRoomMember peerMember = chatService.findAnyMemberByUserId(targetUserId);
            if (peerMember != null) {
                peerName = peerMember.getUsername() != null ? peerMember.getUsername() : peerName;
                peerNick = peerMember.getNickname() != null ? peerMember.getNickname() : peerName;
                peerAvatar = peerMember.getAvatar();
            }
        }
        com.web3.forum.entity.ChatRoom room = chatService.getOrCreatePrivateRoom(
                uid, myName, myNick, null,
                targetUserId, peerName, peerNick, peerAvatar);
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("id", room.getId());
        result.put("name", peerNick != null ? peerNick : peerName);
        java.util.Map<String, Object> peerInfo = new java.util.HashMap<>();
        peerInfo.put("userId", targetUserId);
        peerInfo.put("username", peerName);
        peerInfo.put("nickname", peerNick);
        peerInfo.put("avatar", peerAvatar);
        result.put("peer", peerInfo);
        return ApiResponse.ok(result);
    }

    // ========== 服务间内部接口（注册后自动入群） ==========

    /**
     * 内部接口：用户注册后自动加入所有公开群。
     * 由 user-service 在注册成功后调用，通过 X-Internal-Key 鉴权（环境变量 INTERNAL_API_KEY）。
     */
    @PostMapping("/internal/auto-join")
    public ApiResponse<Void> autoJoin(@RequestBody Map<String, Object> body,
                                      @RequestHeader(value = "X-Internal-Key", required = false) String key) {
        String expected = System.getenv().getOrDefault("INTERNAL_API_KEY", "");
        if (expected.isEmpty() || !expected.equals(key)) {
            return ApiResponse.fail(403, "forbidden");
        }
        try {
            Long userId = Long.valueOf(body.get("userId").toString());
            String username = (String) body.get("username");
            String nickname = (String) body.get("nickname");
            String avatar = (String) body.get("avatar");
            if (nickname == null || nickname.isBlank()) nickname = username;
            for (com.web3.forum.entity.ChatRoom room : chatService.listRooms()) {
                if (room.getRequireApproval() != null && room.getRequireApproval() == 0) {
                    chatService.ensureMembership(room.getId(), userId, username, nickname, avatar);
                }
            }
            return ApiResponse.ok();
        } catch (Exception e) {
            return ApiResponse.fail(500, "auto-join failed: " + e.getMessage());
        }
    }
}
