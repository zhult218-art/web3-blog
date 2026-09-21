<template>
  <!-- ============================================================
       BookDesk —— 图书标签页主体
       去掉书桌场景与弹窗：书架直接内嵌平铺，点击书后打开 3D 阅读器
       新增书籍文档无需改本组件，只需在 classicBooks.js 新增数据
       ============================================================ -->
  <div class="book-desk-page">
    <!-- 书架（内嵌网格，常驻显示） -->
    <BookListModal @select="onBookSelect" />

    <!-- 3D 翻书阅读器（浮层，合书后销毁回到书架） -->
    <BookReader
      v-if="currentBook"
      :book="currentBook"
      :origin="readerOrigin"
      @closed="onReaderClosed"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import BookListModal from './BookListModal.vue'
import BookReader from './BookReader.vue'

const currentBook = ref(null)        // 当前阅读的书
const readerOrigin = ref(null)       // 拉近动画起点（封面点击坐标）

// 从书架选中一本书：以封面位置为起点打开阅读器
function onBookSelect({ book, origin }) {
  readerOrigin.value = origin
  currentBook.value = book
}

// 阅读器合书结束 → 销毁阅读器，回到书架
function onReaderClosed() {
  currentBook.value = null
  readerOrigin.value = null
}
</script>

<style scoped>
.book-desk-page {
  width: 100%;
}
</style>
