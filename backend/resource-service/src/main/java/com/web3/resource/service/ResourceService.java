/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.core.conditions.Wrapper
 *  com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper
 *  com.baomidou.mybatisplus.core.metadata.IPage
 *  com.baomidou.mybatisplus.extension.plugins.pagination.Page
 *  com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
 *  com.web3.common.core.PageResult
 *  org.springframework.stereotype.Service
 *  org.springframework.util.StringUtils
 *  org.springframework.web.multipart.MultipartFile
 */
package com.web3.resource.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.web3.common.core.PageResult;
import com.web3.resource.config.ResourceProperties;
import com.web3.resource.entity.Resource;
import com.web3.resource.mapper.ResourceMapper;
import com.web3.resource.vo.ResourceVO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 资源业务服务(resource-service 模块)。
 *
 * <p>继承 MyBatis-Plus 的 {@link com.baomidou.mybatisplus.extension.service.impl.ServiceImpl},
 * 提供资源的分页查询(支持关键词/分类筛选)、详情、文件上传落盘、下载路径
 * 校验、信息更新与删除(同步清理磁盘文件)等逻辑。文件存储目录由
 * {@link com.web3.resource.config.ResourceProperties} 配置。</p>
 */
@Service
public class ResourceService
extends ServiceImpl<ResourceMapper, Resource> {
    private final ResourceProperties resourceProperties;

    public ResourceService(ResourceProperties resourceProperties) {
        this.resourceProperties = resourceProperties;
    }

    public PageResult<ResourceVO> page(int pageNum, int pageSize, String keyword, String category) {
        LambdaQueryWrapper<Resource> wrapper = new LambdaQueryWrapper<Resource>();
        if (StringUtils.hasText((String)keyword)) {
            wrapper.and(w -> w.like(Resource::getTitle, (Object)keyword).or().like(Resource::getDescription, (Object)keyword));
        }
        if (StringUtils.hasText((String)category)) {
            wrapper.eq(Resource::getCategory, (Object)category);
        }
        wrapper.orderByDesc(Resource::getCreatedAt);
        Page<Resource> p = new Page<Resource>((long)pageNum, (long)pageSize);
        IPage<Resource> result = this.page(p, wrapper);
        List<ResourceVO> records = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    /**
     * 按 ID 查询资源详情并转换为 VO。
     *
     * @param id 资源 ID
     * @return 资源 VO;不存在时返回 null
     */
    public ResourceVO findById(Long id) {
        Resource entity = (Resource)this.getById(id);
        if (entity == null) {
            return null;
        }
        return this.toVO(entity);
    }

    /**
     * 上传资源文件:校验并提取安全的文件扩展名,以 UUID 重命名文件存入
     * 上传目录,再保存资源记录到数据库。
     *
     * @param file        上传的文件
     * @param title       资源标题(为空时使用原文件名)
     * @param description 资源描述
     * @param category    资源分类
     * @return 上传成功后的资源 VO
     * @throws IOException 目录创建或文件写入失败时抛出
     */
    public ResourceVO upload(MultipartFile file, String title, String description, String category) throws IOException {
        String uploadDir = this.resourceProperties.getUploadDir();
        Path uploadPath = Paths.get(uploadDir, new String[0]);
        if (!Files.exists(uploadPath, new LinkOption[0])) {
            Files.createDirectories(uploadPath, new FileAttribute[0]);
        }
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            String rawExt = originalFilename.substring(originalFilename.lastIndexOf("."));
            if (rawExt.matches("\\.[A-Za-z0-9]{1,10}")) {
                ext = rawExt;
            }
        }
        String filename = UUID.randomUUID().toString() + ext;
        Path targetPath = uploadPath.resolve(filename);
        file.transferTo(targetPath.toFile());
        Resource resource = new Resource();
        resource.setTitle(StringUtils.hasText((String)title) ? title : originalFilename);
        resource.setDescription(description);
        resource.setCategory(category);
        resource.setFilename(filename);
        resource.setOriginalFilename(originalFilename != null ? originalFilename.replaceAll("[\\\\/]", "_") : null);
        resource.setContentType(file.getContentType());
        resource.setSize(file.getSize());
        resource.setDownloadUrl("/resource/download/" + filename);
        resource.setCreatedAt(LocalDateTime.now());
        this.save(resource);
        return this.toVO(resource);
    }

    /**
     * 计算文件下载路径:将文件名解析到上传目录下,并做路径归一化校验,
     * 防止目录穿越攻击。
     *
     * @param filename 文件名
     * @return 上传目录下的目标文件路径
     * @throws IllegalArgumentException 文件名越界或非法时抛出
     */
    public Path getDownloadPath(String filename) {
        Path uploadPath = Paths.get(this.resourceProperties.getUploadDir(), new String[0]).toAbsolutePath().normalize();
        Path resolved = uploadPath.resolve(filename).normalize();
        if (!resolved.startsWith(uploadPath)) {
            throw new IllegalArgumentException("Invalid filename");
        }
        return resolved;
    }

    /**
     * 更新资源信息:标题/分类仅在传入非空时更新,描述直接覆盖。
     *
     * @param id          资源 ID
     * @param title       新标题(可空)
     * @param description 新描述(可空)
     * @param category    新分类(可空)
     * @return 更新后的资源 VO;资源不存在时返回 null
     */
    public ResourceVO updateInfo(Long id, String title, String description, String category) {
        Resource resource = (Resource)this.getById(id);
        if (resource == null) {
            return null;
        }
        if (StringUtils.hasText((String)title)) {
            resource.setTitle(title);
        }
        resource.setDescription(description);
        if (StringUtils.hasText((String)category)) {
            resource.setCategory(category);
        }
        this.updateById(resource);
        return this.toVO(resource);
    }

    /**
     * 删除资源:先尝试删除磁盘上的文件(失败仅忽略),再从数据库逻辑删除记录。
     *
     * @param id 资源 ID
     * @return true 删除成功;资源不存在时返回 false
     */
    public boolean deleteResource(Long id) {
        Resource resource = (Resource)this.getById(id);
        if (resource == null) {
            return false;
        }
        try {
            Path filePath = Paths.get(this.resourceProperties.getUploadDir(), new String[0]).resolve(resource.getFilename());
            Files.deleteIfExists(filePath);
        }
        catch (IOException iOException) {
            // empty catch block
        }
        return this.removeById(id);
    }

    /**
     * 资源实体转换为展示层 VO(字段同名拷贝)。
     *
     * @param r 资源实体
     * @return 资源 VO
     */
    private ResourceVO toVO(Resource r) {
        ResourceVO vo = new ResourceVO();
        vo.setId(r.getId());
        vo.setTitle(r.getTitle());
        vo.setDescription(r.getDescription());
        vo.setCategory(r.getCategory());
        vo.setFilename(r.getFilename());
        vo.setOriginalFilename(r.getOriginalFilename());
        vo.setContentType(r.getContentType());
        vo.setSize(r.getSize());
        vo.setDownloadUrl(r.getDownloadUrl());
        vo.setDownloadCount(0L);
        vo.setCreatedAt(r.getCreatedAt());
        return vo;
    }
}
