<template>
  <div class="xingtu-container" v-show="!orbHidden" :style="containerStyle" ref="containerRef">
    <!-- Panel（absolute 挂在球上方，不参与布局、不干扰拖动） -->
    <div class="xingtu-panel" :class="{ open: isOpen }">
      <div class="panel-header">
        <div class="panel-logo">
          <div class="logo-orb"></div>
          <div class="logo-ring"></div>
        </div>
        <div class="panel-title">
          <h3>星途 <small>XINGTU</small></h3>
          <span class="version">v2.0 · 智能语音系统</span>
        </div>
        <button class="panel-close" @click="isOpen = false">✕</button>
      </div>

      <div class="panel-body">
        <!-- Status -->
        <div class="status-bar">
          <div class="status-item">
            <span class="status-dot" :class="statusClass"></span>
            <span class="status-label">{{ statusText }}</span>
          </div>
          <button v-if="micFixAction === 'open-tab'" class="mic-fix-btn" @click="openMicInNewTab">
            在新标签页打开
          </button>
          <button v-if="micFixAction === 'os-settings'" class="mic-fix-btn" @click="openOsMicSettings">
            打开麦克风设置
          </button>
          <button
            v-if="micFixAction === 'os-settings' || micFixAction === 'settings'"
            class="mic-fix-btn"
            @click="recheckMic"
          >
            重新检测
          </button>
          <div class="status-item">
            <span class="wake-hint">唤醒："星图星图" / "星途星途"</span>
          </div>
        </div>

        <!-- Messages -->
        <div class="messages" ref="msgBox">
          <div v-if="messages.length === 0" class="empty-hint">
            <div class="hint-icon">◈</div>
            <p>点击星途球或说"星途星途"/"星途"开始对话</p>
            <div class="quick-actions">
              <button v-for="c in quickCmds" :key="c.label" @click="execCmd(c.cmd)" class="qbtn">
                {{ c.icon }} {{ c.label }}
              </button>
            </div>
          </div>
          <div v-for="(m, i) in messages" :key="i" class="msg" :class="m.role">
            <div class="msg-avatar">{{ m.role === 'user' ? '👤' : '◈' }}</div>
            <div class="msg-content">
              <div class="msg-role">
                {{ m.role === 'user' ? '你' : '星途' }}
                <button v-if="m.role !== 'user'" class="msg-save-btn" title="存入知识库" @click="openSaveToKB(m)">🧠</button>
              </div>
              <div class="msg-text">{{ m.text }}</div>
            </div>
          </div>
          <div v-if="isListening || liveText" class="msg user">
            <div class="msg-avatar">👤</div>
            <div class="msg-content">
              <div class="msg-role">你</div>
              <div v-if="liveText" class="msg-text live-text">{{ liveText }}</div>
              <div v-else class="msg-text listening-dots"><span></span><span></span><span></span></div>
            </div>
          </div>
        </div>

        <!-- Controls -->
        <div class="panel-controls">
          <button
            class="ctrl-btn"
            :class="{ 'wrec-active': wBusy }"
            @pointerdown="wStart"
            @pointerup="wStop"
            @pointercancel="wStop"
            @pointerleave="wStop"
            title="按住说话，松开后 whisper 高精度识别"
          >
            <span class="ctrl-icon">{{ wBusy ? '☁' : '🎙' }}</span>
            <span class="ctrl-label">{{ wBusy ? '松开识别' : '按住说' }}</span>
          </button>
          <button
            class="ctrl-btn"
            :class="{ active: isListening || (localStt && wBusy) }"
            @click="onMicToggle"
          >
            <span class="ctrl-icon">{{ isListening ? '⏹' : (localStt && wBusy ? '☁' : '🎤') }}</span>
            <span class="ctrl-label">{{ isListening ? '停止' : (localStt ? (wBusy ? '识别中' : '本地识别') : '语音') }}</span>
          </button>
          <button class="ctrl-btn" @click="testSpeak">
            <span class="ctrl-icon">🔊</span>
            <span class="ctrl-label">试听</span>
          </button>
          <button class="ctrl-btn" @click="clearMsgs">
            <span class="ctrl-icon">🗑</span>
            <span class="ctrl-label">清空</span>
          </button>
        </div>

        <!-- Voice Selector -->
        <div class="voice-selector">
          <label>音色：</label>
          <select v-model="voiceURI" @change="onVoiceChange">
            <optgroup v-for="g in voiceGroups" :key="g.label" :label="g.label">
              <option v-for="v in g.items" :key="v.uri" :value="v.uri" :title="v.desc">
                {{ v.name }}{{ v.desc ? '（' + v.desc + '）' : '' }}
              </option>
            </optgroup>
          </select>
        </div>
      </div>

      <!-- 存入知识库弹窗 -->
      <Modal v-model="showKbModal" title="🧠 存入知识库">
        <div class="space-y-3">
          <p v-if="!kbOwner" class="text-xs text-amber-300/80">
            ⚠️ 尚未登录知识库站主。保存需要在「知识库」页面用 Supabase 账号登录后才能写入。
          </p>
          <input v-model="kbForm.title" class="web3-input text-sm" placeholder="知识标题 *" />
          <textarea v-model="kbForm.content" rows="8" class="web3-input text-sm w-full font-mono resize-y"
                    placeholder="Markdown 内容"></textarea>
          <select v-model="kbForm.category" class="web3-input text-sm">
            <option v-for="c in KB_TAXONOMY" :key="c.key" :value="c.key">{{ c.icon }} {{ c.key }}</option>
          </select>
          <input v-model="kbForm.tags" class="web3-input text-sm" placeholder="标签，逗号分隔" />
          <p v-if="kbError" class="text-xs text-red-400">{{ kbError }}</p>
          <div class="flex justify-end gap-2">
            <button class="web3-btn-outline text-xs !px-4 !py-2" @click="showKbModal = false">取消</button>
            <button class="web3-btn text-xs !px-4 !py-2" :disabled="kbBusy" @click="saveToKB">
              {{ kbBusy ? '保存中…' : '💾 保存到知识库' }}
            </button>
          </div>
        </div>
      </Modal>
  </div>

  <!-- Floating Orb Button (可拖动) -->
  <button
    ref="orbBtn"
    class="xingtu-orb"
    :class="{ 'orb-speaking': isSpeaking, 'orb-listening': isListening, 'orb-dragging': dragging, 'orb-guide-active': guideVisible }"
    @pointerdown="onOrbPointerDown"
    @pointermove="onOrbPointerMove"
    @pointercancel="finishDrag"
    @pointerup="finishDrag"
    @click="onOrbClick"
  >
    <div class="orb-ring"></div>
    <div class="orb-core"></div>
    <div v-if="isSpeaking" class="orb-waves">
      <span></span><span></span><span></span><span></span>
    </div>
  </button>

  <!-- 页面引导小框（导航栏页面跳转时顶部弹出，与右下角星途球联动） -->
  <transition name="guide-pop">
    <div v-if="guideVisible" class="page-guide" :class="{ 'guide-typing': guideTyping }" @click="dismissGuide">
      <div class="guide-orb">◈</div>
      <div class="guide-body">
        <div class="guide-title">{{ guideTitle }}</div>
        <div class="guide-text">{{ guideTyped }}<span v-if="guideTyping" class="guide-caret"></span></div>
      </div>
      <button class="guide-close" @click.stop="dismissGuide">✕</button>
      <!-- 联动连线：从引导框指向右下角星途球 -->
      <div class="guide-link-line"></div>
    </div>
  </transition>
</div>
</template>

<script setup>
// 星途智能语音助手（XingTuAssistant）
// 悬浮球：拖拽、唤醒词"星途星途"、本地/云端语音识别（vosk-browser 优先，Web Speech 后备）
// 语音输出仅用 Edge 神经网络音色（晓晓/云希等 10 个）；已移除本地旧音色兜底
// 指令由 jarvis-service 后端识别；后端不可用时本地降级
// ============================================================
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { recognizeVoice, createVoiceSession } from '@/api/jarvis'
import { searchNetease, getNeteaseSongUrl } from '@/api/music'
import { usePlayerStore } from '@/stores/modules/player'
import {
  startVoskRecognition, stopVoskRecognition, isVoskRunning, loadVoskModel, callbacks as voskCb,
} from '@/composables/useVoskStt'
import { speakEdgeStream, stopEdgeSpeech, EDGE_VOICES, isEdgeTtsSupported } from '@/api/edgeTts'
import { startWhisperRecord, whisperTranscribe } from '@/composables/useWhisperStt'
import { agentChatStream, consumeAgentStream } from '@/api/agent'
import {
  startVoiceStream, sendVoice, disposeVoiceStream, isVoiceStreamRunning,
} from '@/composables/useVoiceStream'
import { guideTo, clearVoiceHighlight } from '@/composables/useVoiceGuide'
import { createEntry, getOwnerUser } from '@/api/knowledge'
import { alert as dlgAlert } from '@/composables/useDialog'
import { KB_TAXONOMY } from '@/data/knowledgeTaxonomy'
import Modal from '@/components/common/Modal.vue'

const router = useRouter()
const route = useRoute()
const player = usePlayerStore()
const isOpen = ref(false)
const isListening = ref(false)
const isSpeaking = ref(false)
const messages = ref([])
const statusText = ref('就绪')
// 麦克风诊断后的可执行动作：'' | 'open-tab' | 'settings'（驱动状态栏内的修复按钮）
const micFixAction = ref('')
const msgBox = ref(null)
let recognition = null
let wakeWordDetected = false
let manualStop = false
let stopByError = false    // 因麦克风权限/硬件错误被动停止（允许用户手势一键重试恢复）
let disposed = false
let wakeTimer = 0
let wakeWordTimer = 0
let sessionId = null
let backendOk = false
let edgeOk = false
let voskOk = false
let micHintShown = false   // 麦克风授权类提示只播报一次，避免反复骚扰
const voices = EDGE_VOICES
// 音色下拉按语种/性别分组，10 个音色才不至于糊成一列
const voiceGroups = computed(() => {
  const defs = [
    { label: '普通话 · 女声', match: v => v.locale === '普通话' && v.gender === '女' },
    { label: '普通话 · 男声', match: v => v.locale === '普通话' && v.gender === '男' },
    { label: '方言', match: v => v.locale.includes('方言') },
    { label: '粤语 / 台湾腔', match: v => ['粤语', '台湾国语'].includes(v.locale) },
  ]
  return defs
    .map(d => ({ label: d.label, items: voices.filter(d.match) }))
    .filter(g => g.items.length)
})
const voiceURI = ref('zh-CN-XiaoxiaoNeural')
let selectedVoiceURI = 'zh-CN-XiaoxiaoNeural'

