<template>
  <div class="flex items-center justify-between rounded-xl border border-white/10 bg-white/5 p-3">
    <button class="web3-btn" :disabled="page <= 1" @click="change(page - 1)">上一页</button>
    <span class="text-sm text-gray-300">第 {{ page }} / {{ totalPages }} 页</span>
    <button class="web3-btn" :disabled="page >= totalPages" @click="change(page + 1)">下一页</button>
  </div>
</template>

<script setup>
// ============================================================
// 分页组件（Pagination）
// 上一页 / 页码 / 下一页；page 双向绑定，翻页触发 update:page 事件
// ============================================================
const props = defineProps({ page: Number, pageSize: Number, total: Number })
const emit = defineEmits(['update:page'])

// 总页数（至少 1 页）
const totalPages = Math.max(1, Math.ceil((props.total || 0) / (props.pageSize || 20)))

// 翻页：校验边界后通知父组件
function change(p) {
  if (p < 1 || p > totalPages) return
  emit('update:page', p)
}
</script>
