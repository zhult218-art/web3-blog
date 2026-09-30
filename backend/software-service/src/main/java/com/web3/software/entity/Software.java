package com.web3.software.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 类名：Software
 * 所属模块：software-service（软件下载服务）
 * 职责：软件下载实体类，对应数据库表 software，保存网站"软件下载"页面的软件条目
 *       （名称、描述、下载地址、分类、版本、大小、适用系统、图标等）。
 * 关键注解：@Data Lombok 化；@TableName("software") 表映射；
 *           @TableId(type = ASSIGN_ID) 雪花 ID 自动分配；
 *           @TableField(fill = INSERT) 插入时自动填充；@TableLogic 逻辑删除。
 */
@Data
@TableName("software")
public class Software {
    /** 软件主键（雪花 ID，自动分配） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 软件名称 */
    private String name;
    /** 软件描述 */
    private String description;
    /** 下载地址 */
    private String downloadUrl;
    /** 软件分类 */
    private String category;
    /** 版本号 */
    private String version;
    /** 文件大小（字节） */
    private Long size;
    /** 适用操作系统，如 Windows/macOS/Linux */
    private String os;
    /** 图标地址 */
    private String icon;
    /** 下载次数 */
    private Long downloadCount;
    /** 创建时间（插入时自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 逻辑删除标记（0 正常，1 已删除） */
    @TableLogic
    private Integer deleted;
}
