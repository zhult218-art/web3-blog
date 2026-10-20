<template>
  <div class="music-player">
    <!-- 主区：黑胶 + 歌词 -->
    <div class="player-main" :class="{ 'player-main-single': !showVinyl }">
      <!-- 黑胶唱片 -->
      <div v-if="showVinyl" class="vinyl-side">
        <div class="tonearm" :class="{ 'tonearm-on': ui.playing }">
          <div class="tonearm-head"></div>
        </div>
        <div class="vinyl" :class="{ 'vinyl-spin': ui.playing }">
          <div class="vinyl-grooves"></div>
          <div class="vinyl-shine"></div>
          <div class="vinyl-label" :style="labelStyle">
            <img v-if="currentCover" :src="currentCover" class="label-img" alt="" />
            <div v-else class="label-fallback">{{ currentInitials }}</div>
          </div>
        </div>
        <div class="vinyl-tip">
          <span class="dot" :class="{ 'dot-on': ui.playing }"></span>
          {{ ui.playing ? 'PLAYING · 45RPM' : 'PAUSED' }}
        </div>
      </div>

      <!-- 当前歌词：仅显示当前唱的两行（当前行 + 下一行），居中 -->
      <div class="lyrics-wrap" ref="lyricsWrap" @click="toggleLyricsView">
        <div v-if="lyricLines.length" class="lyrics-current">
          <p class="lyric-now" :key="lyricIndex">{{ currentLyricText || '♪' }}</p>
          <p class="lyric-next">{{ nextLyricText || ' ' }}</p>
        </div>
        <div v-else class="lyrics-empty">
          <span class="empty-note">NO LYRICS</span>
          暂无歌词，静心聆听
        </div>
      </div>
    </div>

    <!-- 底部播放器栏 -->
    <div class="player-bar" data-glow>
      <!-- 进度区（支持点击与拖拽定位） -->
      <div class="bar-progress">
        <span class="bar-time">{{ format(ui.time) }}</span>
        <div class="progress-track" ref="progressTrack" @pointerdown="onSeekPointerDown">
          <div class="progress-fill" :style="{ width: (dragPct ?? progressPct) + '%' }">
            <span class="progress-knob" :class="{ 'knob-active': dragging }"></span>
          </div>
        </div>
        <span class="bar-time">{{ format(ui.duration) }}</span>
      </div>

      <!-- 控制按钮区 -->
      <div class="bar-controls">
        <!-- 音质下拉（标准 128k / 较高 192k / 极高 320k / 无损） -->
        <select
          class="quality-select"
          v-model="quality"
          @change="onQualityChange"
          :title="`音质：${qualityLabel}`"
        >
          <option :value="128000">标准 128k</option>
          <option :value="192000">较高 192k</option>
          <option :value="320000">极高 320k</option>
          <option :value="999000">无损</option>
        </select>

        <button class="ctl" :title="modeLabel" @click="cycleMode">
          <svg v-if="ui.modeIndex === 1" class="icon" viewBox="0 0 24 24"><path fill="currentColor" d="M7 7h10v3l4-4-4-4v3H5v7h2V7zm10 10H7v-3l-4 4 4 4v-3h12v-7h-2v5z"/></svg>
          <svg v-else-if="ui.modeIndex === 2" class="icon" viewBox="0 0 24 24"><path fill="currentColor" d="M10.59 9.17L5.41 4 4 5.41l5.17 5.17 1.42-1.41zM14.5 4l2.04 2.04L4 18.59 5.41 20 17.96 7.46 20 9.5V4h-5.5zm.33 9.41l-1.41 1.41 3.13 3.13L14.5 20H20v-5.5l-2.04 2.04-3.13-3.13z"/></svg>
          <svg v-else-if="ui.modeIndex === 3" class="icon" viewBox="0 0 24 24"><path fill="currentColor" d="M7 7h10v3l4-4-4-4v3H5v7h2V7zm10 10H7v-3l-4 4 4 4v-3h12v-7h-2v5zm-4-2v-4h-1.5v4H13z"/></svg>
          <svg v-else class="icon" viewBox="0 0 24 24"><path fill="currentColor" d="M7 7h10v3l4-4-4-4v3H5v7h2V7zm10 10H7v-3l-4 4 4 4v-3h12v-7h-2v5z"/></svg>
        </button>

        <button class="ctl" title="上一曲" @click="player.prev()">
          <svg class="icon" viewBox="0 0 24 24"><path fill="currentColor" d="M6 6h2v12H6V6zm3.5 6l8.5 6V6l-8.5 6z"/></svg>
        </button>

        <button class="play-btn" title="播放 / 暂停" @click="player.toggle()">
          <svg v-if="!ui.playing" class="icon icon-lg" viewBox="0 0 24 24"><path fill="currentColor" d="M8 5v14l11-7z"/></svg>
          <svg v-else class="icon icon-lg" viewBox="0 0 24 24"><path fill="currentColor" d="M6 4h4v16H6V4zm8 0h4v16h-4V4z"/></svg>
        </button>

        <button class="ctl" title="下一曲" @click="player.next()">
          <svg class="icon" viewBox="0 0 24 24"><path fill="currentColor" d="M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z"/></svg>
        </button>

        <span class="ctl-divider"></span>

        <button class="ctl" title="音量" @click="toggleMute">
          <svg v-if="muted || volumePct === 0" class="icon" viewBox="0 0 24 24"><path fill="currentColor" d="M16.5 12A4.5 4.5 0 0014 7.97v2.21l2.45 2.45c.03-.2.05-.41.05-.63zm2.5 0c0 .94-.2 1.82-.54 2.64l1.51 1.51C20.63 14.91 21 13.5 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 5 3.54 5 6.71zM4.27 3L3 4.27 7.73 9H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 1.18v2.06c1.38-.31 2.63-.95 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z"/></svg>
          <svg v-else class="icon" viewBox="0 0 24 24"><path fill="currentColor" d="M3 9v6h4l5 5V4L7 9H3zm13.5 3A4.5 4.5 0 0014 7.97v8.05A4.5 4.5 0 0016.5 12zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z"/></svg>
        </button>
        <input class="vol-slider" type="range" min="0" max="100" :value="volumePct"
          @input="onVolumeInput" :title="`音量 ${volumePct}%`" />

        <span class="ctl-divider"></span>

        <!-- 喜欢（接通真实接口） -->
        <button class="ctl" :class="{ 'ctl-on': liked }" :title="liked ? '取消喜欢' : '喜欢'" @click="toggleLike">
          <svg class="icon" :class="{ 'like-on': liked }" viewBox="0 0 24 24">
            <path fill="currentColor" d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
          </svg>
        </button>

        <!-- 评论（接通真实数量 + 点击打开评论抽屉） -->
        <button class="ctl" :class="{ 'ctl-on': commentOpen }" title="评论" @click="toggleComment">
          <svg class="icon" viewBox="0 0 24 24"><path fill="currentColor" d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12z"/></svg>
          <span class="ctl-num">{{ commentCount }}</span>
        </button>

        <!-- 播放历史 -->
        <button class="ctl" :class="{ 'ctl-on': panel === 'history' }" title="播放历史" @click="togglePanel('history')">
          <svg class="icon" viewBox="0 0 24 24"><path fill="currentColor" d="M13 3a9 9 0 00-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42A8.954 8.954 0 0013 21a9 9 0 000-18zm-1 5v5l4.28 2.54.72-1.21-3.5-2.08V8H12z"/></svg>
        </button>

        <!-- 热榜 -->
        <button class="ctl" :class="{ 'ctl-on': panel === 'hot' }" title="热榜" @click="togglePanel('hot')">
          <svg class="icon" viewBox="0 0 24 24"><path fill="currentColor" d="M13.5.67s.74 2.65.74 4.8c0 2.06-1.35 3.73-3.41 3.73-2.07 0-3.53-1.67-3.53-3.73l.03-.36C5.171 8.71 4 11.13 4 13.88c0 4.41 3.59 8 8 8s8-3.59 8-8C20 8.61 17.41 3.8 13.5.67zM11.71 19c-1.78 0-3.22-1.4-3.22-3.14 0-1.62 1.05-2.76 2.81-3.12 1.77-.36 3.6-1.21 4.66-2.58.39 1.29.59 2.65.59 4 0 3.18-2.18 4.84-4.84 4.84z"/></svg>
        </button>

        <!-- 搜索 -->
        <button class="ctl" :class="{ 'ctl-on': panel === 'search' }" title="搜索" @click="togglePanel('search')">
          <svg class="icon" viewBox="0 0 24 24"><path fill="currentColor" d="M15.5 14h-.79l-.28-.27A6.471 6.471 0 0016 9.5 6.5 6.5 0 109.5 16c1.61 0 3.09-.59 4.23-1.56l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>
        </button>

        <!-- 歌单 -->
        <button class="ctl" :class="{ 'ctl-on': listOpen }" title="歌单" @click="listOpen = !listOpen; panel = ''">
          <svg class="icon" viewBox="0 0 24 24"><path fill="currentColor" d="M6 6h2v12H6V6zm3.5 6l8.5 6V6l-8.5 6zM15 4h4v2h-4V4z"/></svg>
        </button>
      </div>

      <!-- 浮层面板（歌单 / 热榜 / 历史 / 搜索，统一面板） -->
      <transition name="list-fade">
        <div v-if="listOpen || panel" class="playlist-panel" data-glow>
          <!-- Tab 头 -->
          <div class="pl-tabs">
            <button class="pl-tab" :class="{ on: listOpen && !panel }" @click="listOpen = true; panel = ''">歌单</button>
            <button class="pl-tab" :class="{ on: panel === 'hot' }" @click="switchPanel('hot')">热榜</button>
            <button class="pl-tab" :class="{ on: panel === 'history' }" @click="switchPanel('history')">历史</button>
            <button class="pl-tab" :class="{ on: panel === 'search' }" @click="switchPanel('search')">搜索</button>
            <span class="pl-count">{{ panelCountLabel }}</span>
          </div>

          <!-- 歌单内容（保留原样） -->
          <div v-if="listOpen && !panel" class="pl-list">
            <div
              v-for="(t, i) in ui.playlist"
              :key="t.id || i"
              class="pl-item"
              :class="{ 'pl-item-on': ui.track && (ui.track.id || ui.track.musicId) === (t.id || t.musicId) }"
              @click="player.play(t)"
            >
              <span class="pl-idx">{{ isCurrent(t) ? '♪' : String(i + 1).padStart(2, '0') }}</span>
              <span class="pl-title">{{ t.title }}</span>
              <span class="pl-artist">{{ t.artist }}</span>
              <span class="pl-dur">{{ format(t.duration) }}</span>
            </div>
            <div v-if="!ui.playlist.length" class="pl-empty">歌单为空</div>
          </div>

          <!-- 热榜内容 -->
          <div v-else-if="panel === 'hot'" class="pl-list">
            <div
              v-for="(t, i) in hotList"
              :key="t.id || i"
              class="pl-item"
              :class="{ 'pl-item-on': isCurrent(t) }"
              @click="playFromExternal(t)"
            >
              <span class="pl-idx" :class="{ 'pl-idx-top': i < 3 }">{{ i + 1 }}</span>
              <span class="pl-title">{{ t.title }}</span>
              <span class="pl-artist">{{ t.artist }}</span>
              <span class="pl-play-count">{{ formatPlayCount(t.playCount) }} 次</span>
            </div>
            <div v-if="!hotList.length" class="pl-empty">{{ hotLoading ? '加载中…' : '暂无热榜数据' }}</div>
          </div>

          <!-- 历史内容 -->
          <div v-else-if="panel === 'history'" class="pl-list">
            <div
              v-for="(t, i) in historyList"
              :key="t.id || i"
              class="pl-item"
              :class="{ 'pl-item-on': isCurrent(t) }"
              @click="playFromExternal(t)"
            >
              <span class="pl-idx">{{ i + 1 }}</span>
              <span class="pl-title">{{ t.title }}</span>
              <span class="pl-artist">{{ t.artist }}</span>
              <span class="pl-dur">{{ format(t.duration) }}</span>
            </div>
            <div v-if="!historyList.length" class="pl-empty">暂无播放历史</div>
            <button v-else class="pl-clear" @click="onClearHistory">清空历史</button>
          </div>

          <!-- 搜索内容 -->
          <div v-else-if="panel === 'search'" class="pl-search-wrap">
            <div class="ps-input-row">
              <input
                ref="searchInputEl"
                v-model="searchKw"
                class="ps-input"
                placeholder="搜索本地曲库 + 网易云…"
                @keyup.enter="doSearch"
              />
              <button class="ps-go" @click="doSearch">搜索</button>
            </div>
            <div class="pl-list">
              <div
                v-for="(t, i) in searchResults"
                :key="(t.source || '') + (t.id || i)"
                class="pl-item"
                :class="{ 'pl-item-on': isCurrent(t) }"
                @click="playFromExternal(t)"
              >
                <span class="pl-idx">{{ i + 1 }}</span>
                <span class="pl-title">{{ t.title }}</span>
                <span class="pl-artist">{{ t.artist }}</span>
                <span class="pl-source" :class="`src-${t.source}`">{{ t.source === 'local' ? '本地' : t.source === 'netease' ? '网易云' : t.source || '' }}</span>
              </div>
              <div v-if="!searchResults.length" class="pl-empty">{{ searchLoading ? '搜索中…' : (searchKw ? '无结果' : '输入关键字开始搜索') }}</div>
            </div>
          </div>
        </div>
      </transition>

      <!-- 评论抽屉 -->
      <transition name="drawer-slide">
        <div v-if="commentOpen" class="comment-drawer" data-glow>
          <div class="cd-head">
            <span class="cd-title">评论 · {{ commentCount }}</span>
            <button class="cd-close" @click="commentOpen = false">×</button>
          </div>
          <div class="cd-list" ref="commentListEl">
            <div v-for="c in comments" :key="c.id" class="cd-item">
              <img v-if="c.avatar" :src="c.avatar" class="cd-avatar" alt="" />
              <span v-else class="cd-avatar cd-avatar-fallback">{{ (c.username || 'M').slice(0, 1).toUpperCase() }}</span>
              <div class="cd-main">
                <div class="cd-user">{{ c.username }}<span class="cd-time"> · {{ formatTime(c.createdAt) }}</span></div>
                <div class="cd-content">{{ c.content }}</div>
              </div>
            </div>
            <div v-if="!comments.length" class="pl-empty">{{ commentLoading ? '加载中…' : '暂无评论，抢沙发' }}</div>
          </div>
          <div class="cd-input-row">
            <input
              v-model="commentText"
              class="cd-input"
              placeholder="发表评论…"
              @keyup.enter="submitComment"
              maxlength="500"
            />
            <button class="cd-send" :disabled="!commentText.trim() || commentSending" @click="submitComment">
              {{ commentSending ? '发送中' : '发送' }}
            </button>
          </div>
        </div>
      </transition>
    </div>
  </div>
