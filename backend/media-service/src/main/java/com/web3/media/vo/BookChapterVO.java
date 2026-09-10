package com.web3.media.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;

/**
 * BookChapterVO —— 书籍章节视图对象（VO）
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 职责：对前端输出的章节数据载体，由 BookChapter 实体转换而来；
 * id 与 bookId 均使用 ToStringSerializer 序列化为字符串，避免雪花 ID 精度丢失。
 * 注意：章节列表接口（getChapters）会将该对象的 content 置空以减小传输量。
 */
public class BookChapterVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long bookId;
    private Integer chapterNo;
    private String chapterTitle;
    private String content;
    private Integer wordCount;
    private LocalDateTime createdAt;

    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }
    public Long getBookId() { return this.bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    public Integer getChapterNo() { return this.chapterNo; }
    public void setChapterNo(Integer chapterNo) { this.chapterNo = chapterNo; }
    public String getChapterTitle() { return this.chapterTitle; }
    public void setChapterTitle(String chapterTitle) { this.chapterTitle = chapterTitle; }
    public String getContent() { return this.content; }
    public void setContent(String content) { this.content = content; }
    public Integer getWordCount() { return this.wordCount; }
    public void setWordCount(Integer wordCount) { this.wordCount = wordCount; }
    public LocalDateTime getCreatedAt() { return this.createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}