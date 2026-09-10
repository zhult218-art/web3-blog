USE web3_tool;

CREATE TABLE IF NOT EXISTS `script` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(200) NOT NULL,
  `description` TEXT,
  `content` LONGTEXT,
  `category` VARCHAR(64) DEFAULT NULL,
  `language` VARCHAR(32) DEFAULT 'python',
  `version` VARCHAR(32) DEFAULT '1.0.0',
  `download_count` BIGINT DEFAULT 0,
  `tags` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `site_share` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `category` VARCHAR(64) DEFAULT NULL,
  `name` VARCHAR(200) NOT NULL,
  `url` VARCHAR(500) NOT NULL,
  `description` VARCHAR(500) DEFAULT NULL,
  `icon` VARCHAR(50) DEFAULT NULL,
  `sort` INT DEFAULT 0,
  `status` INT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
