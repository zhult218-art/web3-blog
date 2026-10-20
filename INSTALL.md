# 本地安装指南（INSTALL）

> 最后更新：2026-10-20
> 本文件只讲「从零装环境」，启动顺序见 `STARTUP.md`，生产部署见 `DEPLOY.md`。
> 2026-10-20 重写：原版指向 `C:\Users\zhult\Documents\Codex\...` 与 `backend\mvnw.cmd`，
> 二者在本仓库中均已不存在（**项目未内置 Maven Wrapper**），照原文操作会直接失败。

---

## 一、安装依赖

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | 17 | Spring Boot 3.3 要求 17+，不要用 8/11 |
| Maven | 3.9+ | 项目**不含** `mvnw`，必须装 Maven 并加入 PATH |
| Node.js | 18+ | 前端 Vite 5 要求 |
| Python | 3.11+ | py-service（量化/AI/语音模块） |
| MySQL | 8.0+ | 11 个 `web3_*` 库 |
| Redis | 6+ | 缓存/会话 |
| RabbitMQ | 3.12+ | jarvis-service 依赖；需配套 Erlang |
| Nacos | 2.x | 可选（`NACOS_ENABLED=false` 时不需要） |

验证安装：

```powershell
java -version      # 期望 17.x
mvn -v             # 期望 3.9.x
node -v            # 期望 >= 18
python --version   # 期望 >= 3.11
```

> 若 `mvn` 不在 PATH：把 `<Maven目录>\bin` 加入系统 PATH，或全程用绝对路径
> `<Maven目录>\bin\mvn.cmd` 代替下文 `mvn`。

---

## 二、后端

```powershell
cd backend
mvn -B clean package        # 含 24 个单元测试；首次会下载依赖，耗时较长
```

只编译不打包（快速验证改动）：

```powershell
mvn -B -DskipTests compile
```

> 打包前请先 `taskkill /F /IM java.exe`，否则 `target/*.jar` 被占用会写入失败。
> 若只想跑测试：`mvn -B test`（全部为 Mockito 单测，不需要 DB / 中间件）。

产物位置：`<模块>/target/<模块>-1.0.0.jar`

---

## 三、前端

```powershell
cd frontend
npm install        # 仅首次
npm run dev        # http://localhost:5173
```

生产构建：

```powershell
npm run build      # 产物在 frontend/dist
npm run preview    # 本地预览构建结果（默认 4173）
```

---

## 四、py-service

```powershell
cd py-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python main.py      # 默认 9006，文档 http://localhost:9006/docs
```

---

## 五、数据库

```powershell
# 1. 建库（11 个 web3_* 库）
mysql -u root -p < backend/deploy/mysql/init/01-databases.sql

# 2. 按文件名顺序逐个导入剩余 18 个脚本
Get-ChildItem backend/deploy/mysql/init/*.sql | Where-Object { $_.Name -ne '01-databases.sql' } |
  Sort-Object Name | ForEach-Object {
    mysql -u root -p --default-character-set=utf8mb4 < $_.FullName
  }
```

> 每个脚本内都带 `USE <库>;`，无需手动指定 `--database`。
> 全部 INSERT 为幂等写法（`INSERT IGNORE` / `ON DUPLICATE KEY`），可重复执行。
>
> Docker 方式无需手动导入：MySQL 容器首次启动会自动按文件名顺序执行 `init/` 下全部脚本。

---

## 六、Docker 方式（可选）

```powershell
# 基础设施
docker compose up -d mysql redis rabbitmq

# 后端全部服务（注意：含 ai-proxy-service / jarvis-service / admin-service）
docker compose up -d gateway user-service blog-service forum-service shop-service `
  media-service quant-service tool-service software-service resource-service `
  ai-proxy-service jarvis-service admin-service
```

> 执行前必须先准备 `.env`：`MYSQL_ROOT_PASSWORD`、`REDIS_PASSWORD`、`RABBITMQ_DEFAULT_USER/PASS`
> 在 compose 中为强制变量，缺失会让 `docker compose up` 直接报错。

---

## 七、环境变量

```powershell
Copy-Item .env.example .env
```

必填项：

| 变量 | 说明 |
|---|---|
| `JWT_SECRET` | **≥32 字符**，缺失或过短会导致所有服务启动失败（无兜底默认值） |
| `MYSQL_PASSWORD` | MySQL 密码 |
| `REDIS_PASSWORD` | Redis 密码（本地无密码时留空） |

其余第三方 Key（AI 提供商、支付、OSS、Shodan 等）均可留空，对应功能自动降级或跳过，不影响启动。

---

## 八、常见问题

1. **`mvn` 不是内部或外部命令** → Maven 未装或未入 PATH，见第一节。
2. **`JWT_SECRET must be at least 32 characters`** → 密钥太短，换一个长随机串。
3. **`npm install` 报缓存权限错误** → 关掉占用 `AppData\Local\npm-cache` 的编辑器后重试。
4. **MySQL 初始化只成功了一部分** → 说明某个脚本中途报错；容器方式需 `docker compose down -v` 清 volume 后重来。
5. **前端 401** → 前后端 `JWT_SECRET` 不一致。
