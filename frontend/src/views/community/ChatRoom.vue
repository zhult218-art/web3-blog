<template>
  <Teleport to="body">
  <div class="chat2">
    <!-- 未登录 -->
    <div v-if="!auth.isLoggedIn" class="chat2-state">
      <div class="state-emoji">🔒</div>
      <p class="state-title">登录后加入「极光广场」</p>
      <button class="c-btn primary" @click="router.push('/login')">去登录</button>
    </div>

    <template v-else-if="room">
      <!-- ============ 顶栏 ============ -->
      <header class="chat2-top">
        <button class="icon-btn back" @click="goBack" title="返回">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M15 19l-7-7 7-7"/></svg>
        </button>
        <div class="top-room">
          <span class="top-avatar">🌌</span>
          <div class="top-meta">
            <span class="top-name">{{ room.name }}</span>
            <span class="top-sub">{{ members.length }} 位成员 · 注册即加入</span>
          </div>
        </div>
        <div class="top-actions">
          <button class="icon-btn" :class="{active: showSearch}" @click="toggleSearch" title="查找聊天记录">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4.3-4.3"/></svg>
          </button>
          <button class="icon-btn" :class="{active: drawerOpen && drawerTab==='notify'}" @click="openDrawer('notify')" title="群通知">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 8a6 6 0 10-12 0c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.7 21a2 2 0 01-3.4 0"/></svg>
            <span v-if="unreadNotify > 0" class="badge-dot">{{ unreadNotify }}</span>
          </button>
          <button class="icon-btn" :class="{active: drawerOpen && drawerTab==='announce'}" @click="openDrawer('announce')" title="群公告">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 11l18-8-4 18-5-8z"/></svg>
          </button>
          <button class="icon-btn" :class="{active: drawerOpen && drawerTab==='album'}" @click="openDrawer('album')" title="群相册">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="3"/><circle cx="9" cy="9" r="2"/><path d="M21 15l-5-5L5 21"/></svg>
          </button>
          <button class="icon-btn" :class="{active: drawerOpen && drawerTab==='members'}" @click="openDrawer('members')" title="群成员">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75"/></svg>
          </button>
          <span :class="['ws-pill', wsConnected ? 'on' : 'off']">
            <i></i>{{ wsConnected ? '在线' : '连接中' }}
          </span>
        </div>
      </header>

      <!-- ============ 主体 ============ -->
      <div class="chat2-content">
        <!-- 消息列表 -->
        <section class="msg-area" ref="messagesRef">
          <div v-if="loadingHistory" class="center-hint">正在加载历史消息…</div>
          <template v-else>
            <div v-for="msg in messages" :key="msg.id" class="msg-wrap">
              <!-- 系统消息 -->
              <div v-if="msg.msgType === 'system'" class="sys-line">{{ msg.content }}</div>
              <!-- 图片消息 -->
              <div v-else-if="msg.msgType === 'image'" :class="['bubble-row', { mine: msg.userId === myUserId }]">
                <div class="bubble-avatar">{{ avatarChar(msg) }}</div>
                <div class="bubble-col">
                  <div class="bubble-meta"><span>{{ nameOf(msg) }}</span><time>{{ fmtTime(msg.createdAt) }}</time></div>
                  <img class="bubble-img" :src="msg.content" loading="lazy" @click="lightbox = msg.content" referrerpolicy="no-referrer" />
                </div>
              </div>
              <!-- 大表情 -->
              <div v-else-if="msg.msgType === 'emoji'" :class="['bubble-row', { mine: msg.userId === myUserId }]">
                <div class="bubble-avatar">{{ avatarChar(msg) }}</div>
                <div class="bubble-col">
                  <div class="bubble-meta"><span>{{ nameOf(msg) }}</span><time>{{ fmtTime(msg.createdAt) }}</time></div>
                  <div class="bubble-emoji">{{ msg.content }}</div>
                </div>
              </div>
              <!-- 文本 -->
              <div v-else :class="['bubble-row', { mine: msg.userId === myUserId }]">
                <div class="bubble-avatar">{{ avatarChar(msg) }}</div>
                <div class="bubble-col">
                  <div class="bubble-meta"><span>{{ nameOf(msg) }}</span><time>{{ fmtTime(msg.createdAt) }}</time></div>
                  <div class="bubble-text">{{ msg.content }}</div>
                </div>
              </div>
            </div>
            <div class="sys-line">{{ wsConnected ? '已连接到极光广场' : '正在建立连接…' }}</div>
          </template>
        </section>

        <!-- 搜索结果面板 -->
        <section v-if="showSearch" class="side-card search-card">
          <div class="side-card-head">
            <span>🔍 查找聊天记录</span>
            <button class="mini-x" @click="showSearch = false">✕</button>
          </div>
          <div class="search-input-row">
            <input v-model="searchKw" @keyup.enter="doSearch" placeholder="输入关键词，回车搜索" />
            <button class="c-btn small" @click="doSearch">搜索</button>
          </div>
          <div class="search-results">
            <div v-if="!searchResults.length && searched" class="muted">没有找到相关记录</div>
            <div v-for="r in searchResults" :key="r.id" class="search-item" @click="jumpToMessage(r)">
              <div class="search-item-meta">{{ nameOf(r) }} · {{ fmtTime(r.createdAt) }}</div>
              <div class="search-item-text" :class="{ 'is-img': r.msgType==='image' }">
                {{ r.msgType === 'image' ? '🖼 [图片] ' + r.content : r.content }}
              </div>
            </div>
          </div>
        </section>

        <!-- 右侧功能抽屉 -->
        <transition name="slide">
          <aside v-if="drawerOpen" class="side-card drawer">
            <div class="tabs">
              <button v-for="t in tabs" :key="t.key" :class="{on: drawerTab===t.key}" @click="switchTab(t.key)">{{ t.label }}</button>
            </div>

            <!-- 群公告 -->
            <div v-if="drawerTab==='announce'" class="tab-panel">
              <button class="c-btn small block" @click="annForm.show = !annForm.show">
                {{ annForm.show ? '取消' : '＋ 发布公告' }}
              </button>
              <div v-if="annForm.show" class="form-box">
                <input v-model="annForm.title" placeholder="公告标题" />
                <textarea v-model="annForm.content" rows="4" placeholder="公告内容"></textarea>
                <label class="chk"><input type="checkbox" v-model="annForm.pinned" /> 置顶</label>
                <button class="c-btn small" @click="saveAnnouncement">发布</button>
              </div>
              <div v-for="a in announcements" :key="a.id" class="ann-card">
                <div class="ann-card-head">
                  <span>{{ a.pinned ? '📌 ' : '' }}{{ a.title }}</span>
                  <button class="mini-x" @click="removeAnnouncement(a.id)">✕</button>
                </div>
                <p class="ann-card-body">{{ a.content }}</p>
                <div class="ann-card-foot">{{ a.nickname }} · {{ fmtDate(a.createdAt) }}</div>
              </div>
              <div v-if="!announcements.length" class="muted">暂无公告</div>
            </div>

            <!-- 群相册 -->
            <div v-if="drawerTab==='album'" class="tab-panel">
              <!-- 文件夹视图 -->
              <template v-if="albumView==='folders'">
                <div class="album-toolbar">
                  <button class="c-btn small" @click="newAlbumShow = !newAlbumShow">＋ 新建文件夹</button>
                </div>
                <div v-if="newAlbumShow" class="inline-form">
                  <input v-model="newAlbumName" placeholder="文件夹名称" @keyup.enter="saveAlbum" />
                  <button class="c-btn small" @click="saveAlbum">确定</button>
                </div>
                <div class="folder-grid">
                  <div v-for="al in albums" :key="al.id" class="folder-card" @click="openAlbum(al)">
                    <button v-if="al.id !== 1" class="folder-del" title="删除文件夹" @click.stop="removeAlbum(al)">✕</button>
                    <div class="folder-cover">
                      <img v-if="al.coverUrl" :src="al.coverUrl" referrerpolicy="no-referrer" />
                      <div v-else class="folder-cover-emoji">🖼</div>
                      <span class="folder-count">{{ al.photoCount }}</span>
                    </div>
                    <div class="folder-name">{{ al.name }}</div>
                  </div>
                </div>
              </template>

              <!-- 照片墙视图 -->
              <template v-else>
                <div class="album-toolbar">
                  <button class="back-link" @click="albumView='folders'">← 全部相册</button>
                  <span class="cur-folder">📁 {{ currentAlbum.name }}</span>
                  <button class="c-btn small" @click="triggerAlbumUpload">上传</button>
                </div>
                <div v-if="!albumPhotos.length" class="muted center pad">这个文件夹还是空的，点“上传”添加照片吧</div>
                <div class="photo-wall">
                  <div v-for="p in albumPhotos" :key="p.id" class="photo-cell">
                    <img :src="p.url" referrerpolicy="no-referrer" loading="lazy" @click="lightbox = p.url" />
                    <button class="photo-del" @click="removePhoto(p.id)" title="删除">✕</button>
                  </div>
                </div>
              </template>
            </div>

            <!-- 群通知 -->
            <div v-if="drawerTab==='notify'" class="tab-panel">
              <button class="c-btn small block" @click="notifyForm.show = !notifyForm.show">
                {{ notifyForm.show ? '取消' : '＋ 发布通知' }}
              </button>
              <div v-if="notifyForm.show" class="form-box">
                <select v-model="notifyForm.type">
                  <option value="info">通知</option>
                  <option value="event">活动</option>
                  <option value="important">重要</option>
                </select>
                <input v-model="notifyForm.title" placeholder="通知标题" />
                <textarea v-model="notifyForm.content" rows="3" placeholder="通知内容"></textarea>
                <button class="c-btn small" @click="saveNotification">发布</button>
              </div>
              <div v-for="n in notifications" :key="n.id" class="notify-card" :class="n.type">
                <div class="notify-card-head">
                  <span>{{ typeIcon(n.type) }} {{ n.title }}</span>
                  <button class="mini-x" @click="removeNotification(n.id)">✕</button>
                </div>
                <p>{{ n.content }}</p>
                <div class="ann-card-foot">{{ n.nickname }} · {{ fmtDate(n.createdAt) }}</div>
              </div>
              <div v-if="!notifications.length" class="muted">暂无通知</div>
            </div>

            <!-- 群成员 -->
            <div v-if="drawerTab==='members'" class="tab-panel">
              <div class="member-count-line">共 {{ members.length }} 人</div>
              <div v-for="m in members" :key="m.id" class="member-row">
                <div class="member-avatar">{{ (m.nickname || m.username || '?').charAt(0).toUpperCase() }}</div>
                <div class="member-info">
                  <span class="member-n">{{ m.nickname || m.username }}</span>
                  <span class="member-u">@{{ m.username }}</span>
                </div>
                <span v-if="m.id === room.ownerId" class="owner-tag">群主</span>
              </div>
            </div>
          </aside>
        </transition>
      </div>

      <!-- ============ 输入区 ============ -->
      <footer class="chat2-input">
        <div class="input-buttons">
          <div class="emoji-wrap">
            <button class="icon-btn round" @click="showEmoji = !showEmoji" title="表情">😊</button>
            <transition name="pop">
              <div v-if="showEmoji" class="emoji-panel">
                <div class="emoji-tabs">
                  <button v-for="(g,i) in emojiGroups" :key="g.name"
                    :class="{on: emojiGroupIdx===i}" @click="emojiGroupIdx=i">{{ g.name }}</button>
                </div>
                <div class="emoji-grid">
                  <button v-for="e in emojiGroups[emojiGroupIdx].emojis" :key="e"
                    class="emoji-cell" @click="pickEmoji(e)">{{ e }}</button>
                </div>
              </div>
            </transition>
          </div>
          <button class="icon-btn round" @click="triggerChatUpload" title="发送图片">🖼</button>
        </div>
        <textarea ref="inputRef" v-model="inputText" @keydown.enter.exact.prevent="sendText"
          placeholder="在极光广场说点什么… Enter 发送，Shift+Enter 换行" rows="1"></textarea>
        <button class="c-btn primary send" @click="sendText" :disabled="!inputText.trim()">发送</button>
      </footer>

      <!-- 图片灯箱 -->
      <transition name="fade">
        <div v-if="lightbox" class="lightbox" @click="lightbox=''">
          <img :src="lightbox" referrerpolicy="no-referrer" />
        </div>
      </transition>

      <!-- 隐藏文件选择 -->
      <input ref="chatFileRef" type="file" accept="image/*" class="hidden-file" @change="onChatFile" />
      <input ref="albumFileRef" type="file" accept="image/*" multiple class="hidden-file" @change="onAlbumFiles" />
    </template>

    <!-- 加载失败 -->
    <div v-else-if="loadError" class="chat2-state">
      <div class="state-emoji">📡</div>
      <p class="state-title">{{ loadError }}</p>
      <button class="c-btn primary" @click="init">重新连接</button>
    </div>

    <!-- 加载中 -->
    <div v-else class="chat2-state">
      <div class="spinner"></div>
      <p class="state-title">正在进入极光广场…</p>
    </div>
  </div>
  </Teleport>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/modules/auth'
