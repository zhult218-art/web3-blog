package com.web3.tool.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;

/**
 * SiteShareVO —— 分享网站视图对象（VO）
 * <p>
 * 所属模块：tool-service（工具模块）。
 * <p>
 * 对前端输出的分享网站数据载体，由 SiteShare 实体转换而来；
 * id 使用 ToStringSerializer 序列化为字符串，避免雪花 ID 超出 JS Number 安全范围导致精度丢失。
 */
public class SiteShareVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String category;
    private String name;
    private String url;
    private String description;
    private String icon;
    private Integer sort;
    private Integer status;
    private LocalDateTime createdAt;

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
}