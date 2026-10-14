package com.web3.forum.controller;

import com.web3.common.core.ApiResponse;
import com.web3.common.core.UnauthorizedException;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import com.web3.forum.entity.ChatAlbum;
import com.web3.forum.entity.ChatAnnouncement;
import com.web3.forum.entity.ChatNotification;
import com.web3.forum.entity.ChatPhoto;
import com.web3.forum.service.ChatExtraService;
import io.jsonwebtoken.Claims;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 群聊增强接口：群公告、群相册、照片、群通知、聊天记录搜索
 */
@RestController
public class ChatExtraController {

    private final ChatExtraService extraService;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    public ChatExtraController(ChatExtraService extraService, JwtUtil jwtUtil, JwtProperties jwtProperties) {
        this.extraService = extraService;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
    }

    /** 解析当前登录用户信息 */
    private Claims claims(String auth) {
        String token = AuthUtils.resolveToken(auth, jwtProperties);
        return jwtUtil.parseClaims(token);
    }
    private Long uid(String auth) {
        return AuthUtils.requireUserId(auth, jwtUtil, jwtProperties);
    }
    private boolean isAdmin(String auth) {
        try {
            AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
            return true;
        } catch (UnauthorizedException ex) {
            if (ex.getCode() == 403) return false;
            throw ex;
        }
    }
    private String nick(Claims c) {
        String n = c.get("nickname", String.class);
        return n != null ? n : c.get("username", String.class);
    }

    // ========== 群公告 ==========

    @GetMapping("/chat/rooms/{roomId}/announcements")
    public ApiResponse<List<ChatAnnouncement>> listAnnouncements(@PathVariable Long roomId) {
        return ApiResponse.ok(extraService.listAnnouncements(roomId));
    }

    @PostMapping("/chat/rooms/{roomId}/announcements")
    public ApiResponse<ChatAnnouncement> createAnnouncement(@PathVariable Long roomId,
            @RequestHeader("Authorization") String auth, @RequestBody Map<String, Object> body) {
        Long userId = uid(auth);
        boolean admin = isAdmin(auth);
        Claims c = claims(auth);
        return ApiResponse.ok(extraService.createAnnouncement(roomId, userId, nick(c),
                String.valueOf(body.getOrDefault("title", "")),
                String.valueOf(body.getOrDefault("content", "")),
                admin && body.get("pinned") != null && Boolean.parseBoolean(String.valueOf(body.get("pinned"))) ? 1 : 0));
    }

    @PutMapping("/chat/announcements/{id}")
    public ApiResponse<ChatAnnouncement> updateAnnouncement(@PathVariable Long id,
            @RequestHeader("Authorization") String auth,
            @RequestBody Map<String, Object> body) {
        Long userId = uid(auth);
        boolean admin = isAdmin(auth);
        return ApiResponse.ok(extraService.updateAnnouncement(id,
                body.containsKey("title") ? String.valueOf(body.get("title")) : null,
                body.containsKey("content") ? String.valueOf(body.get("content")) : null,
                body.containsKey("pinned") && admin ? (Boolean.parseBoolean(String.valueOf(body.get("pinned"))) ? 1 : 0) : null, userId, admin));
    }

    @DeleteMapping("/chat/announcements/{id}")
    public ApiResponse<Void> deleteAnnouncement(@PathVariable Long id, @RequestHeader("Authorization") String auth) {
        Long userId = uid(auth);
        extraService.deleteAnnouncement(id, userId, isAdmin(auth));
        return ApiResponse.ok();
    }

    // ========== 群相册 ==========

    @GetMapping("/chat/rooms/{roomId}/albums")
    public ApiResponse<List<ChatExtraService.AlbumVO>> listAlbums(@PathVariable Long roomId) {
        return ApiResponse.ok(extraService.listAlbums(roomId));
    }

    @PostMapping("/chat/rooms/{roomId}/albums")
    public ApiResponse<ChatAlbum> createAlbum(@PathVariable Long roomId,
            @RequestHeader("Authorization") String auth, @RequestBody Map<String, Object> body) {
        Claims c = claims(auth);
        return ApiResponse.ok(extraService.createAlbum(roomId, uid(auth), nick(c),
                String.valueOf(body.getOrDefault("name", "新相册"))));
    }

