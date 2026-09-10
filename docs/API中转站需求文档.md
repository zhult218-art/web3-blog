# API Token 中转站模块 · 需求文档（Requirements Specification）
> 文档版本：v1.0　|　编写日期：2026-08-17　|　对标参考：one-api（songquanpeng/one-api）
> 优先级定义：**P0**＝必修（核心闭环）　**P1**＝完善（运营/体验）　**P2**＝增值（商业化/锦上添花）

---

## 1. 模块定位

在现有 Web3 博客体系内自建一个 **OpenAI 兼容的 API 中转站**：管理方在后台配置各 AI 渠道（DeepSeek / OpenAI / Claude / 通义等）的上游 Key，向用户发放独立令牌（Token），用户侧无需持有任何上游 Key，只用一枚令牌即可按 OpenAI 标准接口调用所有已接入模型。系统自动记账：**令牌额度扣减 → 请求日志 → 用量统计 → 管理后台可视化**。

与直接部署 one-api 的区别：**不引入任何新语言/新框架**，所有能力建设在本仓库现有微服务（gateway + user-service + admin-service）之上，前端复用现有 admin 后台框架，数据落现有 MySQL。

---

## 2. 现状与差距

### 2.1 现有资产
- `backend/gateway` 已有 `AiProxyController`（`POST /v1/chat/completions`）：无鉴权的纯反代，目标地址取环境变量 `APP_OPENAI_API_BASE_URL`（默认 `http://localhost:8000/v1`），透传客户端 `Authorization` 头。→ **这是中转站的最小雏形，但没有任何令牌校验、额度、计费、日志能力。**
- `user-service`：已有用户体系与 JWT 鉴权（`AuthUtils`）。
- `admin-service`：已有 `admin_dashboard_stat` 仪表盘模式、`admin_operation_log` 操作日志模式。
- 前端 `frontend/src/views/admin/*`：已有 dashboard / users / orders / quant / traffic / settings 等完整管理页 + `AdminLayout.vue`。
- 建表惯例：`backend/deploy/mysql/init/10-web3_admin.sql` 等按库分文件。

### 2.2 差距清单
| 差距 | 说明 |
|---|---|
| 无令牌体系 | 谁来调用、是否有权，完全无法区分 |
| 无额度/计费 | 无法限制用量、无法对账 |
| 无渠道管理 | 上游 Key 写死在环境变量，换渠道要重启 |
| 无日志与统计 | 调了哪些模型、花了多少 token，无据可查 |
| 无模型清单/倍率 | 无法定义"哪些模型可被调用、按什么倍率计费" |
| 无流式支持 | 现有 WebClient 反代为非流式，打字机效果缺失（P1） |

---

## 3. 目标架构

```
                        ┌──────────────────────────────┐
  用户应用 (OpenAI SDK) │        管理后台 (admin)        │
  Base = https://你的域名/v1   ├─ 渠道管理 / 令牌管理       │
  Key  = sk-xxxxxxxx        ├─ 用量统计 / 明细日志       │
        │                  └─ 模型倍率 / 系统设置        │
        ▼                            ▲
┌───────────────────┐                 │
│ gateway /v1/*      │                 │
│ ─ 令牌鉴权(JWT式)   │                 ▼
│ ─ 额度校验+扣减    │ ──────────  admin-service
│ ─ 模型白名单+倍率  │   统计/查询    └─ proxy_token
│ ─ 渠道选择(轮询/权重)                 proxy_channel
│ ─ 上游转发(含流式)  │                proxy_model
│ ─ 请求日志落库     │                proxy_usage_stat
└───────────────────┘                web3_admin (库)
        │
        ▼
  上游渠道 × N (DeepSeek / OpenAI / Claude / ...)
```

**关键设计决策**
1. **服务归属**：中转引擎（鉴权/转发/计费）落在 `gateway` 的 `AiProxyController` 内部扩展，或新建 `ai-proxy-service`（待定，默认先扩展 gateway，避免新服务维护成本）；统计接口落在 `admin-service`；令牌/渠道管理的写接口由 admin-service 提供，前端复用 admin 后台。
2. **令牌格式**：`sk-` 前缀 + 随机 48 位（`sk-` + 8 位可见前缀 + 32 位密钥），数据库只存哈希 + 前缀；用户页面仅创建时展示完整令牌一次。
3. **计费模型**（对齐 one-api）：`本次消费额度 = 分组倍率 × 模型倍率 × (提示 token + 补全 token × 补全倍率)`；额度货币化（默认按人民币分/元 记账，后台可调展示单位）。

---

## 4. 需求与验收

