<template>
  <div class="qp-vault space-y-4">
    <p class="text-xs text-amber-600/70 leading-relaxed">
      📦 github-myblog 中的 6 个开源量化项目已与本站量化模块打通：每个项目的核心能力都由站内功能真实实现（A 股真实行情驱动），
      点击「前往体验」直接跳转到对应工作台。
    </p>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
      <article v-for="p in projects" :key="p.name" class="qp-card">
        <div class="qp-head">
          <span class="qp-icon">{{ p.icon }}</span>
          <div class="min-w-0">
            <h3 class="text-sm font-bold text-amber-200 truncate">{{ p.title }}</h3>
            <code class="text-[10px] text-amber-600/60 font-mono">{{ p.name }}</code>
          </div>
          <span class="qp-lang">{{ p.lang }}</span>
        </div>
        <p class="text-[11px] text-gray-400 leading-relaxed mt-2">{{ p.desc }}</p>

        <!-- 能力映射：项目功能 → 站内实现 -->
        <ul class="qp-feats mt-3">
          <li v-for="f in p.feats" :key="f.name">
            <span class="qp-check">✓</span>
            <span class="text-[11px] text-gray-300">{{ f.name }}</span>
            <span class="qp-impl">→ {{ f.impl }}</span>
          </li>
        </ul>

        <button class="qp-go mt-3" @click="$emit('navigate', p.target)">
          ▶ 前往体验 · {{ p.targetLabel }}
        </button>
      </article>
    </div>

    <!-- 相关：AI/数据类仓库入口 -->
    <div class="qp-related glass-panel-sm p-4 flex flex-wrap items-center gap-x-6 gap-y-2">
      <span class="text-[11px] text-gray-500">相关仓库：</span>
      <router-link v-for="r in related" :key="r.to" :to="r.to" class="text-[11px] text-cyan-400 hover:text-cyan-300">
        {{ r.icon }} {{ r.name }} ↗
      </router-link>
    </div>
  </div>
</template>

<script setup>
// ============================================================
// 开源量化项目集成：6 个量化仓库 → 站内量化模块功能映射
// 每个能力均由本站真实功能实现（非链接占位）
// ============================================================
defineEmits(['navigate'])

const projects = [
  {
    name: 'Financial-API-main', title: 'Financial-API · 同花顺金融数据',
    icon: '📊', lang: 'Python SDK',
    desc: 'A 股行情、K 线、财务报表、基金数据接口封装；站内数据层由 akshare/腾讯行情 + Vibe-Research 多通道实现同等能力。',
    target: 'overview', targetLabel: '市场概览',
    feats: [
      { name: '实时行情 / 指数', impl: '市场概览 · A股行情' },
      { name: 'K 线历史数据', impl: '股票详情 K 线（10 周期）' },
      { name: '财务 / 基金数据', impl: 'VR 财务接口 + 详情页 Tab' },
    ],
  },
  {
    name: 'worth-buy-stocks-main', title: '什么值得买 · 纪律评分',
    icon: '🎯', lang: 'Python',
    desc: '把趋势交易纪律落成可复现的评分流程；站内由「8 种策略选股 + 智能推荐」实现 A 股版纪律筛选，输出明确候选。',
    target: 'dashboard', targetLabel: '智能看板·策略选股',
    feats: [
      { name: '纪律评分候选筛选', impl: '8 策略选股（真实 A 股）' },
      { name: '全市场扫描', impl: '量化推荐 · 10 分钟缓存' },
      { name: '风险约束与结论', impl: '推荐结果含涨跌/换手/市值口径' },
    ],
  },
  {
    name: 'go-stock-dev', title: 'Go-Stock · AI 股票分析',
    icon: '🤖', lang: 'Go + Vue3',
    desc: '实时盯盘 + 大模型消息面摘要；站内 A股行情 + AI分析双 Tab 实现同构能力，技术栈同样是 Vue3。',
    target: 'stocks', targetLabel: 'A股行情',
    feats: [
      { name: '自选股实时行情', impl: 'A 股行情表（真实报价）' },
      { name: 'AI 消息面摘要', impl: 'AI 分析 Tab（LLM）' },
      { name: '多数据源通道', impl: 'akshare/腾讯/VR 三源' },
    ],
  },
  {
    name: 'abu-master', title: '阿布量化 AbuQuant',
    icon: '📈', lang: 'Python · abupy',
    desc: '经典量化系统：择时/选股/仓位/回测全流程；站内回测工作台内置 11 种策略、真实日线数据与完整绩效指标。',
    target: 'backtest', targetLabel: '回测分析',
    feats: [
      { name: '策略回测全流程', impl: '回测分析（11 策略真实数据）' },
      { name: '资金曲线 / 绩效指标', impl: '夏普/回撤/胜率/盈亏比' },
      { name: '择时选股框架', impl: '策略管理 + 策略选股' },
    ],
  },
  {
    name: 'ai_quant_trade-master', title: 'AI量化交易操盘手',
    icon: '🧠', lang: 'Python',
    desc: '一站式平台：LLM、因子挖掘、传统/ML/RL 策略；站内由策略管理、回测、AI 深度投研模块覆盖核心链路。',
    target: 'strategies', targetLabel: '策略管理',
    feats: [
      { name: '多元策略谱系', impl: '策略管理 + 11 回测策略' },
      { name: '大模型应用', impl: 'AI 分析 / 深度投研' },
      { name: '辅助操盘工具', impl: '智能看板 + 新闻雷达' },
    ],
  },
  {
    name: 'QuantMind-master', title: 'QuantMind 量化大脑',
    icon: '🌌', lang: 'Py + TS',
    desc: 'AI 原生投研：数据→因子→模型→推理→回测→实盘闭环；站内 AI 深度投研实现 15 步 LLM 流水线真实研报。',
    target: 'deepai', targetLabel: 'AI深度投研',
    feats: [
      { name: 'AI 投研流水线', impl: '深度投研（15 步 LLM）' },
      { name: '模型工场 / 推理', impl: '可配置多 LLM Provider' },
      { name: '多市场扩展预留', impl: '全球指数接口 + VR 数据源' },
    ],
  },
]

