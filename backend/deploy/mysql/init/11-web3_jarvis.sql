-- ============================================================
-- web3_jarvis：星途语音助手 建表 + 种子数据
-- 对应服务：jarvis-service（9001）
-- ============================================================
USE web3_jarvis;

CREATE TABLE IF NOT EXISTS `voice_command` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `command_key` VARCHAR(128) NOT NULL COMMENT '命令标识',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '命令描述',
  `action_type` VARCHAR(32) DEFAULT 'TTS' COMMENT '动作类型: NAVIGATE/LISTEN/BACKEND',
  `target_service` VARCHAR(128) DEFAULT NULL COMMENT '目标服务',
  `target_url` VARCHAR(255) DEFAULT NULL COMMENT '目标前端路由',
  `params_template` VARCHAR(512) DEFAULT NULL COMMENT '参数模板(JSON)',
  `voice_trigger` VARCHAR(128) DEFAULT NULL COMMENT '语音触发词(子串匹配)',
  `tts_response` VARCHAR(512) DEFAULT NULL COMMENT 'TTS 回复文案',
  `sort_order` INT DEFAULT 0,
  `enabled` INT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_trigger` (`voice_trigger`),
  KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='语音命令表';

CREATE TABLE IF NOT EXISTS `voice_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID(UUID无横线)',
  `user_id` BIGINT DEFAULT NULL,
  `status` VARCHAR(16) DEFAULT 'active',
  `last_command` VARCHAR(512) DEFAULT NULL,
  `last_response` VARCHAR(512) DEFAULT NULL,
  `context` TEXT,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session` (`session_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='语音会话表';

-- ============================================================
-- 种子命令（触发词按长→短排序，保证 contains 匹配优先命中长指令）
-- ============================================================
INSERT INTO `voice_command`
  (`command_key`, `description`, `action_type`, `target_service`, `target_url`, `params_template`, `voice_trigger`, `tts_response`, `sort_order`, `enabled`)
VALUES
  ('open_home', '打开首页', 'NAVIGATE', 'frontend', '/', NULL, '回到首页', '好的，已为你打开首页', 1, 1),
  ('open_home2', '打开首页(短)', 'NAVIGATE', 'frontend', '/', NULL, '去首页', '好的，正在前往首页', 2, 1),
  ('open_community', '打开社区', 'NAVIGATE', 'frontend', '/community', NULL, '去社区', '好的，已为你打开社区', 3, 1),
  ('open_shop', '打开商城', 'NAVIGATE', 'frontend', '/shop', NULL, '商城', '好的，已为你打开商城', 4, 1),
  ('open_quant', '打开量化', 'NAVIGATE', 'frontend', '/quant', NULL, '量化', '好的，已为你打开量化行情中心', 5, 1),
  ('open_tools', '打开工具', 'NAVIGATE', 'frontend', '/tools', NULL, '工具', '好的，已为你打开在线工具', 6, 1),
  ('open_software', '打开软件', 'NAVIGATE', 'frontend', '/software', NULL, '软件', '好的，已为你打开软件库', 7, 1),
  ('open_resources', '打开资源', 'NAVIGATE', 'frontend', '/resources', NULL, '资源', '好的，已为你打开资源库', 8, 1),
  ('open_media', '打开媒体', 'NAVIGATE', 'frontend', '/media', NULL, '媒体', '好的，已为你打开多媒体', 9, 1),
  ('open_music', '打开音乐馆', 'NAVIGATE', 'frontend', '/music', NULL, '音乐', '好的，已为你打开音乐馆', 10, 1),
  ('open_album', '打开相册', 'NAVIGATE', 'frontend', '/album', NULL, '相册', '好的，已为你打开相册集', 11, 1),
  ('open_link', '打开友链', 'NAVIGATE', 'frontend', '/link', NULL, '友链', '好的，已为你打开友人帐', 12, 1),
  ('open_profile', '打开个人中心', 'NAVIGATE', 'frontend', '/profile', NULL, '个人中心', '好的，已为你打开个人中心', 13, 1),
  ('say_time', '报时', 'TTS', 'local', NULL, NULL, '现在几点', '让我看看时间，现在是晚上好时光', 14, 1),
  ('say_time2', '报时(问候)', 'TTS', 'local', NULL, NULL, '你好', '你好呀，我是星途，随时为你服务', 15, 1),
  ('say_time3', '报时(自我介绍)', 'TTS', 'local', NULL, NULL, '你是谁', '我是星途，你的 Web3 门户智能助手', 16, 1);