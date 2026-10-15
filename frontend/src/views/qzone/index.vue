<template>
  <div class="qz-page">
    <div class="mx-auto max-w-6xl px-6 md:px-12 py-10">
      <!-- ── Header：90s CRT 终端大字报 ── -->
      <div class="mb-8 flex flex-wrap items-end justify-between gap-4">
        <div class="flex-1 min-w-0">
          <div class="qz-kicker">QZONE.EXE —— RETRO MEMORY BACKUP v2.0</div>
          <h1 class="qz-title">
            <span class="qz-title-glow">时光回溯 · QQ 空间考古</span><span class="blink-cursor">█</span>
          </h1>
          <p class="qz-subtitle">
            <span class="qz-blink">▣</span> 接入 90 年代复古终端 · 扫码即回千年虫前的互联网往事 <span class="qz-blink">▣</span>
          </p>
        </div>
        <div class="qz-hud text-right">
          <div>SYS.STATUS: <span class="text-emerald-400">● ONLINE</span></div>
          <div>NODE: {{ healthText }}</div>
          <div class="qz-hud-time">{{ nowTime }}</div>
        </div>
      </div>

      <!-- ── ASCII 分隔线 ── -->
      <pre class="qz-ascii">╔══════════════════════════════════════════════════════════════╗</pre>

      <!-- ── 功能介绍 README ── -->
      <div class="qz-card qz-intro mb-6">
        <div class="term-bar">
          <span class="term-dot"></span><span class="term-dot"></span><span class="term-dot"></span>
          <span class="term-title">README.TXT —— 这是什么 / 能做什么</span>
        </div>

        <!-- 一句话定义 -->
        <div class="intro-hero">
          <span class="intro-prompt">$</span>
          <span class="intro-cmd">时光回溯.exe</span>
          <span class="intro-desc">—— 把你的 QQ 空间说说完整打包下载到本地，永远存下来。</span>
        </div>
        <p class="intro-lead">
          用手机 QQ 扫码登录自己的空间，工具会在服务器后台逐页抓取你<strong class="text-emerald-300">所有可见的说说</strong>，
          连同图片、视频、评论、地点、来源端一起导出成
          <span class="tag">JSON</span> <span class="tag">HTML</span> <span class="tag">CSV</span> 三种格式，
          供你本地永久存档。数据只在服务器临时保留 <strong class="text-amber-300">24 小时</strong>，到期自动清除。
        </p>

        <div class="grid md:grid-cols-2 gap-5 mt-5">
          <!-- 左：能抓到什么 -->
          <div>
            <div class="intro-h">▸ 能备份的内容（每条说说包含）</div>
            <ul class="intro-list">
              <li><b>📝 说说正文</b> — 文字内容、发布时间、当时昵称</li>
              <li><b>🖼 图片</b> — 高清原图直链（含宽高），可批量下载</li>
              <li><b>🎬 视频</b> — 原视频直链，可直接播放/保存</li>
              <li><b>💬 评论 + 楼中楼</b> — 评论人、内容、时间、嵌套回复</li>
              <li><b>📍 地点定位</b> — 说说附带的地理位置</li>
              <li><b>📱 来源端</b> — 来自 QQ空间 / 手机QQ / 微博等</li>
              <li><b>🔒 私密标记</b> — "仅自己可见"的说说会标注</li>
              <li><b>📊 统计汇总</b> — 条数、时间跨度、图片/视频/评论计数</li>
            </ul>
          </div>

          <!-- 右：工作流程 + 安全 -->
          <div>
            <div class="intro-h">▸ 工作流程（3 步）</div>
            <ol class="intro-list intro-ol">
              <li><b>扫码登录</b> — 手机 QQ 扫二维码，确认授权你的空间</li>
              <li><b>后台抓取</b> — 服务器分页拉取全部说说，可关页面进度不丢</li>
              <li><b>下载导出</b> — 完成后一键下载 JSON / HTML / CSV</li>
            </ol>

            <div class="intro-h mt-4">▸ 安全与隐私</div>
            <ul class="intro-list intro-safe">
              <li>登录凭据只存服务器内存，不写磁盘、不落库</li>
              <li>导出文件 24 小时后自动删除，不留痕</li>
              <li>仅抓取你账号可见的内容，他人私密内容无法获取</li>
            </ul>
          </div>
        </div>

        <!-- 限制说明：诚实告知 -->
        <div class="intro-limit">
          <span class="intro-limit-tag">⚠ 能力边界</span>
          <div>
            <strong>已删除的说说无法恢复</strong>——QQ 空间删除后服务器端也会清除，本工具只能抓取「登录后当前可见」的说说。
            若你想保留某条说说，请在删除前来这里备份。相册 / 日志 / 留言板 / 访客记录暂不支持（后续版本考虑加入）。
          </div>
        </div>
      </div>

      <!-- ── 可获取内容清单 ── -->
      <div class="qz-card p-4 mb-5">
        <div class="term-bar -mx-4 -mt-4 mb-3">
          <span class="term-dot"></span><span class="term-dot"></span><span class="term-dot"></span>
          <span class="term-title">WHAT_YOU_GET.TXT —— 可备份的内容清单</span>
        </div>
        <div class="grid grid-cols-2 md:grid-cols-4 gap-2 font-mono text-[11px]">
          <div v-for="c in contentList" :key="c.t" class="content-chip">
            <span class="text-cyan-300">{{ c.i }}</span> {{ c.t }}
            <span class="block text-[9px] text-slate-500 mt-0.5">{{ c.d }}</span>
          </div>
        </div>
        <p class="font-mono text-[10px] text-amber-300/60 mt-3 leading-relaxed">
          <span class="text-cyan-400/70">!NOTICE:</span>
          仅能备份"你账号登录后可见"的说说，他人的私密内容无法获取；暂不含相册/日志/留言板。
        </p>
      </div>

      <!-- ── STEP 01 扫码登录 ── -->
      <div class="qz-card p-4 mb-5" :class="{ 'qz-active': !loginUser }">
        <div class="term-bar -mx-4 -mt-4 mb-3">
          <span class="term-dot"></span><span class="term-dot"></span><span class="term-dot"></span>
          <span class="term-title">STEP_01 // 扫码登录 QQ 空间</span>
          <span class="term-status" :class="loginStatusClass">{{ loginStatusText }}</span>
        </div>

        <div class="flex flex-col md:flex-row gap-5 items-center md:items-start">
          <div class="qr-box">
            <img
              v-if="qrImg"
              :src="qrImg"
              alt="QQ 登录二维码"
              class="w-44 h-44 rounded"
              :class="{ 'qr-dim': loginStatus === 'expired' || loginStatus === 'canceled' }"
            />
            <div
              v-else
              class="w-44 h-44 rounded flex items-center justify-center font-mono text-[10px] text-cyan-400/50 leading-5 text-center"
            >
              NO SIGNAL<br />点击右侧按钮<br />召唤登录矩阵
            </div>
            <div v-if="qrLoading" class="qr-mask"><span class="spinner"></span></div>
            <div class="scanline" v-if="qrImg && loginStatus === 'waiting'"></div>
          </div>

          <div class="flex-1 w-full space-y-3">
            <template v-if="!loginUser">
              <p class="text-xs text-slate-300/90 leading-relaxed">
                使用手机 QQ 扫描左侧矩阵码并确认登录。二维码约 2 分钟失效，过期可重新获取。 登录凭据<span
                  class="text-cyan-300"
                  >只保存在服务器内存</span
                >，不写磁盘、不落库。
              </p>
              <div v-if="loginStatus === 'scanned'" class="font-mono text-[11px] text-cyan-300 flex items-center gap-2">
                <span class="spinner-sm"></span>SCANNED // 请在手机上点击确认授权…
              </div>
              <div v-else-if="loginStatus === 'expired'" class="font-mono text-[11px] text-red-400">
                SESSION EXPIRED // 二维码已失效
              </div>
              <div v-else-if="loginStatus === 'canceled'" class="font-mono text-[11px] text-red-400">
                ACCESS DENIED // 已取消授权
              </div>
              <button @click="getQr" :disabled="qrLoading" class="qz-btn">
                {{ qrImg ? '↻ 重新获取二维码' : '▶ 获取登录二维码' }}
              </button>
            </template>
            <template v-else>
              <div class="flex items-center gap-3 p-3 rounded-lg border border-cyan-400/25 bg-cyan-400/5">
                <img
                  :src="loginUser.avatar"
                  class="w-12 h-12 rounded-full border border-cyan-400/30"
                  @error="onAvatarError"
                />
                <div>
                  <div class="text-sm font-bold text-cyan-100">{{ loginUser.name || '神秘冒险者' }}</div>
                  <div class="font-mono text-[10px] text-cyan-400/60">UID: {{ loginUser.uin }} // ACCESS GRANTED</div>
                </div>
                <span class="ml-auto font-mono text-[9px] text-emerald-400 flex items-center gap-1.5">
                  <span class="w-1.5 h-1.5 rounded-full bg-emerald-400 shadow-[0_0_6px_#34d399]"></span>ONLINE
                </span>
              </div>
              <button
                @click="resetLogin"
                class="font-mono text-[10px] text-slate-400 hover:text-cyan-300 underline underline-offset-2 transition"
              >
                ↩ 切换账号重新认证
              </button>
            </template>
          </div>
        </div>
      </div>

      <!-- ── STEP 02 参数与启动 ── -->
      <div
        class="qz-card p-4 mb-5"
        :class="{ 'qz-active': loginUser && !job, 'opacity-50 pointer-events-none': !loginUser }"
      >
        <div class="term-bar -mx-4 -mt-4 mb-3">
          <span class="term-dot"></span><span class="term-dot"></span><span class="term-dot"></span>
          <span class="term-title">STEP_02 // 配置抓取参数</span>
        </div>

        <div class="grid md:grid-cols-2 gap-4">
          <div>
            <label class="block font-mono text-[10px] text-cyan-400/70 mb-1.5 tracking-wider"
              >① 抓取节奏 / REQUEST INTERVAL</label
            >
            <select v-model="interval" :disabled="jobRunning" class="qz-select">
              <option v-for="o in intervalOptions" :key="o.v" :value="o.v">{{ o.label }}</option>
            </select>
          </div>
          <div>
            <label class="block font-mono text-[10px] text-cyan-400/70 mb-1.5 tracking-wider"
              >② 并发线程 / THREADS</label
            >
            <select v-model="workers" :disabled="jobRunning" class="qz-select">
              <option v-for="o in workerOptions" :key="o.v" :value="o.v">{{ o.label }}</option>
            </select>
          </div>
        </div>

        <div class="flex items-center gap-3 mt-4">
          <button @click="startFetch" :disabled="!loginUser || jobRunning || starting" class="qz-btn qz-btn-primary">
            {{ starting ? 'BOOTING...' : '▶▶ 开始回溯全部说说' }}
          </button>
          <span class="font-mono text-[9px] text-slate-500 leading-4">
            任务在服务器后台线程运行<br />切走页面 / 关掉再回来，进度不丢
          </span>
        </div>
      </div>

      <!-- ── STEP 03 进度 ── -->
      <div v-if="job" class="qz-card p-4 mb-5 qz-active">
        <div class="term-bar -mx-4 -mt-4 mb-3">
          <span class="term-dot"></span><span class="term-dot"></span><span class="term-dot"></span>
          <span class="term-title">STEP_03 // FETCH PROGRESS</span>
          <span class="term-status" :class="jobStatusClass">{{ jobStatusText }}</span>
        </div>

        <div>
          <div
            class="font-mono text-[13px] text-cyan-300 tracking-wider"
            style="text-shadow: 0 0 10px rgba(34, 211, 238, 0.45)"
          >
            {{ asciiBar }} {{ pct }}%
          </div>
          <div class="h-2 rounded-full bg-slate-900/80 border border-cyan-400/15 overflow-hidden mt-2.5">
            <div
              class="h-full rounded-full bg-gradient-to-r from-cyan-400 via-sky-400 to-fuchsia-400 transition-all duration-500"
              :class="{ 'progress-indeterminate': !job.total && jobRunning }"
              :style="{ width: Math.max(pct, jobRunning ? 6 : 0) + '%' }"
            ></div>
          </div>
          <div class="flex flex-wrap items-center gap-x-4 gap-y-1 mt-2 font-mono text-[10px] text-slate-400">
            <span class="text-cyan-300/80">{{ progressPhase }}</span>
            <span
              >{{ job.fetched }}<template v-if="job.total != null"> / {{ job.total }}</template> 条</span
            >
            <span v-if="job.total_pages">PAGE {{ job.page }}/{{ job.total_pages }}</span>
            <span v-if="job.workers">{{ job.workers }}×THREAD</span>
            <span v-if="jobRunning" class="flex items-center gap-1 text-cyan-300"
              ><span class="spinner-sm"></span>RUNNING</span
            >
            <span v-if="job.finished_at" class="text-slate-500">ENDED {{ job.finished_at }}</span>
          </div>
        </div>

        <div v-if="job.logs?.length" class="mt-3 log-box">
          <div v-for="(l, i) in job.logs.slice(-8)" :key="i" class="font-mono text-[10px] text-cyan-200/60 leading-5">
            <span class="text-fuchsia-400/60">&gt;</span> {{ l }}
          </div>
        </div>

        <div v-if="jobRunning" class="mt-3">
          <button
            @click="onCancel"
            class="font-mono text-[10px] px-3 py-1.5 rounded border border-red-400/30 text-red-300/90 hover:bg-red-500/10 transition"
          >
            ■ ABORT 取消（已抓数据仍可导出）
          </button>
        </div>
      </div>

      <!-- ── STEP 04 结果 ── -->
      <div v-if="job && jobFinished" class="qz-card p-4 mb-5 qz-active">
        <div class="term-bar -mx-4 -mt-4 mb-3">
          <span class="term-dot"></span><span class="term-dot"></span><span class="term-dot"></span>
          <span class="term-title">STEP_04 // EXPORT &amp; PREVIEW</span>
        </div>

        <div
          v-if="job.status === 'failed'"
          class="p-3 rounded-lg border border-red-400/25 bg-red-500/5 font-mono text-[11px] text-red-300/90 mb-3"
        >
          FATAL: {{ job.error || '未知错误' }}。已抓取的部分仍可导出，也可重新登录再试。
        </div>
        <div
          v-else-if="failedHint"
          class="p-3 rounded-lg border border-amber-400/25 bg-amber-500/5 font-mono text-[11px] text-amber-300/90 mb-3"
        >
          WARN: {{ failedHint }}
        </div>

        <div class="grid grid-cols-2 md:grid-cols-4 gap-2.5">
          <div class="stat-chip">
            <div class="stat-num">{{ job.fetched }}</div>
            <div class="stat-label">说说 MOMENTS</div>
          </div>
          <div class="stat-chip">
            <div class="stat-num">{{ job.pic_count }}</div>
            <div class="stat-label">图片 PHOTOS</div>
          </div>
          <div class="stat-chip">
            <div class="stat-num">{{ job.comment_count }}</div>
            <div class="stat-label">评论 COMMENTS</div>
          </div>
          <div class="stat-chip">
            <div class="stat-num">{{ job.video_count }}</div>
            <div class="stat-label">视频 VIDEOS</div>
          </div>
        </div>
        <div v-if="job.range_from" class="font-mono text-[10px] text-slate-500 mt-2">
          TIMESPAN: {{ job.range_from }} → {{ job.range_to }}
        </div>

        <div class="flex flex-wrap gap-2.5 mt-4">
          <a v-for="d in downloads" :key="d.fmt" :href="downloadUrl(job.id, d.fmt)" class="qz-btn">
            {{ d.icon }} {{ d.label }}
          </a>
          <button
            @click="onClear"
            class="ml-auto font-mono text-[10px] px-3 py-2 rounded border border-slate-600/40 text-slate-400 hover:text-red-300 hover:border-red-400/30 transition"
          >
            DEL 清除本次记录
          </button>
        </div>
        <p class="font-mono text-[9px] text-slate-500 mt-2">
          LINK EXPIRES IN 24H —— 服务器仅临时保存 24 小时，请尽快下载
        </p>

        <!-- 预览 -->
        <div class="mt-5 pt-4 border-t border-cyan-400/10">
          <div class="font-mono text-xs text-cyan-300/90 mb-3 tracking-wider">
            ▍PREVIEW —— {{ moments.length }} / {{ job.fetched }} 条
          </div>
          <div class="space-y-2.5">
            <div v-for="m in moments" :key="m.tid" class="moment-item">
              <div class="font-mono text-[10px] text-cyan-300/80 flex flex-wrap items-center gap-2">
                <span>{{ m.time }}</span>
                <span v-if="m.source" class="text-slate-500">◈ {{ m.source }}</span>
                <span v-if="m.lbs" class="text-fuchsia-300/60">⚑ {{ m.lbs }}</span>
                <span v-if="m.secret" class="text-amber-300/70">🔒 仅自己可见</span>
              </div>
              <div
                v-if="m.content_html"
                class="qz-content text-xs text-slate-200/90 mt-1.5 whitespace-pre-wrap break-words leading-relaxed"
                v-html="m.content_html"
              ></div>
              <div v-if="m.pics?.length" class="flex flex-wrap gap-1.5 mt-2">
                <a v-for="(p, pi) in m.pics.slice(0, 9)" :key="pi" :href="p.url" target="_blank" rel="noopener">
                  <img
                    :src="p.small"
                    loading="lazy"
                    class="w-16 h-16 object-cover rounded border border-cyan-400/15 hover:border-fuchsia-400/50 transition"
                  />
                </a>
                <span
                  v-if="m.pics.length > 9"
                  class="w-16 h-16 rounded flex items-center justify-center font-mono text-[10px] text-cyan-400/60 bg-slate-900/70 border border-cyan-400/10"
                  >+{{ m.pics.length - 9 }}</span
                >
              </div>
              <div v-if="m.videos?.length" class="mt-2">
                <video
                  v-for="(v, vi) in m.videos.slice(0, 2)"
                  :key="vi"
                  :src="v.url"
                  controls
                  preload="none"
                  class="max-w-[220px] max-h-36 rounded border border-cyan-400/15 mr-2"
                ></video>
              </div>
              <div v-if="m.comments?.length" class="mt-2">
                <button
                  @click="toggleComment(m)"
                  class="font-mono text-[10px] text-cyan-400/70 hover:text-fuchsia-300 transition"
                >
                  {{ openComments[m.tid] ? '▼ 收起评论' : `▶ 展开 ${m.comments.length} 条评论` }}
                </button>
                <div v-if="openComments[m.tid]" class="mt-2 space-y-1.5 border-l-2 border-cyan-400/20 pl-3">
                  <div v-for="(c, ci) in m.comments" :key="ci" class="text-[11px] leading-relaxed">
                    <span class="text-cyan-200">{{ c.name }}</span>
                    <span class="font-mono text-[9px] text-slate-500 ml-2">{{ c.time }}</span>
                    <span class="text-slate-300/90 ml-1 qz-content" v-html="c.content_html || c.content"></span>
                    <div v-for="(r, ri) in c.replies" :key="ri" class="text-slate-400 pl-3 text-[10px] qz-content" v-html="`↳ ${r.name}：${r.content_html || r.content}`"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div v-if="moments.length < job.fetched" class="text-center mt-4">
            <button @click="loadMore" :disabled="loadingMore" class="qz-btn">
              {{ loadingMore ? 'LOADING…' : `▼ 加载更多（剩 ${job.fetched - moments.length} 条）` }}
            </button>
          </div>
          <div v-else-if="moments.length" class="text-center font-mono text-[10px] text-slate-500 mt-4">
            —— EOF · {{ moments.length }} RECORDS ——
          </div>
          <div v-if="!moments.length" class="text-center font-mono text-[11px] text-slate-500 py-6">
            NO RECORDS —— 没有可展示的说说数据
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useToastStore } from '@/stores/modules/toast'
import { useQzoneStore } from '@/stores/modules/qzone'
import { confirm } from '@/composables/useDialog'
import { startQr, qrStatus, startJob, getJobMoments, deleteJob, downloadUrl } from '@/api/qzone'

