<template>
  <div class="min-h-screen px-4 md:px-6 py-10">
    <div class="mx-auto max-w-6xl">
      <PageHeader title="网盘直链助手" subtitle="LinkSwift · Userscript · 九大网盘直链获取 · 多下载器协同"
        :crumbs="[{ label: '首页', to: '/' }, { label: '运维工具箱', to: '/tools' }, { label: '网盘直链助手' }]" />

      <!-- 安装面板 -->
      <section class="glass-panel p-6 mb-6">
        <div class="flex flex-col lg:flex-row gap-6">
          <div class="flex-1">
            <div class="flex items-center gap-3 mb-3">
              <span class="text-3xl">☁️</span>
              <div>
                <h2 class="text-lg font-bold text-white">LinkSwift</h2>
                <p class="text-xs text-gray-500">一个基于 JavaScript 的网盘文件下载地址获取工具</p>
              </div>
            </div>
            <p class="text-sm text-gray-400 leading-relaxed mb-4">
              在网盘网页端一键解析出文件直链，支持推送到 IDM / Aria2 / 比特彗星等下载器。
              本页托管脚本副本，供站内一键安装与备用下载（脚本更新源仍指向官方仓库）。
            </p>
            <div class="flex flex-wrap items-center gap-2 text-[11px]">
              <span class="px-2 py-1 rounded border border-purple-400/25 text-purple-300">v{{ info.version }}</span>
              <span class="px-2 py-1 rounded border border-white/10 text-gray-400">更新 {{ info.updated }}</span>
              <span class="px-2 py-1 rounded border border-white/10 text-gray-400">AGPL-3.0</span>
              <a :href="info.repo" target="_blank" rel="noopener" class="px-2 py-1 rounded border border-cyan-400/25 text-cyan-300 hover:text-cyan-200">官方仓库 ↗</a>
              <a :href="info.scriptcat" target="_blank" rel="noopener" class="px-2 py-1 rounded border border-cyan-400/25 text-cyan-300 hover:text-cyan-200">脚本猫 ↗</a>
            </div>
          </div>

          <div class="lg:w-80 flex-shrink-0">
            <div class="glass-panel-sm p-5">
              <p class="text-xs text-gray-400 mb-3 flex items-center gap-1">📦 安装步骤</p>
              <ol class="text-xs text-gray-400 space-y-2 mb-4 list-decimal list-inside">
                <li>浏览器安装扩展 <a href="https://www.tampermonkey.net/" target="_blank" rel="noopener" class="text-cyan-400 hover:text-cyan-300">Tampermonkey（篡改猴）</a></li>
                <li>点击下方「一键安装」按钮</li>
                <li>在打开的安装页确认安装</li>
                <li>刷新网盘网页即可看到下载助手按钮</li>
              </ol>
              <a :href="installUrl" class="web3-btn text-sm w-full text-center block">⬇ 一键安装脚本</a>
              <div class="flex gap-2 mt-3">
                <button class="web3-btn-outline text-xs flex-1 !py-2" @click="copyInstall">🔗 复制安装链接</button>
                <a :href="info.repo" target="_blank" rel="noopener" class="web3-btn-ghost text-xs flex-1 !py-2 text-center">官方源安装</a>
              </div>
              <p class="text-[10px] text-gray-600 mt-3">如安装后无反应，请确认脚本已在扩展中启用，并刷新已打开的网盘页面。</p>
            </div>
          </div>
        </div>
      </section>

      <!-- 支持网盘 -->
      <section class="mb-6">
        <h2 class="text-lg font-semibold text-white mb-4 flex items-center gap-2"><span>🌐</span>支持的九大网盘</h2>
        <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-3 xl:grid-cols-3 gap-3">
          <div v-for="d in disks" :key="d.name" class="glass-panel-sm p-4 flex items-start gap-3 hover:border-cyan-400/20 transition">
            <span class="text-2xl flex-shrink-0">{{ d.icon }}</span>
            <div class="min-w-0">
              <p class="text-sm text-white">{{ d.name }}</p>
              <p class="text-[11px] text-gray-500 mt-0.5">{{ d.desc }}</p>
              <p class="text-[10px] text-gray-600 mt-1 font-mono truncate">{{ d.host }}</p>
            </div>
          </div>
        </div>
      </section>

      <div class="holo-bar mb-6"></div>

      <!-- 下载方式 -->
      <section class="glass-panel p-6 mb-6">
        <h2 class="text-sm font-bold text-white mb-4 flex items-center gap-2"><span>📥</span>支持的下载方式</h2>
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          <div v-for="m in downloaders" :key="m.name" class="glass-panel-sm p-4">
            <div class="flex items-center gap-2 mb-1">
              <span class="text-base">{{ m.icon }}</span>
              <p class="text-sm text-white">{{ m.name }}</p>
            </div>
            <p class="text-[11px] text-gray-500 leading-relaxed">{{ m.desc }}</p>
            <a :href="m.link" target="_blank" rel="noopener" class="text-[11px] text-cyan-400 hover:text-cyan-300 mt-2 inline-block">前往下载器 ↗</a>
          </div>
        </div>
      </section>

      <!-- 配置文件 -->
      <section class="glass-panel p-6 mb-6">
        <h2 class="text-sm font-bold text-white mb-2 flex items-center gap-2"><span>⚙️</span>离线配置备份</h2>
        <p class="text-xs text-gray-500 mb-4 leading-relaxed">
          当脚本无法访问官方配置服务器时，可下载对应网盘的配置备份。在脚本设置页选择「本地配置文件」导入即可。
          各文件按网盘拆分，含请求接口与页面元素匹配规则。
        </p>
        <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-2">
          <a v-for="c in configs" :key="c.file" :href="c.href" :download="c.download"
            class="glass-panel-sm px-4 py-3 flex items-center justify-between hover:border-purple-400/25 transition group">
            <div>
              <p class="text-xs text-gray-300 group-hover:text-white">{{ c.name }}</p>
              <p class="text-[10px] text-gray-600 mt-0.5">{{ c.size }}</p>
            </div>
            <span class="text-xs text-gray-600">⬇</span>
          </a>
        </div>
        <p class="text-[11px] text-gray-600 mt-4">
          另有配套脚本「百度网盘会员青春版」（显示会员身份 / 视频倍速 / 生成字幕）：
          <a :href="youthScriptUrl" class="text-cyan-400 hover:text-cyan-300 ml-1">查看脚本 ↗</a>
        </p>
      </section>

      <!-- 合规声明 -->
      <section class="glass-panel p-5">
        <h2 class="text-sm font-bold text-white mb-2 flex items-center gap-2"><span>⚠️</span>使用须知</h2>
        <ul class="text-xs text-gray-500 space-y-1.5 leading-relaxed">
          <li>· 本项目所有下载链接均基于各大网盘服务商公开 API 获取，未以任何形式破解或规避网盘的限速机制。</li>
          <li>· 速度差异通常来源于服务商接口策略、本地网络环境或阶段性带宽调整，不代表具备「破解限速」能力。</li>
          <li>· 脚本在你的浏览器本地运行，不会将账号凭证上传至本站；本站仅托管脚本文本副本。</li>
          <li>· 请遵守各网盘服务条款，仅用于合法的个人文件下载需求。</li>
        </ul>
      </section>
    </div>
  </div>
