-- 博客：用 Cloudflare Pages 部署 Vue3 全栈项目（2026 实战）
-- 在 Supabase Dashboard → SQL Editor 中执行
-- 幂等：slug 唯一，已存在则跳过

insert into public.blog_articles
  (title, slug, summary, content, category, tags, author_name, views, likes, is_top, published, created_at)
select '用 Cloudflare Pages 部署 Vue3 全栈项目（2026 实战）',
  'deploy-vue3-to-cloudflare-pages-guide',
  '把一个含 13 个 Java 微服务后端、Vite5 前端、Supabase 数据层的 Web3 全栈项目，拆解为 Cloudflare Pages 托管前端静态产物 + 后端后续独立部署的分层架构。覆盖 wrangler CLI 直传、GitHub 自动集成、SPA 路由兜底、大文件清理、环境变量配置、网络错误降级等实战细节。',
$md$## 场景与目标

我的 Web3 Portal 项目结构：

- **前端**：Vue 3 + Vite 5 + TailwindCSS + Three.js + ECharts（≈140 个页面）
- **后端**：13 个 Spring Boot 微服务（Gateway/User/Blog/Forum/Shop/Media/Quant/Tool/Software/Resource/Jarvis/Admin/AiProxy）+ Nacos + MySQL + Redis + RabbitMQ
- **数据层**：Supabase（Postgres + Auth，前端只读、后端写）

全部用 docker-compose 跑通了，想让**博客**这部分先上线（其他人登录/发帖等后端功能可以暂缺），Cloudflare Pages 是最合适的选择。

## 核心思路

**Cloudflare Pages 只能托管静态产物，无法运行 Java 后端**。但博客有个好处：**文章读接口优先走 Supabase**（publishable key + RLS 只读已发布），后端不可用时自动回退到 `/api` 网关。所以：

```
用户浏览器
  ├─ 读博客：Supabase 直连 ✅（后端完全不需要）
  ├─ 点赞/收藏/发帖：走 /api ❌（后端未部署，节流降级，不刷屏）
  ├─ 其他功能（商城/量化/论坛）：走 /api ❌（后端未部署）
  └─ 主页/3D 可视化：前端自渲染 ✅
```

这就是"前端先行、后端可后补"的部署策略。

## 一、代码层面的改造

### 1. SPA 路由兜底：public/_redirects

Cloudflare Pages 对 SPA 默认只返回 404，需要显式配置 fallback：

```
/*  /index.html  200
```

放在 `frontend/public/_redirects`，vite build 后会原样复制到 dist 根目录。

### 2. 自定义响应头：public/_headers

```
/*.html
  X-Content-Type-Options: nosniff
  X-Frame-Options: SAMEORIGIN
  Referrer-Policy: strict-origin-when-cross-origin

/assets/*
  Cache-Control: public, max-age=31536000, immutable
```

### 3. API 基址环境变量化

`src/api/request.js`：

```js
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 15000
})
```

- 开发时 Vite 代理 `/api` → `localhost:8080`
- 生产时如果后端已部署，在 Pages 控制台设 `VITE_API_BASE=https://api.your-domain.com`
- 后端未部署时不设，前端相对 `/api` 请求自然返回 404，触发降级

### 4. 网络错误节流降级

后端未部署时，很多页面（商城/量化/论坛）会弹一堆 toast 刷屏。加个节流：

```js
let lastNetErrorAt = 0
const NET_ERROR_THROTTLE_MS = 5000

// 响应拦截器里：
const isNetError = !error.response  // 连接失败/超时
if (isNetError) {
  const now = Date.now()
  if (now - lastNetErrorAt < NET_ERROR_THROTTLE_MS)
    return Promise.reject(error)  // 5 秒内只弹一次
  lastNetErrorAt = now
}
```

### 5. 大文件自动清理

