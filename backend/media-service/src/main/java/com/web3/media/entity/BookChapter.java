package com.web3.media.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * BookChapter —— 书籍章节实体类
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 对应数据库表：book_chapter（书籍章节表）。
 * <p>
 * bookId 关联所属书籍；chapterNo 章节号（同一书籍内唯一递增）；wordCount 按内容长度记录字数；
 * deleted 为逻辑删除标记（@TableLogic 自动过滤）；createdAt 插入时自动填充。
 */
@TableName("book_chapter")
public class BookChapter {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long bookId;
    private Integer chapterNo;
    private String chapterTitle;
    private String content;
    private Integer wordCount;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableLogic
    private Integer deleted;

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
    public Integer getDeleted() { return this.deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}