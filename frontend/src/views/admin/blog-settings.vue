<template>
  <div class="min-h-screen py-8 px-4 md:px-6">
    <div class="max-w-5xl mx-auto">
      <div class="flex items-center justify-between gap-4 mb-6">
        <h1 class="text-xl font-bold text-white"><span class="text-gradient-cyber">Blog</span> Settings</h1>
        <button class="web3-btn text-xs !px-5 !py-2" :disabled="saving" @click="saveAll">
          {{ saving ? '保存中...' : '保存全部配置' }}
        </button>
      </div>

      <div class="text-xs text-gray-600 mb-4">
        全站配置为 JSON 键值存储，公开接口 <code class="text-cyan-400">GET /blog/settings</code> 直接透出；
        修改后点击「保存全部配置」批量写入。
      </div>

      <div class="space-y-5">
        <div v-for="item in items" :key="item.key" class="glass-panel p-5">
          <div class="flex items-center justify-between mb-2">
            <label class="text-sm font-medium text-white">{{ item.label }}
              <code class="text-xs text-gray-500 ml-2">{{ item.key }}</code>
            </label>
            <button v-if="item.key in settings && !builtinKeys.includes(item.key)" class="text-xs text-red-400 hover:text-red-300"
              @click="removeKey(item.key)">删除</button>
          </div>
          <textarea v-model="texts[item.key]" rows="5" class="web3-input text-xs font-mono w-full resize-y" :placeholder="item.placeholder"></textarea>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// ====================================================
// 站点配置管理：作者信息 / 打赏二维码 / 页脚友链 / ICP 备案
// 按 JSON key-value 展示与编辑，批量保存
// ====================================================
import { ref, reactive, onMounted } from 'vue'
import { getBlogSettings, saveAdminSettings } from '@/api/blog'
import { useToastStore } from '@/stores/modules/toast'

const toast = useToastStore()
const settings = reactive({})
const texts = reactive({})
const saving = ref(false)

// 内置配置项说明（含默认值提示）
const builtinKeys = ['author', 'donate', 'footer_links', 'icp']
const items = [
  { key: 'author', label: '作者信息', placeholder: '{"nickname":"站名","slogan":"一句话签名","avatar":"https://...","email":"","github":"","weibo":""}' },
  { key: 'donate', label: '打赏二维码', placeholder: '{"btc_qr":"https://...","eth_qr":"https://..."}' },
  { key: 'footer_links', label: '页脚友链分组', placeholder: '{"默认分组":[{"name":"站点名","url":"https://..."}]}' },
  { key: 'icp', label: 'ICP 备案 / 版权', placeholder: '{"icp_no":"京ICP备XXXXXXXX号","copyright":"© 2024-2026"}' }
]

async function load() {
  try {
    const s = (await getBlogSettings()) || {}
    for (const k of Object.keys(s)) settings[k] = s[k]
    for (const it of items) texts[it.key] = s[it.key] ? JSON.stringify(formatValue(s[it.key]), null, 2) : ''
  } catch {}
}

function formatValue(v) {
  if (typeof v === 'string') { try { return JSON.parse(v) } catch { return v } }
  return v
}

function removeKey(key) {
  delete settings[key]
  delete texts[key]
  toast.success('已移除（保存后生效）')
}

async function saveAll() {
  const body = {}
  for (const it of items) {
    if (!texts[it.key]) continue
    try { body[it.key] = JSON.stringify(JSON.parse(texts[it.key])) }
    catch { toast.error(`${it.key} 不是合法 JSON`); return }
  }
  saving.value = true
  try {
    await saveAdminSettings(body)
    toast.success('配置已保存')
    load()
  } catch {}
  finally { saving.value = false }
}

onMounted(load)
</script>