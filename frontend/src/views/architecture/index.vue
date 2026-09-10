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
        <div class="badge fade">● MICROSERVICES · SPRING CLOUD</div>
        <h1 class="title fade">微服务架构拓扑</h1>
        <p class="sub fade">
          七个业务服务 + 统一网关 + Nacos 注册中心：独立部署、独立伸缩，
          以消息队列解耦，用分布式链路追踪观测全局。
        </p>
        <div class="tags fade">
          <span v-for="t in heroTags" :key="t">#{{ t }}</span>
        </div>
      </div>
    </section>

    <!-- TOPOLOGY 拓扑 -->
    <section class="topo">
      <div class="sec-head">
        <span class="sec-num">[ 01 ]</span>
        <h2>服务拓扑</h2>
        <p>SERVICE TOPOLOGY</p>
      </div>
      <div class="topo-wrap">
        <!-- 网关 -->
        <div class="topo-node gateway">
          <div class="node-icon">🛡️</div>
          <div class="node-name">API 网关</div>
          <div class="node-sub">SPRING CLOUD GATEWAY</div>
        </div>
        <div class="gateway-lines">
          <div class="line line-a"></div>
          <div class="line line-b"></div>
          <div class="line line-c"></div>
        </div>
        <!-- 服务区 -->
        <div class="services-grid">
          <div v-for="(s, i) in services" :key="s.name" class="topo-node service">
            <div class="node-icon">{{ s.icon }}</div>
            <div class="node-name">{{ s.name }}</div>
            <div class="node-sub">{{ s.sub }}</div>
          </div>
        </div>
        <!-- 基础设施 -->
        <div class="infra-row">
          <div class="topo-node infra">
            <div class="node-icon">🗄️</div>
            <div class="node-name">MySQL</div>
            <div class="node-sub">主从集群</div>
          </div>
          <div class="topo-node infra">
            <div class="node-icon">📡</div>
            <div class="node-name">Nacos</div>
            <div class="node-sub">注册中心</div>
          </div>
          <div class="topo-node infra">
            <div class="node-icon">🔁</div>
            <div class="node-name">RabbitMQ</div>
            <div class="node-sub">异步解耦</div>
          </div>
          <div class="topo-node infra">
            <div class="node-icon">🪣</div>
            <div class="node-name">MinIO</div>
            <div class="node-sub">对象存储</div>
          </div>
        </div>
      </div>
    </section>

    <!-- STACK 技术栈 -->
    <section class="stack">
      <div class="sec-head">
        <span class="sec-num">[ 02 ]</span>
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
// ====================================================
// 系统架构页：GSAP 滚动动画展示微服务架构，
// 含服务清单与技术栈介绍
// ====================================================
import { onMounted, onBeforeUnmount } from 'vue'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import PageBack from '@/components/PageBack.vue'

gsap.registerPlugin(ScrollTrigger)

// 首页顶部特性标签
const heroTags = ['Spring Cloud', 'Nacos', 'Gateway', 'K8s', 'SkyWalking']

// 微服务清单（用户/博客/社区/媒体/商城/量化/AI）
const services = [
  { icon: '👤', name: 'user', sub: '用户服务 · 认证鉴权' },
  { icon: '📝', name: 'blog', sub: '博客服务 · 内容管理' },
  { icon: '💬', name: 'forum', sub: '社区服务 · 帖子互动' },
  { icon: '🎬', name: 'media', sub: '媒体服务 · 资源分发' },
  { icon: '🛍️', name: 'shop', sub: '商城服务 · 订单支付' },
  { icon: '📈', name: 'quant', sub: '量化服务 · 策略引擎' },
  { icon: '🤖', name: 'ai', sub: 'AI 服务 · 推理问答' },
]

