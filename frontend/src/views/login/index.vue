<template>
  <div class="relative min-h-screen overflow-hidden bg-[#050014]">
    <!-- ═══════ 矩阵雨 canvas ═══════ -->
    <canvas ref="matrixCanvas" class="absolute inset-0 h-full w-full opacity-15"></canvas>

    <!-- ═══════ 扫描线叠加 ═══════ -->
    <div class="cyber-scanline absolute inset-0 bg-gradient-to-b from-transparent via-cyan-400/5 to-transparent pointer-events-none"></div>

    <!-- ═══════ 主容器 ═══════ -->
    <div ref="pageWrap" class="relative z-10 mx-auto flex min-h-screen items-center justify-center px-6 py-12">
      <div ref="card" class="w-full max-w-[420px]">

        <!-- ─── 顶部标题 ─── -->
        <div ref="header" class="mb-8 text-center opacity-0">
          <div class="mb-3 inline-flex h-12 w-12 items-center justify-center rounded-xl bg-gradient-to-br from-cyan-400/20 to-purple-500/20 ring-1 ring-cyan-400/30">
            <svg class="h-6 w-6 text-cyan-300" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 5.25a3 3 0 013 3m3 0a6 6 0 01-7.029 5.912c-.563-.097-1.159.026-1.563.43L10.5 17.25H8.25v2.25H6v2.25H2.25v-2.818c0-.597.237-1.17.659-1.591l6.499-6.499c.404-.404.527-1 .43-1.563A6 6 0 1121.75 8.25z" />
            </svg>
          </div>
          <h1 class="text-2xl font-bold tracking-tight text-white">Aurora-朱</h1>
          <p class="mt-1.5 text-sm text-slate-400">极光之境 &middot; 数字星球</p>
          <div class="mx-auto mt-4 h-px w-24 bg-gradient-to-r from-transparent via-cyan-400/60 to-transparent"></div>
        </div>

        <!-- ─── 主面板 ─── -->
        <div ref="panel" class="rounded-2xl border border-white/[0.08] bg-[#141432] p-7 shadow-2xl shadow-black/40 backdrop-blur-xl opacity-0">

          <!-- 主 Tab -->
          <div ref="tabs" class="mb-6 flex gap-1 rounded-xl bg-black/30 p-1 opacity-0">
            <button v-for="t in mainTabs" :key="t.key"
              :class="[
                'relative flex-1 rounded-lg py-2 text-[13px] font-medium transition-all duration-200',
                mainTab === t.key ? 'text-white' : 'text-slate-500 hover:text-slate-300'
              ]" @click="switchTab(t.key)">
              <span v-if="mainTab === t.key" ref="tabIndicator" class="absolute inset-0 rounded-lg bg-[#18183c] ring-1 ring-white/[0.06]"></span>
              <span class="relative z-10">{{ t.label }}</span>
            </button>
          </div>

          <!-- ═══════ 表单 ═══════ -->
          <form class="space-y-4" @submit.prevent="onSubmit">

            <!-- ── 密码登录 ── -->
            <template v-if="mainTab === 'password'">
              <div ref="fieldUser" class="space-y-1.5 opacity-0">
                <label class="text-[11px] font-medium uppercase tracking-wider text-slate-400">账号</label>
                <div class="relative">
                  <input v-model="form.username" class="web3-input-field" placeholder="用户名 / 邮箱 / 手机号" autocomplete="username" />
                </div>
              </div>
              <div ref="fieldPass" class="space-y-1.5 opacity-0">
                <label class="text-[11px] font-medium uppercase tracking-wider text-slate-400">密码</label>
                <input v-model="form.password" type="password" class="web3-input-field" placeholder="输入密码" autocomplete="current-password" />
              </div>
              <div ref="fieldCaptcha" class="space-y-1.5 opacity-0">
                <label class="text-[11px] font-medium uppercase tracking-wider text-slate-400">验证码</label>
                <div class="flex gap-3">
                  <input v-model="form.captchaCode" class="web3-input-field flex-1" placeholder="输入验证码" maxlength="4" autocomplete="off" />
                  <div class="relative flex-shrink-0 w-[120px] h-[42px] rounded-lg overflow-hidden cursor-pointer border border-white/[0.08] bg-black/30" @click="refreshCaptcha" title="点击刷新">
                    <img v-if="captchaImg" :src="captchaImg" class="h-full w-full object-cover" alt="captcha" />
                    <template v-else>
                      <div v-if="captchaError" class="flex h-full flex-col items-center justify-center gap-0.5">
                        <svg class="h-4 w-4 text-red-400/80" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" /></svg>
                        <span class="text-[10px] text-red-400/80">加载失败</span>
                      </div>
                      <div v-else class="flex h-full items-center justify-center">
                        <span class="h-4 w-4 rounded-full border-2 border-cyan-400/30 border-t-cyan-400 animate-spin"></span>
                      </div>
                    </template>
                  </div>
                </div>
              </div>
            </template>

            <!-- ── 免密登录（Supabase Magic Link）── -->
            <template v-if="mainTab === 'magic'">
              <div ref="fieldMail" class="space-y-1.5">
                <label class="text-[11px] font-medium uppercase tracking-wider text-slate-400">邮箱</label>
                <input v-model="magicEmail" type="email" class="web3-input-field" placeholder="your@email.com" autocomplete="email" @keyup.enter="handleMagicLogin" />
              </div>
              <p class="text-[11px] leading-relaxed text-slate-500">
                无需密码：我们向你的邮箱发送一封<b class="text-cyan-400/70">魔法链接</b>，点开即可登录。
                <span v-if="magicSent" class="block mt-2 text-emerald-400/80">✓ 已发送，请查收邮箱并点击链接</span>
              </p>
            </template>

            <!-- ── 验证码登录 ── -->
            <template v-if="mainTab === 'otp'">
              <!-- 邮箱/手机子 Tab -->
              <div class="flex gap-4 text-xs font-medium">
                <button type="button" :class="['transition', otpType === 'email' ? 'text-cyan-400' : 'text-slate-500 hover:text-slate-300']" @click="otpType = 'email'">邮箱</button>
                <button type="button" :class="['transition', otpType === 'phone' ? 'text-cyan-400' : 'text-slate-500 hover:text-slate-300']" @click="otpType = 'phone'">手机</button>
              </div>

              <div class="space-y-1.5">
                <label class="text-[11px] font-medium uppercase tracking-wider text-slate-400">{{ otpType === 'email' ? '邮箱' : '手机号' }}</label>
                <input v-model="otpTarget" :type="otpType === 'email' ? 'email' : 'tel'" class="web3-input-field"
                  :placeholder="otpType === 'email' ? 'your@email.com' : '输入手机号'" autocomplete="off" />
              </div>

              <div class="space-y-1.5">
                <label class="text-[11px] font-medium uppercase tracking-wider text-slate-400">图形验证码</label>
                <div class="flex gap-3">
                  <input v-model="otpCaptchaCode" class="web3-input-field flex-1" placeholder="输入验证码" maxlength="4" autocomplete="off" />
                  <div class="relative flex-shrink-0 w-[120px] h-[42px] rounded-lg overflow-hidden cursor-pointer border border-white/[0.08] bg-black/30" @click="refreshCaptcha" title="点击刷新">
                    <img v-if="captchaImg" :src="captchaImg" class="h-full w-full object-cover" alt="captcha" />
                    <template v-else>
                      <div v-if="captchaError" class="flex h-full flex-col items-center justify-center gap-0.5">
                        <svg class="h-4 w-4 text-red-400/80" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" /></svg>
                        <span class="text-[10px] text-red-400/80">加载失败</span>
                      </div>
                      <div v-else class="flex h-full items-center justify-center">
                        <span class="h-4 w-4 rounded-full border-2 border-cyan-400/30 border-t-cyan-400 animate-spin"></span>
                      </div>
                    </template>
                  </div>
                </div>
              </div>

              <div class="space-y-1.5">
                <label class="text-[11px] font-medium uppercase tracking-wider text-slate-400">验证码</label>
                <div class="flex gap-3">
                  <input v-model="otpCode" class="web3-input-field flex-1" placeholder="输入验证码" maxlength="6" autocomplete="off" />
                  <button type="button" ref="sendBtn" class="web3-send-btn flex-shrink-0 px-4 text-xs font-medium whitespace-nowrap"
                    :disabled="otpCooldown > 0 || sendingCode" @click="sendOtpCode">
                    {{ sendingCode ? '发送中...' : otpCooldown > 0 ? `${otpCooldown}s` : '获取验证码' }}
                  </button>
                </div>
              </div>
            </template>

            <!-- ── 注册 ── -->
            <template v-if="mainTab === 'register'">
              <div class="space-y-1.5">
                <label class="text-[11px] font-medium uppercase tracking-wider text-slate-400">用户名</label>
                <input v-model="form.username" class="web3-input-field" placeholder="输入用户名" autocomplete="username" />
              </div>
              <div class="space-y-1.5">
                <label class="text-[11px] font-medium uppercase tracking-wider text-slate-400">邮箱</label>
                <input v-model="form.email" class="web3-input-field" placeholder="your@email.com" autocomplete="email" />
              </div>
              <div class="space-y-1.5">
                <label class="text-[11px] font-medium uppercase tracking-wider text-slate-400">昵称</label>
                <input v-model="form.nickname" class="web3-input-field" placeholder="你的昵称" />
              </div>
              <div class="space-y-1.5">
                <label class="text-[11px] font-medium uppercase tracking-wider text-slate-400">密码</label>
                <input v-model="form.password" type="password" class="web3-input-field" placeholder="至少6位" autocomplete="new-password" />
              </div>
            </template>

            <!-- 提交按钮 -->
            <button type="submit" ref="submitBtn" class="web3-submit-btn group relative w-full overflow-hidden rounded-xl py-3 text-sm font-semibold tracking-wide text-white transition-all duration-200"
              :disabled="loading">
              <span class="relative z-10 flex items-center justify-center gap-2">
                <span v-if="loading" class="inline-block h-4 w-4 rounded-full border-2 border-white/30 border-t-white animate-spin"></span>
                {{ loadingText }}
              </span>
            </button>

            <p v-if="mainTab === 'password'" class="text-center text-xs text-slate-500">没有账号？切换「注册」自动创建</p>
          </form>

          <!-- ─── 分割线 + 社交登录 ─── -->
          <div class="mt-6 flex items-center gap-3">
            <div class="flex-1 h-px bg-[#141432]"></div>
            <span class="text-[11px] font-medium uppercase tracking-wider text-slate-500">或者</span>
            <div class="flex-1 h-px bg-[#141432]"></div>
          </div>

          <div class="mt-4">
            <button type="button" ref="googleBtn" class="google-btn group flex w-full items-center justify-center gap-3 rounded-xl border border-white/[0.08] bg-[#10102a] py-3 text-sm font-medium text-slate-300 transition-all duration-200 hover:border-white/[0.14] hover:bg-[#1a1a44] hover:text-white"
              @click="handleGoogleLogin">
              <svg class="h-5 w-5 transition-transform group-hover:scale-110" viewBox="0 0 24 24"><path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 01-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z"/><path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/><path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/><path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/></svg>
              <span v-if="googleLoading" class="h-4 w-4 rounded-full border-2 border-white/30 border-t-white animate-spin"></span>
              {{ googleLoading ? '跳转中...' : '使用 Google 账号登录' }}
            </button>
          </div>
        </div>

        <!-- 底部装饰文字 -->
        <p ref="footer" class="mt-6 text-center text-[11px] text-slate-600 opacity-0">
          Secured by Web3 Authentication
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/modules/auth'
import { useToastStore } from '@/stores/modules/toast'
import { getCaptcha, sendEmailCode, sendPhoneCode, getGoogleUrl, googleDevLogin } from '@/api/user'
import { animate, stagger } from 'animejs'
import { useSupabase } from '@/composables/useSupabase'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const toast = useToastStore()
const { signInWithMagicLink } = useSupabase()

