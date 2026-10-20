<template>
  <div class="min-h-screen px-4 md:px-6 py-10">
    <div class="mx-auto max-w-6xl">
      <PageHeader title="Google Hacking 检索" subtitle="Google Dorks · 敏感文件定位 · 授权安全测试"
        :crumbs="[{ label: '首页', to: '/' }, { label: '运维工具箱', to: '/tools' }, { label: 'Google Hacking' }]" />

      <!-- 域名输入 -->
      <section class="glass-panel p-5 mb-5">
        <div class="flex flex-col md:flex-row gap-3">
          <input v-model="domain" class="web3-input flex-1 text-sm font-mono"
            placeholder="输入目标域名，如 example.com（可粘贴完整 URL，会自动取域名）"
            @keyup.enter="openFirst" />
          <button class="web3-btn text-sm !px-6" :disabled="!host" @click="openFirst">🚀 打开首类检索</button>
          <button class="web3-btn-outline text-sm !px-5" :disabled="!host" @click="copyAll">📋 复制全部语法</button>
        </div>
        <p class="mt-3 text-[11px] text-gray-500">
          当前目标：<span class="text-cyan-300 font-mono">{{ host || '（未填写）' }}</span>
          · 点击下方任一分类即在 Google 新窗口执行检索，也可单独复制语法自行使用
        </p>
      </section>

      <!-- 分类卡片 -->
      <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
        <div v-for="c in CATEGORIES" :key="c.key" class="glass-panel p-4 flex flex-col hover:border-cyan-400/30 transition">
          <div class="flex items-center gap-2.5">
            <span class="text-xl">{{ c.icon }}</span>
            <div class="min-w-0">
              <h3 class="text-sm font-semibold text-white truncate">{{ c.name }}</h3>
              <p class="text-[11px] text-gray-500 truncate">{{ c.desc }}</p>
            </div>
          </div>
          <pre class="mt-3 rounded-lg border border-white/10 bg-black/40 px-3 py-2 text-[11px] text-green-300 font-mono whitespace-pre-wrap break-all max-h-24 overflow-auto">{{ c.build(host || 'example.com') }}</pre>
          <div class="mt-auto pt-3 flex items-center gap-2">
            <button class="web3-btn text-xs !px-4 !py-1.5" :disabled="!host" @click="search(c)">搜索</button>
            <button class="web3-btn-outline text-xs !px-4 !py-1.5" @click="copyOne(c)">复制语法</button>
            <span v-if="copiedKey === c.key" class="text-[11px] text-green-400 ml-auto">已复制 ✓</span>
          </div>
        </div>
      </div>

      <div class="holo-bar my-6"></div>

      <!-- 说明与免责 -->
      <section class="glass-panel p-5">
        <h2 class="text-sm font-bold text-white mb-3 flex items-center gap-2"><span>💡</span>使用说明</h2>
        <ul class="text-xs text-gray-400 leading-relaxed space-y-1.5 list-disc pl-5">
          <li>本页把常见的 Google Hacking 语法按用途归类，仅负责生成检索语句并跳转到 Google，不抓取、不存储任何目标站点数据。</li>
          <li>「目录遍历 / 配置文件 / 数据库 / 日志 / 备份」等分类用于发现被搜索引擎意外收录的敏感文件。</li>
          <li>「登录页面」用于定位后台入口；「SQL 错误」通过报错特征辅助判断注入点。</li>
          <li>粘贴含路径的 URL 时会自动提取主机名，避免 <code class="text-cyan-300">site:</code> 后带路径导致检索失效。</li>
        </ul>
        <p class="mt-4 pt-3 border-t border-white/10 text-[11px] text-amber-400/90">
          ⚠️ 请勿用于未授权目标。上述语法仅限对自有资产或已获得书面授权的目标开展安全测试，使用者需自行承担合规责任。
        </p>
      </section>
    </div>
  </div>
</template>

<script setup>
// ====================================================
// Google Hacking 检索页：把常见的 Google Dorks 语法
// 按用途归类，输入域名后一键生成检索语句并跳转 Google
// ====================================================
import { ref, computed } from 'vue'
import { useToastStore } from '@/stores/modules/toast'
import PageHeader from '@/components/common/PageHeader.vue'

const toast = useToastStore()
const domain = ref('')
const copiedKey = ref('')

