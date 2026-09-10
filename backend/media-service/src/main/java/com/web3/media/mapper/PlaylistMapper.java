/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.core.mapper.BaseMapper
 *  org.apache.ibatis.annotations.Mapper
 */
package com.web3.media.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.media.entity.Playlist;
import org.apache.ibatis.annotations.Mapper;

/**
 * PlaylistMapper —— 播放列表表数据访问接口
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 职责：操作 playlist 播放列表表，继承 BaseMapper 获得通用 CRUD。
 */
@Mapper
public interface PlaylistMapper
extends BaseMapper<Playlist> {
}