</template>

<script setup>
// ====================================================
// 音乐播放器组件（YesPlayMusic 风格强化）
// 真实接通后端接口：收藏 / 评论 / 热榜 / 搜索 / 历史 / 音质切换
// 黑胶视觉、歌词同步、进度拖拽保留
// ====================================================
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { usePlayerStore } from '@/stores/modules/player'
import { getLocalHistory, clearLocalHistory } from '@/stores/modules/player'
import {
  toggleLike as toggleLikeApi, getLikeStatus, getHotSongs, searchMusic, recordPlay, getHistory, clearHistory,
  searchNetease,
} from '@/api/music'
import { getCommentList, createComment } from '@/api/forum'
import { useAuthStore } from '@/stores/modules/auth'
import { useToastStore } from '@/stores/modules/toast'
import { formatRelative as formatTime } from '@/utils/date.js'

const props = defineProps({
  tracks: { type: Array, default: () => [] },
  lyricsMap: { type: Object, default: () => ({}) },
  showVinyl: { type: Boolean, default: true },
  initialId: { type: [String, Number], default: null },
})

const emit = defineEmits(['track-change', 'focus-search'])

const player = usePlayerStore()
const auth = useAuthStore()
const toast = useToastStore()

// ---- 浮层面板 ----
const listOpen = ref(false)
const panel = ref('') // '' | 'hot' | 'history' | 'search'

