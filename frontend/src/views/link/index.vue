<template>
  <div class="link-page" v-reveal>
    <!-- 背景层 -->
    <div class="link-bg" aria-hidden="true">
      <div class="bg-glow-a"></div>
      <div class="bg-glow-b"></div>
    </div>

    <div class="link-container">
      <!-- 页头 -->
      <header class="link-header">
        <span class="link-badge">◈ FRIENDS · 江湖相逢</span>
        <h1 class="link-title">友链</h1>
        <p class="link-sub">往来皆是同路人 · 这些是本站真正交换过链接的伙伴</p>
        <div class="link-stat">
          <span><b>{{ total }}</b> 位伙伴</span>
          <span class="dot">│</span>
          <span><b>{{ groups.length }}</b> 个分类</span>
          <span class="dot">│</span>
          <span>持续更新中</span>
        </div>
      </header>

      <!-- 分类分组 -->
      <section v-for="g in groups" :key="g.name" class="link-group">
        <div class="group-head">
          <span class="group-icon">{{ g.icon }}</span>
          <h2 class="group-name">{{ g.name }}</h2>
          <span class="group-en">{{ g.en }}</span>
          <span class="group-count">{{ g.items.length }}</span>
        </div>

        <div class="link-grid">
          <a v-for="l in g.items" :key="l.url" :href="l.url" target="_blank" rel="noopener"
            class="link-card" :style="{ '--accent': g.color }" data-glow>
            <span class="lc-avatar">{{ initial(l.name) }}</span>
            <div class="lc-body">
              <div class="lc-top">
                <h3 class="lc-name">{{ l.name }}</h3>
                <span class="lc-arrow">↗</span>
              </div>
              <p class="lc-desc">{{ l.desc }}</p>
              <span class="lc-host">{{ host(l.url) }}</span>
            </div>
          </a>
        </div>
      </section>

      <!-- 申请友链 -->
      <div class="link-foot glass-panel">
        <span class="lf-icon">🤝</span>
        <div class="lf-text">
          <p class="lf-title">想交换友链？</p>
          <p class="lf-desc">欢迎在留言板留言，或通过关于页的联系方式找到我。本站只收录内容稳定、长期更新的独立博客。</p>
        </div>
        <router-link to="/comments" class="lf-btn">去留言 →</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
// ====================================================
// 友链页：按分类展示真实交换的友情链接
// 数据来自参考站 https://lznauy.cn/friends/ 的友链清单，
// 已剔除原页面里属于同一站点的工具型子域（探针/图床/云盘等非友链条目）
// ====================================================
import { computed } from 'vue'

