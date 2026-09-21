<template>
  <div class="agent-page">
    <div class="agent-shell">
      <!-- ═══ 头部 ═══ -->
      <header class="agent-head">
        <div class="flex items-center gap-3">
          <div class="agent-orb"><span></span></div>
          <div>
            <h1 class="agent-title">Agent 会话台<span class="agent-dot">.</span></h1>
            <p class="agent-sub">输入任意需求 · 按 Workflow 路由到专用 Agent · 流式输出</p>
          </div>
        </div>
        <div class="flex items-center gap-2">
          <span v-if="aiServiceStatus === 'online'" class="route-badge route-on">
            <span class="w-1.5 h-1.5 rounded-full bg-emerald-400"></span> AI 在线
          </span>
          <span v-else-if="aiServiceStatus === 'offline'" class="route-badge" style="border-color:rgba(248,113,113,0.3)">
            <span class="w-1.5 h-1.5 rounded-full bg-red-400"></span> AI 离线
          </span>
          <span class="route-badge" :class="activeFlow || lastAgent ? 'route-on' : ''">
            <span class="w-1.5 h-1.5 rounded-full" :class="activeFlow || lastAgent ? 'bg-emerald-400 animate-pulse' : 'bg-slate-600'"></span>
            {{ activeFlow ? `技能 → ${activeFlow.name}` : (lastAgent ? `路由 → ${agentLabel(lastAgent)}` : '待机') }}
          </span>
          <button v-if="messages.length" @click="clearAll" class="agent-icon-btn" title="清空会话">🗑</button>
        </div>
      </header>

      <!-- ═══ 消息 / 欢迎 ═══ -->
      <div ref="msgBox" class="agent-body">
        <div v-if="messages.length === 0" class="agent-empty">
          <div class="empty-orb"><span></span></div>
          <p class="empty-title">你好，我是你的 Web3 门户 Agent</p>
          <p class="empty-desc">可以直接对话（写作 / 音乐 / 数据 / 通用），也可以点选下方的技能卡片：分步收集需求，自动产出图表、表格、Word 或海报。</p>

          <div class="skill-grid">
            <button v-for="s in skills" :key="s.slug" class="skill-card" @click="startFromSkill(s)" :disabled="busy">
              <span class="skill-icon" :style="{ background: s.color + '22' }">{{ s.icon }}</span>
              <span class="skill-name">{{ s.name }}</span>
              <span class="skill-sample">{{ s.sample }}</span>
              <span class="skill-cta">开始 <b>→</b></span>
            </button>
          </div>

          <div class="empty-suggests">
            <button v-for="ch in suggests" :key="ch" class="suggest-chip" @click="quickAsk(ch)">{{ ch }}</button>
          </div>
        </div>

        <div v-else class="space-y-5">
          <div v-for="(m, i) in messages" :key="i" class="msg-row" :class="m.role">
            <div class="msg-avatar">{{ m.role === 'user' ? '你' : 'A' }}</div>
            <div class="msg-main">
              <div v-if="m.isQuestion && activeFlow" class="msg-agent-tag">{{ activeFlow.icon }} {{ activeFlow.name }} · 第 {{ m.stepOf }}/{{ m.stepTotal }} 步</div>
              <div v-else-if="m.agent && m.agent !== 'general'" class="msg-agent-tag">{{ agentLabel(m.agent) }}</div>

              <div v-if="m.artifact" class="artifact-card">
                <!-- 图表 -->
                <div v-if="m.artifact.format === 'chart'" class="chart-wrap">
                  <div :ref="el => bindChart(el, m)" class="chart-box"></div>
                  <div class="artifact-foot">
                    <span class="artifact-title">{{ m.artifact.payload.title || '图表' }}</span>
                    <button class="mini-btn" @click="downloadArtifact(m)">⬇ 下载 PNG</button>
                  </div>
                </div>
                <!-- 表格 -->
                <div v-else-if="m.artifact.format === 'table'" class="table-wrap">
                  <div class="artifact-foot">
                    <span class="artifact-title">{{ m.artifact.payload.title || '表格' }}</span>
                    <button class="mini-btn" @click="downloadArtifact(m)">⬇ 下载 CSV</button>
                  </div>
                  <table class="flow-table">
                    <thead>
                      <tr><th v-for="(c, ci) in m.artifact.payload.columns" :key="ci">{{ c }}</th></tr>
                    </thead>
                    <tbody>
                      <tr v-for="(row, ri) in m.artifact.payload.rows" :key="ri">
                        <td v-for="(cell, ci) in row" :key="ci">{{ cell }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
                <!-- 文档 -->
                <div v-else-if="m.artifact.format === 'word'" class="word-wrap">
                  <span class="artifact-title">{{ m.artifact.payload.title || '文档' }}</span>
                  <div v-for="(sec, si) in m.artifact.payload.sections" :key="si" class="word-sec">
                    <div class="word-heading">{{ sec.heading }}</div>
                    <p v-for="(p, pi) in sec.paragraphs" :key="pi" class="word-para">{{ p }}</p>
                    <ul v-if="sec.bullets && sec.bullets.length">
                      <li v-for="(b, bi) in sec.bullets" :key="bi">· {{ b }}</li>
                    </ul>
                    <div v-if="sec.note" class="word-note">{{ sec.note }}</div>
                  </div>
                  <div class="artifact-foot">
                    <span class="artifact-title">{{ m.artifact.payload.footer }}</span>
                    <button class="mini-btn" @click="downloadArtifact(m)">⬇ 下载</button>
                  </div>
                </div>
                <!-- 海报 -->
                <div v-else-if="m.artifact.format === 'image'" class="image-wrap">
                  <div class="poster-box" v-html="m.artifact.payload.svg"></div>
                  <div class="artifact-foot">
                    <span class="artifact-title">海报</span>
                    <button class="mini-btn" @click="downloadArtifact(m)">⬇ 下载 SVG</button>
                  </div>
                </div>

                <div class="meta-line" v-if="m.meta">
                  {{ m.meta.llm ? '智能生成' : '本地生成' }} · {{ m.meta.saved ? '已存档' : '未存档' }}
                </div>
              </div>

              <div v-else class="msg-bubble">
                <span v-if="m.role === 'assistant' && !m.cancelled" class="msg-spinner" v-show="m.streaming && !m.raw">▍</span>
                <span class="whitespace-pre-wrap text-[13.5px] leading-relaxed">{{ m.raw }}</span>
              </div>

              <div v-if="m.role === 'assistant' && (m.streaming || m.actions)" class="msg-actions">
                <button v-if="m.streaming && !m.cancelled" class="mini-btn danger" @click="m.cancelled = true">■ 停止</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ═══ 输入 ═══ -->
      <footer class="agent-input-wrap">
        <div class="agent-input-box">
          <textarea ref="inputArea" v-model="input" rows="1" @keydown="onKey"
            class="agent-textarea" :placeholder="placeholderText"
            :disabled="busy"></textarea>
          <button class="agent-send" :disabled="busy || !input.trim()" @click="send">
            <span v-if="busy" class="send-spinner"></span>
            <span v-else>➤</span>
          </button>
        </div>
        <p class="agent-input-hint">{{ inputHint }}</p>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, watch, computed } from 'vue'
