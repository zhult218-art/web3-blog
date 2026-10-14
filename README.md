# Web3 Portal

Web3 极简未来科技风个人综合技术门户：博客 / 论坛 / 商城 / 媒体 / 量化 / 工具 / 软件 / 资源 / AI 助手 / 管理后台于一体的前后端分离项目。

## 技术栈

| 端 | 技术 |
|---|---|
| 前端 | Vue 3 + Vite 5 + Pinia + Vue Router + TailwindCSS + GSAP + Three.js + ECharts |
| 后端 | Java 17 + Spring Boot 3.3 + Spring Cloud Alibaba（Nacos 注册/配置）+ Spring Cloud Gateway + MyBatis-Plus |
| 基础设施 | MySQL 8、Redis、RabbitMQ、Nacos |
| 附加服务 | `quant-py-service`（Python FastAPI 量化数据服务，端口 9006） |

## 目录结构

```text
web3-blog/
├── frontend/                    # Vue3 + Vite 前端（详见 frontend/README.md）
├── backend/                     # Spring Cloud 后端微服务（详见 backend/README.md）
│   ├── gateway/                 # API 网关统一入口（8080）
│   ├── user-service/            # 用户认证 / 资料 / 权限（8081）
│   ├── blog-service/            # 文章 / 分类 / 标签（8082）
│   ├── forum-service/           # 帖子 / 评论 / 点赞（8083）
│   ├── shop-service/            # 商品 / 订单 / 支付（8084）
│   ├── media-service/           # 音乐 / 视频 / 书籍 / 播放列表（8085）
│   ├── quant-service/           # 量化策略 / 行情数据（8086）
│   ├── tool-service/            # 在线脚本工具（8087）
│   ├── software-service/        # 软件下载（8088）
│   ├── resource-service/        # 资源上传 / 下载（8089）
│   ├── jarvis-service/          # JARVIS AI 语音助手（9001）
│   ├── admin-service/           # 管理后台（9002）
│   ├── common/                  # 公共模块（core / security / util）
│   ├── deploy/                  # MySQL 初始化 SQL 等部署文件
│   └── nacos/                   # Nacos 配置目录
├── py-service/                  # 统一 Python 服务（9006：量化/语音/视频/OSINT 等）
├── docs/                        # 需求文档 / 概述说明文档
├── start-all-services.bat       # Windows 一键启动脚本
├── start-all-services.ps1       # PowerShell 一键启动脚本
├── docker-compose.yml           # 基础设施与后端容器编排
├── deploy.sh                    # Linux 部署脚本
├── HANDOFF.md                   # 开发会话记录（按会话追加）
├── INSTALL.md                   # 环境安装指南
├── DEPLOY.md                    # 部署指南
├── MODULES.md                   # 模块说明
└── RUN.md                       # 运行指南（环境要求、手动启动）
```

## 功能模块

| 模块 | 前端页面 | 后端服务 | 说明 |
|---|---|---|---|
| 首页 | `/` | - | GhibliHome 宫崎骏风格 + 3D 终端 + 全站特效 |
| 社区 | `/community` | forum-service | 帖子、评论、点赞 |
| 商城 | `/shop` `/cart` | shop-service | 商品、购物车、订单、支付 |
| 媒体 | `/media` | media-service | 音乐、视频、书籍、播放列表 |
| 量化 | `/quant` | quant-service + quant-py-service | 策略、龙虎榜、资金流、研报 |
| 工具 | `/tools` | tool-service | 在线脚本工具（加密/二维码等） |
| 软件 | `/software` | software-service | 软件列表与下载 |
| 资源 | `/resources` | resource-service | 资源上传/下载 |
| 音乐 | `/music` | media-service | 音乐播放器 |
| AI | `/ai` | gateway（/v1 代理） | ChatGPT 风格 AI 问答 |
| 3D | `/three` | - | Three.js 粒子场景 |
| 个人中心 | `/profile` | user-service | 资料、订单 |
| 管理后台 | `/admin` | admin-service | 仪表盘、用户、文章、订单、流量、量化 |
| 友链/相册/留言/关于 | `/link` `/album` `/comments` `/about` | user-service / forum-service | 扩展内容页 |

## 快速启动

### Docker Compose（推荐）

仓库使用统一的 `py-service` 提供量化、语音识别、QQ 空间、WiFi 审计、OSINT 和市场驾驶舱 API。首次启动请先配置环境变量，再启动完整服务栈：