// 从用户输入中提取主机名：兼容 http(s)://、带路径、带端口、带 www 的整段 URL
const host = computed(() => {
  let s = String(domain.value || '').trim()
  if (!s) return ''
  s = s.replace(/^[a-z]+:\/\//i, '')
  s = s.split('/')[0].split('?')[0].split('#')[0]
  return s.trim()
})

// 11 类常见语法（与 GoogleHackingTool 的分类保持一致，改写为可读的 dork 语句）
const CATEGORIES = [
  {
    key: 'indexof', icon: '📂', name: '目录遍历漏洞', desc: '站点目录结构被意外暴露',
    build: d => `site:${d} intitle:index.of`,
  },
  {
    key: 'config', icon: '⚙️', name: '配置文件泄露', desc: 'xml/conf/ini/txt 等配置外泄',
    build: d => `site:${d} ext:xml | ext:conf | ext:cnf | ext:reg | ext:inf | ext:rdp | ext:cfg | ext:txt | ext:ora | ext:ini`,
  },
  {
    key: 'database', icon: '🗄️', name: '数据库文件泄露', desc: 'sql/dbf/db/mdb 暴露在公网',
    build: d => `site:${d} ext:sql | ext:dbf | ext:db | ext:mdb`,
  },
  {
    key: 'log', icon: '📜', name: '日志文件泄露', desc: '运行日志被搜索引擎收录',
    build: d => `site:${d} ext:log`,
  },
  {
    key: 'backup', icon: '💾', name: '备份和历史文件', desc: 'bak/old/backup 等历史遗留',
    build: d => `site:${d} ext:bkf | ext:bkp | ext:bak | ext:old | ext:backup`,
  },
  {
    key: 'login', icon: '🔑', name: '登录页面', desc: '定位后台管理与认证入口',
    build: d => `site:${d} inurl:login | inurl:admin | inurl:manage | inurl:system | inurl:backend | intitle:登陆 | intitle:后台 | intitle:管理 | intitle:认证`,
  },
  {
    key: 'sqlerror', icon: '🐞', name: 'SQL 错误', desc: '通过报错特征辅助判断注入点',
    build: d => `site:${d} intext:"sql syntax near" | intext:"syntax error has occurred" | intext:"incorrect syntax near" | intext:"unexpected end of SQL command" | intext:"Warning: mysql_connect()" | intext:"Warning: mysql_query()" | intext:"Warning: pg_connect()"`,
  },
  {
    key: 'docs', icon: '📄', name: '公开文件', desc: 'doc/pdf/ppt/csv 等文档外泄',
    build: d => `site:${d} ext:doc | ext:docx | ext:odt | ext:pdf | ext:rtf | ext:sxw | ext:psw | ext:ppt | ext:pptx | ext:pps | ext:csv`,
  },
  {
    key: 'phpinfo', icon: '🐘', name: 'phpinfo 探针', desc: 'PHP 环境信息页面未清理',
    build: d => `site:${d} ext:php intitle:phpinfo "published by the PHP Group"`,
  },
  {
    key: 'paste', icon: '📋', name: '代码粘贴站泄露', desc: '在 Pastebin 等站点搜索该域名',
    build: d => `site:pastebin.com | site:paste2.org | site:pastehtml.com | site:slexy.org | site:snipplr.com | site:snipt.net | site:textsnip.com | site:jsitor.com | site:codepen.io | site:jsfiddle.net | site:dpaste.org | site:dpaste.com | site:codepad.org | site:ideone.com | site:rentry.co | site:justpaste.it | site:hastebin.com | site:trello.com "${d}"`,
  },
  {
    key: 'code', icon: '💻', name: '代码托管站泄露', desc: '在 GitHub/GitLab 搜索该域名',
    build: d => `site:github.com | site:gitlab.com "${d}"`,
  },
]

// 生成 Google 检索地址（空格与 | 交给 encodeURIComponent 处理）
function searchUrl(query) {
  return 'https://www.google.com/search?q=' + encodeURIComponent(query)
}

function openSearch(query) {
  window.open(searchUrl(query), '_blank', 'noopener')
}

// 未填域名时用示例域名占位，仍可直接复制语法自行替换
function queryOf(c) { return c.build(host.value || 'example.com') }

function search(c) { openSearch(queryOf(c)) }
function openFirst() { openSearch(queryOf(CATEGORIES[0])) }

async function copyText(text, key) {
  try {
    await navigator.clipboard.writeText(text)
    copiedKey.value = key
    setTimeout(() => { if (copiedKey.value === key) copiedKey.value = '' }, 1500)
    toast.success('语法已复制')
  } catch {
    toast.error('复制失败，请手动选择文本')
  }
}

function copyOne(c) { copyText(queryOf(c), c.key) }

// 导出全部语法，便于一次性存档或交给其它工具使用
function copyAll() {
  const all = CATEGORIES.map(c => `# ${c.name}\n${c.build(host.value || 'example.com')}`).join('\n\n')
  copyText(all, 'all')
}
</script>
