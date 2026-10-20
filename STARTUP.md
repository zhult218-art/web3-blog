# Web3 Portal 启动文档（STARTUP）

> 最后更新：2026-10-20
> 适用：Windows 本地原生启动（不用 Docker）。Docker 方式见 `DEPLOY.md`，逐服务手动启动见 `RUN.md`。
> 本文件不再写死任何本机绝对路径——`<项目根>`、`<JDK17>`、`<MAVEN>` 请按实际环境替换。

---

## 一、组件清单（启动前确认）

| 组件 | 说明 | 端口 | 是否必需 |
|---|---|---|---|
| MySQL 8 | 系统服务（如 `MySQL80`） | 3306 | 必需 |
| Redis | `redis-server.exe` | 6379 | 必需（缓存/会话） |
| RabbitMQ | 需配套 Erlang OTP | 5672 | jarvis-service 需要，其余可缺 |
| Nacos | `startup.cmd -m standalone` | 8848 | 可选（`NACOS_ENABLED=false` 时服务可独立启动） |
| JDK 17 | 编译/运行后端 | - | 必需 |
| Maven 3.9+ | 打包后端 | - | 必需 |
| Node.js 18+ | 前端构建 | - | 必需 |
| Python 3.11+ | py-service | 9006 | 量化/AI/语音等模块需要 |

### 后端服务与端口（13 个）

| 服务 | 端口 | 服务 | 端口 |
|---|---|---|---|
| gateway | 8080 | resource-service | 8089 |
| user-service | 8081 | software-service | 8090 |
| blog-service | 8082 | ai-proxy-service | 8093 |
| forum-service | 8083 | jarvis-service | 9001 |
| shop-service | 8084 | admin-service | 9002 |
| media-service | 8085 | py-service | 9006 |
| quant-service | 8086 | node-service | 3000 / 3010 |
| tool-service | 8087 | | |

---

## 二、启动步骤（按顺序）

### 1. 依赖服务

```powershell
net start MySQL80
# Redis
& "<Redis目录>\redis-server.exe"
# Nacos（可选）
cd "<Nacos目录>\bin"; .\startup.cmd -m standalone
```

### 2. 环境变量

```powershell
Copy-Item .env.example .env    # 首次
# 至少填写：JWT_SECRET（≥32 字符）、MYSQL_PASSWORD、REDIS_PASSWORD
```

> `JWT_SECRET` 缺失或短于 32 字符时，所有服务会 **fail-fast 拒绝启动**（无兜底默认值，属刻意设计）。

### 3. 打包后端

```powershell
$env:JAVA_HOME = "<JDK17>"
cd backend
& "<MAVEN>\bin\mvn.cmd" clean package      # 含 24 个单元测试
```

> 打包前先 `taskkill /F /IM java.exe`，否则 jar 被占用会写入失败。

### 4. 启动后端

```powershell
# 仓库根目录执行；脚本会按依赖顺序拉起 Redis + 全部 Java 服务 + py-service
powershell -ExecutionPolicy Bypass -File .\start-all-services.ps1
```

- 日志：`logs\` 与各服务目录下 `*.log`
- 全部 `Started` 约需 1–2 分钟

### 5. 前端

```powershell
cd frontend
npm install     # 仅首次
npm run dev     # → http://localhost:5173
```

---

## 三、访问入口

| 用途 | 地址 |
|---|---|
| 前端门户 | http://localhost:5173 |
| API 网关 | http://localhost:8080 |
| py-service 文档 | http://localhost:9006/docs |
| py-service 健康检查 | http://localhost:9006/health |

---

## 四、自检

```powershell
foreach ($p in 3306,6379,8080,8081,8082,8083,8084,8085,8086,8087,8089,8090,8093,9001,9002,9006) {
  "{0}: {1}" -f $p, (Test-NetConnection localhost -Port $p -WarningAction SilentlyContinue).TcpTestSucceeded
}
# 8848 / 5672 可缺省
```

冒烟测试：

```powershell
curl http://localhost:8080/article/list       # 200
curl http://localhost:8080/admin/dashboard    # 401 属正常（需管理员登录）
curl http://localhost:9006/health             # ok:true + 各模块状态
```

---

## 五、常见问题

1. **RabbitMQ 起不来**：查 `%APPDATA%\RabbitMQ\log\*.log`；若报 `incompatible_feature_flags`，备份后删除 `%APPDATA%\RabbitMQ` 再重启（会重建数据）。
2. **服务起不来**：看对应 `*.log`；端口占用先 `taskkill /F /IM java.exe`。
3. **网关 502**：多为下游服务未启动或 `*_SERVICE_HOST` 配错（网关按 `${USER_SERVICE_HOST:localhost}:8081` 直连，不走 Nacos 服务发现）。
4. **前端能开但接口 401**：`JWT_SECRET` 与后端不一致（各服务必须用同一个密钥）。
5. **5173 被占用**：Vite 会自动换端口，以终端输出为准。
6. **Google dev mock 登录**：默认关闭（`GOOGLE_DEV_LOGIN_ENABLED=false`），本地调试置 `true`，生产必须 `false`。

---

## 六、当前环境注意

2026-10-20 核查：项目已从 `F:\project\my-blog\web3-blog` 迁移到 `Z:\project\myblog\web3`，
原文档中的 `F:\tools\jdk17`、`F:\tools\apache-maven-3.9.6`、`F:\soft\nodejs` 等路径**均已不存在**。
在本机重新编译后端前，需先安装 JDK 17 + Maven 3.9，并修正上述占位符。
