<template>
  <div class="twin-page">
    <!-- HERO -->
    <section class="hero">
      <div class="hero-back"><PageBack label="返回首页" to="/" /></div>
      <div class="hero-bg">
        <div class="orb o1"></div>
        <div class="orb o2"></div>
        <div class="grid-lines"></div>
      </div>
      <div class="hero-inner">
        <div class="badge fade">● DIGITAL TWIN · REALTIME TOPOLOGY</div>
        <h1 class="title fade">世界之眼 · 服务数字孪生</h1>
        <p class="sub fade">
          把网关、12 个微服务与全部基础设施映射成一座可旋转的三维拓扑：
          节点颜色即健康状态，链路粒子即调用流量，停掉的服务会当场熄灭。
        </p>
        <div class="stats fade">
          <span class="stat run"><i></i>运行中 {{ stat.run }}</span>
          <span class="stat stop"><i></i>已停止 {{ stat.stop }}</span>
          <span class="stat unknown"><i></i>未知 {{ stat.unknown }}</span>
          <span class="stat total">共 {{ stat.total }} 个探测目标</span>
        </div>
      </div>
    </section>

    <!-- 三维拓扑 -->
    <section class="stage">
      <div class="stage-head">
        <div>
          <h2>实时拓扑</h2>
          <p>数据来源 <code>/admin/services/status</code> · 30 秒轮询 · 最后更新 {{ lastRefresh }}</p>
        </div>
        <button class="refresh" :disabled="loading" @click="doRefresh">
          {{ loading ? '探测中...' : '立即探测' }}
        </button>
      </div>

      <div class="stage-body">
        <ServiceTwin
          :nodes="nodes"
          :links="LINKS"
          :height="620"
          :selected-id="selectedId"
          @select="onSelect"
        />

        <aside class="panel">
          <!-- 选中详情 -->
          <div v-if="selected" class="detail">
            <div class="d-head">
              <span class="d-dot" :class="'st-' + (selected.status || 'UNKNOWN').toLowerCase()"></span>
              <div>
                <p class="d-name">{{ selected.label }}</p>
                <p class="d-sub">{{ selected.sub }}</p>
              </div>
              <button class="d-close" @click="selectedId = ''">×</button>
            </div>
            <dl class="d-list">
              <div><dt>层级</dt><dd>{{ LAYER_NAME[selected.layer] }}</dd></div>
              <div><dt>状态</dt><dd :class="'tx-' + (selected.status || 'UNKNOWN').toLowerCase()">{{ STATUS_TEXT[selected.status] || '状态未知' }}</dd></div>
              <div><dt>标识</dt><dd class="mono">{{ selected.id }}</dd></div>
            </dl>
            <p class="d-role">{{ ROLE[selected.id] || '该节点未在注册表中单独描述。' }}</p>
            <p v-if="selected.status === 'UNKNOWN'" class="d-note">
              未纳入健康探测（基础设施与辅助服务由外部进程托管），显示为中性色。
            </p>
          </div>

          <!-- 默认：节点清单 -->
          <div v-else class="roster">
            <div v-for="g in roster" :key="g.layer" class="roster-group">
              <p class="rg-title"><span>L{{ g.layer }}</span>{{ LAYER_NAME[g.layer] }}<em>{{ g.items.length }}</em></p>
              <button
                v-for="n in g.items"
                :key="n.id"
                class="rg-item"
                @click="selectedId = n.id"
              >
                <span class="rg-dot" :class="'st-' + (n.status || 'UNKNOWN').toLowerCase()"></span>
                <span class="rg-label">{{ n.label }}</span>
                <span class="rg-sub">{{ n.sub }}</span>
              </button>
            </div>
            <p class="roster-tip">点击右侧条目或三维节点查看详情</p>
          </div>
        </aside>
      </div>
    </section>

    <!-- 说明 -->
    <section class="note-sec">
      <div v-for="c in NOTES" :key="c.title" class="note-card">
        <h3>{{ c.title }}</h3>
        <p>{{ c.desc }}</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import ServiceTwin from '@/components/three/ServiceTwin.vue'
import PageBack from '@/components/PageBack.vue'
import { SERVICE_REGISTRY } from '@/api/admin'
import { useServiceHealth } from '@/composables/useServiceHealth'

const { serviceStatus, loading, refresh } = useServiceHealth()

const LAYER_NAME = { 1: '客户端层', 2: '网关层', 3: '业务服务层', 4: '基础设施 / 辅助服务' }
const STATUS_TEXT = { RUNNING: '运行中', STOPPED: '已停止', UNKNOWN: '状态未知' }

