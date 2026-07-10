<template>
  <div class="role-panel">
    <div class="left-section">
      <div class="greeting">
        <h2>你好，{{ displayInfo.nickname.split(' ')[0] }} <span class="emoji">{{ displayInfo.nickname.split(' ')[1] }}</span></h2>
        <p class="slogan">{{ displayInfo.slogan }}</p>
      </div>
    </div>
    
    <div class="center-section">
      <div class="avatar-wrapper">
        <img :src="displayInfo.avatar" class="avatar" />
        <div class="check-icon"><el-icon><Check /></el-icon></div>
      </div>
      <div class="level-info">
        <div class="level-header">
          <h3>Lv.{{ displayInfo.level }}</h3>
        </div>
        <div class="exp-text">生存年数 <b>{{ displayInfo.level }}年</b> 
          <el-icon class="info-icon"><InfoFilled /></el-icon>
        </div>
        <el-progress :percentage="displayInfo.experience" :show-text="false" color="#66C28A" />
        <div class="exp-hint">{{ displayInfo.nextLevelExp }}</div>
      </div>
    </div>

    <div class="right-section">
      <div class="stat-item">
        <div class="stat-icon"><el-icon><Sunrise /></el-icon> 生存天数</div>
        <div class="stat-value">{{ displayInfo.survivalDays }} <span>天</span></div>
      </div>
      <div class="stat-item">
        <div class="stat-icon"><el-icon><Calendar /></el-icon> 出生日期</div>
        <div class="stat-value">{{ displayInfo.birthDate }}</div>
      </div>
      <div class="stat-item">
        <div class="stat-icon"><el-icon><Timer /></el-icon> 生存时长</div>
        <div class="stat-value">{{ displayInfo.survivalDuration }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useProfileStore } from '@/stores/profile'

const props = defineProps({
  userInfo: Object
})

const profileStore = useProfileStore()

const displayInfo = computed(() => {
  if (!props.userInfo) return {}
  return {
    ...props.userInfo,
    ...(profileStore.userStats || {})
  }
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.role-panel {
  background: url('https://images.unsplash.com/photo-1518173946687-a4c8892bbd9f?auto=format&fit=crop&q=80') no-repeat center/cover;
  border-radius: $radius-base;
  padding: 30px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: $shadow-base;
  position: relative;
  overflow: hidden;
  color: #fff;
  
  &::before {
    content: '';
    position: absolute;
    top: 0; left: 0; right: 0; bottom: 0;
    background: linear-gradient(90deg, rgba(255,255,255,0.95) 0%, rgba(255,255,255,0.85) 40%, rgba(255,255,255,0.95) 100%);
    z-index: 0;
  }

  > * {
    position: relative;
    z-index: 1;
  }
}

.left-section {
  flex: 1;
  .greeting {
    h2 {
      margin: 0 0 12px 0;
      color: $text-main;
      font-size: 24px;
      .emoji { font-size: 20px; }
    }
    .slogan {
      margin: 0;
      color: $text-regular;
      font-size: 14px;
      line-height: 1.6;
    }
  }
}

.center-section {
  flex: 1.2;
  display: flex;
  align-items: center;
  gap: 20px;
  background: rgba(255,255,255,0.6);
  padding: 20px;
  border-radius: 16px;
  backdrop-filter: blur(10px);

  .avatar-wrapper {
    position: relative;
    .avatar {
      width: 80px;
      height: 80px;
      border-radius: 50%;
      border: 4px solid #fff;
    }
    .check-icon {
      position: absolute;
      bottom: 0; right: 0;
      background: $primary-color;
      color: #fff;
      border-radius: 50%;
      width: 24px; height: 24px;
      display: flex; align-items: center; justify-content: center;
      border: 2px solid #fff;
      font-size: 12px;
    }
  }

  .level-info {
    flex: 1;
    .level-header h3 {
      margin: 0 0 4px 0;
      color: $text-main;
      font-size: 22px;
    }
    .exp-text {
      font-size: 13px;
      color: $text-regular;
      margin-bottom: 8px;
      display: flex; align-items: center; gap: 4px;
      b { color: $text-main; }
      .info-icon { font-size: 14px; color: $text-secondary; }
    }
    .exp-hint {
      font-size: 12px;
      color: $text-secondary;
      text-align: right;
      margin-top: 4px;
    }
  }
}

.right-section {
  flex: 1;
  display: flex;
  justify-content: space-around;
  
  .stat-item {
    display: flex;
    flex-direction: column;
    gap: 8px;
    
    .stat-icon {
      font-size: 13px;
      color: $primary-color;
      display: flex;
      align-items: center;
      gap: 4px;
    }
    
    .stat-value {
      font-size: 18px;
      font-weight: bold;
      color: $text-main;
      span {
        font-size: 13px;
        font-weight: normal;
        color: $text-regular;
      }
    }
  }
}
</style>