function togglePanel(name) {
  if (panel.value === name) { panel.value = ''; listOpen.value = false; return }
  listOpen.value = false
  panel.value = name
}

function switchPanel(name) {
  listOpen.value = false
  panel.value = name
}

// ---- 评论抽屉 ----
const commentOpen = ref(false)
const commentCount = ref(0)
const comments = ref([])
const commentText = ref('')
const commentLoading = ref(false)
const commentSending = ref(false)
const commentListEl = ref(null)

function toggleComment() {
  commentOpen.value = !commentOpen.value
  if (commentOpen.value && comments.value.length === 0) loadComments()
}

// ---- 收藏 ----
const liked = ref(false)
const likeLoading = ref(false)

async function toggleLike() {
  const track = ui.track
  if (!track || !/^\d+$/.test(String(track.id || ''))) {
    toast.info('当前曲目不支持收藏')
    return
  }
  if (!auth.isLoggedIn) {
    toast.info('请先登录后再收藏')
    return
  }
  if (likeLoading.value) return
  likeLoading.value = true
  try {
    const r = await toggleLikeApi(track.id)
    liked.value = !!r?.data?.liked
    toast.success(liked.value ? '已加入收藏' : '已取消收藏')
  } catch (e) {
    toast.error('操作失败，请稍后再试')
  } finally {
    likeLoading.value = false
  }
}

async function refreshLikeStatus() {
  const track = ui.track
  if (!track || !/^\d+$/.test(String(track.id || ''))) { liked.value = false; return }
  if (!auth.isLoggedIn) { liked.value = false; return }
  try {
    const r = await getLikeStatus(track.id)
    liked.value = !!r?.data?.liked
  } catch { liked.value = false }
}

// ---- 评论数据 ----
async function refreshCommentCount() {
  const track = ui.track
  if (!track || !/^\d+$/.test(String(track.id || ''))) { commentCount.value = 0; return }
  try {
    const r = await getCommentList(track.id, 'music', { page: 1, size: 1 })
    commentCount.value = r?.data?.total || r?.total || 0
  } catch { commentCount.value = 0 }
}

async function loadComments() {
  const track = ui.track
  if (!track || !/^\d+$/.test(String(track.id || ''))) { comments.value = []; return }
  commentLoading.value = true
  try {
    const r = await getCommentList(track.id, 'music', { page: 1, size: 50 })
    const data = r?.data
    comments.value = data?.records || data?.list || []
    commentCount.value = data?.total || comments.value.length || 0
    await nextTick()
    if (commentListEl.value) commentListEl.value.scrollTop = 0
  } catch {
    comments.value = []
  } finally {
    commentLoading.value = false
  }
}

