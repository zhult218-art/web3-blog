USE web3_blog;

CREATE TABLE IF NOT EXISTS `article` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(200) NOT NULL,
  `summary` VARCHAR(500) DEFAULT NULL,
  `content` LONGTEXT,
  `category` VARCHAR(64) DEFAULT NULL,
  `tags` VARCHAR(255) DEFAULT NULL,
  `cover` VARCHAR(512) DEFAULT NULL,
  `author_id` BIGINT DEFAULT NULL,
  `author_name` VARCHAR(64) DEFAULT NULL,
  `view_count` BIGINT DEFAULT 0,
  `like_count` INT DEFAULT 0,
  `status` VARCHAR(32) DEFAULT 'PUBLISHED',
  `is_top` INT DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `article_like` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `article_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_like` (`article_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `article` (`title`, `summary`, `content`, `category`, `tags`, `author_id`, `status`, `is_top`) VALUES
('\u6b22\u8fce\u6765\u5230 Web3 Portal', '\u5168\u80fd Web3 \u6280\u672f\u95e8\u6237\uff0c\u63a2\u7d22\u533a\u5757\u94fe\u4e0e\u5143\u5b87\u5b99\u7684\u65e0\u9650\u53ef\u80fd\u3002', '# \u6b22\u8fce\u6765\u5230 Web3 Portal\n\n\u8fd9\u662f\u6211\u4eec\u7684\u7b2c\u4e00\u7bc7\u6587\u7ae0\uff0c\u4e5f\u662f\u6574\u4e2a\u5e73\u53f0\u7684\u8d77\u70b9\u3002\n\n## \u4e3a\u4ec0\u4e48\u8981\u505a Web3 Portal\n\nWeb3 \u662f\u4e0b\u4e00\u4ee3\u4e92\u8054\u7f51\u7684\u57fa\u7840\u8bbe\u65bd\uff0c\u5b83\u5c06\u5f71\u54cd\u6211\u4eec\u751f\u6d3b\u7684\u65b9\u65b9\u9762\u9762\uff1a\n\n- **\u53bb\u4e2d\u5fc3\u5316\u5e94\u7528**\uff1a\u4e0d\u518d\u4f9d\u8d56\u5355\u4e00\u5e73\u53f0\n- **\u6570\u5b57\u8d44\u4ea7**\uff1aNFT\u3001\u52a0\u5bc6\u8d27\u5e01\u3001\u4ee3\u5e01\n- **\u667a\u80fd\u5408\u7ea6**\uff1a\u81ea\u52a8\u5316\u6267\u884c\u7684\u6761\u6b3e\n- **DAO \u6cbb\u7406**\uff1a\u53bb\u4e2d\u5fc3\u5316\u7684\u7ec4\u7ec7\u51b3\u7b56\n\n## \u6211\u4eec\u7684\u76ee\u6807\n\n\u6253\u9020\u4e00\u4e2a\u5168\u9762\u7684 Web3 \u5f00\u53d1\u8005\u793e\u533a\uff0c\u63d0\u4f9b\u6559\u7a0b\u3001\u5de5\u5177\u3001\u8d44\u8baf\u548c\u5b9e\u6218\u7ecf\u9a8c\u3002\n\n> Web3 \u4e0d\u53ea\u662f\u6280\u672f\uff0c\u66f4\u662f\u4e00\u79cd\u601d\u7ef4\u65b9\u5f0f\u3002', '\u516c\u544a', 'web3,welcome,\u5165\u95e8', 1, 'PUBLISHED', 1),
('\u4ece\u96f6\u5f00\u59cb\u5b66\u4e60 Solidity \u667a\u80fd\u5408\u7ea6\u5f00\u53d1', '\u672c\u6587\u5e26\u4f60\u4ece\u73af\u5883\u642d\u5efa\u5230\u7b2c\u4e00\u4e2a\u5408\u7ea6\u90e8\u7f72\u3002', '# Solidity \u667a\u80fd\u5408\u7ea6\u5f00\u53d1\u5165\u95e8\n\n## 1. \u73af\u5883\u51c6\u5907\n\n\u9996\u5148\u5b89\u88c5 Node.js \u548c npm\uff0c\u7136\u540e\u4f7f\u7528 Hardhat\uff1a\n\n```bash\nnpm init -y\nnpm install --save-dev hardhat\nnpx hardhat init\n```\n\n## 2. \u7b2c\u4e00\u4e2a\u5408\u7ea6\n\n\u521b\u5efa `contracts/Hello.sol`\uff1a\n\n```solidity\n// SPDX-License-Identifier: MIT\npragma solidity ^0.8.20;\n\ncontract Hello {\n    string public message;\n    address public owner;\n\n    event MessageChanged(string newMessage);\n\n    constructor(string memory _message) {\n        message = _message;\n        owner = msg.sender;\n    }\n\n    function setMessage(string memory _newMessage) public {\n        require(msg.sender == owner, "Only owner");\n        message = _newMessage;\n        emit MessageChanged(_newMessage);\n    }\n}\n```\n\n## 3. \u90e8\u7f72\u5230\u6d4b\u8bd5\u7f51\n\n```bash\nnpx hardhat run scripts/deploy.js --network sepolia\n```\n\n## \u603b\u7ed3\n\nSolidity \u5f00\u53d1\u7684\u6838\u5fc3\u662f\u7406\u89e3 EVM \u673a\u5236\u548c gas \u4f18\u5316\u3002\u4e0b\u4e00\u7bc7\u6211\u4eec\u5c06\u63a2\u8ba8\u4e8c\u6b21\u5f00\u53d1\u6a21\u5f0f\u548c\u534f\u8c03\u534f\u4f5c\u3002', '\u6559\u7a0b\u8bfe\u7a0b', 'solidity,\u667a\u80fd\u5408\u7ea6,hardhat,\u5f00\u53d1', 1, 'PUBLISHED', 0),
('DeFi \u534f\u8bae\u6df1\u5ea6\u89e3\u6790\uff1a\u4ece AMM \u5230\u6d41\u52a8\u6027\u6316\u6398', '\u6df1\u5165\u7406\u89e3\u53bb\u4e2d\u5fc3\u5316\u4ea4\u6613\u7684\u5e95\u5c42\u903b\u8f91\u3002', '# DeFi \u534f\u8bae\u6df1\u5ea6\u89e3\u6790\n\n## AMM \u539f\u7406\n\n\u81ea\u52a8\u505a\u5546\u5e02\u573a\u534f\u8bae\uff08AMM\uff09\u662f DeFi \u7684\u6838\u5fc3\u521b\u65b0\uff0c\u4ee3\u8868\u9879\u76ee\u6709 Uniswap\u3001PancakeSwap \u7b49\u3002\n\n### \u6838\u5fc3\u516c\u5f0f\n\n```\nx * y = k\n```\n\n\u5176\u4e2d x \u548c y \u662f\u6c60\u5b50\u4e2d\u4e24\u79cd\u4ee3\u5e01\u7684\u50a8\u5907\u91cf\uff0ck \u662f\u5e38\u6570\u3002\n\n### \u6d41\u52a8\u6027\u6316\u6398\n\n\u6d41\u52a8\u6027\u6316\u6398\u8005\u63d0\u4f9b\u4ee3\u5e01\u5230\u6c60\u5b50\u4e2d\u83b7\u53d6\u4ea4\u6613\u624b\u7eed\u8d39\u7684\u884c\u4e3a\u3002\n\n- **\u6536\u76ca**\uff1a\u4ea4\u6613\u624b\u7eed\u8d39\u7684\u4e00\u90e8\u5206\n- **\u98ce\u9669**\uff1a\u65e0\u5e38\u635f\u5931\u3001\u6c60\u5b50\u5355\u5411\u635f\u5931\n\n### \u5b9e\u8df5\u5efa\u8bae\n\n1. \u4ece\u5c0f\u989d\u5f00\u59cb\n2. \u5206\u6563\u6295\u5165\u591a\u4e2a\u6c60\u5b50\n3. \u5173\u6ce8\u6c60\u5b50\u7684\u6df1\u5ea6\u548c\u6d41\u52a8\u6027\n\n> DeFi \u7684\u672a\u6765\u5728\u4e8e\u53ef\u7ec4\u5408\u6027\uff0c\u6bcf\u4e2a\u534f\u8bae\u90fd\u53ef\u4ee5\u88ab\u62fc\u63a5\u6210\u66f4\u590d\u6742\u7684\u4ea7\u54c1\u3002', '\u6280\u672f\u5206\u6790', 'DeFi,AMM,\u6d41\u52a8\u6027,\u6316\u6398,Uniswap', 1, 'PUBLISHED', 0),
('Web3 \u524d\u7aef\u5f00\u53d1\u5b9e\u6218\uff1a\u6784\u5efa\u4f60\u7684\u7b2c\u4e00\u4e2a DApp', '\u4f7f\u7528 React + ethers.js \u6784\u5efa\u53bb\u4e2d\u5fc3\u5316\u5e94\u7528\u3002', '# Web3 \u524d\u7aef\u5f00\u53d1\u5b9e\u6218\n\n## \u6280\u672f\u6808\n\n- React / Next.js\n- ethers.js v6\n- wagmi + viem\n- WalletConnect\n\n## \u94b1\u5305\u8fde\u63a5\n\n```javascript\nimport { useAccount, useConnect } from \'wagmi\'\n\nfunction WalletButton() {\n  const { address, isConnected } = useAccount()\n  const { connect, connectors } = useConnect()\n\n  if (isConnected) {\n    return <span>{address.slice(0,6)}...{address.slice(-4)}</span>\n  }\n\n  return (\n    <button onClick={() => connect({ connector: connectors[0] })}>\n      \u8fde\u63a5\u94b1\u5305\n    </button>\n  )\n}\n```\n\n## \u5408\u7ea6\u4ea4\u4e92\n\n\u4f7f\u7528 ethers.js \u8c03\u7528\u667a\u80fd\u5408\u7ea6\u65b9\u6cd5\uff0c\u5904\u7406 gas \u8d39\u7528\u548c\u4ea4\u6613\u786e\u8ba4\u3002\n\n## \u603b\u7ed3\n\nWeb3 \u524d\u7aef\u5f00\u53d1\u7684\u6838\u5fc3\u662f\u7406\u89e3\u533a\u5757\u94fe\u4e0e\u524d\u7aef\u7684\u4ea4\u4e92\u65b9\u5f0f\uff0c\u6ce8\u610f\u4f53\u9a8c\u4f18\u5316\u548c\u5b89\u5168\u6027\u3002', '\u6559\u7a0b\u8bfe\u7a0b', 'DApp,React,ethers.js,\u524d\u7aef,web3.js', 1, 'PUBLISHED', 0),
('NFT \u6280\u672f\u6982\u8ff0\uff1a\u4ece ERC-721 \u5230\u52a3\u52bf\u5408\u7ea6', '\u5168\u9762\u4e86\u89e3 NFT \u6280\u672f\u6808\u4e0e\u5e94\u7528\u573a\u666f\u3002', '# NFT \u6280\u672f\u6982\u8ff0\n\n## \u4ec0\u4e48\u662f NFT\n\nNFT\uff08Non-Fungible Token\uff09\u662f\u4e0d\u53ef\u66ff\u4ee3\u4ee3\u5e01\uff0c\u6bcf\u4e00\u4e2a\u90fd\u662f\u72ec\u4e00\u65e0\u4e8c\u7684\u6570\u5b57\u8d44\u4ea7\u3002\n\n## \u6838\u5fc3\u6807\u51c6\n\n### ERC-721\n\n\u6700\u57fa\u672c\u7684 NFT \u6807\u51c6\uff0c\u6bcf\u4e2a\u4ee3\u5e01\u4e00\u4e2a\u552f\u4e00 ID\u3002\n\n### ERC-1155\n\n\u591a\u4ee3\u5e01\u6807\u51c6\uff0c\u53ef\u4ee5\u540c\u65f6\u94f8\u9020\u540c\u8d28\u548c\u5f02\u8d28\u4ee3\u5e01\u3002\n\n## \u5e94\u7528\u573a\u666f\n\n1. **\u6570\u5b57\u827a\u672f**\uff1a\u753b\u4f5c\u3001\u97f3\u4e50\u3001\u6444\u5f71\n2. **\u6e38\u620f NFT**\uff1a\u89d2\u8272\u3001\u88c5\u5907\u3001\u5730\u56fe\n3. **\u8eab\u4efd\u8bc6\u522b**\uff1a\u540d\u7247\u3001\u8bc1\u4e66\n4. **\u793c\u5238**\uff1a\u4f1a\u5458\u8d44\u683c\u3001\u6d3b\u52a8\u5165\u573a\u5238\n\n## \u672a\u6765\u8d8b\u52bf\n\n- **\u52a3\u52bf\u5408\u7ea6**\uff1a\u964d\u4f4e\u94f8\u9020\u6210\u672c\n- **\u52a8\u6001 NFT**\uff1a\u53ef\u53d8\u5c5e\u6027 NFT\n- **\u8de8\u94fe NFT**\uff1a\u591a\u94fe\u4e92\u64cd\u4f5c\n\n> NFT \u7684\u672a\u6765\u4e0d\u53ea\u662f\u6536\u85cf\u54c1\uff0c\u66f4\u662f\u6570\u5b57\u8eab\u4efd\u7684\u57fa\u7840\u8bbe\u65bd\u3002', '\u6280\u672f\u5206\u6790', 'NFT,ERC721,ERC1155,\u6570\u5b57\u827a\u672f,\u533a\u5757\u94fe', 1, 'PUBLISHED', 0),
('\u533a\u5757\u94fe\u5b89\u5168\u5ba1\u8ba1\u5b9e\u6218\u6307\u5357', '\u5982\u4f55\u5bf9\u667a\u80fd\u5408\u7ea6\u8fdb\u884c\u5168\u9762\u7684\u5b89\u5168\u5ba1\u8ba1\u3002', '# \u533a\u5757\u94fe\u5b89\u5168\u5ba1\u8ba1\u5b9e\u6218\u6307\u5357\n\n## \u5ba1\u8ba1\u5de5\u5177\n\n- **Slither**\uff1a\u9759\u6001\u5206\u6790\u5de5\u5177\n- **Mythril**\uff1a\u7b26\u53f7\u6267\u884c\u5f15\u64ce\n- **Echidna**\uff1a\u5c40\u90e8\u6d4b\u8bd5\u5de5\u5177\n- **Certora**\uff1a\u5f62\u5f0f\u5316\u9a8c\u8bc1\n\n## \u5e38\u89c1\u6f0f\u6d1e\n\n### \u91cd\u5165\u6f0f\u6d1e\n\n```solidity\n// \u9519\u8bef\u5199\u6cd5\nfunction withdraw() external {\n    uint amount = balances[msg.sender];\n    (bool sent, ) = msg.sender.call{value: amount}(\"\");\n    require(sent, \"Failed to send Ether\");\n    balances[msg.sender] = 0; // \u91cd\u5165\uff01\!\n}\n\n// \u6b63\u786e\u5199\u6cd5\nfunction withdraw() external {\n    uint amount = balances[msg.sender];\n    balances[msg.sender] = 0; // \u5148\u7f6e\u96f6\n    (bool sent, ) = msg.sender.call{value: amount}(\"\");\n    require(sent, \"Failed to send Ether\");\n}\n```\n\n### \u6574\u6570\u6ea2\u51fa\n\n\u4f7f\u7528 SafeMath \u6216 Solidity 0.8+ \u7684\u5185\u7f6e\u68c0\u67e5\u3002\n\n## \u5ba1\u8ba1\u6d41\u7a0b\n\n1. \u4ee3\u7801\u5206\u6790\u4e0e\u9759\u6001\u626b\u63cf\n2. \u52a8\u6001\u5206\u6790\u4e0e\u6d4b\u8bd5\n3. \u5f62\u5f0f\u5316\u9a8c\u8bc1\n4. \u4eba\u5de5\u5ba1\u67e5\n5. \u62a5\u544a\u7f16\u5199\n\n> \u5b89\u5168\u4e0d\u662f\u4e00\u6b21\u6027\u7684\uff0c\u9700\u8981\u6301\u7eed\u5173\u6ce8\u548c\u66f4\u65b0\u3002', '\u6280\u672f\u5206\u6790', '\u5b89\u5168,audit,Slither,Mythril,\u5408\u7ea6\u5ba1\u8ba1', 1, 'PUBLISHED', 0);

-- ============================================================
-- 博客增强模块（移植自 lololowe 博客）
-- 友链 / 公告 / 站点配置
-- ============================================================

CREATE TABLE IF NOT EXISTS `friend_link` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `name`        VARCHAR(64)  NOT NULL COMMENT '站名',
  `url`         VARCHAR(255) NOT NULL COMMENT '链接',
  `avatar`      VARCHAR(512) DEFAULT NULL COMMENT '头像',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '一句话描述',
  `group_name`  VARCHAR(32)  DEFAULT 'default' COMMENT '分组',
  `status`      VARCHAR(16)  DEFAULT 'PUBLISHED' COMMENT 'PUBLISHED/PENDING/DISABLED',
  `sort`        INT          DEFAULT 0 COMMENT '排序（小的在前）',
  `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='友情链接';

CREATE TABLE IF NOT EXISTS `site_notice` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `content`    VARCHAR(512) NOT NULL COMMENT '公告内容',
  `enabled`    TINYINT      DEFAULT 1 COMMENT '1=启用',
  `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站点公告';

CREATE TABLE IF NOT EXISTS `blog_setting` (
  `setting_key`   VARCHAR(64)  NOT NULL COMMENT '配置键',
  `setting_value` TEXT         NULL COMMENT '配置值（JSON 字符串）',
  `updated_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='博客站点配置';

INSERT INTO `blog_setting` (`setting_key`, `setting_value`) VALUES
('author', '{"nickname":"Web3 Portal","slogan":"探秘元宇宙，记录成长","avatar":"","github":"","email":"","weibo":""}'),
('donate', '{"btc_qr":"","eth_qr":""}'),
('footer_links', '{"资产测绘":[{"name":"FOFA","url":"https://fofa.info"}],"云沙箱":[],"网络检测":[]}'),
('icp', '{"icp_no":"","copyright":"© 2024-2026"}');
