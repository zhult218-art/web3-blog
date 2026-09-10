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
├── quant-py-service/            # Python 量化数据服务（9006，A股数据）
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

### 方式一：Docker Compose（推荐，基础设施 + 后端）

```bash
# 1. 启动基础设施与后端服务
docker compose up -d mysql redis rabbitmq
docker compose up -d gateway user-service blog-service forum-service shop-service media-service quant-service tool-service software-service resource-service jarvis-service admin-service

# 2. 启动前端（开发模式）
cd frontend
npm install
npm run dev
```

### 方式二：本地手动启动（Windows）

```powershell
# 1. 启动基础设施：MySQL / Redis / RabbitMQ / Nacos（见 INSTALL.md / RUN.md）

# 2. 打包后端（只需一次）
mvn clean package -DskipTests
cd backend
mvn clean package -DskipTests

# 3. 启动后端服务（每个开一个终端窗口，端口见下表）
java -jar gateway/target/gateway-1.0.0.jar
java -jar user-service/target/user-service-1.0.0.jar
# ... 其余服务同理

# 4. 启动前端
cd frontend
npm install
npm run dev        # 访问 http://localhost:5173
```

### 方式三：一键脚本

双击 `start-all-services.bat`，或执行 `powershell -ExecutionPolicy Bypass -File start-all-services.ps1`。

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
| Quant Py Service | 9006 | - | Python 量化数据（A股） |
| Nacos | 8848 | - | 服务注册/配置中心 |
| Redis | 6379 | - | 缓存 |
| RabbitMQ | 5672 | - | 消息队列 |
| MySQL | 3306 | 11 个 web3_* 库 | 数据存储 |

## 调用链路

```
浏览器 → http://localhost:5173 (Vite Dev)
            ├─ /api/**   → Gateway 8080 → 对应微服务 → MySQL / Redis / RabbitMQ
            └─ /pyquant/** → quant-py-service 9006
```

## 数据库初始化

```powershell
# 容器方式：backend/deploy/mysql/init/ 下 12 个 SQL 由 MySQL 容器自动执行
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
