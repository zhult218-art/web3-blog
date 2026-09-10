# API Token 中转站 · 说明文档（Overview & Operating Guide）
> 文档版本：v1.0　|　编写日期：2026-08-17　|　需求依据：《API中转站需求文档.md》

---

## 1. 这是什么

**API Token 中转站**是 Web3 博客体系内的一个 OpenAI 兼容 API 网关模块：把多个上游大模型渠道（DeepSeek / OpenAI / Claude / 通义等）的密钥统一收管，向用户发放 `sk-` 开头的独立令牌。用户侧体验与直连 OpenAI 完全一致——改一个 `base_url`、换一把钥匙，即可调用所有已接入模型。系统自动完成 **令牌鉴权 → 额度扣减 → 请求日志 → 用量统计 → 管理后台可视化** 的完整闭环。

一句话版本：**别人的 Key 我统一托管，你的 Key 我发给你，谁用了多少、花了多少钱，后台一看便知。**

---

## 2. 核心概念

| 概念 | 解释 | 类比 |
|---|---|---|
| **渠道（Channel）** | 一个上游 AI 平台的接入配置：地址 + Key + 可用模型 | 开在超市里的供货商 |
| **令牌（Token）** | 发给用户（或用户自己申请）的调用凭证，形如 `sk-xxxxxxxx...` | 超市给顾客的会员卡 |
| **模型倍率（Rate）** | 模型计费权重，`deepseek-chat=1`、`gpt-4o=15` 之类 | 商品计价系数 |
| **额度（Quota）** | 令牌/用户可消耗的总量，按倍率和 token 数扣减 | 会员卡里的余额 |
| **请求日志** | 每次调用的全量流水：谁、什么模型、多少 token、多少额度、是否成功 | 收银小票 |

**计费公式**（与 one-api 对齐）：
```
本次消耗额度 = 分组倍率 × 模型倍率 × (提示 token + 补全 token × 补全倍率)
```

---

## 3. 系统架构与模块职责

```
                    管理后台 /admin/ai-proxy
                ┌────────────────────────────────────┐
                │ 渠道管理·令牌管理·模型倍率·用量统计·日志 │
                └───────────────┬────────────────────┘
                                │ REST (admin-service)
┌─────────────┐   /v1/*        ▼
│ 用户应用     │ ─────────► gateway  AiProxyController
│ (OpenAI SDK)│              ┌──────────────────┐
└─────────────┘              │ ① JWT 式令牌鉴权    │
                             │ ② 额度校验+扣减    │
                             │ ③ 模型白名单+倍率  │
                             │ ④ 渠道选择与转发    │
                             │ ⑤ 日志落库        │
                             └────────┬─────────┘
                                      ▼
                   渠道1 DeepSeek ▲ 渠道2 OpenAI 渠道3 Claude…
```

| 模块 | 职责 | 位置 |
|---|---|---|
| 中转引擎（转发+鉴权+计费+日志） | 对外暴露 `/v1/chat/completions`、`/v1/models`，处理 OpenAI 兼容请求全生命周期 | `gateway` 内扩展 `AiProxyController` |
| 管理接口 | 渠道/令牌/模型倍率的增删改查、用量聚合报表 | `admin-service` 新增 |
| 管理前端 | `/admin/ai-proxy` 页面（4 Tab：渠道/令牌/统计/日志） | `frontend/src/views/admin/ai-proxy.vue` |
| 用户侧用量查看（P1） | 用户在小前台查看自己令牌余额与用量 | `user-service` + 前端「我的」页 |
| 数据库 | 表落在 `web3_admin` 库（见 §5） | `deploy/mysql/init/` 新增 `13-web3_ai_proxy.sql` |

**调用时序（一次完整请求）**

