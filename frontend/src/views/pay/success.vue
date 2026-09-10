<template>
  <!-- 支付成功页：展示沙箱支付成功的确认信息与后续操作入口 -->
  <div class="min-h-screen px-4 md:px-6 py-10">
    <div class="mx-auto max-w-3xl text-center">
      <div class="text-left mb-5"><PageBack label="返回商城" to="/shop" /></div>
      <div class="glass-panel p-12 neon-border">
        <div class="text-7xl mb-6 animate-float">✅</div>
        <h1 class="text-3xl font-black text-gradient-cyber motions-reveal" v-reveal>支付成功</h1>
        <p class="mt-4 text-gray-400 motions-reveal delay-100" v-reveal>感谢您的购买！订单已生成并确认。</p>
        <div class="holo-bar w-48 mx-auto my-8"></div>
        <div class="flex gap-4 justify-center motions-reveal delay-200" v-reveal>
          <router-link to="/profile/orders" class="web3-btn">查看我的订单</router-link>
          <router-link to="/shop" class="web3-btn-outline">继续购物</router-link>
        </div>
        <p class="mt-8 text-xs text-gray-600 motions-reveal delay-300" v-reveal>当前为沙箱模拟支付环境，不会产生真实扣款</p>
      </div>
    </div>
  </div>
</template>
