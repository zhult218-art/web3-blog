// ============================================================
// 媒体页手工维护的资源数据（前端静态数据，无需后端接口）
// 视频模块：动漫推荐 / 影视剧集 / 磁力资源
// ============================================================

// 动漫/剧集通用字段：title 标题，year 年份，rating 评分，desc 简介，tags 标签，
//                link 站外观看地址（详情/官方），links 可观看平台链接（可选）
export const VIDEO_TABS = [
  { key: 'online', label: '在线视频', icon: '▶' },
  { key: 'download', label: '下载', icon: '⬇' },
  { key: 'anime', label: '动漫推荐', icon: '🍙' },
  { key: 'movie', label: '影视剧集', icon: '🎬' },
  { key: 'magnet', label: '磁力资源', icon: '🧲' },
]

export const ANIME_RECOMMENDS = [
  {
    title: '攻壳机动队 SAC 2045',
    year: '2020',
    rating: '8.7',
    desc: '公安九课再度集结，在网络与实体物理交叠的近未来追查新型威胁。',
    tags: ['科幻', '动作', '赛博朋克'],
    link: 'https://www.bilibili.com',
    links: [{ label: 'B站', url: 'https://www.bilibili.com' }, { label: 'Netflix', url: 'https://www.netflix.com' }],
  },
  {
    title: '咒术回战',
    year: '2020',
    rating: '8.9',
    desc: '诅咒与咒术师的世界，热血少年踏上注定孤独的战斗之路。',
    tags: ['热血', '奇幻', '战斗'],
    link: 'https://www.bilibili.com',
    links: [{ label: 'B站', url: 'https://www.bilibili.com' }],
  },
  {
    title: '进击的巨人 最终季',
    year: '2023',
    rating: '9.2',
    desc: '墙外的世界与墙内的真相交织，人类与巨人终局之战。',
    tags: ['热血', '暗黑', '史诗'],
    link: 'https://www.bilibili.com',
    links: [{ label: 'B站', url: 'https://www.bilibili.com' }],
  },
  {
    title: '葬送的芙莉莲',
    year: '2023',
    rating: '9.4',
    desc: '勇者一行冒险结束后的故事，与旅途中的回忆为伴，一部安静的公路片。',
    tags: ['奇幻', '治愈', '冒险'],
    link: 'https://www.bilibili.com',
    links: [{ label: 'B站', url: 'https://www.bilibili.com' }],
  },
  {
    title: '孤独摇滚！',
    year: '2022',
    rating: '9.0',
    desc: '社恐少女与乐队伙伴们一起追逐梦想的青春喜剧。',
    tags: ['日常', '音乐', '治愈'],
    link: 'https://www.bilibili.com',
    links: [{ label: 'B站', url: 'https://www.bilibili.com' }],
  },
  {
    title: '赛博朋克：边缘行者',
    year: '2022',
    rating: '9.1',
    desc: '夜之城底层少年的生与死，一部献给《赛博朋克2077》的情书。',
    tags: ['科幻', '动作', '致郁'],
    link: 'https://www.netflix.com',
    links: [{ label: 'Netflix', url: 'https://www.netflix.com' }],
  },
  {
    title: '鬼灭之刃 锻刀村篇',
    year: '2023',
    rating: '8.8',
    desc: '炭治郎与上弦之月面对面，血染锻刀村的决战之夜。',
    tags: ['热血', '奇幻', '战斗'],
    link: 'https://www.bilibili.com',
    links: [{ label: 'B站', url: 'https://www.bilibili.com' }],
  },
  {
    title: '间谍过家家',
    year: '2022',
    rating: '8.7',
    desc: '间谍、杀手与超能力女孩组成的临时家庭，爆笑又温暖。',
    tags: ['日常', '喜剧', '家庭'],
    link: 'https://www.bilibili.com',
    links: [{ label: 'B站', url: 'https://www.bilibili.com' }],
  },
]

