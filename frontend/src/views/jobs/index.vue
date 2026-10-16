<template>
  <div class="jobboard-bg min-h-screen relative overflow-hidden">
    <!-- 背景光晕 -->
    <div class="absolute top-0 left-1/4 w-[500px] h-[500px] rounded-full bg-cyan-500/[0.05] blur-[120px] pointer-events-none"></div>
    <div class="absolute bottom-0 right-1/4 w-[400px] h-[400px] rounded-full bg-fuchsia-500/[0.05] blur-[100px] pointer-events-none"></div>
    <div class="absolute top-1/3 right-0 w-[450px] h-[450px] rounded-full bg-violet-500/[0.05] blur-[110px] pointer-events-none"></div>

    <div class="relative z-10 mx-auto max-w-7xl px-4 md:px-6 py-10">
      <!-- 返回 -->
      <div class="mb-5"><PageBack label="返回首页" to="/" /></div>

      <!-- ═════ Hero ═════ -->
      <div ref="heroBox" class="text-center mb-8 opacity-0">
        <div class="inline-flex items-center mb-3 px-3 py-1 rounded-full border border-cyan-400/30 bg-cyan-400/[0.05] text-[11px] text-cyan-300 font-bold tracking-wider">
          <span class="inline-block w-1.5 h-1.5 rounded-full bg-cyan-400 animate-pulse mr-1.5"></span>
          ADVENTURERS GUILD · 招募亭
        </div>
        <h1 class="text-4xl md:text-5xl font-black text-transparent bg-clip-text bg-gradient-to-r from-cyan-200 via-violet-200 to-fuchsia-200 mb-2 leading-tight">
          冒险者招募亭
        </h1>
        <p class="text-slate-400 text-sm">各大公会与势力招募人才 · 点击直达官网投递简历</p>

        <!-- 统计仪表盘 -->
        <div class="grid grid-cols-2 md:grid-cols-4 gap-3 max-w-3xl mx-auto mt-6">
          <div class="stat-mini">
            <div class="text-xl mb-1 opacity-80">🗂️</div>
            <p class="text-lg font-black text-cyan-300">{{ totalCount }}</p>
            <p class="text-[10px] text-slate-500 uppercase tracking-wider">收录单位</p>
          </div>
          <div class="stat-mini">
            <div class="text-xl mb-1 opacity-80">🏷️</div>
            <p class="text-lg font-black text-violet-300">{{ catCount }}</p>
            <p class="text-[10px] text-slate-500 uppercase tracking-wider">分类数量</p>
          </div>
          <div class="stat-mini">
            <div class="text-xl mb-1 opacity-80">⚡</div>
            <p class="text-lg font-black text-fuchsia-300">即时</p>
            <p class="text-[10px] text-slate-500 uppercase tracking-wider">实时搜索</p>
          </div>
          <div class="stat-mini">
            <div class="text-xl mb-1 opacity-80">🔗</div>
            <p class="text-lg font-black text-emerald-300">官方</p>
            <p class="text-[10px] text-slate-500 uppercase tracking-wider">直达官网</p>
          </div>
        </div>

        <!-- 分类分布条 -->
        <div class="max-w-3xl mx-auto mt-4 flex h-1.5 rounded-full overflow-hidden bg-white/[0.04]">
          <div v-for="c in categories.filter(x => x.key !== 'all')" :key="c.key"
            :style="{ width: (c.items.length / totalCount * 100) + '%', background: catColor(c.key).grad }"
            :title="c.name + ' ' + c.items.length" class="dist-seg"></div>
        </div>
        <div class="max-w-3xl mx-auto mt-2 flex flex-wrap gap-x-3 gap-y-1 justify-center text-[10px] text-slate-500">
          <span v-for="c in categories.filter(x => x.key !== 'all')" :key="c.key" class="inline-flex items-center gap-1">
            <span class="w-2 h-2 rounded-sm" :style="{ background: catColor(c.key).color }"></span>{{ c.name }}
          </span>
        </div>
      </div>

      <!-- ═════ 热门直投 ═════ -->
      <div v-if="!search" class="mb-8">
        <h2 class="section-title mb-3 flex items-center gap-2">
          <span class="text-base">🔥</span><span>热门直投</span>
          <span class="flex-1 h-px bg-gradient-to-r from-white/[0.1] to-transparent ml-2"></span>
        </h2>
        <div class="flex flex-wrap gap-2">
          <a v-for="item in hotPicks" :key="item.url" :href="item.url" target="_blank" rel="noopener"
            class="hot-chip" :style="{ '--c': item.color }">
            <span class="hot-dot">{{ item.name[0] }}</span>
            <span class="hot-name">{{ item.name }}</span>
            <span class="hot-arrow">→</span>
          </a>
        </div>
      </div>

      <!-- ═════ Sticky 搜索 + 分类栏 ═════ -->
      <div class="sticky-bar mb-8">
        <div class="sticky-inner">
          <!-- 搜索 -->
          <div class="relative max-w-xl mx-auto">
            <input v-model="search" type="text" placeholder="搜索企业名称或关键词..." class="isekai-input w-full pl-10 pr-10" />
            <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-4.35-4.35M16 11a5 5 0 11-10 0 5 5 0 0110 0z" />
            </svg>
            <button v-if="search" class="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300 text-lg leading-none" @click="search = ''">×</button>
          </div>
          <!-- 分类 -->
          <div class="flex flex-wrap gap-2 justify-center mt-3">
            <button v-for="c in categories" :key="c.key" @click="activeCat = c.key"
              :class="['cat-tab', activeCat === c.key ? 'cat-tab-active' : '']"
              :style="activeCat === c.key && c.key !== 'all' ? { '--accent': catColor(c.key).color, '--accent-bg': catColor(c.key).bg, '--accent-grad': catColor(c.key).grad } : {}">
              <span class="text-sm">{{ c.icon }}</span>
              <span>{{ c.name }}</span>
              <span class="text-[10px] opacity-60 ml-0.5">{{ countByCat(c.key) }}</span>
            </button>
          </div>
          <!-- 搜索结果计数 -->
          <div v-if="search" class="text-center mt-2 text-xs text-slate-500">
            搜索「<span class="text-cyan-300 font-medium">{{ search }}</span>」找到 <span class="text-cyan-300 font-bold">{{ searchResultCount }}</span> 家 ·
            <button class="text-slate-400 hover:text-slate-200 underline underline-offset-2" @click="search = ''">清除</button>
          </div>
        </div>
      </div>

      <!-- ═════ 列表 ═════ -->
      <template v-if="search">
        <!-- 搜索结果：按分类聚合 -->
        <div v-for="c in categories.filter(x => x.key !== 'all' && filterCompanies(x.items).length)" :key="c.key" class="mb-10">
          <h2 class="section-title mb-4 flex items-center gap-2">
            <span class="text-base">{{ c.icon }}</span>
            <span>{{ c.name }}</span>
            <span class="text-xs text-cyan-300">{{ filterCompanies(c.items).length }}</span>
            <span class="flex-1 h-px bg-gradient-to-r from-white/[0.1] to-transparent ml-2"></span>
          </h2>
          <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
            <a v-for="item in filterCompanies(c.items)" :key="item.url" :href="item.url" target="_blank" rel="noopener" v-tilt
              class="company-card" :style="{ '--c1': catColor(c.key).c1, '--c2': catColor(c.key).c2, '--cc': catColor(c.key).color }">
              <div class="company-logo">{{ item.name[0] }}</div>
              <p class="company-name" :title="item.name" v-html="highlight(item.name)"></p>
              <p class="company-tag" v-if="item.tag" v-html="highlight(item.tag)"></p>
              <div class="company-go">↗</div>
            </a>
          </div>
        </div>
        <!-- 搜索无结果：统一 EmptyState 组件 -->
        <EmptyState
          v-if="searchResultCount === 0"
          :text="`未找到匹配「${search}」的企业 · 试试搜索：阿里、华为、国家电网、谷歌...`"
        >
          <template #icon><span class="text-2xl">🔍</span></template>
        </EmptyState>
      </template>

      <template v-else>
        <!-- 全部 -->
        <template v-if="activeCat === 'all'">
          <div v-for="c in categories.filter(x => x.key !== 'all')" :key="c.key" class="mb-10">
            <h2 class="section-title mb-4 flex items-center gap-2">
              <span class="text-base">{{ c.icon }}</span>
              <span>{{ c.name }}</span>
              <span class="text-xs text-cyan-300">{{ c.items.length }}</span>
              <span class="flex-1 h-px bg-gradient-to-r from-white/[0.1] to-transparent ml-2"></span>
            </h2>
            <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
              <a v-for="item in c.items" :key="item.url" :href="item.url" target="_blank" rel="noopener" v-tilt
                class="company-card" :style="{ '--c1': catColor(c.key).c1, '--c2': catColor(c.key).c2, '--cc': catColor(c.key).color }">
                <div class="company-logo">{{ item.name[0] }}</div>
                <p class="company-name" :title="item.name">{{ item.name }}</p>
                <p class="company-tag" v-if="item.tag">{{ item.tag }}</p>
                <div class="company-go">↗</div>
              </a>
            </div>
          </div>
        </template>

        <!-- 单个分类 -->
        <template v-else>
          <div v-for="c in categories.filter(x => x.key === activeCat)" :key="c.key" class="mb-10">
            <h2 class="section-title mb-4 flex items-center gap-2">
              <span class="text-base">{{ c.icon }}</span>
              <span>{{ c.name }}</span>
              <span class="text-xs text-cyan-300">{{ c.items.length }}</span>
              <span class="flex-1 h-px bg-gradient-to-r from-white/[0.1] to-transparent ml-2"></span>
            </h2>
            <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
              <a v-for="item in c.items" :key="item.url" :href="item.url" target="_blank" rel="noopener" v-tilt
                class="company-card" :style="{ '--c1': catColor(c.key).c1, '--c2': catColor(c.key).c2, '--cc': catColor(c.key).color }">
                <div class="company-logo">{{ item.name[0] }}</div>
                <p class="company-name" :title="item.name">{{ item.name }}</p>
                <p class="company-tag" v-if="item.tag">{{ item.tag }}</p>
                <div class="company-go">↗</div>
              </a>
            </div>
          </div>
        </template>
      </template>

      <!-- 免责声明 -->
      <div class="mt-12 p-5 rounded-2xl border border-amber-400/15 bg-amber-400/[0.03] text-center">
        <p class="text-[11px] text-slate-500 leading-relaxed">
          ⚠️ 链接均指向企业官方招聘网站，本站不收集任何个人信息，投递请以官网为准。<br/>
          如发现链接失效或希望补充企业，请联系站点管理员。
        </p>
      </div>
    </div>

    <!-- 回到顶部 -->
    <transition name="fade">
      <button v-if="showTop" class="back-top" @click="scrollTop" title="回到顶部">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 10l7-7m0 0l7 7m-7-7v18"/></svg>
      </button>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { animate, stagger } from 'animejs'
