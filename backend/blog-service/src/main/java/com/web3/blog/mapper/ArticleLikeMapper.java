/*
 * ArticleLikeMapper
 */
package com.web3.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.blog.entity.ArticleLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * ArticleLikeMapper —— 文章点赞记录表数据访问接口
 * <p>
 * 所属模块：blog-service（博客模块）。
 * <p>
 * 职责：操作 article_like 点赞记录表，继承 BaseMapper 获得通用 CRUD，
 * 用于查询某用户是否已点赞某文章以及记录的新增/删除。
 */
@Mapper
public interface ArticleLikeMapper
extends BaseMapper<ArticleLike> {
}
