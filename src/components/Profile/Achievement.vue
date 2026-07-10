<template>
  <div class="achievement module-card">
    <div class="module-header">
      <div class="title">
        <h3>最近成就</h3>
      </div>
      <div class="more-link">
        查看全部 <el-icon><ArrowRight /></el-icon>
      </div>
    </div>
    
    <div class="badges">
      <div 
        class="badge-item" 
        v-for="(item, index) in achievements" 
        :key="index"
        :class="{ 'inactive': !item.active }"
      >
        <div class="icon-circle">
          <el-icon><component :is="item.icon" /></el-icon>
        </div>
        <span class="name">{{ item.name }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  achievements: Array
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.module-card {
  background: $card-bg;
  border-radius: $radius-base;
  padding: 24px;
  box-shadow: $shadow-base;
}

.module-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;

  .title {
    h3 { margin: 0; font-size: 16px; color: $text-main; }
  }
  
  .more-link {
    font-size: 13px;
    color: $text-secondary;
    cursor: pointer;
    display: flex;
    align-items: center;
    &:hover { color: $primary-color; }
  }
}

.badges {
  display: flex;
  justify-content: space-between;
  
  .badge-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    
    .icon-circle {
      width: 50px;
      height: 50px;
      border-radius: 50%;
      background: linear-gradient(135deg, rgba(230, 246, 235, 1) 0%, rgba(200, 235, 212, 1) 100%);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
      color: #FACC15;
      box-shadow: 0 4px 10px rgba(102, 194, 138, 0.2);
    }
    
    .name {
      font-size: 12px;
      color: $text-main;
      text-align: center;
    }
    
    &.inactive {
      .icon-circle {
        background: #f0f0f0;
        color: #ccc;
        box-shadow: none;
      }
      .name {
        color: $text-secondary;
      }
    }
  }
}
</style>
