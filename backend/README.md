# Web3 Portal 后端

Spring Cloud Alibaba 微服务集群：Spring Boot 3 + Spring Cloud Gateway + Nacos（注册/配置中心）+ MyBatis-Plus + MySQL / Redis / RabbitMQ。共 12 个业务微服务 + 3 个公共模块。

## 架构

```
┌────────────┐   /api/**   ┌─────────┐   路由    ┌──────────────────┐
│  Vue 前端   │ ─────────► │ Gateway │ ─────────► │ 各业务微服务 8081~9002 │
└────────────┘             │  8080   │           └──────────────────┘
                           └────┬────┘                    │
                                │ 注册/发现/配置           ▼
                                │              MySQL / Redis / RabbitMQ
                          ┌─────┴─────┐
                          │ Nacos 8848 │
                          └───────────┘
```

- **Nacos**：服务注册发现 + 配置中心。各服务通过 `optional:nacos:${spring.application.name}.yaml` 拉取配置，Nacos 不可用时回退本地配置（dev/prod profile 均已内置），不影响运行。
- **Gateway**：唯一对外入口，按路径前缀路由到各服务。
- **权限**：`common-security` 提供 JWT token 解析过滤器与权限校验，`PublicSecurityConfig`（各服务内）放行公开接口。

## 服务清单

| 模块 | 端口 | 数据库 | 职责 |
|---|---|---|---|
| `gateway` | 8080 | - | API 网关：统一路由、跨域、AI 代理（/v1） |
| `user-service` | 8081 | web3_user | 注册/登录（JWT）、用户资料、权限码管理 |
| `blog-service` | 8082 | web3_blog | 文章 CRUD、分类、标签、公开接口 |
| `forum-service` | 8083 | web3_forum | 帖子、评论、点赞（含积分/金币逻辑） |
| `shop-service` | 8084 | web3_shop | 商品、订单、支付（支付宝沙箱/模拟） |
| `media-service` | 8085 | web3_media | 音乐、视频、书籍、播放列表，媒体文件安全校验 |
| `quant-service` | 8086 | web3_quant | 量化策略、股票行情、龙虎榜、研报 |
| `tool-service` | 8087 | web3_tool | 在线脚本工具（加解密、二维码等） |
| `software-service` | 8088 | web3_software | 软件列表与下载 |
| `resource-service` | 8089 | web3_resource | 资源上传 / 下载（本地磁盘存储） |
| `jarvis-service` | 9001 | web3_jarvis | JARVIS AI 语音助手（语音会话 + RabbitMQ 命令） |
| `admin-service` | 9002 | web3_admin | 管理后台：仪表盘、用户管理、订单、流量统计、操作日志 |

## 公共模块（common/）

| 模块 | 职责 |
|---|---|
| `common-core` | 统一返回体 `Result`、全局异常处理、通用枚举/常量、Web 配置（CORS、JSON） |
| `common-security` | JWT 生成/校验、`@RequireAdmin` / `@RequireLogin` 注解、token 过滤器（Gateway 与各服务共用） |
| `common-util` | 通用工具类（时间、ID 生成、字符串等） |

## Gemini 公共模块依赖

每个业务服务 `pom.xml` 均依赖 `common-core` 与 `common-security`，需以 `mvn install` 方式先安装到本地仓库：

```powershell
$env:JAVA_HOME = "F:\tools\jdk17"
cd backend
F:\tools\apache-maven-3.9.6\bin\mvn.cmd install -pl common/common-core,common/common-security,common/common-util -DskipTests
```

## 网关路由表（gateway/src/main/resources/application.yml）

| 前缀 | 目标服务 |
|---|---|
| `/user/**` | user-service:8081 |
| `/article/**` | blog-service:8082 |
| `/post/**` `/comment/**` | forum-service:8083 |
| `/product/**` `/order/**` `/pay/**` | shop-service:8084 |
| `/music/**` `/book/**` `/video/**` `/playlist/**` | media-service:8085 |
| `/strategy/**` `/quant/**` `/stock/**` | quant-service:8086 |
| `/script/**` | tool-service:8087 |
| `/software/**` | software-service:8088 |
| `/resource/**` | resource-service:8089 |
| `/jarvis/**` | jarvis-service:9001 |
| `/admin/**` | admin-service:9002 |
| `/v1/**` | Gateway 内置 AI 代理（ChatGPT 风格，转发到外部模型） |

## 构建与启动

### 1. 环境要求

JDK 17+、Maven 3.8+（本机路径 `F:\tools\jdk17`、`F:\tools\apache-maven-3.9.6`）、MySQL 8、Redis、RabbitMQ、Nacos 2.x。

### 2. 一键启动（Windows）

双击根目录 `start-all-services.bat`，或：

```powershell
powershell -ExecutionPolicy Bypass -File ..\start-all-services.ps1   # 在 backend 目录下执行
```

脚本会自动以 jar 方式启动全部 12 个服务。

### 3. 手动打包 + 启动

```powershell
$env:JAVA_HOME = "F:\tools\jdk17"
cd backend

# 首次：安装公共模块 + 全量打包
F:\tools\apache-maven-3.9.6\bin\mvn.cmd clean install -DskipTests

# 逐个启动（prod 模式不依赖 Nacos 即可运行；dev 模式需 Nacos 可用）
java -jar gateway/target/gateway-1.0.0.jar --spring.profiles.active=prod
java -jar user-service/target/user-service-1.0.0.jar --spring.profiles.active=prod
# ... 其余服务同理（各服务 jar 名形如 <name>-1.0.0.jar）
```

> 各服务本地配置（端口、数据库账号密码、Redis 等）位于各自 `src/main/resources/application.yml`；`SPRING_PROFILES_ACTIVE=dev` 的容器化配置由 docker-compose.yml 注入环境变量。

### 4. 数据库初始化

```powershell
# 方式一（容器）：MySQL 容器启动时自动执行 backend/deploy/mysql/init/ 下的 12 个 SQL
# 方式二（本地）：
mysql -u root -proot < backend/deploy/mysql/init/01-databases.sql    # 建 11 个 web3_* 库
mysql -u root -proot --database=web3_user < backend/deploy/mysql/init/02-web3_user.sql
# ... 02~12 按编号与库名一一对应导入
```

### 5. Docker 部署

```bash
docker compose up -d mysql redis rabbitmq nacos
docker compose up -d gateway user-service ... # 所有服务
# 或使用 Linux 脚本：./deploy.sh
```

## 服务内部结构

所有业务服务遵循同一分层（以 blog-service 为例）：

```text
blog-service/src/main/java/.../
├── BlogApplication.java      # 启动类
├── controller/               # Controller：接收请求、返回 Result（路由前缀与网关一致）
├── service/                  # Service：业务逻辑
├── mapper/                   # Mapper：MyBatis-Plus 数据访问
├── entity/                   # Entity：数据库实体（对应表名已在类注释标注）
├── dto/                      # DTO：入参对象
└── vo/                       # VO：出参对象
```

## 鉴权说明

- **注册/登录**：`POST /user/register`、`POST /user/login` → 返回 JWT token，前端存 localStorage。
- **受保护接口**：携带 `Authorization: Bearer <token>` 请求头。
- **管理员接口**：`@RequireAdmin` 注解 + 前端 `permissions` 字段（用户权限码）双重校验。
- **公开接口**：各服务 `PublicSecurityConfig` 显式放行（如文章列表、媒体流等）。

## 相关文档

- 网关路由/端口全集：见根目录 `README.md` 端口清单
- 数据库表设计：`backend/deploy/mysql/init/*.sql`
- 运行排障：`RUN.md` 常见问题章节