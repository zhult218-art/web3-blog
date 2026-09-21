// ====================================================
// 品牌图标解析：按软件/商品名称映射到 simple-icons 官方图标。
// https://cdn.simpleicons.org/{slug} 返回自带品牌色的 SVG，纯 URL 方案、零依赖。
// ====================================================

// 别名词典：key 为归一化后的名称（小写、去空格/点/横线/括号），value 为 simple-icons slug。
// 只收录 slug 十分确定的条目，宁缺毋滥，未命中的名称一律回退展示，避免出现错误图标。
const BRAND_ALIAS = {
  // 编辑器 / IDE
  vscode: 'visualstudiocode', visualstudiocode: 'visualstudiocode', code: 'visualstudiocode',
  visualstudio: 'visualstudio', sublimetext: 'sublimetext',
  'notepad++': 'notepadplusplus', notepadplusplus: 'notepadplusplus',
  vim: 'vim', neovim: 'neovim',
  intellijidea: 'intellijidea', intellij: 'intellijidea', idea: 'intellijidea',
  webstorm: 'webstorm', pycharm: 'pycharm', goland: 'goland', datagrip: 'datagrip',
  clion: 'clion', rider: 'rider', eclipse: 'eclipseide',
  // 版本控制 / 协作
  git: 'git', github: 'github', gitlab: 'gitlab', gitee: 'gitee', bitbucket: 'bitbucket', sourcetree: 'sourcetree',
  // 语言 / 运行时 / 前端
  javascript: 'javascript', typescript: 'typescript', vue: 'vuedotjs', vuejs: 'vuedotjs',
  react: 'react', angular: 'angular', node: 'nodedotjs', nodejs: 'nodedotjs',
  npm: 'npm', yarn: 'yarn', pnpm: 'pnpm', python: 'python',
  java: 'openjdk', jdk: 'openjdk', openjdk: 'openjdk',
  go: 'go', golang: 'go', rust: 'rust', 'c++': 'cplusplus', cpp: 'cplusplus',
  dart: 'dart', flutter: 'flutter', swift: 'swift', kotlin: 'kotlin', php: 'php',
  deno: 'deno', bun: 'bun', dotnet: 'dotnet', net: 'dotnet', spring: 'spring', electron: 'electron',
  tailwind: 'tailwindcss', tailwindcss: 'tailwindcss', sass: 'sass', bootstrap: 'bootstrap',
  webpack: 'webpack', vite: 'vite', eslint: 'eslint', prettier: 'prettier', markdown: 'markdown',
  // 构建 / 后端 / 数据库
  maven: 'apachemaven', gradle: 'gradle',
  mysql: 'mysql', postgresql: 'postgresql', mongodb: 'mongodb', redis: 'redis',
  sqlite: 'sqlite', mariadb: 'mariadb', clickhouse: 'clickhouse', dbeaver: 'dbeaver',
  nginx: 'nginx', docker: 'docker', kubernetes: 'kubernetes', jenkins: 'jenkins',
  elasticsearch: 'elasticsearch', kafka: 'apachekafka', tomcat: 'apachetomcat',
  spark: 'apachespark', flink: 'apacheflink', hive: 'apachehive',
  hadoop: 'apachehadoop', rabbitmq: 'rabbitmq',
  // API / 终端 / 远程 / 虚拟化
  postman: 'postman', apifox: 'apifox', insomnia: 'insomnia', hoppscotch: 'hoppscotch', bruno: 'bruno', swagger: 'swagger',
  powershell: 'powershell', windowsterminal: 'windowsterminal', iterm2: 'iterm2',
  filezilla: 'filezilla',
  teamviewer: 'teamviewer', anydesk: 'anydesk', rustdesk: 'rustdesk',
  virtualbox: 'virtualbox', vmware: 'vmware',
  // 浏览器 / 系统 / 硬件
  chrome: 'googlechrome', googlechrome: 'googlechrome', edge: 'microsoftedge', microsoftedge: 'microsoftedge',
  firefox: 'firefoxbrowser', firefoxbrowser: 'firefoxbrowser', brave: 'brave', opera: 'opera',
  safari: 'safari', vivaldi: 'vivaldi',
  windows: 'windows', linux: 'linux', macos: 'macos', apple: 'apple', android: 'android',
  ubuntu: 'ubuntu', debian: 'debian', centos: 'centos', deepin: 'deepin',
  nvidia: 'nvidia', amd: 'amd', intel: 'intel', arduino: 'arduino', raspberrypi: 'raspberrypi',
  // 办公 / 笔记
  word: 'microsoftword', excel: 'microsoftexcel',
  powerpoint: 'microsoftpowerpoint', ppt: 'microsoftpowerpoint', onenote: 'microsoftonenote',
  onedrive: 'microsoftonedrive', teams: 'microsoftteams',
  libreoffice: 'libreoffice', obsidian: 'obsidian', notion: 'notion',
  joplin: 'joplin', logseq: 'logseq', zotero: 'zotero',
  // 设计 / 图形
  figma: 'figma', sketch: 'sketch', canva: 'canva', miro: 'miro',
  photoshop: 'adobephotoshop', ps: 'adobephotoshop', illustrator: 'adobeillustrator',
  aftereffects: 'adobeaftereffects', blender: 'blender',
  gimp: 'gimp', inkscape: 'inkscape', davinciresolve: 'davinciresolve',
  unity: 'unity', unrealengine: 'unrealengine',
  // 影音 / 桌面工具
  vlc: 'vlcmediaplayer', obs: 'obsstudio', audacity: 'audacity', ffmpeg: 'ffmpeg',
  foobar2000: 'foobar2000', spotify: 'spotify',
  neteasecloudmusic: 'neteasecloudmusic',
  // 压缩 / 下载
  '7zip': '7zip',
  idm: 'internetdownloadmanager', internetdownloadmanager: 'internetdownloadmanager',
  qbittorrent: 'qbittorrent', utorrent: 'utorrent',
  // 通讯 / 社区
  telegram: 'telegram', discord: 'discord', whatsapp: 'whatsapp', slack: 'slack', zoom: 'zoom',
  wechat: 'wechat', weixin: 'wechat', 微信: 'wechat', qq: 'tencentqq',
  twitter: 'x', x: 'x', instagram: 'instagram', youtube: 'youtube', linkedin: 'linkedin', reddit: 'reddit',
  bilibili: 'bilibili', zhihu: 'zhihu',
  // AI / 数据科学
  openai: 'openai', jupyter: 'jupyter', anaconda: 'anaconda',
  pytorch: 'pytorch', tensorflow: 'tensorflow',
  // 云服务
  alibabacloud: 'alibabacloud', 阿里云: 'alibabacloud', tencentcloud: 'tencentcloud',
  alipay: 'alipay', 支付宝: 'alipay', baidu: 'baidu',
  // 游戏 / 静态站点
  steam: 'steam', epic: 'epicgames', epicgames: 'epicgames', hugo: 'hugo', hexo: 'hexo',
  // 网络 / 代理
  wireshark: 'wireshark', openvpn: 'openvpn', wireguard: 'wireguard',
  // 终端 / 其他补充
  warp: 'warp', warpterminal: 'warp', alacritty: 'alacritty',
  // Web3 / 加密（metamask/uniswap/tron 已从 simple-icons 移除且旧版无收录，勿加）
  solidity: 'solidity', smartcontract: 'solidity', ethereum: 'ethereum',
  binance: 'binance', coinbase: 'coinbase', opensea: 'opensea',
  chainlink: 'chainlink', solana: 'solana', polygon: 'polygon',
  dogecoin: 'dogecoin', cardano: 'cardano', polkadot: 'polkadot', tether: 'tether',
}