const toast = useToastStore()
const store = useQzoneStore()

// ── 登录（扫码会话局部状态；成功后写入全局 store 跨页面保持） ──
const sid = ref('')
const qrImg = ref('')
const qrLoading = ref(false)
const loginStatus = ref('idle')
let qrTimer = null

// ── 抓取参数 ──
const interval = ref(1.5)
const intervalOptions = [
  { v: 1.5, label: '标准 · 1.5 秒/页（推荐）' },
  { v: 2.5, label: '温和 · 2.5 秒/页' },
  { v: 5, label: '极慢 · 5 秒/页（账号敏感时）' },
]
const workers = ref(2)
const workerOptions = [
  { v: 1, label: '单线程 · 最稳妥' },
  { v: 2, label: '双线程 · 速度×2（推荐）' },
  { v: 3, label: '三线程 · 速度×3（风控风险↑）' },
]
const starting = ref(false)

// ── 结果预览 ──
const moments = ref([])
const openComments = reactive({})
const loadingMore = ref(false)

const downloads = [
  { fmt: 'json', label: '完整 JSON', icon: '📦' },
  { fmt: 'html', label: '网页版 HTML', icon: '📜' },
  { fmt: 'csv', label: '表格 CSV', icon: '📊' },
]

// 可获取内容清单（页面上明确标明能抓到什么）
const contentList = [
  { i: '✦', t: '说说文字', d: '正文 / 时间 / 昵称' },
  { i: '🖼', t: '图片', d: '高清原图直链' },
  { i: '🎬', t: '视频', d: '原视频直链' },
  { i: '💬', t: '评论+楼中楼', d: '回复人/内容/时间' },
  { i: '📍', t: '地点定位', d: '说说附带位置' },
  { i: '🔒', t: '私密标记', d: '仅自己可见标识' },
  { i: '📱', t: '来源端', d: 'QQ空间/手机等' },
  { i: '📊', t: '统计汇总', d: '条数/跨度/计数' },
]

