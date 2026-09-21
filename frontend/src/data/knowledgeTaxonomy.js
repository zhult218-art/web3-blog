// ============================================================
// 知识库分类体系（持续演进的学习地图）
// 层级：硬件底层 → 嵌入式 → 软件 → 网络工程 → 网络安全 → 运维效率 → 前沿技术
// 新增分类只需在对应大组的 children 中追加；知识库页面与 SQL 种子共用此结构
// ============================================================

// 成长阶段：🌱 待学习 → 🌿 学习中 → 🌳 已掌握
export const STAGES = [
  { key: 'seedling', label: '待学习', icon: '🌱', color: '#a3a380' },
  { key: 'growing', label: '学习中', icon: '🌿', color: '#6b9e78' },
  { key: 'mastered', label: '已掌握', icon: '🌳', color: '#3f7d5b' },
]

export const stageMeta = key => STAGES.find(s => s.key === key) || STAGES[0]

// 知识分类树：key 同时是 kb_entries.category 的取值
export const KB_TAXONOMY = [
  {
    key: '硬件底层',
    icon: '🔩',
    desc: '数字世界的物理根基：从晶体管、指令集到整机体系结构',
    children: [
      '数字电路与逻辑设计', '计算机组成原理', 'CPU 体系结构', '存储体系（Cache/内存/磁盘）',
      '总线与接口（PCIe/USB）', '主板与芯片组', 'GPU 与并行计算', '固件（BIOS/UEFI）',
    ],
  },
  {
    key: '嵌入式',
    icon: '📟',
    desc: '让软件跑进硬件：单片机、RTOS、嵌入式 Linux 与驱动开发',
    children: [
      'C 语言与裸机开发', 'STM32/ARM Cortex-M', '寄存器与中断/DMA', '通信协议（UART/I2C/SPI/CAN）',
      'RTOS（FreeRTOS/RT-Thread）', '嵌入式 Linux 与驱动', '设备树与内核裁剪', '物联网与低功耗设计',
    ],
  },
  {
    key: '软件工程',
    icon: '💻',
    desc: '构建可靠软件系统：语言、算法、架构、数据库与工程化',
    children: [
      '编程语言（C/C++/Java/Go/Rust/Python/JS）', '数据结构与算法', '操作系统原理（Linux）',
      '数据库（MySQL/Redis）', '设计模式与系统设计', '分布式与微服务', '前端工程（Vue3/工程化）',
      '后端工程与 API 设计', 'DevOps 与 CI/CD', 'Docker/K8s 与云原生',
    ],
  },
  {
    key: '网络工程',
    icon: '🌐',
    desc: '连接一切的规则：TCP/IP 协议栈、路由交换与现代组网',
    children: [
      'OSI/TCP-IP 模型', 'HTTP/HTTPS 与 DNS/TLS', '路由与交换（静态/OSPF/BGP/VLAN）',
      '无线网络（Wi-Fi/5G）', '防火墙与 NAT', 'VPN 与零信任组网（Tailscale/WireGuard）',
      'SD-WAN 与网络自动化', '网络抓包与排障（Wireshark/tcpdump）',
    ],
  },
  {
    key: '网络安全',
    icon: '🛡️',
    desc: '攻防对抗视角：Web 安全、渗透测试、逆向与应急响应',
    children: [
      '安全基础（CIA/加密与证书）', 'Web 安全（OWASP Top 10/SQL注入/XSS/SSRF）',
      '渗透测试流程（信息收集/漏洞利用/提权）', 'Burp Suite 与常用工具',
      '逆向工程（汇编/IDA/调试器）', '二进制漏洞（栈溢出/堆利用）', '内网渗透与横向移动',
      '红蓝对抗与威胁狩猎', '应急响应与取证', '等保合规与安全加固',
    ],
  },
  {
    key: '运维与效率',
    icon: '⚙️',
    desc: '让系统稳定运转、让工作事半功倍',
    children: [
      'Linux 运维与 Shell', '远程办公（RustDesk/向日葵/ToDesk）', '监控告警（Prometheus/Grafana）',
      '自动化脚本', 'Nginx 与反向代理', '备份与容灾',
    ],
  },
  {
    key: '前沿技术',
    icon: '🚀',
    desc: '主流前沿：大模型、AI 应用开发、Web3 与新技术追踪',
    children: [
      '大模型原理（Transformer/RAG/Agent）', 'AI 编程实践', 'Vercel AI SDK / AI Gateway',
      'Web3 与区块链', 'WebAssembly', 'Rust 生态',
    ],
  },
]

// 一级分类扁平列表
export const KB_CATEGORIES = KB_TAXONOMY.map(t => ({ key: t.key, icon: t.icon, desc: t.desc }))

// 所有二级标签 → 一级分类的反查表
export const SUB_TO_CATEGORY = KB_TAXONOMY.reduce((m, t) => {
  t.children.forEach(sub => { m[sub] = t.key })
  return m
}, {})
