<template>
  <div class="min-h-screen px-4 md:px-6 py-8">
    <div class="mx-auto max-w-[1400px]">
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
          @click="activeTab = tab.key"
          :class="['px-4 py-2 text-xs font-medium transition-all border-b-2 whitespace-nowrap flex-shrink-0 font-serif',
            activeTab === tab.key
              ? 'text-amber-300 border-amber-400 bg-amber-500/5'
              : 'text-amber-700 border-transparent hover:text-amber-400 hover:bg-[#1a1208]']">
          {{ tab.label }}
        </button>
        <div class="hidden md:block border-b border-amber-800/20 flex-1"></div>
      </div>

      <!-- ========== TAB: 市场概览 ========== -->
      <div v-if="activeTab === 'overview'">
        <!-- Index Cards -->
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
          <div v-for="idx in indexCards" :key="idx.name" class="scroll-card p-4 motions-reveal" v-reveal>
            <div class="text-[10px] text-amber-600/70 mb-1.5 font-serif">{{ idx.name }}</div>
            <div class="text-xl font-black text-amber-100 scroll-title">{{ idx.price || '--' }}</div>
            <div class="flex items-center gap-2 mt-1">
              <span :class="['text-xs font-mono', idx.change >= 0 ? 'text-red-400' : 'text-green-400']">
                {{ idx.change >= 0 ? '+' : '' }}{{ idx.change?.toFixed(2) || '0.00' }}%
              </span>
            </div>
          </div>
        </div>

        <!-- Global Indices + Market Sentiment -->
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-5">
          <div class="scroll-card overflow-hidden">
            <div class="p-4 border-b border-amber-800/20">
              <h3 class="text-sm font-bold text-amber-200 font-serif">热门异动</h3>
            </div>
            <div class="p-4">
              <div v-if="globalIndices.length" class="space-y-2.5">
                <div v-for="g in globalIndices" :key="g.name"
                  class="flex items-center justify-between py-2 border-b border-amber-800/10 last:border-0">
                  <div>
                    <span class="text-xs text-amber-100/80">{{ g.name }}</span>
                    <span class="text-[10px] text-amber-700/60 ml-2">{{ g.symbol }}</span>
                  </div>
                  <div class="text-right">
                    <div class="text-xs font-mono text-amber-100">{{ g.price?.toFixed(2) }}</div>
                    <div :class="['text-[10px] font-mono', (g.change_pct || 0) >= 0 ? 'text-red-400' : 'text-green-400']">
                      {{ (g.change_pct || 0) >= 0 ? '+' : '' }}{{ g.change_pct?.toFixed(2) }}%
                    </div>
                  </div>
                </div>
              </div>
              <div v-else class="text-xs text-amber-700/50 py-6 text-center">暂无全球市场数据</div>
            </div>
          </div>

          <div class="scroll-card overflow-hidden h-full">
            <div class="p-4 border-b border-amber-800/20">
              <h3 class="text-sm font-bold text-amber-200 font-serif">市场情绪</h3>
            </div>
            <div class="p-4">
              <div v-if="marketEmotion" class="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                <div v-for="(val, key) in marketEmotion" :key="key"
                  class="rounded-lg bg-[#1e150a] border border-amber-800/15 p-3 min-w-0">
                  <div class="flex items-center justify-between gap-2 mb-2">
                    <span class="text-[10px] text-amber-600/70 uppercase tracking-wider truncate font-serif" :title="key">{{ formatEmotionKey(key) }}</span>
                    <span class="text-[11px] font-mono text-right flex-shrink-0" :class="getEmotionTextColor(key)">{{ typeof val === 'number' ? val.toFixed(1) : val }}</span>
                  </div>
                  <div class="h-1.5 rounded-full bg-amber-800/20 overflow-hidden">
                    <div class="h-full rounded-full transition-all duration-700" :class="getEmotionColor(key)"
                      :style="{ width: getEmotionWidth(key, val) + '%' }"></div>
                  </div>
                </div>
              </div>
              <div v-else class="text-xs text-amber-700/50 py-6 text-center">暂无情绪数据</div>
            </div>
          </div>
        </div>
      </div>

      <!-- ========== TAB: 市场热点 ========== -->
      <div v-if="activeTab === 'hot'">
        <!-- THS Sector Heat -->
        <div class="scroll-card overflow-hidden mb-5">
          <div class="p-4 border-b border-amber-800/20 flex items-center justify-between">
            <h3 class="text-sm font-bold text-amber-200 font-serif">行业板块热度 <span class="text-[10px] text-amber-400/70 ml-1">同花顺 · {{ thsSectorSource }}</span></h3>
            <button @click="loadSectors" class="text-[10px] px-2.5 py-1 rounded-lg bg-[#332314] text-amber-400 hover:text-amber-300 border border-amber-400/20 transition font-serif">刷新板块</button>
          </div>
          <div class="p-4">
            <div v-if="thsSectors.length" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2">
              <div v-for="(s, i) in thsSectors" :key="s.name"
                class="flex items-center gap-2.5 rounded-lg bg-[#1e150a] border border-amber-800/15 px-3 py-2 min-w-0"
                :title="s.name">
                <span class="text-[10px] text-amber-700/60 font-mono w-6 flex-shrink-0">{{ i + 1 }}</span>
                <span class="text-xs text-amber-100/80 truncate flex-1 min-w-0">{{ s.name }}</span>
                <span class="text-[10px] font-mono w-16 flex-shrink-0" :class="s.change_pct >= 0 ? 'text-red-400' : 'text-green-400'">
                  {{ s.change_pct >= 0 ? '+' : '' }}{{ s.change_pct.toFixed(2) }}%
                </span>
                <div class="w-14 h-1 rounded-full bg-amber-800/20 overflow-hidden flex-shrink-0">
                  <div class="h-full rounded-full" :class="s.change_pct >= 0 ? 'bg-red-400' : 'bg-green-400'"
                    :style="{ width: Math.min(Math.abs(s.change_pct) * 8, 100) + '%' }"></div>
                </div>
              </div>
            </div>
            <div v-else class="text-xs text-amber-700/50 py-6 text-center">板块数据加载中或源暂不可用...</div>
          </div>
        </div>

        <div class="grid grid-cols-1 lg:grid-cols-2 gap-5">
          <!-- Hot Concepts -->
          <div class="scroll-card overflow-hidden">
            <div class="p-4 border-b border-amber-800/20">
              <h3 class="text-sm font-bold text-amber-200 font-serif">领涨个股 Top 10</h3>
            </div>
            <div class="p-4">
              <div v-if="hotConcepts.length" class="space-y-2">
                <div v-for="(c, i) in hotConcepts" :key="c.name + i"
                  class="flex items-center justify-between py-2.5 px-3 rounded-lg bg-[#1e150a] hover:bg-[#1a1208] transition">
                  <div class="flex items-center gap-3 min-w-0">
                    <span :class="['text-xs font-bold w-5 text-center flex-shrink-0',
                      i < 3 ? 'text-amber-300' : 'text-amber-700/60']">{{ i + 1 }}</span>
                    <span class="text-xs text-amber-100/80 truncate">{{ c.name }}</span>
                  </div>
                  <span :class="['text-xs font-mono flex-shrink-0', (c.change_pct || 0) >= 0 ? 'text-red-400' : 'text-green-400']">
                    {{ (c.change_pct || 0) >= 0 ? '+' : '' }}{{ c.change_pct?.toFixed(2) }}%
                  </span>
                </div>
              </div>
              <div v-else class="text-xs text-amber-700/50 py-8 text-center">暂无领涨数据</div>
            </div>
          </div>

          <!-- Industry Ranking -->
          <div class="scroll-card overflow-hidden">
            <div class="p-4 border-b border-amber-800/20">
              <h3 class="text-sm font-bold text-amber-200 font-serif">行业板块排行</h3>
            </div>
            <div class="p-4">
              <div v-if="industryList.length" class="space-y-2">
                <div v-for="(ind, i) in industryList" :key="ind.name"
                  class="flex items-center justify-between py-2.5 px-3 rounded-lg bg-[#1e150a] hover:bg-[#1a1208] transition">
                  <div class="flex items-center gap-3 min-w-0">
                    <span :class="['text-xs font-bold w-5 text-center flex-shrink-0',
                      i < 3 ? 'text-amber-300' : 'text-amber-700/60']">{{ i + 1 }}</span>
                    <span class="text-xs text-amber-100/80 truncate">{{ ind.name }}</span>
                  </div>
                  <span :class="['text-xs font-mono flex-shrink-0', (ind.change_pct || 0) >= 0 ? 'text-red-400' : 'text-green-400']">
                    {{ (ind.change_pct || 0) >= 0 ? '+' : '' }}{{ ind.change_pct?.toFixed(2) }}%
                  </span>
                </div>
              </div>
              <div v-else class="text-xs text-amber-700/50 py-8 text-center">暂无行业板块数据</div>
            </div>
          </div>
        </div>
      </div>

      <!-- ========== TAB: A股行情 ========== -->
      <div v-if="activeTab === 'stocks'">
        <!-- Market Breadth Stats -->
        <div class="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-5" v-if="stockList.length">
          <div class="scroll-card p-3.5 flex items-center justify-between">
            <div>
              <div class="text-lg font-black matrix-text text-red-400">{{ breadth.up }}</div>
              <div class="text-[10px] text-amber-600/70 uppercase tracking-wider font-serif">上涨</div>
            </div>
            <div class="text-lg">📈</div>
          </div>
          <div class="scroll-card p-3.5 flex items-center justify-between">
            <div>
              <div class="text-lg font-black matrix-text text-green-400">{{ breadth.down }}</div>
              <div class="text-[10px] text-amber-600/70 uppercase tracking-wider font-serif">下跌</div>
            </div>
            <div class="text-lg">📉</div>
          </div>
          <div class="scroll-card p-3.5 flex items-center justify-between">
            <div>
              <div class="text-lg font-black matrix-text text-amber-100">{{ breadth.total }}</div>
              <div class="text-[10px] text-amber-600/70 uppercase tracking-wider font-serif">样本数</div>
            </div>
            <div class="text-lg">🗂</div>
          </div>
          <div class="scroll-card p-3.5 flex items-center justify-between">
            <div>
              <div class="text-lg font-black text-amber-400 scroll-title">{{ lastUpdate || '--' }}</div>
              <div class="text-[10px] text-amber-600/70 uppercase tracking-wider font-serif">更新时间</div>
            </div>
            <div class="text-lg">🕐</div>
          </div>
        </div>

        <!-- Search + Stock Table -->
        <div class="scroll-card overflow-hidden mb-5">
          <div class="p-4 border-b border-amber-800/20 flex flex-wrap items-center gap-3">
            <h3 class="text-sm font-bold text-amber-200 font-serif">A股实时行情 <span class="text-[10px] text-amber-600/70 ml-1">共 {{ stockTotal }} 只</span></h3>
            <input v-model="searchCode" placeholder="输入代码/名称搜索 (如 600519)"
              class="text-xs bg-[#22180c] border border-amber-800/20 rounded-lg px-3 py-1.5 text-amber-100 placeholder-amber-700/50 focus:outline-none focus:border-amber-400/30 w-full sm:w-48 sm:ml-auto" @keyup.enter="fetchStockQuote" />
            <button @click="fetchStockQuote" class="text-[10px] px-3 py-1.5 rounded-lg bg-[#332314] text-amber-400 border border-amber-400/20 hover:bg-amber-500/20 transition">
              查询
            </button>
          </div>
          <div class="overflow-x-auto">
            <table class="web3-table min-w-[980px]">
              <thead>
                <tr>
                  <th>走势</th>
                  <th>代码</th>
                  <th>名称</th>
                  <th>最新价</th>
                  <th>涨跌幅</th>
                  <th>涨跌额</th>
                  <th>成交量(万)</th>
                  <th>成交额(亿)</th>
                  <th>振幅</th>
                  <th>换手率</th>
                  <th>市盈率</th>
                  <th>数据源</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="s in pagedStocks" :key="s.code || s.symbol" class="cursor-pointer hover:bg-[#22180c] transition"
                  @click="selectStock(s)">
                  <td class="py-1">
                    <svg v-if="sparkCache[s.symbol || s.code]" width="56" height="22" viewBox="0 0 56 22">
                      <polyline :points="sparkPoints(s.symbol || s.code)"
                        fill="none" stroke="#22d3ee" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round" />
                    </svg>
                    <span v-else class="text-amber-700/40 text-[10px]">--</span>
                  </td>
                  <td class="matrix-text text-xs">{{ s.code || s.symbol }}</td>
                  <td class="text-amber-100/80 text-xs">{{ s.name }}</td>
                  <td class="matrix-text text-sm font-bold">{{ s.price?.toFixed(2) || s.close?.toFixed(2) || '--' }}</td>
                  <td>
                    <div class="flex items-center gap-1.5">
                      <span :class="(s.change_pct || s.changePercent || 0) >= 0 ? 'text-red-400' : 'text-green-400'">
                        {{ (s.change_pct || s.changePercent || 0) >= 0 ? '+' : '' }}{{ (s.change_pct || s.changePercent || 0)?.toFixed(2) || '0.00' }}%
                      </span>
                      <div class="w-10 h-1 rounded-full bg-amber-800/20 overflow-hidden">
                        <div class="h-full rounded-full" :class="(s.change_pct || 0) >= 0 ? 'bg-red-400' : 'bg-green-400'"
                          :style="{ width: Math.min(Math.abs(s.change_pct || 0) / 3 * 100, 100) + '%', marginLeft: (s.change_pct || 0) >= 0 ? 'auto' : '0' }"></div>
                      </div>
                    </div>
                  </td>
                  <td :class="(s.change_amt || s.change || 0) >= 0 ? 'text-red-400' : 'text-green-400'">
                    {{ (s.change_amt || s.change || 0) >= 0 ? '+' : '' }}{{ (s.change_amt || s.change || 0)?.toFixed(2) || '0.00' }}
                  </td>
                  <td class="text-amber-100/60 matrix-text text-xs">{{ formatVol(s.amount_wan || s.volume) }}</td>
                  <td class="text-amber-100/60 matrix-text text-xs">{{ formatMoney(s.amount || s.turnover) }}</td>
                  <td class="text-amber-100/60">{{ s.amplitude_pct || s.amplitude || '--' }}%</td>
                  <td class="text-amber-100/60">{{ s.turnover_pct || s.turnoverRate || '--' }}%</td>
                  <td class="text-amber-100/60">{{ s.pe_ttm || '--' }}</td>
                  <td>
                    <span v-if="s.source" class="text-[10px] px-1.5 py-0.5 rounded bg-[#261b0e] border border-amber-800/20 text-amber-400/80">{{ s.source }}</span>
                    <span v-else class="text-amber-700/50 text-[10px]">--</span>
                  </td>
                  <td><button @click.stop="selectStock(s)" class="text-[10px] px-2 py-1 rounded bg-purple-500/10 text-purple-400 border border-purple-400/20 hover:bg-purple-500/20">详情</button></td>
                </tr>
                <tr v-if="!stockList.length">
                  <td colspan="13" class="text-center text-amber-700/50 py-8">暂无数据，点击查询获取行情</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div v-if="stockTotal > stockPageSize" class="mb-5">
          <Pagination v-model:page="stockPage" :page-size="stockPageSize" :total="stockTotal" />
        </div>

        <!-- Stock Detail Panel -->
        <div v-if="selectedStock" class="scroll-card overflow-hidden">
          <div class="p-4 border-b border-amber-800/20 flex items-center justify-between">
            <div>
              <h3 class="text-sm font-bold text-amber-100">{{ selectedStock.name }} ({{ selectedStock.code || selectedStock.symbol }})</h3>
              <div class="flex items-center gap-3 mt-1">
                <span class="text-lg font-black matrix-text" :class="(selectedStock.change_pct || 0) >= 0 ? 'text-red-400' : 'text-green-400'">
                  {{ selectedStock.price?.toFixed(2) || '--' }}
                </span>
                <span :class="['text-xs font-mono', (selectedStock.change_pct || 0) >= 0 ? 'text-red-400' : 'text-green-400']">
                  {{ (selectedStock.change_pct || 0) >= 0 ? '+' : '' }}{{ (selectedStock.change_pct || 0)?.toFixed(2) }}%
                </span>
              </div>
            </div>
            <button @click="selectedStock = null" class="text-amber-600/60 hover:text-amber-100 text-xs">关闭</button>
          </div>

          <!-- K-line Chart -->
          <div class="p-4 border-b border-amber-800/20">
            <div class="flex items-center gap-2 mb-3 flex-wrap">
              <button v-for="p in periods" :key="p.key"
                @click="fetchKline(selectedStock.symbol || selectedStock.code, p.key)"
                :class="['text-[10px] px-2.5 py-1 rounded-lg transition',
                  klinePeriod === p.key ? 'bg-amber-500/15 text-amber-400 border border-amber-400/20' : 'text-amber-600/60 hover:text-amber-200']">
                {{ p.label }}
              </button>
              <span v-if="klineSource" class="ml-auto text-[10px] px-2.5 py-1 rounded-lg bg-[#261b0e] text-amber-500/70 border border-amber-800/20">
                数据源: <span class="text-amber-400">{{ klineSource }}</span>
              </span>
            </div>
            <div ref="klineChartRef" class="w-full" style="height: 350px;"></div>
          </div>

          <!-- Stock Info Grid -->
          <div class="p-4 grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-3">
            <div v-for="info in stockInfoItems" :key="info.label" class="bg-[#1a1208] rounded-lg p-3 min-w-0">
              <div class="text-[10px] text-amber-600/60">{{ info.label }}</div>
              <div class="text-xs font-mono text-amber-100 mt-1 truncate" :title="info.value">{{ info.value }}</div>
            </div>
          </div>

          <!-- Tabs: 资金流向 / 龙虎榜 / 研报 -->
          <div class="p-4">
            <div class="flex gap-1 mb-3">
              <button @click="stockDetailTab = 'fund'" :class="['text-[10px] px-3 py-1.5 rounded-lg', stockDetailTab === 'fund' ? 'bg-[#332314] text-amber-400' : 'text-amber-600/60']">资金流向</button>
              <button @click="stockDetailTab = 'dragon'" :class="['text-[10px] px-3 py-1.5 rounded-lg', stockDetailTab === 'dragon' ? 'bg-[#332314] text-amber-400' : 'text-amber-600/60']">龙虎榜</button>
              <button @click="stockDetailTab = 'report'" :class="['text-[10px] px-3 py-1.5 rounded-lg', stockDetailTab === 'report' ? 'bg-[#332314] text-amber-400' : 'text-amber-600/60']">研报</button>
              <button @click="stockDetailTab = 'margin'" :class="['text-[10px] px-3 py-1.5 rounded-lg', stockDetailTab === 'margin' ? 'bg-[#332314] text-amber-400' : 'text-amber-600/60']">融资融券</button>
            </div>

            <div v-if="stockDetailTab === 'fund'">
              <FundFlowChart :code="selectedStock.code || selectedStock.symbol" />
            </div>
            <div v-if="stockDetailTab === 'dragon'">
              <DragonTigerTable :code="selectedStock.code || selectedStock.symbol" />
            </div>
            <div v-if="stockDetailTab === 'report'">
              <ReportsTable :code="selectedStock.code || selectedStock.symbol" />
            </div>
            <div v-if="stockDetailTab === 'margin'">
              <MarginTable :code="selectedStock.code || selectedStock.symbol" />
            </div>
          </div>
        </div>
      </div>

      <!-- ========== TAB: 策略管理 ========== -->
      <div v-if="activeTab === 'strategies'">
        <!-- Create Strategy Button -->
        <div class="mb-5">
          <button class="web3-btn text-xs !px-5 !py-2.5" @click="showCreateForm = true">
            + 创建策略
          </button>
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
              <textarea v-model="newStrategy.code" class="web3-input text-sm !min-h-[100px] font-mono" placeholder="def handle(data): ..."></textarea>
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
                <button class="text-[11px] px-2.5 py-1 rounded-lg bg-[#332314] text-amber-400 border border-amber-400/20 hover:bg-amber-500/20 transition" @click.stop="handleRunStrategy(item.id)">运行</button>
                <span class="text-[11px] text-purple-400 opacity-0 group-hover:opacity-100 transition flex items-center gap-1">
                  详情 <span>→</span>
                </span>
              </div>
            </div>
          </div>
        </div>
        <div v-if="!strategyList.length" class="py-10"><Loading /></div>
        <div class="mt-6" v-if="strategyTotal > 0">
          <Pagination v-model:page="strategyPage" :page-size="strategySize" :total="strategyTotal" @update:page="fetchStrategies" />
        </div>
      </div>

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
              <label class="text-[10px] text-amber-600/60 mb-1 block">策略ID</label>
              <select v-model="btForm.strategy_id" class="web3-input text-xs">
                <option value="bse_smallcap">小市值动量策略</option>
                <option value="ma_cross">均线交叉策略</option>
                <option value="grid">网格交易策略</option>
              </select>
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

          <!-- Performance Metrics -->
          <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-5 gap-4 mb-6">
            <div class="bg-gradient-to-br from-purple-500/10 to-blue-500/10 border border-amber-800/20 rounded-xl p-4 text-center">
              <div class="text-[10px] text-amber-600/60 mb-1">累计收益率</div>
              <div class="text-lg font-black text-green-400 matrix-text">{{ fmtBtPct(btMetrics.total_return) }}</div>
            </div>
            <div class="bg-gradient-to-br from-amber-500/10 to-orange-500/10 border border-amber-800/20 rounded-xl p-4 text-center">
              <div class="text-[10px] text-amber-600/60 mb-1">年化收益率</div>
              <div class="text-lg font-black text-amber-300 scroll-title">{{ fmtBtPct(btMetrics.annual_return) }}</div>
            </div>
            <div class="bg-gradient-to-br from-red-500/10 to-orange-500/10 border border-amber-800/20 rounded-xl p-4 text-center">
              <div class="text-[10px] text-amber-600/60 mb-1">最大回撤</div>
              <div class="text-lg font-black text-red-400 matrix-text">{{ fmtBtPct(btMetrics.max_drawdown) }}</div>
            </div>
            <div class="bg-gradient-to-br from-amber-500/10 to-yellow-500/10 border border-amber-800/20 rounded-xl p-4 text-center">
              <div class="text-[10px] text-amber-600/60 mb-1">夏普比率</div>
              <div class="text-lg font-black text-amber-400 matrix-text">{{ fmtBtNum(btMetrics.sharpe_ratio) }}</div>
            </div>
            <div class="bg-gradient-to-br from-green-500/10 to-emerald-500/10 border border-amber-800/20 rounded-xl p-4 text-center">
              <div class="text-[10px] text-amber-600/60 mb-1">胜率 / 交易数</div>
              <div class="text-lg font-black text-green-400 matrix-text">{{ fmtBtPct(btMetrics.win_rate) }}</div>
              <div class="text-[10px] text-amber-700/50">{{ btMetrics.trade_count ?? '--' }} 笔交易</div>
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
        </div>
      </div>

      <!-- ========== TAB: 新闻雷达 ========== -->
      <div v-if="activeTab === 'news'">
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
        <div class="scroll-card p-6 mb-6">
          <h3 class="text-sm font-bold text-amber-100 mb-4">持仓概览</h3>
          <div v-if="portfolioData.holdings?.length" class="space-y-3">
            <div v-for="h in portfolioData.holdings" :key="h.code" class="flex items-center justify-between p-4 rounded-xl bg-[#1a1208] border border-amber-800/15">
              <div>
                <p class="text-sm font-medium text-amber-100">{{ h.name }} <span class="text-amber-600/60 font-mono">{{ h.code }}</span></p>
                <p class="text-[11px] text-amber-600/60 mt-0.5">持仓 {{ h.shares }} 股 · 成本 ¥{{ h.costPrice }}</p>
              </div>
              <div class="text-right">
                <p class="text-sm font-bold" :class="(h.profitPct || 0) >= 0 ? 'text-green-400' : 'text-red-400'">
                  {{ (h.profitPct || 0) >= 0 ? '+' : '' }}{{ h.profitPct }}%
                </p>
                <p class="text-[11px] text-amber-600/60">市值 ¥{{ h.marketValue }}</p>
              </div>
            </div>
          </div>
          <div v-else-if="loadingPortfolio" class="flex items-center justify-center gap-2 py-8">
            <span class="h-4 w-4 rounded-full border-2 border-amber-800/25 border-t-web3-accent animate-spin"></span>
            <span class="text-xs text-amber-700/50">加载持仓数据...</span>
          </div>
          <div v-else class="py-8 text-center">
            <p class="text-xs text-amber-700/50 mb-3">暂无持仓数据</p>
            <button class="web3-btn text-xs" @click="loadPortfolio">加载示例组合</button>
          </div>
        </div>
      </div>

      <!-- ========== TAB: AI分析 ========== -->
      <div v-if="activeTab === 'ai'">
        <div class="scroll-card p-6">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-sm font-bold text-amber-100 flex items-center gap-2">
              <span class="text-amber-400">🤖</span> AI 量化助手
            </h3>
            <button @click="showAiConfig = !showAiConfig" class="text-[10px] px-3 py-1 rounded-lg border border-amber-800/20 text-amber-500/70 hover:text-amber-100 transition">
              {{ showAiConfig ? '收起设置' : '⚙ AI 模型设置' }}
            </button>
          </div>

          <!-- AI Config Panel -->
          <div v-if="showAiConfig" class="scroll-card-sm p-4 mb-4 space-y-3">
            <p class="text-[10px] text-amber-600/60">配置 OpenAI 兼容的模型接口（保存到本地），用于量化问答分析</p>
            <div class="grid grid-cols-1 md:grid-cols-3 gap-3">
              <div>
                <label class="text-[10px] text-amber-600/60 mb-1 block">Base URL</label>
                <input v-model="aiConfig.baseURL" class="web3-input text-xs font-mono" placeholder="如 http://127.0.0.1:8801/v1" />
              </div>
              <div>
                <label class="text-[10px] text-amber-600/60 mb-1 block">API Key</label>
                <input v-model="aiConfig.apiKey" type="password" class="web3-input text-xs font-mono" placeholder="sk-..." />
              </div>
              <div>
                <label class="text-[10px] text-amber-600/60 mb-1 block">模型</label>
                <input v-model="aiConfig.model" class="web3-input text-xs font-mono" placeholder="如 deepseek-chat" />
              </div>
            </div>
            <div class="flex justify-end">
              <button class="text-[10px] px-3 py-1 rounded-lg bg-[#332314] text-amber-400 border border-amber-400/20 hover:bg-amber-500/20 transition" @click="saveAiConfig">保存配置</button>
            </div>
          </div>

          <div class="space-y-3 max-h-[500px] overflow-y-auto mb-4 p-4 rounded-xl bg-[#1a1208] border border-amber-800/15">
            <div v-if="chatMessages.length" v-for="msg in chatMessages" :key="msg.id"
              :class="['flex gap-3', msg.role === 'user' ? 'justify-end' : 'justify-start']">
              <div :class="['max-w-[80%] p-3 rounded-xl text-sm', msg.role === 'user' ? 'bg-purple-500/15 text-purple-200 border border-purple-400/15' : 'bg-[#1a1208] text-amber-200/70 border border-amber-800/20']">
                <span v-if="msg.content" class="whitespace-pre-wrap">{{ msg.content }}</span>
                <span v-if="msg.streaming" class="inline-block w-1.5 h-4 bg-amber-400 animate-pulse align-middle ml-0.5"></span>
              </div>
            </div>
            <div v-else class="text-center text-xs text-amber-700/50 py-8">
              输入股票代码或量化问题，AI助手将为您分析
            </div>
          </div>
          <div class="flex gap-2">
            <input v-model="chatInput" class="web3-input flex-1 text-sm" placeholder="如: 分析贵州茅台(600519)的投资价值..." @keydown.enter="sendChat" :disabled="chatLoading" />
            <button class="web3-btn text-sm" :disabled="chatLoading" @click="sendChat">
              {{ chatLoading ? '思考中...' : '发送' }}
            </button>
          </div>
        </div>
      </div>

      <!-- ========== TAB: AI深度投研 (TradingAgents-Astock) ========== -->
      <div v-if="activeTab === 'deepai'">
        <div class="scroll-card p-6">
          <div class="flex items-center justify-between mb-5">
            <h3 class="text-sm font-bold text-amber-100 flex items-center gap-2">
              <span class="text-amber-400">🧠</span> AI 深度投研 · TradingAgents-Astock
            </h3>
            <span class="text-[10px] text-amber-700/50">7 AI Analysts → Bull/Bear Debate → Portfolio Manager</span>
          </div>

          <!-- Stock Input + Model Selection -->
          <div class="flex flex-wrap gap-2 mb-4">
            <select v-model="deepaiModel" class="web3-input text-xs !w-40 flex-shrink-0">
              <option value="">选择AI模型</option>
              <option value="gpt-4o">GPT-4o</option>
              <option value="gpt-4-turbo">GPT-4 Turbo</option>
              <option value="claude-3.5-sonnet">Claude 3.5 Sonnet</option>
              <option value="claude-3-opus">Claude 3 Opus</option>
              <option value="deepseek-chat">DeepSeek V3</option>
              <option value="qwen-turbo">通义千问</option>
              <option value="gemini-pro">Gemini Pro</option>
            </select>
            <input v-model="deepaiStockCode" class="web3-input flex-1 text-sm font-mono min-w-[160px]" placeholder="输入A股代码，如 600519" maxlength="10" />
            <input v-model="deepaiStockName" class="web3-input w-full sm:w-28 text-sm" placeholder="名称" />
            <button class="web3-btn text-sm" :disabled="!deepaiStockCode || deepaiRunning" @click="runDeepAnalysis">
              {{ deepaiRunning ? '分析中...' : '深度分析' }}
            </button>
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
      <div v-if="activeTab === 'dashboard'">
        <!-- 统计概览 -->
        <div class="grid grid-cols-3 sm:grid-cols-6 gap-3 mb-6">
          <div class="scroll-card p-3 text-center">
            <div class="text-lg font-black text-amber-100 scroll-title">{{ dashLimitStats.total }}</div>
            <div class="text-[10px] text-amber-600/60 font-serif">全部</div>
          </div>
          <div class="scroll-card p-3 text-center">
            <div class="text-lg font-black text-red-400 scroll-title">{{ dashLimitStats.up }}</div>
            <div class="text-[10px] text-amber-600/60 font-serif">上涨</div>
          </div>
          <div class="scroll-card p-3 text-center">
            <div class="text-lg font-black text-green-400 scroll-title">{{ dashLimitStats.down }}</div>
            <div class="text-[10px] text-amber-600/60 font-serif">下跌</div>
          </div>
          <div class="limit-up-card p-3 text-center">
            <div class="text-lg font-black text-red-400 scroll-title">{{ dashLimitStats.limitUp }}</div>
            <div class="text-[10px] text-red-400/60 font-serif">涨停</div>
          </div>
          <div class="scroll-card p-3 text-center">
            <div class="text-lg font-black text-green-400 scroll-title">{{ dashLimitStats.limitDown }}</div>
            <div class="text-[10px] text-amber-600/60 font-serif">跌停</div>
          </div>
          <div class="scroll-card p-3 text-center">
            <div class="text-lg font-black text-amber-300 scroll-title">{{ dashLimitStats.st }}</div>
            <div class="text-[10px] text-amber-600/60 font-serif">ST</div>
          </div>
        </div>

        <div class="grid grid-cols-1 lg:grid-cols-3 gap-5">
          <!-- 左侧：AI 推荐 + 涨停板 -->
          <div class="space-y-5">
            <!-- AI 推荐优质股 -->
            <div class="recommend-card overflow-hidden">
              <div class="p-4 border-b border-green-400/10">
                <h3 class="text-sm font-bold text-green-300 font-serif flex items-center gap-2">
                  <span class="w-2 h-2 rounded-full bg-green-400 animate-pulse"></span>
                  AI 量化推荐
                </h3>
                <p class="text-[10px] text-amber-600/50 mt-1">涨幅2-9.5% · 换手3-25% · PE&lt;80</p>
              </div>
              <div class="p-3 max-h-[400px] overflow-y-auto">
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

            <!-- 涨停板 / 连板 -->
            <div class="limit-up-card overflow-hidden">
              <div class="p-4 border-b border-red-400/10">
                <h3 class="text-sm font-bold text-red-300 font-serif flex items-center gap-2">
                  <span class="w-2 h-2 rounded-full bg-red-400"></span>
                  涨停板 · 连板
                </h3>
                <p class="text-[10px] text-amber-600/50 mt-1">涨幅≥9.5%（创业板/科创板≥19.5%）</p>
              </div>
              <div class="p-3 max-h-[300px] overflow-y-auto">
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
          </div>

          <!-- 右侧：全部股票数据表 -->
          <div class="lg:col-span-2 scroll-card overflow-hidden">
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
                </select>
              </div>
            </div>
            <div class="overflow-x-auto max-h-[600px] overflow-y-auto">
              <table class="web3-table text-xs min-w-[700px]">
                <thead class="sticky top-0 bg-[#2d1e0f]">
                  <tr>
                    <th class="text-left">代码</th>
                    <th class="text-left">名称</th>
                    <th class="text-right">现价</th>
                    <th class="text-right">涨跌%</th>
                    <th class="text-right">成交额</th>
                    <th class="text-right">换手%</th>
                    <th class="text-right">振幅%</th>
                    <th class="text-right">PE</th>
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
                    <td class="text-right font-mono text-amber-100/60">{{ s.turnover_pct?.toFixed(1) || '--' }}</td>
                    <td class="text-right font-mono text-amber-100/60">{{ s.amplitude_pct?.toFixed(1) || '--' }}</td>
                    <td class="text-right font-mono text-amber-100/60">{{ s.pe_ttm?.toFixed(0) || '--' }}</td>
                  </tr>
                  <tr v-if="!dashPagedStocks.length">
                    <td colspan="8" class="text-center text-amber-700/40 py-8">暂无数据</td>
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
            <button class="web3-btn text-xs !px-4" @click.stop="handleRunStrategy(strategyDetail.id)">
              运行策略
            </button>
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
// ====================================================
import { ref, reactive, computed, onMounted, nextTick, watch, onBeforeUnmount } from 'vue'
import { getQuantList, getStockKlineVR, getIndices, getMarketEmotion, getConceptHot, getIndustryList, getRadarData, getGlobalIndices, getPortfolioData, createStrategy, runStrategy, runPyBacktest, getPyBacktestStatus, getPyBacktestResult, getPyQuotes, getPyQuote, getPyKline, getPySearch, getPyStockInfo, getPySectors, getPySparklines, getPyAiAnalyze, getPyNews, getPyDashboard, getPyPortfolio } from '@/api/quant'
import { getIndexQuotes } from '@/api/akshare'
import Pagination from '@/components/common/Pagination.vue'
import Modal from '@/components/common/Modal.vue'
import * as echarts from 'echarts'
import { useToastStore } from '@/stores/modules/toast'
import FundFlowChart from './components/FundFlowChart.vue'
import DragonTigerTable from './components/DragonTigerTable.vue'
import ReportsTable from './components/ReportsTable.vue'
import MarginTable from './components/MarginTable.vue'