async function submitComment() {
  const text = commentText.value.trim()
  if (!text) return
  if (!auth.isLoggedIn) { toast.info('请先登录后再评论'); return }
  const track = ui.track
  if (!track || !/^\d+$/.test(String(track.id || ''))) return
  commentSending.value = true
  try {
    await createComment({ targetType: 'music', targetId: Number(track.id), content: text })
    commentText.value = ''
    toast.success('评论发表成功')
    await loadComments()
  } catch (e) {
    toast.error('评论发表失败')
  } finally {
    commentSending.value = false
  }
}

// ---- 音质切换 ----
// 默认极高 320k，无损 999000 是网易云"无损"档位标识
const quality = ref(320000)
const qualityLabel = computed(() => {
  const map = { 128000: '标准 128k', 192000: '较高 192k', 320000: '极高 320k', 999000: '无损' }
  return map[quality.value] || '极高 320k'
})

async function onQualityChange() {
  const track = ui.track
  if (!track) return
  // 仅对网易云曲目生效（其他源无 br 概念）
  if (!track.neteaseId || !/^\d+$/.test(String(track.neteaseId))) {
    toast.info('当前曲目不支持音质切换')
    return
  }
  toast.info(`切换到 ${qualityLabel.value}，重新加载…`)
  try {
    const { getNeteaseSongUrl } = await import('@/api/music')
    const r = await getNeteaseSongUrl(track.neteaseId, quality.value)
    const newUrl = r?.data?.[0]?.url
    if (newUrl) {
      const newTrack = { ...track, url: newUrl }
      player.play(newTrack)
    } else {
      toast.error('该音质暂不可用，可能是 VIP 专属')
    }
  } catch (e) {
    toast.error('音质切换失败')
  }
}

// ---- 热榜 ----
const hotList = ref([])
const hotLoading = ref(false)
async function loadHot() {
  hotLoading.value = true
  try {
    const r = await getHotSongs(20)
    hotList.value = r?.data || []
  } catch { hotList.value = [] }
  finally { hotLoading.value = false }
}

function formatPlayCount(n) {
  if (!n) return '0'
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return String(n)
}

// ---- 播放历史 ----
const historyList = ref([])
const historyLoading = ref(false)
async function loadHistory() {
  historyLoading.value = true
  try {
    // 优先用后端历史（登录用户）；未登录或失败时降级到 localStorage
    if (auth.isLoggedIn) {
      try {
        const r = await getHistory(1, 50)
        const data = r?.data
        const records = data?.records || data?.list || []
        if (records.length) { historyList.value = records.map(t => ({ ...t, source: 'local' })); return }
      } catch { /* 后端不可用，降级本地 */ }
    }
    historyList.value = getLocalHistory()
  } finally {
    historyLoading.value = false
  }
}

async function onClearHistory() {
  // 后端清空（登录用户）+ 本地清空
  if (auth.isLoggedIn) {
    try { await clearHistory() } catch { /* ignore */ }
  }
  clearLocalHistory()
  historyList.value = []
  toast.success('播放历史已清空')
}

// ---- 搜索 ----
const searchKw = ref('')
const searchResults = ref([])
const searchLoading = ref(false)
const searchInputEl = ref(null)

async function doSearch() {
  const kw = searchKw.value.trim()
  if (!kw) return
  searchLoading.value = true
  searchResults.value = []
  // 并行：本地曲库（media-service）+ 网易云
  const tasks = [
    (async () => {
      try {
        const r = await searchMusic(kw, 1, 20)
        const data = r?.data
        const records = data?.records || data?.list || []
        return records.map(t => ({ ...t, source: 'local' }))
      } catch { return [] }
    })(),
    (async () => {
      try {
        const r = await searchNetease(kw, 20)
        // 网易云返回结构：{ result: { songs: [...] } } 或网关直出
        const songs = r?.result?.songs || r?.data?.result?.songs || r?.songs || []
        return songs.map(s => ({
          id: s.id,
          neteaseId: String(s.id),
          title: s.name || s.title || '',
          artist: (s.artists || s.ar || []).map(a => a.name).join('/') || '未知艺术家',
          album: s.album?.name || s.al?.name || '',
          duration: Math.floor((s.duration || s.dt || 0) / 1000),
          cover: s.album?.picUrl || s.al?.picUrl || '',
          source: 'netease',
        }))
      } catch { return [] }
    })(),
  ]
  const [localRes, neteaseRes] = await Promise.all(tasks)
  searchResults.value = [...localRes, ...neteaseRes]
  searchLoading.value = false
}

function focusSearchInput() {
  panel.value = 'search'
  listOpen.value = false
  nextTick(() => searchInputEl.value?.focus())
}

// 把外部曲目并入当前播放列表并播放
function playFromExternal(track) {
  if (!track) return
  const list = player.playlist.slice()
  const exists = list.find(t => String(t.id) === String(track.id))
  if (!exists) list.push(track)
  player.setPlaylist(list)
  player.play({ ...track, id: track.id || track.musicId })
}

// ---- 进度条拖拽（保留原实现） ----
const progressTrack = ref(null)
const dragging = ref(false)
const dragPct = ref(null)
const lyricsWrap = ref(null)
const muted = ref(false)
let savedVolume = player.volume > 0 ? player.volume : 0.7

function pctFromEvent(e) {
  const el = progressTrack.value
  if (!el) return 0
  const rect = el.getBoundingClientRect()
  return Math.min(100, Math.max(0, ((e.clientX - rect.left) / rect.width) * 100))
}

function onSeekPointerDown(e) {
  dragging.value = true
  dragPct.value = pctFromEvent(e)
  window.addEventListener('pointermove', onSeekPointerMove)
  window.addEventListener('pointerup', onSeekPointerUp)
}

function onSeekPointerMove(e) {
  if (!dragging.value) return
  dragPct.value = pctFromEvent(e)
}

function onSeekPointerUp(e) {
  if (!dragging.value) return
  const pct = pctFromEvent(e)
  dragging.value = false
  dragPct.value = null
  window.removeEventListener('pointermove', onSeekPointerMove)
  window.removeEventListener('pointerup', onSeekPointerUp)
  player.seekByPercent(pct)
}

