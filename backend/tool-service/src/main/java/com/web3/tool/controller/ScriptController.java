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
 *  org.springframework.web.bind.annotation.RequestHeader
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.web3.tool.controller;

import com.web3.common.core.ApiResponse;
import com.web3.common.core.PageResult;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import com.web3.tool.entity.Script;
import com.web3.tool.service.ScriptService;
import com.web3.tool.vo.ScriptVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 脚本管理控制器(tool-service 模块)。
 *
 * <p>负责对外暴露实用脚本的列表查询、详情、创建(管理员)与下载计数
 * 接口,所有接口统一以 {@code /script} 为前缀(由 {@code @RestController}
 * 与 {@code @RequestMapping("/script")} 声明)。创建脚本要求请求头携带
 * {@code Authorization} 登录令牌且具备管理员权限。</p>
 */
@RestController
@RequestMapping(value={"/script"})
public class ScriptController {
    private final ScriptService scriptService;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    public ScriptController(ScriptService scriptService, JwtUtil jwtUtil, JwtProperties jwtProperties) {
        this.scriptService = scriptService;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 分页查询脚本列表(前端路由 GET /script/list),支持关键词与分类筛选。
     *
     * @param page     页码,从 1 开始,默认 1
     * @param size     每页条数,默认 20
     * @param keyword  关键词(可选),匹配名称/描述/标签
     * @param category 分类(可选),精确匹配
     * @return 脚本分页结果
     */
    @GetMapping(value={"/list"})
    public ApiResponse<PageResult<ScriptVO>> list(@RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int size, @RequestParam(required=false) String keyword, @RequestParam(required=false) String category) {
        return ApiResponse.ok(this.scriptService.page(page, size, keyword, category));
    }

    /**
     * 按 ID 查询脚本详情(前端路由 GET /script/{id})。
     *
     * @param id 脚本 ID(路径参数)
     * @return 脚本详情;不存在时返回 code=404
     */
    @GetMapping(value={"/{id}"})
    public ApiResponse<ScriptVO> getById(@PathVariable Long id) {
        ScriptVO vo = this.scriptService.findById(id);
        if (vo == null) {
            return ApiResponse.fail((int)404, (String)"Script not found");
        }
        return ApiResponse.ok(vo);
    }

    /**
     * 新建脚本(前端路由 POST /script),仅管理员可操作。
     *
     * @param script       脚本信息(request body)
     * @param authorization 登录令牌(请求头),需具备管理员角色
     * @return 创建成功后的脚本详情
     */
    @PostMapping
    public ApiResponse<ScriptVO> create(@RequestBody Script script, @RequestHeader(value="Authorization") String authorization) {
        AuthUtils.requireAdmin(authorization, this.jwtUtil, this.jwtProperties);
        return ApiResponse.ok(this.scriptService.create(script));
    }

    /**
     * 记录脚本下载(前端路由 POST /script/{id}/download),
     * 将脚本的下载计数 +1。
     *
     * @param id 脚本 ID
     * @return 空 data 的成功响应
     */
    @PostMapping(value={"/{id}/download"})
    public ApiResponse<Object> download(@PathVariable Long id) {
        this.scriptService.incrementDownload(id);
        return ApiResponse.ok();
    }
}
