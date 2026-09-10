# lololowe 博客移植 · 说明文档（详细设计）
> 文档版本：v1.0　|　编写日期：2026-08-17　|　需求依据：《lololowe博客移植需求文档.md》
> 目标架构：本项目现有 Vue3 + Spring Cloud 微服务体系，不引入 Hexo

---

## 0. 移植策略总纲

| 决策点 | 结论 | 理由 |
|---|---|---|
| 博客引擎 | 不复刻 Hexo/anzhiyu，用本项目 Vue3 + blog-service 微服务实现同等体验 | 已有 article 体系，避免双栈 |
| 前端组件 | 新增 `frontend/src/components/blog/` 目录承载博客增强组件 | 与 Web3Layout 解耦 |
| 布局 | 首页/列表页使用新 `BlogLayout.vue`（三栏），详情页复用其左右栏 | 一套布局两处使用 |
| 数据 | 文章主数据在 blog-service 扩表；氛围组件纯前端；友链/留言板新表 | 分工明确 |
| 音乐 | 后端做网易云歌单代理（防跨域 + Key 统一），前端复用现有 music 组件 | 现有 music 雏形可升级 |
| 管理端 | 友链/工具/相册/公告 全部进现有 admin 后台 | 复用 admin-service |

---

## 1. 后端设计

### 1.1 表结构（新增/修改）

#### 1.1.1 `article` 表扩展（blog-service，web3_blog 库）
```sql
-- 在现有 article 表基础上新增字段（ALTER 或随建表脚本）
ALTER TABLE `article`
  ADD COLUMN `cover_url`     VARCHAR(512) NULL COMMENT '封面图' AFTER `summary`,
  ADD COLUMN `word_count`    INT          NOT NULL DEFAULT 0 COMMENT '正文字数',
  ADD COLUMN `reading_min`   INT          NOT NULL DEFAULT 0 COMMENT '预计阅读分钟',
  ADD COLUMN `source_type`   VARCHAR(16)  NOT NULL DEFAULT 'ORIGINAL' COMMENT 'ORIGINAL/REPRINT',
  ADD COLUMN `reprint_url`   VARCHAR(512) NULL COMMENT '转载来源',
  ADD COLUMN `like_count`    INT          NOT NULL DEFAULT 0 COMMENT '点赞数(冗余)',
  ADD COLUMN `comment_count` INT          NOT NULL DEFAULT 0 COMMENT '评论数(冗余)',
  ADD COLUMN `published_at`  DATETIME     NULL COMMENT '发表时间(区别于 created_at)';
```
> `word_count` 与 `reading_min`（按 300字/分钟 取整）在发布/编辑时后端计算写入；`comment_count` 由评论服务回调或定时任务同步。

#### 1.1.2 新表 `friend_link`（友链，web3_blog 库）
```sql
CREATE TABLE IF NOT EXISTS `friend_link` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `name`        VARCHAR(64)  NOT NULL COMMENT '站名',
  `url`         VARCHAR(255) NOT NULL COMMENT '链接',
  `avatar`      VARCHAR(512) NULL COMMENT '头像',
  `description` VARCHAR(255) NULL COMMENT '一句话描述',
  `group_name`  VARCHAR(32)  DEFAULT 'default' COMMENT '分组(页脚分组用)',
  `status`      VARCHAR(16)  DEFAULT 'PUBLISHED' COMMENT 'PUBLISHED/PENDING/DISABLED',
  `sort`        INT          DEFAULT 0,
  `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='友情链接';
```

#### 1.1.3 新表 `site_notice`（公告，web3_blog 库）
```sql
CREATE TABLE IF NOT EXISTS `site_notice` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `content`    VARCHAR(512) NOT NULL,
  `enabled`    TINYINT      DEFAULT 1,
  `updated_at` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站点公告';
