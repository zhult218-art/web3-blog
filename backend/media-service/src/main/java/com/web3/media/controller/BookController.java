package com.web3.media.controller;

import com.web3.common.core.ApiResponse;
import com.web3.common.core.PageResult;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import com.web3.media.entity.Book;
import com.web3.media.entity.BookChapter;
import com.web3.media.service.BookService;
import com.web3.media.vo.BookChapterVO;
import com.web3.media.vo.BookVO;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/book"})
/**
 * BookController —— 书籍接口控制器
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 职责：管理书籍与章节的对外 REST 接口，包括书籍分页列表、详情、章节列表、
 * 章节内容读取，以及书籍/章节的增删改（仅管理员）。
 * <p>
 * 关键注解作用：
 * <ul>
 *   <li>@RestController：声明为 REST 控制器，返回值自动序列化为 JSON</li>
 *   <li>@RequestMapping("/book")：对外接口统一前缀为 /book</li>
 * </ul>
 */
public class BookController {
    /** 书籍业务服务 */
    private final BookService bookService;
    /** JWT 令牌校验工具 */
    private final JwtUtil jwtUtil;
    /** JWT 配置属性 */
    private final JwtProperties jwtProperties;

    public BookController(BookService bookService, JwtUtil jwtUtil, JwtProperties jwtProperties) {
        this.bookService = bookService;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 书籍分页列表（前端路由：GET /book/list）
     * 支持按书名/作者/分类关键字筛选，按创建时间倒序返回
     *
     * @param page    页码，默认 1
     * @param size    每页条数，默认 20
     * @param keyword 书名/作者/分类关键字，可为空
     * @return 分页的书籍视图对象列表
     */
    @GetMapping({"/list"})
    public ApiResponse<PageResult<BookVO>> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size, @RequestParam(defaultValue = "") String keyword) {
        return ApiResponse.ok(this.bookService.page(page, size, keyword));
    }

    /**
     * 书籍详情（前端路由：GET /book/{id}）
     *
     * @param id 书籍 ID
     * @return 书籍视图对象
     */
    @GetMapping({"/{id}"})
    public ApiResponse<BookVO> getById(@PathVariable Long id) {
        return ApiResponse.ok(this.bookService.getDetail(id));
    }

    /**
     * 书籍章节目录列表（前端路由：GET /book/{id}/chapters）
     * 按章节号正序返回（不含章节正文，减轻传输负担）
     *
     * @param id 书籍 ID
     * @return 章节视图对象列表
     */
    @GetMapping({"/{id}/chapters"})
    public ApiResponse<List<BookChapterVO>> chapters(@PathVariable Long id) {
        return ApiResponse.ok(this.bookService.getChapters(id));
    }

    /**
     * 读取某个章节内容（前端路由：GET /book/{id}/chapter/{chapterNo}）
     *
     * @param id        书籍 ID
     * @param chapterNo 章节号
     * @return 含正文的章节视图对象
     */
    @GetMapping({"/{id}/chapter/{chapterNo}"})
    public ApiResponse<BookChapterVO> chapter(@PathVariable Long id, @PathVariable Integer chapterNo) {
        return ApiResponse.ok(this.bookService.getChapter(id, chapterNo));
    }

    /**
     * 创建书籍（前端路由：POST /book，需管理员）
     *
     * @param book         书籍实体（JSON 请求体）
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 创建成功后的书籍视图对象
     */
    @PostMapping
    public ApiResponse<BookVO> create(@RequestBody Book book, @RequestHeader(value = "Authorization") String authorization) {
        AuthUtils.requireAdmin(authorization, this.jwtUtil, this.jwtProperties);
        return ApiResponse.ok(this.bookService.create(book));
    }

    /**
     * 更新书籍（前端路由：PUT /book/{id}，需管理员）
     *
     * @param id           书籍 ID
     * @param book         书籍实体（JSON 请求体）
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 更新后的书籍视图对象
     */
    @PutMapping({"/{id}"})
    public ApiResponse<BookVO> update(@PathVariable Long id, @RequestBody Book book, @RequestHeader(value = "Authorization") String authorization) {
        AuthUtils.requireAdmin(authorization, this.jwtUtil, this.jwtProperties);
        return ApiResponse.ok(this.bookService.update(id, book));
    }

    /**
     * 删除书籍及其所有章节（前端路由：DELETE /book/{id}，需管理员）
     *
     * @param id           书籍 ID
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 成功提示
     */
    @DeleteMapping({"/{id}"})
    public ApiResponse<Object> delete(@PathVariable Long id, @RequestHeader(value = "Authorization") String authorization) {
        AuthUtils.requireAdmin(authorization, this.jwtUtil, this.jwtProperties);
        this.bookService.delete(id);
        return ApiResponse.ok();
    }

    /**
     * 为书籍新增章节（前端路由：POST /book/{id}/chapters，需管理员）
     * 章节号缺省时自动取最大章节号 +1；同时更新书籍的章节总数
     *
     * @param id           书籍 ID
     * @param chapter      章节实体（JSON 请求体）
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 创建成功后的章节视图对象
     */
    @PostMapping({"/{id}/chapters"})
    public ApiResponse<BookChapterVO> createChapter(@PathVariable Long id, @RequestBody BookChapter chapter, @RequestHeader(value = "Authorization") String authorization) {
        AuthUtils.requireAdmin(authorization, this.jwtUtil, this.jwtProperties);
        return ApiResponse.ok(this.bookService.createChapter(id, chapter));
    }

    /**
     * 更新章节（前端路由：PUT /book/chapters/{chapterId}，需管理员）
     *
     * @param chapterId     章节 ID
     * @param chapter       章节实体（JSON 请求体）
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 更新后的章节视图对象
     */
    @PutMapping({"/chapters/{chapterId}"})
    public ApiResponse<BookChapterVO> updateChapter(@PathVariable Long chapterId, @RequestBody BookChapter chapter, @RequestHeader(value = "Authorization") String authorization) {
        AuthUtils.requireAdmin(authorization, this.jwtUtil, this.jwtProperties);
        return ApiResponse.ok(this.bookService.updateChapter(chapterId, chapter));
    }

    /**
     * 删除章节（前端路由：DELETE /book/chapters/{chapterId}，需管理员）
     * 删除后同步更新所属书籍的章节总数
     *
     * @param chapterId     章节 ID
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 成功提示
     */
    @DeleteMapping({"/chapters/{chapterId}"})
    public ApiResponse<Object> deleteChapter(@PathVariable Long chapterId, @RequestHeader(value = "Authorization") String authorization) {
        AuthUtils.requireAdmin(authorization, this.jwtUtil, this.jwtProperties);
        this.bookService.deleteChapter(chapterId);
        return ApiResponse.ok();
    }
}