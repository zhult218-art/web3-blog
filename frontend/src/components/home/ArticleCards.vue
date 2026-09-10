<template>
  <section id="articles" class="articles">
    <div class="section-bg">
      <div class="bg-grid"></div>
    </div>

    <div class="section-container">
      <SectionHead num="[ 03 ]" title="技术干货与深度文章" sub="FEATURED ARTICLES" />

      <div class="article-grid">
        <article
          v-for="(art, i) in list"
          :key="art.id || i"
          class="article-card"
          data-glow
          @click="go(art.id)"
        >
          <div class="article-cover">
            <div class="cover-grad" :style="gradStyle(i)"></div>
            <div class="cover-noise"></div>
            <div class="cover-index">0{{ i + 1 }}</div>
            <div class="cover-chip">{{ art.tag || 'TECH' }}</div>
          </div>

          <div class="article-body">
            <div class="article-tags">
              <span v-for="t in art.tags" :key="t" class="atag">#{{ t }}</span>
            </div>
            <h3 class="article-title">{{ art.title }}</h3>
            <p class="article-excerpt">{{ art.excerpt }}</p>
            <div class="article-meta">
              <span class="meta-read">{{ art.readTime }} 分钟阅读</span>
              <span class="meta-date">{{ art.date }}</span>
            </div>
            <div class="article-link">
              <span>阅读全文</span>
              <svg class="link-arrow" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M5 12h14M12 5l7 7-7 7"/>
              </svg>
            </div>
          </div>
        </article>
      </div>
    </div>
  </section>
</template>

<script setup>
// ============================================================
// 首页"技术干货"文章卡片区（ArticleCards）
// 优先从 blog-service 拉取最新 3 篇文章，接口不可用时回退静态示例
// 卡片带 GSAP 滚动入场动画，点击跳转对应文章
// ============================================================
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import SectionHead from './SectionHead.vue'
import { getBlogList } from '@/api/blog'

gsap.registerPlugin(ScrollTrigger)

const router = useRouter()

// 接口不可用时的兜底文章（站内精选）
const DEFAULT_ARTICLES = [
  {
    title: '从零搭建 Spring Cloud 微服务网关',
    excerpt: '网关是微服务体系的流量大门。本文完整拆解 Gateway 路由、熔断、限流与灰度发布的落地实践。',
    tags: ['SpringCloud', 'Gateway'],
    readTime: 8,
    date: '2026 站内精选',
    tag: 'MICRO',
  },
  {
    title: 'WebGL 粒子系统：从 0 到 1 构建星系模拟',
    excerpt: '用 Three.js 实现上万粒子实时演化的完整教程，涉及 BufferGeometry、着色器与性能优化。',
    tags: ['WebGL', 'Three.js'],
    readTime: 6,
    date: '2026 站内精选',
    tag: '3D',
  },
  {
    title: 'RAG 加持的 LLM 智能体实战',
    excerpt: 'FastAPI + 向量检索 + 大模型推理，手把手搭建具备记忆与工具调用能力的知识库问答体。',
    tags: ['FastAPI', 'RAG', 'LLM'],
    readTime: 7,
    date: '2026 站内精选',
    tag: 'AI',
  },
]

const list = ref(DEFAULT_ARTICLES)
let ctx = null

// 三档封面渐变（按卡片顺序循环取用）
const grads = [
  'linear-gradient(135deg, #1c1240 0%, #2a1a5e 45%, #4a2a8a 100%)',
  'linear-gradient(135deg, #061a2c 0%, #0a2840 45%, #0e4a6e 100%)',
  'linear-gradient(135deg, #2c0a2e 0%, #4a1040 45%, #7a1e58 100%)',
]

// 根据卡片下标返回封面渐变样式
function gradStyle(i) {
  return { background: grads[i % grads.length] }
}

// 点击卡片跳转博客文章详情（无 id 时进入博客列表）
function go(id) {
  if (id) router.push(`/blog/post/${id}`)
  else router.push('/blog')
}

onMounted(async () => {
  try {
    const res = await getBlogList({ page: 1, size: 3 })
    const rows = res?.data?.list || res?.data?.records || []
    if (Array.isArray(rows) && rows.length) {
      const tagArr = Array.isArray(b.tagsArr) && b.tagsArr.length
        ? b.tagsArr
        : String(b.tags || '').split(',').map(t => t.trim()).filter(Boolean)
      list.value = rows.map((b, i) => ({
        id: b.id,
        title: b.title || DEFAULT_ARTICLES[i % 3].title,
        excerpt: (b.summary || b.description || b.content || '').slice(0, 90),
        tags: (tagArr.length ? tagArr.slice(0, 3) : DEFAULT_ARTICLES[i % 3].tags),
        readTime: Math.max(3, Math.ceil((b.content || '').length / 500)),
        date: (b.updateTime || b.createTime || '').slice(0, 10),
        tag: 'LATEST',
      }))
    }
  } catch (e) {
    // 接口不可用时保留静态文章
  }

  ctx = gsap.context(() => {
    gsap.from('.article-card', {
      y: 70,
      opacity: 0,
      duration: 1,
      stagger: 0.14,
      ease: 'power3.out',
      scrollTrigger: {
        trigger: '.article-grid',
        start: 'top 82%',
        once: true,
      },
    })
  })
})

