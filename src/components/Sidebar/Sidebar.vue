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
        <el-icon><component :is="item.icon" /></el-icon>
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
  { path: '/timeline', label: '时间轴', icon: 'Clock' },
  { path: '/yearly', label: '年度记录', icon: 'Calendar' },
  { path: '/capsule', label: '时间胶囊', icon: 'Collection' },
  { path: '/achievements', label: '成就徽章', icon: 'Trophy' },
  { path: '/friends', label: '朋友的地球', icon: 'Connection' }
]

const isActive = (path) => route.path === path

const navigate = (path) => {
  router.push(path)
}
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

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
    width: 40px;
    height: 40px;
    border-radius: 50%;
    object-fit: cover;
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
</style>
