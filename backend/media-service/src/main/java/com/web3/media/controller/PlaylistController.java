/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.web3.common.core.ApiResponse
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package com.web3.media.controller;

import com.web3.common.core.ApiResponse;
import com.web3.common.core.UnauthorizedException;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import com.web3.media.entity.Playlist;
import com.web3.media.service.PlaylistService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/playlist"})
/**
 * PlaylistController —— 播放列表接口控制器
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 职责：管理用户自定义播放列表的对外 REST 接口，包括列表查询，
 * 以及播放列表的增删改（归属登录用户本人，越权抛 403）。
 * <p>
 * 关键注解作用：
 * <ul>
 *   <li>@RestController：声明为 REST 控制器，返回值自动序列化为 JSON</li>
 *   <li>@RequestMapping("/playlist")：对外接口统一前缀为 /playlist</li>
 * </ul>
 */
public class PlaylistController {
    /** 播放列表业务服务 */
    private final PlaylistService playlistService;
    /** JWT 令牌校验工具 */
    private final JwtUtil jwtUtil;
    /** JWT 配置属性 */
    private final JwtProperties jwtProperties;

    public PlaylistController(PlaylistService playlistService, JwtUtil jwtUtil, JwtProperties jwtProperties) {
        this.playlistService = playlistService;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 播放列表查询（前端路由：GET /playlist/list）
     *
     * @return 全部播放列表
     */
    @GetMapping(value={"/list"})
    public ApiResponse<List<Playlist>> list() {
        return ApiResponse.ok(this.playlistService.list());
    }

    /**
     * 创建播放列表（前端路由：POST /playlist，需登录）
     * 播放列表归属由令牌解析出的当前用户
     *
     * @param playlist     播放列表实体（JSON 请求体）
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 创建成功后的播放列表
     */
    @PostMapping
    public ApiResponse<Playlist> create(@RequestBody Playlist playlist, @RequestHeader(value="Authorization") String authorization) {
        Long userId = AuthUtils.requireUserId(authorization, this.jwtUtil, this.jwtProperties);
        playlist.setId(null);
        playlist.setUserId(userId);
        this.playlistService.save(playlist);
        return ApiResponse.ok(playlist);
    }

    /**
     * 更新播放列表（前端路由：PUT /playlist/{id}，需登录）
     * 仅播放列表所有者本人可修改，否则抛 403 无权限错误；不存在时返回 404
     *
     * @param id           播放列表 ID
     * @param playlist     播放列表实体（JSON 请求体）
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 更新后的播放列表
     */
    @PutMapping(value={"/{id}"})
    public ApiResponse<Playlist> update(@PathVariable Long id, @RequestBody Playlist playlist, @RequestHeader(value="Authorization") String authorization) {
        Long userId = AuthUtils.requireUserId(authorization, this.jwtUtil, this.jwtProperties);
        Playlist exist = (Playlist)this.playlistService.getById(id);
        if (exist == null) {
            return ApiResponse.fail((int)404, (String)"Playlist not found");
        }
        if (exist.getUserId() != null && !exist.getUserId().equals(userId)) {
            throw new UnauthorizedException(403, "\u65e0\u6743\u4fee\u6539\u8be5\u64ad\u653e\u5217\u8868");
        }
        playlist.setId(id);
        playlist.setUserId(exist.getUserId());
        this.playlistService.updateById(playlist);
        return ApiResponse.ok(playlist);
    }

    /**
     * 删除播放列表（前端路由：DELETE /playlist/{id}，需登录）
     * 仅播放列表所有者本人可删除，否则抛 403 无权限错误；不存在时返回 404
     *
     * @param id           播放列表 ID
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 成功提示
     */
    @DeleteMapping(value={"/{id}"})
    public ApiResponse<Object> delete(@PathVariable Long id, @RequestHeader(value="Authorization") String authorization) {
        Long userId = AuthUtils.requireUserId(authorization, this.jwtUtil, this.jwtProperties);
        Playlist exist = (Playlist)this.playlistService.getById(id);
        if (exist == null) {
            return ApiResponse.fail((int)404, (String)"Playlist not found");
        }
        if (exist.getUserId() != null && !exist.getUserId().equals(userId)) {
            throw new UnauthorizedException(403, "\u65e0\u6743\u5220\u9664\u8be5\u64ad\u653e\u5217\u8868");
        }
        this.playlistService.removeById(id);
        return ApiResponse.ok();
    }
}
