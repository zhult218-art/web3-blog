package com.web3.blog.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.web3.blog.entity.BlogSetting;
import com.web3.blog.mapper.BlogSettingMapper;
import com.web3.blog.service.ArticleService;
import com.web3.blog.vo.ArticleVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * FeedController —— 博客 RSS 订阅源（/blog 前缀，经网关转发）
 * <p>
 * 所属模块：blog-service（博客模块）。
 * <p>
 * 职责：动态生成站点 RSS 2.0 订阅源（application/rss+xml），
 * 数据实时来自 MySQL 中已发布文章，供 RSS 阅读器 / 订阅器读取。
 * <p>
 * 站点绝对地址来源优先级：
 * <ol>
 *   <li>配置项 {@code blog.site-url}（环境变量 BLOG_SITE_URL 覆盖，生产环境指向正式域名）；</li>
 *   <li>未配置时按请求头 X-Forwarded-Proto / Host 推断。</li>
 * </ol>
 */
@RestController
@RequestMapping(value = "/blog")
public class FeedController {
    /** RFC 822 时间格式（RSS pubDate 规范要求） */
    private static final DateTimeFormatter RFC822 =
        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
    /** 文章业务服务 */
    private final ArticleService articleService;
    /** 站点配置表数据访问（读取 author 昵称/标语） */
    private final BlogSettingMapper blogSettingMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 站点前端绝对地址（生产环境配置为正式域名，如 https://blog.example.com） */
    @Value("${blog.site-url:}")
    private String configuredSiteUrl;

    public FeedController(ArticleService articleService, BlogSettingMapper blogSettingMapper) {
        this.articleService = articleService;
        this.blogSettingMapper = blogSettingMapper;
    }

    /**
     * RSS 2.0 订阅源（前端路由：GET /blog/feed.xml）
     *
     * @param limit   返回文章条数，默认 20（钳制 1..50）
     * @param request 用于在未配置 site-url 时推断站点地址
     * @return RSS 2.0 XML 文本
     */
    @GetMapping(value = "/feed.xml", produces = "application/rss+xml;charset=UTF-8")
    public String feed(@RequestParam(defaultValue = "20") int limit, HttpServletRequest request) {
        int size = Math.min(Math.max(limit, 1), 50);
        String base = resolveBaseUrl(request);
        SiteMeta meta = loadSiteMeta();

        List<ArticleVO> articles = this.articleService.recent(size);

        StringBuilder sb = new StringBuilder(4096);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<rss version=\"2.0\">\n<channel>\n");
        appendTag(sb, "title", meta.title);
        appendTag(sb, "link", base + "/");
        appendTag(sb, "description", meta.slogan);
        appendTag(sb, "language", "zh-CN");
        appendTag(sb, "lastBuildDate", formatDate(LocalDateTime.now()));
        appendTag(sb, "generator", "web3-blog-service");

        for (ArticleVO a : articles) {
            String url = base + "/blog/post/" + a.getId();
            sb.append("<item>\n");
            appendTag(sb, "title", a.getTitle());
            appendTag(sb, "link", url);
            appendTag(sb, "guid", url);
            appendTag(sb, "description", a.getSummary() != null ? a.getSummary() : "");
            if (a.getCategory() != null && !a.getCategory().isBlank()) {
                appendTag(sb, "category", a.getCategory());
            }
            if (a.getAuthorName() != null && !a.getAuthorName().isBlank()) {
                appendTag(sb, "author", a.getAuthorName());
            }
            appendTag(sb, "pubDate", formatDate(a.getCreatedAt() != null ? a.getCreatedAt() : a.getUpdatedAt()));
            sb.append("</item>\n");
        }
        sb.append("</channel>\n</rss>\n");
        return sb.toString();
    }

    /** 写入一个已转义的 XML 标签 */
    private void appendTag(StringBuilder sb, String tag, String value) {
        sb.append('<').append(tag).append('>')
          .append(escapeXml(value == null ? "" : value))
          .append("</").append(tag).append(">\n");
    }

    /** XML 特殊字符转义，防止内容破坏文档结构 */
    private String escapeXml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /** LocalDateTime → RFC822 时间（东八区） */
    private String formatDate(LocalDateTime t) {
        ZonedDateTime zdt = t.atZone(ZoneId.of("Asia/Shanghai"));
        return RFC822.format(zdt);
    }

    /** 解析站点前端绝对地址：优先配置项，其次按请求推断 */
    private String resolveBaseUrl(HttpServletRequest request) {
        if (configuredSiteUrl != null && !configuredSiteUrl.isBlank()) {
            return stripTrailingSlash(configuredSiteUrl.trim());
        }
        String proto = request.getHeader("X-Forwarded-Proto");
        if (proto == null || proto.isBlank()) {
            proto = request.getScheme();
        }
        String host = request.getHeader("X-Forwarded-Host");
        if (host == null || host.isBlank()) {
            host = request.getHeader("Host");
        }
        return stripTrailingSlash(proto + "://" + (host == null ? "localhost" : host));
    }

    private String stripTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    /** 站点标题与标语：读取 blog_setting 的 author 配置，缺省时用兜底文案 */
    private SiteMeta loadSiteMeta() {
        String title = "Aurora-朱 · 极光数字空间";
        String slogan = "以代码构建无限可能";
        try {
            BlogSetting setting = this.blogSettingMapper.selectOne(
                new LambdaQueryWrapper<BlogSetting>().eq(BlogSetting::getSettingKey, "author"), false);
            if (setting != null && setting.getSettingValue() != null) {
                JsonNode node = this.objectMapper.readTree(setting.getSettingValue());
                String nick = node.path("nickname").asText("");
                String slo = node.path("slogan").asText("");
                if (!nick.isBlank()) {
                    title = nick + " · 极光数字空间";
                }
                if (!slo.isBlank()) {
                    slogan = slo;
                }
            }
        } catch (Exception ignore) {
            // 配置缺失/格式异常时使用兜底文案，不影响订阅源可用
        }
        return new SiteMeta(title, slogan);
    }

    /** 站点元信息载体 */
    private static class SiteMeta {
        final String title;
        final String slogan;

        SiteMeta(String title, String slogan) {
            this.title = title;
            this.slogan = slogan;
        }
    }
}