import {
  getChatRooms, getChatMembers, getChatMessages, uploadChatImage,
  getAnnouncements, createAnnouncement, deleteAnnouncement,
  getAlbums, createAlbum, deleteAlbum,
  getPhotos, addPhoto, deletePhoto,
  getNotifications, createNotification, deleteNotification,
  searchMessages,
} from '@/api/chat'

const router = useRouter()
const auth = useAuthStore()

const room = ref(null)
const members = ref([])
const messages = ref([])
const inputText = ref('')
const inputRef = ref(null)
const messagesRef = ref(null)
const wsConnected = ref(false)
const loadingHistory = ref(true)
const loadError = ref('')
let ws = null

const myUserId = computed(() => auth.user?.id)

// ========== 工具 ==========
function avatarChar(msg) { return (msg.nickname || msg.username || '?').charAt(0).toUpperCase() }
function nameOf(msg) { return msg.nickname || msg.username || '匿名' }
function fmtTime(t) {
  if (!t) return ''
  const d = new Date(t)
  if (isNaN(d)) return ''
  return `${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}
function fmtDate(t) { return t ? String(t).slice(0,10) : '' }
function goBack() { router.push('/community') }

// ========== 初始化 ==========
async function init() {
  if (!auth.isLoggedIn) return
  loadError.value = ''
  loadingHistory.value = true
  try {
    const res = await getChatRooms()
    const list = res?.data ?? res
    room.value = Array.isArray(list) ? list[0] : null
    if (!room.value) {
      loadError.value = '群聊服务尚未就绪'
      loadingHistory.value = false
      return
    }
    notifySeenAt.value = Number(localStorage.getItem(seenKey.value) || 0)
    const [memRes, msgRes] = await Promise.all([
      getChatMembers(room.value.id).catch(() => ({ data: [] })),
      getChatMessages(room.value.id, 100).catch(() => ({ data: [] })),
      loadNotifications(),
    ])
    members.value = memRes?.data ?? memRes ?? []
    messages.value = msgRes?.data ?? msgRes ?? []
    loadingHistory.value = false
    scrollBottom()
    connectWs(room.value.id)
  } catch (e) {
    loadingHistory.value = false
    loadError.value = e?.response?.status === 404 ? '群聊服务尚未启动，请稍后再试' : '群聊连接失败，请检查网络'
  }
}

// ========== WebSocket ==========
function connectWs(roomId) {
  disconnectWs()
  const token = auth.token
  const proto = location.protocol === 'https:' ? 'wss' : 'ws'
  const url = `${proto}://${location.host}/chat/ws/${roomId}?token=${encodeURIComponent(token)}`
  try {
    ws = new WebSocket(url)
    ws.onopen = () => { wsConnected.value = true }
    ws.onmessage = (e) => {
      try { messages.value.push(JSON.parse(e.data)); scrollBottom() } catch {}
    }
    ws.onclose = () => { wsConnected.value = false }
    ws.onerror = () => { wsConnected.value = false }
  } catch { wsConnected.value = false }
}
function disconnectWs() { if (ws) { ws.close(); ws = null } wsConnected.value = false }