const toast = useToastStore()

// API 响应缓存（避免重复请求同一接口，60 秒 TTL）
const _apiCache = new Map()
function cachedPy(fn, cacheKey, ttlMs = 60000) {
  const now = Date.now()
  const cached = _apiCache.get(cacheKey)
  if (cached && (now - cached.ts) < ttlMs) return Promise.resolve(cached.data)
  const p = fn().then(res => { _apiCache.set(cacheKey, { data: res, ts: Date.now() }); return res })
  _apiCache.set(cacheKey, { data: p, ts: now })
  return p
}
function cachedDashboard() { return cachedPy(getPyDashboard, 'dashboard') }
function cachedQuotes() { return cachedPy(getPyQuotes, 'quotes') }

const tabs = [
  { key: 'dashboard', label: '智能看板' },
  { key: 'overview', label: '市场概览' },
  { key: 'hot', label: '市场热点' },
  { key: 'stocks', label: 'A股行情' },
  { key: 'strategies', label: '策略管理' },
  { key: 'portfolio', label: '组合管理' },
  { key: 'backtest', label: '回测分析' },
  { key: 'news', label: '新闻雷达' },
  { key: 'ai', label: 'AI分析' },
  { key: 'deepai', label: 'AI深度投研' },
]
const activeTab = ref('dashboard')

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
const searchCode = ref('')
const selectedStock = ref(null)
const stockPage = ref(1)
const stockPageSize = ref(30)
const stockTotal = ref(0)
const sparkCache = ref({})
// 计算行情涨跌家数统计
const breadth = computed(() => {
  const total = stockList.value.length
  const up = stockList.value.filter(s => (s.change_pct || s.changePercent || 0) > 0).length
  return { total, up, down: total - up }
})
// 按分页参数截取当前页行情列表
const pagedStocks = computed(() => {
  const start = (stockPage.value - 1) * stockPageSize.value
  return stockList.value.slice(start, start + stockPageSize.value)
})
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