const loginUser = computed(() => store.loginUser)
const job = computed(() => store.job)
const jobRunning = computed(() => store.running)
const jobFinished = computed(() => store.finished)

const loginStatusText = computed(
  () =>
    ({
      idle: 'IDLE',
      waiting: '等待扫码',
      scanned: '已扫描',
      success: '已登录',
      expired: '已过期',
      canceled: '已取消',
    })[loginStatus.value] || 'IDLE'
)
const loginStatusClass = computed(() =>
  loginStatus.value === 'success'
    ? 'text-emerald-400'
    : ['expired', 'canceled'].includes(loginStatus.value)
      ? 'text-red-400'
      : loginStatus.value === 'scanned'
        ? 'text-cyan-300'
        : 'text-slate-500'
)

const jobStatusText = computed(
  () =>
    ({
      running: '抓取中',
      done: 'DONE',
      failed: '失败',
      canceled: '已取消',
    })[job.value?.status] || '-'
)
const jobStatusClass = computed(() =>
  job.value?.status === 'done'
    ? 'text-emerald-400'
    : job.value?.status === 'running'
      ? 'text-cyan-300'
      : job.value?.status === 'canceled'
        ? 'text-amber-300'
        : 'text-red-400'
)

const pct = computed(() => {
  const j = job.value
  if (!j?.total) return 0
  return Math.min(100, Math.round((j.fetched / j.total) * 100))
})
const asciiBar = computed(() => {
  const filled = Math.round((pct.value / 100) * 24)
  return '[' + '█'.repeat(filled) + '░'.repeat(24 - filled) + ']'
})
const progressPhase = computed(() => {
  const j = job.value
  if (!j) return ''
  if (j.status === 'running' && !j.total) return '读取说说总数…'
  if (j.status === 'running') return '分页抓取中（含评论/图片链接）'
  if (j.status === 'done') return '备份完成'
  if (j.status === 'canceled') return '已取消（保留部分数据）'
  return '任务失败'
})

