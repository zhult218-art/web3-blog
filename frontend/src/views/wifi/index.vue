<template>
  <div class="wifi-page">
    <div class="wifi-container">
      <!-- 头部 -->
      <header class="wifi-hero">
        <div class="hero-badge">📶</div>
        <h1 class="hero-title">WiFi 安全审计</h1>
        <p class="hero-sub">周边网络扫描 · 无线网卡与本机配置审计 · 钓鱼热点 / WPA破解 / wifiphisher / wifite</p>

        <div class="hero-meta">
          <span class="meta-chip" :class="caps.platform">平台：{{ platformLabel }}</span>
          <span class="meta-chip" :class="caps.elevated ? 'ok' : 'warn'">
            权限：{{ elevatedLabel }}{{ caps.elevated ? '' : '（未提权）' }}
          </span>
          <span class="meta-chip">可见网络：{{ networks.length }}</span>
        </div>
        <p class="hero-disclaimer">⚠️ 仅可用于你拥有或已获书面授权的网络进行安全测试，禁止任何非法用途。</p>

        <!-- 加载失败横幅 -->
        <div v-if="loadError" class="load-banner">
          <span>⚠️ {{ loadError }}</span>
          <button class="btn small" @click="init">重试连接</button>
        </div>
      </header>

      <!-- Tab 导航 -->
      <nav class="wifi-tabs">
        <button v-for="t in tabs" :key="t.key" class="tab-btn"
          :class="{ active: tab === t.key }" @click="tab = t.key">
          <span>{{ t.icon }}</span> {{ t.label }}
        </button>
      </nav>

      <!-- ============ 周边扫描 ============ -->
      <section v-if="tab === 'scan'" class="panel">
        <div class="panel-head">
          <h2>周边网络</h2>
          <button class="btn primary" :disabled="scanning" @click="doScan">
            {{ scanning ? '扫描中…' : '🔄 重新扫描' }}
          </button>
        </div>

        <div v-if="!networks.length && !scanning" class="empty">未发现网络（请确认无线网卡已启用）。</div>

        <div class="net-list">
          <article v-for="(net, i) in networks" :key="i" class="net-card"
            :class="{ open: openNets.has(i) }" @click="toggleNet(i)">
            <div class="net-main">
              <div class="net-id">
                <span class="net-ssid">{{ net.hidden ? '（隐藏网络）' : net.ssid }}</span>
                <span class="net-auth">{{ net.authentication || '开放' }}</span>
                <span class="net-enc" v-if="net.encryption">{{ net.encryption }}</span>
              </div>
              <div class="net-side">
                <div class="signal">
                  <div class="signal-bar">
                    <div class="signal-fill" :style="{ width: (net.best_signal ?? 0) + '%', background: signalColor(net.best_signal) }"></div>
                  </div>
                  <span class="signal-num">{{ net.best_signal ?? '—' }}{{ net.best_signal != null ? '%' : '' }}</span>
                </div>
                <span class="net-chan">CH {{ net.channel ?? '—' }}</span>
                <span class="net-aps">{{ net.ap_count }} AP</span>
                <span class="net-caret">{{ openNets.has(i) ? '▾' : '▸' }}</span>
              </div>
            </div>

            <div v-if="openNets.has(i) && net.bssids.length" class="bssid-list" @click.stop>
              <div v-for="(ap, j) in net.bssids" :key="j" class="bssid-row">
                <code>{{ ap.bssid }}</code>
                <span>CH {{ ap.channel ?? '—' }}</span>
                <span>{{ ap.signal ?? '—' }}%</span>
                <span v-if="ap.radio">{{ ap.radio }}</span>
              </div>
            </div>
          </article>
        </div>
      </section>

      <!-- ============ 无线网卡 ============ -->
      <section v-if="tab === 'iface'" class="panel">
        <div class="panel-head">
          <h2>无线网卡</h2>
          <button class="btn" @click="loadInterfaces">刷新</button>
        </div>
        <div v-if="!interfaces.length" class="empty">未检测到无线网卡。</div>
        <div class="iface-grid">
          <article v-for="(it, i) in interfaces" :key="i" class="iface-card">
            <h3>{{ it.name || '无线网卡' }}</h3>
            <dl>
              <template v-if="isWindows">
                <div><dt>状态</dt><dd>{{ it.state }}</dd></div>
                <div><dt>描述</dt><dd>{{ it.description }}</dd></div>
                <div><dt>MAC</dt><dd>{{ it.mac }}</dd></div>
                <div><dt>SSID</dt><dd>{{ it.ssid || '—' }}</dd></div>
                <div><dt>信道</dt><dd>{{ it.channel ?? '—' }}</dd></div>
                <div><dt>信号</dt><dd>{{ it.signal ?? '—' }}{{ it.signal != null ? '%' : '' }}</dd></div>
                <div><dt>制式</dt><dd>{{ it.radio }}</dd></div>
              </template>
              <template v-else>
                <div><dt>状态</dt><dd>{{ it.state }}</dd></div>
                <div><dt>SSID</dt><dd>{{ it.ssid || '—' }}</dd></div>
                <div v-if="it.monitor"><dt>模式</dt><dd>monitor</dd></div>
              </template>
            </dl>
          </article>
        </div>
      </section>

      <!-- ============ 配置审计（Windows） ============ -->
      <section v-if="tab === 'profile'" class="panel">
        <div class="panel-head">
          <h2>本机已存配置</h2>
          <button class="btn" @click="loadProfiles">刷新</button>
        </div>

        <div v-if="!isWindows" class="empty">该能力仅在 Windows 提供。</div>
        <template v-else>
          <p class="hint">展示当前用户保存过的无线配置；点击查看详情（读取明文密钥需以管理员运行）。</p>
          <div class="prof-chips">
            <button v-for="p in profiles" :key="p" class="prof-chip"
              :class="{ active: profileDetail?.name === p }" @click="showProfile(p)">{{ p }}</button>
          </div>

          <div v-if="profileDetail" class="prof-detail">
            <h3>{{ profileDetail.name }}</h3>
            <div class="pwd-row">
              <span>保存的密钥：</span>
              <code>{{ profileDetail.password || '（未读取到 / 需要管理员）' }}</code>
            </div>
          </div>
        </template>
      </section>

      <!-- ============ 攻击引擎 ============ -->
      <section v-if="tab === 'engine'" class="panel">
        <div class="panel-head"><h2>攻击引擎</h2></div>

        <!-- ═══════ Windows 原生引擎 ═══════ -->
        <div v-if="isWindows" class="win-engines">

          <!-- ── 邪恶孪生·钓鱼热点 ── -->
          <div class="engine-block">
            <div class="block-title">
              <h3>📡 邪恶孪生 · 钓鱼热点</h3>
              <span class="mode-tag" v-if="rogue.running" style="color:#6ee7b7">运行中</span>
            </div>

            <!-- 未运行：配置 -->
            <div v-if="!rogue.running" class="rogue-form">
              <p class="hint">创建一个伪造的 WiFi 热点，连接设备会被强制跳转到仿冒登录页，输入的 WiFi 密码会被记录。</p>
              <p class="hint" v-if="rogue.reason && !rogue.needs">引擎：{{ rogue.reason }}</p>
              <div class="form-row">
                <label>热点名称（仿冒 SSID）</label>
                <input v-model="rogueForm.ssid" placeholder="例如：ChinaNet-Free">
              </div>
              <div class="form-row">
                <label>热点密码</label>
                <input v-model="rogueForm.key" placeholder="至少 8 位">
              </div>
              <div class="form-row">
                <label>钓鱼页面模板</label>
                <select v-model="rogueForm.template">
                  <option value="wifi_login">通用 WiFi 登录页</option>
                  <option value="firmware">路由器固件升级</option>
                  <option value="oauth">运营商认证页</option>
                </select>
              </div>
              <div v-if="rogue.needs" class="need-warn">{{ rogue.needs }}</div>
              <button class="btn primary" :disabled="rogue.busy" @click="launchRogue">
                {{ rogue.busy ? '启动中…' : '🚀 启动钓鱼热点' }}
              </button>
            </div>

            <!-- 运行中：监控 -->
            <div v-else class="rogue-monitor">
              <div class="mon-grid">
                <div><span>SSID</span><strong>{{ rogue.ssid }}</strong></div>
                <div><span>门户地址</span><strong>{{ rogue.bound_ip }}</strong></div>
                <div><span>模式</span><strong>{{ rogue.mode === 'hotspot' ? '移动热点（免管理员）' : '承载网络（管理员）' }}</strong></div>
                <div><span>模板</span><strong>{{ rogue.template }}</strong></div>
                <div><span>DNS 劫持</span><strong :class="rogue.dns_active ? 'got' : ''">{{ rogue.dns_active ? '已接管' : '未接管' }}</strong></div>
                <div><span>DNS 查询</span><strong>{{ rogue.dns_queries }}</strong></div>
                <div><span>已连接设备</span><strong>{{ rogue.leases.length }}</strong></div>
                <div><span>捕获口令</span><strong :class="rogue.captures.length ? 'got' : ''">{{ rogue.captures.length }}</strong></div>
              </div>

              <div v-if="rogue.leases.length" class="sub-box">
                <h4>已连接设备（DHCP 租约）</h4>
                <div v-for="l in rogue.leases" :key="l.ip" class="lease-row">
                  <code>{{ l.ip }}</code><span>{{ l.mac }}</span><span>{{ l.name }}</span>
                </div>
              </div>

              <div v-if="rogue.captures.length" class="sub-box captures-box">
                <h4>★ 捕获的凭据</h4>
                <div v-for="(c,ci) in rogue.captures" :key="ci" class="cap-card">
                  <div class="cap-time">{{ c.time }} · {{ c.ip }}</div>
                  <div v-for="(v,k) in c.fields" :key="k" class="cap-field">
                    <span>{{ k }}</span><code>{{ v }}</code>
                  </div>
                </div>
              </div>

              <button class="btn danger" :disabled="rogue.busy" @click="killRogue">停止热点</button>
            </div>
          </div>

          <!-- ── WPA/WPA2 离线破解 ── -->
          <div class="engine-block">
            <div class="block-title">
              <h3>🔑 WPA/WPA2 离线破解</h3>
              <span class="mode-tag" :class="hashcatInfo ? 'ok' : 'warn'">
                {{ hashcatInfo ? 'hashcat 可用' : 'Python 引擎' }}
              </span>
            </div>

            <p class="hint">
              输入 hashcat 22000 格式握手（在 Linux 上用 wifite/hcxdumptool 抓取），
              配上字典即可离线破解；检测到 hashcat 时自动走 GPU 加速。
            </p>

            <div class="crack-tabs">
              <button :class="{active: crackMode==='paste'}" @click="crackMode='paste'">粘贴哈希</button>
              <button :class="{active: crackMode==='upload'}" @click="crackMode='upload'">上传文件</button>
              <button :class="{active: crackMode==='path'}" @click="crackMode='path'">服务器路径</button>
            </div>

            <!-- 破解模式：字典 / 规则 / 暴力 -->
            <div class="form-row" style="margin-bottom: .8rem;">
              <label>破解模式</label>
              <div class="attack-mode-row">
                <label class="radio-chip" :class="{active: crackAttackMode==='dictionary'}">
                  <input type="radio" v-model="crackAttackMode" value="dictionary"> 📖 字典
                </label>
                <label class="radio-chip" :class="{active: crackAttackMode==='rule'}">
                  <input type="radio" v-model="crackAttackMode" value="rule"> 📋 规则
                </label>
                <label class="radio-chip" :class="{active: crackAttackMode==='brute'}">
                  <input type="radio" v-model="crackAttackMode" value="brute"> 🔨 暴力
                </label>
              </div>
            </div>
            <!-- 规则模式：规则文件路径 -->
            <div v-if="crackAttackMode==='rule'" class="form-row">
              <label>规则文件（hashcat -r）</label>
              <input v-model="crackForm.rulePath" placeholder="Z:\rules\best64.rule">
            </div>
            <!-- 暴力模式：掩码 -->
            <div v-if="crackAttackMode==='brute'" class="form-row">
              <label>掩码（hashcat -a 3）</label>
              <input v-model="crackForm.mask" placeholder="?d?d?d?d?d?d?d?d（8位纯数字）">
              <small class="hint">?d=数字 ?l=小写 ?u=大写 ?s=特殊 ?a=全部 ?h=十六进制</small>
            </div>

            <!-- 粘贴 -->
            <template v-if="crackMode==='paste'">
              <textarea v-model="crackForm.hash" rows="4"
                placeholder="WPA*01*... 或 WPA*02*...（每行一个）"></textarea>
              <div class="form-row">
                <label>字典文件（服务器路径）</label>
                <input v-model="crackForm.wordlistPath" placeholder="Z:\wordlists\rockyou.txt">
              </div>
              <button class="btn primary" @click="startCrackPaste">开始破解</button>
            </template>

            <!-- 上传 -->
            <template v-if="crackMode==='upload'">
              <div class="up-row">
                <div>
                  <label>哈希文件（.22000 / .hccapx）</label>
                  <input type="file" @change="onHashFile" accept=".txt,.22000,.hccapx">
                </div>
                <div>
                  <label>字典文件（.txt）</label>
                  <input type="file" @change="onWordlist" accept=".txt">
                </div>
              </div>
              <button class="btn primary" :disabled="!files.hash||!files.wordlist" @click="startCrackUpload">
                上传并破解
              </button>
            </template>

            <!-- 路径 -->
            <template v-if="crackMode==='path'">
              <div class="form-row">
                <label>哈希文件路径</label>
                <input v-model="crackForm.hashPath" placeholder="Z:\handshakes\target.22000">
              </div>
              <div class="form-row">
                <label>字典文件路径</label>
                <input v-model="crackForm.wordlistPath" placeholder="Z:\wordlists\rockyou.txt">
              </div>
              <button class="btn primary" @click="startCrackFromPath">开始破解</button>
            </template>

            <!-- 任务列表 -->
            <div v-if="crackJobs.length" class="sub-box">
              <h4>破解任务（{{ crackJobs.length }}）</h4>
              <div v-for="j in crackJobs" :key="j.jid" class="job-row"
                :class="{active: consoleJob?.jid === j.jid}">
                <button class="job-main" @click="openCrackConsole(j)">
                  <strong>{{ j.engine }}</strong>
                  <span class="jstatus" :class="j.status">{{ j.status }}</span>
                  <span class="jprog">{{ j.tried }} tried</span>
                  <span v-if="j.found.length" class="jfound">★ {{ j.found.length }} 命中</span>
                </button>
                <button class="btn mini danger" :disabled="j.status!=='running'" @click="stopCrack(j)">停止</button>
              </div>
            </div>

            <!-- 实时日志 -->
            <div v-if="consoleJob" class="console">
              <div class="console-head">
                <span>{{ consoleJob.jid.slice(0,8) }} · {{ consoleJob.status }}</span>
                <span v-if="consoleJob.found.length" class="jfound">★ {{ consoleJob.found[0].password }}</span>
              </div>
              <pre class="console-body">{{ consoleText }}</pre>
            </div>
          </div>
        </div>

        <!-- ═══════ Linux 引擎 ═══════ -->
        <div v-else class="engine-grid">
          <article v-for="e in engines" :key="e.name" class="engine-card"
            :class="e.available ? 'ready' : 'down'">
            <div class="engine-head">
              <h3>{{ e.name }}</h3>
              <span class="engine-mode">{{ e.mode }}</span>
            </div>
            <p class="engine-reason">{{ e.available ? '可启动' : e.reason }}</p>

            <div class="arg-box" v-if="e.available">
              <label>参数（空格分隔，支持引号）</label>
              <input v-model="argInputs[e.name]" class="arg-input"
                :placeholder="e.name === 'wifiphisher' ? '-i wlan0 -e Free-WiFi' : '-i wlan0 --kill'">
              <div class="presets">
                <button v-for="pre in presets(e.name)" :key="pre" class="preset"
                  @click="argInputs[e.name] = pre">{{ pre }}</button>
              </div>
              <button class="btn primary" @click="launch(e)">启动会话</button>
            </div>
          </article>

          <div v-if="sessions.length" class="sessions">
            <h3>会话</h3>
            <div v-for="s in sessions" :key="s.sid" class="session-row"
              :class="{ active: consoleSid === s.sid }">
              <button class="sess-main" @click="openConsole(s)">
                <strong>{{ s.engine }}</strong>
                <span class="sess-status" :class="s.status">{{ s.status }}</span>
                <code class="sess-id">{{ s.sid.slice(0, 10) }}</code>
              </button>
              <button class="btn small danger" :disabled="s.status !== 'running'" @click="stop(s)">停止</button>
            </div>
          </div>

          <div v-if="consoleLines" class="console">
            <div class="console-head">
              <span>实时日志 · {{ consoleSid?.slice(0, 10) }}</span>
              <span class="sess-status" :class="consoleStatus">{{ consoleStatus }}</span>
            </div>
            <pre class="console-body">{{ consoleLines }}</pre>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
