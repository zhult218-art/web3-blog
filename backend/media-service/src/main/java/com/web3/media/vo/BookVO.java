package com.web3.media.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;

/**
 * BookVO —— 书籍视图对象（VO）
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 职责：对前端输出的书籍数据载体，由 Book 实体转换而来；
 * id 使用 ToStringSerializer 序列化为字符串，避免雪花 ID 超出 JS Number 安全范围导致精度丢失。
 */
public class BookVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String title;
    private String author;
    private String dynasty;
    private String category;
    private String categorySub;
    private String description;
    private String cover;
    private Integer chapterCount;
    private LocalDateTime createdAt;

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
}