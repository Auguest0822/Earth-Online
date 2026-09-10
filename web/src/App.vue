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
      <div v-if="!profileStore.backendAvailable" class="service-banner" role="status">
        <span><strong>后台服务暂时不可用</strong>　页面已切换为离线展示，数据暂时无法读取或保存。</span>
        <button type="button" :disabled="profileStore.profileLoading" @click="retryBackend">
          {{ profileStore.profileLoading ? '正在重试…' : '重新连接' }}
        </button>
      </div>
      <div class="content-body">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import { watch } from 'vue'
import { useRoute } from 'vue-router'
import Sidebar from "@/components/Sidebar/Sidebar.vue"
import Header from "@/components/Header/Header.vue"
import { useProfileStore } from '@/stores/profile'
import { getAccessToken } from '@/api/httpClient'
import { useChatNotificationStore } from '@/stores/chatNotifications'

const route = useRoute()
const profileStore = useProfileStore()
const chatNotificationStore = useChatNotificationStore()
const retryBackend = () => profileStore.fetchProfileData()

watch(
  () => route.fullPath,
  async () => {
    if (route.meta.layout === 'blank' || !getAccessToken()) {
      await chatNotificationStore.stop()
      return
    }
    await chatNotificationStore.initialize()
  },
  { immediate: true }
)
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

.service-banner {
  display: flex;
  min-height: 44px;
  margin: 0 32px 12px;
  padding: 9px 14px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border: 1px solid #ecd9a7;
  border-radius: 10px;
  color: #735b24;
  background: #fff9e9;
  font-size: 13px;
}

.service-banner button {
  flex: none;
  padding: 6px 12px;
  border: 1px solid #d4b86d;
  border-radius: 8px;
  color: #735b24;
  background: #fff;
  cursor: pointer;
}

.service-banner button:disabled { opacity: .6; cursor: wait; }

@media (max-width: 1280px) {
  .service-banner { margin-right: 24px; margin-left: 24px; }
  .content-body {
    padding: 0 24px 24px;
  }
}

@media (max-width: 900px) {
  .service-banner { margin-right: 18px; margin-left: 18px; }
  .content-body {
    padding: 0 18px 18px;
  }
}

@media (max-width: 640px) {
  .service-banner {
    margin: 0 12px 10px;
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }
  .app-layout {
    min-height: 100dvh;
    height: auto;
  }

  .main-container {
    min-width: 0;
    min-height: 100dvh;
  }

  .content-body {
    padding: 0 12px calc(80px + env(safe-area-inset-bottom));
  }
}
</style>
