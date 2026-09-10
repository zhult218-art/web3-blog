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
          <span class="route-badge" :class="lastAgent ? 'route-on' : ''">
            <span class="w-1.5 h-1.5 rounded-full" :class="lastAgent ? 'bg-emerald-400 animate-pulse' : 'bg-slate-600'"></span>
            {{ lastAgent ? `路由 → ${agentLabel(lastAgent)}` : '待机' }}
          </span>
          <button v-if="messages.length" @click="clearAll" class="agent-icon-btn" title="清空会话">🗑</button>
        </div>
      </header>

      <!-- ═══ 消息 / 欢迎 ═══ -->
      <div ref="msgBox" class="agent-body">
        <div v-if="messages.length === 0" class="agent-empty">
          <div class="empty-orb"><span></span></div>
          <p class="empty-title">你好，我是你的 Web3 门户 Agent</p>
          <p class="empty-desc">我会按 Workflow 自动路由：写作 / 音乐 / 数据 / 通用对话，还能查 Supabase 与站内服务。</p>
          <div class="empty-suggests">
            <button v-for="s in suggests" :key="s" class="suggest-chip" @click="quickAsk(s)">{{ s }}</button>
          </div>
        </div>

        <div v-else class="space-y-5">
          <div v-for="(m, i) in messages" :key="i" class="msg-row" :class="m.role">
            <div class="msg-avatar">{{ m.role === 'user' ? '你' : 'A' }}</div>
            <div class="msg-main">
              <div v-if="m.agent && m.agent !== 'general'" class="msg-agent-tag">{{ agentLabel(m.agent) }}</div>
              <div class="msg-bubble">
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
            class="agent-textarea" placeholder="给 Agent 下指令，例如：帮我写一篇关于 Supabase 的文章… 或 推荐几首民谣…"
            :disabled="busy"></textarea>
          <button class="agent-send" :disabled="busy || !input.trim()" @click="send">
            <span v-if="busy" class="send-spinner"></span>
            <span v-else>➤</span>
          </button>
        </div>
        <p class="agent-input-hint">Enter 发送 · Shift+Enter 换行 · Agent 由规则路由自动分发</p>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, watch } from 'vue'
import { agentChatStream, consumeAgentStream } from '@/api/agent'

const messages = ref([])
const input = ref('')
const busy = ref(false)
const lastAgent = ref('')
const inputArea = ref(null)
const msgBox = ref(null)

const suggests = [
  '帮我写一篇关于 Supabase 接入的博客文章',
  '推荐几首适合夜晚的民谣',
  '查一下站内有哪些示例数据和服务',
  '用三句话介绍这个 Web3 门户',
]

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
}

function onKey(e) {
  if (e.key === 'Enter' && !e.shiftKey && !busy.value) {
    e.preventDefault()
    send()
  }
}

async function send() {
  const text = input.value.trim()
  if (!text || busy.value) return
  input.value = ''
  resetTextarea()

  const userMsg = { role: 'user', content: text }
  messages.value.push(userMsg)
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
    })
  } catch (e) {
    if (!assistantMsg.cancelled) {
      assistantMsg.raw = (assistantMsg.raw ? assistantMsg.raw + '\n\n' : '') + '⚠️ ' + (e?.message || '请求失败，请稍后重试')
      assistantMsg.content = assistantMsg.raw
    }
    assistantMsg.streaming = false
  } finally {
    busy.value = false
    scrollBottom()
  }
}

