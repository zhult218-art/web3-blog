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
          <div v-if="isListening" class="msg user">
            <div class="msg-avatar">👤</div>
            <div class="msg-content">
              <div class="msg-role">你</div>
              <div class="msg-text listening-dots"><span></span><span></span><span></span></div>
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
            :class="{ active: isListening }"
            @click="toggleListening"
          >
            <span class="ctrl-icon">{{ isListening ? '⏹' : '🎤' }}</span>
            <span class="ctrl-label">{{ isListening ? '停止' : '语音' }}</span>
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
            <option v-for="v in voices" :key="v.uri" :value="v.uri">{{ v.name }}</option>
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
    <div class="orb-ring-2"></div>
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
// 语音输出优先 Edge 神经网络音色（晓晓/云希），失败回退浏览器 speechSynthesis
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
import { speakEdge, stopEdgeSpeech, EDGE_VOICES, isEdgeTtsSupported } from '@/api/edgeTts'
import { startWhisperRecord, whisperTranscribe } from '@/composables/useWhisperStt'
import { agentChatStream, consumeAgentStream } from '@/api/agent'
import { createEntry, getOwnerUser } from '@/api/knowledge'
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
const msgBox = ref(null)
let recognition = null
let synth = null
let wakeWordDetected = false
let manualStop = false
let disposed = false
let lastFinalIndex = 0
let wakeTimer = 0
let wakeWordTimer = 0
let sessionId = null
let backendOk = false
let edgeOk = false
let voskOk = false
const voices = EDGE_VOICES
const voiceURI = ref('zh-CN-XiaoxiaoNeural')
let selectedVoiceURI = 'zh-CN-XiaoxiaoNeural'

// 全屏固定页（群聊）内隐藏悬浮球：避免遮挡页面自身的发送等操作按钮
const orbHidden = computed(() => route.path === '/community/chat')

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
const wBusy = ref(false)
let wHandle = null
async function wStart() {
  if (wBusy.value) return
  if (isListening.value) { toggleListening() }
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
    // 简单的成功提示（不依赖 toast store，用浏览器通知兜底）
    try {
      const { useToastStore } = await import('@/stores/modules/toast')
      useToastStore().success('已存入知识库 🌱')
    } catch {
      alert('已存入知识库')
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

// ---- TTS: 说话（Edge 神经网络音色优先，浏览器语音兜底）----
// Edge 音色质量高但走公网；失败/超时自动回退本地 speechSynthesis，保证必能发声
function localSpeech(text) {
  return new Promise(resolve => {
    if (!synth) { resolve(false); return }
    synth.cancel()
    const utter = new SpeechSynthesisUtterance(text)
    utter.lang = 'zh-CN'
    utter.rate = 0.95
    utter.pitch = 1.1
    utter.onstart = () => { isSpeaking.value = true }
    utter.onboundary = () => { pulseOrbCore() }
    utter.onend = () => { isSpeaking.value = false; resolve(true) }
    utter.onerror = () => { isSpeaking.value = false; resolve(false) }
    synth.speak(utter)
  })
}

async function speak(text) {
  if (isSpeaking.value) return
  const wasListening = isListening.value || isVoskRunning()
  if (wasListening) {
    stopVoskRecognition()
    if (recognition) { try { recognition.stop() } catch {} }
    isListening.value = false
  }
  isSpeaking.value = true
  let ok = false
  if (edgeOk) {
    try { ok = await speakEdge(text, selectedVoiceURI, 0) } catch { ok = false }
  }
  if (!ok) ok = await localSpeech(text)
  isSpeaking.value = false
  if (wasListening && !manualStop && !disposed) {
    setTimeout(() => startListening(), 200)
  }
}

let orbPulseTimer = 0
function pulseOrbCore() {
  if (orbPulseTimer) { clearTimeout(orbPulseTimer); orbPulseTimer = 0 }
  if (!isSpeaking.value) return
  const core = orbBtn.value && orbBtn.value.querySelector('.orb-core')
  if (!core) return
  core.classList.remove('orb-pulse')
  void core.offsetWidth
  core.classList.add('orb-pulse')
  orbPulseTimer = setTimeout(() => {
    if (core) core.classList.remove('orb-pulse')
  }, 300)
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
  { match: ['关于', '介绍'], action: '/about', reply: '正在打开关于页面' },
  { match: ['个人中心', '我的'], action: '/profile', reply: '正在打开个人中心' },
  { match: ['订单'], action: '/profile/orders', reply: '正在打开我的订单' },
  { match: ['支付', '充值'], action: '/pay', reply: '正在打开支付页面' },
  { match: ['登录'], action: '/login', reply: '正在跳转到登录页面' },
]

function goLocal(text) {
  const found = LOCAL_CMDS.find(c => c.match.some(k => text.includes(k)))
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
  }
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

  // 导航指令：本地直接处理，不依赖后端识别
  const nav = goLocal(text)
  if (nav.path) {
    setTimeout(() => {
      addMsg('assistant', nav.reply)
      speak(nav.reply)
      statusText.value = '就绪'
      navigate(nav.path)
    }, 300)
    return
  }

  // 后端优先：已登录且服务可用 → 走 /jarvis/recognize
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
    streamMsg.text = '抱歉，Agent 暂时不可用：' + (e?.message || '网络错误')
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
  manualStop = false
  lastFinalIndex = 0
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
  statusText.value = '语音引擎加载中，请稍候...'
  speak('语音引擎正在加载，请稍后再试')
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
    console.log('[xingtu] Web Speech recognition started')
  } catch (e) {
    try { recognition.stop() } catch {}
    setTimeout(() => {
      try {
        recognition.start()
        isListening.value = true
        statusText.value = '监听中...'
      } catch (_) {
        statusText.value = '就绪'
        console.warn('[xingtu] Web Speech start failed:', _)
      }
    }, 400)
  }
}

