<template>
  <div class="min-h-screen px-4 md:px-6 py-10">
    <div class="mx-auto max-w-5xl">
      <h1 class="text-3xl font-bold mb-2 text-gradient-cyber">运维工具箱</h1>
      <p class="text-sm text-gray-500 matrix-text mb-8">Online DevTools · Scripts · Converters · Generators</p>

      <!-- Online Tools Section -->
      <section class="mb-10">
        <h2 class="text-lg font-semibold text-white mb-4 flex items-center gap-2"><span>⚡</span>在线工具</h2>
        <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-5 gap-3">
          <router-link v-for="tool in onlineTools" :key="tool.id"
            :to="tool.path"
            class="glass-panel-sm p-4 text-center cursor-pointer hover:border-purple-400/30 group transition-all duration-300 hover:-translate-y-1">
            <span class="text-2xl">{{ tool.icon }}</span>
            <p class="mt-2 text-xs text-gray-300 group-hover:text-white transition">{{ tool.name }}</p>
            <p class="text-[10px] text-gray-600 mt-1">{{ tool.desc }}</p>
          </router-link>
        </div>
      </section>

      <div class="holo-bar mb-10"></div>

      <!-- Scripts Section -->
      <section>
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-lg font-semibold text-white flex items-center gap-2"><span>📜</span>运维脚本</h2>
          <button class="web3-btn text-xs !px-4 !py-2" @click="openCreate">+ 新建脚本</button>
        </div>
        <div class="flex gap-2 mb-4">
          <button v-for="cat in categories" :key="cat" @click="selectedCat = cat; page=1; fetch()"
            :class="['px-2.5 py-1 rounded-full text-[11px] transition', selectedCat === cat ? 'bg-purple-500/30 text-purple-200 border border-purple-400/30' : 'border border-white/10 text-gray-500 hover:text-white']">{{ cat }}</button>
        </div>
        <div v-if="list.length" class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div v-for="item in list" :key="item.id" class="glass-panel group p-5 cursor-pointer hover:border-cyan-400/30 transition-all duration-300" @click="viewScript(item)">
            <div class="flex items-start justify-between">
              <div class="flex-1">
                <div class="flex items-center gap-2 mb-2">
                  <span class="text-[10px] text-purple-300 border border-purple-400/20 rounded px-1.5 py-0.5">{{ item.language || 'Shell' }}</span>
                  <span class="text-[10px] text-gray-500">{{ item.category }}</span>
                </div>
                <h3 class="font-semibold text-white text-sm group-hover:text-cyan-300 transition">{{ item.name }}</h3>
                <p class="mt-1.5 text-xs text-gray-400 line-clamp-2">{{ item.description }}</p>
              </div>
            </div>
            <div class="mt-3 flex items-center justify-between">
              <div class="flex gap-3 text-[10px] text-gray-600">
                <span>{{ item.version || 'v1.0' }}</span>
                <span>{{ item.downloadCount || 0 }} 次下载</span>
              </div>
              <div class="flex gap-2 opacity-0 group-hover:opacity-100 transition">
                <button class="text-[11px] text-cyan-400 hover:text-cyan-300" @click.stop="editScript(item)">编辑</button>
                <button class="text-[11px] text-red-400 hover:text-red-300" @click.stop="deleteScriptItem(item)">删除</button>
                <span class="text-[11px] text-purple-400 flex items-center gap-1">查看 <span>→</span></span>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="py-10"><Loading /></div>
        <div class="mt-6" v-if="total > 0">
          <Pagination v-model:page="page" :page-size="size" :total="total" @update:page="fetch" />
        </div>
      </section>

      <!-- Create/Edit Script Form -->
      <div v-if="showForm" class="glass-panel p-6 mb-6">
        <h3 class="text-sm font-bold text-white mb-4">{{ editingScript ? '编辑脚本' : '新建脚本' }}</h3>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label class="text-xs text-gray-400 mb-1 block">脚本名称 <span class="text-red-400">*</span></label>
            <input v-model="form.name" class="web3-input text-sm" placeholder="如: 自动化部署脚本" />
          </div>
          <div>
            <label class="text-xs text-gray-400 mb-1 block">语言</label>
            <select v-model="form.language" class="web3-input text-sm">
              <option value="Shell">Shell</option>
              <option value="Python">Python</option>
              <option value="Docker">Docker</option>
              <option value="K8s">K8s</option>
              <option value="CI/CD">CI/CD</option>
            </select>
          </div>
          <div>
            <label class="text-xs text-gray-400 mb-1 block">分类</label>
            <input v-model="form.category" class="web3-input text-sm" placeholder="如: 部署" />
          </div>
          <div>
            <label class="text-xs text-gray-400 mb-1 block">版本</label>
            <input v-model="form.version" class="web3-input text-sm" placeholder="v1.0" />
          </div>
          <div class="md:col-span-2">
            <label class="text-xs text-gray-400 mb-1 block">描述</label>
            <input v-model="form.description" class="web3-input text-sm" placeholder="脚本功能描述" />
          </div>
          <div class="md:col-span-2">
            <label class="text-xs text-gray-400 mb-1 block">脚本内容</label>
            <textarea v-model="form.content" class="web3-input text-sm !min-h-[120px] font-mono" placeholder="#!/bin/bash&#10;echo 'hello world'"></textarea>
          </div>
          <div class="md:col-span-2 flex items-center justify-between">
            <span v-if="formError" class="text-xs text-red-400">{{ formError }}</span>
            <span v-else></span>
            <div class="flex gap-2">
              <button class="text-xs text-gray-400 hover:text-white px-3" @click="showForm = false">取消</button>
              <button class="web3-btn text-xs !px-5" :disabled="!form.name.trim() || submitting" @click="submitForm">
                {{ submitting ? '保存中...' : (editingScript ? '更新' : '创建') }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Code Modal -->
      <Modal v-model="showScript" :title="selectedScript?.name || '脚本详情'">
        <div v-if="selectedScript" class="space-y-3">
          <p class="text-sm text-gray-400">{{ selectedScript.description }}</p>
          <div class="flex gap-2 mb-2">
            <span class="text-xs text-gray-500">语言: {{ selectedScript.language || 'Shell' }}</span>
            <span class="text-xs text-gray-500">分类: {{ selectedScript.category }}</span>
          </div>
          <div class="relative">
            <button class="absolute top-2 right-2 text-xs border border-white/10 rounded px-2 py-1 text-gray-400 hover:text-white transition z-10"
              @click="copyCode">📋 复制</button>
            <pre class="text-xs text-green-300 bg-black/60 rounded-xl p-5 overflow-x-auto max-h-96 font-mono leading-relaxed"><code>{{ selectedScript.content }}</code></pre>
          </div>
        </div>
      </Modal>
    </div>
  </div>
</template>

<script setup>
// ====================================================
// 脚本工具页：脚本列表 + 分类筛选 + 分页，
// 支持创建/编辑/删除脚本与跳转在线工具台
// ====================================================
import { ref, reactive, onMounted } from 'vue'
import { getToolList, createScript, updateScript, deleteScript } from '@/api/tools'
import { useToastStore } from '@/stores/modules/toast'
import Pagination from '@/components/common/Pagination.vue'
import Modal from '@/components/common/Modal.vue'
import Loading from '@/components/common/Loading.vue'
import { confirm as dlgConfirm } from '@/composables/useDialog'

const toast = useToastStore()
const list = ref([]); const page = ref(1); const size = ref(10); const total = ref(0)
const showScript = ref(false); const selectedScript = ref(null)
const selectedCat = ref('全部')
const showForm = ref(false)
const editingScript = ref(null)
const submitting = ref(false)
const formError = ref('')
const form = reactive({ name: '', language: 'Shell', category: '', version: 'v1.0', description: '', content: '' })

// 重置表单为初始空值
function resetForm() { Object.assign(form, { name: '', language: 'Shell', category: '', version: 'v1.0', description: '', content: '' }); formError.value = ''; editingScript.value = null }

// 打开新建脚本表单
function openCreate() { resetForm(); showForm.value = true }

// 打开编辑脚本表单并回填数据
function editScript(item) { editingScript.value = item; Object.assign(form, { name: item.name, language: item.language || 'Shell', category: item.category || '', version: item.version || 'v1.0', description: item.description || '', content: item.content || '' }); showForm.value = true }

// 提交表单：新增或更新脚本并刷新列表
async function submitForm() {
  if (!form.name.trim()) return
  submitting.value = true; formError.value = ''
  try {
    if (editingScript.value) { await updateScript(editingScript.value.id, { ...form }); toast.success('更新成功') }
    else { await createScript({ ...form }); toast.success('创建成功') }
    showForm.value = false; fetch()
  } catch { formError.value = '操作失败，请重试' }
  finally { submitting.value = false }
}

// 删除指定脚本并刷新列表
async function deleteScriptItem(item) {
  if (!(await dlgConfirm(`确定删除脚本 "${item.name}"?`))) return
  try { await deleteScript(item.id); list.value = list.value.filter(x => x.id !== item.id); toast.success('删除成功') }
  catch { toast.error('删除失败') }
}

// 在线工具快捷入口配置
const onlineTools = [
  { id: 'qrnote', name: 'API 工具中心', icon: '🧩', desc: '第三方免费API聚合', path: '/tools/api' },
  { id: 'sites', name: '分享网站', icon: '🛰️', desc: '优质外链收藏', path: '/tools/sites' },
  { id: 'json', name: 'JSON格式化', icon: '{ }', desc: '格式化/压缩/验证', path: '/tools/json' },
  { id: 'base64', name: 'Base64编解码', icon: '🔤', desc: '加密解密转换', path: '/tools/base64' },
  { id: 'timestamp', name: '时间戳转换', icon: '🕐', desc: '时间戳日期互转', path: '/tools/timestamp' },
  { id: 'regex', name: '正则测试', icon: '🔍', desc: '在线正则调试', path: '/tools/regex' },
  { id: 'diff', name: '文本对比', icon: '📝', desc: '代码差异对比', path: '/tools/diff' },
  { id: 'hash', name: 'Hash生成', icon: '#️⃣', desc: 'MD5/SHA1/SHA256', path: '/tools/hash' },
  { id: 'uuid', name: 'UUID生成', icon: '🆔', desc: 'UUID/GUID生成器', path: '/tools/uuid' },
  { id: 'qrcode', name: '二维码生成', icon: '📱', desc: '在线QR Code', path: '/tools/qrcode' },
  { id: 'color', name: '颜色转换', icon: '🎨', desc: 'HEX/RGB/HSL', path: '/tools/color' },
  { id: 'url', name: 'URL编解码', icon: '🔗', desc: 'encode/decode', path: '/tools/url' }
]

// 按分页与分类筛选条件加载脚本列表
function fetch() {
  const params = { page: page.value, size: size.value }
  if (selectedCat.value !== '全部') params.category = selectedCat.value
  getToolList(params).then(res => { const d = res.data || {}; list.value = d.records || []; total.value = d.total || 0 })
}

// 查看脚本内容弹窗
function viewScript(item) { selectedScript.value = item; showScript.value = true }

// 复制脚本内容到剪贴板
function copyCode() {
  if (!selectedScript.value?.content) return
  navigator.clipboard?.writeText(selectedScript.value.content).then(() => toast.success('已复制')).catch(() => toast.error('复制失败'))
}

onMounted(fetch)
</script>