function onVolumeInput(e) {
  const v = Math.min(1, Math.max(0, Number(e.target.value) / 100))
  muted.value = v === 0
  if (v > 0) savedVolume = v
  player.setVolume(v)
}

function toggleMute() {
  if (!muted.value) {
    if (player.volume > 0) savedVolume = player.volume
    player.setVolume(0)
    muted.value = true
  } else {
    player.setVolume(savedVolume > 0 ? savedVolume : 0.7)
    muted.value = false
  }
}

// 统一暴露给模板的响应式状态：直接映射全局 store
const ui = reactive({
  get track() { return player.currentTrack },
  get playing() { return player.isPlaying },
  get time() { return player.currentTime },
  get duration() { return player.duration || (player.currentTrack?.duration || 0) },
  get playlist() { return player.playlist },
  get modeIndex() { return ({ order: 0, shuffle: 2, 'loop-one': 3 })[player.playMode] ?? 0 },
  get liked() { return liked.value },
  get volume() { return player.volume },
})

// tracks props 变化时同步到全局列表，并自动播放 initialId 曲目
watch(() => props.tracks, (list) => {
  const normalized = (list || []).map((t, i) => ({
    ...t,
    id: t.id || t.musicId || i + 1,
    neteaseId: t.neteaseId || t.netease_id || '',
    duration: Number(t.duration) || 240,
    url: t.url || t.audio_url || '',
  }))
  if (normalized.length) player.setPlaylist(normalized)
  if (props.initialId) {
    const target = normalized.find(t => String(t.id || t.musicId) === String(props.initialId)) || normalized[0]
    if (target && player.currentTrack?.id !== target.id) player.play({ ...target, neteaseId: target.neteaseId })
  }
}, { immediate: true })

watch(() => ui.track, (t) => {
  emit('track-change', t)
  // 切歌时刷新收藏态与评论数
  refreshLikeStatus()
  refreshCommentCount()
  if (commentOpen.value) loadComments()
})

const currentCover = computed(() => ui.track?.cover || '')
const currentInitials = computed(() => {
  const t = ui.track
  if (!t) return '♪'
  return (t.title || 'M')?.slice(0, 1).toUpperCase()
})

const labelStyle = computed(() => {
  if (currentCover.value) return {}
  const seed = (ui.track?.id || 'x').toString().split('').reduce((a, c) => a + c.charCodeAt(0), 0)
  const hues = [26, 340, 200, 160, 45]
  const h = hues[seed % hues.length]
  return { background: `linear-gradient(135deg, hsl(${h},70%,55%), hsl(${(h + 40) % 360},65%,40%))` }
})

const lyricLines = computed(() => player.lyrics && player.lyrics.length ? player.lyrics : [])
const lyricIndex = computed(() => player.currentLyricIndex)

// 当前行歌词文本（点击歌词区后显示在播放框中间）
const currentLyricText = computed(() => {
  const idx = lyricIndex.value
  if (idx < 0 || !lyricLines.value.length) return ''
  return lyricLines.value[idx]?.text || ''
})
// 下一行歌词文本
const nextLyricText = computed(() => {
  const idx = lyricIndex.value
  if (idx < 0 || !lyricLines.value.length) return ''
  return lyricLines.value[idx + 1]?.text || ''
})

// 点击歌词区切换展开/收起（预留交互入口，当前始终显示两行模式）
function toggleLyricsView() {
  /* 预留：后续可做展开全量歌词切换 */
}

const progressPct = computed(() => {
  if (!ui.duration) return 0
  return Math.min(100, Math.max(0, (ui.time / ui.duration) * 100))
})

const volumePct = computed(() => Math.round((player.volume ?? 0.7) * 100))

const panelCountLabel = computed(() => {
  if (listOpen.value && !panel.value) return `${ui.playlist.length} TRACKS`
  if (panel.value === 'hot') return `${hotList.value.length} HOT`
  if (panel.value === 'history') return `${historyList.value.length} PLAYED`
  if (panel.value === 'search') return `${searchResults.value.length} FOUND`
  return ''
})

const MODE_LABELS = ['顺序播放', '循环列表', '随机播放', '单曲循环']
const modeLabel = computed(() => MODE_LABELS[ui.modeIndex] || '顺序播放')

function format(s) { return player.formatTime(s) }


function isCurrent(t) {
  const cur = ui.track
  if (!cur) return false
  return String(cur.id || cur.musicId) === String(t.id || t.musicId)
}

function cycleMode() {
  const map = ['order', 'shuffle', 'loop-one']
  const idx = map.indexOf(player.playMode)
  player.setMode(map[(idx + 1) % map.length])
}

onMounted(() => {
  // 拉取热榜（首屏即用，不依赖切到面板才加载）
  loadHot()
  // 注册锁屏 / 通知栏控制
  player.bindMediaSession?.()
  // 当前已有曲目时立即拉取收藏态与评论数
  refreshLikeStatus()
  refreshCommentCount()
})

onBeforeUnmount(() => {
  window.removeEventListener('pointermove', onSeekPointerMove)
  window.removeEventListener('pointerup', onSeekPointerUp)
})

// 暴露给 useMusicHotkeys 用：S 键聚焦搜索框
defineExpose({ focusSearchInput, toggleLike, refreshLikeStatus })
</script>

<style scoped>
.music-player {
  position: relative;
  width: 100%;
  min-height: 520px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

/* ============ 主区 ============ */
.player-main {
  display: grid;
  grid-template-columns: minmax(320px, 1.05fr) minmax(300px, 1fr);
  gap: 3rem;
  align-items: center;
  flex: 1;
  padding-bottom: 2.4rem;
}

.player-main-single {
  grid-template-columns: 1fr;
}

/* ---- 黑胶 ---- */
.vinyl-side {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 2rem 0 0;
}

.vinyl {
  position: relative;
  width: 330px;
  height: 330px;
  border-radius: 50%;
  background: radial-gradient(circle at 50% 50%, #161616 0%, #0b0b0b 58%, #000 100%);
  box-shadow:
    inset 0 0 0 4px rgba(255, 255, 255, 0.04),
    inset 0 0 24px rgba(0, 0, 0, 0.9),
    0 26px 60px rgba(0, 0, 0, 0.35);
  overflow: hidden;
}

.vinyl-grooves {
  position: absolute;
  inset: 6%;
  border-radius: 50%;
  background: repeating-radial-gradient(circle at 50% 50%, transparent 0 2px, rgba(255, 255, 255, 0.035) 2px 3px);
  mask-image: radial-gradient(circle at 50% 50%, #000 10%, transparent 95%);
}

.vinyl-shine {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.09) 0%, transparent 30%, transparent 62%, rgba(255, 255, 255, 0.05) 82%);
}

.vinyl-label {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 128px;
  height: 128px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.25);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 0 0 6px rgba(255, 255, 255, 0.08), 0 6px 18px rgba(0, 0, 0, 0.5);
}

