<template>
  <div class="header">
    <div class="header-right">
      <el-dropdown trigger="click" @command="handleCommand">
        <div class="user-profile">
          <img :src="profileStore.userInfo?.avatar" class="avatar" />
          <div class="user-info">
            <div class="user-line" :title="profileStore.userInfo?.nickname">
              <span class="level">Lv.{{ displayLevel }}</span>
              <span class="name">{{ displayName }}</span>
            </div>
          </div>
          <el-icon><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="switch">切换账号</el-dropdown-item>
            <el-dropdown-item command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>

      <div class="search-box">
        <el-input placeholder="搜索关键词、事件、心情..." prefix-icon="Search" />
      </div>
      
      <div class="actions">
        <button class="action-button" aria-label="通知">
          <el-badge :value="3" class="badge-item">
            <el-icon><Bell /></el-icon>
          </el-badge>
        </button>
        <button class="action-button" aria-label="设置">
          <el-icon><Setting /></el-icon>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useProfileStore } from '@/store/profile'
import { useRouter } from 'vue-router'

const profileStore = useProfileStore()
const router = useRouter()

const displayLevel = computed(() => {
  return profileStore.userStats?.level ?? profileStore.userInfo?.level ?? 0
})

const displayName = computed(() => {
  const nickname = profileStore.userInfo?.nickname || '地球居民'
  return Array.from(nickname).slice(0, 10).join('')
})

const handleCommand = (command) => {
  if (command === 'switch' || command === 'logout') {
    // 可以在这里补充清除登录状态等逻辑
    router.push('/login')
  }
}
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.header {
  height: $header-height;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding: 0 32px;
  background: transparent;
}

.header-right {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 24px;
  width: 100%;

  .search-box {
    width: 280px;
    :deep(.el-input__wrapper) {
      border-radius: 20px;
      box-shadow: 0 0 0 1px $border-color inset;
      background-color: transparent;
      &:hover, &.is-focus {
        box-shadow: 0 0 0 1px $primary-color inset;
      }
    }
  }

  .actions {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 20px;
    color: $text-regular;

    .action-button {
      width: 36px;
      height: 36px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      padding: 0;
      border: 0;
      border-radius: 50%;
      color: inherit;
      background: transparent;
      cursor: pointer;
      transition: color 0.2s ease, background-color 0.2s ease;

      &:hover {
        color: $primary-color;
        background-color: rgba(102, 194, 138, 0.1);
      }
    }

    .badge-item {
      display: inline-flex;
      align-items: center;
      justify-content: center;
    }
    
    .el-icon {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
    }
  }

  .user-profile {
    display: flex;
    align-items: center;
    gap: 10px;
    cursor: pointer;

    .avatar {
      width: 36px;
      height: 36px;
      object-fit: cover;
      border-radius: 50%;
    }

    .user-info {
      display: flex;
      align-items: center;
      max-width: 180px;
      min-width: 0;

      .user-line {
        display: flex;
        align-items: center;
        gap: 6px;
        min-width: 0;
      }

      .level {
        flex: 0 0 auto;
        color: $text-secondary;
        font-size: 12px;
        font-weight: 500;
      }

      .name {
        overflow: hidden;
        font-size: 14px;
        color: $text-main;
        font-weight: 500;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
  }
}
</style>
