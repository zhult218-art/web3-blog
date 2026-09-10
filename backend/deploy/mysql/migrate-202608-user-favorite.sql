-- ========================================
-- Migration: Add user_favorite table + product covers
-- Date: 2026-08
-- ========================================

-- 1. Create user_favorite table in web3_blog
USE web3_blog;

CREATE TABLE IF NOT EXISTS `user_favorite` (
  `id` BIGINT NOT NULL,
  `article_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_user` (`article_id`, `user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Update product covers in web3_shop
USE web3_shop;

UPDATE `product` SET `cover` = 'https://picsum.photos/seed/web3course/400/300' WHERE `name` LIKE '%Web3%' AND `cover` IS NULL;
UPDATE `product` SET `cover` = 'https://picsum.photos/seed/solidity/400/300' WHERE `name` LIKE '%Solidity%' AND `cover` IS NULL;
UPDATE `product` SET `cover` = 'https://picsum.photos/seed/dex/400/300' WHERE `name` LIKE '%DEX%' AND `cover` IS NULL;
UPDATE `product` SET `cover` = 'https://picsum.photos/seed/nftart/400/300' WHERE `name` LIKE '%NFT%' AND `cover` IS NULL;
UPDATE `product` SET `cover` = 'https://picsum.photos/seed/wallet/400/300' WHERE `name` LIKE '%钱包%' AND `cover` IS NULL;
UPDATE `product` SET `cover` = 'https://picsum.photos/seed/dashboard/400/300' WHERE `name` LIKE '%仪表板%' AND `cover` IS NULL;
UPDATE `product` SET `cover` = 'https://picsum.photos/seed/dao/400/300' WHERE `name` LIKE '%DAO%' AND `cover` IS NULL;
UPDATE `product` SET `cover` = 'https://picsum.photos/seed/uilib/400/300' WHERE `name` LIKE '%组件库%' AND `cover` IS NULL;
UPDATE `product` SET `cover` = 'https://picsum.photos/seed/ide/400/300' WHERE `name` LIKE '%IDE%' AND `cover` IS NULL;
UPDATE `product` SET `cover` = 'https://picsum.photos/seed/identity/400/300' WHERE `name` LIKE '%身份%' AND `cover` IS NULL;
UPDATE `product` SET `cover` = 'https://picsum.photos/seed/fullstack/400/300' WHERE `name` LIKE '%全栈%' AND `cover` IS NULL;
UPDATE `product` SET `cover` = 'https://picsum.photos/seed/placeholder/400/300' WHERE `cover` IS NULL;