```bash
# 1. 创建本地环境文件，并按注释设置强密码/随机密钥
cp .env.example .env

# 2. 构建并启动基础设施、后端、Python 功能服务和前端
docker compose up -d --build

# 3. 查看服务状态与启动日志
docker compose ps
docker compose logs -f gateway py-service frontend
```

`JWT_SECRET` 至少 32 个字符；`MYSQL_ROOT_PASSWORD` 与 `MYSQL_PASSWORD` 必须一致（当前应用使用 root 数据库账号）；还需设置 `REDIS_PASSWORD`、RabbitMQ 凭据和 `VIDEO_ADMIN_TOKEN`。不要把真实密钥提交到 Git。MySQL、Redis、RabbitMQ 和 Nacos 的宿主机端口仅绑定到本机，业务 API 通过前端 Nginx 和网关访问。

前端默认通过 Nginx 将 `/api` 转发到 Gateway，将 `/pyquant`、`/whisper`、`/qzapi`、`/wifiapi`、`/osintapi` 和 `/cockpit` 转发到统一 Python 服务。部署后可通过 `http://localhost/health` 检查前端容器，通过 `docker compose exec py-service python -c "import urllib.request; print(urllib.request.urlopen('http://127.0.0.1:9006/health').read().decode())"` 检查 Python 服务。

### 本地开发（不使用 Docker 前端容器）

如果只需要开发前端，可按需启动后端依赖，然后执行：

```bash
cd frontend
npm install
npm run dev
```

开发代理默认指向本机的 Gateway（8080）和 Python 服务（9006）。本地手动运行后端时，请将相应的主机地址环境变量改为 `127.0.0.1`，不要直接沿用 Compose 容器名。

### Windows 一键脚本

`start-all-services.bat` 是 PowerShell 脚本的薄封装。使用前先按 `RUN.md` 配置本地 Java、Python、Node.js 和基础设施环境，并检查脚本中的服务清单与本机配置。

## 端口清单

| 服务 | 端口 | 数据库 | 功能 |
|---|---|---|---|
| Gateway | 8080 | - | API 网关，统一路由转发 |
| User Service | 8081 | web3_user | 用户管理、认证、权限 |
| Blog Service | 8082 | web3_blog | 文章、分类、标签 |
| Forum Service | 8083 | web3_forum | 帖子、评论 |
| Shop Service | 8084 | web3_shop | 商品、订单、支付 |
| Media Service | 8085 | web3_media | 音乐、视频、播放列表 |
| Quant Service | 8086 | web3_quant | 量化策略、行情数据 |
| Tool Service | 8087 | web3_tool | 脚本工具 |
| Software Service | 8088 | web3_software | 软件下载 |
| Resource Service | 8089 | web3_resource | 资源上传/下载 |
| Jarvis Service | 9001 | web3_jarvis | AI 语音助手 |
| Admin Service | 9002 | web3_admin | 管理后台 |
| Python Service | 9006 | - | 量化、语音、视频、OSINT 等 Python 功能模块 |
| Nacos | 8848 | - | 服务注册/配置中心 |
| Redis | 6379 | - | 缓存 |
| RabbitMQ | 5672 | - | 消息队列 |
| MySQL | 3306 | 13 个 web3_* 库（见 backend/deploy/mysql/init/01-13 号 SQL） | 数据存储 |

## 调用链路

```
浏览器 → http://localhost:5173 (Vite Dev)
            ├─ /api/**   → Gateway 8080 → 对应微服务 → MySQL / Redis / RabbitMQ
            └─ /pyquant/** → quant-py-service 9006
```

## 数据库初始化

```powershell
# 容器方式：backend/deploy/mysql/init/ 下编号 SQL 由 MySQL 容器首次初始化时自动执行（当前 01–13）
# 本地方式：
mysql -u root -p < backend/deploy/mysql/init/01-databases.sql
mysql -u root -p --database=web3_user < backend/deploy/mysql/init/02-web3_user.sql
# ... 其余按编号逐个导入
```

## 相关文档

- `RUN.md` — 详细运行指南（环境要求、手动逐个启动、健康检查、常见问题）
- `INSTALL.md` — 环境安装步骤
- `DEPLOY.md` — 生产部署
- `MODULES.md` — 模块设计说明
- `HANDOFF.md` — 开发会话历史记录
- `docs/` — 需求文档、概述说明文档
- `frontend/README.md` — 前端结构与开发说明
- `backend/README.md` — 后端结构与开发说明