// ─── Refs ───
const matrixCanvas = ref(null)
const pageWrap = ref(null)
const card = ref(null)
const header = ref(null)
const panel = ref(null)
const tabs = ref(null)
const fieldUser = ref(null)
const fieldPass = ref(null)
const fieldCaptcha = ref(null)
const fieldMail = ref(null)
const submitBtn = ref(null)
const googleBtn = ref(null)
const footer = ref(null)

// ─── 主 Tab ───
const mainTabs = [
  { key: 'password', label: '密码登录' },
  { key: 'magic', label: '免密登录' },
  { key: 'otp', label: '验证码登录' },
  { key: 'register', label: '注册' }
]
const mainTab = ref('password')

// ─── 密码登录 ───
const form = reactive({ username: '', password: '', nickname: '', email: '', captchaId: '', captchaCode: '' })

// ─── 免密登录（Supabase Magic Link）───
const magicEmail = ref('')
const magicSent = ref(false)

// ─── 验证码登录 ───
const otpType = ref('email')
const otpTarget = ref('')
const otpCaptchaCode = ref('')
const otpCaptchaId = ref('')
const otpCode = ref('')
const otpCooldown = ref(0)
const sendingCode = ref(false)
let cooldownTimer = null

// ─── 验证码图片 ───
const captchaImg = ref('')
const captchaId = ref('')
const captchaError = ref(false)
const loading = ref(false)
const googleLoading = ref(false)

