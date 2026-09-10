USE web3_forum;

-- 评论楼中楼：为 comment 表新增回复关系字段
ALTER TABLE `comment`
  ADD COLUMN `parent_id` BIGINT DEFAULT NULL COMMENT '父评论 ID，NULL 表示顶级评论' AFTER `content`,
  ADD COLUMN `reply_to_name` VARCHAR(64) DEFAULT NULL COMMENT '被回复人昵称' AFTER `parent_id`,
  ADD KEY `idx_parent` (`parent_id`);

-- 将历史评论显式标记为顶级评论（parent_id NULL 已满足，此列仅作说明）