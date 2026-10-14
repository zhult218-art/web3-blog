<template>
  <!-- 关闭态：左下角悬浮入口按钮 -->
  <button v-if="chat.dockState === 'closed'" class="dock-launcher" title="打开极光广场" @click="chat.openDock()">
    <span class="launcher-emoji">{{ chat.roomEmoji }}</span>
    <span v-if="chat.unread" class="unread-dot">{{ chat.unread > 99 ? '99+' : chat.unread }}</span>
  </button>

  <!-- 窄条态：左侧 52px 竖栏 -->
  <div v-else-if="chat.dockState === 'mini'" class="dock-rail" title="展开聊天" @click="chat.openDock()">
    <span class="rail-emoji">{{ chat.roomEmoji }}</span>
    <span v-if="chat.onlineCount" class="rail-online">{{ chat.onlineCount }}</span>
    <span v-if="chat.unread" class="unread-dot">{{ chat.unread > 99 ? '99+' : chat.unread }}</span>
    <span class="rail-label">聊天</span>
  </div>

  <!-- 展开态：左侧 340px 聊天面板 -->
  <aside v-else class="dock-panel">
    <!-- 顶栏 -->
    <header class="dock-top">
      <span class="dock-room-emoji">{{ chat.roomEmoji }}</span>
      <div class="dock-room-meta">
        <span class="dock-room-name">{{ chat.room?.name || '极光广场' }}</span>
        <span class="dock-room-sub">
          <i :class="['ws-dot', chat.wsConnected ? 'on' : 'off']"></i>
          {{ chat.wsConnected ? `在线 ${chat.onlineCount}/${chat.members.length}` : '连接中…' }}
        </span>
      </div>
      <button class="dock-ic" title="完整模式" @click="goFull">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M8 3H5a2 2 0 00-2 2v3m18 0V5a2 2 0 00-2-2h-3m0 18h3a2 2 0 002-2v-3M3 16v3a2 2 0 002 2h3"/></svg>
      </button>
      <button class="dock-ic" title="最小化" @click="chat.miniDock()">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12h14"/></svg>
      </button>
      <button class="dock-ic" title="关闭" @click="chat.closeDock()">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
      </button>
    </header>

    <!-- 消息 / 成员 切换 -->
    <div class="dock-tabs">
      <button :class="{ on: tab === 'msg' }" @click="tab = 'msg'">消息</button>
      <button :class="{ on: tab === 'mem' }" @click="tab = 'mem'">成员 · {{ chat.onlineCount }} 在线</button>
    </div>

    <!-- ===== 消息面板 ===== -->
    <template v-if="tab === 'msg'">
      <section ref="msgAreaRef" class="dock-msgs" @scroll="onMsgScroll">
        <div v-if="chat.loading" class="dock-hint">正在加载…</div>
        <div v-else-if="chat.loadError" class="dock-hint">
          {{ chat.loadError }}
          <button class="dock-retry" @click="retry">重试</button>
        </div>
        <template v-else>
          <template v-for="(msg, idx) in chat.messages" :key="msg.id">
            <!-- 时间分组分隔线（间隔>5分钟或首条） -->
            <div v-if="showTimeDivider(idx)" class="m-time-div">
              <span>{{ fmtGroupTime(msg.createdAt) }}</span>
            </div>
            <div class="m-wrap">
              <div v-if="msg.msgType === 'system'" class="m-sys">{{ msg.content }}</div>
              <div v-else :class="['m-row', { mine: msg.userId === chat.myUserId, 'same-sender': isSameSender(idx) }]">
                <span class="m-avatar" v-if="!isSameSender(idx)">
                  <img v-if="chat.avatarOf(msg)" :src="chat.avatarOf(msg)" referrerpolicy="no-referrer" loading="lazy" />
                  <i v-else>{{ charOf(msg) }}</i>
                </span>
                <span v-else class="m-avatar-spacer"></span>
                <div class="m-col">
                  <div class="m-meta" v-if="!isSameSender(idx)"><span>{{ chat.nameOf(msg) }}</span><time>{{ fmtTime(msg.createdAt) }}</time></div>
                  <img v-if="msg.msgType === 'image'" class="m-img" :src="msg.content" loading="lazy"
                       referrerpolicy="no-referrer" @click="lightbox = msg.content" />
                  <div v-else-if="msg.msgType === 'emoji'" class="m-emoji">{{ msg.content }}</div>
                  <div v-else class="m-text">{{ msg.content }}</div>
                </div>
              </div>
            </div>
          </template>
          <div class="m-sys">{{ chat.wsConnected ? '· 已连接 ·' : '· 连接中 ·' }}</div>
        </template>
        <!-- 新消息浮动提示 -->
        <transition name="fade-up">
          <button v-if="showNewMsgBtn" class="new-msg-btn" @click="scrollBottom(true)">
            <span>↓ 新消息</span>
          </button>
        </transition>
      </section>

      <!-- 表情面板 -->
      <div v-if="showEmoji" class="dock-emoji">
        <span v-for="e in EMOJIS" :key="e" @click="pickEmoji(e)">{{ e }}</span>
      </div>

      <!-- 输入区 -->
      <footer v-if="chat.canChat" class="dock-input">
        <button class="dock-ic" :class="{ active: showEmoji }" title="表情" @click="showEmoji = !showEmoji">😊</button>
        <button class="dock-ic" title="发图片" @click="fileRef?.click()">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="3"/><circle cx="9" cy="9" r="2"/><path d="M21 15l-5-5L5 21"/></svg>
        </button>
        <input ref="fileRef" type="file" accept="image/*" hidden @change="onFile" />
        <textarea ref="draftRef" v-model="draft" class="dock-field" placeholder="说点什么…  Enter 发送"
               rows="1" maxlength="2000"
               @keydown.enter.exact.prevent="doSend"
               @input="autoResize"
               @focus="chat.unread = 0"></textarea>
        <button class="dock-send" :disabled="!draft.trim()" @click="doSend">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M3 11l18-8-8 18-2-8-8-2z"/></svg>
        </button>
      </footer>
      <!-- 未实名认证：只读提示 -->
      <footer v-else class="dock-input dock-readonly">
        <span class="dock-locked">🔒 完成实名认证后即可发言 · <router-link to="/profile" class="dock-locked-link">前往认证</router-link></span>
      </footer>
    </template>

    <!-- ===== 成员面板 ===== -->
    <section v-else class="dock-members">
      <div v-if="!chat.members.length" class="dock-hint">暂无成员</div>
      <template v-else>
        <!-- 在线成员 -->
        <div class="mem-group-h" v-if="onlineMembers.length">
          <span class="mem-group-dot on"></span>在线 · {{ onlineMembers.length }}
        </div>
        <div v-for="m in onlineMembers" :key="'on'+m.userId" class="mem-row">
          <span class="mem-avatar">
            <img v-if="m.avatar" :src="m.avatar" referrerpolicy="no-referrer" loading="lazy" />
            <i v-else>{{ (m.nickname || m.username || '?').charAt(0).toUpperCase() }}</i>
            <b class="mem-dot on"></b>
          </span>
          <span class="mem-name">{{ m.nickname || m.username }}</span>
          <span v-if="m.role === 'admin' || m.role === 'owner'" class="mem-tag">{{ m.role === 'owner' ? '群主' : '管理' }}</span>
        </div>
        <!-- 离线成员 -->
        <div class="mem-group-h" v-if="offlineMembers.length">
          <span class="mem-group-dot off"></span>离线 · {{ offlineMembers.length }}
        </div>
        <div v-for="m in offlineMembers" :key="'off'+m.userId" class="mem-row off">
          <span class="mem-avatar">
            <img v-if="m.avatar" :src="m.avatar" referrerpolicy="no-referrer" loading="lazy" />
            <i v-else>{{ (m.nickname || m.username || '?').charAt(0).toUpperCase() }}</i>
            <b class="mem-dot off"></b>
          </span>
          <span class="mem-name">{{ m.nickname || m.username }}</span>
          <span v-if="m.role === 'admin' || m.role === 'owner'" class="mem-tag">{{ m.role === 'owner' ? '群主' : '管理' }}</span>
        </div>
      </template>
    </section>

    <!-- 图片灯箱 -->
    <Teleport to="body">
      <div v-if="lightbox" class="dock-lightbox" @click="lightbox = ''">
        <img :src="lightbox" referrerpolicy="no-referrer" />
      </div>
    </Teleport>
  </aside>
