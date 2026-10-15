package com.web3.media.controller;

import com.web3.common.core.ApiResponse;
import com.web3.common.core.PageResult;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import com.web3.media.entity.Music;
import com.web3.media.service.MusicService;
import com.web3.media.vo.MusicVO;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
 * MusicController —— 音乐接口控制器
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 职责：管理音乐资源的对外 REST 接口，包括音乐分页列表、详情、音乐的增删改（仅管理员），
 * 以及扩展能力：收藏 / 播放统计 / 历史 / 热榜 / 本地搜索 / Range 流式。
 * <p>
 * 关键注解作用：
 * <ul>
 *   <li>@RestController：声明为 REST 控制器，返回值自动序列化为 JSON</li>
 *   <li>@RequestMapping("/music")：对外接口统一前缀为 /music</li>
 * </ul>
 */
@RestController
@RequestMapping(value={"/music"})
public class MusicController {
    /** 音乐业务服务 */
    private final MusicService musicService;
    /** JWT 令牌校验工具 */
    private final JwtUtil jwtUtil;
    /** JWT 配置属性 */
    private final JwtProperties jwtProperties;

    public MusicController(MusicService musicService, JwtUtil jwtUtil, JwtProperties jwtProperties) {
        this.musicService = musicService;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 音乐分页列表（前端路由：GET /music/list）
     * 支持按歌名/歌手关键字筛选，按创建时间倒序返回
     *
     * @param page    页码，默认 1
     * @param size    每页条数，默认 20
     * @param keyword 歌名/歌手关键字，可为空
     * @return 分页的音乐视图对象列表
     */
    @GetMapping(value={"/list"})
    public ApiResponse<PageResult<MusicVO>> list(@RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int size, @RequestParam(defaultValue="") String keyword) {
        return ApiResponse.ok(this.musicService.page(page, size, keyword));
    }

    /**
     * 音乐详情（前端路由：GET /music/{id}）
     * 携带 Authorization 时附带当前用户收藏态（liked 字段）
     *
     * @param id   音乐 ID
     * @param auth 请求头 Authorization 的 JWT 令牌（可空）
     * @return 音乐视图对象
     */
    @GetMapping(value={"/{id}"})
    public ApiResponse<MusicVO> getById(@PathVariable Long id,
                                        @RequestHeader(value="Authorization", required=false) String auth) {
        Long userId = this.tryUserId(auth);
        return ApiResponse.ok(this.musicService.getDetailForUser(id, userId));
    }

    /**
     * 创建音乐（前端路由：POST /music）
     * 权限：ADMIN 恒可；普通用户需被授予 upload:music 权限
     */
    @PostMapping
    public ApiResponse<MusicVO> create(@RequestBody Music music, @RequestHeader(value="Authorization") String authorization) {
        AuthUtils.requirePerm(authorization, this.jwtUtil, this.jwtProperties, "upload:music", false);
        return ApiResponse.ok(this.musicService.create(music));
    }

    /**
     * 更新音乐（前端路由：PUT /music/{id}，需管理员）
     */
    @PutMapping(value={"/{id}"})
    public ApiResponse<MusicVO> update(@PathVariable Long id, @RequestBody Music music, @RequestHeader(value="Authorization") String authorization) {
        AuthUtils.requireAdmin(authorization, this.jwtUtil, this.jwtProperties);
        return ApiResponse.ok(this.musicService.update(id, music));
    }

    /**
     * 删除音乐（前端路由：DELETE /music/{id}，需管理员）
     */
    @DeleteMapping(value={"/{id}"})
    public ApiResponse<Object> delete(@PathVariable Long id, @RequestHeader(value="Authorization") String authorization) {
        AuthUtils.requireAdmin(authorization, this.jwtUtil, this.jwtProperties);
        this.musicService.delete(id);
        return ApiResponse.ok();
    }

    // ============================================================
    // 扩展能力：收藏 / 播放统计 / 热榜 / 搜索 / 流式 / 历史
    // ============================================================

    /**
     * 切换收藏（前端路由：POST /music/{id}/like，需登录）
     * 返回 {liked: true/false} 表示操作后的状态
     */
    @PostMapping(value={"/{id}/like"})
    public ApiResponse<java.util.Map<String, Boolean>> toggleLike(@PathVariable Long id,
                                                                  @RequestHeader(value="Authorization") String auth) {
        Long userId = AuthUtils.requireUserId(auth, this.jwtUtil, this.jwtProperties);
        boolean liked = this.musicService.toggleLike(userId, id);
        return ApiResponse.ok(java.util.Map.of("liked", liked));
    }

    /**
     * 查询当前用户对某首歌的收藏态（前端路由：GET /music/{id}/like，需登录）
     */
    @GetMapping(value={"/{id}/like"})
    public ApiResponse<java.util.Map<String, Boolean>> getLike(@PathVariable Long id,
                                                              @RequestHeader(value="Authorization") String auth) {
        Long userId = AuthUtils.requireUserId(auth, this.jwtUtil, this.jwtProperties);
        boolean liked = this.musicService.isLiked(userId, id);
        return ApiResponse.ok(java.util.Map.of("liked", liked));
    }

    /**
     * 当前用户收藏列表（前端路由：GET /music/liked，需登录，分页）
     */
    @GetMapping(value="/liked")
    public ApiResponse<PageResult<MusicVO>> liked(@RequestHeader(value="Authorization") String auth,
                                                  @RequestParam(defaultValue="1") int page,
                                                  @RequestParam(defaultValue="20") int size) {
        Long userId = AuthUtils.requireUserId(auth, this.jwtUtil, this.jwtProperties);
        return ApiResponse.ok(this.musicService.pageLiked(userId, page, size));
    }

    /**
     * 播放次数 +1 埋点（前端路由：POST /music/{id}/play）
     * 兼容游客：未登录时不写历史，但仍累加播放数
     */
    @PostMapping(value={"/{id}/play"})
    public ApiResponse<Object> recordPlay(@PathVariable Long id,
                                          @RequestHeader(value="Authorization", required=false) String auth) {
        this.musicService.incrementPlay(id);
        // 登录用户顺手写历史（非阻塞）
        Long userId = this.tryUserId(auth);
        if (userId != null) {
            this.musicService.addHistory(userId, id);
        }
        return ApiResponse.ok();
    }

    /**
     * 热榜（前端路由：GET /music/hot?limit=20）
     * 按 play_count 倒序，limit 钳制 1..100
     */
    @GetMapping(value="/hot")
    public ApiResponse<java.util.List<MusicVO>> hot(@RequestParam(defaultValue="20") int limit) {
        return ApiResponse.ok(this.musicService.hot(limit));
    }

    /**
     * 本地搜索（前端路由：GET /music/search?keyword=...&page=1&size=20）
     * 网易云/iTunes 由前端独立调用并合并结果
     */
    @GetMapping(value="/search")
    public ApiResponse<PageResult<MusicVO>> search(@RequestParam(defaultValue="") String keyword,
                                                    @RequestParam(defaultValue="1") int page,
                                                    @RequestParam(defaultValue="20") int size) {
        return ApiResponse.ok(this.musicService.search(keyword, page, size));
    }

    /**
     * 流式播放（前端路由：GET /music/{id}/stream）
     * 支持 HTTP Range 206 Partial Content，透传上游 URL 的 Range/Content-Range/Accept-Ranges
     * 用于音频播放器拖拽进度条秒级 seek，不必整文件下载
     */
    @GetMapping(value={"/{id}/stream"})
    public ResponseEntity<Resource> stream(@PathVariable Long id,
                                           @RequestHeader(value="Range", required=false) String range) throws IOException {
        Music music = this.musicService.getById(id);
        if (music == null || music.getUrl() == null || music.getUrl().isBlank()) {
            return ResponseEntity.notFound().build();
        }
        String urlStr = music.getUrl();
        // 仅放行 http/https 远端 URL；本地文件路径暂不支持，避免路径穿越
        if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://")) {
            return ResponseEntity.badRequest().build();
        }
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (web3-blog music streamer)");
        conn.setInstanceFollowRedirects(false);
        if (range != null && !range.isBlank()) {
            conn.setRequestProperty("Range", range);
        }
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(60000);
        int status = conn.getResponseCode();
        // Handle redirects (301/302) manually to ensure we return the final response code
        if (status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_MOVED_TEMP) {
            String location = conn.getHeaderField("Location");
            conn.disconnect();
            if (location == null || location.isBlank()) {
                return ResponseEntity.status(502).build();
            }
            URL redirectUrl = new URL(location);
            conn = (HttpURLConnection) redirectUrl.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (web3-blog music streamer)");
            conn.setInstanceFollowRedirects(false);
            if (range != null && !range.isBlank()) {
                conn.setRequestProperty("Range", range);
            }
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(60000);
            status = conn.getResponseCode();
        }
        // Pass through upstream error status (4xx/5xx) to the client
        if (status >= 400) {
            conn.disconnect();
            return ResponseEntity.status(status).build();
        }
        HttpHeaders headers = new HttpHeaders();
        String contentRange = conn.getHeaderField("Content-Range");
        String acceptRanges = conn.getHeaderField("Accept-Ranges");
        String contentType = conn.getContentType();
        long contentLength = conn.getContentLengthLong();
        if (contentRange != null) headers.set(HttpHeaders.CONTENT_RANGE, contentRange);
        headers.set(HttpHeaders.ACCEPT_RANGES, acceptRanges != null ? acceptRanges : "bytes");
        if (contentType != null) {
            try {
                headers.setContentType(MediaType.parseMediaType(contentType));
            } catch (RuntimeException ignored) {
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            }
        } else {
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        }
        if (contentLength > 0) headers.setContentLength(contentLength);
        InputStream is = conn.getInputStream();
        Resource resource = new InputStreamResource(is);
        return ResponseEntity.status(status).headers(headers).body(resource);
    }

