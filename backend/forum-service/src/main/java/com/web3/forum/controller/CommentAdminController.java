package com.web3.forum.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.web3.common.core.ApiResponse;
import com.web3.common.core.PageResult;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import com.web3.forum.dto.BatchModerateRequest;
import com.web3.forum.dto.CommentVO;
import com.web3.forum.service.CommentService;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * CommentAdminController —— 社区评论内容管理接口（仅管理员）
 * <p>
 * 所属模块：forum-service（论坛模块）。
 * <p>
 * 网关按 /comment/** 转发到本服务；所有接口通过 JWT 强制校验 ROLE_ADMIN，
 * 提供评论全量分页（含待审核/已驳回，附带所属帖子标题）、审核通过、驳回、删除能力，
 * 审核状态迁移时由 CommentService 联动维护帖子回复数。
 */
@RestController
@RequestMapping(value = "/comment/admin")
public class CommentAdminController {
    private final CommentService commentService;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    public CommentAdminController(CommentService commentService, JwtUtil jwtUtil, JwtProperties jwtProperties) {
        this.commentService = commentService;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
    }

    /** 全量评论分页（管理端，按创建时间倒序） */
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<CommentVO>> list(@RequestHeader(value = "Authorization") String token,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "20") int size,
                                                   @RequestParam(defaultValue = "all") String status,
                                                   @RequestParam(defaultValue = "") String keyword,
                                                   @RequestParam(required = false) Long targetId) {
        AuthUtils.requireAdmin(token, this.jwtUtil, this.jwtProperties);
        Page<CommentVO> result = this.commentService.adminPage(page, size, status, keyword, targetId);
        return ApiResponse.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    /** 审核通过：评论状态置为 published，帖子回复数同步 +1（仅顶级评论） */
    @PostMapping(value = "/{id}/approve")
    public ApiResponse<Object> approve(@PathVariable Long id,
                                       @RequestHeader(value = "Authorization") String token) {
        AuthUtils.requireAdmin(token, this.jwtUtil, this.jwtProperties);
        this.commentService.moderate(id, "published");
        return ApiResponse.ok();
    }

    /** 审核驳回：评论状态置为 rejected，公开评论树不可见 */
    @PostMapping(value = "/{id}/reject")
    public ApiResponse<Object> reject(@PathVariable Long id,
                                      @RequestHeader(value = "Authorization") String token) {
        AuthUtils.requireAdmin(token, this.jwtUtil, this.jwtProperties);
        this.commentService.moderate(id, "rejected");
        return ApiResponse.ok();
    }

    /** 管理员删除评论（逻辑删除），已通过的顶级评论会使帖子回复数 -1 */
    @DeleteMapping(value = "/{id}")
    public ApiResponse<Object> delete(@PathVariable Long id,
                                      @RequestHeader(value = "Authorization") String token) {
        AuthUtils.requireAdmin(token, this.jwtUtil, this.jwtProperties);
        this.commentService.adminDelete(id);
        return ApiResponse.ok();
    }

    /**
     * 批量审核/删除：body.action = approve / reject / delete
     *
     * @return data 包含 affected（实际处理条数，回复数联动由 Service 维护）
     */
    @PostMapping(value = "/batch")
    public ApiResponse<Map<String, Object>> batch(@RequestBody BatchModerateRequest body,
                                                  @RequestHeader(value = "Authorization") String token) {
        AuthUtils.requireAdmin(token, this.jwtUtil, this.jwtProperties);
        if (body.getIds() == null || body.getIds().isEmpty()) {
            return ApiResponse.fail(400, "请先选择评论");
        }
        int affected;
        switch (body.getAction() == null ? "" : body.getAction()) {
            case "approve" -> affected = this.commentService.batchModerate(body.getIds(), "published");
            case "reject" -> affected = this.commentService.batchModerate(body.getIds(), "rejected");
            case "delete" -> affected = this.commentService.batchDelete(body.getIds());
            default -> {
                return ApiResponse.fail(400, "不支持的批量操作");
            }
        }
        return ApiResponse.ok(Map.of("affected", affected));
    }
}
