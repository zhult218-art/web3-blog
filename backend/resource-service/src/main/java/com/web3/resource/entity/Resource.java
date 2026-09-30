package com.web3.resource.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 类名：Resource
 * 所属模块：resource-service（资源服务）
 * 职责：资源文件实体类，对应数据库表 resource，保存上传资源文件的元信息
 *       （标题、描述、分类、存储文件名、原始文件名、文件类型、大小、下载地址等）。
 * 关键注解：@Data Lombok 化；@TableName("resource") 表映射；
 *           @TableId(type = ASSIGN_ID) 雪花 ID 自动分配；
 *           @TableField(fill = INSERT) 插入时自动填充；@TableLogic 逻辑删除。
 */
@Data
@TableName("resource")
public class Resource {
    /** 资源主键（雪花 ID，自动分配） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 资源标题 */
    private String title;
    /** 资源描述 */
    private String description;
    /** 资源分类 */
    private String category;
    /** 存储文件名（UUID 重命名，含扩展名） */
    private String filename;
    /** 用户上传时的原始文件名 */
    private String originalFilename;
    /** 文件 MIME 类型 */
    private String contentType;
    /** 文件大小（字节） */
    private Long size;
    /** 下载地址（/resource/download/{filename}） */
    private String downloadUrl;
    /** 下载次数 */
    private Long downloadCount;
    /** 创建时间（插入时自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 逻辑删除标记（0 正常，1 已删除） */
    @TableLogic
    private Integer deleted;
}
