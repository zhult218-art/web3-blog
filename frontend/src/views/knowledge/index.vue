<template>
  <div class="space-y-6 kb-page">
    <!-- ============ 数据库未初始化引导 ============ -->
    <div v-if="!tableReady && !loading" class="panel p-6 border-amber-500/30">
      <div class="flex items-start gap-4">
        <span class="text-3xl">🗄️</span>
        <div class="flex-1">
          <h3 class="text-lg font-bold text-amber-300">知识库数据表尚未初始化</h3>
          <p class="text-sm text-gray-400 mt-1">
            Supabase 中还没有 <code class="text-amber-300">kb_entries</code> 表。请先执行一次初始化 SQL。
          </p>
          <div class="flex gap-2 mt-3 flex-wrap">
            <button class="web3-btn text-xs !px-4 !py-2" @click="copyInitSql">📋 复制初始化 SQL</button>
            <a href="https://supabase.com/dashboard/project/_/sql/new" target="_blank" rel="noopener"
               class="web3-btn-outline text-xs !px-4 !py-2">前往 Supabase SQL Editor ↗</a>
            <button class="web3-btn-outline text-xs !px-4 !py-2" @click="retryCheck">✅ 已执行，重新检测</button>
          </div>
          <p v-if="sqlCopied" class="text-xs text-emerald-400 mt-2">✓ SQL 已复制到剪贴板，粘贴到 Supabase SQL Editor 执行即可</p>
        </div>
      </div>
    </div>

    <!-- ============ 头部：知识树介绍 + 成长统计 ============ -->
    <div class="panel p-6 md:p-8 relative overflow-hidden" v-if="tableReady">
      <div class="absolute -right-16 -top-16 text-[180px] opacity-[0.06] select-none pointer-events-none">🧠</div>
      <div class="relative">
        <div class="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h1 class="text-2xl md:text-3xl font-bold bg-gradient-to-r from-cyan-300 to-purple-300 bg-clip-text text-transparent">
              我的知识库
            </h1>
            <p class="text-sm text-gray-400 mt-2 max-w-2xl">
              从硬件底层到网络攻防的完整学习地图。知识库会随学习不断生长：🌱 待学习 → 🌿 学习中 → 🌳 已掌握。
            </p>
          </div>
          <div class="flex gap-2 flex-wrap">
            <button class="kb-action-btn" :class="{ due: dueCount > 0 }" @click="openReview">
              🔁 今日复习 <b v-if="dueCount > 0">{{ dueCount }}</b>
            </button>
            <button class="kb-action-btn" @click="exportAll">📥 导出全部 MD</button>
          </div>
        </div>
        <!-- 成长进度 -->
        <div v-if="stats" class="mt-5 grid grid-cols-2 md:grid-cols-4 gap-3">
          <div class="growth-card">
            <div class="text-2xl font-bold text-white">{{ stats.total }}</div>
            <div class="text-xs text-gray-400 mt-0.5">知识条目</div>
          </div>
          <div class="growth-card">
            <div class="text-2xl font-bold" style="color:#3f7d5b">{{ stats.mastered }} 🌳</div>
            <div class="text-xs text-gray-400 mt-0.5">已掌握</div>
          </div>
          <div class="growth-card">
            <div class="text-2xl font-bold" style="color:#6b9e78">{{ growingCount }} 🌿</div>
            <div class="text-xs text-gray-400 mt-0.5">学习中</div>
          </div>
          <div class="growth-card">
            <div class="text-2xl font-bold text-cyan-300">{{ stats.masteryRate }}%</div>
            <div class="text-xs text-gray-400 mt-0.5">掌握率</div>
            <div class="h-1 rounded bg-white/10 mt-2 overflow-hidden">
              <div class="h-full rounded transition-all duration-700"
                   :style="{ width: stats.masteryRate + '%', background: 'linear-gradient(90deg,#6b9e78,#3f7d5b)' }"></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="tableReady" class="grid grid-cols-1 lg:grid-cols-[260px_1fr] gap-6">
      <!-- ============ 左侧：分类树 ============ -->
      <aside class="panel p-4 h-max lg:sticky lg:top-24">
        <div class="flex items-center justify-between mb-3">
          <span class="text-sm font-semibold text-white/90">知识分类树</span>
          <button v-if="!owner" class="text-[11px] text-gray-500 hover:text-cyan-300" @click="showLogin = true">站主入口</button>
          <button v-else class="text-[11px] text-gray-500 hover:text-red-300" @click="doLogout">退出登录</button>
        </div>
        <div class="space-y-1">
          <button class="kb-cat-row" :class="{ active: !activeCategory && !dueMode }" @click="resetFilter">
            <span>🗂️</span><span class="flex-1 text-left">全部</span>
            <sup class="text-[10px] opacity-60">{{ stats?.total || 0 }}</sup>
          </button>
          <template v-for="cat in taxonomy" :key="cat.key">
            <button class="kb-cat-row" :class="{ active: activeCategory === cat.key }" @click="selectCategory(cat.key)">
              <span>{{ cat.icon }}</span>
              <span class="flex-1 text-left truncate">{{ cat.key }}</span>
              <sup class="text-[10px] opacity-60">{{ stats?.byCat[cat.key]?.total || 0 }}</sup>
            </button>
            <div v-if="activeCategory === cat.key" class="ml-4 my-1 space-y-0.5 border-l border-white/10 pl-2">
              <button v-for="sub in cat.children" :key="sub"
                class="kb-sub-row" :class="{ active: activeSub === sub }"
                @click="activeSub = activeSub === sub ? '' : sub">
                {{ sub }}
              </button>
            </div>
          </template>
        </div>

        <div class="mt-4 pt-4 border-t border-white/10">
          <div class="text-xs text-gray-400 mb-2">成长阶段</div>
          <div class="flex flex-wrap gap-1.5">
            <button v-for="s in stageFilters" :key="s.key" class="kb-stage-chip"
              :class="{ active: activeStage === s.key }" @click="activeStage = activeStage === s.key ? '' : s.key">
              {{ s.icon }} {{ s.label }}
            </button>
          </div>
        </div>
      </aside>

      <!-- ============ 右侧：搜索 + 条目列表 ============ -->
      <section class="space-y-4 min-w-0">
        <div class="flex gap-3 flex-wrap">
          <div class="relative flex-1 min-w-[220px]">
            <input v-model="keyword" class="web3-input text-sm pl-9" placeholder="搜索知识条目（标题 / 摘要 / 内容）…"
                   @keyup.enter="reload" />
            <span class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500">🔍</span>
          </div>
          <button v-if="owner" class="web3-btn text-xs !px-4 flex items-center gap-1.5" @click="openEditor()">
            <span>✏️</span> 新增知识
          </button>
        </div>

        <!-- 复习模式提示 -->
        <div v-if="dueMode" class="flex items-center gap-2 text-xs px-3 py-2 rounded-lg bg-amber-500/10 border border-amber-500/20 text-amber-200">
          🔁 今日复习模式：以下条目到达复习时间，请认真回忆后点击卡片自评
          <button class="ml-auto text-amber-300/70 hover:text-amber-200" @click="resetFilter">退出复习 ✕</button>
        </div>

        <div v-if="activeCategory || activeStage || activeSub || keyword" class="flex flex-wrap gap-2 text-xs">
          <span v-if="activeCategory" class="kb-filter-tag">{{ taxonomy.find(t => t.key === activeCategory)?.icon }} {{ activeCategory }} <i @click="activeCategory='';activeSub=''">✕</i></span>
          <span v-if="activeSub" class="kb-filter-tag">{{ activeSub }} <i @click="activeSub=''">✕</i></span>
          <span v-if="activeStage" class="kb-filter-tag">{{ stageMeta(activeStage).icon }} {{ stageMeta(activeStage).label }} <i @click="activeStage=''">✕</i></span>
        </div>

        <div v-if="loading" class="py-20 flex flex-col items-center gap-3 text-gray-500">
          <div class="h-8 w-8 rounded-full border-2 border-white/15 border-t-cyan-400 animate-spin"></div>
          <span class="text-xs">知识生长中…</span>
        </div>
        <div v-else-if="!entries.length" class="py-20 text-center text-gray-500 text-sm">
          <div class="text-4xl mb-3 opacity-60">🌰</div>{{ dueMode ? '今日已无到期复习条目，去学点新东西吧' : '这里还没有种下知识，换个筛选条件试试' }}
        </div>

        <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <article v-for="e in entries" :key="e.id" class="kb-entry-card" @click="goDetail(e)">
            <div class="flex items-start justify-between gap-2">
              <div class="flex items-center gap-2 min-w-0">
                <span class="text-base shrink-0">{{ stageMeta(e.stage).icon }}</span>
                <h3 class="font-semibold text-white/95 text-[15px] truncate">{{ e.title }}</h3>
              </div>
              <div v-if="owner" class="flex gap-1 shrink-0" @click.stop>
                <button class="kb-admin-btn" :title="'推进阶段'" @click="onAdvance(e)">⇆</button>
                <button class="kb-admin-btn" title="编辑" @click="openEditor(e)">⚙</button>
                <button class="kb-admin-btn" title="发布到博客" @click="openPublish(e)">📝</button>
                <button class="kb-admin-btn" title="导出 Markdown" @click="exportOne(e)">⬇</button>
                <button class="kb-admin-btn hover:!text-red-400" title="删除" @click="onDelete(e)">🗑</button>
              </div>
              <div v-else class="flex gap-1 shrink-0" @click.stop>
                <button class="kb-admin-btn" title="导出 Markdown" @click="exportOne(e)">⬇</button>
              </div>
            </div>
            <p class="text-xs text-gray-400 mt-2 line-clamp-3 leading-relaxed">{{ e.summary || stripMd(e.content) }}</p>
            <div class="flex flex-wrap items-center gap-1.5 mt-3">
              <span class="kb-badge cat">{{ taxonomy.find(t => t.key === e.category)?.icon || '📘' }} {{ e.category }}</span>
              <span v-if="e.subcategory" class="kb-badge">{{ e.subcategory }}</span>
              <span v-for="t in e.tags.slice(0, 3)" :key="t" class="kb-badge">#{{ t }}</span>
              <span v-if="dueMode && isDue(e)" class="kb-badge due">⏰ 待复习</span>
            </div>
            <div class="flex items-center justify-between mt-3 text-[11px] text-gray-500">
              <span class="flex items-center gap-1">
                难度
                <i v-for="n in 5" :key="n" class="inline-block w-1.5 h-1.5 rounded-full"
                   :style="{ background: n <= e.difficulty ? '#a78bfa' : 'rgba(255,255,255,.12)' }"></i>
              </span>
              <span>{{ fmtDate(e.updatedAt) }}</span>
            </div>
          </article>
        </div>
      </section>
    </div>

    <!-- ============ 站主登录弹窗 ============ -->
    <Modal v-model="showLogin" title="站主登录 · Supabase">
      <div class="space-y-3">
        <p class="text-xs text-gray-400">知识库写权限独立于站点账号，使用 Supabase 邮箱密码（在 Supabase 后台 Authentication 创建）。</p>
        <input v-model="loginForm.email" class="web3-input text-sm" placeholder="邮箱" />
        <input v-model="loginForm.password" type="password" class="web3-input text-sm" placeholder="密码" @keyup.enter="doLogin" />
        <p v-if="loginError" class="text-xs text-red-400">{{ loginError }}</p>
        <div class="flex justify-end gap-2">
          <button class="web3-btn-outline text-xs !px-4 !py-2" @click="showLogin = false">取消</button>
          <button class="web3-btn text-xs !px-4 !py-2" :disabled="loginBusy" @click="doLogin">
            {{ loginBusy ? '登录中…' : '登录' }}
          </button>
        </div>
      </div>
    </Modal>

    <!-- ============ 复习弹窗：自评记忆质量，触发艾宾浩斯调度 ============ -->
    <Modal v-model="showReview" :title="'复习：' + (reviewEntry?.title || '')">
      <div v-if="reviewEntry" class="space-y-4">
        <div class="text-xs text-gray-400">先尝试回忆内容，再展开核对，然后自评记忆质量。</div>
        <div class="kb-body article-body" v-html="renderMd(reviewEntry.content)"></div>
        <div class="grid grid-cols-3 gap-2">
          <button v-for="q in reviewQualities" :key="q.v" class="kb-quality-btn"
            :style="{ background: q.bg, borderColor: q.bg }" @click="doReview(q.v)">
            <div class="text-lg">{{ q.icon }}</div>
            <div class="text-xs">{{ q.label }}</div>
          </button>
        </div>
      </div>
    </Modal>

    <!-- ============ 发布到博客弹窗 ============ -->
    <Modal v-model="showPublish" title="发布到博客">
      <div v-if="publishEntry" class="space-y-3">
        <p class="text-xs text-gray-400">将知识库条目「{{ publishEntry.title }}」发布为博客文章。可在发布前调整标题、分类、标签。</p>
        <input v-model="publishForm.title" class="web3-input text-sm" placeholder="文章标题" />
        <input v-model="publishForm.summary" class="web3-input text-sm" placeholder="摘要" />
        <div class="grid grid-cols-2 gap-3">
          <select v-model="publishForm.category" class="web3-input text-sm">
            <option v-for="c in blogCategories" :key="c" :value="c">{{ c }}</option>
          </select>
          <input v-model="publishForm.tags" class="web3-input text-sm" placeholder="标签，逗号分隔" />
        </div>
        <label class="flex items-center gap-2 text-xs text-white/70 cursor-pointer">
          <input type="checkbox" v-model="publishForm.published" class="accent-purple-500" /> 立即发布（取消则存为草稿）
        </label>
        <label class="flex items-center gap-2 text-xs text-white/70 cursor-pointer">
          <input type="checkbox" v-model="publishForm.isTop" class="accent-purple-500" /> 置顶
        </label>
        <p v-if="publishError" class="text-xs text-red-400">{{ publishError }}</p>
        <div class="flex justify-end gap-2">
          <button class="web3-btn-outline text-xs !px-4 !py-2" @click="showPublish = false">取消</button>
          <button class="web3-btn text-xs !px-4 !py-2" :disabled="publishBusy" @click="doPublish">
            {{ publishBusy ? '发布中…' : '📝 发布' }}
          </button>
        </div>
      </div>
    </Modal>

    <!-- ============ 知识条目编辑器 ============ -->
    <Modal v-model="showEditor" :title="editorForm.id ? '编辑知识条目' : '新增知识条目'">
      <div class="space-y-3">
        <input v-model="editorForm.title" class="web3-input text-sm" placeholder="标题 *" />
        <input v-model="editorForm.summary" class="web3-input text-sm" placeholder="一句话摘要（列表展示）" />
        <div class="grid grid-cols-2 gap-3">
          <select v-model="editorForm.category" class="web3-input text-sm" @change="syncSubOptions">
            <option v-for="c in taxonomy" :key="c.key" :value="c.key">{{ c.icon }} {{ c.key }}</option>
          </select>
          <select v-model="editorForm.subcategory" class="web3-input text-sm">
            <option value="">二级分类（可空）</option>
            <option v-for="s in currentSubs" :key="s" :value="s">{{ s }}</option>
          </select>
        </div>
        <div class="grid grid-cols-3 gap-3">
          <select v-model="editorForm.stage" class="web3-input text-sm">
            <option v-for="s in stages" :key="s.key" :value="s.key">{{ s.icon }} {{ s.label }}</option>
          </select>
          <select v-model="editorForm.difficulty" class="web3-input text-sm">
            <option v-for="n in 5" :key="n" :value="n">难度 {{ n }}</option>
          </select>
          <select v-model="editorForm.source" class="web3-input text-sm">
            <option value="原创">原创</option>
            <option value="官方文档">官方文档</option>
            <option value="书籍笔记">书籍笔记</option>
            <option value="实战总结">实战总结</option>
            <option value="转载">转载</option>
          </select>
        </div>
        <input v-model="tagsInput" class="web3-input text-sm" placeholder="标签，逗号分隔（如：Linux, TCP, 性能优化）" />
        <input v-model="editorForm.sourceUrl" class="web3-input text-sm" placeholder="参考来源 URL（可空）" />
        <textarea v-model="editorForm.content" rows="12" class="web3-input text-sm w-full font-mono resize-y"
                  placeholder="Markdown 正文… 支持在正文中放学习笔记、命令、代码块、踩坑记录"></textarea>
        <label class="flex items-center gap-2 text-xs text-white/70 cursor-pointer">
          <input type="checkbox" v-model="editorForm.published" class="accent-purple-500" /> 立即发布（取消则存为仅自己可见的草稿）
        </label>
        <p v-if="editorError" class="text-xs text-red-400">{{ editorError }}</p>
        <div class="flex justify-end gap-2">
          <button class="web3-btn-outline text-xs !px-4 !py-2" @click="showEditor = false">取消</button>
          <button class="web3-btn text-xs !px-4 !py-2" :disabled="editorBusy" @click="saveEntry">
            {{ editorBusy ? '保存中…' : '保存' }}
          </button>
        </div>
      </div>
    </Modal>
  </div>