import PageBack from '@/components/PageBack.vue'
import EmptyState from '@/components/common/EmptyState.vue'

defineOptions({ name: 'JobBoard' })

const search = ref('')
const activeCat = ref('all')
const heroBox = ref(null)
const showTop = ref(false)

// ════════════════════════════════════════════════════════════
// 企业数据：所有链接均指向企业官方招聘网站
// ════════════════════════════════════════════════════════════

const centralSOEs = [ // 央国企（国务院国资委监管 + 部属央企）
  { name: '国家电网', url: 'https://zhaopin.sgcc.com.cn/', tag: '电网' },
  { name: '南方电网', url: 'http://95598.csg.cn/', tag: '电网' },
  { name: '中国石油', url: 'https://zhaopin.cnpc.com.cn/', tag: '能源' },
  { name: '中国石化', url: 'https://job.sinopec.com/', tag: '能源' },
  { name: '中国海油', url: 'https://join.cnooc.com.cn/', tag: '能源' },
  { name: '中国华能', url: 'https://zhaopin.chng.com.cn/', tag: '电力' },
  { name: '中国华电', url: 'https://zhaopin.chd.com.cn/', tag: '电力' },
  { name: '国家能源', url: 'https://www.ceic.com/', tag: '能源' },
  { name: '国家电投', url: 'https://zhaopin.spic.com.cn/', tag: '电力' },
  { name: '中核集团', url: 'https://cnnc.zhiye.com/', tag: '核工业' },
  { name: '中国电信', url: 'https://chinatelecom.com.cn/zhaopin/', tag: '通信' },
  { name: '中国移动', url: 'https://hr.10086.cn/', tag: '通信' },
  { name: '中国联通', url: 'https://hr.chinaunicom.cn/', tag: '通信' },
  { name: '中国铁塔', url: 'https://www.chinatowercom.cn/', tag: '通信' },
  { name: '中国建筑', url: 'https://jobs.cscec.com/', tag: '建筑' },
  { name: '中国中铁', url: 'https://zhaopin.crec.cn/', tag: '基建' },
  { name: '中国铁建', url: 'https://zhaopin.crcc.cn/', tag: '基建' },
  { name: '中国交建', url: 'https://job.ccccltd.cn/', tag: '基建' },
  { name: '中国电建', url: 'https://www.powerchina.cn/', tag: '电力·基建' },
  { name: '中国能建', url: 'https://www.ceec.net.cn/', tag: '能源·基建' },
  { name: '中国中车', url: 'https://www.crrcgo.com.cn/', tag: '装备制造' },
  { name: '中国通号', url: 'https://www.crsc.cn/', tag: '轨道交通' },
  { name: '中国中化', url: 'https://www.sinochem.com/', tag: '化工' },
  { name: '中粮集团', url: 'https://cofco.zhiye.com/', tag: '粮油食品' },
  { name: '中国五矿', url: 'https://www.minmetals.com.cn/', tag: '矿冶' },
  { name: '中国铝业', url: 'https://www.chalco.com.cn/', tag: '有色金属' },
  { name: '中国宝武', url: 'https://www.baowugroup.com/', tag: '钢铁' },
  { name: '鞍钢集团', url: 'https://www.ansteel.cn/', tag: '钢铁' },
  { name: '中钢集团', url: 'https://www.sinosteel.com/', tag: '钢铁服务' },
  { name: '中国建材', url: 'https://www.cnbm.com.cn/', tag: '建材' },
  { name: '中国电子', url: 'https://www.cec.com.cn/', tag: '电子信息' },
  { name: '中国电科', url: 'https://cetc.zhiye.com/', tag: '电子科技' },
  { name: '中国航天科技', url: 'https://www.spacechina.com/', tag: '航天' },
  { name: '中国航天科工', url: 'https://www.casic.com.cn/', tag: '航天' },
  { name: '中国航空工业', url: 'https://www.avic.com/', tag: '航空' },
  { name: '中国兵器工业', url: 'https://www.norincogroup.com.cn/', tag: '兵器' },
  { name: '中国兵器装备', url: 'https://www.chinasouth.com.cn/', tag: '兵器' },
  { name: '中国船舶集团', url: 'https://www.cssc.net.cn/', tag: '船舶' },
  { name: '中国商飞', url: 'https://www.comac.cc/', tag: '民用航空' },
  { name: '中国航发', url: 'https://www.aecc.cn/', tag: '航空发动机' },
  { name: '中国邮政', url: 'https://www.chinapost.com.cn/', tag: '邮政物流' },
  { name: '招商局', url: 'https://www.cmhk.com/', tag: '综合' },
  { name: '华润集团', url: 'https://crc.zhiye.com/', tag: '综合' },
  { name: '中信集团', url: 'https://job.citicgroup.com/', tag: '综合金融' },
  { name: '光大集团', url: 'https://www.cebg.com.cn/', tag: '综合金融' },
  { name: '保利集团', url: 'https://www.poly.com.cn/', tag: '综合' },
  { name: '中旅集团', url: 'https://www.ctg.cn/', tag: '旅游' },
  { name: '华侨城', url: 'https://www.oct.com.cn/', tag: '文旅' },
  { name: '中国黄金', url: 'https://www.sinochemgold.com/', tag: '矿冶' },
  { name: '国药集团', url: 'https://www.sinopharm.com/', tag: '医药' },
  { name: '中储粮', url: 'https://www.sinograin.com/', tag: '粮油' },
]

