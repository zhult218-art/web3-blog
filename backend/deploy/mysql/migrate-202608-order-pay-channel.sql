-- 迁移：order 表增加 pay_channel 支付渠道列（2026-08）
-- 已有部署环境手动执行；新环境由 05-web3_shop.sql 直接建表包含该列
ALTER TABLE `order` ADD COLUMN IF NOT EXISTS `pay_channel` VARCHAR(16) DEFAULT NULL AFTER `paid_at`;
