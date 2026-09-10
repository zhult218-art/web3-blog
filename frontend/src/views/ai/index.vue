<template>
  <div class="ai-page">
    <!-- HERO -->
    <section class="hero" ref="hero">
      <div class="hero-back"><PageBack label="返回首页" to="/" /></div>
      <div class="hero-bg">
        <div class="orb o1"></div>
        <div class="orb o2"></div>
        <div class="grid-lines"></div>
      </div>
      <div class="hero-inner">
        <div class="badge fade">● AI ENGINE · FASTAPI</div>
        <h1 class="title fade">AI 智能引擎</h1>
        <p class="sub fade">
          ● FastAPI 驱动的多模态 Agent 服务：LLM 推理 + RAG 知识库检索，
          让每一次问答都拥有长期记忆、引用溯源与工具调用能力。
        </p>
        <div class="tags fade">
          <span v-for="t in heroTags" :key="t">#{{ t }}</span>
        </div>
        <router-link to="/community" class="cta fade">进入社区体验 →</router-link>
      </div>
    </section>

    <!-- PIPELINE 流水线 -->
    <section class="pipeline">
      <div class="sec-head">
        <span class="sec-num">[ 01 ]</span>
        <h2>一次问答的完整链路</h2>
        <p>REQUEST PIPELINE</p>
      </div>
      <div class="pipe-row">
        <div v-for="(step, i) in pipeline" :key="step.name" class="pipe-step" :class="'ps' + i">
          <div class="pipe-icon">{{ step.icon }}</div>
          <h3>{{ step.name }}</h3>
          <p>{{ step.desc }}</p>
        </div>
        <div class="pipe-flow"></div>
      </div>
    </section>

    <!-- CAPABILITIES 能力矩阵 -->
    <section class="caps">
      <div class="sec-head">
        <span class="sec-num">[ 02 ]</span>
        <h2>核心能力</h2>
        <p>CORE CAPABILITIES</p>
      </div>
      <div class="cap-grid">
        <div v-for="(c, i) in capabilities" :key="c.title" class="cap-card" :class="'cc' + i">
          <div class="cap-icon">{{ c.icon }}</div>
          <h3>{{ c.title }}</h3>
          <p>{{ c.desc }}</p>
          <div class="cap-meta">{{ c.meta }}</div>
        </div>
      </div>
    </section>

    <!-- CTA -->
    <section class="cta-sec">
      <h2>想和智能体聊点什么？</h2>
      <p>打开社区，或对星途说"星途星途，介绍一下 AI 引擎"</p>
      <router-link to="/community" class="cta">前往社区 →</router-link>
    </section>
  </div>
</template>

<script setup>
// ====================================================
// AI 平台首页：GSAP 滚动动画展示 AI 能力、
// RAG 处理管线与技术架构
// ====================================================
import { onMounted, onBeforeUnmount } from 'vue'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import PageBack from '@/components/PageBack.vue'

gsap.registerPlugin(ScrollTrigger)

// 首页顶部特性标签
const heroTags = ['FastAPI', 'RAG', 'LLM', 'Agent', 'Memory']

// AI 处理管线步骤数据（输入→意图→RAG→知识库→推理→工具编排）
const pipeline = [
  { icon: '✍️', name: '用户输入', desc: '文本 / 语音指令进入会话上下文' },
  { icon: '🧭', name: '意图解析', desc: '识别意图并决定是否触发工具调用' },
  { icon: '📚', name: 'RAG 检索', desc: '向量检索相关文档并引用溯源' },
  { icon: '🏛️', name: '向量知识库', desc: '文档切块 Embedding 后落库' },
  { icon: '🧠', name: 'LLM 推理', desc: '结合上下文与检索结果生成回答' },
  { icon: '🔧', name: '工具编排', desc: '函数调用 / 查询系统数据并回答' },
]

