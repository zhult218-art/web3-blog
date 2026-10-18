<template>
  <footer v-if="hasContent" class="site-footer relative z-10">
    <div class="sf-inner mx-auto max-w-6xl px-6">
      <!-- 上半：品牌区 + 社交链接 -->
      <div class="sf-top">
        <div class="sf-brand">
          <router-link to="/" class="sf-logo" :title="brandName">
            <img v-if="logoUrl" :src="logoUrl" :alt="brandName" class="sf-logo-img" />
            <span v-else class="sf-logo-mark">A</span>
            <span class="sf-logo-name">{{ brandName }}</span>
          </router-link>
          <p v-if="note" class="sf-note">{{ note }}</p>
        </div>
        <div v-if="links.length" class="sf-socials">
          <a v-for="l in links" :key="l.key" :href="normalize(l.key, l.url)" target="_blank" rel="noopener"
            class="sf-social" :title="l.label" :aria-label="l.label">
            <span v-if="iconIsText(l.icon)" class="sf-social-text">{{ l.icon }}</span>
            <span v-else>{{ l.icon }}</span>
          </a>
        </div>
      </div>

      <!-- 下半：版权 + 备案 + 联系方式（空值自动隐藏） -->
      <div class="sf-bottom">
        <span v-if="copyright" class="sf-copy">{{ copyright }}</span>
        <a v-if="footer.icpNo" :href="footer.icpUrl || 'https://beian.miit.gov.cn/'" target="_blank" rel="noopener"
          class="sf-link">🏛 {{ footer.icpNo }}</a>
        <a v-if="footer.policeNo" :href="footer.policeUrl || 'https://beian.mps.gov.cn/'" target="_blank" rel="noopener"
          class="sf-link">🛡 {{ footer.policeNo }}</a>
        <button v-if="footer.contactEmail" type="button" class="sf-link sf-link-btn" @click="emailOpen = true">✉ {{ footer.contactEmail }}</button>
      </div>
    </div>

    <!-- 联系邮箱弹窗（点击页脚邮箱弹出，替代原生 mailto 直接跳转） -->
    <Teleport to="body">
      <transition name="mail-pop">
        <div v-if="emailOpen" class="mail-mask" @click.self="emailOpen = false">
          <div class="mail-card" role="dialog" aria-modal="true" aria-label="联系邮箱">
            <button class="mail-close" type="button" aria-label="关闭" @click="emailOpen = false">×</button>
            <div class="mail-head">
              <div class="mail-icon">✉</div>
              <div class="mail-head-text">
                <h3 class="mail-title">联系我</h3>
                <p class="mail-sub">欢迎邮件交流 · 合作 / 反馈 / 交个朋友</p>
              </div>
            </div>
            <div class="mail-addr">
              <span class="mail-addr-label">邮箱地址</span>
              <span class="mail-addr-text">{{ footer.contactEmail }}</span>
            </div>
            <div class="mail-actions">
              <button class="mail-btn is-primary" type="button" @click="copyEmail">
                {{ copyState === 'ok' ? '已复制 ✓' : copyState === 'fail' ? '请手动复制' : '复制邮箱' }}
              </button>
              <a class="mail-btn is-ghost" :href="'mailto:' + footer.contactEmail">打开邮件客户端</a>
            </div>
            <p class="mail-tip">若浏览器未安装邮件客户端，请直接复制地址发送。</p>
          </div>
        </div>
      </transition>
    </Teleport>
  </footer>
</template>

<script setup>
// ============================================================
// 全站页脚：版权 / ICP / 公安备案 / 联系邮箱 / 社交链接
// 全部内容来自系统设置（siteSettings store → blog_setting KV 表），
// 未填写的条目自动隐藏；整组均为空时页脚不渲染。
// ============================================================
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import { useSiteSettingsStore } from '@/stores/modules/siteSettings'

const site = useSiteSettingsStore()

const brandName = computed(() => site.brandName)
const logoUrl = computed(() => site.site.logoUrl || '')
const footer = computed(() => site.footer)
const note = computed(() => site.footer.footerNote || site.site.slogan || '')
const links = computed(() => site.socialLinks)
const copyright = computed(() => site.footer.copyright || '')

const hasContent = computed(() =>
  Boolean(copyright.value || note.value || links.value.length ||
    footer.value.icpNo || footer.value.policeNo || footer.value.contactEmail)
)

// 知乎图标用的是汉字「知」，按文本徽标渲染；其余 emoji 直接显示
function iconIsText(icon) {
  return icon === '知'
}