const groups = [
  {
    name: '小伙伴们',
    en: 'COMPANIONS',
    icon: '🍃',
    color: '#06b6d4',
    items: [
      { name: '清羽飞扬', url: 'https://blog.liushen.fun/', desc: '柳影曳曳，清酒孤灯，扬笔撒墨，心境如霜' },
      { name: '逸风亭', url: 'https://blog.fyz666.xyz/', desc: '行所欲行，无问西东。' },
      { name: 'lololowe的博客', url: 'https://blog.lololowe.com/', desc: '计网，网安知识分享' },
      { name: '短巷与雨', url: 'https://www.hudi.space/', desc: '为学日益，为道日损。' },
      { name: 'luoboQAQ的博客', url: 'https://lbqaq.top/', desc: '快乐学习每一天' },
      { name: '小十的个人博客', url: 'https://www.xiaoten.com/', desc: '一个调配电流、像素和代码的 ISFP 型导体' },
      { name: 'LHL\u2019s Blog', url: 'https://blog.lhl.one/', desc: '能烂尾的结局，过程一定很精彩' },
    ],
  },
  {
    name: '技术周刊类',
    en: 'TECH WEEKLY',
    icon: '📮',
    color: '#a855f7',
    items: [
      { name: 'cosの博客', url: 'https://blog.cosine.ren/', desc: 'WA 的一声就哭了' },
      { name: '二丫讲梵', url: 'https://wiki.eryajf.net/', desc: '💻 学习 📝 记录 🔗 分享' },
      { name: '阮一峰的网络日志', url: 'https://www.ruanyifeng.com/blog/', desc: '阮一峰的个人博客' },
      { name: '阿猫的博客', url: 'https://ameow.xyz/', desc: 'Code for fun, and for ever.' },
      { name: 'Tw93', url: 'https://tw93.fun/', desc: 'Mole 的作者' },
      { name: 'AgedCoffee', url: 'https://aged-coffee.me/', desc: '一个记录书影音和生活随想的地方' },
      { name: '我没有三颗心脏', url: 'https://wmyskxz.cn/', desc: '96 年的一枚自由技术人' },
      { name: 'Airing 的小屋', url: 'https://ursb.me/', desc: '应无所住而生其心' },
    ],
  },
  {
    name: '生活随笔',
    en: 'LIFE',
    icon: '☕',
    color: '#10b981',
    items: [
      { name: '崔话记', url: 'https://cuixiping.com/', desc: '向着理想的方向，爬一会儿，躺一会儿' },
      { name: '流情的博客', url: 'https://liuqingwushui.top/', desc: '希望能与你在比特之海的繁星之下相见' },
      { name: '云心怀鹤', url: 'https://bluehe.cn/', desc: '寻真旅摄，笔墨纪实' },
      { name: '喵喵小站・博客志', url: 'https://www.mmbkz.cn/', desc: '记录喵喵的美好生活' },
      { name: '周天记', url: 'https://zhoutian.com/', desc: '我在人间混日子' },
      { name: '网友小宋', url: 'https://xyzbz.cn/', desc: '一介草民，只为生活！' },
      { name: '威言威语', url: 'https://www.weisay.com/', desc: '分享技术、生活、思考' },
      { name: '魏文超\u2019s Blog', url: 'https://www.weiwenchao.com/', desc: '感性地生活，理性地思考，一个八零后的个人博客' },
      { name: '雅余', url: 'https://yayu.net/', desc: '一位咖啡上瘾的产品经理，曾经是位设计师' },
      { name: '瓦匠不舟', url: 'https://airy.ink/', desc: '工控从业者一枚，自学了一些开发相关知识' },
      { name: 'Jack\u2019s Space', url: 'https://veryjack.com/', desc: '一切都是最好的安排' },
      { name: 'xiaogai', url: 'https://xiaogai.fun/', desc: '挺好看的主题博客' },
    ],
  },
  {
    name: '宝藏博主',
    en: 'TREASURE',
    icon: '💎',
    color: '#ec4899',
    items: [
      { name: '唐巧的博客', url: 'https://blog.devtang.com/', desc: '记录下自己学习的点滴' },
      { name: 'JIMMY SONG', url: 'https://jimmysong.io/', desc: 'AI-Native 云原生布道师' },
      { name: '故事的程序猿', url: 'https://blog.lichenghao.cn/', desc: '好好学习，天天向上 ↑' },
      { name: '刘郎阁', url: 'https://vjo.cc/', desc: '一个积极向上的生活探索者！' },
      { name: 'YeungYeah 的乱写地', url: 'https://scottyeung.top/', desc: '精通中英粤三语的地道广东人' },
      { name: 'Xinwei (bear) Xiong', url: 'https://nsddd.top/', desc: '挺优秀的博主' },
      { name: '晚夜的个人博客', url: 'https://www.iczrx.cn/', desc: '做好量变的准备，促进事物的质变。' },
      { name: 'xaoxuu', url: 'https://xaoxuu.com/', desc: 'For all time, always.' },
      { name: '周鑫的个人博客', url: 'https://www.zhouxin.space/', desc: 'Time goes on. We live. We learn.' },
      { name: '宇阳', url: 'https://liuyuyang.net/', desc: '记录所学知识，缩短和大神的差距！' },
      { name: '意琦行的个人博客', url: 'https://www.lixueduan.com/', desc: '探索云原生' },
      { name: '菲兹克斯喵', url: 'https://physnya.top/', desc: 'Eclipse first, the rest nowhere.' },
      { name: 'Lil\u2019Log', url: 'https://lilianweng.github.io/', desc: 'Hi, this is Lilian.' },
      { name: '熊喵君', url: 'https://pandaychen.github.io/', desc: 'Hey, this is 熊喵君 (pandaychen).' },
      { name: '乔克视界', url: 'https://jokerbai.com/', desc: '云原生爱好者' },
      { name: 'soda-official', url: 'https://soda-official.top/', desc: '使一颗心免于哀伤.' },
      { name: '小改学习志', url: 'https://www.haoyu233.com/', desc: '尘埃尚未落定，一切皆有可能。' },
      { name: '吐槽大王部落格', url: 'https://www.tcdw.net/', desc: '正在学习接受不完美的世界' },
      { name: '雪松', url: 'https://fylsen.com/', desc: '向路灯照不亮的身影说声晚安' },
      { name: 'AI超元域的博客', url: 'https://www.aivi.fyi/', desc: '探索人工智能前沿、分享技术心得' },
      { name: '时歌的博客', url: 'https://www.lapis.cafe/', desc: '理解以真实为本，但真实本身并不会自动呈现' },
      { name: 'Wincer.Blog', url: 'https://blog.itswincer.com/', desc: '分享技术、生活、思考' },
      { name: 'Magren\u2019s Blog', url: 'https://magren.cc/', desc: '写点自己想写的东西' },
      { name: 'liruifengv', url: 'https://liruifengv.com/', desc: 'Web 开发者，Astro 项目成员，开源爱好者' },
    ],
  },
]

