// ============================================================
// 品牌图标降级链 composable（useBrandIcon）
// 统一商城 / 软件中心等处「品牌名 → 官网图标」的两级降级逻辑：
//   0 = 主源（simpleicons 品牌色）→ 1 = 旧版源（jsdelivr 单色）→ 2 = 调用方自定义回退
//
// 用法：
//   const { stage, src, onError } = useBrandIcon()
//   <img v-if="src(item)" :src="src(item)" @error="onError(item)"
//        :style="stage(item.id) === 1 ? 'filter:invert(1)' : ''" />
//   <span v-else>回退内容</span>
// ============================================================
import { ref } from 'vue'
import { resolveBrandIcon, resolveBrandIconLegacy } from '@/utils/brandIcons'

export function useBrandIcon() {
  const stages = ref(new Map()) // 按 item.id 记录降级阶段
  const stage = id => stages.value.get(id) || 0

  const src = item => {
    const st = stage(item?.id)
    if (st === 0) return resolveBrandIcon(item?.name)
    if (st === 1) return resolveBrandIconLegacy(item?.name)
    return ''
  }

  const onError = item => stages.value.set(item.id, stage(item.id) + 1)

  return { stage, src, onError }
}
