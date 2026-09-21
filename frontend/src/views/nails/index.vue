<template>
  <div class="nail-page" ref="pageEl">
    <canvas ref="fxCanvas" class="fx-canvas"></canvas>
    <span v-for="s in 14" :key="s" class="float-deco" :class="'d' + s">{{ s % 3 === 0 ? '💗' : s % 3 === 1 ? '✨' : '🌸' }}</span>

    <div class="wrap">
      <!-- 店铺主卡 -->
      <section class="hero-card">
        <div class="hero-left">
          <div class="avatar">💅</div>
          <div>
            <h1 class="shop-name"><span class="shimmer-text">{{ profile.name }}</span></h1>
            <p class="shop-sign">{{ profile.sign }}</p>
          </div>
        </div>
        <div class="hero-chips">
          <button class="chip" @click="copy(profile.loc)">📍 {{ profile.loc }}<small>点击复制</small></button>
          <span class="chip plain">🕐 {{ profile.hours }}</span>
        </div>
        <div class="contact-row">
          <button class="c-btn wx" @click="copy(profile.wx)">💬 微信：{{ profile.wx }}</button>
          <button class="c-btn qq" @click="copy(profile.qq)">🐧 QQ：{{ profile.qq }}</button>
          <a class="c-btn tel" :href="'tel:' + profile.tel.replace(/[^0-9]/g, '')">📱 电话：{{ profile.tel }}</a>
        </div>
        <div class="share-actions">
          <button v-if="isOwner" class="owner-btn" @click="openEdit">🪄 编辑店铺信息</button>
          <button class="share-btn" @click="shareShop">🔗 分享小铺</button>
        </div>
      </section>

      <!-- 店主工具条 -->
      <section v-if="isOwner" class="owner-bar">
        <label class="upload-btn">
          ＋ 上传新作品
          <input type="file" accept="image/*" multiple hidden @change="onFiles" />
        </label>
        <button class="owner-btn" @click="openBookings">📋 预约管理<span v-if="todayPending > 0" class="badge">{{ todayPending }}</span></button>
        <p class="owner-tip">上传后立即展示在小铺里，访客可以看到并联系你～</p>
      </section>

      <!-- 店铺介绍 / 服务价目 / 预约须知 / 交通 -->
      <section class="shop-meta">
        <div class="meta-block">
          <h3 class="meta-title">🎀 关于小铺</h3>
          <p class="meta-text">{{ shopMeta.introduction }}</p>
        </div>
        <div class="meta-block">
          <h3 class="meta-title">💎 服务价目</h3>
          <ul class="service-list">
            <li v-for="(s, i) in shopMeta.services" :key="i" class="service-item">
              <div class="svc-head">
                <span class="svc-name">{{ s.name }}</span>
                <span class="svc-price">{{ s.price }}</span>
              </div>
              <p class="svc-desc">{{ s.desc }}</p>
            </li>
          </ul>
        </div>
        <div class="meta-block">
          <h3 class="meta-title">📌 预约须知</h3>
          <ol class="policy-list">
            <li v-for="(p, i) in shopMeta.policy" :key="i">{{ p }}</li>
          </ol>
        </div>
        <div class="meta-block">
          <h3 class="meta-title">🚇 交通 / 停车</h3>
          <p class="meta-text">{{ shopMeta.traffic }}</p>
        </div>
      </section>

      <!-- 标签筛选 -->
      <section class="filter-row">
        <button
          v-for="t in ['全部', ...allTags]"
          :key="t"
          class="tag-chip"
          :class="{ on: filterTag === t }"
          @click="filterTag = t"
        >{{ t }}</button>
      </section>

      <!-- 作品墙 -->
      <transition-group name="pop" tag="section" class="gallery">
        <article v-for="(w, wi) in shownWorks" :key="w.id"
          class="work-card"
          :style="{ '--enter-delay': (wi % 8) * 60 + 'ms' }"
          @mousemove="onTilt"
          @mouseleave="onTiltLeave"
        >
          <div class="pic-wrap">
            <img :src="w.img" :alt="w.title" />
            <span v-if="w.price" class="price-badge">￥{{ w.price }}</span>
            <button v-if="isOwner" class="del-btn" title="删除" @click="removeWork(w.id)">×</button>
          </div>
          <div class="card-body">
            <h3>{{ w.title }}</h3>
            <div class="tag-line">
              <span v-for="t in w.tags" :key="t" class="mini-tag">{{ t }}</span>
            </div>
            <div class="card-foot">
              <button class="like-btn" :class="{ liked: likedIds.includes(w.id) }" @click="like($event, w)">
                {{ likedIds.includes(w.id) ? '💖' : '🤍' }} {{ w.likes }}
              </button>
              <div class="bk-card-actions">
                <button class="bk-mini" @click="openBooking(w.title)">预约此款</button>
                <button class="share-mini" title="分享" @click="shareWork(w)">🔗</button>
              </div>
              <time>{{ fmtDate(w.ts) }}</time>
            </div>
          </div>
        </article>
      </transition-group>

      <div v-if="!shownWorks.length" class="empty">
        <span class="empty-face">(๑•́ ₃ •̀๑)</span>
        <p>{{ filterTag === '全部' ? '店主正在绘制第一批作品，敬请期待～' : '这个分类还没有作品哦' }}</p>
      </div>

      <!-- 预约引导 -->
      <footer class="book-cta">
        <p>喜欢哪款？随时联系店主预约吧 ✨</p>
        <div class="bk-cta-row">
          <button class="bk-open-btn" @click="openBooking()">💅 在线预约</button>
          <button class="book-btn" @click="copy(profile.wx)">微信预约：{{ profile.wx }}</button>
          <button class="my-bk-btn" @click="openMyBookings">📋 我的预约</button>
        </div>
      </footer>
    </div>

    <!-- 店铺信息编辑弹层 -->
    <transition name="fade">
      <div v-if="editing" class="modal-mask" @click.self="editing = false">
        <div class="modal">
          <h2>🪄 编辑店铺信息</h2>
          <label v-for="f in [
            ['name', '店铺名'], ['sign', '签名标语'],
            ['loc', '店铺位置'], ['hours', '营业时间'],
            ['wx', '微信号'], ['qq', 'QQ号'], ['tel', '联系电话']
          ]" :key="f[0]">
            <span>{{ f[1] }}</span>
            <input v-model="draft[f[0]]" type="text" />
          </label>
          <div class="modal-foot">
            <button class="ghost" @click="editing = false">取消</button>
            <button class="primary" @click="saveProfile">保存 💖</button>
          </div>
        </div>
      </div>
    </transition>

    <!-- 在线预约弹层（公开接口，游客可提交） -->
    <transition name="fade">
      <div v-if="booking" class="modal-mask" @click.self="booking = false">
        <div class="modal bk-modal">
          <template v-if="!bkDone">
            <h2>💅 在线预约</h2>
            <details class="bk-policy" open>
              <summary>📌 预约须知（点击展开/收起）</summary>
              <ol class="bk-policy-list">
                <li v-for="(p, i) in shopMeta.policy.slice(0, 3)" :key="i">{{ p }}</li>
              </ol>
            </details>
            <label><span>称呼 *</span><input v-model="bkForm.name" type="text" placeholder="怎么称呼你呀" /></label>
            <label><span>手机号 *</span><input v-model="bkForm.phone" type="text" maxlength="11" placeholder="11 位手机号，方便店主联系你" /></label>
            <label><span>款式 *</span>
              <select v-model="bkForm.service" class="bk-select">
                <option value="" disabled>请选择款式</option>
                <option v-for="s in bkServices" :key="s" :value="s">{{ s }}</option>
              </select>
            </label>
            <label><span>日期 *</span><input v-model="bkForm.date" type="date" :min="todayStr" /></label>
            <div class="bk-field"><span>时间段 *</span>
              <div class="bk-slots">
                <button v-for="t in bkTimeSlots" :key="t" type="button" class="bk-slot" :class="{ on: bkForm.timeSlot === t }" @click="bkForm.timeSlot = t">{{ t }}</button>
              </div>
            </div>
            <label><span>备注（选填）</span><input v-model="bkForm.note" type="text" placeholder="想补充的小细节～" /></label>
            <div class="modal-foot">
              <button class="ghost" @click="booking = false">取消</button>
              <button class="primary" :disabled="bkSubmitting" @click="submitBooking">{{ bkSubmitting ? '提交中…' : '提交预约 💖' }}</button>
            </div>
          </template>
          <div v-else class="bk-success">
            <span class="bk-check">✓</span>
            <p>预约成功！<br />店主会尽快联系你 💅</p>
            <p class="bk-success-tip">下一步：加店主微信 <b>{{ profile.wx }}</b> 备注「预约 + 手机号尾四位」完成确认<br />
              可在底部「📋 我的预约」入口随时查看 / 取消</p>
            <div class="bk-success-actions">
              <button class="bk-copy-btn" @click="copy(profile.wx)">📋 复制微信号</button>
              <button class="bk-close" @click="booking = false">好哒～</button>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <!-- 店主预约管理弹层 -->
    <transition name="fade">
      <div v-if="bkMgr" class="modal-mask" @click.self="bkMgr = false">
        <div class="modal bk-mgr-modal">
          <h2>📋 预约管理<span v-if="bookings.length" class="mgr-count">共 {{ bookings.length }} 条</span></h2>
          <!-- 今日统计 -->
          <div v-if="bookings.length" class="mgr-stats">
            <div class="stat-chip"><span>今日待确认</span><b>{{ todayPending }}</b></div>
            <div class="stat-chip"><span>今日已确认</span><b>{{ todayConfirmed }}</b></div>
            <div class="stat-chip"><span>今日已完成</span><b>{{ todayDone }}</b></div>
            <div class="stat-chip"><span>今日已取消</span><b>{{ todayCancelled }}</b></div>
          </div>
          <p v-if="bkLoading" class="bk-empty">加载中…</p>
          <p v-else-if="!bookings.length" class="bk-empty">还没有预约哦，快去分享小铺吧～</p>
          <div v-else class="bk-list">
            <div v-for="b in bookings" :key="b.id" class="bk-item">
              <div class="bk-item-main">
                <p class="bk-item-title">{{ b.name }} · {{ b.service }}<em class="bk-status" :class="'s-' + b.status">{{ statusText(b.status) }}</em></p>
                <p class="bk-item-sub">📱 {{ b.phone }} · 🗓 {{ b.date }} {{ b.timeSlot }}<template v-if="b.note"> · 📝 {{ b.note }}</template></p>
              </div>
              <div class="bk-item-ops">
                <button v-if="b.status === 'PENDING'" class="bk-op confirm" @click="setBookingStatus(b, 'CONFIRMED')">确认</button>
                <button v-if="b.status === 'CONFIRMED'" class="bk-op done" @click="setBookingStatus(b, 'DONE')">完成</button>
                <button v-if="b.status === 'PENDING' || b.status === 'CONFIRMED'" class="bk-op cancel" @click="setBookingStatus(b, 'CANCELLED')">取消</button>
              </div>
            </div>
          </div>
          <div class="modal-foot"><button class="ghost" @click="bkMgr = false">关闭</button></div>
        </div>
      </div>
    </transition>

    <!-- 用户端「我的预约」弹层（凭手机号查询 / 自助取消） -->
    <transition name="fade">
      <div v-if="myBk" class="modal-mask" @click.self="myBk = false">
        <div class="modal bk-mgr-modal">
          <h2>📋 我的预约</h2>
          <div v-if="!myBkQueried" class="my-bk-search">
            <p class="my-bk-tip">输入你预约时填写的手机号，即可查询历史与当前预约状态 ✨</p>
            <input v-model="myBkPhone" type="text" maxlength="11" placeholder="11 位手机号" class="my-bk-input" />
            <button class="primary" :disabled="myBkLoading" @click="queryMyBookings">{{ myBkLoading ? '查询中…' : '查询预约' }}</button>
          </div>
          <template v-else>
            <p v-if="myBkLoading" class="bk-empty">加载中…</p>
            <p v-else-if="!myBookings.length" class="bk-empty">还没有找到你的预约哦<br /><button class="bk-link-btn" @click="myBkQueried = false">换个手机号试试</button></p>
            <div v-else class="bk-list">
              <div v-for="b in myBookings" :key="b.id" class="bk-item">
                <div class="bk-item-main">
                  <p class="bk-item-title">{{ b.service }}<em class="bk-status" :class="'s-' + b.status">{{ statusText(b.status) }}</em></p>
                  <p class="bk-item-sub">🗓 {{ b.date }} {{ b.timeSlot }}<template v-if="b.note"> · 📝 {{ b.note }}</template></p>
                </div>
                <div class="bk-item-ops">
                  <button v-if="b.status === 'PENDING' || b.status === 'CONFIRMED'" class="bk-op cancel" :disabled="myBkCancelling === b.id" @click="cancelMyBk(b)">
                    {{ myBkCancelling === b.id ? '取消中…' : '取消预约' }}
                  </button>
                </div>
              </div>
            </div>
            <div class="modal-foot">
              <button class="ghost" @click="myBkQueried = false">换个手机号</button>
              <button class="ghost" @click="myBk = false">关闭</button>
            </div>
          </template>
        </div>
      </div>
    </transition>

    <!-- 作品大图预览弹层（分享链接 ?work=<id> 直达） -->
    <transition name="fade">
      <div v-if="preview" class="modal-mask" @click.self="preview = null">
        <div class="modal bk-preview">
          <img :src="preview.img" :alt="preview.title" />
          <div class="bk-preview-info">
            <h3>{{ preview.title }}</h3>
            <p class="bk-preview-meta">
              <span v-if="preview.price" class="bk-preview-price">￥{{ preview.price }}</span>
              <span v-for="t in preview.tags" :key="t" class="mini-tag">{{ t }}</span>
              <span class="bk-preview-likes">💖 {{ preview.likes }}</span>
            </p>
            <div class="modal-foot">
              <button class="ghost" @click="preview = null">关闭</button>
              <button class="primary" @click="bookFromPreview">预约此款 💅</button>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/modules/auth'
