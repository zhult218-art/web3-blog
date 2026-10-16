-- 为 resource 表添加下载计数字段
-- 2026-10 修订：原 `ADD COLUMN IF NOT EXISTS` 为 MariaDB 专属语法，MySQL 8 报语法错误；
-- 改为 information_schema 判断 + 动态 PREPARE/EXECUTE（MySQL 8 兼容、可重复执行）。
USE web3_resource;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME = 'resource'
                     AND COLUMN_NAME = 'download_count');
SET @sql = IF(@col_exists = 0,
              'ALTER TABLE `resource` ADD COLUMN `download_count` BIGINT DEFAULT 0 COMMENT ''下载次数'' AFTER `download_url`',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