// 邮箱类链接补 mailto: 协议头
function normalize(key, url) {
  if (key === 'email' && url && !/^[a-z]+:/i.test(url)) return 'mailto:' + url
  return url
}

// ── 联系邮箱弹窗 ──
const emailOpen = ref(false)
// copyState: '' 待复制 → 'ok' 已复制 → 'fail' 需手动复制
const copyState = ref('')
const copied = computed(() => copyState.value === 'ok')
let copyTimer = null

// 复制邮箱地址：Clipboard API 失败时回退 execCommand；全部失败则提示手动复制
async function copyEmail() {
  const text = footer.value.contactEmail || ''
  if (!text) return
  let ok = false
  try {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      await navigator.clipboard.writeText(text)
      ok = true
    }
  } catch {}
  if (!ok) {
    try {
      const ta = document.createElement('textarea')
      ta.value = text
      ta.setAttribute('readonly', '')
      ta.style.position = 'fixed'
      ta.style.top = '-1000px'
      ta.style.opacity = '0'
      document.body.appendChild(ta)
      ta.select()
      ta.setSelectionRange(0, text.length)
      ok = document.execCommand('copy')
      document.body.removeChild(ta)
    } catch {}
  }
  copyState.value = ok ? 'ok' : 'fail'
  clearTimeout(copyTimer)
  copyTimer = setTimeout(() => { copyState.value = '' }, 2000)
}

// Esc 关闭弹窗
function onKeydown(e) {
  if (e.key === 'Escape') emailOpen.value = false
}
onMounted(() => window.addEventListener('keydown', onKeydown))
onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
  clearTimeout(copyTimer)
})
</script>

<style scoped>
.site-footer {
  margin-top: 3.5rem;
  border-top: 1px solid rgba(255, 255, 255, 0.07);
  background: linear-gradient(180deg, rgba(10, 10, 28, 0.35), rgba(8, 8, 22, 0.72));
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}
.sf-inner { padding-top: 1.8rem; padding-bottom: 1.6rem; }
.sf-top {
  display: flex; align-items: flex-start; justify-content: space-between; gap: 1.5rem; flex-wrap: wrap;
  padding-bottom: 1.2rem; border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}
