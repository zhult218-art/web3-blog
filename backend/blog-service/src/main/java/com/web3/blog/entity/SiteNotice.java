package com.web3.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * SiteNotice —— 站点公告实体类
 * <p>
 * 所属模块：blog-service（博客模块）。
 * <p>
 * 对应数据库表：site_notice（公告表）。
 */
@TableName(value="site_notice")
public class SiteNotice {
    @TableId(type=IdType.AUTO)
    private Long id;
    private String content;
    private Integer enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return this.id;
    }

    public String getContent() {
        return this.content;
    }

    public Integer getEnabled() {
        return this.enabled;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