// 任务失败但保留了部分数据时给出提示条
const failedHint = computed(() => {
  const j = job.value
  if (!j) return ''
  if (j.status === 'failed' && j.fetched > 0) return '部分页面失败，仅导出已抓到的数据'
  if (j.status === 'done' && (j.logs || []).some((l) => l.includes('仍失败')))
    return (j.logs.filter((l) => l.includes('仍失败')) || []).slice(-1)[0]
  return ''
})

const healthText = computed(() => '9006')
const nowTime = ref('')
let clockTimer = null

onMounted(() => {
  store.restore()
  if (store.finished) loadMoments(0, true)
  nowTime.value = fmtTime()
  clockTimer = setInterval(() => { nowTime.value = fmtTime() }, 1000)
})

function fmtTime() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

// 任务从 running → 终态时自动加载预览
watch(
  () => store.job?.status,
  (s, old) => {
    if (s && s !== 'running' && old === 'running') loadMoments(0, true)
  }
)

async function getQr() {
  stopQrTimer()
  qrLoading.value = true
  try {
    const data = await startQr()
    sid.value = data.sid
    qrImg.value = data.qr_png
    loginStatus.value = 'waiting'
    qrTimer = setInterval(pollQr, 2000)
  } catch {
    /* 拦截器已统一提示 */
  } finally {
    qrLoading.value = false
  }
}

