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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private static final Logger log = LoggerFactory.getLogger(PostService.class);
    /** 点赞记录 Mapper（点赞/取消点赞） */
    private final LikeRecordMapper likeRecordMapper;
    /** 评论 Mapper（统计数据用） */
    private final CommentMapper commentMapper;
    /** JDBC 模板（跨库补全作者昵称：web3_forum 与 web3_user 同 MySQL 实例） */
    private final JdbcTemplate jdbcTemplate;

    public PostService(LikeRecordMapper likeRecordMapper, CommentMapper commentMapper, JdbcTemplate jdbcTemplate) {
        this.likeRecordMapper = likeRecordMapper;
        this.commentMapper = commentMapper;
        this.jdbcTemplate = jdbcTemplate;
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
        // 公开列表仅展示审核通过的帖子（pending/rejected 对普通用户不可见）
        wrapper.eq(Post::getStatus, (Object) "published");
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
        java.util.List<PostVO> vos = result.getRecords().stream().map(this::toVO).toList();
        fillAuthorNames(vos);
        voPage.setRecords(vos);
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
        if (post == null || !"published".equals(post.getStatus())) {
            return null;
        }
        this.baseMapper.incrViewCount(id);
        post.setViewCount((post.getViewCount() == null ? 0L : post.getViewCount()) + 1L);
        PostVO vo = this.toVO(post);
        fillAuthorNames(java.util.List.of(vo));
        return vo;
    }

    /**
     * 创建帖子，初始化点赞数/回复数/浏览量为 0、非置顶。
     * 管理员发帖直接 published；普通用户发帖进入 pending 待审核。
     * 作者名取自客户端提交，缺失时回退 JWT 中的昵称，保证列表不显示「匿名用户」。
     *
     * @param dto      帖子创建参数
     * @param authorId 作者用户 ID（取自登录令牌）
     * @param nickname 作者昵称（取自登录令牌，authorName 缺失时的回退值）
     * @param admin    作者是否为管理员
     * @return 创建成功后的帖子视图对象
     */
    public PostVO create(PostCreateDTO dto, Long authorId, String nickname, boolean admin) {
        Post post = new Post();
        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        post.setCategory(dto.getCategory());
        post.setAuthorId(authorId);
        String authorName = dto.getAuthorName();
        if (authorName == null || authorName.isBlank()) {
            authorName = nickname;
        }
        post.setAuthorName(authorName);
        post.setMediaUrl(dto.getMediaUrl());
        post.setMediaType(dto.getMediaType());
        post.setLikeCount(0);
        post.setReplyCount(0);
        post.setViewCount(0L);
        post.setIsPinned(0);
        post.setStatus(admin ? "published" : "pending");
        this.baseMapper.insert(post);
        invalidateStatsCache();
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
            try {
                this.likeRecordMapper.insert(newRecord);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                // 并发双击：唯一键(target_type,target_id,user_id)保证仅一条记录，后到请求视为已点赞，不再重复计数
                return;
            }
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
        invalidateStatsCache();
    }

    /** 统计数据内存缓存（60s TTL，避免每次加载社区页面都跑 5 次 DB 聚合查询） */
    private volatile Map<String, Object> statsCache;
    private volatile long statsCacheAt = 0;
    private static final long STATS_CACHE_TTL_MS = 60_000;

    /**
     * 论坛统计数据聚合
     * 返回帖子总数、评论总数、今日发帖数、发帖最多的作者 TOP6、最热门的分类 TOP10。
     * 结果缓存 60 秒，减轻社区页面首屏的 DB 压力。
     *
     * @return 包含各项统计数据的 Map
     */
    public Map<String, Object> stats() {
        long now = System.currentTimeMillis();
        if (statsCache != null && (now - statsCacheAt) < STATS_CACHE_TTL_MS) {
            return statsCache;
        }
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
        statsCache = stats;
        statsCacheAt = now;
        return stats;
    }

    /** 帖子新增/删除/审核后调用，使下次 stats() 重新查库 */
    public void invalidateStatsCache() {
        statsCache = null;
        statsCacheAt = 0;
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
        // 分类变更会影响 stats() 的 topTags 聚合，更新后同步失效缓存
        invalidateStatsCache();
        return this.toVO(post);
    }

    /**
     * 管理端分页查询：可按审核状态筛选，默认按创建时间倒序，包含 pending/rejected 全部状态
     *
     * @param page    页码（从 1 开始）
     * @param size    每页条数
     * @param status  审核状态：pending/published/rejected，空或 all 表示全部
     * @param keyword 标题/正文关键字，可为空
     * @return 帖子视图对象分页结果
     */
    public Page<PostVO> adminPage(int page, int size, String status, String keyword) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>();
        if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
            wrapper.eq(Post::getStatus, (Object) status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Post::getTitle, (Object) keyword).or().like(Post::getContent, (Object) keyword));
        }
        wrapper.orderByDesc(Post::getCreatedAt);
        Page<Post> postPage = new Page<Post>((long) page, (long) size);
        Page<Post> result = this.baseMapper.selectPage(postPage, wrapper);
        Page<PostVO> voPage = new Page<PostVO>((long) page, (long) size, result.getTotal());
        java.util.List<PostVO> vos = result.getRecords().stream().map(this::toVO).toList();
        fillAuthorNames(vos);
        voPage.setRecords(vos);
        return voPage;
    }

    /**
     * 管理端审核：更新帖子状态（published=通过 / rejected=驳回）
     *
     * @param id     帖子 ID
     * @param status 目标状态
     */
    public void moderate(Long id, String status) {
        Post post = (Post) this.baseMapper.selectById(id);
        if (post == null) {
            throw new RuntimeException("Post not found");
        }
        post.setStatus(status);
        this.baseMapper.updateById(post);
    }

    /**
     * 管理端删除帖子（逻辑删除），不受作者限制
     *
     * @param id 帖子 ID
     */
    public void adminDelete(Long id) {
        Post post = (Post) this.baseMapper.selectById(id);
        if (post == null) {
            throw new RuntimeException("Post not found");
        }
        this.baseMapper.deleteById(id);
    }

    /**
     * 管理端批量审核：逐条更新帖子状态，跳过不存在的记录
     *
     * @param ids    帖子 ID 列表
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
            Post post = (Post) this.baseMapper.selectById(id);
            if (post == null || status.equals(post.getStatus())) {
                continue;
            }
            post.setStatus(status);
            this.baseMapper.updateById(post);
            count++;
        }
        return count;
    }

    /**
     * 管理端批量删除帖子（逻辑删除）
     *
     * @param ids 帖子 ID 列表
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
                this.baseMapper.deleteById(id);
                count++;
            }
        }
        return count;
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

    /**
     * 批量补全作者名：历史帖子/未携带 author_name 的记录按 author_id 跨库查询
     * web3_user.user 表（web3_forum 与 web3_user 同 MySQL 实例），用昵称或用户名回填。
     * 查询失败静默跳过（不影响列表主流程），已携带作者名的记录不覆盖。
     *
     * @param vos 帖子视图对象列表（原地回填 authorName）
     */
    private void fillAuthorNames(java.util.List<PostVO> vos) {
        if (vos == null || vos.isEmpty()) {
            return;
        }
        java.util.Set<Long> missing = new java.util.HashSet<>();
        for (PostVO vo : vos) {
            if ((vo.getAuthorName() == null || vo.getAuthorName().isBlank()) && vo.getAuthorId() != null) {
                missing.add(vo.getAuthorId());
            }
        }
        if (missing.isEmpty()) {
            return;
        }
        String idList = missing.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        try {
            Map<Long, String> nameById = new HashMap<>();
            this.jdbcTemplate.query(
                    "SELECT id, nickname, username FROM web3_user.`user` WHERE id IN (" + idList + ")",
                    rs -> {
                        while (rs.next()) {
                            String nickname = rs.getString("nickname");
                            String username = rs.getString("username");
                            String name = (nickname != null && !nickname.isBlank()) ? nickname : username;
                            nameById.put(rs.getLong("id"), name);
                        }
                    });
            for (PostVO vo : vos) {
                if (vo.getAuthorName() == null || vo.getAuthorName().isBlank()) {
                    String name = vo.getAuthorId() != null ? nameById.get(vo.getAuthorId()) : null;
                    if (name != null && !name.isBlank()) {
                        vo.setAuthorName(name);
                    }
                }
            }
        } catch (Exception e) {
            // user 库不可达或权限不足时静默跳过，列表仍正常返回
            log.debug("[post] fill author names from user DB skipped: {}", e.getMessage());
        }
    }
}
