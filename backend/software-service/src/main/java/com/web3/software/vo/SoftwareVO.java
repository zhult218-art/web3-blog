package com.web3.software.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 类名：SoftwareVO
 * 所属模块：software-service（软件下载服务）
 * 职责：软件接口返回给前端的展示层 VO，字段与 Software 实体一致，
 *       不包含逻辑删除标记 deleted。
 * 关键注解：@Data 自动生成 getter/setter/equals/hashCode/toString。
 */
@Data
public class SoftwareVO {
    /** 软件 ID */
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
    /** 适用操作系统 */
    private String os;
    /** 图标地址 */
    private String icon;
    /** 下载次数 */
    private Long downloadCount;
    /** 创建时间 */
    private LocalDateTime createdAt;
}
