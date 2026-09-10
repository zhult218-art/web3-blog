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
        <button v-if="isOwner" class="owner-btn" @click="openEdit">🪄 编辑店铺信息</button>
      </section>

      <!-- 店主工具条 -->
      <section v-if="isOwner" class="owner-bar">
        <label class="upload-btn">
          ＋ 上传新作品
          <input type="file" accept="image/*" multiple hidden @change="onFiles" />
        </label>
        <p class="owner-tip">上传后立即展示在小铺里，访客可以看到并联系你～</p>
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
        <button class="book-btn" @click="copy(profile.wx)">💅 微信预约：{{ profile.wx }}</button>
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
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/modules/auth'

const LS_PROFILE = 'vn_nail_profile'
const LS_WORKS = 'vn_nail_works'
const LS_LIKES = 'vn_nail_likes'

const auth = useAuthStore()
const route = useRoute()

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

const allTags = ['日系', '清新', '卡通', '渐变', '猫眼', '法式']
const filterTag = ref('全部')

const PLACEHOLDER_COLORS = [['#ffd3e2,#e6d6ff', '🎀'], ['#ffe9f3,#ffcfe0', '🍓'], ['#e3f6ff,#dcd0ff', '☁️']]
function makePlaceholder(i, title) {
  const [c, e] = PLACEHOLDER_COLORS[i % PLACEHOLDER_COLORS.length]
  const svg = `<svg xmlns='http://www.w3.org/2000/svg' width='720' height='520'><defs><linearGradient id='g' x1='0' y1='0' x2='1' y2='1'><stop offset='0' stop-color='${c.split(',')[0]}'/><stop offset='1' stop-color='${c.split(',')[1]}'/></linearGradient></defs><rect width='720' height='520' fill='url(#g)'/><circle cx='360' cy='210' r='86' fill='#ffffffaa'/><text x='360' y='245' font-size='96' text-anchor='middle'>${e}</text><text x='360' y='400' font-size='40' font-weight='bold' fill='#ff7fab' text-anchor='middle' font-family='sans-serif'>${title}</text></svg>`
  return 'data:image/svg+xml,' + encodeURIComponent(svg)
}

const SEED_WORKS = [
  { id: 'seed1', title: '草莓奶油·短甲', tags: ['日系', '卡通'], price: 128, img: makePlaceholder(0, '草莓奶油'), ts: Date.now() - 86400000 * 2, likes: 23 },
  { id: 'seed2', title: '云朵蓝紫·渐变', tags: ['渐变', '清新'], price: 108, img: makePlaceholder(1, '云朵蓝紫'), ts: Date.now() - 86400000 * 5, likes: 41 },
  { id: 'seed3', title: '碎星猫眼·延长', tags: ['猫眼', '法式'], price: 158, img: makePlaceholder(2, '碎星猫眼'), ts: Date.now() - 86400000 * 8, likes: 17 }
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
  try { likedIds.value = JSON.parse(localStorage.getItem(LS_LIKES) || '[]') } catch {}
  fxTeardown = initFx()
})

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
  heartBurst(evt)
}

function removeWork(id) {
  works.value = works.value.filter(w => w.id !== id)
  persistWorks()
}

function onFiles(e) {
  const files = [...(e.target.files || [])]
  files.forEach((f, i) => {
    const reader = new FileReader()
    reader.onload = () => compress(reader.result, url => {
      works.value.unshift({
        id: 'w' + Date.now() + i,
        title: f.name.replace(/\.[^.]+$/, '').slice(0, 18) || '新作品',
        tags: ['清新'],
        price: 0,
        img: url,
        ts: Date.now(),
        likes: 0
      })
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

function saveProfile() {
  Object.assign(profile, draft)
  localStorage.setItem(LS_PROFILE, JSON.stringify(profile))
  editing.value = false
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
