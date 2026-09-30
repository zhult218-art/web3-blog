package com.web3.forum.controller;

import com.web3.common.core.ApiResponse;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import com.web3.forum.entity.ChatMessage;
import com.web3.forum.entity.ChatRoomMember;
import com.web3.forum.service.ChatService;
import io.jsonwebtoken.Claims;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 群聊 REST 接口：房间列表、加入申请、审批、消息历史、成员列表
 * 实时消息走 WebSocket（/chat/ws/{roomId}）
 */
@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    public ChatController(ChatService chatService, JwtUtil jwtUtil, JwtProperties jwtProperties) {
        this.chatService = chatService;
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
        } catch (Exception ignored) {}
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

    /** 群成员列表：全部注册用户（固定公开群），带在线标记与头像 */
    @GetMapping("/rooms/{roomId}/members")
    public ApiResponse<List<ChatService.MemberVO>> members(@PathVariable Long roomId) {
        return ApiResponse.ok(chatService.listAllMembersWithStatus(
                roomId, com.web3.forum.websocket.ChatWebSocket.getOnlineUserIds(roomId)));
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
