package com.web3.resource.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 类名：ResourceVO
 * 所属模块：resource-service（资源服务）
 * 职责：资源接口返回给前端的展示层 VO，字段与 Resource 实体一致，
 *       另附加 downloadCount 下载计数字段，不包含逻辑删除标记 deleted。
 * 关键注解：@Data 自动生成 getter/setter/equals/hashCode/toString。
 */
@Data
public class ResourceVO {
    /** 资源 ID */
    private Long id;
    /** 资源标题 */
    private String title;
    /** 资源描述 */
    private String description;
    /** 资源分类 */
    private String category;
    /** 存储文件名 */
    private String filename;
    /** 原始文件名 */
    private String originalFilename;
    /** 文件 MIME 类型 */
    private String contentType;
    /** 文件大小（字节） */
    private Long size;
    /** 下载地址 */
    private String downloadUrl;
    /** 下载次数 */
    private Long downloadCount;
    /** 创建时间 */
    private LocalDateTime createdAt;
}
