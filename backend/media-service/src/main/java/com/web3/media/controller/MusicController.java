/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.web3.common.core.ApiResponse
 *  com.web3.common.core.PageResult
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.web3.media.controller;

import com.web3.common.core.ApiResponse;
import com.web3.common.core.PageResult;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import com.web3.media.entity.Music;
import com.web3.media.service.MusicService;
import com.web3.media.vo.MusicVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/music"})
/**
 * MusicController —— 音乐接口控制器
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 职责：管理音乐资源的对外 REST 接口，包括音乐分页列表、详情、以及音乐的增删改（仅管理员）。
 * <p>
 * 关键注解作用：
 * <ul>
 *   <li>@RestController：声明为 REST 控制器，返回值自动序列化为 JSON</li>
 *   <li>@RequestMapping("/music")：对外接口统一前缀为 /music</li>
 * </ul>
 */
public class MusicController {
    /** 音乐业务服务 */
    private final MusicService musicService;
    /** JWT 令牌校验工具 */
    private final JwtUtil jwtUtil;
    /** JWT 配置属性 */
    private final JwtProperties jwtProperties;

    public MusicController(MusicService musicService, JwtUtil jwtUtil, JwtProperties jwtProperties) {
        this.musicService = musicService;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 音乐分页列表（前端路由：GET /music/list）
     * 支持按歌名/歌手关键字筛选，按创建时间倒序返回
     *
     * @param page    页码，默认 1
     * @param size    每页条数，默认 20
     * @param keyword 歌名/歌手关键字，可为空
     * @return 分页的音乐视图对象列表
     */
    @GetMapping(value={"/list"})
    public ApiResponse<PageResult<MusicVO>> list(@RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int size, @RequestParam(defaultValue="") String keyword) {
        return ApiResponse.ok(this.musicService.page(page, size, keyword));
    }

    /**
     * 音乐详情（前端路由：GET /music/{id}）
     *
     * @param id 音乐 ID
     * @return 音乐视图对象
     */
    @GetMapping(value={"/{id}"})
    public ApiResponse<MusicVO> getById(@PathVariable Long id) {
        return ApiResponse.ok(this.musicService.getDetail(id));
    }

    /**
     * 创建音乐（前端路由：POST /music，需管理员）
     *
     * @param music        音乐实体（JSON 请求体）
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 创建成功后的音乐视图对象
     */
    @PostMapping
    public ApiResponse<MusicVO> create(@RequestBody Music music, @RequestHeader(value="Authorization") String authorization) {
        AuthUtils.requireAdmin(authorization, this.jwtUtil, this.jwtProperties);
        return ApiResponse.ok(this.musicService.create(music));
    }

    /**
     * 更新音乐（前端路由：PUT /music/{id}，需管理员）
     *
     * @param id           音乐 ID
     * @param music        音乐实体（JSON 请求体）
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 更新后的音乐视图对象
     */
    @org.springframework.web.bind.annotation.PutMapping(value={"/{id}"})
    public ApiResponse<MusicVO> update(@PathVariable Long id, @RequestBody Music music, @RequestHeader(value="Authorization") String authorization) {
        AuthUtils.requireAdmin(authorization, this.jwtUtil, this.jwtProperties);
        return ApiResponse.ok(this.musicService.update(id, music));
    }

    /**
     * 删除音乐（前端路由：DELETE /music/{id}，需管理员）
     *
     * @param id           音乐 ID
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 成功提示
     */
    @org.springframework.web.bind.annotation.DeleteMapping(value={"/{id}"})
    public ApiResponse<Object> delete(@PathVariable Long id, @RequestHeader(value="Authorization") String authorization) {
        AuthUtils.requireAdmin(authorization, this.jwtUtil, this.jwtProperties);
        this.musicService.delete(id);
        return ApiResponse.ok();
    }
}
