-- ============================================================
-- 社区群聊增强：群公告 / 群相册 / 照片 / 群通知
-- 在 MySQL web3_forum 库执行（可重复执行）
-- ============================================================
USE web3_forum;

-- 群公告
CREATE TABLE IF NOT EXISTS `chat_announcement` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `room_id` BIGINT NOT NULL,
  `title` VARCHAR(200) NOT NULL,
  `content` TEXT,
  `user_id` BIGINT DEFAULT NULL,
  `nickname` VARCHAR(100) DEFAULT NULL,
  `pinned` TINYINT DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_room` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 群相册（文件夹）
CREATE TABLE IF NOT EXISTS `chat_album` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `room_id` BIGINT NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `cover_url` VARCHAR(512) DEFAULT NULL,
  `user_id` BIGINT DEFAULT NULL,
  `nickname` VARCHAR(100) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_room` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 照片（归属于某相册文件夹）
CREATE TABLE IF NOT EXISTS `chat_photo` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `room_id` BIGINT NOT NULL,
  `album_id` BIGINT NOT NULL,
  `url` VARCHAR(512) NOT NULL,
  `user_id` BIGINT DEFAULT NULL,
  `nickname` VARCHAR(100) DEFAULT NULL,
  `width` INT DEFAULT NULL,
  `height` INT DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_album` (`album_id`),
  KEY `idx_room` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 群通知
CREATE TABLE IF NOT EXISTS `chat_notification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `room_id` BIGINT NOT NULL,
  `type` VARCHAR(32) DEFAULT 'info',
  `title` VARCHAR(200) NOT NULL,
  `content` VARCHAR(1000) DEFAULT NULL,
  `user_id` BIGINT DEFAULT NULL,
  `nickname` VARCHAR(100) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_room` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 初始数据：默认相册 + 欢迎公告/通知（幂等）
INSERT INTO `chat_album` (`room_id`, `name`, `user_id`, `nickname`)
SELECT 1, '随手拍', 1, '极光星途'
WHERE NOT EXISTS (SELECT 1 FROM `chat_album` WHERE `room_id` = 1 AND `name` = '随手拍');

INSERT INTO `chat_announcement` (`room_id`, `title`, `content`, `user_id`, `nickname`, `pinned`)
SELECT 1, '欢迎来到极光广场', '这里是全站冒险者的公共客厅 ✨
友善交流、分享技术与生活；发图片点输入框左侧图片按钮，群相册可按文件夹归档精彩瞬间。', 1, '极光星途', 1
WHERE NOT EXISTS (SELECT 1 FROM `chat_announcement` WHERE `room_id` = 1 AND `title` = '欢迎来到极光广场');

INSERT INTO `chat_notification` (`room_id`, `type`, `title`, `content`, `user_id`, `nickname`)
SELECT 1, 'welcome', '群聊已升级', '现已支持图片表情、群公告、群相册与聊天记录搜索。', 1, '极光星途'
WHERE NOT EXISTS (SELECT 1 FROM `chat_notification` WHERE `room_id` = 1 AND `title` = '群聊已升级');