Cloudflare Pages 单文件 ≤ 25MB。我的 `public/vosk/cn.tar.gz`（41.8MB）会导致上传失败。加 postbuild 脚本：

`frontend/scripts/clean-large-assets.mjs`：

```js
import { promises as fs } from 'node:fs'
const LIMIT = 25 * 1024 * 1024

const voskDir = path.join(dist, 'vosk')
if (await fs.access(voskDir).then(() => true).catch(() => false))
  await fs.rm(voskDir, { recursive: true, force: true })

// 兜底扫描：删除所有 >25MB 的文件
await walk(dist)
```

`package.json`：

```json
{
  "scripts": {
    "build": "vite build",
    "postbuild": "node scripts/clean-large-assets.mjs"
  }
}
```

## 二、wrangler CLI 直传部署（最快路径）

wrangler 4.x 已内置 Pages 支持，用 Cloudflare API Token 全自动。

### 1. 创建 API Token

1. 打开 https://dash.cloudflare.com/profile/api-tokens
2. **Create Token** → **Create Custom Token**
3. Permissions 选：
   - Account → **Cloudflare Pages → Edit** ✅（核心）
   - Account → **Access: Custom Pages → Edit**
   - Account → **Account Custom Pages → Edit**
   - Account → **Account Settings → Read**（wrangler 自动查账号 ID 需要）
4. Account Resources → Include - 你的账号
5. Create → 复制 token（只显示一次！）

### 2. 创建 Pages 项目

```bash
export CLOUDFLARE_API_TOKEN=cfm_xxxxxxxxxxxxx
npx wrangler pages project create web3-blog --production-branch=main
```

### 3. 上传构建产物

```bash
cd frontend
npm run build           # 含 postbuild 自动删 >25MB 文件
npx wrangler pages deploy ./dist --project-name=web3-blog --branch=main
```

成功后会返回：

```
✨ Success! Uploaded 185 files (28.53 sec)
✨ Uploading _headers
✨ Uploading _redirects
🌎 Deploying...
✨ Deployment complete! Take a peek over at https://98b4db10.web3-blog-8wa.pages.dev
```

### 4. 环境变量（构建时注入）

Vite 用 `VITE_` 前缀注入到前端 bundle，必须在 **构建时** 就有。用 wrangler secret put：

```bash
echo "https://nyzpglrmxltsirxmtkjo.supabase.co" \
  | npx wrangler pages secret put VITE_SUPABASE_URL --project-name=web3-blog

echo "sb_publishable_xxxxx" \
  | npx wrangler pages secret put VITE_SUPABASE_PUBLISHABLE_KEY --project-name=web3-blog
```

Secret 和 Env Var 都是构建时注入，区别是 Secret 加密存储（生产环境安全），Env Var 明文。Vite 里两者都能用。

## 三、GitHub 自动集成（推荐长期方案）

每次 push 到 `main` 分支自动重新构建部署。**只能在 Cloudflare Pages 控制台手动配置**，wrangler 不支持。

1. 打开 https://dash.cloudflare.com → Workers & Pages → 选项目
2. 左侧 **Source Control** → **Connect to Git**
3. 选 GitHub → 授权 → 选仓库 → Set up builds：

| 字段 | 值 |
|---|---|
| Root directory | `frontend` |
| Framework preset | Vue |
| Build command | `npm run build` |
| Build output directory | `dist` |

4. Environment variables (advanced) → 手动添加两条 `VITE_SUPABASE_*`（和 wrangler secret put 二选一）
5. Save → Cloudflare 立刻触发首次构建

之后每次 push 到 GitHub `main`，Pages 自动拉代码 → `cd frontend && npm install && npm run build` → 上传 dist。

## 四、部署后的验证清单

