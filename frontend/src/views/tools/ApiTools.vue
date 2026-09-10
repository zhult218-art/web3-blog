<template>
  <div class="min-h-screen px-4 md:px-6 py-10">
    <div class="mx-auto max-w-5xl">
      <!-- 头部 -->
      <PageBack label="返回工具箱" to="/tools" class="mb-5" />
      <h1 class="text-3xl font-bold mb-2 text-gradient-cyber">第三方 API 工具中心</h1>
      <p class="text-sm text-gray-500 matrix-text mb-4">FreeAPI · 万维易源 · 山河云 — 聚合免费接口，密钥由网关服务端注入</p>

      <!-- 密钥状态 -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-3 mb-8">
        <div class="glass-panel-sm p-4 flex items-center gap-3">
          <span class="w-2 h-2 rounded-full bg-green-400 shadow-[0_0_8px_rgba(52,211,153,0.9)]"></span>
          <div>
            <p class="text-xs text-gray-400">FreeAPI (j8y.cn)</p>
            <p class="text-xs text-green-300 mt-0.5">AppKey 已配置 · 服务端注入</p>
          </div>
        </div>
        <div class="glass-panel-sm p-4 flex items-center gap-3">
          <span class="w-2 h-2 rounded-full bg-amber-400/80"></span>
          <div>
            <p class="text-xs text-gray-400">万维易源 ShowAPI</p>
            <p class="text-xs text-amber-200/80 mt-0.5">需注册并填写 appKey（网关环境变量 SHOWAPI_APP_KEY）</p>
          </div>
        </div>
        <div class="glass-panel-sm p-4 flex items-center gap-3">
          <span class="w-2 h-2 rounded-full bg-green-400 shadow-[0_0_8px_rgba(52,211,153,0.9)]"></span>
          <div>
            <p class="text-xs text-gray-400">山河云 (shanhe.kim)</p>
            <p class="text-xs text-green-300 mt-0.5">公开接口免密钥 · Token 接口可透传 apikey</p>
          </div>
        </div>
      </div>

      <!-- 平台 Tab -->
      <div class="flex flex-wrap gap-2 mb-6">
        <button v-for="t in tabs" :key="t.id" @click="activeTab = t.id"
          :class="['px-3 py-1.5 rounded-full text-xs transition border', activeTab === t.id
            ? 'bg-purple-500/25 text-purple-200 border-purple-400/40 shadow-[0_0_14px_rgba(168,85,247,0.25)]'
            : 'border-white/10 text-gray-500 hover:text-white hover:border-white/25']">
          {{ t.label }} <span class="opacity-60">{{ t.count }}</span>
        </button>
      </div>

      <!-- 工具卡片 -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-10">
        <div v-for="tool in visibleTools" :key="tool.id" class="glass-panel p-5">
          <div class="flex items-start justify-between mb-1">
            <div class="flex items-center gap-2.5">
              <span class="text-xl">{{ tool.icon }}</span>
              <div>
                <h3 class="font-semibold text-white text-sm">{{ tool.name }}</h3>
                <p class="text-[11px] text-gray-500 mt-0.5">{{ tool.desc }}</p>
              </div>
            </div>
            <span :class="['text-[10px] px-2 py-0.5 rounded-full border shrink-0',
              tool.status === 'ok' ? 'text-green-300 border-green-400/30 bg-green-500/10'
              : tool.status === 'key' ? 'text-amber-200 border-amber-400/30 bg-amber-500/10'
              : 'text-gray-400 border-white/10 bg-[#0e0e26]']">{{ statusText(tool.status) }}</span>
          </div>

          <!-- 参数区 -->
          <div class="mt-3 space-y-2">
            <div v-for="f in tool.fields || []" :key="f.key" class="flex gap-2">
              <label class="w-16 shrink-0 text-[11px] text-gray-500 flex items-center">{{ f.label }}</label>
              <select v-if="f.type === 'select'" v-model="forms[tool.id][f.key]" class="web3-input flex-1 text-xs !py-1.5 cursor-pointer">
                <option v-for="o in f.options" :key="o.value" :value="o.value">{{ o.label }}</option>
              </select>
              <input v-else v-model="forms[tool.id][f.key]" class="web3-input flex-1 text-xs !py-1.5"
                :placeholder="f.placeholder || ''" @keyup.enter="callTool(tool)" />
            </div>
          </div>

          <div class="mt-3 flex items-center gap-3">
            <button class="web3-btn text-xs !px-5" :disabled="loading === tool.id" @click="callTool(tool)">
              {{ loading === tool.id ? '查询中...' : '查询' }}
            </button>
            <span v-if="tool.note" class="text-[10px] text-gray-600">{{ tool.note }}</span>
          </div>

          <!-- 结果区 -->
          <div v-if="results[tool.id]" class="mt-4 rounded-xl border border-white/10 bg-black/30 p-4">
            <div class="flex items-center justify-between mb-2">
              <span class="text-[11px] text-green-300">{{ results[tool.id].ok ? '✓ 查询成功' : '✗ 查询失败' }}</span>
              <div class="flex gap-2">
                <button class="text-[10px] text-gray-400 hover:text-white transition" @click="toggleRaw(tool.id)">{{ results[tool.id].showRaw ? '结构化视图' : '原始 JSON' }}</button>
                <button class="text-[10px] text-cyan-400 hover:text-cyan-300 transition" @click="copyResult(tool.id)">📋 复制</button>
              </div>
            </div>

            <!-- 原始 JSON -->
            <pre v-if="results[tool.id].showRaw" class="text-[11px] text-green-300 font-mono whitespace-pre-wrap break-all max-h-72 overflow-y-auto">{{ results[tool.id].raw }}</pre>

            <!-- 结构化 -->
            <template v-else-if="results[tool.id].ok && results[tool.id].view">
              <!-- KV -->
              <div v-if="results[tool.id].view.kind === 'kv'" class="grid grid-cols-1 sm:grid-cols-2 gap-x-4 gap-y-1.5">
                <div v-for="row in results[tool.id].view.rows" :key="row.l" class="flex gap-2 text-xs">
                  <span class="text-gray-500 shrink-0">{{ row.l }}</span>
                  <span class="text-gray-200 break-all">{{ row.v }}</span>
                </div>
              </div>
              <!-- Items -->
              <div v-else-if="results[tool.id].view.kind === 'items'" class="space-y-1.5">
                <div v-for="(it, i) in results[tool.id].view.items" :key="i" class="text-xs text-gray-300 leading-relaxed">{{ it }}</div>
              </div>
              <!-- List -->
              <div v-else-if="results[tool.id].view.kind === 'list'" class="space-y-1.5 max-h-72 overflow-y-auto">
                <a v-for="(it, i) in results[tool.id].view.rows" :key="i" :href="it.link" target="_blank" rel="noopener"
                  class="block text-xs text-gray-300 hover:text-cyan-300 transition">
                  <span class="text-gray-600 mr-2">{{ i + 1 }}</span>{{ it.t }}
                  <span v-if="it.s" class="text-[10px] text-gray-600 ml-2">{{ it.s }}</span>
                </a>
              </div>
              <!-- Images -->
              <div v-else-if="results[tool.id].view.kind === 'images'" class="flex flex-wrap gap-3">
                <img v-for="(u, i) in results[tool.id].view.urls" :key="i" :src="u" class="h-32 rounded-lg border border-white/10 object-cover" loading="lazy" />
              </div>
              <!-- Multi KV --> 
              <div v-else-if="results[tool.id].view.kind === 'multi-kv'" class="space-y-4">
                <div v-for="(group, gi) in results[tool.id].view.groups" :key="gi">
                  <p v-if="group.title" class="text-[11px] text-purple-300 mb-1.5">{{ group.title }}</p>
                  <div class="grid grid-cols-2 gap-x-4 gap-y-1.5">
                    <div v-for="row in group.rows" :key="row.l" class="flex gap-2 text-xs">
                      <span class="text-gray-500 shrink-0">{{ row.l }}</span>
                      <span class="text-gray-200">{{ row.v }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </template>
            <p v-else class="text-[11px] text-red-400/90">{{ results[tool.id].msg }}</p>
          </div>
        </div>
      </div>

      <!-- 通用调用面板 -->
      <div class="glass-panel p-5 mb-10">
        <h3 class="font-semibold text-white text-sm mb-1">🧩 通用调用面板</h3>
        <p class="text-[11px] text-gray-500 mb-4">适用于参数未确认 / 上游不稳定的接口：指定平台与路径，任意参数透传，返回原始 JSON。</p>
        <div class="flex flex-wrap gap-2 items-center mb-3">
          <select v-model="generic.platform" class="web3-input !w-36 text-xs !py-1.5 cursor-pointer">
            <option value="j8y">j8y (api_path)</option>
            <option value="showapi">showapi (6-1)</option>
            <option value="shanhe">shanhe (接口名)</option>
          </select>
          <input v-model="generic.path" class="web3-input flex-1 min-w-[160px] text-xs !py-1.5 font-mono" placeholder="如 weather / 105-31 / 星座" />
          <button class="web3-btn text-xs !px-5" :disabled="genericLoading" @click="callGeneric">{{ genericLoading ? '调用中...' : '调用' }}</button>
        </div>
        <div class="grid grid-cols-2 md:grid-cols-4 gap-2">
          <div v-for="(v, i) in generic.params" :key="i" class="flex gap-1">
            <input v-model="generic.params[i].k" class="web3-input flex-1 text-[11px] !py-1.5 font-mono" placeholder="key" />
            <input v-model="generic.params[i].v" class="web3-input flex-1 text-[11px] !py-1.5 font-mono" placeholder="value" />
          </div>
        </div>
        <pre v-if="genericResult" class="mt-4 text-[11px] text-green-300 font-mono whitespace-pre-wrap break-all max-h-72 overflow-y-auto rounded-xl border border-white/10 bg-black/30 p-4">{{ genericResult }}</pre>
      </div>

      <!-- 免责说明 -->
      <div class="text-[11px] text-gray-600 leading-relaxed glass-panel-sm p-4">
        <p class="mb-1">📋 使用说明</p>
        <p>1. 数据来自第三方免费接口，仅供学习与娱乐参考，不保证准确性与时效性（外汇报价、星座运势等请以官方为准）。</p>
        <p>2. 各平台均有每日调用配额（如 j8y 1000 次/日），请合理使用。</p>
        <p>3. ShowAPI 工具需先在官网注册获取 appKey，配置于网关环境变量 <code class="text-cyan-400">SHOWAPI_APP_KEY</code> 后即可生效。</p>
        <p>4. 接口接入详情见 <span class="text-gray-400">docs/第三方API工具中心接入文档.md</span>。</p>
      </div>
    </div>
  </div>
</template>

<script setup>
// ============================================================
// 第三方 API 工具中心（/tools/api）
// 聚合 FreeAPI(j8y.cn) / 万维易源 ShowAPI / 山河云 三平台免费接口，
// 请求统一走后端网关（/api/j8y /api/showapi /api/shanhe），
// 密钥服务端注入，前端零密钥。含通用调用面板与原始 JSON 视图。
// ============================================================
import { ref, reactive, computed, onBeforeUnmount } from 'vue'
import { useToastStore } from '@/stores/modules/toast'
import PageBack from '@/components/PageBack.vue'
import {
  j8yIpLookup, j8yQqInfo, j8yCat, j8yHistory, j8yDeltaPwd, j8yDouyin,
  j8yWeather, j8yWyMusic, j8yQsyy, j8yTxtp,
  showapiPhone, showapiFxList, showapiFxRate, showapiZodiac,
  showapiApi, j8yApi, shanheApi,
} from '@/api/third-party'

const toast = useToastStore()
const activeTab = ref('j8y')
const loading = ref('')
const genericLoading = ref(false)
const genericResult = ref('')

// 十二星座选项（showapi star 拼音）
const ZODIACS = [
  ['baiyang', '白羊座'], ['jinniu', '金牛座'], ['shuangzi', '双子座'], ['juxie', '巨蟹座'],
  ['shizi', '狮子座'], ['chunv', '处女座'], ['tiancheng', '天秤座'], ['tianxie', '天蝎座'],
  ['sheshou', '射手座'], ['mojie', '摩羯座'], ['shuiping', '水瓶座'], ['shuangyu', '双鱼座'],
].map(([value, label]) => ({ value, label }))

// ==================== 工具配置 ====================
const tools = ref([
  // ---------- FreeAPI ----------
  {
    platform: 'j8y', id: 'j8y-ip', icon: '🌐', name: 'IP 归属地查询', status: 'ok',
    desc: 'FreeAPI · ip-lookup · 全球 IP 地理位置与 ISP 信息',
    fields: [{ key: 'ip', label: 'IP 地址', placeholder: '如 8.8.8.8' }],
    call: f => j8yIpLookup(f.ip),
    view: d => ({ kind: 'kv', rows: [
      ['国家', d.data?.country], ['地区', [d.data?.region, d.data?.regionName].filter(Boolean).join(' ')],
      ['城市', d.data?.city], ['邮编', d.data?.zip], ['ISP', d.data?.isp], ['组织', d.data?.org],
      ['ASN', d.data?.as], ['经纬度', d.data ? `${d.data.lat}, ${d.data.lon}` : ''],
      ['时区', d.data?.timezone], ['IP', d.data?.query],
    ].map(([l, v]) => ({ l, v: v ?? '' })) }),
  },
  {
    platform: 'j8y', id: 'j8y-qq', icon: '🐧', name: 'QQ 信息查询', status: 'ok',
    desc: 'FreeAPI · cxqq · 昵称 / 头像 / 邮箱',
    fields: [{ key: 'qq', label: 'QQ 号', placeholder: '如 10001' }],
    call: f => j8yQqInfo(f.qq),
    view: d => ({ kind: 'kv', rows: [
      ['QQ', d.data?.data?.qq], ['昵称', d.data?.data?.nickname],
      ['邮箱', d.data?.data?.email], ['VIP', d.data?.data?.vip_level],
    ].map(([l, v]) => ({ l, v: v ?? '' })) }),
    note: '可与头像地址搭配展示',
  },
  {
    platform: 'j8y', id: 'j8y-cat', icon: '🐱', name: '随机猫咪图片', status: 'ok',
    desc: 'FreeAPI · cat · 随机猫咪 GIF/图片',
    call: f => j8yCat(),
    view: d => ({ kind: 'images', urls: (d.data || []).map(x => x.url) }),
  },
  {
    platform: 'j8y', id: 'j8y-history', icon: '📜', name: '历史上的今天', status: 'ok',
    desc: 'FreeAPI · history · 今日大事记（含百科链接）',
    call: f => j8yHistory(),
    view: d => ({ kind: 'list', rows: (d.data?.data?.list || []).map(x => ({ t: `${x.year} · ${x.title}`, s: x.type, link: x.link })) }),
  },
  {
    platform: 'j8y', id: 'j8y-sjzmm', icon: '🔑', name: '三角洲每日密码', status: 'ok',
    desc: 'FreeAPI · sjzmm · 三角洲行动每日密码房',
    call: f => j8yDeltaPwd(),
    view: d => ({ kind: 'kv', rows: [
      ['更新日期', d.data?.data?.update_date], ['密码', d.data?.data?.passwords],
      ['说明', d.data?.data?.message], ['来源', d.data?.data?.source], ['入库时间', d.data?.data?.last_updated],
    ].map(([l, v]) => ({ l, v: v ?? '' })) }),
  },
  {
    platform: 'j8y', id: 'j8y-dy', icon: '🎬', name: '抖音去水印', status: 'ok',
    desc: 'FreeAPI · dyqsy · 短视频 / 图集无水印解析',
    fields: [{ key: 'url', label: '视频链接', placeholder: 'https://v.douyin.com/...' }],
    call: f => j8yDouyin(f.url),
    view: d => ({ kind: 'kv', rows: (() => {
      const items = []
      const dd = d.data?.data || d.data
      if (Array.isArray(dd)) {
        dd.forEach((it, i) => {
          const u = it?.videoUrl || it?.video || it?.url || it?.download_url
          items.push(['链接 ' + (i + 1), u])
        })
      } else if (dd && typeof dd === 'object') {
        Object.entries(dd).forEach(([k, v]) => { if (typeof v === 'string') items.push([k, v]) })
      }
      return items.length ? items : [['msg', d.data?.msg || '请检查链接']]
    })().map(([l, v]) => ({ l, v: v ?? '' })) }),
  },
  {
    platform: 'j8y', id: 'j8y-weather', icon: '⛅', name: '天气查询', status: 'warn',
    desc: 'FreeAPI · weather · 指定城市天气（平台侧暂不稳）',
    fields: [{ key: 'city', label: '城市', placeholder: '如 beijing' }],
    call: f => j8yWeather(f.city),
    view: d => ({ kind: 'kv', rows: [['结果', JSON.stringify(d.data || d)].map(([l, v]) => ({ l, v }))] }),
  },
  {
    platform: 'j8y', id: 'j8y-wy', icon: '🎵', name: '网易云无损解析', status: 'warn',
    desc: 'FreeAPI · wy_music · 参数格式待确认，用通用面板排查',
    fields: [{ key: 'url', label: '分享内容', placeholder: '歌单/歌曲链接或 ID' }],
    call: f => j8yWyMusic({ url: f.url }),
    view: d => ({ kind: 'kv', rows: [['结果', JSON.stringify(d.data || d)].map(([l, v]) => ({ l, v }))] }),
  },
  {
    platform: 'j8y', id: 'j8y-qsyy', icon: '🥤', name: '汽水音乐解析', status: 'warn',
    desc: 'FreeAPI · qsyy · 平台侧暂不稳定',
    fields: [{ key: 'url', label: '分享链接', placeholder: 'https://qs.qq.com/...' }],
    call: f => j8yQsyy(f.url),
    view: d => ({ kind: 'kv', rows: [['结果', JSON.stringify(d.data || d)].map(([l, v]) => ({ l, v }))] }),
  },
  {
    platform: 'j8y', id: 'j8y-txtp', icon: '🛡️', name: '图片安全检测', status: 'warn',
    desc: 'FreeAPI · txtp · 腾讯图片安全校验（平台侧暂不稳）',
    fields: [{ key: 'url', label: '图片地址', placeholder: 'https://...' }],
    call: f => j8yTxtp(f.url),
    view: d => ({ kind: 'kv', rows: [['结果', JSON.stringify(d.data || d)].map(([l, v]) => ({ l, v }))] }),
  },

  // ---------- ShowAPI ----------
  {
    platform: 'showapi', id: 'sa-phone', icon: '📱', name: '手机号归属地', status: 'key',
    desc: 'ShowAPI 6-1 · 省份 / 城市 / 邮编 / 区号 / 运营商',
    fields: [{ key: 'num', label: '手机号', placeholder: '如 1890871xxxx' }],
    call: f => showapiPhone(f.num),
    view: d => ({ kind: 'kv', rows: [
      ['号码', d.showapi_res_body?.num], ['省份', d.showapi_res_body?.prov],
      ['城市', d.showapi_res_body?.city], ['区号', d.showapi_res_body?.areaCode],
      ['邮编', d.showapi_res_body?.postCode], ['运营商', d.showapi_res_body?.name],
      ['城市编码', d.showapi_res_body?.cityCode], ['省份编码', d.showapi_res_body?.provCode],
    ].map(([l, v]) => ({ l, v: v ?? '' })) }),
  },
  {
    platform: 'showapi', id: 'sa-fx-list', icon: '💱', name: '外汇币种列表', status: 'key',
    desc: 'ShowAPI 105-35 · 银行外汇支持币种与国旗',
    call: f => showapiFxList(),
    view: d => ({ kind: 'list', rows: (d.showapi_res_body?.exchange_list || []).map(x => ({ t: `${x.name} (${x.code})`, link: null })) }),
  },
  {
    platform: 'showapi', id: 'sa-fx-rate', icon: '🏦', name: '银行汇率查询', status: 'key',
    desc: 'ShowAPI 105-30 · 中行外汇牌价（10 秒级更新）',
    fields: [{ key: 'code', label: '币种', type: 'select', options: [] }],
    call: f => showapiFxRate(f.code),
    view: d => ({ kind: 'kv', rows: (d.showapi_res_body?.list || []).map(x => {
      return [
        ['币种', x.name + ' (' + x.code + ')'],
        ['汇买价', x.hui_in], ['钞买价', x.chao_in],
        ['汇卖价', x.hui_out], ['钞卖价', x.chao_out],
        ['折算价', x.zhesuan], ['更新时间', (x.day || '') + ' ' + (x.time || '')],
      ].map(([l, v]) => ({ l, v: v ?? '' }))
    }).flat().slice(0, 14) }),
  },
  {
    platform: 'showapi', id: 'sa-zodiac', icon: '♈', name: '星座运势查询', status: 'key',
    desc: 'ShowAPI 872-1 · 当日运势 / 幸运数字 / 吉利方向',
    fields: [
      { key: 'star', label: '星座', type: 'select', options: ZODIACS },
      { key: 'date', label: '日期', placeholder: '如 0818' },
    ],
    call: f => showapiZodiac({ star: f.star, date: f.date || '', needTomorrow: 0, needWeek: 0, needMonth: 0, needYear: 0 }),
    view: d => {
      const day = d.showapi_res_body?.day
      return { kind: 'multi-kv', groups: day ? [
        { title: `总要 ${'★'.repeat(day.summary_star || 0)}`, rows: [
          ['综合', day.general_txt], ['提醒', day.day_notice],
        ] },
        { title: `财运 ${'★'.repeat(day.money_star || 0)}`, rows: [['财运', day.money_txt]] },
        { title: `事业 ${'★'.repeat(day.work_star || 0)}`, rows: [['事业', day.work_txt]] },
        { title: `爱情 ${'★'.repeat(day.love_star || 0)}`, rows: [['爱情', day.love_txt]] },
        { title: '幸运', rows: [
          ['幸运数字', day.lucky_num], ['吉利方向', day.lucky_direction], ['吉时颜色', day.lucky_time_color],
        ] },
      ].map(g => ({ ...g, rows: g.rows.map(([l, v]) => ({ l, v: v ?? '' })) })) : [] }
    },
  },

  // ---------- 山河云 ----------
  {
    platform: 'shanhe', id: 'sh-zodiac', icon: '✨', name: '星座运势（山河云）', status: 'ok',
    desc: '山河云 · 星座.php · 综合 / 事业 / 财运 / 爱情',
    fields: [{ key: 'name', label: '星座', type: 'select', options: ZODIACS.map(x => ({ value: x.label.replace('座', ''), label: x.label })) }],
    call: f => shanheApi('星座', { name: f.name, type: 'json' }),
    view: d => ({ kind: 'multi-kv', groups: [
      { title: d.data?.name_full || d.data?.name || '', rows: [
        ['综合', d.data?.data?.overall], ['事业', d.data?.data?.career],
        ['财运', d.data?.data?.fortune], ['爱情', d.data?.data?.love],
      ].map(([l, v]) => ({ l, v: v ?? '' })) },
    ] }),
  },
  {
    platform: 'shanhe', id: 'sh-weather', icon: '🌤️', name: '今日天气（山河云）', status: 'ok',
    desc: '山河云 · 天气.php · 详细天气',
    fields: [{ key: 'city', label: '城市', placeholder: '如 成都' }],
    call: f => shanheApi('天气', { city: f.city, type: 'json' }),
    view: d => ({ kind: 'kv', rows: Object.entries(d.data?.data || {}).map(([k, v]) => ({ l: k, v: String(v ?? '') })).slice(0, 16) }),
  },
  {
    platform: 'shanhe', id: 'sh-lunar', icon: '🌙', name: '农历查询', status: 'ok',
    desc: '山河云 · 农历.php · 今日公历 / 农历 / 节气',
    call: f => shanheApi('农历', { type: 'json' }),
    view: d => ({ kind: 'kv', rows: flattenObj(d.data?.data).slice(0, 20) }),
  },
  {
    platform: 'shanhe', id: 'sh-history', icon: '📖', name: '历史上的今天（山河云）', status: 'ok',
    desc: '山河云 · 历史上的今天.php',
    fields: [{ key: 'num', label: '条数', placeholder: '如 10' }],
    call: f => shanheApi('历史上的今天', { type: 'json', num: f.num || '' }),
    view: d => ({ kind: 'items', items: parseTextList(d) }),
  },
  {
    platform: 'shanhe', id: 'sh-yiyan', icon: '💬', name: '随机一言', status: 'ok',
    desc: '山河云 · 一言.php · 随机句子',
    call: f => shanheApi('一言', { type: 'json' }),
    view: d => ({ kind: 'items', items: parseTextList(d) }),
  },
  {
    platform: 'shanhe', id: 'sh-weibo', icon: '🔥', name: '微博热榜', status: 'ok',
    desc: '山河云 · 微博热榜.php',
    call: f => shanheApi('微博热榜', { type: 'json' }),
    view: d => ({ kind: 'items', items: parseTextList(d) }),
  },
  {
    platform: 'shanhe', id: 'sh-zhihu', icon: '📰', name: '知乎热榜', status: 'ok',
    desc: '山河云 · 知乎热榜.php',
    call: f => shanheApi('知乎热榜', { type: 'json' }),
    view: d => ({ kind: 'items', items: parseTextList(d) }),
  },
  {
    platform: 'shanhe', id: 'sh-fish', icon: '🐟', name: '摸鱼日历', status: 'key',
    desc: '山河云 · 摸鱼日历.php · 需 Token（apikey 透传）',
    fields: [
      { key: 'style', label: '样式', type: 'select', options: [1, 2, 3].map(x => ({ value: x, label: 'style ' + x })) },
      { key: 'apikey', label: 'Token', placeholder: '山河云 apikey' },
    ],
    call: f => shanheApi('摸鱼日历', { style: f.style, apikey: f.apikey || '' }),
    view: d => ({ kind: 'images', urls: extractImages(d) }),
  },
  {
    platform: 'shanhe', id: 'sh-bing', icon: '🖼️', name: '必应每日壁纸', status: 'ok',
    desc: '山河云 · 必应壁纸.php · 今日壁纸',
    call: f => shanheApi('必应壁纸', { type: 'json' }),
    view: d => ({ kind: 'images', urls: extractImages(d) }),
  },
  {
    platform: 'shanhe', id: 'sh-wall', icon: '🎨', name: '随机壁纸', status: 'key',
    desc: '山河云 · 随机壁纸.php · 需 Token',
    fields: [{ key: 'apikey', label: 'Token', placeholder: '山河云 apikey' }],
    call: f => shanheApi('随机壁纸', { apikey: f.apikey || '' }),
    view: d => ({ kind: 'images', urls: extractImages(d) }),
  },
])

const tabs = [
  { id: 'j8y', label: '⚡ FreeAPI (j8y.cn)', count: tools.value.filter(t => t.platform === 'j8y').length },
  { id: 'showapi', label: '🔗 万维易源 ShowAPI', count: tools.value.filter(t => t.platform === 'showapi').length },
  { id: 'shanhe', label: '⛰️ 山河云', count: tools.value.filter(t => t.platform === 'shanhe').length },
]

const visibleTools = computed(() => tools.value.filter(t => t.platform === activeTab.value))

// ==================== 表单 / 结果 ====================
const forms = reactive({})
const results = reactive({})
tools.value.forEach(t => {
  forms[t.id] = {}
  ;(t.fields || []).forEach(f => { forms[t.id][f.key] = f.default ?? '' })
})

// 汇率工具的动态币种下拉（从 105-35 拉取）
const fxTool = tools.value.find(t => t.id === 'sa-fx-rate')
const fxOptions = ref([])
showapiFxList().then(d => {
  const list = (d.showapi_res_body?.exchange_list || []).map(x => ({ value: x.code, label: `${x.name} ${x.code}` }))
  fxOptions.value = list
  fxTool.fields[0].options = list
}).catch(() => {})

// 结果视图：成功/失败封装
function callTool(tool) {
  const id = tool.id
  if (loading.value) return
  loading.value = id
  const form = Object.fromEntries(Object.entries(forms[id]).filter(([, v]) => v !== '' && v !== null && v !== undefined))
  tool.call(form)
    .then(d => {
      results[id] = { ok: isOkRes(d), raw: JSON.stringify(d, null, 2), showRaw: false, ...(tool.view ? { view: tool.view(d) } : {}) }
      if (!isOkRes(d)) results[id].msg = extractMsg(d)
    })
    .catch(e => {
      results[id] = { ok: false, raw: (e?.response?.data ? JSON.stringify(e.response.data) : e.message) || '请求失败', showRaw: true, msg: e?.response?.status === 502 || e?.response?.status === 504 ? '网关/上游超时' : (e?.response?.data?.message || e.message || '请求失败') }
    })
    .finally(() => { loading.value = '' })
}

function isOkRes(d) {
  if (d?.code === 0 || d?.code === 200) return true
  if (d?.showapi_res_code === 0) return true
  if (d?.showapi_res_code !== undefined && d?.showapi_res_code !== 0) return false
  return d && !(d instanceof Error)
}

function extractMsg(d) {
  if (d && typeof d === 'object') {
    if (d.showapi_res_error) return d.showapi_res_error
    if (d.msg) return typeof d.msg === 'string' ? d.msg : ''
  }
  return '接口返回异常，请查看原始 JSON'
}

// 工具状态文案
function statusText(s) {
  return s === 'ok' ? '✓ 可用' : s === 'key' ? '需密钥' : '⚠ 待确认'
}

// 复制结果
function copyResult(id) {
  const r = results[id]
  if (!r) return
  const text = r.showRaw ? r.raw : JSON.stringify(r.view || '', null, 2)
  navigator.clipboard?.writeText(text).then(() => toast.success('已复制')).catch(() => toast.error('复制失败'))
}

function toggleRaw(id) { results[id].showRaw = !results[id].showRaw }

// ==================== 通用调用面板 ====================
const generic = reactive({ platform: 'j8y', path: 'weather', params: [{ k: '', v: '' }, { k: '', v: '' }, { k: '', v: '' }, { k: '', v: '' }] })

async function callGeneric() {
  if (!generic.path.trim()) return
  genericLoading.value = true
  genericResult.value = ''
  const params = {}
  generic.params.forEach(p => { if (p.k.trim()) params[p.k.trim()] = p.v })
  try {
    let d
    if (generic.platform === 'j8y') d = await j8yApi(generic.path.trim(), params)
    else if (generic.platform === 'showapi') d = await showapiApi(generic.path.trim(), params)
    else d = await shanheApi(generic.path.trim(), params)
    genericResult.value = JSON.stringify(d, null, 2)
  } catch (e) {
    genericResult.value = JSON.stringify(e?.response?.data || { error: e.message }, null, 2)
  } finally {
    genericLoading.value = false
  }
}

// ==================== 辅助 ====================
function flattenObj(obj, prefix = '') {
  if (!obj || typeof obj !== 'object') return []
  const out = []
  for (const [k, v] of Object.entries(obj)) {
    if (v && typeof v === 'object') {
      if (Array.isArray(v) || typeof v === 'object') {
        const sub = flattenObj(v, prefix + k + '.')
        out.push(...(sub.length ? sub : [{ l: prefix + k, v: JSON.stringify(v) }]))
      }
    } else out.push({ l: prefix + k, v: String(v ?? '') })
  }
  return out
}

// 山河云文本类接口：data 可能是数组 / 对象 / 字符串
function parseTextList(d) {
  const data = d.data?.data ?? d.data
  if (typeof data === 'string') return [data]
  if (Array.isArray(data)) return data.map(x => typeof x === 'string' ? x : (x?.content || x?.title || x?.text || JSON.stringify(x)))
  return [JSON.stringify(data).slice(0, 500)]
}

function extractImages(d) {
  const data = d.data?.data ?? d.data
  const urls = []
  const walk = o => {
    if (!o || typeof o !== 'object') return
    if (Array.isArray(o)) { o.forEach(walk); return }
    for (const [k, v] of Object.entries(o)) {
      if (typeof v === 'string' && /^https?:\/\//.test(v) && /\.(png|jpe?g|gif|webp)($|\?)/i.test(v)) urls.push(v)
      else walk(v)
    }
  }
  walk(data)
  return urls.slice(0, 6)
}

onBeforeUnmount(() => {})
</script>

<style scoped>
</style>