const filteredDashStocks = computed(() => {
  let list = stockList.value
  const q = dashSearch.value.trim().toLowerCase()
  if (q) list = list.filter(s => (s.code || '').includes(q) || (s.name || '').toLowerCase().includes(q))
  if (dashFilter.value === 'up') list = list.filter(s => (s.change_pct || 0) > 0)
  else if (dashFilter.value === 'down') list = list.filter(s => (s.change_pct || 0) < 0)
  else if (dashFilter.value === 'limitUp') list = list.filter(s => (s.change_pct || 0) >= 9.5)
  else if (dashFilter.value === 'limitDown') list = list.filter(s => (s.change_pct || 0) <= -9.5)
  else if (dashFilter.value === 'st') list = list.filter(s => (s.name || '').includes('ST'))
  const [key, dir] = dashSort.value.split('_')
  list = [...list].sort((a, b) => {
    let va = key === 'change' ? (a.change_pct || 0) : key === 'price' ? (a.price || 0) : key === 'amount' ? (a.amount || 0) : (a.change_pct || 0)
    let vb = key === 'change' ? (b.change_pct || 0) : key === 'price' ? (b.price || 0) : key === 'amount' ? (b.amount || 0) : (b.change_pct || 0)
    return dir === 'desc' ? vb - va : va - vb
  })
  return list
})
const dashPagedStocks = computed(() => {
  const s = (dashPage.value - 1) * dashPageSize.value
  return filteredDashStocks.value.slice(s, s + dashPageSize.value)
})
const dashTotalPages = computed(() => Math.ceil(filteredDashStocks.value.length / dashPageSize.value))
const klinePeriod = ref('daily')
const klineSource = ref('')
const periods = [
  { key: '1m', label: '分时' },
  { key: '5m', label: '5分' },
  { key: '15m', label: '15分' },
  { key: '30m', label: '30分' },
  { key: '60m', label: '60分' },
  { key: 'daily', label: '日线' },
  { key: 'weekly', label: '周线' },
  { key: 'monthly', label: '月线' },
]
const lastUpdate = ref('')
const stockDetailTab = ref('fund')
const stockInfoItems = ref([])

