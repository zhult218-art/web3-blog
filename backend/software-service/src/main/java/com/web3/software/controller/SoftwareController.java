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
package com.web3.software.controller;

import com.web3.common.core.ApiResponse;
import com.web3.common.core.PageResult;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import com.web3.software.entity.Software;
import com.web3.software.service.SoftwareService;
import com.web3.software.vo.SoftwareVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 软件管理控制器(software-service 模块)。
 *
 * <p>负责对外暴露软件下载列表、详情、创建(管理员)与下载计数接口,
 * 所有接口统一以 {@code /software} 为前缀(由 {@code @RestController}
 * 与 {@code @RequestMapping("/software")} 声明)。创建软件要求请求头
 * 携带 {@code Authorization} 登录令牌且具备管理员权限。</p>
 */
@RestController
@RequestMapping(value={"/software"})
public class SoftwareController {
    private final SoftwareService softwareService;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    public SoftwareController(SoftwareService softwareService, JwtUtil jwtUtil, JwtProperties jwtProperties) {
        this.softwareService = softwareService;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 分页查询软件列表(前端路由 GET /software/list),支持关键词与分类筛选。
     *
     * @param page     页码,从 1 开始,默认 1
     * @param size     每页条数,默认 20
     * @param keyword  关键词(可选),匹配名称/描述
     * @param category 分类(可选),精确匹配
     * @return 软件分页结果
     */
    @GetMapping(value={"/list"})
    public ApiResponse<PageResult<SoftwareVO>> list(@RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int size, @RequestParam(required=false) String keyword, @RequestParam(required=false) String category) {
        return ApiResponse.ok(this.softwareService.page(page, size, keyword, category));
    }

    /**
     * 按 ID 查询软件详情(前端路由 GET /software/{id})。
     *
     * @param id 软件 ID(路径参数)
     * @return 软件详情;不存在时返回 code=404
     */
    @GetMapping(value={"/{id}"})
    public ApiResponse<SoftwareVO> getById(@PathVariable Long id) {
        SoftwareVO vo = this.softwareService.findById(id);
        if (vo == null) {
            return ApiResponse.fail((int)404, (String)"Software not found");
        }
        return ApiResponse.ok(vo);
    }

    /**
     * 新建软件条目(前端路由 POST /software),仅管理员可操作。
     *
     * @param software     软件信息(request body)
     * @param authorization 登录令牌(请求头),需具备管理员角色
     * @return 创建成功后的软件详情
     */
    @PostMapping
    public ApiResponse<SoftwareVO> create(@RequestBody Software software, @RequestHeader(value="Authorization") String authorization) {
        AuthUtils.requireAdmin(authorization, this.jwtUtil, this.jwtProperties);
        return ApiResponse.ok(this.softwareService.create(software));
    }

    /**
     * 记录软件下载(前端路由 POST /software/{id}/download),
     * 将软件的下载计数 +1。
     *
     * @param id 软件 ID
     * @return 空 data 的成功响应
     */
    @PostMapping(value={"/{id}/download"})
    public ApiResponse<Object> download(@PathVariable Long id) {
        this.softwareService.incrementDownload(id);
        return ApiResponse.ok();
    }
}
