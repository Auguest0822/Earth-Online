<template>
  <section class="personal-timeline-rail module-card">
    <div class="module-header">
      <div>
        <p class="eyebrow">我的时间轴</p>
        <h3>地球上的我</h3>
      </div>
      <router-link class="timeline-link" to="/timeline">
        查看全部
        <el-icon><ArrowRight /></el-icon>
      </router-link>
    </div>

    <div v-if="items.length" class="timeline-list">
      <article v-for="item in items" :key="item.id" class="timeline-item">
        <div class="date-mark">
          <strong>{{ formatDay(item.date) }}</strong>
          <span>{{ formatMonth(item.date) }}</span>
        </div>
        <div class="event-body">
          <p>{{ item.content }}</p>
          <div v-if="item.keywords?.length" class="keyword-row">
            <span v-for="keyword in item.keywords.slice(0, 3)" :key="keyword">{{ keyword }}</span>
          </div>
        </div>
      </article>
    </div>

    <el-empty v-else description="暂无时间轴记录" />
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  timelines: {
    type: Array,
    default: () => []
  }
})

const items = computed(() => {
  return [...props.timelines]
    .sort((a, b) => new Date(b.date) - new Date(a.date))
    .slice(0, 4)
})

const toDate = (value) => {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? new Date() : date
}

const formatDay = (value) => String(toDate(value).getDate()).padStart(2, '0')
const formatMonth = (value) => `${toDate(value).getFullYear()}.${String(toDate(value).getMonth() + 1).padStart(2, '0')}`
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

.timeline-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: $text-secondary;
  font-size: 13px;
  text-decoration: none;

  &:hover {
    color: $primary-color;
  }
}

.timeline-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.timeline-item {
  display: grid;
  grid-template-columns: 58px minmax(0, 1fr);
  gap: 14px;
  padding: 14px 0;
  border-top: 1px solid rgba(235, 238, 245, 0.9);

  &:first-child {
    border-top: 0;
    padding-top: 0;
  }
}

.date-mark {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  padding-right: 14px;
  color: $text-main;

  &::after {
    position: absolute;
    top: 8px;
    right: -2px;
    width: 8px;
    height: 8px;
    content: '';
    border-radius: 50%;
    background: $primary-color;
    box-shadow: 0 0 0 4px rgba(102, 194, 138, 0.18);
  }

  strong {
    font-size: 18px;
    line-height: 1;
  }

  span {
    margin-top: 6px;
    color: $text-secondary;
    font-size: 12px;
  }
}

.event-body {
  min-width: 0;

  p {
    margin: 0;
    color: $text-main;
    font-size: 14px;
    line-height: 1.7;
  }
}

.keyword-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;

  span {
    color: $primary-color;
    font-size: 12px;
  }
}
</style>