```
SDK → POST /v1/chat/completions
  → 取 Authorization: Bearer sk-xxx
  → 查 proxy_token（哈希匹配）
      ├─ 不存在/停用/过期 → 401 { "error": { "message": "Invalid token" } }
      ├─ 模型不在白名单 → 403
      └─ 额度不足 → 403 { "error": { "message": "Insufficient quota" } }
  → 按渠道策略选渠道（轮询/权重/随机），失败自动重试下一渠道
  → 转发上游（透传 body；流式走 SSE）
  → 收到 usage，按倍率算额度，扣 token.used_quota 与用户累计
  → 写 proxy_request_log → 返回上游响应
```

---

## 4. 安装与启动（实施期）

### 4.1 前置条件
- MySQL（本仓库 `deploy/mysql` 体系），`web3_admin` 库
- Nacos 配置中心或环境变量可用
- gateway、admin-service、user-service、前端 dev server 正常启动

### 4.2 步骤
1. **建表**：执行 `backend/deploy/mysql/init/13-web3_ai_proxy.sql`（含种子：1 个示例渠道、deepseek-chat 倍率记录）。
2. **配置**（Nacos 或环境变量）：
   | 配置项 | 默认 | 说明 |
   |---|---|---|
   | `AI_PROXY_TOKEN_SECRET` | 随机生成 | 令牌哈希 salt，务必改 |
   | `AI_PROXY_CHANNEL_KEY_SECRET` | 随机生成 | 渠道上游 Key 加密密钥 |
   | `APP_OPENAI_API_BASE_URL` | `http://localhost:8000/v1` | 保留原直连模式（未配置渠道时兜底） |
3. **启动/重启** `gateway`、`admin-service`。
4. **前端**：路由 `{ path: 'ai-proxy', component: () => import('@/views/admin/ai-proxy.vue') }` 挂到 AdminLayout children，菜单加「中转站」入口。
5. **联调自检**（同需求文档 P0 验收）：
   - 后台建渠道（DeepSeek）+ 给某用户发令牌（额度 10.00）
   - 用 OpenAI SDK 指向 `http://localhost:8080/v1`（gateway 端口）与令牌调用：
     ```python
     from openai import OpenAI
     client = OpenAI(base_url="http://localhost:8080/v1", api_key="sk-xxxx")
     resp = client.chat.completions.create(model="deepseek-chat",
                                           messages=[{"role":"user","content":"你好"}])
     ```
   - 后台「用量统计」出现该次请求，令牌额度相应扣减；`/v1/models` 返回白名单内模型。

---

## 5. 数据库设计（`web3_admin` 库）

### 5.1 proxy_channel（渠道）
| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | |
| name | VARCHAR(64) | 渠道名称 |
| base_url | VARCHAR(255) | 上游 Base URL，如 `https://api.deepseek.com/v1` |
| api_key_encrypted | VARCHAR(512) | 上游 Key，AES 加密存储 |
| models | JSON | 可用模型列表 `["deepseek-chat","deepseek-reasoner"]` |
| group_name | VARCHAR(32) | 分组（默认 `default`） |
| weight | INT | 权重（权重策略用） |
| status | TINYINT | 0 停用 / 1 启用 |
| balance | DECIMAL(12,2) | 上游余额（人工/自动维护，P2 自动查询） |
| created_at / updated_at | DATETIME | |

### 5.2 proxy_token（令牌）
| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | |
| user_id | BIGINT | 归属用户（web3_user） |
| token_hash | CHAR(64) | SHA-256(secret_salt + 完整令牌) |
| token_prefix | CHAR(8) | `sk-` 后 8 位，列表展示用 |
| name | VARCHAR(64) | 令牌备注名 |
| status | TINYINT | 0 停用 / 1 启用 |
| expired_at | DATETIME NULL | 空=永不过期 |
| quota | DECIMAL(12,4) | 总额度（空=不限） |
| used_quota | DECIMAL(12,4) | 已消耗 |
| model_whitelist | JSON NULL | 空=全部 |
| ip_whitelist | JSON NULL | 空=不限 |
| created_at | DATETIME | |

### 5.3 proxy_model（模型倍率）
| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | |
| model_name | VARCHAR(128) | 如 `deepseek-chat` |
| rate | DECIMAL(8,4) | 倍率（默认 1） |
| completion_rate | DECIMAL(8,4) | 补全倍率（默认 1） |
| group_name | VARCHAR(32) | 分组 |
| status | TINYINT | |

