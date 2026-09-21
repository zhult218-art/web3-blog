<template>
  <!-- 桌面物品：桌面端绝对定位（由父级 top/left 决定位置），移动端自动转网格 -->
  <button
    class="desktop-item"
    :style="positionStyle"
    @click="$emit('click', item)"
    @contextmenu.prevent
  >
    <div class="di-image-wrap">
      <!-- 透明 PNG 素材（加载失败时自动降级为 emoji 占位，方便先跑通再替换素材） -->
      <img
        v-if="!imgError"
        :src="item.imageUrl"
        :alt="item.name"
        class="di-image"
        draggable="false"
        @error="imgError = true"
        @contextmenu.prevent
      />
      <span v-else class="di-emoji">{{ item.emoji || '📦' }}</span>
    </div>
    <!-- 物品名称标签：hover 时 0.3 → 1 并轻微放大 -->
    <span class="di-label">{{ item.name }}</span>
  </button>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

// item: { id, name, imageUrl, emoji, type, top, left, routePath }
const props = defineProps({
  item: { type: Object, required: true },
})
defineEmits(['click'])

// 图片加载失败标记（切换物品时重置）
const imgError = ref(false)
watch(() => props.item.imageUrl, () => { imgError.value = false })

// 桌面端绝对定位的位置；移动端 CSS 媒体查询会覆盖为静态网格
const positionStyle = computed(() => ({
  top: props.item.top ?? '50%',
  left: props.item.left ?? '50%',
}))
</script>

<style scoped>
.desktop-item {
  position: absolute;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 8px;
  z-index: 2;
  -webkit-tap-highlight-color: transparent;
}

.di-image-wrap {
  /* 多层柔和白色外发光（box-shadow，不用 filter，避免性能开销） */
  border-radius: 18px;
  transition: transform 0.3s ease-out, box-shadow 0.3s ease-out;
  display: flex;
  align-items: center;
  justify-content: center;
}

.di-image {
  width: 110px;
  height: 110px;
  object-fit: contain;
  user-select: none;
  -webkit-user-drag: none;
  display: block;
}

/* PNG 未就位时的 emoji 兜底样式 */
.di-emoji {
  width: 110px;
  height: 110px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 64px;
  border-radius: 22px;
  background: rgba(250, 247, 242, 0.55);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.4);
}

.di-label {
  font-size: 14px;
  letter-spacing: 0.08em;
  color: #5c5650;                 /* 莫兰迪暖灰 */
  background: rgba(250, 247, 242, 0.72);
  padding: 3px 12px;
  border-radius: 999px;
  opacity: 0.3;
  transform: scale(0.94);
  transition: opacity 0.3s ease-out, transform 0.3s ease-out;
  white-space: nowrap;
}

/* hover：上浮 4px + 多层柔和白光 + 标签清晰放大 */
.desktop-item:hover .di-image-wrap,
.desktop-item:active .di-image-wrap {
  transform: translateY(-4px);
  box-shadow:
    0 0 0 1px rgba(255, 255, 255, 0.55),
    0 0 18px 2px rgba(255, 255, 255, 0.65),
    0 0 42px 10px rgba(255, 248, 238, 0.45),
    0 14px 30px -12px rgba(120, 105, 90, 0.35);
}
.desktop-item:hover .di-label,
.desktop-item:active .di-label {
  opacity: 1;
  transform: scale(1.04);
}

/* ===== 移动端（<768px）：放弃绝对定位，改为 2×2 网格 ===== */
@media (max-width: 767px) {
  .desktop-item {
    position: static;
    transform: none;
    padding: 12px 6px;
  }
  .di-image,
  .di-emoji {
    width: 72px;
    height: 72px;
    font-size: 42px;
  }
  .di-label {
    font-size: 12px;
    opacity: 0.85;     /* 移动端标签常显，无 hover 条件 */
    transform: none;
  }
  /* 触摸按下时上浮发光 */
  .desktop-item:active .di-image-wrap {
    transform: translateY(-4px);
  }
}
</style>