// ===== 全双工语音流状态（阶段5）=====
// PCM 全双工 WS：负责服务端 VAD 打断 / LLM 流式按句播报 / 页面引导。
// 浏览器 Web Speech 仍负责 ASR（服务端 faster-whisper 为可选升级）。
let wsReady = false            // /api/voice/ws 已建立
let wsBackend = ''             // 服务端 LLM 后端标识（cockpit / agent）
let serverAsr = false          // 服务端流式 ASR 可用 → 停用浏览器 Web Speech，避免双识别竞态
const liveText = ref('')       // 服务端实时字幕（边说边出）
let pendingTranscript = ''     // Web Speech 累积的最终文本（等待端点检测）
let endpointTimer = 0          // 静音端点计时器
let streamIdx = -1             // 流式助手消息在 messages 中的下标（-1 表示无）
let ttsQueue = []              // 待播报句子（服务端 sentence 事件）
let ttsDraining = false        // 句子播报串行化标志
const VOICE_ENDPOINT_MS = 700  // 静音多久视为「说完了」

// 全屏固定页（群聊）内隐藏悬浮球：避免遮挡页面自身的发送等操作按钮
const orbHidden = computed(() => route.path === '/community/chat')

// 等待服务端 ready 帧（判断是否有流式 ASR）的短重试计数
let asrWaitRetry = 0

// ===== 页面引导小框（导航栏页面跳转时顶部弹出）=====
const GUIDE_PAGES = {
  '/':         { title: '主城广场', text: '这里是极光世界的中心枢纽，汇聚最新动态、精选内容与快捷入口，随时开启你的冒险。' },
  '/blog':     { title: '冒险日志', text: '记录技术探索与生活随笔，每一篇都是一段冒险的足迹，欢迎翻阅。' },
  '/community':{ title: '冒险者酒馆', text: '与来自各地的冒险者交流心得、分享发现、结交同伴，畅所欲言。' },
  '/shop':     { title: '魔法杂货铺', text: '精选数字商品与创意周边，用魔法币兑换你心仪的宝物。' },
  '/media':    { title: '幻镜水晶', text: '书影音的收藏夹，记录触动心灵的文字、旋律与画面。' },
}
const guideVisible = ref(false)
const guideTitle = ref('')
const guideText = ref('')
const guideTyped = ref('')
const guideTyping = ref(false)
let guideTimer = null
let guideTypeTimer = null

function startGuideTypewriter() {
  guideTyped.value = ''
  guideTyping.value = true
  let i = 0
  const full = guideText.value
  if (guideTypeTimer) clearInterval(guideTypeTimer)
  guideTypeTimer = setInterval(() => {
    if (i < full.length) {
      guideTyped.value = full.slice(0, i + 1)
      i++
    } else {
      clearInterval(guideTypeTimer)
      guideTypeTimer = null
      guideTyping.value = false
    }
  }, 38)
}

function showGuide(path) {
  const info = GUIDE_PAGES[path]
  if (!info) return
  guideTitle.value = info.title
  guideText.value = info.text
  guideVisible.value = true
  // 下一帧启动打字机（等过渡动画）
  nextTick(() => startGuideTypewriter())
  // 8 秒后自动消失
  if (guideTimer) clearTimeout(guideTimer)
  guideTimer = setTimeout(() => dismissGuide(), 9000)
}

function dismissGuide() {
  guideVisible.value = false
  guideTyping.value = false
  if (guideTypeTimer) { clearInterval(guideTypeTimer); guideTypeTimer = null }
  if (guideTimer) { clearTimeout(guideTimer); guideTimer = null }
}

// 监听路由变化：仅主导航栏页面触发引导
watch(() => route.path, (newPath) => {
  // 只在 5 个主导航页面触发
  if (GUIDE_PAGES[newPath]) {
    showGuide(newPath)
  } else {
    dismissGuide()
  }
})

// ---- whisper 高精度语音（按住说话：录音 → 上传本地 faster-whisper → 转写）----
// 服务端流式 ASR 可用时改走 WS push-to-talk（无需上传、无需额外模型）；
// 不可用时退回旧的「录音→上传 /whisper/transcribe」链路。
const wBusy = ref(false)
let wHandle = null
let wPtt = false
async function wStart() {
  if (wBusy.value) return
  if (isListening.value) { toggleListening() }
  if (serverAsr && isVoiceStreamRunning()) {
    wBusy.value = true
    wPtt = true
    statusText.value = '请说话...'
    liveText.value = ''
    sendVoice({ type: 'ptt', action: 'start' })
    return
  }
  try {
    wBusy.value = true
    wHandle = null
    statusText.value = '识别中(whisper)...'
    wHandle = await startWhisperRecord()
  } catch {
    statusText.value = '麦克风不可用'
    wBusy.value = false
  }
}
async function wStop() {
  if (!wBusy.value) return
  // WS push-to-talk：松手即收尾，最终文本由 asr 事件统一处理
  if (wPtt) {
    wPtt = false
    wBusy.value = false
    statusText.value = '识别中...'
    sendVoice({ type: 'ptt', action: 'end' })
    return
  }
  const h = wHandle
  wHandle = null
  let blob = null
  if (h) {
    try { blob = await h.stop() } catch (e) { blob = null }
  }
  if (blob && blob.size > 0) {
    try {
      const text = await whisperTranscribe(blob)
      statusText.value = '识别完成'
      if (text) {
        addMsg('user', text)
        processTranscript(text)
      } else {
        statusText.value = '没听清'
        speak('没有听清，请再说一次')
      }
    } catch {
      statusText.value = '识别服务不可用'
      speak('语音识别服务暂不可用，请稍后再试')
    }
  } else {
    statusText.value = '未录音'
  }
  wBusy.value = false
}

// ---- 本地语音兜底 ----
// Web Speech（Chrome/Edge 依赖 Google 语音服务，国内常被阻断）不可用时，自动把
// 主「语音」按钮切换为本地 whisper 模式：点击开始录音，再点结束并转写（无需联网）。
const localStt = ref(false)
function enableLocalFallback(msg) {
  localStt.value = true
  micFixAction.value = ''
  statusText.value = msg || '已切换本地识别 · 点击麦克风开始'
}
function onMicToggle() {
  if (localStt.value) {
    if (wBusy.value) void wStop()
    else void wStart()
    return
  }
  toggleListening()
}

// 状态点的样式分类：监听 / 说话 / 处理中 / 就绪
const statusClass = computed(() => {
  if (isListening.value) return 'listening'
  if (isSpeaking.value) return 'speaking'
  if (statusText.value === '处理中') return 'processing'
  return 'ready'
})

// ---- 悬浮球拖动（定位在容器上，orb 只负责 hover/点击，互不干扰） ----
const containerRef = ref(null)
const orbPos = ref(null) // { x, y }：null 表示未拖动过，走默认右下角
const dragging = ref(false)
const orbBtn = ref(null)
let dragState = null
let suppressClickOnce = false
const ORB_SIZE = 56

const containerStyle = computed(() => {
  if (!orbPos.value) return { bottom: '24px', right: '24px' }
  return {
    left: orbPos.value.x + 'px',
    top: orbPos.value.y + 'px',
    bottom: 'auto',
    right: 'auto',
    transition: dragging.value ? 'none' : 'left 0.3s cubic-bezier(0.22,1,0.36,1), top 0.3s cubic-bezier(0.22,1,0.36,1)',
  }
})

function clampPos(x, y) {
  const vw = window.innerWidth
  const vh = window.innerHeight
  return {
    x: Math.min(Math.max(x, 8), vw - ORB_SIZE - 8),
    y: Math.min(Math.max(y, 8), vh - ORB_SIZE - 8),
  }
}

function onOrbPointerDown(e) {
  if (e.button !== undefined && e.button !== 0) return
  e.preventDefault()
  const rect = containerRef.value.getBoundingClientRect()
  dragState = {
    sx: e.clientX,
    sy: e.clientY,
    ox: orbPos.value ? orbPos.value.x : rect.left,
    oy: orbPos.value ? orbPos.value.y : rect.top,
    moved: false,
  }
  dragging.value = true
  try { orbBtn.value.setPointerCapture(e.pointerId) } catch (err) { /* ignore */ }
}

function onOrbPointerMove(e) {
  if (!dragState) return
  const dx = e.clientX - dragState.sx
  const dy = e.clientY - dragState.sy
  if (!dragState.moved && Math.hypot(dx, dy) > 8) dragState.moved = true
  if (dragState.moved) {
    orbPos.value = clampPos(dragState.ox + dx, dragState.oy + dy)
  }
}

function finishDrag() {
  if (!dragState) return
  const wasDragged = dragState.moved
  dragState = null
  dragging.value = false
  if (wasDragged) {
    suppressClickOnce = true
    setTimeout(() => { suppressClickOnce = false }, 120)
    if (orbPos.value) {
      const vw = window.innerWidth
      const snapX = orbPos.value.x + ORB_SIZE / 2 > vw / 2 ? vw - ORB_SIZE - 8 : 8
      orbPos.value = clampPos(snapX, orbPos.value.y)
    }
  }
}

function onOrbPointerUp(e) { finishDrag() }

const quickCmds = [
  { icon: '🏠', label: '回首页', cmd: '打开首页' },
  { icon: '📝', label: '社区', cmd: '打开社区' },
  { icon: '🛒', label: '商城', cmd: '打开商城' },
  { icon: '📊', label: '量化', cmd: '打开量化' },
  { icon: '🎵', label: '音乐', cmd: '打开音乐' },
  { icon: '👤', label: '我的', cmd: '打开个人中心' },
]

// ---- localStorage: first visit ----
const hasVisited = () => localStorage.getItem('xingtu_visited') === 'true'
const markVisited = () => localStorage.setItem('xingtu_visited', 'true')

// ---- Panel toggle ----
function togglePanel() {
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    scrollMsgs()
    // 用户手势窗口内建立全双工语音流（AudioContext/麦克风需要手势）
    void ensureVoiceStream()
    // 打开面板时自动开始监听
    if (!isListening.value && !manualStop) {
      startListening()
    }
  }
}