    /**
     * 记录播放历史（前端路由：POST /music/history，body: {musicId}）
     * 单用户单首歌保留最新一条，避免重复堆积
     */
    @PostMapping(value="/history")
    public ApiResponse<Object> addHistory(@RequestBody java.util.Map<String, Object> body,
                                          @RequestHeader(value="Authorization") String auth) {
        Long userId = AuthUtils.requireUserId(auth, this.jwtUtil, this.jwtProperties);
        Object mid = body.get("musicId");
        if (mid == null) return ApiResponse.fail(400, "musicId 不能为空");
        Long musicId = Long.valueOf(String.valueOf(mid));
        this.musicService.addHistory(userId, musicId);
        return ApiResponse.ok();
    }

    /**
     * 当前用户播放历史（前端路由：GET /music/history，需登录，分页）
     */
    @GetMapping(value="/history")
    public ApiResponse<PageResult<MusicVO>> pageHistory(@RequestHeader(value="Authorization") String auth,
                                                        @RequestParam(defaultValue="1") int page,
                                                        @RequestParam(defaultValue="20") int size) {
        Long userId = AuthUtils.requireUserId(auth, this.jwtUtil, this.jwtProperties);
        return ApiResponse.ok(this.musicService.pageHistory(userId, page, size));
    }

    /**
     * 清空当前用户播放历史（前端路由：DELETE /music/history，需登录）
     */
    @DeleteMapping(value="/history")
    public ApiResponse<Object> clearHistory(@RequestHeader(value="Authorization") String auth) {
        Long userId = AuthUtils.requireUserId(auth, this.jwtUtil, this.jwtProperties);
        this.musicService.clearHistory(userId);
        return ApiResponse.ok();
    }

    /**
     * 尝试解析 Authorization 头中的 userId，未登录或令牌失效时返回 null（不抛异常）
     * 用于 getById 这类游客也可访问的接口，附带当前用户态时使用
     */
    private Long tryUserId(String auth) {
        if (auth == null || auth.isBlank()) return null;
        try {
            return AuthUtils.requireUserId(auth, this.jwtUtil, this.jwtProperties);
        } catch (RuntimeException e) {
            return null;
        }
    }
}

