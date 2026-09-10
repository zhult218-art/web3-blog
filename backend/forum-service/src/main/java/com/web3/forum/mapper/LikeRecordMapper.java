/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.core.mapper.BaseMapper
 *  org.apache.ibatis.annotations.Mapper
 */
package com.web3.forum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.forum.entity.LikeRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * LikeRecordMapper —— 点赞记录表数据访问接口
 * <p>
 * 所属模块：forum-service（论坛模块）。
 * <p>
 * 职责：操作 like_record 点赞记录表，继承 BaseMapper 获得通用 CRUD，
 * 用于查询某用户是否已点赞某目标（如帖子）以及点赞记录的新增/删除。
 */
@Mapper
public interface LikeRecordMapper
extends BaseMapper<LikeRecord> {
}
