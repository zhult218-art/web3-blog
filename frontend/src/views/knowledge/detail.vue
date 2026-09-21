<template>
  <div class="space-y-6">
    <!-- 加载 -->
    <div v-if="loading" class="py-24 flex flex-col items-center gap-3 text-gray-500">
      <div class="h-8 w-8 rounded-full border-2 border-white/15 border-t-cyan-400 animate-spin"></div>
      <span class="text-xs">正在翻阅知识库…</span>
    </div>

    <template v-else-if="entry">
      <!-- 返回 + 元信息 -->
      <router-link to="/knowledge" class="inline-flex items-center gap-1.5 text-xs text-gray-500 hover:text-cyan-300 transition-colors">
        ← 返回知识库
      </router-link>

      <article class="panel p-6 md:p-9">
        <div class="flex flex-wrap items-center gap-2 text-xs mb-4">
          <span class="kb-badge cat">{{ taxonomy.find(t => t.key === entry.category)?.icon || '📘' }} {{ entry.category }}</span>
          <span v-if="entry.subcategory" class="kb-badge">{{ entry.subcategory }}</span>
          <span class="kb-badge">{{ stageMeta(entry.stage).icon }} {{ stageMeta(entry.stage).label }}</span>
          <span v-if="entry.source" class="kb-badge">来源：{{ entry.source }}</span>
        </div>

        <h1 class="text-2xl md:text-3xl font-bold text-white leading-snug">{{ entry.title }}</h1>
        <p v-if="entry.summary" class="text-sm text-gray-400 mt-3 leading-relaxed">{{ entry.summary }}</p>

        <div class="flex flex-wrap items-center gap-3 mt-4 text-xs text-gray-500">
          <span>更新于 {{ fmtDate(entry.updatedAt || entry.createdAt) }}</span>
          <span>阅读 {{ entry.viewCount }}</span>
          <span class="flex items-center gap-1">
            难度
            <i v-for="n in 5" :key="n" class="inline-block w-1.5 h-1.5 rounded-full"
               :style="{ background: n <= entry.difficulty ? '#a78bfa' : 'rgba(255,255,255,.12)' }"></i>
          </span>
          <a v-if="entry.sourceUrl" :href="entry.sourceUrl" target="_blank" rel="noopener"
             class="text-cyan-400/80 hover:text-cyan-300">参考原文 ↗</a>
          <span class="ml-auto flex gap-2">
            <button class="kb-act-btn" @click="exportMd" title="导出 Markdown">⬇ MD</button>
            <button class="kb-act-btn" @click="openPublish" title="发布到博客">📝 发布</button>
          </span>
        </div>

        <!-- Markdown 正文（内部链接交给 Vue Router） -->
        <div class="kb-body article-body mt-8" ref="bodyRef" v-html="html"
             @click="onBodyClick" @contextmenu.prevent></div>

        <div v-if="entry.tags.length" class="flex flex-wrap gap-2 mt-8 pt-6 border-t border-white/10">
          <router-link v-for="t in entry.tags" :key="t" :to="{ path: '/knowledge', query: { tag: t } }"
            class="kb-badge hover:!text-cyan-300 cursor-pointer transition-colors">#{{ t }}</router-link>
        </div>
      </article>

      <!-- 同类知识 -->
      <div v-if="related.length" class="panel p-6">
        <h3 class="text-sm font-semibold text-white/90 mb-4">🧭 同分类的其他知识</h3>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
          <router-link v-for="r in related" :key="r.id" :to="`/knowledge/${r.id}`"
            class="related-item">
            <span class="text-sm">{{ stageMeta(r.stage).icon }}</span>
            <span class="text-sm text-white/80 truncate flex-1">{{ r.title }}</span>
          </router-link>
        </div>
      </div>
    </template>

    <!-- 未找到 -->
    <div v-else class="py-24 text-center text-gray-500">
      <div class="text-4xl mb-3">🔍</div>
      <p class="text-sm">没有找到这条知识，可能尚未发布</p>
      <router-link to="/knowledge" class="inline-block mt-4 web3-btn-outline text-xs !px-5 !py-2">返回知识库</router-link>
    </div>

    <!-- 发布到博客弹窗 -->
    <Modal v-model="showPublish" title="发布到博客">
      <div class="space-y-3">
        <input v-model="pubForm.title" class="web3-input text-sm" placeholder="文章标题" />
        <input v-model="pubForm.summary" class="web3-input text-sm" placeholder="摘要" />
        <div class="grid grid-cols-2 gap-3">
          <select v-model="pubForm.category" class="web3-input text-sm">
            <option v-for="c in blogCategories" :key="c" :value="c">{{ c }}</option>
          </select>
          <input v-model="pubForm.tags" class="web3-input text-sm" placeholder="标签，逗号分隔" />
        </div>
        <label class="flex items-center gap-2 text-xs text-white/70 cursor-pointer">
          <input type="checkbox" v-model="pubForm.published" class="accent-purple-500" /> 立即发布
        </label>
        <p v-if="pubError" class="text-xs text-red-400">{{ pubError }}</p>
        <div class="flex justify-end gap-2">
          <button class="web3-btn-outline text-xs !px-4 !py-2" @click="showPublish = false">取消</button>
          <button class="web3-btn text-xs !px-4 !py-2" :disabled="pubBusy" @click="doPublish">
            {{ pubBusy ? '发布中…' : '📝 发布' }}
          </button>
        </div>
      </div>
    </Modal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Modal from '@/components/common/Modal.vue'
