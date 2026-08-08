<template>
  <div class="yearly-view" v-if="profileStore.userInfo">
    <section class="yearly-hero module-card">
      <div>
        <p class="eyebrow">年度记录</p>
        <h2>{{ currentYear }} 地球生活档案</h2>
        <p class="desc">把个人时间线沉淀成年度视角，查看事件分布、心情趋势和关键词变化。</p>
      </div>
      <div class="year-selector">
        <el-icon><Calendar /></el-icon>
        <el-select v-model="selectedYear" size="small">
          <el-option label="全部年份" value="all" />
          <el-option v-for="year in yearOptions" :key="year" :label="`${year} 年`" :value="String(year)" />
        </el-select>
      </div>
    </section>

    <div class="stats-grid">
      <div class="stat-card module-card">
        <span>记录数量</span>
        <strong>{{ filteredTimelines.length }}</strong>
      </div>
      <div class="stat-card module-card">
        <span>关键词数量</span>
        <strong>{{ profileStore.keywords.length }}</strong>
      </div>
      <div class="stat-card module-card">
        <span>平均心情</span>
        <strong>{{ averageMood }}</strong>
      </div>
    </div>

    <div class="analysis-grid">
      <DataAnalysis
        :eventsData="profileStore.eventsData"
        :moodTrend="profileStore.moodTrend"
      />
      <WordCloudCard :keywords="profileStore.keywords" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useProfileStore } from '@/stores/profile'
import DataAnalysis from '@/components/Profile/DataAnalysis.vue'
import WordCloudCard from '@/components/Profile/WordCloudCard.vue'

const profileStore = useProfileStore()
const selectedYear = ref('all')
const currentYear = new Date().getFullYear()

const yearOptions = computed(() => {
  const years = new Set()
  profileStore.timelines.forEach((item) => {
    const date = new Date(item.date)
    if (!Number.isNaN(date.getTime())) {
      years.add(date.getFullYear())
    }
  })
  return Array.from(years).sort((a, b) => b - a)
})

const filteredTimelines = computed(() => {
  if (selectedYear.value === 'all') return profileStore.timelines
  return profileStore.timelines.filter((item) => {
    const date = new Date(item.date)
    return !Number.isNaN(date.getTime()) && String(date.getFullYear()) === selectedYear.value
  })
})

const averageMood = computed(() => {
  if (!profileStore.moodTrend.length) return '0.0'
  const total = profileStore.moodTrend.reduce((sum, item) => sum + item.score, 0)
  return (total / profileStore.moodTrend.length).toFixed(1)
})

onMounted(() => {
  profileStore.fetchMockTimelineData()
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.yearly-view {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.module-card {
  background: rgba(255, 255, 255, 0.94);
  border-radius: $radius-base;
  padding: 24px;
  box-shadow: $shadow-base;
}

.yearly-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  min-height: 150px;
  background:
    linear-gradient(105deg, rgba(255, 255, 255, 0.95), rgba(255, 255, 255, 0.74)),
    linear-gradient(135deg, rgba(102, 194, 138, 0.14), rgba(255, 184, 122, 0.14));

  .eyebrow {
    margin: 0 0 8px;
    color: $primary-color;
    font-size: 14px;
    font-weight: 600;
  }

  h2 {
    margin: 0;
    color: $text-main;
    font-size: 28px;
  }

  .desc {
    margin: 12px 0 0;
    color: $text-regular;
    font-size: 15px;
    line-height: 1.8;
  }
}

.year-selector {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 10px;
  color: $primary-color;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.stat-card {
  min-height: 106px;
  display: flex;
  flex-direction: column;
  justify-content: center;

  span {
    color: $text-secondary;
    font-size: 13px;
  }

  strong {
    margin-top: 10px;
    color: $text-main;
    font-size: 28px;
    line-height: 1;
  }
}

.analysis-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(280px, 0.65fr);
  gap: 24px;
  align-items: stretch;
}

@media (max-width: 960px) {
  .yearly-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .stats-grid,
  .analysis-grid {
    grid-template-columns: 1fr;
  }
}
</style>
