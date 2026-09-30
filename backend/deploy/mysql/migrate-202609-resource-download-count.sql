-- 为 resource 表添加下载计数字段
USE web3_resource;

ALTER TABLE `resource` ADD COLUMN IF NOT EXISTS `download_count` BIGINT DEFAULT 0 COMMENT '下载次数' AFTER `download_url`;
