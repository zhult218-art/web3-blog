USE web3_forum;

CREATE TABLE IF NOT EXISTS `post` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(200) NOT NULL,
  `content` LONGTEXT,
  `category` VARCHAR(64) DEFAULT NULL,
  `status` VARCHAR(32) DEFAULT 'published',
  `author_id` BIGINT DEFAULT NULL,
  `author_name` VARCHAR(64) DEFAULT NULL,
  `like_count` INT DEFAULT 0,
  `reply_count` INT DEFAULT 0,
  `view_count` BIGINT DEFAULT 0,
  `is_pinned` INT DEFAULT 0,
  `media_url` VARCHAR(512) DEFAULT NULL,
  `media_type` VARCHAR(32) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `target_id` BIGINT NOT NULL,
  `target_type` VARCHAR(32) NOT NULL,
  `content` TEXT NOT NULL,
  `author_id` BIGINT DEFAULT NULL,
  `author_name` VARCHAR(64) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `like_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `target_id` BIGINT NOT NULL,
  `target_type` VARCHAR(32) NOT NULL,
  `user_id` BIGINT NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_like` (`target_type`, `target_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `post` (`title`, `content`, `category`, `author_id`, `status`, `is_pinned`) VALUES
('\u6b22\u8fce\u6765\u5230\u793e\u533a\u5e7f\u573a', '\u5206\u4eab\u4f60\u7684 Web3 \u601d\u60f3\u548c\u9879\u76ee\uff0c\u4e0e\u5fd7\u540c\u9053\u5408\u4f5c\u3002', 'general', 1, 'published', 1),
('\u5982\u4f55\u9009\u62e9\u5408\u9002\u7684\u533a\u5757\u94fe\u7f51\u7edc\uff1f', '\u4ee5\u592a\u574a\u3001Polygon\u3001BSC\u3001Arbitrum\u3001Optimism\u3001Solana\u2026\u9762\u5bf9\u8fd9\u4e48\u591a\u7f51\u7edc\uff0c\u65b0\u624b\u8be5\u5982\u4f55\u9009\u62e9\uff1f\n\n\u6211\u7684\u5efa\u8bae\uff1a\n1. \u5148\u770b\u751f\u6001\u4e30\u5bcc\u5ea6\u548c\u5f00\u53d1\u8005\u4f53\u9a8c\n2. \u5173\u6ce8 gas \u8d39\u7528\u548c\u4ea4\u6613\u901f\u5ea6\n3. \u8003\u8651\u5b89\u5168\u6027\u548c\u53bb\u4e2d\u5fc3\u5316\u7a0b\u5ea6\n\n\u5927\u5bb6\u90fd\u5728\u7528\u54ea\u4e2a\u7f51\u7edc\uff1f\u6b22\u8fce\u5206\u4eab\u4f60\u7684\u7ecf\u9a8c\uff01', 'discussion', 1, 'published', 1),
('Solidity \u5f00\u53d1\u5de5\u5177\u63a8\u8350', '\u6211\u5e0c\u671b\u6574\u7406\u4e00\u4e0b\u76ee\u524d\u6d41\u884c\u7684 Solidity \u5f00\u53d1\u5de5\u5177\uff1a\n\n**\u7f16\u8f91\u5668\uff1a**\n- VS Code + Solidity \u63d2\u4ef6\n- Remix IDE\n\n**\u6784\u5efa\u5de5\u5177\uff1a**\n- Hardhat\n- Foundry\n- Brownie\n\n**\u6d4b\u8bd5\u5de5\u5177\uff1a**\n- Waffle\n- Chai\n- Foundry Test\n\n**\u90e8\u7f72\u5de5\u5177\uff1a**\n- hardhat-deploy\n- OpenZeppelin Upgrades\n\n\u5927\u5bb6\u5e38\u7528\u54ea\u4e9b\uff1f\u6b22\u8fce\u8865\u5145\uff01', 'discussion', 1, 'published', 0),
('\u5173\u4e8e gas \u4f18\u5316\u7684\u4e00\u4e9b\u5fc3\u5f97', 'gas \u8d39\u7528\u662f\u533a\u5757\u94fe\u5f00\u53d1\u4e2d\u5fc5\u987b\u5173\u6ce8\u7684\u95ee\u9898\u3002\u4ee5\u4e0b\u662f\u6211\u79ef\u7d2f\u7684\u4e00\u4e9b\u4f18\u5316\u6280\u5de7\uff1a\n\n1. **\u7528 mapping \u4ee3\u66ff\u6570\u7ec4**\uff1amapping \u8bfb\u5199\u66f4\u5feb\uff0cgas \u66f4\u4f4e\n2. **\u7528 calldata \u4ee3\u66ff memory**\uff1a\u53ea\u8bfb\u53c2\u6570\u7528 calldata\n3. **\u77ed\u8def\u5f84\u8fd0\u7b97**\uff1a\u5148\u8ba1\u7b97\u53ef\u80fd\u4e3a 0 \u7684\u60c5\u51b5\n4. **\u4f7f\u7528 unchecked**\uff1a\u786e\u8ba4\u4e0d\u4f1a\u6ea2\u51fa\u65f6\u8df3\u8fc7\u68c0\u67e5\n5. **\u6253\u5305\u53d8\u91cf**\uff1a\u591a\u4e2a\u5c0f\u53d8\u91cf\u6253\u5305\u6210 struct\n\n\u5927\u5bb6\u6709\u4ec0\u4e48\u5176\u4ed6\u4f18\u5316\u6280\u5de7\uff1f', 'discussion', 1, 'published', 0),
('NFT \u9879\u76ee\u5206\u4eab\uff1a\u6211\u505a\u7684\u4e00\u4e2a\u94fe\u4e0a\u827a\u672f\u5e73\u53f0', '\u6211\u6700\u8fd1\u505a\u4e86\u4e00\u4e2a\u94fe\u4e0a\u827a\u672f NFT \u5e73\u53f0\uff0c\u5206\u4eab\u4e00\u4e0b\u6280\u672f\u6808\u548c\u7ecf\u9a8c\uff1a\n\n**\u6280\u672f\u6808\uff1a**\n- \u524d\u7aef\uff1aNext.js + Tailwind CSS\n- \u5408\u7ea6\uff1aSolidity + OpenZeppelin\n- \u5b58\u50a8\uff1aIPFS + Arweave\n- \u7d22\u5f15\uff1aThe Graph\n\n**\u6838\u5fc3\u529f\u80fd\uff1a**\n- \u521b\u4f5c\u8005\u94f8\u9020 NFT\n- \u62cd\u5356\u884c\u60c5\u5e02\u573a\n- \u6536\u85cf\u529f\u80fd\n- \u521b\u4f5c\u8005\u5206\u6210\n\n\u6b22\u8fce\u63d0\u51fa\u5efa\u8bae\uff01', 'showcase', 1, 'published', 0),
('DAO \u6cbb\u7406\u5b9e\u6218\u7ecf\u9a8c\u5206\u4eab', '\u6211\u5728\u4e00\u4e2a DAO \u7ec4\u7ec7\u4e2d\u53c2\u4e0e\u4e86\u534a\u5e74\uff0c\u5206\u4eab\u4e00\u4e9b\u5b9e\u6218\u7ecf\u9a8c\uff1a\n\n**\u6cbb\u7406\u673a\u5236\uff1a**\n- \u63d0\u6848\u6d41\u7a0b\uff1a\u8bae\u8bba\u671f \u2192 \u6295\u7968\u671f \u2192 \u6267\u884c\u671f\n- \u6295\u7968\u6743\u91cd\uff1a\u6309\u6301\u5e01\u6570\u91cf\u548c\u65f6\u957f\n- \u59d4\u5458\u4f1a\u5236\uff1a\u6267\u884c\u59d4\u5458\u4f1a\u8d1f\u8d23\u65e5\u5e38\u51b3\u7b56\n\n**\u5b9e\u8df5\u7ecf\u9a8c\uff1a**\n1. \u63d0\u6848\u8d28\u91cf\u6bd4\u6570\u91cf\u91cd\u8981\n2. \u516c\u5f00\u8ba8\u8bba\u80fd\u63d0\u9ad8\u51b3\u7b56\u8d28\u91cf\n3. \u5c0f\u989d\u8bd5\u70b9\u518d\u5927\u89c4\u6a21\u63a8\u5e7f\n4. \u53c2\u4e0e\u6fc0\u52b1\u5f88\u91cd\u8981\n\n\u5927\u5bb6\u6709\u53c2\u52a0 DAO \u7684\u7ecf\u9a8c\u5417\uff1f', 'discussion', 1, 'published', 0);
