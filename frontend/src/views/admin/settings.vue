<template>
  <div class="min-h-screen py-8 px-6">
    <div class="max-w-7xl mx-auto">
      <h1 class="text-xl font-bold text-white mb-6"><span class="text-gradient-cyber">System</span> Settings</h1>
      <div class="glass-panel p-6 max-w-2xl">
        <div class="space-y-6">
          <div>
            <h3 class="text-sm font-bold text-white mb-3">General</h3>
            <div class="space-y-4">
              <div>
                <label class="text-xs text-gray-500 block mb-1.5">Site Name</label>
                <input class="web3-input" value="Aurora-朱" />
              </div>
              <div>
                <label class="text-xs text-gray-500 block mb-1.5">Site Description</label>
                <input class="web3-input" value="Web3极简未来科技风个人综合技术门户" />
              </div>
              <div>
                <label class="text-xs text-gray-500 block mb-1.5">Meta Keywords</label>
                <input class="web3-input" value="web3, blog, forum, shop, quant, metaverse" />
              </div>
            </div>
          </div>
          <div class="border-t border-white/[0.06] pt-6">
            <h3 class="text-sm font-bold text-white mb-3">Feature Toggles</h3>
            <div class="space-y-3">
              <label v-for="opt in featureToggles" :key="opt.key" class="flex items-center justify-between cursor-pointer group">
                <span class="text-sm text-gray-400 group-hover:text-white/80 transition">{{ opt.label }}</span>
                <button @click="opt.enabled = !opt.enabled"
                  class="relative w-10 h-5 rounded-full transition-colors duration-300"
                  :class="opt.enabled ? 'bg-purple-500' : 'bg-white/10'">
                  <span class="absolute top-0.5 left-0.5 w-4 h-4 rounded-full bg-white transition-transform duration-300"
                    :class="opt.enabled ? 'translate-x-5' : 'translate-x-0'"></span>
                </button>
              </label>
            </div>
          </div>
          <div class="border-t border-white/[0.06] pt-6">
            <button class="web3-btn px-6 py-2.5 flex items-center gap-2" :disabled="saving" @click="saveSettings">
              <span v-if="saving" class="inline-block h-3.5 w-3.5 rounded-full border-2 border-white/30 border-t-white animate-spin"></span>
              {{ saving ? '保存中...' : 'Save Changes' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// ====================================================
// 站点设置：基础信息表单 + 功能开关，
// 保存到本地 localStorage
// ====================================================
import { ref, reactive } from 'vue'
import { useToastStore } from '@/stores/modules/toast'

const toast = useToastStore()

// 站点基础设置表单（站点名/简介等）
const form = reactive({
  siteName: 'Aurora-朱',
  description: 'Web3极简未来科技风个人综合技术门户',
  keywords: 'web3, blog, forum, shop, quant, metaverse'
})

// 功能开关配置列表
const featureToggles = ref([
  { key: 'quant', label: 'Quant Trading Module', enabled: true },
  { key: 'jarvis', label: 'Jarvis Voice Assistant', enabled: true },
  { key: 'metaverse', label: '3D Metaverse Scene', enabled: true },
  { key: 'forum', label: 'Forum Module', enabled: true },
  { key: 'shop', label: 'E-commerce Shop', enabled: true },
  { key: 'media', label: 'Media Center', enabled: true },
])

const saving = ref(false)

// 保存设置内容到本地 localStorage
async function saveSettings() {
  saving.value = true
  try {
    localStorage.setItem('web3_settings', JSON.stringify({
      ...form,
      features: featureToggles.value.reduce((acc, f) => { acc[f.key] = f.enabled; return acc }, {})
    }))
    toast.success('设置保存成功！')
  } catch {
    toast.error('保存失败')
  } finally { saving.value = false }
}
</script>
