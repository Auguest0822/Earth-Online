<template>
  <div class="data-analysis module-card">
    <nav class="analysis-tabs" aria-label="年度数据看板">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        type="button"
        :class="{ active: activeTab === tab.key }"
        @click="selectTab(tab.key)"
      >
        {{ tab.label }}
      </button>
    </nav>

    <div class="charts-container">
      <div v-show="activeTab === 'events'" class="chart-box">
        <h4 class="chart-title">事件类型分布</h4>
        <div v-if="hasEvents" ref="pieChartRef" class="chart-content"></div>
        <div v-else class="chart-empty-state">
          <div class="empty-orbit" aria-hidden="true">
            <span class="empty-orbit__core">✦</span>
            <i></i><b></b>
          </div>
          <p class="chart-empty-state__eyebrow">等待第一段轨迹</p>
          <h5>还没有事件类型分布</h5>
          <p>记录一次生活、学习或旅行，
            <br />这里会慢慢呈现你的生活地图。</p>
          <button type="button" @click="emit('create-record')">记录第一件事</button>
        </div>
      </div>

      <WordCloudCard v-show="activeTab === 'keywords'" :keywords="keywords" embedded />
      
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onBeforeUnmount, onMounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import WordCloudCard from './WordCloudCard.vue'

const props = defineProps({
  eventsData: {
    type: Array,
    default: () => []
  },
  keywords: {
    type: Array,
    default: () => []
  }
})
const emit = defineEmits(['create-record'])

const pieChartRef = ref(null)
const activeTab = ref('events')
const hasEvents = computed(() => props.eventsData.length > 0)
const tabs = [
  { key: 'events', label: '事件类型分布' },
  { key: 'keywords', label: '关键词云' }
]

let pieChart = null

const selectTab = async (tab) => {
  activeTab.value = tab
  await nextTick()
  if (tab === 'events') pieChart?.resize()
}

const initCharts = () => {
  if (pieChart) pieChart.dispose()
  pieChart = null

  if (pieChartRef.value && hasEvents.value) {
    pieChart = echarts.init(pieChartRef.value)
    pieChart.setOption({
      tooltip: { trigger: 'item' },
      color: ['#66C28A', '#9ED8B3', '#FFB87A', '#F78E8E', '#A0C6FF'],
      legend: {
        orient: 'vertical',
        right: '10%',
        top: 'middle',
        icon: 'circle',
        itemWidth: 10,
        itemHeight: 10,
        formatter: (name) => {
          let item = props.eventsData.find(i => i.name === name)
          return `${name} ${item.value}%`
        }
      },
      series: [
        {
          type: 'pie',
          radius: ['45%', '70%'],
          center: ['30%', '50%'],
          avoidLabelOverlap: false,
          label: { show: false },
          labelLine: { show: false },
          data: props.eventsData
        }
      ]
    })
  }

}

const resizeChart = () => pieChart?.resize()

onMounted(() => {
  nextTick(() => {
    initCharts()
  })
  window.addEventListener('resize', resizeChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  pieChart?.dispose()
})

watch(() => props.eventsData, () => {
  nextTick(() => {
    initCharts()
  })
}, { deep: true })
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.module-card {
  background: $card-bg;
  border-radius: $radius-base;
  padding: clamp(24px, 3vw, 40px);
  box-shadow: $shadow-base;
}

.analysis-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: clamp(20px, 2vw, 30px);
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(102, 194, 138, 0.18);

  button {
    padding: 9px 16px;
    border: 0;
    border-radius: 999px;
    color: $text-regular;
    background: transparent;
    font-size: 15px;
    cursor: pointer;
    transition: .2s ease;

    &:hover { color: $primary-color; background: rgba(102, 194, 138, 0.08); }
    &.active { color: #fff; background: $primary-color; box-shadow: 0 5px 14px rgba(102, 194, 138, .22); }
  }
}

.charts-container {
  display: flex;
  flex-direction: column;
  gap: clamp(20px, 2vw, 30px);
  min-height: clamp(330px, 30vw, 390px);
}

.chart-box {
  box-sizing: border-box;
  min-height: clamp(330px, 30vw, 390px);
  background: #fafafa;
  border-radius: 12px;
  padding: clamp(18px, 2vw, 28px);
  
  .chart-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin: 0 0 16px 0;
    font-size: 16px;
    color: $text-main;
    font-weight: 600;
  }
  
  .chart-content {
    height: clamp(250px, 22vw, 310px);
    width: 100%;
  }

  .chart-empty-state {
    display: flex;
    height: clamp(250px, 22vw, 310px);
    padding: 12px;
    align-items: center;
    flex-direction: column;
    justify-content: center;
    text-align: center;

    .empty-orbit {
      position: relative;
      width: 88px;
      height: 88px;
      margin-bottom: 14px;
      border: 2px dashed rgba(102, 194, 138, .42);
      border-radius: 50%;
      animation: orbit-breathe 3.2s ease-in-out infinite;

      &::before {
        position: absolute;
        inset: 15px;
        content: '';
        border-radius: 50%;
        background: radial-gradient(circle at 35% 30%, #e9faef, #bce9ca);
      }
      .empty-orbit__core {
        position: absolute;
        z-index: 1;
        inset: 0;
        display: grid;
        place-items: center;
        color: #4fa875;
        font-size: 24px;
      }
      i, b { position: absolute; z-index: 2; display: block; border-radius: 50%; }
      i { top: 8px; right: 9px; width: 12px; height: 12px; background: $primary-color; box-shadow: 0 0 0 4px #fafafa; }
      b { bottom: 16px; left: 3px; width: 7px; height: 7px; background: #f1c879; }
    }

    &__eyebrow { margin: 0 0 7px; color: $primary-color; font-size: 11px; font-weight: 700; letter-spacing: .12em; }
    h5 { margin: 0; color: $text-main; font-size: 18px; }
    > p:not(.chart-empty-state__eyebrow) { margin: 10px 0 16px; color: $text-secondary; font-size: 13px; line-height: 1.7; }
    button {
      padding: 8px 14px;
      border: 1px solid rgba(79, 168, 117, .3);
      border-radius: 999px;
      color: #378b5e;
      background: #effaf2;
      font-size: 12px;
      cursor: pointer;
      transition: .2s ease;
      &:hover { color: #fff; background: $primary-color; box-shadow: 0 5px 12px rgba(79, 168, 117, .22); }
    }
  }

}

@keyframes orbit-breathe {
  50% { transform: scale(1.04); box-shadow: 0 0 0 9px rgba(102, 194, 138, .05); }
}

@media (max-width: 560px) {
  .analysis-tabs {
    gap: 4px;
    overflow-x: auto;
    white-space: nowrap;

    button { padding: 8px 12px; font-size: 13px; }
  }

  .chart-box .chart-title {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
