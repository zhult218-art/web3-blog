<template>
  <div class="min-h-screen px-4 md:px-6 py-10">
    <div class="mx-auto max-w-3xl">
      <button class="text-gray-400 hover:text-white mb-6 flex items-center gap-1 text-sm transition-colors" @click="$router.push('/tools')">
        <span>←</span> 返回工具箱
      </button>

      <h1 class="text-3xl font-bold text-gradient-cyber mb-2">{{ toolTitle }}</h1>
      <p class="text-sm text-gray-500 matrix-text mb-8">{{ toolDesc }}</p>

      <div class="glass-panel p-6">
        <div v-if="activeTool === 'json'" class="space-y-4">
          <div class="flex gap-2">
            <button class="web3-btn text-xs" @click="formatJSON">格式化</button>
            <button class="web3-btn-outline text-xs" @click="compressJSON">压缩</button>
            <button class="web3-btn-outline text-xs" @click="validateJSON">验证</button>
            <button class="web3-btn-ghost text-xs ml-auto" @click="clearInput">清空</button>
          </div>
          <textarea v-model="input" class="web3-input h-80 font-mono text-sm" :placeholder="placeholder"></textarea>
          <div v-if="output !== null" class="rounded-xl border border-white/10 bg-black/30 p-5">
            <div class="flex items-center justify-between mb-2">
              <span class="text-xs text-gray-400">输出结果</span>
              <button class="text-xs text-cyan-400 hover:text-cyan-300" @click="copyOutput">📋 复制</button>
            </div>
            <pre class="text-sm font-mono whitespace-pre-wrap break-all" :class="outputError ? 'text-red-400' : 'text-green-300'">{{ output }}</pre>
          </div>
        </div>

        <div v-else-if="activeTool === 'base64'" class="space-y-4">
          <div class="flex gap-2">
            <button class="web3-btn text-xs" @click="base64Encode">编码</button>
            <button class="web3-btn-outline text-xs" @click="base64Decode">解码</button>
            <button class="web3-btn-ghost text-xs ml-auto" @click="clearInput">清空</button>
          </div>
          <textarea v-model="input" class="web3-input h-52 font-mono text-sm" :placeholder="placeholder"></textarea>
          <div v-if="output !== null">
            <div class="flex items-center justify-between mb-2">
              <span class="text-xs text-gray-400">输出结果</span>
              <button class="text-xs text-cyan-400" @click="copyOutput">📋 复制</button>
            </div>
            <textarea readonly class="web3-input h-52 font-mono text-sm text-green-300" :value="output"></textarea>
          </div>
        </div>

        <div v-else-if="activeTool === 'timestamp'" class="space-y-4">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label class="text-xs text-gray-400 mb-1 block">Unix时间戳 → 日期</label>
              <div class="flex gap-2">
                <input v-model="tsInput" class="web3-input flex-1 text-sm" placeholder="如: 1700000000" />
                <button class="web3-btn text-xs" @click="tsToDate">转换</button>
              </div>
              <div v-if="tsResult" class="mt-3 text-sm text-green-300 font-mono">{{ tsResult }}</div>
            </div>
            <div>
              <label class="text-xs text-gray-400 mb-1 block">日期 → Unix时间戳</label>
              <div class="flex gap-2">
                <input v-model="dateInput" class="web3-input flex-1 text-sm" placeholder="如: 2024-01-01 12:00:00" />
                <button class="web3-btn text-xs" @click="dateToTs">转换</button>
              </div>
              <div v-if="dateResult" class="mt-3 text-sm text-green-300 font-mono">{{ dateResult }}</div>
            </div>
          </div>
          <div class="text-xs text-gray-500 mt-4">
            当前时间戳: <span class="text-cyan-400 font-mono">{{ nowTs }}</span> &nbsp;|&nbsp;
            当前时间: <span class="text-cyan-400">{{ nowDate }}</span>
          </div>
        </div>

        <div v-else-if="activeTool === 'uuid'" class="space-y-4 text-center py-10">
          <div class="text-2xl font-mono text-green-300 glass-panel-sm p-6 select-all">{{ generatedUUID }}</div>
          <div class="flex gap-3 justify-center">
            <button class="web3-btn text-sm" @click="generateUUID">重新生成</button>
            <button class="web3-btn-outline text-sm" @click="copyUUID">📋 复制</button>
          </div>
          <p class="text-xs text-gray-600 mt-2">Version 4 UUID (随机)</p>
        </div>

        <div v-else-if="activeTool === 'uuid'" class="space-y-4 text-center py-10">
          <div class="text-2xl font-mono text-green-300 glass-panel-sm p-6 select-all">{{ generatedUUID }}</div>
          <div class="flex gap-3 justify-center">
            <button class="web3-btn text-sm" @click="generateUUID">重新生成</button>
            <button class="web3-btn-outline text-sm" @click="copyUUID">📋 复制</button>
          </div>
          <p class="text-xs text-gray-600 mt-2">Version 4 UUID (随机)</p>
        </div>

        <!-- 正则表达式测试 -->
        <div v-else-if="activeTool === 'regex'" class="space-y-4">
          <div class="flex gap-2 items-center">
            <input v-model="regexPattern" class="web3-input flex-1 text-sm font-mono" placeholder="正则表达式，如: \d+" />
            <select v-model="regexFlags" class="web3-input !w-20 text-sm text-center">
              <option value="g">g</option><option value="gi">gi</option><option value="gim">gim</option>
            </select>
            <button class="web3-btn text-xs" @click="runRegex">匹配</button>
          </div>
          <div v-if="regexError" class="text-xs text-red-400 bg-red-500/10 rounded-lg px-3 py-2">{{ regexError }}</div>
          <textarea v-model="regexTestStr" class="web3-input h-40 font-mono text-sm" placeholder="输入要测试的文本..."></textarea>
          <div v-if="regexMatches.length" class="glass-panel-sm p-4">
            <p class="text-xs text-gray-400 mb-3">匹配到 {{ regexMatches.length }} 个结果：</p>
            <div class="space-y-2 max-h-60 overflow-y-auto">
              <div v-for="(m, i) in regexMatches" :key="i" class="flex items-center gap-3 text-sm">
                <span class="text-[11px] text-gray-600 w-8">{{ i + 1 }}</span>
                <span class="text-green-300 font-mono bg-black/30 px-2 py-1 rounded">{{ m.text }}</span>
                <span class="text-[11px] text-gray-500">位置 {{ m.index }}</span>
              </div>
            </div>
          </div>
          <div v-else-if="regexMatches.length === 0 && regexPattern" class="text-xs text-gray-500 text-center py-3">未找到匹配</div>
        </div>

        <!-- Hash 生成器 -->
        <div v-else-if="activeTool === 'hash'" class="space-y-4">
          <textarea v-model="hashInput" class="web3-input h-24 font-mono text-sm" placeholder="输入待计算哈希的内容..."></textarea>
          <div class="flex gap-2">
            <button class="web3-btn text-xs" @click="computeHashes" :disabled="!hashInput.trim()">计算哈希</button>
            <button class="web3-btn-ghost text-xs" @click="hashInput=''; hashResults={}">清空</button>
          </div>
          <div v-if="Object.keys(hashResults).length" class="space-y-3">
            <div v-for="(val, alg) in hashResults" :key="alg" class="glass-panel-sm p-4 flex items-center gap-3">
              <span class="text-xs text-purple-300 font-mono w-16 flex-shrink-0">{{ alg }}</span>
              <code class="flex-1 text-xs text-green-300 font-mono break-all">{{ val }}</code>
              <button class="text-[11px] text-cyan-400 hover:text-cyan-300 flex-shrink-0" @click="copyHash(val)">📋</button>
            </div>
          </div>
        </div>

        <!-- 二维码生成 -->
        <div v-else-if="activeTool === 'qrcode'" class="space-y-4">
          <textarea v-model="qrText" class="web3-input h-20 font-mono text-sm" placeholder="输入文本或URL生成二维码..."></textarea>
          <div class="flex gap-2 items-center">
            <label class="text-xs text-gray-400">尺寸:</label>
            <select v-model="qrSize" class="web3-input !w-24 text-sm text-center">
              <option :value="128">128</option><option :value="256">256</option><option :value="384">384</option><option :value="512">512</option>
            </select>
            <button class="web3-btn text-xs" @click="generateQR" :disabled="!qrText.trim()">生成二维码</button>
            <button v-if="qrDataUrl" class="web3-btn-outline text-xs" @click="downloadQR">💾 下载</button>
          </div>
          <div v-if="qrDataUrl" class="flex justify-center">
            <div class="glass-panel p-4 inline-block">
              <img :src="qrDataUrl" class="rounded-lg" style="image-rendering: pixelated;" />
            </div>
          </div>
        </div>

        <!-- 颜色转换 -->
        <div v-else-if="activeTool === 'color'" class="space-y-4">
          <div class="flex gap-3 items-center">
            <div class="flex gap-2 flex-1">
              <input v-model="colorInput" class="web3-input flex-1 text-sm font-mono" placeholder="#ff6600 或 rgb(255,102,0)" @input="convertColor" />
            </div>
            <button class="web3-btn text-xs" @click="convertColor">转换</button>
          </div>
          <div v-if="colorResults.hex" class="flex items-center gap-3 mb-3">
            <div class="w-12 h-12 rounded-xl border border-white/10" :style="{ background: colorInput }"></div>
            <span class="text-xs text-gray-400">预览颜色</span>
          </div>
          <div v-if="Object.keys(colorResults).length" class="grid grid-cols-1 sm:grid-cols-2 gap-2">
            <div v-for="(val, key) in colorResults" :key="key" class="glass-panel-sm px-4 py-3 flex items-center justify-between cursor-pointer hover:border-cyan-400/20 transition" @click="copyColor(val)">
              <div>
                <span class="text-[10px] text-gray-500 uppercase">{{ key }}</span>
                <p class="text-sm text-white font-mono">{{ val }}</p>
              </div>
              <span class="text-xs text-gray-600">📋</span>
            </div>
          </div>
        </div>

        <!-- URL 编解码 -->
        <div v-else-if="activeTool === 'url'" class="space-y-4">
          <textarea v-model="urlInput" class="web3-input h-28 font-mono text-sm" placeholder="输入URL或文本..."></textarea>
          <div class="flex gap-2">
            <button class="web3-btn text-xs" @click="urlEncode">Encode</button>
            <button class="web3-btn-outline text-xs" @click="urlDecode">Decode</button>
            <button class="web3-btn-ghost text-xs ml-auto" @click="urlInput=''; urlOutput=''">清空</button>
          </div>
          <div v-if="urlOutput">
            <p class="text-xs text-gray-400 mb-2">输出结果：</p>
            <div class="glass-panel-sm p-4 flex items-center justify-between gap-3">
              <code class="text-sm text-green-300 font-mono break-all flex-1">{{ urlOutput }}</code>
              <button class="text-xs text-cyan-400 hover:text-cyan-300 flex-shrink-0" @click="navigator.clipboard?.writeText(urlOutput).then(() => toast.success('已复制'))">📋</button>
            </div>
          </div>
        </div>

        <!-- 文本差异对比 -->
        <div v-else-if="activeTool === 'diff'" class="space-y-4">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <p class="text-xs text-gray-400 mb-1">原文</p>
              <textarea v-model="diffLeft" class="web3-input h-48 font-mono text-xs" placeholder="输入原始文本..." @input="computeDiff"></textarea>
            </div>
            <div>
              <p class="text-xs text-gray-400 mb-1">修改后</p>
              <textarea v-model="diffRight" class="web3-input h-48 font-mono text-xs" placeholder="输入对比文本..." @input="computeDiff"></textarea>
            </div>
          </div>
          <div v-if="diffResult.length" class="glass-panel-sm overflow-x-auto">
            <table class="w-full text-left text-xs font-mono">
              <thead><tr class="border-b border-white/[0.06]">
                <th class="px-3 py-2 text-gray-500 w-10">#</th>
                <th class="px-3 py-2 text-gray-500">原文</th>
                <th class="px-3 py-2 text-gray-500">修改后</th>
              </tr></thead>
              <tbody>
                <tr v-for="row in diffResult" :key="row.line" :class="row.type === 'diff' ? 'bg-red-500/[0.06]' : ''">
                  <td class="px-3 py-1.5 text-gray-600">{{ row.line }}</td>
                  <td class="px-3 py-1.5" :class="row.left === null ? 'text-gray-700' : row.type === 'diff' ? 'text-red-300' : 'text-gray-400'">{{ row.left ?? '∅' }}</td>
                  <td class="px-3 py-1.5" :class="row.right === null ? 'text-gray-700' : row.type === 'diff' ? 'text-green-300' : 'text-gray-400'">{{ row.right ?? '∅' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// ====================================================
// 在线工具台：JSON/Base64/时间戳/UUID/正则/Hash/
// 二维码/颜色转换/URL 编解码/文本 Diff 等综合工具
// ====================================================
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { useToastStore } from '@/stores/modules/toast'
import CryptoJS from 'crypto-js'
import QRCode from 'qrcode'

const route = useRoute()
const toast = useToastStore()
const activeTool = ref('')
const input = ref('')
const output = ref(null)
const outputError = ref(false)
const placeholder = ref('')

// Timestamp
const tsInput = ref('')
const tsResult = ref('')
const dateInput = ref('')
const dateResult = ref('')
const nowTs = ref(0)
const nowDate = ref('')
let tsInterval = null

// UUID
const generatedUUID = ref('')

// Regex
const regexPattern = ref('')
const regexFlags = ref('gi')
const regexTestStr = ref('')
const regexMatches = ref([])
const regexError = ref('')

// Hash
const hashInput = ref('')
const hashResults = ref({})

// QR Code
const qrText = ref('')
const qrDataUrl = ref('')
const qrSize = ref(256)

// Color
const colorInput = ref('')
const colorResults = ref({})

// URL
const urlInput = ref('')
const urlOutput = ref('')

// Diff
const diffLeft = ref('')
const diffRight = ref('')
const diffResult = ref([])

const toolConfig = {
  json: { title: 'JSON 格式化', desc: '格式化 · 压缩 · 验证 JSON 数据', placeholder: '粘贴JSON数据...' },
  base64: { title: 'Base64 编解码', desc: 'Base64 加密 · 解密转换', placeholder: '输入文本...' },
  timestamp: { title: '时间戳转换', desc: 'Unix时间戳 ↔ 日期 互转', placeholder: '' },
  uuid: { title: 'UUID 生成器', desc: '在线生成 Version 4 UUID', placeholder: '' },
  regex: { title: '正则表达式测试', desc: '在线正则匹配调试，实时高亮匹配结果', placeholder: '输入正则表达式...' },
  hash: { title: 'Hash 生成器', desc: 'MD5 · SHA1 · SHA256 · SHA512 哈希计算', placeholder: '输入待计算内容...' },
  qrcode: { title: '二维码生成', desc: '在线生成 QR Code 二维码图片', placeholder: '输入要编码的文本或URL...' },
  color: { title: '颜色转换', desc: 'HEX ↔ RGB ↔ HSL 颜色格式互转', placeholder: '输入颜色值，如 #ff6600 或 rgb(255,102,0)' },
  url: { title: 'URL 编解码', desc: 'encodeURIComponent / decodeURIComponent URL编码转换', placeholder: '输入URL或文本...' },
  diff: { title: '文本差异对比', desc: '并排对比两段文本的差异，高亮不同行', placeholder: '' }
}

const toolTitle = computed(() => toolConfig[activeTool.value]?.title || '在线工具')
const toolDesc = computed(() => toolConfig[activeTool.value]?.desc || '')

// ==================== JSON ====================
// 格式化 JSON 文本并输出（失败提示错误）
function formatJSON() {
  try { output.value = JSON.stringify(JSON.parse(input.value), null, 2); outputError.value = false }
  catch (e) { output.value = e.message; outputError.value = true }
}
// 压缩 JSON 文本为单行
function compressJSON() {
  try { output.value = JSON.stringify(JSON.parse(input.value)); outputError.value = false }
  catch (e) { output.value = e.message; outputError.value = true }
}
// 校验 JSON 文本是否合法
function validateJSON() {
  try { JSON.parse(input.value); output.value = '✅ 有效的 JSON'; outputError.value = false }
  catch (e) { output.value = '❌ ' + e.message; outputError.value = true }
}

// ==================== Base64 ====================
// Base64 编码
function base64Encode() {
  try { output.value = btoa(unescape(encodeURIComponent(input.value))) }
  catch (e) { output.value = e.message }
}
// Base64 解码
function base64Decode() {
  try { output.value = decodeURIComponent(escape(atob(input.value))) }
  catch (e) { output.value = e.message }
}

// ==================== Timestamp ====================
// 时间戳（秒）转日期字符串
function tsToDate() {
  const ts = parseInt(tsInput.value)
  if (isNaN(ts)) { tsResult.value = '无效时间戳'; return }
  const d = new Date(ts * 1000)
  tsResult.value = d.toLocaleString('zh-CN', { year:'numeric', month:'2-digit', day:'2-digit', hour:'2-digit', minute:'2-digit', second:'2-digit' })
}
// 日期字符串转时间戳（秒）
function dateToTs() {
  const ts = new Date(dateInput.value).getTime()
  if (isNaN(ts)) { dateResult.value = '无效日期'; return }
  dateResult.value = String(Math.floor(ts / 1000))
}
// 刷新当前时间戳与当前时间显示
function updateNow() {
  nowTs.value = Math.floor(Date.now() / 1000)
  nowDate.value = new Date().toLocaleString('zh-CN')
}

// ==================== UUID ====================
// 生成 UUID v4
function generateUUID() {
  generatedUUID.value = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
    const r = Math.random() * 16 | 0; return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16)
  })
}

