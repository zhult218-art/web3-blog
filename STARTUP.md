# Web3-Blog 项目启动文档

2026-08-13 实测可用版。组件全部在本地 Windows,无需 Docker。

## 一、组件清单(启动前确认)

| 组件 | 版本/路径 | 端口 | 状态检查 |
|------|----------|------|---------|
| MySQL | 系统服务(`MySQL Server 8.0`) | 3306 | 已运行 ✅ |
| Redis | `F:\tools\redis\redis-server.exe` | 6379 | 由脚本启动 |
| Erlang OTP | `F:\tools\rabbitmq\Erlang OTP`(27.3.4.16,2026-08-13 纯净重装) | - | - |
| RabbitMQ | `F:\tools\rabbitmq\Rabbitmq Server4.3.4`(4.3.4) | 5672 | 手动启动/脚本 |
| Nacos | `F:\tools\nacos\bin`(standalone) | 8848 | 已运行(可选) |
| 后端微服务 ×12 | `F:\tools\jdk17`,jar 在 `backend\*\target\` | 8080-8089, 9001-9002 | 一键脚本 |
| 前端 | `F:\soft\nodejs\nodejs`(Vite 5) | 5173 | `npm run dev` |

> RabbitMQ/Erlang 曾损坏(混合 OTP 版本导致),已于 2026-08-13 重装 Erlang OTP 27 并重置 RabbitMQ 数据,现运行正常。

## 二、启动步骤(按顺序)

### 1. 依赖服务

```powershell
# MySQL、Nacos 若未运行:
net start MySQL80                     # 或服务管理器启动
cd F:\tools\nacos\bin; .\startup.cmd -m standalone
```

### 2. 后端一键启动(含 Redis + 12 个微服务)

双击 `F:\project\my-blog\web3-blog\start-all-services.bat`,
或:

```powershell
Start-Process -FilePath "F:\project\my-blog\web3-blog\start-all-services.bat" -WindowStyle Hidden
```

- 日志:`backend\target\logs\svc_*.log`
- 等 1-2 分钟全部 `Started` 后可用

### 3. 前端

```powershell
cd F:\project\my-blog\web3-blog\frontend
npm install   # 仅首次
npm run dev   # → http://localhost:5173
```

## 三、打开

| 访问 | 地址 |
|------|------|
| **前端门户** | **http://localhost:5173** |
| API 网关 | http://localhost:8080 |
| 微服务 | 8081 user / 8082 blog / 8083 forum / 8084 shop / 8085 media / 8086 quant / 8087 tool / 8088 software / 8089 resource / 9001 jarvis / 9002 admin |

## 四、端口与状态自检

```powershell
foreach ($p in 3306,6379,5672,8848,5173,8080,8081) {
  "${p}: $((Test-NetConnection localhost -Port $p -WarningAction SilentlyContinue).TcpTestSucceeded)"
}
# 期望全部 True(8848 可缺省)
```

API 冒烟测试:

```powershell
curl http://localhost:8080/article/list      # 200
curl http://localhost:8080/admin/dashboard   # 401 属正常(需登录)
```

## 五、常见问题

1. **RabbitMQ 启动失败** → 查 `%APPDATA%\RabbitMQ\log\*.log`。若报 `incompatible_feature_flags ... unknown_instruction`,是数据目录与 Erlang 版本不匹配:
   备份后删除 `%APPDATA%\RabbitMQ`(含 db/log),再重启 RabbitMQ(会重建全新数据)。
2. **RabbitMQ 手动启动(前台查错)**:
   `set ERLANG_HOME=F:\tools\rabbitmq\Erlang OTP` 后运行
   `F:\tools\rabbitmq\Rabbitmq Server4.3.4\rabbitmq_server-4.3.4\sbin\rabbitmq-server.bat`
3. **服务起不来** → 看 `backend\target\logs\svc_*.log`;端口被占则先 `taskkill /F /IM java.exe` 再启动。
4. **重新构建 jar**:`$env:JAVA_HOME="F:\tools\jdk17"; F:\tools\apache-maven-3.9.6\bin\mvn.cmd clean package -DskipTests`(backend 目录)。
5. **5173 冲突**:OpenCut 项目可能占用,前端端口看 vite 输出。

## 六、现在的运行实例(保存于 2026-08-13)

- RabbitMQ 5672 ✅ / Redis 6379 ✅ / MySQL 3306 ✅ / Nacos 8848 ✅
- 网关 8080 + 全部 12 微服务 ✅
- 前端 5173 ✅

> 注:仓库内 `RUN.md` 为旧版文档(端口 8099、RabbitMQ 3.12 等描述已过时),以本文档为准。