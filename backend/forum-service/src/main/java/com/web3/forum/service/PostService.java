package com.web3.forum.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.web3.forum.dto.PostCreateDTO;
import com.web3.forum.dto.PostVO;
import com.web3.forum.entity.Comment;
import com.web3.forum.entity.LikeRecord;
import com.web3.forum.entity.Post;
import com.web3.forum.mapper.CommentMapper;
import com.web3.forum.mapper.LikeRecordMapper;
import com.web3.forum.mapper.PostMapper;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PostService —— 帖子业务服务
 * <p>
 * 所属模块：forum-service（论坛模块）。
 * <p>
 * 职责：实现论坛帖子的核心业务逻辑，包括分页查询（关键字/排序）、详情（浏览量自增）、创建、点赞/取消点赞、
 * 删除与更新（含权限校验）、论坛统计数据聚合，以及实体到视图对象（PostVO）的转换。
 * <p>
 * 关键注解作用：
 * <ul>
 *   <li>@Service：声明为 Spring 业务组件</li>
 *   <li>继承 ServiceImpl&lt;PostMapper, Post&gt;：获得 MyBatis-Plus 内置 CRUD 能力</li>
 * </ul>
 */
@Service
public class PostService
extends ServiceImpl<PostMapper, Post> {
    /** 点赞记录 Mapper（点赞/取消点赞） */
    private final LikeRecordMapper likeRecordMapper;
    /** 评论 Mapper（统计数据用） */
    private final CommentMapper commentMapper;

    public PostService(LikeRecordMapper likeRecordMapper, CommentMapper commentMapper) {
        this.likeRecordMapper = likeRecordMapper;
        this.commentMapper = commentMapper;
    }

    /**
     * 分页查询帖子列表
     * 按置顶优先排序；sort=hot 时再按点赞数倒序，否则按创建时间倒序
     *
     * @param page    页码（从 1 开始）
     * @param size    每页条数
     * @param keyword 标题/正文关键字，可为空
     * @param sort    排序方式：latest=最新（默认），hot=最热
     * @return 帖子视图对象的分页结果
     */
    public Page<PostVO> page(int page, int size, String keyword, String sort) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Post::getTitle, (Object)keyword).or().like(Post::getContent, (Object)keyword));
        }
        if ("hot".equals(sort)) {
            wrapper.orderByDesc(Post::getIsPinned).orderByDesc(Post::getLikeCount).orderByDesc(Post::getCreatedAt);
        } else {
            wrapper.orderByDesc(Post::getIsPinned).orderByDesc(Post::getCreatedAt);
        }
        Page<Post> postPage = new Page<Post>((long)page, (long)size);
        Page<Post> result = this.baseMapper.selectPage(postPage, wrapper);
        Page<PostVO> voPage = new Page<PostVO>((long)page, (long)size, result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    /**
     * 获取帖子详情，同时将浏览量 +1 并落库
     *
     * @param id 帖子 ID
     * @return 帖子视图对象；帖子不存在时返回 null
     */
    public PostVO getDetail(Long id) {
        Post post = (Post)this.baseMapper.selectById(id);
        if (post == null) {
            return null;
        }
        this.baseMapper.incrViewCount(id);
        post.setViewCount((post.getViewCount() == null ? 0L : post.getViewCount()) + 1L);
        return this.toVO(post);
    }

    /**
     * 创建帖子，初始化点赞数/回复数/浏览量为 0、非置顶、状态为 published
     *
     * @param dto      帖子创建参数
     * @param authorId 作者用户 ID（取自登录令牌）
     * @return 创建成功后的帖子视图对象
     */
    public PostVO create(PostCreateDTO dto, Long authorId) {
        Post post = new Post();
        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        post.setCategory(dto.getCategory());
        post.setAuthorId(authorId);
        post.setAuthorName(dto.getAuthorName());
        post.setMediaUrl(dto.getMediaUrl());
        post.setMediaType(dto.getMediaType());
        post.setLikeCount(0);
        post.setReplyCount(0);
        post.setViewCount(0L);
        post.setIsPinned(0);
        post.setStatus("published");
        this.baseMapper.insert(post);
        return this.toVO(post);
    }

    /**
     * 帖子点赞/取消点赞（切换式操作）
     * 未点赞过则新增 like_record 记录并给帖子点赞数 +1；已点赞则删除记录并 -1
     *
     * @param postId 帖子 ID
     * @param userId 当前用户 ID
     */
    @Transactional
    public void like(Long postId, Long userId) {
        LambdaQueryWrapper<LikeRecord> wrapper = new LambdaQueryWrapper<LikeRecord>();
        wrapper.eq(LikeRecord::getTargetId, (Object)postId).eq(LikeRecord::getTargetType, (Object)"post").eq(LikeRecord::getUserId, (Object)userId);
        LikeRecord record = this.likeRecordMapper.selectOne(wrapper);
        Post post = (Post)this.baseMapper.selectById(postId);
        if (post == null) {
            throw new RuntimeException("Post not found");
        }
        if (record == null) {
            LikeRecord newRecord = new LikeRecord();
            newRecord.setTargetId(postId);
            newRecord.setTargetType("post");
            newRecord.setUserId(userId);
            this.likeRecordMapper.insert(newRecord);
            this.baseMapper.incrLikeCount(postId);
        } else {
            this.likeRecordMapper.deleteById(record.getId());
            this.baseMapper.decrLikeCount(postId);
        }
    }

    /**
     * 删除帖子（逻辑删除）；非管理员只能删除自己的帖子，否则抛 403 无权限异常
     *
     * @param id     帖子 ID
     * @param userId 当前用户 ID
     * @param admin  是否管理员身份
     */
    public void delete(Long id, Long userId, boolean admin) {
        Post post = (Post)this.baseMapper.selectById(id);
        if (post == null) {
            throw new RuntimeException("Post not found");
        }
        if (!admin && post.getAuthorId() != null && !post.getAuthorId().equals(userId)) {
            throw new com.web3.common.core.UnauthorizedException(403, "\u65e0\u6743\u5220\u9664\u8be5\u5e16\u5b50");
        }
        this.baseMapper.deleteById(id);
    }

    /**
     * 论坛统计数据聚合
     * 返回帖子总数、评论总数、今日发帖数、发帖最多的作者 TOP6、最热门的分类 TOP10
     *
     * @return 包含各项统计数据的 Map
     */
    public Map<String, Object> stats() {
        Map<String, Object> stats = new LinkedHashMap<String, Object>();
        stats.put("totalPosts", this.baseMapper.selectCount(new LambdaQueryWrapper<Post>()));
        stats.put("totalComments", this.commentMapper.selectCount(new LambdaQueryWrapper<Comment>()));
        stats.put("todayPosts", this.baseMapper.selectCount(new LambdaQueryWrapper<Post>().ge(Post::getCreatedAt, LocalDate.now().atStartOfDay())));
        stats.put("topAuthors", this.baseMapper.selectMaps(new QueryWrapper<Post>()
            .select("author_name AS name", "COUNT(*) AS cnt")
            .isNotNull("author_name")
            .ne("author_name", "")
            .groupBy("author_name")
            .orderByDesc("cnt")
            .last("LIMIT 6")));
        stats.put("topTags", this.baseMapper.selectMaps(new QueryWrapper<Post>()
            .select("category AS tag", "COUNT(*) AS cnt")
            .isNotNull("category")
            .ne("category", "")
            .groupBy("category")
            .orderByDesc("cnt")
            .last("LIMIT 10")));
        return stats;
    }

    /**
     * 更新帖子（仅标题、内容、分类支持局部更新）；非管理员只能编辑自己的帖子
     *
     * @param id     帖子 ID
     * @param dto    更新参数（非空字段才会被更新）
     * @param userId 当前用户 ID
     * @param admin  是否管理员身份
     * @return 更新后的帖子视图对象
     */
    public PostVO update(Long id, PostCreateDTO dto, Long userId, boolean admin) {
        Post post = (Post)this.baseMapper.selectById(id);
        if (post == null) {
            throw new RuntimeException("Post not found");
        }
        if (!admin && post.getAuthorId() != null && !post.getAuthorId().equals(userId)) {
            throw new com.web3.common.core.UnauthorizedException(403, "\u65e0\u6743\u7f16\u8f91\u8be5\u5e16\u5b50");
        }
        if (dto.getTitle() != null) {
            post.setTitle(dto.getTitle());
        }
        if (dto.getContent() != null) {
            post.setContent(dto.getContent());
        }
        if (dto.getCategory() != null) {
            post.setCategory(dto.getCategory());
        }
        this.baseMapper.updateById(post);
        return this.toVO(post);
    }

    /**
     * 实体转视图对象（内部工具方法）
     *
     * @param post 帖子实体
     * @return 帖子视图对象
     */
    private PostVO toVO(Post post) {
        PostVO vo = new PostVO();
        BeanUtils.copyProperties((Object)post, (Object)vo);
        return vo;
    }
}