import * as echarts from 'echarts'
import { agentChatStream, consumeAgentStream, agentWorkflows, agentMatchWorkflow, agentExecuteFlow, consumeAgentFlowEvents } from '@/api/agent'

const messages = ref([])
const input = ref('')
const busy = ref(false)
const lastAgent = ref('')
const inputArea = ref(null)
const msgBox = ref(null)
const skills = ref([])
const activeFlow = ref(null)
const aiServiceStatus = ref('checking') // checking/online/offline

// 探测 ai-proxy-service(8093) 是否在线：用 GET /api/agent/workflows 作轻量 ping
async function checkAiStatus() {
  try {
    const ctrl = new AbortController()
    const timer = setTimeout(() => ctrl.abort(), 3000)
    const res = await fetch('/api/agent/workflows', { method: 'get', credentials: 'include', signal: ctrl.signal })
      .catch(() => null)
    clearTimeout(timer)
    aiServiceStatus.value = (res && res.ok) ? 'online' : 'offline'
  } catch {
    aiServiceStatus.value = 'offline'
  }
}

const suggests = [
  '帮我写一篇关于 Supabase 接入的博客文章',
  '推荐几首适合夜晚的民谣',
  '查一下站内有哪些示例数据和服务',
  '用三句话介绍这个 Web3 门户',
]

