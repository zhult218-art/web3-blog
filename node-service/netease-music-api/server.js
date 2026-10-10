// ============================================================
// 网易云音乐 API 代理启动器（NeteaseCloudMusicApi）
// 由 gateway /netease/** 路由转发到本机 3000 端口
// 启动：node server.js
// ============================================================
const { serveNcmApi } = require('NeteaseCloudMusicApi')

serveNcmApi({ port: 3000 }).then(() => {
  console.log('[netease] NeteaseCloudMusicApi listening on http://localhost:3000')
})