// ==================== Regex ====================
// 按正则模式与标志位执行匹配并列出结果
function runRegex() {
  regexError.value = ''
  regexMatches.value = []
  if (!regexPattern.value.trim()) { regexError.value = '请输入正则表达式'; return }
  try {
    const re = new RegExp(regexPattern.value, regexFlags.value)
    let match
    const matches = []
    if (re.global) {
      while ((match = re.exec(regexTestStr.value)) !== null) {
        matches.push({ text: match[0], index: match.index, groups: match.slice(1) })
      }
    } else {
      match = re.exec(regexTestStr.value)
      if (match) matches.push({ text: match[0], index: match.index, groups: match.slice(1) })
    }
    regexMatches.value = matches
  } catch (e) {
    regexError.value = '正则表达式语法错误: ' + e.message
  }
}

// ==================== Hash ====================
// 计算输入文本的 MD5/SHA1/SHA256 哈希
function computeHashes() {
  const text = hashInput.value
  if (!text) { hashResults.value = {}; return }
  hashResults.value = {
    MD5: CryptoJS.MD5(text).toString(),
    SHA1: CryptoJS.SHA1(text).toString(),
    SHA256: CryptoJS.SHA256(text).toString(),
    SHA512: CryptoJS.SHA512(text).toString()
  }
}

