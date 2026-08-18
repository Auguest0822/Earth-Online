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
      
      <div v-show="activeTab === 'moods'" class="chart-box">
        <h4 class="chart-title">
          心情趋势
          <span class="trend-filters">
            <el-select v-model="trendRange" size="small" class="trend-range-select">
              <el-option label="近一周" value="week" />
              <el-option label="近一个月" value="month" />
              <el-option label="近一年" value="year" />
            </el-select>
            <el-select v-if="trendRange === 'year'" v-model="trendYear" size="small" class="trend-year-select">
              <el-option v-for="year in trendYearOptions" :key="year" :label="`${year} 年`" :value="year" />
            </el-select>
          </span>
        </h4>
        <p v-if="!filteredMoodTrend.length" class="empty-trend">所选时间范围内还没有已选择心情的记录。</p>
        <div v-show="filteredMoodTrend.length" ref="lineChartRef" class="chart-content"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import WordCloudCard from './WordCloudCard.vue'

const props = defineProps({
  eventsData: Array,
  moodTrend: Array,
  timelines: {
    type: Array,
    default: () => []
  },
  keywords: {
    type: Array,
    default: () => []
  }
})

const pieChartRef = ref(null)
const lineChartRef = ref(null)
const activeTab = ref('events')
const trendRange = ref('week')
const trendYear = ref(new Date().getFullYear())
const tabs = [
  { key: 'events', label: '事件类型分布' },
  { key: 'keywords', label: '关键词云' },
  { key: 'moods', label: '心情趋势' }
]

let pieChart = null
let lineChart = null

const moodScoreMap = {
  '😊': 9, '😌': 7, '🥰': 8, '🤔': 6, '🥳': 9, '😴': 4,
  '😭': 3, '😤': 3, '😔': 3, '😱': 4, '😎': 8, '😶': 5,
  '😡': 2, '😍': 8, '😳': 6, '🤩': 8
}

const trendYearOptions = computed(() => {
  const years = new Set(props.timelines
    .map(item => new Date(item.date).getFullYear())
    .filter(Number.isFinite))
  years.add(new Date().getFullYear())
  return [...years].sort((a, b) => b - a)
})

const filteredMoodTrend = computed(() => {
  const now = new Date()
  now.setHours(23, 59, 59, 999)
  const start = new Date(now)
  const group = new Map()

  if (trendRange.value === 'week') start.setDate(now.getDate() - 6)
  if (trendRange.value === 'month') start.setDate(now.getDate() - 29)

  props.timelines.forEach((item) => {
    const date = new Date(item.date)
    const mood = item.mood || item.moods?.[0]
    const score = moodScoreMap[mood]
    if (Number.isNaN(date.getTime()) || !Number.isFinite(score)) return

    let key
    let label
    if (trendRange.value === 'year') {
      if (date.getFullYear() !== trendYear.value) return
      key = date.getMonth() + 1
      label = `${key}月`
    } else {
      if (date < start || date > now) return
      key = `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`
      label = `${date.getMonth() + 1}/${date.getDate()}`
    }

    const current = group.get(key) || { label, total: 0, count: 0 }
    current.total += score
    current.count += 1
    group.set(key, current)
  })

  return [...group.entries()]
    .sort(([a], [b]) => typeof a === 'number' ? a - b : String(a).localeCompare(String(b)))
    .map(([, item]) => ({ label: item.label, score: Number((item.total / item.count).toFixed(1)) }))
})

const selectTab = async (tab) => {
  activeTab.value = tab
  await nextTick()
  if (tab === 'events') pieChart?.resize()
  if (tab === 'moods') lineChart?.resize()
}

const initCharts = () => {
  if (pieChart) pieChart.dispose()
  if (lineChart) lineChart.dispose()

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

  if (lineChartRef.value && filteredMoodTrend.value.length) {
    lineChart = echarts.init(lineChartRef.value)
    lineChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: '10%', right: '5%', bottom: '15%', top: '15%' },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: filteredMoodTrend.value.map(item => item.label),
        axisLine: { lineStyle: { color: '#EBEEF5' } },
        axisLabel: { color: '#999' }
      },
      yAxis: {
        type: 'value',
        show: false
      },
      series: [
        {
          data: filteredMoodTrend.value.map(item => item.score),
          type: 'line',
          smooth: true,
          symbolSize: 8,
          itemStyle: { color: '#66C28A' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(102,194,138,0.3)' },
              { offset: 1, color: 'rgba(102,194,138,0)' }
            ])
          }
        }
      ]
    })
  }
}

onMounted(() => {
  nextTick(() => {
    initCharts()
  })
  window.addEventListener('resize', () => {
    pieChart?.resize()
    lineChart?.resize()
  })
})

watch(() => [props.eventsData, props.moodTrend, props.timelines, trendRange.value, trendYear.value], () => {
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

  .trend-filters {
    display: inline-flex;
    gap: 8px;
  }

  .trend-range-select { width: 108px; }
  .trend-year-select { width: 96px; }

  .empty-trend {
    display: grid;
    min-height: clamp(250px, 22vw, 310px);
    margin: 0;
    place-items: center;
    color: $text-secondary;
    font-size: 14px;
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
