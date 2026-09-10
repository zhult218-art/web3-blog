# 单体化迁移计划书

> 本文档为规划性质，不涉及代码变更。

## 现状

当前项目采用 Spring Cloud 微服务架构（16 个 Maven 模块、13 个独立服务进程），基础设施依赖 MySQL + Redis + RabbitMQ + Nacos。对于个人技术门户项目，这一架构存在以下问题：

| 问题 | 影响 |
|------|------|
| 部署复杂 | 需要管理 13+ 个进程，重启一个模块需独立操作 |
| 资源浪费 | 本地开发需同时运行 MySQL/Redis/RabbitMQ/Nacos，占用大量内存 |
| 跨服务调用 | 服务间 Feign 调用增加了延迟和故障点 |
| Nacos 依赖 | 注册/配置中心强依赖，无 Nacos 时部分功能异常（虽然已 optional 化） |
| 调试困难 | 分布式链路追踪需要额外工具，本地联调不便 |

## 目标架构

将现有 13 个服务合并为 **2 个单体应用**：

```
web3-blog
├── app/                          # 主应用（合并所有业务服务）
│   ├── controller/               # 按原模块分包
│   │   ├── UserController        # 原 user-service
│   │   ├── ArticleController     # 原 blog-service
│   │   ├── PostController        # 原 forum-service
│   │   ├── OrderController       # 原 shop-service
│   │   ├── MusicController       # 原 media-service
│   │   ├── QuantController       # 原 quant-service
│   │   ├── ScriptController      # 原 tool-service
│   │   ├── SoftwareController    # 原 software-service
│   │   ├── ResourceController    # 原 resource-service
│   │   ├── ChatController        # 原 jarvis-service
│   │   └── AdminController       # 原 admin-service
│   ├── service/                  # 业务逻辑层
│   ├── mapper/                   # MyBatis-Plus Mapper
│   ├── entity/                   # 实体类
│   ├── config/                   # 全局配置（安全、跨域、限流）
│   └── Application.java          # 单一入口
│
├── gateway/                      # 网关层（仅保留路由转发 + 鉴权）
│   └── 保留 Spring Cloud Gateway，但移除服务发现依赖
│
└── common/                       # 公共模块（保持不变）
    ├── common-core/
    ├── common-security/
    └── common-util/
```

## 迁移策略

### 第一阶段：渐进式合并（推荐）

按依赖关系从低到高逐个合并：

1. **无状态服务先行**（media/tool/software/resource/admin）
   - 这些服务间无互相调用
   - 直接将 controller/service/mapper/entity 移入主应用
   - 保留原有包结构（`com.web3.media.controller` 等）避免大范围 import 变更

2. **有状态服务**（user/blog/forum/shop/quant）
   - user-service 是核心（鉴权），最先合并
   - blog/forum/shop 通过 user-service 的 Feign 调用改为本地注入
   - jarvis-service 的 RabbitMQ 消息消费改为 Spring Event 或本地队列

3. **网关瘦身**
   - 移除 Nacos 服务发现
   - 网关路由改为固定 URL（`uri: http://localhost:8081/user/**`）
   - 或者进一步将网关功能合并到主应用（Spring MVC + Filter 实现鉴权）

### 第二阶段：基础设施简化

1. 移除 Nacos（配置迁移至 `application.yml` 或 Spring Cloud Config）
2. RabbitMQ 改为可选（仅 jarvis-service 使用，可替换为 Spring Event）
3. 端口统一：主应用监听 8080，前端直接对接

### 第三阶段：前端适配

1. Vite 代理从 `/api → gateway:8080` 改为 `/api → app:8080`
2. 移除前端路由守卫中的服务健康检查逻辑（单体不存在服务不可达问题）

## 数据库兼容性

合并后的主应用连接同一组 MySQL 数据库（`web3_user`、`web3_blog` 等），**不做库合并**。每个业务仍使用独立的 DataSource（通过 `AbstractRoutingDataSource` 或 Spring 多数据源配置），保持数据隔离。

## 风险与回退

| 风险 | 缓解措施 |
|------|---------|
| 合并过程中引入 bug | 每合并一个服务后立即运行回归测试 |
| 数据库连接池耗尽 | 监控 HikariCP 指标，必要时调整 pool size |
| 内存占用增加 | 单体 JVM 堆调至 2-4GB（远低于 13 个服务的总和） |

## 预期收益

- 部署从 `docker compose up 13 services` → `java -jar app.jar`（1 个进程）
- 本地开发仅需 MySQL + Redis（无需 RabbitMQ/Nacos）
- 启动时间从 30s × 13 ≈ 6.5 分钟 → 15-20 秒
- 跨服务调用延迟归零（本地方法调用）
- 资源占用降低约 70%（13 个 JVM → 1 个 JVM）