    @PutMapping("/chat/albums/{id}")
    public ApiResponse<ChatAlbum> renameAlbum(@PathVariable Long id, @RequestBody Map<String, Object> body,
            @RequestHeader("Authorization") String auth) {
        Long userId = uid(auth);
        return ApiResponse.ok(extraService.renameAlbum(id, String.valueOf(body.getOrDefault("name", "新相册")), userId, isAdmin(auth)));
    }

    @DeleteMapping("/chat/albums/{id}")
    public ApiResponse<Void> deleteAlbum(@PathVariable Long id, @RequestHeader("Authorization") String auth) {
        Long userId = uid(auth);
        extraService.deleteAlbum(id, userId, isAdmin(auth));
        return ApiResponse.ok();
    }

    // ========== 照片 ==========

    @GetMapping("/chat/rooms/{roomId}/photos")
    public ApiResponse<List<ChatPhoto>> listPhotos(@PathVariable Long roomId,
            @RequestParam(value = "albumId", required = false) Long albumId) {
        return ApiResponse.ok(extraService.listPhotos(roomId, albumId));
    }

    @PostMapping("/chat/rooms/{roomId}/photos")
    public ApiResponse<ChatPhoto> addPhoto(@PathVariable Long roomId,
            @RequestHeader("Authorization") String auth, @RequestBody Map<String, Object> body) {
        Claims c = claims(auth);
        Long albumId = body.get("albumId") != null
                ? Long.parseLong(String.valueOf(body.get("albumId"))) : 1L;
        Integer width = body.get("width") != null ? Integer.parseInt(String.valueOf(body.get("width"))) : null;
        Integer height = body.get("height") != null ? Integer.parseInt(String.valueOf(body.get("height"))) : null;
        return ApiResponse.ok(extraService.addPhoto(roomId, albumId, uid(auth), nick(c),
                String.valueOf(body.getOrDefault("url", "")), width, height));
    }

    @PutMapping("/chat/photos/{id}/move")
    public ApiResponse<ChatPhoto> movePhoto(@PathVariable Long id, @RequestBody Map<String, Object> body,
            @RequestHeader("Authorization") String auth) {
        Long target = Long.parseLong(String.valueOf(body.getOrDefault("albumId", "1")));
        Long userId = uid(auth);
        return ApiResponse.ok(extraService.movePhoto(id, target, userId, isAdmin(auth)));
    }

    @DeleteMapping("/chat/photos/{id}")
    public ApiResponse<Void> deletePhoto(@PathVariable Long id, @RequestHeader("Authorization") String auth) {
        Long userId = uid(auth);
        extraService.deletePhoto(id, userId, isAdmin(auth));
        return ApiResponse.ok();
    }

    // ========== 群通知 ==========

    @GetMapping("/chat/rooms/{roomId}/notifications")
    public ApiResponse<List<ChatNotification>> listNotifications(@PathVariable Long roomId) {
        return ApiResponse.ok(extraService.listNotifications(roomId));
    }

    @PostMapping("/chat/rooms/{roomId}/notifications")
    public ApiResponse<ChatNotification> createNotification(@PathVariable Long roomId,
            @RequestHeader("Authorization") String auth, @RequestBody Map<String, Object> body) {
        Claims c = claims(auth);
        return ApiResponse.ok(extraService.createNotification(roomId, uid(auth), nick(c),
                String.valueOf(body.getOrDefault("type", "info")),
                String.valueOf(body.getOrDefault("title", "")),
                String.valueOf(body.getOrDefault("content", ""))));
    }

    @DeleteMapping("/chat/notifications/{id}")
    public ApiResponse<Void> deleteNotification(@PathVariable Long id, @RequestHeader("Authorization") String auth) {
        Long userId = uid(auth);
        extraService.deleteNotification(id, userId, isAdmin(auth));
        return ApiResponse.ok();
    }

    // ========== 聊天记录搜索 ==========

    @GetMapping("/chat/rooms/{roomId}/search")
    public ApiResponse<List<com.web3.forum.entity.ChatMessage>> search(@PathVariable Long roomId,
            @RequestParam("keyword") String keyword) {
        return ApiResponse.ok(extraService.searchMessages(roomId, keyword));
    }
}