const placeholderText = computed(() =>
  activeFlow.value ? `回答「${currentStep().question}」…` : '给 Agent 下指令，例如：帮我画一张季度销售折线图…'
)
const inputHint = computed(() =>
  activeFlow.value
    ? `技能模式：Enter 提交答案 · 第 ${currentStep() ? currentStepIndex() + 1 : 0}/${activeFlow.value.steps.length} 步`
    : 'Enter 发送 · Shift+Enter 换行 · 命中「图表/表格/文档/海报」等关键词会自动进入技能'
)

function currentStep() {
  return activeFlow.value?.steps?.[activeFlow.value.index] || null
}
function currentStepIndex() {
  return activeFlow.value?.index ?? 0
}

function agentLabel(agent) {
  return { music: '音乐 Agent', article: '写作 Agent', data: '数据 Agent', general: '通用 Agent' }[agent] || agent
}

function quickAsk(text) {
  input.value = text
  send()
}

function scrollBottom() {
  nextTick(() => {
    if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight
  })
}

function clearAll() {
  messages.value = []
  lastAgent.value = ''
  activeFlow.value = null
}

function onKey(e) {
  if (e.key === 'Enter' && !e.shiftKey && !busy.value) {
    e.preventDefault()
    send()
  }
}

function pushUser(text) {
  messages.value.push({ role: 'user', content: text })
  scrollBottom()
}

function pushAssistant(extra) {
  messages.value.push({ role: 'assistant', raw: '', streaming: false, cancelled: false, agent: '', ...extra })
  scrollBottom()
}

// ═════════ 技能工作流 ═════════

async function startFromSkill(wf) {
  if (busy.value) return
  await beginFlow(wf)
}

function beginFlow(wf) {
  const steps = wf.steps && wf.steps.length ? wf.steps : []
  activeFlow.value = {
    slug: wf.slug,
    name: wf.name,
    icon: wf.icon || '🧠',
    color: wf.color || '#a8b593',
    steps,
    index: 0,
    answers: {},
  }
  lastAgent.value = wf.slug
  pushAssistant({ raw: `${wf.icon || ''} 已进入「${wf.name}」技能，我来分步收集需求。` })
  if (!steps.length) {
    executeFlow()
  } else {
    pushQuestion()
  }
}

function pushQuestion() {
  const f = activeFlow.value
  const step = f.steps[f.index]
  pushAssistant({
    raw: step.question || '',
    isQuestion: true,
    stepOf: f.index + 1,
    stepTotal: f.steps.length,
    placeholder: step.placeholder || '',
  })
}

function answerFlow(text) {
  const f = activeFlow.value
  const step = f.steps[f.index]
  if (step) f.answers[step.id] = text
  f.index += 1
  if (f.index < f.steps.length) {
    pushQuestion()
  } else {
    executeFlow()
  }
}

async function executeFlow() {
  const f = activeFlow.value
  if (!f) return
  busy.value = true
  pushAssistant({ raw: '', streaming: true })
  const msg = messages.value[messages.value.length - 1]
  try {
    const res = await agentExecuteFlow(f.slug, f.answers)
    await consumeAgentFlowEvents(res, {
      onText: (t) => {
        if (msg.cancelled) return
        msg.raw += t
        scrollBottom()
      },
      onArtifact: (evt) => {
        msg.artifact = { format: evt.format, payload: evt.payload }
      },
      onError: (errText) => {
        if (!msg.raw && !msg.artifact) msg.raw = '⚠️ ' + errText
      },
    })
  } catch (e) {
    if (!msg.cancelled) {
      msg.raw = (msg.raw ? msg.raw + '\n\n' : '') + '⚠️ ' + (e?.message || '执行失败，请稍后重试')
    }
  } finally {
    msg.streaming = false
    busy.value = false
    activeFlow.value = null
    scrollBottom()
  }
}

// ═════════ 对话主入口 ═════════