// ============================================================
// WiFi 安全审计页
// ============================================================
import { computed, reactive, ref, onMounted, onBeforeUnmount } from 'vue'
import {
  getCapabilities, scanNetworks, getInterfaces, listProfiles, getProfile,
  getEngines, createSession, listSessions, stopSession, sessionWsUrl,
  getRogueStatus, startRogue, stopRogue,
  getHashcatInfo, createCrackJob, uploadCrackJob, listCrackJobs,
  stopCrackJob, crackWsUrl,
} from '@/api/wifi'

const tab = ref('scan')
const tabs = [
  { key: 'scan', label: '周边扫描', icon: '🛰️' },
  { key: 'iface', label: '无线网卡', icon: '📡' },
  { key: 'profile', label: '配置审计', icon: '🗂️' },
  { key: 'engine', label: '攻击引擎', icon: '⚔️' },
]

const caps = reactive({ platform: 'windows', elevated: false, win_engines: [] })
const isWindows = computed(() => caps.platform === 'windows')
const platformLabel = computed(() => ({ windows: 'Windows', linux: 'Linux', macos: 'macOS' }[caps.platform] || caps.platform))
const elevatedLabel = computed(() => caps.platform === 'linux' ? 'root' : '管理员')

const scanning = ref(false)
const networks = ref([])
const interfaces = ref([])
const openNets = reactive(new Set())
function toggleNet(i) { openNets.has(i) ? openNets.delete(i) : openNets.add(i) }

