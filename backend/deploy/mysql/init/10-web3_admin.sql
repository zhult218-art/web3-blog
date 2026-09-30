USE web3_admin;

CREATE TABLE IF NOT EXISTS `admin_dashboard_stat` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `stat_key` VARCHAR(64) NOT NULL,
  `stat_value` VARCHAR(128) DEFAULT NULL,
  `category` VARCHAR(64) DEFAULT NULL,
  `stat_date` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2026-09-17 修订：表名由 operation_log 改为 admin_operation_log，
-- 与 admin-service OperationLog 实体上的 @TableName("admin_operation_log") 一致，
-- 修复"实体表名声明 admin_operation_log，SQL 建表 operation_log"导致的表不存在的运行时错误。
CREATE TABLE IF NOT EXISTS `admin_operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `operator_id` BIGINT DEFAULT NULL,
  `operator_name` VARCHAR(64) DEFAULT NULL,
  `module` VARCHAR(64) DEFAULT NULL,
  `action` VARCHAR(64) DEFAULT NULL,
  `target_id` VARCHAR(64) DEFAULT NULL,
  `target_name` VARCHAR(128) DEFAULT NULL,
  `detail` TEXT,
  `ip_address` VARCHAR(64) DEFAULT NULL,
  `user_agent` VARCHAR(512) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2026-09-17 新增：visit_log 表对齐 admin-service VisitLog 实体。
-- 历史上 SQL 缺 visit_log 表，导致访问日志接口直接抛 SQL 异常。
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
