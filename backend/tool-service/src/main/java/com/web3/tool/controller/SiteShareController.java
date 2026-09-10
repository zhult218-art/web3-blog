package com.web3.tool.controller;

import com.web3.common.core.ApiResponse;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import com.web3.tool.entity.SiteShare;
import com.web3.tool.service.SiteShareService;
import com.web3.tool.vo.SiteShareVO;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * SiteShareController —— 分享网站接口控制器
 * <p>
 * 所属模块：tool-service（工具模块）。
 * <p>
 * 职责：对外暴露分享网站的列表查询（公开）、分类列表（公开），以及新增/更新/删除（仅管理员）。
 * 管理类接口通过请求头 Authorization 校验管理员角色。
 */
@RestController
@RequestMapping("/site")
public class SiteShareController {
    private final SiteShareService siteShareService;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    public SiteShareController(SiteShareService siteShareService, JwtUtil jwtUtil, JwtProperties jwtProperties) {
        this.siteShareService = siteShareService;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 分享网站列表（公开，前端路由：GET /site/list）
     * 支持分类/关键词筛选；onlyVisible=true 时仅返回显示中的站点（前台展示用）
     *
     * @param category   分类（可选）
     * @param keyword    关键词（可选）
     * @param onlyVisible 是否仅返回显示状态的站点，默认 true
     * @return 站点视图对象列表
     */
    @GetMapping("/list")
    public ApiResponse<List<SiteShareVO>> list(@RequestParam(required = false) String category,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(defaultValue = "true") boolean onlyVisible) {
        return ApiResponse.ok(this.siteShareService.listSites(category, keyword, onlyVisible));
    }

    /**
     * 分享网站分类列表（公开，前端路由：GET /site/categories）
     *
     * @return 去重后的分类名称列表
     */
    @GetMapping("/categories")
    public ApiResponse<List<String>> categories() {
        return ApiResponse.ok(this.siteShareService.listCategories());
    }

    /**
     * 新增分享网站（前端路由：POST /site，仅管理员）
     *
     * @param site          站点信息（JSON 请求体）
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 创建成功后的站点视图对象
     */
    @PostMapping
    public ApiResponse<SiteShareVO> create(@RequestBody SiteShare site,
                                           @RequestHeader(value = "Authorization") String authorization) {
        AuthUtils.requireAdmin(authorization, this.jwtUtil, this.jwtProperties);
        return ApiResponse.ok(this.siteShareService.create(site));
    }

    /**
     * 更新分享网站（前端路由：PUT /site/{id}，仅管理员）
     *
     * @param id           站点 ID
     * @param site         站点信息（JSON 请求体）
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 更新后的站点视图对象
     */
    @PutMapping("/{id}")
    public ApiResponse<SiteShareVO> update(@PathVariable Long id, @RequestBody SiteShare site,
                                           @RequestHeader(value = "Authorization") String authorization) {
        AuthUtils.requireAdmin(authorization, this.jwtUtil, this.jwtProperties);
        return ApiResponse.ok(this.siteShareService.update(id, site));
    }

    /**
     * 删除分享网站（前端路由：DELETE /site/{id}，仅管理员）
     *
     * @param id           站点 ID
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 成功响应
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Object> delete(@PathVariable Long id,
                                      @RequestHeader(value = "Authorization") String authorization) {
        AuthUtils.requireAdmin(authorization, this.jwtUtil, this.jwtProperties);
        this.siteShareService.delete(id);
        return ApiResponse.ok();
    }
}