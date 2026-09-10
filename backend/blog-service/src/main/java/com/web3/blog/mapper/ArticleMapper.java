/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.core.mapper.BaseMapper
 *  org.apache.ibatis.annotations.Mapper
 */
package com.web3.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.blog.entity.Article;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * ArticleMapper —— 文章表数据访问接口
 * <p>
 * 所属模块：blog-service（博客模块）。
 * <p>
 * 职责：操作 article 文章表。继承 MyBatis-Plus BaseMapper 获得通用 CRUD，并额外提供点赞数增减、归档/分类/统计等 SQL 方法。
 * <p>
 * 关键注解作用：
 * <ul>
 *   <li>@Mapper：声明为 MyBatis Mapper 接口，由框架扫描并生成实现</li>
 *   <li>@Update：自定义 SQL 更新语句（点赞数 +1/-1，忽略逻辑删除记录）</li>
 *   <li>@Select：自定义聚合查询（归档、分类、站点统计）</li>
 * </ul>
 */
@Mapper
public interface ArticleMapper
extends BaseMapper<Article> {
    /** 文章点赞数 +1（仅统计未删除记录） */
    @Update("UPDATE article SET like_count = IFNULL(like_count, 0) + 1 WHERE id = #{id} AND deleted = 0")
    int incrLikeCount(@Param("id") Long id);

    /** 文章点赞数 -1（下限为 0，仅统计未删除记录） */
    @Update("UPDATE article SET like_count = GREATEST(IFNULL(like_count, 0) - 1, 0) WHERE id = #{id} AND deleted = 0")
    int decrLikeCount(@Param("id") Long id);

    /** 归档：按年月分组统计已发布文章数 */
    @Select("SELECT DATE_FORMAT(created_at, '%Y-%m') AS month, COUNT(*) AS count FROM article WHERE status = 'PUBLISHED' AND deleted = 0 GROUP BY DATE_FORMAT(created_at, '%Y-%m') ORDER BY month DESC")
    List<Map<String, Object>> selectArchives();

    /** 分类：分类名 + 文章数，按文章数倒序 */
    @Select("SELECT category AS name, COUNT(*) AS count FROM article WHERE status = 'PUBLISHED' AND deleted = 0 AND category IS NOT NULL AND category <> '' GROUP BY category ORDER BY count DESC")
    List<Map<String, Object>> selectCategories();

    /** 站点资讯：文章总数 / 全站字数 / 最后更新时间 */
    @Select("SELECT COUNT(*) AS totalArticles, IFNULL(SUM(CHAR_LENGTH(REPLACE(content, ' ', ''))), 0) AS totalWords, MAX(updated_at) AS lastUpdated FROM article WHERE status = 'PUBLISHED' AND deleted = 0")
    Map<String, Object> selectStats();

    /** 查询用户点赞的文章列表 */
    @Select("SELECT a.* FROM article a INNER JOIN article_like al ON a.id = al.article_id WHERE al.user_id = #{userId} AND a.deleted = 0 AND a.status = 'PUBLISHED' ORDER BY al.created_at DESC")
    List<Article> selectLikedByUser(@Param("userId") Long userId);

    /** 查询用户收藏的文章列表 */
    @Select("SELECT a.* FROM article a INNER JOIN user_favorite uf ON a.id = uf.article_id WHERE uf.user_id = #{userId} AND a.deleted = 0 AND a.status = 'PUBLISHED' ORDER BY uf.created_at DESC")
    List<Article> selectFavoritedByUser(@Param("userId") Long userId);
}
