package com.web3.forum.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.web3.forum.entity.ChatAlbum;
import com.web3.forum.entity.ChatAnnouncement;
import com.web3.forum.entity.ChatMessage;
import com.web3.forum.entity.ChatNotification;
import com.web3.forum.entity.ChatPhoto;
import com.web3.common.core.UnauthorizedException;
import com.web3.common.core.BusinessException;
import java.util.Objects;
import com.web3.forum.mapper.ChatAlbumMapper;
import com.web3.forum.mapper.ChatAnnouncementMapper;
import com.web3.forum.mapper.ChatMessageMapper;
import com.web3.forum.mapper.ChatNotificationMapper;
import com.web3.forum.mapper.ChatPhotoMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.springframework.stereotype.Service;

/**
 * 群聊增强服务：群公告、群相册（文件夹）、照片、群通知、聊天记录搜索
 */
@Service
public class ChatExtraService {

    private final ChatAnnouncementMapper announcementMapper;
    private final ChatAlbumMapper albumMapper;
    private final ChatPhotoMapper photoMapper;
    private final ChatNotificationMapper notificationMapper;
    private final ChatMessageMapper messageMapper;

    public ChatExtraService(ChatAnnouncementMapper announcementMapper, ChatAlbumMapper albumMapper,
                            ChatPhotoMapper photoMapper, ChatNotificationMapper notificationMapper,
                            ChatMessageMapper messageMapper) {
        this.announcementMapper = announcementMapper;
        this.albumMapper = albumMapper;
        this.photoMapper = photoMapper;
        this.notificationMapper = notificationMapper;
        this.messageMapper = messageMapper;
    }

    // ========== 群公告 ==========

    public List<ChatAnnouncement> listAnnouncements(Long roomId) {
        List<ChatAnnouncement> list = announcementMapper.selectList(new LambdaQueryWrapper<ChatAnnouncement>()
                .eq(ChatAnnouncement::getRoomId, roomId)
                .orderByDesc(ChatAnnouncement::getPinned)
                .orderByDesc(ChatAnnouncement::getCreatedAt));
        return list;
    }