### 5.4 proxy_request_log（请求日志）
| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | |
| request_id | CHAR(32) | 幂等/排查用 |
| token_id | BIGINT | |
| user_id | BIGINT | |
| model | VARCHAR(128) | |
| channel_id | BIGINT | 实际转发渠道 |
| prompt_tokens / completion_tokens | INT | |
| rate | DECIMAL(8,4) | 本次实际倍率 |
| quota_used | DECIMAL(12,4) | 本次消耗 |
| latency_ms | INT | 上游耗时 |
| status_code | INT | 200/401/403/5xx… |
| client_ip | VARCHAR(64) | |
| created_at | DATETIME | 按日分区/索引 `(user_id, created_at)`、`(created_at)` |

> 日志表为只追加结构；用量统计 = 对日志表聚合（`GROUP BY 用户/模型/日期`），不额外建汇总表，量级可控。

---

## 6. 接口清单

### 6.1 OpenAI 兼容（对外，gateway `/v1`）
| 方法/路径 | 说明 | 鉴权 |
|---|---|---|
| `POST /v1/chat/completions` | 对话补全，支持 `stream`（SSE，P1） | Bearer 令牌 |
| `GET /v1/models` | 模型列表（按令牌白名单过滤） | Bearer 令牌 |
| `GET /v1/models/{model}` | 单模型详情 | Bearer 令牌 |

### 6.2 管理（admin-service，需 admin JWT）
| 方法/路径 | 说明 |
|---|---|
| `GET/POST/PUT/DELETE /admin/ai/channels` | 渠道 CRUD |
| `POST /admin/ai/channels/{id}/test` | 渠道连通测试（P1） |
| `GET/POST/PUT/DELETE /admin/ai/tokens` | 令牌 CRUD；创建返回完整令牌仅一次 |
| `GET /admin/ai/models` + `PUT /admin/ai/models/{id}` | 倍率管理 |
| `GET /admin/ai/stats/overview` | 总览：今日请求数/今日消耗/令牌数/渠道数 |
| `GET /admin/ai/stats/by-user` | 按用户聚合（用量排行） |
| `GET /admin/ai/stats/by-model` | 按模型聚合 |
| `GET /admin/ai/logs` | 日志明细（分页 + 筛选：用户/模型/状态/时间） |

---

## 7. 安全要点

1. **令牌只展示一次**：创建成功后仅弹窗返回完整令牌，此后只能看到前 8 位前缀。
2. **库中无明文**：令牌存 SHA-256(secret+token) 哈希；渠道上游 Key AES 加密；`AI_PROXY_TOKEN_SECRET`、`AI_PROXY_CHANNEL_KEY_SECRET` 部署时随机生成并妥善保管。
3. **错误信息不外泄上游**：对上游 5xx 统一返回 `503 upstream error`，不透出上游 Key/地址细节。
4. **限流**（P2）：单令牌 QPS、全局并发限制，超限 429。
5. **审计**：令牌停用/额度修改等管理操作写入现有 `admin_operation_log`，可追溯。

---

## 8. 常见问题

| 问题 | 解答 |
|---|---|
| 令牌能指定走某个渠道吗？ | P2 支持：`Authorization: Bearer sk-xxx-<渠道ID>`，与 one-api 行为一致 |
| 用户在哪注册？ | 复用现有 user-service 体系，中转站不做注册 |
| 流式响应怎么计费？ | 非流式精确按 usage 计费；流式先透传并累加流内 usage（上游支持时），否则乘数估算，P1 明细 |
| 没有配置任何渠道时？ | 回退到原有 `APP_OPENAI_API_BASE_URL` 直连模式，老功能不破坏 |
| 部署文件在哪？ | 新增建表脚本 `backend/deploy/mysql/init/13-web3_ai_proxy.sql`；新接口全部挂已有服务，无新容器 |