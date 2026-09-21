-- ============================================================
-- 量化扩展数据表（研报 / 新闻 / 龙虎榜）
-- 用途：所有获取到的非行情数据均落库，便于回测/复盘/AI分析
-- 用法：登录 Supabase → SQL Editor → 粘贴本文件执行
-- ============================================================

-- ------------------------------------------------------------
-- 1) 券商研报元数据 quant_research_reports
--    主键 report_id（东财 infoCode）；upsert 幂等
-- ------------------------------------------------------------
create table if not exists public.quant_research_reports (
  id bigint generated always as identity primary key,
  report_id text not null,
  title text,
  stock_code text,
  stock_name text,
  org_name text,
  author text,
  publish_date text,
  rating text,
  rating_change text,
  target_price numeric(14,3),
  eps_y1 numeric(10,4),
  eps_y2 numeric(10,4),
  eps_y3 numeric(10,4),
  summary text,
  url text,
  created_at timestamptz not null default now(),
  unique (report_id)
);

create index if not exists idx_reports_code on public.quant_research_reports (stock_code, publish_date desc);
create index if not exists idx_reports_date on public.quant_research_reports (publish_date desc);

-- ------------------------------------------------------------
-- 2) 财经新闻 quant_news
--    主键 id（源站新闻ID）；upsert 幂等
-- ------------------------------------------------------------
create table if not exists public.quant_news (
  id bigint generated always as identity primary key,
  news_id text not null,
  title text,
  source text,
  category text,
  sentiment text,
  impact text,
  news_time text,
  summary text,
  url text,
  created_at timestamptz not null default now(),
  unique (news_id)
);

create index if not exists idx_news_time on public.quant_news (news_time desc);

-- ------------------------------------------------------------
-- 3) 龙虎榜 quant_dragon_tiger
--    主键 (trade_date, code)
-- ------------------------------------------------------------
create table if not exists public.quant_dragon_tiger (
  id bigint generated always as identity primary key,
  trade_date text not null,
  code text not null,
  name text,
  close numeric(18,3),
  change_percent numeric(10,3),
  buy_amount numeric(20,2),
  sell_amount numeric(20,2),
  net_amount numeric(20,2),
  seats jsonb,
  reason text,
  created_at timestamptz not null default now(),
  unique (trade_date, code)
);

create index if not exists idx_dragon_date on public.quant_dragon_tiger (trade_date desc);

-- ------------------------------------------------------------
-- RLS
-- ------------------------------------------------------------
alter table public.quant_research_reports enable row level security;
alter table public.quant_news enable row level security;
alter table public.quant_dragon_tiger enable row level security;

create policy "reports_read" on public.quant_research_reports for select using (true);
create policy "news_read" on public.quant_news for select using (true);
create policy "dragon_read" on public.quant_dragon_tiger for select using (true);