import { useToastStore } from '@/stores/modules/toast'
import {
  getNailProfile, updateNailProfile, getNailWorks, createNailWork, likeNailWork,
  deleteNailWork, createNailBooking, getNailBookings, updateBookingStatus,
  getMyNailBookings, cancelMyNailBooking
} from '@/api/nail'

const LS_PROFILE = 'vn_nail_profile'
const LS_WORKS = 'vn_nail_works'
const LS_LIKES = 'vn_nail_likes'

const auth = useAuthStore()
const route = useRoute()
const toast = useToastStore()

const isOwner = computed(() => {
  if (route.query.owner === '1') return true
  const u = (auth.user?.username || '').toLowerCase()
  return auth.isLoggedIn && ['nailqueen', 'admin'].includes(u)
})

const DEFAULT_PROFILE = {
  name: '小仙女美甲屋',
  sign: '指尖上的小小魔法 ✨ 每一款都值得被宠爱',
  loc: '星光市 云朵街 12 号 · 巷尾粉色小店',
  hours: '10:00 - 21:00（周一休息）',
  wx: 'nail-fairy',
  qq: '2880006666',
  tel: '138-0000-6666'
}
const profile = reactive({ ...DEFAULT_PROFILE })
const draft = reactive({ ...DEFAULT_PROFILE })
const editing = ref(false)

