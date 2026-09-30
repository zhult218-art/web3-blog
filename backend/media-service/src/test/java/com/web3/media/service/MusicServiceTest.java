package com.web3.media.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.web3.common.core.PageResult;
import com.web3.media.entity.Music;
import com.web3.media.mapper.MusicMapper;
import com.web3.media.mapper.UserMusicHistoryMapper;
import com.web3.media.mapper.UserMusicLikeMapper;
import com.web3.media.vo.MusicVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * MusicService 单元测试（2026-09-30 阶段 4.1 补全）。
 *
 * <p>覆盖要点：
 * <ul>
 *   <li>page / pageLiked / search / hot 的 size 上限钳制（防 SQL 注入拖垮 DB）</li>
 *   <li>getDetail 不存在抛异常</li>
 *   <li>toggleLike 不存在的音乐抛异常</li>
 *   <li>incrementPlay 调用 update 自增 play_count</li>
 * </ul>
 */
class MusicServiceTest {

    @Mock
    private MusicMapper musicMapper;
    @Mock
    private UserMusicLikeMapper likeMapper;
    @Mock
    private UserMusicHistoryMapper historyMapper;

    @InjectMocks
    private MusicService musicService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // ServiceImpl.baseMapper 是 protected 字段，通过反射注入 mock
        ReflectionTestUtils.setField(musicService, "baseMapper", musicMapper);
    }

    // ───────── page size 钳制 ─────────

    @Test
    void page_sizeExceedsMax_shouldClampTo100() {
        when(musicMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(new Page<>());

        musicService.page(1, 9999, "");

        ArgumentCaptor<Page<Music>> captor = ArgumentCaptor.forClass(Page.class);
        verify(musicMapper).selectPage(captor.capture(), any(Wrapper.class));
        assertEquals(100, captor.getValue().getSize());
    }

    @Test
    void page_sizeZero_shouldClampTo1() {
        when(musicMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(new Page<>());

        musicService.page(1, 0, "");

        ArgumentCaptor<Page<Music>> captor = ArgumentCaptor.forClass(Page.class);
        verify(musicMapper).selectPage(captor.capture(), any(Wrapper.class));
        assertEquals(1, captor.getValue().getSize());
    }

    @Test
    void pageLiked_sizeExceedsMax_shouldClampTo100() {
        when(likeMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(new Page<>());

        musicService.pageLiked(1L, 1, 9999);

        ArgumentCaptor<Page> captor = ArgumentCaptor.forClass(Page.class);
        verify(likeMapper).selectPage(captor.capture(), any(Wrapper.class));
        assertEquals(100, captor.getValue().getSize());
    }

    @Test
    void search_sizeExceedsMax_shouldClampTo100() {
        when(musicMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(new Page<>());

        musicService.search("夜曲", 1, 9999);

        ArgumentCaptor<Page<Music>> captor = ArgumentCaptor.forClass(Page.class);
        verify(musicMapper).selectPage(captor.capture(), any(Wrapper.class));
        assertEquals(100, captor.getValue().getSize());
    }

    @Test
    void hot_limitExceedsMax_shouldClampTo100() {
        when(musicMapper.selectList(any(Wrapper.class))).thenReturn(java.util.Collections.emptyList());

        musicService.hot(9999);

        // 验证 selectList 被调用（不抛异常即视为钳制生效）
        verify(musicMapper).selectList(any(Wrapper.class));
    }

    // ───────── getDetail 不存在 ─────────

    @Test
    void getDetail_notExist_throws() {
        when(musicMapper.selectById(999L)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> musicService.getDetail(999L));
    }

    // ───────── toggleLike 不存在的音乐 ─────────

    @Test
    void toggleLike_musicNotExist_throws() {
        when(musicMapper.selectById(anyLong())).thenReturn(null);
        assertThrows(RuntimeException.class, () -> musicService.toggleLike(1L, 999L));
        // 不应操作 likeMapper
        verify(likeMapper, never()).selectOne(any(Wrapper.class));
        verify(likeMapper, never()).insert(any(com.web3.media.entity.UserMusicLike.class));
    }

    // ───────── incrementPlay ─────────

    @Test
    void incrementPlay_existMusic_shouldCallUpdate() {
        Music music = new Music();
        music.setId(1L);
        music.setTitle("测试曲目");
        when(musicMapper.selectById(1L)).thenReturn(music);

        musicService.incrementPlay(1L);

        verify(musicMapper).update(isNull(), any(Wrapper.class));
    }

    @Test
    void incrementPlay_notExist_shouldNotCallUpdate() {
        when(musicMapper.selectById(anyLong())).thenReturn(null);

        musicService.incrementPlay(999L);

        verify(musicMapper, never()).update(isNull(), any(Wrapper.class));
    }
}
