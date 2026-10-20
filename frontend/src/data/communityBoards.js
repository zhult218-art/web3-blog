// ============================================================
// 社区板块与预置话题
// 社区按主题分成若干板块（技术 / 抽象 / 工作 / 感情 / 金融 / 吃瓜 / 搞笑），
// 每个板块先放入若干条示例话题撑起版面，用户可随时用自己的内容覆盖；
// 帖子时间按「距今多少小时」动态生成，保证列表看起来是活的。
// ============================================================

function hoursAgo(h) {
  return new Date(Date.now() - h * 3600 * 1000).toISOString()
}

// 板块定义：key 与后端帖子的 category / tags 做匹配
export const boards = [
  { key: 'tech', name: '技术探讨', en: 'TECH', icon: '🧠', color: '#06b6d4', desc: '架构、踩坑、源码与性能优化' },
  { key: 'abstract', name: '抽象思考', en: 'ABSTRACT', icon: '🌀', color: '#a855f7', desc: '哲学、脑洞与说不清的念头' },
  { key: 'work', name: '牛马工作', en: 'WORK', icon: '🐂', color: '#f59e0b', desc: '打工日常、求职与职场吐槽' },
  { key: 'love', name: '感情', en: 'EMOTION', icon: '💗', color: '#ec4899', desc: '恋爱、友情与那些说不出口的心事' },
  { key: 'finance', name: '金融', en: 'FINANCE', icon: '💹', color: '#10b981', desc: '理财、投资与搞钱思路' },
  { key: 'gossip', name: '吃瓜', en: 'GOSSIP', icon: '🍉', color: '#f43f5e', desc: '热搜、行业内幕与八卦现场' },
  { key: 'humor', name: '搞笑分享', en: 'FUN', icon: '😂', color: '#eab308', desc: '段子、梗图与沙雕日常' },
]

