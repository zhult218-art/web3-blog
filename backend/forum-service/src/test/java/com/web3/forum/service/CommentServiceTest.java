package com.web3.forum.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.web3.common.core.UnauthorizedException;
import com.web3.forum.dto.CommentCreateDTO;
import com.web3.forum.dto.CommentVO;
import com.web3.forum.entity.Comment;
import com.web3.forum.entity.Post;
import com.web3.forum.mapper.CommentMapper;
import com.web3.forum.mapper.PostMapper;
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
 * CommentService 单元测试（2026-09-30 阶段 4.1 补全）。
 *
 * <p>覆盖要点：
 * <ul>
 *   <li>create 管理员 → 评论直接 published</li>
 *   <li>create 普通用户 → 评论进入 pending 待审核</li>
 *   <li>delete 非作者非管理员 → 抛 UnauthorizedException（越权拒绝）</li>
 *   <li>delete 作者本人 → 正常删除</li>
 * </ul>
 */
class CommentServiceTest {

    @Mock
    private CommentMapper commentMapper;
    @Mock
    private PostMapper postMapper;

    @InjectMocks
    private CommentService commentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // ServiceImpl.baseMapper 是 protected 字段，通过反射注入 mock
        ReflectionTestUtils.setField(commentService, "baseMapper", commentMapper);
    }

    // ───────── create ─────────

    @Test
    void create_admin_commentShouldBePublished() {
        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setTargetId(1L);
        dto.setTargetType("post");
        dto.setContent("管理员评论");
        when(commentMapper.insert(any(Comment.class))).thenReturn(1);

        CommentVO vo = commentService.create(dto, 99L, true);

        assertEquals("published", vo.getStatus());
        verify(commentMapper).insert(any(Comment.class));
    }

    @Test
    void create_normalUser_commentShouldBePending() {
        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setTargetId(1L);
        dto.setTargetType("post");
        dto.setContent("用户评论");
        when(commentMapper.insert(any(Comment.class))).thenReturn(1);

        CommentVO vo = commentService.create(dto, 11L, false);

        assertEquals("pending", vo.getStatus());
    }

    // ───────── delete 越权拒绝 ─────────

    @Test
    void delete_nonAuthorNonAdmin_throwsUnauthorized() {
        Comment comment = new Comment();
        comment.setId(500L);
        comment.setAuthorId(11L); // 作者是 11
        when(commentMapper.selectById(500L)).thenReturn(comment);

        // 当前用户 22，非管理员 → 越权
        assertThrows(UnauthorizedException.class,
                () -> commentService.delete(500L, 22L, false));
        verify(commentMapper, never()).deleteById(anyLong());
    }

    @Test
    void delete_owner_shouldDelete() {
        Comment comment = new Comment();
        comment.setId(500L);
        comment.setAuthorId(11L);
        when(commentMapper.selectById(500L)).thenReturn(comment);

        commentService.delete(500L, 11L, false);

        verify(commentMapper).deleteById(500L);
    }

    @Test
    void delete_admin_canDeleteOthers() {
        Comment comment = new Comment();
        comment.setId(500L);
        comment.setAuthorId(11L);
        when(commentMapper.selectById(500L)).thenReturn(comment);

        // 管理员 99 删除 11 的评论
        commentService.delete(500L, 99L, true);

        verify(commentMapper).deleteById(500L);
    }

    @Test
    void delete_notExist_throwsRuntime() {
        when(commentMapper.selectById(999L)).thenReturn(null);
        assertThrows(RuntimeException.class,
                () -> commentService.delete(999L, 1L, false));
    }
}