.label-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.label-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 3rem;
  font-weight: 900;
  color: rgba(255, 255, 255, 0.92);
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.35);
}

.vinyl-spin {
  animation: vinylRotate 1.8s linear infinite;
}

@keyframes vinylRotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 唱臂 */
.tonearm {
  position: absolute;
  top: 12px;
  right: calc(50% - 118px);
  width: 150px;
  height: 26px;
  transform-origin: 6px 12px;
  transform: rotate(14deg);
  transition: transform 0.6s cubic-bezier(0.23, 1, 0.32, 1);
  z-index: 3;
  pointer-events: none;
}

.tonearm::before {
  content: '';
  position: absolute;
  top: 8px;
  left: 0;
  width: 146px;
  height: 5px;
  border-radius: 4px;
  background: linear-gradient(90deg, #3b3b45, #23232b);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.4);
}

.tonearm::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 30%, #6b6b78, #23232b);
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.5);
}

.tonearm-head {
  position: absolute;
  top: 2px;
  right: -10px;
  width: 16px;
  height: 20px;
  border-radius: 0 40% 40% 0;
  background: linear-gradient(90deg, #4a4a56, #1c1c22);
}

.tonearm-on { transform: rotate(0deg); }

.vinyl-tip {
  margin-top: 1.6rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.68rem;
  letter-spacing: 0.3em;
  font-family: 'Courier New', monospace;
  color: rgba(148, 163, 184, 0.65);
}

.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: rgba(148, 163, 184, 0.6);
}

.dot-on {
  background: #6ee7b7;
  box-shadow: 0 0 10px rgba(110, 231, 183, 0.8);
  animation: dotPulse 1.2s ease-in-out infinite;
}

@keyframes dotPulse { 50% { opacity: 0.4; } }

/* ---- 歌词：只显示当前两行，居中，渐变高亮 ---- */
.lyrics-wrap {
  position: relative;
  height: 320px;
  overflow: hidden;
  padding: 0 1.5rem;
  border-left: 1px solid rgba(148, 163, 184, 0.14);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background: linear-gradient(180deg, rgba(34, 211, 238, 0.04) 0%, rgba(168, 85, 247, 0.06) 50%, rgba(34, 211, 238, 0.04) 100%);
  border-radius: 16px;
  transition: background 0.4s ease;
}
.lyrics-wrap:hover {
  background: linear-gradient(180deg, rgba(34, 211, 238, 0.08) 0%, rgba(168, 85, 247, 0.1) 50%, rgba(34, 211, 238, 0.08) 100%);
}

.lyrics-current {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  text-align: center;
  width: 100%;
}

.lyric-now {
  margin: 0;
  font-size: 1.2rem;
  font-weight: 700;
  color: #f1f5ff;
  text-shadow: 0 0 18px rgba(165, 243, 252, 0.4);
  letter-spacing: 0.06em;
  line-height: 1.5;
  opacity: 1;
  transition: opacity 0.35s ease;
}

.lyric-next {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 500;
  color: rgba(203, 213, 225, 0.5);
  letter-spacing: 0.06em;
  line-height: 1.5;
  transition: opacity 0.35s ease;
}

.lyrics-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.7rem;
  font-size: 0.85rem;
  color: rgba(148, 163, 184, 0.5);
  letter-spacing: 0.1em;
}

.empty-note {
  font-size: 0.66rem;
  letter-spacing: 0.35em;
  color: rgba(148, 163, 184, 0.35);
  font-family: 'Courier New', monospace;
}

/* ============ 底部播放器栏 ============ */
.player-bar {
  position: relative;
  padding: 1.1rem 1.6rem 1rem;
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(10, 12, 32, 0.82) 0%, rgba(14, 14, 38, 0.72) 100%);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  border: 1px solid rgba(148, 163, 184, 0.14);
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.45), inset 0 1px 0 rgba(148, 163, 184, 0.12);
}
@media (max-width: 768px) {
  .player-bar { padding: 0.9rem 0.8rem 0.8rem; border-radius: 14px; }
  .bar-progress { gap: 0.5rem; margin-bottom: 0.6rem; }
  .bar-time { min-width: 36px; font-size: 0.62rem; }
  .vinyl { width: 240px; height: 240px; }
  .vinyl-label { width: 96px; height: 96px; }
  .play-btn { width: 44px; height: 44px; }
  .icon { width: 17px; height: 17px; }
  .quality-select { font-size: 0.55rem; padding: 0.25rem 0.5rem; }
  .vol-slider { width: 48px; }
}

.bar-progress {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 0.9rem;
}

.bar-time {
  font-size: 0.7rem;
  font-family: 'Courier New', monospace;
  color: rgba(148, 163, 184, 0.7);
  min-width: 44px;
  text-align: center;
}

.progress-track {
  flex: 1;
  height: 18px;
  display: flex;
  align-items: center;
  cursor: pointer;
  position: relative;
}

.progress-track::before {
  content: '';
  width: 100%;
  height: 4px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.18);
}