</template>

<script setup>
// ============================================================
// 知识库主页：分类树 + 成长体系 + 艾宾浩斯复习 + 搜索
//   + 导出 Markdown + 发布到博客 + 站主（Supabase Auth）管理
// ============================================================
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import Modal from '@/components/common/Modal.vue'
import { useToastStore } from '@/stores/modules/toast'
import { confirm as dlgConfirm } from '@/composables/useDialog'
import { renderMarkdown } from '@/utils/markdown'
import { KB_TAXONOMY as taxonomy, STAGES as stages, stageMeta } from '@/data/knowledgeTaxonomy'
import {
  listEntries, getGrowthStats, getDueCount, isTableReady, INIT_SQL,
  ownerLogin, ownerLogout, onAuthChange,
  createEntry, updateEntry, deleteEntry, advanceStage,
  scheduleReview, publishToBlog, entryToMarkdown, exportAllToMarkdown,
} from '@/api/knowledge'

const router = useRouter()
const toast = useToastStore()

const tableReady = ref(true)       // kb_entries 表是否已创建
const sqlCopied = ref(false)
const entries = ref([])
const stats = ref(null)
const dueCount = ref(0)
const dueMode = ref(false)
const loading = ref(false)
const keyword = ref('')
const activeCategory = ref('')
const activeSub = ref('')
const activeStage = ref('')