```

#### 1.1.4 新表 `blog_setting`（打赏/页脚/社交 配置，web3_blog 库）
```sql
CREATE TABLE IF NOT EXISTS `blog_setting` (
  `setting_key`   VARCHAR(64)  NOT NULL,
  `setting_value` TEXT         NULL,
  PRIMARY KEY (`setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客站点配置';
-- 种子：btc_qr, eth_qr, weibo_url, github_url, email, icp, slogan, announcement...
```

### 1.2 接口设计（blog-service 新增）

#### 文章
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/blog/articles?page=&size=&category=&tag=&kw=` | 列表（分页+筛选），返回卡片全字段 |
| GET | `/api/blog/articles/{id}` | 详情 + 上一篇/下一篇 id + TOC 章节锚点 |
| GET | `/api/blog/articles/{id}/related?limit=5` | 相关推荐（同分类优先→同标签→最新） |
| GET | `/api/blog/articles/random` | 随机一篇 |
| GET | `/api/blog/archives` | 按年月分组 `[{month:'2026-08', count:4, items:[...]}]` |
| GET | `/api/blog/categories` | 分类 + 计数 `[{name, count, cover}]` |
| GET | `/api/blog/tags` | 标签 + 计数 `[{name, count}]` |
| GET | `/api/blog/stats` | 文章总数 / 全站字数 / 最后更新时间（侧栏 W6/W8） |
| GET | `/api/blog/recent?limit=5` | 最近更新（侧栏 W7） |
| POST | `/api/blog/articles/{id}/read` | 阅读计数 +1（可选，幂等节流由前端控制） |

#### 友链 / 公告 / 配置（读接口公开，写接口 admin 鉴权）
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/blog/links?group=` | 友链列表 |
| POST | `/api/blog/links/apply` | 访客申请（status=PENDING） |
| GET | `/api/blog/notice` | 当前启用公告 |
| GET | `/api/blog/settings` | 公开配置（打赏 QR/社交/ICP/签名） |
| CRUD | `/admin/blog/links` / `/admin/blog/notice` / `/admin/blog/settings` | 管理端维护 |

#### 音乐代理（新 `music-service` 或并入 gateway 的通用代理）
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/music/playlist?id=652135520&server=netease` | 歌单元数据+歌曲列表（服务端请求网易云，拼真实 URL） |
| GET | `/api/music/url?id=&server=netease` | 单曲播放地址（解析 302 重定向后的真实地址） |
| GET | `/api/music/lyric?id=` | 歌词（现有 `lyricsData.js` 可并行保留为兜底） |
> 服务端需实现网易云接口签名（weapi 加密）或使用开源库 `NeteaseCloudMusicApi`（推荐直接对接该开源服务，部署为独立容器，本项目只做转发）。

---

## 2. 前端设计

### 2.1 新增文件清单
```
frontend/src/layouts/BlogLayout.vue        # 三栏布局（左/中/右）
frontend/src/views/blog/index.vue          # 文章流首页（重写现有首页博客区 or 新路由 /blog）
frontend/src/views/blog/detail.vue         # 文章详情
frontend/src/views/blog/categories.vue     # 分类页
frontend/src/views/blog/tags.vue           # 标签云页
frontend/src/views/blog/archives.vue       # 归档时间轴
frontend/src/views/blog/link.vue           # 友人帐（重写现有占位页）
frontend/src/views/blog/comments.vue       # 留言板（重写现有 mock 页）
frontend/src/components/blog/ArticleCard.vue      # 文章卡片
frontend/src/components/blog/PostToc.vue          # 目录
frontend/src/components/blog/PostRelated.vue      # 相关推荐
frontend/src/components/blog/PostShare.vue        # 分享/打赏
frontend/src/components/blog/Shortcuts.vue        # 快捷键面板
frontend/src/components/blog/CustomContextMenu.vue# 右键增强
frontend/src/components/blog/ConsolePanel.vue     # 中控台
frontend/src/components/blog/NewsBar.vue          # 公告条
frontend/src/components/blog/CommentBarrage.vue   # 评论弹幕(P2)
frontend/src/components/blog/SidebarWidgets.vue   # 侧栏组件聚合
frontend/src/components/blog/LoadingMask.vue      # 首屏加载
frontend/src/composables/useShortcuts.js         # 快捷键注册器
frontend/src/composables/useTheme.js             # 深色/浅色(升级现有)
frontend/src/composables/useReadMark.js          # 已读标记(localStorage)
frontend/src/api/blog.js                          # 新接口封装
```

### 2.2 BlogLayout.vue 布局结构
```
<div class="blog-layout">
  <header>（复用 Web3Nav，可选叠加公告条 NewsBar）</header>
  <div class="blog-body max-w-[1400px] mx-auto grid grid-cols-1 lg:grid-cols-[240px_1fr] xl:grid-cols-[240px_1fr_300px] gap-6 px-4">
    <aside class="hidden lg:block order-1">  <!-- 左栏 -->
      <SidebarWidgets :slot="'left'" />
    </aside>
    <main class="order-2">
      <router-view />   <!-- 文章流/详情/分类/标签/归档 -->
    </main>
    <aside class="hidden xl:block order-3">  <!-- 右栏 -->
      <SidebarWidgets :slot="'right'" />
    </aside>
  </div>
  <footer>（复用 Web3Footer，扩展友链分组）</footer>
</div>
```
- 左右栏内容由配置决定：左栏=作者卡+兴趣点+归档；右栏=最近更新+网站资讯+统计数字
- 移动端隐藏左右栏，顶部汉堡菜单内提供「标签云/归档」入口

### 2.3 文章卡片 ArticleCard.vue
```vue
<article class="article-card group">
  <a class="cover"> <!-- 懒加载: loading3.gif 占位, data-src 真实图, 16:9 -->
    <img v-lazy="cover" :alt="title" class="aspect-video object-cover" />
    <span v-if="category" class="badge">{{ category }}</span>
  </a>
  <div class="body">
    <h2 class="title group-hover:text-primary transition-colors">{{ title }}</h2>
    <p class="summary line-clamp-2">{{ summary }}</p>
    <div class="meta flex gap-3 text-xs text-muted">
      <time>{{ publishedAt }}</time>
      <span v-for="t in tags.slice(0,3)" class="tag">{{ t }}</span>
      <span v-if="!isRead" class="unread-dot" title="未读"></span>
    </div>
  </div>
</article>
```

### 2.4 文章详情 detail.vue 数据流
1. 进入页面：`GET /api/blog/articles/{id}` → 渲染正文（markdown-it + highlight.js）
2. 正文渲染完成后从 DOM 提取 `h1~h3` 构建 TOC 数组 → PostToc.vue 滚动监听高亮
3. 记录已读：`useReadMark` 写 localStorage，返回列表时红点消失
4. 并行加载：`/related`（相关推荐）、`/list?page=1`（上一篇/下一篇取自详情接口返回）
5. 评论区组件挂在正文尾部（见 2.6）

### 2.5 快捷键 useShortcuts.js
```js
// 注册表（shift 组合，全部挂 keydown，输入框聚焦时跳过）
const bindings = [
  { key: 'S', fn: () => openSearch() },       // 站内搜索
  { key: 'D', fn: () => toggleTheme() },
  { key: 'R', fn: () => goRandomPost() },
  { key: 'H', fn: () => router.push('/') },
  { key: 'M', fn: () => musicStore.toggle() },
  { key: 'A', fn: () => consoleStore.toggle() },
  { key: 'L', fn: () => router.push('/blog/link') },
  { key: 'P', fn: () => router.push('/about') },
  { key: 'I', fn: () => ctxMenuStore.toggleNative() },
  { key: 'K', fn: () => shortcutsPanel.toggle() }, // 开关面板
]
// localStorage['shortcuts_disabled'] = true 时整体禁用
```

### 2.6 评论区（复用 forum 评论能力）
方案：forum-service `comment` 表已有，新增 `biz_type`（ARTICLE/MESSAGE/LINK）与 `biz_id` 字段：
```sql
ALTER TABLE `comment` ADD COLUMN `biz_type` VARCHAR(16) DEFAULT 'POST' COMMENT 'POST/ARTICLE/MESSAGE';
ALTER TABLE `comment` ADD COLUMN `biz_id`   BIGINT  NULL;
```
- 文章评论：`biz_type='ARTICLE', biz_id=article.id`
- 留言板：`biz_type='MESSAGE', biz_id=0`（或独立表）
- 前端 `<CommentPanel :biz-type="'ARTICLE'" :biz-id="id" />` 统一组件，含：输入框（昵称/邮箱/内容）、列表、楼中楼回复、删除（本人/管理员）
- 匿名规则：不登录可评论（昵称必填），管理员登录后显示管理标识可删除

### 2.7 深色/浅色切换 useTheme.js
```js
// 本项目已基于 CSS 变量实现暗色，升级为显式开关：
const theme = ref(localStorage.getItem('theme') || 'dark')
const setTheme = (t) => {
  theme.value = t
  document.documentElement.classList.toggle('light', t === 'light')
  localStorage.setItem('theme', t)
}
// 页面载入时先读 localStorage 再挂类，避免闪烁（head 内联一段脚本优先执行）
```

### 2.8 中控台 ConsolePanel.vue
- 右下角悬浮按钮（与音乐播放器并排）
- 展开面板：站点统计（文章/标签/分类）、主题切换、音乐控制、快捷入口（分类/标签/归档/随机）、当前页 URL
- 移动端转为底部抽屉（`position: fixed; bottom: 0;` 全宽）

### 2.9 右键菜单 CustomContextMenu.vue
```js
// 全局 mousedown 监听：右键且非 native 菜单时 preventDefault 并定位渲染
// 菜单项动态生成：
//   - 命中 <a>  → 新窗口打开 / 复制链接地址
//   - 命中 <img> → 复制图片 / 下载图片 / 新窗口打开图片
//   - 命中纯文本 → 复制选中文本 / 粘贴文本 / 引用到评论
//   - 通用项：站内搜索 / 百度搜索 / 播放音乐 / 上一首 / 下一首 / 查看所有歌曲 / 复制歌名
//             / 随便逛逛 / 博客分类 / 文章标签 / 复制地址 / 关闭热评 / 深色模式 / 轉為繁體
// 点击任意菜单项后立即销毁（keydown Esc 亦销毁）
```
> 繁简转换实现：引入 opencc-js 在前端做运行时转换，或提供繁简两套文案包（建议 opencc-js，约 60KB）。

### 2.10 音乐播放器升级
- 现有 `views/music/` 保留 UI，数据源替换为 `/api/music/playlist`（后端代理）
- 新建全局 `musicStore`（Pinia）：`{ playlist, index, playing, progress, volume }`，悬浮球 + 快捷键共用
- 页面切换不断流（组件挂 App 级，不随路由销毁）

---

## 3. 管理端扩展（admin-service + admin 前端）

| 菜单 | 页面 | 能力 |
|---|---|---|
| 博客 → 友链管理 | `admin/blog-links.vue` | 列表/新增/编辑/删除/审核（PENDING→PUBLISHED）/排序 |
| 博客 → 公告 | `admin/blog-notice.vue` | 编辑启用公告 |
| 博客 → 站点配置 | `admin/blog-settings.vue` | 打赏二维码、社交链接、ICP、签名、页脚友链分组 |
| 相册管理 | 复用现有 media 上传 + `album` 分组字段 | 分组/排序 |
| 工具库管理 | 扩展现有 `tools` 接口 | 分类/卡片/下载链接 CRUD |

---

## 4. 路由与菜单整合

```js
// router 新增（挂 Web3Layout 或独立 BlogLayout）
{
  path: '/blog',
  component: BlogLayout,
  children: [
    { path: '',          name: 'BlogHome',    component: () => import('@/views/blog/index.vue') },
    { path: 'post/:id',  name: 'BlogDetail',  component: () => import('@/views/blog/detail.vue') },
    { path: 'categories',name: 'BlogCates',   component: () => import('@/views/blog/categories.vue') },
    { path: 'tags',      name: 'BlogTags',    component: () => import('@/views/blog/tags.vue') },
    { path: 'archives',  name: 'BlogArchives',component: () => import('@/views/blog/archives.vue') },
    { path: 'link',      name: 'BlogLink',    component: () => import('@/views/blog/link.vue') },
    { path: 'comments',  name: 'BlogComments',component: () => import('@/views/blog/comments.vue') },
  ],
}
// 顶栏 Web3Nav 增加「博客」下拉：分类/标签/归档/友人帐/留言板
```

---

## 5. 关键实现细节与坑位

| # | 细节 | 实现要点 |
|---|---|---|
| 1 | 阅读进度条 | 监听 `window.scroll`，`progress = scrollTop / (scrollHeight - clientHeight)`，顶部 fixed 2px 细条，`transition: width .1s` |
| 2 | TOC 高亮 | IntersectionObserver 观察各 h2/h3 锚点，命中即高亮目录项并 `scrollIntoView({block:'nearest'})` 目录滚动 |
| 3 | 代码块复制 | 渲染后遍历 `pre>code` 加复制按钮，`navigator.clipboard.writeText(code.textContent)` |
| 4 | 未读标记 | key=`read:{articleId}`，进入详情 set，列表渲染时读取；批量清除入口可选 |
| 5 | 随机文章 | `/random` 接口后端 `ORDER BY RAND() LIMIT 1`（数据量小可接受） |
| 6 | 弹幕（P2） | 评论渲染为绝对定位气泡，从右往左 CSS 动画 `translateX(100vw→-100%)`，随机 top/时长，容器 `overflow:hidden` 且 pointer-events:none |
| 7 | 加载动画 | App 级 overlay，`document.fonts.ready + window.load` 双条件后 fade-out 并卸载；为防白屏，overlay 背景用深色而非透明 |
| 8 | 深色闪烁 | `index.html <head>` 内联：`<script>if(localStorage.theme==='light')document.documentElement.classList.add('light')</script>` |
| 9 | 网易云代理 | 部署 NeteaseCloudMusicApi 容器（可 Docker），本项目 `music-service` 仅转发并缓存；注意歌曲 URL 有时效（约1天），客户端缓存 key 含过期时间 |
| 10 | 侧栏最新评论 | `GET /api/forum/comments/recent?limit=5` 聚合（biz_type 不限），带文章标题联查 |
| 11 | 图片懒加载 | 现有 `v-lazy` 指令或原生 `loading="lazy"`；占位用 loading3.gif 风格深色动图 |
| 12 | 文章字数统计 | 后端 `content.replace(/[#>*`_\-\[\]()!|]/g,'').length` 粗略计数即可，300字/分钟 |

---

## 6. 配置项（Nacos / 环境变量）

| 配置 | 默认 | 说明 |
|---|---|---|
| `MUSIC_API_BASE` | `http://localhost:3000` | NeteaseCloudMusicApi 地址 |
| `MUSIC_DEFAULT_PLAYLIST` | `652135520` | 默认网易云歌单 ID |
| `BLOG_PAGE_SIZE` | `10` | 文章分页大小 |
| `BLOG_ANON_COMMENT` | `true` | 是否允许未登录评论 |
| `BLOG_AI_SUMMARY_KEY` | 空 | LLM Key，非空才显示 AI 摘要卡片（走现有 AI 代理） |

---

## 7. 验收链路（全链路走通标准）

1. **首页**：三栏布局完整，文章卡片真实数据（封面/分类/标签/日期/摘要），分页正常，未读红点显示
2. **详情**：目录高亮、上一篇/下一篇、相关推荐、评论区（匿名发帖→列表出现→弹幕开启可见）
3. **氛围**：Shift+D 切主题、Shift+R 随机跳转、Shift+S 搜索、右键菜单各项可用、中控台统计真实
4. **音乐**：悬浮球播放网易云歌单，页面切换不断流，Shift+M 暂停/继续
5. **侧栏**：作者卡/最近更新/网站资讯/最新评论/统计数字 全部真实接口
6. **页脚**：友链分组（资产测绘/云沙箱/网络检测）、协议三页、备案号
7. **管理端**：友链审核、公告编辑、打赏 QR 配置生效
8. **移动端**：375px 无横向滚动，左右栏收敛，菜单可用