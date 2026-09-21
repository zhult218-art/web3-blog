-- ============================================================
-- 个股基本面历史通用表（quant_stock_history）
-- 用途：财务摘要 / 股东户数 / 融资融券 / 资金流向 / 分红送配 / 大宗交易
--       六类历史数据统一落库，规避东财/同花顺对 IP 的间歇风控，
--       并为回测/复盘提供本地历史数据源。
-- 执行方式：Supabase Dashboard → SQL Editor → 粘贴执行一次即可
-- 写入方：quant-py-service（service key，绕过 RLS）
-- ============================================================
create table if not exists public.quant_stock_history (
  kind      text        not null,                 -- finance/holders/margin/fundflow/dividend/blocktrade
  code      text        not null,                 -- 6 位股票代码（如 600519）
  date_key  text        not null,                 -- 数据日期或报告期（YYYY-MM-DD）
  payload   jsonb       not null default '{}'::jsonb,  -- 原始数据行
  updated_at timestamptz not null default now(),
  primary key (kind, code, date_key)
);

-- 常用查询索引
create index if not exists idx_quant_stock_history_code
  on public.quant_stock_history (code, kind, date_key desc);

-- updated_at 自动更新
create or replace function public.touch_quant_stock_history()
returns trigger as $$
begin
  new.updated_at = now();
  return new;
end;
$$ language plpgsql;

drop trigger if exists trg_touch_quant_stock_history on public.quant_stock_history;
create trigger trg_touch_quant_stock_history
  before update on public.quant_stock_history
  for each row execute function public.touch_quant_stock_history();

-- 服务端直连写入（service key 自动绕过 RLS），匿名读不到，防数据泄露
alter table public.quant_stock_history enable row level security;
