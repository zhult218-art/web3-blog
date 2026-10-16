<template>
  <!-- 社区发布弹窗：覆盖在社区广场上层，不再另开页面 -->
  <transition name="modal">
    <div v-if="modelValue" class="publish-overlay" @click.self="$emit('update:modelValue', false)">
      <div class="publish-panel">
        <div class="panel-head">
          <div>
            <h2 class="panel-title">发布内容</h2>
            <p class="panel-sub">分享你的技术见解与创意</p>
          </div>
          <button class="close-btn" @click="$emit('update:modelValue', false)" aria-label="关闭">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
          </button>
        </div>

        <div class="panel-body">
          <!-- 类型选择 -->
          <div class="type-row">
            <button v-for="t in types" :key="t.key" @click="form.type = t.key"
              :class="['type-btn', form.type === t.key ? 'active' : '']">
              <span class="type-icon">{{ t.icon }}</span>{{ t.label }}
            </button>
          </div>

          <!-- 分类 -->
          <div class="field">
            <label>分类</label>
            <select v-model="form.category" class="web3-input cursor-pointer">
              <option value="">选择分类</option>
              <option v-for="cat in categories" :key="cat" :value="cat">{{ cat }}</option>
            </select>
          </div>

          <!-- 标题 -->
          <div class="field">
            <label>标题</label>
            <input v-model="form.title" class="web3-input !text-base" placeholder="给你的内容起个吸引人的标题" maxlength="100" />
            <div class="counter">{{ form.title.length }}/100</div>
          </div>

          <!-- 封面（仅文章） -->
          <div v-if="form.type === 'article'" class="field">
            <label>封面图片 (可选)</label>
            <div class="upload-zone" @click="pickCover">
              <input ref="coverInput" type="file" accept="image/*" class="hidden" @change="uploadCover" />
              <div v-if="!form.coverImage" class="upload-placeholder">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"/></svg>
                <p class="text-xs mt-1">{{ coverUploading ? '上传中...' : '点击上传封面' }}</p>
              </div>
              <img v-else :src="form.coverImage" class="w-full h-40 object-cover rounded-lg" />
            </div>
          </div>

          <!-- 视频上传 -->
          <div v-if="form.type === 'video'" class="field">
            <label>上传视频</label>
            <div class="upload-zone" @click="pickVideo">
              <input ref="videoInput" type="file" accept="video/*" class="hidden" @change="uploadVideo" />
              <div v-if="!form.mediaUrl" class="upload-placeholder">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"/></svg>
                <p class="text-xs mt-1">{{ videoUploading ? '上传中...' : '点击选择视频' }}</p>
              </div>
              <video v-else :src="form.mediaUrl" controls class="w-full max-h-60 rounded-lg bg-black/40" />
            </div>
            <div v-if="videoUploadProgress > 0 && videoUploadProgress < 100" class="mt-2">
              <div class="h-1.5 rounded-full bg-[#141432] overflow-hidden">
                <div class="h-full bg-gradient-to-r from-purple-500 to-cyan-500" :style="{ width: videoUploadProgress + '%' }"></div>
              </div>
              <p class="text-[10px] text-gray-500 mt-1 text-right">{{ videoUploadProgress }}%</p>
            </div>
          </div>

          <!-- 内容 -->
          <div class="field">
            <div class="field-label-row">
              <label>内容</label>
              <button type="button" class="ai-polish-btn" :disabled="aiBusy || !form.content.trim()" @click="polishContent">
                {{ aiBusy ? '✦ 润色中…' : '✦ AI 润色' }}
              </button>
            </div>
            <textarea v-model="form.content" class="web3-input !min-h-[160px] resize-y" placeholder="写下你的想法...&#10;支持 Markdown" maxlength="10000"></textarea>
            <div v-if="aiPreview" class="ai-preview">
              <div class="ai-preview-head">
                <span>✦ AI 润色结果预览（可替换后再自行修改）</span>
                <span class="ai-preview-actions">
                  <button type="button" class="ai-apply" @click="applyPolish">替换正文</button>
                  <button type="button" class="ai-discard" @click="aiPreview = ''">放弃</button>
                </span>
              </div>
              <div class="ai-preview-text">{{ aiPreview }}<span v-if="aiBusy" class="animate-pulse">▍</span></div>
            </div>
            <div class="counter">{{ form.content.length }}/10000</div>
          </div>

          <!-- 标签 -->
          <div class="field">
            <label>标签 (逗号分隔)</label>
            <input v-model="tagsInput" class="web3-input" placeholder="Vue3, Web3, AI" />
            <div class="tags-wrap" v-if="tagList.length">
              <span v-for="tag in tagList" :key="tag" class="tag-chip">#{{ tag }}</span>
            </div>
          </div>

          <!-- 操作 -->
          <div class="actions">
            <span class="hint-text">草稿将自动保存</span>
            <div class="btn-group">
              <button class="ghost-btn" @click="$emit('update:modelValue', false)">取消</button>
              <button class="web3-btn text-xs !px-6 !py-2.5" :disabled="!canSubmit || submitting" @click="submit">
                {{ submitting ? '发布中...' : '立即发布' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
// ====================================================
// 社区发布弹窗：文章/讨论/视频，含封面视频上传
// 由社区广场页直接调用，不再跳转独立页面
// ====================================================
import { ref, reactive, computed } from 'vue'
import { createBlog } from '@/api/blog'
import { createPost } from '@/api/forum'
import { uploadMedia } from '@/api/media'
import { aiChatStream } from '@/api/freeAi'
import { useToastStore } from '@/stores/modules/toast'
import { useAuthStore } from '@/stores/modules/auth'
import { hasPerm } from '@/utils/permissions'

const props = defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue', 'published'])

const toast = useToastStore()
const authStore = useAuthStore()
const submitting = ref(false)
const tagsInput = ref('')
const coverInput = ref(null)
const videoInput = ref(null)
const coverUploading = ref(false)
const videoUploading = ref(false)
const videoUploadProgress = ref(0)

// ── AI 润色（免费通道，流式）：预览后可一键替换正文 ──
const aiBusy = ref(false)
const aiPreview = ref('')
let aiSeq = 0

const types = [
  { key: 'article', label: '文章', icon: '📝' },
  { key: 'discussion', label: '讨论', icon: '💬' },
  { key: 'video', label: '视频', icon: '🎬' },
]
const categories = ['前端', '后端', 'Web3', 'AI', 'DevOps', '数据库', '架构设计', '职场', '开源', '其他']

const form = reactive({
  type: 'article', category: '', title: '', content: '',
  coverImage: '', summary: '', mediaUrl: '', mediaType: '',
})

const tagList = computed(() => tagsInput.value.split(',').map(t => t.trim()).filter(Boolean))
const canSubmit = computed(() => {
  if (form.type === 'video') return form.title.trim() && form.content.trim() && form.category && form.mediaUrl
  return form.title.trim() && form.content.trim() && form.category
})
const currentUserName = computed(() => authStore.user?.nickname || authStore.user?.username || '')

function pickCover() { coverInput.value?.click() }
function pickVideo() { videoInput.value?.click() }
function fullMediaUrl(u) { return u && u.startsWith('/') && !u.startsWith('/api') ? `/api${u}` : u }

async function uploadCover(e) {
  const file = e.target.files?.[0]
  if (!file) return
  coverUploading.value = true
  try {
    const res = await uploadMedia(file)
    form.coverImage = fullMediaUrl(res.data?.url || res.url)
    toast.success('封面已上传')
  } catch (err) { toast.error(err.response?.data?.message || '封面上传失败'); e.target.value = '' }
  finally { coverUploading.value = false }
}

async function uploadVideo(e) {
  const file = e.target.files?.[0]
  if (!file) return
  videoUploading.value = true; videoUploadProgress.value = 0
  try {
    const res = await uploadMedia(file, { onUploadProgress: p => { videoUploadProgress.value = p.total ? Math.round((p.loaded / p.total) * 100) : 0 } })
    form.mediaUrl = fullMediaUrl(res.data?.url || res.url)
    form.mediaType = 'video'
    toast.success('视频上传成功')
  } catch (err) { toast.error(err.response?.data?.message || '视频上传失败'); e.target.value = ''; form.mediaUrl = '' }
  finally { videoUploading.value = false; videoUploadProgress.value = 0 }
}

async function polishContent() {
  const text = form.content.trim()
  if (!text || aiBusy.value) return
  const seq = ++aiSeq
  aiBusy.value = true
  aiPreview.value = ''
  try {
    const ctx = [
      form.title ? `标题：${form.title}` : '',
      form.category ? `分类：${form.category}` : '',
    ].filter(Boolean).join('\n')
    await aiChatStream(
      [{ role: 'user', content: text.slice(0, 5000) }],
      d => { if (seq === aiSeq) aiPreview.value += d },
      {
        system: '你是社区内容编辑助手。请润色用户提供的 Markdown 正文：保持原意与 Markdown 结构，改善表达流畅度与专业性，修正错别字；不要添加事实性信息，不要输出解释或客套，直接输出润色后的全文。' + (ctx ? `\n\n背景信息：\n${ctx}` : ''),
        max_tokens: 1600,
      }
    )
    if (seq === aiSeq) aiPreview.value = aiPreview.value || '（未返回内容）'
  } catch (e) {
    if (seq === aiSeq) aiPreview.value = '（AI 润色失败：' + (e?.message || '服务暂时不可用') + '）'
  } finally {
    if (seq === aiSeq) aiBusy.value = false
  }
}

function applyPolish() {
  if (aiPreview.value && !aiPreview.value.startsWith('（')) {
    form.content = aiPreview.value.slice(0, 10000)
    toast.success('已替换正文，可继续编辑')
  }
  aiPreview.value = ''
}

async function submit() {
  if (!canSubmit.value) return
  // 发帖权限：互动类默认拥有，管理员可收回（-post）
  if (authStore.isLoggedIn && !hasPerm(authStore.user, 'post')) {
    toast.warning('你已被限制发帖，请联系管理员')
    return
  }
  submitting.value = true
  try {
    const authorName = currentUserName.value
    if (form.type === 'article') {
      await createBlog({
        title: form.title, content: form.content, category: form.category,
        tags: tagsInput.value, coverImage: form.coverImage,
        summary: form.content.slice(0, 200), authorName,
      })
      toast.success('文章发布成功！')
    } else {
      const res = await createPost({
        title: form.title, content: form.content, category: form.category,
        authorName, mediaUrl: form.mediaUrl, mediaType: form.mediaType,
      })
      toast.success(res?.data?.status === 'pending'
        ? '发布成功，等待管理员审核通过后将公开展示'
        : (form.type === 'video' ? '视频发布成功！' : '帖子发布成功！'))
    }
    emit('published')
    resetForm()
    emit('update:modelValue', false)
  } catch { toast.error('发布失败，请先登录') }
  finally { submitting.value = false }
}

function resetForm() {
  form.type = 'article'; form.category = ''; form.title = ''; form.content = ''
  form.coverImage = ''; form.mediaUrl = ''; form.mediaType = ''
  tagsInput.value = ''
  aiSeq++
  aiPreview.value = ''
  aiBusy.value = false
}
</script>

<style scoped>
.publish-overlay {
  position: fixed; inset: 0; z-index: 200;
  background: rgba(6, 6, 14, 0.75);
  backdrop-filter: blur(8px);
  display: flex; align-items: center; justify-content: center;
  padding: 1rem;
}
.publish-panel {
  width: 100%; max-width: 680px; max-height: 92vh; overflow-y: auto;
  background: linear-gradient(180deg, #0e0e26, #0a0a1e);
  border: 1px solid rgba(168, 85, 247, 0.25);
  border-radius: 20px;
  box-shadow: 0 0 60px rgba(168, 85, 247, 0.2), 0 30px 80px rgba(0,0,0,0.6);
}
.panel-head {
  display: flex; justify-content: space-between; align-items: flex-start;
  padding: 1.5rem 1.75rem 1rem;
  border-bottom: 1px solid rgba(255,255,255,0.06);
}
.panel-title { font-size: 1.25rem; font-weight: 700; color: #fff; }
.panel-sub { font-size: 0.72rem; color: rgba(255,255,255,0.4); margin-top: 2px; }
.close-btn { color: rgba(255,255,255,0.5); transition: color 0.2s; background: none; border: none; cursor: pointer; }
.close-btn:hover { color: #fff; }
.panel-body { padding: 1.5rem 1.75rem 1.75rem; }

.type-row { display: flex; gap: 0.75rem; margin-bottom: 1.25rem; }
.type-btn {
  flex: 1; padding: 0.75rem; border-radius: 12px; font-size: 0.85rem;
  border: 1px solid rgba(255,255,255,0.08); background: rgba(255,255,255,0.02);
  color: rgba(255,255,255,0.5); cursor: pointer; transition: all 0.3s;
}
.type-btn.active {
  background: linear-gradient(135deg, rgba(168,85,247,0.15), rgba(34,211,238,0.1));
  border-color: rgba(168, 85, 247, 0.4); color: #fff;
  box-shadow: 0 0 20px rgba(168,85,247,0.15);
}
.type-icon { margin-right: 4px; }

.field { margin-bottom: 1.1rem; }
.field > label { display: block; font-size: 0.72rem; color: rgba(255,255,255,0.55); margin-bottom: 6px; }
.counter { text-align: right; font-size: 10px; color: rgba(255,255,255,0.3); margin-top: 4px; }

/* ── AI 润色 ── */
.field-label-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.field-label-row > label { font-size: 0.72rem; color: rgba(255,255,255,0.55); }
.ai-polish-btn {
  font-size: 0.68rem; padding: 3px 10px; border-radius: 8px; color: #d8b4fe;
  background: rgba(168,85,247,0.12); border: 1px solid rgba(168,85,247,0.3); cursor: pointer; transition: all 0.2s;
}
.ai-polish-btn:hover:not(:disabled) { background: rgba(168,85,247,0.22); }
.ai-polish-btn:disabled { opacity: 0.45; cursor: not-allowed; }
.ai-preview {
  margin-top: 10px; border: 1px solid rgba(168,85,247,0.25); border-radius: 12px;
  background: rgba(168,85,247,0.06); padding: 10px 12px;
}
.ai-preview-head { display: flex; justify-content: space-between; align-items: center; font-size: 10px; color: #c4b5fd; margin-bottom: 6px; gap: 8px; }
.ai-preview-actions { display: flex; gap: 6px; }
.ai-apply { font-size: 10px; padding: 2px 8px; border-radius: 6px; background: rgba(34,211,238,0.15); border: 1px solid rgba(34,211,238,0.35); color: #67e8f9; cursor: pointer; }
.ai-apply:hover { background: rgba(34,211,238,0.25); }
.ai-discard { font-size: 10px; padding: 2px 8px; border-radius: 6px; background: none; border: 1px solid rgba(255,255,255,0.12); color: rgba(255,255,255,0.5); cursor: pointer; }
.ai-preview-text { font-size: 0.75rem; line-height: 1.6; color: rgba(255,255,255,0.85); white-space: pre-wrap; max-height: 220px; overflow-y: auto; }

.upload-zone {
  border: 2px dashed rgba(255,255,255,0.08); border-radius: 12px;
  padding: 1.5rem; text-align: center; cursor: pointer;
  transition: border-color 0.3s;
}
.upload-zone:hover { border-color: rgba(168, 85, 247, 0.3); }
.upload-placeholder { color: rgba(255,255,255,0.35); }

.tags-wrap { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 8px; }
.tag-chip { font-size: 10px; padding: 2px 8px; border-radius: 6px; background: rgba(168,85,247,0.1); color: #c4b5fd; border: 1px solid rgba(168,85,247,0.2); }

.actions { display: flex; justify-content: space-between; align-items: center; margin-top: 1.5rem; padding-top: 1rem; border-top: 1px solid rgba(255,255,255,0.06); }
.hint-text { font-size: 0.68rem; color: rgba(255,255,255,0.3); }
.btn-group { display: flex; gap: 0.75rem; }
.ghost-btn { padding: 0.6rem 1.25rem; border-radius: 10px; font-size: 0.75rem; color: rgba(255,255,255,0.5); border: 1px solid rgba(255,255,255,0.08); background: none; cursor: pointer; transition: all 0.2s; }
.ghost-btn:hover { background: rgba(255,255,255,0.05); color: #fff; }

.modal-enter-active, .modal-leave-active { transition: opacity 0.3s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
.modal-enter-active .publish-panel, .modal-leave-active .publish-panel { transition: transform 0.3s cubic-bezier(0.34,1.56,0.64,1), opacity 0.3s; }
.modal-enter-from .publish-panel, .modal-leave-to .publish-panel { transform: scale(0.92) translateY(20px); opacity: 0; }
</style>
