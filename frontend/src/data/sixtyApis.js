// ============================================================
// 60s API 全量接口目录
// 数据来源：github-myblog/60s-main src/router.ts（一个都不少）
// 公共实例：https://60s.viki.moe（站内经 /sixty 代理同源访问）
// params：接口支持的查询参数（plaza 据此渲染输入框）
// kind：json | image | text | rss —— 决定结果区如何渲染
// ============================================================

export const SIXTY_BASE = '/sixty/v2'

export const sixtyGroups = [
  {
    id: 'daily', name: '每日速览', icon: '📰',
    apis: [
      { path: '/60s', name: '每天60秒读懂世界', kind: 'json', params: [
        { key: 'encoding', label: '返回格式', placeholder: 'json（默认）/text/markdown/image' },
        { key: 'date', label: '指定日期', placeholder: 'YYYY-MM-DD（可选）' } ] },
      { path: '/60s/rss', name: '60秒 RSS 订阅源', kind: 'rss' },
    ],
  },
  {
    id: 'hot', name: '热榜聚合', icon: '🔥',
    apis: [
      { path: '/weibo', name: '微博热搜', kind: 'json' },
      { path: '/zhihu', name: '知乎热榜', kind: 'json' },
      { path: '/toutiao', name: '今日头条热榜', kind: 'json' },
      { path: '/bili', name: 'B站热门视频', kind: 'json' },
      { path: '/douyin', name: '抖音热点', kind: 'json' },
      { path: '/baidu/hot', name: '百度热搜', kind: 'json' },
      { path: '/baidu/teleplay', name: '百度电视剧榜', kind: 'json' },
      { path: 'baidu/tieba', name: '百度贴吧热议', kind: 'json' },
      { path: '/rednote', name: '小红书热点', kind: 'json' },
      { path: '/dongchedi', name: '懂车帝热榜', kind: 'json' },
      { path: '/quark', name: '夸克热搜', kind: 'json' },
      { path: '/moyu', name: '摸鱼热搜', kind: 'json' },
      { path: '/hacker-news/top', name: 'HN 热帖 Top', kind: 'json' },
      { path: '/hacker-news/new', name: 'HN 最新 New', kind: 'json' },
      { path: '/hacker-news/best', name: 'HN 精华 Best', kind: 'json' },
    ],
  },
  {
    id: 'news', name: '资讯排行', icon: '📡',
    apis: [
      { path: '/ai-news', name: 'AI 资讯', kind: 'json' },
      { path: '/it-news', name: 'IT 资讯', kind: 'json' },
      { path: '/it-news/rank', name: 'IT 资讯排行榜', kind: 'json' },
      { path: '/ncm-rank/list', name: '网易云音乐榜单列表', kind: 'json' },
      { path: '/ncm-rank/19723756', name: '网易云榜单详情(示例ID)', kind: 'json', params: [
        { key: '__path:id', label: '榜单ID（路径参数）', placeholder: '如 19723756' } ] },
      { path: '/epic', name: 'Epic 喜加一', kind: 'json' },
      { path: '/chemical', name: '化工行情', kind: 'json' },
      { path: '/answer', name: '十万个答案', kind: 'json' },
    ],
  },
  {
    id: 'fun', name: '轻松一刻', icon: '😄',
    apis: [
      { path: '/duanzi', name: '段子', kind: 'json' },
      { path: '/dad-joke', name: '冷笑话', kind: 'json' },
      { path: '/hitokoto', name: '一言', kind: 'json' },
      { path: '/luck', name: '每日运势', kind: 'json' },
      { path: '/fabing', name: '发病文学', kind: 'json' },
      { path: '/changya', name: '唱鸭', kind: 'json' },
      { path: '/lyric', name: '歌词搜索', kind: 'json', params: [
        { key: 'name', label: '歌曲名', placeholder: '如：晴天' },
        { key: 'singer', label: '歌手（可选）', placeholder: '如：周杰伦' } ] },
    ],
  },
  {
    id: 'life', name: '生活查询', icon: '🌦️',
    apis: [
      { path: '/weather/realtime', name: '实时天气', kind: 'json', params: [
        { key: 'city', label: '城市', placeholder: '如：北京' } ] },
      { path: '/weather/forecast', name: '天气预报', kind: 'json', params: [
        { key: 'city', label: '城市', placeholder: '如：上海' } ] },
      { path: '/gold-price', name: '黄金价格', kind: 'json' },
      { path: '/exchange-rate', name: '实时汇率', kind: 'json' },
      { path: '/fuel-price', name: '今日油价', kind: 'json' },
      { path: '/lunar', name: '农历/老黄历', kind: 'json' },
      { path: '/today-in-history', name: '历史上的今天', kind: 'json' },
      { path: '/health', name: '健康提示', kind: 'json' },
    ],
  },
  {
    id: 'ent', name: '影音文娱', icon: '🎬',
    apis: [
      { path: '/maoyan/all/movie', name: '猫眼全部电影', kind: 'json' },
      { path: '/maoyan/realtime/movie', name: '猫眼实时电影票房', kind: 'json' },
      { path: '/maoyan/realtime/tv', name: '猫眼剧集热度', kind: 'json' },
      { path: '/maoyan/realtime/web', name: '猫眼综艺热度', kind: 'json' },
      { path: '/douban/weekly/movie', name: '豆瓣电影一周口碑', kind: 'json' },
      { path: '/douban/weekly/tv_chinese', name: '豆瓣国产剧榜', kind: 'json' },
      { path: '/douban/weekly/tv_global', name: '豆瓣全球剧榜', kind: 'json' },
      { path: '/douban/weekly/show_chinese', name: '豆瓣国内综艺', kind: 'json' },
      { path: '/douban/weekly/show_global', name: '豆瓣国外综艺', kind: 'json' },
      { path: '/olympics', name: '奥运奖牌榜', kind: 'json' },
      { path: '/olympics/events', name: '奥运赛程', kind: 'json' },
      { path: '/bing', name: '必应每日壁纸', kind: 'json' },
    ],
  },
  {
    id: 'tools', name: '开发者工具', icon: '🛠️',
    apis: [
      { path: '/ip', name: 'IP / 归属地', kind: 'json' },
      { path: '/whois', name: 'Whois 查询', kind: 'json', params: [
        { key: 'domain', label: '域名', placeholder: '如：example.com' } ] },
      { path: '/qrcode', name: '二维码生成', kind: 'image', params: [
        { key: 'text', label: '内容', placeholder: 'https://...' },
        { key: 'size', label: '尺寸', placeholder: '400（可选）' } ] },
      { path: '/color/random', name: '随机配色', kind: 'json' },
      { path: '/color/palette', name: '调色板推荐', kind: 'json' },
      { path: '/baike', name: '百科查询', kind: 'json', params: [
        { key: 'word', label: '词条', placeholder: '如：量子计算' } ] },
      { path: '/awesome-js', name: 'JS 优秀开源榜', kind: 'json' },
    ],
  },
  {
    id: 'compute', name: '计算类（支持POST）', icon: '🧮',
    apis: [
      { path: '/fanyi', name: '翻译', kind: 'json', method: 'POST', params: [
        { key: 'text', label: '待翻译文本', placeholder: 'Hello world' },
        { key: 'from', label: '源语言', placeholder: 'auto（默认）' },
        { key: 'to', label: '目标语言', placeholder: 'zh（默认）' } ] },
      { path: '/fanyi/langs', name: '支持语言列表', kind: 'json' },
      { path: '/hash', name: '哈希计算', kind: 'json', method: 'POST', params: [
        { key: 'text', label: '原文', placeholder: '输入文本' },
        { key: 'type', label: '算法', placeholder: 'md5/sha1/sha256' } ] },
      { path: '/og', name: 'OG 站点信息解析', kind: 'json', method: 'POST', params: [
        { key: 'url', label: '页面URL', placeholder: 'https://example.com' } ] },
      { path: '/password', name: '随机强密码生成', kind: 'json' },
      { path: '/password/check', name: '密码强度检测', kind: 'json', params: [
        { key: 'password', label: '待检测密码', placeholder: '输入密码' } ] },
    ],
  },
  {
    id: 'beta', name: 'Beta / 兼容接口', icon: '🧪',
    apis: [
      { path: '/beta/kuan', name: '宽客数据(beta)', kind: 'json' },
      { path: '/beta/qq/profile', name: 'QQ资料卡(beta)', kind: 'json', params: [
        { key: 'qq', label: 'QQ号', placeholder: '如：10000' } ] },
      { path: '/kfc', name: 'KFC 疯狂星期四', kind: 'json' },
      { path: '/exchange_rate', name: '汇率(兼容旧版)', kind: 'json' },
      { path: '/today_in_history', name: '历史今天(兼容旧版)', kind: 'json' },
      { path: '/maoyan', name: '猫眼电影(兼容旧版)', kind: 'json' },
      { path: '/baidu/realtime', name: '百度热搜(兼容旧版)', kind: 'json' },
      { path: '/weather', name: '天气(兼容旧版)', kind: 'json', params: [
        { key: 'city', label: '城市', placeholder: '北京' } ] },
      { path: '/ncm-rank', name: '云榜单(兼容旧版)', kind: 'json' },
      { path: '/color', name: '随机色(兼容旧版)', kind: 'json' },
    ],
  },
]

// 扁平化接口总数（供广场页统计）
export const sixtyApiTotal = sixtyGroups.reduce((n, g) => n + g.apis.length, 0)
