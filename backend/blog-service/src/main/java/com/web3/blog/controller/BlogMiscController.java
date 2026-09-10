package com.web3.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.web3.blog.entity.BlogSetting;
import com.web3.blog.entity.FriendLink;
import com.web3.blog.entity.SiteNotice;
import com.web3.blog.mapper.BlogSettingMapper;
import com.web3.blog.mapper.FriendLinkMapper;
import com.web3.blog.mapper.SiteNoticeMapper;
import com.web3.common.core.ApiResponse;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import java.util.HashMap;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * BlogMiscController —— 博客辅助接口控制器（/blog 前缀，经网关转发）
 * <p>
 * 所属模块：blog-service（博客模块）。
 * <p>
 * 职责：提供友链列表/申请、公告、站点公开配置等博客增强接口，
 * 对应移植自 lololowe 博客的侧栏与页脚能力。所有接口公开可读，无需登录。
 * <p>
 * 关键注解作用：
 * <ul>
 *   <li>@RestController：声明为 REST 控制器，返回值自动序列化为 JSON</li>
 *   <li>@RequestMapping("/blog")：对外接口统一前缀为 /blog，由网关路由转发给本服务</li>
 * </ul>
 */
@RestController
@RequestMapping(value={"/blog"})
public class BlogMiscController {
    /** 友链表数据访问 */
    private final FriendLinkMapper friendLinkMapper;
    /** 公告表数据访问 */
    private final SiteNoticeMapper siteNoticeMapper;
    /** 配置表数据访问 */
    private final BlogSettingMapper blogSettingMapper;
    /** JWT 令牌校验工具 */
    private final JwtUtil jwtUtil;
    /** JWT 配置属性 */
    private final JwtProperties jwtProperties;

