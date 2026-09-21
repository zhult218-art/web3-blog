// ============================================================
// musicFixes —— 本地曲库 netease_id 纠正表
// Supabase music_tracks 中部分曲目的 netease_id 与真实歌曲不符，
// 导致点击「走马」等歌曲时取流为空/串歌；源数据暂时无法更新，
// 因此在数据加载层按曲目名纠正（音乐馆与详情页共用，保证一致）。
// ============================================================
export const NETEASE_ID_FIX = {
  走马: 30431367,       // 陈粒
  理想三旬: 31445772,   // 陈鸿宇
  平凡之路: 28815250,   // 朴树
  南山南: 29715551,     // 马頔
  光年之外: 449818741, // G.E.M.邓紫棋
}

// 返回纠正后的网易云 id（无纠正项时保留原值）
export function fixNeteaseId(track) {
  const fixed = NETEASE_ID_FIX[track?.title]
  return fixed ? String(fixed) : String(track?.netease_id || track?.neteaseId || '')
}
