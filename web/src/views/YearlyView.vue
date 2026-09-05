<template>
  <div class="yearly-view" v-if="profileStore.userInfo">
    <section class="yearly-hero module-card">
      <div>
        <p class="eyebrow">年度记录</p>
        <h2>{{ selectedYear === 'all' ? '全部年份' : selectedYear }} 地球生活档案</h2>
        <p class="desc">把个人时间线沉淀成年度视角，查看事件分布和关键词变化。</p>
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
    </div>

    <div class="analysis-grid">
      <DataAnalysis
        :eventsData="filteredEventsData"
        :keywords="filteredKeywords"
      />
    </div>

    <section class="ai-memory module-card">
      <div class="ai-summary-header">
        <div>
          <p class="eyebrow">AI 年度回忆</p>
          <h3>重新遇见这一年的自己</h3>
          <p>年度总结梳理全貌，回忆问答只检索所选年份中与你问题相关的个人记录。</p>
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

      <el-tabs v-model="aiTab" class="memory-tabs">
        <el-tab-pane label="年度总结" name="summary">
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
          <h4>年度亮点</h4>
          <ul><li v-for="item in aiSummary.highlights" :key="item">{{ item }}</li></ul>
        </div>
        <div class="keyword-insights">
          <div v-for="item in aiSummary.keywordInsights" :key="item.keyword" class="keyword-insight">
            <strong>{{ item.keyword }}</strong><span v-if="summaryExpanded">{{ item.explanation }}</span>
          </div>
        </div>
        <el-collapse-transition>
          <div v-show="summaryExpanded" class="summary-details">
            <div class="summary-block">
              <h4>情绪概览</h4>
              <p>{{ aiSummary.moodOverview }}</p>
            </div>
            <blockquote>{{ aiSummary.letterToFuture }}</blockquote>
          </div>
        </el-collapse-transition>
        <div class="summary-footer">
          <small>基于 {{ aiSummary.recordCount }} 条记录 · {{ formatSummaryTime(aiSummary.updatedAt) }}</small>
          <el-button link type="primary" @click="summaryExpanded = !summaryExpanded">
            {{ summaryExpanded ? '收起完整总结' : '展开完整总结' }}
          </el-button>
        </div>
      </div>
      <el-empty v-else-if="selectedYear !== 'all' && !aiLoading" description="还没有生成这一年的总结" />
        </el-tab-pane>

        <el-tab-pane label="问问这一年" name="qa">
          <div class="qa-intro">
            <div>
              <h4>问问你的 {{ selectedYear === 'all' ? '这一年' : selectedYear + ' 年' }}</h4>
              <p>{{ selectedYear === 'all' ? '请先选择具体年份再开始回忆。' : `AI 只会检索你在 ${selectedYear} 年的记录，并附上回答依据。` }}</p>
            </div>
            <span class="rag-badge">RAG 检索增强</span>
          </div>

          <div class="question-box">
            <el-input v-model="memoryQuestion" type="textarea" :rows="3" maxlength="200"
              show-word-limit resize="none" placeholder="例如：这一年有哪些让我开心的事情？"
              :disabled="selectedYear === 'all' || memoryLoading" @keydown.ctrl.enter.prevent="askMemory" />
            <el-button type="primary" :loading="memoryLoading"
              :disabled="!canAskMemory" @click="askMemory">开始回忆</el-button>
          </div>

          <div class="suggested-questions">
            <span>试着问：</span>
            <button v-for="item in suggestedQuestions" :key="item" type="button"
              :disabled="selectedYear === 'all' || memoryLoading" @click="useSuggestedQuestion(item)">{{ item }}</button>
          </div>

          <div v-if="memoryLoading" class="memory-loading">
            <el-skeleton :rows="4" animated />
            <span>正在翻阅与你问题相关的记录…</span>
          </div>

          <div v-else-if="memoryAnswer" class="memory-answer">
            <div class="answer-heading"><span class="answer-mark">AI</span><h4>回忆回答</h4></div>
            <p>{{ memoryAnswer.answer }}</p>
            <div class="source-heading">
              <h4>回答依据</h4><span>{{ memoryAnswer.sources.length }} 条记录</span>
            </div>
            <div class="memory-sources">
              <button v-for="source in memoryAnswer.sources" :key="source.recordId" type="button"
                class="source-card" @click="openSource(source.recordId)">
                <div><time>{{ source.date }}</time><strong>{{ source.title }}</strong></div>
                <p>{{ source.excerpt }}</p>
                <span>查看原记录 →</span>
              </button>
            </div>
          </div>

          <el-empty v-else-if="selectedYear !== 'all' && !filteredTimelines.length"
            description="这一年还没有记录，先记录一些生活再来回顾吧" />
        </el-tab-pane>
      </el-tabs>
    </section>

  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useProfileStore } from '@/stores/profile'
import DataAnalysis from '@/components/Profile/DataAnalysis.vue'
import { generateYearlySummary, getYearlySummary, queryYearlyMemory } from '@/api/modules/stats'

