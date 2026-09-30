USE web3_forum;

-- 社区内容审核：为 comment 表新增审核状态字段
-- post 表已有 status 字段（默认 published），评论此前无状态概念，统一补齐
-- status：published=已通过（公开可见），pending=待审核，rejected=已驳回
ALTER TABLE `comment`
  ADD COLUMN `status` VARCHAR(32) NOT NULL DEFAULT 'published' COMMENT '审核状态：published/pending/rejected' AFTER `author_name`,
  ADD KEY `idx_status` (`status`);

-- 历史评论与帖子均视为已通过（post.status 建表默认值已是 published，无需回填）