    public ChatAnnouncement createAnnouncement(Long roomId, Long userId, String nickname,
                                                String title, String content, Integer pinned) {
        ChatAnnouncement a = new ChatAnnouncement();
        a.setRoomId(roomId);
        a.setUserId(userId);
        a.setNickname(nickname);
        a.setTitle(title);
        a.setContent(content);
        a.setPinned(pinned != null && pinned == 1 ? 1 : 0);
        a.setCreatedAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());
        announcementMapper.insert(a);
        return a;
    }

    public ChatAnnouncement updateAnnouncement(Long id, String title, String content, Integer pinned, Long userId, boolean admin) {
        ChatAnnouncement a = announcementMapper.selectById(id);
        if (a == null) throw new RuntimeException("公告不存在");
        ensureOwnerOrAdmin(a.getUserId(), userId, admin, "公告");
        if (title != null) a.setTitle(title);
        if (content != null) a.setContent(content);
        if (pinned != null) a.setPinned(pinned);
        a.setUpdatedAt(LocalDateTime.now());
        announcementMapper.updateById(a);
        return a;
    }

    public void deleteAnnouncement(Long id, Long userId, boolean admin) {
        ChatAnnouncement a = announcementMapper.selectById(id);
        if (a == null) return;
        ensureOwnerOrAdmin(a.getUserId(), userId, admin, "公告");
        announcementMapper.deleteById(id);
    }

    // ========== 群相册（文件夹） ==========

    @Data
    public static class AlbumVO {
        private Long id;
        private Long roomId;
        private String name;
        private String coverUrl;
        private Long photoCount;
        private String nickname;
        private LocalDateTime createdAt;
    }

    public List<AlbumVO> listAlbums(Long roomId) {
        List<ChatAlbum> albums = albumMapper.selectList(new LambdaQueryWrapper<ChatAlbum>()
                .eq(ChatAlbum::getRoomId, roomId)
                .orderByAsc(ChatAlbum::getId));
        List<ChatPhoto> photos = photoMapper.selectList(new LambdaQueryWrapper<ChatPhoto>()
                .eq(ChatPhoto::getRoomId, roomId));
        Map<Long, Long> countMap = new LinkedHashMap<>();
        Map<Long, String> firstUrl = new LinkedHashMap<>();
        for (ChatPhoto p : photos) {
            countMap.merge(p.getAlbumId(), 1L, Long::sum);
            firstUrl.putIfAbsent(p.getAlbumId(), p.getUrl());
        }
        List<AlbumVO> result = new ArrayList<>();
        for (ChatAlbum a : albums) {
            AlbumVO vo = new AlbumVO();
            vo.setId(a.getId());
            vo.setRoomId(a.getRoomId());
            vo.setName(a.getName());
            vo.setCoverUrl(a.getCoverUrl() != null ? a.getCoverUrl() : firstUrl.get(a.getId()));
            vo.setPhotoCount(countMap.getOrDefault(a.getId(), 0L));
            vo.setNickname(a.getNickname());
            vo.setCreatedAt(a.getCreatedAt());
            result.add(vo);
        }
        return result;
    }

    public ChatAlbum createAlbum(Long roomId, Long userId, String nickname, String name) {
        ChatAlbum a = new ChatAlbum();
        a.setRoomId(roomId);
        a.setUserId(userId);
        a.setNickname(nickname);
        a.setName(name);
        a.setCreatedAt(LocalDateTime.now());
        albumMapper.insert(a);
        return a;
    }

    public ChatAlbum renameAlbum(Long id, String name, Long userId, boolean admin) {
        ChatAlbum a = albumMapper.selectById(id);
        if (a == null) throw new RuntimeException("相册不存在");
        ensureOwnerOrAdmin(a.getUserId(), userId, admin, "相册");
        a.setName(name);
        albumMapper.updateById(a);
        return a;
    }

    public void deleteAlbum(Long id, Long userId, boolean admin) {
        ChatAlbum album = albumMapper.selectById(id);
        if (album == null) return;
        ensureOwnerOrAdmin(album.getUserId(), userId, admin, "相册");
        // 删除相册下的照片关联记录
        photoMapper.delete(new LambdaQueryWrapper<ChatPhoto>().eq(ChatPhoto::getAlbumId, id));
        albumMapper.deleteById(id);
    }

    // ========== 照片 ==========

    public List<ChatPhoto> listPhotos(Long roomId, Long albumId) {
        LambdaQueryWrapper<ChatPhoto> wrapper = new LambdaQueryWrapper<ChatPhoto>()
                .eq(ChatPhoto::getRoomId, roomId)
                .orderByDesc(ChatPhoto::getCreatedAt);
        if (albumId != null) wrapper.eq(ChatPhoto::getAlbumId, albumId);
        return photoMapper.selectList(wrapper);
    }

    public ChatPhoto addPhoto(Long roomId, Long albumId, Long userId, String nickname,
                              String url, Integer width, Integer height, boolean admin) {
        ChatAlbum album = albumMapper.selectById(albumId);
        if (album == null || !Objects.equals(album.getRoomId(), roomId)) {
            throw new BusinessException(400, "相册不存在或不属于该群");
        }
        if (album.getUserId() != null) ensureOwnerOrAdmin(album.getUserId(), userId, admin, "相册");
        ChatPhoto p = new ChatPhoto();
        p.setRoomId(roomId);
        p.setAlbumId(albumId);
        p.setUserId(userId);
        p.setNickname(nickname);
        p.setUrl(url);
        p.setWidth(width);
        p.setHeight(height);
        p.setCreatedAt(LocalDateTime.now());
        photoMapper.insert(p);
        return p;
    }

    public ChatPhoto movePhoto(Long id, Long targetAlbumId, Long userId, boolean admin) {
        ChatPhoto p = photoMapper.selectById(id);
        if (p == null) throw new RuntimeException("照片不存在");
        ensureOwnerOrAdmin(p.getUserId(), userId, admin, "照片");
        ChatAlbum targetAlbum = albumMapper.selectById(targetAlbumId);
        if (targetAlbum == null || !Objects.equals(targetAlbum.getRoomId(), p.getRoomId())) {
            throw new BusinessException(400, "目标相册不存在或不属于该群");
        }
        if (targetAlbum.getUserId() != null) ensureOwnerOrAdmin(targetAlbum.getUserId(), userId, admin, "目标相册");
        p.setAlbumId(targetAlbumId);
        photoMapper.updateById(p);
        return p;
    }

    public void deletePhoto(Long id, Long userId, boolean admin) {
        ChatPhoto photo = photoMapper.selectById(id);
        if (photo == null) return;
        ensureOwnerOrAdmin(photo.getUserId(), userId, admin, "照片");
        photoMapper.deleteById(id);
    }

    // ========== 群通知 ==========

    public List<ChatNotification> listNotifications(Long roomId) {
        return notificationMapper.selectList(new LambdaQueryWrapper<ChatNotification>()
                .eq(ChatNotification::getRoomId, roomId)
                .orderByDesc(ChatNotification::getCreatedAt));
    }

    public ChatNotification createNotification(Long roomId, Long userId, String nickname,
                                                String type, String title, String content) {
        ChatNotification n = new ChatNotification();
        n.setRoomId(roomId);
        n.setUserId(userId);
        n.setNickname(nickname);
        n.setType(type != null ? type : "info");
        n.setTitle(title);
        n.setContent(content);
        n.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(n);
        return n;
    }

    public void deleteNotification(Long id, Long userId, boolean admin) {
        ChatNotification notification = notificationMapper.selectById(id);
        if (notification == null) return;
        ensureOwnerOrAdmin(notification.getUserId(), userId, admin, "通知");
        notificationMapper.deleteById(id);
    }

    private void ensureOwnerOrAdmin(Long ownerId, Long userId, boolean admin, String resource) {
        if (!admin && (ownerId == null || !ownerId.equals(userId))) {
            throw new UnauthorizedException(403, "无权修改该" + resource);
        }
    }

    // ========== 聊天记录搜索 ==========

    public List<ChatMessage> searchMessages(Long roomId, String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isEmpty()) return new ArrayList<>();
        String safe = kw.replace("%", "").replace("_", "");
        return messageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getRoomId, roomId)
                .like(ChatMessage::getContent, safe)
                .orderByDesc(ChatMessage::getCreatedAt)
                .last("LIMIT 50"));
    }
}
