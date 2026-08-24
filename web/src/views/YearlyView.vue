<template>
  <div class="yearly-view" v-if="profileStore.userInfo">
    <section class="yearly-hero module-card">
      <div>
        <p class="eyebrow">年度记录</p>
        <h2>{{ selectedYear === 'all' ? '全部年份' : selectedYear }} 地球生活档案</h2>
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
        <strong>{{ filteredKeywords.length }}</strong>
      </div>
      <button type="button" class="stat-card module-card mood-stat-card" @click="moodScoreVisible = true">
        <span>平均心情</span>
        <strong>{{ averageMood }}</strong>
        <small>查看分值说明</small>
      </button>
    </div>

    <div class="analysis-grid">
      <DataAnalysis
        :eventsData="filteredEventsData"
        :moodTrend="filteredMoodTrend"
        :keywords="filteredKeywords"
        :timelines="filteredTimelines"
      />
    </div>

    <section class="ai-summary module-card">
      <div class="ai-summary-header">
        <div>
          <p class="eyebrow">DEEPSEEK AI</p>
          <h3>年度总结</h3>
          <p>根据所选年份的记录生成，只会把记录正文、日期、心情与关键词发送给 AI。</p>
        </div>
        <el-button
          type="primary"
          :loading="aiLoading"
          :disabled="selectedYear === 'all' || !filteredTimelines.length"
          @click="generateSummary"
        >
          {{ aiSummary ? '重新生成' : '生成年度总结' }}
        </el-button>
      </div>

      <el-alert
        v-if="selectedYear === 'all'"
        title="请先在上方选择一个具体年份"
        type="info"
        :closable="false"
        show-icon
      />
      <el-alert
        v-else-if="aiSummary?.stale"
        title="这一年的记录已发生变化，请重新生成总结"
        type="warning"
        :closable="false"
        show-icon
      />
      <div v-if="aiSummary" class="ai-summary-content">
        <h3>{{ aiSummary.title }}</h3>
        <p class="summary-lead">{{ aiSummary.summary }}</p>
        <div class="summary-block">
          <h4>情绪概览</h4>
          <p>{{ aiSummary.moodOverview }}</p>
        </div>
        <div class="summary-block">
          <h4>年度亮点</h4>
          <ul><li v-for="item in aiSummary.highlights" :key="item">{{ item }}</li></ul>
        </div>
        <div class="keyword-insights">
          <div v-for="item in aiSummary.keywordInsights" :key="item.keyword" class="keyword-insight">
            <strong>{{ item.keyword }}</strong><span>{{ item.explanation }}</span>
          </div>
        </div>
        <blockquote>{{ aiSummary.letterToFuture }}</blockquote>
        <small>基于 {{ aiSummary.recordCount }} 条记录 · {{ formatSummaryTime(aiSummary.updatedAt) }}</small>
      </div>
      <el-empty v-else-if="selectedYear !== 'all' && !aiLoading" description="还没有生成这一年的总结" />
    </section>

    <el-dialog v-model="moodScoreVisible" title="心情指数说明" width="min(92vw, 620px)">
      <p class="score-dialog-intro">每条记录的 emoji 都对应固定分值。平均心情为所选年份内，所有已选择心情分值的平均数。</p>
      <div class="mood-score-grid">
        <div v-for="item in moodScores" :key="item.emoji" class="mood-score-item">
          <span class="mood-emoji">{{ item.emoji }}</span>
          <span>{{ item.label }}</span>
          <strong>{{ item.score }} 分</strong>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useProfileStore } from '@/stores/profile'
import DataAnalysis from '@/components/Profile/DataAnalysis.vue'
import { generateYearlySummary, getYearlySummary } from '@/api/modules/stats'

const profileStore = useProfileStore()
const currentYear = new Date().getFullYear()
const selectedYear = ref(String(currentYear))
const moodScoreVisible = ref(false)
const aiSummary = ref(null)
const aiLoading = ref(false)
const moodScores = [
  { emoji: '😊', label: '开心', score: 9 }, { emoji: '😌', label: '平静', score: 7 },
  { emoji: '🥰', label: '感恩', score: 8 }, { emoji: '🤔', label: '思考', score: 6 },
  { emoji: '🥳', label: '兴奋', score: 9 }, { emoji: '😴', label: '疲惫', score: 4 },
  { emoji: '😭', label: '难过', score: 3 }, { emoji: '😤', label: '烦躁', score: 3 },
  { emoji: '😔', label: '低落', score: 3 }, { emoji: '😱', label: '紧张', score: 4 },
  { emoji: '😎', label: '自信', score: 8 }, { emoji: '😶', label: '平淡', score: 5 },
  { emoji: '😡', label: '生气', score: 2 }, { emoji: '😍', label: '心动', score: 8 },
  { emoji: '😳', label: '惊讶', score: 6 }, { emoji: '🤩', label: '期待', score: 8 }
]
const moodScoreMap = new Map(moodScores.map((item) => [item.emoji, item.score]))

