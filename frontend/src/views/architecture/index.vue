<template>
  <div class="arch-page">
    <!-- HERO -->
    <section class="hero" ref="hero">
      <div class="hero-back"><PageBack label="返回首页" to="/" /></div>
      <div class="hero-bg">
        <div class="orb o1"></div>
        <div class="orb o2"></div>
        <div class="grid-lines"></div>
      </div>
      <div class="hero-inner">
        <div class="badge fade">● MICROSERVICES · SPRING CLOUD ALIBABA</div>
        <h1 class="title fade">微服务架构拓扑</h1>
        <p class="sub fade">
          13 个业务服务 + 统一网关 + Nacos 注册配置中心：独立部署、独立伸缩，
          以消息队列解耦，用分布式链路追踪观测全局。
        </p>
        <div class="tags fade">
          <span v-for="t in heroTags" :key="t">#{{ t }}</span>
        </div>
      </div>
    </section>

    <!-- 分层拓扑 -->
    <section class="topo">
      <div class="sec-head">
        <span class="sec-num">[ 01 ]</span>
        <h2>分层拓扑</h2>
        <p>SERVICE TOPOLOGY · 4 LAYERS</p>
      </div>

      <!-- Layer 1: 客户端 -->
      <div class="layer">
        <div class="layer-label"><span>L1</span> 客户端层</div>
        <div class="layer-row client-row">
          <div class="node client">
            <div class="node-icon">🌐</div>
            <div class="node-name">Web SPA</div>
            <div class="node-sub">Vue3 · Vite</div>
          </div>
          <div class="node client">
            <div class="node-icon">📱</div>
            <div class="node-name">移动端</div>
            <div class="node-sub">H5 / PWA</div>
          </div>
        </div>
      </div>

      <div class="flow-arrow">↓ HTTPS / WSS</div>

      <!-- Layer 2: 网关 -->
      <div class="layer">
        <div class="layer-label"><span>L2</span> 网关层</div>
        <div class="layer-row">
          <div class="node gateway">
            <div class="node-icon">🛡️</div>
            <div class="node-name">API 网关</div>
            <div class="node-sub">Spring Cloud Gateway · :8080</div>
          </div>
        </div>
      </div>

      <div class="flow-arrow">↓ 路由 · 鉴权 · 限流 · 熔断</div>

      <!-- Layer 3: 业务服务 -->
      <div class="layer">
        <div class="layer-label"><span>L3</span> 业务服务层（13 服务）</div>
        <div class="services-grid">
          <div v-for="s in services" :key="s.name" class="node service" :data-cat="s.cat">
            <div class="node-icon">{{ s.icon }}</div>
            <div class="node-name">{{ s.name }}</div>
            <div class="node-sub">:{{ s.port }}</div>
            <div class="node-db">🗄️ {{ s.db }}</div>
          </div>
        </div>
      </div>

      <div class="flow-arrow">↓ 数据访问 · 消息通信</div>

      <!-- Layer 4: 数据与基础设施 -->
      <div class="layer">
        <div class="layer-label"><span>L4</span> 数据与基础设施层</div>
        <div class="infra-grid">
          <div class="node infra">
            <div class="node-icon">🗄️</div>
            <div class="node-name">MySQL</div>
            <div class="node-sub">14 业务库 · :3306</div>
          </div>
          <div class="node infra">
            <div class="node-icon">☁️</div>
            <div class="node-name">Supabase</div>
            <div class="node-sub">博客 + K线历史</div>
          </div>
          <div class="node infra">
            <div class="node-icon">⚡</div>
            <div class="node-name">Redis</div>
            <div class="node-sub">缓存 · 会话 · :6379</div>
          </div>
          <div class="node infra">
            <div class="node-icon">📡</div>
            <div class="node-name">Nacos</div>
            <div class="node-sub">注册 + 配置中心</div>
          </div>
          <div class="node infra">
            <div class="node-icon">🔁</div>
            <div class="node-name">RabbitMQ</div>
            <div class="node-sub">异步解耦 · :5672</div>
          </div>
          <div class="node infra">
            <div class="node-icon">🪣</div>
            <div class="node-name">MinIO</div>
            <div class="node-sub">对象存储</div>
          </div>
        </div>
      </div>
    </section>

    <!-- 服务清单 -->
    <section class="svc-list">
      <div class="sec-head">
        <span class="sec-num">[ 02 ]</span>
        <h2>服务清单</h2>
        <p>SERVICE REGISTRY</p>
      </div>
      <div class="svc-table">
        <div class="svc-th">
          <span>服务</span><span>端口</span><span>数据库</span><span>职责</span>
        </div>
        <div v-for="s in services" :key="s.name" class="svc-tr">
          <span class="svc-name"><i>{{ s.icon }}</i>{{ s.name }}</span>
          <span class="mono">:{{ s.port }}</span>
          <span class="db-tag">{{ s.db }}</span>
          <span class="svc-desc">{{ s.desc }}</span>
        </div>
      </div>
    </section>

    <!-- 数据存储分布 -->
    <section class="data-map">
      <div class="sec-head">
        <span class="sec-num">[ 03 ]</span>
        <h2>数据存储分布</h2>
        <p>DATA STORAGE MAP</p>
      </div>
      <div class="dm-grid">
        <!-- MySQL -->
        <div class="dm-card mysql">
          <div class="dm-head">
            <span class="dm-icon">🗄️</span>
            <h3>MySQL（本地 · 14 库）</h3>
            <span class="dm-tag">主存储</span>
          </div>
          <ul class="dm-list">
            <li v-for="db in mysqlDbs" :key="db.name">
              <strong>{{ db.name }}</strong>
              <span class="dm-tables">{{ db.tables }}</span>
            </li>
          </ul>
        </div>
        <!-- Supabase -->
        <div class="dm-card supabase">
          <div class="dm-head">
            <span class="dm-icon">☁️</span>
            <h3>Supabase（云端 Postgres）</h3>
            <span class="dm-tag">直连 + RLS</span>
          </div>
          <ul class="dm-list">
            <li><strong>blog_articles</strong><span class="dm-tables">博客文章（前端主数据源，RLS 只读已发布）</span></li>
            <li><strong>kline 历史</strong><span class="dm-tables">量化 K 线历史数据（quant-py-service 数据源）</span></li>
          </ul>
          <p class="dm-note">前端 publishable key 直连，写操作走后端网关兜底</p>
        </div>
        <!-- Redis / MinIO -->
        <div class="dm-card cache">
          <div class="dm-head">
            <span class="dm-icon">⚡</span>
            <h3>Redis（本地）</h3>
            <span class="dm-tag">缓存</span>
          </div>
          <ul class="dm-list">
            <li><strong>会话 / Token</strong><span class="dm-tables">登录态、JWT 黑名单</span></li>
            <li><strong>热点缓存</strong><span class="dm-tables">文章、商品、行情快照</span></li>
            <li><strong>分布式锁</strong><span class="dm-tables">秒杀、库存扣减</span></li>
          </ul>
        </div>
        <div class="dm-card obj">
          <div class="dm-head">
            <span class="dm-icon">🪣</span>
            <h3>MinIO（本地）</h3>
            <span class="dm-tag">对象存储</span>
          </div>
          <ul class="dm-list">
            <li><strong>图片</strong><span class="dm-tables">博客封面、头像、商品图</span></li>
            <li><strong>音视频</strong><span class="dm-tables">音乐、视频、美甲作品</span></li>
            <li><strong>文件</strong><span class="dm-tables">软件包、资源附件</span></li>
          </ul>
        </div>
      </div>
    </section>

    <!-- 请求流转 -->
    <section class="flow">
      <div class="sec-head">
        <span class="sec-num">[ 04 ]</span>
        <h2>请求流转链路</h2>
        <p>REQUEST FLOW</p>
      </div>
      <div class="flow-steps">
        <div v-for="(f, i) in flowSteps" :key="i" class="flow-step">
          <div class="fs-num">{{ String(i + 1).padStart(2, '0') }}</div>
          <div class="fs-body">
            <h4>{{ f.title }}</h4>
            <p>{{ f.desc }}</p>
          </div>
          <div v-if="i < flowSteps.length - 1" class="fs-arrow">→</div>
        </div>
      </div>
    </section>

    <!-- 技术栈 -->
    <section class="stack">
      <div class="sec-head">
        <span class="sec-num">[ 05 ]</span>
        <h2>技术栈</h2>
        <p>TECH STACK</p>
      </div>
      <div class="stack-grid">
        <div v-for="(c, i) in stack" :key="c.name" class="stack-card">
          <h3>{{ c.name }}</h3>
          <p>{{ c.desc }}</p>
          <div class="stack-tags">
            <span v-for="t in c.tags" :key="t">{{ t }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA -->
    <section class="cta-sec">
      <h2>想看链路细节？</h2>
      <p>在社区发帖，或浏览「硬核干货」模块的架构实战笔记</p>
      <router-link to="/community" class="cta">前往社区 →</router-link>
    </section>
  </div>
</template>

<script setup>
import { onMounted, onBeforeUnmount } from 'vue'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import PageBack from '@/components/PageBack.vue'

gsap.registerPlugin(ScrollTrigger)

const heroTags = ['Spring Cloud', 'Nacos', 'Gateway', 'Sentinel', 'RabbitMQ', 'Redis', 'Supabase', 'SkyWalking']

// 13 个业务服务（含网关外的全部）
const services = [
  { icon: '👤', name: 'user-service', port: 8081, db: 'web3_user', cat: 'core', desc: '用户注册登录、鉴权、权限、公会' },
  { icon: '📝', name: 'blog-service', port: 8082, db: 'web3_blog', cat: 'content', desc: '博客文章、分类、标签、友链、公告' },
  { icon: '💬', name: 'forum-service', port: 8083, db: 'web3_forum', cat: 'content', desc: '社区帖子、评论、点赞' },
  { icon: '🛍️', name: 'shop-service', port: 8084, db: 'web3_shop', cat: 'biz', desc: '商品、订单、支付' },
  { icon: '🎬', name: 'media-service', port: 8085, db: 'web3_media', cat: 'content', desc: '音乐、视频、相册、书籍、美甲' },
  { icon: '📈', name: 'quant-service', port: 8086, db: 'web3_quant', cat: 'biz', desc: '策略、行情日志、股票池' },
  { icon: '🛠️', name: 'tool-service', port: 8087, db: 'web3_tool', cat: 'biz', desc: '脚本执行、网站分享、工具资源' },
  { icon: '💿', name: 'software-service', port: 8088, db: 'web3_software', cat: 'biz', desc: '软件中心、版本管理' },
  { icon: '📦', name: 'resource-service', port: 8089, db: 'web3_resource', cat: 'biz', desc: '资源下载、素材库' },
  { icon: '🤖', name: 'ai-proxy-service', port: 8093, db: 'web3_admin', cat: 'ai', desc: 'AI 模型代理、Token、渠道、用量统计' },
  { icon: '🎙️', name: 'jarvis-service', port: 9001, db: 'web3_jarvis', cat: 'ai', desc: '语音助手、唤醒、会话记录' },
  { icon: '⚙️', name: 'admin-service', port: 9002, db: 'web3_admin', cat: 'core', desc: '后台管理、访问日志、服务健康' },
  { icon: '🛡️', name: 'gateway', port: 8080, db: '—', cat: 'core', desc: '统一入口、路由、鉴权、限流熔断' },
]

// MySQL 14 库及主要表
const mysqlDbs = [
  { name: 'web3_user', tables: 'user · guild · guild_member' },
  { name: 'web3_blog', tables: 'article · blog_setting · friend_link · site_notice' },
  { name: 'web3_forum', tables: 'post · comment · like_record' },
  { name: 'web3_media', tables: 'music · video · photo · book · nail_*' },
  { name: 'web3_shop', tables: 'product · order' },
  { name: 'web3_quant', tables: 'stock_price · strategy · quant_log' },
  { name: 'web3_tool', tables: 'script · site_share · software · resource' },
  { name: 'web3_software', tables: 'software' },
  { name: 'web3_resource', tables: 'resource' },
  { name: 'web3_admin', tables: 'visit_log · proxy_* · admin_*' },
  { name: 'web3_jarvis', tables: 'voice_command · voice_session' },
]

// 请求流转
const flowSteps = [
  { title: '客户端发起请求', desc: 'Web SPA / 移动端通过 HTTPS 请求 API，或 WebSocket 建立长连接' },
  { title: '网关统一入口', desc: 'Gateway 鉴权、路由分发、限流熔断、跨域处理，转发到对应服务' },
  { title: '服务处理', desc: '业务服务完成核心逻辑，读 MySQL / Supabase，写 Redis 缓存' },
  { title: '异步消息', desc: '耗时操作投递 RabbitMQ，由消费者异步处理（通知、日志、统计）' },
  { title: '数据持久化', desc: '结构化数据写 MySQL 分库，对象存 MinIO，热点回写 Redis' },
  { title: '观测与治理', desc: 'Nacos 注册发现、Sentinel 熔断、SkyWalking 链路追踪' },
]

const stack = [
  { name: 'Spring Cloud Alibaba', desc: '微服务治理全家桶：注册发现、配置中心、网关路由、熔断限流', tags: ['Nacos', 'Gateway', 'Sentinel'] },
  { name: '数据与消息', desc: 'MySQL 分库 + MyBatis-Plus，RabbitMQ 异步解耦，Redis 缓存热点', tags: ['MySQL', 'Redis', 'RabbitMQ'] },
  { name: '云端直连', desc: 'Supabase Postgres 承载博客与 K 线历史，前端 publishable key + RLS 直连', tags: ['Supabase', 'PostgreSQL', 'RLS'] },
  { name: '观测与部署', desc: 'SkyWalking 链路追踪，Docker 镜像 + K8s 编排滚动发布', tags: ['SkyWalking', 'Docker', 'K8s'] },
  { name: '对象存储', desc: 'MinIO 私有化对象存储，统一承载图片、音频与文件资源', tags: ['MinIO', 'S3'] },
  { name: '前端工程', desc: 'Vue3 Composition API + Vite + TailwindCSS，Three.js 沉浸式渲染', tags: ['Vue3', 'Vite', 'Three.js'] },
]

let ctx = null

onMounted(() => {
  ctx = gsap.context(() => {
    gsap.from('.fade', { y: 40, opacity: 0, duration: 1, stagger: 0.15, ease: 'power3.out' })
    gsap.from('.layer', {
      y: 50, opacity: 0, duration: 0.8, stagger: 0.15, ease: 'power3.out',
      scrollTrigger: { trigger: '.topo', start: 'top 70%', once: true },
    })
    gsap.from('.svc-tr', {
      x: -30, opacity: 0, duration: 0.5, stagger: 0.04, ease: 'power2.out',
      scrollTrigger: { trigger: '.svc-table', start: 'top 75%', once: true },
    })
    gsap.from('.dm-card', {
      y: 40, opacity: 0, duration: 0.7, stagger: 0.1, ease: 'power3.out',
      scrollTrigger: { trigger: '.dm-grid', start: 'top 75%', once: true },
    })
    gsap.from('.flow-step', {
      x: -40, opacity: 0, duration: 0.6, stagger: 0.1, ease: 'power3.out',
      scrollTrigger: { trigger: '.flow-steps', start: 'top 75%', once: true },
    })
    gsap.from('.stack-card', {
      y: 60, opacity: 0, duration: 0.9, stagger: 0.12, ease: 'power3.out',
      scrollTrigger: { trigger: '.stack-grid', start: 'top 80%', once: true },
    })
  })
})

onBeforeUnmount(() => { if (ctx) ctx.revert() })
</script>

<style scoped>
.arch-page { position: relative; min-height: 100vh; background: linear-gradient(180deg, #06060e, #0a0a1e); color: #e0e0f0; overflow: hidden; }

/* HERO */
.hero { position: relative; min-height: 72vh; display: flex; align-items: center; padding: 4rem 2rem; }
.hero-back { position: absolute; top: 24px; left: 24px; z-index: 20; }
@media (max-width: 640px) { .hero-back { top: 16px; left: 16px; } }
.hero-bg { position: absolute; inset: 0; pointer-events: none; }
.orb { position: absolute; border-radius: 50%; filter: blur(90px); animation: orbFloat 8s ease-in-out infinite; }
.orb.o1 { width: 420px; height: 420px; right: 6%; top: 8%; background: radial-gradient(circle, rgba(192, 38, 211, 0.28), transparent 65%); }
.orb.o2 { width: 360px; height: 360px; left: -60px; bottom: 4%; background: radial-gradient(circle, rgba(0, 229, 255, 0.16), transparent 65%); animation-delay: -4s; }
@keyframes orbFloat { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-26px); } }
.grid-lines { position: absolute; inset: 0; }

.hero-inner { position: relative; max-width: 980px; margin: 0 auto; text-align: center; }
.badge { display: inline-flex; padding: 0.45rem 1.1rem; border: 1px solid rgba(0, 229, 255, 0.4); border-radius: 4px; color: #7fd9ff; letter-spacing: 0.2em; font-size: 0.72rem; margin-bottom: 1.4rem; }
.title { font-size: clamp(2.4rem, 6vw, 4.6rem); font-weight: 900; letter-spacing: 0.06em; margin-bottom: 1.2rem; background: linear-gradient(135deg, #fff, #00e5ff 55%, #c026d3); -webkit-background-clip: text; background-clip: text; -webkit-text-fill-color: transparent; }
.sub { font-size: 1.02rem; line-height: 1.9; color: rgba(255,255,255,0.82); max-width: 640px; margin: 0 auto 1.6rem; }
.tags { display: flex; gap: 0.6rem; justify-content: center; flex-wrap: wrap; }
.tags span { font-size: 0.72rem; font-family: monospace; color: #7fd9ff; padding: 0.3rem 0.8rem; border: 1px solid rgba(0, 229, 255, 0.25); border-radius: 999px; background: rgba(0, 229, 255, 0.06); }

/* 通用 section head */
.sec-head { text-align: center; margin-bottom: 3rem; }
.sec-num { color: #00e5ff; font-family: monospace; font-size: 0.75rem; letter-spacing: 0.25em; }
.sec-head h2 { font-size: clamp(1.6rem, 3.4vw, 2.4rem); font-weight: 800; margin: 0.5rem 0 0.3rem; }
.sec-head p { font-size: 0.7rem; letter-spacing: 0.3em; color: rgba(255,255,255,0.6); }

/* 分层拓扑 */
.topo { position: relative; padding: 5rem 2rem; }
.layer { margin-bottom: 1.2rem; }
.layer-label { display: flex; align-items: center; gap: 0.6rem; margin-bottom: 0.9rem; font-size: 0.72rem; letter-spacing: 0.15em; color: rgba(255,255,255,0.5); font-family: monospace; }
.layer-label span { padding: 0.1rem 0.5rem; background: rgba(0, 229, 255, 0.12); border: 1px solid rgba(0, 229, 255, 0.3); border-radius: 4px; color: #00e5ff; }
.layer-row { display: flex; gap: 1rem; flex-wrap: wrap; justify-content: center; }
.flow-arrow { text-align: center; font-size: 0.7rem; color: rgba(0, 229, 255, 0.5); font-family: monospace; margin: 0.2rem 0 1rem; letter-spacing: 0.1em; }

.node { text-align: center; padding: 1.1rem 1.2rem; border-radius: 14px; background: rgba(12, 12, 28, 0.7); border: 1px solid rgba(255,255,255,0.09); backdrop-filter: blur(10px); transition: all 0.3s; min-width: 120px; }
.node:hover { border-color: rgba(0, 229, 255, 0.5); box-shadow: 0 0 26px rgba(0, 229, 255, 0.18); transform: translateY(-3px); }
.node-icon { font-size: 1.5rem; margin-bottom: 0.35rem; }
.node-name { font-size: 0.9rem; font-weight: 700; }
.node-sub { font-size: 0.58rem; letter-spacing: 0.1em; color: rgba(255,255,255,0.5); font-family: monospace; margin-top: 0.2rem; }
.node-db { font-size: 0.55rem; color: rgba(244, 63, 94, 0.8); font-family: monospace; margin-top: 0.3rem; }

.gateway { border-color: rgba(0, 229, 255, 0.4); background: linear-gradient(135deg, rgba(0, 229, 255, 0.12), rgba(192, 38, 211, 0.12)); }
.client { min-width: 140px; border-color: rgba(192, 38, 211, 0.3); }
.infra { min-width: 130px; border-color: rgba(34, 197, 94, 0.25); }

.services-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 0.7rem; }
.infra-grid { display: grid; grid-template-columns: repeat(6, 1fr); gap: 0.7rem; }
.client-row { gap: 1.5rem; }

/* 服务清单表格 */
.svc-list { padding: 4rem 2rem; }
.svc-table { max-width: 1080px; margin: 0 auto; border-radius: 14px; overflow: hidden; border: 1px solid rgba(255,255,255,0.08); background: rgba(12, 12, 28, 0.5); }
.svc-th, .svc-tr { display: grid; grid-template-columns: 1.4fr 0.7fr 1.2fr 2.5fr; gap: 1rem; padding: 0.85rem 1.3rem; align-items: center; }
.svc-th { background: rgba(0, 229, 255, 0.08); font-size: 0.72rem; letter-spacing: 0.12em; color: #7fd9ff; font-family: monospace; border-bottom: 1px solid rgba(0, 229, 255, 0.2); }
.svc-tr { font-size: 0.82rem; border-bottom: 1px solid rgba(255,255,255,0.05); transition: background 0.2s; }
.svc-tr:hover { background: rgba(192, 38, 211, 0.06); }
.svc-tr:last-child { border-bottom: none; }
.svc-name { display: flex; align-items: center; gap: 0.5rem; font-weight: 600; color: #e0e0f0; }
.svc-name i { font-style: normal; }
.mono { font-family: monospace; color: #fbbf24; }
.db-tag { font-family: monospace; font-size: 0.72rem; color: #f472b6; background: rgba(244, 63, 94, 0.1); padding: 0.15rem 0.5rem; border-radius: 4px; display: inline-block; width: fit-content; }
.svc-desc { color: rgba(255,255,255,0.6); font-size: 0.78rem; }

/* 数据存储分布 */
.data-map { padding: 4rem 2rem; }
.dm-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 1.3rem; max-width: 1080px; margin: 0 auto; }
.dm-card { padding: 1.6rem 1.7rem; border-radius: 16px; background: rgba(12, 12, 28, 0.65); border: 1px solid rgba(255,255,255,0.07); transition: all 0.35s; }
.dm-card:hover { border-color: rgba(0, 229, 255, 0.4); box-shadow: 0 0 34px rgba(0, 229, 255, 0.1); transform: translateY(-3px); }
.dm-head { display: flex; align-items: center; gap: 0.7rem; margin-bottom: 1.1rem; flex-wrap: wrap; }
.dm-icon { font-size: 1.4rem; }
.dm-head h3 { font-size: 1rem; font-weight: 700; }
.dm-tag { margin-left: auto; font-size: 0.6rem; padding: 0.18rem 0.6rem; border-radius: 999px; font-family: monospace; }
.mysql .dm-tag { color: #fbbf24; background: rgba(251, 191, 36, 0.1); border: 1px solid rgba(251, 191, 36, 0.25); }
.supabase .dm-tag { color: #34d399; background: rgba(52, 211, 153, 0.1); border: 1px solid rgba(52, 211, 153, 0.25); }
.cache .dm-tag { color: #f472b6; background: rgba(244, 114, 182, 0.1); border: 1px solid rgba(244, 114, 182, 0.25); }
.obj .dm-tag { color: #60a5fa; background: rgba(96, 165, 250, 0.1); border: 1px solid rgba(96, 165, 250, 0.25); }
.dm-list { list-style: none; display: flex; flex-direction: column; gap: 0.5rem; }
.dm-list li { display: flex; flex-direction: column; gap: 0.15rem; padding: 0.5rem 0.7rem; background: rgba(255,255,255,0.02); border-radius: 8px; }
.dm-list strong { font-size: 0.8rem; color: #e0e0f0; font-family: monospace; }
.dm-tables { font-size: 0.7rem; color: rgba(255,255,255,0.45); }
.dm-note { margin-top: 0.9rem; font-size: 0.7rem; color: rgba(255,255,255,0.4); line-height: 1.6; padding-top: 0.7rem; border-top: 1px solid rgba(255,255,255,0.05); }

/* 请求流转 */
.flow { padding: 4rem 2rem; }
.flow-steps { max-width: 900px; margin: 0 auto; display: flex; flex-direction: column; gap: 0.8rem; }
.flow-step { display: flex; align-items: center; gap: 1rem; padding: 1.1rem 1.4rem; background: rgba(12, 12, 28, 0.6); border: 1px solid rgba(255,255,255,0.07); border-radius: 14px; position: relative; }
.fs-num { font-family: monospace; font-size: 1.3rem; font-weight: 800; color: #c026d3; text-shadow: 0 0 10px rgba(192, 38, 211, 0.5); flex-shrink: 0; }
.fs-body { flex: 1; }
.fs-body h4 { font-size: 0.92rem; font-weight: 700; margin-bottom: 0.2rem; }
.fs-body p { font-size: 0.76rem; color: rgba(255,255,255,0.55); line-height: 1.6; }
.fs-arrow { position: absolute; right: -1.1rem; color: rgba(0, 229, 255, 0.4); font-size: 1.1rem; z-index: 1; }

/* 技术栈 */
.stack { padding: 4rem 2rem; }
.stack-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 1.4rem; max-width: 1000px; margin: 0 auto; }
.stack-card { padding: 1.8rem 1.8rem; border-radius: 16px; background: rgba(12, 12, 28, 0.65); border: 1px solid rgba(255,255,255,0.07); transition: all 0.35s; }
.stack-card:hover { border-color: rgba(0, 229, 255, 0.45); box-shadow: 0 0 34px rgba(0, 229, 255, 0.12); }
.stack-card h3 { font-size: 1.08rem; font-weight: 700; margin-bottom: 0.55rem; }
.stack-card p { font-size: 0.84rem; line-height: 1.8; color: rgba(255,255,255,0.75); margin-bottom: 1rem; }
.stack-tags { display: flex; gap: 0.5rem; flex-wrap: wrap; }
.stack-tags span { font-size: 0.68rem; color: #7fd9ff; font-family: monospace; padding: 0.25rem 0.7rem; border-radius: 999px; border: 1px solid rgba(0, 229, 255, 0.22); background: rgba(0, 229, 255, 0.05); }

/* CTA */
.cta-sec { text-align: center; padding: 6rem 2rem 7rem; }
.cta-sec h2 { font-size: clamp(1.5rem, 3vw, 2.2rem); font-weight: 800; margin-bottom: 0.8rem; }
.cta-sec p { color: rgba(255,255,255,0.72); font-size: 0.9rem; margin-bottom: 2rem; }
.cta { display: inline-flex; padding: 0.85rem 1.9rem; border-radius: 12px; background: linear-gradient(135deg, #00e5ff, #c026d3); color: #fff; font-size: 0.88rem; font-weight: 700; letter-spacing: 0.08em; box-shadow: 0 8px 30px rgba(0, 229, 255, 0.35); transition: all 0.3s; }
.cta:hover { box-shadow: 0 12px 44px rgba(0, 229, 255, 0.55); transform: translateY(-2px); }

@media (max-width: 1100px) {
  .services-grid { grid-template-columns: repeat(4, 1fr); }
  .infra-grid { grid-template-columns: repeat(3, 1fr); }
  .svc-th, .svc-tr { grid-template-columns: 1.4fr 0.7fr 1.2fr 2fr; }
}
@media (max-width: 768px) {
  .services-grid { grid-template-columns: repeat(3, 1fr); }
  .infra-grid { grid-template-columns: repeat(2, 1fr); }
  .dm-grid { grid-template-columns: 1fr; }
  .stack-grid { grid-template-columns: 1fr; }
  .svc-th, .svc-tr { grid-template-columns: 1fr 0.6fr; }
  .svc-th span:nth-child(3), .svc-th span:nth-child(4),
  .svc-tr .db-tag, .svc-tr .svc-desc { display: none; }
}
@media (max-width: 480px) {
  .services-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
