package com.web3.forum.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.web3.forum.dto.CommentCreateDTO;
import com.web3.forum.dto.CommentVO;
import com.web3.forum.entity.Comment;
import com.web3.forum.entity.Post;
import com.web3.forum.mapper.CommentMapper;
import com.web3.forum.mapper.PostMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CommentService —— 评论业务服务
 * <p>
 * 所属模块：forum-service（论坛模块）。
 * <p>
 * 职责：实现评论的核心业务逻辑，包括按目标对象分页查询评论、发表评论（评论帖子时同步帖子回复数 +1）、
 * 删除评论（含权限校验），以及实体到视图对象（CommentVO）的转换。
 * <p>
 * 关键注解作用：
 * <ul>
 *   <li>@Service：声明为 Spring 业务组件</li>
 *   <li>继承 ServiceImpl&lt;CommentMapper, Comment&gt;：获得 MyBatis-Plus 内置 CRUD 能力</li>
 * </ul>
 */
@Service
public class CommentService
extends ServiceImpl<CommentMapper, Comment> {
    /** 帖子 Mapper（评论帖子时维护帖子回复数） */
    private final PostMapper postMapper;

    public CommentService(PostMapper postMapper) {
        this.postMapper = postMapper;
    }

    /**
     * 分页查询某目标对象下的评论列表，按创建时间正序返回
     *
     * @param targetId   目标对象 ID（如帖子 ID）
     * @param targetType 目标类型（如 post）
     * @param page       页码（从 1 开始）
     * @param size       每页条数
     * @return 评论视图对象的分页结果
     */
    /**
     * 分页查询某目标对象下的评论列表，返回楼中楼树形结构（顶级评论 + children 子评论），按创建时间正序
     * <p>
     * 实现说明：一次取出该目标下的全部评论，在内存中按 parentId 组装为树（顶级评论 parentId 为空），
     * 再对顶级评论做分页；每个顶级评论的 children 收录其所有后代回复（递归）。
     *
     * @param targetId   目标对象 ID（如帖子 ID）
     * @param targetType 目标类型（如 post）
     * @param page       页码（从 1 开始）
     * @param size       每页条数
     * @return 评论视图对象的树形分页结果
     */
    public Page<CommentVO> page(Long targetId, String targetType, int page, int size) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<Comment>();
        // 公开评论树仅包含审核通过的评论
        wrapper.eq(Comment::getTargetId, (Object)targetId).eq(Comment::getTargetType, (Object)targetType)
                .eq(Comment::getStatus, (Object) "published").orderByAsc(Comment::getCreatedAt);
        java.util.List<Comment> all = this.baseMapper.selectList(wrapper);
        java.util.List<CommentVO> voList = all.stream().map(this::toVO).toList();
        java.util.List<CommentVO> tops = new java.util.ArrayList<CommentVO>();
        java.util.Map<Long, CommentVO> byId = new java.util.LinkedHashMap<Long, CommentVO>();
        for (CommentVO vo : voList) {
            byId.put(vo.getId(), vo);
        }
        for (CommentVO vo : voList) {
            Long pid = vo.getParentId();
            CommentVO parent = pid == null ? null : byId.get(pid);
            if (parent == null) {
                tops.add(vo);
            } else {
                parent.addChild(vo);
            }
        }
        int total = tops.size();
        int from = Math.min((page - 1) * size, total);
        int to = Math.min(from + size, total);
        Page<CommentVO> voPage = new Page<CommentVO>((long)page, (long)size, total);
        voPage.setRecords(new java.util.ArrayList<CommentVO>(tops.subList(from, to)));
        return voPage;
    }

    /**
     * 发表评论（同一事务）。管理员评论直接 published，普通用户/匿名评论进入 pending 待审核；
     * 仅已通过的顶级帖子评论会立刻让帖子回复数 +1，待审核评论通过后再计数
     *
     * @param dto      评论创建参数
     * @param authorId 评论者用户 ID（取自登录令牌，匿名为 null）
     * @param admin    评论者是否为管理员
     * @return 创建成功后的评论视图对象
     */
    @Transactional
    public CommentVO create(CommentCreateDTO dto, Long authorId, boolean admin) {
        Post post;
        Comment comment = new Comment();
        comment.setTargetId(dto.getTargetId());
        comment.setTargetType(dto.getTargetType());
        comment.setContent(dto.getContent());
        comment.setParentId(dto.getParentId());
        comment.setReplyToName(dto.getReplyToName());
        comment.setAuthorId(authorId);
        comment.setAuthorName(dto.getAuthorName());
        comment.setStatus(admin ? "published" : "pending");
        this.baseMapper.insert(comment);
        if (admin && "post".equals(dto.getTargetType()) && dto.getParentId() == null && (post = this.postMapper.selectById(dto.getTargetId())) != null) {
            post.setReplyCount(safeReplyCount(post) + 1);
            this.postMapper.updateById(post);
        }
        return this.toVO(comment);
    }

    /**
     * 管理端评论分页：跨帖子按时间倒序，可按状态/关键字/目标帖子筛选，附带所属帖子标题
     *
     * @param page     页码
     * @param size     每页条数
     * @param status   pending/published/rejected，空或 all 为全部
     * @param keyword  评论内容关键字
     * @param targetId 目标帖子 ID，可空
     * @return 评论视图对象分页结果
     */
    public Page<CommentVO> adminPage(int page, int size, String status, String keyword, Long targetId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<Comment>();
        if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
            wrapper.eq(Comment::getStatus, (Object) status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Comment::getContent, (Object) keyword);
        }
        if (targetId != null) {
            wrapper.eq(Comment::getTargetId, (Object) targetId);
        }
        wrapper.orderByDesc(Comment::getCreatedAt);
        Page<Comment> commentPage = new Page<Comment>((long) page, (long) size);
        Page<Comment> result = this.baseMapper.selectPage(commentPage, wrapper);
        Page<CommentVO> voPage = new Page<CommentVO>((long) page, (long) size, result.getTotal());
        java.util.List<CommentVO> records = result.getRecords().stream().map(this::toVO).toList();
        fillPostTitles(records);
        voPage.setRecords(records);
        return voPage;
    }

    /**
     * 管理端审核评论：通过 / 驳回，并维护顶级帖子评论的回复计数
     *
     * @param id     评论 ID
     * @param status 目标状态 published / rejected
     */
    @Transactional
    public void moderate(Long id, String status) {
        Comment comment = (Comment) this.baseMapper.selectById(id);
        if (comment == null) {
            throw new RuntimeException("Comment not found");
        }
        String oldStatus = comment.getStatus();
        if (status.equals(oldStatus)) {
            return;
        }
        comment.setStatus(status);
        this.baseMapper.updateById(comment);
        adjustPostReplyCount(comment, oldStatus, status);
    }

    /**
     * 管理端删除评论（逻辑删除）；若该评论此前已通过且为顶级帖子评论，回复数 -1
     *
     * @param id 评论 ID
     */
    @Transactional
    public void adminDelete(Long id) {
        Comment comment = (Comment) this.baseMapper.selectById(id);
        if (comment == null) {
            throw new RuntimeException("Comment not found");
        }
        this.baseMapper.deleteById(id);
        if ("published".equals(comment.getStatus())) {
            adjustPostReplyCount(comment, "published", "deleted");
        }
    }

    /**
     * 管理端批量审核：复用单条 moderate 逻辑（自动维护帖子回复数），
     * 跳过不存在的记录
     *
     * @param ids    评论 ID 列表
     * @param status 目标状态（published / rejected）
     * @return 实际处理条数
     */
    @Transactional
    public int batchModerate(java.util.List<Long> ids, String status) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (Long id : ids) {
            Comment comment = (Comment) this.baseMapper.selectById(id);
            if (comment == null || status.equals(comment.getStatus())) {
                continue;
            }
            this.moderate(id, status);
            count++;
        }
        return count;
    }

    /**
     * 管理端批量删除评论（逻辑删除，复用单条删除的回复数联动逻辑）
     *
     * @param ids 评论 ID 列表
     * @return 实际处理条数
     */
    @Transactional
    public int batchDelete(java.util.List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (Long id : ids) {
            if (this.baseMapper.selectById(id) != null) {
                this.adminDelete(id);
                count++;
            }
        }
        return count;
    }

    /**
     * 状态迁移时维护帖子回复数：仅顶级帖子评论参与计数，
     * 非 published → published 时 +1，published → 其他状态时 -1
     */
    private void adjustPostReplyCount(Comment comment, String oldStatus, String newStatus) {
        if (!"post".equals(comment.getTargetType()) || comment.getParentId() != null) {
            return;
        }
        boolean wasPublished = "published".equals(oldStatus);
        boolean becomesPublished = "published".equals(newStatus);
        if (wasPublished == becomesPublished) {
            return;
        }
        Post post = (Post) this.postMapper.selectById(comment.getTargetId());
        if (post == null) {
            return;
        }
        int next = safeReplyCount(post) + (becomesPublished ? 1 : -1);
        post.setReplyCount(Math.max(0, next));
        this.postMapper.updateById(post);
    }

    private int safeReplyCount(Post post) {
        return post.getReplyCount() == null ? 0 : post.getReplyCount();
    }

    /** 批量填充评论所属帖子标题（避免逐条查询） */
    private void fillPostTitles(java.util.List<CommentVO> records) {
        java.util.List<Long> postIds = records.stream()
                .filter(v -> "post".equals(v.getTargetType()) && v.getTargetId() != null)
                .map(CommentVO::getTargetId).distinct().toList();
        if (postIds.isEmpty()) {
            return;
        }
        java.util.Map<Long, String> titleMap = new java.util.HashMap<Long, String>();
        this.postMapper.selectBatchIds(postIds).forEach(p -> titleMap.put(p.getId(), p.getTitle()));
        records.forEach(v -> v.setPostTitle(titleMap.get(v.getTargetId())));
    }

    /**
     * 删除评论（逻辑删除）；非管理员只能删除自己的评论，否则抛 403 无权限异常
     *
     * @param id     评论 ID
     * @param userId 当前用户 ID
     * @param admin  是否管理员身份
     */
    public void delete(Long id, Long userId, boolean admin) {
        Comment comment = (Comment)this.baseMapper.selectById(id);
        if (comment == null) {
            throw new RuntimeException("Comment not found");
        }
        if (!admin && comment.getAuthorId() != null && !comment.getAuthorId().equals(userId)) {
            throw new com.web3.common.core.UnauthorizedException(403, "\u65e0\u6743\u5220\u9664\u8be5\u8bc4\u8bba");
        }
        this.baseMapper.deleteById(id);
    }

    /**
     * 实体转视图对象（内部工具方法）
     *
     * @param comment 评论实体
     * @return 评论视图对象
     */
    private CommentVO toVO(Comment comment) {
        CommentVO vo = new CommentVO();
        BeanUtils.copyProperties((Object)comment, (Object)vo);
        return vo;
    }
}
