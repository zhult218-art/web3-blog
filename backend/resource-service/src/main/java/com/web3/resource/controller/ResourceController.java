/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.web3.common.core.ApiResponse
 *  com.web3.common.core.PageResult
 *  org.springframework.core.io.FileSystemResource
 *  org.springframework.core.io.Resource
 *  org.springframework.http.MediaType
 *  org.springframework.http.ResponseEntity
 *  org.springframework.http.ResponseEntity$BodyBuilder
 *  org.springframework.web.bind.annotation.DeleteMapping
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestHeader
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 *  org.springframework.web.multipart.MultipartFile
 */
package com.web3.resource.controller;

import com.web3.common.core.ApiResponse;
import com.web3.common.core.PageResult;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import com.web3.resource.service.ResourceService;
import com.web3.resource.vo.ResourceVO;
import java.io.IOException;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 资源管理控制器(resource-service 模块)。
 *
 * <p>负责对外暴露资源文件的列表查询、详情、上传、信息更新、下载与删除
 * 接口,所有接口统一以 {@code /resource} 为前缀(由 {@code @RestController}
 * 与 {@code @RequestMapping("/resource")} 声明)。上传、更新、删除接口
 * 均要求请求头携带 {@code Authorization} 登录令牌且具备管理员权限。</p>
 */
@RestController
@RequestMapping(value={"/resource"})
public class ResourceController {
    private final ResourceService resourceService;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    public ResourceController(ResourceService resourceService, JwtUtil jwtUtil, JwtProperties jwtProperties) {
        this.resourceService = resourceService;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 分页查询资源列表(前端路由 GET /resource/list),支持关键词与分类筛选。
     *
     * @param page     页码,从 1 开始,默认 1
     * @param size     每页条数,默认 20
     * @param keyword  关键词(可选),匹配标题/描述
     * @param category 分类(可选),精确匹配
     * @return 资源分页结果
     */
    @GetMapping(value={"/list"})
    public ApiResponse<PageResult<ResourceVO>> list(@RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int size, @RequestParam(required=false) String keyword, @RequestParam(required=false) String category) {
        return ApiResponse.ok(this.resourceService.page(page, size, keyword, category));
    }

    /**
     * 按 ID 查询资源详情(前端路由 GET /resource/{id})。
     *
     * @param id 资源 ID(路径参数)
     * @return 资源详情;不存在时返回 code=404
     */
    @GetMapping(value={"/{id}"})
    public ApiResponse<ResourceVO> getById(@PathVariable Long id) {
        ResourceVO vo = this.resourceService.findById(id);
        if (vo == null) {
            return ApiResponse.fail((int)404, (String)"Resource not found");
        }
        return ApiResponse.ok(vo);
    }

    /**
     * 上传资源文件(前端路由 POST /resource/upload),multipart 文件上传,仅管理员可操作。
     *
     * @param file          上传的文件(必填 form-data 参数)
     * @param title         资源标题(可选,缺省用原文件名)
     * @param description   资源描述(可选)
     * @param category      资源分类(可选)
     * @param authorization 登录令牌(请求头),需具备管理员角色
     * @return 上传成功后的资源详情
     * @throws IOException 文件写入磁盘失败时抛出
     */
    @PostMapping(value={"/upload"})
    public ApiResponse<ResourceVO> upload(@RequestParam(value="file") MultipartFile file, @RequestParam(required=false) String title, @RequestParam(required=false) String description, @RequestParam(required=false) String category, @RequestHeader(value="Authorization") String authorization) throws IOException {
        AuthUtils.requireAdmin(authorization, this.jwtUtil, this.jwtProperties);
        return ApiResponse.ok(this.resourceService.upload(file, title, description, category));
    }

    /**
     * 更新资源信息(前端路由 PUT /resource/{id}),仅管理员可操作。
     *
     * @param id            资源 ID(路径参数)
     * @param title         新标题(可选,为空则不更新)
     * @param description   新描述(可选)
     * @param category      新分类(可选,为空则不更新)
     * @param authorization 登录令牌(请求头)
     * @return 更新后的资源详情;资源不存在时返回 code=404
     */
    @PutMapping(value={"/{id}"})
    public ApiResponse<ResourceVO> updateInfo(@PathVariable Long id, @RequestParam(required=false) String title, @RequestParam(required=false) String description, @RequestParam(required=false) String category, @RequestHeader(value="Authorization") String authorization) {
        AuthUtils.requireAdmin(authorization, this.jwtUtil, this.jwtProperties);
        ResourceVO vo = this.resourceService.updateInfo(id, title, description, category);
        if (vo == null) {
            return ApiResponse.fail((int)404, (String)"Resource not found");
        }
        return ApiResponse.ok(vo);
    }

    /**
     * 下载资源文件(前端路由 GET /resource/download/{filename})。
     * 校验文件名位于上传目录内,以附件形式返回文件流。
     *
     * @param filename 存储在服务端的文件名
     * @return 文件下载响应(application/octet-stream);文件不存在时返回 404
     */
    @GetMapping(value={"/download/{filename}"})
    public ResponseEntity<Resource> download(@PathVariable String filename) {
        Path path = this.resourceService.getDownloadPath(filename);
        FileSystemResource fsr = new FileSystemResource(path);
        if (!fsr.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).header("Content-Disposition", "attachment; filename=\"" + filename + "\"").body(fsr);
    }

    /**
     * 删除资源(前端路由 DELETE /resource/{id}),删除数据库记录并清理磁盘文件,仅管理员可操作。
     *
     * @param id            资源 ID
     * @param authorization 登录令牌(请求头)
     * @return 删除成功返回空 data;资源不存在时返回 code=404
     */
    @DeleteMapping(value={"/{id}"})
    public ApiResponse<Object> delete(@PathVariable Long id, @RequestHeader(value="Authorization") String authorization) {
        AuthUtils.requireAdmin(authorization, this.jwtUtil, this.jwtProperties);
        boolean deleted = this.resourceService.deleteResource(id);
        if (!deleted) {
            return ApiResponse.fail((int)404, (String)"Resource not found");
        }
        return ApiResponse.ok();
    }
}
