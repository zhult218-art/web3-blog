import { reactive } from 'vue'

const state = reactive({
  visible: false,
  type: 'confirm',
  title: '',
  message: '',
  placeholder: '',
  inputValue: '',
})

let _resolve = null

function open(type, message, opts = {}) {
  return new Promise(resolve => {
    state.type = type
    state.title = opts?.title || (type === 'confirm' ? '确认' : type === 'prompt' ? '输入' : '提示')
    state.message = message
    state.placeholder = opts?.placeholder || ''
    state.inputValue = opts?.defaultValue || ''
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
