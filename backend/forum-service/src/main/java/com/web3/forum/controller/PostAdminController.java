package com.web3.forum.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.web3.common.core.ApiResponse;
import com.web3.common.core.PageResult;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import com.web3.forum.dto.BatchModerateRequest;
import com.web3.forum.dto.PostVO;
import com.web3.forum.service.PostService;
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
 * PostAdminController —— 社区帖子内容管理接口（仅管理员）
 * <p>
 * 所属模块：forum-service（论坛模块）。
 * <p>
 * 网关按 /post/** 转发到本服务；所有接口通过 JWT 强制校验 ROLE_ADMIN，
 * 提供帖子的全量分页（含待审核/已驳回）、审核通过、驳回、删除能力。
 */
@RestController
@RequestMapping(value = "/post/admin")
public class PostAdminController {
    private final PostService postService;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    public PostAdminController(PostService postService, JwtUtil jwtUtil, JwtProperties jwtProperties) {
        this.postService = postService;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
    }

    /** 全量帖子分页（管理端，默认按创建时间倒序） */
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<PostVO>> list(@RequestHeader(value = "Authorization") String token,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "20") int size,
                                                @RequestParam(defaultValue = "all") String status,
                                                @RequestParam(defaultValue = "") String keyword) {
        AuthUtils.requireAdmin(token, this.jwtUtil, this.jwtProperties);
        Page<PostVO> result = this.postService.adminPage(page, size, status, keyword);
        return ApiResponse.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    /** 审核通过：帖子状态置为 published，前端社区立即可见 */
    @PostMapping(value = "/{id}/approve")
    public ApiResponse<Object> approve(@PathVariable Long id,
                                       @RequestHeader(value = "Authorization") String token) {
        AuthUtils.requireAdmin(token, this.jwtUtil, this.jwtProperties);
        this.postService.moderate(id, "published");
        return ApiResponse.ok();
    }

    /** 审核驳回：帖子状态置为 rejected，公开列表与详情均不可见 */
    @PostMapping(value = "/{id}/reject")
    public ApiResponse<Object> reject(@PathVariable Long id,
                                      @RequestHeader(value = "Authorization") String token) {
        AuthUtils.requireAdmin(token, this.jwtUtil, this.jwtProperties);
        this.postService.moderate(id, "rejected");
        return ApiResponse.ok();
    }

    /** 管理员删除帖子（逻辑删除） */
    @DeleteMapping(value = "/{id}")
    public ApiResponse<Object> delete(@PathVariable Long id,
                                      @RequestHeader(value = "Authorization") String token) {
        AuthUtils.requireAdmin(token, this.jwtUtil, this.jwtProperties);
        this.postService.adminDelete(id);
        return ApiResponse.ok();
    }

    /**
     * 批量审核/删除：body.action = approve / reject / delete
     *
     * @return data 包含 affected（实际处理条数）
     */
    @PostMapping(value = "/batch")
    public ApiResponse<Map<String, Object>> batch(@RequestBody BatchModerateRequest body,
                                                  @RequestHeader(value = "Authorization") String token) {
        AuthUtils.requireAdmin(token, this.jwtUtil, this.jwtProperties);
        if (body.getIds() == null || body.getIds().isEmpty()) {
            return ApiResponse.fail(400, "请先选择帖子");
        }
        int affected;
        switch (body.getAction() == null ? "" : body.getAction()) {
            case "approve" -> affected = this.postService.batchModerate(body.getIds(), "published");
            case "reject" -> affected = this.postService.batchModerate(body.getIds(), "rejected");
            case "delete" -> affected = this.postService.batchDelete(body.getIds());
            default -> {
                return ApiResponse.fail(400, "不支持的批量操作");
            }
        }
        return ApiResponse.ok(Map.of("affected", affected));
    }
}
