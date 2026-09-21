// Cloudflare Pages 单文件 ≤ 25MB，构建后清理超限资源
// 主要清除：public/vosk/cn.tar.gz（41.8MB 语音模型离线包）
// 部署到非 Cloudflare Pages 时，这些资源仍在 public/ 里，本地/自建部署不受影响
import { promises as fs } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const dist = path.resolve(__dirname, '..', 'dist')
const LIMIT = 25 * 1024 * 1024

const remove = async (p) => {
  try {
    const stat = await fs.stat(p)
    if (stat.isDirectory()) {
      await fs.rm(p, { recursive: true, force: true })
      console.log(`  ✂️  删除目录  ${path.relative(dist, p)}/  (${(stat.size / 1024).toFixed(1)} KB)`)
    } else {
      await fs.unlink(p)
      console.log(`  ✂️  删除文件  ${path.relative(dist, p)}  (${(stat.size / 1024 / 1024).toFixed(1)} MB)`)
    }
  } catch { /* 已不存在或权限不足 */ }
}

const walk = async (dir) => {
  const items = await fs.readdir(dir, { withFileTypes: true })
  for (const item of items) {
    const full = path.join(dir, item.name)
    if (item.isDirectory()) await walk(full)
    else {
      try {
        const stat = await fs.stat(full)
        if (stat.size > LIMIT) await remove(full)
      } catch { /* skip */ }
    }
  }
}

// 1) 显式删除 vosk 目录（已知 41.8MB 的 cn.tar.gz）
const voskDir = path.join(dist, 'vosk')
if (await fs.access(voskDir).then(() => true).catch(() => false)) {
  await remove(voskDir)
}

// 2) 兜底扫描 dist 下所有 >25MB 的文件
await walk(dist)

console.log('✅ 大文件清理完成（Cloudflare Pages ≤ 25MB）')
