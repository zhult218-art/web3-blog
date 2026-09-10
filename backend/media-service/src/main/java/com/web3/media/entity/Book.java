package com.web3.media.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * Book —— 书籍实体类
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 对应数据库表：book（书籍表）。
 * <p>
 * 字段说明：id 为雪花算法主键；dynasty 朝代；category 分类（如国学/小说），categorySub 子分类；
 * chapterCount 章节总数（冗余维护）；deleted 为逻辑删除标记（@TableLogic 自动过滤）；
 * createdAt 插入时自动填充。
 */
@TableName("book")
public class Book {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String title;
    private String author;
    private String dynasty;
    private String category;
    private String categorySub;
    private String description;
    private String cover;
    private Integer chapterCount;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableLogic
    private Integer deleted;

    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return this.title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return this.author; }
    public void setAuthor(String author) { this.author = author; }
    public String getDynasty() { return this.dynasty; }
    public void setDynasty(String dynasty) { this.dynasty = dynasty; }
    public String getCategory() { return this.category; }
    public void setCategory(String category) { this.category = category; }
    public String getCategorySub() { return this.categorySub; }
    public void setCategorySub(String categorySub) { this.categorySub = categorySub; }
    public String getDescription() { return this.description; }
    public void setDescription(String description) { this.description = description; }
    public String getCover() { return this.cover; }
    public void setCover(String cover) { this.cover = cover; }
    public Integer getChapterCount() { return this.chapterCount; }
    public void setChapterCount(Integer chapterCount) { this.chapterCount = chapterCount; }
    public LocalDateTime getCreatedAt() { return this.createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Integer getDeleted() { return this.deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}