// 平台核心能力清单
const capabilities = [
  { icon: '🧠', title: '多轮对话 + 记忆', desc: '跨会话持久记忆，记得你们聊过的每一句话', meta: 'MEMORY' },
  { icon: '📑', title: 'RAG 引用溯源', desc: '每个回答都指向知识库原文，可追溯、可验证', meta: 'RAG' },
  { icon: '🔧', title: '函数调用 / 工具编排', desc: '访问系统数据、查询行情、执行站点导航', meta: 'TOOLS' },
  { icon: '⚡', title: '流式输出', desc: 'Token 级流式响应，首字延迟极低', meta: 'STREAM' },
  { icon: '🎤', title: '语音交互', desc: '对星途说"星途星途"即可语音唤起对话', meta: 'VOICE' },
  { icon: '🌐', title: '多模态接入', desc: '文本 + 图片 + 文档多模态输入统一处理', meta: 'MULTIMODAL' },
]

let ctx = null

onMounted(() => {
  ctx = gsap.context(() => {
    gsap.from('.fade', { y: 40, opacity: 0, duration: 1, stagger: 0.15, ease: 'power3.out' })
    gsap.from('.pipe-step', {
      y: 60, opacity: 0, duration: 0.9, stagger: 0.12, ease: 'power3.out',
      scrollTrigger: { trigger: '.pipe-row', start: 'top 80%', once: true },
    })
    gsap.from('.cap-card', {
      y: 70, opacity: 0, duration: 0.9, stagger: 0.1, ease: 'power3.out',
      scrollTrigger: { trigger: '.cap-grid', start: 'top 80%', once: true },
    })
  })
})

onBeforeUnmount(() => { if (ctx) ctx.revert() })
</script>

<style scoped>
.ai-page { position: relative; min-height: 100vh; background: linear-gradient(180deg, #06060e, #0a0a1e); color: #e0e0f0; overflow: hidden; }

.hero { position: relative; min-height: 78vh; display: flex; align-items: center; padding: 4rem 2rem; }
.hero-back { position: absolute; top: 24px; left: 24px; z-index: 20; }
@media (max-width: 640px) { .hero-back { top: 16px; left: 16px; } }
.hero-bg { position: absolute; inset: 0; pointer-events: none; }
.orb { position: absolute; border-radius: 50%; filter: blur(90px); animation: orbFloat 8s ease-in-out infinite; }
.orb.o1 { width: 420px; height: 420px; right: 6%; top: 8%; background: radial-gradient(circle, rgba(102, 126, 234, 0.28), transparent 65%); }
.orb.o2 { width: 360px; height: 360px; left: -60px; bottom: 4%; background: radial-gradient(circle, rgba(118, 75, 162, 0.22), transparent 65%); animation-delay: -4s; }
@keyframes orbFloat { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-26px); } }
.grid-lines { position: absolute; inset: 0; }

