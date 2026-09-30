package com.web3.forum.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.web3.forum.entity.ChatMessage;
import com.web3.forum.entity.ChatRoom;
import com.web3.forum.entity.ChatRoomMember;
import com.web3.forum.mapper.ChatMessageMapper;
import com.web3.forum.mapper.ChatRoomMapper;
import com.web3.forum.mapper.ChatRoomMemberMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import org.springframework.stereotype.Service;

/**
 * 群聊业务服务：房间管理、成员申请/审批、消息存取
 */
@Service
public class ChatService {

    private final ChatRoomMapper roomMapper;
    private final ChatRoomMemberMapper memberMapper;
    private final ChatMessageMapper messageMapper;

    public ChatService(ChatRoomMapper roomMapper, ChatRoomMemberMapper memberMapper,
                       ChatMessageMapper messageMapper) {
        this.roomMapper = roomMapper;
        this.memberMapper = memberMapper;
        this.messageMapper = messageMapper;
    }

    // ========== 房间 ==========

    /** 列出所有群 */
    public List<ChatRoom> listRooms() {
        return roomMapper.selectList(new LambdaQueryWrapper<ChatRoom>().orderByDesc(ChatRoom::getCreatedAt));
    }

    /** 获取群详情 */
    public ChatRoom getRoom(Long roomId) {
        return roomMapper.selectById(roomId);
    }

    /** 创建默认群（首次启动时若不存在则创建） */
    public ChatRoom ensureDefaultRoom() {
        List<ChatRoom> rooms = roomMapper.selectList(null);
        if (!rooms.isEmpty()) return rooms.get(0);
        ChatRoom room = new ChatRoom();
        room.setName("极光广场");
        room.setDescription("Aurora 极光数字空间 · 全站冒险者的公共客厅");
        room.setOwnerId(1L);
        room.setRequireApproval(0);   // 固定公开群：注册用户自动加入，无需审批
        room.setCreatedAt(LocalDateTime.now());
        room.setUpdatedAt(LocalDateTime.now());
        roomMapper.insert(room);
        return room;
    }

    /**
     * 确保用户是群成员：注册用户访问群聊时自动加入（approved）。
     * 已存在记录（含 pending/rejected）一律直接校正为 approved。
     */
    public ChatRoomMember ensureMembership(Long roomId, Long userId, String username, String nickname) {
        return ensureMembership(roomId, userId, username, nickname, null);
    }

    /** 同上，附加头像冗余（资料变更时同步刷新成员记录） */
    public ChatRoomMember ensureMembership(Long roomId, Long userId, String username, String nickname, String avatar) {
        ChatRoomMember m = memberMapper.selectOne(new LambdaQueryWrapper<ChatRoomMember>()
                .eq(ChatRoomMember::getRoomId, roomId)
                .eq(ChatRoomMember::getUserId, userId));
        if (m != null) {
            boolean dirty = false;
            if (!"approved".equals(m.getStatus())) { m.setStatus("approved"); dirty = true; }
            if (username != null && !username.equals(m.getUsername())) { m.setUsername(username); dirty = true; }
            if (nickname != null && !nickname.equals(m.getNickname())) { m.setNickname(nickname); dirty = true; }
            if (avatar != null && !avatar.isBlank() && !avatar.equals(m.getAvatar())) { m.setAvatar(avatar); dirty = true; }
            if (dirty) {
                m.setUpdatedAt(LocalDateTime.now());
                memberMapper.updateById(m);
            }
            return m;
        }
        m = new ChatRoomMember();
        m.setRoomId(roomId);
        m.setUserId(userId);
        m.setUsername(username);
        m.setNickname(nickname);
        m.setAvatar(avatar);
        m.setRole("member");
        m.setStatus("approved");
        m.setCreatedAt(LocalDateTime.now());
        m.setUpdatedAt(LocalDateTime.now());
        memberMapper.insert(m);
        return m;
    }

    // ========== 成员 / 申请 ==========

