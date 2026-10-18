package com.web3.blog.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.web3.blog.entity.NavCategory;
import com.web3.blog.entity.NavLink;
import com.web3.blog.mapper.NavCategoryMapper;
import com.web3.blog.mapper.NavLinkMapper;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * NavDataInitRunner —— 导航数据初始化器
 * <p>
 * 所属模块：blog-service（博客模块）。
 * <p>
 * 职责：服务启动时（1）幂等创建 nav_category / nav_link 两张表；
 * （2）若某页面尚无任何条目，则从 classpath:nav-data.json 读取内置数据整体播种，
 * 使「冒险者招募亭 /jobs」与「赏金猎人 /earn」无需手工执行 SQL 即可使用。
 * <p>
 * 说明：表名为 IF NOT EXISTS，重复启动不会重复建表；播种仅在表中无数据时执行，不会覆盖后台维护结果。
 */
@Component
public class NavDataInitRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(NavDataInitRunner.class);
    /** 内置种子数据所在的资源路径 */
    private static final String SEED_RESOURCE = "nav-data.json";
    /** 页面标识列表 */
    private static final String[] PAGES = {"jobs", "earn"};

    private static final String DDL_CATEGORY = "CREATE TABLE IF NOT EXISTS `nav_category` ("
        + "`id` BIGINT NOT NULL,"
        + "`page` VARCHAR(16) NOT NULL COMMENT '所属页面 jobs/earn',"
        + "`cat_key` VARCHAR(32) NOT NULL COMMENT '分类键',"
        + "`name` VARCHAR(64) NOT NULL COMMENT '分类名称',"
        + "`icon` VARCHAR(16) DEFAULT NULL COMMENT '分类图标',"
        + "`description` VARCHAR(255) DEFAULT NULL COMMENT '分类说明',"
        + "`sort` INT DEFAULT 0 COMMENT '排序（小的在前）',"
        + "`enabled` TINYINT DEFAULT 1 COMMENT '1=启用',"
        + "`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,"
        + "`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
        + "PRIMARY KEY (`id`),"
        + "UNIQUE KEY `uk_page_cat` (`page`,`cat_key`)"
        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='导航分类'";

    private static final String DDL_LINK = "CREATE TABLE IF NOT EXISTS `nav_link` ("
        + "`id` BIGINT NOT NULL,"
        + "`page` VARCHAR(16) NOT NULL COMMENT '所属页面 jobs/earn',"
        + "`cat_key` VARCHAR(32) NOT NULL COMMENT '所属分类键',"
        + "`name` VARCHAR(128) NOT NULL COMMENT '名称',"
        + "`url` VARCHAR(512) NOT NULL COMMENT '官网链接',"
        + "`tag` VARCHAR(128) DEFAULT NULL COMMENT '短标签',"
        + "`description` VARCHAR(512) DEFAULT NULL COMMENT '详细说明',"
        + "`icon_url` VARCHAR(512) DEFAULT NULL COMMENT '图标地址（可空，空则自动取官网 favicon）',"
        + "`sort` INT DEFAULT 0 COMMENT '排序（小的在前）',"
        + "`enabled` TINYINT DEFAULT 1 COMMENT '1=启用',"
        + "`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,"
        + "`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
        + "`deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',"
        + "PRIMARY KEY (`id`),"
        + "KEY `idx_page_cat` (`page`,`cat_key`)"
        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='导航条目'";

    private final JdbcTemplate jdbcTemplate;
    private final NavCategoryMapper navCategoryMapper;
    private final NavLinkMapper navLinkMapper;
    private final ObjectMapper objectMapper;

    public NavDataInitRunner(JdbcTemplate jdbcTemplate, NavCategoryMapper navCategoryMapper,
                             NavLinkMapper navLinkMapper, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.navCategoryMapper = navCategoryMapper;
        this.navLinkMapper = navLinkMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) {
        try {
            jdbcTemplate.execute(DDL_CATEGORY);
            jdbcTemplate.execute(DDL_LINK);
        } catch (Exception e) {
            log.error("导航数据表创建失败（不影响其它功能）", e);
            return;
        }
        try {
            seedIfEmpty();
        } catch (Exception e) {
            log.error("导航数据播种失败（可稍后通过管理端维护）", e);
        }
    }

    /** 某个页面若一条条目都没有，则从内置 JSON 整体播种该页面的分类与条目 */
    private void seedIfEmpty() throws Exception {
        ClassPathResource resource = new ClassPathResource(SEED_RESOURCE);
        if (!resource.exists()) {
            log.warn("未找到内置导航种子数据 {}", SEED_RESOURCE);
            return;
        }
        JsonNode root;
        try (InputStream in = resource.getInputStream()) {
            root = objectMapper.readTree(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
        for (String page : PAGES) {
            Long exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM nav_link WHERE page = ?", Long.class, page);
            if (exists != null && exists > 0) {
                continue;
            }
            JsonNode pageNode = root.path(page);
            if (pageNode.isMissingNode()) {
                continue;
            }
            int sort = 0;
            for (JsonNode c : pageNode.path("categories")) {
                NavCategory cat = new NavCategory();
                cat.setPage(page);
                cat.setCatKey(c.path("key").asText());
                cat.setName(c.path("name").asText());
                cat.setIcon(c.path("icon").asText(null));
                cat.setDescription(c.path("description").asText(null));
                cat.setSort(sort++);
                cat.setEnabled(1);
                navCategoryMapper.insert(cat);
            }
            int linkCount = 0;
            for (JsonNode it : pageNode.path("items")) {
                NavLink link = new NavLink();
                link.setPage(page);
                link.setCatKey(it.path("cat").asText());
                link.setName(it.path("name").asText());
                link.setUrl(it.path("url").asText());
                link.setTag(it.path("tag").asText(null));
                link.setDescription(it.path("description").asText(null));
                link.setSort(linkCount);
                link.setEnabled(1);
                navLinkMapper.insert(link);
                linkCount++;
            }
            log.info("导航数据初始化完成：page={} 分类 {} 个，条目 {} 条", page,
                pageNode.path("categories").size(), linkCount);
        }
    }
}
