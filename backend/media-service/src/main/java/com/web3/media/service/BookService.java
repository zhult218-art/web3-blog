package com.web3.media.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.web3.common.core.PageResult;
import com.web3.media.entity.Book;
import com.web3.media.entity.BookChapter;
import com.web3.media.mapper.BookChapterMapper;
import com.web3.media.mapper.BookMapper;
import com.web3.media.vo.BookChapterVO;
import com.web3.media.vo.BookVO;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

/**
 * BookService —— 书籍业务服务
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 职责：实现书籍与章节的核心业务逻辑，包括书籍分页查询、详情、章节列表/内容读取、
 * 书籍与章节的增删改（管理员操作，维护书籍章节总数），以及实体到视图对象的转换。
 * <p>
 * 关键注解作用：
 * <ul>
 *   <li>@Service：声明为 Spring 业务组件</li>
 *   <li>继承 ServiceImpl&lt;BookMapper, Book&gt;：获得 MyBatis-Plus 内置 CRUD 能力</li>
 * </ul>
 */
@Service
public class BookService extends ServiceImpl<BookMapper, Book> {
    /** 书籍章节 Mapper */
    private final BookChapterMapper chapterMapper;

    public BookService(BookChapterMapper chapterMapper) {
        this.chapterMapper = chapterMapper;
    }

