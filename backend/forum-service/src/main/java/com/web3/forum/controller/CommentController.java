/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.extension.plugins.pagination.Page
 *  com.web3.common.core.ApiResponse
 *  com.web3.common.core.PageResult
 *  com.web3.common.security.JwtUtil
 *  io.jsonwebtoken.Claims
 *  org.springframework.web.bind.annotation.GetMapping
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
import com.web3.forum.dto.CommentCreateDTO;
import com.web3.forum.dto.CommentVO;
import com.web3.forum.service.CommentService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/comment"})
/**
 * CommentController —— 评论接口控制器
 * <p>
 * 所属模块：forum-service（论坛模块）。
 * <p>
 * 职责：管理评论的对外 REST 接口，包括按目标（帖子/其他）分页查询评论、发表评论、删除评论。
 * 评论支持 targetType 区分评论对象（如 post），创建需登录，删除需作者或管理员。
 * <p>
 * 关键注解作用：
 * <ul>
 *   <li>@RestController：声明为 REST 控制器，返回值自动序列化为 JSON</li>
 *   <li>@RequestMapping("/comment")：对外接口统一前缀为 /comment</li>
 * </ul>
 */
public class CommentController {
    /** 评论业务服务 */
    private final CommentService commentService;
    /** JWT 令牌校验工具 */
    private final JwtUtil jwtUtil;
    /** JWT 配置属性 */
    private final JwtProperties jwtProperties;

    public CommentController(CommentService commentService, JwtUtil jwtUtil, JwtProperties jwtProperties) {
        this.commentService = commentService;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 评论分页列表（前端路由：GET /comment/list）
     * 按目标对象（targetId + targetType）查询其下的评论列表，按时间正序返回
     *
     * @param targetId   目标对象 ID（如帖子 ID）
     * @param targetType 目标类型（如 post）
     * @param page       页码，默认 1
     * @param size       每页条数，默认 20
     * @return 分页的评论视图对象列表
     */
    @GetMapping(value={"/list"})
    public ApiResponse<PageResult<CommentVO>> list(@RequestParam Long targetId, @RequestParam String targetType, @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int size) {
        Page<CommentVO> result = this.commentService.page(targetId, targetType, page, size);
        PageResult<CommentVO> pageResult = PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
        return ApiResponse.ok(pageResult);
    }

    /**
     * 发表评论（前端路由：POST /comment）
     * 登录可选：携带有效令牌时记录作者 ID；未登录可匿名评论（昵称必填）
     * 评论对象为帖子时同步帖子回复数 +1
     *
     * @param dto   评论创建参数（目标 ID、目标类型、内容、昵称）
     * @param token 请求头 Authorization 的 JWT 令牌（可空）
     * @return 创建成功后的评论视图对象
     */
    @PostMapping
    public ApiResponse<CommentVO> create(@RequestBody CommentCreateDTO dto, @RequestHeader(value="Authorization", required=false) String token) {
        Long authorId = null;
        if (token != null && !token.isBlank()) {
            try {
                authorId = AuthUtils.requireUserId(token, this.jwtUtil, this.jwtProperties);
            }
            catch (RuntimeException e) {
                authorId = null;
            }
        }
        if (authorId == null && (dto.getAuthorName() == null || dto.getAuthorName().isBlank())) {
            return ApiResponse.fail(400, "匿名评论请填写昵称");
        }
        CommentVO vo = this.commentService.create(dto, authorId);
        return ApiResponse.ok(vo);
    }

    /**
     * 删除评论（前端路由：DELETE /comment/{id}，需登录）
     * 仅作者本人或管理员可删除，否则抛 403 无权限错误
     *
     * @param id    评论 ID
     * @param token 请求头 Authorization 的 JWT 令牌
     * @return 成功提示
     */
    @org.springframework.web.bind.annotation.DeleteMapping(value={"/{id}"})
    public ApiResponse<Object> delete(@PathVariable Long id, @RequestHeader(value="Authorization") String token) {
        Long userId = AuthUtils.requireUserId(token, this.jwtUtil, this.jwtProperties);
        this.commentService.delete(id, userId, this.isAdmin(token));
        return ApiResponse.ok();
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
            io.jsonwebtoken.Claims claims = this.jwtUtil.parseClaims(token);
            return "ROLE_ADMIN".equals(claims.get("authorities", String.class));
        }
        catch (RuntimeException e) {
            return false;
        }
    }
}