const stageFilters = stages

async function copyInitSql() {
  try {
    await navigator.clipboard.writeText(INIT_SQL)
    sqlCopied.value = true
    setTimeout(() => (sqlCopied.value = false), 4000)
  } catch {
    toast.error('复制失败，请手动打开项目 supabase/0001_knowledge_and_blog_seed.sql')
  }
}
const growingCount = computed(() => {
  let n = 0
  for (const k in stats.value?.byCat || {}) n += stats.value.byCat[k].growing || 0
  return n
})

async function checkTable() {
  tableReady.value = await isTableReady()
}

async function retryCheck() {
  loading.value = true
  await checkTable()
  if (tableReady.value) {
    toast.success('检测到知识库表，正在加载…')
    await reload()
  } else {
    toast.warning('仍未检测到 kb_entries 表，请确认 SQL 已在 Supabase 执行成功')
  }
  loading.value = false
}

async function reload() {
  if (!tableReady.value) return
  loading.value = true
  try {
    entries.value = await listEntries({
      category: activeCategory.value,
      subcategory: activeSub.value,
      stage: activeStage.value,
      keyword: keyword.value.trim(),
      due: dueMode.value,
    })
    stats.value = await getGrowthStats()
    dueCount.value = await getDueCount()
  } catch (e) {
    // 表不存在时给出初始化引导而非红色报错
    const msg = String(e.message || '')
    if (msg.includes("'public.kb_entries'")) {
      tableReady.value = false
    } else {
      toast.error('知识库加载失败：' + msg)
    }
  } finally {
    loading.value = false
  }
}

