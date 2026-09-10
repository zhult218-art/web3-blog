USE web3_software;

CREATE TABLE IF NOT EXISTS `software` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(200) NOT NULL,
  `description` TEXT,
  `download_url` VARCHAR(512) DEFAULT NULL,
  `category` VARCHAR(64) DEFAULT NULL,
  `version` VARCHAR(32) DEFAULT '1.0.0',
  `size` BIGINT DEFAULT 0,
  `os` VARCHAR(64) DEFAULT NULL,
  `icon` VARCHAR(512) DEFAULT NULL,
  `download_count` BIGINT DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

USE web3_resource;

CREATE TABLE IF NOT EXISTS `resource` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(200) NOT NULL,
  `description` TEXT,
  `category` VARCHAR(64) DEFAULT NULL,
  `filename` VARCHAR(255) NOT NULL,
  `original_filename` VARCHAR(255) DEFAULT NULL,
  `content_type` VARCHAR(128) DEFAULT NULL,
  `size` BIGINT DEFAULT 0,
  `download_url` VARCHAR(512) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