.sf-logo { display: inline-flex; align-items: center; gap: 0.55rem; text-decoration: none; }
.sf-logo-mark {
  width: 30px; height: 30px; border-radius: 9px; flex-shrink: 0;
  display: inline-flex; align-items: center; justify-content: center;
  font-weight: 800; font-size: 0.85rem; color: #fff;
  background: linear-gradient(135deg, #22d3ee, #a855f7 52%, #34d399);
  box-shadow: 0 0 14px rgba(168, 85, 247, 0.35);
}
.sf-logo-img { width: 30px; height: 30px; border-radius: 9px; object-fit: cover; }
.sf-logo-name {
  font-size: 0.95rem; font-weight: 700;
  background: linear-gradient(90deg, #67e8f9, #c4b5fd 55%, #6ee7b7);
  -webkit-background-clip: text; background-clip: text; color: transparent;
}
.sf-note {
  margin: 0.6rem 0 0; max-width: 420px;
  font-size: 0.75rem; line-height: 1.7; color: rgba(255, 255, 255, 0.38);
}
.sf-socials { display: flex; flex-wrap: wrap; gap: 0.55rem; }
.sf-social {
  width: 34px; height: 34px; border-radius: 10px;
  display: inline-flex; align-items: center; justify-content: center;
  font-size: 0.95rem; text-decoration: none;
  color: rgba(255, 255, 255, 0.65);
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  transition: all 0.22s ease;
}
.sf-social:hover {
  color: #fff; transform: translateY(-2px);
  border-color: rgba(124, 108, 240, 0.45);
  background: rgba(124, 108, 240, 0.12);
  box-shadow: 0 8px 18px -8px rgba(124, 108, 240, 0.55);
}
.sf-social-text { font-size: 0.7rem; font-weight: 700; }
.sf-bottom {
  display: flex; align-items: center; flex-wrap: wrap; gap: 0.4rem 1.1rem;
  padding-top: 1.05rem;
  font-size: 0.72rem; color: rgba(255, 255, 255, 0.32);
}
.sf-copy { letter-spacing: 0.02em; }
.sf-link {
  color: rgba(255, 255, 255, 0.38); text-decoration: none;
  transition: color 0.2s;
}
.sf-link:hover { color: rgba(103, 232, 249, 0.9); }
.sf-link-btn { background: none; border: 0; padding: 0; font: inherit; cursor: pointer; text-align: left; }

/* ── 联系邮箱弹窗 ── */
.mail-mask {
  position: fixed; inset: 0; z-index: 4000;
  display: flex; align-items: center; justify-content: center;
  padding: 1rem;
  background: rgba(4, 4, 14, 0.62);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
}
.mail-card {
  position: relative;
  width: 100%; max-width: 380px;
  padding: 1.5rem 1.4rem 1.25rem;
  border-radius: 1.1rem;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: linear-gradient(160deg, rgba(18, 18, 46, 0.96), rgba(10, 10, 26, 0.98));
  box-shadow: 0 28px 60px -18px rgba(0, 0, 0, 0.75), inset 0 1px 0 rgba(255, 255, 255, 0.06);
  overflow: hidden;
}
.mail-card::before {
  content: '';
  position: absolute; inset: 0; pointer-events: none;
  background: radial-gradient(120px 120px at 12% 0%, rgba(168, 85, 247, 0.22), transparent 70%),
              radial-gradient(140px 140px at 100% 100%, rgba(34, 211, 238, 0.16), transparent 70%);
}
.mail-close {
  position: absolute; top: 0.55rem; right: 0.7rem; z-index: 2;
  width: 1.9rem; height: 1.9rem; border-radius: 9999px;
  border: 1px solid rgba(255, 255, 255, 0.1); background: rgba(255, 255, 255, 0.04);
  color: rgba(255, 255, 255, 0.7); font-size: 1.05rem; line-height: 1; cursor: pointer;
  transition: all 0.2s ease;
}
.mail-close:hover { color: #fff; border-color: rgba(255, 255, 255, 0.3); transform: rotate(90deg); }
.mail-head { position: relative; display: flex; align-items: center; gap: 0.75rem; margin-bottom: 1.15rem; }
.mail-icon {
  width: 2.6rem; height: 2.6rem; border-radius: 0.8rem; flex-shrink: 0;
  display: inline-flex; align-items: center; justify-content: center; font-size: 1.2rem; color: #fff;
  background: linear-gradient(135deg, #22d3ee, #a855f7 55%, #34d399);
  box-shadow: 0 0 18px rgba(168, 85, 247, 0.45);
}
.mail-title { margin: 0; font-size: 1.05rem; font-weight: 700; color: #fff; }
.mail-sub { margin: 0.15rem 0 0; font-size: 0.72rem; color: rgba(255, 255, 255, 0.42); }
.mail-addr {
  position: relative;
  display: flex; flex-direction: column; gap: 0.3rem;
  padding: 0.75rem 0.9rem; border-radius: 0.7rem;
  border: 1px dashed rgba(34, 211, 238, 0.28);
  background: rgba(34, 211, 238, 0.05);
}
.mail-addr-label { font-size: 0.62rem; letter-spacing: 0.14em; text-transform: uppercase; color: rgba(103, 232, 249, 0.7); }
.mail-addr-text { font-size: 0.9rem; font-weight: 600; color: #e2e8f0; word-break: break-all; }
.mail-actions { position: relative; display: flex; gap: 0.6rem; margin-top: 1.1rem; }
.mail-btn {
  flex: 1; text-align: center; padding: 0.55rem 0.9rem; border-radius: 0.7rem;
  font-size: 0.8rem; font-weight: 600; text-decoration: none; cursor: pointer;
  border: 1px solid transparent; transition: all 0.2s ease;
}
.mail-btn.is-primary {
  color: #06121a;
  background: linear-gradient(135deg, #67e8f9, #4ade80);
  box-shadow: 0 8px 20px -8px rgba(103, 232, 249, 0.6);
}
.mail-btn.is-primary:hover { transform: translateY(-1px); filter: brightness(1.06); }
.mail-btn.is-ghost {
  color: rgba(255, 255, 255, 0.8);
  border-color: rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.03);
}
.mail-btn.is-ghost:hover { color: #fff; border-color: rgba(255, 255, 255, 0.32); background: rgba(255, 255, 255, 0.07); }
.mail-tip { position: relative; margin: 0.85rem 0 0; font-size: 0.66rem; color: rgba(255, 255, 255, 0.32); text-align: center; }

.mail-pop-enter-active { transition: opacity 0.22s ease; }
.mail-pop-leave-active { transition: opacity 0.18s ease; }
.mail-pop-enter-from, .mail-pop-leave-to { opacity: 0; }
.mail-pop-enter-active .mail-card { animation: mailIn 0.3s cubic-bezier(0.22, 1, 0.36, 1); }
.mail-pop-leave-active .mail-card { transition: transform 0.18s ease; transform: scale(0.96); }
@keyframes mailIn {
  from { opacity: 0; transform: translateY(14px) scale(0.95); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
</style>