</template>

<script setup>
// ====================================================
// 网盘直链助手（LinkSwift）工具页
// 托管 .user.js 副本 + 一键安装 + 九大网盘说明 + 下载器引导
// 静态资源位于 public/tools/linkswift/（构建后随 dist 一起发布）
// ====================================================
import { computed } from 'vue'
import { useToastStore } from '@/stores/modules/toast'
import PageHeader from '@/components/common/PageHeader.vue'

const toast = useToastStore()

// 脚本元信息（与托管文件的 @version 保持一致）
const info = {
  version: '1.1.4',
  updated: '2026-09-09',
  repo: 'https://github.com/hmjz100/LinkSwift',
  scriptcat: 'https://scriptcat.org/script-show-page/1604',
}

// 一键安装地址：用当前站点 origin 拼出绝对地址，便于复制到其他浏览器安装
const installUrl = computed(() => (typeof window !== 'undefined' ? window.location.origin : '') + '/tools/linkswift/linkswift.user.js')
const youthScriptUrl = computed(() => (typeof window !== 'undefined' ? window.location.origin : '') + '/tools/linkswift/linkswift-baidu-youth.user.js')

// 支持的九大网盘
const disks = [
  { name: '百度网盘', icon: '🐻', desc: '国内用户量最大，支持分享页与网盘主页', host: 'pan.baidu.com' },
  { name: '阿里云盘', icon: '🅰️', desc: '支持 alipan / aliyundrive 双域名', host: 'alipan.com' },
  { name: '中国移动云盘', icon: '📶', desc: '原和彩云，支持 139 域名', host: 'yun.139.com' },
  { name: '天翼云盘', icon: '🛰️', desc: '中国电信云盘', host: 'cloud.189.cn' },
  { name: '迅雷云盘', icon: '⚡', desc: '迅雷云盘网页端直链获取', host: 'pan.xunlei.com' },
  { name: '光鸭云盘', icon: '🦆', desc: '新接入网盘（v1.1.4 新增）', host: 'guangyapan.com' },
  { name: '夸克网盘', icon: '🔵', desc: '支持分享页与个人网盘', host: 'pan.quark.cn' },
  { name: 'UC 网盘', icon: '🟠', desc: 'UC 浏览器云盘', host: 'drive.uc.cn' },
  { name: '123 云盘', icon: '🔢', desc: '支持多域名（123pan.com/.cn 等）', host: '123pan.com' },
]

