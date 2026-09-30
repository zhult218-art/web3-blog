# web3-blog 安全加固 + 音乐模块重写 + 整体完善实施计划

## Context

用户要求：
1. **网络安全加固**：防止 DDoS 与 SQL 注入（已有基础但需查漏补缺）
2. **音乐模块完善**：参考 GitHub 优秀项目（YesPlayMusic/NeteaseCloudMusicApi/howler.js）完全重写播放器，补全后端接口
3. **整体项目分析**：找出不完善处并优化

经四轮 AskUserQuestion 确认，执行范围：
- 三大维度全部执行
- 音乐后端：评论+收藏+播放统计+流式 Range+聚合搜索+播放历史（全做）
- 音乐前端：接通现有按钮+全局快捷键+MediaSession API+音质切换+历史（全做）
- 播放器 UI：**完全重写**（参考 YesPlayMusic 风格）

## 阶段 1：网络安全加固

### 1.1 XFF 信任加固
- **文件**：[RateLimitFilter.java](file:///f:/project/my-blog/web3-blog/backend/gateway/src/main/java/com/web3/gateway/filter/RateLimitFilter.java#L147-L158)、[application.yml](file:///f:/project/my-blog/web3-blog/backend/gateway/src/main/resources/application.yml#L200-L207)
- **改动**：新增 `gateway.trusted-proxies: 127.0.0.1,10.0.0.0/8` 配置；`resolveClientIp` 改为「代理链逆推」——仅当 remoteAddress 命中可信代理时才解析 XFF，从右向左取第一个非可信 IP；否则直接用 remoteAddress。杜绝攻击者伪造 XFF 绕过 IP 限流。

### 1.2 安全响应头
- **新建**：`backend/gateway/src/main/java/com/web3/gateway/filter/SecurityHeadersFilter.java`（`GlobalFilter, Ordered`，`getOrder() = -200` 早于限流）
- **响应头清单**：
  - `Content-Security-Policy: default-src 'self'; img-src 'self' data: https:; media-src 'self' https: blob:; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'`
  - `X-Frame-Options: SAMEORIGIN`（marketingdashboard 同源 iframe 仍可加载）
  - `X-Content-Type-Options: nosniff`
  - `Strict-Transport-Security: max-age=31536000; includeSubDomains`（仅 HTTPS 响应）
  - `Referrer-Policy: strict-origin-when-cross-origin`
  - `Permissions-Policy: geolocation=(), microphone=(self)`

### 1.3 IP 黑名单机制
- **新建**：`backend/gateway/src/main/java/com/web3/gateway/filter/IpBlocklistFilter.java`（`getOrder() = -150`，介于安全头与限流之间）
- **逻辑**：Redis Set `ip:blocklist` 存黑名单 IP，`redis.opsForSet().isMember()` 命中返回 403；Redis 故障 fail-open
- **管理 API**：新建 `backend/admin-service/.../IpBlocklistController.java`，`POST/DELETE /admin/ip-blocklist/{ip}` + `GET /admin/ip-blocklist`，`AuthUtils.requireAdmin` 守卫

### 1.4 SQL 注入全量审计
- **现状**：项目无 Mapper.xml，统一 MyBatis-Plus `LambdaQueryWrapper`（参数绑定，安全）。已知钳制：[ArticleService.java](file:///f:/project/my-blog/web3-blog/backend/blog-service/src/main/java/com/web3/blog/service/ArticleService.java#L87-L100) limit 1..50。
- **改动**：grep 所有 `*Service.java` 中 `page(page, size)` 调用，对 `size` 参数统一 `Math.min(size, 100)` 钳制；检查是否有字符串拼接 SQL 的反模式（已确认无）。
- **代表文件**：ArticleService、StockPriceService、MusicService、CommentService

### 1.5 越权风险审计
- **现状**：CommentController.delete 已做 `userId + isAdmin` 校验，安全。
- **改动**：审计 OrderController、ResourceController、ChatController 的 update/delete 接口，统一规则 `if (!obj.getOwnerId().equals(userId) && !isAdmin) throw new BizException(403, "无权操作")`。
- **代表文件**：`backend/shop-service/.../OrderController.java`、`backend/resource-service/.../ResourceController.java`、`backend/forum-service/.../ChatController.java`

### 1.6 文件上传加固
- **现状**：已有 `FileSecurityChecker`（media/resource 服务，扩展名白名单+魔数+大小+路径穿越）。
- **改动**：将 `FileSecurityChecker` 提取到 `backend/common/common-core` 共享模块；ChatUploadController 引入调用 `FileSecurityChecker.validate(file, 5_000_000)`。
- **文件**：[ChatUploadController.java](file:///f:/project/my-blog/web3-blog/backend/forum-service/src/main/java/com/web3/forum/controller/ChatUploadController.java)

### 1.7 日志脱敏
- **新建**：`backend/common/common-util/src/main/java/com/web3/common/util/LogMasker.java`
  - `maskPhone("13812345678") → "138****5678"`
  - `maskToken("Bearer eyJ...") → "Bearer eyJ***"`
  - `maskPassword → "***"`
- **替换**：全局 `log.info("...phone={}", phone)` 改为 `LogMasker.maskPhone(phone)`。重点：[OtpService.java:110-114](file:///f:/project/my-blog/web3-blog/backend/user-service/src/main/java/com/web3/user/service/OtpService.java#L110-L114)、UserController 登录/注册接口。

### 1.8 CORS 显式收紧
- **新建**：`backend/gateway/src/main/java/com/web3/gateway/config/CorsConfig.java`
- **配置**：`allowedOrigins` 写白名单（生产域名 frp-six.com:44598 + `http://localhost:5173`），`allowedMethods=GET,POST,PUT,DELETE,OPTIONS`，`allowCredentials=true`，`maxAge=3600`。杜绝 `*` 通配。

---

## 阶段 2：音乐模块后端完善

### 2.1 DDL 变更
- **music 表加列**：`play_count BIGINT DEFAULT 0`（[06-web3_media.sql](file:///f:/project/my-blog/web3-blog/backend/deploy/mysql/init/06-web3_media.sql#L2-L14)）
- **新建 user_music_like 表**：`(user_id BIGINT, music_id BIGINT, created_at DATETIME, PK(user_id, music_id), INDEX(music_id))`
- **新建 user_music_history 表**：`(id BIGINT PK, user_id BIGINT, music_id BIGINT, played_at DATETIME, INDEX(user_id, played_at))`

### 2.2 新增接口（[MusicController.java](file:///f:/project/my-blog/web3-blog/backend/media-service/src/main/java/com/web3/media/controller/MusicController.java) 扩展）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/music/{id}/like` | 切换收藏态（需登录），写 user_music_like |
| GET | `/music/{id}/like` | 查询当前用户收藏态 |
| GET | `/music/liked` | 当前用户收藏列表（分页） |
| POST | `/music/{id}/play` | 埋点播放统计，`update().setSql("play_count = play_count + 1")` |
| GET | `/music/hot` | 按 play_count 倒序的 hot 榜（默认 limit=20） |
| GET | `/music/search` | 聚合搜索：本地 LIKE + 调用网易云 `/netease/search` + iTunes（前端已有 JSONP，后端聚合） |
| GET | `/music/{id}/stream` | 流式音频，支持 HTTP Range 206 Partial Content，`Accept-Ranges: bytes` |
| POST | `/music/history` | 记录播放历史，写 user_music_history |
| GET | `/music/history` | 当前用户播放历史分页（JOIN music 表） |
| DELETE | `/music/history` | 清空当前用户历史 |

### 2.3 评论复用方案
- **不新建表**：复用 forum-service `CommentController`，`targetType=music`、`targetId=musicId`。CommentController 已支持任意 targetType。
- **接口**：`POST /comment` body `{targetType:'music', targetId, content}`，`GET /comment?targetType=music&targetId={id}&page=1&size=20`

### 2.4 流式 Range 实现思路
- 参考 Spring `ResourceRegionHttpMessageConverter`
- 读取 `Range` 请求头，解析 `bytes=start-end`
- 返回 `ResponseEntity<InputStreamResource>` + 状态 206 + `Content-Range: bytes start-end/total` + `Accept-Ranges: bytes`
- 大文件音频拖拽进度条可秒 seek，不必全文件下载

---

## 阶段 3：音乐模块前端完全重写（参考 YesPlayMusic）

### 3.1 重写 [MusicPlayer.vue](file:///f:/project/my-blog/web3-blog/frontend/src/views/music/components/MusicPlayer.vue)
- **保留**：黑胶视觉、歌词同步、进度拖拽核心交互
- **重写方向**：参考 YesPlayMusic 视觉语言——更克制的留白、玻璃拟态卡片、更细腻的歌词过渡动画、播放暂停按钮的弹性反馈
- **组件结构**（保持单文件，不拆）：
  - 主区：黑胶（保留） + 滚动歌词（增强过渡）
  - 底部栏：进度条 + 控制按钮 + 音质下拉 + 喜欢按钮 + 评论数 + 歌单
  - 浮层：歌单面板、评论抽屉（新增）

### 3.2 接通后端真实接口
- **新建**：`frontend/src/api/music.js` 增加 `toggleLike/getLikeStatus/getLikedList/getHotSongs/searchMusic/recordPlay/getHistory/clearHistory/streamUrl`
- **改造**：
  - [MusicPlayer.vue:88-93](file:///f:/project/my-blog/web3-blog/frontend/src/views/music/components/MusicPlayer.vue#L88-L93) `toggleLike` 改为真实接口调用，`liked` 改为 `ref(false)` + onMounted 拉取 `getLikeStatus`
  - [MusicPlayer.vue:99-102](file:///f:/project/my-blog/web3-blog/frontend/src/views/music/components/MusicPlayer.vue#L99-L102) `commentCount` 改为 `ref(0)` + onMounted 拉取评论数；点击打开评论抽屉
  - [player.js:142-146](file:///f:/project/my-blog/web3-blog/frontend/src/stores/modules/player.js#L142-L146) `play()` 后调 `recordPlay(track.id)` 埋点

### 3.3 全局键盘快捷键
- **新建**：`frontend/src/composables/useMusicHotkeys.js`
- **键位**：
  - `Space` 播放/暂停（聚焦 input 时不拦截）
  - `Ctrl+←` / `Ctrl+→` 上一首/下一首
  - `↑` / `↓` 音量加/减 5%
  - `L` 喜欢/取消
  - `M` 静音切换
  - `S` 聚焦搜索框
- **挂载**：在 [player.vue](file:///f:/project/my-blog/web3-blog/frontend/src/views/music/player.vue) onMounted 启用

### 3.4 MediaSession API
- **改造**：[player.js](file:///f:/project/my-blog/web3-blog/frontend/src/stores/modules/player.js) `play()` 内同步 `navigator.mediaSession.metadata = new MediaMetadata({title, artist, album, artwork: [{src: cover}]})`
- 注册 `setActionHandler('play'/'pause'/'previoustrack'/'nexttrack'/'seekto', ...)`
- 浏览器锁屏、通知栏可控制播放

### 3.5 音质切换
- **改造**：[api/music.js:25](file:///f:/project/my-blog/web3-blog/frontend/src/api/music.js#L25) `getNeteaseSongUrl(id, br=320000)` 加 br 参数
- **UI**：[MusicPlayer.vue:58](file:///f:/project/my-blog/web3-blog/frontend/src/views/music/components/MusicPlayer.vue#L58) `LOSSLESS · FLAC` 静态标签改为下拉，选项 标准 128k / 较高 192k / 极高 320k / 无损 999000
- 切换后自动重新加载当前曲目

### 3.6 播放历史
- **前端兜底**：localStorage key=`music_history`，每次切歌 `unshift + slice(100)`，弱网/未登录时仍可用
- **接通后端**：登录用户每次播放后调 `recordPlay` 写 user_music_history；`/music/history` 页面（或在歌单面板加 tab）展示完整历史

---

## 阶段 4：整体项目完善

### 4.1 测试补全
- **新建**：
  - `backend/media-service/src/test/.../MusicServiceTest.java`（仿 [OrderServiceTest.java](file:///f:/project/my-blog/web3-blog/backend/shop-service/src/test/java/com/web3/shop/service/OrderServiceTest.java) 模板）
  - `backend/blog-service/src/test/.../ArticleServiceTest.java`
  - `backend/forum-service/src/test/.../ChatServiceTest.java`
- **覆盖**：page 分页/上限钳制、getById、越权拒绝、play_count 自增

### 4.2 TODO 清理
- [OtpService.java:121-140](file:///f:/project/my-blog/web3-blog/backend/user-service/src/main/java/com/web3/user/service/OtpService.java#L121-L140) `sendSmsByGateway` 改为「仅邮件 OTP」方案：注释说明不发短信，Controller 不再下发手机验证码，移除未实现 SDK 调用

### 4.3 死代码清理
- **删除** console.log 调试残留：`useVoskStt.js`（L21/27/77）、`XingTuAssistant.vue`（L943/1186/1193/1194）改为 `import.meta.env.DEV` 守卫
- **删除未使用导出**：用 grep 找出 `export` 但全项目未 `import` 的函数

### 4.4 大文件拆分
- [XingTuAssistant.vue](file:///f:/project/my-blog/web3-blog/frontend/src/views/ai/XingTuAssistant.vue)（>1100 行）按职责拆分：
  - `XingTuStt.vue` 语音识别子组件
  - `XingTuChat.vue` 对话区子组件
  - `useXingTu.js` 状态 composable

### 4.5 README 完善
- **更新**：`backend/README.md` 补充：
  - 部署文档（Nacos/Redis 启动顺序、各服务端口、PowerShell 启动脚本）
  - API 清单（按微服务分组）
  - 环境变量表（`SMS_PROVIDER/J8Y_APP_KEY/NACOS_SERVER/OPENROUTER_API_KEY/ARTIFICIAL_ANALYSIS_API_KEY`）

---

## Verification（端到端测试方案）

### 安全验证
1. 伪造 `X-Forwarded-For: 1.2.3.4` 请求网关，验证 IP 限流仍按真实 remoteAddress 计数（curl 测试）
2. 浏览器 devtools 查看任意响应头，确认 CSP/X-Frame-Options/HSTS/nosniff 全部下发
3. 调用 `POST /admin/ip-blocklist/1.2.3.4` 后再用该 IP 请求，确认返回 403
4. 用普通用户 token 调 `DELETE /comment/{他人评论id}`，确认返回 403
5. 上传 .exe 文件到 ChatUploadController，确认被拒

### 音乐模块验证
1. 登录后点击喜欢按钮，刷新页面后状态保持
2. 在评论区发评论，刷新后能看到
3. 拖拽进度条到音频中段，确认秒级 seek（Range 流式）
4. 按空格暂停、Ctrl+→ 切下一首、↑ 调大音量
5. 浏览器锁屏（或 devtools 模拟）看到通知栏媒体控件，能控制播放
6. 切换音质 320k → 128k，听到音质变化
7. 播放 5 首歌后打开历史页，看到最近 5 首
8. GET /music/hot 看到按播放量排序的榜单

### 整体完善验证
1. `cd backend && mvn -DskipTests test` 跑通新增的 3 个 ServiceTest
2. grep 全项目无残留 `console.log`（除 import.meta.env.DEV 守卫的）
3. XingTuAssistant.vue 拆分后页面行为不变（语音识别、对话、TTS 全部正常）

## 关键文件清单

### 阶段 1（安全）
- [RateLimitFilter.java](file:///f:/project/my-blog/web3-blog/backend/gateway/src/main/java/com/web3/gateway/filter/RateLimitFilter.java)
- [application.yml](file:///f:/project/my-blog/web3-blog/backend/gateway/src/main/resources/application.yml)
- 新建 SecurityHeadersFilter.java、IpBlocklistFilter.java、CorsConfig.java、IpBlocklistController.java
- [ChatUploadController.java](file:///f:/project/my-blog/web3-blog/backend/forum-service/src/main/java/com/web3/forum/controller/ChatUploadController.java)
- 新建 LogMasker.java

### 阶段 2（音乐后端）
- [MusicController.java](file:///f:/project/my-blog/web3-blog/backend/media-service/src/main/java/com/web3/media/controller/MusicController.java)
- [MusicService.java](file:///f:/project/my-blog/web3-blog/backend/media-service/src/main/java/com/web3/media/service/MusicService.java)
- [06-web3_media.sql](file:///f:/project/my-blog/web3-blog/backend/deploy/mysql/init/06-web3_media.sql)
- 新建 MusicLikeMapper、MusicHistoryMapper、MusicStreamController

### 阶段 3（音乐前端）
- [MusicPlayer.vue](file:///f:/project/my-blog/web3-blog/frontend/src/views/music/components/MusicPlayer.vue)（完全重写）
- [player.js](file:///f:/project/my-blog/web3-blog/frontend/src/stores/modules/player.js)
- [api/music.js](file:///f:/project/my-blog/web3-blog/frontend/src/api/music.js)
- 新建 useMusicHotkeys.js、评论抽屉组件

### 阶段 4（整体完善）
- 新建 MusicServiceTest.java、ArticleServiceTest.java、ChatServiceTest.java
- [OtpService.java](file:///f:/project/my-blog/web3-blog/backend/user-service/src/main/java/com/web3/user/service/OtpService.java)
- [XingTuAssistant.vue](file:///f:/project/my-blog/web3-blog/frontend/src/views/ai/XingTuAssistant.vue)（拆分）
- [backend/README.md](file:///f:/project/my-blog/web3-blog/backend/README.md)
