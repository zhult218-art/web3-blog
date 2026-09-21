-- ============================================================
-- Supabase 定时任务：每日自动刷新与维护历史数据
-- 执行方式：Supabase Dashboard → SQL Editor → 粘贴执行一次
-- 依赖：pg_cron 扩展（Supabase 内置，无需额外安装）
--
-- 说明：
--   1) 本脚本在数据库层做「每日维护」——清理过期数据、更新统计信息、
--      记录刷新日志到 quant_sync_log。
--   2) 实际行情/板块/个股 K 线数据的抓取由 quant-py-service 的
--      _schedule_daily_sync 线程完成（交易日 15:30 后自动触发）。
--      若需从数据库侧强制触发后端刷新，可在 pg_cron 中调用
--      http 扩展请求后端 /api/quant/market/sync 端点。
-- ============================================================

-- 启用 pg_cron 扩展
create extension if not exists pg_cron;

-- ============================================================
-- 1) 每日维护函数：清理 + ANALYZE + 写日志
-- ============================================================
create or replace function public.quant_daily_maintenance()
returns void
language plpgsql
as $$
declare
  v_cleaned int := 0;
begin
  -- 1.1) 清理 quant_stock_history 中超过 2 年的非 K 线历史数据
  --      (kind 为 finance/holders/margin/fundflow/dividend/blocktrade)
  delete from public.quant_stock_history
  where kind in ('finance', 'holders', 'margin', 'fundflow', 'dividend', 'blocktrade')
    and date_key < to_char(current_date - interval '2 years', 'YYYY-MM-DD');

  get diagnostics v_cleaned = row_count;

  -- 1.2) 清理 quant_daily_snapshot 中超过 365 天的快照（保留 1 年）
  --      表存在才执行，避免首次部署报错
  begin
    execute 'delete from public.quant_daily_snapshot where trade_date < $1'
      using current_date - 365;
  exception when undefined_table then
    null;
  end;

  -- 1.3) 更新统计信息（优化查询计划）
  analyze public.quant_stock_history;
  analyze public.quant_daily_count;
  analyze public.quant_sync_log;

  -- 1.4) 记录维护日志
  insert into public.quant_sync_log (kind, trade_date, status, detail, created_at)
  values (
    'cron-maintenance',
    current_date::text,
    'ok',
    'cleaned=' || v_cleaned || ' rows; analyzed; at ' || now()::text,
    now()
  );
end;
$$;

-- ============================================================
-- 2) 调度：每周一至周五 16:00 (UTC+8 收盘后) 执行维护
--    Supabase 服务器时区为 UTC，16:00 北京时间 = 08:00 UTC
-- ============================================================
select cron.schedule(
  'quant-daily-maintenance',
  '0 8 * * 1-5',
  $$select public.quant_daily_maintenance()$$
);

-- ============================================================
-- 3) （可选）如果 quant-py-service 部署在公网可访问地址，
--    启用 http 扩展并在收盘后调用后端 /sync 端点强制刷新。
--    本地部署（localhost）时不需要启用此段。
-- ============================================================
-- create extension if not exists http;
-- select cron.schedule(
--   'quant-daily-sync-trigger',
--   '30 8 * * 1-5',
--   $$select status from http_post(
--     'http://YOUR_BACKEND_HOST:9006/api/quant/market/sync?force=true',
--     '{}',
--     'application/json'
--   )$$
-- );

-- ============================================================
-- 验证：查看已注册的定时任务
-- ============================================================
-- select jobid, schedule, command, active from cron.job order by jobid;