const total = computed(() => groups.reduce((n, g) => n + g.items.length, 0))

// 头像占位：取名称首字（中文取首字，英文取首字母大写）
function initial(name) {
  return (name || '友').trim()[0].toUpperCase()
}

// 展示域名（去掉协议与 www. 前缀）
function host(url) {
  try { return new URL(url).hostname.replace(/^www\./, '') } catch { return url }
}
</script>

<style scoped>
.link-page {
  position: relative;
  min-height: 100vh;
  padding: 4rem 1.5rem 5rem;
  overflow-x: hidden;
}

.link-bg {
  position: fixed;
  inset: 0;
  z-index: -1;
  pointer-events: none;
}
.bg-glow-a, .bg-glow-b {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
}
.bg-glow-a {
  width: 560px; height: 560px; top: -12%; left: -8%;
  background: radial-gradient(circle, rgba(6, 182, 212, 0.14), transparent 70%);
}
.bg-glow-b {
  width: 520px; height: 520px; bottom: -12%; right: -8%;
  background: radial-gradient(circle, rgba(168, 85, 247, 0.13), transparent 70%);
}

.link-container {
  position: relative;
  max-width: 1100px;
  margin: 0 auto;
}

/* ── 页头 ── */
.link-header {
  text-align: center;
  margin-bottom: 3.2rem;
}
.link-badge {
  display: inline-block;
  font-size: 0.62rem;
  letter-spacing: 0.28em;
  color: #00d4ff;
  font-family: 'Courier New', monospace;
  padding: 0.35rem 0.9rem;
  border: 1px solid rgba(0, 212, 255, 0.28);
  border-radius: 999px;
  background: rgba(0, 212, 255, 0.05);
  margin-bottom: 1rem;
}
.link-title {
  font-size: clamp(2rem, 5vw, 3rem);
  font-weight: 900;
  letter-spacing: 0.1em;
  background: linear-gradient(135deg, #06b6d4, #a855f7 55%, #ec4899);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  margin-bottom: 0.7rem;
}
.link-sub {
  font-size: 0.82rem;
  color: rgba(255, 255, 255, 0.5);
  letter-spacing: 0.04em;
  margin-bottom: 1rem;
}
.link-stat {
  display: inline-flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 0.7rem;
  font-size: 0.68rem;
  color: rgba(255, 255, 255, 0.42);
  font-family: 'Courier New', monospace;
}
.link-stat b { color: #00d4ff; font-weight: 700; }
.link-stat .dot { color: rgba(255, 255, 255, 0.16); }

/* ── 分组 ── */
.link-group { margin-bottom: 3rem; }

.group-head {
  display: flex;
  align-items: center;
  gap: 0.7rem;
  margin-bottom: 1.2rem;
  padding-bottom: 0.7rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.group-icon { font-size: 1.05rem; }
.group-name {
  font-size: 1.05rem;
  font-weight: 800;
  color: #fff;
  letter-spacing: 0.05em;
}
.group-en {
  font-size: 0.56rem;
  letter-spacing: 0.26em;
  color: rgba(255, 255, 255, 0.3);
  font-family: 'Courier New', monospace;
}
.group-count {
  margin-left: auto;
  font-size: 0.6rem;
  color: rgba(255, 255, 255, 0.35);
  font-family: 'Courier New', monospace;
  padding: 0.12rem 0.55rem;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 999px;
}

/* ── 卡片网格 ── */
.link-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.9rem;
}

.link-card {
  display: flex;
  align-items: flex-start;
  gap: 0.85rem;
  padding: 1rem 1.05rem;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: linear-gradient(150deg, rgba(22, 20, 46, 0.72), rgba(10, 10, 24, 0.6));
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  text-decoration: none;
  transition: transform 0.3s cubic-bezier(0.22, 1, 0.36, 1), border-color 0.3s, box-shadow 0.3s;
}
.link-card:hover {
  transform: translateY(-4px);
  border-color: color-mix(in srgb, var(--accent) 55%, transparent);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.42), 0 0 22px color-mix(in srgb, var(--accent) 26%, transparent);
}

.lc-avatar {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 11px;
  display: grid;
  place-items: center;
  font-size: 1.05rem;
  font-weight: 800;
  color: #fff;
  background: linear-gradient(140deg, color-mix(in srgb, var(--accent) 70%, #1a1a2e), color-mix(in srgb, var(--accent) 22%, #0d0d20));
  border: 1px solid color-mix(in srgb, var(--accent) 34%, transparent);
}

.lc-body { flex: 1; min-width: 0; }

.lc-top {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}
.lc-name {
  font-size: 0.86rem;
  font-weight: 700;
  color: #fff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.link-card:hover .lc-name { color: var(--accent); }
.lc-arrow {
  margin-left: auto;
  font-size: 0.85rem;
  color: rgba(255, 255, 255, 0.28);
  transition: transform 0.3s, color 0.3s;
}
.link-card:hover .lc-arrow {
  color: var(--accent);
  transform: translate(2px, -2px);
}

.lc-desc {
  font-size: 0.7rem;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 0.28rem;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.lc-host {
  display: block;
  margin-top: 0.45rem;
  font-size: 0.58rem;
  color: rgba(255, 255, 255, 0.28);
  font-family: 'Courier New', monospace;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ── 底部申请 ── */
.link-foot {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-top: 3.4rem;
  padding: 1.5rem 1.8rem;
}
.lf-icon { font-size: 1.6rem; }
.lf-text { flex: 1; min-width: 0; }
.lf-title { font-size: 0.92rem; font-weight: 700; color: #fff; margin-bottom: 0.25rem; }
.lf-desc { font-size: 0.72rem; line-height: 1.7; color: rgba(255, 255, 255, 0.5); }
.lf-btn {
  flex-shrink: 0;
  padding: 0.6rem 1.2rem;
  font-size: 0.72rem;
  font-weight: 600;
  color: #fff;
  border-radius: 10px;
  text-decoration: none;
  background: linear-gradient(135deg, #06b6d4, #a855f7);
  transition: transform 0.3s, box-shadow 0.3s;
}
.lf-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 24px rgba(168, 85, 247, 0.35);
}

/* ── 响应式 ── */
@media (max-width: 900px) {
  .link-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 600px) {
  .link-page { padding: 2.6rem 1rem 3.4rem; }
  .link-grid { grid-template-columns: 1fr; }
  .link-foot { flex-direction: column; text-align: center; align-items: center; }
  .group-en { display: none; }
}
</style>