    /** 用户加入群：若群需审批则状态=pending，否则直接 approved */
    public ChatRoomMember joinRoom(Long roomId, Long userId, String username, String nickname) {
        ChatRoom room = roomMapper.selectById(roomId);
        if (room == null) throw new RuntimeException("群不存在");
        // 检查是否已有记录
        ChatRoomMember exist = memberMapper.selectOne(new LambdaQueryWrapper<ChatRoomMember>()
                .eq(ChatRoomMember::getRoomId, roomId)
                .eq(ChatRoomMember::getUserId, userId));
        if (exist != null) {
            if ("approved".equals(exist.getStatus())) return exist;
            if ("pending".equals(exist.getStatus())) return exist;
            // rejected 后可重新申请
            exist.setStatus("pending");
            exist.setUpdatedAt(LocalDateTime.now());
            memberMapper.updateById(exist);
            return exist;
        }
        ChatRoomMember m = new ChatRoomMember();
        m.setRoomId(roomId);
        m.setUserId(userId);
        m.setUsername(username);
        m.setNickname(nickname);
        m.setRole("member");
        m.setStatus(room.getRequireApproval() == 1 ? "pending" : "approved");
        m.setCreatedAt(LocalDateTime.now());
        m.setUpdatedAt(LocalDateTime.now());
        memberMapper.insert(m);
        return m;
    }

    /** 查询用户在某群的成员状态 */
    public ChatRoomMember getMemberStatus(Long roomId, Long userId) {
        return memberMapper.selectOne(new LambdaQueryWrapper<ChatRoomMember>()
                .eq(ChatRoomMember::getRoomId, roomId)
                .eq(ChatRoomMember::getUserId, userId));
    }

    /** 列出某群已批准成员 */
    public List<ChatRoomMember> listApprovedMembers(Long roomId) {
        return memberMapper.selectList(new LambdaQueryWrapper<ChatRoomMember>()
                .eq(ChatRoomMember::getRoomId, roomId)
                .eq(ChatRoomMember::getStatus, "approved")
                .orderByAsc(ChatRoomMember::getCreatedAt));
    }

    /** 列出待审批申请（管理员用） */
    public List<ChatRoomMember> listPendingApplications() {
        return memberMapper.selectList(new LambdaQueryWrapper<ChatRoomMember>()
                .eq(ChatRoomMember::getStatus, "pending")
                .orderByDesc(ChatRoomMember::getCreatedAt));
    }

    /** 审批通过 */
    public ChatRoomMember approveApplication(Long id) {
        ChatRoomMember m = memberMapper.selectById(id);
        if (m == null) throw new RuntimeException("申请不存在");
        m.setStatus("approved");
        m.setUpdatedAt(LocalDateTime.now());
        memberMapper.updateById(m);
        return m;
    }

    /** 审批拒绝 */
    public ChatRoomMember rejectApplication(Long id) {
        ChatRoomMember m = memberMapper.selectById(id);
        if (m == null) throw new RuntimeException("申请不存在");
        m.setStatus("rejected");
        m.setUpdatedAt(LocalDateTime.now());
        memberMapper.updateById(m);
        return m;
    }

    /** 退出群 */
    public void leaveRoom(Long roomId, Long userId) {
        ChatRoomMember m = getMemberStatus(roomId, userId);
        if (m != null) memberMapper.deleteById(m.getId());
    }

    // ========== 消息 ==========

    /** 保存消息并返回 */
    public ChatMessage saveMessage(Long roomId, Long userId, String username, String nickname,
                                   String msgType, String content) {
        return saveMessage(roomId, userId, username, nickname, null, msgType, content);
    }

    /** 保存消息并返回（带发送者头像冗余） */
    public ChatMessage saveMessage(Long roomId, Long userId, String username, String nickname, String avatar,
                                   String msgType, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setRoomId(roomId);
        msg.setUserId(userId);
        msg.setUsername(username);
        msg.setNickname(nickname);
        msg.setAvatar(avatar);
        msg.setMsgType(msgType);
        msg.setContent(content);
        msg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(msg);
        return msg;
    }