const profiles = ref([])
const profileDetail = ref(null)

const engines = ref([])
const sessions = ref([])
const argInputs = reactive({ wifiphisher: '', wifite: '' })
const loadError = ref('')

// ── Linux 控制台 ──
const consoleSid = ref(null)
const consoleStatus = ref('')
const consoleLogs = ref([])
const consoleLines = computed(() => consoleLogs.value.join('\n'))
let ws = null

// ── Rogue AP ──
const rogue = reactive({
  running: false, ssid: '', template: 'wifi_login', bound_ip: '',
  dns_queries: 0, leases: [], captures: [], needs: '', busy: false,
  mode: '', dns_active: null, reason: '',
})
const rogueForm = reactive({ ssid: '', key: '', template: 'wifi_login' })

// ── Crack ──
const hashcatInfo = ref('')
const crackMode = ref('paste')
const crackAttackMode = ref('dictionary')
const crackForm = reactive({ hash: '', wordlistPath: '', hashPath: '', rulePath: '', mask: '' })
const files = reactive({ hash: null, wordlist: null })
const crackJobs = ref([])
const consoleJob = ref(null)
const consoleText = ref('')
let crackWs = null

function signalColor(v) {
  if (v == null) return '#64748b'
  if (v >= 60) return '#34d399'
  if (v >= 35) return '#fbbf24'
  return '#fb7185'
}

