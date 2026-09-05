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
        <div ref="pieChartRef" class="chart-content"></div>
      </div>

      <WordCloudCard v-show="activeTab === 'keywords'" :keywords="keywords" embedded />
      
    </div>
  </div>
</template>

<script setup>
import { ref, onBeforeUnmount, onMounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import WordCloudCard from './WordCloudCard.vue'

const props = defineProps({
  eventsData: Array,
  keywords: {
    type: Array,
    default: () => []
  }
})

const pieChartRef = ref(null)
const activeTab = ref('events')
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

  if (pieChartRef.value && props.eventsData.length) {
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
