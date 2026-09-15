<template>
  <div class="sidebar">
    <div class="logo-area">
      <img :src="logo" class="logo-img" alt="Earth Online logo" />
      <div class="logo-text">
        <h3>地球 Online</h3>
        <span>Earth Online</span>
      </div>
    </div>
    
    <div class="menu-list">
      <div
        v-for="item in menuItems"
        :key="item.path"
        class="menu-item"
        :class="{ active: isActive(item.path) }"
        @click="navigate(item.path)"
      >
        <span class="menu-icon-wrap">
          <el-icon v-if="item.icon === 'Friends'" class="friends-icon" viewBox="0 0 24 24">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <circle cx="7.5" cy="8" r="3" fill="none" stroke="currentColor" stroke-width="1.8" />
              <circle cx="16.5" cy="8" r="3" fill="none" stroke="currentColor" stroke-width="1.8" />
              <path d="M2.5 19c.25-3.65 2.15-5.6 5-5.6s4.75 1.95 5 5.6" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
              <path d="M11.5 19c.25-3.65 2.15-5.6 5-5.6s4.75 1.95 5 5.6" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
            </svg>
          </el-icon>
          <el-icon v-else><component :is="item.icon" /></el-icon>
          <span v-if="item.icon === 'Friends' && chatNotificationStore.friendsBadge" class="unread-badge" :aria-label="`${chatNotificationStore.friendsBadge} 条朋友消息（含好友申请）`">
            {{ chatNotificationStore.friendsBadge > 99 ? '99+' : chatNotificationStore.friendsBadge }}
          </span>
        </span>
        <span>{{ item.label }}</span>
      </div>
    </div>

  </div>
</template>

<script setup>
import { useRouter, useRoute } from 'vue-router'
import logo from '@/assets/picture/logo.png'
import { useChatNotificationStore } from '@/stores/chatNotifications'

const router = useRouter()
const route = useRoute()
const chatNotificationStore = useChatNotificationStore()

const menuItems = [
  { path: '/', label: '首页', icon: 'House' },
  { path: '/timeline', label: '时间线', icon: 'Clock' },
  { path: '/yearly', label: '年度记录', icon: 'Calendar' },
  { path: '/future-letters', label: '写给未来', icon: 'MessageBox' },
  { path: '/friends', label: '朋友', icon: 'Friends' },
  { path: '/profile', label: '我的地球档案', icon: 'User' }
]

const isActive = (path) => route.path === path

const navigate = (path) => {
  router.push(path)
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.sidebar {
  width: $sidebar-width;
  height: 100%;
  background-color: transparent;
  display: flex;
  flex-direction: column;
  padding: 24px;
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 40px;
  
  .logo-img {
    width: 44px;
    height: 44px;
    border-radius: 50%;
    object-fit: cover;
    image-rendering: -webkit-optimize-contrast;
  }

  .logo-text {
    h3 {
      margin: 0;
      font-size: 16px;
      color: $text-main;
    }
    span {
      font-size: 12px;
      color: $text-secondary;
    }
  }
}

.menu-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 12px;
  cursor: pointer;
  color: $text-regular;
  transition: all 0.3s ease;

  .el-icon {
    font-size: 18px;
  }

  .menu-icon-wrap { position: relative; display: grid; place-items: center; }
  .unread-badge {
    position: absolute;
    top: -8px;
    right: -13px;
    display: grid;
    min-width: 17px;
    height: 17px;
    padding: 0 4px;
    place-items: center;
    border: 2px solid #f9fdf9;
    border-radius: 10px;
    color: #fff;
    background: #ef5b5b;
    font-size: 9px;
    font-weight: 700;
    line-height: 1;
  }

  .friends-icon svg { width: 1em; height: 1em; }

  &:hover {
    background-color: rgba(102, 194, 138, 0.1);
    color: $primary-color;
  }

  &.active {
    background-color: rgba(102, 194, 138, 0.15);
    color: $primary-color;
    font-weight: 500;
  }
}

@media (max-width: 1280px) {
  .sidebar {
    width: 200px;
    padding: 20px 16px;
  }

  .logo-area { margin-bottom: 30px; }
  .menu-item { padding: 11px 12px; }
}

@media (max-width: 900px) {
  .sidebar {
    width: 76px;
    flex: 0 0 76px;
    padding: 16px 10px;
  }

  .logo-area {
    justify-content: center;
    margin-bottom: 28px;

    .logo-img { width: 38px; height: 38px; }
    .logo-text { display: none; }
  }

  .menu-list { gap: 10px; }
  .menu-item { justify-content: center; padding: 13px 8px; }
  .menu-item span { display: none; }
  .menu-item .el-icon { font-size: 20px; }
}

@media (max-width: 640px) {
  .sidebar {
    position: fixed;
    z-index: 100;
    right: 0;
    bottom: 0;
    left: 0;
    width: 100%;
    height: calc(64px + env(safe-area-inset-bottom));
    padding: 6px 8px env(safe-area-inset-bottom);
    border-top: 1px solid rgba(102, 194, 138, .18);
    background: rgba(250, 253, 250, .96);
    box-shadow: 0 -8px 24px rgba(45, 90, 61, .08);
    backdrop-filter: blur(16px);
  }

  .logo-area { display: none; }

  .menu-list { display: grid; grid-template-columns: repeat(6, 1fr); gap: 2px; }
  .menu-item { min-width: 0; padding: 11px 4px; border-radius: 10px; }
  .menu-item .el-icon { font-size: 18px; }
}
</style>
