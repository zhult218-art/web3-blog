# Web3 Portal 前端

Vue 3 + Vite 5 单页应用，Web3 极简未来科技风，全站特效（3D 粒子背景、宿傩火焰光标、GSAP 动效）。

## 技术栈

- Vue 3（`<script setup>` 组合式 API）
- Vite 5（构建工具）
- Pinia（状态管理）
- Vue Router 4（前端路由）
- TailwindCSS + 自定义 CSS 变量（样式）
- GSAP + ScrollTrigger（动效）
- Three.js（3D 场景：首页终端、/three 粒子页、全站背景）
- ECharts（量化图表）
- axios（HTTP 请求，封装于 `src/api/request.js`）
- crypto-js / qrcode（工具页功能）

## 目录结构

```text
frontend/
├── index.html                # HTML 入口
├── vite.config.js            # Vite 配置（别名 @、代理、构建）
├── tailwind.config.js        # Tailwind 配置
├── postcss.config.js         # PostCSS 配置
├── nginx.conf                # 生产环境 Nginx 配置（配合 Dockerfile）
├── Dockerfile                # 前端镜像构建（npm run build → nginx 托管 dist/）
├── public/
│   ├── favicon.ico           # 站点图标
│   └── cursors/              # 宿傩火焰箭头光标素材（.cur 系统光标 + .png 备用）
└── src/
    ├── main.js               # 应用入口：创建 Vue 实例、注册 Pinia/Router/全局指令
    ├── App.vue               # 根组件：滚动进度条、极光跟随、访问上报、全局注册
    ├── api/                  # API 层：每个业务模块一个文件（axios 实例 + 接口函数）
    │   ├── request.js        # axios 封装：baseURL、token 注入、401 跳登录
    │   ├── user.js           # 用户注册/登录/资料/权限
    │   ├── blog.js           # 文章、分类、标签
    │   ├── forum.js          # 帖子、评论、点赞
    │   ├── shop.js           # 商品、订单、支付
    │   ├── media.js          # 音乐、视频、播放列表
    │   ├── tools.js          # 脚本工具
    │   ├── software.js       # 软件
    │   ├── resources.js      # 资源上传/下载
    │   ├── quant.js          # 量化策略、行情（含 A股 akshare 接口）
    │   ├── akshare.js        # A股数据接口（quant-py-service）
    │   ├── jarvis.js         # JARVIS AI 语音助手
    │   ├── admin.js          # 管理后台接口
    │   └── upload.js         # 文件上传
    ├── assets/
    │   └── styles/main.css   # 全局样式：CSS 变量、通用组件类、动效、光标隐藏
    ├── components/           # 可复用组件（按用途分组）
    │   ├── common/           # 通用：Toast 提示 / Modal 弹窗 / Loading / Pagination / GlobalPlayer 播放条 / XingTuAssistant 星途 AI 助手
    │   ├── home/             # 首页专属：ArticleCards / SectionHead / ShowcaseGrid / TechMatrix / GlowCursor 火焰光标
    │   └── layout/           # 布局：Web3Nav 导航 / ThreeBackground 3D 星空背景 / ParticleBackground 粒子背景
    ├── layouts/
    │   └── Web3Layout.vue    # 全站布局：导航 + 3D 背景 + 全局播放器 + 光标
    ├── router/
    │   └── index.js          # 路由表：懒加载、权限 meta.perm、Admin 鉴权门禁
    ├── stores/
    │   └── modules/          # Pinia 状态：auth（登录态）/ cart（购物车）/ player（播放器）/ toast（提示）
    ├── utils/
    │   ├── page.js           # 分页参数工具
    │   └── permissions.js    # 前端权限码表（hasPerm / effectivePerms）
    ├── directives/
    │   └── reveal.js         # v-reveal 系列滚动入场指令
    └── views/                # 页面视图（按业务模块分目录）
        ├── home/             # 首页 GhibliHome（宫崎骏风格 + 3D 终端 + 统计数字滚动）
        ├── community/        # 社区（index 列表 / create 发帖 / detail 详情）
        ├── shop/             # 商城（index / detail / cart）
        ├── media/            # 媒体（index 音视频 / book 书籍）
        ├── music/            # 音乐（index 列表 / player 播放页 / musicLogic 播放逻辑 / lyricsData 歌词数据）
        ├── quant/            # 量化（index 大盘 + components/ 龙虎榜/资金流/研报/融资融券表）
        ├── tools/            # 工具（index 列表 / ToolPage 工具页）
        ├── software/         # 软件下载
        ├── resources/        # 资源中心
        ├── upload/           # 上传页
        ├── ai/               # AI 问答
        ├── three/            # 3D 粒子体验页
        ├── architecture/     # 架构图页
        ├── album/            # 相册
        ├── link/             # 友链
        ├── comments/         # 留言板
        ├── about/            # 关于
        ├── login/            # 登录/注册
        ├── profile/          # 个人中心（index / orders 订单）
        ├── pay/              # 支付（index / success 成功页）
        ├── privacy/          # 隐私政策
        └── admin/            # 管理后台（AdminLayout + dashboard/users/blogs/orders/media/quant/traffic/settings）
```

## 常用命令

```bash
npm install        # 安装依赖
npm run dev        # 开发模式，http://localhost:5173
npm run build      # 生产构建，输出到 dist/
npm run preview    # 本地预览构建产物，http://localhost:4173
```

## 开发代理（vite.config.js）

| 前缀 | 目标 | 说明 |
|---|---|---|
| `/api` | http://localhost:8080 | Gateway，路径去掉 `/api` 前缀后转发 |
| `/pyquant` | http://localhost:9006 | Python 量化数据服务 |
| `/vr` | http://localhost:8900 | Vibe-Research A股数据 |

## 关键约定

- **请求封装**：所有接口走 `src/api/request.js`，自动附带 `Authorization` 请求头（token 存 localStorage），401 时跳转登录页。
- **权限控制**：路由 `meta.perm` 声明所需权限，`utils/permissions.js` 中的 `hasPerm()` 判断；`/admin` 路由有额外门禁（sessionStorage 校验），仅能由导航内"管理后台"入口进入。
- **全局特效**：`GlowCursor.vue` 注册系统级宿傩火焰箭头光标（CSS `.cur` 不依赖 JS）+ 7 粒火焰粒子贴附箭头尖端；`ThreeBackground.vue` 提供全站 3D 粒子星空。
- **滚动动效**：`v-reveal / v-reveal-left / v-reveal-right / v-reveal-scale` 指令实现元素入场动画，配合 `App.vue` 中的 IntersectionObserver 自动注册。
- **样式体系**：全局 CSS 变量（`--color-primary` 等）定义于 `assets/styles/main.css`，玻璃拟态（glass-panel）、霓虹按钮（web3-btn）等通用类也在此定义。
- **登录态恢复**：`App.vue` 挂载时若本地有 token 则调用 `fetchProfile()` 恢复用户信息。
- **访问上报**：`App.vue` 会向 admin-service 上报访问（IP/地理位置，会话级 6h 去重）。
