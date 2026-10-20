<template>
  <div class="fp-map">
    <!-- 分类筛选（全部足迹 / 上学 / 家乡 / 工作 / 旅行…） -->
    <div class="fp-toolbar">
      <button v-for="c in categories" :key="c" class="fp-cat"
        :class="{ on: c === activeCat }" @click="activeCat = c">{{ c }}</button>
      <span class="fp-count">{{ filtered.length }} 个足迹</span>
    </div>

    <div class="fp-body">
      <div ref="chartEl" class="fp-chart" :class="{ loading }">
        <span v-if="loading" class="fp-loading">地图加载中…</span>
        <span v-else-if="error" class="fp-loading fp-err">{{ error }}</span>
      </div>

      <!-- 选中地点详情 -->
      <aside class="fp-detail">
        <template v-if="active">
          <span class="fp-detail-date">{{ active.date }}</span>
          <h4 class="fp-detail-name">{{ active.name }}</h4>
          <p class="fp-detail-desc">{{ active.description || '暂无描述' }}</p>
          <div class="fp-detail-tags">
            <span v-for="t in active.categories" :key="t" class="fp-tag">{{ t }}</span>
          </div>
        </template>
        <div v-else class="fp-detail-empty">
          <span class="fp-detail-hint-icon">🗺️</span>
          <p>点击地图上的光点<br />查看该地足迹详情</p>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
// ============================================================
// 足迹地图（FootprintMap）
// 技术方案：ECharts geo 地图（china）+ effectScatter 光点
// 数据源：/data/footprints.json（可在不改代码的情况下直接替换）
// 底图：/geo/china.json（本地 GeoJSON，运行时按需加载，无外部依赖）
// ============================================================
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import echarts from '@/utils/echarts'

const chartEl = ref(null)
const locations = ref([])
const categories = ref(['全部足迹'])
const activeCat = ref('全部足迹')
const active = ref(null)
const loading = ref(true)
const error = ref('')

let chart = null
let resizeObserver = null

// 标记配色：与足迹数据里的 markerColor 语义名对应
const MARKER_COLORS = {
  sunset: '#ff7a59',
  ocean: '#22d3ee',
  forest: '#34d399',
  amber: '#fbbf24',
  violet: '#a855f7',
  citrus: '#f472b6'
}
const colorOf = loc => MARKER_COLORS[loc.markerColor] || '#a855f7'

const filtered = computed(() =>
  activeCat.value === '全部足迹'
    ? locations.value
    : locations.value.filter(l => (l.categories || []).includes(activeCat.value))
)

// 解析 "lng,lat" 字符串坐标
function parseCoord(s) {
  if (!s || typeof s !== 'string') return null
  const [lng, lat] = s.split(',').map(v => parseFloat(v.trim()))
  if (Number.isNaN(lng) || Number.isNaN(lat)) return null
  return [lng, lat]
}

function buildOption() {
  const points = filtered.value.map(loc => {
    const coord = parseCoord(loc.coordinates)
    return coord ? {
      name: loc.name,
      value: [...coord, 1],
      raw: loc,
      itemStyle: { color: colorOf(loc), shadowBlur: 12, shadowColor: colorOf(loc) }
    } : null
  }).filter(Boolean)

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(10,10,26,0.92)',
      borderColor: 'rgba(168,85,247,0.45)',
      textStyle: { color: '#e5e7ff', fontSize: 12 },
      formatter: p => {
        const r = p.data?.raw
        if (!r) return p.name
        return `<b>${r.name}</b>${r.date ? ' · ' + r.date : ''}<br/>${(r.description || '').slice(0, 40)}`
      }
    },
    geo: {
      map: 'china',
      roam: true,
      zoom: 1.15,
      silent: false,
      itemStyle: {
        areaColor: 'rgba(18,18,42,0.85)',
        borderColor: 'rgba(129,140,248,0.5)',
        borderWidth: 0.8,
        shadowColor: 'rgba(88,101,242,0.35)',
        shadowBlur: 14
      },
      emphasis: {
        itemStyle: { areaColor: 'rgba(88,101,242,0.35)' },
        label: { show: false }
      },
      label: { show: false }
    },
    series: [{
      type: 'effectScatter',
      coordinateSystem: 'geo',
      data: points,
      symbolSize: 9,
      showEffectOn: 'render',
      rippleEffect: { brushType: 'stroke', scale: 3.2, period: 4 },
      emphasis: { scale: 1.6 },
      zlevel: 2
    }]
  }
}