const loadingText = computed(() => {
  if (loading.value) return '处理中...'
  if (mainTab.value === 'password') return '登 录'
  if (mainTab.value === 'magic') return '发送魔法链接'
  if (mainTab.value === 'otp') return '验证码登录'
  return '注 册'
})

// ─── 图形验证码 ───
async function refreshCaptcha() {
  captchaError.value = false
  captchaImg.value = ''
  try {
    const res = await getCaptcha()
    const data = res.data || res
    captchaImg.value = data.image
    captchaId.value = data.captchaId
    form.captchaId = data.captchaId
    otpCaptchaId.value = data.captchaId
  } catch (e) {
    captchaError.value = true
    captchaImg.value = ''
  }
}

// ─── 切换 Tab（含 anime.js 动画） ───
function switchTab(key) {
  if (mainTab.value === key) return
  mainTab.value = key
  nextTick(() => animateFields())
}

function animateFields() {
  nextTick(() => {
    const fields = [fieldUser.value, fieldPass.value, fieldCaptcha.value].filter(Boolean)
    if (!fields.length) return
    animate(fields, {
      opacity: [0, 1],
      translateY: [8, 0],
      duration: 300,
      ease: 'outQuad',
      delay: stagger(60)
    })
  })
}

// ─── 发送 OTP 验证码 ───
async function sendOtpCode() {
  if (!otpTarget.value) {
    toast.warning(otpType.value === 'email' ? '请输入邮箱' : '请输入手机号')
    return
  }
  if (!otpCaptchaCode.value) {
    toast.warning('请输入图形验证码')
    return
  }
  sendingCode.value = true
  try {
    const data = { captchaId: otpCaptchaId.value, captchaCode: otpCaptchaCode.value }
    let res
    if (otpType.value === 'email') {
      data.email = otpTarget.value
      res = await sendEmailCode(data)
    } else {
      data.phone = otpTarget.value
      res = await sendPhoneCode(data)
    }
    const body = res.data || res
    if (body.devCode) {
      toast.success(`验证码：${body.devCode}（开发模式）`)
    } else {
      toast.success('验证码已发送')
    }
    refreshCaptcha()
    otpCaptchaCode.value = ''
    startCooldown()
  } catch (e) {
    toast.error(e?.response?.data?.message || e?.message || '发送验证码失败')
  } finally {
    sendingCode.value = false
  }
}

