// ============================================================
// 轻量 Markdown 渲染器（离线可用）
// 支持：标题 / 粗体 / 斜体 / 行内代码 / 代码块 / 引用 / 列表 / 链接 / 图片 / 表格 / 分隔线
// 输出 HTML 字符串，配合 v-html 使用；代码块使用 highlight.js 做语法高亮
// ============================================================
import hljs from 'highlight.js/lib/core'
import javascript from 'highlight.js/lib/languages/javascript'
import typescript from 'highlight.js/lib/languages/typescript'
import python from 'highlight.js/lib/languages/python'
import java from 'highlight.js/lib/languages/java'
import xml from 'highlight.js/lib/languages/xml'
import css from 'highlight.js/lib/languages/css'
import json from 'highlight.js/lib/languages/json'
import bash from 'highlight.js/lib/languages/bash'
import sql from 'highlight.js/lib/languages/sql'
import yaml from 'highlight.js/lib/languages/yaml'
import markdown from 'highlight.js/lib/languages/markdown'
import c from 'highlight.js/lib/languages/c'
import cpp from 'highlight.js/lib/languages/cpp'
import csharp from 'highlight.js/lib/languages/csharp'
import go from 'highlight.js/lib/languages/go'
import rust from 'highlight.js/lib/languages/rust'
import php from 'highlight.js/lib/languages/php'
import ruby from 'highlight.js/lib/languages/ruby'
import shell from 'highlight.js/lib/languages/shell'
import dockerfile from 'highlight.js/lib/languages/dockerfile'
import nginx from 'highlight.js/lib/languages/nginx'
import plaintext from 'highlight.js/lib/languages/plaintext'

hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('js', javascript)
hljs.registerLanguage('typescript', typescript)
hljs.registerLanguage('ts', typescript)
hljs.registerLanguage('python', python)
hljs.registerLanguage('py', python)
hljs.registerLanguage('java', java)
hljs.registerLanguage('xml', xml)
hljs.registerLanguage('html', xml)
hljs.registerLanguage('css', css)
hljs.registerLanguage('json', json)
hljs.registerLanguage('bash', bash)
hljs.registerLanguage('sh', bash)
hljs.registerLanguage('sql', sql)
hljs.registerLanguage('yaml', yaml)
hljs.registerLanguage('yml', yaml)
hljs.registerLanguage('markdown', markdown)
hljs.registerLanguage('md', markdown)
hljs.registerLanguage('c', c)
hljs.registerLanguage('cpp', cpp)
hljs.registerLanguage('c++', cpp)
hljs.registerLanguage('csharp', csharp)
hljs.registerLanguage('cs', csharp)
hljs.registerLanguage('go', go)
hljs.registerLanguage('rust', rust)
hljs.registerLanguage('rs', rust)
hljs.registerLanguage('php', php)
hljs.registerLanguage('ruby', ruby)
hljs.registerLanguage('rb', ruby)
hljs.registerLanguage('shell', shell)
hljs.registerLanguage('dockerfile', dockerfile)
hljs.registerLanguage('nginx', nginx)
hljs.registerLanguage('plaintext', plaintext)