.hero-inner { position: relative; max-width: 980px; margin: 0 auto; text-align: center; }
.badge { display: inline-flex; padding: 0.45rem 1.1rem; border: 1px solid rgba(102, 126, 234, 0.4); border-radius: 4px; color: #9db2ff; letter-spacing: 0.2em; font-size: 0.72rem; margin-bottom: 1.4rem; }
.title { font-size: clamp(2.4rem, 6vw, 4.6rem); font-weight: 900; letter-spacing: 0.06em; margin-bottom: 1.2rem; background: linear-gradient(135deg, #fff, #9db2ff 60%, #667eea); -webkit-background-clip: text; background-clip: text; -webkit-text-fill-color: transparent; }
.sub { font-size: 1.02rem; line-height: 1.9; color: rgba(255,255,255,0.82); max-width: 640px; margin: 0 auto 1.6rem; }
.tags { display: flex; gap: 0.6rem; justify-content: center; flex-wrap: wrap; margin-bottom: 2rem; }
.tags span { font-size: 0.72rem; font-family: monospace; color: #7fd9ff; padding: 0.3rem 0.8rem; border: 1px solid rgba(0, 212, 255, 0.25); border-radius: 999px; background: rgba(0, 212, 255, 0.06); }
.cta { display: inline-flex; padding: 0.85rem 1.9rem; border-radius: 12px; background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; font-size: 0.88rem; font-weight: 700; letter-spacing: 0.08em; box-shadow: 0 8px 30px rgba(102, 126, 234, 0.35); transition: all 0.3s; }
.cta:hover { box-shadow: 0 12px 44px rgba(102, 126, 234, 0.55); transform: translateY(-2px); }

.pipeline { position: relative; padding: 6rem 2rem; }
.sec-head { text-align: center; margin-bottom: 3.5rem; }
.sec-num { color: #667eea; font-family: monospace; font-size: 0.75rem; letter-spacing: 0.25em; }
.sec-head h2 { font-size: clamp(1.6rem, 3.4vw, 2.4rem); font-weight: 800; margin: 0.5rem 0 0.3rem; }
.sec-head p { font-size: 0.7rem; letter-spacing: 0.3em; color: rgba(255,255,255,0.6); }

.pipe-row { position: relative; display: grid; grid-template-columns: repeat(6, 1fr); gap: 1.2rem; max-width: 1180px; margin: 0 auto; }
.pipe-step { position: relative; background: rgba(12, 12, 28, 0.7); border: 1px solid rgba(255,255,255,0.08); border-radius: 16px; padding: 1.5rem 1.2rem; text-align: center; backdrop-filter: blur(10px); transition: all 0.3s; }
.pipe-step:hover { border-color: rgba(102, 126, 234, 0.5); box-shadow: 0 0 30px rgba(102, 126, 234, 0.15); transform: translateY(-4px); }
.pipe-icon { font-size: 1.6rem; margin-bottom: 0.7rem; }
.pipe-step h3 { font-size: 0.92rem; font-weight: 700; margin-bottom: 0.5rem; }
.pipe-step p { font-size: 0.74rem; line-height: 1.7; color: rgba(255,255,255,0.72); }
.pipe-flow { position: absolute; top: -14px; left: 50%; transform: translateX(-50%); width: 60px; height: 2px; background: linear-gradient(90deg, transparent, #00d4ff, transparent); animation: flowSweep 2.4s ease-in-out infinite; border-radius: 2px; }
@keyframes flowSweep { 0%, 100% { opacity: 0.25; } 50% { opacity: 1; box-shadow: 0 0 12px rgba(0, 212, 255, 0.8); } }

.caps { position: relative; padding: 6rem 2rem; }
.cap-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1.4rem; max-width: 1100px; margin: 0 auto; }
.cap-card { position: relative; padding: 2rem 1.6rem; border-radius: 18px; background: rgba(12, 12, 28, 0.65); border: 1px solid rgba(255,255,255,0.07); overflow: hidden; transition: all 0.35s; }
.cap-card::before { content: ''; position: absolute; inset: 0; background: radial-gradient(360px circle at 50% 0%, rgba(102, 126, 234, 0.14), transparent 70%); opacity: 0; transition: opacity 0.4s; }
.cap-card:hover { border-color: rgba(102, 126, 234, 0.5); box-shadow: 0 0 40px rgba(102, 126, 234, 0.15); }
.cap-card:hover::before { opacity: 1; }
.cap-icon { font-size: 2rem; margin-bottom: 1rem; }
.cap-card h3 { font-size: 1.1rem; font-weight: 700; margin-bottom: 0.6rem; }
.cap-card p { font-size: 0.84rem; line-height: 1.8; color: rgba(255,255,255,0.75); }
.cap-meta { margin-top: 1.1rem; font-size: 0.62rem; letter-spacing: 0.25em; color: #7fd9ff; font-family: monospace; }

.cta-sec { position: relative; text-align: center; padding: 6rem 2rem 7rem; }
.cta-sec h2 { font-size: clamp(1.5rem, 3vw, 2.2rem); font-weight: 800; margin-bottom: 0.8rem; }
.cta-sec p { color: rgba(255,255,255,0.72); font-size: 0.9rem; margin-bottom: 2rem; }

@media (max-width: 1000px) {
  .pipe-row { grid-template-columns: repeat(3, 1fr); }
  .cap-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 640px) {
  .pipe-row { grid-template-columns: repeat(2, 1fr); }
  .cap-grid { grid-template-columns: 1fr; }
}
</style>