/**
 * ECharts 按需引入（统一出口）
 *
 * 背景：原先 9 个文件各自 `import * as echarts from 'echarts'`，全量引入导致
 * vendor-ui chunk 达 1.1MB（gzip 379KB），而项目实际只用到 8 种图表。
 *
 * 用法：把 `import * as echarts from 'echarts'` 换成
 *      `import echarts from '@/utils/echarts'`
 * 其余 `echarts.init(...)` / `echarts.graphic.LinearGradient` / `echarts.registerMap(...)`
 * 调用方式完全不变（echarts/core 已导出这些 API）。
 *
 * 新增图表类型时，务必在此处补充对应的 Chart / Component 并加入 use()，
 * 否则该图表会静默不渲染（ECharts 按需模式下未注册的图表不会报错）。
 */
import * as echarts from 'echarts/core'

import {
  BarChart,
  CandlestickChart,
  EffectScatterChart,
  GaugeChart,
  HeatmapChart,
  LineChart,
  PieChart,
  ScatterChart,
} from 'echarts/charts'

import {
  DataZoomComponent,
  GeoComponent,
  GraphicComponent,
  GridComponent,
  LegendComponent,
  MarkLineComponent,
  MarkPointComponent,
  TitleComponent,
  TooltipComponent,
  VisualMapComponent,
} from 'echarts/components'

import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  // 图表类型
  BarChart,
  CandlestickChart,
  EffectScatterChart,
  GaugeChart,
  HeatmapChart,
  LineChart,
  PieChart,
  ScatterChart,
  // 组件（axisPointer 随 GridComponent 一并注册）
  DataZoomComponent,
  GeoComponent,
  GraphicComponent,
  GridComponent,
  LegendComponent,
  MarkLineComponent,
  MarkPointComponent,
  TitleComponent,
  TooltipComponent,
  VisualMapComponent,
  // 渲染器
  CanvasRenderer,
])

export default echarts
