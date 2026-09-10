# 项目交接文档 — Web3个人站点前端迭代

> 复制本文件全部内容到新会话即可。AI 收到后应先复述理解并指出歧义，经你确认后再继续执行。

---

## 一、最终目标

基于现有前后端代码框架，对 Web3 个人站点前端进行**增量迭代优化**，不重构整套项目，修复全部已知缺陷，完成视觉风格重塑与功能补全，最终交付一套流畅运行、风格统一的全栈 Web3 门户。

---

## 二、关键背景

- **技术栈**：Vue 3 + Vite + Pinia + Tailwind CSS（前端，端口 5173/5174）；Spring Cloud Gateway + 12 个微服务（后端，端口 8900 网关）
- **项目路径**：`F:\project\my-blog\web3-blog\`，前端在 `frontend/` 子目录
- **设计风格**：赛博朋克 + 二次元 + 元宇宙，紫色 `#a855f7` + 青色 `#06b6d4` 双主色，玻璃拟态面板，粒子背景，自定义光标，霓虹边框呼吸动效
- **命名变更**：原"博客"和"论坛"已合并为一个模块，命名为**"社区"（Community）**，路由统一为 `/community`
- **AI 助手**：只保留**星途**全息AI助手（唤醒词"星途，星途"），已移除 Jarvis 助手
- **构建结果**：`npx vite build` 通过，`✓ built in ~28s`

---

## 三、已确认的事实

1. Axios 拦截器返回的是 `ApiResponse` 对象本身（`response.data` 就是 `{code, msg, data: {...}}`），不是嵌套再包一层
2. 后端 JWT 登录接口返回格式：`{code, msg, data: {token, user: {...}}}`
3. 后端接口全量清单见 `src/api/` 目录（user.js, blog.js, forum.js, shop.js, media.js, tools.js, software.js, resources.js, admin.js, quant.js, jarvis.js）
4. `features/` 目录（整个）为死代码，零引用，已删除
5. 旧 `views/blog/` 和 `views/forum/` 已删除，路由已合并到 `/community`
6. 路由使用 `createWebHistory()`（无 hash 模式）
7. 管理员页面需鉴权（`requiresAuth: true`），通过路由守卫拦截
8. 后端可能尚未实现媒体素材删除接口（前端调用 `/music/:id` 和 `/video/:id` DELETE，若后端未实现会 405）
9. 量化页面部分数据依赖 Vibe-Research 服务（端口 8900），可能未启动导致空数据
10. `upload/index.vue` 原有一个预存 bug：`isDragging` 声明为 `const` 但模板中赋值，已修复为 `let`

---

## 四、长期偏好

1. **增量开发**：禁止重构整套项目，只修改和扩展现有代码
2. **复用优先**：先检索项目内已有接口、组件、样式、工具函数，最大化复用
3. **中文注释**：新增功能代码添加中文注释
4. **事件隔离**：3D 场景（WASD）、星途全息UI、弹窗之间做好按键冲突隔离
5. **空数据友好展示**：无数据时显示友好提示，不用无限加载 spinner
6. **异常降级**：API 失败时静默降级，不阻塞页面其他功能
7. **每块功能自测**：修改后检查控制台报错、页面白屏、接口请求失败
8. **功能开发顺序**：登录状态 → 量化数据 → 媒体播放 → 在线工具 → UI风格 → 3D场景稳定性

---

## 五、硬性规则

1. 不得删除 `src/views/` 下的任何剩余视图文件（album, link, comments, about, privacy, pay 等），除非用户明确要求
2. 不得修改后端代码（交接范围仅前端）
3. 不得更换设计系统的主色（紫色 + 青色），不得引入新的全局样式冲突
4. 路由变更必须先添加旧路由的 redirect，不得静默删除
5. 所有 API 调用必须经过 `src/api/` 层，不得在组件内直接 `axios.get`
6. 新增组件放 `src/components/common/`，新增视图放 `src/views/` 对应目录
7. 修改构建产物（dist/）相关配置前需征得同意

---

## 六、输出格式要求

- 代码修改使用 Edit/Write 工具，禁止用 Bash 重写文件
- 构建验证使用 `npx vite build`，禁止 `npm run dev` 手动观察
- 浏览器验证使用 preview_screenshot / preview_snapshot / preview_eval，禁止让用户手动检查
- 任务进度使用 TaskCreate/TaskUpdate 跟踪
- 复杂探索使用 Agent 工具（Explore 子代理）

---

## 七、已完成的工作

