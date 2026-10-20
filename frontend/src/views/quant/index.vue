<template>
  <div class="quant-terminal min-h-screen px-4 md:px-6 py-8">
    <div class="w-full">
      <!-- Header -->
      <div class="flex flex-wrap items-center justify-between gap-3 mb-6">
        <div class="min-w-0">
          <h1 class="text-2xl font-bold text-amber-300 scroll-title">Web3 量化交易终端</h1>
          <p class="text-xs text-amber-600/70 mt-1 font-serif">A股行情 · 策略管理 · 回测分析 · 市场情绪 · 资金流向 · AI投研</p>
        </div>
        <div class="flex items-center gap-3">
          <span class="text-[10px] text-amber-600/50 font-serif">{{ lastUpdate || '--' }}</span>
          <button @click="refreshAll" class="text-[10px] px-3 py-1.5 rounded-lg bg-[#332314] text-amber-400 border border-amber-400/20 hover:bg-amber-500/20 transition">
            刷新数据
          </button>
        </div>
      </div>

      <!-- Tab Navigation -->
      <div class="scroll-tabs flex gap-1 mb-6 overflow-x-auto scrollbar-hide border-b border-amber-800/30">
        <button v-for="tab in tabs" :key="tab.key"
          @click="onTabClick(tab)"
          :class="['px-4 py-2 text-xs font-medium transition-all border-b-2 whitespace-nowrap flex-shrink-0 font-serif',
            activeTab === tab.key
              ? 'text-amber-300 border-amber-400 bg-amber-500/5'
              : 'text-amber-700 border-transparent hover:text-amber-400 hover:bg-[#1a1208]']">
          {{ tab.label }}
          <span v-if="tab.badge" class="ml-1 text-[9px] px-1 py-0.5 rounded bg-cyan-500/20 text-cyan-300 align-middle">新</span>
        </button>
        <div class="hidden md:block border-b border-amber-800/20 flex-1"></div>
      </div>

      <!-- ========== TAB: 市场概览（已抽取为独立组件 MarketOverviewTab） ========== -->
      <MarketOverviewTab v-if="activeTab === 'overview'"
        :index-cards="indexCards"
        :global-indices="globalIndices"
        :market-emotion="marketEmotion"
        :ths-sectors="thsSectors"
        :ths-sector-source="thsSectorSource"
        :hot-concepts="hotConcepts"
        :industry-list="industryList"
        @refresh-sectors="loadSectors" />

      <!-- ========== TAB: A股行情（已抽取为独立组件 StockQuotesTab） ========== -->
      <StockQuotesTab v-if="activeTab === 'stocks'"
        :stock-list="stockList"
        :stock-total="stockTotal"
        :last-update="lastUpdate"
        @search="fetchStockQuote"
        @select-stock="selectStock" />

      <!-- ========== TAB: 策略管理 ========== -->
      <div v-if="activeTab === 'strategies'">
        <!-- Create / Screen Buttons -->
        <div class="mb-5 flex flex-wrap items-center gap-3">
          <button class="web3-btn text-xs !px-5 !py-2.5" @click="showCreateForm = true">
            + 创建策略
          </button>
          <button class="text-xs !px-5 !py-2.5 rounded-xl bg-[#332314] text-amber-400 border border-amber-400/20 hover:bg-amber-500/20 transition" @click="openScreen">
            🎯 策略选股
          </button>
          <button class="text-xs !px-5 !py-2.5 rounded-xl bg-[#0e2230] text-cyan-300 border border-cyan-400/25 hover:bg-cyan-500/15 transition" @click="openDashboard">
            📊 监控大屏
          </button>
          <span v-if="screenLastResult" class="text-[10px] text-amber-600/40">
            上次筛选：{{ screenLastResult.strategy_name || '自定义' }} · {{ (screenLastResult.items || []).length }} 只 · {{ screenLastResult.generated_at }}
          </span>
        </div>

        <!-- Create Strategy Form -->
        <div v-if="showCreateForm" class="scroll-card p-6 mb-6">
          <h3 class="text-sm font-bold text-amber-100 mb-4">新建量化策略</h3>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label class="text-xs text-amber-500/70 mb-1 block">策略名称 <span class="text-red-400">*</span></label>
              <input v-model="newStrategy.name" class="web3-input text-sm" placeholder="如: 小市值动量策略" />
            </div>
            <div>
              <label class="text-xs text-amber-500/70 mb-1 block">风险等级</label>
              <select v-model="newStrategy.riskLevel" class="web3-input text-sm">
                <option value="LOW">低风险</option>
                <option value="MEDIUM">中风险</option>
                <option value="HIGH">高风险</option>
              </select>
            </div>
            <div class="md:col-span-2">
              <label class="text-xs text-amber-500/70 mb-1 block">策略描述</label>
              <textarea v-model="newStrategy.description" class="web3-input text-sm !min-h-[60px] resize-none" placeholder="描述策略逻辑..."></textarea>
            </div>
            <div class="md:col-span-2">
              <label class="text-xs text-amber-500/70 mb-1 block">策略代码 (Python)</label>
              <textarea v-model="newStrategy.code" class="web3-input text-sm !min-h-[140px] font-mono" placeholder='# 回测策略代码（可选约定 handle(data) -> "BUY"/"SELL"/"HOLD"）&#10;def handle(data):&#10;    # data: code/date/close/open/high/low/volume/ma5/ma20/ma60/rsi/dif/dea/boll_low/boll_up&#10;    if data["ma5"] > data["ma20"] and data["rsi"] < 60:&#10;        return "BUY"&#10;    if data["ma5"] < data["ma20"]:&#10;        return "SELL"&#10;    return "HOLD"'></textarea>
            </div>
            <div class="md:col-span-2 flex items-center justify-between">
              <span v-if="createError" class="text-xs text-red-400">{{ createError }}</span>
              <span v-else></span>
              <div class="flex gap-2">
                <button class="text-xs text-amber-500/70 hover:text-amber-100 px-3" @click="showCreateForm = false">取消</button>
                <button class="web3-btn text-xs !px-5" :disabled="!canCreateStrategy || creating" @click="createNewStrategy">
                  {{ creating ? '创建中...' : '创建策略' }}
                </button>
              </div>
            </div>
          </div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          <div v-for="item in strategyList" :key="item.id"
            class="scroll-card p-5 group cursor-pointer hover:border-purple-400/30 transition-all duration-300"
            @click="showStrategyDetail(item)">
            <div class="flex items-center gap-2 mb-3">
              <span :class="['text-[10px] px-2 py-0.5 rounded-full font-medium',
                item.riskLevel === 'LOW' ? 'bg-green-500/15 text-green-400 border border-green-400/20' :
                item.riskLevel === 'HIGH' ? 'bg-red-500/15 text-red-400 border border-red-400/20' :
                'bg-yellow-500/15 text-yellow-400 border border-yellow-400/20']">
                {{ item.riskLevel === 'LOW' ? '低风险' : item.riskLevel === 'HIGH' ? '高风险' : '中风险' }}
              </span>
              <span class="text-[10px] text-amber-700/50">{{ item.status }}</span>
            </div>
            <h3 class="font-semibold text-amber-100 group-hover:text-amber-300 transition text-sm">{{ item.name }}</h3>
            <p class="mt-2 text-xs text-amber-500/70 line-clamp-3">{{ item.description }}</p>
            <div class="mt-3 flex items-center justify-between">
              <span v-if="item.returns" class="text-xs font-mono" :class="parseFloat(item.returns) > 0 ? 'text-green-400' : 'text-red-400'">
                {{ parseFloat(item.returns) > 0 ? '+' : '' }}{{ item.returns }}%
              </span>
              <div class="flex gap-2">
                <button v-if="canOperateStrategy(item)" class="text-[11px] px-2.5 py-1 rounded-lg bg-[#332314] text-amber-400 border border-amber-400/20 hover:bg-amber-500/20 transition" @click.stop="handleRunStrategy(item.id)">运行</button>
                <span v-else class="text-[11px] px-2.5 py-1 rounded-lg bg-[#1a1408] text-amber-700/50 border border-amber-400/10 cursor-not-allowed" :title="runBlockReason(item)">🔒 不可运行</span>
                <span class="text-[11px] text-purple-400 opacity-0 group-hover:opacity-100 transition flex items-center gap-1">
                  详情 <span>→</span>
                </span>
              </div>
            </div>
          </div>
        </div>
        <div v-if="!strategyList.length" class="py-10 text-center">
          <Loading v-if="!strategyListError" />
          <div v-else class="text-xs text-red-400/80">
            <p>⚠ 策略加载失败</p>
            <p class="mt-1 text-[10px] text-amber-600/50">{{ strategyListError }}</p>
            <button class="mt-3 text-[10px] px-3 py-1 rounded-lg border border-amber-800/30 text-amber-500/70 hover:text-amber-100 transition" @click="fetchStrategies">重试</button>
          </div>
        </div>
        <div class="mt-6 flex items-center gap-4" v-if="strategyTotal > 0">
          <Pagination v-model:page="strategyPage" :page-size="strategySize" :total="strategyTotal" @update:page="fetchStrategies" />
          <div class="flex items-center gap-2 text-xs text-amber-600/60">
            <span>每页</span>
            <select v-model="strategySize" class="bg-[#2a1810] border border-amber-500/20 rounded-lg px-2 py-1 text-amber-200 text-xs outline-none cursor-pointer" @change="onStrategySizeChange">
              <option :value="9">9</option>
              <option :value="15">15</option>
              <option :value="20">20</option>
              <option :value="50">50</option>
            </select>
            <span>条</span>
          </div>
        </div>
      </div>

      <!-- ========== 策略选股弹窗（抽出到子组件 StrategyScreenModal.vue，v-model 控制显示，v-model:lastResult 同步上次筛选） ========== -->
      <StrategyScreenModal v-model:open="screenOpen" v-model:lastResult="screenLastResult" @saved="loadStrategyPortfolio" />

      <!-- ========== TAB: 回测分析 ========== -->
      <div v-if="activeTab === 'backtest'">
        <!-- Backtest Setup -->
        <div class="scroll-card p-6 mb-5">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-sm font-bold text-amber-100">回测参数设置</h3>
            <span class="text-[10px] text-amber-600/60">数据源: Vibe-Research · 回测引擎: quant-py-service</span>
          </div>
          <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-5 gap-3">
            <div>
              <label class="text-[10px] text-amber-600/60 mb-1 block">策略</label>
              <select v-model="btForm.strategy_id" class="web3-input text-xs">
                <optgroup label="策略管理 · 我的策略">
                  <option v-if="!allStrategies.length" value="" disabled>暂无自定义策略（请先在策略管理中创建）</option>
                  <option v-for="s in allStrategies" :key="s.id" :value="String(s.id)">{{ s.name }}</option>
                </optgroup>
                <optgroup label="内置策略 (quant-py)">
                  <option v-for="m in BACKTEST_STRATEGY_META" :key="m.id" :value="m.id">{{ m.name }} ({{ m.id }})</option>
                </optgroup>
              </select>
              <p class="text-[9px] text-amber-700/40 mt-1">
                {{ selectedBtStrategy?.kind === 'user'
                  ? (selectedBtStrategy.code ? '自定义策略：代码来自「策略管理」（仅可在策略管理内增改）' : '⚠️ 该策略暂无回测代码，将按持仓持有运行')
                  : (selectedBtStrategy?.desc || '内置策略（quant-py-service 回测引擎原生支持）') }}
              </p>
            </div>
            <div>
              <label class="text-[10px] text-amber-600/60 mb-1 block">股票代码</label>
              <input v-model="btSymbols" class="web3-input text-xs font-mono" placeholder="sh600519,sz000858" />
            </div>
            <div>
              <label class="text-[10px] text-amber-600/60 mb-1 block">初始资金</label>
              <input v-model="btForm.initial_cash" type="number" class="web3-input text-xs font-mono" />
            </div>
            <div>
              <label class="text-[10px] text-amber-600/60 mb-1 block">回测天数</label>
              <input v-model="btForm.days" type="number" class="web3-input text-xs font-mono" />
            </div>
            <div>
              <label class="text-[10px] text-amber-600/60 mb-1 block">止损线</label>
              <input v-model="btForm.stop_loss" type="number" step="0.01" class="web3-input text-xs font-mono" />
            </div>
          </div>
          <div class="flex items-center justify-end gap-3 mt-4">
            <span v-if="btStatus" class="text-[10px" :class="btError ? 'text-red-400' : 'text-amber-400'">{{ btStatus }}</span>
            <button class="web3-btn text-xs !px-6" :disabled="btRunning" @click="runBacktest">
              {{ btRunning ? '回测运行中...' : '▶ 运行回测' }}
            </button>
          </div>
        </div>

        <div class="scroll-card p-6">
          <h3 class="text-sm font-bold text-amber-100 mb-4">{{ btResult?.strategy_name || '北证50 小市值策略' }} - 回测报告</h3>

          <!-- 回测标的信息：名称/现价/涨跌 -->
          <div v-if="btStocksInfo.length" class="flex flex-wrap gap-3 mb-5">
            <div v-for="s in btStocksInfo" :key="s.symbol"
              class="flex items-center gap-2.5 px-3.5 py-2.5 rounded-xl bg-[#1a1208] border border-amber-800/15">
              <div>
                <div class="text-xs font-medium text-amber-100">{{ s.name }}
                  <span class="text-[10px] text-amber-600/50 font-mono ml-1">{{ s.symbol }}</span>
                </div>
              </div>
              <div class="text-right">
                <div class="text-xs font-mono font-bold text-amber-200">{{ s.price != null ? s.price.toFixed(2) : '--' }}</div>
                <div class="text-[9px] font-mono" :class="(s.change_percent || 0) >= 0 ? 'text-red-400' : 'text-green-400'">
                  {{ s.change_percent != null ? ((s.change_percent > 0 ? '+' : '') + s.change_percent.toFixed(2) + '%') : '--' }}
                </div>
              </div>
            </div>
          </div>

          <!-- Performance Metrics：全量绩效指标（收益/风险/交易三类） -->
          <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-6 gap-4 mb-6">
            <div class="bg-gradient-to-br from-purple-500/10 to-blue-500/10 border border-amber-800/20 rounded-xl p-4 text-center">
              <div class="text-[10px] text-amber-600/60 mb-1">累计收益率</div>
              <div class="text-lg font-black text-green-400 matrix-text">{{ fmtBtPct(btMetrics.total_return) }}</div>
            </div>
            <div class="bg-gradient-to-br from-amber-500/10 to-orange-500/10 border border-amber-800/20 rounded-xl p-4 text-center">
              <div class="text-[10px] text-amber-600/60 mb-1">年化收益率</div>
              <div class="text-lg font-black text-amber-300 scroll-title">{{ fmtBtPct(btMetrics.annual_return) }}</div>
            </div>
            <div class="bg-gradient-to-br from-blue-500/10 to-indigo-500/10 border border-amber-800/20 rounded-xl p-4 text-center">
              <div class="text-[10px] text-amber-600/60 mb-1">基准收益率 <span class="text-[8px]">(上证)</span></div>
              <div class="text-lg font-black text-cyan-300 matrix-text">{{ fmtBtPct(btMetrics.benchmark_return) }}</div>
            </div>
            <div class="bg-gradient-to-br from-emerald-500/10 to-teal-500/10 border border-amber-800/20 rounded-xl p-4 text-center">
              <div class="text-[10px] text-amber-600/60 mb-1">超额收益(年化)</div>
              <div class="text-lg font-black matrix-text" :class="Number(btMetrics.excess_return) >= 0 ? 'text-green-400' : 'text-red-400'">{{ fmtBtPct(btMetrics.excess_return) }}</div>
            </div>
            <div class="bg-gradient-to-br from-red-500/10 to-orange-500/10 border border-amber-800/20 rounded-xl p-4 text-center">
              <div class="text-[10px] text-amber-600/60 mb-1">最大回撤</div>
              <div class="text-lg font-black text-red-400 matrix-text">{{ fmtBtPct(btMetrics.max_drawdown) }}</div>
            </div>
            <div class="bg-gradient-to-br from-amber-500/10 to-yellow-500/10 border border-amber-800/20 rounded-xl p-4 text-center">
              <div class="text-[10px] text-amber-600/60 mb-1">夏普比率</div>
              <div class="text-lg font-black text-amber-400 matrix-text">{{ fmtBtNum(btMetrics.sharpe_ratio) }}</div>
            </div>
            <div class="bg-gradient-to-br from-amber-500/10 to-yellow-500/10 border border-amber-800/20 rounded-xl p-4 text-center">
              <div class="text-[10px] text-amber-600/60 mb-1">索提诺比率</div>
              <div class="text-lg font-black text-amber-400 matrix-text">{{ fmtBtNum(btMetrics.sortino) }}</div>
            </div>
            <div class="bg-gradient-to-br from-amber-500/10 to-yellow-500/10 border border-amber-800/20 rounded-xl p-4 text-center">
              <div class="text-[10px] text-amber-600/60 mb-1">卡玛比率</div>
              <div class="text-lg font-black text-amber-400 matrix-text">{{ fmtBtNum(btMetrics.calmar) }}</div>
            </div>
            <div class="bg-gradient-to-br from-green-500/10 to-emerald-500/10 border border-amber-800/20 rounded-xl p-4 text-center">
              <div class="text-[10px] text-amber-600/60 mb-1">胜率</div>
              <div class="text-lg font-black text-green-400 matrix-text">{{ fmtBtPct(btMetrics.win_rate) }}</div>
            </div>
            <div class="bg-gradient-to-br from-green-500/10 to-emerald-500/10 border border-amber-800/20 rounded-xl p-4 text-center">
              <div class="text-[10px] text-amber-600/60 mb-1">盈亏比</div>
              <div class="text-lg font-black text-green-400 matrix-text">{{ fmtBtNum(btMetrics.profit_loss_ratio) }}</div>
            </div>
            <div class="bg-gradient-to-br from-cyan-500/10 to-blue-500/10 border border-amber-800/20 rounded-xl p-4 text-center">
              <div class="text-[10px] text-amber-600/60 mb-1">交易笔数</div>
              <div class="text-lg font-black text-cyan-300 matrix-text">{{ btMetrics.trade_count ?? '--' }}</div>
            </div>
          </div>

          <!-- Backtest Charts -->
          <div class="grid grid-cols-1 lg:grid-cols-2 gap-5">
            <div>
              <h4 class="text-xs text-amber-500/70 mb-2">净值曲线</h4>
              <div ref="equityChartRef" class="w-full rounded-xl bg-[#1a1208] border border-amber-800/15" style="height: 280px;"></div>
            </div>
            <div>
              <h4 class="text-xs text-amber-500/70 mb-2">回撤曲线</h4>
              <div ref="drawdownChartRef" class="w-full rounded-xl bg-[#1a1208] border border-amber-800/15" style="height: 280px;"></div>
            </div>
            <div>
              <h4 class="text-xs text-amber-500/70 mb-2">月度收益热力图</h4>
              <div ref="heatmapChartRef" class="w-full rounded-xl bg-[#1a1208] border border-amber-800/15" style="height: 280px;"></div>
            </div>
            <div>
              <h4 class="text-xs text-amber-500/70 mb-2">收益分布</h4>
              <div ref="distChartRef" class="w-full rounded-xl bg-[#1a1208] border border-amber-800/15" style="height: 280px;"></div>
            </div>
            <div class="lg:col-span-2">
              <h4 class="text-xs text-amber-500/70 mb-2">组合资产配置</h4>
              <div ref="pieChartRef" class="w-full rounded-xl bg-[#1a1208] border border-amber-800/15" style="height: 280px;"></div>
            </div>
          </div>

          <!-- Transaction Details: 回测引擎真实成交记录（BUY/SELL） -->
          <div class="mt-7">
            <div class="flex items-center justify-between mb-3">
              <h4 class="text-xs text-amber-500/70 flex items-center gap-2">
                <span class="w-2 h-2 rounded-full bg-cyan-400"></span>
                交易明细
              </h4>
              <div class="flex items-center gap-2">
                <span class="text-[10px] text-amber-600/40">{{ btTransactions.length }} 笔成交 · 持仓成本含佣金(0.03%)与滑点(0.1%)</span>
                <button v-if="btTransactions.length" @click="btTransactionsShow = !btTransactionsShow"
                  class="text-[10px] px-2.5 py-1 rounded-lg border border-amber-800/30 text-amber-500/70 hover:text-amber-100 transition">
                  {{ btTransactionsShow ? '收起明细' : '展开明细' }}
                </button>
              </div>
            </div>
            <div v-if="btTransactionsShow && btTransactions.length"
              class="rounded-xl bg-[#1a1208] border border-amber-800/15 overflow-hidden">
              <div class="overflow-x-auto">
                <table class="w-full text-xs">
                  <thead>
                    <tr class="text-left text-[10px] text-amber-600/60 border-b border-amber-800/15">
                      <th class="px-3 py-2.5 font-medium">日期</th>
                      <th class="px-3 py-2.5 font-medium">代码</th>
                      <th class="px-3 py-2.5 font-medium">方向</th>
                      <th class="px-3 py-2.5 font-medium text-right">数量</th>
                      <th class="px-3 py-2.5 font-medium text-right">成交价</th>
                      <th class="px-3 py-2.5 font-medium text-right">金额</th>
                      <th class="px-3 py-2.5 font-medium text-right">盈亏</th>
                      <th class="px-3 py-2.5 font-medium text-right">盈亏%</th>
                      <th class="px-3 py-2.5 font-medium">原因</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(t, i) in btTransactions" :key="i"
                      class="border-b border-amber-900/10 last:border-b-0 hover:bg-amber-500/5 transition">
                      <td class="px-3 py-2 font-mono text-amber-200/70">{{ t.date }}</td>
                      <td class="px-3 py-2 font-mono">{{ t.code }}</td>
                      <td class="px-3 py-2">
                        <span :class="['text-[10px] px-1.5 py-0.5 rounded-full',
                          t.action === 'BUY' ? 'bg-red-500/10 text-red-400' : 'bg-green-500/10 text-green-400']">
                          {{ t.action === 'BUY' ? '买入' : '卖出' }}
                        </span>
                      </td>
                      <td class="px-3 py-2 text-right font-mono">{{ fmtNum(t.shares) }}</td>
                      <td class="px-3 py-2 text-right font-mono">{{ Number(t.price).toFixed(2) }}</td>
                      <td class="px-3 py-2 text-right font-mono">{{ fmtNum(t.amount) }}</td>
                      <td class="px-3 py-2 text-right font-mono" :class="t.pnl != null && t.pnl !== '' && t.pnl !== undefined ? (Number(t.pnl) >= 0 ? 'text-green-400' : 'text-red-400') : 'text-amber-700/40'">
                        {{ t.pnl != null && t.pnl !== '' && t.pnl !== undefined ? (Number(t.pnl) >= 0 ? '+' : '') + Number(t.pnl).toFixed(2) : '--' }}
                      </td>
                      <td class="px-3 py-2 text-right font-mono" :class="t.pnl_pct != null && t.pnl_pct !== '' && t.pnl_pct !== undefined ? (Number(t.pnl_pct) >= 0 ? 'text-green-400' : 'text-red-400') : 'text-amber-700/40'">
                        {{ t.pnl_pct != null && t.pnl_pct !== '' && t.pnl_pct !== undefined ? (Number(t.pnl_pct) >= 0 ? '+' : '') + Number(t.pnl_pct).toFixed(2) + '%' : '--' }}
                      </td>
                      <td class="px-3 py-2 text-amber-200/60">{{ t.reason || '--' }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
            <div v-else-if="btResult && btTransactionsShow"
              class="text-xs text-amber-700/40 py-6 text-center rounded-xl bg-[#1a1208] border border-amber-800/15">
              本轮回测未产生成交（行情不足或策略无信号）
            </div>
          </div>
        </div>
      </div>

      <!-- ========== TAB: 新闻雷达 ========== -->
      <div v-if="activeTab === 'news'" class="space-y-5">
        <!-- 研报速递（全量） -->
        <div class="scroll-card overflow-hidden">
          <div class="p-4 border-b border-amber-800/20 flex items-center justify-between">
            <h3 class="text-sm font-bold text-amber-100 font-serif flex items-center gap-2">
              <span class="w-2 h-2 rounded-full bg-cyan-400"></span>
              券商研报
            </h3>
            <span class="text-[10px] text-amber-600/40">东财研报中心元数据 · 仅供研究，不构成投资建议</span>
          </div>
          <div class="p-4">
            <div v-if="newsReports.length" class="space-y-2">
              <div v-for="r in newsReports" :key="r.report_id + r.publish_date"
                class="p-3 rounded-xl bg-[#1a1208] border border-amber-800/15 hover:border-cyan-400/30 transition">
                <a :href="r.url" target="_blank" rel="noopener noreferrer"
                  class="text-xs text-amber-100/90 leading-relaxed hover:text-cyan-300">{{ r.title }}</a>
                <div class="flex flex-wrap items-center gap-3 mt-2">
                  <span class="text-[10px] text-amber-100/80">{{ r.stock_name }}</span>
                  <span :class="['text-[10px] px-1.5 py-0.5 rounded-full',
                    r.rating?.includes('买入') ? 'bg-red-500/10 text-red-400' :
                    r.rating?.includes('增持') ? 'bg-orange-500/10 text-orange-400' :
                    r.rating?.includes('持有') ? 'bg-yellow-500/10 text-yellow-400' :
                    'bg-[#a855f7]/15 text-[#c084fc]']">{{ r.rating || '未评级' }}</span>
                  <span class="text-[10px] text-amber-600/40">{{ r.org_name }}</span>
                  <span v-if="r.author" class="text-[10px] text-amber-600/40">{{ r.author }}</span>
                  <span class="text-[10px] text-amber-600/40">{{ r.publish_date }}</span>
                  <span v-if="r.target_price" class="text-[10px] text-cyan-300">目标价 {{ r.target_price }}</span>
                  <span v-if="r.eps_y1" class="text-[10px] text-green-400">EPS {{ r.eps_y1 }}</span>
                </div>
              </div>
            </div>
            <div v-else class="text-xs text-amber-700/40 py-8 text-center">暂无研报数据</div>
          </div>
        </div>

        <!-- 行业研报 -->
        <div v-if="newsIndustryReports.length" class="scroll-card overflow-hidden">
          <div class="p-4 border-b border-amber-800/20">
            <h3 class="text-sm font-bold text-amber-100 font-serif flex items-center gap-2">
              <span class="w-2 h-2 rounded-full bg-cyan-400"></span>
              行业研报
            </h3>
          </div>
          <div class="p-4 space-y-2">
            <div v-for="r in newsIndustryReports" :key="r.report_id + r.publish_date"
              class="p-3 rounded-xl bg-[#1a1208] border border-amber-800/15 hover:border-cyan-400/30 transition">
              <a :href="r.url" target="_blank" rel="noopener noreferrer"
                class="text-xs text-amber-100/90 leading-relaxed hover:text-cyan-300">{{ r.title }}</a>
              <div class="flex flex-wrap items-center gap-3 mt-2">
                <span :class="['text-[10px] px-1.5 py-0.5 rounded-full',
                    r.rating?.includes('买入') ? 'bg-red-500/10 text-red-400' :
                    r.rating?.includes('增持') ? 'bg-orange-500/10 text-orange-400' :
                    r.rating?.includes('持有') ? 'bg-yellow-500/10 text-yellow-400' :
                    'bg-[#a855f7]/15 text-[#c084fc]']">{{ r.rating || '未评级' }}</span>
                <span class="text-[10px] text-amber-600/40">{{ r.org_name }}</span>
                <span class="text-[10px] text-amber-600/40">{{ r.publish_date }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 投资资讯 -->
        <div class="scroll-card overflow-hidden">
          <div class="p-4 border-b border-amber-800/20">
            <h3 class="text-sm font-bold text-amber-100">投资资讯雷达</h3>
          </div>
          <div class="p-4">
            <div v-if="newsItems.length" class="space-y-3">
              <div v-for="n in newsItems" :key="n.title"
                class="p-4 rounded-xl bg-[#1a1208] border border-amber-800/15 hover:border-amber-700/25 transition cursor-pointer">
                <div class="flex items-start justify-between gap-3">
                  <div class="flex-1">
                    <h4 class="text-xs font-medium text-amber-100/90 leading-relaxed">{{ n.title }}</h4>
                    <div class="flex items-center gap-3 mt-2">
                      <span class="text-[10px] text-amber-700/50">{{ n.source }}</span>
                      <span class="text-[10px] text-amber-700/50">{{ n.time }}</span>
                    </div>
                  </div>
                  <span v-if="n.sentiment" :class="['text-[10px] px-2 py-0.5 rounded-full shrink-0',
                    n.sentiment === 'positive' ? 'bg-red-500/10 text-red-400' :
                    n.sentiment === 'negative' ? 'bg-green-500/10 text-green-400' :
                    'bg-[#332314] text-amber-500/70']">
                    {{ n.sentiment === 'positive' ? '利好' : n.sentiment === 'negative' ? '利空' : '中性' }}
                  </span>
                </div>
              </div>
            </div>
            <div v-else class="text-xs text-amber-700/50 py-8 text-center">暂无新闻数据</div>
          </div>
        </div>
      </div>

      <!-- ========== TAB: 组合管理 ========== -->
      <div v-if="activeTab === 'portfolio'">
        <!-- 组合策略：多策略选股合并 -->
        <div class="scroll-card p-6 mb-6">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-sm font-bold text-amber-100 flex items-center gap-2"><span>🎯</span> 组合策略</h3>
            <div class="flex items-center gap-3">
              <span class="text-[10px] text-amber-600/50">{{ strategyPortfolio.length }} 个策略 · 选中 {{ selectedStrategyIds.length }} · 合并 {{ combinedStocks.length }} 只</span>
              <button v-if="strategyPortfolio.length" class="text-[10px] px-2 py-1 rounded-lg border border-red-500/20 text-red-400/70 hover:text-red-300 transition" @click="clearStrategyPortfolio">清空</button>
            </div>
          </div>

          <div v-if="strategyPortfolio.length" class="space-y-2 mb-4">
            <label v-for="sp in strategyPortfolio" :key="sp.id"
              class="flex items-center gap-3 p-3 rounded-xl bg-[#1a1208] border border-amber-800/15 cursor-pointer hover:border-amber-500/30 transition">
              <input type="checkbox" :value="sp.id" v-model="selectedStrategyIds"
                class="w-4 h-4 accent-cyan-400 cursor-pointer" />
              <div class="flex-1">
                <span class="text-xs font-medium text-amber-100">{{ sp.strategy_name }}</span>
                <span class="text-[10px] text-amber-600/50 ml-2">{{ sp.items.length }} 只 · 存于 {{ sp.saved_at }}</span>
              </div>
              <button class="text-[10px] text-red-500/50 hover:text-red-400 transition" @click.stop="removeStrategyFromPortfolio(sp.id)">删除</button>
            </label>
          </div>
          <div v-else class="py-6 text-center">
            <p class="text-xs text-amber-700/50 mb-2">暂无组合策略</p>
            <p class="text-[10px] text-amber-700/40">在「策略管理」中执行策略选股后，点击「📥 存入组合策略」即可在此查看</p>
          </div>

          <!-- 合并后的股票列表 -->
          <div v-if="combinedStocks.length" class="rounded-xl bg-[#0b1422] border border-cyan-500/15 overflow-hidden">
            <div class="px-4 py-2.5 border-b border-cyan-500/10 flex items-center gap-2">
              <span class="text-xs font-bold text-cyan-300">合并选股结果</span>
              <span class="text-[10px] text-cyan-500/50">{{ combinedStocks.length }} 只标的 · 来自 {{ selectedStrategyIds.length }} 个策略</span>
            </div>
            <div class="overflow-x-auto max-h-[400px] overflow-y-auto">
              <table class="w-full text-xs">
                <thead class="sticky top-0 bg-[#0b1422]">
                  <tr class="text-left text-[10px] text-cyan-500/60 border-b border-cyan-500/10">
                    <th class="px-3 py-2 font-medium">代码</th>
                    <th class="px-3 py-2 font-medium">名称</th>
                    <th class="px-3 py-2 font-medium text-right">现价</th>
                    <th class="px-3 py-2 font-medium text-right">涨跌%</th>
                    <th class="px-3 py-2 font-medium">入选策略</th>
                    <th class="px-3 py-2 font-medium">评分</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="s in combinedStocks" :key="s.code"
                    class="border-b border-cyan-900/10 last:border-b-0 hover:bg-cyan-500/5 transition">
                    <td class="px-3 py-2 font-mono">{{ s.code }}</td>
                    <td class="px-3 py-2 text-amber-100/90">{{ s.name }}</td>
                    <td class="px-3 py-2 text-right font-mono">{{ s.price != null ? Number(s.price).toFixed(2) : '--' }}</td>
                    <td class="px-3 py-2 text-right font-mono" :class="Number(s.pct) >= 0 ? 'text-red-400' : 'text-green-400'">
                      {{ s.pct != null ? (Number(s.pct) > 0 ? '+' : '') + Number(s.pct).toFixed(2) + '%' : '--' }}
                    </td>
                    <td class="px-3 py-2">
                      <div class="flex flex-wrap gap-1">
                        <span v-for="sn in s.strategy_names" :key="sn"
                          class="text-[9px] px-1.5 py-0.5 rounded bg-purple-500/10 text-purple-300 border border-purple-400/15 whitespace-nowrap">{{ sn }}</span>
                      </div>
                    </td>
                    <td class="px-3 py-2 text-right font-mono">
                      <span v-if="s.score" class="text-[10px] px-1.5 py-0.5 rounded-full bg-purple-500/15 text-purple-300 border border-purple-400/25">{{ s.score }}</span>
                      <span v-else class="text-amber-700/30">--</span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- 持仓概览 -->
        <div class="scroll-card p-6">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-sm font-bold text-amber-100 flex items-center gap-2"><span>📊</span> 我的持仓</h3>
            <div class="flex items-center gap-3">
              <span v-if="holdingsIsAdmin" class="text-[10px] px-2 py-0.5 rounded-full bg-red-500/10 text-red-400/70 border border-red-500/20">管理员视角 · 可见全部用户</span>
              <button class="web3-btn text-xs !px-3 !py-1.5" @click="showHoldingForm = !showHoldingForm">
                {{ showHoldingForm ? '收起' : '+ 添加持仓' }}
              </button>
              <button class="text-[10px] px-2 py-1 rounded-lg border border-amber-800/20 text-amber-500/70 hover:text-amber-100 transition" @click="loadMyHoldings">刷新</button>
            </div>
          </div>

          <!-- 添加/编辑持仓表单 -->
          <div v-if="showHoldingForm" class="mb-4 p-4 rounded-xl bg-[#1a1208] border border-amber-800/15">
            <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
              <div>
                <label class="text-[10px] text-amber-600/60 mb-1 block">股票代码 *</label>
                <input v-model="holdingForm.stock_code" class="web3-input text-xs font-mono" placeholder="如 600519" />
              </div>
              <div>
                <label class="text-[10px] text-amber-600/60 mb-1 block">股票名称</label>
                <input v-model="holdingForm.stock_name" class="web3-input text-xs" placeholder="如 贵州茅台" />
              </div>
              <div>
                <label class="text-[10px] text-amber-600/60 mb-1 block">持仓股数</label>
                <input v-model.number="holdingForm.shares" type="number" min="0" class="web3-input text-xs font-mono" placeholder="100" />
              </div>
              <div>
                <label class="text-[10px] text-amber-600/60 mb-1 block">成本价</label>
                <input v-model.number="holdingForm.cost_price" type="number" step="0.001" min="0" class="web3-input text-xs font-mono" placeholder="1688.00" />
              </div>
              <div>
                <label class="text-[10px] text-amber-600/60 mb-1 block">买入日期</label>
                <input v-model="holdingForm.buy_date" type="date" class="web3-input text-xs font-mono" />
              </div>
              <div class="col-span-2">
                <label class="text-[10px] text-amber-600/60 mb-1 block">备注</label>
                <input v-model="holdingForm.notes" class="web3-input text-xs" placeholder="可选" />
              </div>
            </div>
            <div class="flex gap-2 mt-3">
              <button class="web3-btn text-xs !px-4 !py-2" @click="submitHolding">保存</button>
              <button class="text-xs px-3 py-2 rounded-lg border border-amber-800/20 text-amber-500/70 hover:text-amber-100 transition" @click="showHoldingForm = false">取消</button>
            </div>
          </div>

          <!-- 持仓列表 -->
          <div v-if="loadingHoldings" class="flex items-center justify-center gap-2 py-6">
            <span class="h-4 w-4 rounded-full border-2 border-amber-800/25 border-t-web3-accent animate-spin"></span>
            <span class="text-xs text-amber-700/50">加载持仓...</span>
          </div>
          <div v-else-if="myHoldings.length" class="overflow-x-auto rounded-xl bg-[#1a1208] border border-amber-800/15">
            <table class="w-full text-xs">
              <thead>
                <tr class="text-left text-[10px] text-amber-600/60 border-b border-amber-800/15">
                  <th v-if="holdingsIsAdmin" class="px-3 py-2.5 font-medium">用户</th>
                  <th class="px-3 py-2.5 font-medium">代码</th>
                  <th class="px-3 py-2.5 font-medium">名称</th>
                  <th class="px-3 py-2.5 font-medium text-right">股数</th>
                  <th class="px-3 py-2.5 font-medium text-right">成本</th>
                  <th class="px-3 py-2.5 font-medium">买入日期</th>
                  <th class="px-3 py-2.5 font-medium">备注</th>
                  <th class="px-3 py-2.5 font-medium text-center">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="h in myHoldings" :key="h.id" class="border-b border-amber-900/10 last:border-b-0 hover:bg-amber-500/5 transition">
                  <td v-if="holdingsIsAdmin" class="px-3 py-2 text-amber-500/70">{{ h.username || h.user_id }}</td>
                  <td class="px-3 py-2 font-mono">{{ h.stock_code }}</td>
                  <td class="px-3 py-2 text-amber-100/90">{{ h.stock_name || '--' }}</td>
                  <td class="px-3 py-2 text-right font-mono">{{ h.shares }}</td>
                  <td class="px-3 py-2 text-right font-mono">{{ Number(h.cost_price).toFixed(3) }}</td>
                  <td class="px-3 py-2 font-mono text-amber-200/60">{{ h.buy_date || '--' }}</td>
                  <td class="px-3 py-2 text-amber-200/70 max-w-[160px] truncate">{{ h.notes || '--' }}</td>
                  <td class="px-3 py-2 text-center">
                    <button class="text-[10px] text-amber-500/70 hover:text-amber-300 transition mr-2" @click="editHolding(h)">编辑</button>
                    <button class="text-[10px] text-red-500/60 hover:text-red-400 transition" @click="removeHolding(h.id)">删除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="py-8 text-center">
            <p class="text-xs text-amber-700/50 mb-2">暂无持仓数据</p>
            <p class="text-[10px] text-amber-700/40">点击「添加持仓」录入你的股票持仓</p>
          </div>
        </div>
      </div>

      <!-- ========== TAB: AI分析（含 AI 量化助手 + AI 深度投研） ========== -->
      <div v-if="activeTab === 'ai'">
        <!-- 内部分区切换 -->
        <div class="flex items-center gap-1 mb-5 p-1 rounded-xl bg-[#1a0f08] border border-amber-800/15 w-fit">
          <button :class="['text-xs px-4 py-1.5 rounded-lg transition', aiSubTab === 'chat' ? 'bg-amber-500/20 text-amber-100 font-medium' : 'text-amber-500/60 hover:text-amber-100']" @click="aiSubTab = 'chat'">🤖 量化助手</button>
          <button :class="['text-xs px-4 py-1.5 rounded-lg transition', aiSubTab === 'deep' ? 'bg-amber-500/20 text-amber-100 font-medium' : 'text-amber-500/60 hover:text-amber-100']" @click="aiSubTab = 'deep'">🧠 深度投研</button>
        </div>

        <div v-if="aiSubTab === 'chat'" class="scroll-card p-6">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-sm font-bold text-amber-100 flex items-center gap-2">
              <span class="text-amber-400">🤖</span> AI 量化助手
            </h3>
            <button @click="showAiConfig = !showAiConfig" class="text-[10px] px-3 py-1 rounded-lg border border-amber-800/20 text-amber-500/70 hover:text-amber-100 transition">
              {{ showAiConfig ? '收起设置' : '⚙ AI 模型设置' }}
            </button>
          </div>

          <!-- AI Config Panel（抽出到子组件 AiConfigPanel.vue，v-model 控制显示，v-model:config 同步配置） -->
          <AiConfigPanel v-model="showAiConfig" v-model:config="aiConfig" />

          <!-- ChatPanel（抽出到子组件 ChatPanel.vue，内部持有聊天状态与 sendChat 逻辑，通过 v-model:config 读取 aiConfig） -->
          <ChatPanel v-model:config="aiConfig" />
        </div>

        <!-- AI 深度投研（原独立 tab，合并为 ai 模块子分区） -->
        <div v-if="aiSubTab === 'deep'" class="scroll-card p-6">
          <div class="flex items-center justify-between mb-5">
            <h3 class="text-sm font-bold text-amber-100 flex items-center gap-2">
              <span class="text-amber-400">🧠</span> AI 深度投研 · TradingAgents-Astock
            </h3>
            <span class="text-[10px] text-amber-700/50">7 AI Analysts → Bull/Bear Debate → Portfolio Manager</span>
          </div>

          <!-- Stock Input + Model Selection -->
          <div class="flex flex-wrap gap-2 mb-4">
            <input v-model="deepaiStockCode" class="web3-input flex-1 text-sm font-mono min-w-[160px]" placeholder="输入A股代码，如 600519" maxlength="10" />
            <input v-model="deepaiStockName" class="web3-input w-full sm:w-28 text-sm" placeholder="名称" />
            <button class="web3-btn text-sm" :disabled="!deepaiStockCode || deepaiRunning" @click="runDeepAnalysis">
              {{ deepaiRunning ? '分析中...' : '深度分析' }}
            </button>
          </div>

          <!-- Deep AI Model Config: 保存后锁定，可解锁再次编辑 -->
          <div class="scroll-card-sm p-4 mb-4 border border-amber-800/20">
            <div class="flex items-center justify-between mb-3">
              <p class="text-[10px] text-amber-600/60">🤖 模型接口（OpenAI 兼容 · 保存后锁定，可再次编辑）</p>
              <span v-if="!llmDefaultLoading && llmDefault" :class="['text-[10px] px-2 py-0.5 rounded-full border shrink-0',
                llmDefault.configured ? 'bg-green-500/10 text-green-400 border-green-400/20' : 'bg-amber-500/10 text-amber-500/70 border-amber-500/20']">
                {{ llmDefault.configured ? `服务端默认：${llmDefault.provider || ''} / ${llmDefault.model}` : '服务端未配置默认模型' }}
              </span>
              <span v-else-if="llmDefaultLoading" class="text-[10px] text-amber-700/40 shrink-0">检测默认模型...</span>
              <div class="flex gap-2">
                <span v-if="deepaiCfg.locked" class="text-[10px] text-green-400/80">✓ 已锁定</span>
                <button v-if="deepaiCfg.locked" @click="editDeepaiCfg" class="text-[10px] px-2.5 py-1 rounded-lg border border-amber-800/30 text-amber-500/70 hover:text-amber-100 transition" :disabled="deepaiRunning">
                  ✏️ 编辑
                </button>
              </div>
            </div>

            <template v-if="!deepaiCfg.locked">
              <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-3">
                <div>
                  <label class="text-[10px] text-amber-600/60 mb-1 block">供应商</label>
                  <select v-model="deepaiCfg.providerKey" @change="onDeepaiProviderChange" class="web3-input text-xs">
                    <option value="">自定义</option>
                    <option v-for="p in llmProviders" :key="p.key" :value="p.key">{{ p.name }}</option>
                  </select>
                </div>
                <div>
                  <label class="text-[10px] text-amber-600/60 mb-1 block">Base URL</label>
                  <input v-model="deepaiCfg.baseUrl" class="web3-input text-xs font-mono" placeholder="https://api.deepseek.com/v1" />
                </div>
                <div>
                  <label class="text-[10px] text-amber-600/60 mb-1 block">API Key</label>
                  <input v-model="deepaiCfg.apiKey" type="password" class="web3-input text-xs font-mono" placeholder="sk-..." autocomplete="off" />
                </div>
                <div>
                  <label class="text-[10px] text-amber-600/60 mb-1 block">模型</label>
                  <div class="flex gap-2">
                    <select v-if="deepaiModels.length" v-model="deepaiCfg.model" class="web3-input text-xs flex-1">
                      <option v-for="m in deepaiModels" :key="m" :value="m">{{ m }}</option>
                    </select>
                    <input v-else v-model="deepaiCfg.model" class="web3-input text-xs font-mono flex-1" placeholder="如 deepseek-chat" />
                    <button class="flex-shrink-0 text-[10px] px-2.5 py-1 rounded-lg border border-amber-800/30 text-amber-500/70 hover:text-amber-100 transition" :disabled="deepaiModelsLoading || !deepaiCfg.baseUrl.trim()" @click="loadDeepaiModels">
                      {{ deepaiModelsLoading ? '获取中...' : '获取模型' }}
                    </button>
                  </div>
                  <p v-if="deepaiModelsErr" class="text-[10px] text-red-400/80 mt-1">{{ deepaiModelsErr }}</p>
                  <p v-else-if="deepaiModels.length" class="text-[10px] text-green-400/60 mt-1">已获取 {{ deepaiModels.length }} 个模型</p>
                </div>
              </div>
              <div class="flex items-center justify-between mt-3">
                <span v-if="llmProviderErr" class="text-[10px] text-red-400/80">{{ llmProviderErr }}</span>
                <span v-else class="text-[10px] text-amber-700/40">配置保存在本地浏览器（localStorage），提交时传递给后端启用真实 LLM 分析</span>
                <div class="flex gap-2">
                  <button v-if="deepaiCfg.saved && !deepaiCfg.locked" @click="resetDeepaiCfg" class="text-[10px] px-2.5 py-1 rounded-lg border border-amber-800/30 text-amber-500/70 hover:text-amber-100 transition">重置</button>
                  <button class="text-[10px] px-2.5 py-1 rounded-lg border border-green-700/30 text-green-500/70 hover:text-green-100 transition" :disabled="deepaiRunning || !llmDefault?.configured" @click="useServerLlm">
                    使用服务端默认
                  </button>
                  <button class="text-[10px] px-3 py-1 rounded-lg bg-[#332314] text-amber-400 border border-amber-400/20 hover:bg-amber-500/20 transition" :disabled="!deepaiCfg.baseUrl || !deepaiCfg.apiKey || !deepaiCfg.model" @click="saveDeepaiCfg">
                    保存并锁定 🔒
                  </button>
                </div>
              </div>
            </template>

            <template v-else>
              <div v-if="deepaiCfg.serverDefault" class="flex flex-wrap items-center gap-x-4 gap-y-1 text-[11px] text-amber-200/70">
                <span><b class="text-amber-100">模式：</b>服务端默认模型（后端 LLM_DEFAULTS 兜底）</span>
                <span v-if="llmDefault?.configured" class="text-[10px] px-1.5 py-0.5 rounded-full bg-green-500/10 text-green-400 border border-green-400/20">
                  {{ llmDefault.provider || '' }} / {{ llmDefault.model }}
                </span>
                <span class="text-[9px] text-amber-700/50">↓ 按「深度分析」将使用服务端默认模型运行 7 位分析师，无需本地 Key</span>
              </div>
              <div v-else class="flex flex-wrap items-center gap-x-4 gap-y-1 text-[11px] text-amber-200/70">
                <span><b class="text-amber-100">供应商：</b>{{ deepaiProviderName }}</span>
                <span class="max-w-[260px] truncate"><b class="text-amber-100">Base：</b>{{ deepaiCfg.baseUrl }}</span>
                <span><b class="text-amber-100">模型：</b>{{ deepaiCfg.model }}</span>
                <span><b class="text-amber-100">Key：</b>{{ maskKey(deepaiCfg.apiKey) }}</span>
                <span v-if="deepaiCfg.model" class="text-[9px] text-amber-700/50">↓ 按「深度分析」将使用此模型运行 7 位分析师</span>
              </div>
            </template>
          </div>

          <!-- Pipeline Status -->
          <div v-if="deepaiRunning" class="mb-5">
            <div class="flex items-center gap-2 mb-3">
              <div class="w-2 h-2 rounded-full bg-amber-400 animate-pulse"></div>
              <span class="text-xs text-amber-400">{{ deepaiStep }}</span>
            </div>
            <div class="w-full h-1.5 rounded-full bg-amber-500/5 overflow-hidden">
              <div class="h-full rounded-full bg-gradient-to-r from-purple-500 to-amber-400 transition-all duration-500" :style="{ width: deepaiProgress + '%' }"></div>
            </div>
          </div>

          <!-- Analysis Report -->
          <div v-if="deepaiReport" class="space-y-4">
            <!-- Verdict Banner -->
            <div :class="['p-4 rounded-xl border', deepaiVerdict === 'BUY' ? 'bg-green-500/10 border-green-400/20' : deepaiVerdict === 'SELL' ? 'bg-red-500/10 border-red-400/20' : 'bg-yellow-500/10 border-yellow-400/20']">
              <div class="flex items-center justify-between">
                <div>
                  <span class="text-[10px] text-amber-600/60">最终结论</span>
                  <p class="text-lg font-black" :class="deepaiVerdict === 'BUY' ? 'text-green-400' : deepaiVerdict === 'SELL' ? 'text-red-400' : 'text-yellow-400'">
                    {{ deepaiVerdict === 'BUY' ? '📈 建议买入' : deepaiVerdict === 'SELL' ? '📉 建议卖出' : '⏸ 建议观望' }}
                  </p>
                </div>
                <div class="text-right">
                  <span class="text-[10px] text-amber-600/60">置信度</span>
                  <p class="text-sm font-bold text-amber-100 matrix-text">{{ deepaiConfidence }}%</p>
                </div>
              </div>
            </div>

            <!-- Portfolio Manager Decision -->
            <div v-if="deepaiPm" :class="['scroll-card-sm p-4 border-l-2', deepaiVerdict === 'BUY' ? 'border-green-400/40' : deepaiVerdict === 'SELL' ? 'border-red-400/40' : 'border-yellow-400/40']">
              <div class="flex items-center justify-between mb-2">
                <h4 class="text-xs font-bold text-amber-100">组合经理 · 最终决策</h4>
                <span class="text-[10px] px-2 py-0.5 rounded font-mono" :class="deepaiRatingMeta.cls">{{ deepaiRatingMeta.text }}</span>
              </div>
              <div class="flex flex-wrap gap-x-4 gap-y-1 text-[11px] text-amber-500/80 mb-2">
                <span v-if="deepaiPm.price_target"><span class="text-amber-700/50">目标价 </span>{{ deepaiPm.price_target }}</span>
                <span v-if="deepaiPm.time_horizon"><span class="text-amber-700/50">周期 </span>{{ deepaiPm.time_horizon }}</span>
                <span v-if="deepaiTrader && deepaiTrader.position_sizing"><span class="text-amber-700/50">建议仓位 </span>{{ deepaiTrader.position_sizing }}</span>
                <span v-if="deepaiTrader && (deepaiTrader.entry_price || deepaiTrader.stop_loss)">
                  <span class="text-amber-700/50">入场 </span>{{ deepaiTrader.entry_price ?? '--' }}
                  <span class="text-amber-700/50 ml-2">止损 </span>{{ deepaiTrader.stop_loss ?? '--' }}
                </span>
              </div>
              <p v-if="deepaiPm.executive_summary" class="text-[11px] text-amber-100/90 leading-relaxed">{{ deepaiPm.executive_summary }}</p>
              <p v-if="deepaiPm.investment_thesis" class="text-[11px] text-amber-500/60 mt-1 leading-relaxed">{{ deepaiPm.investment_thesis }}</p>
            </div>

            <!-- Investment Plan (Research Manager) -->
            <div v-if="deepaiPlan" class="scroll-card-sm p-4">
              <div class="flex items-center justify-between mb-2">
                <h4 class="text-xs font-bold text-amber-100">研究经理 · 投资计划</h4>
                <span v-if="deepaiPlan.recommendation" class="text-[10px] px-2 py-0.5 rounded font-mono bg-violet-500/15 text-violet-300">{{ deepaiPlan.recommendation }}</span>
              </div>
              <p v-if="deepaiPlan.rationale" class="text-[11px] text-amber-500/70 leading-relaxed">{{ deepaiPlan.rationale }}</p>
              <p v-if="deepaiPlan.strategic_actions" class="text-[11px] text-amber-300/80 mt-1 leading-relaxed">🎯 {{ deepaiPlan.strategic_actions }}</p>
            </div>

            <!-- Trader Proposal -->
            <div v-if="deepaiTrader" class="scroll-card-sm p-4">
              <div class="flex items-center justify-between mb-2">
                <h4 class="text-xs font-bold text-amber-100">交易员 · 交易提案</h4>
                <span class="text-[10px] px-2 py-0.5 rounded font-mono" :class="deepaiActionLabel.cls">{{ deepaiActionLabel.text }}</span>
              </div>
              <p v-if="deepaiTrader.reasoning" class="text-[11px] text-amber-500/70 leading-relaxed">{{ deepaiTrader.reasoning }}</p>
            </div>

            <!-- Risk Debate -->
            <div v-if="deepaiRiskSpeakers.length" class="scroll-card-sm p-4">
              <h4 class="text-xs font-bold text-amber-100 mb-3">风险辩论 · 三位风控辩手</h4>
              <div class="space-y-2 max-h-56 overflow-y-auto">
                <div v-for="rs in deepaiRiskSpeakers" :key="rs.label" class="text-xs">
                  <span class="text-[10px] px-1.5 py-0.5 rounded bg-violet-500/15 text-violet-300 mr-1.5">{{ rs.label }}</span>
                  <span class="text-amber-300/90">{{ rs.text }}</span>
                </div>
              </div>
            </div>

            <!-- Quality Gate -->
            <div v-if="deepaiGate" class="scroll-card-sm p-4">
              <h4 class="text-xs font-bold text-amber-100 mb-2">数据质量门控</h4>
              <div class="flex flex-wrap gap-1 mb-2">
                <span v-for="(g, key) in deepaiGate.grades" :key="key" class="text-[10px] px-1.5 py-0.5 rounded font-mono"
                  :class="g === 'A' ? 'bg-green-500/15 text-green-400' : g === 'B' ? 'bg-amber-500/15 text-amber-500/80' : g === 'C' ? 'bg-orange-500/15 text-orange-400' : 'bg-red-500/15 text-red-400'">
                  {{ key.slice(0, 8) }}:{{ g }}
                </span>
              </div>
              <p v-if="deepaiGateText" class="text-[10px] text-amber-500/60 whitespace-pre-line leading-relaxed">{{ deepaiGateText }}</p>
            </div>

            <!-- Historical Memory -->
            <div v-if="deepaiMemory" class="scroll-card-sm p-4">
              <h4 class="text-xs font-bold text-amber-100 mb-2">历史决策复盘 · 记忆</h4>
              <p class="text-[10px] text-amber-500/60 whitespace-pre-line leading-relaxed">{{ deepaiMemory }}</p>
            </div>

            <!-- Analyst Reports Grid -->
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
              <div v-for="analyst in analystReports" :key="analyst.role" class="scroll-card-sm p-4">
                <div class="flex items-center gap-2 mb-2">
                  <span class="text-base">{{ analyst.icon }}</span>
                  <div>
                    <p class="text-xs font-semibold text-amber-100">{{ analyst.role }}</p>
                    <p class="text-[10px] text-amber-600/60">{{ analyst.name }}</p>
                  </div>
                  <span :class="['ml-auto text-[10px] px-1.5 py-0.5 rounded', analyst.stance === 'bull' ? 'bg-green-500/15 text-green-400' : analyst.stance === 'bear' ? 'bg-red-500/15 text-red-400' : 'bg-amber-500/15 text-amber-500/70']">
                    {{ analyst.stance === 'bull' ? '看多' : analyst.stance === 'bear' ? '看空' : '中性' }}
                  </span>
                </div>
                <p class="text-[11px] text-amber-500/70 leading-relaxed">{{ analyst.summary }}</p>
              </div>
            </div>

            <!-- Debate Transcript -->
            <div v-if="debateLog.length" class="scroll-card-sm p-4">
              <h4 class="text-xs font-bold text-amber-100 mb-3">辩论记录</h4>
              <div class="space-y-2 max-h-48 overflow-y-auto">
                <div v-for="(line, i) in debateLog" :key="i" class="flex gap-2 text-xs">
                  <span class="text-[10px] text-amber-700/50 flex-shrink-0">{{ line.speaker }}:</span>
                  <span :class="line.side === 'bull' ? 'text-green-300' : 'text-red-300'">{{ line.text }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Empty State -->
          <div v-if="!deepaiRunning && !deepaiReport" class="text-center py-12">
            <div class="text-4xl mb-3 opacity-15">🧠</div>
            <p class="text-xs text-amber-700/50">输入A股代码，启动7位AI分析师联合深度投研</p>
            <p class="text-[10px] text-amber-700/40 mt-1">基本面 · 技术面 · 政策面 · 资金面 · 情绪面 · 风险面 · 估值面</p>
          </div>
        </div>
      </div>

      <!-- ========== TAB: 智能看板 ========== -->
      <div v-if="activeTab === 'dashboard'" class="dash-screen">
        <!-- 统计概览 -->
        <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
          <div class="scroll-card text-center flex flex-col items-center justify-center min-h-[92px]">
            <div class="text-3xl font-black text-amber-100 scroll-title leading-none">{{ dashLimitStats.total }}</div>
            <div class="text-[11px] text-amber-600/60 font-serif mt-2">全部</div>
          </div>
          <div class="scroll-card text-center flex flex-col items-center justify-center min-h-[92px]">
            <div class="text-3xl font-black text-red-400 leading-none">{{ dashLimitStats.up }}</div>
            <div class="text-[11px] text-amber-600/60 font-serif mt-2">上涨</div>
          </div>
          <div class="scroll-card text-center flex flex-col items-center justify-center min-h-[92px]">
            <div class="text-3xl font-black text-green-400 leading-none">{{ dashLimitStats.down }}</div>
            <div class="text-[11px] text-amber-600/60 font-serif mt-2">下跌</div>
          </div>
          <div class="limit-up-card text-center flex flex-col items-center justify-center min-h-[92px]">
            <div class="text-3xl font-black text-red-400 leading-none">{{ dashLimitStats.limitUp }}</div>
            <div class="text-[11px] text-red-400/60 font-serif mt-2">涨停</div>
          </div>
          <div class="scroll-card text-center flex flex-col items-center justify-center min-h-[92px]">
            <div class="text-3xl font-black text-green-400 leading-none">{{ dashLimitStats.limitDown }}</div>
            <div class="text-[11px] text-amber-600/60 font-serif mt-2">跌停</div>
          </div>
          <div class="scroll-card text-center flex flex-col items-center justify-center min-h-[92px]">
            <div class="text-3xl font-black text-amber-300 leading-none">{{ dashLimitStats.st }}</div>
            <div class="text-[11px] text-amber-600/60 font-serif mt-2">ST</div>
          </div>
        </div>

        <!-- 可视化图表 第一排：涨跌分布 / 涨幅TOP10 / 跌幅TOP10 / 情绪温度计（4 列并排，压缩留白） -->
        <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-3">
          <div class="scroll-card overflow-hidden">
            <div class="p-3 border-b border-amber-800/20"><h3 class="text-xs font-bold text-amber-200 font-serif">涨跌分布</h3></div>
            <div id="dash-breadth-chart" class="w-full" style="height: 176px;"></div>
          </div>
          <div class="scroll-card overflow-hidden">
            <div class="p-3 border-b border-amber-800/20"><h3 class="text-xs font-bold text-red-400 font-serif">涨幅 TOP 10</h3></div>
            <div id="dash-gainers-chart" class="w-full" style="height: 176px;"></div>
          </div>
          <div class="scroll-card overflow-hidden">
            <div class="p-3 border-b border-amber-800/20"><h3 class="text-xs font-bold text-green-400 font-serif">跌幅 TOP 10</h3></div>
            <div id="dash-losers-chart" class="w-full" style="height: 176px;"></div>
          </div>
          <div class="scroll-card overflow-hidden">
            <div class="p-3 border-b border-amber-800/20"><h3 class="text-xs font-bold text-amber-200 font-serif">市场情绪温度计</h3></div>
            <div id="dash-emotion-gauge" class="w-full" style="height: 176px;"></div>
          </div>
        </div>

        <!-- 可视化图表 第二排：换手率 / 成交额 / 振幅分布 / PE 分布 -->
        <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-3">
          <div class="scroll-card overflow-hidden">
            <div class="p-3 border-b border-amber-800/20"><h3 class="text-xs font-bold text-amber-200 font-serif">换手率 TOP 10</h3></div>
            <div id="dash-turnover-chart" class="w-full" style="height: 176px;"></div>
          </div>
          <div class="scroll-card overflow-hidden">
            <div class="p-3 border-b border-amber-800/20"><h3 class="text-xs font-bold text-amber-200 font-serif">成交额 TOP 10</h3></div>
            <div id="dash-amount-chart" class="w-full" style="height: 176px;"></div>
          </div>
          <div class="scroll-card overflow-hidden">
            <div class="p-3 border-b border-amber-800/20"><h3 class="text-xs font-bold text-amber-200 font-serif">振幅分布</h3></div>
            <div id="dash-amplitude-chart" class="w-full" style="height: 176px;"></div>
          </div>
          <div class="scroll-card overflow-hidden">
            <div class="p-3 border-b border-amber-800/20"><h3 class="text-xs font-bold text-amber-200 font-serif">PE 分布</h3></div>
            <div id="dash-pe-chart" class="w-full" style="height: 176px;"></div>
          </div>
        </div>

        <!-- 拼级 + 大盘资金流 -->
        <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3 items-stretch">
          <!-- 晋级数据 -->
          <div class="scroll-card overflow-hidden flex flex-col">
            <div class="p-4 border-b border-amber-800/20">
              <h3 class="text-sm font-bold text-amber-200 font-serif flex items-center gap-2">
                <span class="w-2 h-2 rounded-full bg-cyan-400 animate-pulse"></span>
                连板晋级
              </h3>
              <p class="text-[10px] text-amber-600/50 mt-1">昨日连板今日晋级成功率</p>
            </div>
            <div class="p-3 flex-1 grid grid-cols-2 gap-2 content-start">
              <div v-for="p in dashBoardProgress" :key="p.name"
                class="rounded-lg bg-[#101c30]/60 border border-amber-800/20 p-3 text-center cursor-pointer transition hover:border-cyan-400/40 hover:bg-[#12243c] flex flex-col items-center justify-center"
                @click="p.expanded = !p.expanded" :title="'点击查看晋级个股'">
                <div class="text-[10px] text-amber-500/70 font-serif">{{ p.name }}</div>
                <div class="text-lg font-black text-amber-200 scroll-title">{{ p.count }}<span class="text-[10px] text-amber-600/50 ml-0.5">/{{ p.prev_count }}</span></div>
                <div class="text-[10px] font-mono" :class="p.rate >= 30 ? 'text-red-400' : p.rate < 10 ? 'text-green-400' : 'text-cyan-300'">{{ p.rate }}%</div>
              </div>
              <div v-if="!dashBoardProgress.length" class="col-span-2 text-xs text-amber-700/40 py-8 text-center">暂无晋级数据</div>
            </div>
            <!-- 晋级个股明细（点击展开） -->
            <div class="px-3 pb-2 space-y-1.5">
              <div v-for="p in dashBoardProgress.filter(x => x.expanded)" :key="'x' + p.name"
                class="rounded-lg bg-cyan-900/10 border border-cyan-400/15 px-3 py-2">
                <div class="flex items-center gap-2 mb-1">
                  <span class="text-[10px] font-black text-cyan-300 font-mono">{{ p.name }}成功个股</span>
                  <span class="text-[9px] text-amber-600/40">共 {{ (p.stocks || []).length }} 家</span>
                </div>
                <div class="flex flex-wrap gap-1">
                  <span v-for="(st, i) in (p.stocks || []).slice(0, 12)" :key="st.code + i"
                    class="text-[10px] text-amber-100/80 bg-[#101c30]/70 rounded px-1.5 py-0.5 cursor-pointer hover:text-cyan-300"
                    @click="selectStock(st)">{{ st.name }}</span>
                  <span v-if="!(p.stocks || []).length" class="text-[9px] text-amber-600/40">无</span>
                </div>
              </div>
            </div>
            <!-- 连板梯队（含具体个股） -->
            <div v-if="dashLadders.length" class="px-3 pb-3 space-y-1.5 max-h-[170px] overflow-y-auto">
              <div v-for="lad in dashLadders" :key="lad.lb"
                class="rounded-lg bg-red-900/20 border border-red-400/15 px-3 py-2">
                <div class="flex items-center gap-2 mb-1">
                  <span class="text-[10px] font-black text-red-400 font-mono">{{ lad.lb }}连板</span>
                  <span class="text-[9px] text-amber-600/40">共 {{ lad.count }} 家</span>
                </div>
                <div class="flex flex-wrap gap-1">
                  <span v-for="(st, i) in lad.stocks.slice(0, 12)" :key="st.code + i"
                    class="text-[10px] text-amber-100/80 bg-[#101c30]/70 rounded px-1.5 py-0.5 cursor-pointer hover:text-cyan-300"
                    @click="selectStock(st)">{{ st.name }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 大盘指数资金流 -->
          <div class="scroll-card overflow-hidden flex flex-col">
            <div class="p-4 border-b border-amber-800/20">
              <h3 class="text-sm font-bold text-amber-200 font-serif flex items-center gap-2">
                <span class="w-2 h-2 rounded-full bg-cyan-400"></span>
                大盘指数资金流
              </h3>
              <p class="text-[10px] text-amber-600/50 mt-1">
                {{ dashFundFlow?.flow_source === 'sina' ? '新浪口径：流入 − 流出（≠ 东财主力净额）' : '主力净流入（亿元）' }}
              </p>
            </div>
            <div class="p-3 flex-1 flex flex-col justify-evenly gap-2">
              <div v-for="idx in (dashFundFlow?.indices || [])" :key="idx.name"
                class="flex items-center justify-between rounded-lg bg-[#101c30]/60 border border-amber-800/20 px-3 py-2.5 flex-1">
                <div>
                  <div class="text-xs text-amber-100/80">{{ idx.name }}</div>
                  <div class="text-[9px] text-amber-600/40 font-mono">{{ idx.date }}</div>
                </div>
                <div class="text-right">
                  <div class="text-sm font-black font-mono" :class="idx.main >= 0 ? 'text-red-400' : 'text-green-400'">
                    {{ (idx.main / 1e8).toFixed(1) }} 亿
                  </div>
                  <div class="text-[9px] text-amber-600/40">
                    <!-- tiered=true：东财给四档净额，展示超大/大；false：新浪只给流入/流出总额 -->
                    <template v-if="idx.tiered">
                      超大<span :class="idx.super >= 0 ? 'text-red-400' : 'text-green-400'">{{ (idx.super / 1e8).toFixed(1) }}</span>
                      ·大<span :class="idx.big >= 0 ? 'text-red-400' : 'text-green-400'">{{ (idx.big / 1e8).toFixed(1) }}</span>
                    </template>
                    <template v-else>
                      流入<span class="text-red-400">{{ ((idx.inflow || 0) / 1e8).toFixed(0) }}</span>
                      ·流出<span class="text-green-400">{{ ((idx.outflow || 0) / 1e8).toFixed(0) }}</span>
                    </template>
                  </div>
                </div>
              </div>
              <div v-if="dashFundLoading && !(dashFundFlow?.indices?.length)" class="text-xs text-amber-700/40 py-6 text-center flex-1 flex items-center justify-center">加载中...</div>
              <div v-else-if="!(dashFundFlow?.indices?.length)" class="text-xs text-amber-700/40 py-6 text-center flex-1 flex items-center justify-center">暂无资金流数据</div>
            </div>
          </div>

          <!-- 板块资金流向 -->
          <div class="scroll-card overflow-hidden flex flex-col">
            <div class="p-4 border-b border-amber-800/20">
              <h3 class="text-sm font-bold text-amber-200 font-serif flex items-center gap-2">
                <span class="w-2 h-2 rounded-full bg-cyan-400"></span>
                板块主力净流入 Top
              </h3>
              <p v-if="dashFundFlow?.flow_source === 'sina'" class="text-[10px] text-amber-600/50 mt-1">
                新浪行业口径（约48个）· 流入 − 流出
              </p>
            </div>
            <div class="p-3 flex-1 overflow-y-auto flex flex-col justify-evenly">
              <div v-for="(s, i) in (dashFundFlow?.sectors_in || []).slice(0, 6)" :key="'in' + (s.code || '') + i"
                class="flex items-center justify-between py-1.5 px-2 rounded-lg hover:bg-[#101c30]/60 cursor-pointer text-xs"
                @click="selectStock(s)">
                <span class="text-amber-100/80 truncate">{{ s.name }}</span>
                <span class="text-red-400 font-mono ml-2 flex-shrink-0">+{{ (s.main / 1e8).toFixed(1) }}亿</span>
              </div>
              <div v-for="(s, i) in (dashFundFlow?.sectors_out || []).slice(0, 4)" :key="'out' + (s.code || '') + i"
                class="flex items-center justify-between py-1.5 px-2 rounded-lg hover:bg-[#101c30]/60 cursor-pointer text-xs"
                @click="selectStock(s)">
                <span class="text-amber-100/80 truncate">{{ s.name }}</span>
                <span class="text-green-400 font-mono ml-2 flex-shrink-0">{{ (s.main / 1e8).toFixed(1) }}亿</span>
              </div>
              <div v-if="!dashFundFlow?.sectors_in?.length && !dashFundLoading" class="text-xs text-amber-700/40 py-6 text-center">暂无数据</div>
            </div>
          </div>
        </div>

        <!-- 双图并排：量价散点 + 板块涨跌走势（各占半宽，压缩纵向占比） -->
        <div class="grid grid-cols-1 xl:grid-cols-2 gap-3 items-stretch">
          <!-- 量价散点（换手率 × 涨跌幅 × 成交额） -->
          <div class="scroll-card overflow-hidden">
            <div class="p-3 border-b border-amber-800/20 flex items-center justify-between">
              <h3 class="text-xs font-bold text-amber-200 font-serif">量价散点（活跃度前300 · 换手率 × 涨跌幅）</h3>
              <span class="text-[10px] text-amber-600/40">点大小=成交额 · 滚轮可放大</span>
            </div>
            <div class="p-2">
              <div id="dash-scatter-chart" class="w-full" style="height: 196px;"></div>
            </div>
          </div>

          <!-- 板块涨跌曲线 -->
          <div class="scroll-card overflow-hidden">
          <div class="p-4 border-b border-amber-800/20 flex items-center justify-between">
            <h3 class="text-sm font-bold text-amber-200 font-serif flex items-center gap-2">
              <span class="w-2 h-2 rounded-full bg-cyan-400"></span>
              板块涨跌走势（近30日行业指数）
            </h3>
            <button @click="loadSectorTrends" class="text-[10px] px-2.5 py-1 rounded-lg bg-[#332314] text-amber-400 hover:text-amber-300 border border-amber-400/20 transition font-serif">重绘</button>
          </div>
          <div class="p-3">
            <div v-if="dashSectorLoading" class="text-xs text-amber-700/40 py-10 text-center">板块曲线加载中（约2~3秒）...</div>
            <div v-else-if="dashSectorTrends" id="dash-sector-trends-chart" class="w-full" style="height: 196px;"></div>
            <div v-else class="text-xs text-amber-700/40 py-10 text-center">暂无板块曲线数据</div>
          </div>
          </div>
        </div>

        <!-- 研报速递（合规：仅元数据 + 原文链接；面板只放最新滚动条） -->
        <div class="scroll-card overflow-hidden">
          <div class="p-4 border-b border-amber-800/20 flex items-center justify-between">
            <h3 class="text-sm font-bold text-amber-200 font-serif flex items-center gap-2">
              <span class="w-2 h-2 rounded-full bg-cyan-400"></span>
              研报速递
            </h3>
            <button @click="activeTab = 'news'"
              class="text-[10px] px-2.5 py-1 rounded-lg bg-[#332314] text-amber-400 hover:text-amber-300 border border-amber-400/20 transition font-serif">查看全部 →</button>
          </div>
          <div class="overflow-hidden relative" style="background: rgba(16,28,48,0.4);">
            <div v-if="dashReports.length" class="dash-report-marquee">
              <div class="dash-report-track">
                <a v-for="(r, i) in [...dashReports, ...dashReports]" :key="r.report_id + i" :href="r.url"
                  target="_blank" rel="noopener noreferrer"
                  class="inline-flex items-center gap-2 px-4 py-2.5 hover:bg-[#101c30]/70 transition">
                  <span class="text-[11px] text-amber-300 shrink-0">{{ r.stock_name }}</span>
                  <span :class="['text-[9px] px-1.5 py-0.5 rounded-full shrink-0',
                    r.rating?.includes('买入') ? 'bg-red-500/10 text-red-400' :
                    r.rating?.includes('增持') ? 'bg-orange-500/10 text-orange-400' :
                    r.rating?.includes('持有') ? 'bg-yellow-500/10 text-yellow-400' :
                    'bg-[#a855f7]/15 text-[#c084fc]']">{{ r.rating || '未评级' }}</span>
                  <span class="text-[11px] text-amber-100/80 whitespace-nowrap">{{ r.title }}</span>
                  <span class="text-[10px] text-amber-600/40 shrink-0">{{ r.org_name }}</span>
                  <span class="text-[10px] text-amber-600/40 shrink-0">{{ r.publish_date }}</span>
                </a>
              </div>
            </div>
            <div v-else-if="dashReportsLoading" class="text-xs text-amber-700/40 py-4 text-center">研报加载中...</div>
            <div v-else class="text-xs text-amber-700/40 py-4 text-center">暂无研报数据</div>
          </div>
        </div>

        <!-- 列表类数据统一一行铺开（5 卡并排，占比小、内部滚动）：
             AI 量化推荐 / 低位荐股 / 多策略选股 / 涨停板·连板 / 跌停板·炸板 -->
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-3">
          <!-- AI 推荐优质股 -->
          <div class="recommend-card overflow-hidden">
              <div class="p-4 border-b border-green-400/10">
                <h3 class="text-sm font-bold text-green-300 font-serif flex items-center gap-2">
                  <span class="w-2 h-2 rounded-full bg-green-400 animate-pulse"></span>
                  AI 量化推荐
                </h3>
                <p class="text-[10px] text-amber-600/50 mt-1">涨幅2-9.5% · 换手3-25% · PE&lt;80</p>
              </div>
              <div class="p-3 max-h-[260px] overflow-y-auto">
                <div v-if="dashRecommendations.length" class="space-y-2">
                  <div v-for="(s, i) in dashRecommendations" :key="s.code + i"
                    class="flex items-center justify-between py-2 px-3 rounded-lg bg-green-900/30 hover:bg-green-900/50 transition cursor-pointer"
                    @click="selectStock(s)">
                    <div class="min-w-0">
                      <div class="text-xs text-amber-100 font-medium truncate">{{ s.name }}</div>
                      <div class="text-[10px] text-amber-600/50 font-mono">{{ s.code }}</div>
                    </div>
                    <div class="text-right flex-shrink-0 ml-2">
                      <div class="text-[10px] text-green-400 font-mono">+{{ s.change_pct?.toFixed(2) }}%</div>
                      <div class="text-[9px] text-amber-600/40">{{ s.reason }}</div>
                    </div>
                  </div>
                </div>
                <div v-else class="text-xs text-amber-700/40 py-6 text-center">暂无符合条件的推荐</div>
              </div>
            </div>

            <!-- 低位荐股（技面+基本面+研报） -->
            <div class="recommend-card overflow-hidden">
              <div class="p-4 border-b border-cyan-400/10 flex items-center justify-between">
                <div>
                  <h3 class="text-sm font-bold text-cyan-300 font-serif flex items-center gap-2">
                    <span class="w-2 h-2 rounded-full bg-cyan-400 animate-pulse"></span>
                    低位荐股
                  </h3>
                  <p class="text-[10px] text-amber-600/50 mt-1">52周低位 + PE/市值 + 研报覆盖评分</p>
                </div>
                <button v-if="dashRecommend" class="text-[9px] text-amber-600/50" @click="loadRecommend(true)">刷新 ⟳</button>
              </div>
              <div class="p-3 max-h-[260px] overflow-y-auto">
                <div v-if="dashRecommendLoading" class="text-xs text-amber-700/40 py-6 text-center">荐股评分中（拉取K线+研报，约需数秒）...</div>
                <div v-else-if="dashRecommend?.items?.length" class="space-y-2">
                  <div v-for="(s, i) in dashRecommend.items" :key="s.code + i"
                    class="rounded-lg bg-cyan-900/20 border border-cyan-400/10 hover:bg-cyan-900/35 transition cursor-pointer px-3 py-2"
                    @click="selectStock(s)">
                    <div class="flex items-center justify-between">
                      <div class="flex items-center gap-2 min-w-0">
                        <span class="text-base font-black font-mono text-cyan-300">{{ s.score }}</span>
                        <div class="min-w-0">
                          <div class="text-xs text-amber-100 font-medium truncate">{{ s.name }}<span class="text-[9px] text-amber-600/50 font-mono ml-1">{{ s.code }}</span></div>
                          <div class="text-[9px] text-amber-600/40 font-mono">PE {{ s.pe?.toFixed?.(1) ?? s.pe }} · 位置 {{ s.pos52 }}% · 研报 {{ s.report_cnt }}份</div>
                        </div>
                      </div>
                      <div class="text-right flex-shrink-0 ml-2">
                        <div class="text-[10px] font-mono" :class="s.pct >= 0 ? 'text-red-400' : 'text-green-400'">{{ s.pct >= 0 ? '+' : '' }}{{ s.pct?.toFixed?.(2) ?? s.pct }}%</div>
                        <div class="text-[9px] text-amber-600/40">{{ s.up_trend ? '多头' : '低位' }}</div>
                      </div>
                    </div>
                    <div class="mt-1.5 flex flex-wrap gap-1">
                      <span v-for="(r, j) in (s.reasons || []).slice(0, 3)" :key="j"
                        class="text-[9px] text-cyan-200/70 bg-[#101c30]/60 rounded px-1.5 py-0.5">{{ r }}</span>
                    </div>
                  </div>
                </div>
                <div v-else class="text-xs text-amber-700/40 py-6 text-center">暂无低位荐股数据</div>
              </div>
            </div>

            <!-- 多策略选股（Python 8种策略分组推荐） -->
            <div class="scroll-card overflow-hidden">
              <div class="p-4 border-b border-amber-800/20 flex items-center justify-between">
                <div>
                  <h3 class="text-sm font-bold text-amber-200 font-serif flex items-center gap-2">
                    <span class="w-2 h-2 rounded-full bg-cyan-400 animate-pulse"></span>
                    多策略选股
                  </h3>
                  <p class="text-[10px] text-amber-600/50 mt-1">8种量化策略分组评分推荐</p>
                </div>
                <button v-if="dashMultiStrategies.length" @click="loadMultiStrategies(true)"
                  class="text-[10px] px-2.5 py-1 rounded-lg bg-[#332314] text-amber-400 hover:text-amber-300 border border-amber-400/20 transition font-serif">刷新 ⟳</button>
              </div>
              <div class="p-3">
                <!-- 策略 chip 切换 -->
                <div v-if="dashMultiStrategies.length" class="flex flex-wrap gap-1.5 mb-3">
                  <button v-for="st in dashMultiStrategies" :key="st.id" @click="dashMultiActive = st.id"
                    :title="st.desc"
                    :class="['text-[10px] px-2.5 py-1 rounded-full border transition',
                      dashMultiActive === st.id
                        ? 'bg-amber-400/20 text-amber-300 border-amber-400/40 font-bold'
                        : 'bg-[#101c30]/60 text-amber-100/60 border-amber-800/30 hover:text-amber-300 hover:border-amber-400/30']">
                    {{ st.name }}
                  </button>
                </div>
                <div v-if="dashMultiLoading" class="text-xs text-amber-700/40 py-6 text-center">多策略评分中...</div>
                <template v-else-if="dashMultiActiveData">
                  <div v-if="dashMultiActiveData.disabled"
                    class="text-[10px] text-amber-700/40 bg-[#101c30]/40 rounded-lg px-3 py-2 mb-2">
                    ⚠️ 该策略当前停用{{ dashMultiActiveData.disabled_reason ? '：' + dashMultiActiveData.disabled_reason : '' }}
                  </div>
                  <div class="max-h-[260px] overflow-y-auto space-y-2">
                    <div v-if="dashMultiActiveData.items?.length" class="space-y-2">
                      <div v-for="(s, i) in dashMultiActiveData.items" :key="s.code + i"
                        class="rounded-lg bg-[#101c30]/50 border border-amber-800/20 hover:border-cyan-400/40 hover:bg-[#12243c]/60 transition cursor-pointer px-3 py-2"
                        @click="selectStock(s)">
                        <div class="flex items-center justify-between">
                          <div class="flex items-center gap-2 min-w-0">
                            <span class="text-base font-black font-mono text-cyan-300">{{ s.score }}</span>
                            <div class="min-w-0">
                              <div class="text-xs text-amber-100 font-medium truncate">{{ s.name }}<span class="text-[9px] text-amber-600/50 font-mono ml-1">{{ s.code }}</span></div>
                              <div class="text-[9px] text-amber-600/40 font-mono">价格 {{ s.price?.toFixed?.(2) ?? s.price }}</div>
                            </div>
                          </div>
                          <div class="text-right flex-shrink-0 ml-2">
                            <div class="text-[10px] font-mono" :class="(s.change_pct || 0) >= 0 ? 'text-red-400' : 'text-green-400'">
                              {{ (s.change_pct || 0) >= 0 ? '+' : '' }}{{ s.change_pct?.toFixed?.(2) ?? s.change_pct }}%
                            </div>
                          </div>
                        </div>
                        <div class="mt-1.5 flex flex-wrap gap-1">
                          <span v-for="(r, j) in (s.reasons || []).slice(0, 2)" :key="j"
                            class="text-[9px] text-cyan-200/70 bg-[#101c30]/60 rounded px-1.5 py-0.5">{{ r }}</span>
                        </div>
                      </div>
                    </div>
                    <div v-else class="text-xs text-amber-700/40 py-6 text-center">该策略暂无推荐标的</div>
                  </div>
                </template>
                <div v-else class="text-xs text-amber-700/40 py-6 text-center">暂无多策略数据（点击右上角刷新重试）</div>
              </div>
            </div>

          <!-- 涨停板 / 连板 -->
          <div class="limit-up-card overflow-hidden">
              <div class="p-4 border-b border-red-400/10">
                <h3 class="text-sm font-bold text-red-300 font-serif flex items-center gap-2">
                  <span class="w-2 h-2 rounded-full bg-red-400"></span>
                  涨停板 · 连板
                </h3>
                <p class="text-[10px] text-amber-600/50 mt-1">涨幅≥9.5%（创业板/科创板≥19.5%）</p>
              </div>
              <div class="p-3 max-h-[240px] overflow-y-auto">
                <div v-if="dashConsecutive.length" class="space-y-1.5">
                  <div v-for="(s, i) in dashConsecutive" :key="s.code + i"
                    class="flex items-center justify-between py-1.5 px-3 rounded-lg bg-red-900/30 hover:bg-red-900/50 transition cursor-pointer text-xs"
                    @click="selectStock(s)">
                    <span class="text-amber-100/80 truncate">{{ s.name }}</span>
                    <span class="text-red-400 font-mono ml-2 flex-shrink-0">+{{ s.change_pct?.toFixed(2) }}%</span>
                  </div>
                </div>
                <div v-else-if="dashLimitUp.length" class="space-y-1.5">
                  <div v-for="(s, i) in dashLimitUp.slice(0, 20)" :key="s.code + i"
                    class="flex items-center justify-between py-1.5 px-3 rounded-lg bg-red-900/30 hover:bg-red-900/50 transition cursor-pointer text-xs"
                    @click="selectStock(s)">
                    <span class="text-amber-100/80 truncate">{{ s.name }}</span>
                    <span class="text-red-400 font-mono ml-2 flex-shrink-0">+{{ s.change_pct?.toFixed(2) }}%</span>
                  </div>
                </div>
                <div v-else class="text-xs text-amber-700/40 py-6 text-center">暂无涨停数据</div>
              </div>
            </div>

            <!-- 跌停板 / 炸板 -->
            <div class="limit-down-card overflow-hidden">
              <div class="p-4 border-b border-emerald-400/10">
                <h3 class="text-sm font-bold text-emerald-300 font-serif flex items-center gap-2">
                  <span class="w-2 h-2 rounded-full bg-emerald-400"></span>
                  跌停板 · 炸板
                </h3>
                <p class="text-[10px] text-amber-600/50 mt-1">跌停 {{ (dashLimitPools?.limit_down || []).length }} · 炸板 {{ (dashLimitPools?.zhaban || []).length }}</p>
              </div>
              <div class="p-3 max-h-[240px] overflow-y-auto">
                <div v-if="(dashLimitPools?.limit_down || []).length" class="space-y-1.5">
                  <div v-for="(s, i) in (dashLimitPools.limit_down || []).slice(0, 15)" :key="s.code + i"
                    class="flex items-center justify-between py-1.5 px-3 rounded-lg bg-emerald-900/20 hover:bg-emerald-900/40 transition cursor-pointer text-xs"
                    @click="selectStock(s)">
                    <div class="min-w-0">
                      <div class="text-amber-100/80 truncate">{{ s.name }}</div>
                      <div class="text-[9px] text-amber-600/40 font-mono">{{ s.hybk }}</div>
                    </div>
                    <span class="text-green-400 font-mono ml-2 flex-shrink-0">{{ (s.pct || 0).toFixed(2) }}%</span>
                  </div>
                </div>
                <div v-if="(dashLimitPools?.zhaban || []).length" class="mt-2 space-y-1.5">
                  <div v-for="(s, i) in (dashLimitPools.zhaban || []).slice(0, 15)" :key="'z' + s.code + i"
                    class="flex items-center justify-between py-1.5 px-3 rounded-lg bg-yellow-900/20 hover:bg-yellow-900/40 transition cursor-pointer text-xs"
                    @click="selectStock(s)">
                    <div class="min-w-0">
                      <div class="text-amber-100/80 truncate">{{ s.name }}</div>
                      <div class="text-[9px] text-amber-600/40 font-mono">{{ s.hybk }}</div>
                    </div>
                    <span class="text-amber-400 font-mono ml-2 flex-shrink-0">{{ s.zbc || 0 }}炸</span>
                  </div>
                </div>
                <div v-if="!dashLimitPools && !dashFundLoading" class="text-xs text-amber-700/40 py-6 text-center">暂无数据</div>
                <div v-else-if="dashLimitPools && !(dashLimitPools?.limit_down || []).length && !(dashLimitPools?.zhaban || []).length"
                  class="text-xs text-amber-700/40 py-6 text-center">今日无跌停 · 无炸板</div>
              </div>
            </div>
        </div>

        <!-- 全部 A 股：整宽面板（表头吸顶 + 分页） -->
        <div class="scroll-card overflow-hidden flex flex-col">
            <div class="p-4 border-b border-amber-800/20">
              <div class="flex flex-wrap items-center gap-3">
                <h3 class="text-sm font-bold text-amber-200 font-serif">全部 A 股 <span class="text-[10px] text-amber-600/50">共 {{ filteredDashStocks.length }} 只</span></h3>
                <input v-model="dashSearch" placeholder="搜索代码/名称"
                  class="text-[11px] bg-[#22180c] border border-amber-800/20 rounded-lg px-2.5 py-1 text-amber-100 placeholder-amber-700/40 focus:outline-none focus:border-amber-400/30 w-36" />
                <select v-model="dashFilter"
                  class="text-[11px] bg-[#22180c] border border-amber-800/20 rounded-lg px-2 py-1 text-amber-100 focus:outline-none">
                  <option value="all">全部</option>
                  <option value="up">上涨</option>
                  <option value="down">下跌</option>
                  <option value="limitUp">涨停</option>
                  <option value="limitDown">跌停</option>
                  <option value="st">ST</option>
                </select>
                <select v-model="dashSort"
                  class="text-[11px] bg-[#22180c] border border-amber-800/20 rounded-lg px-2 py-1 text-amber-100 focus:outline-none">
                  <option value="change_desc">涨幅↓</option>
                  <option value="change_asc">涨幅↑</option>
                  <option value="price_desc">价格↓</option>
                  <option value="price_asc">价格↑</option>
                  <option value="amount_desc">成交额↓</option>
                  <option value="turnover_desc">换手率↓</option>
                  <option value="amplitude_desc">振幅↓</option>
                  <option value="pe_desc">PE↓</option>
                </select>
              </div>
            </div>
            <div class="overflow-x-auto flex-1 overflow-y-auto min-h-0">
              <table class="web3-table text-xs min-w-[860px]">
                <thead class="sticky top-0 bg-[#2d1e0f]">
                  <tr>
                    <th class="text-left">代码</th>
                    <th class="text-left">名称</th>
                    <!-- 数值列表头可点击排序：↓/↑ 指示当前排序方向 -->
                    <th class="text-right cursor-pointer select-none hover:text-amber-300 transition" @click="dashHeaderSort('price')">现价<span class="ml-0.5 text-[9px]">{{ dashSortArrow('price') }}</span></th>
                    <th class="text-right cursor-pointer select-none hover:text-amber-300 transition" @click="dashHeaderSort('change')">涨跌%<span class="ml-0.5 text-[9px]">{{ dashSortArrow('change') }}</span></th>
                    <th class="text-right cursor-pointer select-none hover:text-amber-300 transition" @click="dashHeaderSort('amount')">成交额<span class="ml-0.5 text-[9px]">{{ dashSortArrow('amount') }}</span></th>
                    <th class="text-right cursor-pointer select-none hover:text-amber-300 transition" @click="dashHeaderSort('high')">最高<span class="ml-0.5 text-[9px]">{{ dashSortArrow('high') }}</span></th>
                    <th class="text-right cursor-pointer select-none hover:text-amber-300 transition" @click="dashHeaderSort('low')">最低<span class="ml-0.5 text-[9px]">{{ dashSortArrow('low') }}</span></th>
                    <th class="text-right cursor-pointer select-none hover:text-amber-300 transition" @click="dashHeaderSort('open')">今开<span class="ml-0.5 text-[9px]">{{ dashSortArrow('open') }}</span></th>
                    <th class="text-right cursor-pointer select-none hover:text-amber-300 transition" @click="dashHeaderSort('turnover')">换手%<span class="ml-0.5 text-[9px]">{{ dashSortArrow('turnover') }}</span></th>
                    <th class="text-right cursor-pointer select-none hover:text-amber-300 transition" @click="dashHeaderSort('amplitude')">振幅%<span class="ml-0.5 text-[9px]">{{ dashSortArrow('amplitude') }}</span></th>
                    <th class="text-right cursor-pointer select-none hover:text-amber-300 transition" @click="dashHeaderSort('pe')">PE<span class="ml-0.5 text-[9px]">{{ dashSortArrow('pe') }}</span></th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="s in dashPagedStocks" :key="s.code"
                    class="cursor-pointer hover:bg-[#261b0e] transition"
                    @click="selectStock(s)">
                    <td class="font-mono text-amber-100/70">{{ s.code }}</td>
                    <td class="text-amber-100/80">{{ s.name }}</td>
                    <td class="text-right font-mono text-amber-100">{{ s.price?.toFixed(2) }}</td>
                    <td class="text-right font-mono" :class="(s.change_pct || 0) >= 0 ? 'text-red-400' : 'text-green-400'">
                      {{ (s.change_pct || 0) >= 0 ? '+' : '' }}{{ s.change_pct?.toFixed(2) }}%
                    </td>
                    <td class="text-right font-mono text-amber-100/60">{{ formatMoney(s.amount) }}</td>
                    <td class="text-right font-mono text-red-400/70">{{ s.high?.toFixed(2) || '--' }}</td>
                    <td class="text-right font-mono text-green-400/70">{{ s.low?.toFixed(2) || '--' }}</td>
                    <td class="text-right font-mono text-amber-100/60">{{ s.open?.toFixed(2) || '--' }}</td>
                    <td class="text-right font-mono text-amber-100/60">{{ s.turnover_pct?.toFixed(1) || '--' }}</td>
                    <td class="text-right font-mono text-amber-100/60">{{ s.amplitude_pct?.toFixed(1) || '--' }}</td>
                    <td class="text-right font-mono text-amber-100/60">{{ s.pe_ttm?.toFixed(0) || '--' }}</td>
                  </tr>
                  <tr v-if="!dashPagedStocks.length">
                    <td colspan="11" class="text-center text-amber-700/40 py-8">暂无数据</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <!-- 分页 -->
            <div class="p-3 border-t border-amber-800/15 flex items-center justify-between">
              <span class="text-[10px] text-amber-600/40">第 {{ dashPage }} / {{ dashTotalPages || 1 }} 页</span>
              <div class="flex gap-1">
                <button @click="dashPage = Math.max(1, dashPage - 1)" :disabled="dashPage <= 1"
                  class="text-[10px] px-2 py-1 rounded bg-[#332314] text-amber-400 border border-amber-400/20 disabled:opacity-30">上一页</button>
                <button @click="dashPage = Math.min(dashTotalPages, dashPage + 1)" :disabled="dashPage >= dashTotalPages"
                  class="text-[10px] px-2 py-1 rounded bg-[#332314] text-amber-400 border border-amber-400/20 disabled:opacity-30">下一页</button>
              </div>
            </div>
          </div>
      </div>

      <!-- ========== Strategy Detail Modal ========== -->
      <Modal v-model="showStrategyModal" :title="strategyDetail?.name">
        <div v-if="strategyDetail" class="space-y-4">
          <p class="text-sm text-amber-500/70">{{ strategyDetail.description }}</p>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
            <div class="scroll-card-sm p-3"><span class="text-amber-600/60">风险等级</span><p class="text-amber-100 mt-1">{{ strategyDetail.riskLevel || 'MEDIUM' }}</p></div>
            <div class="scroll-card-sm p-3"><span class="text-amber-600/60">状态</span><p class="text-amber-100 mt-1">{{ strategyDetail.status || 'DRAFT' }}</p></div>
            <div class="scroll-card-sm p-3"><span class="text-amber-600/60">收益率</span><p class="text-amber-100 mt-1">{{ strategyDetail.returns || '-' }}%</p></div>
            <div class="scroll-card-sm p-3"><span class="text-amber-600/60">标签</span><p class="text-amber-100 mt-1">{{ strategyDetail.tags || '-' }}</p></div>
          </div>
          <div v-if="strategyDetail.code" class="mt-4">
            <h4 class="text-sm font-semibold text-amber-200/70 mb-2">策略代码</h4>
            <pre class="text-xs text-green-300 bg-black/60 rounded-xl p-4 overflow-x-auto max-h-60 font-mono"><code>{{ strategyDetail.code }}</code></pre>
          </div>
          <div v-if="strategyDetail.backtestData" class="mt-4">
            <h4 class="text-sm font-semibold text-amber-200/70 mb-2">回测数据</h4>
            <pre class="text-xs text-amber-200/70 bg-black/60 rounded-xl p-4 overflow-x-auto max-h-40">{{ strategyDetail.backtestData }}</pre>
          </div>
          <div class="mt-4 flex gap-3">
            <button v-if="canOperateStrategy(strategyDetail)" class="web3-btn text-xs !px-4" @click.stop="handleRunStrategy(strategyDetail.id)">
              运行策略
            </button>
            <span v-else class="text-[11px] px-3 py-2 rounded-lg bg-[#1a1408] text-amber-700/50 border border-amber-400/10 cursor-not-allowed" :title="runBlockReason(strategyDetail)">🔒 {{ runBlockReason(strategyDetail) }}</span>
            <button class="web3-btn-outline text-xs !px-4" @click.stop="showStrategyModal = false">关闭</button>
          </div>
        </div>
      </Modal>
    </div>
  </div>
