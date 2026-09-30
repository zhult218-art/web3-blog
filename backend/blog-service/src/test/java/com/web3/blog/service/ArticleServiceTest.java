package com.web3.blog.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.web3.blog.entity.Article;
import com.web3.blog.mapper.ArticleLikeMapper;
import com.web3.blog.mapper.ArticleMapper;
import com.web3.blog.mapper.UserFavoriteMapper;
import com.web3.common.core.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ArticleService 单元测试（2026-09-30 阶段 4.1 补全）。
 *
 * <p>覆盖要点：
 * <ul>
 *   <li>page size 上限钳制（防 SQL 注入拖垮 DB）</li>
 *   <li>delete 非作者非管理员 → 抛 UnauthorizedException（越权拒绝）</li>
 *   <li>delete 作者本人 → 正常删除</li>
 *   <li>delete 管理员 → 可删除他人文章</li>
 * </ul>
 */
class ArticleServiceTest {

    @Mock
    private ArticleMapper articleMapper;
    @Mock
    private ArticleLikeMapper articleLikeMapper;
    @Mock
    private UserFavoriteMapper userFavoriteMapper;

    @InjectMocks
    private ArticleService articleService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // ServiceImpl.baseMapper 是 protected 字段，通过反射注入 mock
        ReflectionTestUtils.setField(articleService, "baseMapper", articleMapper);
    }

    // ───────── page size 钳制 ─────────

    @Test
    void page_sizeExceedsMax_shouldClampTo100() {
        when(articleMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(new Page<>());

        articleService.page(1, 9999, null, null, null);

        ArgumentCaptor<Page<Article>> captor = ArgumentCaptor.forClass(Page.class);
        verify(articleMapper).selectPage(captor.capture(), any(Wrapper.class));
        assertEquals(100, captor.getValue().getSize());
    }

    @Test
    void page_sizeNegative_shouldClampTo1() {
        when(articleMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(new Page<>());

        articleService.page(1, -1, null, null, null);

        ArgumentCaptor<Page<Article>> captor = ArgumentCaptor.forClass(Page.class);
        verify(articleMapper).selectPage(captor.capture(), any(Wrapper.class));
        assertEquals(1, captor.getValue().getSize());
    }

    // ───────── delete 越权拒绝 ─────────

    @Test
    void delete_nonAuthorNonAdmin_throwsUnauthorized() {
        Article article = new Article();
        article.setId(100L);
        article.setAuthorId(11L); // 作者是 11
        when(articleMapper.selectById(100L)).thenReturn(article);

        // 当前用户 22，非管理员 → 越权
        assertThrows(UnauthorizedException.class,
                () -> articleService.delete(100L, 22L, false));
        verify(articleMapper, never()).deleteById(anyLong());
    }

    @Test
    void delete_owner_shouldDelete() {
        Article article = new Article();
        article.setId(100L);
        article.setAuthorId(11L);
        when(articleMapper.selectById(100L)).thenReturn(article);

        articleService.delete(100L, 11L, false);

        verify(articleMapper).deleteById(100L);
    }

    @Test
    void delete_admin_canDeleteOthers() {
        Article article = new Article();
        article.setId(100L);
        article.setAuthorId(11L);
        when(articleMapper.selectById(100L)).thenReturn(article);

        // 管理员 99 删除 11 的文章
        articleService.delete(100L, 99L, true);

        verify(articleMapper).deleteById(100L);
    }

    @Test
    void delete_notExist_shouldReturnSilently() {
        when(articleMapper.selectById(999L)).thenReturn(null);

        // 不存在则静默返回，不抛异常
        assertDoesNotThrow(() -> articleService.delete(999L, 1L, false));
        verify(articleMapper, never()).deleteById(anyLong());
    }
}