// 店铺介绍 / 服务价目 / 预约须知 / 交通停车
// 这部分纯前端配置，店主可在「编辑店铺信息」弹窗中调整后存 localStorage
const LS_SHOP_META = 'vn_nail_shop_meta'
const DEFAULT_SHOP_META = {
  introduction: '小仙女美甲屋是一家专注日系可爱风与轻奢法式的小工作室，由三位小姐姐主理，每位都有 5 年以上美甲师经验。我们坚持一客一消毒、一客一份耗材，所有甲油胶均来自 OPI / MSI / LeChat 等正规品牌，孕婴友好可卸除配方。店内同时提供茶水小点、漫画绘本和轻音乐，希望每一次到访都像回家一样轻松。',
  services: [
    { name: '基础单色甲油胶', price: '88 起', desc: '含修型 + 打磨 + 单色 + 顶层封胶，约 60 分钟' },
    { name: '渐变 / 晕染款', price: '128 起', desc: '双色或多色渐变 + 亮粉点缀，约 90 分钟' },
    { name: '法式 / 边染款', price: '138 起', desc: '微笑线 / 反法式 / 创意边染，约 90 分钟' },
    { name: '猫眼 / 磁吸款', price: '158 起', desc: '光疗猫眼 + 磁吸花纹，约 100 分钟' },
    { name: '手绘 / 3D 装饰款', price: '188 起', desc: '复杂图案 / 立体饰品 / 客制设计，约 120 分钟' },
    { name: '甲片延长（光疗/塑型）', price: '208 起', desc: '本甲薄弱或想加长者首选，约 150 分钟' }
  ],
  policy: [
    '预约成功后请加店主微信（nail-fairy）备注「预约 + 手机号尾四位」进行确认，未确认的预约 24 小时后将自动释放时段。',
    '如需改期或取消，请提前 4 小时通过「我的预约」入口自助操作或微信告知，避免爽约影响下一位仙女。',
    '到店请提前 5 分钟到达，迟到超过 15 分钟将自动顺延至下一空档或重新预约。',
    '孕妇 / 哺乳期可做，请提前告知，我们将使用孕婴友好配方并加强通风。',
    '甲片延长 / 复杂手绘款建议预留 2 小时以上，请合理安排时间。'
  ],
  traffic: '地铁 7 号线「云朵街站」B 口步行 3 分钟，巷尾粉色招牌即是；自驾可停在「云朵里地下停车场」，2 小时免费，店内可代领停车券。'
}
const shopMeta = reactive({ ...DEFAULT_SHOP_META })
const draftMeta = reactive({ services: [], policy: [], introduction: '', traffic: '' })

const allTags = ['日系', '清新', '卡通', '渐变', '猫眼', '法式', '光疗', '延长', '手绘']
const filterTag = ref('全部')

const PLACEHOLDER_COLORS = [['#ffd3e2,#e6d6ff', '🎀'], ['#ffe9f3,#ffcfe0', '🍓'], ['#e3f6ff,#dcd0ff', '☁️']]
function makePlaceholder(i, title) {
  const [c, e] = PLACEHOLDER_COLORS[i % PLACEHOLDER_COLORS.length]
  const svg = `<svg xmlns='http://www.w3.org/2000/svg' width='720' height='520'><defs><linearGradient id='g' x1='0' y1='0' x2='1' y2='1'><stop offset='0' stop-color='${c.split(',')[0]}'/><stop offset='1' stop-color='${c.split(',')[1]}'/></linearGradient></defs><rect width='720' height='520' fill='url(#g)'/><circle cx='360' cy='210' r='86' fill='#ffffffaa'/><text x='360' y='245' font-size='96' text-anchor='middle'>${e}</text><text x='360' y='400' font-size='40' font-weight='bold' fill='#ff7fab' text-anchor='middle' font-family='sans-serif'>${title}</text></svg>`
  return 'data:image/svg+xml,' + encodeURIComponent(svg)
}

// AI 生成美甲实拍图（统一走 trae text_to_image，无外部图床依赖）
function nailImg(prompt) {
  return `https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=${encodeURIComponent(prompt)}&image_size=square_hd`
}

const SEED_WORKS = [
  { id: 'seed1', title: '草莓奶油·短甲', tags: ['日系', '卡通'], price: 128, img: nailImg('kawaii Japanese strawberry shortcake nail art, pastel pink cream color, 3D candy decoration, short square shape, glossy top coat, professional macro studio photo'), ts: Date.now() - 86400000 * 2, likes: 23 },
  { id: 'seed2', title: '云朵蓝紫·渐变', tags: ['渐变', '清新'], price: 108, img: nailImg('dreamy cloud gradient nail art blue to purple aurora, shimmering glitter, glossy finish, almond shape, professional macro photo'), ts: Date.now() - 86400000 * 5, likes: 41 },
  { id: 'seed3', title: '碎星猫眼·延长', tags: ['猫眼', '法式'], price: 158, img: nailImg('magnetic cat eye nail art with stardust sparkle, deep purple base, extended stiletto shape, glossy finish, macro studio shot'), ts: Date.now() - 86400000 * 8, likes: 17 },
  { id: 'seed4', title: '樱花漫·手绘', tags: ['日系', '清新'], price: 138, img: nailImg('cherry blossom hand-painted nail art, light pink petals on milky white base, Japanese spring kawaii style, short round nails, macro photo'), ts: Date.now() - 86400000 * 10, likes: 35 },
  { id: 'seed5', title: '海盐焦糖·晕染', tags: ['渐变', '法式'], price: 148, img: nailImg('watercolor salted caramel blend nail art, warm brown and gold marble, glossy finish, medium length, professional macro shot'), ts: Date.now() - 86400000 * 12, likes: 28 },
  { id: 'seed6', title: '暗夜星辰·猫眼', tags: ['猫眼', '渐变'], price: 168, img: nailImg('midnight galaxy magnetic cat eye nail art, deep blue black base with star sparkle aurora, long stiletto shape, macro photography'), ts: Date.now() - 86400000 * 15, likes: 52 },
  { id: 'seed7', title: '蜜桃乌龙·法式', tags: ['法式', '日系'], price: 138, img: nailImg('peach oolong French manicure, soft pink peach gradient with milky white tip, kawaii Japanese style, medium almond shape, macro shot'), ts: Date.now() - 86400000 * 18, likes: 19 },
  { id: 'seed8', title: '玻璃碎钻·延长', tags: ['渐变', '卡通'], price: 188, img: nailImg('glass shattered diamond nail art, transparent base with crushed crystals and gold flakes, extended coffin shape, glossy finish, macro photography'), ts: Date.now() - 86400000 * 22, likes: 64 },
  { id: 'seed9', title: '薰衣草·田园', tags: ['清新', '法式'], price: 118, img: nailImg('lavender field French tip nail art, soft purple and green floral design on nude base, minimalist elegant style, short square shape, macro photo'), ts: Date.now() - 86400000 * 25, likes: 31 }
]

const works = ref([])
const likedIds = ref([])

onMounted(() => {
  try {
    Object.assign(profile, JSON.parse(localStorage.getItem(LS_PROFILE) || '{}'))
    Object.assign(draft, profile)
    const saved = JSON.parse(localStorage.getItem(LS_WORKS) || 'null')
    works.value = Array.isArray(saved) ? saved : SEED_WORKS
  } catch {
    works.value = SEED_WORKS
  }
  try { Object.assign(shopMeta, JSON.parse(localStorage.getItem(LS_SHOP_META) || '{}')) } catch {}
  try { likedIds.value = JSON.parse(localStorage.getItem(LS_LIKES) || '[]') } catch {}
  if (route.query.work) openPreview(route.query.work) // 分享链接直达作品大图
  loadCloud()
  fxTeardown = initFx()
})

// ============================================================
// 云端数据层：优先走后端接口，失败静默回退 localStorage
// ============================================================
// 云端作品字段 → 页面渲染结构（tags 逗号串转数组、createdAt 转 ts）
function normalizeWork(w) {
  return {
    id: String(w.id),
    title: w.title,
    tags: Array.isArray(w.tags) ? w.tags : String(w.tags || '').split(',').map(s => s.trim()).filter(Boolean),
    price: Number(w.price) || 0,
    img: w.img,
    likes: Number(w.likes) || 0,
    ts: parseTs(w.createdAt)
  }
}
function parseTs(v) {
  if (typeof v === 'number') return v
  const n = Date.parse(v)
  return Number.isNaN(n) ? Date.now() : n
}

