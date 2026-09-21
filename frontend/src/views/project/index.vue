<template>
  <div class="project-page">
    <!-- ============ HERO: 项目总览 ============ -->
    <section class="project-hero">
      <div class="hero-bg">
        <div class="bg-grid"></div>
        <div class="bg-glow"></div>
      </div>

      <div class="hero-container">
        <PageBack label="返回首页" to="/" />
        <p class="hero-eyebrow">项目总览 PROJECT SHOWCASE</p>
        <h1 class="hero-title">
          <span>技术与作品</span>
          <span class="hero-title-accent">综合展陈</span>
        </h1>
        <p class="hero-subtitle">
          一站式汇集本站核心架构、代表作品与动效实验，集中呈现工程深度与视觉表达的双重维度。
        </p>

        <div class="hero-stats">
          <div class="stat">
            <span class="stat-num">4</span>
            <span class="stat-label">架构层级</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat">
            <span class="stat-num">6</span>
            <span class="stat-label">代表作品</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat">
            <span class="stat-num">5</span>
            <span class="stat-label">动效合集</span>
          </div>
        </div>
      </div>
    </section>

    <div class="section-seam" aria-hidden="true"></div>

    <!-- ============ [01] 技术矩阵与系统架构 ============ -->
    <TechMatrix />

    <div class="section-seam" aria-hidden="true"></div>

    <!-- ============ [02] 核心作品与 Demo 橱窗 ============ -->
    <ShowcaseGrid />

    <div class="section-seam" aria-hidden="true"></div>

    <!-- ============ [03] 动效实验室 ============ -->
    <MotionLab />

    <div class="section-seam" aria-hidden="true"></div>

    <!-- ============ [04] 开源仓库馆 ============ -->
    <OpenSourceVault />
  </div>
</template>

<script setup>
// ============================================================
// /project 综合项目展示页
// 汇聚 TechMatrix / ShowcaseGrid / MotionLab / OpenSourceVault
// 提供单页深度浏览体验
// ============================================================
import TechMatrix from '@/components/home/TechMatrix.vue'
import ShowcaseGrid from '@/components/home/ShowcaseGrid.vue'
import MotionLab from '@/components/home/MotionLab.vue'
import OpenSourceVault from '@/components/project/OpenSourceVault.vue'
import PageBack from '@/components/PageBack.vue'
</script>

<style scoped>
.project-page {
  position: relative;
  min-height: 100vh;
  background: linear-gradient(180deg, #06060e 0%, #0a0a1a 50%, #06060e 100%);
  color: #e0e0f0;
  overflow-x: hidden;
}

/* ============================================================
   HERO
   ============================================================ */
.project-hero {
  position: relative;
  padding: 6rem 2rem 4rem;
  overflow: hidden;
}

.hero-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(102, 126, 234, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(102, 126, 234, 0.06) 1px, transparent 1px);
  background-size: 44px 44px;
  mask-image: radial-gradient(ellipse 70% 75% at 50% 40%, #000 20%, transparent 78%);
}

.bg-glow {
  position: absolute;
  width: 720px;
  height: 720px;
  left: 50%;
  top: 30%;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  background: radial-gradient(circle, rgba(0, 212, 255, 0.1), rgba(102, 126, 234, 0.06) 40%, transparent 70%);
  filter: blur(90px);
}

.hero-container {
  position: relative;
  z-index: 1;
  max-width: 960px;
  margin: 0 auto;
  text-align: center;
}

.hero-eyebrow {
  font-size: 0.78rem;
  letter-spacing: 0.32em;
  color: #67e8f9;
  margin-bottom: 1.2rem;
  font-family: 'Courier New', monospace;
  text-shadow: 0 0 10px rgba(103, 232, 249, 0.5);
}

.hero-title {
  font-size: clamp(2.2rem, 5.5vw, 3.6rem);
  font-weight: 800;
  line-height: 1.15;
  margin-bottom: 1.4rem;
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  color: #fff;
  text-shadow: 0 0 24px rgba(120, 160, 255, 0.35);
}

.hero-title-accent {
  background: linear-gradient(135deg, #67e8f9, #22d3ee 50%, #667eea);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-subtitle {
  max-width: 56ch;
  margin: 0 auto 2.6rem;
  font-size: 0.92rem;
  line-height: 1.9;
  color: rgba(255, 255, 255, 0.55);
}

.hero-stats {
  display: inline-flex;
  align-items: center;
  gap: 1.8rem;
  padding: 1rem 2rem;
  background: rgba(12, 12, 28, 0.6);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(103, 232, 249, 0.18);
  border-radius: 16px;
  box-shadow: 0 0 40px rgba(0, 212, 255, 0.1);
}

.stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.3rem;
}

.stat-num {
  font-size: 2rem;
  font-weight: 800;
  color: #67e8f9;
  font-family: 'Courier New', monospace;
  text-shadow: 0 0 12px rgba(103, 232, 249, 0.6);
}

.stat-label {
  font-size: 0.7rem;
  letter-spacing: 0.22em;
  color: rgba(255, 255, 255, 0.5);
}

.stat-divider {
  width: 1px;
  height: 36px;
  background: linear-gradient(180deg, transparent, rgba(103, 232, 249, 0.4), transparent);
}

/* ============================================================
   SECTION SEAM
   ============================================================ */
.section-seam {
  position: relative;
  height: 110px;
  z-index: 1;
  pointer-events: none;
}

.section-seam::before {
  content: '';
  position: absolute;
  left: 50%;
  top: 50%;
  width: min(720px, 74vw);
  height: 1.5px;
  transform: translate(-50%, -50%);
  background: linear-gradient(90deg,
    transparent 0%,
    rgba(102, 126, 234, 0.05) 12%,
    rgba(102, 126, 234, 0.5) 38%,
    rgba(0, 212, 255, 0.65) 50%,
    rgba(139, 124, 246, 0.5) 62%,
    rgba(118, 75, 162, 0.05) 88%,
    transparent 100%);
  filter: drop-shadow(0 0 10px rgba(102, 126, 234, 0.45));
  animation: seamBreath 5s ease-in-out infinite;
}

.section-seam::after {
  content: '';
  position: absolute;
  left: 50%;
  top: 50%;
  width: min(720px, 74vw);
  height: 1.5px;
  transform: translate(-50%, -50%);
  background: linear-gradient(90deg,
    transparent 0%, transparent 42%,
    rgba(255, 255, 255, 0.85) 50%,
    transparent 58%, transparent 100%);
  background-size: 220% 100%;
  mix-blend-mode: screen;
  animation: seamSweep 6s linear infinite;
}

@keyframes seamBreath {
  0%, 100% { opacity: 0.55; }
  50% { opacity: 1; }
}

@keyframes seamSweep {
  from { background-position: 130% 0; }
  to { background-position: -130% 0; }
}

/* ============================================================
   RESPONSIVE
   ============================================================ */
@media (max-width: 640px) {
  .project-hero { padding: 4rem 1.25rem 2.5rem; }
  .hero-stats {
    gap: 1rem;
    padding: 0.8rem 1.2rem;
  }
  .stat-num { font-size: 1.5rem; }
  .stat-label { font-size: 0.62rem; }
}
</style>
