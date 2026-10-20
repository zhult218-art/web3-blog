// ============================================================
// 个人档案数据（关于页 / 首页「关于我」共用）
// 同一份数据被两处消费，改这里即可同时生效，避免两边各写一套慢慢走形
// ============================================================

// 「关于我」正文（关于页用作滚动打字机文本）
export const aboutText = `我是一名沉迷于「沉浸式 Web 体验」的前端工程师与全栈爱好者。
从 Vue 3 的 Composition API 到 Three.js 的 GLSL 着色器，从 Spring Cloud 微服务到 FastAI 驱动的 LLM Agent，
我始终相信代码不仅是工具，更是一种表达——它能把抽象的思维变成可触摸的视觉与交互。

这里的每一行代码、每一篇文章、每一个 Demo，都来自日常的真实积累：
没有速成的捷径，也没有轻松成功的保证。我把学习中遇到的问题与解法沉淀在这个站点，
希望对正在探索同样路径的你，能有一点帮助或灵感。

当前阶段，我正在深入 AI Agent 编排、RAG 检索增强与 WebGL 实时渲染的结合，
探索如何让 Web 不只是「展示信息的容器」，而是「承载智能与感知的现场」。
如果你也在做类似的事情，欢迎在留言板或邮箱聊聊。`

// 社群入口（预留）：url 留空即展示「敬请期待」
export const communities = [
  { name: 'QQ 交流群', icon: '🐧', url: '' },
  { name: 'Telegram 群', icon: '✈️', url: '' },
]

// 技能矩阵：每项含「投入年限 + 熟练度 + 具体做过什么」，
// 用真实经历替代星级打分，避免千篇一律的刻板数值
export const skillGroups = [
  {
    name: '前端工程', en: 'FRONTEND', icon: '🎨', role: '主力方向',
    items: [
      { name: 'Vue 3 / Composition API', level: 92, since: 2020, note: '自研 40+ 业务组件 · Pinia 状态架构与路由守卫体系' },
      { name: 'TypeScript / JavaScript', level: 88, since: 2019, note: '全量类型化 API 层 · 泛型与类型收窄实战' },
      { name: 'TailwindCSS / SCSS', level: 86, since: 2021, note: '设计令牌驱动的暗色主题体系 · 响应式栅格' },
      { name: 'Three.js / WebGL', level: 78, since: 2021, note: 'GLSL 着色器粒子系统 · 星系体渲染与后处理' },
      { name: 'Vite / 构建优化', level: 82, since: 2020, note: '首屏分包 · 资源体积治理与按需加载' },
    ],
  },
  {
    name: '后端服务', en: 'BACKEND', icon: '🛰️', role: '主力方向',
    items: [
      { name: 'Spring Boot / Cloud', level: 90, since: 2020, note: '网关·注册·配置三件套 · 多服务串行编排与内存治理' },
      { name: 'FastAPI / Python', level: 80, since: 2023, note: 'AI 能力网关 · 多 provider 轮询与超时淘汰策略' },
      { name: 'Node.js / Express', level: 76, since: 2021, note: 'BFF 聚合层 · 中间件与静态资源代理' },
      { name: 'Socket.IO / 实时通信', level: 74, since: 2022, note: '群聊私聊同源架构 · 心跳重连与长轮询降级' },
    ],
  },
  {
    name: '数据存储', en: 'DATABASE', icon: '🗄️', role: '熟练运用',
    items: [
      { name: 'MySQL / 索引调优', level: 86, since: 2020, note: '慢查询定位与联合索引重排 · 读写分离' },
      { name: 'Redis / 缓存策略', level: 82, since: 2021, note: '热点缓存与击穿防护 · PubSub 集群广播预留' },
      { name: 'PostgreSQL', level: 70, since: 2022, note: 'JSONB 半结构化存储 · 全文检索' },
      { name: '向量检索 / 文档库', level: 76, since: 2023, note: 'RAG 文档切片入库 · 相似度召回与重排' },
    ],
  },
  {
    name: 'DevOps', en: 'DEVOPS', icon: '🚀', role: '熟练运用',
    items: [
      { name: 'Docker / Compose', level: 88, since: 2021, note: '多服务容器编排 · 镜像分层瘦身与健康探针' },
      { name: 'Kubernetes / K8s', level: 72, since: 2023, note: 'Deployment / Service / Ingress · 资源限额与滚动发布' },
      { name: 'CI 流水线', level: 80, since: 2022, note: '构建即代码 · 自动检测与产物归档' },
      { name: 'Nacos / Grafana', level: 78, since: 2023, note: '配置中心动态刷新 · 指标看板与告警阈值' },
    ],
  },
  {
    name: 'AI 与算法', en: 'AI · ML', icon: '🧠', role: '持续钻研',
    items: [
      { name: 'LLM Prompt / Function Call', level: 86, since: 2023, note: '多模型路由与工具调用编排 · 结构化输出约束' },
      { name: 'RAG 检索增强生成', level: 82, since: 2023, note: '混合检索 + 重排 · 引用溯源与幻觉抑制' },
      { name: 'Agent 工作流编排', level: 78, since: 2024, note: '多步任务拆解 · 记忆持久化与失败回退' },
      { name: 'Python 数据科学栈', level: 80, since: 2021, note: 'Pandas 因子计算 · 回测统计与可视化' },
    ],
  },
  {
    name: '工具与协作', en: 'TOOLING', icon: '🛠️', role: '日常必备',
    items: [
      { name: 'Git / 协作流程', level: 90, since: 2019, note: '分支策略与冲突治理 · 提交规范与变更追溯' },
      { name: 'Figma / 设计还原', level: 76, since: 2022, note: '设计令牌对齐 · 交互稿到像素级组件落地' },
      { name: 'Linux / Shell', level: 84, since: 2019, note: '服务巡检脚本 · 端口探测与进程内存看护' },
      { name: '性能压测 / 可观测', level: 80, since: 2022, note: '压测基线 · 首屏指标与接口 P95 跟踪' },
    ],
  },
]

