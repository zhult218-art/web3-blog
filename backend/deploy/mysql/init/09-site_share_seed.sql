-- ============================================================
-- site_share 种子数据：分享网站分类列表
-- 表结构见 08-web3_tool.sql（site_share）
-- 说明：
--   1) 分类由 category 字段区分，前端「分享网站」页自动按分类分组
--   2) 重复执行本文件会重复插入；若需重建表数据，请先清空：
--      DELETE FROM site_share;
--   3) t.co 短链可能失效，如失效请替换为真实目标地址
-- ============================================================
USE web3_tool;

INSERT INTO `site_share` (`category`, `name`, `url`, `description`, `icon`, `sort`, `status`) VALUES
-- ---------- 在线赚钱 ----------
('在线赚钱', 'Clickworker', 'https://clickworker.com', '做点小任务、填填问卷之类的，约 $3-12/小时', '💰', 0, 1),
('在线赚钱', 'Toloka', 'https://toloka.ai', '简单任务 + 数据采集，约 $5-15/小时', '🤖', 1, 1),
('在线赚钱', 'Outlier AI', 'https://outlier.ai', '给 AI 模型做训练和评估，约 $15-30/小时', '🧠', 2, 1),
('在线赚钱', 'CrowdGen (Appen)', 'https://crowdgen.com', '搜索评估、数据标注、AI 项目，约 $3-18/小时', '🔎', 3, 1),
('在线赚钱', 'Mercor', 'https://work.mercor.com', '专业技能单价更高，专家级 $80-250+/小时', '💼', 4, 1),
('在线赚钱', 'Alignerr', 'https://alignerr.com', 'AI 训练和专家任务，约 $15-30/小时', '🎯', 5, 1),
('在线赚钱', 'Remotasks', 'https://remotasks.com', '数据标注和 AI 相关工作，约 $3-15/小时', '🏷️', 6, 1),
('在线赚钱', 'TELUS Digital AI', 'https://telusinternational.ai', '搜索评估和 AI 项目，约 $3-8/小时', '🌐', 7, 1),
-- ---------- 远程工作 ----------
('远程工作', 'Kickresume', 'https://kickresume.com', 'AI 优化简历，匹配招聘 ATS 过滤条件', '📝', 0, 1),
('远程工作', 'KellyConnect', 'https://kellyconnect.com', '远程客户服务和技术支持，约 $15-17/小时', '📞', 1, 1),
('远程工作', 'Paidwork', 'https://paidwork.com', '看视频、玩游戏、填问卷赚钱，月收入可达 $700', '🎮', 2, 1),
('远程工作', 'Toptal', 'https://www.toptal.com', '高薪远程自由职业：软件开发/设计/金融', '💎', 3, 1),
('远程工作', 'Transcription Hub', 'https://transcriptionhub.com', '教育/医疗/法律/媒体等远程笔录，$10-22/小时', '🎧', 4, 1),
('远程工作', 'Preply', 'https://preply.com', '在线教学，连接全球学生，优秀导师 $20+/小时', '👩‍🏫', 5, 1),
('远程工作', 'Omni Interactions', 'https://omniinteractions.com', '远程客户服务，独立承包商，$14-20/小时', '🤝', 6, 1),
('远程工作', 'Jobspresso', 'https://jobspresso.co', '初创公司和国际企业的优质远程职位', '🚀', 7, 1),
('远程工作', 'NoDesk', 'https://nodesk.co', '每日汇总全球远程职位，多用美元/欧元支付', '🗓️', 8, 1),
-- ---------- 免翻节点 ----------
('免翻节点', 'Clash节点分享', 'https://clashgithub.com/clashnode-20260914.html', '免费 Clash/V2ray/Shadowrocket 节点，定期更新', '🛰️', 0, 1),
('免翻节点', 'freeclashnode', 'https://node.freeclashnode.com', '免费节点下载：Trojan/V2ray/Clash/Sing-Box', '⚡', 1, 1),
-- ---------- 在线观影 ----------
('在线观影', '看片狂人', 'https://kuang3.cfd/index.html', '在线观影，美剧资源丰富，支持 4K，适合直接追剧', '📺', 0, 1),
('在线观影', 'Pomo4K', 'https://pomo.mom', '偏下载使用，主打 4K 资源整理，适合保存到本地', '🎬', 1, 1),
-- ---------- 磁力资源 ----------
('磁力资源', '黑马磁力', 'https://heimacili.org', '磁力搜索引擎，资源全、速度快', '🔍', 0, 1),
('磁力资源', '磁力多', 'https://na.btdo.cc', '磁力搜索，界面清爽，找东西不费劲', '🧲', 1, 1),
('磁力资源', '磁力搜索', 'https://skrbtso.top', '磁力搜索引擎，搜啥有啥，专治索引荒', '🔎', 2, 1),
('磁力资源', '搜番', 'https://ar.sofan1.cc', '动漫磁力搜索，番剧一把抓', '⛩️', 3, 1),
('磁力资源', '磁力星球', 'https://so1.starok.top', '磁力搜索备用站，稳得很', '🪐', 4, 1),
-- ---------- 视频工具 ----------
('视频工具', 'AI视频文案提取', 'https://www.transcriptgenerate.com/zh-CN', '抖音/TikTok/YouTube/小红书/B站，粘贴链接秒出文案，免费', '📝', 0, 1),
('视频工具', 'X视频下载', 'https://t.co/4H7Hr24Alg', 'X (Twitter) 视频下载（短链，如失效请替换）', '⬇️', 1, 1),
('视频工具', '抖音/TikTok 去水印', 'https://t.co/1Q0xaaqU7J', '抖音/TikTok 视频去水印（短链，如失效请替换）', '🚿', 2, 1),
('视频工具', 'ins / Facebook 下载', 'https://t.co/tbGKY5AsFL', 'Instagram / Facebook 资源下载（短链，如失效请替换）', '📸', 3, 1),
('视频工具', '国内全平台下载', 'https://t.co/FTt7Z1FcTM', '国内全平台视频下载（短链，如失效请替换）', '🌏', 4, 1),
('视频工具', '视频去水印', 'https://t.co/f1ui7Lfpnw', '通用视频去水印（短链，如失效请替换）', '💧', 5, 1),
('视频工具', '图片去水印', 'https://t.co/eB4LumLEPb', '图片去水印（短链，如失效请替换）', '🖼️', 6, 1),
('视频工具', 'YouTube 开源版', 'https://t.co/wUAEf1nNj6', 'YouTube 开源下载工具（短链，如失效请替换）', '▶️', 7, 1),
('视频工具', '桌面下载工具', 'https://t.co/KtpJxPsFjr', '桌面端视频下载工具（短链，如失效请替换）', '💻', 8, 1),
-- ---------- 效率工具 ----------
('效率工具', '网页时光机', 'https://web.archive.org', '网页存档，页面被删/网站跑路也不怕，可取证据', '🕰️', 0, 1),
('效率工具', 'Radio Garden', 'https://radio.garden', '地球仪上点哪听哪，全球广播随意切换', '📻', 1, 1),
('效率工具', '临时邮箱', 'https://temp-mail.org', '一次性临时邮箱，注册网站专用，告别垃圾邮件', '📧', 2, 1),
('效率工具', 'Zapier', 'https://zapier.com', '自动化重复操作，繁琐流程串成链', '⚙️', 3, 1),
('效率工具', 'n8n', 'https://n8n.io', '开源工作流，可自部署，比 Zapier 更自由', '🔧', 4, 1),
('效率工具', '写给未来的信', 'https://futureme.org', '给未来的自己写信，一年/五年后准时送达', '💌', 5, 1),
-- ---------- 图片工具 ----------
('图片工具', 'Cleanup.pictures', 'https://cleanup.pictures', '框选抹除图片中的碍眼物体，效果好', '🧽', 0, 1),
('图片工具', 'Photopea', 'https://www.photopea.com', '浏览器里的 PS，功能足够硬核', '🎨', 1, 1),
('图片工具', 'remove.photos', 'https://remove.photos', '一键抠图去背景，做图效率翻倍', '✂️', 2, 1),
('图片工具', 'LetsEnhance', 'https://letsenhance.io', 'AI 补细节放大模糊图片，老照片也能救', '✨', 3, 1),
('图片工具', 'TinyWow', 'https://tinywow.com', 'PDF 转换、图片压缩、格式互转，全免费', '🪄', 4, 1),
-- ---------- PDF工具 ----------
('PDF工具', 'PDF24', 'https://www.pdf24.org', '免费 PDF 工具箱：合并/拆分/压缩/加密', '📄', 0, 1),
-- ---------- 设计白板 ----------
('设计白板', 'Excalidraw', 'https://excalidraw.com', '手绘风格流程图/架构图，开会临时画图神器', '✏️', 0, 1),
('设计白板', 'tldraw', 'https://www.tldraw.com', '无限白板，头脑风暴、整理思路', '🗂️', 1, 1),
-- ---------- 音频字幕 ----------
('音频字幕', 'Dictation.io', 'https://dictation.io', '在线语音转文字，不想打字就直接说', '🗣️', 0, 1),
('音频字幕', 'Otter.ai', 'https://otter.ai', '会议录音转文字，开完会不用自己扒', '🎙️', 1, 1),
('音频字幕', 'Descript', 'https://www.descript.com', '编辑音频像改文字一样简单，播客党必备', '🎚️', 2, 1),
('音频字幕', 'Loom', 'https://www.loom.com', '录屏 + 摄像头，比打文字解释高效十倍', '🎥', 3, 1),
-- ---------- 免费素材 ----------
('免费素材', 'Pixabay', 'https://pixabay.com', '图片/视频/音乐全有，商用免费', '📸', 0, 1),
('免费素材', 'Mixkit', 'https://mixkit.co', '视频素材和背景音乐，短视频剪辑好帮手', '🎵', 1, 1),
-- ---------- 开发资源 ----------
('开发资源', 'Roadmap.sh', 'https://roadmap.sh', '前端/后端/AI 工程师职业路线图', '🗺️', 0, 1),
('开发资源', 'Learn X in Y Minutes', 'https://learnxinyminutes.com', '几分钟快速上手一门编程语言核心语法', '⏱️', 1, 1),
('开发资源', 'DevDocs', 'https://devdocs.io', '主流语言与框架文档聚合，不用到处跳转', '📚', 2, 1),
('开发资源', 'JSON Crack', 'https://jsoncrack.com', 'JSON 数据可视化，接口结构一目了然', '🌳', 3, 1),
('开发资源', 'crontab guru', 'https://crontab.guru', 'Cron 定时表达式翻译成人话', '🕒', 4, 1),
-- ---------- 实用杂项 ----------
('实用杂项', 'AlternativeTo', 'https://alternativeto.net', '找任何软件的免费平替', '🔄', 0, 1),
('实用杂项', 'BuiltWith', 'https://builtwith.com', '扒网站技术栈，竞品分析利器', '🛰️', 1, 1),
('实用杂项', 'Untools', 'https://untools.co', '决策思维框架，选择困难症时打开看看', '🧩', 2, 1),
('实用杂项', 'CamelCamelCamel', 'https://camelcamelcamel.com', '亚马逊商品价格历史跟踪，防虚假折扣', '🐫', 3, 1),
('实用杂项', 'JustTheRecipe', 'https://justtherecipe.com', '只给食材和步骤的食谱工具', '🍳', 4, 1),
('实用杂项', 'CopyChar', 'https://copychar.cc', '特殊符号一键复制，排版省心', '🔣', 5, 1),
('实用杂项', 'Screenshot Guru', 'https://screenshot.guru', '整网页截图，长页面不用拼', '🖼️', 6, 1),
('实用杂项', 'WordCounter', 'https://wordcounter.net', '字数统计，满足硬性字数要求', '🔢', 7, 1),
-- ---------- 开源API ----------
('开源API', 'public-apis', 'https://github.com/public-apis/public-apis', '1000+ 免费公开 API 集合，413K+ Stars', '📦', 0, 1),
('开源API', 'public-api-lists', 'https://github.com/public-api-lists/public-api-lists', '小众冷门 API 收录，加密货币/AI/游戏数据等', '🗂️', 1, 1),
('开源API', 'vikiboss/60s', 'https://github.com/vikiboss/60s', '国内平台热搜聚合：知乎/微博/B站等', '🔥', 2, 1),
-- ---------- 知识百科 ----------
('知识百科', '人体生殖系统学习', 'https://knowsex.net/main/human-reproductive-system/', '人体生殖系统知识学习', '🧬', 0, 1),
('知识百科', 'AccuWeb 免费 WordPress', 'https://www.accuwebhosting.com/web-hosting/free-wordpress-hosting', '免费 WordPress 托管', '🌍', 1, 1);