async function pollQr() {
  try {
    const r = await qrStatus(sid.value)
    if (r.status === 'waiting') return
    if (r.status === 'scanned') {
      loginStatus.value = 'scanned'
      return
    }
    stopQrTimer()
    loginStatus.value = r.status
    if (r.status === 'success') {
      store.setLogin(sid.value, { uin: r.uin, name: r.name, avatar: r.avatar })
      toast.success(`登录成功，欢迎回来 ${r.name || r.uin}`)
    } else if (r.status === 'expired') {
      toast.warning('二维码已失效，请重新获取')
    } else if (r.status === 'canceled') {
      toast.warning('已取消授权')
    }
  } catch {
    stopQrTimer()
  }
}

function stopQrTimer() {
  if (qrTimer) {
    clearInterval(qrTimer)
    qrTimer = null
  }
}

function resetLogin() {
  stopQrTimer()
  sid.value = ''
  qrImg.value = ''
  loginStatus.value = 'idle'
  store.clearLogin()
}

async function startFetch() {
  if (!store.loginUser) return
  starting.value = true
  try {
    const r = await startJob(sid.value, interval.value, workers.value)
    moments.value = []
    store.setJobId(r.jid)
    toast.info(`任务已启动（${workers.value} 线程后台运行），切走页面进度不丢`)
  } catch {
    /* 拦截器已提示 */
  } finally {
    starting.value = false
  }
}

