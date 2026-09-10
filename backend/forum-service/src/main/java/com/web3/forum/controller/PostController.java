/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.extension.plugins.pagination.Page
 *  com.web3.common.core.ApiResponse
 *  com.web3.common.core.PageResult
 *  com.web3.common.security.JwtUtil
 *  io.jsonwebtoken.Claims
 *  org.springframework.web.bind.annotation.DeleteMapping
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestHeader
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.web3.forum.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.web3.common.core.ApiResponse;
import com.web3.common.core.PageResult;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import com.web3.forum.dto.PostCreateDTO;
import com.web3.forum.dto.PostVO;
import com.web3.forum.service.PostService;
import io.jsonwebtoken.Claims;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/post"})
/**
 * PostController —— 帖子接口控制器
 * <p>
 * 所属模块：forum-service（论坛模块）。
 * <p>
 * 职责：管理论坛帖子的对外 REST 接口，包括帖子列表、论坛统计数据、详情、创建、更新、点赞、删除。
 * 需要登录的接口通过请求头 Authorization 携带 JWT 令牌，删除/编辑校验作者或管理员权限。
 * <p>
 * 关键注解作用：
 * <ul>
 *   <li>@RestController：声明为 REST 控制器，返回值自动序列化为 JSON</li>
 *   <li>@RequestMapping("/post")：对外接口统一前缀为 /post</li>
 * </ul>
 */
public class PostController {
    /** 帖子业务服务 */
    private final PostService postService;
    /** JWT 令牌校验工具 */
    private final JwtUtil jwtUtil;
    /** JWT 配置属性 */
    private final JwtProperties jwtProperties;

    public PostController(PostService postService, JwtUtil jwtUtil, JwtProperties jwtProperties) {
        this.postService = postService;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 校验请求头令牌是否为管理员（ROLE_ADMIN）身份
     *
     * @param auth Authorization 请求头原始值（Bearer token）
     * @return true=管理员，false=非管理员或令牌无效
     */
    private boolean isAdmin(String auth) {
        try {
            String token = AuthUtils.resolveToken(auth, this.jwtProperties);
            if (!this.jwtUtil.validate(token)) {
                return false;
            }
            Claims claims = this.jwtUtil.parseClaims(token);
            return "ROLE_ADMIN".equals(claims.get("authorities", String.class));
        }
        catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * 帖子分页列表（前端路由：GET /post/list）
     * 支持按标题/正文关键字筛选与排序方式（latest=最新，hot=最热）
     *
     * @param page    页码，默认 1
     * @param size    每页条数，默认 20
     * @param keyword 关键字，可为空
     * @param sort    排序方式，默认 latest
     * @return 分页的帖子视图对象列表
     */
    @GetMapping(value={"/list"})
    public ApiResponse<PageResult<PostVO>> list(@RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int size, @RequestParam(defaultValue="") String keyword, @RequestParam(defaultValue="latest") String sort) {
        Page<PostVO> result = this.postService.page(page, size, keyword, sort);
        PageResult<PostVO> pageResult = PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
        return ApiResponse.ok(pageResult);
    }

    /**
     * 论坛统计数据（前端路由：GET /post/stats）
     * 返回帖子总数、评论总数、今日发帖数、活跃作者 TOP6、热门分类 TOP10
     *
     * @return 统计数据 Map
     */
    @GetMapping(value={"/stats"})
    public ApiResponse<java.util.Map<String, Object>> stats() {
        return ApiResponse.ok(this.postService.stats());
    }

    /**
     * 帖子详情（前端路由：GET /post/{id}）
     * 查看详情时浏览量 +1；帖子不存在时返回 404
     *
     * @param id 帖子 ID
     * @return 帖子视图对象
     */
    @GetMapping(value={"/{id}"})
    public ApiResponse<PostVO> detail(@PathVariable Long id) {
        PostVO vo = this.postService.getDetail(id);
        if (vo == null) {
            return ApiResponse.fail((int)404, (String)"Post not found");
        }
        return ApiResponse.ok(vo);
    }

    /**
     * 创建帖子（前端路由：POST /post，需登录）
     * 从请求头令牌解析出作者 ID 后创建帖子
     *
     * @param dto   帖子创建参数（标题、内容、分类、媒体等）
     * @param token 请求头 Authorization 的 JWT 令牌
     * @return 创建成功后的帖子视图对象
     */
    @PostMapping
    public ApiResponse<PostVO> create(@RequestBody PostCreateDTO dto, @RequestHeader(value="Authorization") String token) {
        Long authorId = AuthUtils.requireUserId(token, this.jwtUtil, this.jwtProperties);
        PostVO vo = this.postService.create(dto, authorId);
        return ApiResponse.ok(vo);
    }

    /**
     * 更新帖子（前端路由：PUT /post/{id}，需登录）
     * 仅作者本人或管理员可编辑，否则抛 403 无权限错误
     *
     * @param id    帖子 ID
     * @param dto   帖子更新参数
     * @param token 请求头 Authorization 的 JWT 令牌
     * @return 更新后的帖子视图对象
     */
    @org.springframework.web.bind.annotation.PutMapping(value={"/{id}"})
    public ApiResponse<PostVO> update(@PathVariable Long id, @RequestBody PostCreateDTO dto, @RequestHeader(value="Authorization") String token) {
        Long userId = AuthUtils.requireUserId(token, this.jwtUtil, this.jwtProperties);
        PostVO vo = this.postService.update(id, dto, userId, this.isAdmin(token));
        return ApiResponse.ok(vo);
    }

    /**
     * 帖子点赞/取消点赞（前端路由：POST /post/{id}/like，需登录）
     * 切换式操作：未点赞则点赞，已点赞则取消
     *
     * @param id    帖子 ID
     * @param token 请求头 Authorization 的 JWT 令牌
     * @return 成功提示
     */
    @PostMapping(value={"/{id}/like"})
    public ApiResponse<Object> like(@PathVariable Long id, @RequestHeader(value="Authorization") String token) {
        Long userId = AuthUtils.requireUserId(token, this.jwtUtil, this.jwtProperties);
        this.postService.like(id, userId);
        return ApiResponse.ok();
    }

    /**
     * 删除帖子（前端路由：DELETE /post/{id}，需登录）
     * 仅作者本人或管理员可删除
     *
     * @param id    帖子 ID
     * @param token 请求头 Authorization 的 JWT 令牌
     * @return 成功提示
     */
    @DeleteMapping(value={"/{id}"})
    public ApiResponse<Object> delete(@PathVariable Long id, @RequestHeader(value="Authorization") String token) {
        Long userId = AuthUtils.requireUserId(token, this.jwtUtil, this.jwtProperties);
        this.postService.delete(id, userId, this.isAdmin(token));
        return ApiResponse.ok();
    }
}