// 预置话题：按板块分开，id 以 seed- 开头，前端识别为示例内容
export const seedPosts = [
  // ── 技术探讨 ──
  {
    id: 'seed-tech-1', board: 'tech', authorName: '夜航船', category: '技术探讨',
    title: 'Vue 3 的 shallowRef 到底该不该用？我踩了两次坑才想明白',
    summary: 'shallowRef 只追踪 .value 的引用替换，深层属性变更不会触发更新。对大型只读数据它能把 diff 成本砍掉一大截，但只要忘了手动 triggerRef，就会掉进「数据变了、视图不动」的坑里。',
    tags: 'Vue3,性能优化,响应式', hoursAgo: 3, likeCount: 42, viewCount: 386, replyCount: 12,
  },
  {
    id: 'seed-tech-2', board: 'tech', authorName: '贴地飞行', category: '技术探讨',
    title: '为什么我最后把原生 WebSocket 换成了 Socket.IO',
    summary: '原生 WS 在移动网络抖动、手机息屏后连接会假死，没有心跳也没有自动重连。换成 Socket.IO 之后，心跳、断线重连、长轮询降级都是自带的，前端只需要监听状态就行。',
    tags: 'Socket.IO,实时通信,踩坑', hoursAgo: 9, likeCount: 61, viewCount: 720, replyCount: 23,
  },
  {
    id: 'seed-tech-3', board: 'tech', authorName: '半盏清茶', category: '技术探讨',
    title: 'MySQL 联合索引的最左匹配，别等慢查询找上门才想起来',
    summary: '一次线上接口 P95 从 800ms 掉到 90ms，改动只是把联合索引的字段顺序调换了。最左匹配这件事，写在文档里没人看，写在慢查询日志里谁都记得住。',
    tags: 'MySQL,索引,调优', hoursAgo: 26, likeCount: 88, viewCount: 1043, replyCount: 31,
  },
  {
    id: 'seed-tech-4', board: 'tech', authorName: '码上开花', category: '技术探讨',
    title: 'Docker 镜像从 1.2G 瘦到 180M，我只做了三件事',
    summary: '换多阶段构建、合并 RUN 层、剔除 devDependencies 与缓存目录。镜像瘦下来之后，部署拉取时间从 4 分钟变成 20 秒，CI 也跟着快了一截。',
    tags: 'Docker,CI,部署', hoursAgo: 51, likeCount: 55, viewCount: 612, replyCount: 9,
  },

  // ── 抽象思考 ──
  {
    id: 'seed-abstract-1', board: 'abstract', authorName: '山有木兮', category: '抽象思考',
    title: '如果时间不是线性的，那我们到底在纪念什么？',
    summary: '所有的纪念都建立在「时间一去不回」这个前提上。可如果过去并没有消失、只是我们无法访问，那纪念这件事，纪念的是事件本身，还是我们自己的记忆版本？',
    tags: '时间,记忆,哲学', hoursAgo: 5, likeCount: 37, viewCount: 298, replyCount: 18,
  },
  {
    id: 'seed-abstract-2', board: 'abstract', authorName: '路人乙', category: '抽象思考',
    title: '「努力就有回报」会不会是一种幸存者偏差的叙事？',
    summary: '我们听到的成功故事，都是努力并且恰好被回报的人讲的。那些同样努力却没有回报的人，没有麦克风。承认运气的作用，不是否定努力，而是别把失败全归因于自己不够拼。',
    tags: '幸存者偏差,叙事,思考', hoursAgo: 14, likeCount: 96, viewCount: 874, replyCount: 42,
  },
  {
    id: 'seed-abstract-3', board: 'abstract', authorName: '林深见鹿', category: '抽象思考',
    title: '为什么深夜的我们，总是比白天更诚实？',
    summary: '白天的坦诚要付出社交成本，深夜的坦诚只需要面对自己。也许不是我们变了，只是那个时刻，没有别人在看。',
    tags: '夜晚,自我,情绪', hoursAgo: 33, likeCount: 74, viewCount: 651, replyCount: 27,
  },

  // ── 牛马工作 ──
  {
    id: 'seed-work-1', board: 'work', authorName: '老周', category: '牛马工作',
    title: '入职三个月，我终于明白「对齐」这两个字有多贵',
    summary: '一次需求改了三版，不是因为方案难，而是因为每次对齐的都是不同的人。开会不是为了推进事情，而是为了确认大家说的是同一件事。',
    tags: '职场,沟通,吐槽', hoursAgo: 2, likeCount: 128, viewCount: 1520, replyCount: 56,
  },
  {
    id: 'seed-work-2', board: 'work', authorName: '打工人小陈', category: '牛马工作',
    title: '周一早上打开邮箱，128 封未读里真正需要我的有几封？',
    summary: '抄送的、通知的、别人已经在群里讨论完的……真正等我决策的往往只有两三封。学会分辨「被抄送」和「被需要」，是打工人的第一课。',
    tags: '效率,邮箱,日常', hoursAgo: 20, likeCount: 45, viewCount: 512, replyCount: 15,
  },
  {
    id: 'seed-work-3', board: 'work', authorName: '阿宝', category: '牛马工作',
    title: '面试官问「你的职业规划是什么」，怎么答才不像背台词？',
    summary: '别说「三年做到主管、五年做到总监」。说你最近在补什么短板、想往哪个方向深挖、为什么这家公司能给你这个场景，比任何头衔都可信。',
    tags: '面试,求职,规划', hoursAgo: 44, likeCount: 82, viewCount: 1180, replyCount: 34,
  },
  {
    id: 'seed-work-4', board: 'work', authorName: '通勤人', category: '牛马工作',
    title: '每天通勤两小时换来的那份工资，到底值不值得？',
    summary: '算过一笔账：一年 480 小时在路上，相当于多上了两个月的班。有人用这两小时刷完了播客和课程，有人只是更累了。值不值，取决于你把这两小时当成了什么。',
    tags: '通勤,生活成本,选择', hoursAgo: 68, likeCount: 63, viewCount: 690, replyCount: 29,
  },

  // ── 感情 ──
  {
    id: 'seed-love-1', board: 'love', authorName: '南风知我意', category: '感情',
    title: '和不合适的人在一起，最累的其实是「解释自己」',
    summary: '你解释为什么要加班、为什么要独处、为什么突然不想说话。解释到最后你会发现，一段需要反复解释的关系，本身就已经给出了答案。',
    tags: '关系,沟通,情绪', hoursAgo: 6, likeCount: 152, viewCount: 1340, replyCount: 61,
  },
  {
    id: 'seed-love-2', board: 'love', authorName: '一只猫的午后', category: '感情',
    title: '成年人的友情，都是从「不用解释」开始的',
    summary: '半年不联系，一个电话就能接着上次的话题往下聊；不用寒暄也不用找理由。能让你不必解释的关系，才是真正省力的关系。',
    tags: '友情,成长,陪伴', hoursAgo: 29, likeCount: 87, viewCount: 764, replyCount: 22,
  },
  {
    id: 'seed-love-3', board: 'love', authorName: '雾里看花', category: '感情',
    title: '喜欢一个人很久之后，会不会突然就喜欢不动了？',
    summary: '不是某一天突然不爱了，而是某一次你发出去的消息，没有像以前那样紧张地等回复了。感情的消退从来不是断崖，是一点点变安静。',
    tags: '喜欢,时间,心事', hoursAgo: 58, likeCount: 104, viewCount: 982, replyCount: 38,
  },

  // ── 金融 ──
  {
    id: 'seed-finance-1', board: 'finance', authorName: '算账的人', category: '金融',
    title: '为什么我劝普通人别碰杠杆',
    summary: '杠杆放大的从来不是收益，而是波动。10 倍杠杆下，标的跌 10% 你就出局了，而这在正常行情里一周就能发生。活着比赚得多更重要。',
    tags: '投资,风险,杠杆', hoursAgo: 8, likeCount: 116, viewCount: 1408, replyCount: 47,
  },
  {
    id: 'seed-finance-2', board: 'finance', authorName: '慢慢变富', category: '金融',
    title: '第一笔闲钱到手，先做这三件事再谈投资',
    summary: '一、留出 6 个月生活费的应急金；二、把高息负债还掉；三、想清楚这笔钱多久不用。做完这三步，剩下的钱才叫「可以投资的钱」。',
    tags: '理财,应急金,规划', hoursAgo: 36, likeCount: 93, viewCount: 1120, replyCount: 25,
  },
  {
    id: 'seed-finance-3', board: 'finance', authorName: '指数信徒', category: '金融',
    title: '黄金、宽基指数、债券，普通人到底该怎么配比例？',
    summary: '没有万能比例，只有「亏多少你还能睡着」。先确定能承受的最大回撤，再按股债金的比例倒推配置。仓位管理比择时重要一百倍。',
    tags: '配置,资产,比例', hoursAgo: 73, likeCount: 71, viewCount: 856, replyCount: 33,
  },

  // ── 吃瓜 ──
  {
    id: 'seed-gossip-1', board: 'gossip', authorName: '瓜田里的猹', category: '吃瓜',
    title: '某大厂「毕业」名单又更新了，聊聊这轮调整的几个信号',
    summary: '先从边缘业务收缩、再到中台合并、最后才是核心团队优化——这几乎是每轮调整的标准顺序。看到第一个信号的时候，就该开始更新简历了。',
    tags: '行业,裁员,观察', hoursAgo: 4, likeCount: 134, viewCount: 1860, replyCount: 72,
  },
  {
    id: 'seed-gossip-2', board: 'gossip', authorName: '时间线整理员', category: '吃瓜',
    title: '昨天刷屏的那个瓜，我把时间线捋了一遍',
    summary: '各方声明、截图、反转、再反转……按时间顺序排下来，其实漏洞在第一份声明里就有了。吃瓜最好的姿势是等三天再看。',
    tags: '热搜,时间线,反转', hoursAgo: 22, likeCount: 158, viewCount: 2140, replyCount: 88,
  },
  {
    id: 'seed-gossip-3', board: 'gossip', authorName: '开源观察', category: '吃瓜',
    title: '开源项目被商业公司「白嫖」，这事你怎么看？',
    summary: '作者免费写代码，公司拿去做商业产品还不署名。许可证写清楚了是合规，但合规和体面之间，隔着一条叫做「回馈」的沟。',
    tags: '开源,许可证,争议', hoursAgo: 47, likeCount: 102, viewCount: 1290, replyCount: 54,
  },

  // ── 搞笑分享 ──
  {
    id: 'seed-humor-1', board: 'humor', authorName: '段子手阿伟', category: '搞笑分享',
    title: '程序员最怕听到的十句话，第一句就破防了',
    summary: '「这个需求很简单，你随便改改就行」「昨天还好好的，今天怎么不行了」「你先做出来，我们再看看效果」……每一句都能精准击中打工人的痛觉神经。',
    tags: '段子,程序员,日常', hoursAgo: 1, likeCount: 186, viewCount: 2210, replyCount: 66,
  },
  {
    id: 'seed-humor-2', board: 'humor', authorName: '我妈以为我在修电脑', category: '搞笑分享',
    title: '亲戚问我「能不能帮我修个电脑」，我该怎么说？',
    summary: '说「能」的人，接下来一个星期都在装系统；说「不能」的人，会被说「读了这么多年书连这都不会」。标准答案：先问你是什么系统。',
    tags: '搞笑,亲戚,互联网', hoursAgo: 17, likeCount: 143, viewCount: 1680, replyCount: 49,
  },
  {
    id: 'seed-humor-3', board: 'humor', authorName: '注释收藏家', category: '搞笑分享',
    title: '这些沙雕代码注释，建议收藏备用',
    summary: '「// 别碰这段，它自己会跑」「// 我也不知道为什么，但删了会崩」「// 如果这个 bug 又出现了，请参考上一条注释」——前人的血泪，后人的宝藏。',
    tags: '搞笑,代码注释,收藏', hoursAgo: 40, likeCount: 121, viewCount: 1450, replyCount: 37,
  },
].map(p => ({
  ...p,
  createdAt: hoursAgo(p.hoursAgo),
  type: 'discussion',
  isSeed: true,
}))