// 技术栈说明（微服务治理/数据与消息/观测与部署/对象存储）
const stack = [
  { name: 'Spring Cloud Alibaba', desc: '微服务治理全家桶：注册发现、配置中心、网关路由、熔断限流', tags: ['Nacos', 'Gateway', 'Sentinel'] },
  { name: '数据与消息', desc: 'MySQL 主从 + MyBatis-Plus，RabbitMQ 异步解耦，Redis 缓存热点', tags: ['MySQL', 'Redis', 'RabbitMQ'] },
  { name: '观测与部署', desc: 'SkyWalking 链路追踪聚合依赖，Docker 镜像 + K8s 编排滚动发布', tags: ['SkyWalking', 'Docker', 'K8s'] },
  { name: '对象存储', desc: 'MinIO 私有化对象存储，统一承载图片、音频与文件资源', tags: ['MinIO', 'S3'] },
]

let ctx = null

onMounted(() => {
  ctx = gsap.context(() => {
    gsap.from('.fade', { y: 40, opacity: 0, duration: 1, stagger: 0.15, ease: 'power3.out' })
    gsap.from('.topo-node:not(.gateway)', {
      scale: 0.6, opacity: 0, duration: 0.7, stagger: 0.08, ease: 'back.out(1.6)',
      scrollTrigger: { trigger: '.topo-wrap', start: 'top 75%', once: true },
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

.hero { position: relative; min-height: 72vh; display: flex; align-items: center; padding: 4rem 2rem; }
.hero-back { position: absolute; top: 24px; left: 24px; z-index: 20; }
@media (max-width: 640px) { .hero-back { top: 16px; left: 16px; } }
.hero-bg { position: absolute; inset: 0; pointer-events: none; }
.orb { position: absolute; border-radius: 50%; filter: blur(90px); animation: orbFloat 8s ease-in-out infinite; }
.orb.o1 { width: 420px; height: 420px; right: 6%; top: 8%; background: radial-gradient(circle, rgba(102, 126, 234, 0.28), transparent 65%); }
.orb.o2 { width: 360px; height: 360px; left: -60px; bottom: 4%; background: radial-gradient(circle, rgba(0, 212, 255, 0.16), transparent 65%); animation-delay: -4s; }
@keyframes orbFloat { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-26px); } }
.grid-lines { position: absolute; inset: 0; }

.hero-inner { position: relative; max-width: 980px; margin: 0 auto; text-align: center; }
.badge { display: inline-flex; padding: 0.45rem 1.1rem; border: 1px solid rgba(0, 212, 255, 0.4); border-radius: 4px; color: #7fd9ff; letter-spacing: 0.2em; font-size: 0.72rem; margin-bottom: 1.4rem; }
.title { font-size: clamp(2.4rem, 6vw, 4.6rem); font-weight: 900; letter-spacing: 0.06em; margin-bottom: 1.2rem; background: linear-gradient(135deg, #fff, #7fd9ff 55%, #667eea); -webkit-background-clip: text; background-clip: text; -webkit-text-fill-color: transparent; }
.sub { font-size: 1.02rem; line-height: 1.9; color: rgba(255,255,255,0.82); max-width: 640px; margin: 0 auto 1.6rem; }
.tags { display: flex; gap: 0.6rem; justify-content: center; flex-wrap: wrap; }
.tags span { font-size: 0.72rem; font-family: monospace; color: #7fd9ff; padding: 0.3rem 0.8rem; border: 1px solid rgba(0, 212, 255, 0.25); border-radius: 999px; background: rgba(0, 212, 255, 0.06); }
.cta { display: inline-flex; padding: 0.85rem 1.9rem; border-radius: 12px; background: linear-gradient(135deg, #00a8ff, #667eea); color: #fff; font-size: 0.88rem; font-weight: 700; letter-spacing: 0.08em; box-shadow: 0 8px 30px rgba(0, 168, 255, 0.35); transition: all 0.3s; }
.cta:hover { box-shadow: 0 12px 44px rgba(0, 168, 255, 0.55); transform: translateY(-2px); }

.topo { position: relative; padding: 5rem 2rem; }
.sec-head { text-align: center; margin-bottom: 3.5rem; }
.sec-num { color: #00a8ff; font-family: monospace; font-size: 0.75rem; letter-spacing: 0.25em; }
.sec-head h2 { font-size: clamp(1.6rem, 3.4vw, 2.4rem); font-weight: 800; margin: 0.5rem 0 0.3rem; }
.sec-head p { font-size: 0.7rem; letter-spacing: 0.3em; color: rgba(255,255,255,0.6); }

.topo-wrap { position: relative; max-width: 1080px; margin: 0 auto; }
.gateway { margin: 0 auto 1rem; }
.gateway-lines { position: relative; height: 42px; margin-bottom: 1.2rem; }
.line { position: absolute; top: 0; width: 2px; height: 42px; background: linear-gradient(180deg, #00d4ff, rgba(0, 212, 255, 0.1)); }
.line-a { left: 20%; } .line-b { left: 50%; } .line-c { left: 80%; }
.line::after { content: ''; position: absolute; top: 0; left: -3px; width: 8px; height: 8px; border-radius: 50%; background: #00d4ff; box-shadow: 0 0 10px #00d4ff; animation: lineDrop 2.2s ease-in-out infinite; }
.line-b::after { animation-delay: 0.6s; } .line-c::after { animation-delay: 1.2s; }
@keyframes lineDrop { 0% { top: 0; opacity: 0; } 15% { opacity: 1; } 85% { opacity: 1; } 100% { top: 42px; opacity: 0; } }

.topo-node { text-align: center; padding: 1.3rem 1.4rem; border-radius: 14px; background: rgba(12, 12, 28, 0.7); border: 1px solid rgba(255,255,255,0.09); backdrop-filter: blur(10px); transition: all 0.3s; }
.topo-node:hover { border-color: rgba(0, 212, 255, 0.5); box-shadow: 0 0 26px rgba(0, 212, 255, 0.18); transform: translateY(-3px); }
.gateway { display: inline-flex; align-items: center; gap: 0.9rem; width: fit-content; padding: 1rem 2.2rem; border-color: rgba(0, 212, 255, 0.35); }
.node-icon { font-size: 1.5rem; margin-bottom: 0.4rem; }
.gateway .node-icon { margin-bottom: 0; }
.node-name { font-size: 0.95rem; font-weight: 700; }
.node-sub { font-size: 0.6rem; letter-spacing: 0.14em; color: rgba(255,255,255,0.6); font-family: monospace; margin-top: 0.25rem; }

.services-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 0.9rem; margin-bottom: 1.6rem; }
.infra-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 0.9rem; }

.stack { position: relative; padding: 5rem 2rem; }
.stack-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 1.4rem; max-width: 1000px; margin: 0 auto; }
.stack-card { padding: 1.8rem 1.8rem; border-radius: 16px; background: rgba(12, 12, 28, 0.65); border: 1px solid rgba(255,255,255,0.07); transition: all 0.35s; }
.stack-card:hover { border-color: rgba(0, 168, 255, 0.45); box-shadow: 0 0 34px rgba(0, 168, 255, 0.12); }
.stack-card h3 { font-size: 1.08rem; font-weight: 700; margin-bottom: 0.55rem; }
.stack-card p { font-size: 0.84rem; line-height: 1.8; color: rgba(255,255,255,0.75); margin-bottom: 1rem; }
.stack-tags { display: flex; gap: 0.5rem; flex-wrap: wrap; }
.stack-tags span { font-size: 0.68rem; color: #7fd9ff; font-family: monospace; padding: 0.25rem 0.7rem; border-radius: 999px; border: 1px solid rgba(0, 212, 255, 0.22); background: rgba(0, 212, 255, 0.05); }

.cta-sec { position: relative; text-align: center; padding: 6rem 2rem 7rem; }
.cta-sec h2 { font-size: clamp(1.5rem, 3vw, 2.2rem); font-weight: 800; margin-bottom: 0.8rem; }
.cta-sec p { color: rgba(255,255,255,0.72); font-size: 0.9rem; margin-bottom: 2rem; }

@media (max-width: 1100px) {
  .services-grid { grid-template-columns: repeat(4, 1fr); }
  .infra-row { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 640px) {
  .services-grid { grid-template-columns: repeat(2, 1fr); }
  .stack-grid { grid-template-columns: 1fr; }
}
</style>