// 首屏拉取云端店铺资料与作品；服务未启动时沿用本地兜底
async function loadCloud() {
  try {
    const [pr, wr] = await Promise.all([getNailProfile(), getNailWorks()])
    if (pr?.data) {
      Object.assign(profile, pr.data)
      Object.assign(draft, profile)
    }
    if (Array.isArray(wr?.data)) works.value = wr.data.map(normalizeWork)
  } catch { /* 后端未就绪，保持本地数据 */ }
  if (route.query.work) openPreview(route.query.work)
}

function persistWorks() { localStorage.setItem(LS_WORKS, JSON.stringify(works.value)) }

const shownWorks = computed(() =>
  [...works.value]
    .sort((a, b) => b.ts - a.ts)
    .filter(w => filterTag.value === '全部' || w.tags.includes(filterTag.value))
)

function openEdit() {
  Object.assign(draft, profile)
  editing.value = true
}

function like(evt, w) {
  if (likedIds.value.includes(w.id)) return
  likedIds.value.push(w.id)
  w.likes++
  localStorage.setItem(LS_LIKES, JSON.stringify(likedIds.value))
  persistWorks()
  likeNailWork(w.id).catch(() => {}) // 点赞上报：fire-and-forget，失败静默
  heartBurst(evt)
}

async function removeWork(id) {
  const backup = works.value
  works.value = works.value.filter(w => w.id !== id)
  persistWorks()
  try {
    await deleteNailWork(id)
  } catch {
    works.value = backup // 云端删除失败，恢复本地列表
    persistWorks()
  }
}

function onFiles(e) {
  const files = [...(e.target.files || [])]
  files.forEach((f, i) => {
    const reader = new FileReader()
    reader.onload = () => compress(reader.result, async url => {
      const title = f.name.replace(/\.[^.]+$/, '').slice(0, 18) || '新作品'
      try {
        // 云端优先：压缩后的 dataURL 直接入库
        const res = await createNailWork({ title, tags: '清新', price: 0, img: url })
        if (!res?.data) throw new Error('空响应')
        works.value.unshift(normalizeWork(res.data))
      } catch {
        // 回退：仅存本地
        works.value.unshift({ id: 'w' + Date.now() + i, title, tags: ['清新'], price: 0, img: url, ts: Date.now(), likes: 0 })
      }
      persistWorks()
    })
    reader.readAsDataURL(f)
  })
  e.target.value = ''
}

function compress(dataUrl, cb) {
  const img = new Image()
  img.onload = () => {
    const max = 760
    const scale = Math.min(1, max / Math.max(img.width, img.height))
    const cv = document.createElement('canvas')
    cv.width = img.width * scale
    cv.height = img.height * scale
    cv.getContext('2d').drawImage(img, 0, 0, cv.width, cv.height)
    cb(cv.toDataURL('image/jpeg', 0.82))
  }
  img.src = dataUrl
}

async function saveProfile() {
  Object.assign(profile, draft)
  localStorage.setItem(LS_PROFILE, JSON.stringify(profile))
  editing.value = false
  try {
    await updateNailProfile({ ...profile })
    toast.success('店铺信息已保存 💖')
  } catch {
    toast.warning('云端保存失败，已暂存本地')
  }
}

// ============================================================
// 在线预约（公开接口，游客可提交）
// ============================================================
const booking = ref(false)
const bkDone = ref(false)
const bkSubmitting = ref(false)
const bkForm = reactive({ name: '', phone: '', service: '', date: '', timeSlot: '', note: '' })
const bkTimeSlots = ['10:00', '13:00', '15:00', '17:00', '19:00']
const _d = new Date()
const todayStr = `${_d.getFullYear()}-${String(_d.getMonth() + 1).padStart(2, '0')}-${String(_d.getDate()).padStart(2, '0')}`
// 款式下拉 = 当前作品标题 + 自定义款式
const bkServices = computed(() => [...new Set(works.value.map(w => w.title).filter(Boolean)), '自定义款式'])

function openBooking(title = '') {
  Object.assign(bkForm, { name: '', phone: '', service: title || '', date: '', timeSlot: '', note: '' })
  bkDone.value = false
  booking.value = true
}

async function submitBooking() {
  const f = bkForm
  if (!f.name.trim()) { toast.warning('先告诉我怎么称呼你呀～'); return }
  if (!/^\d{11}$/.test(f.phone.trim())) { toast.warning('请填写 11 位手机号'); return }
  if (!f.service) { toast.warning('请选择一款心仪的款式'); return }
  if (!f.date) { toast.warning('请选择到店日期'); return }
  if (!f.timeSlot) { toast.warning('请挑一个时间段'); return }
  bkSubmitting.value = true
  try {
    await createNailBooking({ name: f.name.trim(), phone: f.phone.trim(), service: f.service, date: f.date, timeSlot: f.timeSlot, note: f.note.trim() })
    bkDone.value = true // 切换成功态（打勾动画）
  } catch { /* 失败已全局 toast */ }
  bkSubmitting.value = false
}

// ============================================================
// 店主预约管理
// ============================================================
const bkMgr = ref(false)
const bkLoading = ref(false)
const bookings = ref([])
const BK_STATUS_TEXT = { PENDING: '待确认', CONFIRMED: '已确认', DONE: '已完成', CANCELLED: '已取消' }
const statusText = s => BK_STATUS_TEXT[s] || s

async function openBookings() {
  bkMgr.value = true
  bkLoading.value = true
  try {
    const res = await getNailBookings()
    bookings.value = Array.isArray(res?.data) ? res.data : []
  } catch { /* 失败已全局 toast */ }
  bkLoading.value = false
}

async function setBookingStatus(b, status) {
  try {
    await updateBookingStatus(b.id, status)
    b.status = status // 本地更新状态徽标
    toast.success('预约状态已更新')
  } catch { /* 失败已全局 toast */ }
}

// 今日预约统计：按今日日期字符串 + 状态分组
const todayStr2 = (() => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
})()
const todayBookings = computed(() => bookings.value.filter(b => b.date === todayStr2))
const todayPending = computed(() => todayBookings.value.filter(b => b.status === 'PENDING').length)
const todayConfirmed = computed(() => todayBookings.value.filter(b => b.status === 'CONFIRMED').length)
const todayDone = computed(() => todayBookings.value.filter(b => b.status === 'DONE').length)
const todayCancelled = computed(() => todayBookings.value.filter(b => b.status === 'CANCELLED').length)

// ============================================================
// 用户端「我的预约」：凭手机号查询 / 自助取消
// ============================================================
const myBk = ref(false)
const myBkQueried = ref(false)
const myBkPhone = ref('')
const myBkLoading = ref(false)
const myBookings = ref([])
const myBkCancelling = ref(null)

function openMyBookings() {
  myBk.value = true
  myBkQueried.value = false
  myBkPhone.value = ''
  myBookings.value = []
}

async function queryMyBookings() {
  if (!/^\d{11}$/.test(myBkPhone.value.trim())) {
    toast.warning('请填写 11 位手机号')
    return
  }
  myBkLoading.value = true
  try {
    const res = await getMyNailBookings(myBkPhone.value.trim())
    myBookings.value = Array.isArray(res?.data) ? res.data : []
    myBkQueried.value = true
  } catch {
    myBookings.value = []
    myBkQueried.value = true
  }
  myBkLoading.value = false
}

async function cancelMyBk(b) {
  myBkCancelling.value = b.id
  try {
    await cancelMyNailBooking(b.id, myBkPhone.value.trim())
    b.status = 'CANCELLED'
    toast.success('预约已取消')
  } catch { /* 失败已全局 toast */ }
  myBkCancelling.value = null
}