// Strategy data
const strategyList = ref([])
const strategyPage = ref(1)
const strategySize = ref(9)
const strategyTotal = ref(0)
const showStrategyModal = ref(false)
const strategyDetail = ref(null)
const showCreateForm = ref(false)
const creating = ref(false)
const createError = ref('')
const newStrategy = reactive({ name: '', riskLevel: 'MEDIUM', description: '', code: '' })
// 策略名非空时允许创建
const canCreateStrategy = computed(() => newStrategy.name.trim())

// News
const newsItems = ref([])

// Portfolio
const portfolioData = ref({ holdings: [], summary: {} })
const loadingPortfolio = ref(false)

// AI Chat
const chatMessages = ref([])
const chatInput = ref('')
const chatLoading = ref(false)
const showAiConfig = ref(false)
const aiConfig = ref({ baseURL: '', apiKey: '', model: '' })

// 从 localStorage 读取 AI 模型配置
function loadAiConfig() {
  try {
    const saved = JSON.parse(localStorage.getItem('quant_llm_config') || '{}')
    aiConfig.value = { baseURL: saved.baseURL || '', apiKey: saved.apiKey || '', model: saved.model || '' }
  } catch {}
}

// 保存 AI 模型配置到 localStorage
function saveAiConfig() {
  localStorage.setItem('quant_llm_config', JSON.stringify(aiConfig.value))
  toast.success('AI 模型配置已保存')
}

