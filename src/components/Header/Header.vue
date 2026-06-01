<template>
  <div class="header">
    <div class="header-right">
      <div class="search-box">
        <el-input placeholder="搜索关键词、事件、心情..." prefix-icon="Search" />
      </div>
      
      <div class="actions">
        <el-badge :value="3" class="badge-item">
          <el-icon><Bell /></el-icon>
        </el-badge>
        <el-icon><Setting /></el-icon>
      </div>

      <div class="user-profile">
        <img :src="profileStore.userInfo?.avatar" class="avatar" />
        <div class="user-info">
          <span class="name">{{ profileStore.userInfo?.nickname }}</span>
          <span class="level">Lv.{{ profileStore.userInfo?.level }}</span>
        </div>
        <el-icon><ArrowDown /></el-icon>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useProfileStore } from '@/store/profile'

const profileStore = useProfileStore()
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.header {
  height: $header-height;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 32px;
  background: transparent;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 24px;

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
    gap: 16px;
    font-size: 20px;
    color: $text-regular;
    
    .el-icon {
      cursor: pointer;
      &:hover {
        color: $primary-color;
      }
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
      border-radius: 50%;
    }

    .user-info {
      display: flex;
      flex-direction: column;
      
      .name {
        font-size: 14px;
        color: $text-main;
        font-weight: 500;
      }
      .level {
        font-size: 12px;
        color: $text-secondary;
      }
    }
  }
}
</style>