function onOrbClick() {
  if (suppressClickOnce) return
  togglePanel()
}

// ---- Messages ----
function addMsg(role, text) {
  messages.value.push({ role, text })
  scrollMsgs()
}
function scrollMsgs() {
  nextTick(() => { if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight })
}
function clearMsgs() { messages.value = [] }

// ---- 把 AI 回复一键存入知识库 ----
const showKbModal = ref(false)
const kbBusy = ref(false)
const kbError = ref('')
const kbOwner = ref(false)
const kbForm = ref({ title: '', content: '', category: '软件工程', tags: '' })

async function openSaveToKB(m) {
  kbError.value = ''
  const { data } = await getOwnerUser()
  kbOwner.value = !!data?.user
  const text = String(m.text || '').trim()
  // 自动取第一句作为标题
  const firstLine = text.split(/[。\n]/).map(s => s.trim()).find(Boolean) || ''
  kbForm.value = {
    title: firstLine.slice(0, 40) || '来自星途助手的知识',
    content: text,
    category: '软件工程',
    tags: '',
  }
  showKbModal.value = true
}

async function saveToKB() {
  if (!kbForm.value.title.trim()) { kbError.value = '标题必填'; return }
  kbBusy.value = true
  kbError.value = ''
  try {
    await createEntry({
      title: kbForm.value.title,
      content: kbForm.value.content,
      category: kbForm.value.category,
      tags: kbForm.value.tags,
      summary: kbForm.value.content.slice(0, 100),
      stage: 'seedling',
      published: true,
    })
    showKbModal.value = false
    // 优先 toast，失败时兜底为网站主题弹窗（不使用浏览器原生 alert）
    try {
      const { useToastStore } = await import('@/stores/modules/toast')
      useToastStore().success('已存入知识库 🌱')
    } catch {
      dlgAlert('已存入知识库 🌱', { title: '保存成功' })
    }
  } catch (e) {
    if (String(e.message || '').includes('permission') || String(e.message || '').includes('policy')) {
      kbError.value = '无写入权限：请到 /knowledge 页面用 Supabase 账号登录站主'
    } else {
      kbError.value = e.message || '保存失败'
    }
  } finally {
    kbBusy.value = false
  }
}

// ---- TTS: 说话（仅 Edge 神经网络音色）----
// 2026-10-16：应用户要求移除 Windows speechSynthesis 旧音色兜底。
// 此前 Edge 在线音色失败时会回退本地机械音，出现「两个声音/音色僵硬」的体感；
// 现在只保留 Edge 神经网络音色，失败时仅状态栏可见提示，不再发声。

// ===== 播报：服务端 Edge 流式（逐段合成即播）=====
// 关键改动（阶段5）：播报期间不再停麦克风（保持全双工），
// 由服务端 VAD 的 interrupt 事件负责 barge-in 打断。
//
// 2026-10-15：合成已下沉到 py-service（官方 edge-tts 客户端）。此处若返回 false
// 说明后端不可用。2026-10-16 起不再回退本地旧音色，但失败必须让用户看得见，
// 而不是静默——之前的静默降级正是「音色僵硬」长期未被发现的根源。
let fallbackNotified = false
const EDGE_FAIL_TEXT = '语音不可用（请检查 9006）'
function notifyEdgeFallback() {
  if (fallbackNotified) return
  fallbackNotified = true
  console.warn('[星途助手] 在线音色不可用，本轮不发声。请检查 py-service(9006) 是否运行。')
  statusText.value = EDGE_FAIL_TEXT
}

// 播报序号：新一轮播报会打断旧一轮（clearTts 触发 abort），旧轮返回 false 属正常流程，
// 不应触发「语音不可用」告警；仅最新一轮的真实失败才提示。任意一次成功即清除告警——
// 2026-10-16 修复：此前一次瞬时失败（或被新播报打断）会把状态栏永久锁在
// 「语音不可用（请检查 9006）」，即使后续播报正常也不恢复，误导用户以为语音坏了。
let speakSeq = 0

async function speak(text) {
  const t = String(text || '').trim()
  if (!t || disposed) return
  const seq = ++speakSeq
  clearTts()
  isSpeaking.value = true
  let ok = false
  if (edgeOk) {
    try { ok = await speakEdgeStream(t, selectedVoiceURI) } catch { ok = false }
  }
  if (disposed) return
  if (ok) {
    fallbackNotified = false
    if (statusText.value === EDGE_FAIL_TEXT) statusText.value = '就绪'
  } else if (seq === speakSeq) {
    notifyEdgeFallback()
  }
  if (seq === speakSeq) isSpeaking.value = false
}

// 服务端流式播报：sentence 事件按序入队，串行播报（保证语序）
function enqueueSentence(text) {
  const t = String(text || '').trim()
  if (!t || disposed) return
  ttsQueue.push(t)
  void drainTts()
}

async function drainTts() {
  if (ttsDraining || disposed) return
  ttsDraining = true
  try {
    while (ttsQueue.length && !disposed) {
      const s = ttsQueue.shift()
      isSpeaking.value = true
      let ok = false
      if (edgeOk) { try { ok = await speakEdgeStream(s, selectedVoiceURI) } catch { ok = false } }
      if (!ok && !disposed) notifyEdgeFallback()
    }
  } finally {
    ttsDraining = false
    if (!disposed) isSpeaking.value = false
  }
}

// 立即静音在途播报（打断/取消）
function clearTts() {
  ttsQueue = []
  ttsDraining = false
  try { stopEdgeSpeech() } catch { /* ignore */ }
  isSpeaking.value = false
}

// ===== 流式助手消息（WS 逐字回传）=====
function beginStreamMsg() {
  if (streamIdx >= 0) return
  streamIdx = messages.value.length
  messages.value.push({ role: 'assistant', text: '' })
  scrollMsgs()
}
function appendStreamMsg(delta) {
  if (!delta) return
  beginStreamMsg()
  messages.value[streamIdx].text += delta
  scrollMsgs()
}
function endStreamMsg(placeholder = '') {
  if (streamIdx < 0) return
  const m = messages.value[streamIdx]
  if (!m.text) {
    if (placeholder) m.text = placeholder
    else messages.value.splice(streamIdx, 1)
  }
  streamIdx = -1
}

// 统一打断入口：静音 + 丢弃未完成的流式消息
function interruptAll() {
  clearTts()
  endStreamMsg()
  if (statusText.value === '思考中...') statusText.value = '监听中...'
}

// ===== 服务端引导：跳转 + 元素高亮 + 提示气泡 =====
async function applyGuide(g) {
  if (!g || !g.route) return
  try {
    await guideTo(router, { route: g.route, tip: g.tip, selector: g.selector })
  } catch {
    navigate(g.route)
  }
}

// ===== 全双工 WS 事件分发 =====
function handleVoiceEvent(msg) {
  if (disposed || !msg) return
  switch (msg.type) {
    case 'ready':
      wsReady = true
      wsBackend = msg.backend || ''
      serverAsr = !!msg.asr
      // 服务端 ASR 可用：停掉浏览器 Web Speech，由服务端流式识别统一出字
      if (serverAsr) {
        localStt.value = false
        if (recognition) {
          try { recognition.onend = null; recognition.stop() } catch { /* ignore */ }
        }
      }
      break
    case 'interrupt':
      // 用户开口 → 立即打断 TTS 与在途生成
      interruptAll()
      liveText.value = ''
      isListening.value = true
      break
    case 'asr_partial':
      // 实时字幕：边说边出
      if (msg.text) { liveText.value = msg.text; statusText.value = '识别中...' }
      break
    case 'asr': {
      // 本轮最终文本：走统一的唤醒词/对话判定，再由 respond 决策执行
      const t = String(msg.text || '').trim()
      liveText.value = ''
      if (t) processTranscript(t)
      break
    }
    case 'text':
      appendStreamMsg(msg.text || '')
      break
    case 'sentence':
      enqueueSentence(msg.text)
      break
    case 'guide':
      // 纯导航意图（服务端未走 LLM）时没有 text 增量，补一条助手消息
      if (streamIdx < 0 && msg.tip) addMsg('assistant', msg.tip)
      void applyGuide(msg)
      break
    case 'endpoint':
      // 服务端端点：本轮说话结束。ASR 有文本时会先发 asr 事件；这里兜底清理字幕
      liveText.value = ''
      break
    case 'done':
      endStreamMsg('（没有获取到内容）')
      statusText.value = isListening.value ? '监听中...' : '就绪'
      break
    case 'error':
      addMsg('assistant', '抱歉，' + friendlyServiceError(msg.message, '语音服务暂时不可用'))
      streamIdx = -1
      statusText.value = '就绪'
      break
    default:
      break
  }
}

// 启动全双工语音流（麦克风 PCM → py-service VAD/LLM 编排）
async function ensureVoiceStream() {
  if (disposed || isVoiceStreamRunning()) return
  try {
    const ok = await startVoiceStream({
      onEvent: handleVoiceEvent,
      onStatus: (s) => { wsReady = (s === 'ready') },
    })
    if (!ok) wsReady = false
  } catch {
    wsReady = false
  }
}

function testSpeak() {
  speak('你好，我是星途智能助手，很高兴为你服务。')
}

