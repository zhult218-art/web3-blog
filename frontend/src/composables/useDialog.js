import { reactive } from 'vue'

const state = reactive({
  visible: false,
  type: 'confirm',
  title: '',
  message: '',
  placeholder: '',
  inputValue: '',
  // 主题色调：default=品牌紫；danger=红色（删除等不可逆操作）
  tone: 'default',
  // 确认按钮文案，空时按类型使用默认文案
  okText: '',
  cancelText: '',
})

let _resolve = null

function open(type, message, opts = {}) {
  return new Promise(resolve => {
    state.type = type
    state.title = opts?.title || (type === 'confirm' ? '确认' : type === 'prompt' ? '输入' : '提示')
    state.message = message
    state.placeholder = opts?.placeholder || ''
    state.inputValue = opts?.defaultValue || ''
    state.tone = opts?.tone === 'danger' ? 'danger' : 'default'
    state.okText = opts?.okText || ''
    state.cancelText = opts?.cancelText || ''
    state.visible = true
    _resolve = resolve
  })
}

export function confirm(message, opts) { return open('confirm', message, opts) }
export function alert(message, opts) { return open('alert', message, opts) }
export function prompt(message, opts) { return open('prompt', message, opts) }

export function handleOk() {
  state.visible = false
  const r = _resolve; _resolve = null
  r?.(state.type === 'prompt' ? state.inputValue : true)
}

export function handleCancel() {
  state.visible = false
  const r = _resolve; _resolve = null
  r?.(state.type === 'prompt' ? null : false)
}

export function useDialog() {
  return { state, confirm, alert, prompt, handleOk, handleCancel }
}
