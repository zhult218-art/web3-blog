<template>
  <div class="min-h-screen py-6 px-6">
    <div class="max-w-[1400px] mx-auto">
      <!-- Header -->
      <div class="flex items-center justify-between mb-5">
        <h1 class="text-xl font-bold text-white"><span class="text-gradient-cyber">A股</span> 量化看板</h1>
        <div class="flex items-center gap-3">
          <span class="text-[11px] text-gray-500" v-if="lastRefresh">更新于 {{ lastRefresh }}</span>
          <button class="web3-btn-outline text-xs !px-3 !py-1.5" :disabled="refreshing" @click="refreshAll">
            <span v-if="refreshing" class="inline-block h-3 w-3 rounded-full border-2 border-white/15 border-t-web3-accent animate-spin mr-1 align-middle"></span>
            {{ refreshing ? '刷新中...' : '刷新' }}
          </button>
          <button class="text-[11px] px-2 py-1 rounded border" :class="autoRefresh ? 'border-green-500/40 text-green-400' : 'border-white/10 text-gray-500'" @click="autoRefresh = !autoRefresh">
            {{ autoRefresh ? '● 自动' : '○ 手动' }}
          </button>
          <button class="web3-btn text-xs !px-4 !py-2" @click="openModal()">+ 新建策略</button>
        </div>
      </div>

      <!-- Market Stats -->
      <div class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-8 gap-3 mb-5">
        <div class="glass-panel p-3" v-for="s in marketStats" :key="s.label">
          <div class="text-lg font-bold" :class="s.color">{{ s.value }}</div>
          <div class="text-[10px] text-gray-500 uppercase tracking-wider">{{ s.label }}</div>
        </div>
      </div>

      <!-- Tab Navigation -->
      <div class="flex items-center gap-1 mb-4 overflow-x-auto pb-1">
        <button v-for="tab in tabs" :key="tab.key" class="text-xs px-3 py-1.5 rounded-t whitespace-nowrap transition" :class="activeTab === tab.key ? 'bg-white/[0.06] text-white border-b-2 border-web3-accent' : 'text-gray-500 hover:text-gray-300'" @click="activeTab = tab.key">
          {{ tab.label }}
        </button>
      </div>

      <!-- Tab 1: 市场行情 -->
      <div v-show="activeTab === 'quotes'" class="glass-panel overflow-hidden">
        <div class="p-3 border-b border-white/[0.06] flex items-center justify-between">
          <h3 class="text-sm font-bold text-white">市场行情 <span class="text-[11px] text-gray-500 ml-2">共 {{ stockList.length }} 只</span></h3>
          <div class="flex items-center gap-2">
            <input v-model="stockSearch" class="web3-input !text-xs !py-1 !w-32" placeholder="搜索代码/名称" @input="filterStocks" />
            <select v-model="stockFilter" class="web3-input !text-xs !py-1 !w-24" @change="filterStocks">
              <option value="all">全部</option>
              <option value="up">上涨</option>
              <option value="down">下跌</option>
              <option value="limit_up">涨停</option>
              <option value="limit_down">跌停</option>
            </select>
          </div>
        </div>
        <div class="overflow-x-auto max-h-[600px] overflow-y-auto">
          <table class="web3-table min-w-[860px]">
            <thead class="sticky top-0 bg-[#0a0a1e]">
              <tr>
                <th>代码</th><th>名称</th><th>最新价</th><th>涨跌幅</th><th>涨跌额</th>
                <th>成交量(手)</th><th>振幅</th><th>换手率</th><th>数据源</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="stock in displayStocks" :key="stock.symbol">
                <td class="matrix-text text-xs">{{ stock.symbol }}</td>
                <td class="text-white/80">{{ stock.name }}</td>
                <td class="matrix-text text-sm font-bold">{{ stock.price }}</td>
                <td :class="getChangeClass(stock.changePercent)">{{ stock.changePercent > 0 ? '+' : '' }}{{ stock.changePercent }}%</td>
                <td :class="getChangeClass(stock.change)">{{ stock.change > 0 ? '+' : '' }}{{ stock.change }}</td>
                <td class="text-white/60 matrix-text text-xs">{{ formatVolume(stock.volume) }}</td>
                <td class="text-white/60">{{ stock.amplitude }}%</td>
                <td class="text-white/60">{{ stock.turnoverRate }}%</td>
                <td><span class="text-[10px] px-1.5 py-0.5 rounded bg-[#10102a] border border-white/[0.06] text-cyan-400/80">{{ stock.source || '--' }}</span></td>
              </tr>
              <tr v-if="!displayStocks.length && !quotesLoaded">
                <td colspan="9" class="text-center py-8"><span class="text-xs text-gray-500">加载中...</span></td>
              </tr>
              <tr v-else-if="!displayStocks.length">
                <td colspan="9" class="text-center text-gray-600 py-8">暂无行情数据</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="p-2 border-t border-white/[0.06] flex items-center justify-between text-[11px] text-gray-500" v-if="filteredStocks.length > displayLimit">
          <span>显示 {{ displayLimit }} / {{ filteredStocks.length }} 条</span>
          <button class="text-cyan-400 hover:text-cyan-300" @click="displayLimit += 50">加载更多 +50</button>
        </div>
      </div>

      <!-- Tab 2: 板块全景 -->
      <div v-show="activeTab === 'sectors'" class="space-y-4">
        <!-- Sector Heat -->
        <div class="glass-panel overflow-hidden">
          <div class="p-3 border-b border-white/[0.06]"><h3 class="text-sm font-bold text-white">板块涨跌热度 <span class="text-[11px] text-gray-500 ml-2">{{ sectorSource }}</span></h3></div>
          <div class="p-3 grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-2">
            <div v-for="(val, name) in sectorHeat" :key="name" class="p-2 rounded border text-center" :class="val >= 0 ? 'border-red-500/20 bg-red-500/[0.05]' : 'border-green-500/20 bg-green-500/[0.05]'">
              <div class="text-xs text-white/80 truncate" :title="name">{{ name }}</div>
              <div class="text-sm font-bold" :class="getChangeClass(val)">{{ val > 0 ? '+' : '' }}{{ val }}%</div>
            </div>
            <div v-if="!Object.keys(sectorHeat).length" class="col-span-full text-center text-gray-600 py-6 text-xs">暂无板块数据</div>
          </div>
        </div>
        <!-- Sector Trends -->
        <div class="glass-panel overflow-hidden">
          <div class="p-3 border-b border-white/[0.06]"><h3 class="text-sm font-bold text-white">板块涨跌走势 <span class="text-[11px] text-gray-500 ml-2">{{ sectorTrends.source }} · {{ sectorTrends.dates?.length || 0 }} 日</span></h3></div>
          <div class="p-3 overflow-x-auto">
            <div v-for="s in sectorTrends.series" :key="s.name" class="mb-3">
              <div class="flex items-center justify-between mb-1">
                <span class="text-xs text-white/80">{{ s.name }}</span>
                <span class="text-xs font-bold" :class="getChangeClass(s.today_pct)">{{ s.today_pct > 0 ? '+' : '' }}{{ s.today_pct }}%</span>
              </div>
              <div class="flex items-end gap-[2px] h-8">
                <div v-for="(p, i) in s.pcts" :key="i" class="flex-1 min-w-[3px] transition-all"
                     :style="{ height: Math.min(Math.abs(p) * 8 + 2, 32) + 'px', background: p >= 0 ? 'rgba(248,113,113,0.6)' : 'rgba(52,211,153,0.6)', alignSelf: p >= 0 ? 'flex-end' : 'flex-start' }"
                     :title="sectorTrends.dates[i] + ': ' + p + '%'">
                </div>
              </div>
            </div>
            <div v-if="!sectorTrends.series?.length" class="text-center text-gray-600 py-6 text-xs">暂无板块走势数据</div>
          </div>
        </div>
      </div>

      <!-- Tab 3: 资金流向 -->
      <div v-show="activeTab === 'fund'" class="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div class="glass-panel overflow-hidden">
          <div class="p-3 border-b border-white/[0.06]"><h3 class="text-sm font-bold text-red-400">主力资金流入 TOP 10</h3></div>
          <table class="web3-table">
            <thead><tr><th>板块</th><th>涨跌幅</th><th>主力净流入</th><th>净占比</th></tr></thead>
            <tbody>
              <tr v-for="s in fundFlow.sectors_in" :key="s.code">
                <td class="text-white/80 text-xs">{{ s.name }}</td>
                <td :class="getChangeClass(s.pct)">{{ s.pct > 0 ? '+' : '' }}{{ s.pct }}%</td>
                <td class="text-red-400 text-xs">{{ formatMoney(s.main) }}</td>
                <td class="text-red-400/60 text-xs">{{ s.main_pct }}%</td>
              </tr>
              <tr v-if="!fundFlow.sectors_in?.length"><td colspan="4" class="text-center text-gray-600 py-4 text-xs">暂无数据</td></tr>
            </tbody>
          </table>
        </div>
        <div class="glass-panel overflow-hidden">
          <div class="p-3 border-b border-white/[0.06]"><h3 class="text-sm font-bold text-green-400">主力资金流出 TOP 10</h3></div>
          <table class="web3-table">
            <thead><tr><th>板块</th><th>涨跌幅</th><th>主力净流出</th><th>净占比</th></tr></thead>
            <tbody>
              <tr v-for="s in fundFlow.sectors_out" :key="s.code">
                <td class="text-white/80 text-xs">{{ s.name }}</td>
                <td :class="getChangeClass(s.pct)">{{ s.pct > 0 ? '+' : '' }}{{ s.pct }}%</td>
                <td class="text-green-400 text-xs">{{ formatMoney(s.main) }}</td>
                <td class="text-green-400/60 text-xs">{{ s.main_pct }}%</td>
              </tr>
              <tr v-if="!fundFlow.sectors_out?.length"><td colspan="4" class="text-center text-gray-600 py-4 text-xs">暂无数据</td></tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Tab 4: 涨跌停 -->
      <div v-show="activeTab === 'limit'" class="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div class="glass-panel overflow-hidden">
          <div class="p-3 border-b border-white/[0.06]"><h3 class="text-sm font-bold text-red-400">涨停池 <span class="text-[11px] text-gray-500 ml-1">{{ limitPools.limit_up?.length || 0 }} 只</span></h3></div>
          <div class="max-h-[400px] overflow-y-auto">
            <table class="web3-table">
              <thead><tr><th>代码</th><th>名称</th><th>最新价</th><th>涨幅</th><th>连板</th></tr></thead>
              <tbody>
                <tr v-for="s in limitPools.limit_up" :key="s.code">
                  <td class="matrix-text text-xs">{{ s.code }}</td>
                  <td class="text-white/80 text-xs">{{ s.name }}</td>
                  <td class="text-red-400 text-xs">{{ s.price }}</td>
                  <td class="text-red-400">+{{ s.pct?.toFixed(2) }}%</td>
                  <td class="text-yellow-400 text-xs">{{ s.lbc }}板</td>
                </tr>
                <tr v-if="!limitPools.limit_up?.length"><td colspan="5" class="text-center text-gray-600 py-4 text-xs">今日无涨停</td></tr>
              </tbody>
            </table>
          </div>
        </div>
        <div class="glass-panel overflow-hidden">
          <div class="p-3 border-b border-white/[0.06]"><h3 class="text-sm font-bold text-green-400">跌停池 <span class="text-[11px] text-gray-500 ml-1">{{ limitPools.limit_down?.length || 0 }} 只</span></h3></div>
          <div class="max-h-[400px] overflow-y-auto">
            <table class="web3-table">
              <thead><tr><th>代码</th><th>名称</th><th>最新价</th><th>跌幅</th><th>连板</th></tr></thead>
              <tbody>
                <tr v-for="s in limitPools.limit_down" :key="s.code">
                  <td class="matrix-text text-xs">{{ s.code }}</td>
                  <td class="text-white/80 text-xs">{{ s.name }}</td>
                  <td class="text-green-400 text-xs">{{ s.price }}</td>
                  <td class="text-green-400">{{ s.pct?.toFixed(2) }}%</td>
                  <td class="text-yellow-400 text-xs">{{ s.lbc }}板</td>
                </tr>
                <tr v-if="!limitPools.limit_down?.length"><td colspan="5" class="text-center text-gray-600 py-4 text-xs">今日无跌停</td></tr>
              </tbody>
            </table>
          </div>
        </div>
        <div class="glass-panel overflow-hidden">
          <div class="p-3 border-b border-white/[0.06]"><h3 class="text-sm font-bold text-white">连板晋级</h3></div>
          <div class="p-3 flex flex-wrap gap-2">
            <span v-for="b in boardProgress" :key="b.level" class="px-3 py-1.5 rounded border border-white/[0.06] text-xs">
              <span class="text-yellow-400 font-bold">{{ b.level }}板</span>
              <span class="text-gray-500 ml-2">{{ b.count }} 只</span>
            </span>
            <span v-if="!boardProgress.length" class="text-gray-600 text-xs">暂无连板数据</span>
          </div>
        </div>
      </div>

      <!-- Tab 5: 低位荐股 -->
      <div class="glass-panel overflow-hidden" v-show="activeTab === 'recommend'">
        <div class="p-3 border-b border-white/[0.06] flex items-center justify-between">
          <h3 class="text-sm font-bold text-white">低位荐股 <span class="text-[11px] text-gray-500 ml-2">技术面+基本面+研报覆盖综合评分</span></h3>
          <span class="text-[11px] text-gray-500" v-if="recommend.generated_at">生成于 {{ recommend.generated_at }}</span>
        </div>
        <div class="overflow-x-auto">
          <table class="web3-table min-w-[960px]">
            <thead>
              <tr><th>代码</th><th>名称</th><th>最新价</th><th>涨跌幅</th><th>PE</th><th>PB</th><th>52周位置</th><th>RSI</th><th>研报</th><th>评分</th><th>推荐理由</th></tr>
            </thead>
            <tbody>
              <tr v-for="s in recommend.items" :key="s.code">
                <td class="matrix-text text-xs">{{ s.code }}</td>
                <td class="text-white/80">{{ s.name }}</td>
                <td class="matrix-text text-sm">{{ s.price }}</td>
                <td :class="getChangeClass(s.pct)">{{ s.pct > 0 ? '+' : '' }}{{ s.pct }}%</td>
                <td class="text-white/60 text-xs">{{ s.pe?.toFixed(1) }}</td>
                <td class="text-white/60 text-xs">{{ s.pb?.toFixed(2) }}</td>
                <td>
                  <div class="flex items-center gap-1">
                    <div class="w-12 h-1.5 rounded-full bg-white/10"><div class="h-full rounded-full" :class="s.pos52 < 20 ? 'bg-green-400' : s.pos52 < 40 ? 'bg-yellow-400' : 'bg-red-400'" :style="{ width: s.pos52 + '%' }"></div></div>
                    <span class="text-xs text-gray-400">{{ s.pos52?.toFixed(0) }}%</span>
                  </div>
                </td>
                <td class="text-xs" :class="s.rsi < 30 ? 'text-green-400' : s.rsi > 70 ? 'text-red-400' : 'text-gray-400'">{{ s.rsi?.toFixed(1) }}</td>
                <td class="text-xs text-cyan-400">{{ s.report_cnt }} 篇</td>
                <td class="text-lg font-bold text-yellow-400">{{ s.score }}</td>
                <td class="text-[11px] text-gray-400 max-w-[200px]">
                  <div v-for="(r, i) in s.reasons" :key="i" v-if="r" class="truncate">• {{ r }}</div>
                </td>
              </tr>
              <tr v-if="!recommend.items?.length && !recommendLoaded">
                <td colspan="11" class="text-center py-8"><span class="text-xs text-gray-500">分析中（需拉取K线+研报数据，约10-30秒）...</span></td>
              </tr>
              <tr v-else-if="!recommend.items?.length">
                <td colspan="11" class="text-center text-gray-600 py-8 text-xs">暂无推荐</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Tab 6: 研报速递 -->
      <div class="glass-panel overflow-hidden" v-show="activeTab === 'reports'">
        <div class="p-3 border-b border-white/[0.06]"><h3 class="text-sm font-bold text-white">最新券商研报 <span class="text-[11px] text-gray-500 ml-2">东财研报中心</span></h3></div>
        <div class="max-h-[600px] overflow-y-auto">
          <table class="web3-table min-w-[960px]">
            <thead><tr><th>股票</th><th>标题</th><th>机构</th><th>分析师</th><th>评级</th><th>目标价</th><th>EPS</th><th>日期</th></tr></thead>
            <tbody>
              <tr v-for="r in reports" :key="r.report_id">
                <td class="text-xs"><div class="text-cyan-400">{{ r.stock_code }}</div><div class="text-white/60 text-[10px]">{{ r.stock_name }}</div></td>
                <td class="text-white/80 text-xs max-w-[300px]"><a :href="r.url" target="_blank" class="hover:text-cyan-300 truncate block" :title="r.title">{{ r.title }}</a></td>
                <td class="text-white/60 text-xs">{{ r.org_name }}</td>
                <td class="text-white/60 text-xs">{{ r.author }}</td>
                <td><span class="text-xs px-1.5 py-0.5 rounded" :class="r.rating?.includes('买入') ? 'bg-red-500/10 text-red-400' : r.rating?.includes('增持') ? 'bg-orange-500/10 text-orange-400' : 'bg-white/[0.04] text-gray-400'">{{ r.rating || '-' }}</span></td>
                <td class="text-yellow-400 text-xs">{{ r.target_price || '-' }}</td>
                <td class="text-white/60 text-xs">{{ r.eps_y1 || '-' }}/{{ r.eps_y2 || '-' }}</td>
                <td class="text-gray-500 text-xs matrix-text">{{ r.publish_date }}</td>
              </tr>
              <tr v-if="!reports.length && !reportsLoaded"><td colspan="8" class="text-center py-8 text-xs text-gray-500">加载中...</td></tr>
              <tr v-else-if="!reports.length"><td colspan="8" class="text-center py-8 text-xs text-gray-600">暂无研报</td></tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Tab 7: 财经新闻 -->
      <div class="glass-panel overflow-hidden" v-show="activeTab === 'news'">
        <div class="p-3 border-b border-white/[0.06]"><h3 class="text-sm font-bold text-white">财经快讯 <span class="text-[11px] text-gray-500 ml-2">东方财富 7x24 实时</span></h3></div>
        <div class="max-h-[600px] overflow-y-auto divide-y divide-white/[0.04]">
          <div v-for="n in news" :key="n.id" class="p-3 flex items-start gap-3 hover:bg-white/[0.02]">
            <div class="flex-shrink-0">
              <span class="text-[10px] px-1.5 py-0.5 rounded" :class="n.sentiment === 'positive' ? 'bg-red-500/10 text-red-400' : n.sentiment === 'negative' ? 'bg-green-500/10 text-green-400' : 'bg-white/[0.04] text-gray-400'">
                {{ n.sentiment === 'positive' ? '利好' : n.sentiment === 'negative' ? '利空' : '中性' }}
              </span>
            </div>
            <div class="flex-1 min-w-0">
              <div class="text-sm text-white/80 mb-1" v-if="n.url"><a :href="n.url" target="_blank" class="hover:text-cyan-300">{{ n.title }}</a></div>
              <div class="text-sm text-white/80 mb-1" v-else>{{ n.title }}</div>
              <div class="text-[11px] text-gray-500" v-if="n.summary">{{ n.summary }}</div>
              <div class="text-[10px] text-gray-600 mt-1">{{ n.source }} · {{ n.timestamp }}</div>
            </div>
          </div>
          <div v-if="!news.length && !newsLoaded" class="text-center py-8 text-xs text-gray-500">加载中...</div>
          <div v-else-if="!news.length" class="text-center py-8 text-xs text-gray-600">暂无新闻</div>
        </div>
      </div>

      <!-- Tab 8: 量化策略 -->
      <div class="glass-panel overflow-hidden" v-show="activeTab === 'strategy'">
        <div class="p-3 border-b border-white/[0.06]"><h3 class="text-sm font-bold text-white">量化策略</h3></div>
        <div class="overflow-x-auto">
          <table class="web3-table min-w-[860px]">
            <thead><tr><th>ID</th><th>策略</th><th>状态</th><th>收益率</th><th>风险</th><th>创建时间</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="s in strategies" :key="s.id">
                <td class="matrix-text text-xs">#{{ s.id }}</td>
                <td>
                  <div class="text-white/80">{{ s.name }}</div>
                  <div class="text-gray-600 text-[11px] max-w-[180px] truncate" v-if="s.description" :title="s.description">{{ s.description }}</div>
                </td>
                <td><span :class="s.status === 'ACTIVE' ? 'web3-badge-green' : 'web3-badge-orange'">{{ s.status || '-' }}</span></td>
                <td :class="(s.returns || 0) >= 0 ? 'text-green-400' : 'text-red-400'">{{ s.returns ?? '-' }}%</td>
                <td class="text-white/60">{{ s.riskLevel || '-' }}</td>
                <td class="text-gray-500 text-xs matrix-text">{{ formatDate(s.createdAt) }}</td>
                <td>
                  <div class="flex items-center gap-2">
                    <button class="text-cyan-400 hover:text-cyan-300 text-[11px]" @click="handleRun(s)">▶ 运行</button>
                    <button class="text-purple-400 hover:text-purple-300 text-[11px]" @click="openModal(s)">编辑</button>
                    <button class="text-red-400 hover:text-red-300 text-[11px]" @click="removeStrategy(s)">删除</button>
                  </div>
                </td>
              </tr>
              <tr v-if="!strategies.length && strategiesLoaded"><td colspan="7" class="text-center text-gray-600 py-8">暂无策略，点击"+ 新建策略"创建</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Strategy Modal -->
    <div v-if="modalOpen" class="fixed inset-0 z-[100] flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black/70 backdrop-blur-sm" @click="modalOpen = false"></div>
      <div class="relative w-full max-w-lg glass-panel p-6">
        <h3 class="text-sm font-bold text-white mb-4">{{ form.id ? '编辑策略' : '新建策略' }}</h3>
        <div class="space-y-4">
          <div>
            <label class="text-[11px] text-gray-500 uppercase tracking-wider block mb-1.5">策略名称 *</label>
            <input v-model="form.name" class="web3-input" placeholder="如：双均线趋势跟随" />
          </div>
          <div>
            <label class="text-[11px] text-gray-500 uppercase tracking-wider block mb-1.5">风险等级</label>
            <input v-model="form.riskLevel" class="web3-input" placeholder="LOW / MEDIUM / HIGH" />
          </div>
          <div>
            <label class="text-[11px] text-gray-500 uppercase tracking-wider block mb-1.5">策略描述</label>
            <textarea v-model="form.description" rows="2" class="web3-input" placeholder="策略逻辑简介"></textarea>
          </div>
          <div>
            <label class="text-[11px] text-gray-500 uppercase tracking-wider block mb-1.5">策略代码</label>
            <textarea v-model="form.code" rows="4" class="web3-input font-mono text-xs" placeholder="import numpy as np..."></textarea>
          </div>
        </div>
        <div class="flex justify-end gap-3 mt-6">
          <button class="web3-btn-outline text-xs !py-2" @click="modalOpen = false">取消</button>
          <button class="web3-btn text-xs !py-2" :disabled="saving" @click="saveStrategy">{{ saving ? '保存中...' : '保存' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// ====================================================
// 量化看板：市场行情 + 板块全景 + 资金流向 + 涨跌停 +
// 低位荐股 + 研报速递 + 财经新闻 + 量化策略
// 数据源：quant-py-service(9006) + quant-service(8086)
// ====================================================
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useToastStore } from '@/stores/modules/toast'
import {
  getPyQuotes, getPySectors, getPySectorTrends, getPyFundFlow,
  getPyLimitPools, getPyBoardProgress, getPyRecommend,
  getPyReportsLatest, getPyNews,
  getQuantList, createStrategy, updateStrategy, deleteStrategy, runStrategy
} from '@/api/quant'
import { formatDateTimeMinute } from '@/utils/date'

const toast = useToastStore()

// ---- 状态 ----
const activeTab = ref('quotes')
const tabs = [
  { label: '市场行情', key: 'quotes' },
  { label: '板块全景', key: 'sectors' },
  { label: '资金流向', key: 'fund' },
  { label: '涨跌停', key: 'limit' },
  { label: '低位荐股', key: 'recommend' },
  { label: '研报速递', key: 'reports' },
  { label: '财经新闻', key: 'news' },
  { label: '量化策略', key: 'strategy' },
]
const refreshing = ref(false)
const autoRefresh = ref(true)
const lastRefresh = ref('')
const modalOpen = ref(false)
const saving = ref(false)
const form = ref({})

// 行情
const stockList = ref([])
const filteredStocks = ref([])
const displayStocks = ref([])
const displayLimit = ref(50)
const stockSearch = ref('')
const stockFilter = ref('all')
const quotesLoaded = ref(false)

// 板块
const sectorHeat = ref({})
const sectorSource = ref('')
const sectorTrends = ref({ series: [], dates: [], source: '' })

// 资金流
const fundFlow = ref({})

// 涨跌停
const limitPools = ref({})
const boardProgress = ref([])

// 低位荐股
const recommend = ref({})
const recommendLoaded = ref(false)

// 研报
const reports = ref([])
const reportsLoaded = ref(false)

// 新闻
const news = ref([])
const newsLoaded = ref(false)

// 策略
const strategies = ref([])
const strategiesLoaded = ref(false)

// 统计
const marketStats = ref([
  { label: '总股票', value: '-', color: 'text-white' },
  { label: '上涨', value: '-', color: 'text-red-400' },
  { label: '下跌', value: '-', color: 'text-green-400' },
  { label: '涨停', value: '-', color: 'text-red-500' },
  { label: '跌停', value: '-', color: 'text-green-500' },
  { label: '平均涨幅', value: '-', color: 'text-white' },
  { label: '活跃策略', value: '-', color: 'text-cyan-400' },
  { label: '平均收益', value: '-', color: 'text-yellow-400' },
])

let refreshTimer = null

// ---- 工具函数 ----
// 日期格式化（统一走 utils/date）
function formatDate(d) { return formatDateTimeMinute(d, '-') }
function getChangeClass(val) {
  if (val > 0) return 'text-red-400'
  if (val < 0) return 'text-green-400'
  return 'text-gray-400'
}
function formatVolume(v) {
  if (!v) return '-'
  if (v > 100000) return (v / 10000).toFixed(1) + '万'
  return v.toString()
}
function formatMoney(v) {
  if (!v && v !== 0) return '-'
  const abs = Math.abs(v)
  if (abs >= 1e8) return (v / 1e8).toFixed(2) + '亿'
  if (abs >= 1e4) return (v / 1e4).toFixed(2) + '万'
  return v.toFixed(0)
}
function getChangePercent(q) {
  const v = q.change_percent
  return typeof v === 'number' ? v : parseFloat(q.changePercent || 0)
}
function getPrice(q) { return typeof q.price === 'number' ? q.price : parseFloat(q.price || 0) }
function getVolume(q) {
  const v = q.volume
  return typeof v === 'number' ? v : parseFloat(q.volume || 0)
}

// ---- 数据加载 ----
async function loadQuotes() {
  try {
    const res = await getPyQuotes(0, 0)
    const quotes = res?.data
    if (Array.isArray(quotes) && quotes.length) {
      stockList.value = quotes.map(q => ({
        symbol: q.symbol,
        name: q.name,
        price: getPrice(q),
        change: q.change,
        changePercent: getChangePercent(q),
        volume: getVolume(q),
        amplitude: q.amplitude,
        turnoverRate: q.turnover_rate,
        source: q.source || 'py',
      }))
      // 统计
      const total = quotes.length
      const up = quotes.filter(q => getChangePercent(q) > 0).length
      const down = quotes.filter(q => getChangePercent(q) < 0).length
      const limitUp = quotes.filter(q => getChangePercent(q) >= 9.5).length
      const limitDown = quotes.filter(q => getChangePercent(q) <= -9.5).length
      const avg = quotes.reduce((a, q) => a + getChangePercent(q), 0) / total
      marketStats.value[0].value = total + ' 只'
      marketStats.value[1].value = up + ' 只'
      marketStats.value[2].value = down + ' 只'
      marketStats.value[3].value = limitUp + ' 只'
      marketStats.value[4].value = limitDown + ' 只'
      marketStats.value[5].value = (avg > 0 ? '+' : '') + avg.toFixed(2) + '%'
      marketStats.value[5].color = avg >= 0 ? 'text-red-400' : 'text-green-400'
      filterStocks()
    }
  } catch (e) {
    console.error('[quant] loadQuotes failed:', e)
  } finally {
    quotesLoaded.value = true
  }
}

function filterStocks() {
  let list = stockList.value
  if (stockSearch.value) {
    const q = stockSearch.value.toLowerCase()
    list = list.filter(s => s.symbol?.toLowerCase().includes(q) || s.name?.toLowerCase().includes(q))
  }
  if (stockFilter.value === 'up') list = list.filter(s => s.changePercent > 0)
  else if (stockFilter.value === 'down') list = list.filter(s => s.changePercent < 0)
  else if (stockFilter.value === 'limit_up') list = list.filter(s => s.changePercent >= 9.5)
  else if (stockFilter.value === 'limit_down') list = list.filter(s => s.changePercent <= -9.5)
  filteredStocks.value = list
  displayStocks.value = list.slice(0, displayLimit.value)
}

watch(displayLimit, () => { displayStocks.value = filteredStocks.value.slice(0, displayLimit.value) })

async function loadSectors() {
  try {
    const res = await getPySectors()
    if (res?.data && typeof res.data === 'object') {
      sectorHeat.value = res.data
      sectorSource.value = res.source || ''
    }
  } catch (e) { console.error('[quant] loadSectors:', e) }
}

async function loadSectorTrends() {
  try {
    const res = await getPySectorTrends(30, 10)
    if (res?.data) sectorTrends.value = res.data
  } catch (e) { console.error('[quant] loadSectorTrends:', e) }
}

async function loadFundFlow() {
  try {
    const res = await getPyFundFlow()
    if (res?.data) fundFlow.value = res.data
  } catch (e) { console.error('[quant] loadFundFlow:', e) }
}

async function loadLimitPools() {
  try {
    const res = await getPyLimitPools()
    if (res?.data) limitPools.value = res.data
  } catch (e) { console.error('[quant] loadLimitPools:', e) }
}

async function loadBoardProgress() {
  try {
    const res = await getPyBoardProgress()
    if (res?.data) boardProgress.value = res.data
  } catch (e) { console.error('[quant] loadBoardProgress:', e) }
}

async function loadRecommend() {
  try {
    const res = await getPyRecommend(10, false)
    if (res?.data) recommend.value = res.data
  } catch (e) { console.error('[quant] loadRecommend:', e) }
  finally { recommendLoaded.value = true }
}

async function loadReports() {
  try {
    const res = await getPyReportsLatest(20)
    if (res?.data) reports.value = res.data
  } catch (e) { console.error('[quant] loadReports:', e) }
  finally { reportsLoaded.value = true }
}

async function loadNews() {
  try {
    const res = await getPyNews(30)
    if (res?.data) news.value = res.data
  } catch (e) { console.error('[quant] loadNews:', e) }
  finally { newsLoaded.value = true }
}

async function loadStrategies() {
  try {
    const res = await getQuantList({ page: 1, size: 100 })
    if (res?.data?.records) {
      strategies.value = res.data.records
      const active = strategies.value.filter(s => s.status === 'ACTIVE').length
      marketStats.value[6].value = active + '/' + strategies.value.length
      const avgRet = strategies.value.length
        ? strategies.value.reduce((a, s) => a + (s.returns || 0), 0) / strategies.value.length
        : 0
      marketStats.value[7].value = avgRet.toFixed(1) + '%'
    }
  } catch (e) { console.error('[quant] loadStrategies:', e) }
  finally { strategiesLoaded.value = true }
}

// ---- 刷新全部 ----
async function refreshAll() {
  refreshing.value = true
  lastRefresh.value = new Date().toLocaleTimeString('zh-CN')
  // 行情和统计每次都刷
  const fast = [loadQuotes(), loadStrategies()]
  // 根据当前 Tab 加载对应数据
  const tab = activeTab.value
  const slow = []
  if (tab === 'sectors') slow.push(loadSectors(), loadSectorTrends())
  if (tab === 'fund') slow.push(loadFundFlow())
  if (tab === 'limit') slow.push(loadLimitPools(), loadBoardProgress())
  if (tab === 'recommend') slow.push(loadRecommend())
  if (tab === 'reports') slow.push(loadReports())
  if (tab === 'news') slow.push(loadNews())
  await Promise.allSettled([...fast, ...slow])
  refreshing.value = false
}

// Tab 切换时加载对应数据
watch(activeTab, (tab) => {
  if (tab === 'sectors' && !Object.keys(sectorHeat.value).length) { loadSectors(); loadSectorTrends() }
  if (tab === 'fund' && !fundFlow.value.sectors_in) loadFundFlow()
  if (tab === 'limit' && !limitPools.value.limit_up) { loadLimitPools(); loadBoardProgress() }
  if (tab === 'recommend' && !recommend.value.items) loadRecommend()
  if (tab === 'reports' && !reports.value.length) loadReports()
  if (tab === 'news' && !news.value.length) loadNews()
  if (tab === 'strategy' && !strategies.value.length) loadStrategies()
})

// ---- 策略 CRUD ----
function openModal(strategy) {
  form.value = strategy ? { id: strategy.id, name: strategy.name, riskLevel: strategy.riskLevel, description: strategy.description, code: strategy.code } : {}
  modalOpen.value = true
}

async function saveStrategy() {
  if (!form.value.name?.trim()) { toast.warning('请填写策略名称'); return }
  saving.value = true
  try {
    if (form.value.id) { await updateStrategy(form.value.id, form.value); toast.success('策略已更新') }
    else { await createStrategy({ ...form.value, status: 'DRAFT' }); toast.success('策略已创建') }
    modalOpen.value = false
    await loadStrategies()
  } catch (e) { toast.error(e?.message || '保存失败') }
  finally { saving.value = false }
}

async function removeStrategy(s) {
  try { await deleteStrategy(s.id); toast.success('策略「' + s.name + '」已删除'); await loadStrategies() }
  catch (e) { toast.error(e?.message || '删除失败') }
}

async function handleRun(s) {
  try { await runStrategy(s.id); toast.success('策略「' + s.name + '」已开始运行'); await loadStrategies() }
  catch (e) { toast.error(e?.message || '运行失败') }
}

// ---- 生命周期 ----
onMounted(async () => {
  await refreshAll()
  // 自动刷新：行情 30s，其他 5min
  refreshTimer = setInterval(() => {
    if (autoRefresh.value) {
      loadQuotes()
      loadStrategies()
      const tab = activeTab.value
      if (tab === 'news') loadNews()
      if (tab === 'sectors') loadSectorTrends()
    }
  }, 30000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>
