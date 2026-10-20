<template>
  <div class="min-h-screen px-4 md:px-6 py-10">
    <div class="mx-auto max-w-6xl">
      <PageHeader title="GhostTrack 网络追踪" subtitle="IP 归属 · 手机号解析 · 用户名跨站检测"
        :crumbs="[{ label: '首页', to: '/' }, { label: '运维工具箱', to: '/tools' }, { label: 'GhostTrack' }]" />

      <!-- 功能切换 -->
      <div class="flex flex-wrap gap-2 mb-5">
        <button v-for="t in TABS" :key="t.key" class="web3-pill"
          :class="{ 'web3-pill-active': tab === t.key }" @click="tab = t.key">
          {{ t.icon }} {{ t.name }}
        </button>
      </div>

      <!-- ── IP 追踪 ── -->
      <template v-if="tab === 'ip'">
        <section class="glass-panel p-5">
          <div class="flex flex-col md:flex-row gap-3">
            <input v-model="ipInput" class="web3-input flex-1 text-sm font-mono"
              placeholder="输入 IP（如 8.8.8.8 / 2001:4860:4860::8888），留空查询你自己的公网 IP"
              @keyup.enter="doIp" />
            <button class="web3-btn text-sm !px-6" :disabled="ipLoading" @click="doIp">
              {{ ipLoading ? '查询中...' : '🔍 查询 IP' }}
            </button>
          </div>
          <p class="mt-3 text-[11px] text-gray-500">
            留空时优先用服务端观测到的你的公网 IP；本地开发下该地址是回环地址，
            会自动改为查询本机出口公网 IP，并在结果中标注来源。
          </p>
          <p v-if="ipError" class="mt-3 text-xs text-red-400">{{ ipError }}</p>
        </section>

        <section v-if="ipResult" class="glass-panel p-5 mt-5">
          <div class="flex items-center gap-3 flex-wrap">
            <span class="text-3xl">{{ ipResult.flag }}</span>
            <div>
              <h3 class="text-lg font-bold text-white font-mono">{{ ipResult.ip }}</h3>
              <p class="text-[11px] text-gray-500">
                {{ ipResult.type }} · 来源：{{ SOURCE_LABEL[ipResult.source] || ipResult.source }}
              </p>
            </div>
            <a v-if="ipResult.mapUrl" :href="ipResult.mapUrl" target="_blank" rel="noopener"
              class="web3-btn-outline text-xs !px-4 !py-1.5 ml-auto">🗺️ 在地图中查看</a>
          </div>

          <div class="holo-bar my-5"></div>

          <div class="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-4 gap-3">
            <div v-for="f in ipFields" :key="f.label" class="rounded-lg border border-white/10 bg-black/30 px-3 py-2">
              <p class="text-[10px] text-gray-500">{{ f.label }}</p>
              <p class="text-xs text-gray-200 mt-1 break-all" :class="{ 'font-mono': f.mono }">
                {{ f.value || '—' }}
              </p>
            </div>
          </div>

          <p class="mt-4 pt-3 border-t border-white/10 text-[11px] text-gray-500">
            数据来源：ipwho.is（公开接口）。仅用于网络排障与归属地核对。
          </p>
        </section>
      </template>

      <!-- ── 手机号解析 ── -->
      <template v-else-if="tab === 'phone'">
        <section class="glass-panel p-5">
          <div class="flex flex-col md:flex-row gap-3">
            <input v-model="phoneInput" class="web3-input flex-1 text-sm font-mono"
              placeholder="输入手机号，如 13800138000 或 +14155552671" @keyup.enter="doPhone" />
            <select v-model="phoneRegion" class="web3-input text-sm md:w-40">
              <option v-for="r in REGIONS" :key="r.code" :value="r.code">{{ r.label }}</option>
            </select>
            <button class="web3-btn text-sm !px-6" :disabled="phoneLoading" @click="doPhone">
              {{ phoneLoading ? '解析中...' : '📱 解析号码' }}
            </button>
          </div>
          <p class="mt-3 text-[11px] text-gray-500">
            全部解析由服务端本地号码库完成（Google libphonenumber 移植版），
            <span class="text-cyan-300">不向任何第三方站点发起请求</span>。
            带 <code class="text-cyan-300">+</code> 前缀的号码按国际格式解析，地区选择不影响它。
          </p>
          <p v-if="phoneError" class="mt-3 text-xs text-red-400">{{ phoneError }}</p>
        </section>

        <section v-if="phoneResult" class="glass-panel p-5 mt-5">
          <div class="flex items-center gap-2.5 flex-wrap">
            <span class="text-[11px] px-2 py-0.5 rounded border"
              :class="phoneResult.valid ? 'text-green-400 border-green-400/30 bg-green-400/10'
                                      : 'text-amber-400 border-amber-400/30 bg-amber-400/10'">
              {{ phoneResult.valid ? '✓ 号段有效' : '⚠ 未通过号段校验' }}
            </span>
            <span class="text-lg font-bold text-white font-mono">{{ phoneResult.e164 }}</span>
            <span v-if="phoneResult.regionCode" class="text-[11px] text-gray-500">地区码 {{ phoneResult.regionCode }}</span>
          </div>

          <div class="holo-bar my-5"></div>

          <div class="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-4 gap-3">
            <div v-for="f in phoneFields" :key="f.label" class="rounded-lg border border-white/10 bg-black/30 px-3 py-2">
              <p class="text-[10px] text-gray-500">{{ f.label }}</p>
              <p class="text-xs text-gray-200 mt-1 break-all" :class="{ 'font-mono': f.mono }">
                {{ f.value || '—' }}
              </p>
            </div>
          </div>

          <p v-if="phoneResult.note" class="mt-4 pt-3 border-t border-white/10 text-[11px] text-amber-400/90">
            💡 {{ phoneResult.note }}
          </p>
        </section>
      </template>

      <!-- ── 用户名跨站检测 ── -->
      <template v-else>
        <section class="glass-panel p-5">
          <div class="flex flex-col md:flex-row gap-3">
            <input v-model="userInput" class="web3-input flex-1 text-sm font-mono"
              placeholder="输入用户名（字母 / 数字 / . _ -，最长 40 位）" @keyup.enter="doUser" />
            <button class="web3-btn text-sm !px-6" :disabled="userLoading" @click="doUser">
              {{ userLoading ? '检测中...' : '🔎 跨站检测' }}
            </button>
          </div>
          <p class="mt-3 text-[11px] text-gray-500">
            并发探测 {{ siteTotal }} 个白名单站点，其中
            <span class="text-green-400">{{ reliableTotal }} 个</span> 以「不存在返回 404」为判据可信判定；
            其余站点对不存在账号也返回页面，仅提供链接供人工确认。
          </p>
          <p v-if="userError" class="mt-3 text-xs text-red-400">{{ userError }}</p>
        </section>

        <section v-if="userResult" class="glass-panel p-5 mt-5">
          <h3 class="text-sm font-bold text-white">
            用户名 <span class="text-cyan-300 font-mono">{{ userResult.username }}</span>
            · 命中 <span class="text-green-400">{{ userResult.found }}</span> / {{ userResult.count }}
          </h3>

          <div class="holo-bar my-4"></div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-2.5">
            <div v-for="it in userResult.items" :key="it.key"
              class="rounded-lg border px-3 py-2.5 flex items-start gap-3"
              :class="{
                'border-green-400/30 bg-green-400/5': it.found === 'yes',
                'border-white/10 bg-black/30': it.found !== 'yes'
              }">
              <span class="text-base leading-5">{{ FOUND_ICON[it.found] }}</span>
              <div class="min-w-0 flex-1">
                <div class="flex items-center gap-2">
                  <span class="text-xs text-white font-semibold">{{ it.name }}</span>
                  <span v-if="!it.reliable" class="text-[10px] text-amber-400/90 border border-amber-400/20 rounded px-1">需人工确认</span>
                  <span class="text-[10px] text-gray-600 ml-auto">{{ it.status || '—' }}</span>
                </div>
                <p class="text-[10px] text-gray-500 mt-0.5">{{ it.reason }}</p>
              </div>
              <a :href="it.url" target="_blank" rel="noopener"
                class="text-[11px] text-cyan-400 hover:text-cyan-300 shrink-0">打开 ↗</a>
            </div>
          </div>
        </section>
      </template>

      <div class="holo-bar my-6"></div>

      <!-- 说明与免责 -->
      <section class="glass-panel p-5">
        <h2 class="text-sm font-bold text-white mb-3 flex items-center gap-2"><span>💡</span>使用说明</h2>
        <ul class="text-xs text-gray-400 leading-relaxed space-y-1.5 list-disc pl-5">
          <li><b class="text-gray-300">IP 追踪</b>：查询公网 IP 的国家/地区/城市、经纬度、ASN、ISP 与时区，可点「在地图中查看」定位。</li>
          <li><b class="text-gray-300">手机号解析</b>：纯本地号码库解析归属地、运营商、号码类型与时区，过程中不产生任何外部请求。</li>
          <li><b class="text-gray-300">用户名检测</b>：并发探测固定白名单站点，仅依据 HTTP 状态码判定，不抓取页面内容、不存储结果。</li>
          <li>「需人工确认」标记表示该站点对不存在的账号同样返回页面，状态码无法作为依据，请点「打开」自行核对。</li>
        </ul>
        <p class="mt-4 pt-3 border-t border-white/10 text-[11px] text-amber-400/90">
          ⚠️ 本页数据均来自公开渠道，仅供自查与授权范围内的网络排障使用；请勿用于跟踪、骚扰或侵犯他人隐私。
        </p>
      </section>
    </div>
  </div>