// ===== AI Chat (streaming via Vibe-Research /api/chat NDJSON) =====
// 发送聊天消息到 Vibe-Research 服务（/vr/chat，8900），流式输出回复
async function sendChat() {
  if (!chatInput.value.trim() || chatLoading.value) return
  const msg = chatInput.value.trim()
  chatMessages.value.push({ id: Date.now(), role: 'user', content: msg })
  chatInput.value = ''
  chatLoading.value = true

  const replyId = Date.now() + 1
  chatMessages.value.push({ id: replyId, role: 'assistant', content: '', streaming: true })

  const saved = aiConfig.value.baseURL && aiConfig.value.model
  if (!saved) {
    chatMessages.value.find(m => m.id === replyId).content = '请先在「⚙ AI 模型设置」填写 Base URL 与模型名称，然后重试。'
    chatMessages.value.find(m => m.id === replyId).streaming = false
    chatLoading.value = false
    return
  }

  try {
    const resp = await fetch('/vr/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        messages: [{ role: 'user', content: msg }],
        context: '当前页面: 量化交易平台。请用中文回答，关注A股行情与量化分析。',
        llm: {
          provider: '',
          baseURL: aiConfig.value.baseURL,
          apiKey: aiConfig.value.apiKey,
          model: aiConfig.value.model,
        },
      }),
    })

    if (!resp.ok) {
      let detail = `请求失败 (HTTP ${resp.status})`
      try {
        const err = await resp.json()
        if (err?.detail) detail = String(err.detail)
      } catch {}
      throw new Error(detail)
    }

    const reader = resp.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let done = false

    while (!done) {
      const { value, done: streamDone } = await reader.read()
      done = streamDone
      buffer += decoder.decode(value || new Uint8Array(), { stream: !done })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        const trimmed = line.trim()
        if (!trimmed) continue
        let ev
        try { ev = JSON.parse(trimmed) } catch { continue }
        const target = chatMessages.value.find(m => m.id === replyId)
        if (!target) continue
        if (ev.type === 'delta' && ev.content) {
          target.content += ev.content
        } else if (ev.type === 'tool' && ev.name) {
          target.content += (target.content ? '\n' : '') + `[工具: ${ev.name}]`
        } else if (ev.type === 'error') {
          target.content += (target.content ? '\n' : '') + `⚠ ${ev.message || '分析出错'}`
        }
      }
    }
    const target = chatMessages.value.find(m => m.id === replyId)
    if (target) target.streaming = false
    if (!target || !target.content.trim()) {
      if (target) target.content = '分析完成，但未返回有效内容。'
    }
  } catch (e) {
    const target = chatMessages.value.find(m => m.id === replyId)
    if (target) {
      target.streaming = false
      target.content = '分析失败: ' + (e.message || '网络错误') + '。请检查 Vibe-Research 服务 (8900) 与模型配置。'
    }
  } finally {
    chatLoading.value = false
  }
}