### 7.1 登录/注册修复（Task #25）
- `src/stores/modules/auth.js`：登录/注册后立即调用 `fetchProfile()` 拉取完整用户信息
- `src/components/layout/Web3Nav.vue`：登录用户显示渐变圆形头像 + 下拉菜单（个人中心/我的订单/管理后台/退出登录），游客显示登录按钮
- 下拉菜单退出登录后强制刷新页面，确保全站状态同步

### 7.2 博客+论坛合并为社区（已完成）
- 新建 `src/views/community/index.vue`：合并 Feed，标签筛选（全部/文章/讨论/视频/热门），排序（最新/最热/置顶），右侧边栏（热门标签/活跃用户/在线统计）
- 新建 `src/views/community/detail.vue`：文章详情 + 评论系统（头像、时间戳、点赞、作者标签）
- 新建 `src/views/community/create.vue`：创建表单（文章/讨论/视频三种类型）
- `src/router/index.js`：`/blog` → `/community`，`/forum` → `/community` 重定向
- `src/api/blog.js`：新增 re-export `likePost as likeForum`, `createPost`, `createComment` from forum.js

### 7.3 全站 UI 风格（已完成）
- 完整设计系统在 `src/assets/styles/main.css`（625 行）：
  - glass-panel / glass-panel-sm 玻璃面板
  - web3-btn / web3-btn-outline / web3-btn-ghost 按钮体系
  - web3-input 输入框
  - motions-card 3D 悬浮卡片
  - glitch 文字故障特效
  - holo-bar 全息线条
  - neon-border 霓虹呼吸边框
  - matrix-text 矩阵字体
  - aurora 鼠标跟随光效
  - 自定义光标（十字准星 + 跟随光圈）
  - skeleton 骨架屏加载
  - 页面切换动画（fade + slide）
- 自定义光标组件：`src/components/common/CustomCursor.vue`
- 星途全息助手：`src/components/common/XingTuAssistant.vue`（唤醒词"星途，星途"）

### 7.4 3D 首页场景（已完成）
- `src/views/home/index.vue`：英雄区、统计卡片、9 个模块卡片（带 hover glow 效果）
- `src/views/home/MetaverseScene.vue`（603 行）：完整 Three.js RPG 场景
  - 80×80 地面、20 棵树、8 块岩石、7 个传送门（带 Canvas 标签）
  - 400 星粒子系统、角色胶囊体、紫色光晕
  - WASD 移动 + 平滑旋转 + 边界碰撞
  - 相机 lerp 跟随（0.08）
  - 传送门 proximity 检测 + 进入按钮
  - K 线图 + 4 个 ECharts 回测图表

### 7.5 媒体模块（Task #27）
- `src/views/media/index.vue`：音乐列表 + 视频列表，真实 `<audio>` / `<video>` 元素，全局悬浮播放器
- `src/views/music/index.vue`（204 行）：双栏布局（播放列表 + 正在播放侧边栏），8 个分类标签，演示数据 fallback
- `src/components/common/GlobalPlayer.vue`（新建）：全局浮动播放条，跨页面持续播放，上一首/播放暂停/下一首/音量/关闭
- `src/views/admin/media.vue`（新建）：管理员素材管理页，音乐/视频分类上传（名称/艺术家/标签/封面/简介），拖拽上传，已上传列表展示
- `src/App.vue`：注册 GlobalPlayer 为全局组件
- `src/router/index.js`：新增 `/admin/media` 路由

### 7.6 量化数据优化（Task #28）
- `src/views/quant/index.vue`（772 行）：
  - 所有 `<Loading>` 替换为"暂无XX数据"友好提示
  - 新增资产配置饼图（A股/债券/基金/ETF/量化/现金）
  - ECharts 图表：净值曲线、回撤曲线、月度热力图、收益分布、资产配置
  - `chartInstances` 数组统一管理，窗口 resize 自动适配
  - Promise.allSettled 防止单个 API 失败阻塞全部
  - 懒加载：标签页切换时才拉取对应数据

### 7.7 在线工具箱（Task #26）
- `src/views/tools/ToolPage.vue`：6 个占位工具全部实现
  - 正则表达式测试（标志位 g/gi/gim，实时匹配高亮）
  - Hash 生成器（MD5/SHA1/SHA256/SHA512，单键计算，支持复制）
  - 二维码生成（128-512 可调尺寸，下载 PNG）
  - 颜色转换（HEX/RGB/HSL 互转，实时预览，点击复制）
  - URL 编解码（encode/decode）
  - 文本差异对比（并排对比，行号，差异行高亮）
