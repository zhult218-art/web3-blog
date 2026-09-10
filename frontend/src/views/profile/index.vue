<template>
  <div class="profile-bg min-h-screen relative overflow-hidden">
    <!-- 背景粒子 -->
    <canvas ref="bgCanvas" class="absolute inset-0 h-full w-full opacity-40 pointer-events-none"></canvas>
    <!-- 背景光晕 -->
    <div class="absolute top-0 left-1/4 w-[500px] h-[500px] rounded-full bg-purple-500/[0.04] blur-[120px] pointer-events-none"></div>
    <div class="absolute bottom-0 right-1/4 w-[400px] h-[400px] rounded-full bg-amber-400/[0.04] blur-[100px] pointer-events-none"></div>

    <div class="relative z-10 mx-auto max-w-4xl px-4 md:px-6 py-10">

      <!-- 返回首页 -->
      <div class="mb-5"><PageBack label="返回首页" to="/" /></div>

      <!-- ═══════ 未登录 ═══════ -->
      <div v-if="!auth.isLoggedIn || !auth.user" ref="notLoginBox" class="text-center py-32 opacity-0">
        <div class="text-6xl mb-6">⚔️</div>
        <p class="text-lg text-slate-300 mb-2">冒险者尚未登录</p>
        <p class="text-sm text-slate-500 mb-8">请先登录以开启你的异世界之旅</p>
        <router-link to="/login" class="isekai-btn-primary">前往登录</router-link>
      </div>

      <!-- ═══════ 已登录 ═══════ -->
      <template v-if="auth.isLoggedIn && auth.user">

        <!-- ─── 角色卡片 ─── -->
        <div ref="heroCard" class="isekai-card mb-8 opacity-0">
          <div class="absolute inset-0 rounded-2xl overflow-hidden pointer-events-none">
            <div class="absolute inset-0 bg-gradient-to-br from-amber-500/[0.03] via-transparent to-purple-500/[0.03]"></div>
          </div>
          <div class="relative p-6 md:p-8 flex flex-col md:flex-row items-center gap-6">
            <!-- 头像 -->
            <div ref="avatarWrap" class="relative group cursor-pointer shrink-0" @click="showAvatarModal = true">
              <div class="avatar-ring absolute -inset-1 rounded-full opacity-60 group-hover:opacity-100 transition-opacity"></div>
              <div class="relative w-24 h-24 rounded-full overflow-hidden border-2 border-amber-400/30 bg-gradient-to-br from-amber-900/40 to-purple-900/40">
                <img v-if="auth.user.avatar" :src="auth.user.avatar" class="w-full h-full object-cover" />
                <div v-else class="w-full h-full flex items-center justify-center text-3xl font-bold text-amber-200">
                  {{ (auth.user.nickname || auth.user.username || '?')[0]?.toUpperCase() }}
                </div>
              </div>
              <div class="absolute inset-0 rounded-full bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                <svg class="w-6 h-6 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0115.75 21H5.25A2.25 2.25 0 013 18.75V8.25A2.25 2.25 0 015.25 6H10" /></svg>
              </div>
              <div class="absolute -bottom-1 -right-1 w-8 h-8 rounded-full bg-gradient-to-br from-amber-400 to-orange-500 flex items-center justify-center text-xs font-black text-black shadow-lg shadow-amber-500/30">
                {{ auth.user.role === 'ADMIN' ? 'A' : 'U' }}
              </div>
            </div>
            <!-- 信息 -->
            <div class="flex-1 text-center md:text-left min-w-0">
              <div class="flex items-center gap-3 justify-center md:justify-start mb-1">
                <h1 ref="heroName" class="text-2xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-amber-200 via-amber-100 to-yellow-200 truncate">
                  {{ auth.user.nickname || auth.user.username }}
                </h1>
                <span v-if="auth.user.role === 'ADMIN'" class="px-2 py-0.5 rounded-full bg-amber-400/10 border border-amber-400/20 text-[10px] font-bold text-amber-300 uppercase tracking-wider shrink-0">Admin</span>
              </div>
              <p class="text-sm text-slate-500 mb-2">@{{ auth.user.username }}</p>
              <div class="flex flex-wrap gap-3 mt-3 justify-center md:justify-start">
                <div class="stat-badge">
                  <svg class="w-3.5 h-3.5 text-amber-400" fill="currentColor" viewBox="0 0 20 20"><path d="M9.653 16.915l-.005-.003-.019-.01a20.759 20.759 0 01-1.162-.682 22.045 22.045 0 01-2.582-1.9C4.045 12.733 2 10.352 2 7.5a4.5 4.5 0 018-2.828A4.5 4.5 0 0118 7.5c0 2.852-2.044 5.233-3.885 6.82a22.049 22.049 0 01-3.744 2.582l-.019.01-.005.003h-.002a.723.723 0 01-.682 0h-.002z" /></svg>
                  <span>{{ auth.user.email ? '已绑定邮箱' : '邮箱未绑定' }}</span>
                </div>
                <div class="stat-badge">
                  <svg class="w-3.5 h-3.5 text-cyan-400" fill="currentColor" viewBox="0 0 20 20"><path d="M2 3a1 1 0 011-1h2.153a1 1 0 01.986.836l.74 4.435a1 1 0 01-.54 1.06l-1.548.773a11.037 11.037 0 006.105 6.105l.774-1.548a1 1 0 011.059-.54l4.435.74a1 1 0 01.836.986V17a1 1 0 01-1 1h-2C7.82 18 2 12.18 2 5V3z" /></svg>
                  <span>{{ auth.user.phone ? '已绑定手机' : '手机未绑定' }}</span>
                </div>
                <div class="stat-badge" v-if="currentClassInfo">
                  <span>{{ currentClassInfo.icon }}</span>
                  <span>{{ currentClassInfo.label }}</span>
                </div>
                <div class="stat-badge" v-if="guildStatus && guildStatus.status === 'APPROVED'">
                  <span>🛡️</span>
                  <span>{{ guildStatus.guildName }}</span>
                </div>
              </div>
            </div>
            <button class="isekai-btn-primary text-sm shrink-0" @click="startEdit">编辑资料</button>
          </div>
        </div>

        <!-- ─── 编辑资料 ─── -->
        <div v-if="editing" ref="editCard" class="isekai-card mb-8 opacity-0">
          <h3 class="section-title mb-5">编辑角色档案</h3>
          <div class="space-y-4">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div class="space-y-1.5">
                <label class="isekai-label">昵称</label>
                <input v-model="form.nickname" class="isekai-input" placeholder="你的冒险者名号" />
              </div>
              <div class="space-y-1.5">
                <label class="isekai-label">个性签名</label>
                <input v-model="form.bio" class="isekai-input" placeholder="勇者的宣言..." />
              </div>
            </div>
            <div class="flex justify-end gap-2">
              <button class="text-xs text-slate-500 hover:text-slate-300 px-4 py-2 transition" @click="editing = false">取消</button>
              <button class="isekai-btn-primary text-xs" :disabled="saving" @click="saveProfile">{{ saving ? '保存中...' : '保存档案' }}</button>
            </div>
          </div>
        </div>

        <!-- ══════ 我的内容（点赞/收藏） ══════ -->
        <div ref="contentSection" class="mb-8 opacity-0">
          <h2 class="section-title mb-4">我的内容</h2>
          <div class="isekai-card p-5">
            <div class="flex gap-2 mb-5">
              <button @click="contentTab = 'likes'"
                :class="['px-4 py-2 rounded-xl text-xs font-medium transition-all border',
                  contentTab === 'likes' ? 'bg-amber-400/10 text-amber-300 border-amber-400/20' : 'border-white/[0.06] text-slate-500 hover:text-slate-300']">
                点赞的文章 ({{ likedArticles.length }})
              </button>
              <button @click="contentTab = 'favorites'"
                :class="['px-4 py-2 rounded-xl text-xs font-medium transition-all border',
                  contentTab === 'favorites' ? 'bg-amber-400/10 text-amber-300 border-amber-400/20' : 'border-white/[0.06] text-slate-500 hover:text-slate-300']">
                收藏的文章 ({{ favoritedArticles.length }})
              </button>
            </div>
            <div v-if="contentTab === 'likes'">
              <div v-if="likedArticles.length === 0" class="text-center py-8 text-xs text-slate-600">还没有点赞过文章</div>
              <div v-else class="space-y-3">
                <router-link v-for="article in likedArticles" :key="article.id" :to="`/blog/post/${article.id}`"
                  class="flex items-center gap-4 p-3 rounded-xl border border-white/[0.04] bg-[#0c0c22] hover:border-amber-400/15 hover:bg-[#12122e] transition-all group cursor-pointer">
                  <div class="w-12 h-12 rounded-lg overflow-hidden bg-gradient-to-br from-pink-500/20 to-purple-500/20 flex items-center justify-center text-lg shrink-0">
                    <img v-if="article.cover" :src="article.cover" class="w-full h-full object-cover" />
                    <span v-else>❤️</span>
                  </div>
                  <div class="flex-1 min-w-0">
                    <p class="text-sm font-medium text-slate-200 truncate group-hover:text-amber-200 transition">{{ article.title }}</p>
                    <div class="flex items-center gap-2 mt-1">
                      <span v-if="article.category" class="text-[10px] px-1.5 py-0.5 rounded bg-purple-500/10 text-purple-300">{{ article.category }}</span>
                      <span class="text-[10px] text-slate-600">{{ article.createdAt ? new Date(article.createdAt).toLocaleDateString() : '' }}</span>
                    </div>
                  </div>
                  <svg class="w-4 h-4 text-slate-600 group-hover:text-amber-400 transition shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 5l7 7-7 7"/></svg>
                </router-link>
              </div>
            </div>
            <div v-else>
              <div v-if="favoritedArticles.length === 0" class="text-center py-8 text-xs text-slate-600">还没有收藏过文章</div>
              <div v-else class="space-y-3">
                <router-link v-for="article in favoritedArticles" :key="article.id" :to="`/blog/post/${article.id}`"
                  class="flex items-center gap-4 p-3 rounded-xl border border-white/[0.04] bg-[#0c0c22] hover:border-amber-400/15 hover:bg-[#12122e] transition-all group cursor-pointer">
                  <div class="w-12 h-12 rounded-lg overflow-hidden bg-gradient-to-br from-cyan-500/20 to-purple-500/20 flex items-center justify-center text-lg shrink-0">
                    <img v-if="article.cover" :src="article.cover" class="w-full h-full object-cover" />
                    <span v-else>⭐</span>
                  </div>
                  <div class="flex-1 min-w-0">
                    <p class="text-sm font-medium text-slate-200 truncate group-hover:text-amber-200 transition">{{ article.title }}</p>
                    <div class="flex items-center gap-2 mt-1">
                      <span v-if="article.category" class="text-[10px] px-1.5 py-0.5 rounded bg-cyan-500/10 text-cyan-300">{{ article.category }}</span>
                      <span class="text-[10px] text-slate-600">{{ article.createdAt ? new Date(article.createdAt).toLocaleDateString() : '' }}</span>
                    </div>
                  </div>
                  <svg class="w-4 h-4 text-slate-600 group-hover:text-amber-400 transition shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 5l7 7-7 7"/></svg>
                </router-link>
              </div>
            </div>
          </div>
        </div>

        <!-- ══════ 职业选择 ══════ -->
        <div ref="classSection" class="mb-8 opacity-0">
          <h2 class="section-title mb-4">选择你的职业</h2>
          <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
            <div v-for="(cls, i) in classes" :key="cls.key"
              :ref="el => classCards[i] = el"
              class="class-card group cursor-pointer"
              :class="{ 'class-card--active': auth.user.userClass === cls.key }"
              @click="selectClass(cls.key)">
              <div class="text-3xl mb-2 group-hover:scale-110 transition-transform">{{ cls.icon }}</div>
              <p class="text-sm font-semibold text-slate-200">{{ cls.label }}</p>
              <p class="text-[10px] text-slate-600 mt-0.5">{{ cls.desc }}</p>
              <div v-if="auth.user.userClass === cls.key" class="absolute top-2 right-2 w-5 h-5 rounded-full bg-amber-400/20 flex items-center justify-center">
                <svg class="w-3 h-3 text-amber-300" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" /></svg>
              </div>
            </div>
          </div>
        </div>

        <!-- ══════ 公会会员卡 ══════ -->
        <div ref="guildCardSection" class="mb-8 opacity-0">
          <h2 class="section-title mb-4">冒险者卡片</h2>

          <!-- 无公会状态：显示申请/创建入口 -->
          <div v-if="!guildStatus" class="isekai-card p-6 text-center">
            <div class="text-4xl mb-4">🏴</div>
            <p class="text-slate-300 font-medium mb-1">尚未加入任何公会</p>
            <p class="text-xs text-slate-600 mb-5">加入公会后即可获得冒险者卡片</p>
            <div class="flex flex-col sm:flex-row gap-3 justify-center">
              <button class="isekai-btn-primary text-xs" @click="showApplyModal = true">申请加入公会</button>
              <button class="isekai-btn-secondary text-xs" @click="showCreateGuildModal = true">创建公会</button>
            </div>
          </div>

          <!-- 审核中 -->
          <div v-else-if="guildStatus.status === 'PENDING'" class="isekai-card p-6 text-center border-amber-400/20">
            <div class="text-4xl mb-4 animate-pulse">⏳</div>
            <p class="text-amber-200 font-medium mb-1">公会入会申请审核中</p>
            <p class="text-xs text-slate-600 mb-3">请等待会长审批，审批通过后将获得冒险者卡片</p>
            <p class="text-[11px] text-slate-500">公会：{{ guildStatus.guildName || '未知公会' }}</p>
          </div>

          <!-- 已通过：显示会员卡 -->
          <div v-else-if="guildStatus.status === 'APPROVED'" class="guild-card-wrapper">
            <div class="guild-card" ref="guildCard">
              <!-- 卡片顶部装饰线 -->
              <div class="absolute top-0 left-0 right-0 h-1 rounded-t-2xl bg-gradient-to-r from-amber-400 via-orange-400 to-purple-500"></div>
              <!-- 卡片内容 -->
              <div class="relative p-6">
                <!-- 顶部：公会名 + 卡号 -->
                <div class="flex items-start justify-between mb-4">
                  <div>
                    <p class="text-[10px] uppercase tracking-widest text-amber-300/60 font-bold">Adventurer Card</p>
                    <h3 class="text-lg font-bold text-transparent bg-clip-text bg-gradient-to-r from-amber-200 to-yellow-100">
                      {{ guildStatus.guildName }}
                    </h3>
                  </div>
                  <div class="text-right">
                    <p class="text-[10px] text-slate-500">卡号</p>
                    <p class="text-xs font-mono text-amber-300/80">{{ guildStatus.cardNumber || '—' }}</p>
                  </div>
                </div>

                <!-- 中部：头像 + 信息 -->
                <div class="flex items-center gap-4 mb-5">
                  <div class="relative shrink-0">
                    <div class="w-16 h-16 rounded-xl overflow-hidden border border-amber-400/20 bg-gradient-to-br from-amber-900/30 to-purple-900/30">
                      <img v-if="auth.user.avatar" :src="auth.user.avatar" class="w-full h-full object-cover" />
                      <div v-else class="w-full h-full flex items-center justify-center text-xl font-bold text-amber-200">
                        {{ (auth.user.nickname || auth.user.username || '?')[0]?.toUpperCase() }}
                      </div>
                    </div>
                    <div v-if="currentClassInfo" class="absolute -bottom-1 -right-1 text-lg">{{ currentClassInfo.icon }}</div>
                  </div>
                  <div class="flex-1 min-w-0">
                    <p class="text-base font-bold text-white truncate">{{ auth.user.nickname || auth.user.username }}</p>
                    <p class="text-xs text-slate-500 mt-0.5">{{ currentClassInfo ? currentClassInfo.label : '未选择职业' }}</p>
                    <div class="flex items-center gap-1.5 mt-1.5">
                      <span class="guild-card-status">APPROVED</span>
                      <span class="text-[10px] text-slate-600">· {{ guildStatus.approvedAt ? new Date(guildStatus.approvedAt).toLocaleDateString() : '—' }}</span>
                    </div>
                  </div>
                </div>

                <!-- 底部：公会描述 -->
                <div class="border-t border-white/[0.06] pt-3">
                  <p class="text-[11px] text-slate-500 leading-relaxed">{{ guildStatus.guildDescription || '异世界冒险家公会 — 穿越时空的冒险者们聚集之地' }}</p>
                </div>
              </div>
              <!-- 光效 -->
              <div class="absolute inset-0 rounded-2xl bg-gradient-to-br from-amber-400/[0.02] to-purple-400/[0.02] pointer-events-none"></div>
            </div>
          </div>
        </div>

        <!-- ══════ 公会管理（仅会长可见） ══════ -->
        <div v-if="isGuildMaster" ref="adminSection" class="mb-8 opacity-0">
          <h2 class="section-title mb-4">公会管理</h2>
          <!-- 待审核列表 -->
          <div class="isekai-card p-5">
            <h3 class="text-sm font-bold text-amber-200/80 mb-4 flex items-center gap-2">
              <span class="w-2 h-2 rounded-full bg-amber-400 animate-pulse"></span>
              待审核申请（{{ pendingList.length }}）
            </h3>
            <div v-if="pendingList.length === 0" class="text-center py-6 text-xs text-slate-600">暂无待审核申请</div>
            <div v-else class="space-y-3">
              <div v-for="item in pendingList" :key="item.id"
                class="flex items-center gap-4 p-3 rounded-xl border border-white/[0.04] bg-[#0c0c22]">
                <div class="w-10 h-10 rounded-lg overflow-hidden bg-gradient-to-br from-amber-900/30 to-purple-900/30 flex items-center justify-center text-sm font-bold text-amber-200 shrink-0">
                  <img v-if="item.avatar" :src="item.avatar" class="w-full h-full object-cover" />
                  <span v-else>{{ (item.nickname || '?')[0]?.toUpperCase() }}</span>
                </div>
                <div class="flex-1 min-w-0">
                  <p class="text-sm font-medium text-slate-200 truncate">{{ item.nickname || item.username }}</p>
                  <p class="text-[11px] text-slate-500">{{ classLabel(item.userClass) }} · {{ item.message || '无留言' }}</p>
                </div>
                <div class="flex gap-2 shrink-0">
                  <button class="guild-btn-approve" @click="handleApprove(item.id, true)">通过</button>
                  <button class="guild-btn-reject" @click="handleApprove(item.id, false)">拒绝</button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- ══════ 冒险任务 ══════ -->
        <div ref="actionSection" class="mb-8 opacity-0">
          <h2 class="section-title mb-4">冒险任务</h2>
          <div class="grid grid-cols-2 md:grid-cols-4 gap-3">
            <router-link v-for="(act, i) in actions" :key="act.to" :to="act.to"
              :ref="el => actionCards[i] = el"
              class="isekai-card text-center hover:-translate-y-1 transition-all duration-300 cursor-pointer group">
              <div class="text-3xl mb-3 group-hover:scale-110 transition-transform">{{ act.icon }}</div>
              <p class="text-sm text-slate-300 font-medium">{{ act.label }}</p>
              <p class="text-[11px] text-slate-600 mt-1">{{ act.desc }}</p>
            </router-link>
            <div class="isekai-card text-center hover:-translate-y-1 transition-all duration-300 cursor-pointer group hover:border-red-400/20"
              @click="handleLogout">
              <div class="text-3xl mb-3 group-hover:scale-110 transition-transform">🚪</div>
              <p class="text-sm text-slate-300 font-medium">退出登录</p>
              <p class="text-[11px] text-slate-600 mt-1">结束冒险</p>
            </div>
          </div>
        </div>

      </template>
    </div>

    <!-- ═══════ 头像弹窗 ═══════ -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="showAvatarModal" class="fixed inset-0 z-50 flex items-center justify-center p-4" @click.self="showAvatarModal = false">
          <div class="absolute inset-0 bg-black/60 backdrop-blur-sm"></div>
          <div class="isekai-card relative z-10 w-full max-w-sm opacity-0" ref="avatarModalCard">
            <h3 class="text-base font-bold text-amber-200 mb-5">更换头像</h3>
            <div class="space-y-4">
              <div class="space-y-1.5">
                <label class="isekai-label">头像 URL</label>
                <input v-model="avatarUrl" class="isekai-input" placeholder="https://example.com/avatar.jpg" />
              </div>
              <div v-if="avatarUrl" class="flex justify-center">
                <div class="w-20 h-20 rounded-full overflow-hidden border-2 border-amber-400/30">
                  <img :src="avatarUrl" class="w-full h-full object-cover" />
                </div>
              </div>
              <div class="text-[11px] text-slate-600 text-center">推荐使用 <a href="https://dicebear.com" target="_blank" class="text-cyan-400 hover:underline">DiceBear</a> 生成头像</div>
            </div>
            <div class="flex justify-end gap-2 mt-5">
              <button class="text-xs text-slate-500 hover:text-slate-300 px-4 py-2 transition" @click="showAvatarModal = false">取消</button>
              <button class="isekai-btn-primary text-xs" :disabled="avatarSaving" @click="saveAvatar">{{ avatarSaving ? '保存中...' : '确认更换' }}</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ═══════ 申请入会弹窗 ═══════ -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="showApplyModal" class="fixed inset-0 z-50 flex items-center justify-center p-4" @click.self="showApplyModal = false">
          <div class="absolute inset-0 bg-black/60 backdrop-blur-sm"></div>
          <div class="isekai-card relative z-10 w-full max-w-sm opacity-0" ref="applyModalCard">
            <h3 class="text-base font-bold text-amber-200 mb-5">申请加入公会</h3>
            <div class="space-y-4">
              <div class="space-y-1.5">
                <label class="isekai-label">选择公会</label>
                <select v-model="applyForm.guildId" class="isekai-input">
                  <option value="" disabled>请选择公会</option>
                  <option v-for="g in guildList" :key="g.id" :value="g.id">{{ g.name }}（{{ g.memberCount }}/{{ g.maxMembers }}）</option>
                </select>
              </div>
              <div class="space-y-1.5">
                <label class="isekai-label">申请留言</label>
                <textarea v-model="applyForm.message" class="isekai-input h-20 resize-none" placeholder="告诉会长你为什么想加入..."></textarea>
              </div>
            </div>
            <div class="flex justify-end gap-2 mt-5">
              <button class="text-xs text-slate-500 hover:text-slate-300 px-4 py-2 transition" @click="showApplyModal = false">取消</button>
              <button class="isekai-btn-primary text-xs" :disabled="applyForm.sending" @click="submitApply">
                {{ applyForm.sending ? '提交中...' : '提交申请' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ═══════ 创建公会弹窗 ═══════ -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="showCreateGuildModal" class="fixed inset-0 z-50 flex items-center justify-center p-4" @click.self="showCreateGuildModal = false">
          <div class="absolute inset-0 bg-black/60 backdrop-blur-sm"></div>
          <div class="isekai-card relative z-10 w-full max-w-sm opacity-0" ref="createGuildModalCard">
            <h3 class="text-base font-bold text-amber-200 mb-5">创建公会</h3>
            <div class="space-y-4">
              <div class="space-y-1.5">
                <label class="isekai-label">公会名称</label>
                <input v-model="createForm.name" class="isekai-input" placeholder="为你的公会取个名字" />
              </div>
              <div class="space-y-1.5">
                <label class="isekai-label">公会描述</label>
                <textarea v-model="createForm.description" class="isekai-input h-20 resize-none" placeholder="描述你的公会宗旨..."></textarea>
              </div>
            </div>
            <div class="flex justify-end gap-2 mt-5">
              <button class="text-xs text-slate-500 hover:text-slate-300 px-4 py-2 transition" @click="showCreateGuildModal = false">取消</button>
              <button class="isekai-btn-primary text-xs" :disabled="createForm.sending" @click="submitCreateGuild">
                {{ createForm.sending ? '创建中...' : '创建公会' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/modules/auth'
import { useToastStore } from '@/stores/modules/toast'
import { updateProfile, updateAvatar, chooseClass } from '@/api/user'
import { getUserLikes, getUserFavorites } from '@/api/blog'
import { listGuilds, getMyGuild, applyGuild, createGuild, getPendingMembers, approveMember } from '@/api/guild'
import { animate, stagger } from 'animejs'
import PageBack from '@/components/PageBack.vue'

const router = useRouter()
const auth = useAuthStore()
const toast = useToastStore()

// ═══ Refs ═══
const bgCanvas = ref(null)
const notLoginBox = ref(null)
const heroCard = ref(null)
const heroName = ref(null)
const avatarWrap = ref(null)
const editCard = ref(null)
const classSection = ref(null)
const classCards = ref([])
const guildCardSection = ref(null)
const guildCard = ref(null)
const adminSection = ref(null)
const actionSection = ref(null)
const actionCards = ref([])
const avatarModalCard = ref(null)
const applyModalCard = ref(null)
const createGuildModalCard = ref(null)

// ═══ 我的内容 ═══
const contentTab = ref('likes')
const likedArticles = ref([])
const favoritedArticles = ref([])
const contentSection = ref(null)

// ═══ 职业数据 ═══
const classes = [
  { key: 'WARRIOR',    icon: '⚔️',  label: '战士',   desc: '近战物理输出' },
  { key: 'MAGE',       icon: '🔮',  label: '法师',   desc: '远程魔法输出' },
  { key: 'HEALER',     icon: '💚',  label: '牧师',   desc: '团队治疗辅助' },
  { key: 'ASSASSIN',   icon: '🗡️',  label: '刺客',   desc: '暗杀爆发伤害' },
  { key: 'RANGER',     icon: '🏹',  label: '游侠',   desc: '远程物理输出' },
  { key: 'PALADIN',    icon: '🛡️',  label: '圣骑士', desc: '坦克辅助控制' },
  { key: 'NECROMANCER',icon: '💀',  label: '死灵法师', desc: '召唤亡灵操控' },
  { key: 'BERSERKER',  icon: '🔥',  label: '狂战士', desc: '狂暴近战输出' }
]

const currentClassInfo = computed(() => {
  if (!auth.user?.userClass) return null
  return classes.find(c => c.key === auth.user.userClass)
})

function classLabel(key) {
  if (!key) return '未选择'
  const c = classes.find(x => x.key === key)
  return c ? c.label : key
}

// ═══ 公会状态 ═══
const guildStatus = ref(null)
const guildList = ref([])
const pendingList = ref([])
const isGuildMaster = computed(() => {
  if (!guildStatus.value || !auth.user) return false
  // 需要知道公会 masterUserId，通过 getGuild 接口获取
  return guildStatus.value.masterUserId === auth.user.id
})

// ═══ 编辑 ═══
const editing = ref(false)
const saving = ref(false)
const form = reactive({ nickname: '', bio: '' })

function startEdit() {
  form.nickname = auth.user?.nickname || ''
  form.bio = auth.user?.bio || ''
  editing.value = true
  nextTick(() => { if (editCard.value) animate(editCard.value, { opacity: [0, 1], translateY: [15, 0], duration: 400, ease: 'outCubic' }) })
}

async function saveProfile() {
  saving.value = true
  try { await auth.updateUser({ nickname: form.nickname, bio: form.bio }); toast.success('档案更新成功'); editing.value = false }
  catch { toast.error('更新失败') }
  finally { saving.value = false }
}

// ═══ 职业选择 ═══
async function selectClass(key) {
  if (auth.user.userClass === key) return
  try {
    const res = await chooseClass(key)
    const data = res.data || res
    auth.updateUser(data)
    toast.success(`职业已变更为：${classes.find(c => c.key === key)?.label}`)
    nextTick(() => {
      const idx = classes.findIndex(c => c.key === key)
      if (classCards.value[idx]) {
        animate(classCards.value[idx], { scale: [0.9, 1.05, 1], duration: 400, ease: 'outBack' })
      }
    })
  } catch (e) { toast.error(e?.response?.data?.message || '选择失败') }
}

// ═══ 头像 ═══
const showAvatarModal = ref(false)
const avatarUrl = ref('')
const avatarSaving = ref(false)

async function saveAvatar() {
  if (!avatarUrl.value) { toast.warning('请输入头像 URL'); return }
  avatarSaving.value = true
  try {
    const res = await updateAvatar(avatarUrl.value)
    auth.updateUser(res.data || res)
    showAvatarModal.value = false
    toast.success('头像更换成功')
    nextTick(() => { if (avatarWrap.value) animate(avatarWrap.value, { scale: [0.8, 1], duration: 400, ease: 'outBack' }) })
  } catch { toast.error('头像更新失败') }
  finally { avatarSaving.value = false }
}

// ═══ 公会 ═══
const showApplyModal = ref(false)
const showCreateGuildModal = ref(false)
const applyForm = reactive({ guildId: '', message: '', sending: false })
const createForm = reactive({ name: '', description: '', sending: false })

async function loadGuildData() {
  try {
    const [myRes, listRes] = await Promise.all([getMyGuild(), listGuilds()])
    guildStatus.value = (myRes.data !== undefined ? myRes.data : myRes) || null
    guildList.value = (listRes.data !== undefined ? listRes.data : listRes) || []
    if (guildStatus.value && guildStatus.value.status === 'APPROVED' && guildStatus.value.guildId) {
      const { getGuild } = await import('@/api/guild')
      const gRes = await getGuild(guildStatus.value.guildId)
      const gData = (gRes.data !== undefined ? gRes.data : gRes)
      if (gData && gData.masterUserId === auth.user?.id) {
        guildStatus.value.masterUserId = gData.masterUserId
        const pRes = await getPendingMembers(guildStatus.value.guildId)
        pendingList.value = (pRes.data !== undefined ? pRes.data : pRes) || []
      }
    }
  } catch {}
}

async function loadContent() {
  try {
    const [likesRes, favsRes] = await Promise.all([getUserLikes(), getUserFavorites()])
    likedArticles.value = likesRes.data || likesRes || []
    favoritedArticles.value = favsRes.data || favsRes || []
  } catch (e) {
    toast.error(e?.response?.data?.message || '加载点赞收藏列表失败')
  }
}

async function submitApply() {
  if (!applyForm.guildId) { toast.warning('请选择公会'); return }
  applyForm.sending = true
  try {
    await applyGuild(applyForm.guildId, applyForm.message)
    toast.success('申请已提交，等待会长审批')
    showApplyModal.value = false
    applyForm.message = ''
    await loadGuildData()
  } catch (e) { toast.error(e?.response?.data?.message || '申请失败') }
  finally { applyForm.sending = false }
}

async function submitCreateGuild() {
  if (!createForm.name.trim()) { toast.warning('请输入公会名称'); return }
  createForm.sending = true
  try {
    await createGuild(createForm.name, createForm.description)
    toast.success('公会创建成功')
    showCreateGuildModal.value = false
    createForm.name = ''
    createForm.description = ''
    await loadGuildData()
  } catch (e) { toast.error(e?.response?.data?.message || '创建失败') }
  finally { createForm.sending = false }
}

async function handleApprove(memberId, approve) {
  try {
    await approveMember(memberId, approve)
    toast.success(approve ? '已通过该申请' : '已拒绝该申请')
    await loadGuildData()
  } catch (e) { toast.error(e?.response?.data?.message || '操作失败') }
}

// ═══ 冒险任务 ═══
const actions = [
  { to: '/profile/orders', icon: '📋', label: '我的订单', desc: '查看交易记录' },
  { to: '/upload', icon: '📤', label: '上传资源', desc: '分享你的发现' },
  { to: '/community/create', icon: '✍️', label: '撰写文章', desc: '记录冒险日志' },
  { to: '/ai-station', icon: '🤖', label: 'AI 站点', desc: '智能助手工具' }
]

function handleLogout() { auth.logout(); toast.success('冒险已结束，期待下次重逢'); router.push('/') }

// ═══ 弹窗动画 ═══
function animateModal(el) {
  nextTick(() => { if (el) animate(el, { opacity: [0, 1], translateY: [20, 0], scale: [0.95, 1], duration: 350, ease: 'outCubic' }) })
}
function watchModal(show, elRef) {
  // Not needed with Teleport + ref; handled in @click
}

// ═══ 粒子背景 ═══
let raf = 0
function initParticles() {
  const canvas = bgCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  canvas.width = innerWidth; canvas.height = innerHeight
  const particles = Array.from({ length: 50 }, () => ({
    x: Math.random() * canvas.width,
    y: Math.random() * canvas.height,
    vx: (Math.random() - 0.5) * 0.3,
    vy: -Math.random() * 0.4 - 0.1,
    size: Math.random() * 2 + 0.5,
    alpha: Math.random() * 0.5 + 0.15,
    color: Math.random() > 0.5 ? '251,191,36' : '168,85,247'
  }))
  function draw() {
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    for (const p of particles) {
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2)
      ctx.fillStyle = `rgba(${p.color},${p.alpha})`
      ctx.fill()
      p.x += p.vx; p.y += p.vy
      if (p.y < -10) { p.y = canvas.height + 10; p.x = Math.random() * canvas.width }
      if (p.x < -10) p.x = canvas.width + 10
      if (p.x > canvas.width + 10) p.x = -10
    }
    raf = requestAnimationFrame(draw)
  }
  draw()
}

// ═══ 入场动画 ═══
function entranceAnimation() {
  if (notLoginBox.value) {
    animate(notLoginBox.value, { opacity: [0, 1], translateY: [30, 0], duration: 600, ease: 'outCubic' })
    return
  }
  nextTick(() => {
    const targets = [heroCard.value, contentSection.value, classSection.value, guildCardSection.value, adminSection.value, actionSection.value].filter(Boolean)
    animate(targets, { opacity: [0, 1], translateY: [25, 0], duration: 600, ease: 'outCubic', delay: stagger(100) })
    if (heroName.value) animate(heroName.value, { opacity: [0, 1], duration: 800, delay: 300, ease: 'inOutQuad' })
    nextTick(() => {
      const cards = [...classCards.value.filter(Boolean), ...actionCards.value.filter(Boolean)]
      if (cards.length) animate(cards, { opacity: [0, 1], translateY: [20, 0], scale: [0.9, 1], duration: 500, ease: 'outBack', delay: stagger(60, { start: 400 }) })
      if (guildCard.value) animate(guildCard.value, { opacity: [0, 1], scale: [0.95, 1], duration: 500, ease: 'outBack', delay: 600 })
    })
  })
}

onMounted(async () => {
  if (auth.token && !auth.user) await auth.fetchProfile()
  initParticles()
  await Promise.all([loadGuildData(), loadContent()])
  entranceAnimation()
})

onBeforeUnmount(() => { cancelAnimationFrame(raf) })
</script>

<style scoped>
.profile-bg {
  background: linear-gradient(135deg, #080018 0%, #0a0a1a 30%, #0d0820 60%, #0a0014 100%);
}

.isekai-card {
  @apply relative rounded-2xl border border-white/[0.06] bg-[#0e0e26] p-5 backdrop-blur-md shadow-xl shadow-black/20 transition-all duration-300;
}
.isekai-card:hover { @apply border-white/[0.1] bg-[#12122e]; }

.section-title {
  @apply text-sm font-bold text-amber-200/60 uppercase tracking-widest flex items-center gap-2;
}
.section-title::before {
  content: '';
  @apply w-8 h-px bg-gradient-to-r from-amber-400/40 to-transparent;
}

.isekai-label { @apply text-[11px] font-medium uppercase tracking-wider text-slate-500; }
.isekai-input {
  @apply w-full rounded-lg border border-white/[0.07] bg-[#0e0e26] px-3.5 py-2.5 text-[13px] text-white placeholder-slate-600 outline-none transition-all duration-200 focus:border-amber-400/30 focus:bg-[#141434] focus:ring-1 focus:ring-amber-400/15;
}

.isekai-btn-primary {
  @apply inline-flex items-center justify-center gap-1.5 rounded-xl px-5 py-2.5 text-[13px] font-semibold text-black bg-gradient-to-r from-amber-400 to-orange-400 shadow-lg shadow-amber-500/20 hover:shadow-amber-500/30 hover:scale-[1.02] active:scale-[0.98] disabled:opacity-40 disabled:cursor-not-allowed transition-all duration-200;
}
.isekai-btn-secondary {
  @apply inline-flex items-center justify-center gap-1.5 rounded-xl px-5 py-2.5 text-[13px] font-semibold border border-white/[0.08] bg-[#10102a] text-slate-300 hover:border-amber-400/20 hover:text-amber-300 hover:bg-amber-400/5 disabled:opacity-40 disabled:cursor-not-allowed transition-all duration-200;
}

.stat-badge {
  @apply inline-flex items-center gap-1.5 rounded-full border border-white/[0.06] bg-[#0e0e26] px-2.5 py-1 text-[11px] text-slate-400;
}

.avatar-ring {
  background: conic-gradient(from 0deg, rgba(251,191,36,0.4), rgba(168,85,247,0.4), rgba(34,211,238,0.3), rgba(251,191,36,0.4));
  animation: spin 4s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* 职业卡片 */
.class-card {
  @apply relative rounded-xl border border-white/[0.05] bg-[#0c0c22] p-4 text-center transition-all duration-300 hover:border-white/[0.12] hover:bg-[#121230];
}
.class-card--active {
  @apply border-amber-400/30 bg-amber-400/[0.06] shadow-lg shadow-amber-500/10;
}

/* 公会会员卡 */
.guild-card-wrapper { perspective: 800px; }
.guild-card {
  @apply relative rounded-2xl border border-amber-400/15 bg-gradient-to-br from-[#0d0a1a] to-[#0a0814] overflow-hidden;
  box-shadow: 0 0 30px rgba(251,191,36,0.05), 0 0 60px rgba(168,85,247,0.03);
}
.guild-card-status {
  @apply inline-flex items-center px-2 py-0.5 rounded text-[9px] font-bold uppercase tracking-wider bg-emerald-400/10 text-emerald-300 border border-emerald-400/20;
}

/* 公会管理按钮 */
.guild-btn-approve {
  @apply rounded-lg bg-emerald-400/10 border border-emerald-400/20 px-3 py-1.5 text-[11px] font-medium text-emerald-300 hover:bg-emerald-400/20 transition-all;
}
.guild-btn-reject {
  @apply rounded-lg bg-red-400/10 border border-red-400/20 px-3 py-1.5 text-[11px] font-medium text-red-300 hover:bg-red-400/20 transition-all;
}

/* 弹窗动画 */
.modal-enter-active, .modal-leave-active { transition: opacity 0.25s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
</style>
