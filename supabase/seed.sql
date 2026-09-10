-- ============================================================
-- 数据注入 Seed（web3-blog）
-- 在 schema.sql 执行后运行本文件，写入真实/演示数据。
-- 用法：Supabase SQL Editor 粘贴执行，或 supabase db reset。
-- 注意：profiles 依赖 auth.users，若尚无登录用户，请先在前端
--       注册 1~2 个账号后再插入 author_id（或用子查询替换）。
-- ============================================================

-- ------------------------------------------------------------
-- 演示示例表 demo_items
-- ------------------------------------------------------------
insert into public.demo_items (name, description, category, meta)
values
  ('示例任务 A', '来自 Supabase 的第一条演示数据', 'demo', '{"source":"seed"}'),
  ('示例任务 B', '验证读写通路的第二条数据', 'demo', '{"source":"seed"}'),
  ('书单 - 三体', '刘慈欣科幻三部曲', 'book', '{"rating":9.6}'),
  ('书单 - 百年孤独', '加西亚·马尔克斯', 'book', '{"rating":9.3}')
on conflict do nothing;

-- ------------------------------------------------------------
-- 音乐馆歌曲 music_tracks
-- 说明：
--   1) 5 首网易云真歌：只有 netease_id（无内置音频），前端播放时
--      按 netease_id 向网易云按需取流 + 拉歌词。
--   2) 20 首演示音频：audio_url 直链，播放零依赖、音画一致。
--   3) WHERE NOT EXISTS 按 (title, artist) 去重，脚本可重复执行。
-- ------------------------------------------------------------
insert into public.music_tracks (title, artist, album, category, duration_seconds, netease_id, audio_url)
select v.title, v.artist, v.album, v.category, v.duration_seconds, v.netease_id, v.audio_url
from (values
  ('走马', '陈粒', '如也', '民谣', 254, 35627796::bigint, null),
  ('理想三旬', '陈鸿宇', '浓烟下的诗歌电台', '民谣', 269, 31827976::bigint, null),
  ('平凡之路', '朴树', '猎户星座', '流行', 301, 29098184::bigint, null),
  ('南山南', '马頔', '孤岛', '民谣', 317, 29098186::bigint, null),
  ('光年之外', 'G.E.M.邓紫棋', '光年之外', '流行', 236, 458762291::bigint, null),
  ('SoundHelix Song 1', 'SoundHelix', 'SoundHelix Demo', '电子', 444, null, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3'),
  ('SoundHelix Song 2', 'SoundHelix', 'SoundHelix Demo', '流行', 370, null, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3'),
  ('SoundHelix Song 3', 'SoundHelix', 'SoundHelix Demo', '摇滚', 309, null, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3'),
  ('SoundHelix Song 4', 'SoundHelix', 'SoundHelix Demo', '民谣', 406, null, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3'),
  ('SoundHelix Song 5', 'SoundHelix', 'SoundHelix Demo', '古典', 320, null, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3'),
  ('SoundHelix Song 6', 'SoundHelix', 'SoundHelix Demo', '电子', 383, null, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3'),
  ('SoundHelix Song 7', 'SoundHelix', 'SoundHelix Demo', '爵士', 354, null, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3'),
  ('SoundHelix Song 8', 'SoundHelix', 'SoundHelix Demo', '轻音乐', 291, null, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3'),
  ('SoundHelix Song 9', 'SoundHelix', 'SoundHelix Demo', '氛围', 366, null, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3'),
  ('SoundHelix Song 10', 'SoundHelix', 'SoundHelix Demo', '古典', 338, null, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3'),
  ('SoundHelix Song 11', 'SoundHelix', 'SoundHelix Demo', '电子', 401, null, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-11.mp3'),
  ('SoundHelix Song 12', 'SoundHelix', 'SoundHelix Demo', '钢琴', 427, null, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-12.mp3'),
  ('SoundHelix Song 13', 'SoundHelix', 'SoundHelix Demo', '爵士', 345, null, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-13.mp3'),
  ('SoundHelix Song 14', 'SoundHelix', 'SoundHelix Demo', '民谣', 312, null, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-14.mp3'),
  ('SoundHelix Song 15', 'SoundHelix', 'SoundHelix Demo', '摇滚', 396, null, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-15.mp3'),
  ('SoundHelix Song 16', 'SoundHelix', 'SoundHelix Demo', '流行', 378, null, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-16.mp3'),
  ('SoundHelix Song 17', 'SoundHelix', 'SoundHelix Demo', '电子', 361, null, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-17.mp3'),
  ('Kalimba', 'LearningContainer', '免费演示音频', '轻音乐', 300, null, 'https://www.learningcontainer.com/wp-content/uploads/2020/02/Kalimba.mp3'),
  ('Sample MP3', 'FileSamples', 'Sample 演示', '演示', 120, null, 'https://filesamples.com/samples/audio/mp3/sample1.mp3'),
  ('T-Rex Roar', 'MDN', 'MDN 演示', '音效', 10, null, 'https://interactive-examples.mdn.mozilla.net/media/cc0-audio/t-rex-roar.mp3')
) as v(title, artist, album, category, duration_seconds, netease_id, audio_url)
where not exists (
  select 1 from public.music_tracks m
  where m.title = v.title and coalesce(m.artist, '') = coalesce(v.artist, '')
);

-- ------------------------------------------------------------
-- 博客文章 blog_articles （author 可空）
-- ------------------------------------------------------------
insert into public.blog_articles (title, slug, summary, content, tags, category, author_name, published, views, likes)
values
  (
    '从零搭建个人 Web3 博客',
    'build-web3-blog-from-scratch',
    '记录用 Vue3 + Spring Cloud 微服务搭建个人博客的全过程',
    E'# 从零搭建\n\n这是一篇演示文章，用于验证博客从 Supabase 读取数据。',
    array['vue3','spring','web3'], '技术', 'admin', true, 128, 42
  ),
  (
    'Supabase 接入实践',
    'supabase-integration-practice',
    '前端用 publishable key + RLS，后端用 secret key 访问 Postgres',
    E'# Supabase\n\nRow Level Security 是数据安全的关键。',
    array['supabase','database','auth'], '技术', 'admin', true, 96, 31
  ),
  (
    '我的 2026 书单',
    'my-2026-reading-list',
    '今年读过的几本好书推荐',
    E'# 书单\n\n三体、百年孤独……',
    array['阅读'], '生活', null, false, 0, 0
  )
on conflict (slug) do nothing;

-- 后端 blog-service 的 6 篇演示文章，迁入 Supabase 供前端公共读取
insert into public.blog_articles (title, slug, summary, content, tags, category, author_name, published, views, likes, created_at, updated_at)
values
  (
    '欢迎来到 Web3 Portal',
    'welcome-to-web3-portal',
    '全能 Web3 技术门户，探索区块链与元宇宙的无限可能。',
    E'这是我们的第一篇文章，也是整个平台的起点。为什么要 Web3 Portal：Web3 是下一代互联网的基础设施，包括去中心化应用、数字资产、智能合约、DAO 治理。我们的目标：打造一个全面的 Web3 开发者社区，提供教程、工具、资讯和实战经验。',
    array['web3','welcome','入门'], '公告', NULL, true, 9, 0, '2026-08-27T10:54:43+00:00', '2026-08-27T10:54:43+00:00'
  ),
  (
    '从零开始学习 Solidity 智能合约开发',
    'solidity-smart-contract-guide',
    '本文带你从环境搭建到第一个合约部署。',
    E'Solidity 智能合约开发入门：1. 环境准备：安装 Node.js 和 npm，使用 Hardhat。2. 第一个合约：创建 Hello.sol，实现消息存储和事件。3. 部署到测试网：使用 hardhat run 部署到 Sepolia。Solidity 开发的核心是理解 EVM 机制和 gas 优化。',
    array['solidity','智能合约','hardhat','开发'], '教程课程', NULL, true, 4, 0, '2026-08-27T10:54:43+00:00', '2026-08-27T10:54:43+00:00'
  ),
  (
    'DeFi 协议深度解析：从 AMM 到流动性挖掘',
    'defi-amm-liquidity-mining',
    '深入理解去中心化交易的底层逻辑。',
    E'DeFi 协议深度解析：AMM 原理：自动做市商协议是 DeFi 的核心创新，代表项目有 Uniswap、PancakeSwap。核心公式 x * y = k。流动性挖掘：提供代币到池子中获取交易手续费。收益：交易手续费的一部分。风险：无常损失、池子单向损失。DeFi 的未来在于可组合性。',
    array['DeFi','AMM','流动性','挖掘'], '技术分析', NULL, true, 3, 0, '2026-08-27T10:54:43+00:00', '2026-08-27T10:54:43+00:00'
  ),
  (
    'Web3 前端开发实战：构建你的第一个 DApp',
    'web3-dapp-frontend-practice',
    '使用 React + ethers.js 构建去中心化应用。',
    E'Web3 前端开发实战：技术栈：React、ethers.js v6、wagmi、WalletConnect。钱包连接：使用 wagmi 的 useAccount 和 useConnect hook。合约交互：使用 ethers.js 的 Contract 对象调用智能合约。NFT 展示：读取链上 NFT 元数据并渲染。核心是理解区块链与前端的交互方式。',
    array['DApp','React','ethers.js','前端'], '教程课程', NULL, true, 2, 0, '2026-08-27T10:54:43+00:00', '2026-08-27T10:54:43+00:00'
  ),
  (
    'NFT 技术概述：从 ERC-721 到动态 NFT',
    'nft-erc721-to-dynamic-nft',
    '全面了解 NFT 技术栈与应用场景。',
    E'NFT 技术概述：什么是 NFT：Non-Fungible Token，不可替代代币。核心标准：ERC-721 基本 NFT 标准，ERC-1155 多代币标准。应用场景：数字艺术、游戏 NFT、身份识别、礼券。未来趋势：劣势合约降低成本、动态 NFT 可变属性、跨链 NFT 互操作。',
    array['NFT','ERC721','ERC1155','数字艺术'], '技术分析', NULL, true, 2, 0, '2026-08-27T10:54:43+00:00', '2026-08-27T10:54:43+00:00'
  ),
  (
    '区块链安全审计实战指南',
    'blockchain-security-audit-guide',
    '如何对智能合约进行全面的安全审计。',
    E'区块链安全审计实战指南：审计工具：Slither、Mythril、Echidna、Certora。常见漏洞：重入漏洞（先置零再转账）、整数溢出（使用 Solidity 0.8+ 内置检查）、权限控制（严格的访问控制）。审计流程：代码分析、动态测试、形式化验证、人工审查、报告编写。安全不是一次性的，需要持续关注和更新。',
    array['安全','audit','Slither','Mythril'], '技术分析', NULL, true, 4, 0, '2026-08-27T10:54:43+00:00', '2026-08-27T10:54:43+00:00'
  )
on conflict (slug) do nothing;

-- 为已登录用户补 author_id（若有）：
-- update public.blog_articles set author_id = (select id from public.profiles limit 1)
-- where author_id is null and published = true;
