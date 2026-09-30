USE web3_quant;

-- ============================================================
-- 量化策略归属修复（2026-09-17）
-- 背景：strategy 表历史上没有 user_id 字段，导致 update/delete/run 接口
-- 只校验登录态、不校验策略归属，任意登录用户可改/删/跑他人策略。
-- 此迁移为 strategy 表追加 user_id 字段并补索引；现有数据 user_id 设为 NULL
-- （表示"公共/无主"策略，仍可被任何人读取，但写操作必须由 ADMIN 或归属用户执行）。
-- ============================================================

ALTER TABLE `strategy`
  ADD COLUMN `user_id` BIGINT DEFAULT NULL COMMENT '策略归属用户 ID（NULL 表示公共策略）' AFTER `id`,
  ADD KEY `idx_user` (`user_id`);