// 时间线
export const timeline = [
  {
    year: '2019',
    title: '初识编程 · 自学起步',
    desc: '从 Python 入门，写第一个爬虫与自动化脚本，开启「用代码解决问题」的旅程。',
    tags: ['Python', '自学', '爬虫'],
  },
  {
    year: '2021',
    title: '深入 Java 生态 · 后端工程化',
    desc: '系统学习 Spring 全家桶，从单体到微服务，理解分布式系统中的注册、配置、熔断、限流。',
    tags: ['Java', 'Spring Cloud', '微服务'],
  },
  {
    year: '2023',
    title: '前端沉浸 · Vue3 + Three.js',
    desc: '转向 Vue 3 组合式 API 与 Three.js 实时渲染，专注沉浸式 Web 体验与动效叙事。',
    tags: ['Vue3', 'Three.js', 'GSAP'],
  },
  {
    year: '2024',
    title: 'AI 探索 · LLM Agent 与 RAG',
    desc: '接入大模型推理与向量检索，搭建具备长期记忆与工具调用能力的知识库智能体。',
    tags: ['LLM', 'RAG', 'Agent'],
  },
  {
    year: '2026',
    title: '当下 · 综合门户与开源沉淀',
    desc: '将所有学习与项目沉淀为这个 Web3 门户站点，持续输出技术文章与开源实践。',
    tags: ['Aurora-朱', 'Open Source', '持续输出'],
  },
]

// 代表作品
export const projects = [
  {
    icon: '🧠',
    title: 'AI 知识库智能体',
    desc: '接入 RAG 检索与 LLM 推理的问答体，支持长期记忆、工具调用与多轮对话。',
    tags: ['FastAPI', 'RAG', 'LLM'],
    path: '/ai',
    cover: 'linear-gradient(135deg, #1c1240 0%, #2a1a5e 45%, #4a2a8a 100%)',
  },
  {
    icon: '🌌',
    title: '3D 粒子云 · 星系模拟',
    desc: '数万粒子在 GPU 中实时演化，鼠标拖拽旋转视角，沉浸式星云环绕。',
    tags: ['Three.js', 'WebGL', 'GLSL'],
    path: '/three',
    cover: 'linear-gradient(135deg, #061a2c 0%, #0a2840 45%, #0e4a6e 100%)',
  },
  {
    icon: '⚙️',
    title: 'Spring Cloud 脚手架',
    desc: '一键生成网关、注册中心、配置中心与监控链路，生产级微服务工程模板。',
    tags: ['Spring Cloud', 'Gateway', 'Docker'],
    path: '/architecture',
    cover: 'linear-gradient(135deg, #2c0a2e 0%, #4a1040 45%, #7a1e58 100%)',
  },
  {
    icon: '💹',
    title: '量化策略回测平台',
    desc: '多因子选股 + 趋势识别策略，按日/分钟级回测，自动生成归因报告。',
    tags: ['Python', 'Pandas', 'Backtrader'],
    path: '/quant',
    cover: 'linear-gradient(135deg, #0a2c12 0%, #144a20 45%, #1e7a3a 100%)',
  },
]

// 兴趣爱好标签云
export const interests = [
  '🎬 电影', '🎵 音乐', '📚 阅读', '🎮 游戏', '☕ 咖啡',
  '🧗 攀岩', '📷 摄影', '🌌 天文', '🎲 桌游', '🪴 养植',
  '🍳 烹饪', '🏃 跑步', '🎧 播客', '🐱 猫咪', '🗺️ 旅行',
  '🔭 观星', '🖌️ 数字绘画',
]

// 熟练度 → 等级标签 / 色带
export function levelLabel(lv) {
  if (lv >= 90) return 'EXPERT'
  if (lv >= 80) return 'ADVANCED'
  if (lv >= 70) return 'PROFICIENT'
  return 'LEARNING'
}
export function levelBand(lv) {
  if (lv >= 90) return 'expert'
  if (lv >= 80) return 'advanced'
  return 'steady'
}
// 投入年限：从开始接触的年份算起
export function yearsOf(since) {
  return Math.max(1, new Date().getFullYear() - since)
}
