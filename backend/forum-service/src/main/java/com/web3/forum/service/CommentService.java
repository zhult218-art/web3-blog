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
        wrapper.eq(Comment::getTargetId, (Object)targetId).eq(Comment::getTargetType, (Object)targetType).orderByAsc(Comment::getCreatedAt);
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
     * 发表评论；若评论对象为帖子，则同步该帖子的回复数 +1（同一事务）
     *
     * @param dto      评论创建参数
     * @param authorId 评论者用户 ID（取自登录令牌）
     * @return 创建成功后的评论视图对象
     */
    @Transactional
    public CommentVO create(CommentCreateDTO dto, Long authorId) {
        Post post;
        Comment comment = new Comment();
        comment.setTargetId(dto.getTargetId());
        comment.setTargetType(dto.getTargetType());
        comment.setContent(dto.getContent());
        comment.setParentId(dto.getParentId());
        comment.setReplyToName(dto.getReplyToName());
        comment.setAuthorId(authorId);
        comment.setAuthorName(dto.getAuthorName());
        this.baseMapper.insert(comment);
        if ("post".equals(dto.getTargetType()) && dto.getParentId() == null && (post = this.postMapper.selectById(dto.getTargetId())) != null) {
            post.setReplyCount(post.getReplyCount() + 1);
            this.postMapper.updateById(post);
        }
        return this.toVO(comment);
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