- 新增 npm 依赖：`crypto-js`、`qrcode`

### 7.8 其他修复
- `src/router/index.js`：修复 scrollBehavior 中 `querySelector` 对 hash 的异常处理
- `src/views/upload/index.vue`：修复 `isDragging` const 声明 bug → `let`
- `src/stores/modules/auth.js` token 提取 bug 修复（之前的会话已完成）
- 删除死代码目录 `src/features/` 和过期的 `src/views/blog/`、`src/views/forum/`

---

## 八、重要决策及理由

| 决策 | 理由 |
|------|------|
| 博客+论坛合并为"社区" | 功能高度重叠，合并减少维护成本，统一 Feed 流体验更好 |
| 保留 XingTu 移除 Jarvis | 用户明确要求只要星途，两个 AI 助手存在按键冲突风险 |
| 全局播放器放 App.vue | 必须跨页面持续播放，放任何子页面都会被销毁 |
| 量化图表用 ECharts | 项目已引入 echarts，K 线图已在用，保持一致 |
| 媒体上传复用 resources/upload 接口 | 后端尚无独立媒体上传接口，先复用以保证功能可用 |
| Hash/二维码引入 npm 包 | 纯手写实现复杂度高且易错，用成熟库更可靠 |
| 空数据用文字提示不用 spinner | 用户明确要求"没有的数据就显示空就好，不用一直在加载" |
| 量化数据不做静态 mock | 用户明确要求"所有图表、指标自动从接口拉取实时数据，禁止写死静态模拟数据" |

---

## 九、被否定的方案

| 方案 | 否定原因 |
|------|----------|
| 用 hash 模式路由解决 querySelector 报错 | 项目已用 createWebHistory，改路由模式影响太大 |
| 为量化图表写静态 mock 数据 | 用户要求从接口拉取实时数据 |
| 在量化页面加载时显示全屏 Loading | 用户要求标签懒加载，不要一次性全加载 |
| 保留两个 AI 助手 | 用户明确只要星途 |
| 博客和论坛保持两个独立模块 | 用户要求合并为一个更好听的名称 |

---

## 十、当前进度

所有 7 大任务（Task #25-#29）**已标记为 completed**，构建通过，浏览器验证关键页面正常。

---

## 十一、尚未完成的任务

| 优先级 | 任务 | 说明 |
|--------|------|------|
| **高** | 后端媒体素材 API | 前端已实现上传/列表/删除界面，但后端可能缺少 `/music/upload`、`/video/upload`、`DELETE /music/:id`、`DELETE /video/:id` 接口。需在后端补充或确认已有接口路径 |
| **高** | 后端量化数据服务 | 量化页面部分标签（市场热点、行业板块）依赖 Vibe-Research（端口 8900），需确认该服务是否已实现并部署 |
| **中** | 管理员页面真实 API 对接 | admin/users.vue、admin/blogs.vue、admin/orders.vue 仍使用硬编码 mock 数据，需对接后端管理接口 |
| **中** | 登录后页面状态刷新 | 当前退出登录用 `window.location.href = '/'` 强制刷新，理想做法是路由守卫 + 导航守卫自动响应 auth store 变化 |
| **中** | 社区 Feed 真实数据接入 | 当前 community/index.vue 的 feed 数据来自 API 但可能返回空，需确认后端 `/article/list` 和 `/post/list` 接口有真实数据 |
| **低** | 音乐/视频文件存储 | 管理员上传的媒体文件需后端存储方案（本地磁盘 / OSS / 数据库 BLOB） |
| **低** | 代码分割优化 | 构建有 chunk size warning（643KB + 1158KB），可按需拆分 |
| **低** | 管理后台 settings 保存功能 | settings.vue 的 "Save Changes" 按钮无 handler |
| **低** | 3D 场景与页面按键隔离 | WASD 在 3D 模式下可能会影响页面滚动，需做条件判断 |

---

## 十二、不能随意修改的内容

1. **后端代码**：`backend/` 目录全部文件不在本次交接范围内
2. **`src/api/request.js`**：axios 实例配置，拦截器逻辑，除非发现 bug
3. **`src/router/index.js`** 的现有路由结构：只增不减，删除路由前必须确认
4. **`src/assets/styles/main.css`** 的 CSS 变量体系（`:root` 中的 `--color-*` 定义）
5. **`src/stores/modules/auth.js`** 的 token 存储/读取逻辑（localStorage 操作）
6. **`src/components/common/XingTuAssistant.vue`**：星途助手的唤醒词和语音回复逻辑
7. **`src/views/home/MetaverseScene.vue`**：3D 场景的核心渲染逻辑（除非要修复 bug）