    public BlogMiscController(FriendLinkMapper friendLinkMapper, SiteNoticeMapper siteNoticeMapper, BlogSettingMapper blogSettingMapper, JwtUtil jwtUtil, JwtProperties jwtProperties) {
        this.friendLinkMapper = friendLinkMapper;
        this.siteNoticeMapper = siteNoticeMapper;
        this.blogSettingMapper = blogSettingMapper;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 友链列表（前端路由：GET /blog/links）
     * 仅返回已发布友链，按排序值升序；可按分组过滤
     *
     * @param group 分组名，可为空（空=全部）
     * @return 友链列表
     */
    @GetMapping(value={"/links"})
    public ApiResponse<List<FriendLink>> links(@RequestParam(defaultValue="") String group) {
        LambdaQueryWrapper<FriendLink> wrapper = new LambdaQueryWrapper<FriendLink>();
        wrapper.eq(FriendLink::getStatus, (Object)"PUBLISHED");
        if (group != null && !group.isBlank()) {
            wrapper.eq(FriendLink::getGroupName, (Object)group);
        }
        wrapper.orderByAsc(FriendLink::getSort).orderByDesc(FriendLink::getCreatedAt);
        return ApiResponse.ok(this.friendLinkMapper.selectList(wrapper));
    }

    /**
     * 申请友链（前端路由：POST /blog/links/apply）
     * 访客提交友链申请，状态为 PENDING，需管理员在后台审核
     *
     * @param link 友链信息（名称、链接、头像、描述、分组）
     * @return 成功提示
     */
    @PostMapping(value={"/links/apply"})
    public ApiResponse<Object> apply(@RequestBody FriendLink link) {
        if (link.getName() == null || link.getName().isBlank() || link.getUrl() == null || link.getUrl().isBlank()) {
            return ApiResponse.fail(400, "名称与链接不能为空");
        }
        link.setId(null);
        link.setStatus("PENDING");
        link.setSort(0);
        this.friendLinkMapper.insert(link);
        return ApiResponse.ok();
    }

    /**
     * 当前启用公告（前端路由：GET /blog/notice）
     * 返回最新一条启用中的公告
     *
     * @return 公告对象；无公告时返回 null
     */
    @GetMapping(value={"/notice"})
    public ApiResponse<SiteNotice> notice() {
        LambdaQueryWrapper<SiteNotice> wrapper = new LambdaQueryWrapper<SiteNotice>();
        wrapper.eq(SiteNotice::getEnabled, (Object)1);
        wrapper.orderByDesc(SiteNotice::getCreatedAt);
        wrapper.last("LIMIT 1");
        return ApiResponse.ok(this.siteNoticeMapper.selectOne(wrapper, false));
    }

    /**
     * 公开公告列表（前端路由：GET /blog/notice/list）
     * 返回全部启用中的公告，按创建时间倒序（含最新一条）
     *
     * @return 启用公告列表；无时返回空列表
     */
    @GetMapping(value={"/notice/list"})
    public ApiResponse<List<SiteNotice>> noticeListPublic() {
        LambdaQueryWrapper<SiteNotice> wrapper = new LambdaQueryWrapper<SiteNotice>();
        wrapper.eq(SiteNotice::getEnabled, (Object)1);
        wrapper.orderByDesc(SiteNotice::getCreatedAt);
        return ApiResponse.ok(this.siteNoticeMapper.selectList(wrapper));
    }

    /**
     * 站点公开配置（前端路由：GET /blog/settings）
     * 返回作者信息/打赏/页脚友链/ICP 等公开配置的 JSON 映射
     *
     * @return 配置键值映射
     */
    @GetMapping(value={"/settings"})
    public ApiResponse<Map<String, String>> settings() {
        List<BlogSetting> all = this.blogSettingMapper.selectList(new LambdaQueryWrapper<BlogSetting>());
        Map<String, String> map = new HashMap<String, String>();
        for (BlogSetting s : all) {
            map.put(s.getSettingKey(), s.getSettingValue());
        }
        return ApiResponse.ok(map);
    }

    // ============================================================
    // 管理端接口（全部要求 ADMIN 角色，经网关 /blog 透传）
    // ============================================================

    /**
     * 友链管理列表（前端路由：GET /blog/admin/links）
     * 返回全部友链（含 PENDING / DISABLED），按排序值与创建时间倒序
     *
     * @param token Authorization 请求头（管理员）
     * @return 全部友链列表
     */
    @GetMapping(value={"/admin/links"})
    public ApiResponse<List<FriendLink>> adminLinks(@RequestHeader(value="Authorization") String token) {
        AuthUtils.requireAdmin(token, this.jwtUtil, this.jwtProperties);
        LambdaQueryWrapper<FriendLink> wrapper = new LambdaQueryWrapper<FriendLink>();
        wrapper.orderByAsc(FriendLink::getSort).orderByDesc(FriendLink::getCreatedAt);
        return ApiResponse.ok(this.friendLinkMapper.selectList(wrapper));
    }

    /**
     * 友链新增（前端路由：POST /blog/admin/links）
     *
     * @param link  友链信息（名称、链接、头像、描述、分组、状态、排序）
     * @param token Authorization 请求头（管理员）
     * @return 创建后的友链
     */
    @PostMapping(value={"/admin/links"})
    public ApiResponse<FriendLink> createLink(@RequestBody FriendLink link, @RequestHeader(value="Authorization") String token) {
        AuthUtils.requireAdmin(token, this.jwtUtil, this.jwtProperties);
        if (link.getName() == null || link.getName().isBlank() || link.getUrl() == null || link.getUrl().isBlank()) {
            return ApiResponse.fail(400, "名称与链接不能为空");
        }
        link.setId(null);
        if (link.getStatus() == null) {
            link.setStatus("PUBLISHED");
        }
        if (link.getSort() == null) {
            link.setSort(Integer.valueOf(0));
        }
        this.friendLinkMapper.insert(link);
        return ApiResponse.ok((FriendLink)this.friendLinkMapper.selectById(link.getId()));
    }

    /**
     * 友链编辑/审核（前端路由：PUT /blog/admin/links/{id}）
     * 审核（status 置 PUBLISHED/PENDING/DISABLED）、编辑名称/链接/头像/描述/分组/排序
     *
     * @param id    友链 ID
     * @param link  更新字段
     * @param token Authorization 请求头（管理员）
     * @return 更新后的友链
     */
    @PutMapping(value={"/admin/links/{id}"})
    public ApiResponse<FriendLink> updateLink(@PathVariable Long id, @RequestBody FriendLink link, @RequestHeader(value="Authorization") String token) {
        AuthUtils.requireAdmin(token, this.jwtUtil, this.jwtProperties);
        if (this.friendLinkMapper.selectById(id) == null) {
            return ApiResponse.fail(404, "友链不存在");
        }
        link.setId(id);
        this.friendLinkMapper.updateById(link);
        return ApiResponse.ok((FriendLink)this.friendLinkMapper.selectById(id));
    }

    /**
     * 友链删除（前端路由：DELETE /blog/admin/links/{id}）
     *
     * @param id    友链 ID
     * @param token Authorization 请求头（管理员）
     * @return 成功提示
     */
    @DeleteMapping(value={"/admin/links/{id}"})
    public ApiResponse<Object> deleteLink(@PathVariable Long id, @RequestHeader(value="Authorization") String token) {
        AuthUtils.requireAdmin(token, this.jwtUtil, this.jwtProperties);
        this.friendLinkMapper.deleteById(id);
        return ApiResponse.ok();
    }

    /**
     * 公告保存/编辑（前端路由：PUT /blog/admin/notice）
     * 有 id 则更新，无 id 则新增；enabled 控制启用状态
     *
     * @param notice 公告内容（含状态）
     * @param token  Authorization 请求头（管理员）
     * @return 保存后的公告
     */
    @PutMapping(value={"/admin/notice"})
    public ApiResponse<SiteNotice> saveNotice(@RequestBody SiteNotice notice, @RequestHeader(value="Authorization") String token) {
        AuthUtils.requireAdmin(token, this.jwtUtil, this.jwtProperties);
        if (notice.getContent() == null || notice.getContent().isBlank()) {
            return ApiResponse.fail(400, "公告内容不能为空");
        }
        if (notice.getId() != null && this.siteNoticeMapper.selectById(notice.getId()) != null) {
            this.siteNoticeMapper.updateById(notice);
        }
        else {
            notice.setId(null);
            if (notice.getEnabled() == null) {
                notice.setEnabled(Integer.valueOf(1));
            }
            this.siteNoticeMapper.insert(notice);
        }
        return ApiResponse.ok((SiteNotice)this.siteNoticeMapper.selectById(notice.getId()));
    }

    /**
     * 公告列表（前端路由：GET /blog/admin/notice/list）
     * 返回全部公告（含停用）
     *
     * @param token Authorization 请求头（管理员）
     * @return 全部公告列表
     */
    @GetMapping(value={"/admin/notice/list"})
    public ApiResponse<List<SiteNotice>> noticeList(@RequestHeader(value="Authorization") String token) {
        AuthUtils.requireAdmin(token, this.jwtUtil, this.jwtProperties);
        LambdaQueryWrapper<SiteNotice> wrapper = new LambdaQueryWrapper<SiteNotice>();
        wrapper.orderByDesc(SiteNotice::getCreatedAt);
        return ApiResponse.ok(this.siteNoticeMapper.selectList(wrapper));
    }

    /**
     * 公告删除（前端路由：DELETE /blog/admin/notice/{id}）
     *
     * @param id    公告 ID
     * @param token Authorization 请求头（管理员）
     * @return 成功提示
     */
    @DeleteMapping(value={"/admin/notice/{id}"})
    public ApiResponse<Object> deleteNotice(@PathVariable Long id, @RequestHeader(value="Authorization") String token) {
        AuthUtils.requireAdmin(token, this.jwtUtil, this.jwtProperties);
        this.siteNoticeMapper.deleteById(id);
        return ApiResponse.ok();
    }

    /**
     * 站点配置批量保存（前端路由：PUT /blog/admin/settings）
     * body 为键值映射，已存在则更新，不存在则新增
     *
     * @param settings 配置键值映射
     * @param token    Authorization 请求头（管理员）
     * @return 成功提示
     */
    @PutMapping(value={"/admin/settings"})
    public ApiResponse<Object> saveSettings(@RequestBody Map<String, String> settings, @RequestHeader(value="Authorization") String token) {
        AuthUtils.requireAdmin(token, this.jwtUtil, this.jwtProperties);
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            BlogSetting setting = new BlogSetting();
            setting.setSettingKey(entry.getKey());
            setting.setSettingValue(entry.getValue());
            if (this.blogSettingMapper.selectById(entry.getKey()) != null) {
                this.blogSettingMapper.updateById(setting);
            }
            else {
                this.blogSettingMapper.insert(setting);
            }
        }
        return ApiResponse.ok();
    }
}