const internetTech = [ // 互联网大厂
  { name: '阿里巴巴', url: 'https://talent.alibaba.com/', tag: '电商·云' },
  { name: '腾讯', url: 'https://careers.tencent.com/', tag: '社交·游戏' },
  { name: '百度', url: 'https://talent.baidu.com/', tag: '搜索·AI' },
  { name: '字节跳动', url: 'https://jobs.bytedance.com/', tag: '内容·AI' },
  { name: '美团', url: 'https://zhaopin.meituan.com/', tag: '本地生活' },
  { name: '京东', url: 'https://talent.jd.com/', tag: '电商·物流' },
  { name: '网易', url: 'https://hr.163.com/', tag: '游戏·内容' },
  { name: '拼多多', url: 'https://www.pinduoduo.com/careers', tag: '电商' },
  { name: '小米', url: 'https://hr.xiaomi.com/', tag: '硬件·IoT' },
  { name: '华为', url: 'https://career.huawei.com/', tag: '通信·芯片' },
  { name: '荣耀', url: 'https://www.hihonor.com/', tag: '智能终端' },
  { name: 'vivo', url: 'https://hr.vivo.com/', tag: '智能终端' },
  { name: 'OPPO', url: 'https://careers.oppo.com/', tag: '智能终端' },
  { name: '一加', url: 'https://www.oneplus.com/careers', tag: '智能终端' },
  { name: '真我Realme', url: 'https://www.realme.com/careers', tag: '智能终端' },
  { name: '联想', url: 'https://talent.lenovo.com/', tag: 'PC·服务' },
  { name: '滴滴出行', url: 'https://talent.didiglobal.com/', tag: '出行' },
  { name: '快手', url: 'https://zhaopin.kuaishou.cn/', tag: '短视频' },
  { name: 'B站', url: 'https://jobs.bilibili.com/', tag: '视频社区' },
  { name: '携程', url: 'https://job.ctrip.com/', tag: '旅游' },
  { name: '去哪儿', url: 'https://www.qunar.com/', tag: '旅游' },
  { name: '同程', url: 'https://www.ly.com/', tag: '旅游' },
  { name: '贝壳找房', url: 'https://zhaopin.ke.com/', tag: '房产' },
  { name: '链家', url: 'https://www.lianjia.com/', tag: '房产' },
  { name: '58同城', url: 'https://www.58.com/', tag: '分类信息' },
  { name: '新浪', url: 'https://career.sina.com.cn/', tag: '门户·社交' },
  { name: '搜狐', url: 'https://career.sohu.com/', tag: '门户·视频' },
  { name: '360', url: 'https://hr.360.cn/', tag: '安全·搜索' },
  { name: '科大讯飞', url: 'https://job.iflytek.com/', tag: '语音AI' },
  { name: '商汤科技', url: 'https://www.sensetime.com/cn/careers', tag: 'AI视觉' },
  { name: '旷视科技', url: 'https://megvii.com/careers', tag: 'AI视觉' },
  { name: '依图科技', url: 'https://www.yitutech.com/careers', tag: 'AI' },
  { name: '云从科技', url: 'https://www.cloudwalk.com/', tag: 'AI' },
  { name: '第四范式', url: 'https://www.4paradigm.com/careers', tag: 'AI' },
  { name: '蚂蚁集团', url: 'https://talent.antgroup.com/', tag: '金融科技' },
  { name: '菜鸟', url: 'https://talent.cainiao.com/', tag: '物流' },
  { name: '满帮', url: 'https://www.manbang.com/', tag: '货运物流' },
  { name: '货拉拉', url: 'https://www.huolala.cn/', tag: '货运物流' },
  { name: '小红书', url: 'https://job.xiaohongshu.com/', tag: '生活社区' },
  { name: '得物', url: 'https://www.dewu.com/', tag: '潮流电商' },
  { name: 'Shopee', url: 'https://careers.shopee.com/', tag: '跨境电商' },
  { name: 'SHEIN', url: 'https://www.shein.com/careers', tag: '跨境电商' },
  { name: 'TikTok', url: 'https://careers.tiktok.com/', tag: '出海短视频' },
  { name: 'Soul', url: 'https://www.soulapp.cn/', tag: '社交' },
  { name: '陌陌', url: 'https://www.immomo.com/', tag: '社交' },
  { name: '完美世界', url: 'https://www.wanmei.com/', tag: '游戏' },
  { name: '巨人网络', url: 'https://www.ztgame.com/', tag: '游戏' },
  { name: '盛趣游戏', url: 'https://www.shandagames.com/', tag: '游戏' },
  { name: '三七互娱', url: 'https://www.37.com/', tag: '游戏' },
  { name: '吉比特', url: 'https://www.g-bits.com/', tag: '游戏' },
  { name: '米哈游', url: 'https://www.mihoyo.com/', tag: '游戏' },
  { name: '莉莉丝', url: 'https://www.lilithgames.com/', tag: '游戏' },
  { name: '鹰角网络', url: 'https://www.hypergryph.com/', tag: '游戏' },
  { name: '叠纸游戏', url: 'https://www.papegames.com/', tag: '游戏' },
  { name: '金山软件', url: 'https://www.wps.cn/', tag: '办公软件' },
  { name: 'WPS', url: 'https://www.wps.cn/careers', tag: '办公软件' },
  { name: '用友', url: 'https://www.yonyou.com/', tag: '企业服务' },
  { name: '金蝶', url: 'https://www.kingdee.com/', tag: '企业服务' },
  { name: '广联达', url: 'https://www.glodon.com/', tag: '建筑软件' },
  { name: '中通', url: 'https://www.zto.com/', tag: '快递' },
  { name: '圆通', url: 'https://www.yto.net.cn/', tag: '快递' },
  { name: '申通', url: 'https://www.sto.cn/', tag: '快递' },
  { name: '韵达', url: 'https://www.yundaex.com/', tag: '快递' },
  { name: '顺丰', url: 'https://hr.sf-express.com/', tag: '快递' },
  { name: '京东物流', url: 'https://www.jdl.com/', tag: '物流' },
]