</template>

<script setup>
// ====================================================
// 量化模拟盘大屏：指数/全球市场/情绪/板块/行情列表/
// 策略/新闻/回测/持仓组合/AI 深度分析 一站式页面，
// 行情与回测对接 pyquant(9006) 与 Vibe 服务(8900)
//
// 文件结构（~4000 行）：
//   <template> L1-1787：10 个 Tab 面板，各 Tab 以「===== TAB: xxx =====」分隔
//     · dashboard（智能看板）L1225-1787 是最大 Tab，含 9 个 ECharts 图表
//     · 其余 Tab 均在 400 行以内
//   <script setup> L1789+：
//     · 导入与公共状态 L1795-1957
//     · 智能看板数据/图表 L1930-3110（loadDashboardData / renderDashExtraCharts 等）
//     · 行情/策略/回测/组合/新闻 L3115-3801
//     · 生命周期 onMounted / watch / onBeforeUnmount L3804-3897
//
// ⚠ 重构建议：dashboard Tab（L1225-1787，562 行模板 + 9 个 ECharts 实例 + ~15 个
//   共享状态变量）与父脚本深度耦合（图表 ID 初始化、跨 Tab 共享 stockList /
//   selectStock 等），无测试覆盖下提取风险高。建议作为独立重构任务，先补测试
//   再拆分。当前仅修 resize 监听泄漏，不强行拆分。
// ====================================================
import { ref, reactive, computed, onMounted, onActivated, onDeactivated, nextTick, watch, onBeforeUnmount } from 'vue'
import { getQuantList, getStockKlineVR, getConceptHot, getIndustryList, getPortfolioData, createStrategy, runStrategy, runPyBacktest, getPyBacktestStatus, getPyBacktestResult, getPyQuotes, getPyQuote, getPySearch, getPySectors, getPyAiAnalyze, getPyLlmProviders, getPyModels, getPyNews, getPyDashboard, getPyPortfolio, getPyLimitPools, getPyBoardProgress, getPySectorTrends, getPyFundFlow, getPyReportsLatest, getPyReports, getPyRecommend, getPyRecommendStrategies, getPyScreenStrategy, getPyScreenCustom, getPyLlmStatus, getPyStrategies, getMyHoldings, saveHolding, deleteHolding } from '@/api/quant'

