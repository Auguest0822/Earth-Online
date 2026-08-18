<template>
  <div class="sidebar">
    <div class="logo-area">
      <img src='/src/assets/picture/logo.png' class="logo-img" alt="logo" />
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
        <el-icon v-if="item.icon === 'Friends'" class="friends-icon" viewBox="0 0 24 24">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <circle cx="9" cy="8" r="3.1" fill="currentColor" />
            <circle cx="16.7" cy="9.2" r="2.5" fill="currentColor" opacity=".78" />
            <path d="M3.7 19.2c.15-3.38 2.34-5.45 5.3-5.45s5.14 2.07 5.3 5.45H3.7Z" fill="currentColor" />
            <path d="M14.45 19.2c.05-2.06-.57-3.72-1.66-4.84 2.96-.54 5.32 1.06 5.5 4.84h-3.84Z" fill="currentColor" opacity=".78" />
          </svg>
        </el-icon>
        <el-icon v-else><component :is="item.icon" /></el-icon>
        <span>{{ item.label }}</span>
      </div>
    </div>

  </div>
</template>

<script setup>
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const menuItems = [
  { path: '/', label: '首页', icon: 'House' },
  { path: '/earth', label: '地球动态', icon: 'Compass' },
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
    width: 60px;
    flex-basis: 60px;
    padding: 12px 7px;
  }

  .logo-area {
    margin-bottom: 20px;
    .logo-img { width: 34px; height: 34px; }
  }

  .menu-list { gap: 7px; }
  .menu-item { padding: 11px 6px; border-radius: 10px; }
  .menu-item .el-icon { font-size: 18px; }
}
</style>
