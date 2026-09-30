package com.web3.media.controller;

import com.web3.common.core.ApiResponse;
import com.web3.common.core.PageResult;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import com.web3.media.entity.Photo;
import com.web3.media.security.FileSecurityChecker;
import com.web3.media.service.PhotoService;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;

/**
 * PhotoController —— 相册照片接口控制器
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 职责：管理相册照片的对外 REST 接口，包括照片分页列表、创建/删除（仅管理员）、
 * 照片文件上传（仅管理员，仅图片格式）、照片文件在线访问、以及点赞自增。
 * <p>
 * 关键注解作用：
 * <ul>
 *   <li>@RestController：声明为 REST 控制器，返回值自动序列化为 JSON</li>
 *   <li>@RequestMapping("/photo")：对外接口统一前缀为 /photo</li>
 * </ul>
 */
@RestController
@RequestMapping(value={"/photo"})
public class PhotoController {
    /** 相册照片业务服务 */
    private final PhotoService photoService;
    /** JWT 令牌校验工具 */
    private final JwtUtil jwtUtil;
    /** JWT 配置属性 */
    private final JwtProperties jwtProperties;

    public PhotoController(PhotoService photoService, JwtUtil jwtUtil, JwtProperties jwtProperties) {
        this.photoService = photoService;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 照片分页列表（前端路由：GET /photo/list）
     * 按创建时间倒序返回，无需登录
     *
     * @param page 页码，默认 1
     * @param size 每页条数，默认 60
     * @return 分页的照片实体列表
     */
    @GetMapping(value={"/list"})
    public ApiResponse<PageResult<Photo>> list(@RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="60") int size) {
        return ApiResponse.ok(this.photoService.page(page, size));
    }

    /**
     * 创建照片（前端路由：POST /photo）
     * 权限：ADMIN 恒可；普通用户需被授予 upload:photo 权限
     *
     * @param photo         照片实体（JSON 请求体）
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 保存后的照片实体
     */
    @PostMapping
    public ApiResponse<Photo> create(@RequestBody Photo photo, @RequestHeader(value="Authorization") String authorization) {
        String operator = AuthUtils.requirePerm(authorization, this.jwtUtil, this.jwtProperties, "upload:photo", false);
        if (photo.getUploader() == null || photo.getUploader().isBlank()) {
            photo.setUploader(operator);
        }
        return ApiResponse.ok(this.photoService.create(photo));
    }

    /**
     * 照片文件上传（前端路由：POST /photo/upload）
     * 权限：ADMIN 恒可；普通用户需被授予 upload:photo 权限（用户管理后台配置）
     * 仅允许图片扩展名（jpg/jpeg/png/gif/webp），存储到上传目录的 photos 子目录
     *
     * @param file          上传的图片文件（multipart/form-data，参数名 file）
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 包含 filename 与 url（/photo/file/{filename}）的结果
     */
    @PostMapping(value={"/upload"})
    public ApiResponse<Map<String, Object>> upload(@RequestParam(value="file") MultipartFile file, @RequestHeader(value="Authorization") String authorization) {
        AuthUtils.requirePerm(authorization, this.jwtUtil, this.jwtProperties, "upload:photo", false);
        try {
            return ApiResponse.ok(this.photoService.upload(file));
        }
        catch (IllegalArgumentException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
        catch (IOException e) {
            return ApiResponse.fail(500, "上传失败，请重试");
        }
    }

    /**
     * 照片文件访问（前端路由：GET /photo/file/{filename}）
     * 返回本地磁盘中的图片文件流；文件不存在时返回 404
     *
     * @param filename 文件名（UUID.ext 格式）
     * @return 文件流响应
     */
    @GetMapping(value={"/file/{filename}"})
    public ResponseEntity<FileSystemResource> file(@PathVariable String filename) {
        Path target = this.photoService.getFile(filename);
        if (target == null) {
            return ResponseEntity.notFound().build();
        }
        String ext = FileSecurityChecker.extractExtension(filename);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(FileSecurityChecker.resolveContentType(ext)))
            .body(new FileSystemResource(target.toFile()));
    }

    /**
     * 照片点赞（前端路由：PUT /photo/{id}/like）
     *
     * @param id 照片 ID
     * @return 自增后的最新点赞数
     */
    @PutMapping(value={"/{id}/like"})
    public ApiResponse<Integer> like(@PathVariable Long id) {
        return ApiResponse.ok(this.photoService.like(id));
    }

    /**
     * 更新照片信息（前端路由：PUT /photo/{id}，需管理员）
     * 仅允许更新标题与描述，url/点赞数等字段不受影响
     *
     * @param id            照片 ID
     * @param body          请求体（携带 title/description）
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 更新后的照片实体
     */
    @PutMapping(value={"/{id}"})
    public ApiResponse<Photo> update(@PathVariable Long id, @RequestBody Photo body, @RequestHeader(value="Authorization") String authorization) {
        AuthUtils.requireAdmin(authorization, this.jwtUtil, this.jwtProperties);
        Photo updated = this.photoService.updateInfo(id, body.getTitle(), body.getDescription());
        if (updated == null) {
            return ApiResponse.fail(404, "照片不存在");
        }
        return ApiResponse.ok(updated);
    }

    /**
     * 删除照片（前端路由：DELETE /photo/{id}，需管理员，逻辑删除）
     *
     * @param id            照片 ID
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 成功提示
     */
    @DeleteMapping(value={"/{id}"})
    public ApiResponse<Object> delete(@PathVariable Long id, @RequestHeader(value="Authorization") String authorization) {
        AuthUtils.requireAdmin(authorization, this.jwtUtil, this.jwtProperties);
        this.photoService.delete(id);
        return ApiResponse.ok();
    }
}