// ============================================================
// 分享：优先 navigator.share，不支持则复制链接
// ============================================================
function shareUrl(extra = '') {
  return location.origin + location.pathname + extra
}
async function shareLink(payload, url) {
  if (navigator.share) {
    try { await navigator.share(payload); return } catch (e) { if (e?.name === 'AbortError') return }
  }
  await copy(url)
  toast.success('链接已复制，快去分享吧')
}
const shareShop = () => shareLink({ title: profile.name, text: profile.sign, url: shareUrl() }, shareUrl())
const shareWork = w => {
  const url = shareUrl('?work=' + w.id)
  shareLink({ title: w.title, text: `【${profile.name}】${w.title}，快来康康～`, url }, url)
}

// ============================================================
// 作品大图预览（分享链接 ?work=<id> 自动打开）
// ============================================================
const preview = ref(null)
function openPreview(id) {
  const w = works.value.find(w => String(w.id) === String(id))
  if (w) preview.value = w
}
function bookFromPreview() {
  const t = preview.value?.title || ''
  preview.value = null
  openBooking(t)
}

function fmtDate(ts) {
  const d = new Date(ts)
  return `${d.getMonth() + 1}/${d.getDate()}`
}

// ============================================================
// 特效层：花瓣星光粒子场 + 鼠标拖尾（借鉴 3D 体验馆粒子概念）
// ============================================================
const pageEl = ref(null)
const fxCanvas = ref(null)
let fxRaf = 0
let trailLast = { x: -99, y: -99 }

function initFx() {
  const cv = fxCanvas.value
  if (!cv) return
  const ctx = cv.getContext('2d')
  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  let W = 0, H = 0
  const COLORS = ['#ffb7d9', '#cdb4ff', '#ffd3e2', '#bde3ff', '#ffe3a1']
  const parts = []

  function resize() {
    W = cv.width = cv.offsetWidth * dpr
    H = cv.height = cv.offsetHeight * dpr
  }
  resize()
  window.addEventListener('resize', resize)

  function make(seedY) {
    return {
      x: Math.random() * W,
      y: seedY !== undefined ? seedY : Math.random() * H,
      r: (1.6 + Math.random() * 3.4) * dpr,
      vx: (Math.random() - 0.5) * 0.22 * dpr,
      vy: -(0.12 + Math.random() * 0.38) * dpr,
      c: COLORS[(Math.random() * COLORS.length) | 0],
      a: 0.22 + Math.random() * 0.5,
      tw: Math.random() * Math.PI * 2,
      star: Math.random() < 0.35,
      life: Infinity,
    }
  }
  for (let i = 0; i < 46; i++) parts.push(make())

  // 拖尾火花：短寿命、微微上飘后消散
  function spark(x, y) {
    for (let i = 0; i < 3; i++) {
      const p = make(y)
      p.x = x + (Math.random() - 0.5) * 14 * dpr
      p.y = y + (Math.random() - 0.5) * 14 * dpr
      p.r = (1 + Math.random() * 2.2) * dpr
      p.vy = -(0.4 + Math.random() * 0.8) * dpr
      p.a = 0.9
      p.life = 1
      parts.push(p)
    }
  }

  function onMove(e) {
    const rect = pageEl.value?.getBoundingClientRect()
    if (!rect) return
    const x = (e.clientX - rect.left) * dpr
    const y = (e.clientY - rect.top) * dpr
    if (Math.hypot(x - trailLast.x, y - trailLast.y) > 42 * dpr) {
      trailLast = { x, y }
      spark(x, y)
    }
  }
  pageEl.value?.addEventListener('mousemove', onMove)

  function drawStar(c, x, y, r) {
    c.beginPath()
    for (let i = 0; i < 4; i++) {
      const a1 = i * Math.PI / 2
      const a2 = a1 + Math.PI / 4
      c.moveTo(x, y)
      c.lineTo(x + Math.cos(a1) * r * 2.2, y + Math.sin(a1) * r * 2.2)
      c.moveTo(x, y)
      c.lineTo(x + Math.cos(a2) * r * 0.7, y + Math.sin(a2) * r * 0.7)
    }
    c.stroke()
  }

  function loop() {
    fxRaf = requestAnimationFrame(loop)
    ctx.clearRect(0, 0, W, H)
    for (let i = parts.length - 1; i >= 0; i--) {
      const p = parts[i]
      p.x += p.vx
      p.y += p.vy
      p.tw += 0.04
      if (p.life !== Infinity) {
        p.life -= 0.02
        if (p.life <= 0) { parts.splice(i, 1); continue }
      } else if (p.y < -12 * dpr) {
        Object.assign(p, make(H + 10 * dpr))
      }
      const alpha = p.life === Infinity ? p.a * (0.55 + 0.45 * Math.sin(p.tw)) : p.a * p.life
      ctx.globalAlpha = Math.max(alpha, 0)
      ctx.fillStyle = p.c
      ctx.strokeStyle = p.c
      if (p.star) {
        ctx.lineWidth = 1.2 * dpr
        drawStar(ctx, p.x, p.y, p.r)
      } else {
        ctx.beginPath()
        ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
        ctx.fill()
      }
    }
    ctx.globalAlpha = 1
  }
  loop()

  return () => {
    cancelAnimationFrame(fxRaf)
    window.removeEventListener('resize', resize)
    pageEl.value?.removeEventListener('mousemove', onMove)
  }
}
let fxTeardown = null

// 作品卡 3D 倾斜跟随（同首页卡片手感）
function onTilt(e) {
  const el = e.currentTarget
  const r = el.getBoundingClientRect()
  const px = (e.clientX - r.left) / r.width - 0.5
  const py = (e.clientY - r.top) / r.height - 0.5
  el.style.transform = `perspective(700px) rotateY(${px * 8}deg) rotateX(${-py * 8}deg) translateY(-5px)`
  el.style.transition = 'transform 0.08s ease-out'
}
function onTiltLeave(e) {
  const el = e.currentTarget
  el.style.transform = ''
  el.style.transition = 'transform 0.45s cubic-bezier(0.23, 1, 0.32, 1)'
}

// 点赞爱心爆裂：从按钮位置飘起一串小心心
function heartBurst(evt) {
  const r = evt.currentTarget.getBoundingClientRect()
  for (let i = 0; i < 8; i++) {
    const s = document.createElement('span')
    s.className = 'burst-heart'
    s.textContent = ['💖', '💗', '✨'][(Math.random() * 3) | 0]
    s.style.left = r.left + r.width / 2 + 'px'
    s.style.top = r.top + 4 + 'px'
    s.style.setProperty('--dx', (Math.random() * 72 - 36) + 'px')
    s.style.setProperty('--ds', (0.65 + Math.random() * 0.75).toFixed(2))
    s.style.animationDuration = (0.85 + Math.random() * 0.55).toFixed(2) + 's'
    document.body.appendChild(s)
    setTimeout(() => s.remove(), 1500)
  }
}

// 复制成功后在点击处浮出「已复制」提示
async function copy(text, evt) {
  try { await navigator.clipboard.writeText(text) } catch {
    const ta = document.createElement('textarea')
    ta.value = text
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    ta.remove()
  }
  if (!evt) return
  const tip = document.createElement('span')
  tip.className = 'copy-tip'
  tip.textContent = '已复制 ✓'
  tip.style.left = evt.clientX + 'px'
  tip.style.top = evt.clientY - 14 + 'px'
  document.body.appendChild(tip)
  setTimeout(() => tip.remove(), 1100)
}

onBeforeUnmount(() => {
  fxTeardown?.()
  fxTeardown = null
})
</script>

