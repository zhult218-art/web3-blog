# 三大量化项目代码级移植到 py-service 实施方案

## Context

用户在 `Z:\project\quant` 下有三个独立量化项目，要求全部代码级移植到 py-service 的模块体系中：

| 项目 | 定位 | 规模 | 集成方式 |
|------|------|------|----------|
| tick-stock-panel-main | 市场驾驶舱（选股+监控+回测+因子挖掘+AI复盘） | 27 API路由 + 50+ 服务 + React前端 + tickflow SDK + Polars引擎 | 新建 `modules/cockpit` 模块 |
| a-stock-data-main | A股全栈数据工具包（43端点/10层/15数据源） | 纯Python数据获取函数集合（SKILL.md） | 移植到 `modules/quant/datasources/astock_ext.py` |
| daily_stock_analysis-main | AI大模型驱动多市场分析系统 | 14 API端点 + 数据源抽象 + LLM分析 + 调度器 | 新建 `modules/daily_analysis` 模块 |

py-service 模块约定（`core/loader.py`）：每个 `modules/<name>/` 包提供 `router`/`MODULE_META`/`setup`/`startup`/`shutdown`/`health`，框架自动发现挂载。

---

## Phase 1: tick-stock-panel → `modules/cockpit`（市场驾驶舱）

### 1.1 依赖安装

在 py-service venv 安装 tick-stock-panel 的后端依赖（`backend/pyproject.toml`）：

```
pip install polars>=1.44,<1.45 duckdb pyarrow apscheduler openai httpx numba fastexcel pypinyin pytesseract Pillow sse-starlette platformdirs winotify plyer psutil pyyaml python-dotenv tickflow[all]
```

注意：pandas 已装(3.0.6)，fastapi/uvicorn/pydantic 已有。

### 1.2 代码复制

将 `Z:\project\quant\tick-stock-panel-main\backend\app\` 整体复制到 `py-service\modules\cockpit\app\`。

保留原包结构不变（`app.api.*`、`app.services.*`、`app.strategy.*`、`app.tickflow.*` 等），避免大规模 import 路径重写。

### 1.3 模块包装层

创建 `modules/cockpit/__init__.py`：

```python
from .router import router
MODULE_META = {"title": "市场驾驶舱", "description": "A股选股+监控+回测+因子挖掘+AI复盘", "icon": "🚀", "version": "0.2.4"}