// 复制单个哈希结果到剪贴板
function copyHash(value) {
  navigator.clipboard?.writeText(value).then(() => toast.success('已复制')).catch(() => toast.error('复制失败'))
}

// ==================== QR Code ====================
// 生成二维码并输出为 DataURL 图片
async function generateQR() {
  if (!qrText.value.trim()) { qrDataUrl.value = ''; return }
  try {
    qrDataUrl.value = await QRCode.toDataURL(qrText.value, {
      width: qrSize.value,
      margin: 2,
      color: { dark: '#a855f7', light: '#0a0a1a' }
    })
  } catch (e) {
    toast.error('生成失败: ' + e.message)
  }
}

// 下载生成的二维码图片
function downloadQR() {
  if (!qrDataUrl.value) return
  const a = document.createElement('a')
  a.href = qrDataUrl.value
  a.download = 'qrcode.png'
  a.click()
}

// ==================== Color ====================
// 解析 HEX/RGB/HSL 格式颜色并返回 RGB 分量
function parseColor(val) {
  val = val.trim()
  // HEX
  if (val.startsWith('#')) {
    let hex = val.slice(1)
    if (hex.length === 3) hex = hex.split('').map(c => c + c).join('')
    if (hex.length !== 6) return null
    const r = parseInt(hex.slice(0, 2), 16)
    const g = parseInt(hex.slice(2, 4), 16)
    const b = parseInt(hex.slice(4, 6), 16)
    return { r, g, b }
  }
  // rgb()
  const rgbMatch = val.match(/rgba?\s*\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)/)
  if (rgbMatch) return { r: +rgbMatch[1], g: +rgbMatch[2], b: +rgbMatch[3] }
  return null
}