const foreignEnts = [ // 外企
  { name: 'Google', url: 'https://www.google.com/about/careers/app/', tag: '搜索·AI' },
  { name: 'Microsoft', url: 'https://careers.microsoft.com/', tag: '云·办公' },
  { name: 'Apple', url: 'https://www.apple.com/jobs/', tag: '消费电子' },
  { name: 'Amazon', url: 'https://www.amazon.jobs/', tag: '电商·云' },
  { name: 'Meta', url: 'https://www.metacareers.com/', tag: '社交·VR' },
  { name: 'IBM', url: 'https://www.ibm.com/employment/', tag: '咨询·IT' },
  { name: 'Intel', url: 'https://jobs.intel.com/', tag: '芯片' },
  { name: 'AMD', url: 'https://www.amd.com/en/corporate/careers', tag: '芯片' },
  { name: 'NVIDIA', url: 'https://www.nvidia.com/en-us/about-nvidia/careers/', tag: 'GPU·AI' },
  { name: 'Qualcomm', url: 'https://www.qualcomm.com/company/careers', tag: '芯片' },
  { name: 'Oracle', url: 'https://www.oracle.com/corporate/careers/', tag: '数据库·云' },
  { name: 'Cisco', url: 'https://jobs.cisco.com/', tag: '网络' },
  { name: 'Dell', url: 'https://www.dell.com/learn/corporate/en-us/careers', tag: '硬件' },
  { name: 'HP', url: 'https://www.hp.com/careers', tag: '硬件' },
  { name: 'VMware', url: 'https://www.vmware.com/company/careers.html', tag: '云计算' },
  { name: 'Red Hat', url: 'https://www.redhat.com/en/jobs', tag: '开源·Linux' },
  { name: 'SAP', url: 'https://www.sap.com/careers.html', tag: '企业软件' },
  { name: 'Samsung', url: 'https://www.samsung.com/about-us/careers/', tag: '电子' },
  { name: 'Sony', url: 'https://www.sony.com/en/SonyInfo/Careers/', tag: '电子·娱乐' },
  { name: 'Toshiba', url: 'https://www.toshiba.com/tai-careers/', tag: '电子' },
  { name: 'Panasonic', url: 'https://www.panasonic.com/global/careers.html', tag: '电子' },
  { name: 'Hitachi', url: 'https://www.hitachi.com/careers/', tag: '电子·工业' },
  { name: 'Siemens', url: 'https://www.siemens.com/global/en/company/jobs.html', tag: '工业' },
  { name: 'Bosch', url: 'https://www.bosch.com/careers/', tag: '工业·汽车' },
  { name: 'GE通用电气', url: 'https://www.ge.com/careers', tag: '工业' },
  { name: 'Honeywell', url: 'https://careers.honeywell.com/', tag: '工业' },
  { name: '3M', url: 'https://www.3m.com/3M/en_US/careers-us/', tag: '工业' },
  { name: 'P&G宝洁', url: 'https://www.pg.com/careers/', tag: '日化' },
  { name: 'Unilever', url: 'https://www.unilever.com/careers/', tag: '日化' },
  { name: 'Nestlé雀巢', url: 'https://www.nestle.com/careers', tag: '食品' },
  { name: 'Coca-Cola', url: 'https://www.coca-colacompany.com/careers', tag: '饮料' },
  { name: 'PepsiCo', url: 'https://www.pepsicojobs.com/', tag: '饮料食品' },
  { name: "L'Oréal欧莱雅", url: 'https://www.loreal.com/careers', tag: '美妆' },
  { name: 'Estée Lauder', url: 'https://www.elcompanies.com/careers', tag: '美妆' },
  { name: 'Johnson&Johnson', url: 'https://www.careers.jnj.com/', tag: '医药·日化' },
  { name: 'GSK', url: 'https://www.gsk.com/en-gb/careers/', tag: '医药' },
  { name: 'Sanofi赛诺菲', url: 'https://www.sanofi.com/en/careers', tag: '医药' },
  { name: 'Roche罗氏', url: 'https://careers.roche.com/', tag: '医药' },
  { name: 'Novartis', url: 'https://www.novartis.com/careers', tag: '医药' },
  { name: 'Pfizer辉瑞', url: 'https://www.pfizer.com/careers', tag: '医药' },
  { name: 'Bayer拜耳', url: 'https://www.bayer.com/careers', tag: '医药·化工' },
  { name: 'McKinsey麦肯锡', url: 'https://www.mckinsey.com/careers', tag: '咨询' },
  { name: 'BCG波士顿咨询', url: 'https://www.bcg.com/careers', tag: '咨询' },
  { name: 'Bain贝恩', url: 'https://www.bain.com/careers/', tag: '咨询' },
  { name: 'Deloitte德勤', url: 'https://www.deloitte.com/careers', tag: '审计咨询' },
  { name: 'PwC普华永道', url: 'https://www.pwc.com/careers', tag: '审计咨询' },
  { name: 'KPMG毕马威', url: 'https://home.kpmg/careers', tag: '审计咨询' },
  { name: 'EY安永', url: 'https://careers.ey.com/', tag: '审计咨询' },
  { name: 'Accenture埃森哲', url: 'https://www.accenture.com/us-en/careers', tag: '咨询IT' },
  { name: 'Capgemini凯捷', url: 'https://www.capgemini.com/careers/', tag: '咨询IT' },
  { name: 'DHL', url: 'https://careers.dhl.com/', tag: '物流' },
  { name: 'Maersk马士基', url: 'https://www.maersk.com/careers', tag: '航运物流' },
  { name: 'Schlumberger斯伦贝谢', url: 'https://careers.slb.com/', tag: '油气服务' },
  { name: 'Airbus空中客车', url: 'https://www.airbus.com/careers.html', tag: '航空' },
  { name: 'Boeing波音', url: 'https://www.boeing.com/careers', tag: '航空' },
]

