<template>
  <!-- 登录注册页：纯空白布局 -->
  <div v-if="route.meta.layout === 'blank'" class="blank-layout">
    <router-view />
  </div>

  <!-- 其他页面：Sidebar + Header 布局 -->
  <div v-else class="app-layout">
    <Sidebar />
    <div class="main-container">
      <Header />
      <div class="content-body">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
import Sidebar from "@/components/Sidebar/Sidebar.vue"
import Header from "@/components/Header/Header.vue"

const route = useRoute()
</script>

<style lang="scss">
.blank-layout {
  width: 100vw;
  height: 100vh;
}

.app-layout {
  display: flex;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.content-body {
  flex: 1;
  padding: 0 32px 32px 32px;
  overflow-y: auto;
}

@media (max-width: 1280px) {
  .content-body {
    padding: 0 24px 24px;
  }
}

@media (max-width: 900px) {
  .content-body {
    padding: 0 18px 18px;
  }
}

@media (max-width: 640px) {
  .content-body {
    padding: 0 12px 16px;
  }
}
</style>