// 颜色格式互转：输出 HEX/RGB/HSL 结果
function convertColor() {
  const c = parseColor(colorInput.value)
  if (!c) { colorResults.value = {}; return }
  const { r, g, b } = c
  const hex = '#' + [r, g, b].map(v => v.toString(16).padStart(2, '0')).join('')
  // RGB to HSL
  const rr = r / 255, gg = g / 255, bb = b / 255
  const max = Math.max(rr, gg, bb), min = Math.min(rr, gg, bb)
  const l = (max + min) / 2
  let h = 0, s = 0
  if (max !== min) {
    const d = max - min
    s = l > 0.5 ? d / (2 - max - min) : d / (max + min)
    switch (max) {
      case rr: h = ((gg - bb) / d + (gg < bb ? 6 : 0)) / 6; break
      case gg: h = ((bb - rr) / d + 2) / 6; break
      case bb: h = ((rr - gg) / d + 4) / 6; break
    }
  }
  colorResults.value = {
    hex: hex.toUpperCase(),
    rgb: `rgb(${r}, ${g}, ${b})`,
    rgba: `rgba(${r}, ${g}, ${b}, 1)`,
    hsl: `hsl(${Math.round(h * 360)}, ${Math.round(s * 100)}%, ${Math.round(l * 100)}%)`,
    r, g, b
  }
}

