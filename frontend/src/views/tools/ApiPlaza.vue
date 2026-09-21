<template>
  <div class="min-h-screen px-4 md:px-6 py-10">
    <div class="mx-auto max-w-6xl">
      <button class="text-gray-400 hover:text-white mb-6 flex items-center gap-1 text-sm transition-colors" @click="$router.push('/tools')">
        <span>←</span> 返回工具箱
      </button>

      <h1 class="text-3xl font-bold text-gradient-cyber mb-2">60秒 API 广场</h1>
      <p class="text-sm text-gray-500 matrix-text mb-7">
        github-myblog / 60s-main 全量接口在线调试 · 共 {{ total }} 个端点 · 经 /sixty 代理
      </p>

      <!-- 分组切换 -->
      <div class="flex flex-wrap gap-2 mb-7">
        <button v-for="g in groups" :key="g.id" @click="activeGroup = g.id"
          :class="['inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-[11px] transition',
            activeGroup === g.id
              ? 'bg-cyan-500/20 text-cyan-100 border border-cyan-400/40 shadow-[0_0_12px_rgba(34,211,238,0.2)]'
              : 'border border-white/10 text-gray-400 hover:text-white hover:border-white/25']">
          <span>{{ g.icon }}</span>{{ g.name }}
          <span class="px-1.5 rounded-full bg-white/8 text-[10px]">{{ g.apis.length }}</span>
        </button>
      </div>

      <!-- 接口卡片网格 -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <article v-for="api in currentApis" :key="api.path + api.name" class="api-card glass-panel p-4 flex flex-col">
          <div class="flex items-start justify-between gap-2 mb-1">
            <h3 class="text-sm font-bold text-white">{{ api.name }}</h3>
            <span :class="['method', api.method === 'POST' ? 'post' : 'get']">{{ api.method || 'GET' }}</span>
          </div>
          <code class="text-[10px] text-cyan-300/80 font-mono break-all mb-3">{{ api.path }}</code>

          <!-- 参数输入 -->
          <div v-if="api.params?.length" class="space-y-2 mb-3">
            <div v-for="p in api.params" :key="p.key">
              <label class="text-[10px] text-gray-500">{{ p.label }}</label>
              <input v-model="paramStore[key(api, p.key)]" class="api-input" :placeholder="p.placeholder" />
            </div>
          </div>

          <button class="mt-auto self-start text-[11px] px-3.5 py-1.5 rounded-lg bg-cyan-500/15 text-cyan-300 border border-cyan-400/30 hover:bg-cyan-500/25 transition"
                  :disabled="results[api.path]?.loading" @click="invoke(api)">
            {{ results[api.path]?.loading ? '调用中...' : '▶ 调用接口' }}
          </button>

          <!-- 结果区 -->
          <div v-if="results[api.path] && !results[api.path]?.loading" class="mt-3">
            <div v-if="results[api.path].error" class="text-[11px] text-red-400 bg-red-500/10 rounded-lg px-3 py-2">
              {{ results[api.path].error }}
            </div>
            <template v-else>
              <!-- 图片型：直接渲染 -->
              <img v-if="results[api.path].imgUrl" :src="results[api.path].imgUrl" class="rounded-lg max-h-72 mx-auto bg-white/5" />
              <!-- RSS / 文本型 -->
              <pre v-else-if="api.kind === 'rss' || (results[api.path].rawText && !results[api.path].json)"
                   class="api-result text-[10px] max-h-64 overflow-auto whitespace-pre-wrap">{{ results[api.path].rawText?.slice(0, 4000) }}</pre>
              <!-- JSON -->
              <pre v-else-if="results[api.path].json" class="api-result text-[10px] max-h-72 overflow-auto">{{ results[api.path].json }}</pre>
            </template>
          </div>
        </article>
      </div>
    </div>
  </div>
</template>

<script setup>
// ============================================================
// 60秒 API 广场：60s-main 全量接口在线调试
// 参数表单 → GET query / POST JSON body → 结果按类型渲染
// ============================================================
import { ref, reactive, computed } from 'vue'
import { sixtyGroups, SIXTY_BASE } from '@/data/sixtyApis.js'

const groups = sixtyGroups
const total = groups.reduce((n, g) => n + g.apis.length, 0)
const activeGroup = ref(groups[0].id)
const currentApis = computed(() => groups.find(g => g.id === activeGroup.value)?.apis || [])

// 参数值存储：key = path + 参数key
const paramStore = reactive({})
const results = reactive({})

function key(api, k) { return api.path + '|' + k }

// 构造最终路径与查询参数
function buildRequest(api) {
  let path = api.path
  const query = {}
  for (const p of api.params || []) {
    const v = (paramStore[key(api, p.key)] || '').trim()
    if (!v) continue
    if (p.key.startsWith('__path:')) {
      // 路径参数：替换最后一段
      path = path.replace(/\/[^/]+$/, '/' + encodeURIComponent(v))
    } else {
      query[p.key] = v
    }
  }
  const qs = new URLSearchParams(query).toString()
  return { url: SIXTY_BASE + path + (qs ? '?' + qs : ''), query }
}

async function invoke(api) {
  const { url, query } = buildRequest(api)
  results[api.path] = { loading: true, error: '' }

  // 图片型且为 GET：直接用 img 标签加载（二维码/壁纸类）
  if (api.kind === 'image' && api.method !== 'POST') {
    results[api.path] = { loading: false, imgUrl: url }
    return
  }

  try {
    const opts = api.method === 'POST'
      ? { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(query) }
      : {}
    const res = await fetch(url, opts)
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    const ct = res.headers.get('content-type') || ''
    const text = await res.text()

    if (/json/.test(ct)) {
      let pretty
      try { pretty = JSON.stringify(JSON.parse(text), null, 2) } catch { pretty = text }
      results[api.path] = { loading: false, json: pretty }
    } else if (/image/.test(ct)) {
      results[api.path] = { loading: false, imgUrl: url }
    } else {
      results[api.path] = { loading: false, rawText: text }
    }
  } catch (e) {
    results[api.path] = { loading: false, error: e.message || '请求失败（公共实例可能限流，稍后重试）' }
  }
}
</script>

<style scoped>
.method { font-size: 0.6rem; font-family: monospace; padding: 0.1rem 0.45rem; border-radius: 4px; letter-spacing: 0.08em; flex-shrink: 0; }
.method.get { color: #6ee7b7; background: rgba(16,185,129,0.12); border: 1px solid rgba(16,185,129,0.3); }
.method.post { color: #fbbf24; background: rgba(245,158,11,0.12); border: 1px solid rgba(245,158,11,0.35); }

.api-input {
  width: 100%; margin-top: 0.2rem; padding: 0.4rem 0.7rem;
  font-size: 0.75rem; color: #dbeafe;
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 8px; outline: none;
  transition: border-color 0.2s;
}
.api-input:focus { border-color: rgba(34,211,238,0.5); }
.api-input::placeholder { color: rgba(148,163,184,0.35); }

.api-result {
  background: rgba(0,0,0,0.4);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 8px;
  padding: 0.75rem;
  color: #a7f3d0;
  font-family: 'Consolas', monospace;
}
</style>