---

## 十三、新会话接下来应该先做什么

按以下顺序执行，每步完成后自测再继续下一步：

1. **运行 `npx vite build`** 确认构建无报错，这是基线
2. **启动 dev server**：`cd F:\project\my-blog\web3-blog\frontend && npx vite --host --port 5174`
3. **浏览器验证关键页面**：
   - 打开 `http://localhost:5174/` → 确认首页渲染
   - 打开 `http://localhost:5174/login` → 确认登录页
   - 打开 `http://localhost:5174/community` → 确认社区 Feed
   - 打开 `http://localhost:5174/quant` → 确认量化标签页
   - 打开 `http://localhost:5174/tools/json` 等 → 确认工具页面
   - 检查浏览器 Console 是否有红色报错
4. **处理"尚未完成"中的高优先级项**（后端 API 对接）
5. **处理"尚未完成"中的中优先级项**（管理员页面真实 API、社区 Feed 真实数据）
6. **最后做一轮全页面回归测试**：首页、社区、商城、媒体、量化、工具、软件、资源、音乐、个人中心、管理后台

---

## 十四、项目文件结构速查

```
frontend/src/
├── api/                    # API 层（12 个文件）
│   ├── request.js          # axios 实例 + 拦截器
│   ├── user.js             # 登录/注册/用户信息
│   ├── blog.js             # 文章 + re-exports from forum
│   ├── forum.js            # 帖子/评论
│   ├── media.js            # 音乐列表/视频列表
│   ├── quant.js            # 量化数据（quant-service + Vibe-Research proxy）
│   ├── shop.js             # 商品/订单/支付
│   ├── tools.js            # 脚本列表
│   ├── admin.js            # 管理后台
│   ├── resources.js        # 资源上传/下载
│   └── jarvis.js           # 星途助手 API
├── stores/modules/
│   ├── auth.js             # 认证状态（token + user）
│   ├── cart.js             # 购物车
│   ├── player.js           # 音乐播放器
│   ├── toast.js            # Toast 通知
│   └── particle.js         # 粒子加载标记
├── components/
│   ├── common/             # 通用组件（16 个）
│   │   ├── XingTuAssistant.vue  # 星途全息助手 ⭐核心
│   │   ├── CustomCursor.vue     # 自定义光标
│   │   ├── GlobalPlayer.vue     # 全局音乐播放器
│   │   ├── MusicPlayer.vue      # 独立音乐播放器
│   │   ├── Modal.vue            # 弹窗
│   │   ├── Pagination.vue       # 分页
│   │   └── ...
│   └── layout/
│       ├── Web3Nav.vue          # 导航栏 ⭐核心
│       ├── ThreeBackground.vue  # Three.js 粒子背景
│       └── ParticleBackground.vue # Canvas 粒子
├── views/
│   ├── home/
│   │   ├── index.vue            # 首页
│   │   └── MetaverseScene.vue   # 3D RPG 场景 ⭐核心
│   ├── community/               # 合并后的社区模块
│   │   ├── index.vue            # Feed 列表
│   │   ├── detail.vue           # 详情 + 评论
│   │   └── create.vue           # 创建内容
│   ├── quant/
│   │   └── index.vue            # 量化交易平台 ⭐最大视图（772 行）
│   ├── tools/
│   │   ├── index.vue            # 工具箱首页
│   │   └── ToolPage.vue         # 工具页面（10 个工具）
│   ├── admin/                   # 管理后台
│   │   ├── dashboard.vue        # 仪表盘
│   │   ├── media.vue            # 素材管理 ⭐新增
│   │   └── ...
│   ├── media/index.vue          # 媒体中心
│   ├── music/index.vue          # 音乐馆
│   ├── login/index.vue          # 登录/注册
│   └── ...
├── router/index.js              # 路由配置 ⭐核心
├── App.vue                      # 根组件
├── main.js                      # 入口
└── assets/styles/main.css       # 全局样式（设计系统）⭐核心
```

---

## 十五、关键联系方式

- 前端端口：`5174`（5173 被占用时自动切换）
- 后端网关：`http://localhost:8900`
- 量化代理前缀：`/vr` → 转发到端口 8900
- API 基础路径：`/api` → 转发到网关

---

*文档生成时间：2026-08-05 最新会话*
*若信息冲突，以最后一次明确确认的内容为准。*
