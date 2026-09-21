-- ============================================================
-- Supabase 量化行情表结构（web3-blog quant-py-service）
-- 用途：A股行情历史落库（全市场快照 / 个股K线 / 板块曲线 / 涨停池·连板·资金流）
-- 用法：登录 Supabase → SQL Editor → 粘贴本文件执行
-- 说明：数据写入由后端 service key（绕过 RLS）完成，匿名仅可读。
-- ============================================================

-- ------------------------------------------------------------
-- 1) 全市场每日快照 quant_daily_snapshot（每天收盘后存一次，5000+ 行/天）
--    主键 (trade_date, code)：每天每只股票一行，upsert 保证幂等
-- ------------------------------------------------------------
create table if not exists public.quant_daily_snapshot (
  id bigint generated always as identity primary key,
  trade_date text not null,          -- YYYY-MM-DD
  code text not null,                -- 6 位代码，如 600519
  name text,
  price numeric(18,3),
  change numeric(18,3),
  change_percent numeric(10,3),
  volume numeric(20,2),
  turnover numeric(20,2),
  high numeric(18,3),
  low numeric(18,3),
  open numeric(18,3),
  prev_close numeric(18,3),
  amplitude numeric(10,3),
  turnover_rate numeric(10,3),
  pe numeric(14,3),
  pb numeric(14,3),
  market_cap numeric(20,2),
  source text,
  created_at timestamptz not null default now(),
  unique (trade_date, code)
);

-- ------------------------------------------------------------
-- 2) 个股每日K线 quant_daily_kline（回测/盯盘用，全历史累加）
--    主键 (code, trade_date, period)：upsert 幂等
-- ------------------------------------------------------------
create table if not exists public.quant_daily_kline (
  id bigint generated always as identity primary key,
  code text not null,                -- 6 位代码
  trade_date text not null,          -- YYYY-MM-DD
  period text not null default '1d', -- 1d / 1w / 1mo
  open numeric(18,3),
  close numeric(18,3),
  high numeric(18,3),
  low numeric(18,3),
  volume numeric(20,2),
  turnover numeric(20,2) default 0,
  change_percent numeric(10,3),
  amplitude numeric(10,3),
  source text,
  created_at timestamptz not null default now(),
  unique (code, trade_date, period)
);

create index if not exists idx_quant_kline_code_date on public.quant_daily_kline (code, trade_date desc);

-- ------------------------------------------------------------
-- 3) 板块每日走势 quant_sector_daily（行业板块指数每日收盘）
--    主键 (sector, trade_date)
-- ------------------------------------------------------------
create table if not exists public.quant_sector_daily (
  id bigint generated always as identity primary key,
  sector text not null,              -- 板块名，如 半导体
  trade_date text not null,
  close numeric(18,3),
  pct numeric(10,3),
  source text,
  created_at timestamptz not null default now(),
  unique (sector, trade_date)
);

create index if not exists idx_quant_sector_date on public.quant_sector_daily (trade_date desc);

-- ------------------------------------------------------------
-- 4) 涨停池 / 连板晋级 / 资金流 每日快照 quant_daily_pool
--    按 (kind, trade_date) 一行 JSONB 存储
-- ------------------------------------------------------------
create table if not exists public.quant_daily_pool (
  id bigint generated always as identity primary key,
  kind text not null,                -- limit_up / limit_down / zhaban / board_progress / fund_flow
  trade_date text not null,
  payload jsonb not null,
  created_at timestamptz not null default now(),
  unique (kind, trade_date)
);

create index if not exists idx_quant_pool_kind_date on public.quant_daily_pool (kind, trade_date desc);

-- ------------------------------------------------------------
-- 5) 行情同步日志 quant_sync_log（增量任务执行痕迹）
-- ------------------------------------------------------------
create table if not exists public.quant_sync_log (
  id bigint generated always as identity primary key,
  kind text not null,
  trade_date text,
  status text,
  detail text,
  created_at timestamptz not null default now()
);

-- ------------------------------------------------------------
-- 5.1) 每日同步状态计数 quant_daily_count（调度判断当日是否已落库）
--      主键 (trade_date, table_name)：snapshot / sector / kline / pool
-- ------------------------------------------------------------
create table if not exists public.quant_daily_count (
  id bigint generated always as identity primary key,
  trade_date text not null,          -- YYYY-MM-DD
  table_name text not null,          -- snapshot / sector / kline / pool
  row_count integer not null default 0,
  status text,
  detail text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (trade_date, table_name)
);

create index if not exists idx_quant_count_date on public.quant_daily_count (trade_date desc);

-- ------------------------------------------------------------
-- 5.2) 行情同步告警 quant_sync_alert（自动增量失败写入，成功后 resolve；供前端公告轮询）
--      主键 (trade_date)：每天最多一条开放告警，close 后同一交易日不重复打扰
-- ------------------------------------------------------------
create table if not exists public.quant_sync_alert (
  id bigint generated always as identity primary key,
  trade_date text not null,          -- YYYY-MM-DD
  level text not null default 'error',   -- error / warning
  status text not null default 'open',   -- open / resolved
  detail text,
  created_at timestamptz not null default now(),
  resolved_at timestamptz,
  unique (trade_date)
);

create index if not exists idx_quant_alert_status on public.quant_sync_alert (status, trade_date desc);

-- ------------------------------------------------------------
-- RLS：匿名可读，写走 service key（绕过 RLS）
-- ------------------------------------------------------------
alter table public.quant_daily_snapshot enable row level security;
alter table public.quant_daily_kline enable row level security;
alter table public.quant_sector_daily enable row level security;
alter table public.quant_daily_pool enable row level security;
alter table public.quant_sync_log enable row level security;
alter table public.quant_daily_count enable row level security;
alter table public.quant_sync_alert enable row level security;

create policy "quant_snapshot_read" on public.quant_daily_snapshot for select using (true);
create policy "quant_kline_read" on public.quant_daily_kline for select using (true);
create policy "quant_sector_read" on public.quant_sector_daily for select using (true);
create policy "quant_pool_read" on public.quant_daily_pool for select using (true);
create policy "quant_sync_log_read" on public.quant_sync_log for select using (true);
create policy "quant_count_read" on public.quant_daily_count for select using (true);
create policy "quant_alert_read" on public.quant_sync_alert for select using (true);