def health(): ...       # 检查 datastore/repo/quote_service 是否就绪
def startup(): ...      # 移植 main.py _application_lifespan 的启动流程
def shutdown(): ...      # 移植 main.py lifespan 的关闭流程
def setup(app): ...      # 挂载 React 前端静态文件
```

创建 `modules/cockpit/router.py`：
- 导入 `app.api` 下全部 27 个路由模块
- 创建 `APIRouter(prefix="/api/cockpit")`，将所有子路由 include 进去
- 原路由的 `/api/health` → `/api/cockpit/health`，`/api/kline/*` → `/api/cockpit/kline/*`，以此类推

### 1.4 关键适配

| 原始行为 | 适配方式 |
|----------|----------|
| `app/main.py` 的 FastAPI app + lifespan | 移植 lifespan 逻辑到 `startup()`/`shutdown()` |
| auth 中间件（cookie/session） | 删除——认证由 Java 网关处理 |
| CORS 中间件 | 删除——py-service 主 app 已配置 |
| `app/config.py` 的 `settings` | 适配 data_dir → `py-service/data/cockpit/`，从 .env 读环境变量 |
| 原路由前缀 `/api/*` | 统一加 `/api/cockpit/` 前缀 |
| React 前端 API 调用 `/api/*` | 构建时配置 VITE_API_BASE=/api/cockpit |

### 1.5 React 前端构建与挂载

1. `cd Z:\project\quant\tick-stock-panel-main\frontend && npm install && npm run build`
2. 将 `frontend/dist/` 复制到 `py-service/modules/cockpit/static/`
3. `setup(app)` 中 `app.mount("/cockpit", StaticFiles(directory=static_dir, html=True))`
4. 更新 `frontend/src` 中 API base URL 配置（环境变量或 vite.config.ts）使其请求 `/api/cockpit/*`

### 1.6 前端适配

- `vite.config.js`：/cockpit 代理改为指向 9006（py-service），不再指向 3030
- `MarketCockpit.vue`：iframe src 保持 `/cockpit/`，py-service 的 StaticFiles 挂载会提供前端

### 1.7 数据目录

创建 `py-service/data/cockpit/`，tick-stock-panel 的 DataStore/KlineRepository/mining_manager 等在此目录存储 K线数据、enriched 数据、策略文件、回测结果等。

---

## Phase 2: a-stock-data → `modules/quant/datasources/astock_ext.py`（数据函数移植）

### 2.1 依赖安装

```
pip install mootdx stockstats
```

（requests/pandas/lxml/beautifulsoup4 已有）

### 2.2 函数移植

从 `Z:\project\quant\a-stock-data-main\SKILL.md` 提取 43 个数据获取函数，按十层组织到 `modules/quant/datasources/astock_ext.py`：

| 层 | 函数 | 数据源 |
|----|------|--------|
| 行情 | tdx_client/bars/quotes/transaction, tencent_quote, baidu_kline_with_ma | 通达信/腾讯/百度 |
| 研报 | eastmoney_reports, download_pdf, eastmoney_industry_reports, ths_eps_forecast, iwencai_search | 东财/同花顺/iwencai |
| 信号 | ths_hot_reason, hsgt_realtime, eastmoney_concept_blocks, eastmoney_fund_flow_minute, dragon_tiger_board, lockup_expiry, industry_comparison, daily_dragon_tiger | 同花顺/东财 |
| 资金面 | margin_trading, block_trade, holder_num_change, dividend_history, stock_fund_flow_120d | 东财 |
| 新闻 | eastmoney_stock_news, cls_telegraph, eastmoney_global_news | 东财/财联社 |
| 财务 | client.finance, client.F10, eastmoney_stock_info, sina_financial_report | 通达信/东财/新浪 |
| 公告 | cninfo_announcements | 巨潮 |
| 打板 | em_zt_pool, em_zb_pool, em_dt_pool, em_yzt_pool, ths_limit_up_pool, limit_up_sentiment | 东财/同花顺 |
| 期权 | sina_option_codes, sina_option_tquote, sina_option_greeks | 新浪 |
| 舆情 | cninfo_irm, ths_hot_list, em_hot_rank, em_hot_concept | 巨潮/同花顺/东财 |
| 备用源 | dragon_tiger_backup, fund_flow_backup, announcements_backup | 交易所官方/新浪 |
| 估值 | forward_pe, pe_digestion, calc_peg, full_valuation | 本地计算 |

保留 `em_get()` 限流防封机制。

### 2.3 路由暴露

创建 `modules/quant/routers/data_ext.py`，为每个函数创建 GET 端点：
- `/api/quant/data-ext/reports/{code}` → eastmoney_reports
- `/api/quant/data-ext/dragon-tiger/{code}` → dragon_tiger_board
- `/api/quant/data-ext/limit-up-pool` → em_zt_pool
- ... （43 个端点）

在 `modules/quant/routers/__init__.py` 中注册新路由。

---

## Phase 3: daily_stock_analysis → `modules/daily_analysis`（AI 分析移植）

### 3.1 依赖安装

```
pip install litellm tiktoken tavily-python schedule exchange-calendars tenacity sqlalchemy jinja2 markdown2 newspaper3k json-repair orjson fake-useragent google-search-results
```

### 3.2 代码复制

将 `daily_stock_analysis-main/src/` 和 `daily_stock_analysis-main/api/` 复制到 `py-service/modules/daily_analysis/`。

### 3.3 模块包装层

创建 `modules/daily_analysis/__init__.py`：
- `MODULE_META`：title="AI股票分析", icon="🤖"
- `router`：导入 `api/v1/router.py` 的所有端点，挂载到 `/api/daily-analysis` 前缀下
- `startup()`：移植 main.py 的调度器启动 + 首次分析线程
- `shutdown()`：停止调度器
- `health()`：检查调度器状态

### 3.4 关键适配

| 原始行为 | 适配方式 |
|----------|----------|
| `server.py` 的 FastAPI app | 删除——用 py-service 主 app |
| `api/middlewares/auth.py` | 删除——认证由 Java 网关处理 |
| `api/middlewares/error_handler` | 保留，挂到模块 router |
| `src/config.py` 的 .env 加载 | 适配 py-service 的 .env |
| `data_provider/base.py` 数据源抽象 | 保留，新增 akshare/mootdx 适配 |
| `main.py` 的 `--serve` 模式 | 移植到 startup() |
| SQLite 数据库路径 | 改为 `py-service/data/daily_analysis/` |

### 3.5 14 个端点模块路由

原 `/api/v1/*` → `/api/daily-analysis/*`：
- `/api/daily-analysis/stocks` — 股票列表
- `/api/daily-analysis/analysis` — AI 分析
- `/api/daily-analysis/decision-signals` — 决策信号
- `/api/daily-analysis/screening` — 选股
- `/api/daily-analysis/portfolio` — 组合
- `/api/daily-analysis/intelligence` — 情报
- `/api/daily-analysis/history` — 历史
- `/api/daily-analysis/backtest` — 回测
- `/api/daily-analysis/alerts` — 告警
- `/api/daily-analysis/agent` — Agent
- `/api/daily-analysis/data` — 数据
- `/api/daily-analysis/usage` — 使用量
- `/api/daily-analysis/system-config` — 系统配置
- `/api/daily-analysis/health` — 健康检查

---

## 验证方案

1. **启动验证**：`start-all-services.ps1` 启动 py-service 后检查：
   - `/health` 返回包含 `cockpit`、`daily_analysis`、`quant` 三个模块
   - `/api/cockpit/health` 返回 200
   - `/api/daily-analysis/health` 返回 200
   - `/api/quant/data-ext/dragon-tiger/600519` 返回数据

2. **驾驶舱前端验证**：访问 `/cockpit/` 加载 React SPA，API 请求走 `/api/cockpit/*`

3. **数据函数验证**：调用 `/api/quant/data-ext/reports/600519` 返回研报数据

4. **AI 分析验证**：调用 `/api/daily-analysis/analysis` 返回分析结果

5. **start-all-services.ps1 更新**：确保 py-service 启动时三个新模块自动加载

---

## 实施顺序

1. **Phase 2 优先**（a-stock-data）：纯函数移植，风险最低，最快见效
2. **Phase 1**（tick-stock-panel）：依赖安装 + 代码复制 + 模块包装 + 前端构建
3. **Phase 3**（daily_stock_analysis）：AI 分析移植，依赖最重

每个 Phase 完成后单独验证，不混合调试。