// 显式命名以供外层 <keep-alive :include="['QuantIndex']"> 精确缓存本组件，
// 避免从 /quant/cockpit 返回时重新挂载 214KB 懒加载 chunk + 重跑 onMounted 数据请求，
// 进而避免 mode="out-in" 过渡期间出现的瞬时空白。
defineOptions({ name: 'QuantIndex' })
import Pagination from '@/components/common/Pagination.vue'
import Loading from '@/components/common/Loading.vue'
import Modal from '@/components/common/Modal.vue'
import * as echarts from 'echarts'
import { useRouter } from 'vue-router'
import { useToastStore } from '@/stores/modules/toast'
import { useAuthStore } from '@/stores/modules/auth'
import { confirm as dlgConfirm } from '@/composables/useDialog'
import AiConfigPanel from './components/AiConfigPanel.vue'
import ChatPanel from './components/ChatPanel.vue'
import StrategyScreenModal from './components/StrategyScreenModal.vue'
import MarketOverviewTab from './components/MarketOverviewTab.vue'
import StockQuotesTab from './components/StockQuotesTab.vue'
import { formatMoney, marketSymbol } from './components/quoteUtils'

const toast = useToastStore()
const auth = useAuthStore()
const router = useRouter()

// 策略归属判定：ADMIN 可操作全部策略；普通用户仅能操作本人创建的策略。
// 公共策略（userId 为空）由后端强制「仅管理员可操作」，前端据此提前置灰按钮
// 并给出原因，避免用户点击后才收到 403。
const isAdmin = computed(() => auth.user?.role === 'ADMIN')
function canOperateStrategy(item) {
  if (!item) return false
  return isAdmin.value || (item.userId != null && String(item.userId) === String(auth.user?.id))
}
function runBlockReason(item) {
  if (!item) return ''
  return item.userId == null ? '公共策略仅管理员可运行' : '他人创建的策略不可运行'
}