function startCooldown() {
  otpCooldown.value = 60
  cooldownTimer = setInterval(() => {
    otpCooldown.value--
    if (otpCooldown.value <= 0) clearInterval(cooldownTimer)
  }, 1000)
}

// ─── 提交 ───
async function onSubmit() {
  if (mainTab.value === 'password') {
    if (!form.username) { toast.warning('请输入账号'); return }
    if (!form.password) { toast.warning('请输入密码'); return }
    if (!form.captchaCode) { toast.warning('请输入图形验证码'); return }
  } else if (mainTab.value === 'magic') {
    return handleMagicLogin()
  } else if (mainTab.value === 'otp') {
    if (!otpTarget.value) { toast.warning(otpType.value === 'email' ? '请输入邮箱' : '请输入手机号'); return }
    if (!otpCode.value) { toast.warning('请输入验证码'); return }
  } else {
    if (!form.username) { toast.warning('请输入用户名'); return }
    if (!form.password || form.password.length < 6) { toast.warning('密码至少6位'); return }
  }

  loading.value = true
  try {
    if (mainTab.value === 'password') {
      await auth.login(form.username, form.password, form.captchaId, form.captchaCode)
      toast.success('登录成功')
    } else if (mainTab.value === 'otp') {
      if (otpType.value === 'email') {
        await auth.emailLogin(otpTarget.value, otpCode.value)
      } else {
        await auth.phoneLogin(otpTarget.value, otpCode.value)
      }
      toast.success('登录成功')
    } else {
      await auth.register(form.username, form.password, form.nickname, form.email)
      toast.success('注册成功')
    }
    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } catch (e) {
    const detail = e?.response?.data?.message || e?.message?.replace(/^(登录失败|注册失败)/, '') || ''
    toast.error(detail || (mainTab.value === 'register' ? '注册失败' : '登录失败'))
    if (mainTab.value === 'password') refreshCaptcha()
  } finally {
    loading.value = false
  }
}

