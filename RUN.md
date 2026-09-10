# Web3 Portal - 项目运行指南

## 系统要求

| 组件 | 版本要求 | 安装路径 |
|------|---------|---------|
| JDK | 17+ | `F:\tools\jdk17` |
| Maven | 3.8+ | `F:\tools\apache-maven-3.9.6` |
| Node.js | 16+ | `F:\soft\nodejs\nodejs` |
| MySQL | 8.0+ | `C:\Program Files\MySQL\MySQL Server 8.0` |
| Redis | 6+ | `F:\tools\redis` |
| RabbitMQ | 3.12+ | `F:\tools\rabbitmq` |
| Nacos | 2.x | `F:\tools\nacos` |

## 快速启动

### 一键启动（推荐）

双击运行根目录的 `start-all-services.bat`，它将自动启动：
- MySQL（需提前启动）
- Redis
- Nacos
- 所有 12 个后端微服务
- 前端开发服务器

### 手动启动

#### 1. 启动基础设施

```powershell
# Redis
F:\tools\redis\redis-server.exe F:\tools\redis\redis.windows.conf

# Nacos (standalone模式)
cd F:\tools\nacos\bin
.\startup.cmd -m standalone
```

#### 2. 启动后端微服务

```powershell
$env:JAVA_HOME = "F:\tools\jdk17"

cd F:\project\my-blog\web3-blog\backend

# 使用 PowerShell 脚本启动所有服务
powershell -ExecutionPolicy Bypass -File ..\start-all-services.ps1
```

或手动逐个启动（需要先 Maven 打包）：

```powershell
# 打包（只需一次）
$env:JAVA_HOME = "F:\tools\jdk17"
F:\tools\apache-maven-3.9.6\bin\mvn.cmd clean package -DskipTests

# 启动各个服务（每个开一个终端窗口）
cd F:\project\my-blog\web3-blog\backend

java -jar gateway/target/gateway-1.0.0.jar --spring.profiles.active=prod      # 端口 8099
java -jar user-service/target/user-service-1.0.0.jar --spring.profiles.active=prod    # 8081
java -jar blog-service/target/blog-service-1.0.0.jar --spring.profiles.active=prod    # 8082
java -jar forum-service/target/forum-service-1.0.0.jar --spring.profiles.active=prod  # 8083
java -jar shop-service/target/shop-service-1.0.0.jar --spring.profiles.active=prod    # 8084
java -jar media-service/target/media-service-1.0.0.jar --spring.profiles.active=prod  # 8085
java -jar quant-service/target/quant-service-1.0.0.jar --spring.profiles.active=prod  # 8086
java -jar tool-service/target/tool-service-1.0.0.jar --spring.profiles.active=prod    # 8087
java -jar software-service/target/software-service-1.0.0.jar --spring.profiles.active=prod # 8088
java -jar resource-service/target/resource-service-1.0.0.jar --spring.profiles.active=prod # 8089
java -jar jarvis-service/target/jarvis-service-1.0.0.jar --spring.profiles.active=prod   # 9001
java -jar admin-service/target/admin-service-1.0.0.jar --spring.profiles.active=prod    # 9002
```

#### 3. 初始化数据库

```powershell
# 确保 web3_jarvis 和 web3_admin 数据库已创建，然后导入表结构
mysql -u root -proot --database=web3_admin < backend\deploy\mysql\init\10-web3_admin.sql
mysql -u root -proot --database=web3_jarvis < backend\deploy\mysql\init\09-web3_jarvis.sql
```

#### 4. 启动前端

```powershell
cd F:\project\my-blog\web3-blog\frontend
npm install
npm run dev
```

## 服务端口清单