async function onCancel() {
  const ok = await confirm('确定取消本次抓取？已经抓到的数据仍会导出供下载。', {
    title: '取消抓取任务',
    okText: '取消任务',
    tone: 'danger',
  })
  if (!ok) return
  await store.cancel()
}

async function onClear() {
  const ok = await confirm('将清除服务器上本次任务的临时数据与页面记录，确定继续？', {
    title: '清除本次记录',
    okText: '清除',
    tone: 'danger',
  })
  if (!ok) return
  const jid = store.jid
  try {
    if (jid) await deleteJob(jid)
  } catch {
    /* 404 也继续清理 */
  }
  store.clearJob()
  moments.value = []
  Object.keys(openComments).forEach((k) => delete openComments[k])
  toast.success('记录已清除')
}

async function loadMoments(offset, replace = false) {
  if (!store.jid) return
  loadingMore.value = true
  try {
    const r = await getJobMoments(store.jid, offset, 50)
    moments.value = replace ? r.items : [...moments.value, ...r.items]
  } catch {
    /* ignore */
  } finally {
    loadingMore.value = false
  }
}

function loadMore() {
  loadMoments(moments.value.length)
}

function toggleComment(m) {
  openComments[m.tid] = !openComments[m.tid]
}

function onAvatarError(e) {
  e.target.style.visibility = 'hidden'
}

onBeforeUnmount(() => {
  stopQrTimer()
  if (clockTimer) clearInterval(clockTimer)
})
</script>

<style scoped>
/* ============================================================
   时光回溯 —— 90 年代 CRT 终端（CRT Terminal · 1995）
   纯黑底 + 高亮青/绿字 + 强对比边框 + CRT 扫描线 + 像素等宽字 + ASCII 装饰
   ============================================================ */

/* 页面整体：纯黑 CRT 底 */
.qz-page {
  min-height: 100vh;
  background:
    radial-gradient(ellipse at 50% 0%, rgba(8, 20, 30, 0.6) 0%, transparent 60%),
    #02060d;
  color: #d1fae5;
  font-family: ui-monospace, 'Cascadia Code', Consolas, 'Courier New', 'PingFang SC', monospace;
}

