package com.web3.tool.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * SiteShare —— 分享网站实体类
 * <p>
 * 所属模块：tool-service（工具模块）。
 * <p>
 * 对应数据库表：site_share（分享网站表）。
 * 保存用户收藏推荐的优质外链（AI 工具、设计、视频、研究等），按分类展示、后台增删管理。
 * id 为雪花算法主键；sort 排序值越小越靠前；status 控制显示/隐藏；
 * deleted 为逻辑删除标记（@TableLogic 自动过滤）；createdAt 插入时自动填充。
 */
@TableName("site_share")
public class SiteShare {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 分类（如 研究/视频/设计/编码助手） */
    private String category;
    /** 站点名称 */
    private String name;
    /** 站点地址 */
    private String url;
    /** 描述 */
    private String description;
    /** 图标（emoji 或图片地址） */
    private String icon;
    /** 排序（越小越靠前） */
    private Integer sort;
    /** 状态：1 显示 0 隐藏 */
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableLogic
    private Integer deleted;

    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }
    public String getCategory() { return this.category; }
    public void setCategory(String category) { this.category = category; }
    public String getName() { return this.name; }
    public void setName(String name) { this.name = name; }
    public String getUrl() { return this.url; }
    public void setUrl(String url) { this.url = url; }
    public String getDescription() { return this.description; }
    public void setDescription(String description) { this.description = description; }
    public String getIcon() { return this.icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public Integer getSort() { return this.sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getStatus() { return this.status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return this.createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Integer getDeleted() { return this.deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}