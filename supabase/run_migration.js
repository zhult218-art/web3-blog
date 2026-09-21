#!/usr/bin/env node
/**
 * Supabase SQL 执行脚本
 * 通过 PostgREST REST API + service_role key 直接向 kb_entries 表插入数据
 * 用法：node supabase/run_migration.js supabase/0005_kb_fill_gaps.sql
 */
const fs = require('fs')
const path = require('path')

const SUPABASE_URL = process.env.SUPABASE_URL || 'https://nyzpglrmxltsirxmtkjo.supabase.co'
// Secret key 禁止硬编码进仓库，运行前在环境变量中设置：
//   PowerShell: $env:SUPABASE_SERVICE_KEY = "sb_secret_xxx"
const SERVICE_KEY = process.env.SUPABASE_SERVICE_KEY || ''
const TABLE = 'kb_entries'

/**
 * 解析 SQL VALUES，提取每个元组的字段
 * 格式：('title', 'slug', 'summary', $md$content$md$, 'category', 'subcategory', array['t1','t2'], 'stage', 'source', difficulty)
 */
function parseInserts(sql) {
  const rows = []
  // 找到 values 关键字后的内容
  const valuesMatch = sql.match(/values\s*/is)
  if (!valuesMatch) throw new Error('找不到 values 关键字')
  let body = sql.slice(valuesMatch.index + valuesMatch[0].length)
  // 去掉末尾的 on conflict ... ; 部分
  const conflictIdx = body.toLowerCase().indexOf('on conflict')
  if (conflictIdx >= 0) body = body.slice(0, conflictIdx)

  // 用正则提取每个元组（$md$ 作为锚点）
  // 每个元组结构：('title', 'slug',\n 'summary',\n $md$content$md$,\n 'cat', 'sub', array[...], 'stage', 'source', num)
  // 用 $md$ 分割
  const parts = body.split('$md$')
  // parts 结构：[prefix, content1, between1, content2, between2, ..., contentN, suffix]
  // prefix 包含 ('title', 'slug',\n 'summary',\n
  // between 包含 ,\n 'cat', 'sub', array[...], 'stage', 'source', num),\n\n('title2', ...
  for (let p = 0; p < parts.length - 1; p += 2) {
    const prefix = parts[p]       // ('title', 'slug', 'summary',
    const content = parts[p + 1]   // markdown content
    const between = p + 2 < parts.length ? parts[p + 2] : '' // , 'cat', 'sub', array[...], 'stage', 'source', num)

    // 从 prefix 提取 title, slug, summary
    // prefix 格式：...('title', 'slug',\n 'summary多行',\n
    const prefixMatch = prefix.match(/\(\s*'((?:[^'\\]|\\.)*)'\s*,\s*'((?:[^'\\]|\\.)*)'\s*,\s*'((?:[^'\\]|\\.)*)'\s*,?\s*$/s)
    if (!prefixMatch) {
      // 可能是第一个元组或格式不同
      continue
    }
    const title = unescapeSql(prefixMatch[1])
    const slug = unescapeSql(prefixMatch[2])
    const summary = unescapeSql(prefixMatch[3])

    // 从 between 提取 category, subcategory, tags, stage, source, difficulty
    // between 格式：,\n 'cat', 'sub', array['t1','t2'], 'stage', 'source', 3),\n\n...
    const betweenMatch = between.match(/^\s*,\s*'((?:[^'\\]|\\.)*)'\s*,\s*'((?:[^'\\]|\\.)*)'\s*,\s*array\[([^\]]*)\]\s*,\s*'((?:[^'\\]|\\.)*)'\s*,\s*'((?:[^'\\]|\\.)*)'\s*,\s*(\d+)\s*\)/s)
    if (!betweenMatch) {
      console.error(`[WARN] 无法解析 between 部分（slug=${slug}）：${between.slice(0, 100)}...`)
      continue
    }
    const category = unescapeSql(betweenMatch[1])
    const subcategory = unescapeSql(betweenMatch[2])
    const tagsStr = betweenMatch[3]
    const tags = tagsStr.split(',').map(t => t.trim().replace(/^'|'$/g, '')).filter(Boolean)
    const stage = unescapeSql(betweenMatch[4])
    const source = unescapeSql(betweenMatch[5])
    const difficulty = parseInt(betweenMatch[6], 10)

    rows.push({ title, slug, summary, content, category, subcategory, tags, stage, source, difficulty })
  }
  return rows
}

function unescapeSql(s) {
  return s.replace(/\\'/g, "'").replace(/\\\\/g, '\\').replace(/''/g, "'")
}

async function main() {
  const sqlFile = process.argv[2] || 'supabase/0005_kb_fill_gaps.sql'
  const sqlPath = path.resolve(sqlFile)
  console.log(`[INFO] 读取 SQL 文件：${sqlPath}`)
  const sql = fs.readFileSync(sqlPath, 'utf-8')

  const rows = parseInserts(sql)
  console.log(`[INFO] 解析出 ${rows.length} 条记录`)

  let ok = 0, fail = 0
  for (const row of rows) {
    try {
      const res = await fetch(`${SUPABASE_URL}/rest/v1/${TABLE}`, {
        method: 'POST',
        headers: {
          'apikey': SERVICE_KEY,
          'Authorization': `Bearer ${SERVICE_KEY}`,
          'Content-Type': 'application/json',
          'Prefer': 'return=minimal,resolution=ignore-duplicates',
        },
        body: JSON.stringify({
          title: row.title,
          slug: row.slug,
          summary: row.summary,
          content: row.content,
          category: row.category,
          subcategory: row.subcategory,
          tags: row.tags,
          stage: row.stage,
          source: row.source,
          difficulty: row.difficulty,
          published: true,
        }),
      })
      if (res.ok || res.status === 409) {
        ok++
        console.log(`[OK] ${row.slug} (${res.status})`)
      } else {
        const text = await res.text()
        fail++
        console.error(`[FAIL] ${row.slug} (${res.status}): ${text.slice(0, 200)}`)
      }
    } catch (e) {
      fail++
      console.error(`[ERR] ${row.slug}: ${e.message}`)
    }
  }
  console.log(`\n[DONE] 成功 ${ok} 条，失败 ${fail} 条`)
}

main().catch(e => { console.error(e); process.exit(1) })