// ========== 发送文本 ==========
function sendText() {
  const text = inputText.value.trim()
  if (!text || !ws || ws.readyState !== WebSocket.OPEN) return
  ws.send(JSON.stringify({ type: 'text', content: text }))
  inputText.value = ''
}
function scrollBottom() {
  nextTick(() => { if (messagesRef.value) messagesRef.value.scrollTop = messagesRef.value.scrollHeight })
}

// ========== 表情 ==========
const emojiGroups = [
  { name: '笑脸', emojis: ['😀','😁','😂','🤣','😃','😄','😅','😆','😉','😊','😋','😎','😍','😘','🥰','😗','🤗','🤔','😐','😑','🙄','😏','😣','😥','😮','🤐','😯','😪','😫','🥱','😴','😌','😛','😜','😝','🤤','😒','😓','😔','😕','🙃','🤑','😲','☹️','🙁','😖','😞','😟','😤','😢','😭','😦','😧','😨','😩','🤯'] },
  { name: '手势', emojis: ['👍','👎','👌','✌️','🤞','🤟','🤘','🤙','👈','👉','👆','👇','☝️','✋','🤚','🖐️','🖖','👋','🤝','🙏','✊','👊','🤛','🤜','🤲','👏','🙌','👐','💪','🖕'] },
  { name: '心情', emojis: ['❤️','🧡','💛','💚','💙','💜','🖤','🤍','🤎','💔','❣️','💕','💞','💓','💗','💖','💘','💝','💟','♥️','💯','💢','💥','💫','💦','💨','🕳','💬','👁️','🔥','✨','⭐','🌟','💥','🎉','🎊','🎈','🎁','🏆','🌈','☀️','⛅','🌙','⚡','❄️','☃️','⛄','🌊','🍀','🌸','🌹','🌺','🌻','🌷'] },
  { name: '萌物', emojis: ['🐶','🐱','🐭','🐹','🐰','🦊','🐻','🐼','🐨','🐯','🦁','🐮','🐷','🐸','🐵','🙈','🙉','🙊','🐔','🐧','🐦','🐤','🦆','🦅','🦉','🦇','🐺','🐗','🐴','🦄','🐝','🐛','🦋','🐌','🐞','🐜','🪲','🐢','🐍','🐙','🦑','🦐','🦀','🐠','🐟','🐬','🐳','🐋','🦈','🐊'] },
  { name: '吃喝', emojis: ['🍏','🍎','🍐','🍊','🍋','🍌','🍉','🍇','🍓','🫐','🍈','🍒','🍑','🥭','🍍','🥥','🥝','🍅','🍆','🥑','🥦','🥬','🥒','🌶️','🌽','🥕','🧄','🧅','🥔','🍠','🥐','🍞','🥖','🥨','🧀','🥚','🍳','🧈','🥞','🥓','🍔','🍟','🍕','🌭','🥪','🌮','🌯','🥗','🍜','🍝','🍣','🍱','🥟','🍤','🍙','🍚','🍘','🍥','🍢','🍡','🍧','🍨','🍦','🥧','🍰','🎂','🍮','🍭','🍬','🍫','🍿','🍩','🍪','☕','🍵','🥤','🍶','🍺','🍻','🥂','🍷','🥃','🍸','🍹'] },
  { name: '物品', emojis: ['⌚','📱','📲','💻','⌨️','🖥️','🖨️','🖱️','💽','💾','💿','📷','📸','📹','🎥','📞','☎️','📟','📠','📺','🧭','⏰','⏱️','🔋','🔌','💡','🔦','🕯️','🪔','🧯','🛢️','💸','💵','💴','💶','💷','💰','💳','💎','⚖️','🪜','🧰','🔧','🔨','⚒️','🛠️','⛏️','🔩','⚙️','🔫','💣','🔪','🗡️','🛡️','🚬','⚓','🪝','🧲','🔗','⛓️','🧰'] },
]
const showEmoji = ref(false)
const emojiGroupIdx = ref(0)
function pickEmoji(e) {
  // 直接作为大表情发送
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({ type: 'emoji', content: e }))
  }
  showEmoji.value = false
}

