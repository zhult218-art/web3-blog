-- 迁移：order 表增加 pay_channel 支付渠道列（2026-08）
-- 已有部署环境手动执行；新环境由 05-web3_shop.sql 直接建表包含该列
-- 2026-10 修订：原 `ADD COLUMN IF NOT EXISTS` 为 MariaDB 专属语法，MySQL 8 报语法错误；
-- 改为 information_schema 判断 + 动态 PREPARE/EXECUTE（MySQL 8 兼容、可重复执行），并补充 USE。
USE web3_shop;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME = 'order'
                     AND COLUMN_NAME = 'pay_channel');
SET @sql = IF(@col_exists = 0,
              'ALTER TABLE `order` ADD COLUMN `pay_channel` VARCHAR(16) DEFAULT NULL AFTER `paid_at`',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
