// ============================================================
// 网易云音乐 API 代理启动器
// 1. NeteaseCloudMusicApi（逆向 API，搜索/歌曲URL/歌词/歌单）— 已可用
// 2. 网易云开放平台官方 API（RSA_SHA256 签名）— 签名已通过，clientIp 待解决
// 由 gateway /netease/** 路由转发到本机 3000 端口
// 启动：node server.js
// ============================================================
const fs = require('fs')
const path = require('path')
const crypto = require('crypto')
const https = require('https')

// ── 加载 .env（从项目根目录） ──
const envPath = path.resolve(__dirname, '../../.env')
if (fs.existsSync(envPath)) {
  fs.readFileSync(envPath, 'utf8').split('\n').forEach(line => {
    const m = line.match(/^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)\s*$/)
    if (m && !process.env[m[1]]) process.env[m[1]] = m[2].replace(/^["']|["']$/g, '')
  })
}

const APP_ID = process.env.NETEASE_APP_ID || ''
const APP_SECRET = process.env.NETEASE_APP_SECRET || ''
const PUB_KEY = process.env.NETEASE_PUBKEY || ''
// 开放平台 RSA 私钥（.env 中存裸 Base64，此处自动包装为 PEM）
const PRIV_KEY_RAW = process.env.NETEASE_PRIVATE_KEY || ''
const PRIVATE_KEY = PRIV_KEY_RAW.startsWith('-----BEGIN')
  ? PRIV_KEY_RAW
  : (PRIV_KEY_RAW
    ? `-----BEGIN PRIVATE KEY-----\n${PRIV_KEY_RAW.match(/.{1,64}/g).join('\n')}\n-----END PRIVATE KEY-----`
    : '')
const OPEN_API_BASE = 'https://openapi.music.163.com'

// ── 开放平台签名工具 ──
// RSA_SHA256 签名：对所有参数（除 sign、clientIp 外）按 key 排序拼接后签名
function signOpenApi(params) {
  // 排除 sign 和 clientIp（平台不从签名中读取 clientIp）
  const signParams = Object.keys(params)
    .filter(k => k !== 'sign' && k !== 'clientIp')
    .sort()
    .map(k => `${k}=${params[k]}`)
    .join('&')

  const signer = crypto.createSign('SHA256')
  signer.update(signParams)
  const sign = signer.sign({
    key: PRIVATE_KEY,
    padding: crypto.constants.RSA_PKCS1_PADDING,
  }, 'base64')

  return sign
}

function httpsGet(url, timeout = 10000) {
  return new Promise((resolve, reject) => {
    const r = https.get(url, { timeout }, resp => {
      let data = ''
      resp.on('data', chunk => data += chunk)
      resp.on('end', () => resolve({ status: resp.statusCode, body: data }))
    })
    r.on('error', reject)
    r.on('timeout', () => reject(new Error('timeout')))
  })
}

// 检查开放平台是否已配置（需要 privateKey）
function isOpenPlatformReady() {
  return !!(APP_ID && APP_SECRET && PRIVATE_KEY)
}

// 调用开放平台 API
async function callOpenApi(apiPath, bizContentObj, clientIp) {
  const timestamp = Date.now().toString()
  const nonce = crypto.randomBytes(8).toString('hex')
  const bizContent = JSON.stringify(bizContentObj)

  const params = {
    appId: APP_ID,
    appSecret: APP_SECRET,
    bizContent,
    os: 'pc',
    channel: 'web',
    brand: 'web',
    device: '{}',
    nonce,
    signType: 'RSA_SHA256',
    timestamp,
  }

  const sign = signOpenApi(params)

  // 构建 URL（URLSearchParams 会自动 URL-encode）
  const sp = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => sp.set(k, String(v)))
  sp.set('sign', sign)
  if (clientIp) sp.set('clientIp', clientIp)

  const url = OPEN_API_BASE + apiPath + '?' + sp.toString()
  return httpsGet(url)
}

// ── 启动 NeteaseCloudMusicApi 并追加开放平台端点 ──
const { serveNcmApi } = require('NeteaseCloudMusicApi')

