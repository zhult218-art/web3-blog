<template>
  <!-- ============================================================
       书架（内嵌版）—— 直接平铺在"图书"标签页内
       点击封面后把 { book, rect } 交给父组件打开 3D 阅读器
       ============================================================ -->
  <section class="book-shelf">
    <!-- 头部 -->
    <div class="shelf-header">
      <div>
        <h3 class="shelf-title">藏书阁</h3>
        <p class="shelf-subtitle">公版经典，点击封面开始阅读</p>
      </div>
      <span class="shelf-count">{{ books.length }} 本</span>
    </div>

    <!-- 加载态 -->
    <div v-if="loading" class="shelf-state">
      <div class="shelf-spinner"></div>
      <span>正在整理书架…</span>
    </div>

    <!-- 错误态 -->
    <div v-else-if="error" class="shelf-state">
      <span class="shelf-state-icon">📖</span>
      <p class="shelf-state-text">{{ error }}</p>
      <button class="shelf-retry" @click="loadBooks">重新加载</button>
    </div>

    <!-- 空列表 -->
    <div v-else-if="!books.length" class="shelf-state">
      <span class="shelf-state-icon">🗂️</span>
      <p class="shelf-state-text">书架还是空的</p>
    </div>

    <!-- 书籍封面网格 -->
    <div v-else class="shelf-grid">
      <button
        v-for="book in books"
        :key="book.id"
        class="shelf-book-card"
        @click="onSelectBook(book, $event)"
      >
        <div class="shelf-cover-wrap">
          <img
            v-if="book.cover"
            :src="book.cover"
            :alt="book.title"
            class="shelf-cover"
            loading="lazy"
            draggable="false"
            @contextmenu.prevent
          />
          <!-- 无封面时的文艺占位书脊 -->
          <div v-else class="shelf-cover-fallback">
            <span>{{ book.title.slice(0, 8) }}</span>
          </div>
        </div>
        <div class="shelf-book-name" :title="book.title">{{ book.title }}</div>
        <div v-if="book.author" class="shelf-book-author">{{ book.author }}</div>
        <div v-if="book.summary" class="shelf-book-summary">{{ book.summary }}</div>
      </button>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getClassicBookList } from '@/api/classicBooks'

const emit = defineEmits(['select'])

const books = ref([])
const loading = ref(false)
const error = ref('')

async function loadBooks() {
  loading.value = true
  error.value = ''
  try {
    books.value = await getClassicBookList()
  } catch (e) {
    error.value = '书架加载失败，请稍后再试'
  } finally {
    loading.value = false
  }
}

// 选中一本书：记录封面点击位置（用于 3D 阅读器拉近放大动画的起点）
function onSelectBook(book, ev) {
  const rect = ev.currentTarget.getBoundingClientRect()
  emit('select', {
    book,
    origin: {
      x: rect.left + rect.width / 2,
      y: rect.top + rect.height / 2,
      w: rect.width,
      h: rect.height,
    },
  })
}

onMounted(() => loadBooks())
</script>

<style scoped>
/* 内嵌书架容器：深色 glass 风格，适配媒体页主题 */
.book-shelf {
  background: rgba(255, 255, 255, 0.035);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  padding: 24px;
}

/* 头部 */
.shelf-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 16px;
  margin-bottom: 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.shelf-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.05em;
}
.shelf-subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
}
.shelf-count {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.45);
  padding: 3px 10px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.04);
}

/* 状态区 */
.shelf-state {
  min-height: 260px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  color: rgba(255, 255, 255, 0.5);
  font-size: 14px;
}
.shelf-state-icon { font-size: 42px; opacity: 0.5; }
.shelf-state-text { margin: 0; }
.shelf-retry {
  padding: 8px 22px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.25);
  background: transparent;
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.shelf-retry:hover { background: rgba(255, 255, 255, 0.08); }

/* 加载动画 */
.shelf-spinner {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  border: 3px solid rgba(255, 255, 255, 0.15);
  border-top-color: #67e8f9;
  animation: shelf-spin 0.9s linear infinite;
}
@keyframes shelf-spin { to { transform: rotate(360deg); } }

/* 封面网格 */
.shelf-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 22px;
}
.shelf-book-card {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  -webkit-tap-highlight-color: transparent;
}
.shelf-cover-wrap {
  width: 132px;
  height: 178px;
  border-radius: 6px 12px 12px 6px;
  overflow: hidden;
  box-shadow:
    0 2px 4px rgba(0, 0, 0, 0.3),
    0 12px 24px -10px rgba(0, 0, 0, 0.5);
  transition: transform 0.3s ease-out, box-shadow 0.3s ease-out;
  background: linear-gradient(135deg, #4a4238, #2e2820);
}
.shelf-book-card:hover .shelf-cover-wrap {
  transform: translateY(-6px) rotate(-1deg);
  box-shadow:
    0 6px 10px rgba(0, 0, 0, 0.35),
    0 22px 36px -12px rgba(0, 0, 0, 0.6);
}
.shelf-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  user-select: none;
  -webkit-user-drag: none;
}
.shelf-cover-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px;
  background: linear-gradient(135deg, #6b5d4f, #3e3428);
  color: rgba(255, 255, 255, 0.92);
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.1em;
  writing-mode: vertical-rl;
}
.shelf-book-name {
  margin-top: 12px;
  font-size: 14px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.92);
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.shelf-book-author {
  margin-top: 2px;
  font-size: 10.5px;
  color: rgba(255, 255, 255, 0.4);
}
.shelf-book-summary {
  margin-top: 4px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.45);
  line-height: 1.5;
  max-width: 140px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 移动端 */
@media (max-width: 767px) {
  .book-shelf { padding: 18px; }
  .shelf-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }
  .shelf-cover-wrap { width: 100%; max-width: 130px; height: 170px; }
}
</style>