// ========== 聊天图片上传 ==========
const chatFileRef = ref(null)
function triggerChatUpload() { chatFileRef.value?.click() }
async function onChatFile(ev) {
  const file = ev.target.files?.[0]
  ev.target.value = ''
  if (!file) return
  if (!ws || ws.readyState !== WebSocket.OPEN) return
  try {
    const res = await uploadChatImage(file)
    const url = res?.url ?? res?.data?.url
    if (url) {
      ws.send(JSON.stringify({ type: 'image', content: url }))
      // 归档到默认相册（随手拍），非阻塞
      addPhoto(room.value.id, { albumId: 1, url }).catch(() => {})
    }
  } catch {}
}

// ========== 右侧抽屉 ==========
const drawerOpen = ref(false)
const drawerTab = ref('announce')
const tabs = [
  { key: 'announce', label: '公告' },
  { key: 'album', label: '相册' },
  { key: 'notify', label: '通知' },
  { key: 'members', label: '成员' },
]
function openDrawer(tab) {
  if (drawerOpen.value && drawerTab.value === tab) { drawerOpen.value = false; return }
  drawerTab.value = tab
  drawerOpen.value = true
  loadTabData(tab)
}
function switchTab(tab) { drawerTab.value = tab; loadTabData(tab) }

// ---- 公告 ----
const announcements = ref([])
const annForm = ref({ show: false, title: '', content: '', pinned: false })
async function loadAnnouncements() {
  const res = await getAnnouncements(room.value.id).catch(() => ({ data: [] }))
  announcements.value = res?.data ?? res ?? []
}
async function saveAnnouncement() {
  if (!annForm.value.title.trim()) return
  await createAnnouncement(room.value.id, {
    title: annForm.value.title, content: annForm.value.content, pinned: annForm.value.pinned,
  })
  annForm.value = { show: false, title: '', content: '', pinned: false }
  loadAnnouncements()
}
async function removeAnnouncement(id) { await deleteAnnouncement(id); loadAnnouncements() }

