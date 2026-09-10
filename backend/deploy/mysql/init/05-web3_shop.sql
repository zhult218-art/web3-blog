USE web3_shop;

CREATE TABLE IF NOT EXISTS `product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(200) NOT NULL,
  `description` TEXT,
  `price` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `stock` INT DEFAULT 0,
  `cover` VARCHAR(512) DEFAULT NULL,
  `category` VARCHAR(64) DEFAULT NULL,
  `sales` INT DEFAULT 0,
  `status` VARCHAR(16) DEFAULT 'ON',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `status` VARCHAR(16) DEFAULT 'PENDING',
  `quantity` INT DEFAULT 1,
  `order_no` VARCHAR(64) DEFAULT NULL,
  `paid_at` DATETIME DEFAULT NULL,
  `pay_channel` VARCHAR(16) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `product` (`name`, `description`, `price`, `stock`, `category`, `cover`, `sales`) VALUES
('Web3 \u5165\u95e8\u5168\u5957\u88c5', '\u5305\u542b Web3 \u5f00\u53d1\u6240\u9700\u7684\u5de5\u5177\u3001\u6559\u7a0b\u548c\u6a21\u677f\uff0c\u4ece\u96f6\u5f00\u59cb\u5b66\u4e60\u533a\u5757\u94fe\u5f00\u53d1\u3002', 99.00, 100, '\u7535\u5b50\u4ea7\u54c1', NULL, 256),
('Smart Contract \u5408\u7ea6\u6a21\u677f', '\u7ecf\u8fc7\u5ba1\u8ba1\u7684\u667a\u80fd\u5408\u7ea6\u6a21\u677f\uff0c\u652f\u6301 ERC20/ERC721/ERC1155 \u7b49\u5e38\u89c1\u573a\u666f\uff0c\u5f00\u7bb1\u5373\u7528\u3002', 299.00, 50, '\u5f00\u53d1\u5de5\u5177', NULL, 189),
('Node \u8fd0\u7ef4\u5b9e\u6218\u8bfe', '\u4ece\u96f6\u642d\u5efa\u533a\u5757\u94fe\u8282\u70b9\uff0c\u6db5\u76d6\u73af\u5883\u914d\u7f6e\u3001\u76d1\u63a7\u544a\u8b66\u3001\u5b89\u5168\u7ef4\u62a4\u5168\u6d41\u7a0b\u3002', 199.00, 80, '\u6559\u7a0b\u8bfe\u7a0b', NULL, 432),
('DApp \u524d\u7aef\u5f00\u53d1\u6559\u7a0b', '\u4f7f\u7528 React + ethers.js \u6784\u5efa\u53bb\u4e2d\u5fc3\u5316\u5e94\u7528\uff0c\u5305\u542b\u94b1\u5305\u8fde\u63a5\u3001\u5408\u7ea6\u4ea4\u4e92\u3001NFT \u5c55\u793a\u7b49\u5b8c\u6574\u9879\u76ee\u3002', 159.00, 120, '\u6559\u7a0b\u8bfe\u7a0b', NULL, 678),
('DeFi \u7b97\u6cd5\u5b9e\u6218', '\u6df1\u5165\u89e3\u6790 AMM\u3001\u50ac\u5316\u501f\u8d37\u3001\u6c34\u4e0a\u6c89\u7a0b\u7b97\u6cd5\uff0c\u914d\u5408 Python \u5b9e\u73b0\u548c\u56de\u6d4b\u5206\u6790\u3002', 259.00, 60, '\u6559\u7a0b\u8bfe\u7a0b', NULL, 312),
('NFT \u521b\u4f5c\u5de5\u5177\u5305', '\u5305\u542b AI \u751f\u6210\u3001\u56fe\u50cf\u5904\u7406\u3001\u94fe\u4e0a\u90e8\u7f72\u7684\u4e00\u7ad9\u5f0f NFT \u521b\u4f5c\u5de5\u5177\u96c6\u3002', 129.00, 200, '\u6570\u5b57\u827a\u672f', NULL, 89),
('Web3 \u5b89\u5168\u5ba1\u8ba1\u6307\u5357', '\u667a\u80fd\u5408\u7ea6\u5e38\u89c1\u6f0f\u6d1e\u5206\u6790\u3001\u5ba1\u8ba1\u5de5\u5177\u4f7f\u7528\u3001\u5b89\u5168\u6700\u4f73\u5b9e\u8df5\u5b8c\u6574\u6307\u5357\u3002', 89.00, 300, '\u8f6f\u4ef6\u670d\u52a1', NULL, 145),
('Chain Analytics \u6570\u636e\u770b\u677f', '\u5b9e\u65f6\u76d1\u63a7\u591a\u94fe\u6570\u636e\u3001TVL\u3001\u4ea4\u6613\u91cf\u3001\u5de8\u9c8e\u52a8\u5411\uff0c\u652f\u6301\u81ea\u5b9a\u4e49\u544a\u8b66\u548c\u6570\u636e\u5bfc\u51fa\u3002', 399.00, 30, '\u8f6f\u4ef6\u670d\u52a1', NULL, 67),
('Solidity \u8fdb\u9636\u7f16\u7a0b', '\u4ece\u57fa\u7840\u5230\u8fdb\u9636\uff0c\u638c\u63e1\u8bbe\u8ba1\u6a21\u5f0f\u3001\u4f18\u5316\u3001\u534f\u8c03\u5668\u5f00\u53d1\u4e0e\u534f\u540c\u534f\u4f5c\u3002', 179.00, 90, '\u6559\u7a0b\u8bfe\u7a0b', NULL, 567),
('Crypto \u6570\u5b57\u827a\u672f NFT', '\u72ec\u5bb6\u5b9a\u5236\u52a0\u5bc6\u827a\u672f\u4f5c\u54c1\uff0c\u6bcf\u4ef6\u5747\u4e3a\u94fe\u4e0a\u552f\u4e00 NFT\uff0c\u9650\u91cf\u53d1\u884c\u3002', 59.00, 500, '\u6570\u5b57\u827a\u672f', NULL, 1023),
('DAO \u6cbb\u7406\u5b9e\u6218\u624b\u518c', '\u6df1\u5165\u89e3\u6790 DAO \u7ec4\u7ec7\u67b6\u6784\u3001\u6cbb\u7406\u673a\u5236\u3001\u63d0\u6848\u6d41\u7a0b\u4e0e\u5b9e\u6218\u6848\u4f8b\u3002', 149.00, 150, '\u6559\u7a0b\u8bfe\u7a0b', NULL, 234),
('Web3 \u5f00\u53d1\u8005 SDK \u5957\u4ef6', '\u5305\u542b Web3Modal\u3001\u94b1\u5305\u8fde\u63a5\u3001\u5408\u7ea6\u8c03\u7528\u7684\u5b8c\u6574 SDK\uff0c\u652f\u6301 Ethereum/Polygon/BSC \u591a\u94fe\u3002', 0.00, 9999, '\u5f00\u53d1\u5de5\u5177', NULL, 2341);