const sleep = (ms) => new Promise(r => setTimeout(r, ms))

// 带重试的加载器：仅对「无响应」（服务未就绪）重试，服务端错误直接抛出
async function withRetry(fn, name) {
  let lastErr
  for (let i = 0; i < 5; i++) {
    try {
      return await fn({ suppressError: true })
    } catch (e) {
      lastErr = e
      if (e.response) break                 // 服务已响应（500 等），重试无意义
      if (i < 4) await sleep(1000 * (i + 1)) // 1s/2s/3s/4s
    }
  }
  throw lastErr
}

async function loadCapabilities() {
  const c = await getCapabilities()
  Object.assign(caps, c)
  engines.value = c.engines || []
  if (caps.platform === 'windows') {
    const rogueInfo = (c.win_engines || [])[0]
    if (rogueInfo) rogue.reason = rogueInfo.reason
    if (rogueInfo && !rogueInfo.available) rogue.needs = rogueInfo.reason
  }
}

async function doScan() {
  scanning.value = true
  try {
    const r = await scanNetworks()
    networks.value = r.networks || []
  } finally {
    scanning.value = false
  }
}

async function loadInterfaces() {
  const r = await getInterfaces()
  interfaces.value = r.adapters || []
}

async function loadProfiles() {
  if (!isWindows.value) return
  const r = await listProfiles()
  profiles.value = r.profiles || []
}

async function showProfile(name) {
  profileDetail.value = await getProfile(name)
}

// ── Linux 引擎 ──
function tokenize(input) {
  const out = []
  const re = /"([^"]*)"|'([^']*)'|(\S+)/g
  let m
  while ((m = re.exec(input))) out.push(m[1] ?? m[2] ?? m[3])
  return out
}

function presets(name) {
  return name === 'wifiphisher'
    ? ['-i wlan0 -e Free-WiFi', '-i wlan0 -p firmware-upgrade']
    : ['-i wlan0 --kill', '-i wlan0 --wpa --crack']
}