// 版本号/位数等尾部后缀（在去分隔符之后的字符串上匹配）
const VERSION_SUFFIX_RE = /(v\d+|64bit|32bit|arm64|x64|x86|win32|win64|portable|bit|\d+)$/

// 「主名 + 描述性尾巴」的噪声词：如 微信windows版 = 微信 + windows + 版
const TAIL_NOISE_RE = /^(windows|win|mac|macos|linux|pc|desktop|workbench|manager|viewer|studio|android|安卓|ios|电脑版|手机版|windows版|win版|mac版|官方|官方版|正式版|最新版|中文版|国际版|绿色版|免费版|便携版|安装版|app|client|软件|工具|助手|plus|pro|max|版|下载|server|服务端|客户端|桌面版|桌面端)*$/

// 三轮匹配：精确/版本后缀剥离 → 最长前缀+噪声尾巴 → 包含长关键词。命中返回 slug，否则 null
function matchSlug(name) {
  let s = String(name ?? '').trim().toLowerCase().replace(/[\s._()（）\-–—]+/g, '')
  // 第一轮：查完整归一化名，逐级去掉尾部版本号后缀重试
  for (let i = 0; i < 4 && s; i++) {
    if (BRAND_ALIAS[s]) return BRAND_ALIAS[s]
    const stripped = s.replace(VERSION_SUFFIX_RE, '')
    if (!stripped || stripped === s) break
    s = stripped
  }
  // 第二轮：最长前缀匹配——主名命中且剩余部分是纯噪声尾巴（windows版/官方/客户端等）也算命中
  const keys = Object.keys(BRAND_ALIAS).sort((a, b) => b.length - a.length)
  for (const k of keys) {
    if (s.startsWith(k) && TAIL_NOISE_RE.test(s.slice(k.length))) return BRAND_ALIAS[k]
  }
  // 第三轮：包含匹配——名称里含 ≥5 字符的品牌词（如 Another Redis Desktop Manager 含 redis）。
  // 短词不参与，避免 go/ps/x 等误伤；按键长降序取最长命中。
  for (const k of keys) {
    if (k.length >= 5 && s.includes(k)) return BRAND_ALIAS[k]
  }
  return null
}

// 主源：simple-icons 当前版（自带品牌色）。近年已移除微软/Adobe 等品牌，404 时请降级 legacy
export function resolveBrandIcon(name) {
  const slug = matchSlug(name)
  return slug ? `https://cdn.simpleicons.org/${slug}` : ''
}

// 降级源：simple-icons@11（旧版仍收录被主源移除的品牌）。单色黑 SVG，深色底上需 filter: invert(1)
export function resolveBrandIconLegacy(name) {
  const slug = matchSlug(name)
  return slug ? `https://cdn.jsdelivr.net/npm/simple-icons@11/icons/${slug}.svg` : ''
}