const banksSecurities = [ // 银行/券商/基金
  { name: '工商银行', url: 'https://job.icbc.com.cn/', tag: '国有行' },
  { name: '建设银行', url: 'https://job2.ccb.com/', tag: '国有行' },
  { name: '农业银行', url: 'https://job.abchina.com/', tag: '国有行' },
  { name: '中国银行', url: 'https://www.boc.cn/aboutboc/bi4/index.html', tag: '国有行' },
  { name: '交通银行', url: 'https://job.bankcomm.com/', tag: '国有行' },
  { name: '邮储银行', url: 'https://www.psbc.com/careers', tag: '国有行' },
  { name: '国家开发银行', url: 'https://www.cdb.com.cn/', tag: '政策行' },
  { name: '中国进出口银行', url: 'https://www.eximbank.gov.cn/', tag: '政策行' },
  { name: '农发行', url: 'https://www.adbc.com.cn/', tag: '政策行' },
  { name: '招商银行', url: 'https://career.cmbchina.com/', tag: '股份行' },
  { name: '中信银行', url: 'https://job.citicbank.com/', tag: '股份行' },
  { name: '民生银行', url: 'https://www.cmbc.com.cn/', tag: '股份行' },
  { name: '兴业银行', url: 'https://job.cib.com.cn/', tag: '股份行' },
  { name: '平安银行', url: 'https://www.pingan.com/', tag: '股份行' },
  { name: '浦发银行', url: 'https://www.spdb.com.cn/', tag: '股份行' },
  { name: '光大银行', url: 'https://www.cebbank.com/', tag: '股份行' },
  { name: '华夏银行', url: 'https://www.hxb.com.cn/', tag: '股份行' },
  { name: '广发银行', url: 'https://www.cgbchina.com.cn/', tag: '股份行' },
  { name: '浙商银行', url: 'https://www.czbank.com/', tag: '股份行' },
  { name: '渤海银行', url: 'https://www.cbhb.com.cn/', tag: '股份行' },
  { name: '恒丰银行', url: 'https://www.hfbank.com.cn/', tag: '股份行' },
  { name: '北京银行', url: 'https://www.bankofbeijing.com.cn/', tag: '城商行' },
  { name: '上海银行', url: 'https://www.bosc.cn/', tag: '城商行' },
  { name: '宁波银行', url: 'https://www.nbcb.com.cn/', tag: '城商行' },
  { name: '江苏银行', url: 'https://www.jsbchina.cn/', tag: '城商行' },
  { name: '南京银行', url: 'https://www.njcb.com.cn/', tag: '城商行' },
  { name: '杭州银行', url: 'https://www.hzbank.com.cn/', tag: '城商行' },
  { name: '微众银行', url: 'https://www.webank.com/', tag: '互联网银行' },
  { name: '网商银行', url: 'https://www.mybank.cn/', tag: '互联网银行' },
  { name: '中国人寿', url: 'https://www.chinalife.com.cn/', tag: '保险' },
  { name: '中国人保', url: 'https://www.epicc.com.cn/', tag: '保险' },
  { name: '平安保险', url: 'https://www.pingan.com/careers', tag: '保险' },
  { name: '太平洋保险', url: 'https://www.cpic.com.cn/', tag: '保险' },
  { name: '新华保险', url: 'https://www.newchinalife.com/', tag: '保险' },
  { name: '泰康保险', url: 'https://www.taikanglife.com/', tag: '保险' },
  { name: '友邦保险', url: 'https://www.aia.com/careers', tag: '保险' },
  { name: '中信证券', url: 'https://www.citics.com/', tag: '券商' },
  { name: '海通证券', url: 'https://www.htsec.com/', tag: '券商' },
  { name: '国泰君安', url: 'https://www.gtja.com/careers', tag: '券商' },
  { name: '广发证券', url: 'https://www.gf.com.cn/careers', tag: '券商' },
  { name: '招商证券', url: 'https://www.newone.com.cn/', tag: '券商' },
  { name: '华泰证券', url: 'https://www.htsc.com/careers', tag: '券商' },
  { name: '申万宏源', url: 'https://www.swhysc.com/', tag: '券商' },
  { name: '银河证券', url: 'https://www.chinastock.com.cn/', tag: '券商' },
  { name: '中金公司', url: 'https://www.cicc.com/careers', tag: '券商' },
  { name: '中信建投', url: 'https://www.csc108.com/', tag: '券商' },
  { name: '国信证券', url: 'https://www.guosen.com.cn/', tag: '券商' },
  { name: '东方财富', url: 'https://www.eastmoney.com/', tag: '券商' },
  { name: '华夏基金', url: 'https://www.chinaamc.com/', tag: '基金' },
  { name: '易方达基金', url: 'https://www.efunds.com.cn/', tag: '基金' },
  { name: '嘉实基金', url: 'https://www.jsfund.cn/', tag: '基金' },
  { name: '南方基金', url: 'https://www.southernfund.com/', tag: '基金' },
  { name: '汇添富', url: 'https://www.99fund.com/', tag: '基金' },
  { name: '中欧基金', url: 'https://www.zcfund.com/', tag: '基金' },
  { name: '银华基金', url: 'https://www.yhfund.com.cn/', tag: '基金' },
]

