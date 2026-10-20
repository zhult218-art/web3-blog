package com.web3.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.web3.blog.entity.NavCategory;
import com.web3.blog.entity.NavLink;
import com.web3.blog.mapper.NavCategoryMapper;
import com.web3.blog.mapper.NavLinkMapper;
import com.web3.common.core.BusinessException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetAddress;
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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

    /* ---- 第三方图标同源代理相关常量 ---- */
    /** 图标缓存条目上限，超出时清理已过期项，避免长期运行内存膨胀 */
    private static final int ICON_CACHE_MAX = 2048;
    /** 成功图标缓存时长（浏览器侧缓存 7 天，服务端缓存 6 小时） */
    private static final long ICON_TTL_MS = Duration.ofHours(6).toMillis();
    /** 失败负缓存时长：避免反复重试已打不通的站点 */
    private static final long ICON_NEG_TTL_MS = Duration.ofMinutes(10).toMillis();
    /** 单个图标最大字节数，超出即视为失败 */
    private static final int ICON_MAX_BYTES = 512 * 1024;
    /** 主题色合法格式：#RRGGBB 等纯十六进制色值 */
    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9a-fA-F]{3,8}$");
    /** 首字母合法字符：字母、数字、CJK 汉字 */
    private static final Pattern LETTER_CHAR = Pattern.compile("^[0-9A-Za-z\\u4e00-\\u9fa5]$");

    private final NavCategoryMapper navCategoryMapper;
    private final NavLinkMapper navLinkMapper;

    /** 图标抓取结果缓存：key 为原始图标 URL，value 为缓存项（body 为 null 表示负缓存） */
    private final Map<String, IconCacheEntry> iconCache = new ConcurrentHashMap<>();

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

    /* ==================== 第三方图标同源代理 ==================== */

    /**
     * 代理抓取第三方图标。
     * <p>
     * 浏览器直连各站点 favicon 会因 ORB 拦截、证书异常、连接被重置等产生大量控制台报错；
     * 改由服务端抓取并校验后同源返回，可彻底消除这些报错。
     * 抓取失败（或参数非法）时返回首字母色块 SVG，保证响应恒为 200 图片，
     * 前端 &lt;img&gt; 不会因加载失败触发任何控制台错误。
     *
     * @param rawUrl 目标图标地址（http/https）
     * @param rawLetter 首字母回退字符
     * @param rawColor 首字母色块主题色（#RRGGBB）
     * @return 图标内容与 MIME 类型，永不为 null
     */
    public IconResult proxyIcon(String rawUrl, String rawLetter, String rawColor) {
        String url = normalizeIconUrl(rawUrl);
        if (url != null) {
            long now = System.currentTimeMillis();
            IconCacheEntry entry = iconCache.get(url);
            if (entry == null || entry.expireAt <= now) {
                IconResult fetched = fetchImage(url);
                long ttl = fetched == null ? ICON_NEG_TTL_MS : ICON_TTL_MS;
                entry = new IconCacheEntry(fetched == null ? null : fetched.body(),
                    fetched == null ? null : fetched.contentType(), now + ttl);
                if (iconCache.size() >= ICON_CACHE_MAX) {
                    iconCache.entrySet().removeIf(e -> e.getValue().expireAt <= now);
                }
                iconCache.put(url, entry);
            }
            if (entry.body != null) {
                return new IconResult(entry.body, entry.contentType);
            }
        }
        return new IconResult(letterTileSvg(rawLetter, rawColor), "image/svg+xml");
    }

    /** 校验并规范化目标图标地址；data: 内联图标由前端直接使用，不走代理 */
    private String normalizeIconUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        if (s.length() > 2048 || s.startsWith("data:")) {
            return null;
        }
        try {
            URI uri = URI.create(s);
            String scheme = uri.getScheme();
            if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                return null;
            }
            String host = uri.getHost();
            if (host == null || host.isBlank() || !isPublicHost(host)) {
                return null;
            }
            return s;
        } catch (Exception e) {
            return null;
        }
    }

    /** SSRF 防护：拒绝回环 / 内网 / 链路本地 / 多播地址与本地域名 */
    private boolean isPublicHost(String host) {
        String h = host.toLowerCase(Locale.ROOT);
        if (h.equals("localhost") || h.endsWith(".local") || h.endsWith(".internal")) {
            return false;
        }
        try {
            InetAddress[] addrs = InetAddress.getAllByName(host);
            if (addrs.length == 0) {
                return false;
            }
            for (InetAddress addr : addrs) {
                if (addr.isLoopbackAddress() || addr.isAnyLocalAddress() || addr.isLinkLocalAddress()
                    || addr.isSiteLocalAddress() || addr.isMulticastAddress()) {
                    return false;
                }
                byte[] ip = addr.getAddress();
                if (ip.length == 16) {
                    // IPv6 唯一本地地址 fc00::/7
                    if ((ip[0] & 0xfe) == 0xfc) {
                        return false;
                    }
                } else if (ip.length == 4) {
                    int b0 = ip[0] & 0xff;
                    int b1 = ip[1] & 0xff;
                    // 0.0.0.0/8 与 100.64.0.0/10（运营商级 NAT）
                    if (b0 == 0 || (b0 == 100 && b1 >= 64 && b1 <= 127)) {
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 服务端抓取图片：限制大小与超时，并通过魔数校验真实类型。
     * 校验失败（例如站点对图标返回了 HTML 错误页）即视为无图标，避免把非图片内容代理给浏览器。
     */
    private IconResult fetchImage(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .header("User-Agent", BROWSER_UA)
                .header("Accept", "image/*,*/*;q=0.8")
                .GET()
                .build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                return null;
            }
            long declared = response.headers().firstValueAsLong("content-length").orElse(-1L);
            if (declared > ICON_MAX_BYTES) {
                return null;
            }
            byte[] body;
            try (InputStream in = response.body()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int total = 0;
                int n;
                while ((n = in.read(buf)) != -1) {
                    total += n;
                    if (total > ICON_MAX_BYTES) {
                        return null;
                    }
                    out.write(buf, 0, n);
                }
                body = out.toByteArray();
            }
            String type = detectImageType(body);
            return type == null ? null : new IconResult(body, type);
        } catch (Exception e) {
            return null;
        }
    }

    /** 通过魔数识别图片真实类型，无法识别返回 null */
    private String detectImageType(byte[] b) {
        if (b == null || b.length < 4) {
            return null;
        }
        int b0 = b[0] & 0xff;
        int b1 = b[1] & 0xff;
        int b2 = b[2] & 0xff;
        int b3 = b[3] & 0xff;
        if (b0 == 0x89 && b1 == 0x50 && b2 == 0x4e && b3 == 0x47) {
            return "image/png";
        }
        if (b0 == 0xff && b1 == 0xd8 && b2 == 0xff) {
            return "image/jpeg";
        }
        if (b0 == 'G' && b1 == 'I' && b2 == 'F') {
            return "image/gif";
        }
        if (b0 == 'B' && b1 == 'M') {
            return "image/bmp";
        }
        if (b.length >= 12 && b0 == 'R' && b1 == 'I' && b2 == 'F' && b3 == 'F'
            && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        // ICO / CUR：00 00 01|02 00
        if (b0 == 0 && b1 == 0 && (b2 == 1 || b2 == 2) && b3 == 0) {
            return "image/x-icon";
        }
        if (b.length >= 12 && b[4] == 'f' && b[5] == 't' && b[6] == 'y' && b[7] == 'p') {
            return "image/avif";
        }
        String head = new String(b, 0, Math.min(b.length, 256), StandardCharsets.ISO_8859_1).toLowerCase(Locale.ROOT);
        if (head.contains("<svg") || head.contains("<?xml")) {
            return "image/svg+xml";
        }
        return null;
    }

    /** 生成首字母色块 SVG，用于替换原前端 CSS 回退块，保证响应始终为图片 */
    private byte[] letterTileSvg(String rawLetter, String rawColor) {
        String letter = sanitizeLetter(rawLetter);
        String color = rawColor != null && HEX_COLOR.matcher(rawColor.trim()).matches() ? rawColor.trim() : "#7c3aed";
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 64 64\" width=\"64\" height=\"64\">"
            + "<defs><linearGradient id=\"g\" x1=\"0\" y1=\"0\" x2=\"1\" y2=\"1\">"
            + "<stop offset=\"0\" stop-color=\"" + color + "\"/>"
            + "<stop offset=\"1\" stop-color=\"#000000\" stop-opacity=\"0.45\"/>"
            + "</linearGradient></defs>"
            + "<rect width=\"64\" height=\"64\" fill=\"url(#g)\"/>"
            + "<text x=\"32\" y=\"34\" fill=\"#ffffff\" text-anchor=\"middle\" font-family=\"Arial,Helvetica,sans-serif\""
            + " font-size=\"30\" font-weight=\"700\">" + letter + "</text>"
            + "</svg>";
        return svg.getBytes(StandardCharsets.UTF_8);
    }

    /** 首字母取首个合法字符（字母/数字/汉字），非法时用 ? 兜底 */
    private String sanitizeLetter(String raw) {
        String s = raw == null ? "" : raw.trim();
        for (int i = 0; i < s.length(); i++) {
            String ch = String.valueOf(s.charAt(i));
            if (LETTER_CHAR.matcher(ch).matches()) {
                return ch.toUpperCase(Locale.ROOT);
            }
        }
        return "?";
    }

    /** 代理返回的图标内容 */
    public record IconResult(byte[] body, String contentType) {
    }

    /** 图标缓存项：body 为 null 表示负缓存（曾抓取失败） */
    private static final class IconCacheEntry {
        final byte[] body;
        final String contentType;
        final long expireAt;

        IconCacheEntry(byte[] body, String contentType, long expireAt) {
            this.body = body;
            this.contentType = contentType;
            this.expireAt = expireAt;
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