// ─── 免密登录（Supabase Magic Link）───
async function handleMagicLogin() {
  const email = magicEmail.value.trim()
  if (!email) { toast.warning('请输入邮箱'); return }
  loading.value = true
  magicSent.value = false
  try {
    const { error } = await signInWithMagicLink(email, {
      emailRedirectTo: window.location.origin + '/login?magic=return',
    })
    if (error) {
      toast.error(error.message || '发送失败')
    } else {
      magicSent.value = true
      toast.success('魔法链接已发送，请查收邮箱')
    }
  } catch {
    toast.error('发送失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// ─── Google ───
async function handleGoogleLogin() {
  googleLoading.value = true
  try {
    const res = await getGoogleUrl()
    const data = res.data || res
    if (data.url) {
      // 生产已配置真实 Google OAuth → 跳转授权页
      window.location.href = data.url
      return
    }
    // 未配置真实 OAuth → 走 dev mock：弹输入框让用户输入邮箱，后端自动注册/登录
    // 生产部署设置 GOOGLE_OAUTH_ENABLED=true 后，后端 /user/oauth/google/dev-login 会返回 403 拒绝
    const email = window.prompt('Google 登录未配置真实 client_id，当前为 dev mock 模式。\n请输入任意邮箱（将以此邮箱自动注册/登录）：', '')
    if (!email || !email.includes('@')) {
      toast.warning('请输入合法邮箱')
      return
    }
    const devRes = await googleDevLogin({ email })
    const payload = devRes.data?.code === 200 ? (devRes.data?.data || devRes.data) : devRes.data
    if (!payload?.token) throw new Error('未获取到令牌')
    auth.setSession(payload)
    toast.success('Google dev 登录成功')
    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } catch (e) {
    toast.error(e?.response?.data?.message || e?.message || '获取 Google 授权失败，请确认网关与用户服务已启动')
  } finally {
    googleLoading.value = false
  }
}

// ─── OAuth 回调 ───
function handleOAuthCallback() {
  const accessToken = route.query.access_token
  const authError = route.query.auth_error
  if (authError) {
    toast.error('Google 登录失败：' + decodeURIComponent(authError))
    router.replace({ path: '/login' })
    return
  }
  if (accessToken) {
    auth.completeWithToken(accessToken)
    toast.success('Google 登录成功')
    router.push(route.query.redirect || '/')
  }
}

// ─── Supabase Magic Link 回跳：把 Supabase 会话交给后端换取应用 JWT ───
import { supabase as sbClient } from '@/lib/supabase/client'
import { supabaseLogin } from '@/api/user'
async function handleMagicReturn() {
  const magicReturn = route.query.magic !== undefined
  if (!magicReturn || !sbClient) return
  try {
    const { data: { session } } = await sbClient.auth.getSession()
    const accessToken = session?.access_token
    if (!accessToken) return
    // 后端用 Supabase JWT(经 JWKS 验签) 识别邮箱 → 发本应用 JWT（自动注册/登录）
    const res = await supabaseLogin({ accessToken })
    const payload = res.data?.code === 200 ? (res.data?.data || res.data) : res.data
    if (!payload?.token) throw new Error('未获取到令牌')
    auth.setSession(payload)
    toast.success('免密登录成功')
    const redirect = route.query.redirect || '/'
    router.replace(redirect)
  } catch (e) {
    toast.error('免密登录失败：' + (e?.message || ''))
    router.replace({ path: '/login', query: { magic_error: '1' } })
  }
}

// ─── 矩阵雨（anime.js 增强：渐显入场） ───
let raf = 0
onMounted(() => {
  handleOAuthCallback()
  handleMagicReturn()
  refreshCaptcha()
  initMatrixRain()
  entranceAnimation()
})

function initMatrixRain() {
  const canvas = matrixCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  canvas.width = innerWidth; canvas.height = innerHeight
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%^&*~'
  const fontSize = 14
  const columns = Math.floor(canvas.width / fontSize)
  const drops = Array.from({ length: columns }, () => Math.random() * -100)

  function draw() {
    ctx.fillStyle = 'rgba(5,0,20,0.18)'
    ctx.fillRect(0, 0, canvas.width, canvas.height)
    ctx.font = fontSize + 'px monospace'
    for (let i = 0; i < drops.length; i++) {
      const char = chars[Math.floor(Math.random() * chars.length)]
      // 偶尔高亮（青色→白色渐变）
      if (Math.random() > 0.96) {
        ctx.fillStyle = '#ffffff'
        ctx.shadowColor = '#22d3ee'
        ctx.shadowBlur = 8
      } else {
        ctx.fillStyle = `rgba(34,211,238,${0.3 + Math.random() * 0.3})`
        ctx.shadowBlur = 0
      }
      ctx.fillText(char, i * fontSize, drops[i])
      ctx.shadowBlur = 0
      drops[i] += fontSize
      if (drops[i] > canvas.height && Math.random() > 0.975) drops[i] = 0
    }
    raf = requestAnimationFrame(draw)
  }
  draw()
}

function entranceAnimation() {
  nextTick(() => {
    // 标题入场
    animate(header.value, {
      opacity: [0, 1], translateY: [-20, 0],
      duration: 600, ease: 'outCubic'
    })
    // 面板入场（延迟）
    animate(panel.value, {
      opacity: [0, 1], translateY: [30, 0], scale: [0.97, 1],
      duration: 700, ease: 'outCubic', delay: 150
    })
    // Tab 栏入场
    animate(tabs.value, {
      opacity: [0, 1], duration: 400, delay: 350
    })
    // 表单字段依次入场
    setTimeout(() => animateFields(), 450)
    // 底部文字
    animate(footer.value, {
      opacity: [0, 1], duration: 500, delay: 700
    })
  })
}

onBeforeUnmount(() => {
  cancelAnimationFrame(raf)
  if (cooldownTimer) clearInterval(cooldownTimer)
})
</script>

<style scoped>
.web3-input-field {
  @apply w-full rounded-lg border border-white/[0.08] bg-[#10102a] px-3.5 py-2.5
         text-[13px] text-white placeholder-slate-500 outline-none
         transition-all duration-200
         focus:border-cyan-400/40 focus:bg-[#16163a] focus:ring-1 focus:ring-cyan-400/20;
}

.web3-send-btn {
  @apply h-[42px] rounded-lg border border-cyan-400/20 bg-cyan-400/10 px-4 text-xs font-medium text-cyan-300
         transition-all duration-200
         hover:border-cyan-400/40 hover:bg-cyan-400/20 hover:text-cyan-200
         disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:border-cyan-400/20 disabled:hover:bg-cyan-400/10;
}

.web3-submit-btn {
  background: linear-gradient(135deg, rgba(34,211,238,0.2), rgba(168,85,247,0.2));
  border: 1px solid rgba(255,255,255,0.08);
}
.web3-submit-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, rgba(34,211,238,0.3), rgba(168,85,247,0.3));
  border-color: rgba(255,255,255,0.14);
  box-shadow: 0 0 30px rgba(34,211,238,0.15), 0 0 60px rgba(168,85,247,0.08);
}
.web3-submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