const related = [
  { name: 'GPT_API_free', icon: '🔌', to: '/tools/sites' },
  { name: '60s API 广场', icon: '🌐', to: '/tools/api-plaza' },
]
</script>

<style scoped>
.qp-card {
  padding: 1.1rem 1.2rem;
  border-radius: 14px;
  background: linear-gradient(150deg, rgba(20,16,8,0.75), rgba(12,10,6,0.65));
  border: 1px solid rgba(245, 158, 11, 0.16);
  transition: border-color 0.3s, box-shadow 0.3s, transform 0.3s;
}
.qp-card:hover {
  border-color: rgba(245, 158, 11, 0.45);
  box-shadow: 0 0 28px rgba(245, 158, 11, 0.1);
  transform: translateY(-3px);
}
.qp-head { display: flex; align-items: center; gap: 0.7rem; }
.qp-icon {
  width: 2.4rem; height: 2.4rem; flex-shrink: 0;
  display: grid; place-items: center; font-size: 1.2rem;
  border-radius: 10px;
  background: rgba(245,158,11,0.1);
  border: 1px solid rgba(245,158,11,0.25);
}
.qp-lang {
  margin-left: auto; flex-shrink: 0;
  font-size: 0.62rem; color: #fbbf24;
  padding: 0.15rem 0.55rem; border-radius: 999px;
  background: rgba(245,158,11,0.1);
  border: 1px solid rgba(245,158,11,0.28);
}
.qp-feats { list-style: none; display: flex; flex-direction: column; gap: 0.35rem; }
.qp-feats li { display: flex; align-items: center; gap: 0.45rem; }
.qp-check {
  width: 16px; height: 16px; flex-shrink: 0;
  display: inline-grid; place-items: center;
  font-size: 9px; color: #04121a; font-weight: 900;
  border-radius: 50%;
  background: linear-gradient(135deg, #fde047, #f59e0b);
}
.qp-impl { margin-left: auto; font-size: 0.62rem; color: rgba(245,158,11,0.7); flex-shrink: 0; }
.qp-go {
  font-size: 0.74rem; padding: 0.45rem 1rem;
  border-radius: 9px; color: #1a1206; font-weight: 700;
  background: linear-gradient(135deg, #fde047, #f59e0b);
  box-shadow: 0 4px 16px rgba(245,158,11,0.25);
  transition: box-shadow 0.25s, transform 0.2s;
}
.qp-go:hover { box-shadow: 0 6px 24px rgba(245,158,11,0.45); transform: translateY(-1px); }
</style>