```bash
# 1. 首页能打开，Three.js/GSAP 动效正常
curl -I https://web3-blog-8wa.pages.dev/
# → 200 OK, content-type: text/html

# 2. SPA 路由兜底（/blog/post/10 不会 404）
curl -I https://web3-blog-8wa.pages.dev/blog/post/10
# → 200 OK（返回 index.html，vue-router 接管）

# 3. Supabase 数据正常
curl -s https://web3-blog-8wa.pages.dev/blog | grep -c "article-card"
# → >0（从 Supabase 读到了文章）

# 4. 大文件已清理
curl -I https://web3-blog-8wa.pages.dev/vosk/cn.tar.gz
# → 404（postbuild 脚本已删除）

# 5. 安全头生效
curl -I https://web3-blog-8wa.pages.dev/
# → 包含 X-Content-Type-Options / X-Frame-Options / Referrer-Policy
```

## 五、常见问题排障

| 现象 | 可能原因 | 解决方案 |
|---|---|---|
| `Cannot use 'import.meta' outside a module` | Vite 产物不是 ES module | 确认 `vite.config.js` 没有设 build.target 过低（默认 es2020） |
| 单文件 >25MB 上传失败 | vosk/mp4 等静态资源未清理 | 跑 postbuild 脚本，或手动删除 dist 下超限文件 |
| 页面全白 | JS chunk 加载失败 / Service Worker 缓存 | DevTools → Network 看 404 chunk；`/_vercel/path0` 清缓存 |
| 博客列表空 | VITE_SUPABASE_* 环境变量未设 | Pages 控制台 → Settings → Environment variables 添加 |
| `/blog/post/:id` 返回 404 | `_redirects` 未生效 | 确认 `public/_redirects` 存在且内容正确，重新构建 |
| GitHub 集成构建失败 | Root directory 没设 `frontend` | Pages Settings → Build config → Root directory |
| wrangler secret put 卡住 | 需要交互式输入 | 用 `echo "value" \| wrangler pages secret put NAME` 管道输入 |

## 六、后续：后端部署的两种路线

前端上线只是第一步，后端可以后续补回：

**路线 A：VPS + Docker Compose（零成本）**
- 租一台轻量 VPS（腾讯云/阿里云 2核2G ≈50元/月）
- `docker compose up -d` 启动全套服务
- 用 Cloudflare Tunnel 把 `localhost:8080` 暴露到公网
- Pages 控制台设 `VITE_API_BASE=https://api.your-domain.com`
- 无公网 IP、无端口映射、全程加密

**路线 B：Cloudflare Tunnel + Cloudflare Pages 同源**
- Pages 托管前端（已完成）
- Tunnel 暴露后端网关到 Cloudflare 的 `https://your-domain.com/api`
- 前端和后端共享 Cloudflare 边缘，无跨域问题

> 两种路线都保留现在的分层设计：博客读走 Supabase（永不挂），写操作/其他功能走后端（挂了也不影响博客阅读）。

## 总结

把全栈项目拆为"前端静态产物 + 后端独立部署"的架构，是 Cloudflare Pages 托管全栈项目的标准做法。关键改造点：

1. **SPA 路由兜底** → `public/_redirects`
2. **构建时环境变量** → `VITE_` 前缀 + Pages Secret/Env
3. **大文件清理** → postbuild 脚本自动删 >25MB 文件
4. **网络错误降级** → 节流 toast，后端未部署不刷屏
5. **后端可选回退** → 请求拦截器 try-catch，Supabase 不可用时静默

我的部署结果：

- 线上地址：https://98b4db10.web3-blog-8wa.pages.dev
- GitHub 仓库：https://github.com/zhult218-art/web3-blog
- 构建时长：首次 30s（npm install），后续 ≈15s
- 静态资源：185 文件，28.53 MB
- 后端状态：未部署（博客读接口走 Supabase 正常工作）
$md$,
  '部署与运维',
  array['Cloudflare','Pages','Vue3','Vite','wrangler','CI/CD','Supabase'],
  'Aurora-朱',
  0, 0, false, true, now()
on conflict (slug) do nothing;
