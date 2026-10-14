<template>
  <Teleport to="body">
    <transition name="pc-fade">
      <div v-if="open" class="pc-overlay" @click.self="close">
        <div class="pc-panel">
          <!-- 顶栏 -->
          <header class="pc-top">
            <button class="pc-back" @click="close" title="返回">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M15 19l-7-7 7-7"/></svg>
            </button>
            <div class="pc-peer-avatar">
              <img v-if="peer?.avatar" :src="peer.avatar" referrerpolicy="no-referrer" />
              <span v-else>{{ (peer?.nickname || peer?.username || '?')[0]?.toUpperCase() }}</span>
            </div>
            <div class="pc-peer-meta">
              <span class="pc-peer-name">{{ peer?.nickname || peer?.username || '私聊' }}</span>
              <span :class="['pc-ws-pill', wsConnected ? 'on' : 'off']">
                <i></i>{{ wsConnected ? '在线' : '连接中' }}
              </span>
            </div>
            <button class="pc-icon" @click="inviteToGroup" title="邀请进群聊">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M16 21v-2a4 4 0 00-4-4H6a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/>
                <path d="M19 8v6M22 11h-6"/>
              </svg>
            </button>
          </header>

          <!-- 消息区 -->
          <section ref="msgsRef" class="pc-msgs">
            <div v-if="loading" class="pc-hint">正在加载…</div>
            <template v-else>
              <div v-for="msg in messages" :key="msg.id" class="pc-msg-wrap">
                <div v-if="msg.msgType === 'system'" class="pc-sys">{{ msg.content }}</div>
                <div v-else :class="['pc-row', { mine: msg.userId === myUserId }]">
                  <div class="pc-avatar" @click="onPeerAvatarClick(msg)">
                    <img v-if="avatarOf(msg)" :src="avatarOf(msg)" referrerpolicy="no-referrer" />
                    <span v-else>{{ (msg.nickname || msg.username || '?')[0]?.toUpperCase() }}</span>
                  </div>
                  <div class="pc-col">
                    <div class="pc-meta"><span>{{ msg.nickname || msg.username }}</span><time>{{ fmtTime(msg.createdAt) }}</time></div>
                    <div v-if="msg.quotePreview" class="pc-quote">{{ msg.quotePreview }}</div>
                    <img v-if="msg.msgType === 'image'" class="pc-img" :src="msg.content"
                         referrerpolicy="no-referrer" @click="lightbox = msg.content" />
                    <div v-else-if="msg.msgType === 'emoji'" class="pc-emoji">{{ msg.content }}</div>
                    <div v-else class="pc-text">{{ msg.content }}</div>
                    <button class="pc-action" @click="startQuote(msg)">引用</button>
                  </div>
                </div>
              </div>
              <div class="pc-sys">{{ wsConnected ? '· 已连接 ·' : '· 连接中 ·' }}</div>
            </template>
          </section>

          <!-- 输入区 -->
          <footer v-if="canChat" class="pc-input">
            <div v-if="quoteMsg" class="pc-quote-preview">
              <span class="pc-qp-label">回复 {{ quoteMsg.nickname || quoteMsg.username }}：</span>
              <span class="pc-qp-content">{{ quotePreviewText }}</span>
              <button class="pc-qp-x" @click="clearQuote">✕</button>
            </div>
            <div class="pc-input-row">
              <button class="pc-icon" @click="showEmoji = !showEmoji" title="表情">😊</button>
              <button class="pc-icon" @click="triggerUpload" title="图片">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="3"/><circle cx="9" cy="9" r="2"/><path d="M21 15l-5-5L5 21"/></svg>
              </button>
              <input ref="fileRef" type="file" accept="image/*" hidden @change="onFile" />
              <textarea ref="inputRef" v-model="draft" class="pc-field"
                        :placeholder="quoteMsg ? `引用回复 ${quoteMsg.nickname || quoteMsg.username}…` : '说点什么…  Enter 发送'"
                        rows="1" maxlength="2000"
                        @keydown.enter.exact.prevent="send" @input="autoResize"></textarea>
              <button class="pc-send" :disabled="!draft.trim()" @click="send">
                <svg viewBox="0 0 24 24" fill="currentColor"><path d="M3 11l18-8-8 18-2-8-8-2z"/></svg>
              </button>
            </div>
            <div v-if="showEmoji" class="pc-emoji-panel">
              <span v-for="e in EMOJIS" :key="e" @click="pickEmoji(e)">{{ e }}</span>
            </div>
          </footer>
          <footer v-else class="pc-readonly">
            <span>🔒 完成实名认证后即可私聊发言 · <router-link to="/profile" class="pc-link">前往认证</router-link></span>
          </footer>

          <!-- 灯箱 -->
          <Teleport to="body">
            <div v-if="lightbox" class="pc-lightbox" @click="lightbox = ''">
              <img :src="lightbox" referrerpolicy="no-referrer" />
            </div>
          </Teleport>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
