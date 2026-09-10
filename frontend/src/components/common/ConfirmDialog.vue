<template>
  <Teleport to="body">
    <Transition name="dlg">
      <div v-if="d.state.visible" class="dlg-overlay" @click.self="d.handleCancel">
        <div class="dlg-card" @keydown.esc="d.handleCancel">
          <div class="dlg-header">
            <div class="dlg-icon" v-if="d.state.type === 'confirm'">?</div>
            <div class="dlg-icon dlg-icon-warn" v-else-if="d.state.type === 'prompt'">✎</div>
            <div class="dlg-icon dlg-icon-info" v-else>i</div>
            <h3 class="dlg-title">{{ d.state.title }}</h3>
          </div>
          <p class="dlg-message">{{ d.state.message }}</p>
          <input
            v-if="d.state.type === 'prompt'"
            ref="inputRef"
            v-model="d.state.inputValue"
            :placeholder="d.state.placeholder"
            class="dlg-input"
            @keydown.enter="d.handleOk"
          />
          <div class="dlg-actions">
            <button v-if="d.state.type !== 'alert'" class="dlg-btn dlg-btn-cancel" @click="d.handleCancel">取消</button>
            <button class="dlg-btn dlg-btn-ok" @click="d.handleOk" ref="okBtnRef">
              {{ d.state.type === 'alert' ? '知道了' : '确认' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { useDialog } from '@/composables/useDialog'

const d = useDialog()
const inputRef = ref(null)
const okBtnRef = ref(null)

watch(() => d.state.visible, async (v) => {
  if (v) {
    await nextTick()
    if (d.state.type === 'prompt') inputRef.value?.focus()
    else okBtnRef.value?.focus()
  }
})
</script>

<style scoped>
.dlg-overlay {
  position: fixed; inset: 0; z-index: 9999;
  display: flex; align-items: center; justify-content: center;
  background: rgba(0, 0, 0, 0.65);
  backdrop-filter: blur(6px);
}
.dlg-card {
  width: 100%; max-width: 400px; margin: 0 16px;
  background: rgba(12, 12, 45, 0.92);
  border: 1px solid rgba(102, 126, 234, 0.2);
  border-radius: 16px;
  padding: 28px 28px 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5), 0 0 40px rgba(102, 126, 234, 0.08);
  backdrop-filter: blur(20px);
}
.dlg-header {
  display: flex; align-items: center; gap: 12px; margin-bottom: 16px;
}
.dlg-icon {
  width: 36px; height: 36px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-size: 1.1rem; font-weight: 700; flex-shrink: 0;
  background: rgba(102, 126, 234, 0.12);
  border: 1px solid rgba(102, 126, 234, 0.25);
  color: #818cf8;
}
.dlg-icon-warn {
  background: rgba(139, 92, 246, 0.12);
  border-color: rgba(139, 92, 246, 0.25);
  color: #a78bfa;
}
.dlg-icon-info {
  background: rgba(59, 130, 246, 0.12);
  border-color: rgba(59, 130, 246, 0.25);
  color: #60a5fa;
}
.dlg-title {
  margin: 0; font-size: 1rem; font-weight: 600; color: #e2e8f0;
}
.dlg-message {
  margin: 0 0 20px; font-size: 0.85rem; color: rgba(255, 255, 255, 0.55);
  line-height: 1.6;
}
.dlg-input {
  width: 100%; padding: 10px 14px; margin-bottom: 20px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 10px; color: #e2e8f0; font-size: 0.85rem;
  outline: none; transition: border-color 0.2s;
  box-sizing: border-box;
}
.dlg-input:focus {
  border-color: rgba(102, 126, 234, 0.5);
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.1);
}
.dlg-input::placeholder { color: rgba(255, 255, 255, 0.25); }
.dlg-actions {
  display: flex; justify-content: flex-end; gap: 10px;
}
.dlg-btn {
  padding: 8px 22px; border-radius: 10px; font-size: 0.82rem;
  font-weight: 500; cursor: pointer; border: none;
  transition: all 0.2s;
}
.dlg-btn-cancel {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.5);
}
.dlg-btn-cancel:hover {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.8);
}
.dlg-btn-ok {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
}
.dlg-btn-ok:hover {
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.35);
  transform: translateY(-1px);
}

.dlg-enter-active { transition: opacity 0.2s ease; }
.dlg-leave-active { transition: opacity 0.15s ease; }
.dlg-enter-from, .dlg-leave-to { opacity: 0; }
.dlg-enter-active .dlg-card { animation: dlgIn 0.25s ease-out; }
.dlg-leave-active .dlg-card { animation: dlgOut 0.15s ease-in; }
@keyframes dlgIn { from { transform: scale(0.92) translateY(10px); opacity: 0; } to { transform: scale(1) translateY(0); opacity: 1; } }
@keyframes dlgOut { from { transform: scale(1); opacity: 1; } to { transform: scale(0.95); opacity: 0; } }
</style>