// ---- 本地降级：命令路由（未登录/后端不可用时） ----
// 覆盖全站所有路由页面，喊"打开XX"即可跳转
const LOCAL_CMDS = [
  { match: ['首页', '主页'], action: '/', reply: '正在回到首页' },
  { match: ['社区', '论坛', '文章'], action: '/community', reply: '正在打开技术社区' },
  { match: ['博客', '日志'], action: '/blog', reply: '正在打开博客' },
  { match: ['商城', '购物', '商店'], action: '/shop', reply: '正在打开在线商城' },
  { match: ['购物车'], action: '/shop/cart', reply: '正在打开购物车' },
  { match: ['媒体', '书影音', '视频'], action: '/media', reply: '正在打开多媒体中心' },
  { match: ['音乐'], action: '/media', reply: '正在打开音乐馆' },
  { match: ['量化', '交易', '股票', '行情', '看板'], action: '/quant', reply: '正在打开金融量化平台' },
  { match: ['工具', '脚本'], action: '/tools', reply: '正在打开在线工具' },
  { match: ['API中心', 'api'], action: '/tools/api', reply: '正在打开 API 中心' },
  { match: ['分享网站', '外链', '好站'], action: '/tools/sites', reply: '正在打开分享网站' },
  { match: ['软件', '下载'], action: '/software', reply: '正在打开软件库' },
  { match: ['资源'], action: '/resources', reply: '正在打开资源中心' },
  { match: ['上传'], action: '/upload', reply: '正在打开资源上传' },
  { match: ['美甲'], action: '/nails', reply: '正在打开美甲小铺' },
  { match: ['3D', '粒子', '星空漫游'], action: '/three', reply: '正在打开 3D 星空漫游' },
  { match: ['架构'], action: '/architecture', reply: '正在打开微服务架构图鉴' },
  { match: ['AI 中转站', '中转站', 'ai-station', '令牌'], action: '/ai-station', reply: '正在打开 AI 中转站' },
  { match: ['AI引擎', 'AI助手', '知识库'], action: '/ai', reply: '正在打开 AI 引擎' },
  { match: ['Agent', '智能体', '工作流'], action: '/agent', reply: '正在打开 Agent 工作台' },
  { match: ['相册'], action: '/album', reply: '正在打开相册集' },
  { match: ['友链', '友人帐'], action: '/link', reply: '正在打开友人帐' },
  { match: ['留言'], action: '/comments', reply: '正在打开留言板' },
  { match: ['关于'], action: '/about', reply: '正在打开关于页面' },
  { match: ['订单'], action: '/profile/orders', reply: '正在打开我的订单' },
  { match: ['个人中心', '我的'], action: '/profile', reply: '正在打开个人中心' },
  { match: ['支付', '充值'], action: '/pay', reply: '正在打开支付页面' },
  { match: ['登录'], action: '/login', reply: '正在跳转到登录页面' },
]

// 导航意图判定：短句（≤8 字）或含显式动作词才算导航指令，
// 避免「用一句话介绍你自己」这类长句被关键词误判
const NAV_VERBS = ['打开', '去', '进入', '跳转', '前往', '回到', '返回', '看看', '看一下', '瞧瞧', '我想', '我要', '帮我', '带我去']
const NAV_MAX_LEN = 8

function goLocal(text) {
  const t = String(text || '')
  const looksNav = t.length <= NAV_MAX_LEN || NAV_VERBS.some(v => t.includes(v))
  const found = looksNav ? LOCAL_CMDS.find(c => c.match.some(k => t.includes(k))) : null
  if (found) {
    return { reply: found.reply, path: found.action }
  }
  if (text.includes('你好') || text.includes('嗨') || text.includes('hello') || text.includes('hi')) {
    return { reply: '你好！我是星途智能助手，很高兴为你服务。有什么可以帮助你的吗？' }
  }
  if (text.includes('时间') || text.includes('几点') || text.includes('日期')) {
    return { reply: `现在是 ${new Date().toLocaleString('zh-CN')}` }
  }
  if (text.includes('天气')) return { reply: '抱歉，天气功能正在开发中。' }
  if (text.includes('帮助') || text.includes('help') || text.includes('功能') || text.includes('你能做什么')) {
    return { reply: '我可以帮你导航到各个模块，查询系统信息，或者陪你聊天。试试说"打开社区"、"现在几点了"。' }
  }
  if (text.includes('谢谢') || text.includes('感谢')) return { reply: '不客气！还有其他需要帮助的吗？' }
  return { reply: '抱歉，我没有理解这个指令。你可以试试说"打开社区"、"打开商城"、"现在几点"等。' }
}

function navigate(path) {
  if (path) router.push(path)
}

// ---- Wake word detection ----
// 说出"星图星图 / 星途星途"即唤醒并自动弹出面板；唤醒后 5 秒内可直接下达指令
// 支持多种近音识别（Vosk 离线模型对自定义词识别不准，用模糊匹配兜底）
const WAKE_EXACT = ['星图星图', '星途星途', '星图 星图', '星途 星图', 'xingtu xingtu', 'hey xingtu']
const WAKE_FUZZY = ['星图', '星途', '行途', '行图', '新途', '兴途', '刑图', '幸途', 'xingtu']
function matchWake(text) {
  const lower = text.toLowerCase().replace(/\s+/g, '')
  // 精确匹配（重复喊两遍必唤醒）
  for (const w of WAKE_EXACT) { if (lower.includes(w.replace(/\s+/g, ''))) return { match: true, exact: true } }
  // 模糊匹配：仅当文本很短（<=8字）时才判定为唤醒词，避免"我想去星途旅行"误触发
  if (lower.length <= 8) {
    for (const w of WAKE_FUZZY) { if (lower.includes(w)) return { match: true, exact: false } }
  }
  return { match: false }
}
// 端点检测触发：把静音前累积的最终识别文本作为一个完整轮次处理
function flushTranscript() {
  clearTimeout(endpointTimer)
  endpointTimer = 0
  const text = pendingTranscript.trim()
  pendingTranscript = ''
  if (!text || disposed) return
  processTranscript(text)
}

function processTranscript(raw) {
  const text = (raw || '').trim()
  if (!text) return
  const wake = matchWake(text)

  if (wake.match) {
    wakeWordDetected = true
    clearTimeout(wakeWordTimer)
    wakeWordTimer = setTimeout(() => { wakeWordDetected = false }, 5000)
    // 去掉唤醒词部分，提取指令
    let cmd = text
      .replace(/星图星图|星途星途|星图\s*星图|星途\s*星图/g, '')
      .replace(/xingtu\s*xingtu/gi, '')
      .replace(/星图|星途|行途|行图|新途|兴途|刑图|幸途/g, '')
      .replace(/xingtu/gi, '')
      .trim()
    if (!isOpen.value) isOpen.value = true   // 喊唤醒词自动弹出面板
    if (!cmd) {
      addMsg('user', '唤醒')
      const welcome = '我在！可以帮你打开网站各个模块，也可以聊天。'
      addMsg('assistant', welcome)
      speak(welcome)
    } else {
      respond(cmd)
    }
    return
  }

  if (wakeWordDetected) {
    respond(text)
    clearTimeout(wakeWordTimer)
    wakeWordTimer = setTimeout(() => { wakeWordDetected = false }, 5000)
    return
  }

  // 面板已打开 = 用户正在与助手对话，直接响应，无需逐句喊唤醒词
  if (isOpen.value) respond(text)
}

// ---- 音乐指令（语音搜歌 / 播放控制 / 浮窗开关）----
// 例：「我想听周杰伦」「播放晴天」「下一首」「暂停」「关闭播放器」
function reply(text) {
  addMsg('assistant', text)
  speak(text)
}

function playReply(msg, delay = 0) {
  setTimeout(() => { reply(msg); statusText.value = '就绪' }, delay)
}

// 语音搜歌并播放（关键字 → 网易云搜索 → 取第一条播放）
async function searchAndPlay(keyword) {
  statusText.value = '处理中...'
  try {
    const res = await searchNetease(keyword, 20)
    const songs = (res?.result?.songs || []).slice(0, 10).map(s => ({
      id: s.id,
      title: s.name,
      artist: (s.artists || []).map(a => a.name).join(' / ') || '未知歌手',
      cover: s.album?.picUrl || '',
      duration: Math.round((s.duration || 0) / 1000),
      url: '',
    }))
    if (!songs.length) { playReply(`没有找到「${keyword}」相关的歌曲，换个关键词试试`); return }
    const r = await getNeteaseSongUrl(songs[0].id)
    songs[0].url = r?.data?.[0]?.url || ''
    if (!songs[0].url) { playReply(`「${songs[0].title}」暂时无法播放，换一首试试`); return }
    void Promise.all(songs.slice(1).map(async s => {
      try { const rr = await getNeteaseSongUrl(s.id); s.url = rr?.data?.[0]?.url || '' } catch { /* 静默 */ }
    }))
    player.setPlaylist(songs.filter(s => s.url))
    player.play({ ...songs[0] })
    player.showBar()
    playReply(`好的，为你播放《${songs[0].title}》${songs[0].artist ? ' - ' + songs[0].artist : ''}`)
  } catch {
    playReply('音乐搜索暂时不可用，请稍后再试')
  }
}

// 返回 true 表示已处理
function handleMusic(text) {
  const t = text.trim()

  // 播放控制
  if (/^(暂停音乐|暂停|别放了|停一下|静音)$/.test(t)) {
    player.pause()
    playReply('好的，已暂停播放')
    return true
  }
  if (/^(继续播放|接着放|接着播|继续唱|播放吧)$/.test(t)) {
    if (player.hasTrack) {
      player.resume()
      playReply('好的，继续播放')
    } else {
      playReply('当前没有正在播放的音乐，你可以说"我想听周杰伦"')
    }
    return true
  }
  if (/^(下一首|切歌|换一首|下首歌)$/.test(t)) {
    if (player.hasTrack) { player.next(); playReply('已切到下一首') }
    else playReply('当前没有播放列表，先播放一首歌吧')
    return true
  }
  if (/^(上一首|上一首歌|返回上首)$/.test(t)) {
    if (player.hasTrack) { player.prev(); playReply('已回到上一首') }
    else playReply('当前没有播放列表，先播放一首歌吧')
    return true
  }

  // 浮窗开关
  if (/^(关闭音乐浮窗|关闭播放器|收起播放器|隐藏播放器|关掉播放器)$/.test(t)) {
    player.hideBar()
    playReply('好的，已收起播放器，音乐会继续播放')
    return true
  }
  if (/^(显示播放器|打开播放器|展开播放器|音乐浮窗|显示音乐浮窗)$/.test(t)) {
    player.showBar()
    playReply('好的，播放器已显示')
    return true
  }

  // 歌名搜索（「我想听XXX」「放一首XXX」「来一首XXX」「播放XXX」「点一首XXX」）
  const m = t.match(/^(?:我想听|我想听一首|放一首|来一首|播放一首|点一首|听一首|我要听)\s*(.+)$/)
  if (m) {
    const kw = m[1].trim()
    if (kw && kw !== '音乐') { void searchAndPlay(kw); return true }
  }

  // 随机/无目标播放 → 打开音乐馆
  if (/^(播放音乐|放歌|来点音乐|放首歌|随便放一首|来一首)$/.test(t)) {
    playReply('为你打开音乐馆，选一首喜欢的歌吧')
    setTimeout(() => navigate('/music'), 600)
    return true
  }
  if (/^打开音乐$/.test(t)) {
    playReply('正在打开音乐馆')
    setTimeout(() => navigate('/music'), 300)
    return true
  }
  return false
}