    /** 获取历史消息（最近 limit 条，按时间正序返回） */
    public List<ChatMessage> listMessages(Long roomId, int limit) {
        List<ChatMessage> list = messageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getRoomId, roomId)
                .orderByDesc(ChatMessage::getId)
                .last("LIMIT " + Math.min(limit, 200)));
        List<ChatMessage> reversed = new ArrayList<>(list);
        reversed.sort(Comparator.comparing(ChatMessage::getId));
        return reversed;
    }

    // ========== 全量用户成员视图（含在线状态） ==========

    /** 用户目录缓存：userId -> {id,username,nickname,avatar}（60s 过期，避免每次请求都打 user-service） */
    private volatile Map<Long, Map<String, Object>> userDirectoryCache = null;
    private volatile long userDirectoryCacheAt = 0L;
    private static final long USER_DIRECTORY_TTL_MS = 60_000L;

    /** 从 user-service 拉取全量用户目录（内部接口，X-Internal-Key 鉴权） */
    @SuppressWarnings("unchecked")
    public Map<Long, Map<String, Object>> userDirectory() {
        long now = System.currentTimeMillis();
        if (userDirectoryCache != null && now - userDirectoryCacheAt < USER_DIRECTORY_TTL_MS) {
            return userDirectoryCache;
        }
        Map<Long, Map<String, Object>> dir = new java.util.HashMap<>();
        try {
            String base = System.getenv().getOrDefault("USER_SERVICE_URL", "http://127.0.0.1:8081");
            String key = System.getenv().getOrDefault("INTERNAL_API_KEY", "");
            if (!key.isBlank()) {
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.set("X-Internal-Key", key);
                org.springframework.http.ResponseEntity<Map> resp = new org.springframework.web.client.RestTemplate()
                        .exchange(base + "/user/internal/all-users",
                                org.springframework.http.HttpMethod.GET,
                                new org.springframework.http.HttpEntity<>(headers), Map.class);
                Map<String, Object> body = resp.getBody();
                if (body != null && body.get("data") instanceof List<?> list) {
                    for (Object o : list) {
                        if (o instanceof Map) {
                            Map<String, Object> u = (Map<String, Object>) o;
                            Object id = u.get("id");
                            if (id != null) dir.put(Long.valueOf(id.toString()), u);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // user-service 不可用时退回空目录，仅显示已入群成员
        }
        if (!dir.isEmpty()) {
            userDirectoryCache = dir;
            userDirectoryCacheAt = now;
        }
        return dir;
    }

    /** 目录中查单个用户（WebSocket 握手时取头像/最新昵称） */
    public Map<String, Object> directoryUser(Long userId) {
        return userDirectory().get(userId);
    }

    /** 成员视图：所有注册用户 + 群内角色/状态 + 在线标记 */
    @Data
    public static class MemberVO {
        private Long userId;
        private String username;
        private String nickname;
        private String avatar;
        private String role;     // member / admin / owner
        private String status;   // approved（固定群全员 approved）
        private Boolean online;  // 当前是否有 WebSocket 连接
        private LocalDateTime joinedAt;
    }

    /**
     * 群成员列表 = 全部注册用户（固定公开群模式）。
     * 对尚未落库的用户懒创建成员记录；头像/昵称以用户表最新值为准回写冗余。
     */
    public List<MemberVO> listAllMembersWithStatus(Long roomId, java.util.Set<Long> onlineIds) {
        Map<Long, ChatRoomMember> members = new java.util.HashMap<>();
        for (ChatRoomMember m : memberMapper.selectList(new LambdaQueryWrapper<ChatRoomMember>()
                .eq(ChatRoomMember::getRoomId, roomId))) {
            members.put(m.getUserId(), m);
        }
        List<MemberVO> result = new ArrayList<>();
        for (Map<String, Object> u : userDirectory().values()) {
            Long uid = Long.valueOf(u.get("id").toString());
            String username = (String) u.get("username");
            String nickname = (String) u.get("nickname");
            String avatar = (String) u.get("avatar");
            ChatRoomMember m = members.remove(uid);
            if (m == null) {
                m = ensureMembership(roomId, uid, username, nickname, avatar);
            } else if (avatar != null && !avatar.isBlank() && !avatar.equals(m.getAvatar())
                    || nickname != null && !nickname.equals(m.getNickname())) {
                // 用户资料变更 → 同步刷新冗余
                m = ensureMembership(roomId, uid, username, nickname, avatar);
            }
            MemberVO vo = new MemberVO();
            vo.setUserId(uid);
            vo.setUsername(username);
            vo.setNickname(nickname);
            vo.setAvatar(avatar != null && !avatar.isBlank() ? avatar : m.getAvatar());
            vo.setRole(m.getRole());
            vo.setStatus(m.getStatus());
            vo.setOnline(onlineIds != null && onlineIds.contains(uid));
            vo.setJoinedAt(m.getCreatedAt());
            result.add(vo);
        }
        // 兜底：目录不可用时至少返回已入群成员
        for (ChatRoomMember m : members.values()) {
            if (!"approved".equals(m.getStatus())) continue;
            MemberVO vo = new MemberVO();
            vo.setUserId(m.getUserId());
            vo.setUsername(m.getUsername());
            vo.setNickname(m.getNickname());
            vo.setAvatar(m.getAvatar());
            vo.setRole(m.getRole());
            vo.setStatus(m.getStatus());
            vo.setOnline(onlineIds != null && onlineIds.contains(m.getUserId()));
            vo.setJoinedAt(m.getCreatedAt());
            result.add(vo);
        }
        // 在线优先，其次按入群时间
        result.sort((a, b) -> {
            int cmp = Boolean.compare(Boolean.TRUE.equals(b.getOnline()), Boolean.TRUE.equals(a.getOnline()));
            if (cmp != 0) return cmp;
            if (a.getJoinedAt() == null || b.getJoinedAt() == null) return 0;
            return a.getJoinedAt().compareTo(b.getJoinedAt());
        });
        return result;
    }

    // ========== 带用户信息的房间视图 ==========
    @Data
    public static class RoomVO {
        private Long id;
        private String name;
        private String description;
        private Integer requireApproval;
        private Long memberCount;
        private String myStatus; // null=未申请, pending/approved/rejected
    }

    public List<RoomVO> listRoomsWithStatus(Long userId) {
        List<ChatRoom> rooms = listRooms();
        return rooms.stream().map(r -> {
            RoomVO vo = new RoomVO();
            vo.setId(r.getId());
            vo.setName(r.getName());
            vo.setDescription(r.getDescription());
            vo.setRequireApproval(r.getRequireApproval());
            vo.setMemberCount((long) listApprovedMembers(r.getId()).size());
            if (userId != null) {
                ChatRoomMember m = getMemberStatus(r.getId(), userId);
                vo.setMyStatus(m != null ? m.getStatus() : null);
            }
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 带登录用户信息的房间视图：所有注册用户对全部公开群自动入群（approved）。
     */
    public List<RoomVO> listRoomsWithStatus(Long userId, String username, String nickname) {
        if (userId == null) return listRoomsWithStatus((Long) null);
        List<ChatRoom> rooms = listRooms();
        return rooms.stream().map(r -> {
            ensureMembership(r.getId(), userId, username, nickname);
            RoomVO vo = new RoomVO();
            vo.setId(r.getId());
            vo.setName(r.getName());
            vo.setDescription(r.getDescription());
            vo.setRequireApproval(r.getRequireApproval());
            vo.setMemberCount((long) listApprovedMembers(r.getId()).size());
            vo.setMyStatus("approved");
            return vo;
        }).collect(Collectors.toList());
    }
}
