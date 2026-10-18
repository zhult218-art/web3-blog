package com.web3.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.web3.blog.entity.NavCategory;
import com.web3.blog.entity.NavLink;
import com.web3.blog.mapper.NavCategoryMapper;
import com.web3.blog.mapper.NavLinkMapper;
import com.web3.common.core.BusinessException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * NavService —— 导航数据业务服务
 * <p>
 * 所属模块：blog-service（博客模块）。
 * <p>
 * 职责：为「冒险者招募亭 /jobs」与「赏金猎人 /earn」两个导航页提供数据读写能力：
 * <ul>
 *   <li>公开读取：按页面聚合分类与条目，供前端展示；</li>
 *   <li>管理维护：分类与条目的增删改查，数据落 MySQL 持久化；</li>
 *   <li>图标抓取：访问各平台官网解析 favicon 并写回 iconUrl，供前端优先展示。</li>
 * </ul>
 */
@Service
public class NavService {

    /** 合法页面标识集合：jobs（招募亭）/ earn（赏金猎人） */
    private static final Set<String> PAGES = new LinkedHashSet<>(List.of("jobs", "earn"));
    /** 图标抓取并发线程数，避免串行等待过久或并发过高被目标站点限流 */
    private static final int ICON_POOL_SIZE = 8;
    /** 伪装成浏览器 UA，多数站点对默认 Java UA 会直接拒绝 */
    private static final String BROWSER_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    private static final Pattern LINK_TAG = Pattern.compile("<link\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern HREF_ATTR = Pattern.compile("href\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern OG_IMAGE_TAG =
        Pattern.compile("<meta\\b[^>]*property\\s*=\\s*[\"']og:image[\"'][^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONTENT_ATTR = Pattern.compile("content\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

    private final NavCategoryMapper navCategoryMapper;
    private final NavLinkMapper navLinkMapper;

    /** 无状态 HTTP 客户端：3 秒连接超时，自动跟随重定向 */
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    public NavService(NavCategoryMapper navCategoryMapper, NavLinkMapper navLinkMapper) {
        this.navCategoryMapper = navCategoryMapper;
        this.navLinkMapper = navLinkMapper;
    }

    /* ==================== 公开读取 ==================== */

    /**
     * 读取指定页面的全部可见数据（分类 + 条目），供前端导航页渲染。
     *
     * @param page 页面标识（jobs / earn）
     * @return 含 page、categories（含每类条目数）、items 的结果
     */
    public Map<String, Object> publicPage(String page) {
        String p = normalizePage(page);
        List<NavCategory> cats = navCategoryMapper.selectList(new LambdaQueryWrapper<NavCategory>()
            .eq(NavCategory::getPage, p)
            .eq(NavCategory::getEnabled, 1)
            .orderByAsc(NavCategory::getSort)
            .orderByAsc(NavCategory::getId));
        List<NavLink> links = navLinkMapper.selectList(new LambdaQueryWrapper<NavLink>()
            .eq(NavLink::getPage, p)
            .eq(NavLink::getEnabled, 1)
            .orderByAsc(NavLink::getSort)
            .orderByAsc(NavLink::getId));

        Map<String, Integer> counts = new HashMap<>();
        List<Map<String, Object>> items = new ArrayList<>(links.size());
        for (NavLink link : links) {
            counts.merge(link.getCatKey(), 1, Integer::sum);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", link.getId());
            item.put("cat", link.getCatKey());
            item.put("name", link.getName());
            item.put("url", link.getUrl());
            item.put("tag", link.getTag());
            item.put("description", link.getDescription());
            item.put("iconUrl", link.getIconUrl());
            items.add(item);
        }

        List<Map<String, Object>> categories = new ArrayList<>(cats.size());
        for (NavCategory cat : cats) {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("key", cat.getCatKey());
            c.put("name", cat.getName());
            c.put("icon", cat.getIcon());
            c.put("description", cat.getDescription());
            c.put("count", counts.getOrDefault(cat.getCatKey(), 0));
            categories.add(c);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("page", p);
        result.put("categories", categories);
        result.put("items", items);
        return result;
    }

    /* ==================== 管理端读写 ==================== */

    /** 管理端：读取指定页面的全部分类（含未启用），按 sort 升序 */
    public List<NavCategory> adminCategories(String page) {
        return navCategoryMapper.selectList(new LambdaQueryWrapper<NavCategory>()
            .eq(NavCategory::getPage, normalizePage(page))
            .orderByAsc(NavCategory::getSort)
            .orderByAsc(NavCategory::getId));
    }

    /** 管理端：读取指定页面的全部条目（含未启用），按分类与 sort 升序 */
    public List<NavLink> adminLinks(String page) {
        return navLinkMapper.selectList(new LambdaQueryWrapper<NavLink>()
            .eq(NavLink::getPage, normalizePage(page))
            .orderByAsc(NavLink::getCatKey)
            .orderByAsc(NavLink::getSort)
            .orderByAsc(NavLink::getId));
    }

    /** 管理端：新增导航分类 */
    public NavCategory createCategory(NavCategory body) {
        validateCategory(body, true);
        navCategoryMapper.insert(body);
        return body;
    }

    /** 管理端：更新导航分类 */
    public NavCategory updateCategory(Long id, NavCategory body) {
        NavCategory exist = navCategoryMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(404, "分类不存在");
        }
        body.setId(id);
        body.setPage(exist.getPage());
        validateCategory(body, false);
        navCategoryMapper.updateById(body);
        return navCategoryMapper.selectById(id);
    }

    /** 管理端：删除导航分类（同时清理该分类下的条目） */
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        NavCategory exist = navCategoryMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(404, "分类不存在");
        }
        navCategoryMapper.deleteById(id);
        navLinkMapper.delete(new LambdaQueryWrapper<NavLink>()
            .eq(NavLink::getPage, exist.getPage())
            .eq(NavLink::getCatKey, exist.getCatKey()));
    }