async function launch(e) {
  const args = tokenize(argInputs[e.name] || '')
  const s = await createSession(e.name, args)
  sessions.value.push(s)
  openConsole(s)
}

async function refreshSessions() {
  const r = await listSessions()
  sessions.value = r.sessions || []
}

async function stop(s) {
  await stopSession(s.sid)
  await refreshSessions()
}

function openConsole(s) {
  consoleSid.value = s.sid
  consoleStatus.value = s.status
  consoleLogs.value = []
  if (ws) { ws.onclose = null; ws.close() }
  ws = new WebSocket(sessionWsUrl(s.sid))
  ws.onmessage = (ev) => {
    const msg = JSON.parse(ev.data)
    if (msg.type === 'log') consoleLogs.value.push(msg.line)
    else if (msg.type === 'status') consoleStatus.value = msg.status
  }
}

// ── Rogue 热点控制 ──
async function launchRogue() {
  if (!rogueForm.ssid || !rogueForm.key) { rogue.needs = '请填写 SSID 与密码'; return }
  rogue.busy = true
  rogue.needs = ''
  try {
    const st = await startRogue(rogueForm.ssid, rogueForm.key, rogueForm.template)
    applyRogue(st)
  } catch (e) {
    // FastAPI 错误体为 {detail}，Java 网关为 {message}
    rogue.needs = e.response?.data?.detail || e.response?.data?.message || e.message
  } finally {
    rogue.busy = false
  }
}

async function killRogue() {
  rogue.busy = true
  try {
    await stopRogue()
    rogue.running = false
  } finally {
    rogue.busy = false
  }
}

function applyRogue(st) {
  rogue.running = st.running
  rogue.ssid = st.ssid
  rogue.template = st.template
  rogue.bound_ip = st.bound_ip
  rogue.mode = st.mode || ''
  rogue.dns_active = st.dns_active ?? null
  rogue.dns_queries = st.dns_queries
  rogue.leases = st.leases || []
  rogue.captures = st.captures || []
}

// ── 破解任务 ──
function onHashFile(e) { files.hash = e.target.files[0] }
function onWordlist(e) { files.wordlist = e.target.files[0] }

async function startCrackPaste() {
  if (!crackForm.hash.trim() || !crackForm.wordlistPath.trim()) return
  const j = await createCrackJob(crackForm.hash, crackForm.wordlistPath, 'auto', '',
    crackAttackMode.value, crackForm.rulePath, crackForm.mask)
  crackJobs.value.push(j)
  openCrackConsole(j)
}

async function startCrackFromPath() {
  if (!crackForm.hashPath.trim() || !crackForm.wordlistPath.trim()) return
  // 哈希文件由后端按服务器路径读取（hash_path），浏览器无需访问本地文件
  const j = await createCrackJob('', crackForm.wordlistPath, 'auto', crackForm.hashPath,
    crackAttackMode.value, crackForm.rulePath, crackForm.mask)
  crackJobs.value.push(j)
  openCrackConsole(j)
}

async function startCrackUpload() {
  const fd = new FormData()
  fd.append('hash_file', files.hash)
  fd.append('wordlist', files.wordlist)
  const j = await uploadCrackJob(fd)
  crackJobs.value.push(j)
  openCrackConsole(j)
}

function openCrackConsole(job) {
  const j = typeof job === 'string' ? crackJobs.value.find(x => x.jid === job) : job
  consoleJob.value = j
  consoleText.value = ''
  if (crackWs) { crackWs.onclose = null; crackWs.close() }
  crackWs = new WebSocket(crackWsUrl(j.jid))
  crackWs.onmessage = (ev) => {
    const msg = JSON.parse(ev.data)
    if (msg.type === 'log') consoleText.value += msg.line + '\n'
    else if (msg.type === 'status') {
      j.status = msg.status
      j.tried = msg.tried
      j.found = msg.found
      consoleJob.value = { ...j }
    }
  }
}

async function stopCrack(j) {
  await stopCrackJob(j.jid)
}

async function refreshCrackJobs() {
  const r = await listCrackJobs()
  crackJobs.value = r.jobs || []
}

let roguePoll = null

async function init() {
  loadError.value = ''
  try {
    await withRetry(loadCapabilities, 'capabilities')
    const scanR = await withRetry(() => scanNetworks(), 'scan')
    networks.value = scanR.networks || []
    const ifR = await withRetry(getInterfaces, 'interfaces')
    interfaces.value = ifR.adapters || []
    if (isWindows.value) {
      const pr = await withRetry(listProfiles, 'profiles')
      profiles.value = pr.profiles || []
      hashcatInfo.value = (await getHashcatInfo()).hashcat
      const rs = await getRogueStatus()
      if (rs.running) applyRogue(rs)
      await refreshCrackJobs()
      // 热点运行时轮询刷新
      roguePoll = setInterval(async () => {
        try {
          const s = await getRogueStatus()
          if (s.running) applyRogue(s); else { rogue.running = false; clearInterval(roguePoll) }
        } catch { /* ignore */ }
      }, 4000)
    } else {
      await refreshSessions()
    }
  } catch (e) {
    loadError.value = e.response
      ? `服务已响应但发生错误：${e.response.data?.message || e.response.data?.detail || e.response.status}`
      : '无法连接后端服务（py-service 未启动或尚未就绪）'
  }
}

onMounted(init)

