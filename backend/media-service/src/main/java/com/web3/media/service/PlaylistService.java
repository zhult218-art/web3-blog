/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
 *  org.springframework.stereotype.Service
 */
package com.web3.media.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.web3.media.entity.Playlist;
import com.web3.media.mapper.PlaylistMapper;
import org.springframework.stereotype.Service;

/**
 * PlaylistService —— 播放列表业务服务
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 职责：播放列表的通用 CRUD 能力，直接继承 ServiceImpl 使用 MyBatis-Plus 内置方法，
 * 权限校验（归属用户）在 PlaylistController 层完成。
 * <p>
 * 关键注解作用：
 * <ul>
 *   <li>@Service：声明为 Spring 业务组件</li>
 *   <li>继承 ServiceImpl&lt;PlaylistMapper, Playlist&gt;：获得 MyBatis-Plus 内置 CRUD 能力</li>
 * </ul>
 */
@Service
public class PlaylistService
extends ServiceImpl<PlaylistMapper, Playlist> {
}
