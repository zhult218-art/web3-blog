// ====================================================
// VIP 视频解析线路数据源
// 提取自 https://github.com/zhult218-art/video_vip (video_vip.user.js v3.2)
// 全部为 iframe 型：把视频页（或视频直链）URL 拼到接口 url 后用 iframe 嵌入播放。
// 「无水印播放器」来自 wsyzy 站点，接收的是 m3u8 直链（P2P 加速去水印），
// 不是 VIP 网页解析接口，故归入 HLS 分类并给出输入提示。
// 线路对象结构：{ name, url, category: recommend|cloud|hls|backup, direct?, note? }
// direct: true 表示该线路只接收 m3u8 直链，不做视频网页解析。
// ====================================================

export const vipSources = [
  // ── 推荐线路（优先尝试）──
  { name: '虾米解析', url: 'https://jx.xmflv.com/?url=', category: 'recommend' },
  { name: 'TXNQ解析', url: 'https://bfq.txnp.cn/player?url=', category: 'recommend' },
  { name: '789解析', url: 'https://jiexi.789jiexi.icu:4433/?url=', category: 'recommend' },

  // ── 云解析（云端转码，画质较好）──
  { name: '七七云解析', url: 'https://jx.77flv.cc/?url=', category: 'cloud' },
  { name: '邦宁云解析', url: 'https://video.isyour.love/player/getplayer?url=', category: 'cloud' },

  // ── HLS / 直链专项（适合 m3u8 源）──
  { name: 'playm3u8', url: 'https://www.playm3u8.cn/jiexi.php?url=', category: 'hls' },
  { name: 'HLS解析', url: 'https://jx.hls.one/?url=', category: 'hls' },
  { name: '极速解析', url: 'https://jx.2s0.cn/player/?url=', category: 'hls' },
  { name: '无水印播放器', url: 'https://wsyzy.vip/m3u8/?url=', category: 'hls',
    direct: true, note: 'P2P 加速去水印，需粘贴 m3u8 直链' },

  // ── 备用线路（主线路失效时轮换）──
  { name: 'fongmi解析', url: 'https://json.fongmi.cc/web?url=', category: 'backup' },
  { name: '冰豆解析', url: 'https://bd.jx.cn/?url=', category: 'backup' },
  { name: 'CK解析', url: 'https://www.ckplayer.vip/jiexi/?url=', category: 'backup' },
  { name: '花旗解析', url: 'https://www.huaqi.live/?url=', category: 'backup' },
  { name: 'Player-JY', url: 'https://jx.playerjy.com/?url=', category: 'backup' },
  { name: 'Yparse', url: 'https://jx.yparse.com/index.php?url=', category: 'backup' },
  { name: '麒麟解析', url: 'https://free.maccms.xyz/?url=', category: 'backup' },
]

export const vipCategories = [
  { key: 'recommend', label: '推荐线路', desc: '稳定常用，优先尝试' },
  { key: 'cloud', label: '云解析', desc: '云端转码，画质较好' },
  { key: 'hls', label: 'HLS / 直链', desc: '适合 m3u8 流媒体源' },
  { key: 'backup', label: '备用线路', desc: '主线路失效时轮换' },
]

// 支持解析的主流视频平台（用于输入框提示与快速跳转）
export const vipPlatforms = [
  { name: '爱奇艺', host: 'iqiyi.com', icon: '🥝' },
  { name: '腾讯视频', host: 'v.qq.com', icon: '🐧' },
  { name: '优酷', host: 'youku.com', icon: '📺' },
  { name: '芒果TV', host: 'mgtv.com', icon: '🥭' },
  { name: '哔哩哔哩', host: 'bilibili.com', icon: '📼' },
  { name: '搜狐视频', host: 'sohu.com', icon: '🎬' },
  { name: '乐视', host: 'le.com', icon: '🎞️' },
  { name: 'PPTV', host: 'pptv.com', icon: '🎥' },
]