export const MOVIE_LINKS = [
  {
    title: '流浪地球2',
    year: '2023',
    rating: '8.3',
    desc: '太阳危机逼近，人类驾驶地球一同流浪，文明存续的最后赌注。',
    tags: ['科幻', '灾难'],
    link: 'https://www.iqiyi.com',
    links: [{ label: '爱奇艺', url: 'https://www.iqiyi.com' }, { label: '腾讯视频', url: 'https://v.qq.com' }],
  },
  {
    title: '奥本海默',
    year: '2023',
    rating: '8.8',
    desc: '原子弹之父的一生：荣耀、挣扎与原罪。',
    tags: ['传记', '历史', '剧情'],
    link: 'https://www.youku.com',
    links: [{ label: '优酷', url: 'https://www.youku.com' }],
  },
  {
    title: '里斯本丸沉没',
    year: '2024',
    rating: '9.3',
    desc: '真实历史事件改编，二战中国渔民营救英军战俘的故事。',
    tags: ['纪录片', '历史'],
    link: 'https://www.youku.com',
    links: [{ label: '优酷', url: 'https://www.youku.com' }],
  },
  {
    title: '沙丘2',
    year: '2024',
    rating: '8.9',
    desc: '少年保罗走向预言中的救世主之路，沙漠史诗全面展开。',
    tags: ['科幻', '史诗'],
    link: 'https://www.iqiyi.com',
    links: [{ label: '爱奇艺', url: 'https://www.iqiyi.com' }],
  },
  {
    title: '三体',
    year: '2023',
    rating: '8.0',
    desc: '国产科幻剧集，红岸基地与三体文明的第一次接触。',
    tags: ['科幻', '悬疑'],
    link: 'https://www.tencent.com',
    links: [{ label: '腾讯视频', url: 'https://v.qq.com' }],
  },
  {
    title: '繁花',
    year: '2023',
    rating: '8.6',
    desc: '九十年代上海滩，商海沉浮与人间烟火。',
    tags: ['都市', '年代'],
    link: 'https://www.youku.com',
    links: [{ label: '优酷', url: 'https://www.youku.com' }],
  },
]

// 磁力资源：name 名称，size 大小，count 文件数，hash 磁力哈希，source 来源
export const MAGNET_RESOURCES = [
  {
    name: '流浪地球2·4K 蓝光原盘',
    size: '26.4 GB',
    count: 2,
    hash: 'magnet:?xt=urn:btih:9B8E5C9E1A4F2F3A0D1C2B3A4F5E6D7C8B9A0F1E',
    source: '某BT',
  },
  {
    name: '奥本海默·1080P 中英字幕',
    size: '8.2 GB',
    count: 1,
    hash: 'magnet:?xt=urn:btih:1A2B3C4D5E6F708192A3B4C5D6E7F8091A2B3C4D',
    source: '某BT',
  },
  {
    name: '沙丘2·4K HDR 全程',
    size: '19.7 GB',
    count: 1,
    hash: 'magnet:?xt=urn:btih:C0DE0F1A2B3C4D5E6F708192A3B4C5D6E7F8091A',
    source: '某BT',
  },
  {
    name: '赛博朋克：边缘行者·全10集',
    size: '5.1 GB',
    count: 10,
    hash: 'magnet:?xt=urn:btih:FA1B2C3D4E5F60718293A4B5C6D7E8F901A2B3C4D5E',
    source: '某BT',
  },
  {
    name: '咒术回战·第一季 全24集',
    size: '11.3 GB',
    count: 24,
    hash: 'magnet:?xt=urn:btih:1A2B3C4D5E6F708192A3B4C5D6E7F8091A2B3C4D5E6F',
    source: '某BT',
  },
  {
    name: '葬送的芙莉莲·全28集',
    size: '9.8 GB',
    count: 28,
    hash: 'magnet:?xt=urn:btih:0FE9D8C7B6A5F4E3D2C1B0A9F8E7D6C5B4A3F2E1D0C',
    source: '某BT',
  },
]