    /** 管理端：新增导航条目 */
    public NavLink createLink(NavLink body) {
        validateLink(body, true);
        navLinkMapper.insert(body);
        return body;
    }

    /** 管理端：更新导航条目 */
    public NavLink updateLink(Long id, NavLink body) {
        NavLink exist = navLinkMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(404, "条目不存在");
        }
        body.setId(id);
        body.setPage(exist.getPage());
        validateLink(body, false);
        navLinkMapper.updateById(body);
        return navLinkMapper.selectById(id);
    }

    /** 管理端：删除导航条目（逻辑删除） */
    public void deleteLink(Long id) {
        if (navLinkMapper.selectById(id) == null) {
            throw new BusinessException(404, "条目不存在");
        }
        navLinkMapper.deleteById(id);
    }

    /* ==================== 图标抓取 ==================== */

    /**
     * 管理端：为指定页面中尚无图标的条目抓取官网 favicon 并持久化写回。
     *
     * @param page 页面标识（jobs / earn）
     * @return 含 total（待处理数）与 updated（成功写入数）的结果
     */
    public Map<String, Object> fetchIcons(String page) {
        String p = normalizePage(page);
        List<NavLink> pending = navLinkMapper.selectList(new LambdaQueryWrapper<NavLink>()
            .eq(NavLink::getPage, p)
            .and(w -> w.isNull(NavLink::getIconUrl).or().eq(NavLink::getIconUrl, "")));

        int updated = 0;
        if (!pending.isEmpty()) {
            ExecutorService pool = Executors.newFixedThreadPool(ICON_POOL_SIZE);
            try {
                List<Future<AbstractMap.SimpleEntry<NavLink, String>>> tasks = new ArrayList<>(pending.size());
                for (NavLink link : pending) {
                    tasks.add(pool.submit(() -> {
                        return new AbstractMap.SimpleEntry<>(link, resolveFavicon(link.getUrl()));
                    }));
                }
                for (Future<AbstractMap.SimpleEntry<NavLink, String>> task : tasks) {
                    try {
                        AbstractMap.SimpleEntry<NavLink, String> entry = task.get();
                        String icon = entry.getValue();
                        if (icon == null || icon.isEmpty()) {
                            continue;
                        }
                        NavLink patch = new NavLink();
                        patch.setId(entry.getKey().getId());
                        patch.setIconUrl(icon);
                        navLinkMapper.updateById(patch);
                        updated++;
                    } catch (Exception ignored) {
                        /* 单个条目失败不影响整体抓取 */
                    }
                }
            } finally {
                pool.shutdown();
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", pending.size());
        result.put("updated", updated);
        return result;
    }

    /**
     * 尝试解析某网址对应的图标地址。
     * 策略：先探测同域 /favicon.ico，失败则抓取首页解析 &lt;link rel="icon"&gt; 与 og:image。
     *
     * @param url 平台官网地址
     * @return 可用图标地址；解析不到返回 null
     */
    private String resolveFavicon(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme();
            if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                return null;
            }
            String host = uri.getHost();
            if (host == null) {
                return null;
            }
            String origin = scheme + "://" + host + (uri.getPort() > 0 ? ":" + uri.getPort() : "");

            String ico = origin + "/favicon.ico";
            if (isImage(ico)) {
                return ico;
            }

            String html = getText(origin + "/");
            if (html != null) {
                String href = matchIconHref(html);
                if (href != null) {
                    String abs = absolutize(origin, href);
                    if (abs != null && isImage(abs)) {
                        return abs;
                    }
                }
                String og = matchOgImage(html);
                if (og != null) {
                    String abs = absolutize(origin, og);
                    if (abs != null) {
                        return abs;
                    }
                }
            }
        } catch (Exception ignored) {
            /* 解析失败按无图标处理，前端会回退首字母色块 */
        }
        return null;
    }

    /** 探测 URL 是否返回图片（200 且 Content-Type 为 image/*） */
    private boolean isImage(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .header("User-Agent", BROWSER_UA)
                .header("Accept", "image/*,*/*;q=0.8")
                .GET()
                .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() != 200) {
                return false;
            }
            String contentType = response.headers().firstValue("content-type").orElse("");
            return contentType.toLowerCase().startsWith("image");
        } catch (Exception e) {
            return false;
        }
    }

    /** 抓取网页文本（限制响应体大小，超限直接放弃，避免拖慢整体抓取） */
    private String getText(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(6))
                .header("User-Agent", BROWSER_UA)
                .header("Accept", "text/html,application/xhtml+xml,*/*;q=0.8")
                .GET()
                .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                return null;
            }
            byte[] body = response.body();
            if (body == null || body.length == 0 || body.length > 3 * 1024 * 1024) {
                return null;
            }
            return new String(body, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    /** 从首页 HTML 中找出 &lt;link&gt; 图标地址，优先非 apple-touch-icon */
    private String matchIconHref(String html) {
        String appleFallback = null;
        Matcher matcher = LINK_TAG.matcher(html);
        while (matcher.find()) {
            String tag = matcher.group();
            String lower = tag.toLowerCase();
            if (!lower.contains("icon")) {
                continue;
            }
            Matcher hrefMatcher = HREF_ATTR.matcher(tag);
            if (!hrefMatcher.find()) {
                continue;
            }
            String href = hrefMatcher.group(1).trim();
            if (href.isEmpty() || href.startsWith("data:")) {
                continue;
            }
            if (lower.contains("apple-touch-icon")) {
                if (appleFallback == null) {
                    appleFallback = href;
                }
                continue;
            }
            return href;
        }
        return appleFallback;
    }

    /** 从首页 HTML 中找出 og:image 地址，作为图标兜底 */
    private String matchOgImage(String html) {
        Matcher matcher = OG_IMAGE_TAG.matcher(html);
        if (matcher.find()) {
            Matcher contentMatcher = CONTENT_ATTR.matcher(matcher.group());
            if (contentMatcher.find()) {
                String content = contentMatcher.group(1).trim();
                if (!content.isEmpty() && !content.startsWith("data:")) {
                    return content;
                }
            }
        }
        return null;
    }

    /** 把可能是相对路径/协议相对路径的图标地址补全为绝对地址 */
    private String absolutize(String origin, String href) {
        try {
            if (href.startsWith("//")) {
                return "https:" + href;
            }
            if (href.startsWith("http://") || href.startsWith("https://")) {
                return href;
            }
            return URI.create(origin + "/").resolve(href).toString();
        } catch (Exception e) {
            return null;
        }
    }

    /* ==================== 校验与工具 ==================== */

    private String normalizePage(String page) {
        String p = page == null ? "" : page.trim().toLowerCase();
        if (!PAGES.contains(p)) {
            throw new BusinessException(400, "页面标识不合法，仅支持 jobs 或 earn");
        }
        return p;
    }

    private void validateCategory(NavCategory body, boolean requirePage) {
        if (body == null) {
            throw new BusinessException(400, "请求数据为空");
        }
        if (requirePage) {
            body.setPage(normalizePage(body.getPage()));
        }
        if (body.getCatKey() == null || body.getCatKey().isBlank()) {
            throw new BusinessException(400, "分类键不能为空");
        }
        if (body.getName() == null || body.getName().isBlank()) {
            throw new BusinessException(400, "分类名称不能为空");
        }
        body.setCatKey(body.getCatKey().trim());
        body.setName(body.getName().trim());
        if (body.getSort() == null) {
            body.setSort(0);
        }
        if (body.getEnabled() == null) {
            body.setEnabled(1);
        }
    }

    private void validateLink(NavLink body, boolean requirePage) {
        if (body == null) {
            throw new BusinessException(400, "请求数据为空");
        }
        if (requirePage) {
            body.setPage(normalizePage(body.getPage()));
        }
        if (body.getName() == null || body.getName().isBlank()) {
            throw new BusinessException(400, "名称不能为空");
        }
        if (body.getUrl() == null || body.getUrl().isBlank()) {
            throw new BusinessException(400, "链接不能为空");
        }
        if (body.getCatKey() == null || body.getCatKey().isBlank()) {
            throw new BusinessException(400, "所属分类不能为空");
        }
        body.setName(body.getName().trim());
        body.setUrl(body.getUrl().trim());
        body.setCatKey(body.getCatKey().trim());
        if (body.getSort() == null) {
            body.setSort(0);
        }
        if (body.getEnabled() == null) {
            body.setEnabled(1);
        }
    }
}
