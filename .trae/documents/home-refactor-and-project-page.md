# 首页瘦身 + 新增项目展示页 + 社区/服务优化 + Agent 聊天修复

## Context
用户反馈首页内容太杂、排版乱、个人介绍堆在首页；要求新增「项目框架」综合展示页承载技术架构/作品/动效，首页瘦身为简洁逻辑性布局；社区及其他服务按最前沿技术优化；Agent 聊天返回错误提示需修复。目标：整洁、简洁、逻辑性强、框架思维缜密。

## 用户确认的决策
- 新页面定位：**综合项目展示页** `/project`（TechMatrix + ShowcaseGrid + MotionLab 完整迁移）
- 首页保留：Hero星际终端 + 精简文章卡(3篇) + 精简作品预览(2-3个) + SiteMap精简导航条
- Agent 问题：返回错误提示（非无响应，非内容差，非交互bug）

---

## 实施步骤

### 步骤1：新增 /project 综合项目展示页
**新建** `frontend/src/views/project/index.vue`
- 页面结构（逻辑递进）：
  1. 页面 Hero（小标题 + 项目总览统计）
  2. `<TechMatrix />` 技术矩阵与系统架构（4架构支柱+技能矩阵）
  3. `<ShowcaseGrid />` 核心作品与 Demo 橱窗（6个完整）
  4. `<MotionLab />` 动效实验室
- 复用现有组件（直接 import，不重写）：`@/components/home/TechMatrix.vue`、`ShowcaseGrid.vue`、`MotionLab.vue`
- 加 PageBack 返回首页

**修改** `frontend/src/router/index.js`（约 L50 展示/可视化区块）：
```js
{ path: 'project', name: 'Project', component: () => import('@/views/project/index.vue'), meta: { requiresAuth: false } },
```

### 步骤2：首页瘦身重构 GhibliHome.vue
**修改** `frontend/src/views/home/GhibliHome.vue`
- 移除：`<TechMatrix />`、完整 `<ShowcaseGrid />`、`<MotionLab />`
- 保留：Hero 星际终端（不变）
- 新增：精简作品预览（新建 `HomeShowcaseMini.vue`，2-3个代表作 + "查看全部项目→"链接到 /project）
- 保留：`<ArticleCards />`（已是3篇精简版）
- 改造：`<SiteMap />` → 新建 `HomeQuickNav.vue` 单行快速导航条（横向 chip，8个主入口）
- 布局逻辑：Hero → 作品预览(2-3) → 文章(3篇) → 快速导航条 → Footer
- 移除并排两栏（之前加的 home-grid-2col），改为清爽单列纵向流
- 保留回到顶部按钮

**新建组件**：
- `frontend/src/components/home/HomeShowcaseMini.vue`：从 ShowcaseGrid 的6个里挑2-3个代表作（chat/galaxy/chart），精简卡片
- `frontend/src/components/home/HomeQuickNav.vue`：横向 chip 导航（首页/项目/博客/社区/量化/商城/工具/关于）

### 步骤3：关于我页面确认（不改）
`frontend/src/views/about/index.vue` 已有9板块（HERO/关于我/技能矩阵/经历/作品/工具链/联系方式/留言板/坐标），足够承载个人介绍，首页不再重复个人介绍。无需修改。

### 步骤4：社区及其他服务优化
**修改** `frontend/src/views/community/index.vue`：
- 排版整洁化：卡片间距/字体层级统一
- 加"热门标签云"侧边栏
- 空状态优化（无内容时引导发布）

**检查优化**（轻量，仅排版/空状态）：
- `frontend/src/views/shop/index.vue` 商品列表卡片整洁化
- `frontend/src/views/tools/index.vue` 工具卡片逻辑分组
- `frontend/src/views/software/index.vue` 软件卡片
- `frontend/src/views/resources/index.vue` 资源卡片

### 步骤5：Agent 聊天修复（返回错误提示）
**根因诊断**：AgentWorkflowService.chat() 调 `firstAvailableProvider()` 按非空 api-key 选直连 LLM，若无任何 LLM API Key 配置 → 返回 `fallbackHint()` 文案"大模型上游暂未连通"。

**修复**：
1. **后端确认** `backend/target/start-java.ps1`：ai-proxy-service(8093) 启动时是否注入 LLM API Key 环境变量（DEEPSEEK_API_KEY 等）。若未配，需用户在 start-java.ps1 或 .env 补充至少一个 LLM Key。
2. **前端改进** `frontend/src/views/agent/index.vue` 的 `send()`/`chatStream()` catch 块：
   - 区分错误类型：502/超时 → "AI 服务暂不可用，请稍后重试或检查 ai-proxy-service"; fallback 文案 → 提示"请在后台 AI代理配置 中填入 LLM API Key"
   - 加"AI服务状态"指示徽章（调用 `/api/ai/llm-status` 或 agent 接口探测）
3. **后端** `AgentWorkflowService.fallbackHint()`：返回更明确的错误码，前端据此区分提示

**修改文件**：
- `frontend/src/views/agent/index.vue`（send/chatStream catch + 状态徽章）
- `backend/ai-proxy-service/.../AgentWorkflowService.java`（fallback 带错误码）
- `backend/target/start-java.ps1`（确认 LLM Key 环境变量，可能需用户填入真实 Key）

---

## 关键复用
- 复用组件：`@/components/home/TechMatrix.vue`、`ShowcaseGrid.vue`、`MotionLab.vue`、`ArticleCards.vue`、`SectionHead.vue`
- 复用 API：`@/api/agent.js` 的 `agentChatStream`/`consumeAgentStream`
- 复用路由模式：参照 `architecture`/`nails` 等独立页路由写法
- 复用 PageBack 组件

## 验证
1. `cd frontend && npx vite build --mode development` 无错误
2. 访问 `/` 首页：Hero + 2-3作品预览 + 3文章 + 快速导航条，纵向大幅缩短
3. 访问 `/project`：TechMatrix + 6作品 + 动效实验室完整展示
4. 访问 `/about`：9板块个人介绍完整
5. 访问 `/agent`：发消息，若 ai-proxy-service 已启动+配 Key → 正常流式回复；未配 → 明确错误提示而非通用"请求失败"
6. 访问 `/community`：排版整洁，空状态有引导

## 注意
- Agent 聊天要真正可用，最终需要用户在 start-java.ps1 或 .env 配置至少一个真实 LLM API Key（如 DEEPSEEK_API_KEY），这是运行时配置，代码层面只能改进错误提示和状态检测
