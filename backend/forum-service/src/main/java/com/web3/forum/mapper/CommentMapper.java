/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.core.mapper.BaseMapper
 *  org.apache.ibatis.annotations.Mapper
 */
package com.web3.forum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.forum.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

/**
 * CommentMapper —— 评论表数据访问接口
 * <p>
 * 所属模块：forum-service（论坛模块）。
 * <p>
 * 职责：操作 comment 评论表，继承 BaseMapper 获得通用 CRUD，
 * 支持按目标对象分页查询评论以及评论的新增/删除。
 */
@Mapper
public interface CommentMapper
extends BaseMapper<Comment> {
}
