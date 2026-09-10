package com.web3.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * FriendLink —— 友情链接实体类
 * <p>
 * 所属模块：blog-service（博客模块）。
 * <p>
 * 对应数据库表：friend_link（友链表）。
 * <p>
 * status：PUBLISHED=已发布 / PENDING=待审核 / DISABLED=禁用。
 */
@TableName(value="friend_link")
public class FriendLink {
    @TableId(type=IdType.AUTO)
    private Long id;
    private String name;
    private String url;
    private String avatar;
    private String description;
    private String groupName;
    private String status;
    private Integer sort;
    private LocalDateTime createdAt;

    public Long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getUrl() {
        return this.url;
    }

    public String getAvatar() {
        return this.avatar;
    }

    public String getDescription() {
        return this.description;
    }

    public String getGroupName() {
        return this.groupName;
    }

    public String getStatus() {
        return this.status;
    }

    public Integer getSort() {
        return this.sort;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
