-- ============================================================
-- 音乐模块增强：播放统计 / 用户收藏 / 播放历史
-- 在 MySQL web3_media 库执行（幂等，可重复执行）
-- ============================================================
USE web3_media;

-- music 表补加 play_count 列（幂等：列已存在则跳过）
SET @col_exists = (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = 'web3_media' AND TABLE_NAME = 'music' AND COLUMN_NAME = 'play_count'
);
SET @ddl = IF(@col_exists = 0,
  'ALTER TABLE `music` ADD COLUMN `play_count` BIGINT DEFAULT 0 AFTER `tags`',
  'SELECT 1');
PREPARE _stmt FROM @ddl; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

-- 用户收藏关系表（user_id, music_id 联合主键，music_id 加索引便于反查热榜）
CREATE TABLE IF NOT EXISTS `user_music_like` (
  `user_id` BIGINT NOT NULL,
  `music_id` BIGINT NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`, `music_id`),
  KEY `idx_music` (`music_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 用户播放历史表（自增主键 + 用户+时间联合索引，便于按时间倒序分页）
CREATE TABLE IF NOT EXISTS `user_music_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `music_id` BIGINT NOT NULL,
  `played_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_played` (`user_id`, `played_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