async function send() {
  const text = input.value.trim()
  if (!text || busy.value) return
  input.value = ''
  resetTextarea()

  // 技能模式：先回答当前问题
  if (activeFlow.value) {
    pushUser(text)
    answerFlow(text)
    return
  }

  pushUser(text)

  // 命中技能关键词 → 自动进入技能向导
  try {
    const matched = await agentMatchWorkflow(text)
    if (matched && matched.workflow) {
      beginFlow(matched.workflow)
      return
    }
  } catch { /* 匹配失败继续自由对话 */ }

  await chatStream(text)
}

async function chatStream(text) {
  const assistantMsg = { role: 'assistant', content: '', raw: '', streaming: true, cancelled: false, agent: '' }
  messages.value.push(assistantMsg)
  busy.value = true
  scrollBottom()

  const history = messages.value
    .filter(m => m !== assistantMsg && m.raw !== undefined)
    .map(m => ({ role: m.role, content: m.content ?? m.raw }))
    .slice(-20)

  try {
    const res = await agentChatStream([
      ...history.slice(0, -1),
      { role: 'user', content: text },
    ])
    await consumeAgentStream(res, (delta) => {
      if (assistantMsg.cancelled) return
      assistantMsg.raw += delta
      assistantMsg.content = assistantMsg.raw
      scrollBottom()
    }, () => {
      assistantMsg.streaming = false
      if (!assistantMsg.raw) assistantMsg.raw = '（服务暂时没有返回，请稍后重试）'
      // 后端 fallback 带有 [NO_LLM_KEY] 标识 → 替换为更友好的提示
      if (/\[NO_LLM_KEY\]/i.test(assistantMsg.raw)) {
        assistantMsg.raw = '⚠️ 大模型未配置，请在后台「AI代理配置」填入 LLM API Key（如 DeepSeek），并重启 ai-proxy-service。'
        assistantMsg.content = assistantMsg.raw
      }
    })
  } catch (e) {
    if (!assistantMsg.cancelled) {
      const msg = e?.message || ''
      let hint = '⚠️ 请求失败，请稍后重试'
      if (/502|503|network|timeout|超时|ECONNREFUSED|Failed to fetch/i.test(msg)) {
        hint = '⚠️ AI 服务暂不可用，请检查 ai-proxy-service(8093) 是否启动'
      } else if (/fallback|上游|未连通|llm.*key|api.*key|\[NO_LLM_KEY\]/i.test(msg)) {
        hint = '⚠️ 大模型未配置，请在后台「AI代理配置」填入 LLM API Key（如 DeepSeek）'
      } else if (msg) {
        hint = `⚠️ ${msg}`
      }
      assistantMsg.raw = (assistantMsg.raw ? assistantMsg.raw + '\n\n' : '') + hint
      assistantMsg.content = assistantMsg.raw
    }
    assistantMsg.streaming = false
  } finally {
    busy.value = false
    scrollBottom()
  }
}

// ═════════ 图表渲染 / 下载 ═════════

function bindChart(el, m) {
  if (!el || m._chartInited || !m.artifact || m.artifact.format !== 'chart') return
  const option = m.artifact.payload.option
  if (!option) return
  const chart = echarts.init(el)
  chart.setOption(option)
  m._chart = chart
  m._chartInited = true
}

function downloadArtifact(m) {
  const art = m.artifact
  if (!art) return
  if (art.format === 'chart' && m._chart) {
    const url = m._chart.getDataURL({ pixelRatio: 2, backgroundColor: '#0e1220' })
    triggerDownload(url, (art.payload.title || 'chart') + '.png')
  } else if (art.format === 'image') {
    const blob = new Blob([art.payload.svg], { type: 'image/svg+xml' })
    triggerDownload(URL.createObjectURL(blob), (art.payload.title || 'poster') + '.svg')
  } else if (art.format === 'table') {
    const cols = art.payload.columns || []
    const rows = art.payload.rows || []
    const head = cols.map(csvCell).join(',')
    const body = rows.map(r => (r || []).map(csvCell).join(',')).join('\n')
    const csv = '\uFEFF' + (head + (body ? '\n' + body : ''))
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
    triggerDownload(URL.createObjectURL(blob), (art.payload.title || 'table') + '.csv')
  } else if (art.format === 'word') {
    const secs = (art.payload.sections || []).map(s => {
      const paras = (s.paragraphs || []).join('\n')
      const bullets = (s.bullets || []).map(b => '· ' + b).join('\n')
      return '# ' + (s.heading || '') + '\n' + paras + (bullets ? '\n' + bullets : '') + (s.note ? '\n（' + s.note + '）' : '')
    }).join('\n\n')
    const txt = (art.payload.title || '') + '\n\n' + secs + (art.payload.footer ? '\n\n— ' + art.payload.footer : '')
    const blob = new Blob([txt], { type: 'text/plain;charset=utf-8' })
    triggerDownload(URL.createObjectURL(blob), (art.payload.title || 'doc') + '.txt')
  }
}

