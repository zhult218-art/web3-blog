package com.web3.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.web3.blog.dto.ArticleCreateDTO;
import com.web3.blog.entity.Article;
import com.web3.blog.entity.ArticleLike;
import com.web3.blog.entity.UserFavorite;
import com.web3.blog.mapper.ArticleLikeMapper;
import com.web3.blog.mapper.ArticleMapper;
import com.web3.blog.mapper.UserFavoriteMapper;
import com.web3.blog.vo.ArticleVO;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * ArticleService —— 文章业务服务
 * <p>
 * 所属模块：blog-service（博客模块）。
 * <p>
 * 职责：实现文章的核心业务逻辑，包括分页查询、详情（浏览量自增）、创建、更新、删除（含权限校验）、
 * 点赞/取消点赞、热门文章查询，以及实体到视图对象（ArticleVO）的转换。
 * <p>
 * 关键注解作用：
 * <ul>
 *   <li>@Service：声明为 Spring 业务组件，由容器管理并注入到控制器</li>
 *   <li>继承 ServiceImpl&lt;ArticleMapper, Article&gt;：获得 MyBatis-Plus 内置的 CRUD 能力</li>
 * </ul>
 */
@Service
public class ArticleService
extends ServiceImpl<ArticleMapper, Article> {
    /** 文章点赞记录 Mapper */
    private final ArticleLikeMapper articleLikeMapper;
    /** 用户收藏记录 Mapper */
    private final UserFavoriteMapper userFavoriteMapper;

    public ArticleService(ArticleLikeMapper articleLikeMapper, UserFavoriteMapper userFavoriteMapper) {
        this.articleLikeMapper = articleLikeMapper;
        this.userFavoriteMapper = userFavoriteMapper;
    }

    /**
     * 分页查询文章列表，支持标题关键字、分类与标签筛选
     *
     * @param page     页码（从 1 开始）
     * @param size     每页条数
     * @param keyword  标题关键字，可为空
     * @param category 分类，可为空
     * @param tag      标签，可为空
     * @return 文章视图对象的分页结果
     */
    public Page<ArticleVO> page(int page, int size, String keyword, String category, String tag) {
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>();
        if (StringUtils.hasText((String)keyword)) {
            wrapper.like(Article::getTitle, (Object)keyword);
        }
        if (StringUtils.hasText((String)category)) {
            wrapper.eq(Article::getCategory, (Object)category);
        }
        if (StringUtils.hasText((String)tag)) {
            wrapper.like(Article::getTags, (Object)tag);
        }
        wrapper.orderByDesc(Article::getIsTop);
        wrapper.orderByDesc(Article::getCreatedAt);
        Page<Article> articlePage = new Page<Article>((long)page, (long)size);
        Page<Article> result = this.page(articlePage, wrapper);
        Page<ArticleVO> voPage = new Page<ArticleVO>((long)page, (long)size);
        voPage.setTotal(result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return voPage;
    }

    /**
     * 相关推荐：同分类优先，其次同标签，最后按最新；排除自身，取前 limit 条
     *
     * @param id    当前文章 ID
     * @param limit 返回条数
     * @return 文章视图对象列表
     */
    public List<ArticleVO> related(Long id, int limit) {
        Article current = (Article)this.getById(id);
        if (current == null) {
            return java.util.Collections.emptyList();
        }
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>();
        wrapper.eq(Article::getStatus, (Object)"PUBLISHED").ne(Article::getId, (Object)id);
        if (StringUtils.hasText((String)current.getCategory())) {
            wrapper.eq(Article::getCategory, (Object)current.getCategory());
        }
        wrapper.orderByDesc(Article::getCreatedAt);
        wrapper.last("LIMIT " + limit);
        List<Article> articles = this.list(wrapper);
        if (articles.size() < limit) {
            LambdaQueryWrapper<Article> fallback = new LambdaQueryWrapper<Article>();
            fallback.eq(Article::getStatus, (Object)"PUBLISHED").ne(Article::getId, (Object)id);
            if (StringUtils.hasText((String)current.getCategory())) {
                fallback.ne(Article::getCategory, (Object)current.getCategory());
            }
            fallback.orderByDesc(Article::getCreatedAt);
            fallback.last("LIMIT " + (limit - articles.size()));
            articles.addAll(this.list(fallback));
        }
        return articles.stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * 随机一篇已发布文章
     *
     * @return 文章视图对象；无文章时返回 null
     */
    public ArticleVO random() {
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>();
        wrapper.eq(Article::getStatus, (Object)"PUBLISHED");
        wrapper.last("ORDER BY RAND() LIMIT 1");
        Article article = this.getOne(wrapper, false);
        return article == null ? null : this.toVO(article);
    }

    /**
     * 归档：按年月分组返回文章数
     *
     * @return 形如 [{month:'2026-08', count:4}] 的列表，按月份倒序
     */
    public List<java.util.Map<String, Object>> archives() {
        List<java.util.Map<String, Object>> result = this.baseMapper.selectArchives();
        return result;
    }

    /**
     * 分类列表：分类名 + 文章数，按文章数倒序
     *
     * @return 形如 [{name:'计算机网络', count:12}] 的列表
     */
    public List<java.util.Map<String, Object>> categories() {
        return this.baseMapper.selectCategories();
    }

    /**
     * 标签列表：标签名 + 文章数，按文章数倒序
     *
     * @return 形如 [{name:'Linux', count:23}] 的列表
     */
    public List<java.util.Map<String, Object>> tags() {
        List<Article> all = this.list(new LambdaQueryWrapper<Article>().eq(Article::getStatus, (Object)"PUBLISHED").select(Article::getTags));
        java.util.Map<String, Integer> counter = new java.util.LinkedHashMap<String, Integer>();
        for (Article a : all) {
            if (!StringUtils.hasText(a.getTags())) continue;
            String[] parts = a.getTags().split(",");
            for (String p : parts) {
                String tag = p.trim();
                if (tag.isEmpty()) continue;
                counter.put(tag, counter.getOrDefault(tag, 0) + 1);
            }
        }
        List<java.util.Map<String, Object>> list = new java.util.ArrayList<java.util.Map<String, Object>>();
        counter.entrySet().stream().sorted((a, b) -> b.getValue().compareTo(a.getValue())).forEach(e -> {
            java.util.Map<String, Object> item = new java.util.HashMap<String, Object>();
            item.put("name", e.getKey());
            item.put("count", e.getValue());
            list.add(item);
        });
        return list;
    }

    /**
     * 站点资讯统计：文章总数 / 全站字数 / 最后更新时间
     *
     * @return 形如 {totalArticles:145, totalWords:331200, lastUpdated:'2026-08-13T...'}
     */
    public java.util.Map<String, Object> stats() {
        java.util.Map<String, Object> map = this.baseMapper.selectStats();
        return map;
    }

    /**
     * 最近更新文章
     *
     * @param limit 返回条数
     * @return 文章视图对象列表
     */
    public List<ArticleVO> recent(int limit) {
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>();
        wrapper.eq(Article::getStatus, (Object)"PUBLISHED");
        wrapper.orderByDesc(Article::getUpdatedAt);
        wrapper.last("LIMIT " + limit);
        List<Article> articles = this.list(wrapper);
        return articles.stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * 获取文章详情，同时将浏览量 +1 并落库；附带上一篇/下一篇 id 与标题
     *
     * @param id 文章 ID
     * @return 包含 article/prevArticle/nextArticle 的映射；文章不存在时返回 null
     */
    public java.util.Map<String, Object> getDetailWithNav(Long id) {
        Article article = (Article)this.getById(id);
        if (article == null) {
            return null;
        }
        article.setViewCount((article.getViewCount() == null ? 0L : article.getViewCount()) + 1L);
        this.updateById(article);
        java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
        result.put("article", this.toVO(article));
        LambdaQueryWrapper<Article> prevWrapper = new LambdaQueryWrapper<Article>();
        prevWrapper.eq(Article::getStatus, (Object)"PUBLISHED").lt(Article::getCreatedAt, (Object)article.getCreatedAt()).orderByDesc(Article::getCreatedAt).last("LIMIT 1");
        Article prev = this.getOne(prevWrapper, false);
        if (prev != null) {
            java.util.Map<String, Object> prevMap = new java.util.HashMap<String, Object>();
            prevMap.put("id", prev.getId());
            prevMap.put("title", prev.getTitle());
            result.put("prevArticle", prevMap);
        }
        LambdaQueryWrapper<Article> nextWrapper = new LambdaQueryWrapper<Article>();
        nextWrapper.eq(Article::getStatus, (Object)"PUBLISHED").gt(Article::getCreatedAt, (Object)article.getCreatedAt()).orderByAsc(Article::getCreatedAt).last("LIMIT 1");
        Article next = this.getOne(nextWrapper, false);
        if (next != null) {
            java.util.Map<String, Object> nextMap = new java.util.HashMap<String, Object>();
            nextMap.put("id", next.getId());
            nextMap.put("title", next.getTitle());
            result.put("nextArticle", nextMap);
        }
        return result;
    }

    /**
     * 创建文章，初始化浏览量/点赞数为 0，状态为空时默认 PUBLISHED
     *
     * @param dto      文章创建参数
     * @param authorId 作者用户 ID（取自登录令牌）
     * @return 创建成功后的文章视图对象
     */
    public ArticleVO create(ArticleCreateDTO dto, Long authorId) {
        Article article = new Article();
        BeanUtils.copyProperties((Object)dto, (Object)article);
        article.setAuthorId(authorId);
        article.setAuthorName(dto.getAuthorName());
        article.setViewCount(0L);
        article.setLikeCount(0);
        article.setIsTop(dto.getIsTop() == null ? 0 : dto.getIsTop());
        if (!StringUtils.hasText((String)article.getStatus())) {
            article.setStatus("PUBLISHED");
        }
        this.save(article);
        return this.toVO(article);
    }

    /**
     * 点赞/取消点赞（切换式操作）
     * 未点赞过则新增点赞记录并给文章点赞数 +1；已点赞则删除记录并 -1
     *
     * @param id     文章 ID
     * @param userId 当前用户 ID
     */
    @Transactional
    public void like(Long id, Long userId) {
        Article article = (Article)this.getById(id);
        if (article == null) {
            throw new RuntimeException("Article not found");
        }
        LambdaQueryWrapper<ArticleLike> wrapper = new LambdaQueryWrapper<ArticleLike>();
        wrapper.eq(ArticleLike::getArticleId, (Object)id).eq(ArticleLike::getUserId, (Object)userId);
        ArticleLike record = (ArticleLike)this.articleLikeMapper.selectOne(wrapper);
        if (record == null) {
            ArticleLike like = new ArticleLike();
            like.setArticleId(id);
            like.setUserId(userId);
            this.articleLikeMapper.insert(like);
            this.baseMapper.incrLikeCount(id);
        } else {
            this.articleLikeMapper.deleteById(record.getId());
            this.baseMapper.decrLikeCount(id);
        }
    }

    /**
     * 更新文章；非管理员只能修改自己名下的文章，否则抛 403 无权限异常
     *
     * @param id    文章 ID
     * @param dto   更新参数
     * @param userId 当前用户 ID
     * @param admin  是否管理员身份
     * @return 更新后的文章视图对象；文章不存在时返回 null
     */
    public ArticleVO update(Long id, ArticleCreateDTO dto, Long userId, boolean admin) {
        Article article = (Article)this.getById(id);
        if (article == null) {
            return null;
        }
        if (!admin && article.getAuthorId() != null && !article.getAuthorId().equals(userId)) {
            throw new com.web3.common.core.UnauthorizedException(403, "\u65e0\u6743\u4fee\u6539\u8be5\u6587\u7ae0");
        }
        BeanUtils.copyProperties((Object)dto, (Object)article);
        article.setId(id);
        this.updateById(article);
        return this.toVO(article);
    }

    /**
     * 删除文章（逻辑删除）；非管理员只能删除自己的文章
     *
     * @param id     文章 ID
     * @param userId 当前用户 ID
     * @param admin  是否管理员身份
     */
    public void delete(Long id, Long userId, boolean admin) {
        Article article = (Article)this.getById(id);
        if (article == null) {
            return;
        }
        if (!admin && article.getAuthorId() != null && !article.getAuthorId().equals(userId)) {
            throw new com.web3.common.core.UnauthorizedException(403, "\u65e0\u6743\u5220\u9664\u8be5\u6587\u7ae0");
        }
        this.removeById(id);
    }

    /**
     * 查询热门文章：仅已发布的文章按浏览量倒序取前 limit 条
     *
     * @param limit 返回条数
     * @return 文章视图对象列表
     */
    public List<ArticleVO> hotArticles(int limit) {
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>();
        wrapper.eq(Article::getStatus, (Object)"PUBLISHED");
        wrapper.orderByDesc(Article::getViewCount);
        wrapper.last("LIMIT " + limit);
        List<Article> articles = this.list(wrapper);
        return articles.stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * 收藏/取消收藏文章（切换式操作）
     */
    @Transactional
    public void favorite(Long articleId, Long userId) {
        Article article = (Article)this.getById(articleId);
        if (article == null) {
            throw new RuntimeException("Article not found");
        }
        LambdaQueryWrapper<UserFavorite> wrapper = new LambdaQueryWrapper<UserFavorite>();
        wrapper.eq(UserFavorite::getArticleId, (Object)articleId).eq(UserFavorite::getUserId, (Object)userId);
        UserFavorite record = (UserFavorite)this.userFavoriteMapper.selectOne(wrapper);
        if (record == null) {
            UserFavorite fav = new UserFavorite();
            fav.setArticleId(articleId);
            fav.setUserId(userId);
            this.userFavoriteMapper.insert(fav);
        } else {
            this.userFavoriteMapper.deleteById(record.getId());
        }
    }

    /**
     * 检查用户是否已收藏文章
     */
    public boolean isFavorited(Long articleId, Long userId) {
        LambdaQueryWrapper<UserFavorite> wrapper = new LambdaQueryWrapper<UserFavorite>();
        wrapper.eq(UserFavorite::getArticleId, (Object)articleId).eq(UserFavorite::getUserId, (Object)userId);
        return this.userFavoriteMapper.selectCount(wrapper) > 0;
    }

    /**
     * 检查用户是否已点赞文章
     */
    public boolean isLiked(Long articleId, Long userId) {
        LambdaQueryWrapper<ArticleLike> wrapper = new LambdaQueryWrapper<ArticleLike>();
        wrapper.eq(ArticleLike::getArticleId, (Object)articleId).eq(ArticleLike::getUserId, (Object)userId);
        return this.articleLikeMapper.selectCount(wrapper) > 0;
    }

    /**
     * 获取用户收藏的文章列表
     */
    public List<ArticleVO> getUserFavorites(Long userId) {
        List<Article> articles = this.baseMapper.selectFavoritedByUser(userId);
        return articles.stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * 获取用户点赞的文章列表
     */
    public List<ArticleVO> getUserLikes(Long userId) {
        List<Article> articles = this.baseMapper.selectLikedByUser(userId);
        return articles.stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * 实体转视图对象（内部工具方法）
     *
     * @param article 文章实体
     * @return 文章视图对象
     */
    private ArticleVO toVO(Article article) {
        ArticleVO vo = new ArticleVO();
        BeanUtils.copyProperties((Object)article, (Object)vo);
        return vo;
    }
}