### P0 核心闭环（本次交付）
| # | 需求 | 验收标准 | 归属 |
|---|---|---|---|
| T1 | **令牌管理**：admin 可为指定用户生成/停用/删除令牌，令牌含过期时间、额度上限、可用模型白名单、可用 IP 白名单；数据库只存哈希与 8 位前缀 | 创建后仅弹窗展示一次完整 `sk-xxx`；停用后立即生效；过期/超额度请求返回 401/403 与明确错误信息 | gateway + admin-service + admin 前端 |
| T2 | **OpenAI 兼容端点**：`POST /v1/chat/completions`（JSON 透传）、`GET /v1/models`（按令牌白名单过滤返回模型清单） | 用 OpenAI SDK 改 `base_url` 指向本服务即可调通；`models` 返回受令牌限制的列表 | gateway |
| T3 | **额度扣减与记账**：每次请求按计费模型算出消耗额度、扣减令牌与用户剩余、写请求日志 | 日志含：令牌ID、用户、模型、渠道、prompt/补全 token、倍率、消耗额度、耗时、状态码、时间 | gateway |
| T4 | **请求日志与用量统计**：`admin-service` 提供按 用户/令牌/模型/日期 聚合的查询接口；admin 前端新增「中转站」菜单页：总览卡片（今日请求数/今日消耗/活跃令牌数）+ 明细日志表格（筛选：用户、模型、状态、时间范围） | 管理后台可看到真实请求记录与聚合数字，无 mock | admin-service + admin 前端 |
| T5 | **管理员界面**：admin 前端新增路由 `/admin/ai-proxy`（挂到 AdminLayout），包含 4 个 Tab：渠道、令牌、用量统计、日志 | 全流程可在界面完成：配渠道 → 发令牌 → 调接口 → 看到用量 | 前端 |

### P1 完善（运营向）
| # | 需求 | 验收标准 | 归属 |
|---|---|---|---|
| T6 | **渠道管理**：后台增删改查渠道（名称、上游 Base URL、API Key、可用模型列表、权重、分组、状态）；支持 轮询/权重/随机 三种选择策略与失败重试 | 渠道列表可视化；同一模型可配置多个渠道自动分流；某个渠道 Key 失效可自动切换不中断 | admin-service（渠道表）+ gateway 转发逻辑 |
| T7 | **模型倍率管理**：模型 → 倍率映射表（如 deepseek-chat=1.0、gpt-4o=15），后台可编辑；倍率作用于计费 | 修改倍率后新请求按新倍率计费 | admin-service + gateway |
| T8 | **流式支持（SSE）**：`stream: true` 时以 SSE 透传上游流式输出，并正确统计 usage | OpenAI SDK `stream=True` 打字机正常；流式与非流式日志都有 token 数据 | gateway |
| T9 | **用户视角用量查询**：普通用户在小前台（如「我的」页）可查看自己各令牌的剩余额度/已用量（只读） | 用户可看到自身用量，不可操作他人令牌 | user-service + 前端 |
| T10 | **接口健康自检**：渠道列表展示"测试连通"按钮，执行一次最小请求验证 Key 有效性 | 测试结果实时反馈在渠道列表 | admin-service + gateway |

### P2 增值（商业化/可选项）
| # | 需求 | 验收标准 | 归属 |
|---|---|---|---|
| T11 | 兑换码：批量生成/导出兑换码，兑换码为账户充值额度 | 兑换后额度到账、码作废 | admin-service |
| T12 | 余额告警：渠道余额或令牌额度低于阈值时，管理后台提示/邮件通知 | 阈值可配置，触达生效 | admin-service |
| T13 | 令牌指定渠道：`Authorization: Bearer sk-xxx-<渠道ID>` 强制走指定渠道 | 行为与 one-api 一致 | gateway |
| T14 | 用户分组与分组倍率：不同分组（免费/付费/VIP）不同倍率与默认额度 | 分组切换后计费生效 | admin-service + gateway |
| T15 | 失败重放与并发控制：单渠道 QPS 限制、全局并发限制 | 超限返回 429 | gateway |

---

## 5. 数据模型（初稿，细化在说明文档）

| 表 | 库 | 关键字段 |
|---|---|---|
| `proxy_channel` | web3_admin | id, name, base_url, api_key(加密), models(JSON), group_name, weight, status, balance, remark |
| `proxy_token` | web3_admin | id, user_id, token_hash, token_prefix, name, status, expired_at, quota(总), used_quota(已用), model_whitelist(JSON), ip_whitelist(JSON), created_at |
| `proxy_model` | web3_admin | id, model_name, rate, completion_rate, group_name, status |
| `proxy_request_log` | web3_admin | id, token_id, user_id, model, channel_id, prompt_tokens, completion_tokens, rate, quota_used, latency_ms, status_code, client_ip, request_id, created_at |
| `proxy_usage_stat`（可选聚合，或直接对日志表做 SQL 聚合） | web3_admin | stat_date, user_id, model, request_count, total_quota, total_tokens |

> 明确不做：不做用户注册/登录（复用现有 user-service 体系）；不做独立管理端站点（复用现有 admin 后台）。

---

## 6. 参考项目
- **one-api**（https://github.com/songquanpeng/one-api）：令牌管理（额度/过期/IP/模型白名单）、渠道负载均衡、分组倍率计费、兑换码、用量明细——本文档功能取舍与计费公式均对齐 it。
- 本项目 `AiProxyController.java`：现有无鉴权反代，作为改造起点。

---

## 7. 风险与注意
1. **上游 Key 存储**：渠道 API Key 不得明文入库 —— 加密存储 + 管理端展示脱敏（sk-****后4位）。
2. **令牌哈希**：`sk-` 完整令牌只显示一次；数据库遭泄露也不可反推令牌。
3. **大文件/长上下文请求**：反代需关闭超时限制或设置足够大的读超时，避免长报告生成被掐断。
4. **流式记账**：流式响应需同时透传 body 与 usage（若上游在流中给 usage 则累加，否则以请求前预估或按 tiktoken 本地估算，先实现在非流式精确记账、流式乘数估算，P1 注明）。
5. **多环境**：配置项均走 Nacos/环境变量，不动代码。