function csvCell(v) {
  const s = String(v ?? '')
  return /[",\n;]/.test(s) ? '"' + s.replace(/"/g, '""') + '"' : s
}

function triggerDownload(url, filename) {
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  setTimeout(() => a.remove(), 0)
}

// ═════════ 路由提示 + 自动匹配 ═════════

watch(messages, (list) => {
  if (activeFlow.value) return
  const last = list[list.length - 2]
  if (last && last.role === 'user') {
    lastAgent.value = detectAgent(last.content)
    if (messages.value[messages.value.length - 1]) {
      messages.value[messages.value.length - 1].agent = lastAgent.value
    }
  }
})

function detectAgent(t) {
  const s = t || ''
  if (/(歌|音乐|播放|唱|曲|网易云|歌单|music)/.test(s)) return 'music'
  if (/(写|作文|文章|博客|文案|post|blog|标题|大纲|润色)/.test(s)) return 'article'
  if (/(数据|统计|查询|查一下|数据库|supabase|记录|报告|服务|健康|多少)/.test(s)) return 'data'
  return 'general'
}

function resetTextarea() {
  if (inputArea.value) inputArea.value.style.height = 'auto'
}

watch(input, () => {
  if (inputArea.value) {
    inputArea.value.style.height = 'auto'
    inputArea.value.style.height = Math.min(inputArea.value.scrollHeight, 160) + 'px'
  }
})

onMounted(async () => {
  resetTextarea()
  checkAiStatus()
  try {
    const list = await agentWorkflows()
    skills.value = Array.isArray(list) ? list : []
  } catch { /* 技能列表不可用时不展示栏目 */ }
})
</script>

<style scoped>
.agent-page {
  min-height: calc(100vh - 120px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 16px;
}
.agent-shell {
  width: 100%;
  max-width: 860px;
  height: min(76vh, 680px);
  display: flex;
  flex-direction: column;
  background: rgba(16, 18, 30, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.10);
  border-radius: 22px;
  backdrop-filter: blur(14px);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.45), inset 0 1px 0 rgba(255, 255, 255, 0.06);
  overflow: hidden;
}
.agent-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);
}
.agent-orb {
  width: 42px; height: 42px; border-radius: 50%;
  background: radial-gradient(circle at 32% 28%, #c9d6b8 0%, #a8b593 40%, #5a6b4a 100%);
  box-shadow: 0 0 18px rgba(168, 181, 147, 0.35);
  display: flex; align-items: center; justify-content: center;
}
.agent-orb span {
  width: 16px; height: 16px; border-radius: 50%;
  background: radial-gradient(circle, #fff 0%, #a8b593 60%, transparent 100%);
}
.agent-title {
  font-size: 18px; font-weight: 700; color: #fff; letter-spacing: -0.01em; line-height: 1.1;
}
.agent-dot { color: #a8b593; }
.agent-sub { font-size: 11px; color: #8b93a8; margin-top: 2px; }
.route-badge {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: 11px; color: #717a8c;
  padding: 5px 10px; border-radius: 999px;
  border: 1px solid rgba(255,255,255,0.08);
  background: rgba(255,255,255,0.03);
  transition: all .2s;
}
.route-badge.route-on { color: #b6e3c9; border-color: rgba(52,211,153,0.3); }
.agent-icon-btn {
  width: 32px; height: 32px; border-radius: 9px;
  border: 1px solid rgba(255,255,255,0.08); background: rgba(255,255,255,0.03);
  color: #8b93a8; font-size: 13px; cursor: pointer; transition: all .2s;
}
.agent-icon-btn:hover { color: #fff; border-color: rgba(255,255,255,0.2); }

.agent-body {
  flex: 1; overflow-y: auto; padding: 20px;
  scrollbar-width: thin; scrollbar-color: rgba(159,176,140,0.3) transparent;
}
.agent-body::-webkit-scrollbar { width: 5px; }
.agent-body::-webkit-scrollbar-thumb { background: rgba(159,176,140,0.3); border-radius: 999px; }

/* 空态 */
.agent-empty {
  height: 100%; display: flex; flex-direction: column;
  align-items: center; justify-content: center; text-align: center; padding: 16px;
}
.empty-orb {
  width: 72px; height: 72px; border-radius: 50%;
  background: radial-gradient(circle at 30% 25%, #e5ecd9 0%, #a8b593 45%, #3f4d33 100%);
  box-shadow: 0 0 34px rgba(168,181,147,0.35); display: flex; align-items: center; justify-content: center; margin-bottom: 14px;
}
.empty-orb span {
  width: 26px; height: 26px; border-radius: 50%;
  background: radial-gradient(circle, #fff, #a8b593 70%);
}
.empty-title { color: #e9edf5; font-size: 16px; font-weight: 600; }
.empty-desc { color: #7c8494; font-size: 12.5px; margin-top: 8px; max-width: 520px; line-height: 1.7; }

/* 技能卡片 */
.skill-grid {
  display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 10px; width: 100%; max-width: 640px; margin-top: 20px;
}
.skill-card {
  display: flex; flex-direction: column; align-items: flex-start; gap: 6px;
  padding: 14px; border-radius: 16px; text-align: left; cursor: pointer;
  border: 1px solid rgba(255,255,255,0.09); background: rgba(255,255,255,0.03);
  transition: all .2s;
}
.skill-card:hover:not(:disabled) {
  border-color: rgba(168,181,147,0.4); background: rgba(168,181,147,0.08);
  transform: translateY(-2px);
}
.skill-card:disabled { opacity: .5; cursor: not-allowed; }
.skill-icon {
  width: 34px; height: 34px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center; font-size: 17px;
}
.skill-name { color: #e9edf5; font-size: 13px; font-weight: 600; }
.skill-sample {
  color: #717a8c; font-size: 11px; line-height: 1.5;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.skill-cta { color: #b6d0a0; font-size: 11px; margin-top: 2px; }

.empty-suggests {
  display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; margin-top: 18px; max-width: 540px;
}
.suggest-chip {
  padding: 8px 14px; border-radius: 999px; font-size: 12px; color: #b6d0a0;
  border: 1px solid rgba(168,181,147,0.25); background: rgba(168,181,147,0.06); cursor: pointer; transition: all .2s;
}
.suggest-chip:hover { background: rgba(168,181,147,0.16); transform: translateY(-1px); }

/* 消息 */
.msg-row { display: flex; gap: 12px; }
.msg-row.user { flex-direction: row-reverse; }
.msg-avatar {
  width: 32px; height: 32px; border-radius: 10px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 600;
}
.msg-row.assistant .msg-avatar { background: linear-gradient(135deg,#a8b593,#5a6b4a); color: #14160f; }
.msg-row.user .msg-avatar { background: linear-gradient(135deg,#67e8f9,#7c3aed); color: #fff; }
.msg-main { max-width: 84%; display: flex; flex-direction: column; }
.msg-row.user .msg-main { align-items: flex-end; }
.msg-agent-tag { font-size: 10px; color: #9cc98d; margin-bottom: 4px; }
.msg-bubble {
  padding: 10px 14px; border-radius: 14px; font-size: 13.5px; line-height: 1.65; color: #e6eaf1;
}
.msg-row.assistant .msg-bubble {
  background: rgba(255,255,255,0.045); border: 1px solid rgba(255,255,255,0.07); border-top-left-radius: 4px;
}
.msg-row.user .msg-bubble {
  background: linear-gradient(135deg, rgba(103,232,249,0.16), rgba(124,58,237,0.16));
  border: 1px solid rgba(103,232,249,0.2); border-top-right-radius: 4px;
}
.msg-spinner { color: #a8b593; animation: blink 1s infinite; }
@keyframes blink { 50% { opacity: 0.2; } }
.msg-actions { margin-top: 6px; }
.mini-btn {
  font-size: 11px; padding: 4px 10px; border-radius: 8px;
  border: 1px solid rgba(255,255,255,0.1); background: rgba(255,255,255,0.04);
  color: #8b93a8; cursor: pointer; transition: all .2s;
}
.mini-btn:hover { color: #fff; border-color: rgba(255,255,255,0.25); }
.mini-btn.danger:hover { color: #fca5a5; border-color: rgba(252,165,165,0.3); }

/* 产物卡片 */
.artifact-card {
  width: 100%; border-radius: 14px; overflow: hidden;
  border: 1px solid rgba(255,255,255,0.1); background: rgba(10,12,22,0.65);
}
.artifact-foot {
  display: flex; align-items: center; justify-content: space-between; gap: 10px;
  padding: 9px 12px; border-top: 1px solid rgba(255,255,255,0.06);
}
.artifact-title { color: #9aa3b5; font-size: 12px; font-weight: 600; }
.meta-line {
  padding: 7px 12px; font-size: 10px; color: #5b6475;
  border-top: 1px dashed rgba(255,255,255,0.06);
}
.chart-wrap { padding: 10px; }
.chart-box { width: 100%; height: 300px; }
.table-wrap { padding: 12px; }
.flow-table { width: 100%; border-collapse: collapse; font-size: 12px; }
.flow-table th, .flow-table td {
  border: 1px solid rgba(255,255,255,0.09); padding: 6px 9px; text-align: left; color: #cfd6e6;
}
.flow-table th { background: rgba(255,255,255,0.05); color: #b6d0a0; font-weight: 600; }
.word-wrap { padding: 14px 16px; }
.word-sec { margin-top: 10px; }
.word-heading { color: #fcd34d; font-size: 13px; font-weight: 700; margin-bottom: 6px; }
.word-para { color: #cfd6e6; font-size: 12.5px; line-height: 1.7; margin: 4px 0; }
.word-wrap ul { margin: 6px 0 0 0; padding-left: 2px; }
.word-wrap li { color: #aab8cf; font-size: 12px; line-height: 1.7; list-style: none; }
.word-note { color: #717a8c; font-size: 11px; margin-top: 8px; font-style: italic; }
.image-wrap { padding: 12px; }
.poster-box svg { width: 100%; height: auto; border-radius: 10px; display: block; }

/* 输入 */
.agent-input-wrap { padding: 12px 16px 14px; border-top: 1px solid rgba(255,255,255,0.07); }
.agent-input-box {
  display: flex; align-items: flex-end; gap: 10px;
  background: rgba(10,12,22,0.7); border: 1px solid rgba(255,255,255,0.1);
  border-radius: 16px; padding: 8px; transition: border .2s;
}
.agent-input-box:focus-within { border-color: rgba(168,181,147,0.45); }
.agent-textarea {
  flex: 1; background: transparent; border: none; outline: none; color: #e6eaf1;
  font-size: 13.5px; padding: 7px 8px; resize: none; max-height: 160px; line-height: 1.5;
}
.agent-textarea::placeholder { color: #5b6475; }
.agent-send {
  width: 38px; height: 38px; border-radius: 12px; flex-shrink: 0;
  background: linear-gradient(135deg,#a8b593,#6b7d57); color: #14160f; font-size: 15px;
  display: flex; align-items: center; justify-content: center; cursor: pointer; transition: all .2s; border: none;
}
.agent-send:hover:not(:disabled) {
  filter: brightness(1.1); transform: translateY(-1px); box-shadow: 0 6px 18px rgba(168,181,147,0.3);
}
.agent-send:disabled { opacity: 0.4; cursor: not-allowed; }
.send-spinner {
  width: 15px; height: 15px; border-radius: 50%;
  border: 2px solid rgba(20,22,15,0.3); border-top-color: #14160f; animation: spin .7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.agent-input-hint { text-align: center; font-size: 10px; color: #4c5566; margin-top: 8px; }
</style>