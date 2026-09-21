<template>
  <!-- 头像更换弹窗：从 profile/index.vue 抽出 -->
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="open" class="fixed inset-0 z-50 flex items-center justify-center p-4" @click.self="close">
        <div class="absolute inset-0 bg-black/60 backdrop-blur-sm"></div>
        <div class="isekai-card relative z-10 w-full max-w-sm opacity-0" ref="cardRef">
          <h3 class="text-base font-bold text-amber-200 mb-5">更换头像</h3>
          <div class="space-y-4">
            <div class="space-y-1.5">
              <label class="isekai-label">头像 URL</label>
              <input v-model="url" class="isekai-input" placeholder="https://example.com/avatar.jpg" />
            </div>
            <div v-if="url" class="flex justify-center">
              <div class="w-20 h-20 rounded-full overflow-hidden border-2 border-amber-400/30">
                <img :src="url" class="w-full h-full object-cover" />
              </div>
            </div>
            <div class="text-[11px] text-slate-600 text-center">推荐使用 <a href="https://dicebear.com" target="_blank" class="text-cyan-400 hover:underline">DiceBear</a> 生成头像</div>
          </div>
          <div class="flex justify-end gap-2 mt-5">
            <button class="text-xs text-slate-500 hover:text-slate-300 px-4 py-2 transition" @click="close">取消</button>
            <button class="isekai-btn-primary text-xs" :disabled="saving" @click="save">{{ saving ? '保存中...' : '确认更换' }}</button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
// AvatarModal：从 profile/index.vue 抽出的头像更换弹窗
// 通过 v-model:open 控制显示；保存成功后 emit('saved') 通知父组件刷新头像动画
import { ref, watch, nextTick } from 'vue'
import { updateAvatar } from '@/api/user'
import { useAuthStore } from '@/stores/modules/auth'
import { useToastStore } from '@/stores/modules/toast'
import { animate } from 'animejs'

const auth = useAuthStore()
const toast = useToastStore()

const props = defineProps({
  // 控制弹窗显示（v-model:open）
  open: { type: Boolean, default: false }
})

const emit = defineEmits(['update:open', 'saved'])

const url = ref('')
const saving = ref(false)
const cardRef = ref(null)

// 弹窗打开时做入场动画
watch(() => props.open, (v) => {
  if (v) {
    nextTick(() => {
      if (cardRef.value) animate(cardRef.value, { opacity: [0, 1], duration: 300, ease: 'outCubic' })
    })
  }
})

function close() {
  emit('update:open', false)
}

async function save() {
  if (!url.value) { toast.warning('请输入头像 URL'); return }
  saving.value = true
  try {
    const res = await updateAvatar(url.value)
    auth.updateUser(res.data || res)
    emit('update:open', false)
    toast.success('头像更换成功')
    emit('saved')
  } catch { toast.error('头像更新失败') }
  finally { saving.value = false }
}
</script>