/* ── Header ── */
.qz-kicker {
  font-size: 11px;
  letter-spacing: 4px;
  color: #34d399;
  text-shadow: 0 0 8px rgba(52, 211, 153, 0.5);
  margin-bottom: 6px;
}
.qz-title {
  font-size: clamp(26px, 4vw, 40px);
  font-weight: 900;
  letter-spacing: 1px;
  margin: 0;
  line-height: 1.15;
}
.qz-title-glow {
  background: linear-gradient(90deg, #5eead4 0%, #22d3ee 45%, #a78bfa 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  text-shadow: 0 0 24px rgba(34, 211, 238, 0.35);
  filter: drop-shadow(0 0 1px rgba(94, 234, 212, 0.4));
}
.qz-subtitle {
  font-size: 13px;
  color: #86efac;
  margin-top: 8px;
  letter-spacing: 0.5px;
}
.qz-hud {
  font-size: 11px;
  color: #5eead4;
  line-height: 1.7;
  letter-spacing: 1px;
  text-shadow: 0 0 6px rgba(94, 234, 212, 0.3);
}
.qz-hud-time {
  color: #fcd34d;
  font-weight: 700;
}
.qz-blink {
  animation: qz-blink 1.1s steps(1) infinite;
}
@keyframes qz-blink {
  50% { opacity: 0; }
}

/* ASCII 分隔线 */
.qz-ascii {
  font-size: 12px;
  color: #22d3ee;
  margin: 4px 0 18px;
  letter-spacing: 0;
  line-height: 1;
  text-shadow: 0 0 6px rgba(34, 211, 238, 0.4);
  white-space: nowrap;
  overflow: hidden;
}

/* ── 卡片：纯黑不透明底 + 高亮边框 ── */
.qz-card {
  position: relative;
  background: linear-gradient(160deg, rgba(3, 8, 18, 0.96) 0%, rgba(2, 5, 12, 0.98) 100%);
  border: 1px solid rgba(34, 211, 238, 0.4);
  border-radius: 6px;
  box-shadow:
    0 0 0 1px rgba(34, 211, 238, 0.08),
    0 10px 40px rgba(0, 0, 0, 0.7),
    inset 0 0 30px rgba(34, 211, 238, 0.03);
  padding: 18px;
  margin-bottom: 18px;
  transition: border-color 0.3s, box-shadow 0.3s;
}
.qz-card::after {
  /* CRT 扫描线 */
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 6px;
  pointer-events: none;
  background: repeating-linear-gradient(
    0deg,
    rgba(34, 211, 238, 0.035) 0 1px,
    transparent 1px 3px
  );
}
.qz-card:hover {
  border-color: rgba(94, 234, 212, 0.6);
  box-shadow:
    0 0 0 1px rgba(94, 234, 212, 0.12),
    0 10px 40px rgba(0, 0, 0, 0.7),
    0 0 28px rgba(34, 211, 238, 0.12);
}
.qz-active {
  border-color: rgba(52, 211, 153, 0.55);
  box-shadow:
    0 0 0 1px rgba(52, 211, 153, 0.15),
    0 10px 40px rgba(0, 0, 0, 0.7),
    0 0 30px rgba(52, 211, 153, 0.1),
    inset 0 0 30px rgba(52, 211, 153, 0.04);
}

/* 终端标题栏 */
.term-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  margin: -18px -18px 14px;
  border-bottom: 1px solid rgba(34, 211, 238, 0.3);
  background: linear-gradient(90deg, rgba(34, 211, 238, 0.12), rgba(167, 139, 250, 0.06));
  border-radius: 6px 6px 0 0;
}
.term-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #475569;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.5);
}
.term-dot:first-child { background: #f43f5e; }
.term-dot:nth-child(2) { background: #f59e0b; }
.term-dot:nth-child(3) { background: #22c55e; }
.term-title {
  margin-left: 10px;
  font-size: 12px;
  font-weight: 700;
  color: #5eead4;
  letter-spacing: 1.5px;
  text-shadow: 0 0 8px rgba(94, 234, 212, 0.4);
}
.term-status {
  margin-left: auto;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 1px;
}

/* 内容清单 chip */
.content-chip {
  padding: 10px 12px;
  border-radius: 4px;
  background: rgba(34, 211, 238, 0.06);
  border: 1px solid rgba(34, 211, 238, 0.2);
  color: #e2e8f0;
  font-size: 12px;
  transition: all 0.2s;
}
.content-chip:hover {
  border-color: rgba(167, 139, 250, 0.5);
  background: rgba(167, 139, 250, 0.08);
  color: #fff;
}

/* 二维码 */
.qr-box {
  position: relative;
  background: #f0f9ff;
  padding: 10px;
  border-radius: 4px;
  border: 2px solid rgba(34, 211, 238, 0.5);
  box-shadow:
    0 0 30px rgba(34, 211, 238, 0.25),
    inset 0 0 16px rgba(34, 211, 238, 0.1);
  flex-shrink: 0;
}
.qr-dim { filter: grayscale(1) opacity(0.3); }
.qr-mask {
  position: absolute;
  inset: 0;
  border-radius: 4px;
  background: rgba(2, 6, 13, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
}
.scanline {
  position: absolute;
  left: 8px;
  right: 8px;
  height: 2px;
  border-radius: 2px;
  background: linear-gradient(90deg, transparent, rgba(52, 211, 153, 0.9), transparent);
  animation: scan-y 2.2s ease-in-out infinite;
}
@keyframes scan-y {
  0%, 100% { top: 8%; }
  50% { top: 90%; }
}

/* 按钮 / 下拉 */
.qz-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 18px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 700;
  color: #5eead4;
  background: rgba(34, 211, 238, 0.08);
  border: 1px solid rgba(34, 211, 238, 0.4);
  transition: all 0.2s;
  cursor: pointer;
}
.qz-btn:hover:not(:disabled) {
  border-color: rgba(94, 234, 212, 0.7);
  color: #ccfbf1;
  background: rgba(34, 211, 238, 0.15);
  box-shadow: 0 0 18px rgba(34, 211, 238, 0.25);
}
.qz-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.qz-btn-primary {
  padding: 12px 26px;
  font-size: 14px;
  color: #022c22;
  background: linear-gradient(135deg, #34d399, #22d3ee);
  border-color: #5eead4;
  text-shadow: none;
  box-shadow: 0 0 20px rgba(52, 211, 153, 0.35);
}
.qz-btn-primary:hover:not(:disabled) {
  color: #022c22;
  background: linear-gradient(135deg, #6ee7b7, #67e8f9);
  box-shadow: 0 0 28px rgba(52, 211, 153, 0.5);
}
.qz-select {
  width: 100%;
  background: #020617;
  border: 1px solid rgba(34, 211, 238, 0.35);
  border-radius: 4px;
  padding: 10px 12px;
  font-size: 13px;
  color: #5eead4;
}
.qz-select:focus {
  outline: none;
  border-color: #5eead4;
  box-shadow: 0 0 0 2px rgba(94, 234, 212, 0.15);
}
.qz-select option { background: #020617; color: #5eead4; }

/* 日志窗 */
.log-box {
  background: #020617;
  border: 1px solid rgba(34, 211, 238, 0.2);
  border-radius: 4px;
  padding: 12px 14px;
  max-height: 160px;
  overflow-y: auto;
}

/* 统计 chip */
.stat-chip {
  text-align: center;
  padding: 16px 10px;
  border-radius: 4px;
  background: rgba(34, 211, 238, 0.06);
  border: 1px solid rgba(34, 211, 238, 0.25);
}
.stat-num {
  font-size: 26px;
  font-weight: 900;
  background: linear-gradient(135deg, #5eead4, #a78bfa);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  text-shadow: 0 0 16px rgba(94, 234, 212, 0.3);
}
.stat-label {
  font-size: 10px;
  color: #94a3b8;
  letter-spacing: 1.5px;
  margin-top: 4px;
}

/* 预览条目 */
.moment-item {
  padding: 12px 14px;
  border-radius: 4px;
  background: rgba(2, 6, 13, 0.85);
  border: 1px solid rgba(34, 211, 238, 0.18);
  transition: border-color 0.2s;
}
.moment-item:hover {
  border-color: rgba(167, 139, 250, 0.4);
}

/* QQ 表情图：内联渲染，与文字基线对齐 */
.qz-content :deep(img.qz-emo) {
  display: inline-block;
  width: 20px;
  height: 20px;
  vertical-align: middle;
  margin: 0 1px;
  border-radius: 2px;
}

/* 打字机光标 */
.blink-cursor {
  animation: blink 1s steps(1) infinite;
  color: #5eead4;
  margin-left: 2px;
}
@keyframes blink {
  50% { opacity: 0; }
}

.spinner {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: 2.5px solid rgba(52, 211, 153, 0.25);
  border-top-color: #34d399;
  animation: qz-spin 0.8s linear infinite;
}
.spinner-sm {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 2px solid rgba(52, 211, 153, 0.3);
  border-top-color: #34d399;
  animation: qz-spin 0.8s linear infinite;
}
@keyframes qz-spin {
  to { transform: rotate(360deg); }
}

.progress-indeterminate {
  animation: qz-indet 1.4s ease-in-out infinite;
}
@keyframes qz-indet {
  0% { width: 8%; margin-left: 0; }
  50% { width: 40%; margin-left: 30%; }
  100% { width: 8%; margin-left: 92%; }
}

/* ── 功能介绍 README 区块 ── */
.qz-intro {
  border-color: rgba(52, 211, 153, 0.4);
}
.intro-hero {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 16px;
}
.intro-prompt {
  color: #34d399;
  font-weight: 700;
  text-shadow: 0 0 6px rgba(52, 211, 153, 0.5);
}
.intro-cmd {
  color: #5eead4;
  font-weight: 800;
  letter-spacing: 0.5px;
  text-shadow: 0 0 8px rgba(94, 234, 212, 0.4);
}
.intro-desc {
  color: #cbd5e1;
  font-size: 15px;
}
.intro-lead {
  color: #a7f3d0;
  font-size: 13px;
  line-height: 1.85;
  margin-top: 10px;
}
.tag {
  display: inline-block;
  padding: 1px 8px;
  font-size: 11px;
  font-weight: 700;
  color: #022c22;
  background: linear-gradient(135deg, #34d399, #22d3ee);
  border-radius: 3px;
  margin: 0 2px;
}
.intro-h {
  font-size: 13px;
  font-weight: 700;
  color: #5eead4;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
  text-shadow: 0 0 6px rgba(94, 234, 212, 0.3);
}
.intro-list {
  list-style: none;
  padding: 0;
  margin: 0;
  font-size: 12.5px;
  line-height: 2;
  color: #cbd5e1;
}
.intro-list b {
  color: #e2e8f0;
  font-weight: 700;
}
.intro-list li {
  padding-left: 16px;
  position: relative;
}
.intro-list li::before {
  content: '▸';
  position: absolute;
  left: 0;
  color: #22d3ee;
}
.intro-ol {
  counter-reset: step;
}
.intro-ol li::before {
  counter-increment: step;
  content: counter(step) '.';
  color: #fcd34d;
  font-weight: 700;
}
.intro-safe li::before {
  content: '✓';
  color: #34d399;
}
.intro-limit {
  margin-top: 16px;
  padding: 12px 14px;
  background: rgba(251, 191, 36, 0.06);
  border: 1px solid rgba(251, 191, 36, 0.25);
  border-radius: 4px;
  font-size: 12.5px;
  line-height: 1.8;
  color: #fde68a;
}
.intro-limit-tag {
  display: inline-block;
  font-size: 11px;
  font-weight: 700;
  color: #022c22;
  background: #fcd34d;
  padding: 1px 8px;
  border-radius: 3px;
  margin-right: 8px;
  letter-spacing: 0.5px;
}
</style>