// API 响应缓存（SWR：旧数据立即渲染 + 后台静默刷新；TTL 抖动防雪崩、空值短缓存防穿透）
import { swr } from '@/utils/quantCache'
function cachedPy(fn, cacheKey, ttlMs = 60000, isEmpty) {
  return swr(cacheKey, fn, { ttl: ttlMs, isEmpty })
}
// 响应信封 {code,data} 下的空值判定：data 为空数组/空对象视为 miss（负缓存）
const envelopeEmptyList = v => !v?.data || (Array.isArray(v.data) && v.data.length === 0)
const envelopeEmptyObj = v => !v?.data || (typeof v.data === 'object' && Object.keys(v.data).length === 0)
function cachedDashboard() { return cachedPy(getPyDashboard, 'dashboard', 60000, envelopeEmptyObj) }
function cachedQuotes() { return cachedPy(getPyQuotes, 'quotes', 30000, envelopeEmptyList) }

const tabs = [
  { key: 'dashboard', label: '智能看板' },
  { key: 'overview', label: '市场概览' },
  { key: 'stocks', label: 'A股行情' },
  { key: 'strategies', label: '策略管理' },
  { key: 'portfolio', label: '组合管理' },
  { key: 'backtest', label: '回测分析' },
  { key: 'news', label: '新闻雷达' },
  { key: 'ai', label: 'AI分析' },
  // 市场研究驾驶舱（marketingdashboard 全功能聚合大屏，独立路由全屏嵌入）
  { key: 'cockpit', label: '市场驾驶舱', to: '/quant/cockpit', badge: true },
]
const activeTab = ref('dashboard')