// 基础设施与辅助服务：不参与 /admin/services/status 探测，统一显示为未知
const INFRA = [
  { id: 'mysql', label: 'MySQL', sub: '业务库 · :3306' },
  { id: 'redis', label: 'Redis', sub: '缓存 · :6379' },
  { id: 'rabbitmq', label: 'RabbitMQ', sub: '消息队列 · :5672' },
  { id: 'nacos', label: 'Nacos', sub: '注册配置中心' },
  { id: 'minio', label: 'MinIO', sub: '对象存储' },
  { id: 'supabase', label: 'Supabase', sub: '云端 Postgres' },
  { id: 'quant-py', label: '量化引擎', sub: 'FastAPI · :9006' },
  { id: 'whisper', label: '语音识别', sub: 'faster-whisper · :9011' },
  { id: 'netease', label: '网易云代理', sub: 'Node · :3000' },
]

// 节点职责说明（详情面板用）
const ROLE = {
  web: '浏览器端的 Vue3 单页应用，所有请求先经网关再分流到各微服务。',
  gateway: '统一入口：路由转发、JWT 鉴权、限流熔断、跨域与链路追踪起点。',
  'user-service': '注册登录、JWT 签发、权限与角色、公会体系。',
  'blog-service': '博客文章、分类标签、友链、公告与前台站点记录。',
  'forum-service': '社区帖子、评论、点赞，以及「极光广场」群聊与 WebSocket。',
  'shop-service': '商品、订单、支付回调与库存扣减。',
  'media-service': '相册、音乐馆、美甲作品与媒体资源元数据。',
  'quant-service': '行情采集、策略与回测结果，K 线数据落 Supabase。',
  'tool-service': '在线工具集合与工具详情页数据。',
  'software-service': '软件包与安装说明的元数据管理。',
  'resource-service': '资源库、下载统计与附件元数据。',
  'ai-proxy-service': '统一代理多家大模型（DeepSeek / Groq / Ollama 等），屏蔽密钥。',
  'jarvis-service': '星图助手：规则路由 + 专用 Agent 编排 + 工作流执行。',
  'admin-service': '后台仪表盘、操作审计、访问统计与服务健康探测。',
  mysql: '关系型主存储，每个业务服务各自独立库表。',
  redis: '登录态、热点缓存与分布式锁。',
  rabbitmq: '跨服务异步解耦，削峰填谷。',
  nacos: '服务注册发现与配置中心，节点上下线由它广播。',
  minio: '图片、音视频、软件包等对象存储。',
  supabase: '云端 Postgres：博客文章与 K 线历史，前端直连 + RLS。',
  'quant-py': 'Python 量化计算服务，负责指标计算与历史数据回填。',
  whisper: '本地语音转写服务，为笔记与音视频提供字幕。',
  netease: '自建网易云 API 代理，提供搜索与播放地址。',
}

const NOTES = [
  { title: '颜色即状态', desc: '绿=运行中、红=已停止、灰=未知。状态来自 admin-service 的真实端口探测，不是写死的示意数据。' },
  { title: '粒子即流量', desc: '每条链路上循环前进的光点代表一次调用；目标服务停止后，链路会变暗并把粒子降到近乎静止。' },
  { title: '探测边界', desc: 'MySQL / Redis / Nacos 等基础设施与 Python、Node 辅助服务由外部进程托管，不参与探测，统一显示为灰色。' },
]

const bizServices = SERVICE_REGISTRY.filter(s => s.name !== 'gateway')

const statusOf = name => serviceStatus.value[name] || 'UNKNOWN'

const nodes = computed(() => [
  { id: 'web', label: '星空浏览器', sub: 'Vue3 SPA', layer: 1, status: 'RUNNING' },
  { id: 'gateway', label: 'API 网关', sub: ':8080', layer: 2, status: statusOf('gateway') },
  ...bizServices.map(s => ({
    id: s.name, label: s.label, sub: `:${s.port}`, layer: 3, status: statusOf(s.name),
  })),
  ...INFRA.map(i => ({ ...i, layer: 4, status: 'UNKNOWN' })),
])

// 调用关系：入口 → 网关 → 各服务 → 依赖的基础设施
const LINKS = [
  { from: 'web', to: 'gateway' },
  ...bizServices.map(s => ({ from: 'gateway', to: s.name })),
  ...bizServices.map(s => ({ from: s.name, to: 'mysql' })),
  { from: 'gateway', to: 'nacos' },
  { from: 'user-service', to: 'redis' },
  { from: 'blog-service', to: 'redis' },
  { from: 'quant-service', to: 'redis' },
  { from: 'forum-service', to: 'rabbitmq' },
  { from: 'shop-service', to: 'rabbitmq' },
  { from: 'media-service', to: 'rabbitmq' },
  { from: 'blog-service', to: 'supabase' },
  { from: 'blog-service', to: 'minio' },
  { from: 'media-service', to: 'minio' },
  { from: 'software-service', to: 'minio' },
  { from: 'quant-service', to: 'quant-py' },
  { from: 'jarvis-service', to: 'whisper' },
  { from: 'ai-proxy-service', to: 'whisper' },
  { from: 'media-service', to: 'netease' },
]