const institutions = [ // 事业编 / 教育科研 / 公考
  { name: '中央机关公考', url: 'http://www.scs.gov.cn/', tag: '公务员' },
  { name: '国家公务员局', url: 'http://www.scs.gov.cn/', tag: '公务员' },
  { name: '各省公务员考试', url: 'http://www.chinagwy.org/', tag: '公务员' },
  { name: '事业单位招聘', url: 'http://www.shiyebian.net/', tag: '事业编' },
  { name: '全国事业单位', url: 'https://www.sydwzp.com/', tag: '事业编' },
  { name: '中国科学院', url: 'https://www.cas.cn/rcdw/zpxx/', tag: '科研' },
  { name: '中国工程院', url: 'https://www.cae.cn/', tag: '科研' },
  { name: '中国社会科学院', url: 'https://www.cass.cn/', tag: '社科' },
  { name: '国家自然科学基金', url: 'https://www.nsfc.gov.cn/', tag: '科研基金' },
  { name: '中国物理研究院', url: 'https://www.ciap.ac.cn/', tag: '科研' },
  { name: '高校人才网', url: 'https://www.gaoxiaojob.com/', tag: '高校' },
  { name: '中国教育在线', url: 'https://www.eol.cn/', tag: '教育' },
  { name: '博士人才网', url: 'https://www.1000plan.com/', tag: '高层次人才' },
  { name: '公立医院招聘', url: 'https://www.21wecan.com/', tag: '医院事业编' },
  { name: '卫生人才网', url: 'https://www.21wecan.com/', tag: '医疗卫生' },
  { name: '中小学教师招聘', url: 'https://www.jyb.cn/', tag: '教育' },
  { name: '军队文职', url: 'http://81rc.81.cn/', tag: '军队文职' },
  { name: '解放军文职', url: 'http://81rc.81.cn/', tag: '军队文职' },
  { name: '三支一扶', url: 'http://www.scsyf.com/', tag: '基层项目' },
  { name: '西部计划', url: 'https://xibu.youth.cn/', tag: '基层项目' },
  { name: '特岗教师', url: 'https://www.teacher.zj.cn/', tag: '基层项目' },
  { name: '大学生村官', url: 'https://www.54cunguan.com/', tag: '基层项目' },
]

const defense = [ // 国防 / 军工 / 航天科研
  { name: '航天科技一院', url: 'https://www.calt.com/', tag: '运载火箭' },
  { name: '航天科技五院', url: 'https://www.cast.cn/', tag: '空间技术' },
  { name: '航天科技八院', url: 'https://www.sast.net.cn/', tag: '航天' },
  { name: '航天科工二院', url: 'https://www.casic.com.cn/', tag: '防御' },
  { name: '航天科工三院', url: 'https://www.casic3y.com.cn/', tag: '飞航' },
  { name: '航空工业一飞院', url: 'https://www.avic.com/', tag: '航空' },
  { name: '中国航发商发', url: 'https://www.acae.com.cn/', tag: '航空发动机' },
  { name: '中航工业', url: 'https://www.avic.com/careers', tag: '航空装备' },
  { name: '中船重工研究所', url: 'https://csic.zhiye.com/', tag: '船舶研究' },
  { name: '兵器工业集团', url: 'https://www.norincogroup.com.cn/', tag: '兵器装备' },
  { name: '核工业集团', url: 'https://www.cnnc.com.cn/', tag: '核工业' },
  { name: '中国电科研究所', url: 'https://cetc.zhiye.com/', tag: '电子对抗' },
  { name: '中国工程物理研究院', url: 'https://www.caep.ac.cn/', tag: '国防科研' },
  { name: '总装部', url: 'http://www.mod.gov.cn/', tag: '军队' },
]

const autoManufacturing = [ // 汽车制造
  { name: '比亚迪', url: 'https://job.byd.com/', tag: '新能源车' },
  { name: '吉利', url: 'https://www.geely.com/careers', tag: '汽车' },
  { name: '长城汽车', url: 'https://www.gwm-job.com/', tag: '汽车' },
  { name: '长安汽车', url: 'https://www.changan.com.cn/', tag: '汽车' },
  { name: '上汽集团', url: 'https://www.saicmotor.com/careers', tag: '汽车' },
  { name: '一汽集团', url: 'https://www.faw.com.cn/', tag: '汽车' },
  { name: '东风汽车', url: 'https://www.dfmc.com.cn/', tag: '汽车' },
  { name: '广汽集团', url: 'https://www.gac-motor.com/', tag: '汽车' },
  { name: '北汽集团', url: 'https://www.baicmotor.com/', tag: '汽车' },
  { name: '奇瑞汽车', url: 'https://www.chery.cn/', tag: '汽车' },
  { name: '江淮汽车', url: 'https://www.jac.com.cn/', tag: '汽车' },
  { name: '蔚来', url: 'https://nio.com/careers', tag: '新能源车' },
  { name: '小鹏汽车', url: 'https://www.xiaopeng.com/careers', tag: '新能源车' },
  { name: '理想汽车', url: 'https://www.lixiang.com/careers', tag: '新能源车' },
  { name: '上汽大众', url: 'https://www.csvw.com/', tag: '合资汽车' },
  { name: '一汽大众', url: 'https://www.faw-vw.com/', tag: '合资汽车' },
  { name: '华晨宝马', url: 'https://www.bmw-brilliance.cn/', tag: '合资汽车' },
  { name: '北京奔驰', url: 'https://www.bbbc.com.cn/', tag: '合资汽车' },
  { name: '特斯拉中国', url: 'https://www.tesla.cn/careers', tag: '新能源车' },
]

const biotech = [ // 医药生物
  { name: '恒瑞医药', url: 'https://www.hrs.com.cn/', tag: '医药' },
  { name: '药明康德', url: 'https://www.wuxiapptec.com/careers', tag: 'CRO' },
  { name: '药明生物', url: 'https://www.wuxibiologics.com/careers', tag: '生物药' },
  { name: '迈瑞医疗', url: 'https://www.mindray.com/careers', tag: '医疗器械' },
  { name: '联影医疗', url: 'https://www.united-imaging.com/careers', tag: '医疗器械' },
  { name: '百济神州', url: 'https://www.beigene.com/careers', tag: '创新药' },
  { name: '信达生物', url: 'https://www.innoventbio.com/careers', tag: '生物药' },
  { name: '君实生物', url: 'https://www.junshipharma.com/', tag: '生物药' },
  { name: '复星医药', url: 'https://www.fosunpharma.com/', tag: '医药' },
  { name: '云南白药', url: 'https://www.yunnanbaiyao.com.cn/', tag: '中药' },
  { name: '片仔癀', url: 'https://www.pzt.com.cn/', tag: '中药' },
  { name: '同仁堂', url: 'https://www.tongrentang.com/', tag: '中药' },
  { name: '华大基因', url: 'https://www.bgi.com/careers', tag: '基因测序' },
  { name: '金斯威', url: 'https://www.genscript.com/careers', tag: '生物技术' },
  { name: '泰格医药', url: 'https://www.tigermedgrp.com/careers', tag: 'CRO' },
  { name: '康龙化成', url: 'https://www.pharmaron.com/careers', tag: 'CRO' },
]

const categories = [
  { key: 'all', name: '全部', icon: '🌐', items: [] },
  { key: 'central', name: '央国企', icon: '🏛️', items: centralSOEs },
  { key: 'tech', name: '互联网大厂', icon: '💻', items: internetTech },
  { name: '外企', key: 'foreign', icon: '🌍', items: foreignEnts },
  { key: 'bank', name: '银行券商', icon: '💰', items: banksSecurities },
  { key: 'inst', name: '事业编/公考', icon: '📜', items: institutions },
  { key: 'defense', name: '国防军工', icon: '🚀', items: defense },
  { key: 'auto', name: '汽车制造', icon: '🚗', items: autoManufacturing },
  { key: 'bio', name: '医药生物', icon: '🧬', items: biotech },
]