function selectCategory(key) {
  activeSub.value = ''
  activeCategory.value = activeCategory.value === key ? '' : key
}

function resetFilter() {
  activeCategory.value = ''
  activeSub.value = ''
  activeStage.value = ''
  dueMode.value = false
  keyword.value = ''
}

watch([activeCategory, activeStage, activeSub, dueMode], () => reload())

function goDetail(e) {
  if (dueMode.value) { openReview(e); return }
  router.push(`/knowledge/${e.id}`)
}

function fmtDate(d) { return d ? String(d).slice(0, 10) : '' }

function stripMd(md) {
  return String(md || '').replace(/[#>*`_\-\[\]()!|]/g, ' ').replace(/\s+/g, ' ').slice(0, 120)
}

function isDue(e) {
  if (!e.nextReview) return true
  return new Date(e.nextReview) <= new Date()
}

function renderMd(md) { return renderMarkdown(md || '') }

// ---- 站主登录/登出 ----
const owner = ref(null)
const showLogin = ref(false)
const loginBusy = ref(false)
const loginError = ref('')
const loginForm = ref({ email: '', password: '' })
let unsubAuth = null

async function doLogin() {
  loginBusy.value = true
  loginError.value = ''
  try {
    await ownerLogin(loginForm.value.email.trim(), loginForm.value.password)
    showLogin.value = false
    toast.success('站主已登录，可以维护知识库了')
    await reload()
  } catch (e) {
    loginError.value = e.message || '登录失败'
  } finally {
    loginBusy.value = false
  }
}
async function doLogout() {
  await ownerLogout()
  owner.value = null
  toast.info('已退出站主登录')
}

// ---- 条目编辑器 ----
const showEditor = ref(false)
const editorBusy = ref(false)
const editorError = ref('')
const editorForm = ref(blankForm())
const tagsInput = ref('')
const currentSubs = computed(() => taxonomy.find(t => t.key === editorForm.value.category)?.children || [])

function blankForm() {
  return {
    id: null, title: '', summary: '', category: '软件工程', subcategory: '',
    tags: [], stage: 'seedling', source: '原创', sourceUrl: '', difficulty: 1,
    published: true, content: '',
  }
}
function syncSubOptions() {
  if (!currentSubs.value.includes(editorForm.value.subcategory)) editorForm.value.subcategory = ''
}
function openEditor(entry) {
  editorError.value = ''
  if (entry) {
    editorForm.value = { ...entry, published: true }
    tagsInput.value = entry.tags.join(', ')
  } else {
    editorForm.value = blankForm()
    tagsInput.value = ''
  }
  showEditor.value = true
}
async function saveEntry() {
  if (!editorForm.value.title.trim()) { editorError.value = '标题必填'; return }
  editorBusy.value = true
  editorError.value = ''
  try {
    const payload = { ...editorForm.value, tags: tagsInput.value }
    if (editorForm.value.id) await updateEntry(editorForm.value.id, payload)
    else await createEntry(payload)
    showEditor.value = false
    toast.success('知识已保存，知识库又长大了一点 🌱')
    await reload()
  } catch (e) {
    editorError.value = e.message || '保存失败（需站主登录 / 检查 kb_entries 表）'
  } finally {
    editorBusy.value = false
  }
}
async function onDelete(entry) {
  if (!(await dlgConfirm(`确定删除「${entry.title}」？`))) return
  try {
    await deleteEntry(entry.id)
    entries.value = entries.value.filter(x => x.id !== entry.id)
    toast.success('已删除')
  } catch (e) { toast.error(e.message || '删除失败') }
}
async function onAdvance(entry) {
  try {
    const updated = await advanceStage(entry)
    const i = entries.value.findIndex(x => x.id === entry.id)
    if (i >= 0) entries.value[i] = updated
    stats.value = await getGrowthStats()
    toast.success(`成长阶段 → ${stageMeta(updated.stage).icon} ${stageMeta(updated.stage).label}`)
  } catch (e) { toast.error(e.message || '更新失败') }
}

// ---- 艾宾浩斯复习 ----
const showReview = ref(false)
const reviewEntry = ref(null)
const reviewQualities = [
  { v: 0, icon: '😵', label: '完全忘记', bg: 'rgba(239,68,68,.15)' },
  { v: 2, icon: '😓', label: '勉强想起', bg: 'rgba(251,146,60,.15)' },
  { v: 3, icon: '🙂', label: '基本记住', bg: 'rgba(250,204,21,.15)' },
  { v: 4, icon: '😊', label: '轻松回忆', bg: 'rgba(52,211,153,.15)' },
  { v: 5, icon: '🤩', label: '了如指掌', bg: 'rgba(34,211,238,.15)' },
]

function openReview(entry) {
  reviewEntry.value = entry
  showReview.value = true
}
async function doReview(quality) {
  if (!reviewEntry.value) return
  try {
    const updated = await scheduleReview(reviewEntry.value, quality)
    const i = entries.value.findIndex(x => x.id === reviewEntry.value.id)
    if (i >= 0) entries.value.splice(i, 1)
    dueCount.value = Math.max(0, dueCount.value - 1)
    const next = new Date(updated.nextReview)
    const days = Math.ceil((next - Date.now()) / 86400000)
    toast.success(`已记录，下次复习：${days > 0 ? days + ' 天后' : '今天'}`)
    showReview.value = false
    reviewEntry.value = null
    if (dueMode.value && entries.value.length === 0) {
      toast.success('🎉 今日复习已完成')
      dueMode.value = false
    }
  } catch (e) {
    toast.error(e.message || '复习记录失败')
  }
}

// ---- 导出 Markdown ----
function download(filename, text) {
  const blob = new Blob([text], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = filename
  document.body.appendChild(a); a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}
function exportOne(entry) {
  const md = entryToMarkdown(entry)
  download(`${entry.title || 'knowledge'}.md`, md)
  toast.success('已导出 Markdown')
}
async function exportAll() {
  try {
    const md = await exportAllToMarkdown()
    download(`知识库-全量导出-${new Date().toISOString().slice(0, 10)}.md`, md)
    toast.success('已导出全部知识库')
  } catch (e) { toast.error(e.message || '导出失败') }
}

// ---- 发布到博客 ----
const showPublish = ref(false)
const publishEntry = ref(null)
const publishBusy = ref(false)
const publishError = ref('')
const publishForm = ref({ title: '', summary: '', category: '软件工程', tags: '', published: true, isTop: false })
const blogCategories = ['硬件底层', '嵌入式', '软件工程', '网络工程', '网络安全', '运维与效率', '前沿AI', 'Web3', '建站记录', '随笔']

function openPublish(entry) {
  if (!owner.value) { toast.info('请先登录站主'); showLogin.value = true; return }
  publishEntry.value = entry
  publishError.value = ''
  publishForm.value = {
    title: entry.title,
    summary: entry.summary || '',
    category: entry.category || '软件工程',
    tags: entry.tags.join(', '),
    published: true,
    isTop: false,
  }
  showPublish.value = true
}
async function doPublish() {
  if (!publishEntry.value) return
  publishBusy.value = true
  publishError.value = ''
  try {
    const res = await publishToBlog(publishEntry.value, {
      title: publishForm.value.title,
      summary: publishForm.value.summary,
      category: publishForm.value.category,
      tags: publishForm.value.tags,
      published: publishForm.value.published,
      isTop: publishForm.value.isTop,
    })
    showPublish.value = false
    toast.success(`已发布到博客 📝${publishForm.value.published ? '' : '（草稿）'}`)
    // 跳转到新博客
    setTimeout(() => router.push(`/blog/post/${res.id}`), 600)
  } catch (e) {
    publishError.value = e.message || '发布失败'
  } finally {
    publishBusy.value = false
  }
}

onMounted(async () => {
  await checkTable()
  if (tableReady.value) await reload()
  unsubAuth = onAuthChange(u => { owner.value = u })
})
onUnmounted(() => unsubAuth && unsubAuth())
</script>

<style scoped>
.growth-card {
  background: rgba(255,255,255,.04);
  border: 1px solid rgba(255,255,255,.08);
  border-radius: 12px;
  padding: 12px 14px;
}
.kb-cat-row {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 9px;
  font-size: 13px;
  color: rgba(255,255,255,.72);
  transition: all .2s;
}
.kb-cat-row:hover { background: rgba(255,255,255,.06); color: #fff; }
.kb-cat-row.active {
  background: linear-gradient(90deg, rgba(34,211,238,.14), rgba(168,85,247,.14));
  color: #67e8f9;
  border: 1px solid rgba(103,232,249,.25);
}
.kb-sub-row {
  display: block;
  width: 100%;
  text-align: left;
  padding: 5px 8px;
  font-size: 12px;
  color: rgba(255,255,255,.55);
  border-radius: 6px;
  transition: all .2s;
}
.kb-sub-row:hover { color: #fff; background: rgba(255,255,255,.05); }
.kb-sub-row.active { color: #67e8f9; }
.kb-stage-chip {
  font-size: 11px;
  padding: 4px 9px;
  border-radius: 999px;
  border: 1px solid rgba(255,255,255,.12);
  color: rgba(255,255,255,.65);
  transition: all .2s;
}
.kb-stage-chip:hover { border-color: rgba(103,232,249,.4); }
.kb-stage-chip.active { background: rgba(34,211,238,.12); border-color: rgba(103,232,249,.4); color: #67e8f9; }
.kb-filter-tag {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 3px 10px; border-radius: 999px;
  background: rgba(168,85,247,.12); border: 1px solid rgba(168,85,247,.28);
  color: #d8b4fe;
}
.kb-filter-tag i { font-style: normal; cursor: pointer; opacity: .7; }
.kb-entry-card {
  background: rgba(255,255,255,.035);
  border: 1px solid rgba(255,255,255,.08);
  border-radius: 14px;
  padding: 16px;
  cursor: pointer;
  transition: transform .25s ease-out, box-shadow .25s ease-out, border-color .25s;
}
.kb-entry-card:hover {
  transform: translateY(-3px);
  border-color: rgba(103,232,249,.3);
  box-shadow: 0 0 18px rgba(103,232,249,.08), 0 8px 24px rgba(0,0,0,.25);
}
.line-clamp-3 {
  display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden;
}
.kb-badge {
  font-size: 10.5px;
  padding: 2px 8px;
  border-radius: 6px;
  background: rgba(255,255,255,.06);
  color: rgba(255,255,255,.6);
  border: 1px solid rgba(255,255,255,.07);
}
.kb-badge.cat { background: rgba(34,211,238,.08); color: #67e8f9; border-color: rgba(34,211,238,.2); }
.kb-admin-btn {
  width: 24px; height: 24px;
  border-radius: 6px;
  font-size: 12px;
  color: rgba(255,255,255,.5);
  transition: all .2s;
}
.kb-admin-btn:hover { background: rgba(255,255,255,.1); color: #fff; }
.kb-badge.due { background: rgba(251,146,60,.12); color: #fdba74; border-color: rgba(251,146,60,.3); }
.kb-action-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 7px 14px; border-radius: 10px; font-size: 12.5px;
  background: rgba(255,255,255,.05); border: 1px solid rgba(255,255,255,.1);
  color: rgba(255,255,255,.8); transition: all .2s;
}
.kb-action-btn:hover { border-color: rgba(103,232,249,.35); color: #fff; }
.kb-action-btn.due { background: rgba(251,146,60,.1); border-color: rgba(251,146,60,.35); color: #fdba74; }
.kb-action-btn b {
  background: #fb923c; color: #fff; font-size: 10px; padding: 1px 6px; border-radius: 999px;
}
.kb-quality-btn {
  padding: 14px 8px; border-radius: 12px; border: 1px solid;
  text-align: center; transition: transform .15s;
}
.kb-quality-btn:hover { transform: translateY(-2px); }
.kb-body { line-height: 1.7; color: rgba(255,255,255,.82); font-size: 14px; }
</style>
