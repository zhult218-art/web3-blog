<template>
  <!-- AI 配置面板：OpenAI 兼容接口配置，控制显示/隐藏与父组件双向同步 -->
  <div v-if="modelValue" class="scroll-card-sm p-4 mb-4 space-y-3">
    <p class="text-[10px] text-amber-600/60">配置 OpenAI 兼容的模型接口（保存到本地），用于量化问答分析</p>
    <div class="grid grid-cols-1 md:grid-cols-3 gap-3">
      <div>
        <label class="text-[10px] text-amber-600/60 mb-1 block">Base URL</label>
        <input v-model="local.baseURL" class="web3-input text-xs font-mono" placeholder="如 http://127.0.0.1:8801/v1" @input="sync" />
      </div>
      <div>
        <label class="text-[10px] text-amber-600/60 mb-1 block">API Key</label>
        <input v-model="local.apiKey" type="password" class="web3-input text-xs font-mono" placeholder="sk-..." @input="sync" />
      </div>
      <div>
        <label class="text-[10px] text-amber-600/60 mb-1 block">模型</label>
        <input v-model="local.model" class="web3-input text-xs font-mono" placeholder="如 deepseek-chat" @input="sync" />
      </div>
    </div>
    <div class="flex justify-end">
      <button class="text-[10px] px-3 py-1 rounded-lg bg-[#332314] text-amber-400 border border-amber-400/20 hover:bg-amber-500/20 transition" @click="onSave">保存配置</button>
    </div>
  </div>
</template>

<script setup>
// AiConfigPanel：从 quant/index.vue 抽出的 AI 配置面板
// 通过 v-model 双向同步显示状态；通过 v-model:config 双向同步 aiConfig 配置对象
// 持久化策略保留 sessionStorage 修复：apiKey 仅会话级，baseURL/model 持久化到 localStorage
import { reactive, watch, onMounted } from 'vue'
import { useToastStore } from '@/stores/modules/toast'

const toast = useToastStore()

const props = defineProps({
  // 控制面板显示/隐藏（v-model）
  modelValue: { type: Boolean, default: false },
  // AI 配置对象（v-model:config），结构 { baseURL, apiKey, model }
  config: { type: Object, default: () => ({ baseURL: '', apiKey: '', model: '' }) }
})

const emit = defineEmits(['update:modelValue', 'update:config', 'save'])

// 本地副本，避免直接 mutate props.config；通过 sync 上抛 update:config
const local = reactive({ baseURL: '', apiKey: '', model: '' })

// 父 -> 子：配置对象变化时同步到本地
watch(() => props.config, (v) => {
  if (!v) return
  if (v.baseURL !== local.baseURL) local.baseURL = v.baseURL
  if (v.apiKey !== local.apiKey) local.apiKey = v.apiKey
  if (v.model !== local.model) local.model = v.model
}, { deep: true, immediate: true })

// 子 -> 父：本地编辑同步回父组件
function sync() {
  emit('update:config', { ...local })
}

// 从 localStorage 读取 AI 模型配置
// 2026-09-17 修订：apiKey 不再持久化到 localStorage（防 XSS 偷取 + 防磁盘明文泄露），
// 只保存 baseURL 和 model；apiKey 改为会话内存（reactive ref）+ sessionStorage，
// 浏览器关闭即清空，用户每次会话需要重新输入。
function loadAiConfig() {
  try {
    const saved = JSON.parse(localStorage.getItem('quant_llm_config') || '{}')
    local.baseURL = saved.baseURL || ''
    local.apiKey = sessionStorage.getItem('quant_llm_apikey') || ''
    local.model = saved.model || ''
    sync()
  } catch {}
}

// 保存 AI 模型配置：baseURL/model 进 localStorage；apiKey 仅进 sessionStorage
function onSave() {
  localStorage.setItem('quant_llm_config', JSON.stringify({ baseURL: local.baseURL, model: local.model }))
  const key = local.apiKey
  if (key) {
    sessionStorage.setItem('quant_llm_apikey', key)
  } else {
    sessionStorage.removeItem('quant_llm_apikey')
  }
  toast.success('AI 模型配置已保存（apiKey 仅本会话保留）')
  sync()
  emit('save')
}

onMounted(() => {
  loadAiConfig()
})
</script>

<style scoped>
/* 与父组件 quant/index.vue 视觉一致的局部样式（不影响父组件） */
.scroll-card-sm {
  background: linear-gradient(150deg, #0b1320 0%, #091120 100%);
  border: 1px solid rgba(120, 170, 220, 0.11);
  border-radius: 9px;
}
/* amber 家族通配命中（与父组件保持视觉一致） */
[class*="text-amber-100"], [class*="text-amber-200"] { color: #dbe7f3 !important; }
[class*="text-amber-300"], [class*="text-amber-400"] { color: #3ae2ee !important; }
[class*="text-amber-500"], [class*="text-amber-600"] { color: #8fc0d9 !important; }
[class*="text-amber-700"] { color: #6d8fb0 !important; }
[class*="bg-[#332314]"] { background: #101c30 !important; }
[class*="border-amber-400"] { border-color: rgba(34, 211, 238, 0.40) !important; }
[class*="font-serif"] { font-family: 'SF Pro Text', 'PingFang SC', 'Microsoft YaHei', sans-serif !important; }
</style>
