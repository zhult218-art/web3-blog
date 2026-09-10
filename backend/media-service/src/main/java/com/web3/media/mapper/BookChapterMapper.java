package com.web3.media.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.media.entity.BookChapter;

/**
 * BookChapterMapper —— 书籍章节表数据访问接口
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 职责：操作 book_chapter 书籍章节表，继承 BaseMapper 获得通用 CRUD（由 @MapperScan 扫描注册）。
 */
public interface BookChapterMapper extends BaseMapper<BookChapter> {
}