// 带 to 的 tab 跳转独立路由，否则切换当前页 tab
function onTabClick(tab) {
  if (tab.to) { router.push(tab.to); return }
  activeTab.value = tab.key
  // 切到策略管理时立即拉取（不依赖 watch，避免 keep-alive 缓存实例时 watch 不触发）
  if (tab.key === 'strategies') fetchStrategies()
}

const indexCards = ref([
  { name: '上证指数', price: '--', change: 0 },
  { name: '深证成指', price: '--', change: 0 },
  { name: '创业板指', price: '--', change: 0 },
  { name: '科创50', price: '--', change: 0 },
])

// Market data
const globalIndices = ref([])
const marketEmotion = ref(null)
const hotConcepts = ref([])
const industryList = ref([])
const stockList = ref([])
const stockTotal = ref(0)

const thsSectors = ref([])
const thsSectorSource = ref('')

// ===== 智能看板数据 =====
const dashRecommendations = ref([])   // AI 推荐股票列表
const dashLimitUp = ref([])            // 涨停板
const dashConsecutive = ref([])        // 连板
const dashLimitStats = ref({ total: 0, up: 0, down: 0, limitUp: 0, limitDown: 0, st: 0 }) // 统计概览
const dashFilter = ref('all')          // 筛选：all/up/down/limitUp/limitDown
const dashSearch = ref('')
const dashSort = ref('change_desc')    // 排序
const dashPage = ref(1)
const dashPageSize = ref(50)

// ===== 智能看板扩展数据（Python 端点） =====
const dashLimitPools = ref(null)        // 涨停/跌停/炸板池 {limit_up[], limit_down[], zhaban[]}
const dashBoardProgress = ref([])       // 晋级数据 [{name,count,prev_count,rate}]
const dashFundFlow = ref(null)          // 资金流 {indices[], sectors_in[], sectors_out[], stocks_in[], stocks_out[]}
const dashFundLoading = ref(false)
const dashSectorTrends = ref(null)      // 板块曲线 {dates[], series[{name,closes,pcts,today_pct}]}
const dashSectorLoading = ref(false)
const dashReports = ref([])             // 研报速递 {title,org_name,author,publish_date,rating,target_price,eps_y1,stock_name,url}
const dashReportsLoading = ref(false)
const dashLadders = ref([])             // 连板梯队 [{lb,count,stocks[]}]
const dashRecommend = ref(null)         // 低位荐股 {items[], pool_size, generated_at} (技面+基本面+研报)
const dashRecommendLoading = ref(false)
const dashMultiStrategies = ref([])     // 多策略选股 [{id,name,desc,items[],disabled?,disabled_reason?}]
const dashMultiActive = ref('')         // 当前选中的多策略 id
const dashMultiLoading = ref(false)
// 当前选中多策略的数据切片
const dashMultiActiveData = computed(() => dashMultiStrategies.value.find(x => x.id === dashMultiActive.value) || null)

// 多策略选股加载（force=true 时让 Python 端强制重算）
async function loadMultiStrategies(force = false) {
  dashMultiLoading.value = true
  try {
    const res = await swr('multi_strategies', () => getPyRecommendStrategies(force), {
      ttl: 600000, force,
      isEmpty: v => {
        const d = v?.data ?? v
        const arr = Array.isArray(d) ? d : (d?.strategies || [])
        return !arr.length
      },
    })
    const d = res?.data || res
    const arr = Array.isArray(d) ? d : (d?.strategies || [])
    if (Array.isArray(arr) && arr.length) {
      dashMultiStrategies.value = arr
      // 默认选中第一个策略；当前选中项失效时回退到第一个
      if (!arr.some(x => x.id === dashMultiActive.value)) dashMultiActive.value = arr[0].id
    }
  } catch (e) {
    if (import.meta.env.DEV) console.warn('loadMultiStrategies fail', e)
  } finally {
    dashMultiLoading.value = false
  }
}

const filteredDashStocks = computed(() => {
  let list = stockList.value
  const q = dashSearch.value.trim().toLowerCase()
  if (q) list = list.filter(s => (s.code || '').includes(q) || (s.name || '').toLowerCase().includes(q))
  if (dashFilter.value === 'up') list = list.filter(s => (s.change_pct || 0) > 0)
  else if (dashFilter.value === 'down') list = list.filter(s => (s.change_pct || 0) < 0)
  else if (dashFilter.value === 'limitUp') list = list.filter(s => (s.change_pct || 0) >= 9.5)
  else if (dashFilter.value === 'limitDown') list = list.filter(s => (s.change_pct || 0) <= -9.5)
  else if (dashFilter.value === 'st') list = list.filter(s => (s.name || '').includes('ST'))
  // 排序键 → 字段映射（下拉框与表头点击排序共用）
  const sortFieldMap = {
    change: 'change_pct', price: 'price', amount: 'amount', turnover: 'turnover_pct',
    amplitude: 'amplitude_pct', pe: 'pe_ttm', high: 'high', low: 'low', open: 'open',
  }
  const [key, dir] = dashSort.value.split('_')
  const field = sortFieldMap[key] || 'change_pct'
  list = [...list].sort((a, b) => {
    const va = Number(a[field]) || 0
    const vb = Number(b[field]) || 0
    return dir === 'desc' ? vb - va : va - vb
  })
  return list
})
// 表头点击排序：同一列在 ↓/↑ 间切换，首次点击默认降序（名称类字段可后续扩展）
function dashHeaderSort(key) {
  const [curKey, curDir] = dashSort.value.split('_')
  if (curKey === key) {
    dashSort.value = `${key}_${curDir === 'desc' ? 'asc' : 'desc'}`
  } else {
    dashSort.value = `${key}_desc`
  }
  dashPage.value = 1
}
// 表头排序方向指示箭头（仅当前排序列显示）
function dashSortArrow(key) {
  const [curKey, curDir] = dashSort.value.split('_')
  if (curKey !== key) return ''
  return curDir === 'desc' ? '↓' : '↑'
}
const dashPagedStocks = computed(() => {
  const s = (dashPage.value - 1) * dashPageSize.value
  return filteredDashStocks.value.slice(s, s + dashPageSize.value)
})
const dashTotalPages = computed(() => Math.ceil(filteredDashStocks.value.length / dashPageSize.value))

const lastUpdate = ref('')

// Strategy data
const strategyList = ref([])
const strategyPage = ref(1)
const strategySize = ref(9)
const strategyTotal = ref(0)
const strategyListError = ref('')
const showStrategyModal = ref(false)
const strategyDetail = ref(null)
const showCreateForm = ref(false)
const creating = ref(false)
const createError = ref('')
const newStrategy = reactive({ name: '', riskLevel: 'MEDIUM', description: '', code: '' })
// 策略名非空时允许创建
const canCreateStrategy = computed(() => newStrategy.name.trim())

// 策略选股（内置条件筛选 / 自定义代码沙箱）
// 弹窗内部状态（mode/builtinId/limit/customCode/running/error/result）
// 与函数 loadBuiltinStrategies/runScreen 已移入子组件 StrategyScreenModal.vue。
// 父组件保留：screenOpen（按钮触发）、screenLastResult（tab 上展示「上次筛选」提示）、
// builtinStrategyOptions（仅用于回测 tab 的 selectedBtStrategy computed 提示）。
const screenOpen = ref(false)
const screenLastResult = ref(null)
// 回测内置策略元数据（11种，quant-py-service 回测引擎原生支持）
const BACKTEST_STRATEGY_META = [
  { id: 'moving_avg', name: '双均线策略', desc: 'MA5 上穿 MA20 金叉买入，死叉卖出' },
  { id: 'macd_signal', name: 'MACD 信号策略', desc: 'DIF 上穿 DEA 金叉买入，量能确认趋势' },
  { id: 'bollinger', name: '布林带策略', desc: '收盘价跌破下轨买入，回归中轨卖出（均值回归）' },
  { id: 'rsi_meanrev', name: 'RSI 均值回归', desc: 'RSI 超卖(<30)买入，超买(>70)卖出' },
  { id: 'bse_smallcap', name: '北证50 小市值轮动', desc: '北交所小市值高动量定期轮动' },
  { id: 'grid_okx', name: '网格交易策略', desc: '震荡区间内自动低买高卖，赚取网格利润' },
  { id: 'kdj_golden', name: 'KDJ 金叉策略', desc: 'KDJ 低位金叉买入，高位死叉卖出' },
  { id: 'volume_breakout', name: '放量突破策略', desc: '放量突破近期高点买入，缩量回落卖出' },
  { id: 'turtle', name: '海龟交易策略', desc: '唐奇安通道突破入场，ATR 跟踪止损离场' },
  { id: 'momentum', name: '动量轮动策略', desc: '多标的动量排名，持有强势股定期轮动' },
  { id: 'mean_reversion', name: '均值回归策略', desc: '价格偏离均线过远时反向交易回归均值' },
]
const builtinStrategyOptions = ref(BACKTEST_STRATEGY_META)
function openScreen() {
  screenOpen.value = true
}

// 监控大屏：站内路由跳转（/quant/dashboard，Vue 版大屏页面）
function openDashboard() {
  router.push('/quant/dashboard')
}

// News
const newsItems = ref([])
const newsReports = ref([])              // 研报速递全量（新闻雷达）
const newsIndustryReports = ref([])      // 行业研报（新闻雷达）

// Portfolio
const portfolioData = ref({ holdings: [], summary: {} })
const loadingPortfolio = ref(false)

// 用户持仓管理（MySQL user_holdings 表，JWT 鉴权）
const myHoldings = ref([])
const loadingHoldings = ref(false)
const holdingsIsAdmin = ref(false)
const showHoldingForm = ref(false)
const holdingForm = ref({ stock_code: '', stock_name: '', shares: 0, cost_price: 0, buy_date: '', notes: '' })

async function loadMyHoldings() {
  loadingHoldings.value = true
  try {
    const res = await getMyHoldings()
    const d = res.data || res || {}
    myHoldings.value = d.holdings || []
    holdingsIsAdmin.value = d.is_admin || false
  } catch {
    myHoldings.value = []
  } finally { loadingHoldings.value = false }
}

function editHolding(h) {
  holdingForm.value = { stock_code: h.stock_code, stock_name: h.stock_name || '', shares: h.shares, cost_price: h.cost_price, buy_date: h.buy_date || '', notes: h.notes || '' }
  showHoldingForm.value = true
}

async function submitHolding() {
  if (!holdingForm.value.stock_code.trim()) return
  try {
    await saveHolding(holdingForm.value)
    showHoldingForm.value = false
    holdingForm.value = { stock_code: '', stock_name: '', shares: 0, cost_price: 0, buy_date: '', notes: '' }
    await loadMyHoldings()
  } catch { /* toast handled by interceptor */ }
}

async function removeHolding(id) {
  const ok = await dlgConfirm('确认删除此持仓？删除后不可恢复。', {
    title: '删除持仓', okText: '删除', tone: 'danger'
  })
  if (!ok) return
  try {
    await deleteHolding(id)
    await loadMyHoldings()
  } catch { /* toast handled by interceptor */ }
}

// ===== 组合策略：多策略选股合并（localStorage 持久化） =====
const strategyPortfolio = ref([])       // 已存的策略选股结果列表
const selectedStrategyIds = ref([])     // 用户勾选的策略 ID

// 合并选中策略的股票：同股票合并，标注来自哪些策略
const combinedStocks = computed(() => {
  const map = new Map()
  for (const sp of strategyPortfolio.value) {
    if (!selectedStrategyIds.value.includes(sp.id)) continue
    for (const it of (sp.items || [])) {
      const key = it.code
      if (map.has(key)) {
        const ex = map.get(key)
        ex.strategy_names.push(sp.strategy_name)
        // 取最高分
        if ((it.score || 0) > (ex.score || 0)) ex.score = it.score
      } else {
        map.set(key, {
          ...it,
          strategy_names: [sp.strategy_name],
        })
      }
    }
  }
  // 按入选策略数降序，再按评分降序
  return [...map.values()].sort((a, b) => {
    if (b.strategy_names.length !== a.strategy_names.length)
      return b.strategy_names.length - a.strategy_names.length
    return (b.score || 0) - (a.score || 0)
  })
})

// 从 localStorage 加载策略组合
function loadStrategyPortfolio() {
  try {
    const list = JSON.parse(localStorage.getItem('quant_strategy_portfolio') || '[]')
    strategyPortfolio.value = list
    // 默认全选
    selectedStrategyIds.value = list.map(s => s.id)
  } catch { strategyPortfolio.value = []; selectedStrategyIds.value = [] }
}

// 删除单个策略
function removeStrategyFromPortfolio(id) {
  strategyPortfolio.value = strategyPortfolio.value.filter(s => s.id !== id)
  selectedStrategyIds.value = selectedStrategyIds.value.filter(sid => sid !== id)
  localStorage.setItem('quant_strategy_portfolio', JSON.stringify(strategyPortfolio.value))
}

// 清空全部
function clearStrategyPortfolio() {
  strategyPortfolio.value = []
  selectedStrategyIds.value = []
  localStorage.removeItem('quant_strategy_portfolio')
}

// AI Chat 配置：aiConfig / showAiConfig ref 留在父组件（子组件 AiConfigPanel 与 ChatPanel 通过 v-model:config 读写）；
// chatMessages / chatInput / chatLoading / sendChat 已全部移入子组件 ChatPanel.vue。
const showAiConfig = ref(false)
const aiConfig = ref({ baseURL: '', apiKey: '', model: '' })
// AI 模块内部分区（合并原「AI分析」「AI深度投研」两个独立 tab）
const aiSubTab = ref('chat')

// ===== AI Chat (streaming via Vibe-Research /api/chat NDJSON) =====
// sendChat / chatMessages / chatInput / chatLoading 已移入子组件 ChatPanel.vue

// ===== TradingAgents-Astock Deep AI Analysis =====
const deepaiStockCode = ref('')
const deepaiStockName = ref('')
const deepaiRunning = ref(false)
const deepaiStep = ref('')
const deepaiProgress = ref(0)
const deepaiReport = ref(null)
const deepaiVerdict = ref('')
const deepaiConfidence = ref(0)
const analystReports = ref([])
const debateLog = ref([])
const deepaiGate = ref(null)
const deepaiPlan = ref(null)
const deepaiTrader = ref(null)
const deepaiRisk = ref(null)
const deepaiPm = ref(null)
const deepaiSignalRating = ref('')
const deepaiMemory = ref('')

// 风险辩论三位辩手展示（history 顺序：激进/保守/中性）
const deepaiRiskSpeakers = computed(() => {
  const r = deepaiRisk.value
  if (!r || !r.history) return []
  const labels = ['激进视角', '保守视角', '中性视角']
  return r.history.slice(0, 3).map((text, i) => ({ label: labels[i] || '风险视角', text }))
})

