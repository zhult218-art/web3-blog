-- ============================================================
-- Migration: Add missing indexes for high-frequency query columns
-- Covers: web3_user, web3_blog, web3_forum, web3_shop
-- Idempotent: uses INFORMATION_SCHEMA checks so re-runs are safe
-- 2026-10 修订：原实现使用 DELIMITER + CREATE PROCEDURE IF NOT EXISTS
-- （MariaDB / mysql 命令行客户端专属写法），MySQL 8 直连执行会报语法错误。
-- 现改为 information_schema 判断 + 动态 PREPARE/EXECUTE（MySQL 8 兼容，可重复执行）。
-- 所有语句使用全限定库名，无需 USE。
-- ============================================================

-- --------------------------------------------------------
-- web3_user.user
-- --------------------------------------------------------

-- email: login / lookup
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = 'web3_user' AND TABLE_NAME = 'user' AND INDEX_NAME = 'idx_user_email');
SET @sql = IF(@idx_exists = 0, 'CREATE INDEX idx_user_email ON web3_user.`user`(email)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- phone: SMS-login / lookup
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = 'web3_user' AND TABLE_NAME = 'user' AND INDEX_NAME = 'idx_user_phone');
SET @sql = IF(@idx_exists = 0, 'CREATE INDEX idx_user_phone ON web3_user.`user`(phone)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- google_id: OAuth lookup
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = 'web3_user' AND TABLE_NAME = 'user' AND INDEX_NAME = 'idx_user_google_id');
SET @sql = IF(@idx_exists = 0, 'CREATE INDEX idx_user_google_id ON web3_user.`user`(google_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- --------------------------------------------------------
-- web3_blog.article
-- --------------------------------------------------------

-- author_id: list articles by author
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = 'web3_blog' AND TABLE_NAME = 'article' AND INDEX_NAME = 'idx_article_author_id');
SET @sql = IF(@idx_exists = 0, 'CREATE INDEX idx_article_author_id ON web3_blog.article(author_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- status: filter by publish status
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = 'web3_blog' AND TABLE_NAME = 'article' AND INDEX_NAME = 'idx_article_status');
SET @sql = IF(@idx_exists = 0, 'CREATE INDEX idx_article_status ON web3_blog.article(status)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- --------------------------------------------------------
-- web3_forum.post
-- --------------------------------------------------------

-- author_id: list posts by author
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = 'web3_forum' AND TABLE_NAME = 'post' AND INDEX_NAME = 'idx_post_author_id');
SET @sql = IF(@idx_exists = 0, 'CREATE INDEX idx_post_author_id ON web3_forum.post(author_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- category: filter posts by category
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = 'web3_forum' AND TABLE_NAME = 'post' AND INDEX_NAME = 'idx_post_category');
SET @sql = IF(@idx_exists = 0, 'CREATE INDEX idx_post_category ON web3_forum.post(category)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- status: filter posts by status
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = 'web3_forum' AND TABLE_NAME = 'post' AND INDEX_NAME = 'idx_post_status');
SET @sql = IF(@idx_exists = 0, 'CREATE INDEX idx_post_status ON web3_forum.post(status)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- --------------------------------------------------------
-- web3_shop.order
-- --------------------------------------------------------

-- status: filter orders by status (PENDING / PAID / SHIPPED ...)
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = 'web3_shop' AND TABLE_NAME = 'order' AND INDEX_NAME = 'idx_order_status');
SET @sql = IF(@idx_exists = 0, 'CREATE INDEX idx_order_status ON web3_shop.`order`(status)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- product_id: lookup orders for a product
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = 'web3_shop' AND TABLE_NAME = 'order' AND INDEX_NAME = 'idx_order_product_id');
SET @sql = IF(@idx_exists = 0, 'CREATE INDEX idx_order_product_id ON web3_shop.`order`(product_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- --------------------------------------------------------
-- web3_shop.product
-- --------------------------------------------------------

-- status: filter products by ON / OFF
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = 'web3_shop' AND TABLE_NAME = 'product' AND INDEX_NAME = 'idx_product_status');
SET @sql = IF(@idx_exists = 0, 'CREATE INDEX idx_product_status ON web3_shop.product(status)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
