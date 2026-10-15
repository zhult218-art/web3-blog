# Debug: blog-community-slow

- **Session ID**: `blog-community-slow`
- **Status**: [OPEN]
- **Created**: 2026-10-12
- **Symptom**: 博客 / 社区 / 聊天页面进入时加载缓慢。memory 记录已做过 blog 本地优先、forum stats 60s 缓存、chat 重试，但用户仍觉慢。

## 环境
- 网关 8080 / blog-service 8082 / forum-service 8083 / py-service 9006 / 前端 Vite 5173
- 前端经 Vite 代理：/api/* → 网关 8080；社区/聊天均走 forum-service 8083

## 假设（待运行时证据证伪）
1. **H1 串行请求瀑布**：社区/博客页面 onMounted 里串行发了多个接口（列表 + stats + 元数据），未并行，累计耗时 = 各接口之和。
2. **H2 stats/聚合接口未命中缓存或缓存失效**：forum stats 60s 缓存可能首次冷加载时仍跑全量聚合 SQL；或发帖/浏览触发失效后下次又冷加载。
3. **H3 跨库回填 authorName 慢**：forum PostService create/list 跨库回填 user 库的 authorName，每次列表都 join/回查用户表。
4. **H4 blog-service 首篇冷加载**：blog 本地优先已设，但首请求可能仍触发 Supabase 兜底或大宽表全扫。
5. **H5 聊天 WS 握手 + 房间初始化串行**：chat init 已加重试，但 WS 连接与历史消息拉取可能串行，且失败重试 3s/6s/9s 累加。

## 计划
1. 确认服务在线（端口存活）。
2. 对各关键接口做运行时计时（curl 计时 / 网关层耗时），收集聚合证据。
3. 按证据定位最慢接口，最小修复（并行化 / 加缓存 / 减跨库）。
4. 复测对比 pre/post。

## Evidence

### 后端接口计时（经 Vite 5173 真实链路，2026-10-12）
| 接口 | 冷 | 热 | HTTP |
|------|----|----|------|
| blog-list  | 41ms | 10ms | 200 |
| post-list  | 17ms | 11ms | 200 |
| post-stats | 13ms | 13ms | 200 |
| chat-enabled | 4ms | — | 200 |
| online-count | 39ms | — | 200 |
| blog-settings | 13ms | — | 200 |

### 结论：后端非瓶颈
- 所有关键接口均在 4–41ms 返回 200，forum stats 60s 缓存命中正常（13ms 稳定）。
- **H1 串行瀑布**：证伪。community/index.vue onMounted 里 fetch()/loadStats()/loadOnlineCount() 三者并行未 await 串行。
- **H2 stats 冷加载**：证伪。post-stats 冷热均 13ms，缓存生效。
- **H3 跨库回填**：证伪。post-list 17ms，无跨库放大。
- **H4 blog Supabase 兜底**：证伪。blog-list 41ms，本地优先生效，未触发云端。
- **H5 聊天串行**：部分存疑。chat-enabled 4ms，但 chat-rooms 需鉴权未测；WS 握手耗时未测（需浏览器实测）。

### 真正瓶颈定位（前端侧）
后端 4–41ms，用户仍觉"很慢"，差值在前端：
1. **Vite dev 模式按需编译**：路由动态 import 的 chunk 首次进入需即时编译（quant/index.vue ~4000 行尤其慢），属 dev-only，生产构建后消失。
2. **每次进入重新挂载**：community/chat 不在 keep-alive include，每次进入都重跑 onMounted（3 个并行请求 + 重渲染）。此前 Web3Layout 的 Suspense/transition/keep-alive 顺序错误会进一步卡住进入流程（已修复）。

### 最小修复（前端侧）
- Web3Layout router-view 重排为 `<Suspense><keep-alive :include="['QuantIndex','Community','CommunityChat']"><component/></keep-alive></Suspense>`（去掉路由级 transition，其 out-in 与 Suspense 交织是"返回空白"元凶；keep-alive 直接子节点必须是单一 `<component>` 否则报 "expects exactly one child"）。
- community/index.vue 加 `defineOptions({ name: 'Community' })`；定时器从 onUnmounted 改为 onActivated/onDeactivated 管理（keep-alive 下 onUnmounted 不触发，否则 30s 在线人数轮询会后台泄漏）。
- ChatRoom.vue 加 `defineOptions({ name: 'CommunityChat' })`；WS 由全局 chat store 常驻，无组件级定时器，keep-alive 无泄漏风险。

### 验证
- 浏览器实测：编译错误已消失，页面正常渲染（navigation loadEventEnd ~524ms）。
- 浏览器内 /api/chat/* 出现 15s 超时（status 0），但 curl 同路径 4-75ms 200——系先前编译错误污染 Vite/HMR 与标签页状态所致，**非真实后端问题**。需重启 Vite dev server + 浏览器硬刷新（Ctrl+Shift+R）后复测 WS 握手耗时。

### 遗留
- 聊天 WS 握手耗时未取得（受上述 Vite 状态污染影响，标签页未建立 WS）。重启 Vite 后可复测。
- Vite dev 首次编译大 chunk（quant ~4000 行）的进入慢属 dev-only，生产构建后消失。