// 质量门结果文本（去掉 markdown 标题）
const deepaiGateText = computed(() => {
  const s = deepaiGate.value?.summary || ''
  return s.replace(/^#+\s*/gm, '').trim()
})

// 交易员动作/评级的中文与配色
const deepaiActionLabel = computed(() => {
  const a = (deepaiTrader.value?.action || '').toLowerCase()
  if (a.includes('buy')) return { text: '买入', cls: 'bg-green-500/15 text-green-400' }
  if (a.includes('sell')) return { text: '卖出', cls: 'bg-red-500/15 text-red-400' }
  return { text: '持有', cls: 'bg-amber-500/15 text-amber-500/70' }
})
const deepaiRatingMeta = computed(() => {
  const r = deepaiSignalRating.value
  if (r === 'Buy' || r === 'Overweight') return { text: r, cls: 'bg-green-500/15 text-green-400' }
  if (r === 'Sell' || r === 'Underweight') return { text: r, cls: 'bg-red-500/15 text-red-400' }
  return { text: r || 'Hold', cls: 'bg-amber-500/15 text-amber-500/70' }
})

// Deep AI 模型配置（localStorage，保存后锁定，可解锁再次编辑）
const llmProviders = ref([])
const llmProviderErr = ref('')
const deepaiCfg = reactive({ providerKey: '', baseUrl: '', apiKey: '', model: '', locked: false, saved: false, serverDefault: false })
const DEEP_CFG_KEY = 'quant_deepai_llm'
// 服务端默认 LLM 配置状态（/api/quant/ai/llm-status，不暴露 api_key）
const llmDefault = ref(null)
const llmDefaultLoading = ref(false)
const llmDefaultErr = ref('')

const selectedDeepaiProvider = computed(() => llmProviders.value.find(p => p.key === deepaiCfg.providerKey) || null)
const deepaiProviderName = computed(() => selectedDeepaiProvider.value?.name || deepaiCfg.providerKey || '自定义')

// 运行时拉取的模型列表（基于 base_url + api_key）
const deepaiModels = ref([])
const deepaiModelsLoading = ref(false)
const deepaiModelsErr = ref('')

function maskKey(key = '') {
  const s = String(key || '')
  if (!s) return ''
  if (s.length <= 8) return s.slice(0, 2) + '****'
  return s.slice(0, 6) + '****' + s.slice(-4)
}

async function loadLlmProviders() {
  try {
    const res = await getPyLlmProviders()
    const list = Array.isArray(res?.data) ? res.data : []
    if (list.length) {
      llmProviders.value = list
      llmProviderErr.value = ''
    } else {
      llmProviderErr.value = '后端供应商目录为空'
    }
  } catch (e) {
    llmProviderErr.value = '无法获取预置供应商（量化数据服务未启动？）'
    if (import.meta.env.DEV) console.warn('loadLlmProviders failed', e)
  }
}

async function loadLlmDefault() {
  llmDefaultLoading.value = true
  llmDefaultErr.value = ''
  try {
    const res = await getPyLlmStatus()
    llmDefault.value = (res && res.data) || null
    if (llmDefault.value && typeof llmDefault.value.configured === 'undefined') llmDefault.value = null
  } catch (e) {
    llmDefaultErr.value = '无法获取服务端默认模型（量化数据服务未启动？）'
    llmDefault.value = null
  } finally {
    llmDefaultLoading.value = false
  }
}

// 切换到「服务端默认模型」：清空本地配置，交由后端 LLM_DEFAULTS 兜底
function useServerLlm() {
  if (!llmDefault.value || !llmDefault.value.configured) {
    toast.warning('服务端未配置默认 LLM，请手动填写模型配置')
    return
  }
  deepaiCfg.providerKey = ''
  deepaiCfg.baseUrl = ''
  deepaiCfg.apiKey = ''
  deepaiCfg.model = ''
  deepaiCfg.serverDefault = true
  deepaiCfg.locked = true
  deepaiCfg.saved = true
  localStorage.setItem(DEEP_CFG_KEY, JSON.stringify({ serverDefault: true, locked: true, saved: true }))
  toast.success('已切换为服务端默认模型')
}

function initDeepaiCfg() {
  try {
    const saved = JSON.parse(localStorage.getItem(DEEP_CFG_KEY) || '{}')
    if (saved && (saved.baseUrl || saved.serverDefault)) {
      deepaiCfg.providerKey = saved.providerKey || ''
      deepaiCfg.baseUrl = saved.baseUrl || ''
      // 2026-09-17：apiKey 不再从 localStorage 读取，改从 sessionStorage 取（防 XSS/磁盘泄露）
      deepaiCfg.apiKey = sessionStorage.getItem('quant_deepai_apikey') || ''
      deepaiCfg.model = saved.model || ''
      deepaiCfg.locked = !!saved.locked
      deepaiCfg.saved = !!saved.saved
      deepaiCfg.serverDefault = !!saved.serverDefault
    }
  } catch (e) {
    if (import.meta.env.DEV) console.warn('initDeepaiCfg failed', e)
  }
}

async function loadDeepaiModels() {
  if (!deepaiCfg.baseUrl.trim()) {
    deepaiModelsErr.value = '请先填写 Base URL'
    return
  }
  deepaiModelsLoading.value = true
  deepaiModelsErr.value = ''
  try {
    const res = await getPyModels(deepaiCfg.baseUrl.trim(), deepaiCfg.apiKey.trim())
    const list = Array.isArray(res?.data) ? res.data : []
    deepaiModels.value = list
    if (!list.length) {
      deepaiModelsErr.value = '未能获取模型列表：该网关可能不支持 /v1/models，请手动填写模型 ID'
    } else if (!deepaiCfg.model || !list.includes(deepaiCfg.model)) {
      deepaiCfg.model = list[0]
    }
  } catch (e) {
    deepaiModelsErr.value = '获取模型列表失败: ' + (e.message || '网络错误')
  } finally {
    deepaiModelsLoading.value = false
  }
}

function onDeepaiProviderChange() {
  const p = selectedDeepaiProvider.value
  if (p) {
    deepaiCfg.baseUrl = p.base_url || ''
    if (p.models && p.models.length) {
      deepaiModels.value = p.models
      deepaiCfg.model = p.models[0]
    } else {
      deepaiModels.value = []
      deepaiCfg.model = ''
    }
    deepaiModelsErr.value = ''
  } else {
    deepaiCfg.model = ''
    deepaiModels.value = []
    deepaiModelsErr.value = ''
  }
}

function saveDeepaiCfg() {
  if (!deepaiCfg.baseUrl.trim() || !deepaiCfg.apiKey.trim() || !deepaiCfg.model.trim()) return
  deepaiCfg.locked = true
  deepaiCfg.saved = true
  // 2026-09-17：apiKey 不再写入 localStorage，只进 sessionStorage；其他字段进 localStorage
  localStorage.setItem(DEEP_CFG_KEY, JSON.stringify({
    providerKey: deepaiCfg.providerKey,
    baseUrl: deepaiCfg.baseUrl.trim(),
    model: deepaiCfg.model.trim(),
    locked: true, saved: true, serverDefault: false,
  }))
  sessionStorage.setItem('quant_deepai_apikey', deepaiCfg.apiKey.trim())
  toast.success('模型配置已保存并锁定（apiKey 仅本会话保留）')
}

function editDeepaiCfg() {
  deepaiCfg.locked = false
}

function resetDeepaiCfg() {
  deepaiCfg.providerKey = ''
  deepaiCfg.baseUrl = ''
  deepaiCfg.apiKey = ''
  deepaiCfg.model = ''
  deepaiCfg.saved = false
  deepaiCfg.locked = false
  deepaiCfg.serverDefault = false
  localStorage.removeItem(DEEP_CFG_KEY)
}

// ===== TradingAgents-Astock 深度投研（真实接口 quant-py-service /api/quant/ai/analyze） =====
const ANALYST_ROLES = [
  { key: 'market_analyst', role: '市场分析师', name: 'Market Analyst', icon: '📊', stance: '' },
  { key: 'social_analyst', role: '情绪分析师', name: 'Sentiment Analyst', icon: '😤', stance: '' },
  { key: 'news_analyst', role: '新闻分析师', name: 'News Analyst', icon: '📰', stance: '' },
  { key: 'fundamentals_analyst', role: '基本面分析师', name: 'Fundamentals', icon: '📋', stance: '' },
  { key: 'policy_analyst', role: '政策分析师', name: 'Policy Analyst', icon: '📜', stance: '' },
  { key: 'hot_money_analyst', role: '游资追踪', name: 'Capital Flow', icon: '💰', stance: '' },
  { key: 'lockup_analyst', role: '解禁监控', name: 'Lockup Monitor', icon: '🔒', stance: '' },
]

function stanceOf(verdict = '') {
  const v = String(verdict)
  if (v.includes('看多') || v.includes('买入') || v.includes('看涨')) return 'bull'
  if (v.includes('看空') || v.includes('卖出') || v.includes('看跌')) return 'bear'
  return ''
}

function summaryOf(analyst) {
  const rep = analyst?.report || analyst || {}
  if (rep.summary && rep.summary !== '分析完成') return rep.summary
  const parts = []
  if (rep.trend) parts.push(`趋势:${rep.trend}`)
  if (rep.policy_impact) parts.push(`政策影响:${rep.policy_impact}`)
  if (rep.capital_flow) parts.push(`资金流向:${rep.capital_flow}`)
  if (rep.dragon_tiger) parts.push(`龙虎榜:${rep.dragon_tiger}`)
  if (rep.key_points?.length && rep.key_points[0] !== '关键发现1') parts.push(rep.key_points.join('；'))
  if (rep.score !== undefined && rep.score !== null) parts.push(`评分:${rep.score}`)
  return parts.length ? parts.join(' · ') : `${analyst?.name || ''} 完成分析`
}

// 执行 AI 深度投研：调用 quant-py-service(9006) TradingAgents 多 Agent 分析
async function runDeepAnalysis() {
  if (!deepaiStockCode.value || deepaiRunning.value) return
  deepaiRunning.value = true
  deepaiReport.value = null
  deepaiVerdict.value = ''
  analystReports.value = []
  debateLog.value = []
  deepaiGate.value = null
  deepaiPlan.value = null
  deepaiTrader.value = null
  deepaiRisk.value = null
  deepaiPm.value = null
  deepaiSignalRating.value = ''
  deepaiMemory.value = ''
  deepaiProgress.value = 5

  const symbol = deepaiStockCode.value.trim()

  try {
    deepaiStep.value = '提交多 Agent 分析任务...'
    const llmConfig = deepaiCfg.baseUrl && deepaiCfg.model
      ? { base_url: deepaiCfg.baseUrl.trim(), api_key: deepaiCfg.apiKey.trim(), model: deepaiCfg.model.trim() }
      : null
    if (!llmConfig) toast.warning('未配置模型接口，将使用模拟数据分析')
    const res = await getPyAiAnalyze(symbol, llmConfig)
    const data = res?.data || {}
    const analysts = data.analysts || {}
    const signal = data.final_signal || {}

    deepaiStep.value = '汇总 7 位分析师观点...'
    deepaiProgress.value = 90

    const reports = ANALYST_ROLES
      .filter(a => analysts[a.key])
      .map(a => {
        const rep = analysts[a.key] || {}
        const stance = stanceOf(rep.report?.verdict || rep.verdict)
        return {
          ...a,
          stance: stance || (a.key === 'market_analyst' ? 'bull' : ''),
          summary: summaryOf(rep),
        }
      })

    const debate = []
    const db = data.debate || {}
    if (signal.bull_argument || signal.bear_argument) {
      if (signal.bull_argument) debate.push({ speaker: '多方', side: 'bull', text: signal.bull_argument })
      if (signal.bear_argument) debate.push({ speaker: '空方', side: 'bear', text: signal.bear_argument })
      if (!signal.reason && (db.bull_score > 0 || db.bear_score > 0)) {
        debate.push({ speaker: '基金经理', side: '', text: `多空比分 ${(db.bull_score || 0).toFixed(1)} : ${(db.bear_score || 0).toFixed(1)}，多方占比 ${(db.bull_percentage ?? 0).toFixed(1)}%` })
      }
      if (signal.reason) debate.push({ speaker: '基金经理', side: '', text: signal.reason })
    } else if (db.bull_score > 0 || db.bear_score > 0) {
      const bulls = reports.filter(r => r.stance === 'bull')
      const bears = reports.filter(r => r.stance === 'bear')
      if (bulls.length) debate.push({ speaker: '多方', side: 'bull', text: `多空辩论记票：${bulls.length} 位分析师看多，总得分 ${(db.bull_score || 0).toFixed(1)}` })
      if (bears.length) debate.push({ speaker: '空方', side: 'bear', text: `多空辩论记票：${bears.length} 位分析师看空，总得分 ${(db.bear_score || 0).toFixed(1)}` })
      debate.push({ speaker: '基金经理', side: '', text: `多方占比 ${(db.bull_percentage ?? 0).toFixed(1)}%${signal.reason ? '，' + signal.reason : ''}` })
    }
    if (!debate.length && signal.reason) debate.push({ speaker: '基金经理', side: '', text: signal.reason })

    analystReports.value = reports
    debateLog.value = debate
    deepaiVerdict.value = signal.signal || 'HOLD'
    deepaiConfidence.value = Math.round((signal.confidence ?? 0.5) * 100)
    deepaiSignalRating.value = data.signal_rating || signal.rating || ''
    deepaiGate.value = data.quality_gate || null
    deepaiPlan.value = data.investment_plan || null
    deepaiTrader.value = data.trader_proposal || null
    deepaiRisk.value = data.risk_debate || null
    deepaiPm.value = data.pm_decision || null
    deepaiMemory.value = data.memory_context || ''

    if (!reports.length) {
      deepaiStep.value = '分析异常：未返回分析师数据'
      toast.error('深度投研未返回分析师数据，请检查 quant-py-service(9006)')
      return
    }
    deepaiStep.value = '分析完成'
    deepaiProgress.value = 100
    deepaiReport.value = true
  } catch (e) {
    deepaiStep.value = '分析失败'
    toast.error('深度投研失败: ' + (e.message || '网络错误'))
  } finally {
    deepaiRunning.value = false
  }
}

// Chart refs
const equityChartRef = ref(null)
const drawdownChartRef = ref(null)
const heatmapChartRef = ref(null)
const distChartRef = ref(null)
const pieChartRef = ref(null)

// ===== 统一加载：dashboard + quotes 一次拿完 =====
async function loadOverview() {
  try {
    const [dbRes, qRes] = await Promise.allSettled([cachedDashboard(), cachedQuotes()])
    const dbData = dbRes.status === 'fulfilled' ? dbRes.value?.data : {}
    const qList = qRes.status === 'fulfilled' ? (Array.isArray(qRes.value?.data) ? qRes.value.data : []) : []
    const m = dbData?.market || {}
    const total = m.total || qList.length || 0
    const up = m.up ?? qList.filter(s => (s.change_percent || 0) > 0).length
    const down = m.down ?? (total - up)
    const limitUp = qList.filter(s => (s.change_percent || 0) >= 9.5).length
    const limitDown = qList.filter(s => (s.change_percent || 0) <= -9.5).length
    const turnoverYi = qList.reduce((a, s) => a + (s.turnover || 0), 0) / 1e8
    const volume = qList.reduce((a, s) => a + (s.volume || 0), 0) / 1e10

    indexCards.value = [
      { name: '上涨家数', price: up, change: total ? +((up / total) * 100).toFixed(1) : 0 },
      { name: '下跌家数', price: down, change: total ? -((down / total) * 100).toFixed(1) : 0 },
      { name: '涨停(≥9.5%)', price: limitUp, change: 0 },
      { name: '成交总额', price: turnoverYi ? turnoverYi.toFixed(2) + '亿' : '--', change: 0 },
    ]

    const hotList = Array.isArray(dbData?.hot_stocks) ? dbData.hot_stocks : []
    globalIndices.value = hotList.map(d => ({ name: d.name || d.symbol, symbol: d.symbol || '', price: d.price, change_pct: d.change_percent }))

    marketEmotion.value = {
      sentiment: Math.round(50 + ((up - down) / (total || 1)) * 50),
      up, down, limit_up: limitUp, limit_down: limitDown, volume: +volume.toFixed(1),
    }

    hotConcepts.value = qList.slice().sort((a, b) => (b.change_percent || 0) - (a.change_percent || 0))
      .slice(0, 10).map(d => ({ name: d.name || d.symbol, change_pct: d.change_percent || 0, hit: d.price }))

    const sectors = dbData?.sectors || {}
    industryList.value = Object.entries(sectors).map(([name, v]) => ({ name, change_pct: Number(v) || 0 }))
      .sort((a, b) => b.change_pct - a.change_pct)

    if (qList.length) {
      stockList.value = qList.map(mapPyQuote)
      stockTotal.value = stockList.value.length
      // 分页页码由子组件 StockQuotesTab 自己的 watch(props.stockList) 重置，这里不再越权访问 stockPage
    }

    if (m.updated_at) lastUpdate.value = m.updated_at
  } catch (e) { if (import.meta.env.DEV) console.warn('loadOverview fail', e) }
}

// ===== 智能看板数据加载 =====
async function loadDashboardData() {
  // 复用 loadOverview 的数据（stockList 已填充）
  if (!stockList.value.length) await loadOverview()
  const list = stockList.value
  if (!list.length) return

  // 统计概览
  const total = list.length
  const up = list.filter(s => (s.change_pct || 0) > 0).length
  const down = list.filter(s => (s.change_pct || 0) < 0).length
  const limitUp = list.filter(s => (s.change_pct || 0) >= 9.5)
  const limitDown = list.filter(s => (s.change_pct || 0) <= -9.5)
  const st = list.filter(s => (s.name || '').includes('ST'))
  dashLimitStats.value = { total, up, down, limitUp: limitUp.length, limitDown: limitDown.length, st: st.length }
  // 渲染涨跌分布饼图 + 涨幅/跌幅 TOP10 柱状图
  await nextTick()
  renderDashExtraCharts()

  // 涨停板（按涨幅降序）
  dashLimitUp.value = limitUp.sort((a, b) => (b.change_pct || 0) - (a.change_pct || 0)).slice(0, 50)

  // 连板（名称含 "连板" 或 "板" 的特殊标记，或涨幅>=19.5%的创业板/科创板）
  dashConsecutive.value = list.filter(s => {
    const pct = s.change_pct || 0
    const code = s.code || ''
    const isCyber = code.startsWith('3') || code.startsWith('68')
    return (isCyber && pct >= 19.5) || pct >= 9.9
  }).sort((a, b) => (b.change_pct || 0) - (a.change_pct || 0)).slice(0, 30)

  // AI 推荐（基于量化指标简单筛选）
  dashRecommendations.value = list
    .filter(s => {
      const pct = s.change_pct || 0
      const turnover = s.turnover_pct || s.turnoverRate || 0
      const pe = s.pe_ttm || 0
      return pct > 2 && pct < 9.5 && turnover > 3 && turnover < 25 && pe > 0 && pe < 80
    })
    .sort((a, b) => (b.change_pct || 0) - (a.change_pct || 0))
    .slice(0, 20)
    .map(s => ({
      ...s,
      reason: genRecommendReason(s),
    }))
}

// 加载看板扩展数据：涨跌停池 / 晋级+连板梯队 / 资金流 / 研报速递（并行，板块曲线慢独立加载）
async function loadRecommend(force = false) {
  dashRecommendLoading.value = true
  try {
    const res = await swr('recommend', () => getPyRecommend(10, force), {
      ttl: 600000, force,
      isEmpty: v => { const d = v?.data ?? v; return !(d && Array.isArray(d.items) && d.items.length) },
    })
    const d = res?.data || res
    if (d && Array.isArray(d.items)) dashRecommend.value = d
  } catch (e) {
    if (import.meta.env.DEV) console.warn('loadRecommend fail', e)
  } finally {
    dashRecommendLoading.value = false
  }
}

async function loadDashboardExtras() {
  // 低位荐股独立并行（需拉K线+研报较慢，不阻塞其它看板数据）
  loadRecommend()
  // 多策略选股独立并行（Python 8种策略评分，约0.3s）
  loadMultiStrategies()
  const [poolRes, progRes, fundRes, reportRes] = await Promise.allSettled([
    swr('limit_pools', () => getPyLimitPools(), { ttl: 60000, isEmpty: v => { const d = v?.data ?? v; return !(d && (d.limit_up || d.limit_down || d.zhaban)) } }),
    swr('board_progress', () => getPyBoardProgress(), { ttl: 60000, isEmpty: v => { const d = v?.data ?? v; return !Array.isArray(d) || !d.length } }),
    swr('fund_flow', () => getPyFundFlow(), { ttl: 60000, isEmpty: v => { const d = v?.data ?? v; return !(d && Array.isArray(d.indices) && d.indices.length) } }),
    swr('dash_reports', () => getPyReportsLatest(12), { ttl: 60000, isEmpty: envelopeEmptyList }),
  ])
  if (poolRes.status === 'fulfilled') {
    const d = poolRes.value?.data || poolRes.value
    if (d && (d.limit_up || d.limit_down || d.zhaban)) dashLimitPools.value = d
  }
  if (progRes.status === 'fulfilled') {
    const d = progRes.value?.data || progRes.value
    if (Array.isArray(d)) {
      dashBoardProgress.value = d.filter(p => p && p.name)
      const lad = d.find(p => p && p.ladders)
      if (lad && Array.isArray(lad.ladders)) dashLadders.value = lad.ladders
    }
  }
  if (fundRes.status === 'fulfilled') {
    const d = fundRes.value?.data || fundRes.value
    if (d && Array.isArray(d.indices)) dashFundFlow.value = d
  }
  if (reportRes.status === 'fulfilled') {
    const d = reportRes.value?.data || reportRes.value
    if (Array.isArray(d)) dashReports.value = d
  }
  dashRecommendLoading.value = false
  dashFundLoading.value = false
  dashReportsLoading.value = false
  // 冷启动补偿：9006 资金流在服务冷启动/东财限流时可能首返空结构，SWR 负缓存后不会主动重试，
  // 面板会一直空。东财风控多为间歇性，故最多重试 3 次（间隔 20/40/60s），拿到板块数据即停。
  if (!(dashFundFlow.value?.sectors_in?.length)) {
    const retryFundFlow = (attempt, delayMs) => setTimeout(async () => {
      try {
        const res = await getPyFundFlow()
        const d = res?.data || res
        if (d && Array.isArray(d.sectors_in) && d.sectors_in.length) {
          dashFundFlow.value = d
          return
        }
      } catch (e) { /* 静默：下一次补偿或手动刷新恢复 */ }
      if (attempt < 3) retryFundFlow(attempt + 1, 20000 * (attempt + 1))
    }, delayMs)
    retryFundFlow(1, 20000)
  }
  loadSectorTrends()
}

async function loadSectorTrends() {
  dashSectorLoading.value = true
  try {
    const res = await getPySectorTrends(30, 8)  // 原 200 → 8，避免 15s 超时
    const d = res?.data || res
    if (d && Array.isArray(d.dates) && Array.isArray(d.series)) {
      dashSectorTrends.value = d
    }
  } catch (e) {
    if (import.meta.env.DEV) console.warn('loadSectorTrends fail', e)
  } finally {
    dashSectorLoading.value = false
    await nextTick()
    renderSectorTrendsChart()
  }
}

function renderSectorTrendsChart() {
  const d = dashSectorTrends.value
  if (!d) return
  const el = document.getElementById('dash-sector-trends-chart')
  if (!el) return
  try { dashSectorChart?.dispose() } catch {}
  try {
    dashSectorChart = echarts.init(el)
    dashSectorChart.setOption({
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      confine: true,
      backgroundColor: 'rgba(12,20,34,0.95)',
      borderColor: 'rgba(34,211,238,0.4)',
      textStyle: { color: '#dbe7f3', fontSize: 11 },
      axisPointer: { type: 'cross', crossStyle: { color: 'rgba(34,211,238,0.3)' } },
    },
    legend: {
      type: 'scroll',
      top: 0, textStyle: { color: '#8fc0d9', fontSize: 9 }, itemWidth: 10, itemHeight: 6,
      pageIconColor: '#8fc0d9', pageTextStyle: { color: '#8fc0d9' },
      pageIconSize: 10, pageButtonItemGap: 2,
    },
    grid: { left: 8, right: 16, top: 46, bottom: 4, containLabel: true },
    xAxis: {
      type: 'category', data: d.dates,
      axisLine: { lineStyle: { color: 'rgba(120,170,220,0.2)' } },
      axisLabel: { color: '#6d8fb0', fontSize: 9, interval: 4 },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(120,170,220,0.1)' } },
      axisLabel: { color: '#6d8fb0', fontSize: 9, formatter: '{value}%' },
    },
    series: d.series.map(s => {
      const closes = s.closes || []
      const base = closes[0] || 1
      return {
        name: s.name,
        type: 'line', smooth: true, symbol: 'none',
        data: closes.map(c => +(((c - base) / base) * 100).toFixed(2)),
        lineStyle: { width: 1, opacity: 0.7 },
        itemStyle: { opacity: 0 },
        emphasis: { lineStyle: { width: 2.2, opacity: 1 } },
      }
    }),
  })
  } catch (e) {
    if (import.meta.env.DEV) console.warn('sector trends chart render failed, retrying', e)
    setTimeout(renderSectorTrendsChart, 300)
  }
}