.progress-fill {
  position: absolute;
  left: 0;
  height: 4px;
  border-radius: 999px;
  background: linear-gradient(90deg, #22d3ee, #a855f7);
  box-shadow: 0 0 10px rgba(168, 85, 247, 0.4);
  transition: width 0.12s linear;
}

.progress-knob {
  position: absolute;
  right: -6px;
  top: 50%;
  transform: translateY(-50%);
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #e0e7ff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.5);
  opacity: 0;
  transition: opacity 0.25s;
}

.progress-track:hover .progress-knob { opacity: 1; }
.progress-knob.knob-active { opacity: 1; box-shadow: 0 0 0 4px rgba(168, 85, 247, 0.3); }

/* 音量滑块 */
.vol-slider {
  width: 64px;
  height: 3px;
  accent-color: #22d3ee;
  cursor: pointer;
}

.bar-controls {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.6rem;
  flex-wrap: wrap;
}
/* 小屏改为居中 */
@media (max-width: 768px) {
  .bar-controls { justify-content: center; gap: 0.4rem; }
}

/* 音质下拉 */
.quality-select {
  margin-right: auto;
  padding: 0.32rem 0.6rem;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  background: rgba(12, 14, 34, 0.6);
  color: rgba(203, 213, 225, 0.85);
  font-size: 0.62rem;
  letter-spacing: 0.16em;
  font-family: 'Courier New', monospace;
  cursor: pointer;
  outline: none;
  transition: border-color 0.25s, color 0.25s;
  appearance: none;
  -webkit-appearance: none;
  background-image: linear-gradient(45deg, transparent 50%, currentColor 50%), linear-gradient(135deg, currentColor 50%, transparent 50%);
  background-position: calc(100% - 12px) center, calc(100% - 8px) center;
  background-size: 4px 4px, 4px 4px;
  background-repeat: no-repeat;
  padding-right: 1.6rem;
}

.quality-select:hover { border-color: rgba(103, 232, 249, 0.55); color: #a5f3fc; }
.quality-select:focus { border-color: rgba(168, 85, 247, 0.6); }
.quality-select option { background: #0a0c20; color: #e0e7ff; }

.ctl {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  color: rgba(203, 213, 225, 0.65);
  background: none;
  border: none;
  cursor: pointer;
  padding: 0.35rem;
  border-radius: 10px;
  transition: all 0.25s;
  position: relative;
}

.ctl:hover { color: #f1f5ff; background: rgba(148, 163, 184, 0.1); transform: translateY(-1px); }
.ctl-on { background: rgba(103, 232, 249, 0.12); color: #a5f3fc; }

.icon {
  width: 19px;
  height: 19px;
  display: block;
}

.icon-lg { width: 22px; height: 22px; }

.like-on { color: #fb7185; filter: drop-shadow(0 0 6px rgba(251, 113, 133, 0.55)); }

.ctl-num {
  font-size: 0.62rem;
  font-family: 'Courier New', monospace;
  color: inherit;
}

.ctl-divider {
  width: 1px;
  height: 22px;
  background: rgba(148, 163, 184, 0.2);
  margin: 0 0.3rem;
}

.play-btn {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  border: 1px solid rgba(165, 243, 252, 0.4);
  background: radial-gradient(circle at 32% 28%, #312e81, #0f0f2e 68%);
  color: #eef2ff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 8px 26px rgba(124, 58, 237, 0.4), 0 0 18px rgba(34, 211, 238, 0.2);
  transition: transform 0.25s cubic-bezier(0.23, 1, 0.32, 1), box-shadow 0.3s;
}

.play-btn:hover { transform: scale(1.07); box-shadow: 0 12px 34px rgba(124, 58, 237, 0.55), 0 0 26px rgba(34, 211, 238, 0.3); }

/* ---- 浮层面板（歌单 / 热榜 / 历史 / 搜索） ---- */
.playlist-panel {
  position: absolute;
  left: 0;
  right: 0;
  bottom: calc(100% + 12px);
  border-radius: 16px;
  background: rgba(8, 10, 28, 0.95);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(148, 163, 184, 0.16);
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.55), 0 0 24px rgba(124, 58, 237, 0.15);
  padding: 1rem;
  max-height: 360px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.pl-tabs {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.2rem 0.4rem 0.8rem;
  border-bottom: 1px solid rgba(148, 163, 184, 0.12);
  margin-bottom: 0.5rem;
}

.pl-tab {
  padding: 0.32rem 0.7rem;
  border-radius: 999px;
  border: 1px solid transparent;
  background: none;
  color: rgba(148, 163, 184, 0.7);
  font-size: 0.72rem;
  letter-spacing: 0.06em;
  cursor: pointer;
  transition: all 0.2s;
}

.pl-tab:hover { color: #e0e7ff; background: rgba(148, 163, 184, 0.08); }
.pl-tab.on {
  color: #a5f3fc;
  border-color: rgba(103, 232, 249, 0.45);
  background: rgba(103, 232, 249, 0.1);
  box-shadow: 0 0 12px rgba(34, 211, 238, 0.18);
}

.pl-count {
  margin-left: auto;
  font-size: 0.62rem;
  letter-spacing: 0.22em;
  font-family: 'Courier New', monospace;
  color: rgba(148, 163, 184, 0.55);
}

.pl-list {
  flex: 1;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(148, 163, 184, 0.3) transparent;
}

.pl-list::-webkit-scrollbar { width: 5px; }
.pl-list::-webkit-scrollbar-thumb { background: rgba(148, 163, 184, 0.25); border-radius: 999px; }

.pl-item {
  display: flex;
  align-items: center;
  gap: 0.8rem;
  padding: 0.55rem 0.6rem;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s;
}

.pl-item:hover { background: rgba(148, 163, 184, 0.08); }

.pl-item-on { background: rgba(103, 232, 249, 0.1); }

.pl-idx {
  width: 26px;
  font-size: 0.68rem;
  font-family: 'Courier New', monospace;
  color: rgba(148, 163, 184, 0.5);
  text-align: center;
}

.pl-idx-top { color: #f0abfc; font-weight: 700; }
.pl-item-on .pl-idx { color: #67e8f9; }

.pl-title { flex: 1; font-size: 0.86rem; font-weight: 600; color: #eef2ff; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.pl-artist { font-size: 0.72rem; color: rgba(148, 163, 184, 0.7); }
.pl-dur { font-size: 0.68rem; font-family: 'Courier New', monospace; color: rgba(148, 163, 184, 0.55); }
.pl-play-count { font-size: 0.66rem; color: rgba(244, 114, 182, 0.7); font-family: 'Courier New', monospace; }
.pl-source { font-size: 0.6rem; padding: 0.1rem 0.4rem; border-radius: 4px; border: 1px solid rgba(148, 163, 184, 0.25); }
.pl-source.src-local { color: rgba(110, 231, 183, 0.85); border-color: rgba(110, 231, 183, 0.35); }
.pl-source.src-netease { color: rgba(244, 114, 182, 0.85); border-color: rgba(244, 114, 182, 0.35); }

.pl-empty {
  padding: 1.4rem;
  text-align: center;
  font-size: 0.78rem;
  color: rgba(148, 163, 184, 0.5);
  letter-spacing: 0.08em;
}

.pl-clear {
  display: block;
  margin: 0.6rem auto 0;
  padding: 0.3rem 0.8rem;
  border-radius: 999px;
  border: 1px solid rgba(251, 113, 133, 0.4);
  background: rgba(251, 113, 133, 0.08);
  color: rgba(251, 113, 133, 0.85);
  font-size: 0.66rem;
  cursor: pointer;
  transition: all 0.2s;
}

.pl-clear:hover { background: rgba(251, 113, 133, 0.18); color: #fb7185; }

/* 搜索面板 */
.pl-search-wrap { display: flex; flex-direction: column; gap: 0.6rem; height: 100%; }

.ps-input-row {
  display: flex;
  gap: 0.5rem;
  padding: 0 0.2rem 0.4rem;
}

.ps-input {
  flex: 1;
  padding: 0.4rem 0.8rem;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  background: rgba(12, 14, 34, 0.6);
  color: rgba(226, 232, 248, 0.92);
  font-size: 0.74rem;
  outline: none;
  transition: border-color 0.25s;
}

.ps-input::placeholder { color: rgba(148, 163, 184, 0.5); }
.ps-input:focus { border-color: rgba(103, 232, 249, 0.55); }

.ps-go {
  padding: 0.4rem 0.9rem;
  border-radius: 999px;
  border: 1px solid rgba(103, 232, 249, 0.4);
  background: rgba(8, 51, 68, 0.45);
  color: #a5f3fc;
  font-size: 0.72rem;
  cursor: pointer;
  transition: all 0.25s;
}

.ps-go:hover { background: rgba(14, 83, 108, 0.6); box-shadow: 0 0 12px rgba(34, 211, 238, 0.3); }

/* ---- 评论抽屉 ---- */
.comment-drawer {
  position: absolute;
  right: 1.6rem;
  bottom: calc(100% + 12px);
  width: 340px;
  max-height: 460px;
  border-radius: 16px;
  background: rgba(8, 10, 28, 0.95);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(148, 163, 184, 0.16);
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.55), 0 0 24px rgba(124, 58, 237, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.cd-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.9rem 1rem;
  border-bottom: 1px solid rgba(148, 163, 184, 0.12);
}

.cd-title {
  font-size: 0.78rem;
  font-weight: 600;
  color: #eef2ff;
  letter-spacing: 0.08em;
}

.cd-close {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: none;
  background: rgba(148, 163, 184, 0.1);
  color: rgba(203, 213, 225, 0.8);
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.2s;
}

.cd-close:hover { background: rgba(251, 113, 133, 0.18); color: #fb7185; }

.cd-list {
  flex: 1;
  overflow-y: auto;
  padding: 0.5rem 1rem;
  scrollbar-width: thin;
  scrollbar-color: rgba(148, 163, 184, 0.3) transparent;
}

.cd-list::-webkit-scrollbar { width: 5px; }
.cd-list::-webkit-scrollbar-thumb { background: rgba(148, 163, 184, 0.25); border-radius: 999px; }

.cd-item {
  display: flex;
  gap: 0.7rem;
  padding: 0.6rem 0;
  border-bottom: 1px solid rgba(148, 163, 184, 0.08);
}

.cd-item:last-child { border-bottom: none; }

.cd-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  background: linear-gradient(135deg, #312e81, #0f0f2e);
}

.cd-avatar-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #a5f3fc;
  font-size: 0.82rem;
  font-weight: 700;
}

.cd-main { flex: 1; min-width: 0; }

.cd-user {
  font-size: 0.78rem;
  font-weight: 600;
  color: #e0e7ff;
  margin-bottom: 0.25rem;
}

.cd-time {
  font-size: 0.62rem;
  font-weight: 400;
  color: rgba(148, 163, 184, 0.5);
  margin-left: 0.4rem;
}

.cd-content {
  font-size: 0.82rem;
  color: rgba(203, 213, 225, 0.85);
  line-height: 1.5;
  word-break: break-word;
}

.cd-input-row {
  display: flex;
  gap: 0.4rem;
  padding: 0.7rem 1rem;
  border-top: 1px solid rgba(148, 163, 184, 0.12);
  background: rgba(12, 14, 34, 0.4);
}

.cd-input {
  flex: 1;
  padding: 0.42rem 0.7rem;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  background: rgba(8, 10, 28, 0.7);
  color: #e0e7ff;
  font-size: 0.78rem;
  outline: none;
  transition: border-color 0.25s;
}

.cd-input::placeholder { color: rgba(148, 163, 184, 0.5); }
.cd-input:focus { border-color: rgba(103, 232, 249, 0.55); }

.cd-send {
  padding: 0.42rem 0.9rem;
  border-radius: 999px;
  border: 1px solid rgba(103, 232, 249, 0.4);
  background: rgba(8, 51, 68, 0.55);
  color: #a5f3fc;
  font-size: 0.74rem;
  cursor: pointer;
  transition: all 0.2s;
}

.cd-send:hover:not(:disabled) { background: rgba(14, 83, 108, 0.7); box-shadow: 0 0 10px rgba(34, 211, 238, 0.3); }
.cd-send:disabled { opacity: 0.4; cursor: not-allowed; }

.list-fade-enter-active, .list-fade-leave-active { transition: opacity 0.25s, transform 0.25s; }
.list-fade-enter-from, .list-fade-leave-to { opacity: 0; transform: translateY(8px); }

.drawer-slide-enter-active, .drawer-slide-leave-active { transition: opacity 0.25s, transform 0.25s; }
.drawer-slide-enter-from, .drawer-slide-leave-to { opacity: 0; transform: translateY(8px) translateX(20px); }

/* ---- 响应式（优先大屏） ---- */
@media (max-width: 1100px) {
  .vinyl { width: 260px; height: 260px; }
  .vinyl-label { width: 100px; height: 100px; }
  .tonearm { right: calc(50% - 92px); }
}

@media (max-width: 900px) {
  .player-main { grid-template-columns: 1fr; gap: 1.6rem; }
  .lyrics-wrap { border-left: none; border-top: 1px solid rgba(148, 163, 184, 0.14); padding: 1rem 0 0; height: 240px; }
  .quality-select { display: none; }
  .vol-slider { display: none; }
  .bar-controls { gap: 0.5rem; }
  .comment-drawer { width: calc(100% - 3.2rem); right: 1.6rem; }
}
</style>
