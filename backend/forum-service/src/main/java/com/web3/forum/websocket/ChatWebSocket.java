package com.web3.forum.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import com.web3.forum.entity.ChatMessage;
import com.web3.forum.entity.ChatRoomMember;
import com.web3.forum.service.ChatService;
import io.jsonwebtoken.Claims;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 群聊 WebSocket 端点
 * 连接地址：ws://host/chat/ws/{roomId}?token=xxx
 * 消息格式（客户端发送）：{ "type":"text", "content":"hello" }
 * 消息格式（服务端广播）：{ "id":1, "roomId":1, "userId":2, "username":"xx",
 *   "nickname":"xx", "msgType":"text", "content":"hello", "createdAt":"..." }
 */
@Component
@ServerEndpoint("/chat/ws/{roomId}")
public class ChatWebSocket {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocket.class);

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    /** roomId -> { sessionId -> Session } */
    private static final Map<Long, Map<String, Session>> ROOMS = new ConcurrentHashMap<>();

    private static ApplicationContext applicationContext;

    @Autowired
    public void setApplicationContext(ApplicationContext ctx) {
        applicationContext = ctx;
    }

    private Long roomId;
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    /** 令牌中的权限串（逗号分隔，"-chat" 表示被管理员收回聊天权限）；null = 旧令牌/管理员 */
    private String perms;
    private boolean admin;
    /** 实名认证状态（JWT claims 中 verified=2 表示已认证，可发消息） */
    private Integer verified;

    @OnOpen
    public void onOpen(Session session, @PathParam("roomId") Long roomId) {
        this.roomId = roomId;
        // 管理员关闭群聊时拒绝一切新连接
        com.web3.forum.service.ChatSwitchService chatSwitch = applicationContext.getBean(com.web3.forum.service.ChatSwitchService.class);
        if (!chatSwitch.isEnabled()) {
            log.info("[chat-ws] close: chat disabled by admin");
            try { session.close(); } catch (IOException ignored) {}
            return;
        }
        // 从 query 参数解析 token
        String token = null;
        String query = session.getQueryString();
        if (query != null) {
            for (String p : query.split("&")) {
                String[] kv = p.split("=", 2);
                if ("token".equals(kv[0]) && kv.length > 1) token = kv[1];
            }
        }
        if (token == null || token.isBlank()) {
            log.warn("[chat-ws] close: token missing, query={}", query);
            try { session.close(); } catch (IOException ignored) {}
            return;
        }
        try {
            JwtUtil jwtUtil = applicationContext.getBean(JwtUtil.class);
            Claims claims = jwtUtil.parseClaims(token);
            this.userId = claims.get("userId", Long.class);
            this.username = claims.get("username", String.class);
            this.nickname = claims.get("nickname", String.class);
            this.perms = claims.get("perms", String.class);
            this.admin = "ROLE_ADMIN".equals(claims.get("authorities", String.class));
            Integer v = claims.get("verified", Integer.class);
            this.verified = v != null ? v : 0;
            if (this.nickname == null) this.nickname = this.username;
        } catch (Exception e) {
            log.warn("[chat-ws] close: token parse failed: {}", e.getMessage());
            try { session.close(); } catch (IOException ignored) {}
            return;
        }
        // 固定公开群：所有注册用户自动入群（缺记录则现场创建，昵称/头像同步为最新）
        ChatService chatService = applicationContext.getBean(ChatService.class);
        try {
            Map<String, Object> du = chatService.directoryUser(userId);
            if (du != null) {
                Object freshNick = du.get("nickname");
                if (freshNick != null && !freshNick.toString().isBlank()) this.nickname = freshNick.toString();
                Object av = du.get("avatar");
                if (av != null && !av.toString().isBlank()) this.avatar = av.toString();
            }
        } catch (Exception ignored) {}
        ChatRoomMember member = chatService.ensureMembership(roomId, userId, username, nickname, avatar);
        if (member == null || !"approved".equals(member.getStatus())) {
            log.warn("[chat-ws] close: member not approved, userId={}, member={}", userId,
                    member == null ? "null" : member.getStatus());
            try { session.close(); } catch (IOException ignored) {}
            return;
        }
        if (this.avatar == null) this.avatar = member.getAvatar();
        // 加入房间（记录该用户此前是否已有连接，用于去重 presence 广播）
        Map<String, Session> roomSessions = ROOMS.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>());
        boolean firstSession = roomSessions.values().stream()
                .noneMatch(s -> userId.equals(s.getUserProperties().get("userId")));
        roomSessions.put(session.getId(), session);
        session.getUserProperties().put("roomId", roomId);
        session.getUserProperties().put("userId", userId);
        // 注意：userProperties 是 ConcurrentHashMap，null 值会 NPE
        if (avatar != null) session.getUserProperties().put("avatar", avatar);
        // 首次上线广播 presence（在线状态实时同步到所有端）
        if (firstSession) {
            broadcast(roomId, presenceJson(true));
        }
        log.info("[chat-ws] open ok: room={}, userId={}, username={}", roomId, userId, username);
    }

    /** 生成 presence 事件 JSON：{"type":"presence","online":true,"userId":..,...} */
    private String presenceJson(boolean online) {
        try {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("type", "presence");
            m.put("online", online);
            m.put("userId", userId);
            m.put("username", username);
            m.put("nickname", nickname);
            m.put("avatar", avatar);
            return MAPPER.writeValueAsString(m);
        } catch (Exception e) {
            return "{}";
        }
    }

    /** 某房间当前在线的用户 ID 集合（供成员列表标注在线状态） */
    public static java.util.Set<Long> getOnlineUserIds(Long roomId) {
        java.util.Set<Long> ids = new java.util.HashSet<>();
        Map<String, Session> sessions = ROOMS.get(roomId);
        if (sessions == null) return ids;
        for (Session s : sessions.values()) {
            Object uid = s.getUserProperties().get("userId");
            if (uid instanceof Long && s.isOpen()) ids.add((Long) uid);
        }
        return ids;
    }

    @OnMessage
    public void onMessage(Session session, String message) {
        try {
            // 管理员关闭群聊后，存量连接也不允许再发言
            com.web3.forum.service.ChatSwitchService chatSwitch = applicationContext.getBean(com.web3.forum.service.ChatSwitchService.class);
            if (!chatSwitch.isEnabled()) {
                session.getBasicRemote().sendText(MAPPER.writeValueAsString(
                        Map.of("type", "error", "content", "管理员已关闭群聊")));
                return;
            }
            // 实名认证检查：未认证(verified!=2) 的用户只能只读，不能发消息
            if (!admin && (verified == null || verified != 2)) {
                session.getBasicRemote().sendText(MAPPER.writeValueAsString(
                        Map.of("type", "error", "content", "请先完成实名认证后再发言（个人中心 → 实名认证）")));
                return;
            }
            // 聊天权限：互动类默认拥有，"-chat" 表示被管理员收回（禁言）
            if (!admin && perms != null && java.util.Arrays.stream(perms.split(",")).map(String::trim).anyMatch("-chat"::equals)) {
                session.getBasicRemote().sendText(MAPPER.writeValueAsString(
                        Map.of("type", "error", "content", "你已被管理员限制聊天")));
                return;
            }
            JsonNode node = MAPPER.readTree(message);
            String type = node.has("type") ? node.get("type").asText() : "text";
            String content = node.has("content") ? node.get("content").asText() : "";
            if (content == null || content.isBlank()) return;
            if (content.length() > 2000) content = content.substring(0, 2000);

            ChatService chatService = applicationContext.getBean(ChatService.class);
            ChatMessage msg = chatService.saveMessage(roomId, userId, username, nickname, avatar, type, content);

            // 广播给房间内所有连接
            broadcast(roomId, MAPPER.writeValueAsString(msg));
        } catch (Exception e) {
            log.warn("处理聊天消息失败: {}", e.getMessage(), e);
        }
    }

    @OnClose
    public void onClose(Session session) {
        Map<String, Session> sessions = ROOMS.get(roomId);
        if (sessions != null) {
            sessions.remove(session.getId());
            // 该用户的最后一个连接断开 → 广播下线
            boolean stillOnline = sessions.values().stream()
                    .anyMatch(s -> userId != null && userId.equals(s.getUserProperties().get("userId")));
            if (!stillOnline && userId != null) {
                broadcast(roomId, presenceJson(false));
            }
            if (sessions.isEmpty()) ROOMS.remove(roomId);
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.warn("[chat-ws] error: userId=" + userId, error);
        onClose(session);
    }

    private void broadcast(Long roomId, String json) {
        Map<String, Session> sessions = ROOMS.get(roomId);
        if (sessions == null) return;
        for (Session s : sessions.values()) {
            if (s.isOpen()) {
                try { s.getBasicRemote().sendText(json); } catch (IOException ignored) {}
            }
        }
    }
}