// 渲染智能看板额外图表：涨跌分布饼图 + 涨幅/跌幅 TOP10 柱状图
// + 换手率/成交额 TOP10 + 振幅/PE 分布 + 量价散点 + 市场情绪温度计
let dashBreadthChart = null, dashGainersChart = null, dashLosersChart = null
let dashTurnoverChart = null, dashAmountChart = null, dashAmplitudeChart = null
let dashPeChart = null, dashScatterChart = null, dashEmotionGauge = null
const _dashChartTooltip = { confine: true, backgroundColor: 'rgba(12,20,34,0.95)', borderColor: 'rgba(34,211,238,0.4)', textStyle: { color: '#dbe7f3', fontSize: 11 } }
function renderDashExtraCharts() {
  const stats = dashLimitStats.value
  const list = stockList.value || []
  // 1. 涨跌分布饼图
  const el1 = document.getElementById('dash-breadth-chart')
  if (el1 && stats.total) {
    try { dashBreadthChart?.dispose() } catch {}
    dashBreadthChart = echarts.init(el1)
    dashBreadthChart.setOption({
      backgroundColor: 'transparent',
      tooltip: { confine: true, backgroundColor: 'rgba(12,20,34,0.95)', borderColor: 'rgba(34,211,238,0.4)', textStyle: { color: '#dbe7f3', fontSize: 11 } },
      legend: { bottom: 2, textStyle: { color: '#8fc0d9', fontSize: 9 }, itemWidth: 8, itemHeight: 8 },
      series: [{
        type: 'pie', radius: ['35%', '65%'], center: ['50%', '42%'],
        avoidLabelOverlap: true,
        label: { show: true, color: '#dbe7f3', fontSize: 10, formatter: '{b}\n{c} ({d}%)' },
        labelLine: { length: 8, length2: 6 },
        data: [
          { name: '上涨', value: stats.up, itemStyle: { color: '#f87171' } },
          { name: '下跌', value: stats.down, itemStyle: { color: '#34d399' } },
          { name: '涨停', value: stats.limitUp, itemStyle: { color: '#ef4444' } },
          { name: '跌停', value: stats.limitDown, itemStyle: { color: '#10b981' } },
        ],
      }],
    })
  }
  // 2. 涨幅 TOP10 横向柱状图
  const gainers = list.filter(s => (s.change_pct || 0) > 0).sort((a, b) => (b.change_pct || 0) - (a.change_pct || 0)).slice(0, 10).reverse()
  const el2 = document.getElementById('dash-gainers-chart')
  if (el2) {
    try { dashGainersChart?.dispose() } catch {}
    dashGainersChart = echarts.init(el2)
    dashGainersChart.setOption({
      backgroundColor: 'transparent',
      tooltip: { confine: true, backgroundColor: 'rgba(12,20,34,0.95)', borderColor: 'rgba(248,113,113,0.4)', textStyle: { color: '#fca5a5', fontSize: 11 }, formatter: p => `${p.name}: +${p.value}%` },
      grid: { left: 3, right: 30, top: 4, bottom: 4, containLabel: true },
      xAxis: { type: 'value', axisLabel: { color: '#6d8fb0', fontSize: 9, formatter: '{value}%' }, splitLine: { lineStyle: { color: 'rgba(120,170,220,0.08)' } } },
      yAxis: { type: 'category', data: gainers.map(s => s.name?.slice(0, 4) || ''), axisLabel: { color: '#fca5a5', fontSize: 9 }, axisLine: { lineStyle: { color: 'rgba(120,170,220,0.15)' } } },
      series: [{ type: 'bar', data: gainers.map(s => (s.change_pct || 0).toFixed(2)), itemStyle: { color: '#f87171', borderRadius: [0, 3, 3, 0] }, barWidth: 8, label: { show: true, position: 'right', color: '#fca5a5', fontSize: 9, formatter: '+{c}%' } }],
    })
  }
  // 3. 跌幅 TOP10 横向柱状图
  const losers = list.filter(s => (s.change_pct || 0) < 0).sort((a, b) => (a.change_pct || 0) - (b.change_pct || 0)).slice(0, 10).reverse()
  const el3 = document.getElementById('dash-losers-chart')
  if (el3) {
    try { dashLosersChart?.dispose() } catch {}
    dashLosersChart = echarts.init(el3)
    dashLosersChart.setOption({
      backgroundColor: 'transparent',
      tooltip: { confine: true, backgroundColor: 'rgba(12,20,34,0.95)', borderColor: 'rgba(52,211,153,0.4)', textStyle: { color: '#86efac', fontSize: 11 }, formatter: p => `${p.name}: ${p.value}%` },
      grid: { left: 3, right: 30, top: 4, bottom: 4, containLabel: true },
      xAxis: { type: 'value', axisLabel: { color: '#6d8fb0', fontSize: 9, formatter: '{value}%' }, splitLine: { lineStyle: { color: 'rgba(120,170,220,0.08)' } } },
      yAxis: { type: 'category', data: losers.map(s => s.name?.slice(0, 4) || ''), axisLabel: { color: '#86efac', fontSize: 9 }, axisLine: { lineStyle: { color: 'rgba(120,170,220,0.15)' } } },
      series: [{ type: 'bar', data: losers.map(s => (s.change_pct || 0).toFixed(2)), itemStyle: { color: '#34d399', borderRadius: [0, 3, 3, 0] }, barWidth: 8, label: { show: true, position: 'right', color: '#86efac', fontSize: 9, formatter: '{c}%' } }],
    })
  }
  // 4. 换手率 TOP10 横向柱状图（忽略换手为空/0 的）
  const turnoverTop = list.filter(s => (s.turnover_pct || 0) > 0)
    .sort((a, b) => (b.turnover_pct || 0) - (a.turnover_pct || 0)).slice(0, 10).reverse()
  const el4 = document.getElementById('dash-turnover-chart')
  if (el4 && turnoverTop.length) {
    try { dashTurnoverChart?.dispose() } catch {}
    dashTurnoverChart = echarts.init(el4)
    dashTurnoverChart.setOption({
      backgroundColor: 'transparent',
      tooltip: { ..._dashChartTooltip, formatter: p => `${p.name}: 换手 ${p.value}%` },
      grid: { left: 3, right: 34, top: 4, bottom: 4, containLabel: true },
      xAxis: { type: 'value', axisLabel: { color: '#6d8fb0', fontSize: 9, formatter: '{value}%' }, splitLine: { lineStyle: { color: 'rgba(120,170,220,0.08)' } } },
      yAxis: { type: 'category', data: turnoverTop.map(s => s.name?.slice(0, 4) || ''), axisLabel: { color: '#8fc0d9', fontSize: 9 }, axisLine: { lineStyle: { color: 'rgba(120,170,220,0.15)' } } },
      series: [{ type: 'bar', data: turnoverTop.map(s => (s.turnover_pct || 0).toFixed(2)), itemStyle: { color: '#fbbf24', borderRadius: [0, 3, 3, 0] }, barWidth: 8, label: { show: true, position: 'right', color: '#fbbf24', fontSize: 9, formatter: '{c}%' } }],
    })
  }
  // 5. 成交额 TOP10 横向柱状图（亿元）
  const amountTop = list.filter(s => (s.amount || 0) > 0)
    .sort((a, b) => (b.amount || 0) - (a.amount || 0)).slice(0, 10).reverse()
  const el5 = document.getElementById('dash-amount-chart')
  if (el5 && amountTop.length) {
    try { dashAmountChart?.dispose() } catch {}
    dashAmountChart = echarts.init(el5)
    dashAmountChart.setOption({
      backgroundColor: 'transparent',
      tooltip: { ..._dashChartTooltip, formatter: p => `${p.name}: 成交额 ${p.value} 亿` },
      grid: { left: 3, right: 40, top: 4, bottom: 4, containLabel: true },
      xAxis: { type: 'value', axisLabel: { color: '#6d8fb0', fontSize: 9, formatter: '{value}亿' }, splitLine: { lineStyle: { color: 'rgba(120,170,220,0.08)' } } },
      yAxis: { type: 'category', data: amountTop.map(s => s.name?.slice(0, 4) || ''), axisLabel: { color: '#8fc0d9', fontSize: 9 }, axisLine: { lineStyle: { color: 'rgba(120,170,220,0.15)' } } },
      series: [{ type: 'bar', data: amountTop.map(s => +((s.amount || 0) / 1e8).toFixed(1)), itemStyle: { color: '#22d3ee', borderRadius: [0, 3, 3, 0] }, barWidth: 8, label: { show: true, position: 'right', color: '#22d3ee', fontSize: 9, formatter: '{c}亿' } }],
    })
  }
  // 6. 振幅分布直方图（0-2/2-4/4-6/6-8/8-10/10+）
  const ampBuckets = [0, 0, 0, 0, 0, 0]
  list.forEach(s => {
    const a = s.amplitude_pct
    if (a == null || a === '') return
    const v = Number(a)
    if (!isFinite(v)) return
    const i = v >= 10 ? 5 : Math.floor(v / 2)
    ampBuckets[Math.max(0, Math.min(5, i))]++
  })
  const el6 = document.getElementById('dash-amplitude-chart')
  if (el6 && list.length) {
    try { dashAmplitudeChart?.dispose() } catch {}
    dashAmplitudeChart = echarts.init(el6)
    dashAmplitudeChart.setOption({
      backgroundColor: 'transparent',
      tooltip: { ..._dashChartTooltip, formatter: p => `${p.name}: ${p.value} 只` },
      grid: { left: 8, right: 12, top: 20, bottom: 4, containLabel: true },
      xAxis: { type: 'category', data: ['0-2%', '2-4%', '4-6%', '6-8%', '8-10%', '10%+'], axisLabel: { color: '#6d8fb0', fontSize: 9 }, axisLine: { lineStyle: { color: 'rgba(120,170,220,0.15)' } } },
      yAxis: { type: 'value', axisLabel: { color: '#6d8fb0', fontSize: 9 }, splitLine: { lineStyle: { color: 'rgba(120,170,220,0.08)' } } },
      series: [{ type: 'bar', data: ampBuckets, barWidth: 18, itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#22d3ee' }, { offset: 1, color: 'rgba(34,211,238,0.15)' }]), borderRadius: [3, 3, 0, 0] }, label: { show: true, position: 'top', color: '#8fc0d9', fontSize: 9 } }],
    })
  }
  // 7. PE 分布柱状图（亏损/0-20/20-40/40-60/60-100/100+）
  const peBuckets = [0, 0, 0, 0, 0, 0]
  list.forEach(s => {
    const p = s.pe_ttm
    if (p == null || p === '') return
    const v = Number(p)
    if (!isFinite(v)) return
    if (v < 0) peBuckets[0]++
    else if (v < 20) peBuckets[1]++
    else if (v < 40) peBuckets[2]++
    else if (v < 60) peBuckets[3]++
    else if (v < 100) peBuckets[4]++
    else peBuckets[5]++
  })
  const el7 = document.getElementById('dash-pe-chart')
  if (el7 && list.length) {
    try { dashPeChart?.dispose() } catch {}
    dashPeChart = echarts.init(el7)
    dashPeChart.setOption({
      backgroundColor: 'transparent',
      tooltip: { ..._dashChartTooltip, formatter: p => `${p.name}: ${p.value} 只` },
      grid: { left: 8, right: 12, top: 20, bottom: 4, containLabel: true },
      xAxis: { type: 'category', data: ['亏损', '0-20', '20-40', '40-60', '60-100', '100+'], axisLabel: { color: '#6d8fb0', fontSize: 9 }, axisLine: { lineStyle: { color: 'rgba(120,170,220,0.15)' } } },
      yAxis: { type: 'value', axisLabel: { color: '#6d8fb0', fontSize: 9 }, splitLine: { lineStyle: { color: 'rgba(120,170,220,0.08)' } } },
      series: [{ type: 'bar', data: peBuckets, barWidth: 18, itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#a78bfa' }, { offset: 1, color: 'rgba(167,139,250,0.15)' }]), borderRadius: [3, 3, 0, 0] }, label: { show: true, position: 'top', color: '#8fc0d9', fontSize: 9 } }],
    })
  }
  // 8. 量价散点（活跃度前300：x=换手率, y=涨跌幅, 点大小=成交额）
  // 优化：换手率高度右偏（多数集中 0.1%-2%），用对数刻度拉开分布；
  // 成交额点大小用立方根缩放，降低头部票压制；透明度叠加显示密度；
  // 拆上涨/下跌两个系列，legend 可独立切换。
  const _scatterRaw = list.filter(s => (s.amount || 0) > 0)
    .sort((a, b) => (b.amount || 0) - (a.amount || 0)).slice(0, 300)
  const scatterUp = _scatterRaw.filter(s => (s.change_pct || 0) >= 0).map(s => ({
    name: s.name || s.code, code: s.code,
    value: [Math.max(0.01, +(s.turnover_pct || 0).toFixed(2)), +(s.change_pct || 0).toFixed(2), s.amount || 0],
  }))
  const scatterDown = _scatterRaw.filter(s => (s.change_pct || 0) < 0).map(s => ({
    name: s.name || s.code, code: s.code,
    value: [Math.max(0.01, +(s.turnover_pct || 0).toFixed(2)), +(s.change_pct || 0).toFixed(2), s.amount || 0],
  }))
  // 点大小：成交额立方根缩放后收敛到 3~13px。
  // 原上限 40px 会让头部个股糊成一大团、把密集区整片盖住；缩小后密集区也能分辨单点，
  // 成交额差异仍由点大小体现（1亿≈7px，50亿≈13px）。注意 x 轴字段是 turnover_pct
  // （mapPyQuote 归一化后的名字），早前误用 turnover_rate 会让所有点挤在 0.01% 一列上。
  const symSize = d => {
    const v = Math.cbrt((d[2] || 0) / 1e6)
    return Math.max(3, Math.min(13, v * 1.6))
  }
  // 纵轴按 1%~99% 分位截断：实测 300 个点里 76% 落在 ±3% 内，而自动范围会被
  // 北交所/ST 极端值拉到 −12%~+30%，密集区只占约 1/7 高度 → 视觉上糊成一条带。
  // 截断后密集区占约 1/3 高度，代价是少数 |涨跌幅|>10% 的极端票不绘制（标题已注明）。
  const _yAll = [...scatterUp, ...scatterDown].map(d => d.value[1]).sort((a, b) => a - b)
  const _yq = p => _yAll[Math.min(_yAll.length - 1, Math.max(0, Math.round((_yAll.length - 1) * p)))]
  const yLo = Math.min(-5, Math.floor((_yq(0.01) || -5) * 2) / 2)
  const yHi = Math.max(5, Math.ceil((_yq(0.99) || 5) * 2) / 2)
  const el8 = document.getElementById('dash-scatter-chart')
  if (el8) {
    try { dashScatterChart?.dispose() } catch {}
    dashScatterChart = echarts.init(el8)
    dashScatterChart.setOption({
      backgroundColor: 'transparent',
      title: { text: `活跃度前300 · 点大小=成交额 · 纵轴截断 ${yLo}%~${yHi}%`, left: 'center', top: 2, textStyle: { color: '#6d8fb0', fontSize: 9, fontWeight: 400 } },
      tooltip: {
        ..._dashChartTooltip,
        formatter: p => {
          const [t, c, a] = p.value
          const dir = c >= 0 ? '📈' : '📉'
          return `${p.data.name}（${p.data.code}）${dir}<br/>换手率 <b>${t}%</b> · 涨跌幅 <b style="color:${c >= 0 ? '#f87171' : '#34d399'}">${c >= 0 ? '+' : ''}${c}%</b><br/>成交额 <b>${(a / 1e8).toFixed(2)} 亿</b>`
        },
      },
      legend: { data: ['上涨', '下跌'], bottom: 2, textStyle: { color: '#8fc0d9', fontSize: 9 }, itemWidth: 8, itemHeight: 8 },
      grid: { left: 8, right: 20, top: 30, bottom: 30, containLabel: true },
      xAxis: {
        type: 'log', name: '换手率(%) 对数刻度', nameLocation: 'middle', nameGap: 22,
        nameTextStyle: { color: '#6d8fb0', fontSize: 9 },
        min: 0.05,
        axisLine: { lineStyle: { color: 'rgba(120,170,220,0.2)' } },
        axisLabel: { color: '#6d8fb0', fontSize: 9, formatter: v => v + '%' },
        splitLine: { lineStyle: { color: 'rgba(120,170,220,0.08)' } },
      },
      yAxis: {
        type: 'value', name: '涨跌幅(%)', nameLocation: 'middle', nameGap: 28,
        min: yLo, max: yHi,
        nameTextStyle: { color: '#6d8fb0', fontSize: 9 },
        axisLine: { lineStyle: { color: 'rgba(120,170,220,0.2)' } },
        axisLabel: { color: '#6d8fb0', fontSize: 9, formatter: '{value}%' },
        splitLine: { lineStyle: { color: 'rgba(120,170,220,0.08)' } },
      },
      // 密集区支持下钻：滚轮/拖拽放大到关心的一段，默认仍展示全量分布
      dataZoom: [{ type: 'inside', xAxisIndex: 0, yAxisIndex: 0, zoomOnMouseWheel: true, moveOnMouseMove: true, moveOnMouseWheel: false }],
      series: [
        {
          name: '上涨', type: 'scatter', data: scatterUp,
          symbolSize: symSize,
          // 0 轴参考虚线：一眼区分红/绿两侧
          markLine: { silent: true, symbol: 'none', label: { show: false }, lineStyle: { color: 'rgba(140,180,220,0.3)', type: 'dashed', width: 1 }, data: [{ yAxis: 0 }] },
          // 半透明填充 + 浅色描边：重叠时仍能看出是多个点而非一团色块
          itemStyle: { color: 'rgba(248,113,113,0.62)', borderColor: 'rgba(255,200,200,0.45)', borderWidth: 0.5 },
          emphasis: { itemStyle: { color: 'rgba(248,113,113,0.95)', borderColor: '#fff', borderWidth: 1 } },
        },
        {
          name: '下跌', type: 'scatter', data: scatterDown,
          symbolSize: symSize,
          itemStyle: { color: 'rgba(52,211,153,0.62)', borderColor: 'rgba(190,255,220,0.45)', borderWidth: 0.5 },
          emphasis: { itemStyle: { color: 'rgba(52,211,153,0.95)', borderColor: '#fff', borderWidth: 1 } },
        },
      ],
    })
  }
  // 9. 市场情绪温度计（gauge）：涨停贡献 + 上涨占比 - 跌停惩罚，映射 0-100
  const emoScore = (() => {
    if (!stats.total) return null
    const upRatio = (stats.up || 0) / stats.total
    const lu = Math.min(1, (stats.limitUp || 0) / stats.total * 30)
    const ld = Math.min(1, (stats.limitDown || 0) / stats.total * 30)
    return Math.max(0, Math.min(100, Math.round(30 + upRatio * 40 + lu * 25 - ld * 25)))
  })()
  const emoLevel = v => v < 20 ? '冰点' : v < 40 ? '遇冷' : v < 60 ? '中性' : v < 80 ? '活跃' : '狂热'
  const el9 = document.getElementById('dash-emotion-gauge')
  if (el9 && emoScore != null) {
    try { dashEmotionGauge?.dispose() } catch {}
    dashEmotionGauge = echarts.init(el9)
    dashEmotionGauge.setOption({
      backgroundColor: 'transparent',
      series: [{
        type: 'gauge',
        min: 0, max: 100, startAngle: 210, endAngle: -30,
        radius: '95%', center: ['50%', '58%'],
        axisLine: {
          lineStyle: {
            width: 12,
            color: [[0.2, '#34d399'], [0.4, '#22d3ee'], [0.6, '#fbbf24'], [0.8, '#fb923c'], [1, '#ef4444']],
          },
        },
        pointer: { length: '58%', width: 4, itemStyle: { color: '#dbe7f3' } },
        axisTick: { show: false },
        splitLine: { length: 4, distance: -16, lineStyle: { color: 'rgba(219,231,243,0.4)', width: 1 } },
        axisLabel: { color: '#6d8fb0', fontSize: 8, distance: 14 },
        title: { show: true, offsetCenter: [0, '72%'], color: '#8fc0d9', fontSize: 11 },
        detail: { offsetCenter: [0, '42%'], fontSize: 20, fontWeight: 'bold', color: '#dbe7f3', formatter: v => `${v}分` },
        data: [{ value: emoScore, name: `情绪 · ${emoLevel(emoScore)}` }],
      }],
    })
  }
}

function genRecommendReason(s) {
  const reasons = []
  const pct = s.change_pct || 0
  const turnover = s.turnover_pct || s.turnoverRate || 0
  if (pct > 5) reasons.push('强势上涨')
  else if (pct > 3) reasons.push('温和放量')
  if (turnover > 10) reasons.push('高换手活跃')
  else if (turnover > 5) reasons.push('量能配合')
  if (s.amplitude_pct > 8) reasons.push('振幅较大')
  if (!reasons.length) reasons.push('趋势向好')
  return reasons.join('·')
}

// ===== 同花顺板块（单独调用） =====

// 加载同花顺行业板块涨跌排行（pyquant 9006）
async function loadSectors() {
  try {
    const res = await swr('sectors', () => getPySectors(), { ttl: 120000, isEmpty: envelopeEmptyObj })
    const d = res.data || {}
    const arr = typeof d === 'object' ? Object.entries(d).map(([name, v]) => ({ name, change_pct: Number(v) || 0 })) : []
    arr.sort((a, b) => b.change_pct - a.change_pct)
    thsSectors.value = arr.slice(0, 24)
    thsSectorSource.value = res.source || '同花顺'
  } catch (e) { if (import.meta.env.DEV) console.warn('sectors fail', e) }
}

// loadHotStocks 已合并到 loadOverview

// 按代码或关键字搜索并展示行情列表（由 A股行情子组件搜索框 emit('search', q) 上抛触发）
async function fetchStockQuote(query) {
  const q = String(query || '').trim()
  if (!q) return
  try {
    const code = q.startsWith('sh') || q.startsWith('sz') || q.startsWith('bj') ? q : ''
    if (code) {
      const res = await getPyQuote(code)
      const d = res.data
      if (d) stockList.value = [mapPyQuote(d)]
    } else {
      const res = await getPySearch(q)
      const arr = (res.data || []).slice(0, 10)
      const list = []
      for (const s of arr) {
        const qr = await getPyQuote(s.symbol).catch(() => null)
        if (qr?.data) list.push(mapPyQuote(qr.data))
      }
      stockList.value = list.length ? list : []
    }
    lastUpdate.value = new Date().toLocaleTimeString()
  } catch (e) {
    if (import.meta.env.DEV) console.warn('quote fail', e)
  }
}

// 将 pyquant 行情字段映射为页面展示结构
function mapPyQuote(d) {
  return {
    code: (d.symbol || '').replace(/^(sh|sz|bj)/, ''),
    symbol: d.symbol,
    name: d.name,
    price: d.price,
    change_pct: d.change_percent,
    change_amt: d.change,
    amount_wan: (d.volume || 0) / 10000,
    amount: d.turnover,
    high: d.high,
    low: d.low,
    open: d.open,
    amplitude_pct: d.amplitude,
    turnover_pct: d.turnover_rate,
    pe_ttm: d.pe,
    last_close: (d.price || 0) - (d.change || 0),
    source: d.source || '腾讯',
  }
}

// 选中股票并跳转到该股票的详情页面（展示全部数据）
function selectStock(s) {
  const raw = s.symbol || s.code || ''
  const sym = marketSymbol(raw)
  if (!sym) return
  router.push(`/stock/${sym}`)
}

// ===== Strategies =====
// 分页加载策略列表
async function fetchStrategies() {
  try {
    const res = await getQuantList({ page: strategyPage.value, size: strategySize.value })
    // 原生 fetch 返回完整 ApiResponse，axios 拦截器后返回 data；兼容两种形态
    const d = res.data !== undefined ? res.data : res
    strategyList.value = d.records || []
    strategyTotal.value = d.total || (d.records?.length || 0) * 10
  } catch (e) {
    if (import.meta.env.DEV) console.warn('strategies fail', e)
    strategyListError.value = e.message || '加载失败'
  }
}

// 用户切换每页条数：回到第一页重新拉取
function onStrategySizeChange() {
  strategyPage.value = 1
  fetchStrategies()
}

// 打开策略详情弹窗
function showStrategyDetail(item) {
  strategyDetail.value = item
  showStrategyModal.value = true
}

// 创建新策略（初始为草稿状态）
async function createNewStrategy() {
  if (!newStrategy.name.trim()) return
  creating.value = true
  createError.value = ''
  try {
    await createStrategy({
      name: newStrategy.name,
      riskLevel: newStrategy.riskLevel,
      description: newStrategy.description,
      code: newStrategy.code,
      status: 'DRAFT',
    })
    toast.success('策略创建成功！')
    showCreateForm.value = false
    newStrategy.name = ''
    newStrategy.description = ''
    newStrategy.code = ''
    fetchStrategies()
    loadAllUserStrategies()
  } catch (e) {
    createError.value = '创建失败: ' + (e.message || '网络错误')
  } finally { creating.value = false }
}

// 触发运行指定策略（后端会写入一条 quant_log 执行日志）
// 说明：runStrategy 已设置 suppressError，拦截器不再弹全局 toast，
// 由这里统一给出带后端原因的错误提示（如「公共策略仅管理员可操作」）。
async function handleRunStrategy(id) {
  try {
    await runStrategy(id)
    toast.success('策略运行已启动')
  } catch (e) {
    const msg = e?.response?.data?.message || e?.response?.data?.detail || e?.message || '网络错误'
    toast.error('策略运行失败：' + msg)
  }
}

// ===== News =====
// 加载新闻雷达列表（最多 20 条）
async function loadNews() {
  const [newsRes, reportRes, indRes] = await Promise.allSettled([
    swr('news', () => getPyNews(20), { ttl: 120000, isEmpty: envelopeEmptyList }),
    swr('reports_latest', () => getPyReportsLatest(20), { ttl: 120000, isEmpty: envelopeEmptyList }),
    swr('reports_industry', () => getPyReports({ q_type: '1', limit: 10, code: '' }), { ttl: 120000, isEmpty: envelopeEmptyList }),
  ])
  try {
    const res = newsRes.status === 'fulfilled' ? newsRes.value : { data: [] }
    const data = Array.isArray(res.data) ? res.data : []
    newsItems.value = data.slice(0, 20).map(n => ({
      title: n.title || n.content || '--',
      source: n.source || '行情雷达',
      time: n.timestamp || n.time || '--',
      sentiment: n.sentiment || 'neutral',
    }))
  } catch (e) {
    newsItems.value = []
  }
  const collect = (res) => (res?.data && Array.isArray(res.data) ? res.data : [])
  newsReports.value = collect(reportRes.value)
  newsIndustryReports.value = collect(indRes.value)
}

// ===== Backtest =====
// 策略管理(quant-service/Java)创建的用户策略全集，回测下拉与它互通
const allStrategies = ref([])
async function loadAllUserStrategies() {
  try {
    const res = await getQuantList({ page: 1, size: 200 })
    // 原生 fetch 返回完整 ApiResponse，axios 拦截器后返回 data；兼容两种形态
    const d = res.data !== undefined ? res.data : res
    const records = (d && d.records) || []
    allStrategies.value = Array.isArray(records) ? records : []
  } catch (e) {
    if (import.meta.env.DEV) console.warn('load all user strategies fail', e)
  }
}
// 当前选中策略的元信息（用户策略 / 内置策略）
const selectedBtStrategy = computed(() => {
  const u = allStrategies.value.find(s => String(s.id) === String(btForm.strategy_id))
  if (u) return { kind: 'user', name: u.name || '', code: !!(u.code && u.code.trim()) }
  const b = builtinStrategyOptions.value.find(s => s.id === btForm.strategy_id)
  if (b) return { kind: 'builtin', name: b.name, desc: b.desc }
  return null
})
const btForm = reactive({ strategy_id: 'moving_avg', initial_cash: 1000000, stop_loss: -0.10, max_position: 0.05, days: 120 })
const btSymbols = ref('sh600519,sz000858')
const btRunning = ref(false)
const btStatus = ref('')
const btError = ref(false)
const btResult = ref(null)
// 回测标的信息：输入代码后运行回测时拉取名称/现价并展示
const btStocksInfo = ref([])
const btTransactions = computed(() => btResult.value?.transactions || [])
const btTransactionsShow = ref(true)

const btMetrics = computed(() => {
  if (btResult.value) {
    const r = btResult.value
    // 超额收益 = 年化收益率 - 基准收益率（两者均为有效数值时才计算）
    const ar = Number(r.annual_return), br = Number(r.benchmark_return)
    const excess = (isFinite(ar) && isFinite(br)) ? +(ar - br).toFixed(2) : '--'
    return {
      total_return: r.total_return ?? '--',
      annual_return: r.annual_return ?? '--',
      benchmark_return: r.benchmark_return ?? '--',
      excess_return: excess,
      max_drawdown: r.max_drawdown ?? '--',
      sharpe_ratio: r.sharpe_ratio ?? r.sharpe ?? '--',
      sortino: r.sortino ?? '--',
      calmar: r.calmar ?? '--',
      win_rate: r.win_rate ?? '--',
      profit_loss_ratio: r.profit_loss_ratio ?? '--',
      trade_count: r.trade_count ?? '--',
    }
  }
  return {
    total_return: '--', annual_return: '--', benchmark_return: '--', excess_return: '--',
    max_drawdown: '--', sharpe_ratio: '--', sortino: '--', calmar: '--',
    win_rate: '--', profit_loss_ratio: '--', trade_count: '--',
  }
})

// 回测数值格式化（保留两位小数）
function fmtBtNum(v) {
  if (v === null || v === undefined || v === '--') return '--'
  const n = Number(v)
  return isFinite(n) ? n.toFixed(2) : String(v)
}

// 回测百分比格式化（保留一位小数并带符号）
function fmtBtPct(v) {
  if (v === null || v === undefined || v === '--') return '--'
  const n = Number(v)
  return (isFinite(n) ? (n >= 0 ? '+' : '') + n.toFixed(1) + '%' : String(v))
}

// 提交回测任务并轮询进度，完成后渲染回测图表
async function runBacktest() {
  if (btRunning.value) return
  btRunning.value = true
  btStatus.value = '提交回测任务...'
  btError.value = false
  try {
    const symbols = btSymbols.value.split(',').map(s => s.trim()).filter(Boolean)

    // 并行拉取每只标的的名称/现价（失败不阻断回测，显示 --）
    btStocksInfo.value = []
    await Promise.allSettled(symbols.map(async (sy) => {
      try {
        const qres = await getPyQuote(sy)
        const q = qres.data || {}
        btStocksInfo.value.push({
          symbol: sy,
          name: q.name || '--',
          price: q.price != null ? Number(q.price) : null,
          change_percent: q.change_percent != null ? Number(q.change_percent) : null,
        })
      } catch {
        btStocksInfo.value.push({ symbol: sy, name: '--', price: null, change_percent: null })
      }
    }))

    const payload = { ...btForm, symbols }
    // 选中策略管理自定义策略 → 内联其代码与名称（代码仅在策略管理内可增改，回测只读取使用）
    const selUser = allStrategies.value.find(s => String(s.id) === String(btForm.strategy_id))
    if (selUser) {
      payload.code = selUser.code || ''
      payload.name = selUser.name || ''
    }
    const res = await runPyBacktest(payload)
    const data = res.data || {}
    const taskId = data.task_id || data.taskId
    if (!taskId) throw new Error('未获取到任务ID')
    btStatus.value = '回测运行中...'

    let result = null
    for (let i = 0; i < 60; i++) {
      await new Promise(r => setTimeout(r, 2000))
      try {
        const st = await getPyBacktestStatus(taskId)
        const stData = st.data || {}
        if (stData.status === 'completed' || stData.status === 'done' || stData.status === 'finished') {
          const detail = stData.result || stData
          result = stData.result || detail
          break
        }
        if (stData.status === 'failed' || stData.status === 'error') {
          throw new Error(stData.error || '回测失败')
        }
        if (stData.status === 'pending') { btStatus.value = '任务排队中...' }
        else { btStatus.value = '回测运行中...' }
      } catch (err) {
        if (err.message !== '回测失败') throw err
      }
    }

    if (!result) {
      const detailRes = await getPyBacktestResult(taskId)
      result = detailRes.data || {}
    }

    // 异步状态接口返回 { backtest: metrics, ai_analysis, strategy }，同步结果接口直接返回 metrics，这里统一归一化
    result = (result && result.backtest && (result.backtest.equity_curve || result.backtest.total_return !== undefined)) ? result.backtest : result

    if (result && (result.equity_curve || result.final_value || result.total_return !== undefined)) {
      btResult.value = result
      // 报告标题优先使用策略管理中的名称（异步状态接口不带 strategy_name 时兜底）
      const nm = selectedBtStrategy.value?.name || result.strategy_name
      if (nm) btResult.value.strategy_name = nm
      btStatus.value = `回测完成 · 期末净值 ¥${fmtNum(result.final_value)} · 收益率 ${fmtBtPct(result.total_return)}`
      toast.success('回测完成')
      await nextTick()
      renderBacktestCharts()
    } else {
      throw new Error('回测结果为空')
    }
  } catch (e) {
    btError.value = true
    btStatus.value = '回测失败: ' + (e.message || '网络错误')
    toast.error('回测失败: ' + (e.message || '未知错误'))
  } finally {
    btRunning.value = false
  }
}

// 数值千分位格式化
function fmtNum(v) {
  const n = Number(v)
  return isFinite(n) ? Math.round(n).toLocaleString() : String(v)
}

// ===== Backtest Charts =====
// 全部基于 quant-py-service(9006) 真实回测结果渲染，无数据则不画图
function renderBacktestCharts() {
  const r = btResult.value || {}
  const eq = r.equity_curve || r.equity || []
  const bench = r.benchmark_curve || r.benchmark || []
  const dates = Array.isArray(r.dates) && r.dates.length === eq.length ? r.dates : null
  const hasEq = eq.length > 1
  // 有真实日期用日期轴，否则用 T+N 序号
  const xDays = dates || eq.map((_, i) => `T+${i}`)

  // Equity Curve（策略净值 vs 真实上证基准 双线对比）
  if (equityChartRef.value) {
    const c1 = echarts.init(equityChartRef.value)
    chartInstances.push(c1)
    c1.setOption({
      backgroundColor: 'transparent',
      legend: { data: ['策略净值', '基准净值'], textStyle: { color: '#666', fontSize: 10 }, top: 5 },
      grid: { left: 50, right: 20, top: 35, bottom: 25 },
      xAxis: { type: 'category', data: xDays, axisLine: { lineStyle: { color: '#333' } }, axisLabel: { color: '#666', fontSize: 9, interval: Math.max(0, Math.floor(xDays.length / 8)) } },
      yAxis: { type: 'value', scale: true, splitLine: { lineStyle: { color: 'rgba(255,255,255,0.04)' } }, axisLabel: { color: '#666', fontSize: 9 } },
      series: [
        { name: '策略净值', type: 'line', data: eq, smooth: true, lineStyle: { color: '#22d3ee', width: 1.5 }, symbol: 'none', showSymbol: false },
        { name: '基准净值', type: 'line', data: bench.length ? bench : [], smooth: true, lineStyle: { color: bench.length ? '#a855f7' : 'transparent', width: 1.5, type: 'dashed' }, symbol: 'none', showSymbol: false },
      ],
      tooltip: { trigger: 'axis', confine: true, backgroundColor: 'rgba(12,20,34,0.95)', borderColor: 'rgba(34,211,238,0.4)', textStyle: { color: '#dbe7f3', fontSize: 11 } },
    })
    if (!hasEq) {
      c1.clear()
      c1.setOption({ title: { text: '暂无回测数据', left: 'center', top: 'middle', textStyle: { color: '#666', fontSize: 12 } }, backgroundColor: 'transparent' })
    }
  }

  // Drawdown — 由真实净值曲线计算
  if (drawdownChartRef.value) {
    const c2 = echarts.init(drawdownChartRef.value)
    chartInstances.push(c2)
    if (hasEq) {
      let peak = eq[0]
      const dd = eq.map(v => {
        peak = Math.max(peak, v)
        return -((peak - v) / peak) * 100
      })
      c2.setOption({
        backgroundColor: 'transparent',
        grid: { left: 50, right: 20, top: 20, bottom: 25 },
        xAxis: { type: 'category', data: dd.map((_, i) => `T+${i}`), axisLine: { lineStyle: { color: '#333' } }, axisLabel: { color: '#666', fontSize: 9 } },
        yAxis: { type: 'value', splitLine: { lineStyle: { color: 'rgba(255,255,255,0.04)' } }, axisLabel: { color: '#666', fontSize: 9, formatter: '{value}%' } },
        series: [{ type: 'line', data: dd, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(239,68,68,0.3)' }, { offset: 1, color: 'rgba(239,68,68,0)' }]) }, lineStyle: { color: '#ef4444' }, symbol: 'none' }],
      })
    } else {
      c2.clear()
      c2.setOption({ title: { text: '暂无回测数据', left: 'center', top: 'middle', textStyle: { color: '#666', fontSize: 12 } }, backgroundColor: 'transparent' })
    }
  }

  // Monthly Heatmap — 优先使用后端真实 monthly_returns（{YYYY-MM: 收益%}，行=年份 列=12月），缺失时由净值曲线聚合兜底
  if (heatmapChartRef.value) {
    const c3 = echarts.init(heatmapChartRef.value)
    chartInstances.push(c3)
    const months = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']
    let heatmapData = []
    let years = []
    const mr = r.monthly_returns
    if (mr && typeof mr === 'object' && Object.keys(mr).length) {
      // 真实月度收益：按年拆分（行=年份，列=月份）
      years = [...new Set(Object.keys(mr).map(k => String(k).slice(0, 4)))].sort()
      Object.entries(mr).forEach(([k, v]) => {
        const y = String(k).slice(0, 4)
        const m = parseInt(String(k).slice(5, 7), 10) - 1
        if (m >= 0 && m < 12) heatmapData.push([m, years.indexOf(y), Number(v) || 0])
      })
    } else if (hasEq) {
      // 兜底：净值曲线按日收益聚合（无日期时按序号每12个一组近似月份）
      const rets = eq.slice(1).map((v, i) => ((v - eq[i]) / eq[i]) * 100)
      years = ['回测期']
      heatmapData = months.map((m, i) => {
        const chunk = rets.filter((_, j) => j % 12 === i)
        const avg = chunk.length ? chunk.reduce((a, b) => a + b, 0) / chunk.length : 0
        return [i, 0, +avg.toFixed(1)]
      })
    }
    if (heatmapData.length) {
      c3.setOption({
        backgroundColor: 'transparent',
        grid: { left: 50, right: 20, top: 20, bottom: 40, containLabel: true },
        xAxis: { type: 'category', data: months, axisLine: { lineStyle: { color: '#333' } }, axisLabel: { color: '#666', fontSize: 9 } },
        yAxis: { type: 'category', data: years, axisLine: { lineStyle: { color: '#333' } }, axisLabel: { color: '#666', fontSize: 9 } },
        visualMap: { min: -10, max: 15, calculable: true, orient: 'horizontal', left: 'center', bottom: 0, inRange: { color: ['#22c55e', '#fbbf24', '#ef4444'] }, textStyle: { color: '#666', fontSize: 9 } },
        tooltip: { confine: true, backgroundColor: 'rgba(12,20,34,0.95)', borderColor: 'rgba(34,211,238,0.4)', textStyle: { color: '#dbe7f3', fontSize: 11 }, formatter: p => `${p.value[1] >= 0 ? years[p.value[1]] : ''}${months[p.value[0]]}: <b>${p.value[2] >= 0 ? '+' : ''}${p.value[2]}%</b>` },
        series: [{ type: 'heatmap', data: heatmapData, label: { show: true, fontSize: 9, color: '#fff', formatter: p => (p.value[2] >= 0 ? '+' : '') + p.value[2] }, itemStyle: { borderColor: '#111', borderWidth: 1 } }],
      })
    } else {
      c3.clear()
      c3.setOption({ title: { text: '暂无回测数据', left: 'center', top: 'middle', textStyle: { color: '#666', fontSize: 12 } }, backgroundColor: 'transparent' })
    }
  }

  // Returns Distribution — 真实净值曲线的日收益率分布
  if (distChartRef.value) {
    const c4 = echarts.init(distChartRef.value)
    chartInstances.push(c4)
    if (hasEq) {
      const returns = eq.slice(1).map((v, i) => ((v - eq[i]) / eq[i]) * 100)
      const bins = {}
      returns.forEach(r => {
        const b = Math.floor(r)
        bins[b] = (bins[b] || 0) + 1
      })
      const barData = Object.entries(bins).map(([k, v]) => [+k, v]).sort((a, b) => a[0] - b[0])
      c4.setOption({
        backgroundColor: 'transparent',
        grid: { left: 50, right: 20, top: 20, bottom: 25 },
        xAxis: { type: 'category', data: barData.map(d => d[0] + '%'), axisLine: { lineStyle: { color: '#333' } }, axisLabel: { color: '#666', fontSize: 9 } },
        yAxis: { type: 'value', splitLine: { lineStyle: { color: 'rgba(255,255,255,0.04)' } }, axisLabel: { color: '#666', fontSize: 9 } },
        series: [{ type: 'bar', data: barData.map(d => d[1]), itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#0ff' }, { offset: 1, color: '#0ff4' }]) } }],
      })
    } else {
      c4.clear()
      c4.setOption({ title: { text: '暂无回测数据', left: 'center', top: 'middle', textStyle: { color: '#666', fontSize: 12 } }, backgroundColor: 'transparent' })
    }
  }

  // Portfolio Allocation Pie — 真实持仓分布（无持仓时显示空态）
  if (pieChartRef.value) {
    const c5 = echarts.init(pieChartRef.value)
    chartInstances.push(c5)
    const holdings = portfolioData.value?.holdings || []
    if (holdings.length) {
      const data = holdings.map(h => ({
        name: h.name || h.code,
        value: Number(h.marketValue || h.market_value || 0) || 1,
      }))
      c5.setOption({
        backgroundColor: 'transparent',
        legend: { orient: 'vertical', left: 'left', textStyle: { color: '#888', fontSize: 11 }, itemWidth: 12, itemHeight: 12 },
        series: [{
          type: 'pie',
          radius: ['35%', '65%'],
          center: ['55%', '50%'],
          label: { color: '#aaa', fontSize: 11, formatter: '{b}: {d}%' },
          labelLine: { lineStyle: { color: '#555' } },
          itemStyle: { borderRadius: 6, borderColor: '#111', borderWidth: 2 },
          data,
        }],
      })
    } else {
      c5.clear()
      c5.setOption({ title: { text: '暂无持仓数据', left: 'center', top: 'middle', textStyle: { color: '#666', fontSize: 12 } }, backgroundColor: 'transparent' })
    }
  }
}

// ===== Portfolio =====
// 加载持仓组合数据（pyquant /api/quant/portfolio 真实数据）
async function loadPortfolio() {
  loadingPortfolio.value = true
  try {
    const res = await getPyPortfolio()
    portfolioData.value = res.data || res
  } catch {
    portfolioData.value = { holdings: [], summary: {} }
    toast.error('持仓数据加载失败')
  } finally { loadingPortfolio.value = false }
}

// ===== AI Chat =====
// ===== Refresh =====
// 一键刷新市场概览全部数据并更新时间
async function refreshAll() {
  lastUpdate.value = '刷新中...'
  _apiCache.clear()
  await Promise.allSettled([loadOverview(), loadSectors(), fetchStrategies(), loadNews(), loadDashboardExtras()])
  lastUpdate.value = new Date().toLocaleTimeString()
}

// ===== Lifecycle =====
onMounted(async () => {
  await loadOverview()
  await Promise.allSettled([loadSectors(), loadNews(), loadDashboardData(), loadDashboardExtras()])
  nextTick(() => renderBacktestCharts())
  initDeepaiCfg()
  loadLlmProviders()
  loadLlmDefault()
  loadAllUserStrategies()
  fetchStrategies() // 修复：策略管理 tab 从 keep-alive 返回时不触发 watch，onMounted 兜底
  lastUpdate.value = new Date().toLocaleTimeString()
})

// 交易时间自动刷新（每 60s；keep-alive 页面离开时暂停，见 onActivated/onDeactivated）
let autoRefreshTimer = null
function isTradeTime() {
  const now = new Date()
  const h = now.getHours(), m = now.getMinutes(), d = now.getDay()
  if (d === 0 || d === 6) return false
  const t = h * 60 + m
  return (t >= 540 && t <= 690) || (t >= 780 && t <= 1140)
}
function startAutoRefresh() {
  if (autoRefreshTimer) return
  autoRefreshTimer = setInterval(() => {
    if (isTradeTime() && activeTab.value === 'dashboard') {
      loadDashboardData()
      loadOverview()
      lastUpdate.value = new Date().toLocaleTimeString()
    }
  }, 60000)
}
function stopAutoRefresh() {
  if (autoRefreshTimer) { clearInterval(autoRefreshTimer); autoRefreshTimer = null }
}
startAutoRefresh()

watch(activeTab, async (tab) => {
  if (tab === 'dashboard') {
    await loadDashboardData()
    if (!dashLimitPools.value) loadDashboardExtras()
    if (dashSectorTrends.value) {
      await nextTick()
      renderSectorTrendsChart()
    }
  }
  if (tab === 'backtest') {
    loadAllUserStrategies()
    await nextTick()
    renderBacktestCharts()
  }
  if (tab === 'stocks' && !stockList.value.length) {
    loadOverview()
  }
  if (tab === 'overview' && !hotConcepts.value.length) {
    await Promise.allSettled([loadOverview(), loadSectors()])
  }
  if (tab === 'strategies' && !strategyList.value.length) {
    fetchStrategies()
  }
  if (tab === 'news' && !newsItems.value.length) {
    loadNews()
  }
  if (tab === 'portfolio') {
    if (!strategyPortfolio.value.length) loadStrategyPortfolio()
    if (!myHoldings.value.length) loadMyHoldings()
  }
})

let chartInstances = []
let dashSectorChart = null
function handleResize() {
  chartInstances.forEach(c => c?.resize())
  dashSectorChart?.resize()
  dashBreadthChart?.resize()
  dashGainersChart?.resize()
  dashLosersChart?.resize()
  dashTurnoverChart?.resize()
  dashAmountChart?.resize()
  dashAmplitudeChart?.resize()
  dashPeChart?.resize()
  dashScatterChart?.resize()
  dashEmotionGauge?.resize()
}
window.addEventListener('resize', handleResize)

// keep-alive 生命周期：页面离开时暂停轮询与 resize 监听，返回时恢复（onBeforeUnmount 兜底销毁）
onActivated(() => {
  startAutoRefresh()
  window.addEventListener('resize', handleResize)
})
onDeactivated(() => {
  stopAutoRefresh()
  window.removeEventListener('resize', handleResize)
})

// Cleanup charts on unmount
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  stopAutoRefresh()
  if (backfillTimer) { clearTimeout(backfillTimer); backfillTimer = null }
  chartInstances.forEach(c => { try { c?.dispose() } catch {} })
  try { dashSectorChart?.dispose() } catch {}
  try { dashBreadthChart?.dispose() } catch {}
  try { dashGainersChart?.dispose() } catch {}
  try { dashLosersChart?.dispose() } catch {}
  try { dashTurnoverChart?.dispose() } catch {}
  try { dashAmountChart?.dispose() } catch {}
  try { dashAmplitudeChart?.dispose() } catch {}
  try { dashPeChart?.dispose() } catch {}
  try { dashScatterChart?.dispose() } catch {}
  try { dashEmotionGauge?.dispose() } catch {}
})
</script>

<style>
/* ============================================================
   量化终端 — 深色金融行情风格（Deep-Finance / Trading Terminal）
   已由 scoped 改为【非 scoped + .quant-terminal 命名空间】：
   - 页面根节点带 class="quant-terminal"，所有规则限定在页面内部，不污染全站；
   - 后续拆出的子组件渲染在 .quant-terminal 内，自动继承本主题；
   - 属性选择器家族原先靠 scoped 限定，现靠 .quant-terminal 前缀限定，
     统一覆盖模板中海量的 amber/棕 工具类，无需改动 2000+ 行模板。
   涨跌（红涨绿跌）、涨停、AI推荐等语义色予以保留。
   主色板：深蓝黑 #060a12 / 夜色 #0c1422 / 钢蓝描边 / 霓虹青 #22d3ee + 翡翠绿 #34d399
   ============================================================ */
.quant-terminal .scroll-card {
  background: linear-gradient(150deg, #0c1422 0%, #0a111d 55%, #0d1726 100%);
  border: 1px solid rgba(120, 170, 220, 0.14);
  border-radius: 12px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.35);
}
.quant-terminal .scroll-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 85% 0%, rgba(34, 211, 238, 0.06) 0%, transparent 55%),
    radial-gradient(ellipse at 10% 100%, rgba(52, 211, 153, 0.05) 0%, transparent 55%);
  pointer-events: none;
}
.quant-terminal .scroll-card::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(130deg, transparent 35%, rgba(120, 170, 220, 0.05) 45%, transparent 55%);
  pointer-events: none;
}
.quant-terminal .scroll-card:hover {
  border-color: rgba(34, 211, 238, 0.45);
  box-shadow: 0 10px 36px rgba(0, 0, 0, 0.45), 0 0 22px rgba(34, 211, 238, 0.10);
  background: linear-gradient(150deg, #0e192a 0%, #0b1422 55%, #0f1c2e 100%);
}

.quant-terminal .scroll-card-sm {
  background: linear-gradient(150deg, #0b1320 0%, #091120 100%);
  border: 1px solid rgba(120, 170, 220, 0.11);
  border-radius: 9px;
}

.quant-terminal .scroll-title {
  font-family: 'SF Pro Display', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-weight: 800;
  letter-spacing: 0.01em;
  background: linear-gradient(90deg, #34d399, #22d3ee);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.quant-terminal .scroll-tabs {
  position: relative;
}

/* 推荐看板特殊样式（保留绿色语义，金融面板化） */
.quant-terminal .recommend-card {
  background: linear-gradient(150deg, #0d2a20 0%, #081c15 100%);
  border: 1px solid rgba(52, 211, 153, 0.16);
  border-radius: 12px;
}
.quant-terminal .recommend-card:hover {
  border-color: rgba(52, 211, 153, 0.40);
}

/* 涨停板特殊高亮（保留红色语义，金融面板化） */
.quant-terminal .limit-up-card {
  background: linear-gradient(150deg, #431212 0%, #2c0d0d 100%);
  border: 1px solid rgba(248, 113, 113, 0.20);
  border-radius: 12px;
}

/* 跌停板特殊高亮（绿色语义，金融面板化） */
.quant-terminal .limit-down-card {
  background: linear-gradient(150deg, #0d2a1e 0%, #081c15 100%);
  border: 1px solid rgba(52, 211, 153, 0.20);
  border-radius: 12px;
}

/* ============================================================
   AMBER FAMILY → DEEP-FINANCE PALETTE（家族命中，含所有不透明度变体）
   ============================================================ */
/* 文本：亮色系 → 冷白 */
.quant-terminal [class*="text-amber-100"], .quant-terminal [class*="text-amber-200"] { color: #dbe7f3 !important; }
/* 标题/强调色 → 霓虹青 */
.quant-terminal [class*="text-amber-300"], .quant-terminal [class*="text-amber-400"] { color: #3ae2ee !important; }
.quant-terminal [class*="bg-amber-500/15"], .quant-terminal [class*="bg-amber-500/5"] { background: rgba(34, 211, 238, 0.12) !important; }
/* 次级文本 → 钢青 */
.quant-terminal [class*="text-amber-500"], .quant-terminal [class*="text-amber-600"] { color: #8fc0d9 !important; }
.quant-terminal [class*="text-amber-700"] { color: #6d8fb0 !important; }

/* 背景：深棕 → 夜色蓝 */
.quant-terminal [class*="bg-[#332314]"] { background: #101c30 !important; }
.quant-terminal [class*="bg-[#1a1208]"] { background: #0b1422 !important; }
.quant-terminal [class*="bg-[#1e150a]"] { background: #0a121f !important; }
.quant-terminal [class*="bg-[#22180c]"] { background: #0b1422 !important; }
.quant-terminal [class*="bg-[#261b0e]"] { background: #0c1626 !important; }
.quant-terminal [class*="bg-[#2d1e0f]"] { background: #101c30 !important; }

/* 边框：棕 → 钢蓝 */
.quant-terminal [class*="border-amber-800"], .quant-terminal [class*="border-amber-700"] { border-color: rgba(120, 170, 220, 0.16) !important; }
.quant-terminal [class*="border-amber-400"] { border-color: rgba(34, 211, 238, 0.40) !important; }

/* 去除字重上的衬线古风（font-serif → 科技无衬线） */
.quant-terminal [class*="font-serif"] { font-family: 'SF Pro Text', 'PingFang SC', 'Microsoft YaHei', sans-serif !important; }

/* 头部 sticky 表格底色 */
.quant-terminal thead.sticky { background: #101c30 !important; }

/* 页面背景：深色金融星空质感 */
.quant-terminal .scroll-tabs ~ *,
.quant-terminal.min-h-screen,
.quant-terminal .min-h-screen {
  background-color: transparent;
}

/* 滚动条美化（钢青） */
.quant-terminal ::-webkit-scrollbar { width: 6px; height: 6px; }
.quant-terminal ::-webkit-scrollbar-track { background: rgba(10, 18, 31, 0.5); }
.quant-terminal ::-webkit-scrollbar-thumb { background: rgba(58, 226, 238, 0.35); border-radius: 3px; }
.quant-terminal ::-webkit-scrollbar-thumb:hover { background: rgba(58, 226, 238, 0.6); }

/* 研报速递横向滚动跑马灯 */
.quant-terminal .dash-report-marquee { overflow: hidden; }
.quant-terminal .dash-report-track {
  display: inline-flex;
  white-space: nowrap;
  animation: dash-report-scroll 40s linear infinite;
}
.quant-terminal .dash-report-track:hover { animation-play-state: paused; }
@keyframes dash-report-scroll {
  0% { transform: translateX(0); }
  100% { transform: translateX(-50%); }
}

/* ============================================================
   智能看板「数据大屏」排版（根节点 .dash-screen）
   ------------------------------------------------------------
   与上一版「一屏驾驶舱」的区别：不再锁定视口高度、不再用固定网格裁切组件，
   页面按内容自然流动、可纵向滚动，每个分区都是「响应式栅格 + 等高卡片」：
   1. 分区间距由本层统一控制（12px），模板不再各自写 mb-3 / mb-6，
      保证从统计卡到最后一张表的纵向节奏完全一致；
   2. 卡片高度由内容决定；同一行卡片靠 Grid 默认 stretch 自动等高，
      过长的列表由卡片自带 max-h + overflow-y-auto 承载（A 股大表保留
      表头吸顶 + 分页），因此单张卡片不会失控变高；
   3. 标题栏统一冷青渐变 + 左侧强调条，卡片悬停微上浮，贴近行情大屏质感；
   4. 图表按像素高度绘制（ECharts 必须有明确高度），宽度随栅格自适应，
      window resize 时由 handleResize 统一 resize()。
   ============================================================ */
/* 分区间距：模板已去掉各自的 mb-3/mb-6，统一由这里控制 */
.quant-terminal .dash-screen > * + * { margin-top: 0.75rem; }

/* 卡片过渡：悬停微上浮 + 描边微光（与 .scroll-card:hover 叠加） */
.quant-terminal .dash-screen :is(.scroll-card, .recommend-card, .limit-up-card, .limit-down-card) {
  transition: transform 0.18s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}
.quant-terminal .dash-screen :is(.scroll-card, .recommend-card, .limit-up-card, .limit-down-card):hover {
  transform: translateY(-2px);
}

/* 标题栏：冷青渐变底 + 左侧强调条（卡片首个子节点即标题栏，统计卡无标题栏不受影响） */
.quant-terminal .dash-screen :is(.scroll-card, .recommend-card, .limit-up-card, .limit-down-card) > .border-b:first-child {
  background: linear-gradient(90deg, rgba(34, 211, 238, 0.12) 0%, rgba(34, 211, 238, 0.03) 46%, transparent 100%);
  border-bottom-color: rgba(120, 170, 220, 0.22) !important;
  box-shadow: inset 2px 0 0 rgba(34, 211, 238, 0.45);
}
</style>
