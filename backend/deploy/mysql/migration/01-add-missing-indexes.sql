-- ============================================================
-- Migration: Add missing indexes for high-frequency query columns
-- Covers: web3_user, web3_blog, web3_forum, web3_shop
-- Idempotent: uses INFORMATION_SCHEMA checks so re-runs are safe
-- ============================================================

DELIMITER //

CREATE PROCEDURE IF NOT EXISTS _add_missing_indexes()
BEGIN

    -- --------------------------------------------------------
    -- web3_user.user
    -- --------------------------------------------------------

    -- email: login / lookup
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = 'web3_user' AND TABLE_NAME = 'user' AND INDEX_NAME = 'idx_user_email'
    ) THEN
        CREATE INDEX idx_user_email ON web3_user.`user`(email);
    END IF;

    -- phone: SMS-login / lookup
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = 'web3_user' AND TABLE_NAME = 'user' AND INDEX_NAME = 'idx_user_phone'
    ) THEN
        CREATE INDEX idx_user_phone ON web3_user.`user`(phone);
    END IF;

    -- google_id: OAuth lookup
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = 'web3_user' AND TABLE_NAME = 'user' AND INDEX_NAME = 'idx_user_google_id'
    ) THEN
        CREATE INDEX idx_user_google_id ON web3_user.`user`(google_id);
    END IF;

    -- --------------------------------------------------------
    -- web3_blog.article
    -- --------------------------------------------------------

    -- author_id: list articles by author
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = 'web3_blog' AND TABLE_NAME = 'article' AND INDEX_NAME = 'idx_article_author_id'
    ) THEN
        CREATE INDEX idx_article_author_id ON web3_blog.article(author_id);
    END IF;

    -- status: filter by publish status
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = 'web3_blog' AND TABLE_NAME = 'article' AND INDEX_NAME = 'idx_article_status'
    ) THEN
        CREATE INDEX idx_article_status ON web3_blog.article(status);
    END IF;

    -- --------------------------------------------------------
    -- web3_forum.post
    -- --------------------------------------------------------

    -- author_id: list posts by author
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = 'web3_forum' AND TABLE_NAME = 'post' AND INDEX_NAME = 'idx_post_author_id'
    ) THEN
        CREATE INDEX idx_post_author_id ON web3_forum.post(author_id);
    END IF;

    -- category: filter posts by category
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = 'web3_forum' AND TABLE_NAME = 'post' AND INDEX_NAME = 'idx_post_category'
    ) THEN
        CREATE INDEX idx_post_category ON web3_forum.post(category);
    END IF;

    -- status: filter posts by status
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = 'web3_forum' AND TABLE_NAME = 'post' AND INDEX_NAME = 'idx_post_status'
    ) THEN
        CREATE INDEX idx_post_status ON web3_forum.post(status);
    END IF;

    -- --------------------------------------------------------
    -- web3_shop.order
    -- --------------------------------------------------------

    -- status: filter orders by status (PENDING / PAID / SHIPPED ...)
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = 'web3_shop' AND TABLE_NAME = 'order' AND INDEX_NAME = 'idx_order_status'
    ) THEN
        CREATE INDEX idx_order_status ON web3_shop.`order`(status);
    END IF;

    -- product_id: lookup orders for a product
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = 'web3_shop' AND TABLE_NAME = 'order' AND INDEX_NAME = 'idx_order_product_id'
    ) THEN
        CREATE INDEX idx_order_product_id ON web3_shop.`order`(product_id);
    END IF;

    -- --------------------------------------------------------
    -- web3_shop.product
    -- --------------------------------------------------------

    -- status: filter products by ON / OFF
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = 'web3_shop' AND TABLE_NAME = 'product' AND INDEX_NAME = 'idx_product_status'
    ) THEN
        CREATE INDEX idx_product_status ON web3_shop.product(status);
    END IF;

END //

DELIMITER ;

-- Execute the procedure
CALL _add_missing_indexes();

-- Clean up
DROP PROCEDURE IF EXISTS _add_missing_indexes;