// 复制颜色转换结果到剪贴板
function copyColor(val) {
  navigator.clipboard?.writeText(val).then(() => toast.success('已复制')).catch(() => toast.error('复制失败'))
}

// ==================== URL ====================
// URL 编码（encodeURIComponent）
function urlEncode() {
  try { urlOutput.value = encodeURIComponent(urlInput.value) }
  catch (e) { urlOutput.value = e.message }
}
// URL 解码（decodeURIComponent）
function urlDecode() {
  try { urlOutput.value = decodeURIComponent(urlInput.value) }
  catch (e) { urlOutput.value = '解码失败: ' + e.message }
}

// ==================== Diff ====================
// 逐行对比左右文本差异并标记增删
function computeDiff() {
  const leftLines = (diffLeft.value || '').split('\n')
  const rightLines = (diffRight.value || '').split('\n')
  const maxLen = Math.max(leftLines.length, rightLines.length)
  const result = []
  for (let i = 0; i < maxLen; i++) {
    const l = leftLines[i] ?? null
    const r = rightLines[i] ?? null
    if (l === r) result.push({ type: 'same', left: l, right: r, line: i + 1 })
    else result.push({ type: 'diff', left: l, right: r, line: i + 1 })
  }
  diffResult.value = result
}

// ==================== Common ====================
// 清空输入与输出
function clearInput() { input.value = ''; output.value = null }
// 复制输出内容到剪贴板
function copyOutput() {
  if (output.value) navigator.clipboard?.writeText(output.value).then(() => toast.success('已复制'))
}
// 复制生成的 UUID 到剪贴板
function copyUUID() { navigator.clipboard?.writeText(generatedUUID.value).then(() => toast.success('已复制')) }

// ==================== Watch tool switch ====================
watch(() => route.params.tool, (val) => {
  activeTool.value = val
  input.value = ''; output.value = null
  tsInput.value = ''; tsResult.value = ''; dateInput.value = ''; dateResult.value = ''
  placeholder.value = toolConfig[val]?.placeholder || ''
  if (val === 'uuid') generateUUID()
  if (val === 'regex') { runRegex() }
}, { immediate: true })

onMounted(() => {
  activeTool.value = route.params.tool
  if (activeTool.value === 'uuid') generateUUID()
  tsInterval = setInterval(updateNow, 1000)
  updateNow()
})
onBeforeUnmount(() => clearInterval(tsInterval))
</script>