// 简单前端路由提示（与后端规则一致，用于展示路由到哪个 Agent）
watch(messages, (list) => {
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

onMounted(() => { resetTextarea() })
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
  height: min(72vh, 640px);
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

.agent-body { flex: 1; overflow-y: auto; padding: 20px; scrollbar-width: thin; scrollbar-color: rgba(159,176,140,0.3) transparent; }
.agent-body::-webkit-scrollbar { width: 5px; }
.agent-body::-webkit-scrollbar-thumb { background: rgba(159,176,140,0.3); border-radius: 999px; }

/* 空态 */
.agent-empty { height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; padding: 20px; }
.empty-orb {
  width: 72px; height: 72px; border-radius: 50%;
  background: radial-gradient(circle at 30% 25%, #e5ecd9 0%, #a8b593 45%, #3f4d33 100%);
  box-shadow: 0 0 34px rgba(168,181,147,0.35); display: flex; align-items: center; justify-content: center; margin-bottom: 18px;
}
.empty-orb span { width: 26px; height: 26px; border-radius: 50%; background: radial-gradient(circle, #fff, #a8b593 70%); }
.empty-title { color: #e9edf5; font-size: 16px; font-weight: 600; }
.empty-desc { color: #7c8494; font-size: 12.5px; margin-top: 8px; max-width: 460px; line-height: 1.7; }
.empty-suggests { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; margin-top: 22px; max-width: 520px; }
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
.msg-main { max-width: 80%; display: flex; flex-direction: column; }
.msg-row.user .msg-main { align-items: flex-end; }
.msg-agent-tag { font-size: 10px; color: #9cc98d; margin-bottom: 4px; }
.msg-bubble {
  padding: 10px 14px; border-radius: 14px; font-size: 13.5px; line-height: 1.65; color: #e6eaf1;
}
.msg-row.assistant .msg-bubble { background: rgba(255,255,255,0.045); border: 1px solid rgba(255,255,255,0.07); border-top-left-radius: 4px; }
.msg-row.user .msg-bubble { background: linear-gradient(135deg, rgba(103,232,249,0.16), rgba(124,58,237,0.16)); border: 1px solid rgba(103,232,249,0.2); border-top-right-radius: 4px; }
.msg-spinner { color: #a8b593; animation: blink 1s infinite; }
@keyframes blink { 50% { opacity: 0.2; } }
.msg-actions { margin-top: 6px; }
.mini-btn { font-size: 11px; padding: 4px 10px; border-radius: 8px; border: 1px solid rgba(255,255,255,0.1); background: rgba(255,255,255,0.04); color: #8b93a8; cursor: pointer; }
.mini-btn.danger:hover { color: #fca5a5; border-color: rgba(252,165,165,0.3); }

/* 输入 */
.agent-input-wrap { padding: 12px 16px 14px; border-top: 1px solid rgba(255,255,255,0.07); }
.agent-input-box { display: flex; align-items: flex-end; gap: 10px; background: rgba(10,12,22,0.7); border: 1px solid rgba(255,255,255,0.1); border-radius: 16px; padding: 8px; transition: border .2s; }
.agent-input-box:focus-within { border-color: rgba(168,181,147,0.45); }
.agent-textarea { flex: 1; background: transparent; border: none; outline: none; color: #e6eaf1; font-size: 13.5px; padding: 7px 8px; resize: none; max-height: 160px; line-height: 1.5; }
.agent-textarea::placeholder { color: #5b6475; }
.agent-send {
  width: 38px; height: 38px; border-radius: 12px; flex-shrink: 0;
  background: linear-gradient(135deg,#a8b593,#6b7d57); color: #14160f; font-size: 15px;
  display: flex; align-items: center; justify-content: center; cursor: pointer; transition: all .2s; border: none;
}
.agent-send:hover:not(:disabled) { filter: brightness(1.1); transform: translateY(-1px); box-shadow: 0 6px 18px rgba(168,181,147,0.3); }
.agent-send:disabled { opacity: 0.4; cursor: not-allowed; }
.send-spinner { width: 15px; height: 15px; border-radius: 50%; border: 2px solid rgba(20,22,15,0.3); border-top-color: #14160f; animation: spin .7s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.agent-input-hint { text-align: center; font-size: 10px; color: #4c5566; margin-top: 8px; }
</style>
