/*
 * ArticleLike entity - article like record
 */
package com.web3.blog.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * ArticleLike —— 文章点赞记录实体类
 * <p>
 * 所属模块：blog-service（博客模块）。
 * <p>
 * 对应数据库表：article_like（文章点赞记录表）。
 * <p>
 * 一条记录表示"某用户点赞了某文章"，articleId + userId 唯一；createdAt 插入时自动填充。
 */
@TableName(value="article_like")
public class ArticleLike {
    @TableId(type=IdType.ASSIGN_ID)
    private Long id;
    private Long articleId;
    private Long userId;
    @TableField(fill=FieldFill.INSERT)
    private LocalDateTime createdAt;

    public Long getId() {
        return this.id;
    }

    public Long getArticleId() {
        return this.articleId;
    }

    public Long getUserId() {
        return this.userId;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String toString() {
        return "ArticleLike(id=" + this.getId() + ", articleId=" + this.getArticleId() + ", userId=" + this.getUserId() + ", createdAt=" + this.getCreatedAt() + ")";
    }
}
