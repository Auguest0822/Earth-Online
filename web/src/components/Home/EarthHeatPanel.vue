<template>
  <section class="earth-heat-panel module-card">
    <div class="module-header">
      <div>
        <p class="eyebrow">实时热度</p>
        <h3>今日地球关键词</h3>
      </div>
      <el-icon class="pulse-icon"><TrendCharts /></el-icon>
    </div>

    <div class="trend-list">
      <div v-for="item in trends" :key="item.name" class="trend-item">
        <div class="trend-meta">
          <span class="trend-name">{{ item.name }}</span>
          <span class="trend-value">{{ item.value }}</span>
        </div>
        <div class="trend-track">
          <span :style="{ width: `${item.value}%` }"></span>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
defineProps({
  trends: {
    type: Array,
    default: () => []
  }
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.module-card {
  background: rgba(255, 255, 255, 0.94);
  border-radius: $radius-base;
  padding: 24px;
  box-shadow: $shadow-base;
}

.module-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;

  .eyebrow {
    margin: 0 0 4px;
    color: $primary-color;
    font-size: 13px;
  }

  h3 {
    margin: 0;
    color: $text-main;
    font-size: 18px;
  }
}

.pulse-icon {
  color: $primary-color;
  font-size: 22px;
}

.trend-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.trend-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
  color: $text-regular;
  font-size: 13px;
}

.trend-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.trend-value {
  flex: 0 0 auto;
  color: $primary-color;
  font-weight: 600;
}

.trend-track {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(102, 194, 138, 0.13);

  span {
    display: block;
    height: 100%;
    border-radius: inherit;
    background: linear-gradient(90deg, #66C28A, #9ED8B3);
  }
}
</style>
