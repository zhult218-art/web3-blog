/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.core.mapper.BaseMapper
 *  org.apache.ibatis.annotations.Mapper
 */
package com.web3.forum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.forum.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * PostMapper —— 帖子表数据访问接口
 * <p>
 * 所属模块：forum-service（论坛模块）。
 * <p>
 * 职责：操作 post 帖子表。继承 MyBatis-Plus BaseMapper 获得通用 CRUD，
 * 并额外提供点赞数、浏览量增减的自定义 SQL 更新方法。
 * <p>
 * 关键注解作用：
 * <ul>
 *   <li>@Mapper：声明为 MyBatis Mapper 接口，由框架扫描并生成实现</li>
 *   <li>@Update：自定义 SQL 更新语句（点赞数/浏览量增减，忽略逻辑删除记录）</li>
 * </ul>
 */
@Mapper
public interface PostMapper
extends BaseMapper<Post> {
    /** 帖子点赞数 +1（仅统计未删除记录） */
    @Update("UPDATE post SET like_count = like_count + 1 WHERE id = #{id} AND deleted = 0")
    int incrLikeCount(@Param("id") Long id);

    /** 帖子点赞数 -1（下限为 0，仅统计未删除记录） */
    @Update("UPDATE post SET like_count = GREATEST(like_count - 1, 0) WHERE id = #{id} AND deleted = 0")
    int decrLikeCount(@Param("id") Long id);

    /** 帖子浏览量 +1（仅统计未删除记录） */
    @Update("UPDATE post SET view_count = IFNULL(view_count, 0) + 1 WHERE id = #{id} AND deleted = 0")
    int incrViewCount(@Param("id") Long id);
}