const totalCount = computed(() =>
  categories.reduce((sum, c) => sum + (c.key !== 'all' ? c.items.length : 0), 0)
)
const catCount = computed(() => categories.filter(x => x.key !== 'all').length)

// 分类主题色映射（每个分类独立配色，增强视觉区分度）
const catColorMap = {
  central: { color: '#fbbf24', bg: 'rgba(251,191,36,0.12)', c1: 'rgba(251,191,36,0.2)', c2: 'rgba(251,146,60,0.2)', grad: 'linear-gradient(90deg,#fbbf24,#f59e0b)' },
  tech:    { color: '#22d3ee', bg: 'rgba(34,211,238,0.12)', c1: 'rgba(34,211,238,0.2)', c2: 'rgba(167,139,250,0.2)', grad: 'linear-gradient(90deg,#22d3ee,#a855f7)' },
  foreign: { color: '#60a5fa', bg: 'rgba(96,165,250,0.12)', c1: 'rgba(96,165,250,0.2)', c2: 'rgba(59,130,246,0.2)', grad: 'linear-gradient(90deg,#60a5fa,#3b82f6)' },
  bank:    { color: '#34d399', bg: 'rgba(52,211,153,0.12)', c1: 'rgba(52,211,153,0.2)', c2: 'rgba(16,185,129,0.2)', grad: 'linear-gradient(90deg,#34d399,#10b981)' },
  inst:    { color: '#f472b6', bg: 'rgba(244,114,182,0.12)', c1: 'rgba(244,114,182,0.2)', c2: 'rgba(236,72,153,0.2)', grad: 'linear-gradient(90deg,#f472b6,#ec4899)' },
  defense: { color: '#f87171', bg: 'rgba(248,113,113,0.12)', c1: 'rgba(248,113,113,0.2)', c2: 'rgba(239,68,68,0.2)', grad: 'linear-gradient(90deg,#f87171,#ef4444)' },
  auto:    { color: '#a78bfa', bg: 'rgba(167,139,250,0.12)', c1: 'rgba(167,139,250,0.2)', c2: 'rgba(139,92,246,0.2)', grad: 'linear-gradient(90deg,#a78bfa,#8b5cf6)' },
  bio:     { color: '#5eead4', bg: 'rgba(94,234,212,0.12)', c1: 'rgba(94,234,212,0.2)', c2: 'rgba(45,212,191,0.2)', grad: 'linear-gradient(90deg,#5eead4,#2dd4bf)' },
}
function catColor(key) { return catColorMap[key] || catColorMap.tech }

// 热门直投快捷入口
const hotPicks = [
  { name: '阿里巴巴', url: 'https://talent.alibaba.com/', color: '#fb923c' },
  { name: '华为', url: 'https://career.huawei.com/', color: '#22d3ee' },
  { name: '国家电网', url: 'https://zhaopin.sgcc.com.cn/', color: '#fbbf24' },
  { name: '腾讯', url: 'https://careers.tencent.com/', color: '#60a5fa' },
  { name: '字节跳动', url: 'https://jobs.bytedance.com/', color: '#a855f7' },
  { name: '比亚迪', url: 'https://job.byd.com/', color: '#34d399' },
  { name: '工商银行', url: 'https://job.icbc.com.cn/', color: '#f472b6' },
  { name: '中国航天', url: 'https://www.spacechina.com/', color: '#f87171' },
]

function countByCat(key) {
  if (key === 'all') return totalCount.value
  const c = categories.find(x => x.key === key)
  return c ? c.items.length : 0
}

function filterCompanies(items) {
  if (!search.value.trim()) return items
  const kw = search.value.trim().toLowerCase()
  return items.filter(i =>
    i.name.toLowerCase().includes(kw) ||
    (i.tag && i.tag.toLowerCase().includes(kw))
  )
}

const searchResultCount = computed(() =>
  categories.reduce((sum, c) => sum + (c.key !== 'all' ? filterCompanies(c.items).length : 0), 0)
)

// 关键词高亮（搜索结果中标记匹配文字；v-html 输出前先做 HTML 转义，防数据源注入）
function escapeHtml(s) {
  return String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]))
}
function highlight(text) {
  if (!text) return ''
  const kw = search.value.trim()
  if (!kw) return escapeHtml(text)
  const escaped = kw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return escapeHtml(text).replace(new RegExp(escaped, 'gi'), m => `<span class="hl">${m}</span>`)
}

// 滚动监听 + 回到顶部
function onScroll() {
  showTop.value = window.scrollY > 600
}
function scrollTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 卡片入场动画（分类切换/搜索后触发）
let animTimer = null
function animateCards() {
  clearTimeout(animTimer)
  animTimer = setTimeout(() => {
    nextTick(() => {
      const cards = document.querySelectorAll('.company-card')
      if (cards.length && cards.length <= 200) {
        // 大列表只动画前若干张避免性能问题
        animate(cards, {
          opacity: [0, 1], translateY: [12, 0], scale: [0.96, 1],
          duration: 350, ease: 'outCubic', delay: stagger(25, { start: 50 })
        })
      }
    })
  }, 50)
}

watch([search, activeCat], () => {
  // 先重置卡片可见性，让动画能从 0 渐入
  document.querySelectorAll('.company-card').forEach(el => { el.style.opacity = '0' })
  animateCards()
})

onMounted(async () => {
  await nextTick()
  animate(heroBox.value, { opacity: [0, 1], translateY: [20, 0], duration: 800, ease: 'outCubic' })
  setTimeout(animateCards, 300)
  window.addEventListener('scroll', onScroll, { passive: true })
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
  clearTimeout(animTimer)
})
</script>

