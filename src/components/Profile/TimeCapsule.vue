<template>
  <div class="time-capsule-card module-card">
    <div class="module-header">
      <div class="title">
        <h3>时间胶囊</h3>
      </div>
    </div>
    
    <div class="capsule-content" v-if="capsules && capsules.length">
      <div class="info">
        <h4 class="title-text">{{ capsules[0].title }}</h4>
        <p class="desc">{{ capsules[0].desc }}</p>
        <el-button type="success" round size="small" class="create-btn">创建时间胶囊</el-button>
      </div>
      <div class="icon-img">
        <img src="https://images.unsplash.com/photo-1597589827317-4c6d6e0a90bd?auto=format&fit=crop&q=80&w=80&h=120" alt="capsule">
      </div>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  capsules: Array
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.module-card {
  background: $card-bg;
  border-radius: $radius-base;
  padding: 24px;
  box-shadow: $shadow-base;
  height: 100%;
}

.module-header {
  margin-bottom: 20px;
  .title {
    h3 { margin: 0; font-size: 16px; color: $text-main; }
  }
}

.capsule-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 150px;
  
  .info {
    flex: 1;
    .title-text {
      margin: 0 0 8px 0;
      color: $primary-color;
      font-size: 16px;
    }
    .desc {
      margin: 0 0 20px 0;
      color: $text-secondary;
      font-size: 13px;
    }
    .create-btn {
      width: 110px;
    }
  }

  .icon-img {
    width: 60px;
    height: 100px;
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      border-radius: 30px;
    }
  }
}
</style>