function render() {
  if (!chart) return
  chart.setOption(buildOption(), true)
}

// 点击光点 → 展示详情卡
function onChartClick(p) {
  if (p?.data?.raw) active.value = p.data.raw
}

// 分类或数据变化时重绘，并保留当前选中项
watch([filtered, () => activeCat.value], () => {
  render()
  if (active.value && !filtered.value.includes(active.value)) active.value = null
})

onMounted(async () => {
  try {
    const [dataRes, geoRes] = await Promise.all([
      fetch('/data/footprints.json'),
      fetch('/geo/china.json')
    ])
    const data = await dataRes.json()
    const geo = await geoRes.json()
    echarts.registerMap('china', geo)
    locations.value = Array.isArray(data.locations) ? data.locations : []

    // 分类按钮：全部足迹 + 数据中出现的所有分类（保持出现顺序）
    const cats = []
    locations.value.forEach(l => (l.categories || []).forEach(c => { if (!cats.includes(c)) cats.push(c) }))
    categories.value = ['全部足迹', ...cats]

    loading.value = false
    await nextTick()
    chart = echarts.init(chartEl.value)
    chart.on('click', onChartClick)
    render()

    resizeObserver = new ResizeObserver(() => chart?.resize())
    resizeObserver.observe(chartEl.value)
  } catch (e) {
    loading.value = false
    error.value = '足迹地图加载失败'
  }
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.fp-map {
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  background: rgba(10, 10, 26, 0.55);
  overflow: hidden;
}

.fp-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
  padding: 0.9rem 1.1rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.fp-cat {
  padding: 0.35rem 0.85rem;
  font-size: 0.72rem;
  color: rgba(255, 255, 255, 0.6);
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.25s;
}
.fp-cat:hover { color: #fff; border-color: rgba(168, 85, 247, 0.5); }
.fp-cat.on {
  color: #fff;
  background: linear-gradient(135deg, rgba(168, 85, 247, 0.3), rgba(6, 182, 212, 0.3));
  border-color: rgba(168, 85, 247, 0.7);
  box-shadow: 0 0 14px rgba(168, 85, 247, 0.35);
}

.fp-count {
  margin-left: auto;
  font-size: 0.62rem;
  letter-spacing: 0.14em;
  color: rgba(255, 255, 255, 0.35);
  font-family: 'Courier New', monospace;
}

.fp-body {
  display: grid;
  grid-template-columns: 1fr 240px;
  min-height: 420px;
}

.fp-chart {
  position: relative;
  height: 420px;
}

.fp-loading {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.78rem;
  color: rgba(255, 255, 255, 0.4);
  font-family: 'Courier New', monospace;
}
.fp-err { color: rgba(255, 150, 150, 0.7); }

.fp-detail {
  padding: 1.2rem 1.2rem 1.4rem;
  border-left: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.fp-detail-date {
  align-self: flex-start;
  font-size: 0.6rem;
  letter-spacing: 0.16em;
  color: #00d4ff;
  font-family: 'Courier New', monospace;
  padding: 0.15rem 0.5rem;
  border: 1px solid rgba(0, 212, 255, 0.25);
  border-radius: 999px;
}

.fp-detail-name { font-size: 1.05rem; font-weight: 700; color: #fff; }

.fp-detail-desc {
  font-size: 0.76rem;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.55);
}

.fp-detail-tags { display: flex; flex-wrap: wrap; gap: 0.35rem; margin-top: auto; }

.fp-tag {
  font-size: 0.6rem;
  padding: 0.16rem 0.5rem;
  border-radius: 999px;
  color: #c4b5fd;
  background: rgba(168, 85, 247, 0.12);
  border: 1px solid rgba(168, 85, 247, 0.28);
  font-family: 'Courier New', monospace;
}

.fp-detail-empty {
  margin: auto;
  text-align: center;
  color: rgba(255, 255, 255, 0.3);
  font-size: 0.72rem;
  line-height: 1.8;
}
.fp-detail-hint-icon { font-size: 1.8rem; display: block; margin-bottom: 0.5rem; }

@media (max-width: 720px) {
  .fp-body { grid-template-columns: 1fr; }
  .fp-chart { height: 320px; }
  .fp-detail { border-left: 0; border-top: 1px solid rgba(255, 255, 255, 0.06); min-height: 120px; }
}
</style>