function stopListening() {
  if (isListening.value || isVoskRunning()) {
    manualStop = true
    stopVoskRecognition()
    if (recognition) { try { recognition.stop() } catch {} }
    isListening.value = false
    statusText.value = '就绪'
  }
}

function initRecognition() {
  if (recognition) return true
  const SR = window.SpeechRecognition || window.webkitSpeechRecognition
  if (!SR) { console.warn('[xingtu] SpeechRecognition not supported'); return false }
  recognition = new SR()
  recognition.continuous = true
  recognition.interimResults = true
  recognition.lang = 'zh-CN'

  recognition.onresult = (e) => {
    const total = e.results.length
    let interim = ''
    for (let i = lastFinalIndex; i < total; i++) {
      const r = e.results[i]
      if (r.isFinal) {
        lastFinalIndex = i + 1
        processTranscript(r[0].transcript)
      } else {
        interim += r[0].transcript
      }
    }
    if (lastFinalIndex > total) lastFinalIndex = Math.max(0, total)
    statusText.value = interim ? '识别中...' : '监听中...'
  }
  recognition.onerror = (e) => {
    if (disposed) return
    console.warn('[xingtu] recognition error:', e.error)
    if (e.error === 'not-allowed') {
      statusText.value = '麦克风权限被拒绝'
      isListening.value = false
    } else if (e.error === 'no-speech') {
      statusText.value = '监听中...'
    } else if (e.error === 'aborted' || e.error === 'network') {
      isListening.value = false
      statusText.value = '就绪'
    } else {
      isListening.value = false
      statusText.value = '识别错误: ' + e.error
    }
    if (!isListening.value && !manualStop && !disposed && e.error !== 'not-allowed') {
      setTimeout(silentRestart, 800)
    }
  }
  recognition.onend = () => {
    isListening.value = false
    statusText.value = '就绪'
    if (!manualStop && !disposed) setTimeout(silentRestart, 800)
  }
  return true
}

// 连续监听：面板关闭也持续监听；仅被手动停止/页面隐藏/组件卸载时暂停
function silentRestart() {
  if (manualStop || isListening.value || disposed) return
  if (typeof document !== 'undefined' && document.visibilityState === 'hidden') return
  // 优先 Web Speech
  if (initRecognition()) {
    lastFinalIndex = 0
    try {
      recognition.start()
      isListening.value = true
      statusText.value = '监听中...'
    } catch (e) {
      try { recognition.stop() } catch {}
      setTimeout(() => {
        try {
          if (!isListening.value && !manualStop && !disposed) {
            recognition.start()
            isListening.value = true
            statusText.value = '监听中...'
          }
        } catch {}
      }, 400)
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
  setTimeout(() => {
    if (!disposed && !isListening.value && !manualStop) startListening()
  }, 600)
}
function onFirstGesture() {
  tryAutoListen()
  window.removeEventListener('pointerdown', onFirstGesture)
  window.removeEventListener('keydown', onFirstGesture)
}

// ---- Voice: Edge 音色选择 ----
function onVoiceChange() {
  selectedVoiceURI = voiceURI.value || 'zh-CN-XiaoxiaoNeural'
}

// ---- Lifecycle ----
onMounted(() => {
  synth = window.speechSynthesis || null
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
    console.warn('[xingtu] Web Speech API not supported in this browser')
  } else {
    console.log('[xingtu] Web Speech API available')
  }

  // 预加载 Vosk 模型（不自动启动监听，等用户点击球或按钮）
  statusText.value = '加载中...'
  setTimeout(() => {
    loadVoskModel()
      .then(() => { voskOk = true; statusText.value = '就绪'; console.log('[xingtu] vosk ready') })
      .catch(() => { voskOk = false; statusText.value = '就绪'; console.log('[xingtu] vosk unavailable, will use Web Speech') })
  }, 1000)

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
  if (synth) synth.cancel()
  stopEdgeSpeech()
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
.xingtu-orb.orb-guide-active .orb-ring,
.xingtu-orb.orb-guide-active .orb-ring-2 {
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
.orb-ring-2 {
  position: absolute; inset: -12px;
  border: 1px solid rgba(118, 75, 162, 0.15);
  border-radius: 50%;
  animation: orbPulse 2s ease-in-out 1s infinite;
}

/* Speaking animation */
.xingtu-orb.orb-speaking {
  border-color: rgba(102, 126, 234, 0.95);
  box-shadow: 0 0 18px rgba(102, 126, 234, 0.8), 0 0 42px rgba(118, 75, 162, 0.55);
}
.xingtu-orb.orb-speaking .orb-ring,
.xingtu-orb.orb-speaking .orb-ring-2 {
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

/* Mobile */
@media (max-width: 768px) {
  .xingtu-container { bottom: 12px; }
  .xingtu-panel { width: calc(100vw - 24px); max-width: 380px; max-height: 480px; }
}
</style>
