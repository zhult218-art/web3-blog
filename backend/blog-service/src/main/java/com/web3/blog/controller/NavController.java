package com.web3.blog.controller;

import com.web3.blog.entity.NavCategory;
import com.web3.blog.entity.NavLink;
import com.web3.blog.service.NavService;
import com.web3.common.core.ApiResponse;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * NavController —— 导航数据接口控制器（/blog 前缀，经网关转发）
 * <p>
 * 所属模块：blog-service（博客模块）。
 * <p>
 * 职责：为「冒险者招募亭 /jobs」与「赏金猎人 /earn」两个导航页提供数据接口。
 * 公开接口 GET /blog/nav/{page} 供前端页面读取；/blog/admin/nav/** 为管理端接口，
 * 需要管理员身份（通过 AuthUtils.requireAdmin 校验 Authorization 头中的 JWT）。
 * <p>
 * 关键注解作用：
 * <ul>
 *   <li>@RestController：声明为 REST 控制器，返回值自动序列化为 JSON；</li>
 *   <li>@RequestMapping("/blog")：统一前缀，由网关路由转发给本服务。</li>
 * </ul>
 */
@RestController
@RequestMapping(value = {"/blog"})
public class NavController {

    /** 导航数据业务服务 */
    private final NavService navService;
    /** JWT 令牌校验工具 */
    private final JwtUtil jwtUtil;
    /** JWT 配置属性 */
    private final JwtProperties jwtProperties;

    public NavController(NavService navService, JwtUtil jwtUtil, JwtProperties jwtProperties) {
        this.navService = navService;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 公开：读取指定导航页的全部可见数据（前端路由：GET /blog/nav/{page}）
     *
     * @param page 页面标识（jobs / earn）
     * @return 分类与条目数据
     */
    @GetMapping(value = {"/nav/{page}"})
    public ApiResponse<Map<String, Object>> publicPage(@PathVariable String page) {
        return ApiResponse.ok(navService.publicPage(page));
    }

    /**
     * 管理端：分类列表（前端路由：GET /blog/admin/nav/{page}/categories）
     *
     * @param token 管理员 JWT
     * @param page  页面标识
     * @return 含未启用分类在内的全部分类
     */
    @GetMapping(value = {"/admin/nav/{page}/categories"})
    public ApiResponse<List<NavCategory>> adminCategories(@RequestHeader(value = "Authorization") String token,
                                                          @PathVariable String page) {
        AuthUtils.requireAdmin(token, this.jwtUtil, this.jwtProperties);
        return ApiResponse.ok(navService.adminCategories(page));
    }

    /**
     * 管理端：条目列表（前端路由：GET /blog/admin/nav/{page}/links）
     *
     * @param token 管理员 JWT
     * @param page  页面标识
     * @return 含未启用条目在内的全部条目
     */
    @GetMapping(value = {"/admin/nav/{page}/links"})
    public ApiResponse<List<NavLink>> adminLinks(@RequestHeader(value = "Authorization") String token,
                                                 @PathVariable String page) {
        AuthUtils.requireAdmin(token, this.jwtUtil, this.jwtProperties);
        return ApiResponse.ok(navService.adminLinks(page));
    }

    /**
     * 管理端：新增分类（前端路由：POST /blog/admin/nav/category）
     *
     * @param token 管理员 JWT
     * @param body  分类数据
     * @return 新增后的分类
     */
    @PostMapping(value = {"/admin/nav/category"})
    public ApiResponse<NavCategory> createCategory(@RequestHeader(value = "Authorization") String token,
                                                   @RequestBody NavCategory body) {
        AuthUtils.requireAdmin(token, this.jwtUtil, this.jwtProperties);
        return ApiResponse.ok(navService.createCategory(body));
    }

    /**
     * 管理端：编辑分类（前端路由：PUT /blog/admin/nav/category/{id}）
     *
     * @param token 管理员 JWT
     * @param id    分类 ID
     * @param body  分类数据
     * @return 更新后的分类
     */
    @PutMapping(value = {"/admin/nav/category/{id}"})
    public ApiResponse<NavCategory> updateCategory(@RequestHeader(value = "Authorization") String token,
                                                   @PathVariable Long id,
                                                   @RequestBody NavCategory body) {
        AuthUtils.requireAdmin(token, this.jwtUtil, this.jwtProperties);
        return ApiResponse.ok(navService.updateCategory(id, body));
    }

    /**
     * 管理端：删除分类（前端路由：DELETE /blog/admin/nav/category/{id}）
     *
     * @param token 管理员 JWT
     * @param id    分类 ID
     * @return 操作结果
     */
    @DeleteMapping(value = {"/admin/nav/category/{id}"})
    public ApiResponse<Void> deleteCategory(@RequestHeader(value = "Authorization") String token,
                                            @PathVariable Long id) {
        AuthUtils.requireAdmin(token, this.jwtUtil, this.jwtProperties);
        navService.deleteCategory(id);
        return ApiResponse.ok();
    }

    /**
     * 管理端：新增条目（前端路由：POST /blog/admin/nav/link）
     *
     * @param token 管理员 JWT
     * @param body  条目数据
     * @return 新增后的条目
     */
    @PostMapping(value = {"/admin/nav/link"})
    public ApiResponse<NavLink> createLink(@RequestHeader(value = "Authorization") String token,
                                           @RequestBody NavLink body) {
        AuthUtils.requireAdmin(token, this.jwtUtil, this.jwtProperties);
        return ApiResponse.ok(navService.createLink(body));
    }

    /**
     * 管理端：编辑条目（前端路由：PUT /blog/admin/nav/link/{id}）
     *
     * @param token 管理员 JWT
     * @param id    条目 ID
     * @param body  条目数据
     * @return 更新后的条目
     */
    @PutMapping(value = {"/admin/nav/link/{id}"})
    public ApiResponse<NavLink> updateLink(@RequestHeader(value = "Authorization") String token,
                                           @PathVariable Long id,
                                           @RequestBody NavLink body) {
        AuthUtils.requireAdmin(token, this.jwtUtil, this.jwtProperties);
        return ApiResponse.ok(navService.updateLink(id, body));
    }

    /**
     * 管理端：删除条目（前端路由：DELETE /blog/admin/nav/link/{id}）
     *
     * @param token 管理员 JWT
     * @param id    条目 ID
     * @return 操作结果
     */
    @DeleteMapping(value = {"/admin/nav/link/{id}"})
    public ApiResponse<Void> deleteLink(@RequestHeader(value = "Authorization") String token,
                                        @PathVariable Long id) {
        AuthUtils.requireAdmin(token, this.jwtUtil, this.jwtProperties);
        navService.deleteLink(id);
        return ApiResponse.ok();
    }

    /**
     * 管理端：一键抓取官网图标（前端路由：POST /blog/admin/nav/{page}/fetch-icons）
     * 为尚无 iconUrl 的条目解析官网 favicon 并持久化写回。
     *
     * @param token 管理员 JWT
     * @param page  页面标识
     * @return 含 total（待处理数）与 updated（成功写入数）的结果
     */
    @PostMapping(value = {"/admin/nav/{page}/fetch-icons"})
    public ApiResponse<Map<String, Object>> fetchIcons(@RequestHeader(value = "Authorization") String token,
                                                       @PathVariable String page) {
        AuthUtils.requireAdmin(token, this.jwtUtil, this.jwtProperties);
        return ApiResponse.ok(navService.fetchIcons(page));
    }
}