| 服务 | 端口 | 数据库 | 功能 |
|------|------|--------|------|
| Gateway | 8099 | - | API 网关，路由转发 |
| User Service | 8081 | web3_user | 用户管理、认证 |
| Blog Service | 8082 | web3_blog | 文章、分类、标签 |
| Forum Service | 8083 | web3_forum | 论坛帖子、评论 |
| Shop Service | 8084 | web3_shop | 商品、订单、支付 |
| Media Service | 8085 | web3_media | 音乐、视频、播放列表 |
| Quant Service | 8086 | web3_quant | 量化策略、股票数据 |
| Tool Service | 8087 | web3_tool | 脚本、工具 |
| Software Service | 8088 | web3_software | 软件下载 |
| Resource Service | 8089 | web3_resource | 资源上传 |
| Jarvis Service | 9001 | web3_jarvis | AI 语音助手 |
| Admin Service | 9002 | web3_admin | 管理后台 |
| Nacos | 8848 | - | 服务发现/配置中心 |
| Redis | 6379 | - | 缓存 |
| RabbitMQ | 5672 | - | 消息队列 |
| MySQL | 3306 | 11个DB | 数据存储 |

## 前端访问

- 开发环境：http://localhost:5173
- Vite 代理配置：
  - `/api` → http://localhost:8099 (Gateway)
  - `/vr` → http://localhost:8900 (Vibe-Research)

## API 调用流程

```
前端 → Vite Proxy (/api) → Gateway (8099) → 对应微服务
前端 → Vite Proxy (/vr) → Vibe-Research (8900) → A股数据
```

Gateway 路由规则（application.yml）：
- `/user/**` → user-service:8081
- `/article/**` → blog-service:8082
- `/post/**` → forum-service:8083
- `/product/**` → shop-service:8084
- `/music/**` → media-service:8085
- `/strategy/**` → quant-service:8086
- `/script/**` → tool-service:8087
- `/software/**` → software-service:8088
- `/resource/**` → resource-service:8089
- `/jarvis/**` → jarvis-service:9001
- `/admin/**` → admin-service:9002

## 服务健康检查

```powershell
# 检查所有服务端口
netstat -ano | findstr "LISTENING" | findstr ":8081 :8082 :8083 :8084 :8085 :8086 :8087 :8088 :8089 :8099 :9001 :9002"

# 测试 API
curl http://localhost:8099/admin/dashboard
curl http://localhost:8099/article/list
curl http://localhost:8099/jarvis/commands
```

## 常见问题

### 1. Maven 构建失败：JAVA_HOME 未设置

```powershell
$env:JAVA_HOME = "F:\tools\jdk17"
```

确保 JAVA_HOME 指向 JDK 根目录（不是 bin 子目录）。

### 2. Maven 构建失败：文件被占用

先停止所有 Java 进程，再重新构建：

```powershell
taskkill //F //IM java.exe //IM javaw.exe
```

### 3. Nacos 连接失败

服务在 prod 模式下使用本地配置，Nacos 不可用时仍可正常运行（控制台有连接错误但不影响业务）。

### 4. 前端 API 报错 404

检查 Gateway (8099) 是否运行，确认 Vite 代理配置正确。

### 5. 数据库连接失败

确认 MySQL 已启动，且对应数据库已创建：

```powershell
mysql -u root -proot -e "SHOW DATABASES;"
```

## 项目结构

```
web3-blog/
├── backend/
│   ├── gateway/           # Spring Cloud Gateway
│   ├── user-service/      # 用户服务 (8081)
│   ├── blog-service/      # 博客服务 (8082)
│   ├── forum-service/     # 论坛服务 (8083)
│   ├── shop-service/      # 商城服务 (8084)
│   ├── media-service/     # 媒体服务 (8085)
│   ├── quant-service/     # 量化服务 (8086)
│   ├── tool-service/      # 工具服务 (8087)
│   ├── software-service/  # 软件服务 (8088)
│   ├── resource-service/  # 资源服务 (8089)
│   ├── jarvis-service/    # Jarvis AI 服务 (9001)
│   ├── admin-service/     # 管理后台 (9002)
│   └── common/            # 公共模块
│       ├── common-core/   # 核心类
│       ├── common-security/ # 安全
│       └── common-util/   # 工具类
├── frontend/
│   └── src/
│       ├── api/           # API 请求
│       ├── views/         # 页面视图
│       ├── components/    # 组件
│       ├── stores/        # Pinia 状态
│       └── composables/   # 组合函数
├── start-all-services.bat # 一键启动脚本
├── start-all-services.ps1 # PowerShell 启动脚本
└── deploy/                # 部署配置
    └── mysql/init/        # 数据库初始化 SQL
```
