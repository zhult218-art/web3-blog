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

CREATE TABLE IF NOT EXISTS `operation_log` (
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
