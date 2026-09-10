/**
 * 演示曲目与歌词数据（LRC 时间戳结构）
 * 后端音乐接口就绪后，此处数据可被 API 返回值替换，字段结构保持一致。
 */

export const DEMO_LYRICS = {
  '星途列车': [
    { time: 0, text: '窗外星轨划过车窗' },
    { time: 4, text: '我们驰骋在银河之上' },
    { time: 10, text: '引擎低鸣像光的延长' },
    { time: 16, text: '下一站是未命名远方' },
    { time: 22, text: '把烦恼折成纸飞机' },
    { time: 28, text: '丢进黑洞让时间静止' },
    { time: 34, text: '若相遇本就是真理' },
    { time: 40, text: '那我愿为你一再启程' },
    { time: 48, text: '星途列车 永不返航' },
    { time: 56, text: '载着心跳 穿过漫长' },
  ],
  '蓝调霓虹': [
    { time: 0, text: '雨落在午夜的城市' },
    { time: 5, text: '霓虹在积水里低语' },
    { time: 11, text: '影子被路灯拉得很长' },
    { time: 17, text: '像一首没写完的蓝调' },
    { time: 23, text: '你转身带走整个夏天' },
    { time: 29, text: '留我在节点里失眠' },
    { time: 35, text: '若能再给我一个和弦' },
    { time: 41, text: '我会反复把爱重演' },
  ],
  '深海回响': [
    { time: 0, text: '潜进没有边界的蓝' },
    { time: 6, text: '光在这里失了方向' },
    { time: 12, text: '鱼群划开沉默的海' },
    { time: 18, text: '气泡托起你的模样' },
    { time: 24, text: '听的见吗 我的回响' },
    { time: 30, text: '穿过洋流 抵达远方' },
    { time: 36, text: '别害怕 深处的微光' },
    { time: 42, text: '会指引我们 慢慢相认' },
  ],
}

// 演示曲目列表（与上方 DEMO_LYRICS 通过 lyricsKey 字段关联）
export const DEMO_TRACKS = [
  {
    id: 'demo-xltl',
    title: '星途列车',
    artist: '星途乐队',
    album: '银河漫游指南',
    category: '轻音乐',
    duration: 225,
    cover: '',
    lyricsKey: '星途列车',
  },
  {
    id: 'demo-ldns',
    title: '蓝调霓虹',
    artist: '南巷歌手',
    album: '午夜城市频道',
    category: '流行',
    duration: 192,
    cover: '',
    lyricsKey: '蓝调霓虹',
  },
  {
    id: 'demo-shhx',
    title: '深海回响',
    artist: '潮汐胶片',
    album: '深海观测日志',
    category: '纯音乐',
    duration: 268,
    cover: '',
    lyricsKey: '深海回响',
  },
]
