<template>
  <div class="min-h-screen px-4 md:px-6 py-10">
    <div class="mx-auto max-w-6xl">
      <PageHeader title="网页采集" subtitle="Web Scraper · CSS/XPath Selector · Adaptive · Markdown · Sitemap"
        :crumbs="[{ label: '首页', to: '/' }, { label: '运维工具箱', to: '/tools' }, { label: '网页采集' }]" />

      <!-- 抓取面板 -->
      <section class="glass-panel p-5 mb-5">
        <div class="flex flex-col md:flex-row gap-3">
          <input v-model="url" class="web3-input flex-1 text-sm font-mono" placeholder="输入要采集的页面地址，如 https://example.com"
            @keyup.enter="doFetch" />
          <button class="web3-btn text-sm !px-6" :disabled="!url.trim() || loading" @click="doFetch">
            {{ loading ? '抓取中...' : '开始抓取' }}
          </button>
        </div>
        <div class="mt-4 flex flex-wrap items-center gap-4">
          <div class="flex items-center gap-2">
            <span class="text-xs text-gray-500">内容</span>
            <label v-for="c in chooseContent" :key="c.key" class="flex items-center gap-1 text-xs text-gray-400 cursor-pointer">
              <input type="checkbox" v-model="contentKinds" :value="c.key" class="accent-cyan-400" />{{ c.label }}
            </label>
          </div>
          <div class="flex items-center gap-2">
            <span class="text-xs text-gray-500">指纹</span>
            <select v-model="impersonate" class="web3-input !w-24 text-xs text-center !py-1">
              <option v-for="b in browsers" :key="b" :value="b">{{ b }}</option>
            </select>
          </div>
          <div class="flex items-center gap-2">
            <span class="text-xs text-gray-500">超时</span>
            <input v-model.number="timeout" type="number" min="3" max="120" class="web3-input !w-20 text-xs text-center !py-1" />
            <span class="text-xs text-gray-500">秒</span>
          </div>
        </div>
        <div v-if="fetchError" class="mt-3 text-xs text-red-400 bg-red-500/10 rounded-lg px-3 py-2">{{ fetchError }}</div>
      </section>

      <!-- 抓取结果 -->
      <section v-if="result" class="glass-panel p-5 mb-5">
        <div class="flex flex-wrap items-center gap-x-5 gap-y-2 mb-4 text-xs">
          <span class="text-gray-500">状态 <span :class="result.status < 400 ? 'text-green-400' : 'text-red-400'">{{ result.status }}</span></span>
          <span class="text-gray-500">标题 <span class="text-white">{{ result.title || '（无）' }}</span></span>
          <span class="text-gray-500">体积 <span class="text-cyan-300">{{ (result.bytes / 1024).toFixed(1) }} KB</span></span>
          <span class="text-gray-500">耗时 <span class="text-cyan-300">{{ result.elapsed }}s</span></span>
          <span v-if="result.truncated" class="text-yellow-400">HTML 已截断</span>
          <a v-if="result.url" :href="result.url" target="_blank" rel="noopener" class="text-cyan-400 hover:text-cyan-300 ml-auto">打开原页 ↗</a>
        </div>

        <div class="flex gap-2 mb-3 flex-wrap">
          <button v-for="t in tabs" :key="t.key" class="web3-pill" :class="{ 'web3-pill-active': tab === t.key }"
            @click="tab = t.key">{{ t.label }}<span class="text-[10px] text-gray-500 ml-1">{{ tabCount(t.key) }}</span></button>
          <div class="ml-auto flex gap-2">
            <button class="web3-btn-outline text-xs !px-3 !py-1" @click="copyCurrent">📋 复制</button>
            <button class="web3-btn-outline text-xs !px-3 !py-1" @click="downloadCurrent">💾 下载</button>
          </div>
        </div>
        <div class="rounded-xl border border-white/10 bg-black/40 p-4 max-h-[420px] overflow-auto">
          <pre v-if="tab === 'markdown'" class="text-xs text-green-300 font-mono whitespace-pre-wrap break-words leading-relaxed">{{ result.markdown }}</pre>
          <pre v-else-if="tab === 'text'" class="text-xs text-gray-300 font-mono whitespace-pre-wrap break-words leading-relaxed">{{ result.text }}</pre>
          <pre v-else class="text-xs text-gray-400 font-mono whitespace-pre-wrap break-all leading-relaxed">{{ result.html }}</pre>
        </div>
      </section>

      <div class="holo-bar mb-5"></div>

      <!-- 选择器提取 -->
      <section class="glass-panel p-5 mb-5">
        <h2 class="text-sm font-bold text-white mb-3 flex items-center gap-2"><span>🎯</span>选择器提取
          <span class="text-[11px] font-normal text-gray-500">基于 scrapling Selector，支持 css / xpath / 自适应定位</span>
        </h2>
        <div class="flex flex-col md:flex-row gap-3">
          <select v-model="qtype" class="web3-input !w-28 text-sm text-center">
            <option value="css">CSS</option>
            <option value="xpath">XPath</option>
          </select>
          <input v-model="selector" class="web3-input flex-1 text-sm font-mono" placeholder="如 article h2 a 或 //div[@class='item']"
            @keyup.enter="doQuery" />
          <button class="web3-btn text-sm !px-6" :disabled="!selector.trim() || querying || (!html && !url.trim())" @click="doQuery">
            {{ querying ? '查询中...' : '提取' }}
          </button>
        </div>
        <div class="mt-3 flex flex-wrap items-center gap-4">
          <label class="flex items-center gap-1 text-xs text-gray-400 cursor-pointer">
            <input type="checkbox" v-model="adaptive" class="accent-cyan-400" />自适应定位
          </label>
          <label class="flex items-center gap-1 text-xs text-gray-400 cursor-pointer">
            <input type="checkbox" v-model="joinUrl" class="accent-cyan-400" />补全相对链接
          </label>
          <div class="flex items-center gap-2">
            <span class="text-xs text-gray-500">条数上限</span>
            <input v-model.number="limit" type="number" min="1" max="500" class="web3-input !w-20 text-xs text-center !py-1" />
          </div>
          <span class="text-xs text-gray-600">未抓取页面时将按上方 URL 实时抓取</span>
        </div>
        <div class="mt-3">
          <label class="text-xs text-gray-500 mb-1 block">字段映射（可选，每行「字段名=子选择器」，取属性用 @attr）</label>
          <textarea v-model="fieldsText" class="web3-input text-xs font-mono !min-h-[60px]" placeholder="标题=h3&#10;链接=a@href"></textarea>
        </div>
        <div v-if="queryError" class="mt-3 text-xs text-red-400 bg-red-500/10 rounded-lg px-3 py-2">{{ queryError }}</div>

        <div v-if="items.length" class="mt-4">
          <div class="flex items-center justify-between mb-2">
            <span class="text-xs text-gray-500">共 {{ items.length }} 条结果</span>
            <div class="flex gap-2">
              <button class="web3-btn-outline text-xs !px-3 !py-1" @click="copyItems">📋 复制 JSON</button>
              <button class="web3-btn-outline text-xs !px-3 !py-1" @click="downloadItems('json')">JSON</button>
              <button class="web3-btn-outline text-xs !px-3 !py-1" @click="downloadItems('csv')">CSV</button>
            </div>
          </div>
          <div class="overflow-auto max-h-[380px] rounded-xl border border-white/10">
            <table class="w-full text-left text-xs">
              <thead class="bg-white/[0.03] sticky top-0">
                <tr>
                  <th class="px-3 py-2 text-gray-500 w-10">#</th>
                  <th v-for="col in columns" :key="col" class="px-3 py-2 text-gray-500">{{ col }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, i) in items" :key="i" class="border-t border-white/[0.05] hover:bg-white/[0.02]">
                  <td class="px-3 py-2 text-gray-600">{{ i + 1 }}</td>
                  <td v-for="col in columns" :key="col" class="px-3 py-2 text-gray-300 break-all max-w-[380px]">{{ row[col] }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        <div v-else-if="queried && !items.length && !queryError" class="mt-4 text-center text-xs text-gray-600 py-6">
          未匹配到元素，换个选择器试试
        </div>
      </section>

      <!-- Sitemap / Feed -->
      <section class="glass-panel p-5">
        <h2 class="text-sm font-bold text-white mb-3 flex items-center gap-2"><span>🗺️</span>Sitemap / RSS / Atom 解析</h2>
        <p class="text-xs text-gray-600 mb-3">输入 sitemap.xml 或订阅源地址，批量提取 URL 列表，适合做站点全量采集的入口。</p>
        <div class="flex flex-col md:flex-row gap-3">
          <input v-model="feedUrl" class="web3-input flex-1 text-sm font-mono" placeholder="如 https://example.com/sitemap.xml"
            @keyup.enter="doSitemap" />
          <button class="web3-btn text-sm !px-6" :disabled="!feedUrl.trim() || feedLoading" @click="doSitemap">
            {{ feedLoading ? '解析中...' : '解析' }}
          </button>
        </div>
        <div v-if="feedError" class="mt-3 text-xs text-red-400 bg-red-500/10 rounded-lg px-3 py-2">{{ feedError }}</div>
        <div v-if="feedEntries.length" class="mt-4">
          <div class="flex items-center justify-between mb-2">
            <span class="text-xs text-gray-500">{{ feedKind === 'feed' ? '订阅源' : 'Sitemap' }} · 共 {{ feedEntries.length }} 条</span>
            <button class="web3-btn-outline text-xs !px-3 !py-1" @click="downloadFeed">导出 CSV</button>
          </div>
          <div class="overflow-auto max-h-[320px] rounded-xl border border-white/10">
            <table class="w-full text-left text-xs">
              <thead class="bg-white/[0.03] sticky top-0">
                <tr>
                  <th class="px-3 py-2 text-gray-500">标题</th>
                  <th class="px-3 py-2 text-gray-500">地址</th>
                  <th class="px-3 py-2 text-gray-500 w-32">更新时间</th>
                  <th class="px-3 py-2 text-gray-500 w-16"></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(e, i) in feedEntries" :key="i" class="border-t border-white/[0.05] hover:bg-white/[0.02]">
                  <td class="px-3 py-2 text-gray-300 max-w-[220px] truncate">{{ e.title || '—' }}</td>
                  <td class="px-3 py-2 text-cyan-400 break-all max-w-[420px]">{{ e.url }}</td>
                  <td class="px-3 py-2 text-gray-500">{{ e.lastmod || '—' }}</td>
                  <td class="px-3 py-2">
                    <button class="text-[11px] text-purple-400 hover:text-purple-300" @click="useUrl(e.url)">采集</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </section>

      <p class="mt-6 text-[11px] text-gray-600 leading-relaxed">
        说明：采集引擎为 curl_cffi（浏览器 TLS 指纹）+ scrapling Selector，属静态抓取，不执行页面 JavaScript；
        若目标站依赖前端渲染，请改用浏览器渲染方案。为防 SSRF，后端仅允许公网 http/https 地址。
      </p>
    </div>
  </div>
</template>

<script setup>
// ====================================================
// 网页采集工具页
// 能力：静态抓取 / CSS·XPath·自适应选择器 / HTML 转 Markdown
//      / sitemap·RSS·Atom 解析 / 结果复制与导出
// 后端：py-service 9006 的 scrape 模块（/pyscrape → /api/scrape）
// ====================================================
import { ref, computed } from 'vue'
import { scrapeFetch, scrapeQuery, scrapeSitemap } from '@/api/scrape'
import { useToastStore } from '@/stores/modules/toast'
import PageHeader from '@/components/common/PageHeader.vue'

const toast = useToastStore()

const url = ref('')
const loading = ref(false)
const fetchError = ref('')
const result = ref(null)
const html = ref('')
const tab = ref('markdown')
const contentKinds = ref(['markdown', 'text'])
const impersonate = ref('chrome')
const timeout = ref(20)

const chooseContent = [
  { key: 'markdown', label: 'Markdown' },
  { key: 'text', label: '纯文本' },
  { key: 'html', label: 'HTML' },
]
const browsers = ['chrome', 'chrome110', 'chrome116', 'safari17_0', 'edge101', 'firefox133']
const tabs = computed(() => chooseContent.filter(c => c.key !== 'html' || result.value?.html))
const tabCount = key => {
  const v = result.value?.[key]
  return v ? `(${v.length})` : ''
}

// 抓取页面：按所选内容类型请求，并缓存 HTML 供选择器复用
async function doFetch() {
  if (!url.value.trim()) return
  loading.value = true; fetchError.value = ''; result.value = null; items.value = []; queried.value = false
  try {
    const data = await scrapeFetch({
      url: url.value.trim(),
      extract: contentKinds.value.length ? contentKinds.value : ['markdown', 'text', 'html'],
      timeout: timeout.value,
      impersonate: impersonate.value,
    })
    result.value = data
    html.value = data.html || ''
    // 默认展示第一个有内容的 Tab
    tab.value = tabs.value[0]?.key || 'text'
    toast.success(`抓取成功 · ${data.status}`)
    if (!html.value && contentKinds.value.includes('html')) html.value = data.html || ''
  } catch (e) {
    fetchError.value = e?.response?.data?.detail || e?.message || '抓取失败'
  } finally {
    loading.value = false
  }
}

const selector = ref('')
const qtype = ref('css')
const adaptive = ref(false)
const joinUrl = ref(true)
const limit = ref(50)
const fieldsText = ref('')
const querying = ref(false)
const queryError = ref('')
const queried = ref(false)
const items = ref([])

const columns = computed(() => (items.value.length ? Object.keys(items.value[0]) : []))

// 把「字段名=子选择器」多行文本解析为对象
function parseFields() {
  const out = {}
  for (const line of fieldsText.value.split('\n')) {
    const i = line.indexOf('=')
    if (i <= 0) continue
    const k = line.slice(0, i).trim()
    const v = line.slice(i + 1).trim()
    if (k) out[k] = v
  }
  return Object.keys(out).length ? out : null
}

// 选择器提取：优先用已抓取的 HTML，否则按 URL 实时抓取
async function doQuery() {
  if (!selector.value.trim()) return
  querying.value = true; queryError.value = ''; queried.value = false
  try {
    const data = await scrapeQuery({
      selector: selector.value.trim(),
      type: qtype.value,
      url: html.value ? null : url.value.trim(),
      html: html.value || null,
      limit: limit.value,
      fields: parseFields(),
      adaptive: adaptive.value,
      join_url: joinUrl.value,
      timeout: timeout.value,
    })
    items.value = (data.items || []).map(row => {
      if (row.attrs) {
        return { 文本: row.text, ...Object.fromEntries(Object.entries(row.attrs).map(([k, v]) => [`@${k}`, v])) }
      }
      return row
    })
    queried.value = true
    toast.success(`匹配 ${data.count} 条`)
  } catch (e) {
    queryError.value = e?.response?.data?.detail || e?.message || '查询失败'
  } finally {
    querying.value = false
  }
}

const feedUrl = ref('')
const feedLoading = ref(false)
const feedError = ref('')
const feedEntries = ref([])
const feedKind = ref('sitemap')

// 解析 sitemap.xml / RSS / Atom
async function doSitemap() {
  if (!feedUrl.value.trim()) return
  feedLoading.value = true; feedError.value = ''; feedEntries.value = []
  try {
    const data = await scrapeSitemap({ url: feedUrl.value.trim(), timeout: timeout.value })
    feedKind.value = data.kind
    feedEntries.value = data.entries || []
    toast.success(`解析到 ${data.count} 条`)
  } catch (e) {
    feedError.value = e?.response?.data?.detail || e?.message || '解析失败'
  } finally {
    feedLoading.value = false
  }
}

// ── 通用工具：复制 / 下载 / 复用 ──
function copyText(text, tip = '已复制') {
  if (!text) return
  navigator.clipboard?.writeText(text).then(() => toast.success(tip)).catch(() => toast.error('复制失败'))
}
function copyCurrent() {
  copyText(result.value?.[tab.value], `已复制 ${tab.value}`)
}
function copyItems() {
  copyText(JSON.stringify(items.value, null, 2), '已复制 JSON')
}
function downloadBlob(name, text, type = 'text/plain;charset=utf-8') {
  const a = document.createElement('a')
  a.href = URL.createObjectURL(new Blob([text], { type }))
  a.download = name
  a.click()
  setTimeout(() => URL.revokeObjectURL(a.href), 3000)
}
function downloadCurrent() {
  const ext = tab.value === 'markdown' ? 'md' : tab.value === 'html' ? 'html' : 'txt'
  downloadBlob(`scrape-${Date.now()}.${ext}`, result.value?.[tab.value] || '')
}
function toCsv(rows) {
  if (!rows.length) return ''
  const keys = Object.keys(rows[0])
  const esc = v => `"${String(v ?? '').replace(/"/g, '""')}"`
  return [keys.join(','), ...rows.map(r => keys.map(k => esc(r[k])).join(','))].join('\n')
}
function downloadItems(fmt) {
  if (fmt === 'csv') downloadBlob(`scrape-items-${Date.now()}.csv`, '\ufeff' + toCsv(items.value), 'text/csv;charset=utf-8')
  else downloadBlob(`scrape-items-${Date.now()}.json`, JSON.stringify(items.value, null, 2), 'application/json')
}
function downloadFeed() {
  downloadBlob(`scrape-feed-${Date.now()}.csv`, '\ufeff' + toCsv(feedEntries.value), 'text/csv;charset=utf-8')
}
// 从 sitemap 结果一键回填到抓取框
function useUrl(u) {
  url.value = u
  html.value = ''
  result.value = null
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
</script>
