<template>
  <header class="page-hero">
    <!-- 面包屑：crumbs = [{ label: '首页', to: '/' }, { label: '当前页' }] -->
    <nav v-if="crumbs.length" class="ph-crumbs">
      <template v-for="(c, i) in crumbs" :key="i">
        <router-link v-if="c.to" :to="c.to" class="ph-crumb">{{ c.label }}</router-link>
        <span v-else class="ph-crumb is-current">{{ c.label }}</span>
        <span v-if="i < crumbs.length - 1" class="ph-sep">/</span>
      </template>
    </nav>

    <div class="ph-row">
      <div class="ph-main">
        <h1 class="ph-title">
          <span class="ph-title-text">{{ title }}</span>
        </h1>
        <p v-if="subtitle" class="ph-sub">
          <span class="ph-sub-rule" aria-hidden="true"></span>
          <span>{{ subtitle }}</span>
        </p>
      </div>
      <!-- 右侧操作区（如：上传按钮 / 购物车入口） -->
      <div v-if="$slots.actions" class="ph-actions"><slot name="actions" /></div>
    </div>

    <!-- 收口细线：把页头与正文分开，长页面读起来有骨架 -->
    <span class="ph-rule" aria-hidden="true"></span>
  </header>
</template>

<script setup>
// ============================================================
// 页面头部组件（PageHeader）
// 统一内页排版语言：面包屑 + 渐变标题 + 英文副标题 + 右侧操作槽
// title 标题；subtitle 英文副标题；crumbs 面包屑数组
// 非 prop 属性（如 data-voice-target）自动透传到根元素
//
// 为什么带一块「晕影」：内页直接铺在动漫壁纸视频上，标题是渐变色，
// 落在人物浅色发丝上会糊成一片。这里用一层柔和的径向暗晕压住标题背后，
// 只在文字附近生效，不动整页亮度。
// ============================================================
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  crumbs: { type: Array, default: () => [] }
})
</script>

<style scoped>
.page-hero {
  position: relative;
  margin-bottom: 2rem;
  padding-bottom: 1.1rem;
  isolation: isolate;
}

/* 标题背后的柔光暗晕：左上角最重，向右下淡出，正好覆盖面包屑 + 标题 */
.page-hero::before {
  content: '';
  position: absolute;
  z-index: -1;
  left: -1.5rem;
  top: -1rem;
  width: min(760px, 88%);
  height: calc(100% + 2rem);
  pointer-events: none;
  background: radial-gradient(ellipse 70% 100% at 12% 42%, rgba(4, 4, 12, 0.62), rgba(4, 4, 12, 0.28) 46%, transparent 74%);
  filter: blur(10px);
}

/* ── 面包屑 ── */
.ph-crumbs {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  margin-bottom: 0.85rem;
  font-size: 0.78rem;
  letter-spacing: 0.02em;
}
.ph-crumb {
  color: rgba(255, 255, 255, 0.42);
  transition: color 0.2s ease;
}
a.ph-crumb:hover { color: rgba(255, 255, 255, 0.86); }
.ph-crumb.is-current { color: rgba(255, 255, 255, 0.72); }
.ph-sep { color: rgba(255, 255, 255, 0.18); }

/* ── 主行 ── */
.ph-row {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}
.ph-main { min-width: 0; }

/* 标题：左侧一道渐变竖条 + 渐变字。竖条让标题「站住」，不再像浮在壁纸上 */
.ph-title {
  position: relative;
  display: flex;
  align-items: center;
  gap: 0.7rem;
  margin: 0;
  font-size: clamp(1.7rem, 2.4vw, 2.1rem);
  font-weight: 800;
  line-height: 1.18;
  letter-spacing: -0.01em;
  /* 投影挂在外层：直接给 background-clip:text 的元素加 filter，Chrome 会退回用 color
     填充，渐变整个失效（标题会变成一片死白）。外层没有裁切，可以安全承担投影 */
  filter: drop-shadow(0 2px 6px rgba(3, 3, 12, 0.72));
}
.ph-title::before {
  content: '';
  flex: none;
  width: 3px;
  height: 1.05em;
  border-radius: 2px;
  background: linear-gradient(180deg, var(--color-accent, #22d3ee), var(--color-primary, #a855f7));
  box-shadow: 0 0 12px rgba(34, 211, 238, 0.45);
}
.ph-title-text {
  background: linear-gradient(112deg, #6fe3ff 0%, #b9a6ff 52%, #ffb3e6 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  color: #dff6ff;                    /* 不支持 background-clip 时的兜底 */
}

/* 副标题：短横线 + 等宽小字，字距拉开，作为标题的「第二声部」 */
.ph-sub {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  margin: 0.6rem 0 0;
  font-family: var(--font-mono, ui-monospace, monospace);
  font-size: 0.72rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.5);
  text-shadow: 0 1px 6px rgba(0, 0, 0, 0.6);
}
.ph-sub-rule {
  width: 22px;
  height: 1px;
  flex: none;
  background: linear-gradient(90deg, rgba(34, 211, 238, 0.9), transparent);
}

.ph-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding-top: 0.15rem;
}

/* 收口细线：左浓右淡，止于 100%，不做满宽硬边 */
.ph-rule {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 1px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.14), rgba(255, 255, 255, 0.05) 42%, transparent 88%);
}

@media (max-width: 640px) {
  .page-hero { margin-bottom: 1.5rem; }
  .ph-title { font-size: 1.45rem; }
  .ph-sub { font-size: 0.66rem; letter-spacing: 0.12em; }
  .ph-actions { width: 100%; }
}
</style>