    /**
     * 分页查询书籍列表，支持书名/作者/分类关键字筛选，按创建时间倒序
     *
     * @param page    页码（从 1 开始）
     * @param size    每页条数
     * @param keyword 书名/作者/分类关键字，可为空
     * @return 书籍视图对象的分页结果
     */
    public PageResult<BookVO> page(int page, int size, String keyword) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<Book>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Book::getTitle, keyword).or().like(Book::getAuthor, keyword).or().like(Book::getCategory, keyword));
        }
        wrapper.orderByDesc(Book::getCreatedAt);
        Page<Book> mpPage = new Page<Book>((long)page, (long)size);
        Page<Book> result = this.baseMapper.selectPage(mpPage, wrapper);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
    }

    /**
     * 查询书籍详情，不存在时抛异常
     *
     * @param id 书籍 ID
     * @return 书籍视图对象
     */
    public BookVO getDetail(Long id) {
        Book book = this.baseMapper.selectById(id);
        if (book == null) {
            throw new RuntimeException("\u4e66\u7c4d\u4e0d\u5b58\u5728");
        }
        return this.toVO(book);
    }

    /**
     * 获取书籍的章节目录（按章节号正序，不返回正文内容）
     *
     * @param bookId 书籍 ID
     * @return 章节视图对象列表
     */
    public List<BookChapterVO> getChapters(Long bookId) {
        LambdaQueryWrapper<BookChapter> wrapper = new LambdaQueryWrapper<BookChapter>();
        wrapper.eq(BookChapter::getBookId, bookId).orderByAsc(BookChapter::getChapterNo);
        return this.chapterMapper.selectList(wrapper).stream().map(c -> {
            BookChapterVO vo = new BookChapterVO();
            BeanUtils.copyProperties(c, vo);
            vo.setContent(null);
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 读取某章节的完整内容
     *
     * @param bookId    书籍 ID（书籍不存在时抛异常）
     * @param chapterNo 章节号（章节不存在时抛异常）
     * @return 含正文的章节视图对象
     */
    public BookChapterVO getChapter(Long bookId, Integer chapterNo) {
        if (this.baseMapper.selectById(bookId) == null) {
            throw new RuntimeException("\u4e66\u7c4d\u4e0d\u5b58\u5728");
        }
        LambdaQueryWrapper<BookChapter> wrapper = new LambdaQueryWrapper<BookChapter>();
        wrapper.eq(BookChapter::getBookId, bookId).eq(BookChapter::getChapterNo, chapterNo);
        BookChapter chapter = this.chapterMapper.selectOne(wrapper);
        if (chapter == null) {
            throw new RuntimeException("\u7ae0\u8282\u4e0d\u5b58\u5728");
        }
        BookChapterVO vo = new BookChapterVO();
        BeanUtils.copyProperties(chapter, vo);
        return vo;
    }

    /**
     * 创建书籍（管理员操作），章节总数缺省为 0
     *
     * @param book 书籍实体
     * @return 创建成功后的书籍视图对象
     */
    public BookVO create(Book book) {
        if (book.getChapterCount() == null) {
            book.setChapterCount(0);
        }
        this.baseMapper.insert(book);
        return this.toVO(book);
    }

    /**
     * 更新书籍（管理员操作），不存在时抛异常
     *
     * @param id   书籍 ID
     * @param book 书籍实体（含需更新的字段）
     * @return 更新后的书籍视图对象
     */
    public BookVO update(Long id, Book book) {
        Book exist = this.baseMapper.selectById(id);
        if (exist == null) {
            throw new RuntimeException("\u4e66\u7c4d\u4e0d\u5b58\u5728");
        }
        book.setId(id);
        this.baseMapper.updateById(book);
        return this.toVO(this.baseMapper.selectById(id));
    }

    /**
     * 删除书籍及其全部章节（管理员操作），不存在时抛异常
     *
     * @param id 书籍 ID
     */
    public void delete(Long id) {
        Book exist = this.baseMapper.selectById(id);
        if (exist == null) {
            throw new RuntimeException("\u4e66\u7c4d\u4e0d\u5b58\u5728");
        }
        LambdaQueryWrapper<BookChapter> wrapper = new LambdaQueryWrapper<BookChapter>();
        wrapper.eq(BookChapter::getBookId, id);
        this.chapterMapper.delete(wrapper);
        this.baseMapper.deleteById(id);
    }

    /**
     * 为书籍新增章节（管理员操作）
     * 章节号缺省自动取最大章节号 +1；章节标题缺省为"第X章"；按内容长度记录字数；
     * 创建成功后同步书籍章节总数
     *
     * @param bookId  书籍 ID（不存在时抛异常）
     * @param chapter 章节实体
     * @return 创建成功后的章节视图对象
     */
    public BookChapterVO createChapter(Long bookId, BookChapter chapter) {
        Book book = this.baseMapper.selectById(bookId);
        if (book == null) {
            throw new RuntimeException("\u4e66\u7c4d\u4e0d\u5b58\u5728");
        }
        chapter.setId(null);
        chapter.setBookId(bookId);
        if (chapter.getChapterNo() == null) {
            LambdaQueryWrapper<BookChapter> wrapper = new LambdaQueryWrapper<BookChapter>();
            wrapper.eq(BookChapter::getBookId, bookId).orderByDesc(BookChapter::getChapterNo).last("limit 1");
            BookChapter last = this.chapterMapper.selectList(wrapper).stream().findFirst().orElse(null);
            chapter.setChapterNo(last == null ? 1 : last.getChapterNo() + 1);
        }
        if (chapter.getChapterTitle() == null || chapter.getChapterTitle().isEmpty()) {
            chapter.setChapterTitle("\u7b2c" + chapter.getChapterNo() + "\u7ae0");
        }
        if (chapter.getContent() != null) {
            chapter.setWordCount(chapter.getContent().length());
        }
        this.chapterMapper.insert(chapter);
        int count = countChapters(bookId);
        book.setChapterCount(count);
        this.baseMapper.updateById(book);
        return this.toChapterVO(chapter);
    }

    /**
     * 更新章节（管理员操作），不存在时抛异常；按内容长度重新记录字数
     *
     * @param id      章节 ID
     * @param chapter 章节实体（含需更新的字段）
     * @return 更新后的章节视图对象
     */
    public BookChapterVO updateChapter(Long id, BookChapter chapter) {
        BookChapter exist = this.chapterMapper.selectById(id);
        if (exist == null) {
            throw new RuntimeException("\u7ae0\u8282\u4e0d\u5b58\u5728");
        }
        chapter.setId(id);
        chapter.setBookId(exist.getBookId());
        if (chapter.getContent() != null) {
            chapter.setWordCount(chapter.getContent().length());
        }
        this.chapterMapper.updateById(chapter);
        return this.toChapterVO(this.chapterMapper.selectById(id));
    }

    /**
     * 删除章节（管理员操作），不存在时抛异常；删除后同步所属书籍章节总数
     *
     * @param id 章节 ID
     */
    public void deleteChapter(Long id) {
        BookChapter exist = this.chapterMapper.selectById(id);
        if (exist == null) {
            throw new RuntimeException("\u7ae0\u8282\u4e0d\u5b58\u5728");
        }
        this.chapterMapper.deleteById(id);
        Book book = this.baseMapper.selectById(exist.getBookId());
        if (book != null) {
            book.setChapterCount(countChapters(exist.getBookId()));
            this.baseMapper.updateById(book);
        }
    }

    /**
     * 统计书籍的章节总数（内部工具方法）
     *
     * @param bookId 书籍 ID
     * @return 章节数
     */
    private int countChapters(Long bookId) {
        LambdaQueryWrapper<BookChapter> wrapper = new LambdaQueryWrapper<BookChapter>();
        wrapper.eq(BookChapter::getBookId, bookId);
        return Math.toIntExact(this.chapterMapper.selectCount(wrapper));
    }

    /**
     * 书籍实体转视图对象（内部工具方法）
     *
     * @param book 书籍实体
     * @return 书籍视图对象
     */
    private BookVO toVO(Book book) {
        BookVO vo = new BookVO();
        BeanUtils.copyProperties(book, vo);
        return vo;
    }

    /**
     * 章节实体转视图对象（内部工具方法）
     *
     * @param chapter 章节实体
     * @return 章节视图对象
     */
    private BookChapterVO toChapterVO(BookChapter chapter) {
        BookChapterVO vo = new BookChapterVO();
        BeanUtils.copyProperties(chapter, vo);
        return vo;
    }
}