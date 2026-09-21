<template>
  <!-- AI 聊天面板：消息列表 + 输入框，调用 Vibe-Research /vr/chat NDJSON 流式接口 -->
  <div>
    <div class="space-y-3 max-h-[500px] overflow-y-auto mb-4 p-4 rounded-xl bg-[#1a1208] border border-amber-800/15">
      <div v-if="chatMessages.length" v-for="msg in chatMessages" :key="msg.id"
        :class="['flex gap-3', msg.role === 'user' ? 'justify-end' : 'justify-start']">
        <div :class="['max-w-[80%] p-3 rounded-xl text-sm', msg.role === 'user' ? 'bg-purple-500/15 text-purple-200 border border-purple-400/15' : 'bg-[#1a1208] text-amber-200/70 border border-amber-800/20']">
          <span v-if="msg.content" class="whitespace-pre-wrap">{{ msg.content }}</span>
          <span v-if="msg.streaming" class="inline-block w-1.5 h-4 bg-amber-400 animate-pulse align-middle ml-0.5"></span>
        </div>
      </div>
      <div v-else class="text-center text-xs text-amber-700/50 py-8">
        输入股票代码或量化问题，AI助手将为您分析
      </div>
    </div>
    <div class="flex gap-2">
      <input v-model="chatInput" class="web3-input flex-1 text-sm" placeholder="如: 分析贵州茅台(600519)的投资价值..." @keydown.enter="sendChat" :disabled="chatLoading" />
      <button class="web3-btn text-sm" :disabled="chatLoading" @click="sendChat">
        {{ chatLoading ? '思考中...' : '发送' }}
      </button>
    </div>
  </div>
</template>

<script setup>
// ChatPanel：从 quant/index.vue 抽出的 AI 聊天面板
// 内部持有 chatMessages / chatInput / chatLoading；通过 v-model:config 接收父组件 aiConfig
// 调用 Vibe-Research 服务（/vr/chat，8900）流式输出 NDJSON 回复
import { ref } from 'vue'

const props = defineProps({
  // AI 配置对象（v-model:config），由父组件 AiConfigPanel 同步过来
  config: { type: Object, default: () => ({ baseURL: '', apiKey: '', model: '' }) }
})

// 聊天状态
const chatMessages = ref([])
const chatInput = ref('')
const chatLoading = ref(false)

// 发送聊天消息到 Vibe-Research 服务（/vr/chat，8900），流式输出回复
async function sendChat() {
  if (!chatInput.value.trim() || chatLoading.value) return
  const msg = chatInput.value.trim()
  chatMessages.value.push({ id: Date.now(), role: 'user', content: msg })
  chatInput.value = ''
  chatLoading.value = true

  const replyId = Date.now() + 1
  chatMessages.value.push({ id: replyId, role: 'assistant', content: '', streaming: true })

  const cfg = props.config || {}
  const saved = cfg.baseURL && cfg.model
  if (!saved) {
    const target = chatMessages.value.find(m => m.id === replyId)
    if (target) {
      target.content = '请先在「⚙ AI 模型设置」填写 Base URL 与模型名称，然后重试。'
      target.streaming = false
    }
    chatLoading.value = false
    return
  }

  try {
    const resp = await fetch('/vr/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        messages: [{ role: 'user', content: msg }],
        context: '当前页面: 量化交易平台。请用中文回答，关注A股行情与量化分析。',
        llm: {
          provider: '',
          baseURL: cfg.baseURL,
          apiKey: cfg.apiKey,
          model: cfg.model,
        },
      }),
    })

    if (!resp.ok) {
      let detail = `请求失败 (HTTP ${resp.status})`
      try {
        const err = await resp.json()
        if (err?.detail) detail = String(err.detail)
      } catch {}
      throw new Error(detail)
    }

    const reader = resp.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let done = false

    while (!done) {
      const { value, done: streamDone } = await reader.read()
      done = streamDone
      buffer += decoder.decode(value || new Uint8Array(), { stream: !done })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        const trimmed = line.trim()
        if (!trimmed) continue
        let ev
        try { ev = JSON.parse(trimmed) } catch { continue }
        const target = chatMessages.value.find(m => m.id === replyId)
        if (!target) continue
        if (ev.type === 'delta' && ev.content) {
          target.content += ev.content
        } else if (ev.type === 'tool' && ev.name) {
          target.content += (target.content ? '\n' : '') + `[工具: ${ev.name}]`
        } else if (ev.type === 'error') {
          target.content += (target.content ? '\n' : '') + `⚠ ${ev.message || '分析出错'}`
        }
      }
    }
    const target = chatMessages.value.find(m => m.id === replyId)
    if (target) target.streaming = false
    if (!target || !target.content.trim()) {
      if (target) target.content = '分析完成，但未返回有效内容。'
    }
  } catch (e) {
    const target = chatMessages.value.find(m => m.id === replyId)
    if (target) {
      target.streaming = false
      target.content = '分析失败: ' + (e.message || '网络错误') + '。请检查 Vibe-Research 服务 (8900) 与模型配置。'
    }
  } finally {
    chatLoading.value = false
  }
}
</script>

<style scoped>
/* amber 家族通配命中（与父组件保持视觉一致） */
[class*="text-amber-100"], [class*="text-amber-200"] { color: #dbe7f3 !important; }
[class*="text-amber-300"], [class*="text-amber-400"] { color: #3ae2ee !important; }
[class*="text-amber-500"], [class*="text-amber-600"] { color: #8fc0d9 !important; }
[class*="text-amber-700"] { color: #6d8fb0 !important; }
[class*="bg-[#1a1208]"] { background: #0b1422 !important; }
[class*="border-amber-800"] { border-color: rgba(120, 170, 220, 0.16) !important; }
[class*="font-serif"] { font-family: 'SF Pro Text', 'PingFang SC', 'Microsoft YaHei', sans-serif !important; }
</style>