</template>

<script setup>
// 极光广场 · 左侧聊天 Dock（三态：open 展开 / mini 窄条 / closed 关闭）
// 数据与 WebSocket 全部由全局 chat store 驱动，路由切换不断线
import { ref, computed, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useChatStore } from '@/stores/modules/chat'

const chat = useChatStore()
const router = useRouter()

const tab = ref('msg')           // msg=消息 / mem=成员
const draft = ref('')
const showEmoji = ref(false)
const fileRef = ref(null)
const draftRef = ref(null)
const msgAreaRef = ref(null)
const lightbox = ref('')
const showNewMsgBtn = ref(false)

// 常用表情
const EMOJIS = ['😀','😂','🤣','😊','😍','😘','😎','🤔','😭','😤','🥰','😅','👍','👎','👏','🙏','💪','🤝','❤️','💔','💯','🔥','✨','🎉','🎊','🌈','⚡','🌙','☀️','🍀','🌸','🐱','🐶','🐼','🦊','🍉','🍔','🍜','☕','🍺']

// 成员在线/离线分组
const onlineMembers = computed(() => chat.members.filter(m => m.online))
const offlineMembers = computed(() => chat.members.filter(m => !m.online))

function charOf(msg) { return (msg.nickname || msg.username || '?').charAt(0).toUpperCase() }
function fmtTime(t) {
  if (!t) return ''
  const d = new Date(t)
  if (isNaN(d)) return ''
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}
function fmtGroupTime(t) {
  if (!t) return ''
  const d = new Date(t)
  if (isNaN(d)) return ''
  const now = new Date()
  const sameDay = d.toDateString() === now.toDateString()
  const yest = new Date(now); yest.setDate(yest.getDate() - 1)
  const isYest = d.toDateString() === yest.toDateString()
  const hm = `${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
  if (sameDay) return hm
  if (isYest) return `昨天 ${hm}`
  return `${d.getMonth()+1}月${d.getDate()}日 ${hm}`
}

// 时间分组：与上一条间隔>5分钟 或 首条
function showTimeDivider(idx) {
  if (idx === 0) return true
  const msgs = chat.messages
  const cur = new Date(msgs[idx].createdAt).getTime()
  const prev = new Date(msgs[idx - 1].createdAt).getTime()
  if (isNaN(cur) || isNaN(prev)) return true
  return (cur - prev) > 5 * 60 * 1000
}
// 同一发件人连续消息（合并头像和昵称）
function isSameSender(idx) {
  if (idx === 0) return false
  return chat.messages[idx].userId === chat.messages[idx - 1].userId && !showTimeDivider(idx)
}

function goFull() { router.push('/community/chat') }
function retry() { chat.init() }

function doSend() {
  const text = draft.value.trim()
  if (!text) return
  if (chat.sendText(text)) {
    draft.value = ''
    nextTick(() => autoResize())
  }
}
function pickEmoji(e) { chat.sendEmoji(e); showEmoji.value = false }
async function onFile(ev) {
  const file = ev.target.files?.[0]
  ev.target.value = ''
  if (file) await chat.sendImage(file)
}

// textarea 自适应高度（1~4行）
function autoResize() {
  const el = draftRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 96) + 'px'
}

// 滚动逻辑：消息区滚动检测 → 控制"新消息"按钮
function onMsgScroll() {
  const el = msgAreaRef.value
  if (!el) return
  const nearBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 60
  if (nearBottom) { showNewMsgBtn.value = false; chat.unread = 0 }
}

function scrollBottom(force = false) {
  nextTick(() => {
    if (!msgAreaRef.value) return
    msgAreaRef.value.scrollTop = msgAreaRef.value.scrollHeight
    showNewMsgBtn.value = false
  })
  if (force) chat.unread = 0
}

// 新消息到达：如果在底部 → 自动滚；不在底部 → 显示"新消息"按钮
watch(() => chat.messages.length, () => {
  if (chat.dockState !== 'open' || tab.value !== 'msg') return
  const el = msgAreaRef.value
  if (!el) { scrollBottom(); return }
  const nearBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 100
  if (nearBottom) {
    scrollBottom()
  } else {
    showNewMsgBtn.value = true
  }
})
watch(() => chat.dockState, (s) => { if (s === 'open') scrollBottom() })
watch(() => tab.value, (t) => { if (t === 'msg') scrollBottom() })
</script>

<style scoped>
/* ========== 悬浮入口（关闭态） ========== */
.dock-launcher {
  position: fixed; left: 16px; bottom: 20px; z-index: 90;
  width: 52px; height: 52px; border-radius: 16px; border: 1px solid rgba(34, 211, 238, .35);
  background: linear-gradient(135deg, rgba(10, 14, 34, .92), rgba(24, 16, 48, .92));
  backdrop-filter: blur(12px); cursor: pointer; font-size: 1.4rem;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 6px 24px rgba(0, 0, 0, .45), 0 0 18px rgba(34, 211, 238, .15);
  transition: transform .2s, box-shadow .2s;
}
.dock-launcher:hover { transform: translateY(-2px) scale(1.05); box-shadow: 0 10px 30px rgba(0, 0, 0, .5), 0 0 26px rgba(34, 211, 238, .3); }

.unread-dot {
  position: absolute; top: -6px; right: -6px; min-width: 20px; height: 20px; padding: 0 5px;
  border-radius: 10px; background: #f43f8e; color: #fff; font-size: 11px; font-weight: 700;
  display: flex; align-items: center; justify-content: center; box-shadow: 0 2px 8px rgba(244, 63, 142, .6);
}

/* ========== 窄条（mini 态） ========== */
.dock-rail {
  position: fixed; left: 0; top: 50%; transform: translateY(-50%); z-index: 90;
  width: 46px; padding: 14px 0; border-radius: 0 16px 16px 0;
  background: linear-gradient(180deg, rgba(10, 14, 34, .94), rgba(24, 16, 48, .94));
  border: 1px solid rgba(34, 211, 238, .25); border-left: none;
  backdrop-filter: blur(12px); cursor: pointer;
  display: flex; flex-direction: column; align-items: center; gap: 6px;
  box-shadow: 4px 0 20px rgba(0, 0, 0, .4);
  transition: width .2s;
}
.dock-rail:hover { width: 52px; }
.rail-emoji { font-size: 1.3rem; }
.rail-online { font-size: .62rem; color: #34d399; font-weight: 700; }
.rail-label { writing-mode: vertical-rl; font-size: .68rem; color: rgba(255, 255, 255, .5); letter-spacing: .2em; }

/* ========== 展开面板 ========== */
.dock-panel {
  position: fixed; left: 0; top: 0; bottom: 0; z-index: 90;
  width: 340px; display: flex; flex-direction: column;
  background: linear-gradient(165deg, rgba(8, 10, 28, .96), rgba(15, 12, 38, .96));
  border-right: 1px solid rgba(255, 255, 255, .09);
  backdrop-filter: blur(16px);
  box-shadow: 8px 0 32px rgba(0, 0, 0, .5);
  color: #e6e8f5;
  animation: dock-in .25s ease;
}
@keyframes dock-in { from { transform: translateX(-30px); opacity: 0; } to { transform: none; opacity: 1; } }

/* 顶栏 */
.dock-top {
  display: flex; align-items: center; gap: 10px; padding: 12px 12px 10px;
  border-bottom: 1px solid rgba(255, 255, 255, .08); flex-shrink: 0;
}
.dock-room-emoji {
  width: 36px; height: 36px; border-radius: 11px; font-size: 1.15rem; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, rgba(34, 211, 238, .2), rgba(168, 85, 247, .2));
  border: 1px solid rgba(34, 211, 238, .28);
}
.dock-room-meta { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.dock-room-name { font-weight: 700; font-size: .92rem; color: #fff; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.dock-room-sub { display: flex; align-items: center; gap: 5px; font-size: .68rem; color: rgba(255, 255, 255, .45); }
.ws-dot { width: 7px; height: 7px; border-radius: 50%; background: #6b7280; }
.ws-dot.on { background: #34d399; box-shadow: 0 0 8px rgba(52, 211, 153, .8); }

.dock-ic {
  width: 30px; height: 30px; border-radius: 8px; border: none; cursor: pointer; flex-shrink: 0;
  background: transparent; color: rgba(255, 255, 255, .6);
  display: flex; align-items: center; justify-content: center; transition: .2s; font-size: .95rem;
}
.dock-ic svg { width: 16px; height: 16px; }
.dock-ic:hover { background: rgba(255, 255, 255, .1); color: #fff; }
.dock-ic.active { background: rgba(34, 211, 238, .18); color: #67e8f9; }

/* tabs */
.dock-tabs { display: flex; border-bottom: 1px solid rgba(255, 255, 255, .08); flex-shrink: 0; }
.dock-tabs button {
  flex: 1; padding: 10px 4px; border: none; background: none; cursor: pointer;
  color: rgba(255, 255, 255, .5); font-size: .8rem; border-bottom: 2px solid transparent; transition: .2s;
}
.dock-tabs button.on { color: #67e8f9; border-bottom-color: #22d3ee; }

/* 消息区 */
.dock-msgs { flex: 1; overflow-y: auto; padding: 14px 12px; display: flex; flex-direction: column; gap: 3px; min-height: 0; position: relative; }
.dock-msgs::-webkit-scrollbar, .dock-members::-webkit-scrollbar { width: 5px; }
.dock-msgs::-webkit-scrollbar-thumb, .dock-members::-webkit-scrollbar-thumb { background: rgba(255, 255, 255, .15); border-radius: 3px; }
.dock-hint { text-align: center; color: rgba(255, 255, 255, .4); font-size: .8rem; padding: 24px 10px; }
.dock-retry { margin-top: 8px; border: none; border-radius: 8px; padding: 6px 14px; cursor: pointer; background: rgba(34, 211, 238, .2); color: #67e8f9; font-size: .78rem; }

/* 时间分组分隔线 */
.m-time-div { text-align: center; margin: 10px 0 6px; }
.m-time-div span {
  font-size: .62rem; color: rgba(255, 255, 255, .35); background: rgba(255, 255, 255, .06);
  padding: 2px 10px; border-radius: 8px;
}

.m-wrap { display: flex; flex-direction: column; min-width: 0; }
.m-sys { text-align: center; color: rgba(255, 255, 255, .32); font-size: .7rem; padding: 2px; margin: 4px 0; }
.m-row { display: flex; gap: 8px; max-width: 88%; margin-bottom: 2px; }
.m-row.same-sender { margin-bottom: 1px; }
.m-row.mine { align-self: flex-end; flex-direction: row-reverse; }
.m-avatar {
  width: 30px; height: 30px; border-radius: 50%; flex-shrink: 0; overflow: hidden;
  display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #667eea, #764ba2);
}
.m-avatar-spacer { width: 30px; flex-shrink: 0; }
.m-avatar img { width: 100%; height: 100%; object-fit: cover; }
.m-avatar i { font-style: normal; font-size: .75rem; font-weight: 700; color: #fff; }
.m-row.mine .m-avatar { background: linear-gradient(135deg, #3b82f6, #1d4ed8); }
.m-col { min-width: 0; }
.m-row.mine .m-col { text-align: right; }
.m-meta { display: flex; gap: 7px; margin-bottom: 3px; align-items: center; }
.m-row.mine .m-meta { flex-direction: row-reverse; }
.m-meta span { font-size: .68rem; color: rgba(255, 255, 255, .55); }
.m-meta time { font-size: .62rem; color: rgba(255, 255, 255, .3); }
.m-text {
  display: inline-block; background: rgba(255, 255, 255, .07); padding: 8px 11px; border-radius: 14px;
  border-top-left-radius: 4px; color: #fff; font-size: .85rem; word-break: break-word; text-align: left; line-height: 1.55;
  box-shadow: 0 1px 3px rgba(0, 0, 0, .12);
}
.m-row.same-sender .m-text { border-top-left-radius: 14px; }
.m-row.mine .m-text {
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
  border: 1px solid rgba(59, 130, 246, .5); border-top-left-radius: 14px; border-top-right-radius: 4px;
  color: #fff; box-shadow: 0 2px 8px rgba(59, 130, 246, .22);
}
.m-row.mine.same-sender .m-text { border-top-right-radius: 14px; }
.m-img { max-width: 200px; max-height: 220px; border-radius: 10px; cursor: zoom-in; border: 1px solid rgba(255, 255, 255, .1); display: inline-block; }
.m-emoji { font-size: 2rem; line-height: 1.2; }

/* 新消息浮动按钮 */
.new-msg-btn {
  position: sticky; bottom: 8px; margin: 0 auto; z-index: 10;
  border: none; border-radius: 16px; padding: 5px 14px; cursor: pointer;
  background: linear-gradient(135deg, #22d3ee, #a855f7); color: #fff;
  font-size: .72rem; font-weight: 600; box-shadow: 0 4px 14px rgba(34, 211, 238, .4);
  animation: fade-up-in .25s ease;
}
@keyframes fade-up-in { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: none; } }
.fade-up-enter-active, .fade-up-leave-active { transition: all .2s ease; }
.fade-up-enter-from, .fade-up-leave-to { opacity: 0; transform: translateY(6px); }

/* 表情面板 */
.dock-emoji {
  flex-shrink: 0; display: flex; flex-wrap: wrap; gap: 2px; padding: 8px 10px;
  border-top: 1px solid rgba(255, 255, 255, .08); background: rgba(10, 12, 30, .6);
  max-height: 132px; overflow-y: auto;
}
.dock-emoji span { font-size: 1.25rem; padding: 4px; border-radius: 7px; cursor: pointer; transition: .15s; }
.dock-emoji span:hover { background: rgba(255, 255, 255, .12); transform: scale(1.15); }

/* 输入区 */
.dock-input {
  display: flex; align-items: center; gap: 6px; padding: 10px; flex-shrink: 0;
  border-top: 1px solid rgba(255, 255, 255, .08);
}
.dock-field {
  flex: 1; min-width: 0; background: rgba(0, 0, 0, .3); border: 1px solid rgba(255, 255, 255, .12);
  border-radius: 10px; padding: 8px 11px; color: #fff; font-size: .85rem; outline: none; font-family: inherit;
  resize: none; line-height: 1.5; max-height: 96px; overflow-y: auto;
}
.dock-field:focus { border-color: rgba(34, 211, 238, .5); box-shadow: 0 0 0 2px rgba(34, 211, 238, .1); }
.dock-send {
  width: 34px; height: 34px; border-radius: 10px; border: none; cursor: pointer; flex-shrink: 0;
  background: linear-gradient(135deg, #22d3ee, #a855f7); color: #fff;
  display: flex; align-items: center; justify-content: center; transition: .2s;
}
.dock-send svg { width: 15px; height: 15px; }
.dock-send:hover:not(:disabled) { filter: brightness(1.15); }
.dock-send:disabled { opacity: .35; cursor: not-allowed; }

/* 只读模式（未实名认证） */
.dock-readonly { justify-content: center; }
.dock-locked { font-size: .72rem; color: rgba(255, 255, 255, .5); text-align: center; }
.dock-locked-link { color: #67e8f9; text-decoration: underline; }

/* 成员面板 */
.dock-members { flex: 1; overflow-y: auto; padding: 10px; display: flex; flex-direction: column; gap: 2px; min-height: 0; }
.mem-group-h {
  display: flex; align-items: center; gap: 6px; padding: 12px 6px 4px;
  font-size: .66rem; color: rgba(255, 255, 255, .4); font-weight: 600; letter-spacing: .05em;
}
.mem-group-dot { width: 7px; height: 7px; border-radius: 50%; }
.mem-group-dot.on { background: #34d399; }
.mem-group-dot.off { background: #6b7280; }
.mem-row {
  display: flex; align-items: center; gap: 10px; padding: 8px 9px; border-radius: 10px; transition: background .15s;
}
.mem-row:hover { background: rgba(255, 255, 255, .05); }
.mem-row.off { opacity: .55; }
.mem-avatar {
  position: relative; width: 34px; height: 34px; border-radius: 50%; flex-shrink: 0; overflow: visible;
  background: linear-gradient(135deg, #667eea, #764ba2);
  display: flex; align-items: center; justify-content: center;
}
.mem-avatar img { width: 100%; height: 100%; object-fit: cover; border-radius: 50%; }
.mem-avatar i { font-style: normal; font-size: .8rem; font-weight: 700; color: #fff; }
.mem-dot {
  position: absolute; right: -1px; bottom: -1px; width: 11px; height: 11px; border-radius: 50%;
  border: 2px solid rgba(10, 12, 30, .95);
}
.mem-dot.on { background: #34d399; box-shadow: 0 0 6px rgba(52, 211, 153, .8); }
.mem-dot.off { background: #6b7280; }
.mem-name { flex: 1; min-width: 0; font-size: .85rem; color: rgba(255, 255, 255, .88); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.mem-tag {
  font-size: .6rem; padding: 1px 6px; border-radius: 6px; flex-shrink: 0;
  background: rgba(244, 63, 142, .2); color: #f9a8d4; border: 1px solid rgba(244, 63, 142, .35);
}
.mem-state { font-size: .66rem; color: rgba(255, 255, 255, .35); flex-shrink: 0; }
.mem-row:not(.off) .mem-state { color: #34d399; }

/* 灯箱 */
.dock-lightbox {
  position: fixed; inset: 0; z-index: 3200; background: rgba(0, 0, 0, .88);
  display: flex; align-items: center; justify-content: center; cursor: zoom-out;
}
.dock-lightbox img { max-width: 92vw; max-height: 92vh; border-radius: 10px; }

/* 移动端：展开态全宽 */
@media (max-width: 480px) {
  .dock-panel { width: 100vw; }
}
</style>