function escapeHtml(s) {
  return String(s ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

// 行内元素：粗体/斜体/行内代码/链接/图片
function renderInline(text) {
  let t = escapeHtml(text)
  t = t.replace(/!\[([^\]]*)\]\(([^)]+)\)/g, (m, alt, src) => `<img src="${src}" alt="${alt}" loading="lazy">`)
  t = t.replace(/\[([^\]]+)\]\(([^)]+)\)/g, (m, label, href) => {
    const isExternal = /^https?:\/\//.test(href)
    return `<a href="${href}"${isExternal ? ' target="_blank" rel="noopener"' : ''}>${label}</a>`
  })
  t = t.replace(/`([^`]+)`/g, '<code>$1</code>')
  t = t.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
  t = t.replace(/\*([^*]+)\*/g, '<em>$1</em>')
  return t
}

// 代码块提取（带语言标记）：```lang\n...\n```
function splitCodeBlocks(src) {
  const parts = []
  const regex = /```([\w-]*)\n([\s\S]*?)```/g
  let last = 0
  let m
  while ((m = regex.exec(src)) !== null) {
    if (m.index > last) parts.push({ type: 'text', content: src.slice(last, m.index) })
    parts.push({ type: 'code', lang: m[1] || '', content: m[2].replace(/\n$/, '') })
    last = regex.lastIndex
  }
  if (last < src.length) parts.push({ type: 'text', content: src.slice(last) })
  return parts
}

function renderTable(rows) {
  const headers = rows[0].split('|').map(c => c.trim()).filter(Boolean)
  const body = rows.slice(1).filter(r => r.trim() && !/^\s*:?-{3,}:?\s*$/.test(r.trim()))
  let html = '<div class="table-wrap"><table><thead><tr>'
  headers.forEach(h => { html += `<th>${renderInline(h)}</th>` })
  html += '</tr></thead><tbody>'
  body.forEach(row => {
    const cells = row.split('|').map(c => c.trim()).filter(Boolean)
    html += '<tr>'
    cells.forEach(c => { html += `<td>${renderInline(c)}</td>` })
    html += '</tr>'
  })
  html += '</tbody></table></div>'
  return html
}

function renderBlocks(text) {
  let html = ''
  const blocks = splitCodeBlocks(text)
  for (const block of blocks) {
    if (block.type === 'code') {
      const lang = block.lang || 'plaintext'
      let highlighted
      try {
        highlighted = hljs.highlight(block.content, { language: lang, ignoreIllegals: true }).value
      } catch {
        try {
          highlighted = hljs.highlightAuto(block.content).value
        } catch {
          highlighted = escapeHtml(block.content)
        }
      }
      const langLabel = lang.toUpperCase()
      // 复制按钮使用 data-code 属性承载原始代码，配合 post.vue 的事件委托实现复制
      html += `<div class="code-block-wrap"><div class="code-block-header"><span class="code-lang">${langLabel}</span><button class="code-copy" type="button" data-code="${encodeURIComponent(block.content)}">复制</button></div><pre class="code-block"><code class="hljs language-${lang}">${highlighted}</code></pre></div>`
      continue
    }
    const src = block.content
    const lines = src.split('\n')
    let i = 0
    while (i < lines.length) {
      const line = lines[i]
      const trimmed = line.trim()
      if (!trimmed) { i++; continue }

      // 表格块：连续 | 行
      if (trimmed.startsWith('|') && lines[i + 1] && /^\s*\|?[\s:|-]+\|?\s*$/.test(lines[i + 1])) {
        const rows = []
        while (i < lines.length && lines[i].trim().startsWith('|')) { rows.push(lines[i]); i++ }
        html += renderTable(rows)
        continue
      }

      // 标题
      const h = /^(#{1,6})\s+(.*)$/.exec(trimmed)
      if (h) {
        const level = h[1].length
        const slug = `h-${i}-${h[2].replace(/[^\w\u4e00-\u9fa5]+/g, '-').toLowerCase().slice(0, 40)}`
        html += `<h${level} id="${slug}">${renderInline(h[2])}</h${level}>`
        i++
        continue
      }

      // 引用
      if (trimmed.startsWith('>')) {
        const quote = []
        while (i < lines.length && lines[i].trim().startsWith('>')) { quote.push(lines[i].trim().replace(/^>\s?/, '')); i++ }
        html += `<blockquote>${quote.map(q => `<p>${renderInline(q)}</p>`).join('')}</blockquote>`
        continue
      }

      // 无序列表
      if (/^[-*+]\s+/.test(trimmed)) {
        const items = []
        while (i < lines.length && /^[-*+]\s+/.test(lines[i].trim())) { items.push(lines[i].trim().replace(/^[-*+]\s+/, '')); i++ }
        html += `<ul>${items.map(it => `<li>${renderInline(it)}</li>`).join('')}</ul>`
        continue
      }

      // 有序列表
      if (/^\d+\.\s+/.test(trimmed)) {
        const items = []
        while (i < lines.length && /^\d+\.\s+/.test(lines[i].trim())) { items.push(lines[i].trim().replace(/^\d+\.\s+/, '')); i++ }
        html += `<ol>${items.map(it => `<li>${renderInline(it)}</li>`).join('')}</ol>`
        continue
      }

      // 分隔线
      if (/^([-*_])\s*(\1\s*){2,}$/.test(trimmed)) {
        html += '<hr>'
        i++
        continue
      }

      // 普通段落
      const para = []
      while (i < lines.length && lines[i].trim() && !/^(#{1,6}\s|>|[-*+]\s|\d+\.\s|```|\s*([-*_])\s*(\1\s*){2,}$)/.test(lines[i].trim())) {
        para.push(lines[i]); i++
      }
      html += `<p>${renderInline(para.join(' '))}</p>`
    }
  }
  return html
}

// 入口：Markdown 文本 → HTML 字符串
export function renderMarkdown(md) {
  if (!md) return '<p class="empty-hint">暂无内容</p>'
  return renderBlocks(md)
}

// 从 HTML 中提取 h1~h3 构建目录
export function extractToc(html) {
  const toc = []
  const regex = /<h([1-3]) id="([^"]+)">([\s\S]*?)<\/h\1>/g
  let m
  while ((m = regex.exec(html)) !== null) {
    toc.push({
      level: Number(m[1]),
      id: m[2],
      text: m[3].replace(/<[^>]+>/g, '')
    })
  }
  return toc
}
