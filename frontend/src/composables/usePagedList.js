// ============================================================
// 通用分页列表 composable（usePagedList）
// 统一「分页 + 分类筛选 + 加载 / 空状态」列表页逻辑，消除各列表页
// 复制粘贴的 page/size/total/loaded/fetch 样板代码。
//
// 用法：
//   const { list, page, total, loaded, selectedCat, load, setCategory, removeWhere } =
//     usePagedList(getProductList, { pageSize: 12 })
//   <Pagination v-model:page="page" :page-size="size" :total="total" @update:page="load" />
//   <button @click="setCategory(cat)">…</button>
//
// - load()        按当前页码 / 分类拉取列表（竞态安全：仅采纳最后一次请求的结果）
// - setCategory() 切换分类并回到第一页
// - reload()      回到第一页重新拉取（新增 / 编辑后刷新）
// - removeWhere() 删除后本地移除条目，避免整页重拉
// 约定接口返回结构：{ data: { records: [...], total: n } }
// ============================================================
import { ref } from 'vue'

export function usePagedList(fetcher, { pageSize = 12, categoryKey = 'category', allLabel = '全部' } = {}) {
  const list = ref([])
  const page = ref(1)
  const size = ref(pageSize)
  const total = ref(0)
  const loading = ref(false)
  const loaded = ref(false) // 至少完成过一次加载（用于区分骨架屏与空状态）
  const selectedCat = ref(allLabel)
  let seq = 0 // 请求序号：快速切换分类 / 翻页时丢弃过期响应

  async function load() {
    const cur = ++seq
    loading.value = true
    const params = { page: page.value, size: size.value }
    if (selectedCat.value !== allLabel) params[categoryKey] = selectedCat.value
    try {
      const res = await fetcher(params)
      if (cur !== seq) return
      const d = res?.data || {}
      list.value = d.records || []
      total.value = d.total || 0
    } catch {
      /* 失败已由 request 拦截器统一提示 */
    } finally {
      if (cur === seq) {
        loading.value = false
        loaded.value = true
      }
    }
  }

  function reload() {
    page.value = 1
    return load()
  }

  function setCategory(cat) {
    selectedCat.value = cat
    return reload()
  }

  function removeWhere(predicate) {
    list.value = list.value.filter(item => !predicate(item))
  }

  return { list, page, size, total, loading, loaded, selectedCat, load, reload, setCategory, removeWhere }
}