// ===== TradingAgents-Astock Deep AI Analysis =====
const deepaiStockCode = ref('')
const deepaiStockName = ref('')
const deepaiModel = ref('')
const deepaiRunning = ref(false)
const deepaiStep = ref('')
const deepaiProgress = ref(0)
const deepaiReport = ref(null)
const deepaiVerdict = ref('')
const deepaiConfidence = ref(0)
const analystReports = ref([])
const debateLog = ref([])

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
  deepaiProgress.value = 5

  const symbol = deepaiStockCode.value.trim()

  try {
    deepaiStep.value = '提交多 Agent 分析任务...'
    const res = await getPyAiAnalyze(symbol)
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
    if (db.bull_score > 0 || db.bear_score > 0) {
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
const klineChartRef = ref(null)
const equityChartRef = ref(null)
const drawdownChartRef = ref(null)
const heatmapChartRef = ref(null)
const distChartRef = ref(null)
const pieChartRef = ref(null)

let klineChart = null

// 数值格式化为万/亿可读形式
function formatVol(v) {
  if (!v && v !== 0) return '--'
  if (v > 10000) return (v / 10000).toFixed(1) + '万'
  if (v > 100000000) return (v / 100000000).toFixed(2) + '亿'
  return v.toString()
}

// 金额格式化为亿/元可读形式
function formatMoney(v) {
  if (!v && v !== 0) return '--'
  if (v > 100000000) return (v / 100000000).toFixed(2) + '亿'
  return v.toString()
}

// 情绪指标 key 转为中文名称
function formatEmotionKey(key) {
  const map = { sentiment: '综合情绪', fear: '恐慌', greed: '贪婪', volume: '成交量比', up: '上涨家数', down: '下跌家数', limit_up: '涨停', limit_down: '跌停' }
  return map[key] || key
}

// 情绪指标对应的进度条颜色类
function getEmotionColor(key) {
  if (key === 'fear' || key === 'limit_down' || key === 'down') return 'bg-red-400'
  if (key === 'greed' || key === 'limit_up' || key === 'up') return 'bg-green-400'
  return 'bg-amber-400'
}

// 情绪指标对应的文本颜色类
function getEmotionTextColor(key) {
  if (key === 'fear' || key === 'limit_down' || key === 'down') return 'text-red-400'
  if (key === 'greed' || key === 'limit_up' || key === 'up') return 'text-green-400'
  return 'text-amber-400'
}

// 按指标类型计算情绪进度条宽度百分比
function getEmotionWidth(key, val) {
  if (typeof val !== 'number') return 50
  if (key === 'sentiment') return Math.min(Math.max(val, 0), 100)
  if (key === 'volume') return Math.min(Math.max(val * 25, 5), 100)
  return Math.min(Math.max((val / 200) * 100, 5), 100)
}

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
      stockPage.value = 1
      prefetchSparklines(stockList.value)
    }

    if (m.updated_at) lastUpdate.value = m.updated_at
  } catch (e) { console.warn('loadOverview fail', e) }
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
// 根据缓存生成迷你走势图的坐标点串
function sparkPoints(code) {
  const arr = sparkCache.value[code]
  if (!arr || arr.length < 2) return ''
  const w = 56, h = 22, pad = 2
  const min = Math.min(...arr), max = Math.max(...arr)
  const span = (max - min) || 1
  return arr.map((v, i) => {
    const x = pad + (i * (w - pad * 2)) / (arr.length - 1)
    const y = h - pad - ((v - min) / span) * (h - pad * 2)
    return `${x.toFixed(1)},${y.toFixed(1)}`
  }).join(' ')
}

// 缓存股票近 30 日收盘价，用于迷你走势图
function cacheSpark(code, closes) {
  if (!code || !closes?.length) return
  sparkCache.value[code] = closes.slice(-30)
}

// 加载同花顺行业板块涨跌排行（pyquant 9006）
async function loadSectors() {
  try {
    const res = await getPySectors()
    const d = res.data || {}
    const arr = typeof d === 'object' ? Object.entries(d).map(([name, v]) => ({ name, change_pct: Number(v) || 0 })) : []
    arr.sort((a, b) => b.change_pct - a.change_pct)
    thsSectors.value = arr.slice(0, 24)
    thsSectorSource.value = res.source || '同花顺'
  } catch (e) { console.warn('sectors fail', e) }
}

// loadHotStocks 已合并到 loadOverview

// 批量预取列表股票的迷你走势数据
async function prefetchSparklines(list) {
  const syms = list.map(s => (s.symbol || (s.code ? `sh${s.code}` : ''))).filter(Boolean)
  if (!syms.length) return
  try {
    const res = await getPySparklines(syms)
    const d = res.data || {}
    for (const [sym, closes] of Object.entries(d)) {
      if (Array.isArray(closes) && closes.length) cacheSpark(sym, closes)
    }
  } catch (e) { console.warn('sparklines batch fail', e) }
}

// 按代码或关键字搜索并展示行情列表
async function fetchStockQuote() {
  if (!searchCode.value.trim()) return
  try {
    const q = searchCode.value.trim()
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
    console.warn('quote fail', e)
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
    amplitude_pct: d.amplitude,
    turnover_pct: d.turnover_rate,
    pe_ttm: d.pe,
    last_close: (d.price || 0) - (d.change || 0),
    source: d.source || '腾讯',
  }
}

// 选中股票并加载其 K 线与详情信息
function selectStock(s) {
  selectedStock.value = s
  const code = s.symbol || (s.code && (s.code.startsWith('sh') || s.code.startsWith('sz') ? s.code : ''))
  fetchKline(code || `sh${s.code}`, 'daily')
  fetchStockInfo(code || `sh${s.code}`)
}