<style scoped>
.nail-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background: linear-gradient(180deg, #fff5fa 0%, #fdf0ff 55%, #f3efff 100%);
  padding: 32px 16px 80px;
}
.float-deco {
  position: absolute;
  font-size: 18px;
  opacity: 0.35;
  animation: floaty 7s ease-in-out infinite;
  pointer-events: none;
}
.float-deco.d1{top:6%;left:4%}.float-deco.d2{top:14%;right:8%;animation-delay:-1s}
.float-deco.d3{top:40%;left:2%;animation-delay:-2s}.float-deco.d4{top:30%;right:3%;animation-delay:-3s}
.float-deco.d5{bottom:30%;left:6%;animation-delay:-4s}.float-deco.d6{bottom:18%;right:5%;animation-delay:-5s}
.float-deco.d7{top:70%;left:10%;animation-delay:-1.5s}.float-deco.d8{top:8%;left:45%;animation-delay:-2.5s}
.float-deco.d9{bottom:8%;left:40%;animation-delay:-3.5s}.float-deco.d10{top:52%;right:12%;animation-delay:-4.5s}
.float-deco.d11{top:22%;left:25%;animation-delay:-.5s}.float-deco.d12{bottom:45%;right:22%;animation-delay:-6s}
.float-deco.d13{top:64%;right:35%;animation-delay:-2.2s}.float-deco.d14{bottom:60%;left:30%;animation-delay:-5.2s}
@keyframes floaty { 50% { transform: translateY(-14px) rotate(8deg); } }

.wrap { max-width: 1080px; margin: 0 auto; position: relative; }

