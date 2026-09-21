#!/usr/bin/env node
/**
 * 修复知识库 21 条原始条目的中文乱码
 * 通过 REST API 重新写入正确的 UTF-8 内容（绕过 SQL 执行的编码问题）
 *
 * 策略：
 *   1. 读 0001 的 INSERT 取初始 content
 *   2. 读 0002~0004c 的 UPDATE 覆盖 content（最后一次生效）
 *   3. 通过 PATCH /rest/v1/kb_entries?slug=eq.xxx 更新
 */
const fs = require('fs')
const path = require('path')

const SUPABASE_URL = process.env.SUPABASE_URL || 'https://nyzpglrmxltsirxmtkjo.supabase.co'
// Secret key 禁止硬编码进仓库，运行前在环境变量中设置：
//   PowerShell: $env:SUPABASE_SERVICE_KEY = "sb_secret_xxx"
const SERVICE_KEY = process.env.SUPABASE_SERVICE_KEY || ''
const TABLE = 'kb_entries'

const SQL_DIR = 'f:/project/my-blog/web3-blog/supabase'

/** 从 INSERT 语句中提取 slug → content 映射 */
function parseInserts(sql) {
  const result = {}
  // INSERT 格式：...('title', 'slug', ..., $md$content$md$, ...
  const parts = sql.split('$md$')
  for (let p = 0; p < parts.length - 1; p += 2) {
    const prefix = parts[p]
    const content = parts[p + 1]
    // 从 prefix 提取 slug：找最后一个 ,'slug', 模式
    const slugMatch = prefix.match(/'([a-z0-9-]+)',\s*$/m)
    if (slugMatch) {
      result[slugMatch[1]] = content
    }
  }
  return result
}

/** 从 UPDATE 语句中提取 slug → content 映射 */
function parseUpdates(sql) {
  const result = {}
  // UPDATE 格式：update ... set content = $md$content$md$ where slug = 'xxx';
  // split('$md$') 后：奇数索引=content，偶数索引=prefix/suffix
  const parts = sql.split('$md$')
  for (let p = 1; p < parts.length - 1; p += 2) {
    const content = parts[p]           // 奇数索引是 $md$ 之间的内容
    const suffix = parts[p + 1]       // 下一个偶数索引是 closing $md$ 后的部分
    const slugMatch = suffix.match(/where\s+slug\s*=\s*'([^']+)'/i)
    if (slugMatch) {
      result[slugMatch[1]] = content
    }
  }
  return result
}

async function main() {
  // 1. 读 0001 的 INSERT
  const sql1 = fs.readFileSync(path.join(SQL_DIR, '0001_knowledge_and_blog_seed.sql'), 'utf-8')
  let contentMap = parseInserts(sql1)
  console.log(`[INFO] 0001 INSERT: ${Object.keys(contentMap).length} 条`)

  // 2. 依次读 0002~0004c 的 UPDATE，覆盖
  const updateFiles = [
    '0002_kb_expand_content.sql',
    '0003_kb_expand_more.sql',
    '0004a_kb_expand.sql',
    '0004b_kb_expand.sql',
    '0004c_kb_expand.sql',
  ]
  for (const f of updateFiles) {
    const fp = path.join(SQL_DIR, f)
    if (!fs.existsSync(fp)) continue
    const sql = fs.readFileSync(fp, 'utf-8')
    const updates = parseUpdates(sql)
    console.log(`[INFO] ${f} UPDATE: ${Object.keys(updates).length} 条`)
    for (const [slug, content] of Object.entries(updates)) {
      contentMap[slug] = content
    }
  }

  console.log(`[INFO] 最终需更新: ${Object.keys(contentMap).length} 条`)

  // 3. 通过 REST API 更新
  let ok = 0, fail = 0
  for (const [slug, content] of Object.entries(contentMap)) {
    try {
      const res = await fetch(
        `${SUPABASE_URL}/rest/v1/${TABLE}?slug=eq.${encodeURIComponent(slug)}`,
        {
          method: 'PATCH',
          headers: {
            'apikey': SERVICE_KEY,
            'Authorization': `Bearer ${SERVICE_KEY}`,
            'Content-Type': 'application/json',
            'Prefer': 'return=minimal',
          },
          body: JSON.stringify({ content }),
        }
      )
      if (res.ok) {
        ok++
        console.log(`[OK] ${slug}`)
      } else {
        const text = await res.text()
        fail++
        console.error(`[FAIL] ${slug} (${res.status}): ${text.slice(0, 200)}`)
      }
    } catch (e) {
      fail++
      console.error(`[ERR] ${slug}: ${e.message}`)
    }
  }
  console.log(`\n[DONE] 成功 ${ok} 条，失败 ${fail} 条`)
}

main().catch(e => { console.error(e); process.exit(1) })
