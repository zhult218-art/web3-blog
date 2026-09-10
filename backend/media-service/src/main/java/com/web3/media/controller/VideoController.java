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
import com.web3.media.entity.Video;
import com.web3.media.security.FileSecurityChecker;
import com.web3.media.service.VideoService;
import com.web3.media.vo.VideoVO;
import java.io.IOException;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value={"/video"})
/**
 * VideoController —— 视频接口控制器
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 职责：管理视频资源的对外 REST 接口，包括视频分页列表、详情、文件上传、
 * 视频文件在线播放/下载、以及视频的增删改（仅管理员）。视频文件保存在本地磁盘，
 * 通过 /video/file/{filename} 路由对外提供。
 * <p>
 * 关键注解作用：
 * <ul>
 *   <li>@RestController：声明为 REST 控制器，返回值自动序列化为 JSON</li>
 *   <li>@RequestMapping("/video")：对外接口统一前缀为 /video</li>
 * </ul>
 */
public class VideoController {
    /** 视频业务服务 */
    private final VideoService videoService;
    /** JWT 令牌校验工具 */
    private final JwtUtil jwtUtil;
    /** JWT 配置属性 */
    private final JwtProperties jwtProperties;

    public VideoController(VideoService videoService, JwtUtil jwtUtil, JwtProperties jwtProperties) {
        this.videoService = videoService;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 视频分页列表（前端路由：GET /video/list）
     * 支持按标题/描述关键字筛选，按创建时间倒序返回
     *
     * @param page    页码，默认 1
     * @param size    每页条数，默认 20
     * @param keyword 标题/描述关键字，可为空
     * @return 分页的视频视图对象列表
     */
    @GetMapping(value={"/list"})
    public ApiResponse<PageResult<VideoVO>> list(@RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int size, @RequestParam(defaultValue="") String keyword) {
        return ApiResponse.ok(this.videoService.page(page, size, keyword));
    }

    /**
     * 视频详情（前端路由：GET /video/{id}）
     *
     * @param id 视频 ID
     * @return 视频视图对象
     */
    @GetMapping(value={"/{id}"})
    public ApiResponse<VideoVO> getById(@PathVariable Long id) {
        return ApiResponse.ok(this.videoService.getDetail(id));
    }

    /**
     * 视频文件上传（前端路由：POST /video/upload，需登录）
     * 校验文件后存储到本地磁盘并创建视频记录
     *
     * @param file        上传的视频文件（multipart/form-data）
     * @param title       视频标题（可选，缺省用原文件名）
     * @param description 视频描述（可选）
     * @param tags        标签（可选）
     * @param cover       封面地址（可选）
     * @param token       请求头 Authorization 的 JWT 令牌
     * @return 上传成功后的视频视图对象
     */
    @PostMapping(value={"/upload"})
    public ApiResponse<VideoVO> upload(@RequestParam(value="file") MultipartFile file,
            @RequestParam(value="title", required=false) String title,
            @RequestParam(value="description", required=false) String description,
            @RequestParam(value="tags", required=false) String tags,
            @RequestParam(value="cover", required=false) String cover,
            @RequestHeader(value="Authorization") String token) {
        AuthUtils.requireUserId(token, this.jwtUtil, this.jwtProperties);
        try {
            return ApiResponse.ok(this.videoService.upload(file, title, description, tags, cover));
        }
        catch (IllegalArgumentException e) {
            return ApiResponse.fail((int)400, e.getMessage());
        }
        catch (IOException e) {
            return ApiResponse.fail((int)500, "\u4e0a\u4f20\u5931\u8d25\uff0c\u8bf7\u91cd\u8bd5");
        }
    }

    /**
     * 视频文件访问（前端路由：GET /video/file/{filename}）
     * 返回本地磁盘中的视频文件流，用于浏览器播放/下载；文件不存在时返回 404
     *
     * @param filename 文件名（UUID.ext 格式）
     * @return 文件流响应
     */
    @GetMapping(value={"/file/{filename}"})
    public ResponseEntity<FileSystemResource> file(@PathVariable String filename) {
        Path target = this.videoService.getFile(filename);
        if (target == null) {
            return ResponseEntity.notFound().build();
        }
        String ext = FileSecurityChecker.extractExtension(filename);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(FileSecurityChecker.resolveContentType(ext)))
            .body(new FileSystemResource(target.toFile()));
    }

    /**
     * 创建视频（前端路由：POST /video，需管理员）
     *
     * @param video        视频实体（JSON 请求体）
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 创建成功后的视频视图对象
     */
    @PostMapping
    public ApiResponse<VideoVO> create(@RequestBody Video video, @RequestHeader(value="Authorization") String authorization) {
        AuthUtils.requireAdmin(authorization, this.jwtUtil, this.jwtProperties);
        return ApiResponse.ok(this.videoService.create(video));
    }

    /**
     * 更新视频（前端路由：PUT /video/{id}，需管理员）
     *
     * @param id           视频 ID
     * @param video        视频实体（JSON 请求体）
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 更新后的视频视图对象
     */
    @org.springframework.web.bind.annotation.PutMapping(value={"/{id}"})
    public ApiResponse<VideoVO> update(@PathVariable Long id, @RequestBody Video video, @RequestHeader(value="Authorization") String authorization) {
        AuthUtils.requireAdmin(authorization, this.jwtUtil, this.jwtProperties);
        return ApiResponse.ok(this.videoService.update(id, video));
    }

    /**
     * 删除视频（前端路由：DELETE /video/{id}，需管理员）
     *
     * @param id           视频 ID
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 成功提示
     */
    @org.springframework.web.bind.annotation.DeleteMapping(value={"/{id}"})
    public ApiResponse<Object> delete(@PathVariable Long id, @RequestHeader(value="Authorization") String authorization) {
        AuthUtils.requireAdmin(authorization, this.jwtUtil, this.jwtProperties);
        this.videoService.delete(id);
        return ApiResponse.ok();
    }
}
