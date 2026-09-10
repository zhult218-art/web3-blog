-- ============================================================
-- 公会系统迁移：user 表加 user_class + guild 表 + guild_member 表
-- ============================================================

-- 用户职业字段
ALTER TABLE `user`
  ADD COLUMN IF NOT EXISTS `user_class` VARCHAR(32) DEFAULT NULL COMMENT '异世界职业（WARRIOR/MAGE/HEALER/ASSASSIN/RANGER/PALADIN/NECROMANCER/BERSERKER）' AFTER `google_id`;

-- 公会表
CREATE TABLE IF NOT EXISTS `guild` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL COMMENT '公会名称',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '公会描述',
  `emblem` VARCHAR(512) DEFAULT NULL COMMENT '公会徽章（会标图片 URL）',
  `master_user_id` BIGINT NOT NULL COMMENT '会长用户 ID',
  `max_members` INT DEFAULT 50 COMMENT '最大成员数',
  `status` INT DEFAULT 1 COMMENT '状态：1 正常 / 0 关闭',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_master` (`master_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 公会成员表
CREATE TABLE IF NOT EXISTS `guild_member` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `guild_id` BIGINT NOT NULL COMMENT '公会 ID',
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
  `message` VARCHAR(500) DEFAULT NULL COMMENT '申请留言',
  `card_number` VARCHAR(20) DEFAULT NULL COMMENT '冒险者卡号',
  `approved_by` BIGINT DEFAULT NULL COMMENT '审批人用户 ID',
  `approved_at` DATETIME DEFAULT NULL COMMENT '审批时间',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_guild_user` (`guild_id`, `user_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 默认公会：异世界冒险家公会（会长为 admin）
INSERT IGNORE INTO `guild` (`id`, `name`, `description`, `emblem`, `master_user_id`, `status`)
VALUES (1, '异世界冒险家公会', '穿越时空的冒险者们聚集之地。加入公会，接受任务，开启你的异世界传说！', '/images/guild/emblem.png', 1, 1);
