package com.web3.forum.service;

import com.web3.common.core.UnauthorizedException;
import com.web3.common.core.BusinessException;
import com.web3.forum.entity.ChatAlbum;
import com.web3.forum.entity.ChatPhoto;
import com.web3.forum.mapper.ChatAlbumMapper;
import com.web3.forum.mapper.ChatAnnouncementMapper;
import com.web3.forum.mapper.ChatMessageMapper;
import com.web3.forum.mapper.ChatNotificationMapper;
import com.web3.forum.mapper.ChatPhotoMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ChatExtraServiceSecurityTest {

    @Test
    void deletingAnotherUsersPhotoIsForbidden() {
        ChatPhotoMapper photos = mock(ChatPhotoMapper.class);
        ChatPhoto photo = new ChatPhoto();
        photo.setId(11L);
        photo.setUserId(22L);
        when(photos.selectById(11L)).thenReturn(photo);

        ChatExtraService service = new ChatExtraService(
                mock(ChatAnnouncementMapper.class),
                mock(ChatAlbumMapper.class),
                photos,
                mock(ChatNotificationMapper.class),
                mock(ChatMessageMapper.class));

        UnauthorizedException error = assertThrows(
                UnauthorizedException.class, () -> service.deletePhoto(11L, 33L, false));

        assertEquals(403, error.getCode());
        verify(photos, never()).deleteById(11L);
    }

    @Test
    void movingPhotoAcrossRoomsIsRejected() {
        ChatPhotoMapper photos = mock(ChatPhotoMapper.class);
        ChatAlbumMapper albums = mock(ChatAlbumMapper.class);
        ChatPhoto photo = new ChatPhoto();
        photo.setId(11L);
        photo.setUserId(22L);
        photo.setRoomId(44L);
        ChatAlbum target = new ChatAlbum();
        target.setId(99L);
        target.setRoomId(55L);
        when(photos.selectById(11L)).thenReturn(photo);
        when(albums.selectById(99L)).thenReturn(target);
        ChatExtraService service = new ChatExtraService(
                mock(ChatAnnouncementMapper.class), albums, photos,
                mock(ChatNotificationMapper.class), mock(ChatMessageMapper.class));

        BusinessException error = assertThrows(
                BusinessException.class, () -> service.movePhoto(11L, 99L, 22L, false));

        assertEquals(400, error.getCode());
        verify(photos, never()).updateById(photo);
    }
    @Test
    void adminCanDeleteAnotherUsersPhoto() {
        ChatPhotoMapper photos = mock(ChatPhotoMapper.class);
        ChatPhoto photo = new ChatPhoto();
        photo.setId(11L);
        photo.setUserId(22L);
        when(photos.selectById(11L)).thenReturn(photo);

        ChatExtraService service = new ChatExtraService(
                mock(ChatAnnouncementMapper.class),
                mock(ChatAlbumMapper.class),
                photos,
                mock(ChatNotificationMapper.class),
                mock(ChatMessageMapper.class));

        service.deletePhoto(11L, 33L, true);

        verify(photos).deleteById(11L);
    }
}
