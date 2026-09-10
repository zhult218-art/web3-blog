/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.core.mapper.BaseMapper
 *  org.apache.ibatis.annotations.Mapper
 */
package com.web3.media.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.media.entity.Video;
import org.apache.ibatis.annotations.Mapper;

/**
 * VideoMapper —— 视频表数据访问接口
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 职责：操作 video 视频表，继承 BaseMapper 获得通用 CRUD。
 */
@Mapper
public interface VideoMapper
extends BaseMapper<Video> {
}
