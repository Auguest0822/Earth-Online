<template>
  <div class="data-analysis module-card">
    <div class="module-header">
      <div class="title">
        <h3>数据看板</h3>
        <el-icon class="icon" style="cursor:pointer" @click="initCharts"><RefreshRight /></el-icon>
      </div>
    </div>

    <div class="charts-container">
      <div class="chart-box">
        <h4 class="chart-title">事件类型分布</h4>
        <div ref="pieChartRef" class="chart-content"></div>
      </div>
      
      <div class="chart-box">
        <h4 class="chart-title">
          心情趋势
          <el-select size="small" style="width: 90px; float: right;">
            <el-option label="全部年份" value="all" />
          </el-select>
        </h4>
        <div ref="lineChartRef" class="chart-content"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  eventsData: Array,
  moodTrend: Array
})

const pieChartRef = ref(null)
const lineChartRef = ref(null)

let pieChart = null
let lineChart = null

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

  if (lineChartRef.value && props.moodTrend.length) {
    lineChart = echarts.init(lineChartRef.value)
    lineChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: '10%', right: '5%', bottom: '15%', top: '15%' },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: props.moodTrend.map(item => item.year),
        axisLine: { lineStyle: { color: '#EBEEF5' } },
        axisLabel: { color: '#999' }
      },
      yAxis: {
        type: 'value',
        show: false
      },
      series: [
        {
          data: props.moodTrend.map(item => item.score),
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

watch(() => [props.eventsData, props.moodTrend], () => {
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
  padding: 24px;
  box-shadow: $shadow-base;
}

.module-header {
  margin-bottom: 20px;
  .title {
    display: flex;
    align-items: center;
    gap: 8px;
    h3 { margin: 0; font-size: 16px; color: $text-main; }
    .icon { color: $text-secondary; font-size: 14px; }
  }
}

.charts-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.chart-box {
  background: #fafafa;
  border-radius: 12px;
  padding: 16px;
  
  .chart-title {
    margin: 0 0 16px 0;
    font-size: 14px;
    color: $text-main;
    font-weight: 500;
  }
  
  .chart-content {
    height: 180px;
    width: 100%;
  }
}
</style>