// 私聊弹窗：与群聊共用 chat_message 表与 WS 端点（/chat/ws/{roomId}?token=xxx）
// 独立 WS 连接 + 独立消息流，不干扰群聊 store 全局常驻连接
import { ref, computed, watch, nextTick, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/modules/auth'
import { useToastStore } from '@/stores/modules/toast'
import { useChatStore } from '@/stores/modules/chat'
import { createPrivateChat, getChatMessages, uploadChatImage } from '@/api/chat'

const router = useRouter()

const props = defineProps({
  open: { type: Boolean, default: false },
  target: { type: Object, default: null },  // { userId, username, nickname, avatar }
})
const emit = defineEmits(['update:open', 'invite'])

const auth = useAuthStore()
const toast = useToastStore()
const chat = useChatStore()

const myUserId = computed(() => auth.user?.id)
const canChat = computed(() => {
  const u = auth.user
  if (!u) return false
  if (u.role === 'ADMIN') return true
  return u.verified === 2
})

const peer = ref(null)        // 对方资料
const roomId = ref(null)
const messages = ref([])
const wsConnected = ref(false)
const loading = ref(true)
const draft = ref('')
const showEmoji = ref(false)
const quoteMsg = ref(null)
const lightbox = ref('')
const inputRef = ref(null)
const msgsRef = ref(null)
const fileRef = ref(null)

let ws = null
const EMOJIS = ['😀','😂','🤣','😊','😍','😘','😎','🤔','😭','😤','👍','👎','👏','🙏','💪','🤝','❤️','💔','💯','🔥','✨','🎉','🌈','⚡','🌙','☀️','🍀','🌸','🐱','🐶','🍉','☕']

const quotePreviewText = computed(() => {
  if (!quoteMsg.value) return ''
  const c = quoteMsg.value.content || ''
  return c.length > 50 ? c.slice(0, 50) + '…' : c
})

function close() { emit('update:open', false) }

function avatarOf(msg) {
  if (msg.userId === myUserId.value) return auth.user?.avatar || msg.avatar || null
  return peer.value?.avatar || msg.avatar || null
}
function fmtTime(t) {
  if (!t) return ''
  const d = new Date(t)
  if (isNaN(d)) return ''
  return `${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

function onPeerAvatarClick(msg) {
  // 私聊里点击对方头像无操作（已在私聊中），点击自己头像可跳个人中心
  if (msg.userId === myUserId.value) return
}

// 打开弹窗时初始化私聊房间
watch(() => props.open, async (v) => {
  if (!v || !props.target) return
  loading.value = true
  messages.value = []
  peer.value = props.target
  try {
    const res = await createPrivateChat(props.target.userId)
    const data = res?.data ?? res
    roomId.value = data.id
    if (data.peer) peer.value = data.peer
    // 加载历史消息
    try {
      const hisRes = await getChatMessages(roomId.value, 100)
      messages.value = hisRes?.data ?? hisRes ?? []
    } catch {}
    connect()
  } catch (e) {
    toast.error(e?.message || '私聊创建失败')
    loading.value = false
  } finally {
    loading.value = false
  }
  nextTick(() => inputRef.value?.focus())
}, { immediate: true })

function connect() {
  if (!roomId.value || !auth.token) return
  disconnect()
  const proto = location.protocol === 'https:' ? 'wss' : 'ws'
  const url = `${proto}://${location.host}/chat/ws/${roomId.value}?token=${encodeURIComponent(auth.token)}`
  try {
    ws = new WebSocket(url)
    ws.onopen = () => { wsConnected.value = true }
    ws.onmessage = (e) => {
      let data
      try { data = JSON.parse(e.data) } catch { return }
      if (data.type === 'presence') return
      if (data.type === 'error') { toast.warning(data.content || '操作被拒绝'); return }
      messages.value.push(data)
      if (messages.value.length > 300) messages.value.splice(0, messages.value.length - 300)
      nextTick(() => scrollBottom())
    }
    ws.onclose = () => { wsConnected.value = false }
    ws.onerror = () => { wsConnected.value = false }
  } catch { wsConnected.value = false }
}

function disconnect() {
  if (ws) { try { ws.onclose = null; ws.close() } catch {} ws = null }
  wsConnected.value = false
}

function send() {
  const text = draft.value.trim()
  if (!text || !ws || ws.readyState !== WebSocket.OPEN) return
  if (!canChat.value) { toast.warning('请先完成实名认证'); return }
  const payload = { type: 'text', content: text }
  if (quoteMsg.value?.id) payload.quoteId = quoteMsg.value.id
  ws.send(JSON.stringify(payload))
  draft.value = ''
  clearQuote()
  nextTick(() => autoResize())
}

function pickEmoji(e) {
  if (!ws || ws.readyState !== WebSocket.OPEN) return
  if (!canChat.value) { toast.warning('请先完成实名认证'); return }
  ws.send(JSON.stringify({ type: 'emoji', content: e }))
  showEmoji.value = false
}

function triggerUpload() { fileRef.value?.click() }
async function onFile(ev) {
  const file = ev.target.files?.[0]
  ev.target.value = ''
  if (!file) return
  try {
    const res = await uploadChatImage(file)
    const url = res?.url ?? res?.data?.url
    if (url && ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'image', content: url }))
    }
  } catch (e) { toast.error('上传失败') }
}