</template>

<script setup>
// ====================================================
// GhostTrack 网络追踪页：IP 归属 / 手机号解析 / 用户名跨站检测
// 三个面板共用一套请求状态，后端为 py-service 9006 的 ghosttrack 模块
// ====================================================
import { ref, computed, onMounted } from 'vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { gtIp, gtPhone, gtUsername, gtSites } from '@/api/ghosttrack'

const tab = ref('ip')

const TABS = [
  { key: 'ip', icon: '🌐', name: 'IP 追踪' },
  { key: 'phone', icon: '📱', name: '手机号解析' },
  { key: 'user', icon: '🔎', name: '用户名检测' },
]

// 常用地区码：不带 + 前缀的号码按此地区解析
const REGIONS = [
  { code: 'CN', label: '中国大陆 CN' },
  { code: 'HK', label: '中国香港 HK' },
  { code: 'TW', label: '中国台湾 TW' },
  { code: 'US', label: '美国 US' },
  { code: 'JP', label: '日本 JP' },
  { code: 'KR', label: '韩国 KR' },
  { code: 'SG', label: '新加坡 SG' },
  { code: 'GB', label: '英国 GB' },
]

const SOURCE_LABEL = { manual: '指定 IP', client: '你的公网 IP', egress: '本机出口 IP' }
const FOUND_ICON = { yes: '✅', no: '➖', unknown: '❓' }