const selectedId = ref('')
const lastRefresh = ref('--')

const selected = computed(() => nodes.value.find(n => n.id === selectedId.value) || null)

const stat = computed(() => {
  const probe = nodes.value.filter(n => n.layer === 3 || n.id === 'gateway')
  return {
    total: probe.length,
    run: probe.filter(n => n.status === 'RUNNING').length,
    stop: probe.filter(n => n.status === 'STOPPED').length,
    unknown: probe.filter(n => n.status === 'UNKNOWN').length,
  }
})

const roster = computed(() =>
  [1, 2, 3, 4]
    .map(layer => ({ layer, items: nodes.value.filter(n => n.layer === layer) }))
    .filter(g => g.items.length)
)

function onSelect(n) {
  selectedId.value = n ? n.id : ''
}

async function doRefresh() {
  await refresh()
  lastRefresh.value = new Date().toLocaleTimeString('zh-CN')
}
</script>

<style scoped>
.twin-page { max-width: 1400px; margin: 0 auto; padding: 0 20px 64px; }

/* ── HERO ── */
.hero { position: relative; padding: 68px 0 42px; overflow: hidden; }
.hero-back { position: relative; z-index: 2; margin-bottom: 22px; }
.hero-bg { position: absolute; inset: -80px -40px 0; pointer-events: none; }
.orb { position: absolute; border-radius: 50%; filter: blur(70px); opacity: 0.4; }
.o1 { width: 320px; height: 320px; left: 4%; top: 6%; background: #6d5cff; }
.o2 { width: 260px; height: 260px; right: 8%; top: 24%; background: #22d3ee; opacity: 0.28; }
.grid-lines {
  position: absolute; inset: 0;
  background-image: linear-gradient(rgba(140,160,200,0.07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(140,160,200,0.07) 1px, transparent 1px);
  background-size: 46px 46px;
  mask-image: radial-gradient(ellipse at 50% 30%, #000 35%, transparent 78%);
}
.hero-inner { position: relative; z-index: 2; }
.badge {
  display: inline-block; font-size: 11px; letter-spacing: 0.16em;
  color: #7dd3fc; border: 1px solid rgba(125, 211, 252, 0.28);
  background: rgba(125, 211, 252, 0.07); border-radius: 999px; padding: 5px 12px;
}
.title {
  margin: 16px 0 12px; font-size: clamp(28px, 4.4vw, 46px); font-weight: 800;
  color: #fff; letter-spacing: -0.02em;
}
.sub { max-width: 720px; font-size: 14px; line-height: 1.85; color: #9aa8c0; }
.stats { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 22px; }
.stat {
  display: inline-flex; align-items: center; gap: 7px; font-size: 12px;
  padding: 6px 13px; border-radius: 999px;
  background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.07); color: #b6c2d6;
}
.stat i { width: 7px; height: 7px; border-radius: 50%; background: currentColor; }
.stat.run { color: #4ade80; }
.stat.stop { color: #f87171; }
.stat.unknown { color: #94a3b8; }
.stat.total { color: #8ea3c7; }

/* ── 舞台 ── */
.stage { margin-top: 8px; }
.stage-head {
  display: flex; align-items: flex-end; justify-content: space-between; gap: 16px;
  margin-bottom: 14px; flex-wrap: wrap;
}
.stage-head h2 { font-size: 20px; font-weight: 700; color: #fff; }
.stage-head p { margin-top: 5px; font-size: 12px; color: #6b7b93; }
.stage-head code {
  font-family: ui-monospace, Consolas, monospace; font-size: 11px;
  padding: 2px 6px; border-radius: 5px; background: rgba(125,211,252,0.08); color: #7dd3fc;
}
.refresh {
  font-size: 12px; padding: 8px 18px; border-radius: 10px; color: #cbe6ff;
  background: rgba(125,211,252,0.1); border: 1px solid rgba(125,211,252,0.25);
  transition: all 0.2s;
}
.refresh:hover:not(:disabled) { background: rgba(125,211,252,0.2); }
.refresh:disabled { opacity: 0.5; cursor: default; }

.stage-body { display: grid; grid-template-columns: minmax(0, 1fr) 320px; gap: 16px; align-items: start; }

/* ── 侧栏 ── */
.panel {
  border-radius: 18px; border: 1px solid rgba(255,255,255,0.07);
  background: linear-gradient(180deg, rgba(16,22,40,0.9), rgba(8,11,22,0.9));
  padding: 16px; max-height: 620px; overflow-y: auto;
}
.panel::-webkit-scrollbar { width: 6px; }
.panel::-webkit-scrollbar-thumb { background: rgba(140,160,200,0.22); border-radius: 3px; }

.detail .d-head { display: flex; align-items: flex-start; gap: 10px; }
.d-dot { width: 10px; height: 10px; border-radius: 50%; margin-top: 5px; flex: none; }
.d-dot.st-running { background: #34d399; box-shadow: 0 0 8px #34d399; }
.d-dot.st-stopped { background: #f87171; box-shadow: 0 0 8px #f87171; }
.d-dot.st-unknown { background: #94a3b8; }
.d-name { font-size: 15px; font-weight: 700; color: #fff; }
.d-sub { font-size: 11px; color: #7b8ba4; margin-top: 3px; }
.d-close {
  margin-left: auto; width: 22px; height: 22px; border-radius: 6px; color: #7b8ba4;
  background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.08);
  line-height: 1; font-size: 15px; transition: all 0.2s;
}
.d-close:hover { color: #fff; background: rgba(255,255,255,0.12); }
.d-list { margin: 16px 0 12px; display: grid; gap: 1px; background: rgba(255,255,255,0.05); border-radius: 10px; overflow: hidden; }
.d-list > div { display: flex; justify-content: space-between; gap: 10px; padding: 9px 12px; background: rgba(10,14,26,0.86); }
.d-list dt { font-size: 11px; color: #6b7b93; }
.d-list dd { font-size: 12px; color: #dbe5f4; }
.d-list dd.mono { font-family: ui-monospace, Consolas, monospace; font-size: 11px; color: #8ea3c7; }
.tx-running { color: #34d399 !important; }
.tx-stopped { color: #f87171 !important; }
.tx-unknown { color: #94a3b8 !important; }
.d-role { font-size: 12.5px; line-height: 1.85; color: #a7b4c9; }
.d-note {
  margin-top: 10px; padding: 9px 11px; border-radius: 9px; font-size: 11.5px; line-height: 1.7;
  color: #93a3bc; background: rgba(148,163,184,0.08); border: 1px solid rgba(148,163,184,0.16);
}

.roster-group + .roster-group { margin-top: 16px; }
.rg-title {
  display: flex; align-items: center; gap: 7px; font-size: 11px; color: #7b8ba4;
  margin-bottom: 7px; letter-spacing: 0.04em;
}
.rg-title span {
  font-family: ui-monospace, Consolas, monospace; font-size: 10px; padding: 2px 5px;
  border-radius: 4px; background: rgba(125,211,252,0.1); color: #7dd3fc;
}
.rg-title em { margin-left: auto; font-style: normal; color: #56657c; }
.rg-item {
  width: 100%; display: grid; grid-template-columns: 9px 1fr auto; align-items: center; gap: 9px;
  padding: 7px 9px; border-radius: 8px; text-align: left;
  border: 1px solid transparent; transition: all 0.18s;
}
.rg-item:hover { background: rgba(125,211,252,0.07); border-color: rgba(125,211,252,0.16); }
.rg-dot { width: 7px; height: 7px; border-radius: 50%; }
.rg-dot.st-running { background: #34d399; }
.rg-dot.st-stopped { background: #f87171; }
.rg-dot.st-unknown { background: #64748b; }
.rg-label { font-size: 12.5px; color: #dbe5f4; }
.rg-sub { font-size: 10.5px; color: #6b7b93; font-family: ui-monospace, Consolas, monospace; }
.roster-tip { margin-top: 16px; font-size: 11px; color: #56657c; text-align: center; }

/* ── 说明 ── */
.note-sec { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 14px; margin-top: 26px; }
.note-card {
  padding: 18px; border-radius: 14px; background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.06);
}
.note-card h3 { font-size: 13.5px; font-weight: 700; color: #e6edf8; margin-bottom: 8px; }
.note-card p { font-size: 12.5px; line-height: 1.8; color: #93a3bc; }

/* ── 响应式 ── */
@media (max-width: 1080px) {
  .stage-body { grid-template-columns: 1fr; }
  .panel { max-height: 360px; }
}
@media (max-width: 640px) {
  .twin-page { padding: 0 14px 48px; }
  .hero { padding: 44px 0 30px; }
}
</style>