function respond(text) {
  statusText.value = '处理中...'
  addMsg('user', text)

  // 音乐指令优先（本地处理，无需后端）
  if (handleMusic(text)) return

  // 导航指令：本地直接处理，不依赖后端识别（同时给出页面高亮引导）
  const nav = goLocal(text)
  if (nav.path) {
    setTimeout(() => {
      addMsg('assistant', nav.reply)
      speak(nav.reply)
      statusText.value = '就绪'
      // 跳转 + 高亮引导（applyGuide 内部完成路由与元素定位）
      void applyGuide({ route: nav.path, tip: nav.reply })
    }, 300)
    return
  }

  // 主路径：全双工 WS 编排（服务端 LLM 流式按句播报 + 页面引导）
  if (wsReady || isVoiceStreamRunning()) {
    endStreamMsg()
    clearTts()
    streamIdx = -1
    statusText.value = '思考中...'
    sendVoice({ type: 'text', text })
    if (isVoiceStreamRunning()) return
    // WS 未就绪时继续走回退链路，避免卡住
  }

  // 回退：已登录且 jarvis 服务可用 → 走 /jarvis/recognize
  if (backendOk && sessionId) {
    recognizeVoice(text, sessionId)
      .then(res => {
        const data = res?.data || res
        const reply = typeof data === 'string' ? data : data?.response
        if (reply) {
          addMsg('assistant', reply)
          speak(reply)
        }
        const target = data?.targetUrl
        if (data?.action === 'NAVIGATE' && target) navigate(target)
        statusText.value = '就绪'
      })
      .catch(() => {
        agentRespond(text)
      })
    return
  }

  agentRespond(text)
}

// AI 上游报错（尤其是限流 429）常返回一大段英文或原始 JSON，直接展示对用户
// 不友好。这里统一识别限流类关键词，替换成可执行的中文提示。
function isRateLimited(text) {
  return /429|rate ?limit|too many requests|请求过于频繁|限流/i.test(String(text || ''))
}
function friendlyServiceError(raw, fallback) {
  const s = String(raw || '').trim()
  if (isRateLimited(s)) return 'AI 上游限流了（免费模型约 10 次/分钟），请稍等约 1 分钟再试一次'
  return s || fallback || '服务暂时不可用'
}

// 接入 Agent Workflow：把未命中本地指令的内容交给 Agent 编排层（流式），
// 完成后朗读回复。这样悬浮助手与 /agent 会话台共用同一套路由 Agent。
let agentBusy = false
async function agentRespond(text) {
  if (agentBusy) return
  agentBusy = true
  statusText.value = 'Agent 思考中...'
  const streamMsg = { role: 'assistant', text: '' }
  messages.value.push(streamMsg)
  try {
    const res = await agentChatStream([{ role: 'user', content: text }])
    await consumeAgentStream(res, (delta) => {
      streamMsg.text += delta
      scrollMsgs()
    }, () => {})
    streamMsg.text = streamMsg.text || '（Agent 未返回内容）'
    statusText.value = '就绪'
    if (streamMsg.text) speak(streamMsg.text)
  } catch (e) {
    streamMsg.text = '抱歉，' + friendlyServiceError(e?.message, 'Agent 暂时不可用')
    speak(streamMsg.text)
    statusText.value = '就绪'
  } finally {
    agentBusy = false
  }
}

function fallbackHandle(text) {
  const r = goLocal(text)
  setTimeout(() => {
    addMsg('assistant', r.reply)
    speak(r.reply)
    statusText.value = '就绪'
    if (r.path) navigate(r.path)
  }, 300)
}

function execCmd(cmd) {
  isOpen.value = true
  respond(cmd)
}

// ---- Voice Recognition ----
// 引擎优先级：Web Speech API（Chrome/Edge 云端，最可靠）> vosk-browser（本地 WASM 离线识别）
function toggleListening() {
  if (isListening.value) {
    stopListening()
  } else {
    startListening()
  }
}

function startListening() {
  if (isListening.value) return
  // 全双工语音流已启动 → 识别交由服务端流式 ASR 完成，不再启动浏览器 Web Speech
  // （ready 帧未到达时短暂重试，避免过早启动 Web Speech 造成双识别竞态）
  if (isVoiceStreamRunning()) {
    if (serverAsr) {
      manualStop = false
      stopByError = false
      clearMicFixHint()
      isListening.value = true
      statusText.value = '监听中...'
      return
    }
    if (!wsReady && asrWaitRetry < 12) {
      asrWaitRetry++
      setTimeout(() => { if (!disposed && !isListening.value && !manualStop) startListening() }, 350)
      return
    }
  }
  asrWaitRetry = 0
  // 已处于本地兜底模式：不再重试 Web Speech（避免重复触发不可用错误）
  if (localStt.value) {
    statusText.value = '本地识别已就绪 · 点击麦克风开始'
    return
  }
  manualStop = false
  stopByError = false
  clearMicFixHint()
  // 用 event.resultIndex 自动去重，无需 lastFinalIndex
  // 优先使用 Web Speech（Chrome/Edge 内置，最可靠）
  if (initRecognition()) {
    startWebListening()
    return
  }
  // Web Speech 不可用时尝试 vosk
  if (voskOk) {
    startVoskRecognition().then(ok => {
      if (disposed) return
      if (ok) {
        isListening.value = true
        statusText.value = '监听中...'
      } else {
        voskOk = false
        statusText.value = '语音引擎不可用'
        speak('语音识别引擎加载失败，请检查浏览器是否支持语音识别')
      }
    })
    return
  }
  // Web Speech 与 Vosk 都不可用：自动回退本地 whisper 识别
  enableLocalFallback('浏览器不支持在线识别 · 已切换本地识别')
  speak('浏览器内置语音识别不可用，已切换为本地识别，点击麦克风即可开始')
}

// 麦克风诊断：把笼统的 audio-capture 拆成可定位的原因
// （非安全环境 / iframe 权限策略拦截 / 浏览器屏蔽 / 系统隐私拦截 / 无设备 / 被独占）
// 返回值：{ message, action } —— action='open-tab' 表示需在新标签页打开，'settings' 表示需改浏览器/系统设置
function isEmbeddedInIframe() {
  try { return window.self !== window.top } catch { return true }
}
function micAllowedByPolicy() {
  const pp = document.permissionsPolicy || document.featurePolicy
  try { return pp && pp.allowsFeature ? pp.allowsFeature('microphone') : true } catch { return true }
}
async function queryMicPermission() {
  try {
    if (navigator.permissions?.query) {
      const st = await navigator.permissions.query({ name: 'microphone' })
      return st.state
    }
  } catch { /* Safari/Firefox 不支持 name:'microphone' */ }
  return 'unknown'
}
async function diagnoseMic() {
  if (window.isSecureContext === false) {
    return { message: '麦克风需 HTTPS 或 localhost 环境 · 当前页面非安全源', action: 'open-tab' }
  }
  const embedded = isEmbeddedInIframe()
  const iframeMsg = '页面被嵌入在受限 iframe 中 · 请在新标签页打开本站再授权麦克风'
  // iframe 未通过 allow="microphone" 授权时，站点级"已允许"也无效，getUserMedia 会直接 NotAllowedError
  if (embedded && !micAllowedByPolicy()) {
    return { message: iframeMsg, action: 'open-tab' }
  }
  const permState = await queryMicPermission()
  try {
    const devs = await navigator.mediaDevices.enumerateDevices()
    if (!devs.some(d => d.kind === 'audioinput')) {
      return { message: '未检测到麦克风设备 · 请检查系统录音设备', action: 'settings' }
    }
  } catch { /* ignore */ }
  try {
    const s = await navigator.mediaDevices.getUserMedia({ audio: true })
    s.getTracks().forEach(t => t.stop())
    // 能取到麦克风说明授权与硬件都正常，问题在语音引擎本身
    return { message: '麦克风可用 · 语音引擎未启动，点球体重试', action: '' }
  } catch (err) {
    const n = err && err.name
    if (n === 'NotAllowedError' || n === 'SecurityError') {
      if (embedded) return { message: iframeMsg, action: 'open-tab' }
      if (permState === 'denied') return { message: '浏览器已屏蔽本站麦克风 · 点地址栏左侧图标改为“允许”', action: 'settings' }
      // 站点级已允许却仍被拒 → 拦截发生在 Windows 隐私层，逐项给出三处开关
      if (permState === 'granted') {
        return { message: '站点已允许，但被 Windows 拦截 · 开启：①麦克风访问 ②让应用访问你的麦克风 ③允许桌面应用访问麦克风（应用列表里 Edge/Chrome 也要开）', action: 'os-settings' }
      }
      // 部分浏览器不支持权限查询（unknown），无法区分浏览器层/系统层，同样给系统设置入口
      if (permState === 'unknown') {
        return { message: '麦克风被拦截 · 确认浏览器允许本站麦克风，并检查 Windows 麦克风隐私设置', action: 'os-settings' }
      }
      return { message: '麦克风未授权 · 点地址栏左侧图标改为“允许”', action: 'settings' }
    }
    if (n === 'NotReadableError' || n === 'TrackStartError') return { message: '麦克风被其他程序独占 · 关闭占用麦克风的程序后重试', action: '' }
    if (n === 'NotFoundError' || n === 'OverconstrainedError') return { message: '未检测到麦克风设备 · 请检查系统录音设备', action: 'settings' }
    return { message: '麦克风硬件不可用 · 点球体重试', action: '' }
  }
}
// 受限 iframe 内的救场：在新标签页打开当前页（真实顶级窗口才能拿到麦克风授权）
function openMicInNewTab() {
  try {
    window.open(window.location.href, '_blank', 'noopener')
  } catch { /* 被弹窗拦截时提示手动复制地址 */ }
}
// 一键打开 Windows 麦克风隐私设置（ms-settings 协议；Windows 版 Edge 支持直接跳转）
function openOsMicSettings() {
  try { window.location.href = 'ms-settings:privacy-microphone' } catch { /* 非 Windows 或不支持时忽略 */ }
  // 无论是否跳转成功都补上手动路径，避免用户找不到入口
  statusText.value = '需开启：①麦克风访问 ②让应用访问你的麦克风 ③允许桌面应用访问麦克风，并在应用列表里打开 Edge/Chrome'
}
// 改完设置后就地重测，无需刷新页面
function recheckMic() {
  if (disposed) return
  micFixAction.value = ''
  statusText.value = '重新检测中...'
  void diagnoseMic().then(showMicDiag)
}
// 统一呈现诊断结果：精确文案 + 打开面板 + 语音提示 + 修复按钮
function showMicDiag(diag) {
  if (!diag || disposed) return
  statusText.value = diag.message
  micFixAction.value = diag.action || ''
  isOpen.value = true
  if (!micHintShown) { micHintShown = true; speak(diag.message) }
}
// 打开网页即静默申请过的会话里，用户再次点击球体重试时清掉上一次的修复提示
function clearMicFixHint() {
  micFixAction.value = ''
}