onBeforeUnmount(() => {
  if (ctx) ctx.revert()
})
</script>

<style scoped>
.articles {
  position: relative;
  padding: 6.5rem 2rem;
  overflow: hidden;
  /* 上下边缘渐隐：与相邻区块自然融合 */
  background: linear-gradient(180deg,
    transparent 0%, rgba(10, 10, 26, 0.22) 16%,
    rgba(6, 6, 14, 0.38) 84%, transparent 100%);
}

.section-bg { position: absolute; inset: 0; z-index: 0; pointer-events: none; }

.bg-grid {
  position: absolute;
  inset: 0;
  background-image: none;
  mask-image: radial-gradient(ellipse 72% 72% at 50% 45%, #000 20%, transparent 80%);
}

.section-container { position: relative; z-index: 1; max-width: 1200px; margin: 0 auto; }

.article-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1.75rem;
}

.article-card {
  position: relative;
  background: rgba(12, 12, 28, 0.7);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.07);
  border-radius: 18px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.4s cubic-bezier(0.23, 1, 0.32, 1), border-color 0.35s, box-shadow 0.35s;
}

.article-card:hover {
  transform: translateY(-6px);
  border-color: rgba(102, 126, 234, 0.45);
  box-shadow: 0 0 36px rgba(102, 126, 234, 0.16), 0 24px 55px rgba(0, 0, 0, 0.5);
}

/* 封面 */
.article-cover {
  position: relative;
  height: 168px;
  overflow: hidden;
}

.cover-grad {
  position: absolute;
  inset: 0;
  transition: transform 0.6s cubic-bezier(0.23, 1, 0.32, 1);
}

.article-card:hover .cover-grad { transform: scale(1.08); }

.cover-noise {
  position: absolute;
  inset: 0;
  opacity: 0.5;
  background:
    repeating-linear-gradient(0deg, transparent 0 2px, rgba(255, 255, 255, 0.03) 2px 3px),
    radial-gradient(circle at 30% 20%, rgba(255, 255, 255, 0.12), transparent 50%),
    radial-gradient(circle at 75% 80%, rgba(0, 212, 255, 0.14), transparent 45%);
}

.cover-index {
  position: absolute;
  right: 1rem;
  top: 0.75rem;
  font-size: 1.5rem;
  font-weight: 800;
  color: rgba(255, 255, 255, 0.12);
  font-family: 'Courier New', monospace;
}

.cover-chip {
  position: absolute;
  left: 1rem;
  bottom: 0.9rem;
  padding: 0.3rem 0.75rem;
  font-size: 0.62rem;
  letter-spacing: 0.2em;
  color: #fff;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: 999px;
  backdrop-filter: blur(6px);
  font-family: 'Courier New', monospace;
}

/* 正文 */
.article-body { padding: 1.4rem 1.5rem 1.6rem; }

.article-tags { display: flex; flex-wrap: wrap; gap: 0.45rem; margin-bottom: 0.85rem; }

.atag {
  font-size: 0.66rem;
  color: #8fa3ff;
  font-family: 'Courier New', monospace;
}

.article-title {
  font-size: 1.05rem;
  font-weight: 700;
  color: #fff;
  line-height: 1.55;
  margin-bottom: 0.65rem;
  transition: color 0.3s;
}

.article-card:hover .article-title { color: #9db2ff; }

.article-excerpt {
  font-size: 0.8rem;
  line-height: 1.75;
  color: rgba(255, 255, 255, 0.45);
  margin-bottom: 1.1rem;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-meta {
  display: flex;
  justify-content: space-between;
  padding-top: 1rem;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  font-size: 0.7rem;
  color: rgba(255, 255, 255, 0.3);
  font-family: 'Courier New', monospace;
  margin-bottom: 1rem;
}

.article-link {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.8);
  letter-spacing: 0.08em;
  transition: color 0.3s;
}

.link-arrow { transition: transform 0.35s cubic-bezier(0.23, 1, 0.32, 1); }

.article-card:hover .article-link { color: #00d4ff; }
.article-card:hover .link-arrow { transform: translateX(6px); }

@media (max-width: 980px) {
  .article-grid { grid-template-columns: 1fr; }
}

@media (max-width: 640px) {
  .articles { padding: 4rem 1.25rem; }
}
</style>