import { useToastStore } from '@/stores/modules/toast'
import { getEntry, listEntries, entryToMarkdown, publishToBlog, getOwnerUser } from '@/api/knowledge'
import { KB_TAXONOMY as taxonomy, stageMeta } from '@/data/knowledgeTaxonomy'
import { renderMarkdown } from '@/utils/markdown'

const route = useRoute()
const router = useRouter()
const toast = useToastStore()
const entry = ref(null)
const related = ref([])
const loading = ref(true)
const bodyRef = ref(null)

const html = computed(() => entry.value ? renderMarkdown(entry.value.content) : '')

const blogCategories = ['硬件底层', '嵌入式', '软件工程', '网络工程', '网络安全', '运维与效率', '前沿AI', 'Web3', '建站记录', '随笔']
const showPublish = ref(false)
const pubBusy = ref(false)
const pubError = ref('')
const pubForm = ref({ title: '', summary: '', category: '软件工程', tags: '', published: true })

function fmtDate(d) { return d ? String(d).slice(0, 10) : '' }

function download(filename, text) {
  const blob = new Blob([text], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = filename
  document.body.appendChild(a); a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}
function exportMd() {
  if (!entry.value) return
  download(`${entry.value.title || 'knowledge'}.md`, entryToMarkdown(entry.value))
  toast.success('已导出 Markdown')
}
async function openPublish() {
  const { data } = await getOwnerUser()
  if (!data?.user) { toast.info('请先到 /knowledge 页面登录站主'); return }
  pubError.value = ''
  pubForm.value = {
    title: entry.value.title,
    summary: entry.value.summary,
    category: entry.value.category || '软件工程',
    tags: entry.value.tags.join(', '),
    published: true,
  }
  showPublish.value = true
}
async function doPublish() {
  pubBusy.value = true; pubError.value = ''
  try {
    const res = await publishToBlog(entry.value, {
      title: pubForm.value.title, summary: pubForm.value.summary,
      category: pubForm.value.category, tags: pubForm.value.tags,
      published: pubForm.value.published,
    })
    showPublish.value = false
    toast.success('已发布到博客 📝')
    setTimeout(() => router.push(`/blog/post/${res.id}`), 500)
  } catch (e) { pubError.value = e.message || '发布失败' }
  finally { pubBusy.value = false }
}

// 正文中的站内链接（如 /tools/hash、/blog/post/1）走前端路由，不整页刷新
function onBodyClick(e) {
  // 代码块复制按钮（v-html 渲染，事件委托处理）
  const copyBtn = e.target.closest('.code-copy')
  if (copyBtn && bodyRef.value?.contains(copyBtn)) {
    e.preventDefault()
    const code = decodeURIComponent(copyBtn.getAttribute('data-code') || '')
    navigator.clipboard?.writeText(code).then(() => {
      const orig = copyBtn.textContent
      copyBtn.textContent = '已复制 ✓'
      setTimeout(() => { copyBtn.textContent = orig }, 1500)
    })
    return
  }
  const a = e.target.closest('a')
  if (!a || !bodyRef.value.contains(a)) return
  const href = a.getAttribute('href') || ''
  if (href.startsWith('/')) {
    e.preventDefault()
    if (href !== route.fullPath) router.push(href)
  }
}

onMounted(async () => {
  loading.value = true
  try {
    entry.value = await getEntry(route.params.id)
    if (entry.value) {
      const same = await listEntries({ category: entry.value.category })
      related.value = same.filter(x => x.id !== entry.value.id).slice(0, 6)
    }
  } catch (e) {
    entry.value = null
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.kb-badge {
  font-size: 11px;
  padding: 3px 9px;
  border-radius: 7px;
  background: rgba(255,255,255,.06);
  color: rgba(255,255,255,.65);
  border: 1px solid rgba(255,255,255,.08);
}
.kb-badge.cat { background: rgba(34,211,238,.08); color: #67e8f9; border-color: rgba(34,211,238,.2); }
.related-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  background: rgba(255,255,255,.03);
  border: 1px solid rgba(255,255,255,.07);
  transition: all .2s;
}
.related-item:hover { border-color: rgba(103,232,249,.3); transform: translateY(-1px); }
.kb-act-btn {
  padding: 4px 10px; border-radius: 7px; font-size: 11.5px;
  background: rgba(255,255,255,.05); border: 1px solid rgba(255,255,255,.1);
  color: rgba(255,255,255,.7); transition: all .2s;
}
.kb-act-btn:hover { border-color: rgba(103,232,249,.35); color: #67e8f9; }
</style>
