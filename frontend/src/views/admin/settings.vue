<template>
  <div class="settings-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">系统设置</h2>
        <p class="page-sub">站点名称、SEO、备案、社交链接与功能开关，保存后全站立即可生效</p>
      </div>
      <div class="head-actions">
        <button class="web3-btn ghost text-xs" :disabled="saving" @click="reload">重置未保存修改</button>
        <button class="web3-btn text-xs" :disabled="saving" @click="save">
          <span v-if="saving" class="spin"></span>
          {{ saving ? '保存中…' : `保存${tabMeta[activeTab].label}` }}
        </button>
      </div>
    </div>

    <div class="tabs">
      <button v-for="t in tabs" :key="t.key" class="tab" :class="{ active: activeTab === t.key }" @click="activeTab = t.key">
        <span class="tab-ico">{{ t.icon }}</span>{{ t.label }}
      </button>
    </div>

    <!-- ① 站点基础 -->
    <div v-if="activeTab === 'site'" class="card">
      <div class="card-hint">展示在浏览器标题、顶部导航、登录页与搜索引擎抓取信息中</div>
      <div class="grid two">
        <div class="field">
          <label>站点名称 <em>前台品牌名</em></label>
          <input class="web3-input" v-model="form.site.name" maxlength="40" placeholder="Aurora-朱" />
        </div>
        <div class="field">
          <label>浏览器标题 <em>document.title</em></label>
          <input class="web3-input" v-model="form.site.title" maxlength="80" placeholder="Aurora-朱 · 极光数字空间" />
        </div>
      </div>
      <div class="field">
        <label>站点口号 / Slogan</label>
        <input class="web3-input" v-model="form.site.slogan" maxlength="120" placeholder="一句话介绍你的站点" />
      </div>
      <div class="field">
        <label>站点地址 <em>https:// 开头，用于 SEO</em></label>
        <input class="web3-input" v-model="form.site.url" placeholder="https://example.com" />
      </div>
      <div class="field">
        <label>站点描述 <em>Meta Description，建议 80-160 字</em></label>
        <textarea class="web3-input" rows="2" v-model="form.site.description" maxlength="300"></textarea>
      </div>
      <div class="field">
        <label>关键词 <em>Meta Keywords，英文逗号分隔</em></label>
        <input class="web3-input" v-model="form.site.keywords" placeholder="web3, blog, forum" />
      </div>
      <div class="grid two">
        <div class="field">
          <label>Logo 图片 URL <em>留空使用极光渐变图标</em></label>
          <div class="url-with-preview">
            <input class="web3-input" v-model="form.site.logoUrl" placeholder="https://…/logo.png" />
            <img v-if="form.site.logoUrl" :src="form.site.logoUrl" class="url-preview" alt="logo 预览" @error="$event.target.style.opacity = 0.1" />
          </div>
        </div>
        <div class="field">
          <label>Favicon URL <em>浏览器标签小图标</em></label>
          <div class="url-with-preview">
            <input class="web3-input" v-model="form.site.faviconUrl" placeholder="https://…/favicon.ico" />
            <img v-if="form.site.faviconUrl" :src="form.site.faviconUrl" class="url-preview fav" alt="favicon 预览" @error="$event.target.style.opacity = 0.1" />
          </div>
        </div>
      </div>
      <div class="grid two">
        <div class="field">
          <label>默认分享封面 <em>og:image，社交分享缩略图</em></label>
          <input class="web3-input" v-model="form.site.ogImage" placeholder="https://…/og-cover.png" />
        </div>
        <div class="field">
          <label>默认文章封面 <em>无封面文章兜底图</em></label>
          <input class="web3-input" v-model="form.site.defaultCover" placeholder="https://…/cover.png" />
        </div>
      </div>
      <div class="live-preview">
        <span class="lp-label">实时预览</span>
        <div class="lp-card">
          <img v-if="form.site.logoUrl" :src="form.site.logoUrl" class="lp-logo-img" alt="" />
          <span v-else class="lp-logo">A</span>
          <div>
            <div class="lp-name">{{ form.site.name || '站点名称' }}</div>
            <div class="lp-slogan">{{ form.site.slogan || '站点口号' }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- ② 页脚与备案 -->
    <div v-else-if="activeTab === 'footer'" class="card">
      <div class="card-hint">展示在全站底部页脚；备案信息为空时对应条目自动隐藏</div>
      <div class="field">
        <label>版权文字</label>
        <input class="web3-input" v-model="form.footer.copyright" placeholder="© 2024-2026 Aurora-朱" />
      </div>
      <div class="grid two">
        <div class="field">
          <label>ICP 备案号</label>
          <input class="web3-input" v-model="form.footer.icpNo" placeholder="京ICP备XXXXXXXX号" />
        </div>
        <div class="field">
          <label>ICP 跳转链接 <em>工信部默认链接已预填</em></label>
          <input class="web3-input" v-model="form.footer.icpUrl" placeholder="https://beian.miit.gov.cn/" />
        </div>
      </div>
      <div class="grid two">
        <div class="field">
          <label>公安备案号</label>
          <input class="web3-input" v-model="form.footer.policeNo" placeholder="京公网安备 XXXXXXXXXXXXX号" />
        </div>
        <div class="field">
          <label>公安备案链接</label>
          <input class="web3-input" v-model="form.footer.policeUrl" placeholder="https://beian.mps.gov.cn/…" />
        </div>
      </div>
      <div class="field">
        <label>页脚寄语 <em>页脚品牌区的一小段说明，支持多行</em></label>
        <textarea class="web3-input" rows="3" v-model="form.footer.footerNote" maxlength="200"
          placeholder="以代码构建无限可能，让每一个想法都拥有沉浸式表达。"></textarea>
      </div>
      <div class="field">
        <label>商务/联系邮箱</label>
        <input class="web3-input" v-model="form.footer.contactEmail" placeholder="admin@example.com" />
      </div>
    </div>

    <!-- ③ 社交链接 -->
    <div v-else-if="activeTab === 'social'" class="card">
      <div class="card-hint">填写完整链接后自动出现在全站页脚；留空的平台不展示。邮箱可填 mailto: 或纯地址</div>
      <div class="grid two">
        <div v-for="s in socialDefs" :key="s.key" class="field">
          <label><span class="soc-ico">{{ s.icon }}</span>{{ s.label }}</label>
          <input class="web3-input" v-model="form.social[s.key]" :placeholder="s.placeholder" />
        </div>
      </div>
    </div>

    <!-- ④ 功能开关 -->
    <div v-else class="card">
      <div class="card-hint">关闭后对应入口将从前台导航隐藏、路由不可访问（管理员不受限）；保存后立即生效</div>
      <div class="toggle-list">
        <label v-for="f in featureDefs" :key="f.key" class="toggle-item">
          <div class="toggle-text">
            <span class="toggle-name">{{ f.icon }} {{ f.label }}</span>
            <span class="toggle-desc">{{ f.desc }}</span>
          </div>
          <button type="button" class="switch" :class="{ on: form.features[f.key] }" @click="form.features[f.key] = !form.features[f.key]">
            <span class="knob"></span>
          </button>
        </label>
      </div>
    </div>
  </div>
</template>

<script setup>
// ====================================================
// 系统设置（增强版）：站点基础 / 页脚备案 / 社交链接 / 功能开关
// 四组结构化配置，持久化到 blog_setting 键值表，保存后全站立生效
// ====================================================
import { reactive, ref, onMounted } from 'vue'
import { useToastStore } from '@/stores/modules/toast'
import { useSiteSettingsStore } from '@/stores/modules/siteSettings'

const toast = useToastStore()
const store = useSiteSettingsStore()

const tabs = [
  { key: 'site', label: '站点基础', icon: '🌐' },
  { key: 'footer', label: '页脚与备案', icon: '📋' },
  { key: 'social', label: '社交链接', icon: '🔗' },
  { key: 'features', label: '功能开关', icon: '🎛️' }
]
const tabMeta = Object.fromEntries(tabs.map(t => [t.key, t]))
const activeTab = ref('site')

const socialDefs = [
  { key: 'github', label: 'GitHub', icon: '🐙', placeholder: 'https://github.com/yourname' },
  { key: 'twitter', label: 'X / Twitter', icon: '🐦', placeholder: 'https://x.com/yourname' },
  { key: 'weibo', label: '微博', icon: '🔴', placeholder: 'https://weibo.com/yourname' },
  { key: 'bilibili', label: '哔哩哔哩', icon: '📺', placeholder: 'https://space.bilibili.com/…' },
  { key: 'zhihu', label: '知乎', icon: '💡', placeholder: 'https://www.zhihu.com/people/…' },
  { key: 'youtube', label: 'YouTube', icon: '▶️', placeholder: 'https://youtube.com/@…' },
  { key: 'telegram', label: 'Telegram', icon: '✈️', placeholder: 'https://t.me/yourname' },
  { key: 'discord', label: 'Discord', icon: '🎮', placeholder: 'https://discord.gg/…' },
  { key: 'qq', label: 'QQ 群', icon: '🐧', placeholder: 'https://qm.qq.com/…' },
  { key: 'email', label: '联系邮箱', icon: '✉️', placeholder: 'mailto:you@example.com' }
]

const featureDefs = [
  { key: 'quant', icon: '📊', label: '量化分析模块', desc: '隐藏 /quant 入口与导航，普通用户无法访问量化页面' },
  { key: 'jarvis', icon: '🤖', label: '星途语音助手', desc: '关闭后右下角悬浮助手入口隐藏' },
  { key: 'metaverse', icon: '🌀', label: '3D 星空漫游', desc: '隐藏 /three 粒子空间入口' },
  { key: 'forum', icon: '🛡️', label: '社区广场', desc: '隐藏 /community 入口，帖子与评论仍保留在数据库' },
  { key: 'shop', icon: '🛍️', label: '魔法商城', desc: '隐藏商城导航与购物车入口' },
  { key: 'media', icon: '🎬', label: '书影音 / 作品集', desc: '隐藏书影音、美甲小铺等媒体类入口' },
  { key: 'registration', icon: '📝', label: '前台注册入口', desc: '关闭后登录页不再显示注册页签（老用户仍可登录）' },
  { key: 'dynamicBg', icon: '✨', label: 'Three.js 动态背景', desc: '低配设备或希望页面更轻量时可关闭，改用纯色背景' },
  { key: 'maintenance', icon: '🚧', label: '维护模式横幅', desc: '开启后前台顶部显示「站点维护中」提示条' }
]

const form = reactive({
  site: {}, footer: {}, social: {}, features: {}
})
const saving = ref(false)

function hydrate() {
  form.site = { ...store.site }
  form.footer = { ...store.footer }
  form.social = { ...store.social }
  form.features = { ...store.features }
}

async function reload() {
  await store.load(true)
  hydrate()
  toast.success('已恢复为已保存配置')
}

async function save() {
  const key = activeTab.value
  saving.value = true
  try {
    await store.saveGroup(key, form[key])
    toast.success(`${tabMeta[key].label}已保存，前台立即生效`)
  } catch (e) {
    toast.error(e?.response?.data?.message || '保存失败，请确认已以管理员身份登录')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await store.load()
  hydrate()
})
</script>

<style scoped>
.settings-page { padding: 0.25rem; }
.page-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 1rem; flex-wrap: wrap; margin-bottom: 1.1rem; }
.page-title { font-size: 1.25rem; font-weight: 700; color: #fff; margin: 0; }
.page-sub { font-size: 0.78rem; color: rgba(255,255,255,0.4); margin: 4px 0 0; }
.head-actions { display: flex; gap: 0.6rem; }
.spin { display: inline-block; width: 12px; height: 12px; border-radius: 50%; border: 2px solid rgba(255,255,255,.3); border-top-color: #fff; animation: rot .7s linear infinite; margin-right: 5px; vertical-align: -1px; }
@keyframes rot { to { transform: rotate(360deg); } }

.tabs { display: flex; gap: 0.5rem; margin-bottom: 1rem; flex-wrap: wrap; }
.tab {
  padding: 0.5rem 1.15rem; border-radius: 10px; font-size: 0.85rem;
  color: rgba(255,255,255,0.45); background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.07); cursor: pointer; transition: all .2s;
}
.tab:hover { color: #fff; }
.tab.active {
  color: #fff; background: linear-gradient(135deg, var(--color-primary-soft), var(--color-accent-soft));
  border-color: var(--color-primary-border); box-shadow: 0 0 14px var(--color-glow-soft);
}
.tab-ico { margin-right: 5px; }

.card {
  background: rgba(15, 18, 40, 0.5); border: 1px solid rgba(255,255,255,0.06);
  border-radius: 14px; padding: 1.3rem 1.4rem;
}
.card-hint { font-size: 0.74rem; color: rgba(255,255,255,0.35); margin-bottom: 1.1rem; }
.grid { display: grid; gap: 0 1.1rem; }
.grid.two { grid-template-columns: repeat(2, minmax(0, 1fr)); }
@media (max-width: 720px) { .grid.two { grid-template-columns: 1fr; } }

.field { margin-bottom: 1rem; min-width: 0; }
.field label { display: block; font-size: 0.8rem; color: rgba(255,255,255,0.72); margin-bottom: 6px; font-weight: 500; }
.field label em { font-style: normal; font-size: 0.68rem; color: rgba(255,255,255,0.3); margin-left: 6px; }
.field .web3-input { width: 100%; box-sizing: border-box; }
textarea.web3-input { resize: vertical; font-family: inherit; line-height: 1.6; }

.url-with-preview { display: flex; align-items: center; gap: 0.6rem; }
.url-with-preview .web3-input { flex: 1; min-width: 0; }
.url-preview { width: 34px; height: 34px; border-radius: 8px; object-fit: cover; border: 1px solid rgba(255,255,255,0.12); background: rgba(255,255,255,0.04); flex-shrink: 0; }
.url-preview.fav { object-fit: contain; padding: 4px; }

.live-preview { margin-top: 0.4rem; padding-top: 1rem; border-top: 1px dashed rgba(255,255,255,0.08); }
.lp-label { font-size: 0.68rem; letter-spacing: 0.15em; text-transform: uppercase; color: rgba(255,255,255,0.3); }
.lp-card { display: flex; align-items: center; gap: 0.8rem; margin-top: 0.6rem; padding: 0.8rem 1rem; background: rgba(255,255,255,0.03); border-radius: 12px; width: fit-content; max-width: 100%; }
.lp-logo {
  width: 40px; height: 40px; border-radius: 11px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center; font-weight: 800; font-size: 1.05rem; color: #fff;
  background: linear-gradient(135deg, #22d3ee, #a855f7 52%, #34d399);
}
.lp-logo-img { width: 40px; height: 40px; border-radius: 11px; object-fit: cover; }
.lp-name { font-size: 0.9rem; font-weight: 700; color: #fff; }
.lp-slogan { font-size: 0.72rem; color: rgba(255,255,255,0.4); margin-top: 2px; max-width: 320px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.soc-ico { margin-right: 5px; }

.toggle-list { display: grid; grid-template-columns: repeat(2, minmax(0,1fr)); gap: 0.7rem; }
@media (max-width: 720px) { .toggle-list { grid-template-columns: 1fr; } }
.toggle-item {
  display: flex; align-items: center; justify-content: space-between; gap: 1rem;
  padding: 0.85rem 1rem; border-radius: 12px; cursor: pointer;
  background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.06);
  transition: border-color .2s, background .2s;
}
.toggle-item:hover { border-color: rgba(124,108,240,0.35); background: rgba(124,108,240,0.06); }
.toggle-name { font-size: 0.84rem; color: rgba(255,255,255,0.85); font-weight: 500; display: block; }
.toggle-desc { font-size: 0.7rem; color: rgba(255,255,255,0.35); margin-top: 3px; display: block; line-height: 1.5; }
.switch {
  position: relative; width: 42px; height: 23px; border-radius: 999px; flex-shrink: 0;
  background: rgba(255,255,255,0.12); border: none; cursor: pointer; transition: background .25s; padding: 0;
}
.switch .knob {
  position: absolute; top: 3px; left: 3px; width: 17px; height: 17px; border-radius: 50%;
  background: #fff; transition: transform .25s cubic-bezier(.34,1.56,.64,1);
}
.switch.on { background: linear-gradient(135deg, #7c6cf0, #6d28d9); box-shadow: 0 0 12px rgba(124,108,240,0.4); }
.switch.on .knob { transform: translateX(19px); }
</style>
