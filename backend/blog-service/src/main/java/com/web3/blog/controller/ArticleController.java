/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.extension.plugins.pagination.Page
 *  com.web3.common.core.ApiResponse
 *  com.web3.common.core.PageResult
 *  com.web3.common.security.JwtUtil
 *  io.jsonwebtoken.Claims
 *  org.springframework.web.bind.annotation.DeleteMapping
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.PutMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestHeader
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.web3.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.web3.blog.dto.ArticleCreateDTO;
import com.web3.blog.service.ArticleService;
import com.web3.blog.vo.ArticleVO;
import com.web3.common.core.ApiResponse;
import com.web3.common.core.PageResult;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import io.jsonwebtoken.Claims;
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
@RequestMapping(value={"/article"})
/**
 * ArticleController —— 文章接口控制器
 * <p>
 * 所属模块：blog-service（博客模块）。
 * <p>
 * 职责：管理博客文章的对外 REST 接口，包括文章分页列表、热门文章、详情、创建、修改、删除、点赞。
 * 所有接口统一返回 ApiResponse 包装结构，需要登录的接口通过请求头 Authorization 携带 JWT 令牌。
 * <p>
 * 关键注解作用：
 * <ul>
 *   <li>@RestController：声明为 REST 风格控制器，方法返回值自动序列化为 JSON</li>
 *   <li>@RequestMapping("/article")：对外接口统一前缀为 /article，由网关转发给本服务</li>
 * </ul>
 */
public class ArticleController {
    /** 文章业务服务 */
    private final ArticleService articleService;
    /** JWT 令牌校验工具 */
    private final JwtUtil jwtUtil;
    /** JWT 配置属性（密钥、有效期等） */
    private final JwtProperties jwtProperties;