const profileStore = useProfileStore()
const router = useRouter()
const currentYear = new Date().getFullYear()
const selectedYear = ref(String(currentYear))
const aiSummary = ref(null)
const aiLoading = ref(false)
const aiTab = ref('summary')
const summaryExpanded = ref(false)
const memoryQuestion = ref('')
const memoryAnswer = ref(null)
const memoryLoading = ref(false)
const suggestedQuestions = ['总结我的成长', '这一年有哪些开心的事情？', '哪些事情影响了我的心情？', '回顾我的学习经历']

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

const canAskMemory = computed(() => selectedYear.value !== 'all'
  && filteredTimelines.value.length > 0 && memoryQuestion.value.trim().length > 0 && !memoryLoading.value)

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

const askMemory = async () => {
  if (!canAskMemory.value) return
  memoryLoading.value = true
  memoryAnswer.value = null
  try {
    memoryAnswer.value = await queryYearlyMemory(Number(selectedYear.value), memoryQuestion.value.trim())
  } catch (error) {
    ElMessage.error(error.message || 'AI 回忆问答失败')
  } finally {
    memoryLoading.value = false
  }
}

const useSuggestedQuestion = question => {
  memoryQuestion.value = question
  askMemory()
}

const openSource = recordId => router.push({ path: '/timeline', query: { record: String(recordId) } })

const formatSummaryTime = value => value
  ? new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
  : ''

watch(selectedYear, () => {
  memoryAnswer.value = null
  summaryExpanded.value = false
  loadSummary()
})

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
  grid-template-columns: repeat(2, minmax(0, 1fr));
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
  grid-template-columns: minmax(0, 1fr);
  gap: 24px;
  align-items: stretch;
}

.ai-memory {
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

.memory-tabs {
  margin-top: 2px;
}

.summary-footer,
.qa-intro,
.source-heading,
.answer-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.summary-footer {
  margin-top: 14px;

  small { color: $text-secondary; }
}

.qa-intro {
  margin: 6px 0 18px;

  h4 { margin: 0; color: $text-main; font-size: 18px; }
  p { margin: 7px 0 0; color: $text-regular; font-size: 14px; }
}

.rag-badge {
  flex: 0 0 auto;
  padding: 6px 10px;
  border: 1px solid rgba(102, 194, 138, .28);
  border-radius: 999px;
  background: #f1faf3;
  color: $primary-color;
  font-size: 12px;
  font-weight: 600;
}

.question-box {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: end;
  gap: 12px;
}

.suggested-questions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
  color: $text-secondary;
  font-size: 13px;

  button {
    padding: 7px 12px;
    border: 1px solid rgba(102, 194, 138, .22);
    border-radius: 999px;
    background: #fbfefb;
    color: $text-regular;
    cursor: pointer;
    transition: border-color .2s ease, color .2s ease, background .2s ease;

    &:hover:not(:disabled) { border-color: $primary-color; background: #f1faf3; color: $primary-color; }
    &:disabled { cursor: not-allowed; opacity: .5; }
  }
}

.memory-loading,
.memory-answer {
  margin-top: 22px;
  padding: 20px;
  border: 1px solid rgba(102, 194, 138, .18);
  border-radius: 14px;
  background: #fbfefb;
}

.memory-loading > span {
  display: block;
  margin-top: 10px;
  color: $text-secondary;
  text-align: center;
  font-size: 13px;
}

.memory-answer {
  color: $text-regular;

  > p { margin: 14px 0 24px; color: $text-main; font-size: 15px; line-height: 1.9; }
}

.answer-heading {
  justify-content: flex-start;

  h4 { margin: 0; color: $text-main; }
}

.answer-mark {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border-radius: 10px;
  background: linear-gradient(135deg, $primary-color, #8fd8aa);
  color: white;
  font-size: 12px;
  font-weight: 700;
}

.source-heading {
  padding-top: 18px;
  border-top: 1px solid rgba(102, 194, 138, .15);

  h4 { margin: 0; color: $text-main; font-size: 15px; }
  span { color: $text-secondary; font-size: 12px; }
}

.memory-sources {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(230px, 1fr));
  gap: 12px;
  margin-top: 12px;
}

.source-card {
  min-width: 0;
  padding: 15px;
  border: 1px solid rgba(102, 194, 138, .2);
  border-radius: 12px;
  background: white;
  color: $text-regular;
  text-align: left;
  cursor: pointer;
  transition: transform .2s ease, box-shadow .2s ease, border-color .2s ease;

  &:hover { transform: translateY(-2px); border-color: $primary-color; box-shadow: 0 8px 20px rgba(82, 151, 105, .12); }
  div { display: flex; flex-direction: column; gap: 5px; }
  time { color: $primary-color; font-size: 12px; }
  strong { overflow: hidden; color: $text-main; text-overflow: ellipsis; white-space: nowrap; }
  p { display: -webkit-box; overflow: hidden; margin: 10px 0; font-size: 13px; line-height: 1.6; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
  > span { color: $primary-color; font-size: 12px; }
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
  .question-box { grid-template-columns: 1fr; }
  .question-box .el-button { width: 100%; }
  .qa-intro { align-items: flex-start; flex-direction: column; }
  .summary-footer { align-items: flex-start; flex-direction: column; }
}
</style>