.hero-card {
  position: relative;
  background: #fff;
  border: 2px solid #ffd6e8;
  border-radius: 26px;
  padding: 26px 28px;
  box-shadow: 0 14px 40px -18px rgba(255, 111, 165, 0.45);
  display: grid;
  gap: 16px;
}
.hero-left { display: flex; align-items: center; gap: 18px; }
.avatar {
  width: 74px; height: 74px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ffb7d9, #cdb4ff);
  display: grid; place-items: center;
  font-size: 34px;
  box-shadow: 0 8px 20px -8px rgba(205, 120, 255, 0.6);
  flex-shrink: 0;
}
.shop-name { font-size: 24px; font-weight: 800; color: #ff6fa5; letter-spacing: 0.02em; }
.shop-sign { margin-top: 4px; color: #b48ab0; font-size: 13.5px; }
.hero-chips { display: flex; flex-wrap: wrap; gap: 10px; }
.chip {
  border: 1.5px dashed #ffb7d9;
  background: #fff5f9;
  color: #d4699a;
  border-radius: 999px;
  padding: 6px 14px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.25s;
}
.chip small { opacity: 0.55; margin-left: 4px; font-size: 11px; }
.chip:hover { background: #ffeaf3; transform: translateY(-2px); }
.chip.plain { cursor: default; }
.contact-row { display: flex; flex-wrap: wrap; gap: 12px; }
.c-btn {
  border: none;
  border-radius: 999px;
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  cursor: pointer;
  transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.25s;
  text-decoration: none;
  display: inline-block;
}
.c-btn.wx { background: linear-gradient(135deg, #ff8fb7, #ff6fa5); box-shadow: 0 8px 18px -8px rgba(255, 111, 165, 0.8); }
.c-btn.qq { background: linear-gradient(135deg, #b89cff, #9a7bff); box-shadow: 0 8px 18px -8px rgba(154, 123, 255, 0.8); }
.c-btn.tel { background: linear-gradient(135deg, #63d8c2, #45c7ae); box-shadow: 0 8px 18px -8px rgba(69, 199, 174, 0.8); }
.c-btn:hover { transform: translateY(-3px) rotate(-1deg) scale(1.03); filter: brightness(1.05); }
.owner-btn {
  justify-self: start;
  border: 2px dashed #d8a7ff;
  color: #a86ee0;
  background: #faf4ff;
  border-radius: 14px;
  padding: 8px 18px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.25s;
}
.owner-btn:hover { background: #f3e8ff; transform: translateY(-2px); }

.owner-bar {
  margin-top: 18px;
  display: flex;
  align-items: center;
  gap: 14px;
  border: 2px dashed #ffc4dd;
  border-radius: 18px;
  padding: 12px 16px;
  background: #fffafc;
}
.upload-btn {
  background: linear-gradient(135deg, #ffa5cb, #ff7fb2);
  color: #fff;
  font-weight: 800;
  border-radius: 999px;
  padding: 9px 20px;
  cursor: pointer;
  font-size: 14px;
  box-shadow: 0 8px 16px -8px rgba(255, 127, 178, 0.9);
  transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.upload-btn:hover { transform: translateY(-2px) scale(1.04); }
.owner-tip { font-size: 12.5px; color: #cf93ae; }

.filter-row { display: flex; flex-wrap: wrap; gap: 8px; margin: 22px 0 16px; }
.tag-chip {
  border: 1.5px solid #ffd0e4;
  background: #fff;
  color: #d4699a;
  border-radius: 999px;
  padding: 5px 14px;
  font-size: 12.5px;
  cursor: pointer;
  transition: all 0.22s;
}
.tag-chip.on {
  background: linear-gradient(135deg, #ff8fb7, #b89cff);
  color: #fff;
  border-color: transparent;
  box-shadow: 0 6px 14px -6px rgba(184, 140, 255, 0.7);
  transform: translateY(-1px);
}
.tag-chip:not(.on):hover { background: #fff0f7; transform: translateY(-1px); }

.gallery {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
  gap: 18px;
}
.work-card {
  background: #fff;
  border: 2px solid #ffe0ec;
  border-radius: 22px;
  overflow: hidden;
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.3s;
}
.work-card:hover {
  transform: translateY(-6px) rotate(-0.6deg);
  box-shadow: 0 18px 34px -16px rgba(255, 143, 183, 0.55);
}
.pic-wrap { position: relative; aspect-ratio: 4 / 3; overflow: hidden; background: #fff2f8; }
.pic-wrap img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.4s; }
.work-card:hover .pic-wrap img { transform: scale(1.06); }
.price-badge {
  position: absolute;
  left: 10px; top: 10px;
  background: linear-gradient(135deg, #ffde72, #ffb84d);
  color: #7a4b00;
  font-weight: 800;
  font-size: 12px;
  border-radius: 999px;
  padding: 4px 10px;
  box-shadow: 0 4px 10px -4px rgba(255, 168, 60, 0.8);
}
.del-btn {
  position: absolute;
  right: 8px; top: 8px;
  width: 26px; height: 26px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.9);
  color: #ff5c8a;
  font-size: 16px;
  cursor: pointer;
  opacity: 0;
  transition: all 0.2s;
}
.work-card:hover .del-btn { opacity: 1; }
.del-btn:hover { background: #ff5c8a; color: #fff; }
.card-body { padding: 12px 14px 14px; }
.card-body h3 { font-size: 15px; font-weight: 700; color: #7a4b68; }
.tag-line { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 8px; }
.mini-tag {
  font-size: 10.5px;
  color: #b07ad1;
  background: #f6edff;
  border-radius: 999px;
  padding: 3px 9px;
}
.card-foot { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; }
.like-btn {
  border: none;
  background: none;
  font-size: 13px;
  color: #ff6fa5;
  cursor: pointer;
  transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.like-btn:hover { transform: scale(1.15); }
.card-foot time { font-size: 11px; color: #c9a3bb; }

.empty { text-align: center; padding: 60px 0 30px; color: #d493b4; }
.empty-face { font-size: 30px; display: block; margin-bottom: 10px; }

.book-cta {
  margin-top: 40px;
  text-align: center;
  background: linear-gradient(135deg, #ffe3ef, #efe4ff);
  border-radius: 24px;
  padding: 30px 20px;
}
.book-cta p { color: #a86e97; font-weight: 700; margin-bottom: 16px; }
.book-btn {
  border: none;
  background: linear-gradient(135deg, #ff7fb2, #b89cff);
  color: #fff;
  font-weight: 800;
  font-size: 16px;
  border-radius: 999px;
  padding: 13px 34px;
  cursor: pointer;
  box-shadow: 0 12px 26px -10px rgba(184, 140, 255, 0.9);
  transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.book-btn:hover { transform: translateY(-3px) scale(1.04); }

.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(90, 40, 80, 0.35);
  backdrop-filter: blur(4px);
  display: grid;
  place-items: center;
  z-index: 200;
  padding: 16px;
}
.modal {
  background: #fff;
  border-radius: 24px;
  border: 2px solid #ffd6e8;
  padding: 24px 26px;
  width: min(430px, 100%);
  max-height: 88vh;
  overflow-y: auto;
}
.modal h2 { color: #ff6fa5; font-size: 18px; margin-bottom: 16px; }
.modal label { display: grid; gap: 5px; margin-bottom: 12px; }
.modal label span { font-size: 12px; color: #b07aa0; font-weight: 600; }
.modal input {
  border: 1.5px solid #ffd0e4;
  border-radius: 12px;
  padding: 9px 12px;
  font-size: 13.5px;
  color: #7a4b68;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
  background: #fffbfd;
}
.modal input:focus { border-color: #ff8fb7; box-shadow: 0 0 0 3px rgba(255, 143, 183, 0.18); }
.modal-foot { display: flex; justify-content: flex-end; gap: 10px; margin-top: 6px; }
.modal-foot button { border-radius: 999px; padding: 9px 20px; font-size: 13.5px; cursor: pointer; font-weight: 700; }
.modal-foot .ghost { border: 1.5px solid #ffd0e4; background: #fff; color: #d4699a; }
.modal-foot .primary { border: none; background: linear-gradient(135deg, #ff8fb7, #b89cff); color: #fff; }

.pop-enter-active { transition: all 0.35s cubic-bezier(0.34, 1.56, 0.64, 1); transition-delay: var(--enter-delay, 0ms); }
.pop-enter-from { opacity: 0; transform: scale(0.92) translateY(10px); }
.pop-leave-active { transition: all 0.2s; }
.pop-leave-to { opacity: 0; transform: scale(0.94); }
.fade-enter-active, .fade-leave-active { transition: opacity 0.22s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* ---- 粒子画布铺底：内容之上、指针穿透 ---- */
.fx-canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}
.wrap { z-index: 1; }

/* ---- 店名流光扫过（渐变文字 + 高光横扫） ---- */
.shop-name {
  display: inline-block;
  position: relative;
  overflow: hidden;
}
.shimmer-text {
  background: linear-gradient(110deg, #ff6fa5 30%, #ffd3e8 46%, #b89cff 52%, #ff6fa5 70%);
  background-size: 240% 100%;
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: nameShine 4.2s ease-in-out infinite;
}
@keyframes nameShine {
  0% { background-position: 130% 0; }
  55%, 100% { background-position: -130% 0; }
}

@media (max-width: 640px) {
  .hero-left { flex-direction: column; text-align: center; }
  .hero-chips, .contact-row { justify-content: center; }
  .gallery { grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); }
}

/* ---- 在线预约 / 分享 / 预览（新增，bk-/share- 前缀，粉色系 #fb7185/#f9a8d4） ---- */
.share-actions { display: flex; flex-wrap: wrap; gap: 10px; justify-self: start; }
.share-btn {
  border: 1.5px solid #f9a8d4;
  background: #fff5f9;
  color: #fb7185;
  border-radius: 12px;
  padding: 8px 18px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.25s;
}
.share-btn:hover { background: #ffeef5; transform: translateY(-2px); box-shadow: 0 6px 14px -8px rgba(251, 113, 133, 0.7); }

.bk-cta-row { display: flex; flex-wrap: wrap; gap: 12px; justify-content: center; }
.bk-open-btn {
  border: 2px solid #fb7185;
  background: #fff;
  color: #fb7185;
  font-weight: 800;
  font-size: 15px;
  border-radius: 999px;
  padding: 12px 30px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.bk-open-btn:hover { background: #fb7185; color: #fff; transform: translateY(-3px) scale(1.03); box-shadow: 0 12px 26px -10px rgba(251, 113, 133, 0.8); }

.bk-card-actions { display: flex; align-items: center; gap: 6px; }
.bk-mini, .share-mini {
  border: 1.5px solid #f9a8d4;
  background: #fff5f9;
  color: #fb7185;
  border-radius: 999px;
  padding: 3px 10px;
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.22s;
}
.bk-mini:hover, .share-mini:hover { background: #fb7185; border-color: #fb7185; color: #fff; transform: translateY(-2px); }

.bk-select {
  border: 1.5px solid #ffd0e4;
  border-radius: 12px;
  padding: 9px 12px;
  font-size: 13.5px;
  color: #7a4b68;
  outline: none;
  background: #fffbfd;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.bk-select:focus { border-color: #ff8fb7; box-shadow: 0 0 0 3px rgba(255, 143, 183, 0.18); }
.bk-field { display: grid; gap: 5px; margin-bottom: 12px; }
.bk-field > span { font-size: 12px; color: #b07aa0; font-weight: 600; }
.bk-slots { display: flex; flex-wrap: wrap; gap: 8px; }
.bk-slot {
  border: 1.5px solid #f9a8d4;
  background: #fff;
  color: #fb7185;
  border-radius: 999px;
  padding: 6px 14px;
  font-size: 12.5px;
  cursor: pointer;
  transition: all 0.2s;
}
.bk-slot.on {
  background: linear-gradient(135deg, #fb7185, #f9a8d4);
  color: #fff;
  border-color: transparent;
  box-shadow: 0 6px 14px -6px rgba(251, 113, 133, 0.7);
  transform: translateY(-1px);
}
.bk-slot:not(.on):hover { background: #fff0f4; }

.bk-success { text-align: center; padding: 18px 0 6px; }
.bk-check {
  display: grid;
  place-items: center;
  width: 64px;
  height: 64px;
  margin: 0 auto 14px;
  border-radius: 50%;
  background: linear-gradient(135deg, #fb7185, #f9a8d4);
  color: #fff;
  font-size: 30px;
  font-weight: 800;
  box-shadow: 0 10px 24px -10px rgba(251, 113, 133, 0.8);
  animation: bkPop 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}
@keyframes bkPop { from { transform: scale(0); } to { transform: scale(1); } }
.bk-success p { color: #a86e97; font-weight: 700; line-height: 1.7; margin-bottom: 16px; }
.bk-close {
  border: none;
  background: linear-gradient(135deg, #ff8fb7, #b89cff);
  color: #fff;
  border-radius: 999px;
  padding: 9px 26px;
  font-size: 13.5px;
  font-weight: 700;
  cursor: pointer;
}

.bk-mgr-modal { width: min(540px, 100%); }
.bk-list { display: grid; gap: 10px; max-height: 56vh; overflow-y: auto; margin-bottom: 6px; }
.bk-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  border: 1.5px solid #ffd6e8;
  border-radius: 16px;
  padding: 10px 14px;
  background: #fffafc;
}
.bk-item-title { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; font-size: 13.5px; font-weight: 700; color: #7a4b68; }
.bk-item-sub { font-size: 12px; color: #c48fb0; margin-top: 4px; }
.bk-status { font-style: normal; font-size: 10.5px; font-weight: 700; border-radius: 999px; padding: 2px 9px; }
.bk-status.s-PENDING { background: #fff3d6; color: #b97d0e; }
.bk-status.s-CONFIRMED { background: #e5f9ee; color: #1f9d6b; }
.bk-status.s-DONE { background: #eee6ff; color: #7c5bd6; }
.bk-status.s-CANCELLED { background: #ffe4e9; color: #d3506f; }
.bk-item-ops { display: flex; gap: 6px; flex-shrink: 0; }
.bk-op { border-radius: 999px; padding: 5px 12px; font-size: 12px; font-weight: 700; cursor: pointer; transition: all 0.2s; }
.bk-op.confirm { border: 1.5px solid #f9a8d4; background: #fff; color: #fb7185; }
.bk-op.confirm:hover { background: #fb7185; color: #fff; }
.bk-op.done { border: 1.5px solid #c4b5fd; background: #fff; color: #7c5bd6; }
.bk-op.done:hover { background: #7c5bd6; color: #fff; }
.bk-op.cancel { border: 1.5px solid #fecdd3; background: #fff; color: #e11d48; }
.bk-op.cancel:hover { background: #e11d48; color: #fff; }
.bk-empty { text-align: center; color: #d493b4; padding: 26px 0 14px; font-size: 13px; }

.bk-preview { width: min(460px, 100%); padding: 14px; }
.bk-preview img { display: block; width: 100%; aspect-ratio: 4 / 3; object-fit: cover; border-radius: 16px; background: #fff2f8; }
.bk-preview-info { padding: 12px 6px 4px; }
.bk-preview-info h3 { color: #7a4b68; font-size: 16px; }
.bk-preview-meta { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin: 10px 0 14px; }
.bk-preview-price {
  background: linear-gradient(135deg, #ffde72, #ffb84d);
  color: #7a4b00;
  font-weight: 800;
  font-size: 12px;
  border-radius: 999px;
  padding: 3px 10px;
}
.bk-preview-likes { font-size: 12.5px; color: #ff6fa5; }

@media (max-width: 640px) {
  .bk-item { flex-direction: column; align-items: flex-start; }
  .card-foot { flex-wrap: wrap; gap: 6px; }
}

/* ---- 店铺介绍 / 服务 / 须知 / 交通（shop-meta 新增样式） ---- */
.shop-meta {
  margin-top: 18px;
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}
.meta-block {
  background: #fff;
  border: 2px solid #ffe0ec;
  border-radius: 18px;
  padding: 16px 18px;
  box-shadow: 0 8px 22px -14px rgba(255, 143, 183, 0.35);
}
.meta-title {
  font-size: 15px;
  font-weight: 800;
  color: #ff6fa5;
  margin-bottom: 10px;
  border-bottom: 1.5px dashed #ffd6e8;
  padding-bottom: 6px;
}
.meta-text {
  font-size: 13.5px;
  line-height: 1.75;
  color: #7a5b6e;
  white-space: pre-line;
}
.service-list { list-style: none; padding: 0; margin: 0; display: grid; gap: 10px; }
.service-item {
  border: 1.5px dashed #ffe1ee;
  border-radius: 12px;
  padding: 8px 12px;
  background: #fffafc;
  transition: all 0.22s;
}
.service-item:hover { background: #fff0f7; border-color: #ffb7d9; transform: translateX(2px); }
.svc-head { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.svc-name { font-size: 13.5px; font-weight: 700; color: #7a4b68; }
.svc-price {
  font-size: 12.5px;
  font-weight: 800;
  color: #ff6fa5;
  background: #ffeef5;
  border-radius: 999px;
  padding: 2px 10px;
}
.svc-desc { font-size: 12px; color: #b07a96; margin-top: 4px; line-height: 1.6; }
.policy-list { padding-left: 22px; margin: 0; display: grid; gap: 6px; }
.policy-list li { font-size: 12.5px; line-height: 1.65; color: #7a5b6e; }

/* ---- 「我的预约」入口按钮 ---- */
.my-bk-btn {
  border: 1.5px solid #b89cff;
  background: #fff;
  color: #7c5bd6;
  font-weight: 800;
  font-size: 15px;
  border-radius: 999px;
  padding: 12px 26px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.my-bk-btn:hover { background: #b89cff; color: #fff; transform: translateY(-3px) scale(1.03); box-shadow: 0 12px 26px -10px rgba(124, 91, 214, 0.8); }

/* ---- 预约须知折叠块 ---- */
.bk-policy {
  background: #fff5f9;
  border: 1.5px dashed #ffc4dd;
  border-radius: 12px;
  padding: 10px 14px;
  margin-bottom: 14px;
}
.bk-policy summary {
  cursor: pointer;
  font-size: 12.5px;
  font-weight: 700;
  color: #d4699a;
  user-select: none;
  list-style: none;
}
.bk-policy summary::-webkit-details-marker { display: none; }
.bk-policy[open] summary { margin-bottom: 8px; }
.bk-policy-list { padding-left: 20px; margin: 0; display: grid; gap: 4px; }
.bk-policy-list li { font-size: 11.5px; line-height: 1.6; color: #b48ab0; }

/* ---- 预约成功后的提示行 ---- */
.bk-success-tip {
  font-size: 12px;
  color: #b48ab0;
  line-height: 1.7;
  margin: 8px 0 16px;
  background: #fff5f9;
  border-radius: 12px;
  padding: 8px 12px;
}
.bk-success-tip b { color: #ff6fa5; font-weight: 800; }
.bk-success-actions { display: flex; gap: 10px; justify-content: center; flex-wrap: wrap; }
.bk-copy-btn {
  border: 1.5px solid #b89cff;
  background: #fff;
  color: #7c5bd6;
  font-weight: 700;
  border-radius: 999px;
  padding: 9px 20px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.22s;
}
.bk-copy-btn:hover { background: #b89cff; color: #fff; transform: translateY(-2px); }

/* ---- 店主端统计 chips ---- */
.mgr-count {
  font-size: 12px;
  color: #b48ab0;
  font-weight: 600;
  margin-left: 8px;
  background: #fff5f9;
  padding: 2px 10px;
  border-radius: 999px;
}
.mgr-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(110px, 1fr));
  gap: 8px;
  margin-bottom: 14px;
}
.stat-chip {
  border: 1.5px dashed #ffc4dd;
  background: #fffafc;
  border-radius: 12px;
  padding: 8px 10px;
  text-align: center;
}
.stat-chip span { display: block; font-size: 11px; color: #b48ab0; margin-bottom: 4px; }
.stat-chip b { font-size: 18px; font-weight: 800; color: #ff6fa5; }

/* ---- 「我的预约」弹层 ---- */
.my-bk-search { display: grid; gap: 10px; padding: 6px 0; }
.my-bk-tip { font-size: 12.5px; color: #b48ab0; line-height: 1.6; }
.my-bk-input {
  border: 1.5px solid #ffd0e4;
  border-radius: 12px;
  padding: 10px 14px;
  font-size: 14px;
  color: #7a4b68;
  outline: none;
  background: #fffbfd;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.my-bk-input:focus { border-color: #ff8fb7; box-shadow: 0 0 0 3px rgba(255, 143, 183, 0.18); }
.bk-link-btn {
  border: none;
  background: none;
  color: #ff6fa5;
  font-size: 13px;
  cursor: pointer;
  text-decoration: underline;
  margin-top: 6px;
}

/* ---- owner-bar 内置徽标 ---- */
.badge {
  display: inline-block;
  margin-left: 4px;
  background: #ff5c8a;
  color: #fff;
  font-size: 11px;
  border-radius: 999px;
  padding: 1px 8px;
  font-weight: 700;
}
</style>

<!-- 非 scoped：JS 动态插入 body 的元素无法命中 scoped 选择器 -->
<style>
.burst-heart {
  position: fixed;
  z-index: 300;
  pointer-events: none;
  font-size: calc(13px * var(--ds, 1));
  transform: translate(-50%, -50%);
  animation: heartRise 1s ease-out forwards;
  filter: drop-shadow(0 2px 6px rgba(255, 111, 165, 0.5));
}
@keyframes heartRise {
  0% { opacity: 0; transform: translate(-50%, -50%) scale(0.5); }
  15% { opacity: 1; transform: translate(calc(-50% + 2px), -70%) scale(1.15); }
  100% { opacity: 0; transform: translate(calc(-50% + var(--dx, 0px)), calc(-190% - 26px)) scale(0.75) rotate(var(--dr, 12deg)); }
}

.copy-tip {
  position: fixed;
  z-index: 300;
  pointer-events: none;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #ff8fb7, #b89cff);
  padding: 4px 12px;
  border-radius: 999px;
  transform: translate(-50%, -100%);
  box-shadow: 0 6px 16px -6px rgba(184, 140, 255, 0.8);
  animation: tipFloat 1s ease-out forwards;
}
@keyframes tipFloat {
  0% { opacity: 0; margin-top: 6px; }
  18% { opacity: 1; margin-top: 0; }
  80% { opacity: 1; }
  100% { opacity: 0; margin-top: -14px; }
}

@media (prefers-reduced-motion: reduce) {
  .shimmer-text { animation: none; }
  .float-deco { animation: none; }
}
</style>