function startWebListening() {
  if (!recognition) {
    statusText.value = '浏览器不支持语音识别'
    speak('当前浏览器不支持语音识别，请使用 Chrome 或 Edge 浏览器')
    return
  }
  if (isListening.value) return
  if (typeof navigator !== 'undefined' && navigator.permissions?.query) {
    navigator.permissions.query({ name: 'microphone' }).then((p) => {
      if (p.state === 'denied') {
        statusText.value = '麦克风权限已拒绝'
        speak('麦克风权限被拒绝，请在浏览器地址栏左侧点击权限设置，允许使用麦克风')
      }
    }).catch(() => {})
  }
  try {
    recognition.start()
    isListening.value = true
    statusText.value = '监听中...'
  } catch (e) {
    try { recognition.stop() } catch {}
    setTimeout(() => {
      try {
        recognition.start()
        isListening.value = true
        statusText.value = '监听中...'
      } catch (_) {
        // 启动失败（多为麦克风未授权 / 无可用音频输入）：给出可操作提示而非静默回到"就绪"
        statusText.value = '语音未启动 · 点击球体重试'
      }
    }, 400)
  }
}

function stopListening() {
  if (isListening.value || isVoskRunning()) {
    manualStop = true
    stopByError = false
    stopVoskRecognition()
    if (recognition) { try { recognition.stop() } catch {} }
    isListening.value = false
    statusText.value = '就绪'
  }
}

// ===== 重启退避策略 =====
// no-speech/aborted：100ms 快重连（人耳不可察的空窗期）
// network/audio-capture：500→1000→2000→4000 指数退避，并提示用户
// 连续 no-speech 后会进入静默态，状态点不再闪烁，避免"一下获取一下获取不上"的视觉感受
let restartBackoff = 100
let backoffResetTimer = 0
function scheduleRestart(delay) {
  if (manualStop || disposed) return
  clearTimeout(restartTimer)
  restartTimer = setTimeout(silentRestart, delay)
}
function resetBackoff() {
  restartBackoff = 100
  if (backoffResetTimer) { clearTimeout(backoffResetTimer); backoffResetTimer = 0 }
}
function bumpBackoff() {
  // network/audio-capture 类错误用指数退避，最大 4s
  restartBackoff = Math.min(restartBackoff * 2, 4000)
  if (backoffResetTimer) clearTimeout(backoffResetTimer)
  // 30s 无错误后回到 100ms 基线
  backoffResetTimer = setTimeout(() => { restartBackoff = 100 }, 30000)
}
let restartTimer = 0
// 标记本次 onend 是因为 onerror 已主动安排重启，避免重复 setTimeout
let ignoreOnend = false
// 安卓 Chrome 经常返回 confidence=0 的伪 final 结果，必须过滤，否则会插入垃圾文本
const isAndroid = /android/i.test(navigator.userAgent || '')

function initRecognition() {
  if (recognition) return true
  const SR = window.SpeechRecognition || window.webkitSpeechRecognition
  if (!SR) { return false }
  recognition = new SR()
  // 安卓上 continuous=true 反而更易掉线，桌面端用 continuous 保持长会话
  recognition.continuous = !isAndroid
  recognition.interimResults = true
  recognition.lang = 'zh-CN'

  recognition.onresult = (e) => {
    // 用 event.resultIndex 自动去重，无需手动维护 lastFinalIndex
    let interim = ''
    let gotFinal = false
    for (let i = e.resultIndex; i < e.results.length; i++) {
      const r = e.results[i]
      if (r.isFinal) {
        const alt = r[0]
        // 安卓 confidence=0 的伪 final 直接丢弃
        if (alt && (isAndroid ? alt.confidence > 0 : true)) {
          const t = (alt.transcript || '').trim()
          if (t) {
            gotFinal = true
            // 播报期间丢弃识别结果，避免扬声器回环导致助手自问自答；
            // barge-in 打断由服务端 VAD（回声抑制过的 PCM 链路）负责
            if (!isSpeaking.value) {
              pendingTranscript = pendingTranscript ? `${pendingTranscript} ${t}` : t
            }
          }
        }
      } else {
        interim += r[0].transcript
      }
    }
    statusText.value = interim ? '识别中...' : '监听中...'
    // 端点检测：最后一次最终识别后静音 VOICE_ENDPOINT_MS → 视为本句说完
    if (gotFinal && pendingTranscript) {
      clearTimeout(endpointTimer)
      endpointTimer = setTimeout(flushTranscript, VOICE_ENDPOINT_MS)
    }
  }
  recognition.onerror = (e) => {
    if (disposed) return
    if (e.error === 'not-allowed') {
      // 站点权限被拒时给出精确原因（iframe 策略 / 浏览器屏蔽 / 系统隐私）
      isListening.value = false
      manualStop = true       // 停止自动重连（避免反复弹出授权）
      stopByError = true      // 但允许用户手势一键重试
      void diagnoseMic().then(showMicDiag)
      return
    }
    if (e.error === 'service-not-allowed') {
      // 注意：这并非麦克风未授权。Chrome/Edge 的 Web Speech 依赖 Google 语音服务，
      // 国内网络常被阻断而返回此错误，与设备授权无关，故自动回退本地 whisper 识别。
      isListening.value = false
      manualStop = true
      stopByError = true
      enableLocalFallback('内置识别服务不可用 · 已自动切换本地识别')
      if (!micHintShown) {
        micHintShown = true
        speak('浏览器内置语音识别服务不可用，已自动切换为本地识别，点击麦克风即可开始')
      }
      return
    }
    if (e.error === 'no-speech') {
      // 静默超时：标记本次 onend 由 error 触发，100ms 快重连
      ignoreOnend = true
      statusText.value = '监听中...'
      scheduleRestart(100)
      return
    }
    if (e.error === 'aborted') {
      // 主动 abort，不要重启
      ignoreOnend = true
      return
    }
    if (e.error === 'audio-capture') {
      isListening.value = false
      manualStop = true
      stopByError = true
      // 拆解为可定位的原因（安全环境 / iframe 策略 / 浏览器屏蔽 / 系统隐私 / 无设备 / 被独占）
      void diagnoseMic().then(showMicDiag)
      return
    }
    if (e.error === 'network') {
      statusText.value = '网络中断，重连中...'
      isListening.value = false
      ignoreOnend = true
      bumpBackoff()
      scheduleRestart(restartBackoff)
      return
    }
    // 其他未知错误：温和退避
    isListening.value = false
    ignoreOnend = true
    bumpBackoff()
    scheduleRestart(restartBackoff)
  }
  recognition.onend = () => {
    isListening.value = false
    // ignoreOnend 表示 onerror 已安排重启，这里不再重复 setTimeout
    if (ignoreOnend) {
      ignoreOnend = false
      return
    }
    if (!manualStop && !disposed) {
      // 正常 onend（无 error）：100ms 快重连，并重置退避
      resetBackoff()
      scheduleRestart(100)
    } else if (!stopByError) {
      // 因权限/硬件错误被动停止时保留错误提示，不要立刻覆盖成"就绪"（否则用户看不到失败原因）
      statusText.value = '就绪'
    }
  }
  return true
}

// 连续监听：面板关闭也持续监听；仅被手动停止/页面隐藏/组件卸载时暂停
function silentRestart() {
  if (manualStop || isListening.value || disposed) return
  // 服务端 ASR 模式：识别与端点均在服务端完成，前端不重启 Web Speech
  if (serverAsr) return
  if (typeof document !== 'undefined' && document.visibilityState === 'hidden') return
  // 优先 Web Speech
  if (initRecognition()) {
    try {
      recognition.start()
      isListening.value = true
      statusText.value = '监听中...'
      resetBackoff()
    } catch (e) {
      // "already started" 或瞬时占用的处理
      try { recognition.stop() } catch {}
      setTimeout(() => {
        try {
          if (!isListening.value && !manualStop && !disposed) {
            recognition.start()
            isListening.value = true
            statusText.value = '监听中...'
          }
        } catch {}
      }, 200)
    }
    return
  }
  // 回退 vosk
  if (voskOk) {
    startVoskRecognition().then(ok => {
      if (disposed || manualStop) return
      if (ok) {
        isListening.value = true
        statusText.value = '监听中...'
      } else {
        voskOk = false
      }
    })
  }
}

// 打开网页即静默申请麦克风权限，稍后自动开启监听
function requestMicPermission() {
  try {
    if (navigator.mediaDevices?.getUserMedia) {
      navigator.mediaDevices.getUserMedia({ audio: true })
        .then(stream => { stream.getTracks().forEach(t => t.stop()) })
        .catch(() => {})
    }
  } catch {}
}

// ---- 后台自动监听唤醒词 ----
// 页面加载后自动开启连续监听（喊"星图星图"随时唤醒）；
// 浏览器要求首次麦克风授权需用户手势，故兜底在首次点击/按键时再启动
let autoListenTried = false
function tryAutoListen() {
  if (autoListenTried || disposed || isListening.value || wBusy.value) return
  autoListenTried = true
  requestMicPermission()
  // 同步建立全双工语音流（VAD 打断 / 服务端 LLM 编排）
  void ensureVoiceStream()
  setTimeout(() => {
    if (!disposed && !isListening.value && !manualStop) startListening()
  }, 600)
}
function detachGestureArming() {
  window.removeEventListener('pointerdown', onFirstGesture)
  window.removeEventListener('keydown', onFirstGesture)
}
// 浏览器要求用户手势才允许启用麦克风：页面加载时的静默启动常被拒绝（NotAllowed），
// 之前一旦失败就把 manualStop 置为 true 导致整场会话都不再监听（喊唤醒词永远无反应）。
// 这里让「任意一次点击/按键」都能重新拉起唤醒词监听，成功进入监听态后才摘掉兜底监听。
let lastArmAt = 0
function onFirstGesture() {
  if (disposed) return
  if (isListening.value) { detachGestureArming(); return }
  // 用户主动按过"停止"且非错误中断 → 尊重用户意愿，不再自动拉起
  if (manualStop && !stopByError) return
  const now = Date.now()
  if (now - lastArmAt < 1200) return   // 节流，避免连点反复申请麦克风
  lastArmAt = now
  manualStop = false
  stopByError = false
  autoListenTried = false
  tryAutoListen()
  setTimeout(() => { if (isListening.value || disposed) detachGestureArming() }, 1000)
}

