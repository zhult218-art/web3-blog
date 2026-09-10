USE web3_user;

CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL,
  `nickname` VARCHAR(64) DEFAULT NULL,
  `email` VARCHAR(128) DEFAULT NULL,
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号（验证码登录）',
  `google_id` VARCHAR(64) DEFAULT NULL COMMENT 'Google OAuth 唯一ID',
  `password` VARCHAR(255) NOT NULL,
  `avatar` VARCHAR(512) DEFAULT NULL,
  `role` VARCHAR(32) DEFAULT 'USER',
  `permissions` VARCHAR(512) DEFAULT NULL COMMENT '逗号分隔的服务权限码；NULL/空为默认权限',
  `status` INT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 已有部署环境执行以下迁移（新环境由上方建表语句直接包含）：
-- ALTER TABLE `user`
--   ADD COLUMN IF NOT EXISTS `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号（验证码登录）' AFTER `email`,
--   ADD COLUMN IF NOT EXISTS `google_id` VARCHAR(64) DEFAULT NULL COMMENT 'Google OAuth 唯一ID' AFTER `phone`;

-- admin / admin123
INSERT INTO `user` (`username`, `nickname`, `email`, `password`, `role`, `status`) VALUES
('admin', 'Admin', 'admin@web3.local', '$2b$10$kF9ZaRYUyOs4dqfl46PzOOJsQ0JBx12CbA3Xj3SPm5hSSHGy6EZwG', 'ADMIN', 1);
