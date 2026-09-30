package com.web3.media.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.web3.common.core.PageResult;
import com.web3.media.config.MediaUploadProperties;
import com.web3.media.entity.Photo;
import com.web3.media.mapper.PhotoMapper;
import com.web3.media.security.FileSecurityChecker;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * PhotoService —— 相册照片业务服务
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 职责：实现相册照片的核心业务逻辑，包括分页查询、创建/删除（管理员操作）、
 * 点赞自增、图片上传落盘（photos 子目录，仅允许图片扩展名）与本地文件安全读取（防目录穿越）。
 * <p>
 * 关键注解作用：
 * <ul>
 *   <li>@Service：声明为 Spring 业务组件</li>
 *   <li>继承 ServiceImpl&lt;PhotoMapper, Photo&gt;：获得 MyBatis-Plus 内置 CRUD 能力</li>
 * </ul>
 */
@Service
public class PhotoService
extends ServiceImpl<PhotoMapper, Photo> {
    /** 允许上传的图片扩展名白名单 */
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    /** 照片文件相对上传根目录的子目录名 */
    private static final String PHOTO_SUB_DIR = "photos";
    /** 上传目录与大小限制配置 */
    private final MediaUploadProperties uploadProperties;

    public PhotoService(MediaUploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    /**
     * 分页查询照片列表，按创建时间倒序
     *
     * @param page 页码（从 1 开始）
     * @param size 每页条数
     * @return 照片实体的分页结果
     */
    public PageResult<Photo> page(int page, int size) {
        LambdaQueryWrapper<Photo> wrapper = new LambdaQueryWrapper<Photo>().orderByDesc(Photo::getCreatedAt);
        Page<Photo> mpPage = new Page<Photo>((long)page, (long)size);
        Page<Photo> result = this.baseMapper.selectPage(mpPage, wrapper);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    /**
     * 创建照片记录（管理员操作）
     *
     * @param photo 照片实体
     * @return 保存后的照片实体
     */
    public Photo create(Photo photo) {
        if (photo.getLikes() == null) {
            photo.setLikes(0);
        }
        this.baseMapper.insert(photo);
        return photo;
    }

    /**
     * 上传照片文件到本地磁盘 photos 子目录
     * 仅允许图片扩展名（jpg/jpeg/png/gif/webp），以 UUID.ext 命名保存
     *
     * @param file 上传文件
     * @return 包含 filename 与 url（/photo/file/{filename}）的结果
     * @throws IllegalArgumentException 文件为空或扩展名不在白名单
     * @throws IOException              文件写入失败
     */
    public Map<String, Object> upload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        String ext = FileSecurityChecker.extractExtension(file.getOriginalFilename());
        if (ext == null || !IMAGE_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("不支持的文件类型，仅允许图片（jpg/jpeg/png/gif/webp）");
        }
        String filename = UUID.randomUUID().toString() + "." + ext;
        Path dir = Paths.get(this.uploadProperties.getDir(), PHOTO_SUB_DIR).toAbsolutePath().normalize();
        if (!Files.exists(dir, new LinkOption[0])) {
            Files.createDirectories(dir, new FileAttribute[0]);
        }
        Path target = dir.resolve(filename).normalize();
        if (!target.startsWith(dir)) {
            throw new IllegalArgumentException("Invalid filename");
        }
        file.transferTo(target.toFile());
        Map<String, Object> result = new HashMap<>();
        result.put("filename", filename);
        result.put("url", "/photo/file/" + filename);
        return result;
    }

    /**
     * 安全解析照片文件路径（防目录穿越）
     * 仅允许 "UUID格式.扩展名" 的文件名，并校验路径必须位于 photos 目录内
     *
     * @param filename 文件名
     * @return 文件在磁盘上的绝对路径；文件名不合法或文件不存在时返回 null
     */
    public Path getFile(String filename) {
        if (filename == null || !filename.matches("[a-f0-9-]{36}\\.[a-z0-9]{2,5}")) {
            return null;
        }
        Path dir = Paths.get(this.uploadProperties.getDir(), PHOTO_SUB_DIR).toAbsolutePath().normalize();
        Path target = dir.resolve(filename).normalize();
        if (!target.startsWith(dir) || !Files.exists(target, new LinkOption[0])) {
            return null;
        }
        return target;
    }

    /**
     * 照片点赞：likes 自增 1
     *
     * @param id 照片 ID
     * @return 自增后的最新点赞数
     */
    public Integer like(Long id) {
        Photo photo = this.baseMapper.selectById(id);
        if (photo == null) {
            throw new RuntimeException("照片不存在");
        }
        LambdaUpdateWrapper<Photo> wrapper = new LambdaUpdateWrapper<Photo>().eq(Photo::getId, id).setSql("likes = likes + 1");
        this.baseMapper.update(null, wrapper);
        return this.baseMapper.selectById(id).getLikes();
    }

    /**
     * 更新照片标题与描述（管理员操作）
     *
     * @param id          照片 ID
     * @param title       新标题（null 表示不修改）
     * @param description 新描述（null 表示不修改）
     * @return 更新后的照片实体；照片不存在时返回 null
     */
    public Photo updateInfo(Long id, String title, String description) {
        Photo photo = this.baseMapper.selectById(id);
        if (photo == null) {
            return null;
        }
        if (title != null) {
            photo.setTitle(title.trim());
        }
        if (description != null) {
            photo.setDescription(description.trim());
        }
        this.baseMapper.updateById(photo);
        return photo;
    }

    /**
     * 删除照片（管理员操作，逻辑删除）
     *
     * @param id 照片 ID
     */
    public void delete(Long id) {
        this.baseMapper.deleteById(id);
    }
}
