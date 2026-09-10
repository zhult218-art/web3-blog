// ============================================================
// 背景壁纸配置（分组：动态壁纸 / 天空之城 / 哲风精选 / Bing 光影 / 精选）
// 文件位于 frontend/public/wallpapers/
// 图片壁纸：body.has-wallpaper + --wallpaper-img CSS 变量应用
// 视频壁纸：App.vue #bg-layer 内渲染 <video>（poster 兜底 + 智能播放算法）
// ============================================================

export const WALLPAPER_GROUPS = [
  { id: 'dyn', name: '▶ 动态壁纸' },
  { id: 'ghibli', name: '☁ 天空之城' },
  { id: 'haowallpaper', name: '✦ 哲风·云与夜' },
  { id: 'bing', name: '◈ Bing 光影' },
  { id: 'extras', name: '◇ 精选' },
]

const GRP = {
  dyn: 'dyn',
  ghibli: 'ghibli',
  haw: 'haowallpaper',
  bing: 'bing',
  extra: 'extras',
}

export const WALLPAPERS = [
  // ── 动态壁纸（1080p30 低码率 h264 硬解，poster 首帧兜底）──
  { id: 'dyn-01', name: '灵动 01', video: '/wallpapers/dyn-01.mp4', poster: '/wallpapers/dyn-01.jpg', desc: '动态壁纸 · 01', group: GRP.dyn },
  { id: 'dyn-02', name: '灵动 02', video: '/wallpapers/dyn-02.mp4', poster: '/wallpapers/dyn-02.jpg', desc: '动态壁纸 · 02', group: GRP.dyn },
  { id: 'dyn-03', name: '灵动 03', video: '/wallpapers/dyn-03.mp4', poster: '/wallpapers/dyn-03.jpg', desc: '动态壁纸 · 03', group: GRP.dyn },
  { id: 'dyn-04', name: '灵动 04', video: '/wallpapers/dyn-04.mp4', poster: '/wallpapers/dyn-04.jpg', desc: '动态壁纸 · 04', group: GRP.dyn },
  // ── 天空之城（宫崎骏 Laputa）──
  { id: 'w-none', name: '无壁纸', file: '', desc: '纯色光晕背景', group: GRP.ghibli },
  { id: 'laputa-01', name: '天空之城 01', file: '/wallpapers/laputa-001.jpg', desc: 'Laputa · 云海浮岛', group: GRP.ghibli },
  { id: 'laputa-02', name: '天空之城 02', file: '/wallpapers/laputa-002.jpg', desc: 'Laputa · 云海浮岛', group: GRP.ghibli },
  { id: 'laputa-03', name: '天空之城 03', file: '/wallpapers/laputa-003.jpg', desc: 'Laputa · 云海浮岛', group: GRP.ghibli },
  { id: 'laputa-04', name: '天空之城 04', file: '/wallpapers/laputa-004.jpg', desc: 'Laputa · 云海浮岛', group: GRP.ghibli },
  { id: 'laputa-05', name: '天空之城 05', file: '/wallpapers/laputa-005.jpg', desc: 'Laputa · 云海浮岛', group: GRP.ghibli },
  { id: 'laputa-06', name: '天空之城 06', file: '/wallpapers/laputa-006.jpg', desc: 'Laputa · 云海浮岛', group: GRP.ghibli },
  { id: 'laputa-07', name: '天空之城 07', file: '/wallpapers/laputa-007.jpg', desc: 'Laputa · 云海浮岛', group: GRP.ghibli },
  { id: 'laputa-08', name: '天空之城 08', file: '/wallpapers/laputa-008.jpg', desc: 'Laputa · 云海浮岛', group: GRP.ghibli },
  // ── 哲风精选（云海 / 黄昏 / 夜色）──
  { id: 'hw-01', name: '蜡笔新·黄昏', file: '/wallpapers/hw-01.jpg', desc: '蜡笔小新 · 黄昏路灯', group: GRP.haw },
  { id: 'hw-02', name: '天空·动漫云', file: '/wallpapers/hw-02.jpg', desc: '4K 云朵 · 动漫天空', group: GRP.haw },
  { id: 'hw-03', name: '云海·彗星', file: '/wallpapers/hw-03.jpg', desc: '4K 云朵 · 山峦彗星', group: GRP.haw },
  { id: 'hw-04', name: '云舟·天空', file: '/wallpapers/hw-04.jpg', desc: '云朵动画壁纸 · 天空', group: GRP.haw },
  { id: 'hw-05', name: '云夜·星海', file: '/wallpapers/hw-05.jpg', desc: '4K 云团 · 夜空星辰', group: GRP.haw },
  { id: 'hw-06', name: '暮色·山湖', file: '/wallpapers/hw-06.jpg', desc: '4K 傍晚 · 山脉湖泊', group: GRP.haw },
  { id: 'hw-07', name: '星辰·海浪', file: '/wallpapers/hw-07.jpg', desc: '4K 夜晚 · 星空浪涛', group: GRP.haw },
  // ── Bing 光影（1920×1080）──
  { id: 'w-07', name: '天穹流星', file: '/wallpapers/wallpaper-07.jpg', desc: '泰德天文台 · 夜空', group: GRP.bing },
  { id: 'w-01', name: '山巅晨光', file: '/wallpapers/wallpaper-01.jpg', desc: '怀特克利夫公园', group: GRP.bing },
  { id: 'w-05', name: '红岩奇柱', file: '/wallpapers/wallpaper-05.jpg', desc: '新墨西哥荒野', group: GRP.bing },
  { id: 'w-02', name: '星形要塞', file: '/wallpapers/wallpaper-02.jpg', desc: '帕尔马诺瓦航拍', group: GRP.bing },
  { id: 'w-04', name: '绿野修道院', file: '/wallpapers/wallpaper-04.jpg', desc: '爱尔兰遗址', group: GRP.bing },
  { id: 'w-03', name: '珊瑚海', file: '/wallpapers/wallpaper-03.jpg', desc: '海葵小丑鱼', group: GRP.bing },
  { id: 'w-06', name: '林间通道', file: '/wallpapers/wallpaper-06.jpg', desc: '野生动物通道', group: GRP.bing },
  { id: 'w-08', name: '草原象群', file: '/wallpapers/wallpaper-08.jpg', desc: '肯尼亚草原', group: GRP.bing },
  // ── 精选 ──
  { id: 'x-10', name: '精选 A', file: '/wallpapers/extra-cand10.webp', desc: '精选', group: GRP.extra },
  { id: 'x-11', name: '精选 B', file: '/wallpapers/extra-cand11.webp', desc: '精选', group: GRP.extra },
  { id: 'x-12', name: '精选 C', file: '/wallpapers/extra-cand12.webp', desc: '精选', group: GRP.extra },
]

export const DEFAULT_WALLPAPER = 'dyn-01'