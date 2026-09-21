<template>
  <!-- 完整令牌一次性展示弹窗：从 ai-proxy.vue 抽出 -->
  <div v-if="token" class="fixed inset-0 z-[110] bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
    <div class="glass-panel w-full max-w-md p-6 space-y-4 border-web3-accent/30">
      <h3 class="text-base font-bold text-amber-300">⚠ 请立即保存你的完整令牌</h3>
      <p class="text-xs text-gray-400 leading-relaxed">这是唯一一次展示机会，关闭后只能看到前缀。请复制并妥善保管：</p>
      <code class="block matrix-text text-xs break-all bg-black/40 border border-white/10 rounded-lg p-3 text-emerald-300 select-all">{{ token }}</code>
      <div class="flex justify-end gap-2">
        <button class="web3-btn text-xs !px-4" @click="copy">📋 复制</button>
        <button class="web3-btn text-xs !px-4" @click="dismiss">我已保存</button>
      </div>
    </div>
  </div>
</template>

<script setup>
// TokenRevealModal：从 ai-proxy.vue 抽出的完整令牌一次性展示弹窗
// 通过 v-model 接收 token 字符串（非空即显示）；清空 token 即关闭
import { useToastStore } from '@/stores/modules/toast'

const toast = useToastStore()

const props = defineProps({
  // 令牌字符串（v-model），非空时显示弹窗
  token: { type: String, default: '' }
})

const emit = defineEmits(['update:token'])

// 复制令牌到剪贴板
function copy() {
  navigator.clipboard.writeText(props.token).then(() => toast.success('已复制到剪贴板'))
}

// 关闭弹窗：清空 token
function dismiss() {
  emit('update:token', '')
}
</script>
