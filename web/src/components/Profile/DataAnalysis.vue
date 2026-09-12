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
      <div v-show="activeTab === 'events'" ref="chartBoxRef" class="chart-box">
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
const chartBoxRef = ref(null)
const activeTab = ref('events')
const isNarrow = ref(false)
const hasEvents = computed(() => props.eventsData.length > 0)
const tabs = [
  { key: 'events', label: '事件类型分布' },
  { key: 'keywords', label: '关键词云' }
]

// 数据里的 value 是“次数”，不是百分比；图例和中心文案自己算占比，避免出现“1%”这种误导。
const sortedEvents = computed(() => [...props.eventsData]
  .map(item => ({ name: String(item?.name || '').trim(), value: Number(item?.value) || 0 }))
  .filter(item => item.name)
  .sort((a, b) => b.value - a.value || a.name.localeCompare(b.name, 'zh-CN')))

let pieChart = null
let chartResizeObserver = null

const truncateLabel = (text, max = 4) => (text.length > max ? `${text.slice(0, max)}…` : text)

const buildChartOption = () => {
  const narrow = isNarrow.value
  const data = sortedEvents.value
  const total = data.reduce((sum, item) => sum + item.value, 0) || 1
  const top = data[0]
  // 窄屏：饼图居中、图例放到下方；宽屏：保持原来的左图右例布局
  const center = narrow ? ['50%', '40%'] : ['30%', '50%']
  const legendFormatter = name => {
    const item = data.find(candidate => candidate.name === name)
    return item ? `${name} ${Math.round((item.value / total) * 100)}%` : name
  }

  return {
    tooltip: { trigger: 'item', formatter: params => `${params.name}：${params.value} 条（${params.percent}%）` },
    color: ['#66C28A', '#9ED8B3', '#FFB87A', '#F78E8E', '#A0C6FF'],
    title: top
      ? {
        text: truncateLabel(top.name),
        subtext: `${Math.round((top.value / total) * 100)}%`,
        left: center[0],
        top: center[1],
        textAlign: 'center',
        textVerticalAlign: 'middle',
        textStyle: { color: '#2d4638', fontSize: narrow ? 13 : 15, fontWeight: 700 },
        subtextStyle: { color: '#56906d', fontSize: narrow ? 11 : 12 }
      }
      : undefined,
    legend: narrow
      ? {
        orient: 'horizontal',
        left: 'center',
        bottom: 0,
        icon: 'circle',
        itemWidth: 9,
        itemHeight: 9,
        itemGap: 12,
        textStyle: { color: '#4b5f53', fontSize: 12 },
        formatter: legendFormatter
      }
      : {
        orient: 'vertical',
        right: '8%',
        top: 'middle',
        icon: 'circle',
        itemWidth: 10,
        itemHeight: 10,
        itemGap: 12,
        textStyle: { color: '#4b5f53', fontSize: 13 },
        formatter: legendFormatter
      },
    series: [
      {
        type: 'pie',
        radius: narrow ? ['38%', '58%'] : ['45%', '70%'],
        center,
        avoidLabelOverlap: false,
        label: { show: false },
        labelLine: { show: false },
        itemStyle: { borderColor: '#fafafa', borderWidth: 2 },
        data
      }
    ]
  }
}

const initCharts = () => {
  if (pieChart) pieChart.dispose()
  pieChart = null

  if (pieChartRef.value && hasEvents.value) {
    pieChart = echarts.init(pieChartRef.value)
    pieChart.setOption(buildChartOption())
  }
}

const measureWidth = () => chartBoxRef.value?.clientWidth || 0

// 返回 true 表示“宽窄格局变了”，需要重建 option
const applyNarrow = () => {
  const width = measureWidth()
  const narrow = width > 0 && width < 420
  if (narrow === isNarrow.value) return false
  isNarrow.value = narrow
  return true
}

const resizeChart = () => {
  if (applyNarrow()) initCharts()
  else pieChart?.resize()
}

const selectTab = async (tab) => {
  activeTab.value = tab
  await nextTick()
  if (tab === 'events') resizeChart()
}

onMounted(() => {
  const width = measureWidth()
  isNarrow.value = width > 0 && width < 420

  nextTick(() => {
    initCharts()
    if (chartBoxRef.value && typeof ResizeObserver !== 'undefined') {
      chartResizeObserver = new ResizeObserver(resizeChart)
      chartResizeObserver.observe(chartBoxRef.value)
    }
  })
  window.addEventListener('resize', resizeChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  chartResizeObserver?.disconnect()
  chartResizeObserver = null
  pieChart?.dispose()
})

watch(() => props.eventsData, () => {
  nextTick(() => {
    applyNarrow()
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

  .chart-box {
    min-height: 300px;
    padding: 14px 12px 16px;

    .chart-title {
      align-items: flex-start;
      flex-direction: column;
      margin-bottom: 10px;
      font-size: 15px;
    }

    /* 图例移到饼图下方后需要更多高度，否则会被挤在一起 */
    .chart-content { height: 300px; }

    .chart-empty-state { height: 270px; }
  }
}
</style>
