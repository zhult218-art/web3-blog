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
        <a v-if="footer.contactEmail" :href="'mailto:' + footer.contactEmail" class="sf-link">✉ {{ footer.contactEmail }}</a>
      </div>
    </div>
  </footer>
</template>

<script setup>
// ============================================================
// 全站页脚：版权 / ICP / 公安备案 / 联系邮箱 / 社交链接
// 全部内容来自系统设置（siteSettings store → blog_setting KV 表），
// 未填写的条目自动隐藏；整组均为空时页脚不渲染。
// ============================================================
import { computed } from 'vue'
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
</style>