    public ArticleController(ArticleService articleService, JwtUtil jwtUtil, JwtProperties jwtProperties) {
        this.articleService = articleService;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 校验请求头中的令牌是否为管理员（ROLE_ADMIN）身份
     *
     * @param auth Authorization 请求头原始值（Bearer token）
     * @return true=管理员，false=非管理员或令牌无效
     */
    private boolean isAdmin(String auth) {
        try {
            String token = AuthUtils.resolveToken(auth, this.jwtProperties);
            if (!this.jwtUtil.validate(token)) {
                return false;
            }
            Claims claims = this.jwtUtil.parseClaims(token);
            return "ROLE_ADMIN".equals(claims.get("authorities", String.class));
        }
        catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * 文章分页列表（前端路由：GET /article/list）
     * 支持按标题关键字、分类、标签筛选，按置顶状态、创建时间倒序返回
     *
     * @param page     页码，默认 1
     * @param size     每页条数，默认 20
     * @param keyword  标题关键字，可为空
     * @param category 分类，可为空
     * @param tag      标签，可为空
     * @return 分页的文章视图对象列表
     */
    @GetMapping(value={"/list"})
    public ApiResponse<PageResult<ArticleVO>> list(@RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int size, @RequestParam(defaultValue="") String keyword, @RequestParam(defaultValue="") String category, @RequestParam(defaultValue="") String tag) {
        Page<ArticleVO> result = this.articleService.page(page, size, keyword, category, tag);
        return ApiResponse.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    /**
     * 相关推荐（前端路由：GET /article/{id}/related）
     * 同分类优先、其次同标签、最后按最新，排除自身
     *
     * @param id    文章 ID
     * @param limit 返回条数，默认 5
     * @return 推荐文章视图对象列表
     */
    @GetMapping(value={"/{id}/related"})
    public ApiResponse<List<ArticleVO>> related(@PathVariable Long id, @RequestParam(defaultValue="5") int limit) {
        return ApiResponse.ok(this.articleService.related(id, limit));
    }

    /**
     * 随机文章（前端路由：GET /article/random）
     * 随机返回一篇已发布文章
     *
     * @return 随机文章视图对象
     */
    @GetMapping(value={"/random"})
    public ApiResponse<ArticleVO> random() {
        return ApiResponse.ok(this.articleService.random());
    }

    /**
     * 归档（前端路由：GET /article/archives）
     * 按年月分组返回文章数，月份倒序
     *
     * @return 归档列表
     */
    @GetMapping(value={"/archives"})
    public ApiResponse<List<java.util.Map<String, Object>>> archives() {
        return ApiResponse.ok(this.articleService.archives());
    }

    /**
     * 分类列表（前端路由：GET /article/categories）
     * 分类名 + 文章数
     *
     * @return 分类列表
     */
    @GetMapping(value={"/categories"})
    public ApiResponse<List<java.util.Map<String, Object>>> categories() {
        return ApiResponse.ok(this.articleService.categories());
    }

    /**
     * 标签列表（前端路由：GET /article/tags）
     * 标签名 + 文章数，按文章数倒序
     *
     * @return 标签列表
     */
    @GetMapping(value={"/tags"})
    public ApiResponse<List<java.util.Map<String, Object>>> tags() {
        return ApiResponse.ok(this.articleService.tags());
    }

    /**
     * 站点资讯（前端路由：GET /article/stats）
     * 文章总数 / 全站字数 / 最后更新时间
     *
     * @return 统计信息
     */
    @GetMapping(value={"/stats"})
    public ApiResponse<java.util.Map<String, Object>> stats() {
        return ApiResponse.ok(this.articleService.stats());
    }

    /**
     * 最近更新（前端路由：GET /article/recent）
     * 按更新时间倒序返回最近文章
     *
     * @param limit 返回条数，默认 5
     * @return 最近文章视图对象列表
     */
    @GetMapping(value={"/recent"})
    public ApiResponse<List<ArticleVO>> recent(@RequestParam(defaultValue="5") int limit) {
        return ApiResponse.ok(this.articleService.recent(limit));
    }

    /**
     * 热门文章列表（前端路由：GET /article/hot）
     * 按浏览量倒序返回已发布的热门文章
     *
     * @param limit 返回条数，默认 5
     * @return 热门文章视图对象列表
     */
    @GetMapping(value={"/hot"})
    public ApiResponse<List<ArticleVO>> hot(@RequestParam(defaultValue="5") int limit) {
        return ApiResponse.ok(this.articleService.hotArticles(limit));
    }

    /**
     * 文章详情（前端路由：GET /article/{id}）
     * 查看详情时浏览量 +1；返回 {article, prevArticle, nextArticle}，文章不存在时返回 404
     *
     * @param id 文章 ID
     * @return 详情映射（含上一篇/下一篇），不存在则返回 404 错误
     */
    @GetMapping(value={"/{id}"})
    public ApiResponse<java.util.Map<String, Object>> detail(@PathVariable Long id) {
        java.util.Map<String, Object> result = this.articleService.getDetailWithNav(id);
        if (result == null) {
            return ApiResponse.fail((int)404, (String)"Article not found");
        }
        return ApiResponse.ok(result);
    }

    /**
     * 创建文章（前端路由：POST /article，需登录）
     * 从请求头令牌解析出作者 ID 后创建文章
     *
     * @param dto   文章创建参数（标题、内容、分类等）
     * @param token 请求头 Authorization 的 JWT 令牌
     * @return 创建成功后的文章视图对象
     */
    @PostMapping
    public ApiResponse<ArticleVO> create(@RequestBody ArticleCreateDTO dto, @RequestHeader(value="Authorization") String token) {
        Long authorId = AuthUtils.requireUserId(token, this.jwtUtil, this.jwtProperties);
        return ApiResponse.ok(this.articleService.create(dto, authorId));
    }

    /**
     * 更新文章（前端路由：PUT /article/{id}，需登录）
     * 仅作者本人或管理员可修改，否则返回 403 无权限错误
     *
     * @param id    文章 ID
     * @param dto   文章更新参数
     * @param token 请求头 Authorization 的 JWT 令牌
     * @return 更新后的文章视图对象，文章不存在时返回 404
     */
    @PutMapping(value={"/{id}"})
    public ApiResponse<ArticleVO> update(@PathVariable Long id, @RequestBody ArticleCreateDTO dto, @RequestHeader(value="Authorization") String token) {
        Long userId = AuthUtils.requireUserId(token, this.jwtUtil, this.jwtProperties);
        ArticleVO vo = this.articleService.update(id, dto, userId, this.isAdmin(token));
        if (vo == null) {
            return ApiResponse.fail((int)404, (String)"Article not found");
        }
        return ApiResponse.ok(vo);
    }

    /**
     * 删除文章（前端路由：DELETE /article/{id}，需登录）
     * 仅作者本人或管理员可删除（逻辑删除）
     *
     * @param id    文章 ID
     * @param token 请求头 Authorization 的 JWT 令牌
     * @return 成功提示
     */
    @DeleteMapping(value={"/{id}"})
    public ApiResponse<Object> delete(@PathVariable Long id, @RequestHeader(value="Authorization") String token) {
        Long userId = AuthUtils.requireUserId(token, this.jwtUtil, this.jwtProperties);
        this.articleService.delete(id, userId, this.isAdmin(token));
        return ApiResponse.ok();
    }

    /**
     * 点赞/取消点赞（前端路由：POST /article/{id}/like，需登录）
     * 若当前用户未点赞则点赞，已点赞则取消，为切换式操作
     *
     * @param id    文章 ID
     * @param token 请求头 Authorization 的 JWT 令牌
     * @return 成功提示
     */
    @PostMapping(value={"/{id}/like"})
    public ApiResponse<Object> like(@PathVariable Long id, @RequestHeader(value="Authorization") String token) {
        Long userId = AuthUtils.requireUserId(token, this.jwtUtil, this.jwtProperties);
        this.articleService.like(id, userId);
        return ApiResponse.ok();
    }

    /**
     * 收藏/取消收藏文章（切换式操作）
     */
    @PostMapping(value={"/{id}/favorite"})
    public ApiResponse<Object> favorite(@PathVariable Long id, @RequestHeader(value="Authorization") String token) {
        Long userId = AuthUtils.requireUserId(token, this.jwtUtil, this.jwtProperties);
        this.articleService.favorite(id, userId);
        return ApiResponse.ok();
    }

    /**
     * 检查当前用户是否已收藏该文章
     */
    @GetMapping(value={"/{id}/favorite/check"})
    public ApiResponse<java.util.Map<String, Object>> checkFavorite(@PathVariable Long id, @RequestHeader(value="Authorization", required=false) String token) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        if (token == null || token.isEmpty()) {
            result.put("favorited", false);
            return ApiResponse.ok(result);
        }
        try {
            Long userId = AuthUtils.requireUserId(token, this.jwtUtil, this.jwtProperties);
            result.put("favorited", this.articleService.isFavorited(id, userId));
        } catch (Exception e) {
            result.put("favorited", false);
        }
        return ApiResponse.ok(result);
    }

    /**
     * 检查当前用户是否已点赞该文章
     */
    @GetMapping(value={"/{id}/like/check"})
    public ApiResponse<java.util.Map<String, Object>> checkLike(@PathVariable Long id, @RequestHeader(value="Authorization", required=false) String token) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        if (token == null || token.isEmpty()) {
            result.put("liked", false);
            return ApiResponse.ok(result);
        }
        try {
            Long userId = AuthUtils.requireUserId(token, this.jwtUtil, this.jwtProperties);
            result.put("liked", this.articleService.isLiked(id, userId));
        } catch (Exception e) {
            result.put("liked", false);
        }
        return ApiResponse.ok(result);
    }

    /**
     * 获取当前用户收藏的文章列表
     */
    @GetMapping(value={"/user/favorites"})
    public ApiResponse<List<ArticleVO>> userFavorites(@RequestHeader(value="Authorization") String token) {
        Long userId = AuthUtils.requireUserId(token, this.jwtUtil, this.jwtProperties);
        return ApiResponse.ok(this.articleService.getUserFavorites(userId));
    }

    /**
     * 获取当前用户点赞的文章列表
     */
    @GetMapping(value={"/user/likes"})
    public ApiResponse<List<ArticleVO>> userLikes(@RequestHeader(value="Authorization") String token) {
        Long userId = AuthUtils.requireUserId(token, this.jwtUtil, this.jwtProperties);
        return ApiResponse.ok(this.articleService.getUserLikes(userId));
    }
}