const yearOptions = computed(() => {
  const years = new Set([currentYear])
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
  const scores = filteredTimelines.value
    .map((item) => item.mood || item.moods?.[0])
    .map((mood) => moodScoreMap.get(mood))
    .filter((score) => Number.isFinite(score))
  if (!scores.length) return '—'
  return (scores.reduce((sum, score) => sum + score, 0) / scores.length).toFixed(1)
})

const filteredKeywords = computed(() => {
  const counts = new Map()
  filteredTimelines.value.forEach(item => {
    ;(item.keywords || []).forEach(keyword => {
      const value = String(keyword || '').trim()
      if (value) counts.set(value, (counts.get(value) || 0) + 1)
    })
  })
  return [...counts.entries()].map(([name, value]) => ({ name, value }))
})

const filteredEventsData = computed(() => {
  const counts = new Map()
  filteredTimelines.value.forEach(item => {
    const type = item.eventType || '生活日常'
    counts.set(type, (counts.get(type) || 0) + 1)
  })
  return [...counts.entries()].map(([name, value]) => ({ name, value }))
})

const filteredMoodTrend = computed(() => {
  const grouped = new Map()
  filteredTimelines.value.forEach(item => {
    const date = new Date(item.date)
    const score = moodScoreMap.get(item.mood || item.moods?.[0])
    if (Number.isNaN(date.getTime()) || !Number.isFinite(score)) return
    const year = String(date.getFullYear())
    const current = grouped.get(year) || { total: 0, count: 0 }
    current.total += score
    current.count += 1
    grouped.set(year, current)
  })
  return [...grouped.entries()].map(([year, value]) => ({
    year,
    score: Number((value.total / value.count).toFixed(1))
  }))
})

const loadSummary = async () => {
  aiSummary.value = null
  if (selectedYear.value === 'all') return
  aiLoading.value = true
  try {
    aiSummary.value = await getYearlySummary(Number(selectedYear.value)) || null
  } catch (error) {
    ElMessage.error(error.message || '年度总结读取失败')
  } finally {
    aiLoading.value = false
  }
}

const generateSummary = async () => {
  if (selectedYear.value === 'all') return
  aiLoading.value = true
  try {
    aiSummary.value = await generateYearlySummary(Number(selectedYear.value), Boolean(aiSummary.value))
    ElMessage.success('年度总结已生成')
  } catch (error) {
    ElMessage.error(error.message || '年度总结生成失败')
  } finally {
    aiLoading.value = false
  }
}

const formatSummaryTime = value => value
  ? new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
  : ''

watch(selectedYear, loadSummary)

onMounted(async () => {
  await profileStore.fetchProfileData()
  if (profileStore.backendAvailable) await loadSummary()
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

.mood-stat-card {
  width: 100%;
  border: 0;
  text-align: left;
  cursor: pointer;
  transition: transform .2s ease, box-shadow .2s ease;

  small {
    margin-top: 8px;
    color: $primary-color;
    font-size: 12px;
  }

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 10px 24px rgba(82, 151, 105, .16);
  }
}

.score-dialog-intro {
  margin: 0 0 18px;
  color: $text-regular;
  line-height: 1.7;
}

.mood-score-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.mood-score-item {
  display: grid;
  grid-template-columns: auto 1fr;
  align-items: center;
  gap: 4px 8px;
  padding: 12px;
  border: 1px solid rgba(102, 194, 138, .2);
  border-radius: 10px;
  background: #f9fdf9;
  color: $text-regular;
  font-size: 13px;

  .mood-emoji { font-size: 23px; }
  strong { grid-column: 2; color: $primary-color; font-size: 13px; }
}

.analysis-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 24px;
  align-items: stretch;
}

.ai-summary {
  .eyebrow { margin: 0 0 6px; color: $primary-color; font-size: 12px; font-weight: 700; }
}

.ai-summary-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 20px;

  h3 { margin: 0; color: $text-main; font-size: 22px; }
  p:last-child { margin: 8px 0 0; color: $text-regular; line-height: 1.6; }
}

.ai-summary-content {
  padding-top: 8px;
  color: $text-regular;
  line-height: 1.85;

  > h3 { margin: 0 0 12px; color: $text-main; font-size: 24px; }
  .summary-lead { font-size: 16px; }
  h4 { margin: 20px 0 6px; color: $text-main; }
  p, ul { margin-top: 4px; }
  blockquote { margin: 22px 0 12px; padding: 16px 20px; border-left: 4px solid $primary-color; background: #f5fbf6; border-radius: 0 10px 10px 0; }
  > small { color: $text-secondary; }
}

.keyword-insights {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 12px;
  margin-top: 20px;
}

.keyword-insight {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px;
  border: 1px solid rgba(102, 194, 138, .22);
  border-radius: 10px;
  background: #fbfefb;
  strong { color: $primary-color; }
  span { font-size: 13px; line-height: 1.6; }
}

@media (max-width: 960px) {
  .yearly-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .ai-summary-header { flex-direction: column; }

  .stats-grid,
  .analysis-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .mood-score-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
</style>