// ---- 相册 ----
const albums = ref([])
const albumView = ref('folders')
const currentAlbum = ref({})
const albumPhotos = ref([])
const newAlbumShow = ref(false)
const newAlbumName = ref('')
const albumFileRef = ref(null)
async function loadAlbums() {
  const res = await getAlbums(room.value.id).catch(() => ({ data: [] }))
  albums.value = res?.data ?? res ?? []
}
async function saveAlbum() {
  const name = newAlbumName.value.trim()
  if (!name) return
  await createAlbum(room.value.id, name)
  newAlbumName.value = ''
  newAlbumShow.value = false
  loadAlbums()
}
async function openAlbum(al) {
  currentAlbum.value = al
  albumView.value = 'photos'
  const res = await getPhotos(room.value.id, al.id).catch(() => ({ data: [] }))
  albumPhotos.value = res?.data ?? res ?? []
}
function triggerAlbumUpload() { albumFileRef.value?.click() }
async function onAlbumFiles(ev) {
  const files = Array.from(ev.target.files || [])
  ev.target.value = ''
  for (const file of files) {
    try {
      const res = await uploadChatImage(file)
      const url = res?.url ?? res?.data?.url
      if (url) await addPhoto(room.value.id, { albumId: currentAlbum.value.id, url })
    } catch {}
  }
  openAlbum(currentAlbum)
}
async function removePhoto(id) { await deletePhoto(id); openAlbum(currentAlbum) }
async function removeAlbum(al) {
  const tip = al.photoCount > 0
    ? `「${al.name}」里还有 ${al.photoCount} 张照片，删除文件夹会一并移除这些照片记录，确定删除吗？`
    : `确定删除文件夹「${al.name}」吗？`
  if (!window.confirm(tip)) return
  await deleteAlbum(al.id)
  loadAlbums()
}

// ---- 通知 ----
const notifications = ref([])
const notifyForm = ref({ show: false, type: 'info', title: '', content: '' })
const seenKey = computed(() => `chat-notify-seen-${room.value?.id}`)
const notifySeenAt = ref(0)
const unreadNotify = computed(() =>
  notifications.value.filter(n => new Date(n.createdAt).getTime() > notifySeenAt.value).length
)
function typeIcon(t) { return t === 'event' ? '🎉' : t === 'important' ? '⚠️' : '🔔' }
async function loadNotifications() {
  const res = await getNotifications(room.value.id).catch(() => ({ data: [] }))
  notifications.value = res?.data ?? res ?? []
}
async function saveNotification() {
  if (!notifyForm.value.title.trim()) return
  await createNotification(room.value.id, {
    type: notifyForm.value.type, title: notifyForm.value.title, content: notifyForm.value.content,
  })
  notifyForm.value = { show: false, type: 'info', title: '', content: '' }
  loadNotifications()
}
async function removeNotification(id) { await deleteNotification(id); loadNotifications() }

function loadTabData(tab) {
  if (tab === 'announce') loadAnnouncements()
  else if (tab === 'album') { loadAlbums(); albumView.value = 'folders' }
  else if (tab === 'notify') {
    loadNotifications().then(() => {
      notifySeenAt.value = Date.now()
      localStorage.setItem(seenKey.value, String(notifySeenAt.value))
    })
  }
}

// ========== 搜索 ==========
const showSearch = ref(false)
const searchKw = ref('')
const searchResults = ref([])
const searched = ref(false)
function toggleSearch() { showSearch.value = !showSearch.value; if (showSearch.value) searchKw.value = '' }
async function doSearch() {
  if (!searchKw.value.trim()) return
  searched.value = true
  const res = await searchMessages(room.value.id, searchKw.value.trim()).catch(() => ({ data: [] }))
  searchResults.value = res?.data ?? res ?? []
}
function jumpToMessage() { showSearch.value = false; scrollBottom() }

// ========== 灯箱 ==========
const lightbox = ref('')

onMounted(() => { init() })
onBeforeUnmount(() => { disconnectWs() })
</script>

<style scoped>
.chat2 {
  position: fixed; inset: 0; z-index: 100;
  display: flex; flex-direction: column;
  background: linear-gradient(160deg, rgba(8,10,28,.96), rgba(14,12,38,.96));
  color: #e6e8f5;
}