// 页面离开时清理长连接与轮询，避免后台泄漏（WebSocket/定时器）
onBeforeUnmount(() => {
  try { ws && ws.close() } catch { /* ignore */ }
  try { crackWs && crackWs.close() } catch { /* ignore */ }
  if (roguePoll) { clearInterval(roguePoll); roguePoll = null }
})
</script>

<style scoped>
.wifi-page {
  min-height: 100%;
  padding: 2.5rem 1.25rem 5rem;
  color: #e2e8f0;
}
.wifi-container { max-width: 1080px; margin: 0 auto; }

/* hero */
.wifi-hero { text-align: center; margin-bottom: 2rem; }
.hero-badge { font-size: 2.6rem; filter: drop-shadow(0 0 18px rgba(34,211,238,.5)); }
.hero-title {
  margin: .4rem 0 .3rem; font-size: 2rem; font-weight: 800;
  background: linear-gradient(90deg,#67e8f9,#c4b5fd,#6ee7b7);
  -webkit-background-clip: text; background-clip: text; -webkit-text-fill-color: transparent;
}
.hero-sub { color: #94a3b8; font-size: .95rem; }
.hero-meta { display: flex; flex-wrap: wrap; gap: .5rem; justify-content: center; margin: 1rem 0 .6rem; }
.meta-chip {
  font-size: .78rem; padding: .3rem .7rem; border-radius: 999px;
  border: 1px solid rgba(148,163,184,.25); background: rgba(15,23,42,.5); color: #cbd5e1;
}
.meta-chip.ok { color: #6ee7b7; border-color: rgba(110,231,183,.4); }
.meta-chip.warn { color: #fca5a5; border-color: rgba(252,165,165,.4); }
.hero-disclaimer { font-size: .74rem; color: #fbbf24; opacity: .85; }
.load-banner {
  margin: 1rem auto 0; max-width: 560px; display: flex; align-items: center;
  justify-content: space-between; gap: 1rem; padding: .7rem 1rem;
  border-radius: 12px; background: rgba(127,29,29,.25);
  border: 1px solid rgba(252,165,165,.4); font-size: .82rem; color: #fca5a5;
}

/* tabs */
.wifi-tabs { display: flex; gap: .4rem; justify-content: center; flex-wrap: wrap; margin-bottom: 1.4rem; }
.tab-btn {
  display: inline-flex; align-items: center; gap: .35rem;
  padding: .55rem 1.1rem; border-radius: 12px; cursor: pointer;
  font-size: .88rem; color: #94a3b8;
  background: rgba(15,23,42,.5); border: 1px solid rgba(148,163,184,.18);
  transition: all .2s;
}
.tab-btn:hover { color: #e2e8f0; border-color: rgba(103,232,249,.5); }
.tab-btn.active {
  color: #fff; background: linear-gradient(135deg, rgba(34,211,238,.25), rgba(168,85,247,.25));
  border-color: rgba(103,232,249,.6);
  box-shadow: 0 0 18px rgba(34,211,238,.25);
}

/* panels */
.panel { animation: fade .25s ease; }
@keyframes fade { from { opacity: 0; transform: translateY(6px); } to { opacity: 1; transform: none; } }
.panel-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 1rem; }
.panel-head h2 { font-size: 1.1rem; color: #e2e8f0; }
.empty {
  padding: 2rem; text-align: center; color: #94a3b8; font-size: .9rem;
  border: 1px dashed rgba(148,163,184,.25); border-radius: 14px;
}
.hint { color: #94a3b8; font-size: .8rem; margin-bottom: .9rem; }

.btn {
  padding: .5rem 1rem; border-radius: 10px; cursor: pointer; font-size: .85rem;
  color: #cbd5e1; background: rgba(30,41,59,.8); border: 1px solid rgba(148,163,184,.25);
  transition: all .2s;
}
.btn:hover:not(:disabled) { color: #fff; border-color: rgba(103,232,249,.5); }
.btn:disabled { opacity: .5; cursor: not-allowed; }
.btn.primary { background: linear-gradient(135deg,#0891b2,#7c3aed); border: none; color: #fff; }
.btn.small { padding: .3rem .7rem; font-size: .78rem; }
.btn.mini { padding: .25rem .6rem; font-size: .74rem; }
.btn.danger { color: #fca5a5; border-color: rgba(252,165,165,.4); }

/* networks */
.net-list { display: flex; flex-direction: column; gap: .6rem; }
.net-card {
  border: 1px solid rgba(148,163,184,.16); border-radius: 14px;
  background: linear-gradient(150deg, rgba(15,23,42,.75), rgba(10,15,30,.7));
  padding: .9rem 1.1rem; cursor: pointer; transition: all .2s;
}
.net-card:hover { border-color: rgba(103,232,249,.4); }
.net-card.open { border-color: rgba(103,232,249,.55); box-shadow: 0 0 20px rgba(34,211,238,.12); }
.net-main { display: flex; align-items: center; justify-content: space-between; gap: 1rem; flex-wrap: wrap; }
.net-id { display: flex; align-items: center; gap: .6rem; flex-wrap: wrap; }
.net-ssid { font-weight: 700; color: #f1f5f9; }
.net-auth { font-size: .72rem; color: #67e8f9; background: rgba(34,211,238,.1); padding: .15rem .55rem; border-radius: 6px; }
.net-enc { font-size: .72rem; color: #c4b5fd; }
.net-side { display: flex; align-items: center; gap: .8rem; }
.signal { display: flex; align-items: center; gap: .4rem; }
.signal-bar { width: 90px; height: 7px; border-radius: 999px; background: rgba(148,163,184,.18); overflow: hidden; }
.signal-fill { height: 100%; border-radius: 999px; transition: width .3s; }
.signal-num { font-size: .78rem; color: #cbd5e1; min-width: 42px; }
.net-chan, .net-aps { font-size: .75rem; color: #94a3b8; }
.net-caret { color: #64748b; }

.bssid-list { margin-top: .8rem; padding-top: .8rem; border-top: 1px dashed rgba(148,163,184,.2); display: flex; flex-direction: column; gap: .35rem; }
.bssid-row { display: flex; gap: 1.2rem; font-size: .78rem; color: #94a3b8; }
.bssid-row code { color: #67e8f9; }

/* interfaces */
.iface-grid { display: grid; grid-template-columns: repeat(auto-fill,minmax(300px,1fr)); gap: 1rem; }
.iface-card {
  border: 1px solid rgba(148,163,184,.18); border-radius: 14px; padding: 1.1rem 1.2rem;
  background: rgba(15,23,42,.6);
}
.iface-card h3 { margin: 0 0 .8rem; color: #67e8f9; font-size: 1rem; }
.iface-card dl { display: flex; flex-direction: column; gap: .45rem; margin: 0; }
.iface-card dl > div { display: flex; justify-content: space-between; gap: 1rem; font-size: .82rem; }
.iface-card dt { color: #94a3b8; }
.iface-card dd { margin: 0; color: #e2e8f0; text-align: right; word-break: break-all; }

/* profiles */
.prof-chips { display: flex; flex-wrap: wrap; gap: .5rem; }
.prof-chip {
  padding: .45rem .9rem; border-radius: 10px; cursor: pointer; font-size: .85rem;
  color: #cbd5e1; background: rgba(30,41,59,.8); border: 1px solid rgba(148,163,184,.25);
}
.prof-chip:hover { border-color: rgba(103,232,249,.5); color: #fff; }
.prof-chip.active { background: rgba(34,211,238,.18); border-color: rgba(103,232,249,.6); color: #fff; }
.prof-detail { margin-top: 1.2rem; padding: 1rem 1.2rem; border-radius: 12px; border: 1px solid rgba(148,163,184,.2); background: rgba(15,23,42,.6); }
.prof-detail h3 { margin: 0 0 .6rem; color: #e2e8f0; }
.pwd-row { display: flex; gap: .6rem; align-items: center; font-size: .85rem; color: #94a3b8; flex-wrap: wrap; }
.pwd-row code { color: #fcd34d; }

/* Windows engines */
.win-engines { display: flex; flex-direction: column; gap: 1.4rem; }
.engine-block {
  border: 1px solid rgba(148,163,184,.2); border-radius: 16px; padding: 1.3rem 1.4rem;
  background: linear-gradient(160deg, rgba(15,23,42,.65), rgba(8,12,24,.6));
}
.block-title { display: flex; align-items: center; justify-content: space-between; margin-bottom: 1rem; }
.block-title h3 { font-size: 1.02rem; color: #e2e8f0; }
.mode-tag { font-size: .74rem; padding: .2rem .6rem; border-radius: 8px; background: rgba(148,163,184,.12); }
.mode-tag.ok { color: #6ee7b7 !important; }
.mode-tag.warn { color: #fbbf24 !important; }

.form-row { display: flex; flex-direction: column; gap: .35rem; margin-bottom: .9rem; }
.form-row label { font-size: .76rem; color: #94a3b8; }
.form-row input, .form-row select, textarea {
  padding: .6rem .8rem; border-radius: 10px; font-size: .86rem;
  background: rgba(2,6,23,.7); border: 1px solid rgba(148,163,184,.25); color: #e2e8f0; outline: none;
}
.form-row input:focus, .form-row select:focus { border-color: rgba(103,232,249,.6); }
textarea { width: 100%; resize: vertical; margin-bottom: .9rem; font-family: inherit; }
.need-warn { color: #fca5a5; font-size: .8rem; margin-bottom: .8rem; }

.mon-grid { display: grid; grid-template-columns: repeat(auto-fill,minmax(150px,1fr)); gap: .7rem; margin-bottom: 1rem; }
.mon-grid > div {
  display: flex; flex-direction: column; gap: .2rem; padding: .7rem .8rem;
  border-radius: 10px; background: rgba(2,6,23,.5); border: 1px solid rgba(148,163,184,.14);
}
.mon-grid span { font-size: .7rem; color: #7a869e; }
.mon-grid strong { font-size: .9rem; color: #e2e8f0; word-break: break-all; }
.mon-grid strong.got { color: #6ee7b7; }

.sub-box { margin: 1rem 0; }
.sub-box h4 { font-size: .84rem; color: #cbd5e1; margin-bottom: .6rem; }
.lease-row { display: flex; gap: 1rem; font-size: .78rem; color: #94a3b8; padding: .35rem .2rem; border-bottom: 1px dashed rgba(148,163,184,.12); }
.lease-row code { color: #67e8f9; }
.captures-box { }
.cap-card {
  border: 1px solid rgba(110,231,183,.4); border-radius: 12px; padding: .8rem 1rem; margin-bottom: .6rem;
  background: rgba(6,30,22,.4);
}
.cap-time { font-size: .72rem; color: #6ee7b7; margin-bottom: .4rem; }
.cap-field { display: flex; gap: .8rem; font-size: .82rem; }
.cap-field span { color: #7a869e; min-width: 70px; }
.cap-field code { color: #fcd34d; word-break: break-all; }

.crack-tabs { display: flex; gap: .4rem; margin-bottom: 1rem; }
.crack-tabs button {
  padding: .4rem .9rem; border-radius: 9px; font-size: .8rem; cursor: pointer;
  background: rgba(30,41,59,.8); border: 1px solid rgba(148,163,184,.25); color: #94a3b8;
}
.crack-tabs button.active { color: #fff; border-color: rgba(103,232,249,.6); background: rgba(34,211,238,.15); }
.attack-mode-row { display: flex; gap: .5rem; flex-wrap: wrap; }
.radio-chip { display: inline-flex; align-items: center; gap: .3rem; padding: .25rem .7rem; font-size: .8rem; cursor: pointer; border-radius: 8px; border: 1px solid rgba(148,163,184,.2); background: rgba(30,41,59,.5); color: #94a3b8; transition: all .15s; }
.radio-chip input { display: none; }
.radio-chip.active { color: #fff; border-color: rgba(103,232,249,.6); background: rgba(34,211,238,.15); }
.hint { display: block; margin-top: .2rem; font-size: .7rem; color: #64748b; }
.up-row { display: flex; gap: 1rem; flex-wrap: wrap; margin-bottom: 1rem; }
.up-row > div { flex: 1; min-width: 200px; }
.up-row label { font-size: .76rem; color: #94a3b8; display: block; margin-bottom: .4rem; }
.up-row input[type=file] { font-size: .8rem; color: #cbd5e1; }

.job-row { display: flex; align-items: center; justify-content: space-between; gap: .7rem; padding: .45rem .6rem; border: 1px solid rgba(148,163,184,.14); border-radius: 10px; margin-bottom: .45rem; }
.job-main { display: flex; align-items: center; gap: .7rem; background: none; border: none; cursor: pointer; color: #e2e8f0; font-size: .84rem; }
.job-row.active { border-color: rgba(103,232,249,.5); }
.jstatus { font-size: .68rem; padding: .1rem .45rem; border-radius: 6px; background: rgba(148,163,184,.14); }
.jstatus.running { color: #6ee7b7; }
.jstatus.done { color: #67e8f9; }
.jstatus.exhausted { color: #fbbf24; }
.jprog { font-size: .72rem; color: #94a3b8; }
.jfound { font-size: .75rem; color: #6ee7b7; font-weight: 700; }

/* Linux engines */
.engine-grid { display: grid; grid-template-columns: repeat(auto-fill,minmax(340px,1fr)); gap: 1rem; }
.engine-card { border: 1px solid rgba(148,163,184,.2); border-radius: 14px; padding: 1.1rem 1.2rem; background: rgba(15,23,42,.6); }
.engine-card.ready { border-color: rgba(110,231,183,.5); }
.engine-head { display: flex; align-items: center; justify-content: space-between; }
.engine-head h3 { margin: 0; color: #e2e8f0; }
.engine-mode { font-size: .72rem; padding: .15rem .55rem; border-radius: 6px; background: rgba(148,163,184,.15); color: #cbd5e1; }
.engine-reason { font-size: .8rem; color: #94a3b8; margin: .6rem 0; }
.arg-box { display: flex; flex-direction: column; gap: .5rem; }
.arg-box label { font-size: .76rem; color: #94a3b8; }
.arg-input {
  padding: .5rem .7rem; border-radius: 9px; font-size: .85rem;
  background: rgba(2,6,23,.7); border: 1px solid rgba(148,163,184,.25); color: #e2e8f0;
}
.arg-input:focus { outline: none; border-color: rgba(103,232,249,.6); }
.presets { display: flex; flex-wrap: wrap; gap: .35rem; }
.preset { font-size: .72rem; padding: .25rem .55rem; border-radius: 7px; cursor: pointer; color: #94a3b8; background: transparent; border: 1px solid rgba(148,163,184,.25); }
.preset:hover { color: #67e8f9; border-color: rgba(103,232,249,.5); }

.sessions { margin-top: 1.6rem; }
.sessions h3 { font-size: 1rem; }
.session-row { display: flex; align-items: center; justify-content: space-between; gap: .8rem; padding: .5rem .7rem; border: 1px solid rgba(148,163,184,.16); border-radius: 10px; margin-bottom: .5rem; }
.sess-main { display: flex; align-items: center; gap: .8rem; background: none; border: none; cursor: pointer; color: #e2e8f0; font-size: .88rem; }
.session-row.active { border-color: rgba(103,232,249,.5); }
.sess-status { font-size: .7rem; padding: .12rem .5rem; border-radius: 6px; background: rgba(148,163,184,.15); }
.sess-status.running { color: #6ee7b7; }
.sess-status.done { color: #67e8f9; }
.sess-status.failed { color: #fca5a5; }
.sess-id { color: #64748b; font-size: .72rem; }

.console { margin-top: 1.2rem; border: 1px solid rgba(148,163,184,.22); border-radius: 12px; overflow: hidden; }
.console-head { display: flex; justify-content: space-between; align-items: center; padding: .6rem .9rem; background: rgba(2,6,23,.8); font-size: .8rem; color: #cbd5e1; }
.console-body {
  margin: 0; padding: .9rem 1rem; max-height: 360px; overflow: auto;
  font-family: 'Cascadia Code', Consolas, monospace; font-size: .76rem; line-height: 1.55;
  background: #05080f; color: #a7f3d0; white-space: pre-wrap; word-break: break-all;
}

@media (max-width: 640px) {
  .wifi-page { padding: 1.5rem .8rem 3rem; }
  .net-main { gap: .6rem; }
  .signal-bar { width: 60px; }
}
</style>
