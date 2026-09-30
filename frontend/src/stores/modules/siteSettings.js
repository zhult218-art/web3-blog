// ============================================================
// 站点全局配置 Store
// 数据持久化在 blog-service 的 blog_setting 键值表（公开 GET /blog/settings，
// 管理端 PUT /blog/admin/settings）。本站设置页将配置组织为四组结构化 JSON：
//   site     站点基础信息（名称/标题/口号/Logo/SEO 等）
//   footer   页脚备案（版权/ICP/公安备案/联系邮箱/页脚寄语）
//   social   社交链接
//   features 功能开关（量化/助手/3D/社区/商城/书影音/注册入口/动态背景/维护模式）
// 兼容历史裸 JSON 键：author / icp（缺失时回填默认值）。
// ============================================================
import { defineStore } from 'pinia'
import { getBlogSettings, saveAdminSettings } from '@/api/blog'

const DEFAULT_SITE = {
  name: 'Aurora-朱',
  title: 'Aurora-朱 · 极光数字空间',
  slogan: '以代码构建无限可能，让每一个想法都拥有沉浸式表达',
  url: '',
  logoUrl: '',
  faviconUrl: '',
  description: 'Web3 极简未来科技风个人综合技术门户',
  keywords: 'web3, blog, forum, shop, quant, metaverse',
  defaultCover: '',
  ogImage: ''
}

const DEFAULT_FOOTER = {
  copyright: '© 2024-2026 Aurora-朱',
  icpNo: '',
  icpUrl: 'https://beian.miit.gov.cn/',
  policeNo: '',
  policeUrl: '',
  footerNote: '',
  contactEmail: ''
}

const DEFAULT_SOCIAL = {
  github: '', twitter: '', weibo: '', bilibili: '', zhihu: '',
  telegram: '', discord: '', youtube: '', qq: '', email: ''
}

// 功能开关：默认全部开启（maintenance 默认关闭），与站点改造前行为一致
const DEFAULT_FEATURES = {
  quant: true,        // 量化分析模块
  jarvis: true,       // 星途语音助手
  metaverse: true,    // 3D 星空漫游
  forum: true,        // 社区广场
  shop: true,         // 商城
  media: true,        // 书影音 / 美甲小铺
  registration: true, // 前台注册入口
  dynamicBg: true,    // Three.js 动态背景
  maintenance: false  // 维护模式横幅
}

function safeParse(raw, fallback) {
  if (raw == null || raw === '') return { ...fallback }
  if (typeof raw === 'object') return { ...fallback, ...raw }
  try {
    const v = JSON.parse(raw)
    return typeof v === 'object' && v !== null ? { ...fallback, ...v } : { ...fallback }
  } catch {
    return { ...fallback }
  }
}

export const useSiteSettingsStore = defineStore('siteSettings', {
  state: () => ({
    loaded: false,
    loadingPromise: null,
    raw: {},
    site: { ...DEFAULT_SITE },
    footer: { ...DEFAULT_FOOTER },
    social: { ...DEFAULT_SOCIAL },
    features: { ...DEFAULT_FEATURES }
  }),
  getters: {
    brandName: state => state.site.name || DEFAULT_SITE.name,
    featureOn: state => key => state.features[key] !== false,
    // 页脚社交链接：仅返回已填写的项
    socialLinks: state => {
      const labels = {
        github: ['GitHub', '🐙'], twitter: ['X / Twitter', '🐦'], weibo: ['微博', '🔴'],
        bilibili: ['哔哩哔哩', '📺'], zhihu: ['知乎', '知'], telegram: ['Telegram', '✈️'],
        discord: ['Discord', '🎮'], youtube: ['YouTube', '▶️'], qq: ['QQ 群', '🐧'],
        email: ['邮箱', '✉️']
      }
      return Object.keys(DEFAULT_SOCIAL)
        .filter(k => state.social[k])
        .map(k => ({ key: k, label: labels[k][0], icon: labels[k][1], url: state.social[k] }))
    }
  },
  actions: {
    /** 拉取并解析全量配置（带缓存，路由守卫与 App 共用同一 Promise） */
    async load(force = false) {
      if (this.loaded && !force) return this
      if (this.loadingPromise && !force) return this.loadingPromise
      this.loadingPromise = this._fetch()
      await this.loadingPromise
      return this
    },
    async _fetch() {
      let map = {}
      try {
        const res = await getBlogSettings()
        map = res?.data || {}
      } catch {
        map = {}
      }
      this.raw = map
      this.site = safeParse(map.site, DEFAULT_SITE)
      this.footer = safeParse(map.footer, DEFAULT_FOOTER)
      this.social = safeParse(map.social, DEFAULT_SOCIAL)
      this.features = safeParse(map.features, DEFAULT_FEATURES)
      // 兼容历史键：icp / author（旧的“博客配置”页写入）
      const legacyIcp = safeParse(map.icp, {})
      if (!this.footer.icpNo && legacyIcp.icp_no) this.footer.icpNo = legacyIcp.icp_no
      if (this.footer.copyright === DEFAULT_FOOTER.copyright && legacyIcp.copyright) {
        this.footer.copyright = legacyIcp.copyright
      }
      const legacyAuthor = safeParse(map.author, {})
      if (!this.social.email && legacyAuthor.email) this.social.email = legacyAuthor.email
      if (!this.footer.contactEmail && legacyAuthor.email) this.footer.contactEmail = legacyAuthor.email
      this.loaded = true
      this.applyMeta()
    },
    /** 保存某一分组（site/footer/social/features），成功后同步本地并立即生效 */
    async saveGroup(group, value) {
      await saveAdminSettings({ [group]: JSON.stringify(value) })
      this[group] = { ...value }
      if (group === 'site') this.applyMeta()
      this.loaded = true
    },
    /** 将站点标题 / SEO / favicon 写入 document */
    applyMeta() {
      if (typeof document === 'undefined') return
      if (this.site.title) document.title = this.site.title
      this._setMeta('description', this.site.description)
      this._setMeta('keywords', this.site.keywords)
      if (this.site.ogImage) this._setMeta('og:image', this.site.ogImage, true)
      if (this.site.url) this._setMeta('og:url', this.site.url, true)
      if (this.site.faviconUrl) {
        let link = document.querySelector("link[rel='icon']")
        if (!link) {
          link = document.createElement('link')
          link.rel = 'icon'
          document.head.appendChild(link)
        }
        link.href = this.site.faviconUrl
      }
    },
    _setMeta(name, content, property = false) {
      if (!content) return
      const attr = property ? 'property' : 'name'
      let el = document.querySelector(`meta[${attr}="${name}"]`)
      if (!el) {
        el = document.createElement('meta')
        el.setAttribute(attr, name)
        document.head.appendChild(el)
      }
      el.setAttribute('content', content)
    }
  }
})