<style scoped>
.jobboard-bg {
  background: radial-gradient(ellipse at top, #0a0a2e 0%, #050510 100%);
  color: #e2e8f0;
}

.section-title {
  font-size: 1.05rem;
  font-weight: 700;
  color: #cbd5e1;
  letter-spacing: 0.02em;
}

/* ───── 统计迷你卡 ───── */
.stat-mini {
  position: relative;
  padding: 0.85rem 0.5rem;
  border-radius: 0.85rem;
  border: 1px solid rgba(255, 255, 255, 0.06);
  background: linear-gradient(135deg, rgba(12, 12, 36, 0.7) 0%, rgba(18, 16, 48, 0.7) 100%);
  text-align: center;
  transition: all 0.25s ease;
  overflow: hidden;
}
.stat-mini::after {
  content: '';
  position: absolute;
  top: -1rem; right: -1rem;
  width: 3rem; height: 3rem;
  border-radius: 9999px;
  background: radial-gradient(circle, rgba(34, 211, 238, 0.18), transparent 70%);
  filter: blur(8px);
  opacity: 0.5;
}
.stat-mini:hover { transform: translateY(-2px); border-color: rgba(34, 211, 238, 0.25); }

/* ───── 分类分布条 ───── */
.dist-seg {
  transition: width 0.4s cubic-bezier(0.22, 1, 0.36, 1);
  min-width: 2px;
}
.dist-seg:hover { filter: brightness(1.25); }

/* ───── 热门直投芯片 ───── */
.hot-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.4rem 0.75rem 0.4rem 0.4rem;
  border-radius: 9999px;
  border: 1px solid rgba(255, 255, 255, 0.07);
  background: rgba(12, 12, 36, 0.7);
  color: #cbd5e1;
  font-size: 0.78rem;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.22s ease;
}
.hot-dot {
  width: 1.4rem; height: 1.4rem;
  border-radius: 9999px;
  background: var(--c);
  color: #050510;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 0.75rem;
  box-shadow: 0 0 12px color-mix(in srgb, var(--c) 40%, transparent);
}
.hot-arrow {
  opacity: 0;
  transform: translateX(-4px);
  transition: all 0.22s ease;
  color: var(--c);
  font-weight: 700;
}
.hot-chip:hover {
  border-color: var(--c);
  background: color-mix(in srgb, var(--c) 8%, rgba(12, 12, 36, 0.9));
  color: #fff;
  transform: translateY(-2px);
  box-shadow: 0 6px 18px -6px color-mix(in srgb, var(--c) 45%, transparent);
}
.hot-chip:hover .hot-arrow { opacity: 1; transform: translateX(0); }

/* ───── 粘性搜索栏 ───── */
.sticky-bar {
  position: sticky;
  top: 0.5rem;
  z-index: 20;
}
.sticky-inner {
  padding: 0.85rem;
  border-radius: 1rem;
  border: 1px solid rgba(255, 255, 255, 0.06);
  background: rgba(8, 8, 24, 0.85);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  box-shadow: 0 8px 32px -12px rgba(0, 0, 0, 0.6);
}

/* ───── 分类标签 ───── */
.cat-tab {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.4rem 0.85rem;
  border-radius: 9999px;
  font-size: 0.78rem;
  border: 1px solid rgba(255, 255, 255, 0.06);
  background: rgba(12, 12, 36, 0.6);
  color: #94a3b8;
  cursor: pointer;
  transition: all 0.2s ease;
}
.cat-tab:hover {
  border-color: rgba(255, 255, 255, 0.18);
  color: #e2e8f0;
  transform: translateY(-1px);
}
.cat-tab-active {
  border-color: var(--accent, rgba(34, 211, 238, 0.5));
  background: var(--accent-bg, rgba(34, 211, 238, 0.1));
  color: var(--accent, #67e8f9);
  box-shadow: 0 0 16px color-mix(in srgb, var(--accent, #22d3ee) 25%, transparent);
}

/* ───── 企业卡片 ───── */
.company-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 1rem 0.75rem;
  border-radius: 0.9rem;
  border: 1px solid rgba(255, 255, 255, 0.06);
  background: linear-gradient(135deg, rgba(12, 12, 36, 0.85) 0%, rgba(18, 16, 48, 0.85) 100%);
  text-align: center;
  text-decoration: none;
  color: #e2e8f0;
  transition: transform 0.25s cubic-bezier(0.22, 1, 0.36, 1), border-color 0.25s ease, box-shadow 0.25s ease;
  overflow: hidden;
  will-change: transform;
}
.company-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, var(--c1, rgba(34, 211, 238, 0)) 0%, var(--c2, rgba(167, 139, 250, 0)) 100%);
  opacity: 0;
  transition: opacity 0.25s ease;
  pointer-events: none;
}
.company-card::after {
  content: '';
  position: absolute;
  top: -50%; right: -50%;
  width: 100%; height: 100%;
  background: radial-gradient(circle, var(--cc, rgba(34, 211, 238, 0.2)), transparent 60%);
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}
.company-card:hover {
  transform: translateY(-4px) scale(1.02);
  border-color: var(--cc, rgba(34, 211, 238, 0.4));
  box-shadow: 0 12px 28px -10px color-mix(in srgb, var(--cc, #22d3ee) 50%, transparent);
}
.company-card:hover::before { opacity: 1; }
.company-card:hover::after { opacity: 0.5; }

.company-logo {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 0.65rem;
  background: linear-gradient(135deg, var(--c1, rgba(34, 211, 238, 0.15)) 0%, var(--c2, rgba(167, 139, 250, 0.15)) 100%);
  border: 1px solid color-mix(in srgb, var(--cc, #22d3ee) 30%, transparent);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
  font-weight: 700;
  color: var(--cc, #67e8f9);
  margin-bottom: 0.5rem;
  transition: all 0.25s ease;
}
.company-card:hover .company-logo {
  transform: scale(1.08) rotate(-3deg);
  box-shadow: 0 0 16px color-mix(in srgb, var(--cc, #22d3ee) 40%, transparent);
}
.company-name {
  font-size: 0.82rem;
  font-weight: 600;
  color: #e2e8f0;
  line-height: 1.2;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}
.company-tag {
  font-size: 0.65rem;
  color: #64748b;
  margin-top: 0.25rem;
}
.company-go {
  position: absolute;
  top: 0.4rem;
  right: 0.5rem;
  font-size: 0.7rem;
  color: var(--cc, #475569);
  opacity: 0;
  transform: translate(-2px, 2px);
  transition: all 0.2s ease;
}
.company-card:hover .company-go {
  opacity: 1;
  transform: translate(0, 0);
}

/* 关键词高亮 */
:deep(.hl) {
  color: #fde047;
  background: rgba(253, 224, 71, 0.12);
  border-radius: 3px;
  padding: 0 2px;
  font-weight: 700;
}

/* ───── 输入框 ───── */
:deep(.isekai-input) {
  background: rgba(12, 12, 36, 0.7);
  border: 1px solid rgba(34, 211, 238, 0.15);
  color: #e2e8f0;
  border-radius: 0.6rem;
  padding: 0.55rem 0.85rem;
  font-size: 0.85rem;
  transition: all 0.2s ease;
}
:deep(.isekai-input:focus) {
  outline: none;
  border-color: rgba(34, 211, 238, 0.5);
  box-shadow: 0 0 0 3px rgba(34, 211, 238, 0.1);
  background: rgba(12, 12, 36, 0.9);
}

/* ───── 回到顶部 ───── */
.back-top {
  position: fixed;
  right: 1.25rem;
  bottom: 1.25rem;
  z-index: 50;
  width: 2.6rem;
  height: 2.6rem;
  border-radius: 9999px;
  border: 1px solid rgba(34, 211, 238, 0.3);
  background: rgba(12, 12, 36, 0.85);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  color: #67e8f9;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 8px 24px -8px rgba(34, 211, 238, 0.3);
  transition: all 0.2s ease;
}
.back-top:hover {
  transform: translateY(-2px);
  border-color: rgba(34, 211, 238, 0.6);
  box-shadow: 0 12px 28px -8px rgba(34, 211, 238, 0.5);
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.25s ease, transform 0.25s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(8px); }
</style>
