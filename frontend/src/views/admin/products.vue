<template>
  <div class="min-h-screen py-8 px-4 md:px-6">
    <div class="max-w-7xl mx-auto">
      <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <h1 class="text-xl font-bold text-white"><span class="text-gradient-cyber">Product</span> Management</h1>
        <div class="flex flex-col sm:flex-row gap-3">
          <input v-model="search" type="text" placeholder="搜索商品..." class="web3-input w-full sm:w-64" @keyup.enter="fetchProducts" />
          <button class="web3-btn text-xs !px-4 !py-2" @click="fetchProducts">搜索</button>
          <button class="web3-btn text-xs !px-4 !py-2" @click="openCreate">+ 新建商品</button>
        </div>
      </div>
      <div class="glass-panel overflow-hidden">
        <div class="overflow-x-auto">
          <table class="web3-table min-w-[840px]">
            <thead><tr><th>ID</th><th>封面</th><th>名称</th><th>分类</th><th>价格</th><th>库存</th><th>销量</th><th>创建时间</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="9" class="text-center py-10"><Loading text="加载中..." /></td>
              </tr>
              <tr v-else-if="!list.length">
                <td colspan="9" class="text-center text-gray-600 py-10">暂无商品</td>
              </tr>
              <tr v-for="p in list" :key="p.id">
                <td class="matrix-text text-xs">#{{ p.id }}</td>
                <td>
                  <img v-if="p.cover" :src="p.cover" class="w-10 h-10 rounded-lg object-cover" alt="cover"
                    @error="onImgErr" />
                  <span v-else class="text-gray-600 text-xs">无封面</span>
                </td>
                <td class="text-white/80 font-medium max-w-xs truncate">{{ p.name }}</td>
                <td><span class="web3-badge-purple whitespace-nowrap">{{ p.category || '-' }}</span></td>
                <td class="text-white/80 font-medium">¥{{ p.price }}</td>
                <td class="text-white/60 matrix-text text-xs">{{ p.stock ?? 0 }}</td>
                <td class="text-white/60 matrix-text text-xs">{{ p.sales ?? 0 }}</td>
                <td class="text-gray-500 text-xs matrix-text">{{ formatDate(p.createdAt) }}</td>
                <td>
                  <div class="flex gap-2 whitespace-nowrap">
                    <button class="text-xs text-cyan-400 hover:text-cyan-300" @click="openEdit(p)">编辑</button>
                    <button class="text-xs text-red-400 hover:text-red-300" @click="doDelete(p)">删除</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <div class="mt-4" v-if="total > 0">
        <Pagination v-model:page="page" :page-size="size" :total="total" @update:page="fetchProducts" />
      </div>
    </div>

    <Modal v-model="showModal" :title="editing ? '编辑商品' : '新建商品'">
      <div class="space-y-4">
        <div>
          <label class="text-xs text-gray-400 block mb-1">名称 <span class="text-red-400">*</span></label>
          <input v-model="form.name" class="web3-input text-sm" placeholder="商品名称" />
        </div>
        <div>
          <label class="text-xs text-gray-400 block mb-1">分类</label>
          <input v-model="form.category" class="web3-input text-sm" placeholder="如: 书籍 / 周边" />
        </div>
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label class="text-xs text-gray-400 block mb-1">价格 <span class="text-red-400">*</span></label>
            <input v-model.number="form.price" type="number" min="0" step="0.01" class="web3-input text-sm" placeholder="0.00" />
          </div>
          <div>
            <label class="text-xs text-gray-400 block mb-1">库存 <span class="text-red-400">*</span></label>
            <input v-model.number="form.stock" type="number" min="0" step="1" class="web3-input text-sm" placeholder="0" />
          </div>
        </div>
        <div>
          <label class="text-xs text-gray-400 block mb-1">封面URL</label>
          <input v-model="form.cover" class="web3-input text-sm" placeholder="https://..." />
        </div>
        <div>
          <label class="text-xs text-gray-400 block mb-1">描述</label>
          <textarea v-model="form.description" rows="4" class="web3-input text-sm w-full resize-y" placeholder="商品描述..."></textarea>
        </div>
        <div class="flex justify-end gap-3 pt-2">
          <button class="web3-btn-outline text-xs !px-5 !py-2" @click="showModal = false">取消</button>
          <button class="web3-btn text-xs !px-5 !py-2 flex items-center gap-2" :disabled="submitting" @click="submitProduct">
            <span v-if="submitting" class="inline-block h-3 w-3 rounded-full border-2 border-white/30 border-t-white animate-spin"></span>
            {{ submitting ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </Modal>
  </div>
</template>

<script setup>
// ====================================================
// 商品管理：商品列表（搜索 + 分页），
// 支持新建/编辑/删除商品（仅管理员）
// ====================================================
import { ref, onMounted } from 'vue'
import { getProductList, createProduct, updateProduct, deleteProduct } from '@/api/shop'
import Pagination from '@/components/common/Pagination.vue'
import Modal from '@/components/common/Modal.vue'
import Loading from '@/components/common/Loading.vue'
import { useToastStore } from '@/stores/modules/toast'
import { confirm as dlgConfirm } from '@/composables/useDialog'

const toast = useToastStore()
const list = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const search = ref('')
const loading = ref(false)
const showModal = ref(false)
const editing = ref(null)
const submitting = ref(false)
const form = ref({ name: '', description: '', price: null, stock: null, cover: '', category: '' })

// 本地化格式化日期
function formatDate(d) { return d ? new Date(d).toLocaleDateString('zh-CN') : '' }

// 封面图加载失败的兜底：隐藏图片占位
function onImgErr(e) { e.target.style.display = 'none' }

// 按分页与搜索关键字加载商品列表
async function fetchProducts() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (search.value.trim()) params.keyword = search.value.trim()
    const res = await getProductList(params)
    const data = res.data || {}
    list.value = data.records || []
    total.value = data.total || 0
  } catch { list.value = [] }
  finally { loading.value = false }
}

