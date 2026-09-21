// ============================================================
// 免费 AI 站点导航数据
// 数据来源：github-myblog/carrot-main（Free ChatGPT Site List）
// 按分类提炼高频优质站点，供 /tools/ainav 工具页渲染
// ============================================================

export const aiSiteCategories = [
  {
    id: 'hot',
    name: '热门精选',
    icon: '🔥',
    desc: '访问量最高的头部 AI 站点',
    sites: [
      { name: 'ChatGPT', url: 'https://chatgpt.com/', desc: 'OpenAI 官方通用助手，行业标杆' },
      { name: 'Gemini', url: 'https://gemini.google.com/', desc: '谷歌多模态大模型助手' },
      { name: 'Claude', url: 'https://claude.ai/', desc: 'Anthropic 出品，擅长推理、写作与分析' },
      { name: 'DeepSeek', url: 'https://www.deepseek.com/', desc: '热门国产大模型，推理能力强' },
      { name: '豆包', url: 'https://www.doubao.com/chat/', desc: '字节跳动旗下 AI 智能助手' },
      { name: '千问', url: 'https://www.qianwen.com/', desc: '阿里通用 AI，问答/创作/多模态' },
      { name: 'Perplexity', url: 'https://www.perplexity.ai/', desc: '实时网页检索与引用回答的 AI 搜索' },
      { name: '智谱清言', url: 'https://chatglm.cn/', desc: '智谱 GLM 大模型驱动的全能助手' },
      { name: 'Manus', url: 'https://manus.im/', desc: '规划并执行复杂任务的通用智能体' },
      { name: 'Cursor', url: 'https://cursor.com/', desc: 'AI 代码编辑器，智能体式开发' },
      { name: 'Midjourney', url: 'https://www.midjourney.com/', desc: '主流 AI 图像生成平台' },
      { name: '海螺视频', url: 'https://hailuoai.com/', desc: 'MiniMax 旗下 AI 视频创作平台' },
    ],
  },
  {
    id: 'agent',
    name: 'Agent 与自动化',
    icon: '🤖',
    desc: '智能体与任务自动化平台',
    sites: [
      { name: 'Manus', url: 'https://manus.im/', desc: '通用 AI 智能体，可交付研究与分析' },
      { name: 'OpenClaw', url: 'https://openclaw.ai/', desc: '真正行动的 AI：邮箱/日历/邮件自动化' },
      { name: '扣子 Coze', url: 'https://www.coze.cn/', desc: '字节新一代 AI 智能体开发平台' },
      { name: 'ChatNio', url: 'https://coai.drawaspark.com/', desc: '多模型聚合，DeepSeek 满血版' },
    ],
  },
  {
    id: 'api',
    name: '模型 / API / 算力',
    icon: '🔌',
    desc: '免费 API 中转与算力资源',
    sites: [
      { name: '免费 ChatGPT API', url: 'https://chat.freegpt.work/', desc: '多模型，GPT-4 每日 300 次免注册' },
      { name: 'ChatGPT Gratis', url: 'https://chatgptgratis.eu/zh/chat.html', desc: 'GPT-4o / llama3.1-405b 免费用' },
      { name: '商汤秒画', url: 'https://miaohua.sensetime.com/inspiration', desc: 'SenseMirage AI 作画，有手就行' },
      { name: 'GPT-API-free', url: 'https://api.chatanywhere.tech/', desc: 'gpt/deepseek/claude/gemini 免费中转' },
    ],
  },
  {
    id: 'dev',
    name: '编程与开发工具',
    icon: '💻',
    desc: 'AI 编码助手与开发效率工具',
    sites: [
      { name: 'Cursor', url: 'https://cursor.com/', desc: 'AI 代码编辑器：补全/问答/重构' },
      { name: 'Chat-GPT-OSS', url: 'https://chat-gpt-oss.com/', desc: '面向开发者的开放服务工作室' },
      { name: 'Fynix.ai', url: 'https://www.fynix.ai/', desc: '轻量 AI 编码助手' },
      { name: 'FreelyAi', url: 'https://freelyai.liujiarong.online/', desc: '免费免登录，满血 DeepSeek' },
    ],
  },
  {
    id: 'image',
    name: '图像与视觉创作',
    icon: '🎨',
    desc: 'AI 绘画与设计工具',
    sites: [
      { name: 'Midjourney', url: 'https://www.midjourney.com/', desc: '文本提示生成高质量艺术视觉' },
      { name: '360 鸿图', url: 'https://tu.360.cn/', desc: '一句话生成多风格精美画作' },
      { name: 'AIBox', url: 'https://chat.aibox365.cn/', desc: '一站式 AI 工具平台' },
      { name: '免费 GPT-4 绘图', url: 'https://easychat.fun/', desc: '免费 GPT-4 / Claude，无需注册' },
    ],
  },
  {
    id: 'av',
    name: '音频 / 视频 / 数字人',
    icon: '🎬',
    desc: 'AI 音视频生成平台',
    sites: [
      { name: '可灵 AI', url: 'https://klingai.com/', desc: '快手 AI 视频/图像生成平台' },
      { name: '海螺视频', url: 'https://hailuoai.com/', desc: '文生视频、图生视频' },
      { name: 'ElevenLabs', url: 'https://elevenlabs.io/', desc: '文本转语音、声音克隆与配音' },
      { name: 'Suno', url: 'https://suno.com/', desc: '提示词生成完整歌曲与歌词' },
    ],
  },
]

// 扁平化总数（供页面统计展示）
export const aiSiteTotal = aiSiteCategories.reduce((n, c) => n + c.sites.length, 0)