// 拉取 K 线数据并渲染蜡烛图、成交量与均线（ECharts）
async function fetchKline(code, period) {
  klinePeriod.value = period
  const sym = code.startsWith('sh') || code.startsWith('sz') || code.startsWith('bj') ? code : `sh${code}`
  try {
    const daysMap = { '1m': 3, '5m': 5, '15m': 5, '30m': 10, '60m': 15, daily: 120, weekly: 104, monthly: 60 }
    const res = await getPyKline(sym, daysMap[period] || 120, period)
    const data = res.data
    if (!data || !data.length) return
    klineSource.value = (data[0] && data[0].source) || ''
    if (period === 'daily') cacheSpark(sym, data.map(d => Number(d.close) || 0))

    await nextTick()
    if (!klineChartRef.value) return
    if (!klineChart) klineChart = echarts.init(klineChartRef.value)
    klineChart.clear()

    const dates = data.map(d => d.date || d.datetime)
    const ohlc = data.map(d => [d.open, d.close, d.low, d.high])
    const volumes = data.map(d => d.volume || 0)
    const closes = data.map(d => Number(d.close) || 0)

    function ma(n) {
      return closes.map((_, i) => {
        if (i < n - 1) return null
        let s = 0
        for (let j = i - n + 1; j <= i; j++) s += closes[j]
        return +(s / n).toFixed(2)
      })
    }

    const colors = data.map(d => (d.close >= d.open) ? '#ef4444' : '#22c55e')
    const maSeries = []
    if (closes.length >= 5) {
      const configs = [
        { n: 5, name: 'MA5', color: '#f59e0b', dash: [4, 2] },
        { n: 10, name: 'MA10', color: '#3b82f6', dash: [4, 2] },
        { n: 20, name: 'MA20', color: '#a855f7', dash: [6, 2] },
        { n: 60, name: 'MA60', color: '#ec4899', dash: [2, 3] },
      ]
      for (const c of configs) {
        if (closes.length < c.n) continue
        maSeries.push({
          name: c.name, type: 'line', data: ma(c.n), xAxisIndex: 0, yAxisIndex: 0,
          smooth: true, showSymbol: false, lineStyle: { width: 1, color: c.color, type: c.dash },
          itemStyle: { color: c.color },
        })
      }
    }

    klineChart.setOption({
      backgroundColor: 'transparent',
      tooltip: { trigger: 'axis', axisPointer: { type: 'cross' }, backgroundColor: 'rgba(8,8,18,0.92)', borderColor: 'rgba(255,255,255,0.08)', textStyle: { color: '#ccc', fontSize: 10 } },
      legend: { top: 2, right: 6, textStyle: { color: '#888', fontSize: 10 }, itemWidth: 12, itemHeight: 8, data: maSeries.map(s => s.name) },
      grid: [{ left: '8%', right: '8%', top: '16%', bottom: '20%' }, { left: '8%', right: '8%', top: '75%', bottom: '10%' }],
      xAxis: [{ type: 'category', data: dates, gridIndex: 0, axisLine: { lineStyle: { color: '#333' } }, axisLabel: { color: '#666', fontSize: 10 } }, { type: 'category', gridIndex: 1, data: dates, axisLine: { lineStyle: { color: '#333' } }, axisLabel: { show: false } }],
      yAxis: [{ type: 'value', gridIndex: 0, scale: true, splitLine: { lineStyle: { color: 'rgba(255,255,255,0.04)' } }, axisLabel: { color: '#666', fontSize: 10 } }, { type: 'value', gridIndex: 1, scale: true, splitLine: { show: false }, axisLabel: { show: false } }],
      series: [
        {
          type: 'candlestick',
          xAxisIndex: 0, yAxisIndex: 0,
          data: ohlc,
          itemStyle: { color: '#ef4444', color0: '#22c55e', borderColor: '#ef4444', borderColor0: '#22c55e' },
        },
        {
          type: 'bar',
          xAxisIndex: 1, yAxisIndex: 1,
          data: volumes,
          itemStyle: { color: colors },
        },
        ...maSeries,
      ],
      dataZoom: [{ type: 'inside', xAxisIndex: [0, 1] }, { type: 'slider', xAxisIndex: [0, 1], height: 16, bottom: 0, borderColor: 'transparent', fillerColor: 'rgba(0,255,255,0.08)', handleStyle: { color: '#0ff' }, textStyle: { color: '#666' } }],
    }, true)
  } catch (e) {
    console.warn('kline fail', e)
  }
}

// 拉取股票技术指标并组装详情条目
async function fetchStockInfo(code) {
  try {
    const sym = code.startsWith('sh') || code.startsWith('sz') || code.startsWith('bj') ? code : `sh${code}`
    const res = await getPyStockInfo(sym).catch(() => null)
    const info = res?.data?.quote || {}
    const ind = res?.data?.indicators || {}
    const f = (v, nd = 2) => (v === undefined || v === null || isNaN(v)) ? '--' : Number(v).toFixed(nd)
    stockInfoItems.value = [
      { label: '今开', value: f(info.open) },
      { label: '昨收', value: f((info.price || 0) - (info.change || 0)) },
      { label: '最高', value: f(info.high) },
      { label: '最低', value: f(info.low) },
      { label: '振幅', value: info.amplitude != null ? f(info.amplitude) + '%' : '--' },
      { label: '换手率', value: info.turnover_rate != null ? f(info.turnover_rate) + '%' : '--' },
      { label: '市盈率(TTM)', value: f(info.pe) },
      { label: '市净率', value: f(info.pb) },
      { label: 'MA5 / MA10', value: ind.ma5 != null ? `${f(ind.ma5)} / ${f(ind.ma10)}` : '--' },
      { label: 'MA20 / MA60', value: ind.ma20 != null ? `${f(ind.ma20)} / ${f(ind.ma60)}` : '--' },
      { label: 'RSI(14)', value: f(ind.rsi) },
      { label: 'MACD DIF', value: ind.macd?.dif != null ? f(ind.macd.dif, 3) : '--' },
      { label: 'MACD DEA', value: ind.macd?.dea != null ? f(ind.macd.dea, 3) : '--' },
      { label: 'MACD 柱', value: ind.macd?.hist != null ? f(ind.macd.hist, 3) : '--' },
      { label: '总市值(亿)', value: info.market_cap ? f(info.market_cap / 1e8) + '亿' : '--' },
      { label: '成交额(亿)', value: info.turnover ? f(info.turnover / 1e8) + '亿' : '--' },
      { label: '数据源', value: info.source || 'akshare' },
    ]
  } catch (e) { console.warn('stock info fail', e) }
}

// ===== Strategies =====
// 分页加载策略列表
async function fetchStrategies() {
  try {
    const res = await getQuantList({ page: strategyPage.value, size: strategySize.value })
    const d = res.data || {}
    strategyList.value = d.records || []
    strategyTotal.value = d.total || (d.records?.length || 0) * 10
  } catch (e) {
    console.warn('strategies fail', e)
  }
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
  } catch (e) {
    createError.value = '创建失败: ' + (e.message || '网络错误')
  } finally { creating.value = false }
}

// 触发运行指定策略
async function handleRunStrategy(id) {
  try {
    await runStrategy(id)
    toast.success('策略运行已启动')
  } catch { toast.error('策略运行失败') }
}