// 打开新建商品弹窗
function openCreate() {
  editing.value = null
  form.value = { name: '', description: '', price: null, stock: null, cover: '', category: '' }
  showModal.value = true
}

// 打开编辑弹窗并回填商品数据
function openEdit(p) {
  editing.value = p
  form.value = {
    name: p.name || '',
    description: p.description || '',
    price: p.price != null ? Number(p.price) : null,
    stock: p.stock != null ? Number(p.stock) : null,
    cover: p.cover || '',
    category: p.category || '',
  }
  showModal.value = true
}

// 提交创建或更新商品
async function submitProduct() {
  if (!form.value.name.trim()) { toast.warning('商品名称必填'); return }
  if (form.value.price == null || form.value.price < 0) { toast.warning('请输入有效价格'); return }
  if (form.value.stock == null || form.value.stock < 0) { toast.warning('请输入有效库存'); return }
  submitting.value = true
  const payload = {
    name: form.value.name.trim(),
    description: form.value.description,
    price: Number(form.value.price),
    stock: Number(form.value.stock),
    cover: form.value.cover,
    category: form.value.category,
  }
  try {
    if (editing.value) {
      await updateProduct(editing.value.id, payload)
      toast.success('商品已更新')
    } else {
      await createProduct(payload)
      toast.success('商品已创建')
    }
    showModal.value = false
    fetchProducts()
  } catch { toast.error('保存失败') }
  finally { submitting.value = false }
}

// 删除商品（二次确认后执行）
async function doDelete(p) {
  if (!(await dlgConfirm(`确定删除商品 "${p.name}"?`))) return
  try {
    await deleteProduct(p.id)
    list.value = list.value.filter(x => x.id !== p.id)
    toast.success('删除成功')
  } catch { toast.error('删除失败') }
}

// 挂载时加载商品列表
onMounted(fetchProducts)
</script>