// 支持的下载方式与外部下载器引导
const downloaders = [
  { name: 'API 下载', icon: '🔗', desc: '点击链接直接交给浏览器或 IDM 接管，最通用', link: 'https://www.youxiaohou.com/zh-cn/idm.html' },
  { name: 'IDM / NDM', icon: '🚀', desc: '多线程下载利器，需安装浏览器集成扩展', link: 'https://www.internetdownloadmanager.com/' },
  { name: 'Aria2 协议', icon: '🧩', desc: '复制 aria2c 命令粘贴到 Aria2 系下载器', link: 'https://aria2.github.io/' },
  { name: 'RPC 远程推送', icon: '📡', desc: '推送到 Motrix / AriaNg 等本地或远程 RPC 服务', link: 'https://motrix.app/' },
  { name: 'cURL 命令', icon: '⌨️', desc: '复制命令行粘贴到 Windows/Linux/macOS 终端，支持断点续传', link: 'https://www.youxiaohou.com/zh-cn/curl.html' },
  { name: '比特彗星', icon: '☄️', desc: '推送链接到比特彗星 BitComet 下载', link: 'https://www.bitcomet.com/' },
]

// 离线配置备份文件（同名托管于 public/tools/linkswift/config/）
const configs = [
  { file: 'config.json', name: '通用 / 百度', download: 'config.json', size: '5.2 KB' },
  { file: 'ali.json', name: '阿里云盘', download: 'ali.json', size: '4.7 KB' },
  { file: 'quark.json', name: '夸克 / UC', download: 'quark.json', size: '4.6 KB' },
  { file: 'xunlei.json', name: '迅雷云盘', download: 'xunlei.json', size: '12 KB' },
  { file: 'tianyi.json', name: '天翼云盘', download: 'tianyi.json', size: '4.6 KB' },
  { file: 'yidong.json', name: '移动云盘', download: 'yidong.json', size: '4.5 KB' },
].map(c => ({ ...c, href: `/tools/linkswift/config/${c.file}` }))

// 复制安装链接到剪贴板
function copyInstall() {
  navigator.clipboard?.writeText(installUrl.value)
    .then(() => toast.success('安装链接已复制'))
    .catch(() => toast.error('复制失败'))
}
</script>
