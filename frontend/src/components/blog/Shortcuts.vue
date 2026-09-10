<template>
  <Teleport to="body">
    <Transition name="overlay">
      <div v-if="ui.shortcutsOpen" class="fixed inset-0 z-[120] flex items-center justify-center p-4" @click.self="ui.toggleShortcuts()">
        <div class="absolute inset-0 bg-black/50 backdrop-blur-sm"></div>
        <div class="relative w-full max-w-md rounded-2xl border border-white/10 bg-[#0a0a18]/95 backdrop-blur-xl shadow-2xl p-6">
          <div class="flex items-center justify-between mb-5">
            <h3 class="text-base font-bold text-white flex items-center gap-2"><span class="text-purple-300">⌨</span> 快捷键</h3>
            <button class="text-gray-500 hover:text-white transition text-sm" @click="ui.toggleShortcuts()">✕</button>
          </div>

          <div class="space-y-2">
            <div v-for="b in SHORTCUT_BINDINGS" :key="b.key" class="flex items-center justify-between px-3 py-2 rounded-lg bg-[#0e0e26]">
              <span class="text-sm text-gray-300">{{ b.label }}</span>
              <kbd class="kbd">{{ b.key }}</kbd>
            </div>
          </div>

          <label class="mt-5 flex items-center justify-between px-3 py-2 rounded-lg border border-white/[0.06] cursor-pointer">
            <span class="text-sm text-gray-300">启用快捷键</span>
            <input type="checkbox" :checked="!disabled" class="accent-purple-500 w-4 h-4" @change="onToggle" />
          </label>

          <!-- 提示 -->
          <div class="mt-4 text-[11px] leading-relaxed text-gray-600">
            <p>输入框内（昵称/邮箱/评论…）输入时快捷键自动忽略，不冲突。</p>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
// ============================================================
// 快捷键面板（Shift+K 打开）
// 展示全部快捷键并支持整体开关（localStorage 持久化）
// ============================================================
import { ref } from 'vue'
import { useUiStore } from '@/stores/modules/ui'
import { SHORTCUT_BINDINGS, isShortcutsDisabled, setShortcutsDisabled } from '@/composables/useShortcuts'

const ui = useUiStore()
const disabled = ref(isShortcutsDisabled())

function onToggle(e) {
  disabled.value = !e.target.checked
  setShortcutsDisabled(disabled.value)
  if (disabled.value) {
    // 当快捷键被关闭时，也要保证面板能再打开：Esc 与按钮可用
  }
}
</script>

<style scoped>
.kbd {
  @apply px-2 py-1 rounded-md text-[11px] font-mono text-cyan-300 bg-[#141432] border border-white/10;
  box-shadow: inset 0 -2px 0 rgba(255,255,255,0.06);
}
.overlay-enter-active, .overlay-leave-active { transition: opacity 0.2s ease; }
.overlay-enter-from, .overlay-leave-to { opacity: 0; }
</style>