// ── IP ──
const ipInput = ref('')
const ipResult = ref(null)
const ipLoading = ref(false)
const ipError = ref('')

// ── 手机号 ──
const phoneInput = ref('')
const phoneRegion = ref('CN')
const phoneResult = ref(null)
const phoneLoading = ref(false)
const phoneError = ref('')

// ── 用户名 ──
const userInput = ref('')
const userResult = ref(null)
const userLoading = ref(false)
const userError = ref('')
const siteTotal = ref(0)
const reliableTotal = ref(0)

// 从 axios 错误里取后端返回的具体原因（FastAPI 用 detail 字段）
function errMsg(e) {
  const d = e?.response?.data
  return d?.detail || d?.message || e?.message || '请求失败'
}

async function doIp() {
  ipLoading.value = true
  ipError.value = ''
  try {
    ipResult.value = await gtIp({ ip: ipInput.value.trim() })
  } catch (e) {
    ipResult.value = null
    ipError.value = errMsg(e)
  } finally {
    ipLoading.value = false
  }
}

async function doPhone() {
  phoneLoading.value = true
  phoneError.value = ''
  try {
    phoneResult.value = await gtPhone({ number: phoneInput.value.trim(), region: phoneRegion.value })
  } catch (e) {
    phoneResult.value = null
    phoneError.value = errMsg(e)
  } finally {
    phoneLoading.value = false
  }
}

async function doUser() {
  userLoading.value = true
  userError.value = ''
  try {
    userResult.value = await gtUsername({ username: userInput.value.trim() })
  } catch (e) {
    userResult.value = null
    userError.value = errMsg(e)
  } finally {
    userLoading.value = false
  }
}

// IP 结果卡片字段
const ipFields = computed(() => {
  const r = ipResult.value
  if (!r) return []
  return [
    { label: '国家 / 地区', value: [r.flag, r.country, r.countryCode].filter(Boolean).join(' ') },
    { label: '洲', value: r.continent },
    { label: '省 / 州', value: r.region },
    { label: '城市', value: r.city },
    { label: '邮编', value: r.postal },
    { label: '首都', value: r.capital },
    { label: '经纬度', value: r.latitude != null ? `${r.latitude}, ${r.longitude}` : '', mono: true },
    { label: '国际电话区号', value: r.callingCode ? `+${r.callingCode}` : '', mono: true },
    { label: 'ASN', value: r.asn, mono: true },
    { label: 'ISP', value: r.isp },
    { label: '组织', value: r.org },
    { label: '域名', value: r.domain, mono: true },
    { label: '时区', value: [r.tzId, r.tzAbbr, r.tzUtc].filter(Boolean).join(' · ') },
    { label: '当地当前时间', value: r.tzCurrent, mono: true },
    { label: '是否欧盟', value: r.isEU ? '是' : '否' },
  ]
})

// 手机号结果卡片字段
const phoneFields = computed(() => {
  const r = phoneResult.value
  if (!r) return []
  return [
    { label: '国际格式', value: r.international, mono: true },
    { label: '国内格式', value: r.national, mono: true },
    { label: '国际区号', value: r.countryCode ? `+${r.countryCode}` : '', mono: true },
    { label: '归属地', value: r.location },
    { label: '运营商', value: r.carrier },
    { label: '号码类型', value: r.type },
    { label: '长度（不含区号）', value: r.length ? `${r.length} 位` : '' },
    { label: '时区', value: (r.timezones || []).join(' · ') },
    { label: '长度校验', value: r.possible ? '符合号段长度' : '长度不符' },
    { label: '解析地区假设', value: r.assumedRegion || '（按 + 前缀国际解析）' },
  ]
})

// 站点清单仅用于展示规模，失败不阻断页面（后端不可达时不弹错误）
onMounted(async () => {
  try {
    const res = await gtSites()
    siteTotal.value = res?.sites?.length || 0
    reliableTotal.value = res?.reliable || 0
  } catch {
    siteTotal.value = 0
  }
})
</script>