// ---- Voice: Edge 音色选择 ----
function onVoiceChange() {
  selectedVoiceURI = voiceURI.value || 'zh-CN-XiaoxiaoNeural'
}

// ---- Lifecycle ----
onMounted(() => {
  edgeOk = isEdgeTtsSupported()
  if (EDGE_VOICES.length && !voiceURI.value) {
    voiceURI.value = EDGE_VOICES[0].uri
    selectedVoiceURI = EDGE_VOICES[0].uri
  }

  // 识别结果回接（vosk 引擎）
  voskCb.onFinal = (t) => { if (!disposed && t) processTranscript(t) }
  voskCb.onPartial = () => { if (!disposed && statusText.value !== '就绪') statusText.value = '识别中...' }

  // 尝试建立后端会话（登录用户）
  createVoiceSession()
    .then(res => {
      sessionId = res?.data?.sessionId || null
      backendOk = !!sessionId
    })
    .catch(() => { backendOk = false })

  // 检测 Web Speech 是否可用
  const SR = window.SpeechRecognition || window.webkitSpeechRecognition
  if (!SR) {
    // Web Speech API 不可用，静默降级到 Vosk
  }

  // Vosk 模型预加载已禁用，直接进入就绪态
  statusText.value = '就绪'
  // Vosk 模型预加载已禁用：WASM FS error 导致模型总是加载失败（30s 超时）
  // 直接使用 Web Speech API 作为语音识别后端，避免控制台刷屏和无意义等待
  // 如需启用 Vosk，取消下行注释并确保浏览器支持 WASM FS sync
  // setTimeout(() => { loadVoskModel().then(() => { voskOk = true }).catch(() => { voskOk = false }) }, 1000)
  voskOk = false

  window.addEventListener('pointerup', onOrbPointerUp)

  // 自动监听唤醒词：先尝试静默启动；失败则在首次点击/按键时启动
  tryAutoListen()
  window.addEventListener('pointerdown', onFirstGesture)
  window.addEventListener('keydown', onFirstGesture)

  // First visit welcome
  if (!hasVisited()) {
    setTimeout(() => {
      addMsg('assistant', '你好！我是星途智能助手，欢迎来到我的网站！点击我说话，或者直接说"星图星图"唤醒我。')
      isOpen.value = true
      markVisited()
    }, 1500)
  }

  // 初始页面引导（首次加载时若在主导航页面则弹出）
  setTimeout(() => {
    if (GUIDE_PAGES[route.path]) showGuide(route.path)
  }, 800)
})

onBeforeUnmount(() => {
  disposed = true
  window.removeEventListener('pointerup', onOrbPointerUp)
  window.removeEventListener('pointerdown', onFirstGesture)
  window.removeEventListener('keydown', onFirstGesture)
  stopListening()
  stopVoskRecognition()
  stopEdgeSpeech()
  // 全双工语音流：释放麦克风 / AudioWorklet / AudioContext / WebSocket
  clearTimeout(endpointTimer)
  endpointTimer = 0
  pendingTranscript = ''
  clearTts()
  disposeVoiceStream()
  clearVoiceHighlight()
  dismissGuide()
})
</script>

<style scoped>
/* ===== 页面引导小框 ===== */
.page-guide {
  position: fixed;
  top: 72px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  max-width: 420px;
  padding: 10px 14px;
  background: linear-gradient(135deg, rgba(15, 20, 40, 0.92), rgba(20, 15, 35, 0.92));
  backdrop-filter: blur(16px) saturate(1.5);
  -webkit-backdrop-filter: blur(16px) saturate(1.5);
  border: 1px solid rgba(34, 211, 238, 0.25);
  border-radius: 14px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.5), 0 0 20px rgba(34, 211, 238, 0.1);
  cursor: pointer;
}
.guide-orb {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #22d3ee, #a855f7);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #fff;
  box-shadow: 0 0 12px rgba(168, 85, 247, 0.5);
}
.guide-body { flex: 1; min-width: 0; }
.guide-title {
  font-size: 12px;
  font-weight: 700;
  color: #67e8f9;
  margin-bottom: 3px;
  letter-spacing: 0.05em;
}
.guide-text {
  font-size: 11.5px;
  line-height: 1.55;
  color: rgba(255, 255, 255, 0.85);
  word-break: break-word;
}
.guide-caret {
  display: inline-block;
  width: 1.5px;
  height: 1em;
  margin-left: 1px;
  vertical-align: -0.12em;
  background: #67e8f9;
  animation: guideBlink 0.8s steps(1) infinite;
}
@keyframes guideBlink { 0%, 55% { opacity: 1; } 56%, 100% { opacity: 0; } }
.guide-close {
  flex-shrink: 0;
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.35);
  font-size: 12px;
  cursor: pointer;
  padding: 2px 4px;
  line-height: 1;
  transition: color 0.2s;
}
.guide-close:hover { color: #fff; }
.guide-pop-enter-active { transition: all 0.35s cubic-bezier(0.34, 1.56, 0.64, 1); }
.guide-pop-leave-active { transition: all 0.25s ease; }
.guide-pop-enter-from { opacity: 0; transform: translate(-50%, -12px); }
.guide-pop-leave-to { opacity: 0; transform: translate(-50%, -8px); }

/* ===== 引导框与星途球联动 ===== */
/* 球在引导框显示时发光脉冲，模拟"球在介绍页面" */
.xingtu-orb.orb-guide-active {
  border-color: rgba(34, 211, 238, 0.9);
  box-shadow: 0 0 18px rgba(34, 211, 238, 0.6), 0 0 42px rgba(168, 85, 247, 0.4);
  animation: orbGuideGlow 1.6s ease-in-out infinite;
}
.xingtu-orb.orb-guide-active .orb-core {
  background: linear-gradient(135deg, #22d3ee, #a855f7);
  box-shadow: 0 0 20px rgba(34, 211, 238, 0.8), 0 0 40px rgba(168, 85, 247, 0.5);
  animation: coreGuidePulse 1.2s ease-in-out infinite alternate;
}
.xingtu-orb.orb-guide-active .orb-ring {
  border-color: rgba(34, 211, 238, 0.5);
  animation-duration: 1.4s;
}
@keyframes orbGuideGlow {
  0%, 100% { box-shadow: 0 0 18px rgba(34, 211, 238, 0.5), 0 0 42px rgba(168, 85, 247, 0.35); }
  50% { box-shadow: 0 0 26px rgba(34, 211, 238, 0.85), 0 0 60px rgba(168, 85, 247, 0.6); }
}
@keyframes coreGuidePulse {
  from { transform: scale(1); filter: brightness(1); }
  to { transform: scale(1.2); filter: brightness(1.4); }
}

/* 引导框底部的联动连线，指向右下角星途球方向 */
.page-guide {
  position: relative;
}
.guide-link-line {
  position: absolute;
  bottom: -28px;
  right: 18%;
  width: 2px;
  height: 28px;
  background: linear-gradient(180deg, rgba(34, 211, 238, 0.5), rgba(168, 85, 247, 0));
  pointer-events: none;
}
.guide-link-line::after {
  content: '';
  position: absolute;
  bottom: -4px;
  left: 50%;
  transform: translateX(-50%);
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #22d3ee;
  box-shadow: 0 0 8px rgba(34, 211, 238, 0.8);
  animation: guideDotPulse 1.2s ease-in-out infinite;
}
@keyframes guideDotPulse {
  0%, 100% { opacity: 0.4; transform: translateX(-50%) scale(0.8); }
  50% { opacity: 1; transform: translateX(-50%) scale(1.3); }
}
/* 打字时引导框边框呼吸 */
.page-guide.guide-typing {
  border-color: rgba(34, 211, 238, 0.5);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.5), 0 0 24px rgba(34, 211, 238, 0.2);
}

.xingtu-container {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 9999;
  width: 56px;
  height: 56px;
}

/* ---- ORB ---- */
.xingtu-orb {
  width: 100%; height: 100%;
  border-radius: 50%;
  border: 2px solid rgba(102, 126, 234, 0.5);
  background: rgba(10, 10, 30, 0.95);
  backdrop-filter: blur(20px);
  cursor: pointer;
  position: relative;
  transition: all 0.3s;
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.3);
  display: flex; align-items: center; justify-content: center;
  touch-action: none;
}
.xingtu-orb:hover { transform: scale(1.1); border-color: rgba(102, 126, 234, 0.8); box-shadow: 0 4px 30px rgba(102, 126, 234, 0.5); }
.xingtu-orb.orb-dragging { cursor: grabbing; opacity: 0.92; transform: scale(1); }

.orb-core {
  width: 16px; height: 16px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 50%;
  box-shadow: 0 0 15px rgba(102, 126, 234, 0.6);
  z-index: 1;
}
.orb-ring {
  position: absolute; inset: -6px;
  border: 1px solid rgba(102, 126, 234, 0.3);
  border-radius: 50%;
  animation: orbPulse 2s ease-in-out infinite;
}

/* Speaking animation */
.xingtu-orb.orb-speaking {
  border-color: rgba(102, 126, 234, 0.95);
  box-shadow: 0 0 18px rgba(102, 126, 234, 0.8), 0 0 42px rgba(118, 75, 162, 0.55);
}
.xingtu-orb.orb-speaking .orb-ring {
  animation-duration: 1.2s;
  border-color: rgba(102, 126, 234, 0.8);
}
.xingtu-orb.orb-speaking .orb-core {
  animation: coreGlow 0.8s ease-in-out infinite alternate;
  box-shadow: 0 0 25px rgba(102, 126, 234, 0.9), 0 0 50px rgba(118, 75, 162, 0.5);
}
.orb-core.orb-pulse {
  animation: orbCorePulse 0.3s ease-out !important;
}
@keyframes orbCorePulse {
  0% { transform: scale(2); filter: brightness(2); box-shadow: 0 0 45px rgba(0, 255, 255, 0.95), 0 0 90px rgba(102, 126, 234, 0.9); }
  100% { transform: scale(1); filter: brightness(1.1); box-shadow: 0 0 25px rgba(102, 126, 234, 0.9); }
}
@keyframes coreGlow {
  from { transform: scale(1); filter: brightness(1); }
  to { transform: scale(1.15); filter: brightness(1.3); }
}