// ===== News =====
// 加载新闻雷达列表（最多 20 条）
async function loadNews() {
  try {
    const res = await getPyNews(20)
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
}

// ===== Backtest =====
const btForm = reactive({ strategy_id: 'bse_smallcap', initial_cash: 1000000, stop_loss: -0.10, max_position: 0.05, days: 120 })
const btSymbols = ref('sh600519,sz000858')
const btRunning = ref(false)
const btStatus = ref('')
const btError = ref(false)
const btResult = ref(null)

const btMetrics = computed(() => {
  if (btResult.value) {
    return {
      total_return: btResult.value.total_return ?? '--',
      annual_return: btResult.value.annual_return ?? '--',
      max_drawdown: btResult.value.max_drawdown ?? '--',
      sharpe_ratio: btResult.value.sharpe_ratio ?? '--',
      win_rate: btResult.value.win_rate ?? '--',
      trade_count: btResult.value.trade_count ?? '--',
    }
  }
  return {
    total_return: '--', annual_return: '--', max_drawdown: '--',
    sharpe_ratio: '--', win_rate: '--', trade_count: '--',
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
    const res = await runPyBacktest({ ...btForm, symbols })
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

    if (result && (result.equity_curve || result.final_value || result.total_return !== undefined)) {
      btResult.value = result
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
  const eq = btResult.value?.equity_curve || []
  const bench = btResult.value?.benchmark_curve || []
  const hasEq = eq.length > 1

  // Equity Curve (real backtest data)
  if (equityChartRef.value) {
    const c1 = echarts.init(equityChartRef.value)
    chartInstances.push(c1)
    const days = eq.map((_, i) => `T+${i}`)
    c1.setOption({
      backgroundColor: 'transparent',
      legend: { data: ['策略净值', '基准净值'], textStyle: { color: '#666', fontSize: 10 }, top: 5 },
      grid: { left: 50, right: 20, top: 35, bottom: 25 },
      xAxis: { type: 'category', data: days, axisLine: { lineStyle: { color: '#333' } }, axisLabel: { color: '#666', fontSize: 9 } },
      yAxis: { type: 'value', splitLine: { lineStyle: { color: 'rgba(255,255,255,0.04)' } }, axisLabel: { color: '#666', fontSize: 9 } },
      series: [
        { name: '策略净值', type: 'line', data: eq, smooth: true, lineStyle: { color: '#0ff', width: 1.5 }, symbol: 'none', showSymbol: false },
        { name: '基准净值', type: 'line', data: bench.length ? bench : [], smooth: true, lineStyle: { color: hasEq ? '#a855f7' : 'transparent', width: 1.5, type: 'dashed' }, symbol: 'none', showSymbol: false },
      ],
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

  // Monthly Heatmap — 真实净值曲线按日收益聚合（无日期信息时显示空态）
  if (heatmapChartRef.value) {
    const c3 = echarts.init(heatmapChartRef.value)
    chartInstances.push(c3)
    if (hasEq) {
      const rets = eq.slice(1).map((v, i) => ((v - eq[i]) / eq[i]) * 100)
      const months = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']
      const years = ['回测期']
      const heatmapData = months.map((m, i) => {
        const chunk = rets.filter((_, j) => j % 12 === i)
        const avg = chunk.length ? chunk.reduce((a, b) => a + b, 0) / chunk.length : 0
        return [i, 0, +avg.toFixed(1)]
      })
      c3.setOption({
        backgroundColor: 'transparent',
        grid: { left: 50, right: 20, top: 20, bottom: 25 },
        xAxis: { type: 'category', data: months, axisLine: { lineStyle: { color: '#333' } }, axisLabel: { color: '#666', fontSize: 9 } },
        yAxis: { type: 'category', data: years, axisLine: { lineStyle: { color: '#333' } }, axisLabel: { color: '#666', fontSize: 9 } },
        visualMap: { min: -10, max: 15, calculable: true, orient: 'horizontal', left: 'center', bottom: 0, inRange: { color: ['#22c55e', '#fbbf24', '#ef4444'] }, textStyle: { color: '#666', fontSize: 9 } },
        series: [{ type: 'heatmap', data: heatmapData, label: { show: true, fontSize: 9, color: '#fff' }, itemStyle: { borderColor: '#111', borderWidth: 1 } }],
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
  await Promise.allSettled([loadOverview(), loadSectors(), fetchStrategies(), loadNews()])
  lastUpdate.value = new Date().toLocaleTimeString()
}

// ===== Lifecycle =====
onMounted(async () => {
  await loadOverview()
  await Promise.allSettled([loadSectors(), loadNews(), loadDashboardData()])
  nextTick(() => renderBacktestCharts())
  loadAiConfig()
  lastUpdate.value = new Date().toLocaleTimeString()
})

watch(activeTab, async (tab) => {
  if (tab === 'dashboard') {
    await loadDashboardData()
  }
  if (tab === 'backtest') {
    await nextTick()
    renderBacktestCharts()
  }
  if (tab === 'stocks' && !stockList.value.length) {
    loadOverview()
  }
  if (tab === 'hot' && !hotConcepts.value.length) {
    await Promise.allSettled([loadOverview(), loadSectors()])
  }
  if (tab === 'strategies' && !strategyList.value.length) {
    fetchStrategies()
  }
  if (tab === 'news' && !newsItems.value.length) {
    loadNews()
  }
  if (tab === 'portfolio' && !portfolioData.value.holdings?.length) {
    loadPortfolio()
  }
})

let chartInstances = []
window.addEventListener('resize', () => {
  chartInstances.forEach(c => c?.resize())
  klineChart?.resize()
})

// Cleanup charts on unmount
onBeforeUnmount(() => {
  chartInstances.forEach(c => { try { c?.dispose() } catch {} })
  try { klineChart?.dispose() } catch {}
})
</script>

<style scoped>
/* ============================================================
   量化终端 — 深色金融行情风格（Deep-Finance / Trading Terminal）
   通过 scoped + 属性选择器家族命中，统一覆盖模板中海量的 amber/棕 工具类，
   无需改动 2000+ 行模板。涨跌（红涨绿跌）、涨停、AI推荐等语义色予以保留。
   主色板：深蓝黑 #060a12 / 夜色 #0c1422 / 钢蓝描边 / 霓虹青 #22d3ee + 翡翠绿 #34d399
   ============================================================ */
.scroll-card {
  background: linear-gradient(150deg, #0c1422 0%, #0a111d 55%, #0d1726 100%);
  border: 1px solid rgba(120, 170, 220, 0.14);
  border-radius: 12px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.35);
}
.scroll-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 85% 0%, rgba(34, 211, 238, 0.06) 0%, transparent 55%),
    radial-gradient(ellipse at 10% 100%, rgba(52, 211, 153, 0.05) 0%, transparent 55%);
  pointer-events: none;
}
.scroll-card::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(130deg, transparent 35%, rgba(120, 170, 220, 0.05) 45%, transparent 55%);
  pointer-events: none;
}
.scroll-card:hover {
  border-color: rgba(34, 211, 238, 0.45);
  box-shadow: 0 10px 36px rgba(0, 0, 0, 0.45), 0 0 22px rgba(34, 211, 238, 0.10);
  background: linear-gradient(150deg, #0e192a 0%, #0b1422 55%, #0f1c2e 100%);
}

.scroll-card-sm {
  background: linear-gradient(150deg, #0b1320 0%, #091120 100%);
  border: 1px solid rgba(120, 170, 220, 0.11);
  border-radius: 9px;
}

.scroll-title {
  font-family: 'SF Pro Display', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-weight: 800;
  letter-spacing: 0.01em;
  background: linear-gradient(90deg, #34d399, #22d3ee);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.scroll-tabs {
  position: relative;
}

/* 推荐看板特殊样式（保留绿色语义，金融面板化） */
.recommend-card {
  background: linear-gradient(150deg, #0d2a20 0%, #081c15 100%);
  border: 1px solid rgba(52, 211, 153, 0.16);
  border-radius: 12px;
}
.recommend-card:hover {
  border-color: rgba(52, 211, 153, 0.40);
}

/* 涨停板特殊高亮（保留红色语义，金融面板化） */
.limit-up-card {
  background: linear-gradient(150deg, #431212 0%, #2c0d0d 100%);
  border: 1px solid rgba(248, 113, 113, 0.20);
  border-radius: 12px;
}

/* ============================================================
   AMBER FAMILY → DEEP-FINANCE PALETTE（家族命中，含所有不透明度变体）
   ============================================================ */
/* 文本：亮色系 → 冷白 */
[class*="text-amber-100"], [class*="text-amber-200"] { color: #dbe7f3 !important; }
/* 标题/强调色 → 霓虹青 */
[class*="text-amber-300"], [class*="text-amber-400"] { color: #3ae2ee !important; }
[class*="bg-amber-500/15"], [class*="bg-amber-500/5"] { background: rgba(34, 211, 238, 0.12) !important; }
/* 次级文本 → 钢青 */
[class*="text-amber-500"], [class*="text-amber-600"] { color: #8fc0d9 !important; }
[class*="text-amber-700"] { color: #6d8fb0 !important; }

/* 背景：深棕 → 夜色蓝 */
[class*="bg-[#332314]"] { background: #101c30 !important; }
[class*="bg-[#1a1208]"] { background: #0b1422 !important; }
[class*="bg-[#1e150a]"] { background: #0a121f !important; }
[class*="bg-[#22180c]"] { background: #0b1422 !important; }
[class*="bg-[#261b0e]"] { background: #0c1626 !important; }
[class*="bg-[#2d1e0f]"] { background: #101c30 !important; }

/* 边框：棕 → 钢蓝 */
[class*="border-amber-800"], [class*="border-amber-700"] { border-color: rgba(120, 170, 220, 0.16) !important; }
[class*="border-amber-400"] { border-color: rgba(34, 211, 238, 0.40) !important; }

/* 去除字重上的衬线古风（font-serif → 科技无衬线） */
[class*="font-serif"] { font-family: 'SF Pro Text', 'PingFang SC', 'Microsoft YaHei', sans-serif !important; }

/* 头部 sticky 表格底色 */
thead.sticky { background: #101c30 !important; }

/* 页面背景：深色金融星空质感 */
.scroll-tabs ~ *,
.min-h-screen {
  background-color: transparent;
}

/* 滚动条美化（钢青） */
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: rgba(10, 18, 31, 0.5); }
::-webkit-scrollbar-thumb { background: rgba(58, 226, 238, 0.35); border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: rgba(58, 226, 238, 0.6); }
</style>