function startQuote(msg) { quoteMsg.value = msg; nextTick(() => inputRef.value?.focus()) }
function clearQuote() { quoteMsg.value = null }

function autoResize() {
  const el = inputRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 96) + 'px'
}

function scrollBottom() {
  nextTick(() => {
    if (msgsRef.value) msgsRef.value.scrollTop = msgsRef.value.scrollHeight
  })
}

function inviteToGroup() {
  // 「拉去群聊」：固定公开群所有注册用户都已自动入群，
  // 这里关闭私聊弹窗并跳转到极光广场群聊页，与对方在群内继续交流
  emit('invite', peer.value)
  close()
  toast.success(`已为你打开极光广场，与 ${peer.value?.nickname || peer.value?.username || '对方'} 在群聊中继续交流`)
  router.push('/community/chat')
}

onBeforeUnmount(() => disconnect())
</script>

<style scoped>
.pc-overlay {
  position: fixed; inset: 0; z-index: 150;
  display: flex; align-items: center; justify-content: center;
  background: rgba(0,0,0,.55); backdrop-filter: blur(6px);
}
.pc-panel {
  width: min(420px, 92vw); height: min(640px, 88vh);
  display: flex; flex-direction: column;
  background: linear-gradient(160deg, rgba(8,10,28,.98), rgba(14,12,38,.98));
  border: 1px solid rgba(255,255,255,.1); border-radius: 18px;
  overflow: hidden; box-shadow: 0 20px 60px rgba(0,0,0,.5);
  color: #e6e8f5;
}
.pc-top {
  display: flex; align-items: center; gap: 10px; padding: 12px 14px;
  border-bottom: 1px solid rgba(255,255,255,.08);
  background: rgba(10,12,30,.7); flex-shrink: 0;
}
.pc-back, .pc-icon {
  width: 32px; height: 32px; border-radius: 9px; border: none; cursor: pointer;
  background: transparent; color: rgba(255,255,255,.65);
  display: flex; align-items: center; justify-content: center; transition: .2s;
}
.pc-back svg, .pc-icon svg { width: 18px; height: 18px; }
.pc-back:hover, .pc-icon:hover { background: rgba(255,255,255,.1); color: #fff; }
.pc-peer-avatar {
  width: 36px; height: 36px; border-radius: 50%; overflow: hidden; flex-shrink: 0;
  background: linear-gradient(135deg, #667eea, #764ba2);
  display: flex; align-items: center; justify-content: center; color: #fff; font-weight: 700;
}
.pc-peer-avatar img { width: 100%; height: 100%; object-fit: cover; }
.pc-peer-meta { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.pc-peer-name { font-weight: 700; font-size: .92rem; color: #fff; }
.pc-ws-pill { display: inline-flex; align-items: center; gap: 5px; font-size: .68rem; color: rgba(255,255,255,.5); }
.pc-ws-pill i { width: 7px; height: 7px; border-radius: 50%; background: #6b7280; }
.pc-ws-pill.on i { background: #34d399; box-shadow: 0 0 6px rgba(52,211,153,.7); }

.pc-msgs { flex: 1; overflow-y: auto; padding: 16px 14px; display: flex; flex-direction: column; gap: 12px; min-height: 0; }
.pc-msgs::-webkit-scrollbar { width: 5px; }
.pc-msgs::-webkit-scrollbar-thumb { background: rgba(255,255,255,.15); border-radius: 3px; }
.pc-hint { text-align: center; color: rgba(255,255,255,.4); font-size: .82rem; padding: 20px; }
.pc-sys { text-align: center; color: rgba(255,255,255,.35); font-size: .72rem; padding: 2px; }
.pc-msg-wrap { display: flex; flex-direction: column; min-width: 0; }
.pc-row { display: flex; gap: 9px; max-width: 80%; }
.pc-row.mine { align-self: flex-end; flex-direction: row-reverse; }
.pc-avatar {
  width: 32px; height: 32px; border-radius: 50%; flex-shrink: 0; overflow: hidden;
  background: linear-gradient(135deg, #667eea, #764ba2);
  display: flex; align-items: center; justify-content: center; color: #fff; font-weight: 700; font-size: .82rem;
}
.pc-row.mine .pc-avatar { background: linear-gradient(135deg, #3b82f6, #1d4ed8); }
.pc-avatar img { width: 100%; height: 100%; object-fit: cover; }
.pc-col { min-width: 0; }
.pc-row.mine .pc-col { text-align: right; }
.pc-meta { display: flex; gap: 7px; margin-bottom: 3px; align-items: center; }
.pc-row.mine .pc-meta { flex-direction: row-reverse; }
.pc-meta span { font-size: .68rem; color: rgba(255,255,255,.55); }
.pc-meta time { font-size: .62rem; color: rgba(255,255,255,.3); }
.pc-text {
  display: inline-block; background: rgba(255,255,255,.07); padding: 8px 12px; border-radius: 14px;
  border-top-left-radius: 4px; color: #fff; font-size: .85rem; word-break: break-word; text-align: left; line-height: 1.55;
  box-shadow: 0 1px 3px rgba(0,0,0,.12);
}
.pc-row.mine .pc-text {
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
  border: 1px solid rgba(59,130,246,.5); border-top-left-radius: 14px; border-top-right-radius: 4px;
  box-shadow: 0 2px 8px rgba(59,130,246,.22);
}
.pc-img { max-width: 200px; max-height: 220px; border-radius: 10px; cursor: zoom-in; border: 1px solid rgba(255,255,255,.1); }
.pc-emoji { font-size: 2rem; line-height: 1.2; }
.pc-quote {
  display: inline-block; background: rgba(255,255,255,.06); border-left: 2px solid rgba(34,211,238,.5);
  padding: 4px 9px; border-radius: 6px; font-size: .72rem; color: rgba(255,255,255,.6);
  margin-bottom: 4px; max-width: 100%; line-height: 1.5;
}
.pc-row.mine .pc-quote { border-left-color: rgba(59,130,246,.7); }
.pc-action {
  margin-top: 3px; border: none; background: transparent; color: rgba(255,255,255,.35);
  font-size: .64rem; cursor: pointer; padding: 2px 6px; border-radius: 6px; opacity: 0; transition: .15s;
}
.pc-row:hover .pc-action { opacity: 1; }
.pc-action:hover { color: #67e8f9; background: rgba(34,211,238,.1); }
.pc-row.mine .pc-action { align-self: flex-end; }

.pc-input { flex-shrink: 0; border-top: 1px solid rgba(255,255,255,.08); background: rgba(10,12,30,.7); }
.pc-input-row { display: flex; align-items: flex-end; gap: 6px; padding: 10px; }
.pc-field {
  flex: 1; min-width: 0; background: rgba(0,0,0,.3); border: 1px solid rgba(255,255,255,.12);
  border-radius: 10px; padding: 8px 11px; color: #fff; font-size: .85rem; outline: none; font-family: inherit;
  resize: none; line-height: 1.5; max-height: 96px; overflow-y: auto;
}
.pc-field:focus { border-color: rgba(59,130,246,.5); box-shadow: 0 0 0 2px rgba(59,130,246,.1); }
.pc-send {
  width: 34px; height: 34px; border-radius: 10px; border: none; cursor: pointer; flex-shrink: 0;
  background: linear-gradient(135deg, #3b82f6, #1d4ed8); color: #fff;
  display: flex; align-items: center; justify-content: center; transition: .2s;
}
.pc-send svg { width: 15px; height: 15px; }
.pc-send:hover:not(:disabled) { filter: brightness(1.15); }
.pc-send:disabled { opacity: .35; cursor: not-allowed; }
.pc-emoji-panel {
  display: flex; flex-wrap: wrap; gap: 2px; padding: 8px 10px;
  border-top: 1px solid rgba(255,255,255,.08); background: rgba(10,12,30,.6); max-height: 132px; overflow-y: auto;
}
.pc-emoji-panel span { font-size: 1.25rem; padding: 4px; border-radius: 7px; cursor: pointer; transition: .15s; }
.pc-emoji-panel span:hover { background: rgba(255,255,255,.12); transform: scale(1.15); }

.pc-quote-preview {
  display: flex; align-items: center; gap: 8px; padding: 6px 10px; margin: 0 10px 6px;
  background: rgba(59,130,246,.1); border: 1px solid rgba(59,130,246,.3); border-radius: 8px; font-size: .74rem;
}
.pc-qp-label { color: #93c5fd; flex-shrink: 0; }
.pc-qp-content { color: rgba(255,255,255,.7); flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.pc-qp-x { border: none; background: transparent; color: rgba(255,255,255,.5); cursor: pointer; font-size: .85rem; }
.pc-qp-x:hover { color: #f87171; }

.pc-readonly { padding: 14px; text-align: center; border-top: 1px solid rgba(255,255,255,.08); font-size: .78rem; color: rgba(255,255,255,.55); }
.pc-link { color: #67e8f9; text-decoration: underline; }

.pc-lightbox {
  position: fixed; inset: 0; z-index: 3200; background: rgba(0,0,0,.88);
  display: flex; align-items: center; justify-content: center; cursor: zoom-out;
}
.pc-lightbox img { max-width: 92vw; max-height: 92vh; border-radius: 10px; }

.pc-fade-enter-active, .pc-fade-leave-active { transition: opacity .22s ease; }
.pc-fade-enter-from, .pc-fade-leave-to { opacity: 0; }
</style>