.orb-waves {
  position: absolute; inset: -10px;
  display: flex; align-items: center; justify-content: center; gap: 3px;
}
.orb-waves span {
  width: 3px; height: 14px;
  background: linear-gradient(180deg, #667eea, #00ffff);
  border-radius: 2px;
  animation: waveBar 1.2s ease-in-out infinite;
}
.orb-waves span:nth-child(2) { animation-delay: 0.1s; }
.orb-waves span:nth-child(3) { animation-delay: 0.2s; }
.orb-waves span:nth-child(4) { animation-delay: 0.3s; }
@keyframes waveBar { 0%, 100% { height: 6px; opacity: 0.5; } 50% { height: 18px; opacity: 1; } }

@keyframes orbPulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.15); opacity: 0; }
}

/* ---- PANEL（绝对定位挂球上方，不影响布局/拖动）---- */
.xingtu-panel {
  position: absolute;
  right: 0;
  bottom: 64px;
  width: 380px;
  max-height: 520px;
  background: rgba(8, 8, 24, 0.97);
  backdrop-filter: blur(30px);
  border: 1px solid rgba(102, 126, 234, 0.2);
  border-radius: 20px;
  overflow: hidden;
  display: flex; flex-direction: column;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.5), 0 0 60px rgba(102, 126, 234, 0.08);
  transform-origin: bottom right;
  transform: scale(0.8) translateY(20px);
  opacity: 0;
  pointer-events: none;
}
.xingtu-panel.open {
  transform: scale(1) translateY(0);
  opacity: 1;
  pointer-events: auto;
}

/* Header */
.panel-header {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
}
.panel-logo {
  position: relative; width: 36px; height: 36px;
  display: flex; align-items: center; justify-content: center;
}
.logo-orb {
  width: 16px; height: 16px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 50%;
  box-shadow: 0 0 15px rgba(102, 126, 234, 0.6);
  z-index: 1;
}
.logo-ring {
  position: absolute; inset: 0;
  border: 1px solid rgba(102, 126, 234, 0.3);
  border-radius: 50%;
  animation: orbit 4s linear infinite;
}
@keyframes orbit { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

.panel-title h3 { font-size: 0.9rem; font-weight: 700; color: white; }
.panel-title small { font-size: 0.65rem; color: rgba(255,255,255,0.72); font-weight: 400; margin-left: 4px; }
.version { font-size: 0.6rem; color: rgba(255,255,255,0.45); letter-spacing: 0.05em; }

.panel-close {
  margin-left: auto;
  width: 28px; height: 28px;
  border: none; background: rgba(255,255,255,0.05);
  color: rgba(255,255,255,0.78);
  border-radius: 8px; cursor: pointer; font-size: 0.75rem;
  transition: all 0.2s;
}
.panel-close:hover { background: rgba(255,255,255,0.1); color: white; }

/* Status */
.status-bar {
  padding: 8px 16px;
  background: rgba(255,255,255,0.02);
  border-bottom: 1px solid rgba(255,255,255,0.04);
  display: flex; justify-content: space-between; align-items: center;
}
.status-item { display: flex; align-items: center; gap: 6px; }
.status-dot {
  width: 7px; height: 7px; border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 6px rgba(16, 185, 129, 0.6);
}
.status-dot.listening { background: #667eea; box-shadow: 0 0 8px rgba(102, 126, 234, 0.8); animation: blink 1s ease-in-out infinite; }
.status-dot.speaking { background: #f59e0b; box-shadow: 0 0 8px rgba(245, 158, 11, 0.8); animation: blink 0.6s ease-in-out infinite; }
.status-dot.processing { background: #f59e0b; }
.status-label { font-size: 0.7rem; font-family: monospace; color: rgba(255,255,255,0.78); }
/* 受限 iframe 下的救场按钮：一键在新标签页打开以获取麦克风授权 */
.mic-fix-btn {
  flex-shrink: 0;
  padding: 3px 10px;
  font-size: 0.62rem;
  color: #67e8f9;
  background: rgba(34, 211, 238, 0.12);
  border: 1px solid rgba(34, 211, 238, 0.45);
  border-radius: 999px;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}
.mic-fix-btn:hover { background: rgba(34, 211, 238, 0.25); color: #fff; }
@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0.3; } }

.wake-hint { font-size: 0.6rem; color: rgba(255,255,255,0.5); font-style: italic; }

/* Messages */
.messages {
  flex: 1; overflow-y: auto;
  padding: 12px;
  min-height: 180px; max-height: 260px;
}
.messages::-webkit-scrollbar { width: 4px; }
.messages::-webkit-scrollbar-thumb { background: rgba(102, 126, 234, 0.3); border-radius: 2px; }

.empty-hint {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  height: 100%; text-align: center; padding: 1.5rem;
}
.hint-icon { font-size: 2.5rem; color: rgba(102, 126, 234, 0.4); margin-bottom: 1rem; animation: floatIcon 3s ease-in-out infinite; }
@keyframes floatIcon { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-8px); } }
.empty-hint p { font-size: 0.75rem; color: rgba(255,255,255,0.6); margin-bottom: 1rem; }

.quick-actions { display: flex; flex-wrap: wrap; gap: 6px; justify-content: center; }
.qbtn {
  padding: 5px 12px;
  background: rgba(102, 126, 234, 0.08);
  border: 1px solid rgba(102, 126, 234, 0.15);
  border-radius: 16px;
  color: rgba(255,255,255,0.78);
  font-size: 0.7rem; cursor: pointer;
  transition: all 0.2s;
}
.qbtn:hover { background: rgba(102, 126, 234, 0.2); border-color: rgba(102, 126, 234, 0.4); color: white; }

/* Message bubbles */
.msg {
  display: flex; gap: 10px; margin-bottom: 12px;
  animation: msgIn 0.3s ease-out;
}
@keyframes msgIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }

.msg-avatar {
  width: 30px; height: 30px;
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-size: 0.8rem; flex-shrink: 0;
}
.msg.user .msg-avatar { background: rgba(102, 126, 234, 0.15); border: 1px solid rgba(102, 126, 234, 0.25); }
.msg.assistant .msg-avatar { background: rgba(118, 75, 162, 0.15); border: 1px solid rgba(118, 75, 162, 0.25); }

.msg-content { flex: 1; min-width: 0; }
.msg-role { font-size: 0.6rem; color: rgba(255,255,255,0.6); margin-bottom: 3px; letter-spacing: 0.03em; display: flex; align-items: center; gap: 6px; }
.msg-save-btn { font-size: 12px; opacity: 0.45; transition: opacity .2s, transform .2s; }
.msg-save-btn:hover { opacity: 1; transform: scale(1.15); }
.msg-text { font-size: 0.8rem; color: rgba(255,255,255,0.92); line-height: 1.5; word-break: break-word; }

.listening-dots { display: flex; gap: 4px; align-items: center; }
.listening-dots span {
  width: 5px; height: 5px; background: #667eea;
  border-radius: 50%; animation: dotPulse 1.4s ease-in-out infinite;
}
.listening-dots span:nth-child(2) { animation-delay: 0.15s; }
.listening-dots span:nth-child(3) { animation-delay: 0.3s; }
@keyframes dotPulse { 0%, 100% { opacity: 0.3; transform: scale(0.7); } 50% { opacity: 1; transform: scale(1.2); } }

/* 实时字幕：边说边出的识别文本，尾部光标提示仍在识别 */
.live-text {
  color: rgba(255, 255, 255, 0.9);
  position: relative;
}
.live-text::after {
  content: '';
  display: inline-block;
  width: 2px; height: 1em;
  margin-left: 3px;
  vertical-align: -0.15em;
  background: #667eea;
  animation: liveCaret 0.9s steps(1) infinite;
}
@keyframes liveCaret { 0%, 55% { opacity: 1; } 56%, 100% { opacity: 0; } }

/* Controls */
.panel-controls {
  display: flex; gap: 8px; padding: 10px 16px;
  border-top: 1px solid rgba(255,255,255,0.06);
  background: rgba(255,255,255,0.02);
}
.ctrl-btn {
  flex: 1; display: flex; flex-direction: column; align-items: center; gap: 3px;
  padding: 9px 6px;
  border: 1px solid rgba(255,255,255,0.07);
  background: rgba(255,255,255,0.03);
  color: rgba(255,255,255,0.78);
  border-radius: 10px; cursor: pointer;
  transition: all 0.2s; font-size: 0;
}
.ctrl-btn:hover { background: rgba(255,255,255,0.06); border-color: rgba(255,255,255,0.15); color: white; }
.ctrl-btn.active { background: rgba(239, 68, 68, 0.1); border-color: rgba(239, 68, 68, 0.3); color: #ef4444; }
.ctrl-btn.wrec-active { background: rgba(52, 211, 153, 0.12); border-color: rgba(52, 211, 153, 0.4); color: #34d399; box-shadow: 0 0 18px rgba(52,211,153,.25); }
.ctrl-icon { font-size: 1rem; }
.ctrl-label { font-size: 0.58rem; letter-spacing: 0.03em; }

/* Voice Selector */
.voice-selector {
  padding: 8px 16px;
  border-top: 1px solid rgba(255,255,255,0.06);
  display: flex; align-items: center; gap: 8px;
}
.voice-selector label { font-size: 0.65rem; color: rgba(255,255,255,0.65); }
.voice-selector select {
  flex: 1; padding: 4px 8px;
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 6px; color: rgba(255,255,255,0.85);
  font-size: 0.7rem; outline: none;
}
.voice-selector select option { background: #0a0a1a; color: white; }
.voice-selector select optgroup { background: #0a0a1a; color: rgba(255,255,255,0.5); font-style: normal; }

/* Mobile */
@media (max-width: 768px) {
  .xingtu-container { bottom: 12px; }
  .xingtu-panel { width: calc(100vw - 24px); max-width: 380px; max-height: 480px; }
}
</style>
