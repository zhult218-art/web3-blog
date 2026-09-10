<template>
  <div class="min-h-screen px-4 md:px-6 py-6">
    <div class="mx-auto max-w-3xl">
      <div class="flex items-center gap-3 mb-8">
        <button class="text-gray-400 hover:text-white transition-colors" @click="$router.back()">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M10 19l-7-7m0 0l7-7m-7 7h18"/></svg>
        </button>
        <div>
          <h1 class="text-2xl font-bold text-white">发布内容</h1>
          <p class="text-xs text-gray-500 mt-0.5">分享你的技术见解与创意</p>
        </div>
      </div>

      <div class="glass-panel p-6 md:p-8 space-y-5">
        <!-- Type selector -->
        <div class="flex gap-3">
          <button v-for="t in types" :key="t.key" @click="form.type = t.key"
            :class="['flex-1 py-3 rounded-xl text-sm font-medium transition-all duration-300 border',
              form.type === t.key
                ? 'bg-gradient-to-r from-purple-500/15 to-cyan-500/10 border-purple-400/30 text-white shadow-[0_0_20px_rgba(168,85,247,0.1)]'
                : 'border-white/[0.06] text-gray-500 hover:text-gray-300']">
            <span class="text-lg mr-1.5">{{ t.icon }}</span>
            {{ t.label }}
          </button>
        </div>

        <!-- Category -->
        <div>
          <label class="text-xs text-gray-400 mb-2 block">分类</label>
          <select v-model="form.category" class="web3-input cursor-pointer">
            <option value="">选择分类</option>
            <option v-for="cat in categories" :key="cat" :value="cat">{{ cat }}</option>
          </select>
        </div>

        <!-- Title -->
        <div>
          <label class="text-xs text-gray-400 mb-2 block">标题</label>
          <input v-model="form.title" class="web3-input !text-base" placeholder="给你的内容起个吸引人的标题" maxlength="100" />
          <div class="text-right text-[10px] text-gray-600 mt-1">{{ form.title.length }}/100</div>
        </div>

        <!-- Cover image -->
        <div v-if="form.type === 'article'">
          <label class="text-xs text-gray-400 mb-2 block">封面图片 (可选)</label>
          <div class="border-2 border-dashed border-white/[0.08] rounded-xl p-6 text-center hover:border-purple-400/20 transition-colors cursor-pointer relative overflow-hidden"
            @click="pickCover">
            <input ref="coverInput" type="file" accept="image/jpeg,image/png,image/gif,image/webp" class="hidden" @change="uploadCover" />
            <div v-if="!form.coverImage" class="text-gray-600">
              <svg class="w-8 h-8 mx-auto mb-2 opacity-40" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"/></svg>
              <p class="text-xs">{{ coverUploading ? '上传中...' : '点击上传封面图片' }}</p>
              <p class="text-[10px] text-gray-700 mt-1">支持 jpg / png / gif / webp，最大 500MB</p>
            </div>
            <img v-else :src="form.coverImage" class="w-full h-40 object-cover rounded-lg" />
          </div>
        </div>

        <!-- Video upload (discussion with video) -->
        <div v-if="form.type === 'video'">
          <label class="text-xs text-gray-400 mb-2 block">上传视频 <span class="text-gray-600">(mp4 / webm / mov / mkv / avi / flv，最大 500MB)</span></label>
          <div class="border-2 border-dashed border-white/[0.08] rounded-xl p-6 text-center hover:border-purple-400/20 transition-colors cursor-pointer"
            @click="pickVideo">
            <input ref="videoInput" type="file" accept="video/mp4,video/webm,video/quicktime,video/x-matroska,video/x-msvideo,video/x-flv" class="hidden" @change="uploadVideo" />
            <div v-if="!form.mediaUrl" class="text-gray-600">
              <svg class="w-8 h-8 mx-auto mb-2 opacity-40" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"/></svg>
              <p class="text-xs">{{ videoUploading ? '上传中...' : '点击选择视频文件' }}</p>
              <p class="text-[10px] text-gray-700 mt-1">文件将经过安全扫描：格式白名单 + 魔数校验 + 恶意特征检测</p>
            </div>
            <video v-else :src="form.mediaUrl" controls class="w-full max-h-60 rounded-lg bg-black/40" />
          </div>
          <div v-if="videoUploadProgress > 0 && videoUploadProgress < 100" class="mt-2">
            <div class="h-1.5 rounded-full bg-[#141432] overflow-hidden">
              <div class="h-full bg-gradient-to-r from-purple-500 to-cyan-500 transition-all duration-300" :style="{ width: videoUploadProgress + '%' }"></div>
            </div>
            <p class="text-[10px] text-gray-500 mt-1 text-right">{{ videoUploadProgress }}%</p>
          </div>
        </div>

        <!-- Content -->
        <div>
          <label class="text-xs text-gray-400 mb-2 block">内容</label>
          <textarea v-model="form.content" class="web3-input !min-h-[200px] resize-y" placeholder="写下你的想法...&#10;支持 Markdown 格式" maxlength="10000"></textarea>
          <div class="text-right text-[10px] text-gray-600 mt-1">{{ form.content.length }}/10000</div>
        </div>

        <!-- Tags -->
        <div>
          <label class="text-xs text-gray-400 mb-2 block">标签 (逗号分隔)</label>
          <input v-model="tagsInput" class="web3-input" placeholder="Vue3, Web3, AI (用逗号分隔)" />
          <div class="flex flex-wrap gap-1.5 mt-2">
            <span v-for="tag in tagList" :key="tag" class="text-[10px] px-2 py-0.5 rounded-md bg-purple-500/10 text-purple-300 border border-purple-400/15">
              {{ tag }}
            </span>
          </div>
        </div>

        <!-- Submit -->
        <div class="flex items-center justify-between pt-4">
          <span class="text-[11px] text-gray-600">草稿将自动保存</span>
          <div class="flex gap-3">
            <button class="px-5 py-2.5 rounded-xl text-xs text-gray-400 border border-white/[0.08] hover:bg-[#10102a] transition">存为草稿</button>
            <button class="web3-btn text-xs !px-6 !py-2.5" :disabled="!canSubmit || submitting" @click="submit">
              {{ submitting ? '发布中...' : '立即发布' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// ====================================================
// 发布内容页：支持文章/讨论/视频三种类型，
// 含封面与视频上传（带进度）、标签输入与表单校验
// ====================================================
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { createBlog } from '@/api/blog'
import { createPost } from '@/api/forum'
import { uploadMedia } from '@/api/media'
import { useToastStore } from '@/stores/modules/toast'
import { useAuthStore } from '@/stores/modules/auth'

const router = useRouter()
const toast = useToastStore()
const authStore = useAuthStore()
const submitting = ref(false)
const tagsInput = ref('')
const coverInput = ref(null)
const videoInput = ref(null)
const coverUploading = ref(false)
const videoUploading = ref(false)
const videoUploadProgress = ref(0)

const types = [
  { key: 'article', label: '文章', icon: '📝' },
  { key: 'discussion', label: '讨论', icon: '💬' },
  { key: 'video', label: '视频', icon: '🎬' },
]

const categories = ['前端', '后端', 'Web3', 'AI', 'DevOps', '数据库', '架构设计', '职场', '开源', '其他']

const form = reactive({
  type: 'article',
  category: '',
  title: '',
  content: '',
  coverImage: '',
  summary: '',
  mediaUrl: '',
  mediaType: '',
})

// 标签列表：将标签输入按逗号/空格拆分为数组
const tagList = computed(() => {
  return tagsInput.value.split(',').map(t => t.trim()).filter(Boolean)
})

// 是否允许提交：标题、分类、内容齐全且不在提交中
const canSubmit = computed(() => {
  if (form.type === 'video') {
    return form.title.trim() && form.content.trim() && form.category && form.mediaUrl
  }
  return form.title.trim() && form.content.trim() && form.category
})

// 当前用户显示名：优先昵称，其次用户名，默认「访客」
const currentUserName = computed(() => {
  return authStore.user?.nickname || authStore.user?.username || ''
})

// 触发封面文件选择框
function pickCover() {
  coverInput.value?.click()
}

// 触发视频文件选择框
function pickVideo() {
  videoInput.value?.click()
}

// 拼接媒体资源完整 URL（本地或相对地址补全为绝对地址）
function fullMediaUrl(u) {
  return u && u.startsWith('/') && !u.startsWith('/api') ? `/api${u}` : u
}

// 上传封面图，成功后回填到表单
async function uploadCover(e) {
  const file = e.target.files?.[0]
  if (!file) return
  coverUploading.value = true
  try {
    const res = await uploadMedia(file)
    form.coverImage = fullMediaUrl(res.data?.url || res.url)
    toast.success('封面已上传')
  } catch (err) {
    toast.error(err.response?.data?.message || '封面上传失败')
    e.target.value = ''
  } finally {
    coverUploading.value = false
  }
}

// 上传视频（带上传进度），成功后回填到表单
async function uploadVideo(e) {
  const file = e.target.files?.[0]
  if (!file) return
  videoUploading.value = true
  videoUploadProgress.value = 0
  try {
    const res = await uploadMedia(file, { onUploadProgress: p => { videoUploadProgress.value = p.total ? Math.round((p.loaded / p.total) * 100) : 0 } })
    form.mediaUrl = fullMediaUrl(res.data?.url || res.url)
    form.mediaType = 'video'
    toast.success('视频上传成功，已通过安全扫描')
  } catch (err) {
    toast.error(err.response?.data?.message || '视频上传失败')
    e.target.value = ''
    form.mediaUrl = ''
  } finally {
    videoUploading.value = false
    videoUploadProgress.value = 0
  }
}

// 提交发布：文章走 createBlog，讨论/视频走 createPost，成功后跳转详情
async function submit() {
  if (!canSubmit.value) return
  submitting.value = true
  const authorName = currentUserName.value
  try {
    if (form.type === 'article') {
      await createBlog({
        title: form.title,
        content: form.content,
        category: form.category,
        tags: tagsInput.value,
        coverImage: form.coverImage,
        summary: form.content.slice(0, 200),
        authorName,
      })
      toast.success('文章发布成功！')
    } else {
      await createPost({
        title: form.title,
        content: form.content,
        category: form.category,
        authorName,
        mediaUrl: form.mediaUrl,
        mediaType: form.mediaType,
      })
      toast.success(form.type === 'video' ? '视频发布成功！' : '帖子发布成功！')
    }
    router.push('/community')
  } catch {
    toast.error('发布失败，请先登录')
  } finally {
    submitting.value = false
  }
}
</script>