async function main() {
  const app = await serveNcmApi({ port: 3000 })
  console.log('[netease] NeteaseCloudMusicApi listening on http://localhost:3000')

  // ── 开放平台端点（前缀 /ext/ 避免与逆向 API 路由冲突） ──

  // 凭证状态
  app.get('/ext/status', (req, res) => {
    res.json({
      openPlatformConfigured: isOpenPlatformReady(),
      appId: APP_ID ? APP_ID.slice(0, 8) + '...' : null,
      pubKeyRegistered: !!PUB_KEY,
      privateKeyConfigured: !!PRIVATE_KEY,
      message: !isOpenPlatformReady()
        ? '开放平台未就绪：需要 NETEASE_PRIVATE_KEY（RSA 私钥）。逆向 API 已正常工作。'
        : '开放平台已配置（签名通过）。平台返回缺少 clientIp 参数，可能需要在开发者控制台配置或获取 accessToken。',
    })
  })

  // 开放平台搜索
  app.get('/ext/search', async (req, res) => {
    if (!isOpenPlatformReady()) {
      return res.status(503).json({ code: 503, message: '开放平台未配置 privateKey，请使用逆向 API /search?keywords=...' })
    }
    try {
      const { keywords, limit = 30, offset = 0, type = 1 } = req.query
      if (!keywords) return res.status(400).json({ code: 400, message: 'keywords required' })

      const result = await callOpenApi(
        '/openapi/music/basic/search/song/get/v2',
        { s: String(keywords), type: Number(type), limit: Number(limit), offset: Number(offset) },
        req.ip
      )
      try { res.json(JSON.parse(result.body)) }
      catch { res.status(result.status).send(result.body) }
    } catch (err) {
      res.status(500).json({ code: 500, message: err.message })
    }
  })

  // 开放平台获取歌曲 URL
  app.get('/ext/song/url', async (req, res) => {
    if (!isOpenPlatformReady()) {
      return res.status(503).json({ code: 503, message: '开放平台未配置 privateKey，请使用逆向 API /song/url?id=...' })
    }
    try {
      const { ids, br = 320000 } = req.query
      if (!ids) return res.status(400).json({ code: 400, message: 'ids required' })

      const result = await callOpenApi(
        '/openapi/music/basic/song/url/get/v2',
        { songIds: String(ids), br: Number(br) },
        req.ip
      )
      try { res.json(JSON.parse(result.body)) }
      catch { res.status(result.status).send(result.body) }
    } catch (err) {
      res.status(500).json({ code: 500, message: err.message })
    }
  })

  // 开放平台获取歌词
  app.get('/ext/lyric', async (req, res) => {
    if (!isOpenPlatformReady()) {
      return res.status(503).json({ code: 503, message: '开放平台未配置 privateKey，请使用逆向 API /lyric?id=...' })
    }
    try {
      const { id } = req.query
      if (!id) return res.status(400).json({ code: 400, message: 'id required' })

      const result = await callOpenApi(
        '/openapi/music/basic/song/lyric/get/v2',
        { songId: String(id) },
        req.ip
      )
      try { res.json(JSON.parse(result.body)) }
      catch { res.status(result.status).send(result.body) }
    } catch (err) {
      res.status(500).json({ code: 500, message: err.message })
    }
  })

  // 开放平台获取歌单详情
  app.get('/ext/playlist/detail', async (req, res) => {
    if (!isOpenPlatformReady()) {
      return res.status(503).json({ code: 503, message: '开放平台未配置 privateKey，请使用逆向 API /playlist/detail?id=...' })
    }
    try {
      const { id } = req.query
      if (!id) return res.status(400).json({ code: 400, message: 'id required' })

      const result = await callOpenApi(
        '/openapi/music/basic/playlist/detail/get/v2',
        { id: String(id) },
        req.ip
      )
      try { res.json(JSON.parse(result.body)) }
      catch { res.status(result.status).send(result.body) }
    } catch (err) {
      res.status(500).json({ code: 500, message: err.message })
    }
  })

  console.log(`[netease] Open Platform: ${isOpenPlatformReady() ? 'READY (sign OK, clientIp pending)' : 'NOT READY (need NETEASE_PRIVATE_KEY)'}`)
  console.log('[netease] Open Platform endpoints: /ext/status, /ext/search, /ext/song/url, /ext/lyric, /ext/playlist/detail')
  console.log('[netease] Reverse API (working): /search, /song/url, /lyric, /playlist/detail')
}

main().catch(err => {
  console.error('[netease] Failed to start:', err)
  process.exit(1)
})