/* 状态页 */
.chat2-state { flex:1; display:flex; flex-direction:column; align-items:center; justify-content:center; gap:14px; }
.state-emoji { font-size: 3rem; opacity:.5; }
.state-title { color: rgba(255,255,255,.7); font-size:.95rem; margin:0; }
.spinner { width:40px;height:40px;border-radius:50%;border:3px solid rgba(255,255,255,.15);border-top-color:#22d3ee;animation:spin .9s linear infinite; }
@keyframes spin { to { transform:rotate(360deg); } }

/* 按钮 */
.c-btn {
  border:none;border-radius:10px;padding:9px 16px;font-size:.85rem;cursor:pointer;
  background:rgba(255,255,255,.08);color:#fff;transition:.2s;white-space:nowrap;
}
.c-btn:hover { background:rgba(255,255,255,.16); }
.c-btn.primary { background:linear-gradient(135deg,#22d3ee,#a855f7);font-weight:600; }
.c-btn.primary:hover { filter:brightness(1.1); }
.c-btn.primary:disabled { opacity:.4;cursor:not-allowed; }
.c-btn.small { padding:6px 12px;font-size:.78rem; }
.c-btn.block { width:100%;margin-bottom:10px; }

/* 顶栏 */
.chat2-top {
  display:flex;align-items:center;gap:14px;flex-shrink:0;
  padding:10px 16px;border-bottom:1px solid rgba(255,255,255,.07);
  background:rgba(10,12,30,.7);backdrop-filter:blur(14px);
}
.icon-btn {
  position:relative;width:36px;height:36px;border-radius:10px;border:none;cursor:pointer;
  background:transparent;color:rgba(255,255,255,.65);display:flex;align-items:center;justify-content:center;
  transition:.2s;font-size:1rem;
}
.icon-btn svg { width:19px;height:19px; }
.icon-btn:hover { background:rgba(255,255,255,.08);color:#fff; }
.icon-btn.active { background:rgba(34,211,238,.18);color:#67e8f9; }
.icon-btn.round { width:38px;height:38px;border-radius:12px;background:rgba(255,255,255,.06);font-size:1.15rem; }
.badge-dot {
  position:absolute;top:-4px;right:-4px;min-width:17px;height:17px;padding:0 4px;border-radius:9px;
  background:#f43f8e;color:#fff;font-size:10px;display:flex;align-items:center;justify-content:center;font-weight:700;
}
.top-room { display:flex;align-items:center;gap:10px;flex:1;min-width:0; }
.top-avatar {
  width:38px;height:38px;border-radius:12px;display:flex;align-items:center;justify-content:center;font-size:1.2rem;
  background:linear-gradient(135deg,rgba(34,211,238,.2),rgba(168,85,247,.2));border:1px solid rgba(34,211,238,.25);
}
.top-name { display:block;font-weight:700;font-size:.95rem;color:#fff; }
.top-sub { font-size:.72rem;color:rgba(255,255,255,.45); }
.top-actions { display:flex;align-items:center;gap:2px; }
.ws-pill { display:inline-flex;align-items:center;gap:6px;margin-left:8px;font-size:.72rem;color:rgba(255,255,255,.5); }
.ws-pill i { width:7px;height:7px;border-radius:50%;background:#6b7280; }
.ws-pill.on i { background:#34d399;box-shadow:0 0 8px rgba(52,211,153,.8); }

/* 主体 */
.chat2-content { flex:1;display:flex;min-height:0;position:relative; }
.msg-area { flex:1;overflow-y:auto;padding:20px 24px;display:flex;flex-direction:column;gap:14px; }
/* 每条消息的包裹层也是纵向 flex：.bubble-row.mine 的 align-self:flex-end 才能生效 */
.msg-wrap { display:flex;flex-direction:column;min-width:0; }
.center-hint { text-align:center;color:rgba(255,255,255,.4);font-size:.82rem;padding:20px; }
.sys-line { text-align:center;color:rgba(255,255,255,.35);font-size:.74rem;padding:4px; }

.bubble-row { display:flex;gap:10px;max-width:74%; }
.bubble-row.mine { align-self:flex-end;flex-direction:row-reverse; }
.bubble-avatar {
  width:36px;height:36px;border-radius:50%;flex-shrink:0;font-size:.85rem;font-weight:700;color:#fff;
  background:linear-gradient(135deg,#667eea,#764ba2);display:flex;align-items:center;justify-content:center;
}
.bubble-row.mine .bubble-avatar { background:linear-gradient(135deg,#22d3ee,#a855f7); }
.bubble-col { min-width:0; }
.bubble-row.mine .bubble-col { text-align:right; }
.bubble-meta { display:flex;gap:8px;margin-bottom:4px;align-items:center; }
.bubble-meta span { font-size:.72rem;color:rgba(255,255,255,.6); }
.bubble-meta time { font-size:.68rem;color:rgba(255,255,255,.35); }
.bubble-text {
  display:inline-block;background:rgba(255,255,255,.07);padding:9px 13px;border-radius:14px;
  border-top-left-radius:5px;color:#fff;font-size:.9rem;word-break:break-word;text-align:left;line-height:1.6;
}
.bubble-row.mine .bubble-text {
  background:linear-gradient(135deg,rgba(34,211,238,.22),rgba(168,85,247,.22));
  border:1px solid rgba(34,211,238,.28);border-top-left-radius:14px;border-top-right-radius:5px;
}
.bubble-img { max-width:280px;max-height:300px;border-radius:14px;cursor:zoom-in;border:1px solid rgba(255,255,255,.1);display:inline-block; }
.bubble-emoji { font-size:2.6rem;line-height:1.2; }

/* 右侧卡片通用 */
.side-card {
  width:320px;flex-shrink:0;border-left:1px solid rgba(255,255,255,.07);
  background:rgba(12,14,34,.85);backdrop-filter:blur(14px);
  display:flex;flex-direction:column;min-height:0;
}
.side-card.search-card { position:absolute;right:0;top:0;bottom:0;z-index:20;border-radius:14px 0 0 14px;border:1px solid rgba(255,255,255,.1);border-right:none; }
.side-card-head { display:flex;align-items:center;justify-content:space-between;padding:14px 16px;font-weight:700;font-size:.9rem;border-bottom:1px solid rgba(255,255,255,.07); }
.mini-x { border:none;background:none;color:rgba(255,255,255,.4);cursor:pointer;font-size:.8rem;padding:2px 6px;border-radius:6px; }
.mini-x:hover { color:#f87171;background:rgba(248,113,113,.1); }

/* 抽屉 tabs */
.drawer .tabs { display:flex;border-bottom:1px solid rgba(255,255,255,.07); }
.drawer .tabs button {
  flex:1;padding:11px 4px;border:none;background:none;color:rgba(255,255,255,.5);
  font-size:.8rem;cursor:pointer;border-bottom:2px solid transparent;transition:.2s;
}
.drawer .tabs button.on { color:#67e8f9;border-bottom-color:#22d3ee; }
.tab-panel { flex:1;overflow-y:auto;padding:14px; }
.muted { color:rgba(255,255,255,.4);font-size:.8rem; }
.muted.center { text-align:center; }
.pad { padding:30px 10px; }

/* 表单 */
.form-box { background:rgba(255,255,255,.04);border:1px solid rgba(255,255,255,.08);border-radius:12px;padding:12px;margin-bottom:14px;display:flex;flex-direction:column;gap:8px; }
.form-box input,.form-box textarea,.form-box select,.inline-form input {
  background:rgba(0,0,0,.25);border:1px solid rgba(255,255,255,.12);border-radius:8px;padding:8px 10px;color:#fff;font-size:.82rem;outline:none;font-family:inherit;
}
.form-box input:focus,.form-box textarea:focus { border-color:rgba(34,211,238,.5); }
.chk { display:flex;align-items:center;gap:6px;font-size:.78rem;color:rgba(255,255,255,.7); }

/* 公告卡 */
.ann-card { background:rgba(255,255,255,.03);border:1px solid rgba(255,255,255,.08);border-radius:12px;padding:11px 12px;margin-bottom:10px; }
.ann-card-head { display:flex;align-items:center;justify-content:space-between;font-weight:700;font-size:.85rem;color:#fff; }
.ann-card-body { font-size:.8rem;color:rgba(255,255,255,.7);line-height:1.6;margin:7px 0;white-space:pre-wrap; }
.ann-card-foot { font-size:.7rem;color:rgba(255,255,255,.4); }

/* 相册 */
.album-toolbar { display:flex;align-items:center;gap:8px;margin-bottom:12px;flex-wrap:wrap; }
.back-link { border:none;background:none;color:#67e8f9;font-size:.78rem;cursor:pointer;padding:0; }
.cur-folder { font-size:.8rem;color:rgba(255,255,255,.7);flex:1; }
.inline-form { display:flex;gap:6px;margin-bottom:12px; }
.inline-form input { flex:1; }
.folder-grid { display:grid;grid-template-columns:repeat(2,1fr);gap:12px; }
.folder-card { position:relative;cursor:pointer;transition:.2s; }
.folder-card:hover { transform:translateY(-3px); }
.folder-del {
  position:absolute;top:6px;right:6px;z-index:2;width:22px;height:22px;border-radius:50%;border:none;cursor:pointer;
  background:rgba(0,0,0,.65);color:#fff;font-size:.62rem;opacity:0;transition:.2s;display:flex;align-items:center;justify-content:center;
}
.folder-del:hover { background:rgba(220,38,38,.9); }
.folder-card:hover .folder-del { opacity:1; }
.folder-cover {
  position:relative;aspect-ratio:1;border-radius:12px;overflow:hidden;
  background:linear-gradient(135deg,rgba(34,211,238,.12),rgba(168,85,247,.12));
  border:1px solid rgba(34,211,238,.2);display:flex;align-items:center;justify-content:center;
}
.folder-cover img { width:100%;height:100%;object-fit:cover; }
.folder-cover-emoji { font-size:2rem;opacity:.6; }
.folder-count {
  position:absolute;bottom:6px;right:6px;background:rgba(0,0,0,.6);color:#fff;font-size:.68rem;
  padding:2px 8px;border-radius:8px;
}
.folder-name { font-size:.8rem;color:rgba(255,255,255,.8);margin-top:7px;text-align:center;overflow:hidden;text-overflow:ellipsis;white-space:nowrap; }
.photo-wall { display:grid;grid-template-columns:repeat(3,1fr);gap:7px; }
.photo-cell { position:relative;aspect-ratio:1;border-radius:10px;overflow:hidden; }
.photo-cell img { width:100%;height:100%;object-fit:cover;cursor:zoom-in;transition:.25s; }
.photo-cell:hover img { transform:scale(1.08); }
.photo-del {
  position:absolute;top:4px;right:4px;width:20px;height:20px;border-radius:50%;border:none;cursor:pointer;
  background:rgba(0,0,0,.6);color:#fff;font-size:.62rem;opacity:0;transition:.2s;display:flex;align-items:center;justify-content:center;
}
.photo-cell:hover .photo-del { opacity:1; }

/* 通知 */
.notify-card { border-radius:12px;padding:11px 12px;margin-bottom:10px;border:1px solid rgba(255,255,255,.08);background:rgba(255,255,255,.03); }
.notify-card.event { border-color:rgba(255,213,102,.3);background:rgba(255,213,102,.07); }
.notify-card.important { border-color:rgba(244,63,142,.35);background:rgba(244,63,142,.08); }
.notify-card-head { display:flex;align-items:center;justify-content:space-between;font-weight:700;font-size:.84rem;color:#fff; }
.notify-card p { font-size:.8rem;color:rgba(255,255,255,.7);margin:7px 0;line-height:1.6; }

/* 成员 */
.member-count-line { font-size:.75rem;color:rgba(255,255,255,.45);margin-bottom:10px; }
.member-row { display:flex;align-items:center;gap:10px;padding:7px 6px;border-radius:10px; }
.member-row:hover { background:rgba(255,255,255,.04); }
.member-row .member-avatar {
  width:34px;height:34px;border-radius:50%;background:linear-gradient(135deg,#667eea,#764ba2);
  display:flex;align-items:center;justify-content:center;color:#fff;font-weight:700;font-size:.8rem;
}
.member-info { flex:1;display:flex;flex-direction:column;min-width:0; }
.member-n { font-size:.84rem;color:#fff; }
.member-u { font-size:.7rem;color:rgba(255,255,255,.4); }
.owner-tag { font-size:.66rem;padding:2px 8px;border-radius:7px;background:rgba(34,211,238,.18);color:#67e8f9;border:1px solid rgba(34,211,238,.3); }

/* 搜索 */
.search-input-row { display:flex;gap:8px;padding:14px; }
.search-input-row input { flex:1;background:rgba(0,0,0,.25);border:1px solid rgba(255,255,255,.12);border-radius:8px;padding:8px 10px;color:#fff;font-size:.82rem;outline:none; }
.search-results { flex:1;overflow-y:auto;padding:0 14px 14px; }
.search-item { padding:9px 10px;border-radius:10px;cursor:pointer;margin-bottom:6px;border:1px solid transparent; }
.search-item:hover { background:rgba(255,255,255,.05);border-color:rgba(255,255,255,.08); }
.search-item-meta { font-size:.68rem;color:rgba(255,255,255,.45);margin-bottom:3px; }
.search-item-text { font-size:.8rem;color:rgba(255,255,255,.8);overflow:hidden;text-overflow:ellipsis;white-space:nowrap; }

/* 输入区 */
.chat2-input { display:flex;align-items:flex-end;gap:10px;padding:12px 16px;border-top:1px solid rgba(255,255,255,.07);background:rgba(10,12,30,.7);flex-shrink:0; }
.input-buttons { display:flex;gap:4px;position:relative; }
.emoji-wrap { position:relative; }
.chat2-input textarea {
  flex:1;background:rgba(255,255,255,.05);border:1px solid rgba(255,255,255,.1);border-radius:12px;
  padding:10px 13px;color:#fff;font-size:.9rem;resize:none;outline:none;font-family:inherit;line-height:1.5;max-height:120px;
}
.chat2-input textarea:focus { border-color:rgba(34,211,238,.45); }
.send { padding:10px 22px; }
.hidden-file { display:none; }

/* 表情面板 */
.emoji-panel {
  position:absolute;bottom:50px;left:0;width:330px;background:rgba(14,16,38,.97);
  border:1px solid rgba(255,255,255,.12);border-radius:14px;box-shadow:0 18px 50px rgba(0,0,0,.5);
  z-index:50;overflow:hidden;backdrop-filter:blur(16px);
}
.emoji-tabs { display:flex;overflow-x:auto;border-bottom:1px solid rgba(255,255,255,.08); }
.emoji-tabs button { border:none;background:none;color:rgba(255,255,255,.5);font-size:.74rem;padding:9px 11px;cursor:pointer;white-space:nowrap; }
.emoji-tabs button.on { color:#67e8f9;border-bottom:2px solid #22d3ee; }
.emoji-grid { display:grid;grid-template-columns:repeat(9,1fr);gap:2px;padding:8px;max-height:210px;overflow-y:auto; }
.emoji-cell { border:none;background:none;font-size:1.15rem;padding:5px;border-radius:8px;cursor:pointer;transition:.15s; }
.emoji-cell:hover { background:rgba(255,255,255,.12);transform:scale(1.2); }

/* 灯箱 */
.lightbox { position:fixed;inset:0;z-index:200;background:rgba(0,0,0,.9);display:flex;align-items:center;justify-content:center;padding:40px;cursor:zoom-out; }
.lightbox img { max-width:100%;max-height:100%;border-radius:10px; }

/* 过渡 */
.slide-enter-active,.slide-leave-active { transition:transform .28s ease,opacity .28s ease; }
.slide-enter-from,.slide-leave-to { transform:translateX(40px);opacity:0; }
.pop-enter-active,.pop-leave-active { transition:.2s ease;transform-origin:bottom left; }
.pop-enter-from,.pop-leave-to { transform:scale(.9);opacity:0; }
.fade-enter-active,.fade-leave-active { transition:.2s; }
.fade-enter-from,.fade-leave-to { opacity:0; }
</style>
