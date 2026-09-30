USE web3_admin;

-- ============================================================
-- admin 日志表字段对齐迁移（2026-09-17）
-- 背景：
--   1. SQL 建表 operation_log，但 admin-service OperationLog 实体的 @TableName 是 admin_operation_log；
--   2. SQL 完全没有 visit_log 表，但 admin-service 有 VisitLog 实体（@TableName("visit_log")）。
-- 此迁移用于已部署的旧库（rename + add visit_log）。
-- 新部署直接跑 10-web3_admin.sql 即可（已修订为最新结构）。
-- ============================================================

-- operation_log → admin_operation_log（幂等：目标表已存在则跳过）
SET @tbl_exists = (SELECT COUNT(*) FROM information_schema.TABLES
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME = 'admin_operation_log');
SET @sql = IF(@tbl_exists = 0,
              'RENAME TABLE `operation_log` TO `admin_operation_log`',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- visit_log 表（幂等）
CREATE TABLE IF NOT EXISTS `visit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `ip` VARCHAR(64) DEFAULT NULL,
  `country` VARCHAR(64) DEFAULT NULL,
  `region` VARCHAR(64) DEFAULT NULL,
  `city` VARCHAR(64) DEFAULT NULL,
  `isp` VARCHAR(128) DEFAULT NULL,
  `latitude` DECIMAL(10, 7) DEFAULT NULL,
  `longitude` DECIMAL(10, 7) DEFAULT NULL,
  `user_agent` VARCHAR(512) DEFAULT NULL,
  `page_path` VARCHAR(512) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_ip` (`ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
