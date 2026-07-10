<template>
  <div class="profile-view" v-if="profileStore.userInfo">
    <section class="earth-focus module-card">
      <div class="focus-content">
        <div class="section-kicker">
          <el-icon><Connection /></el-icon>
          今日地球焦点
        </div>
        <h2>{{ featuredNews?.title || '正在连接地球新闻网络' }}</h2>
        <p>{{ featuredNews?.summary || '从 GDELT 与新闻 API 聚合全球事件，让首页像一份温柔但敏锐的地球日报。' }}</p>
        <div class="focus-meta" v-if="featuredNews">
          <span>{{ featuredNews.category }}</span>
          <span>{{ featuredNews.region }}</span>
          <span>{{ featuredNews.source }}</span>
          <span>{{ formatTime(featuredNews.publishedAt) }}</span>
        </div>
      </div>

      <div class="focus-panel">
        <div class="focus-stat">
          <span>事件源</span>
          <strong>{{ sourceLabel }}</strong>
        </div>
        <div class="focus-stat">
          <span>追踪事件</span>
          <strong>{{ newsEvents.length }}</strong>
        </div>
        <div class="focus-stat">
          <span>更新状态</span>
          <strong>{{ newsLoading ? '同步中' : '已同步' }}</strong>
        </div>
        <el-button type="success" plain size="small" :loading="newsLoading" @click="loadEarthNews">
          <el-icon><RefreshRight /></el-icon>
          刷新
        </el-button>
      </div>
    </section>

    <section class="category-bar">
      <button
        v-for="category in categories"
        :key="category.value"
        :class="{ active: selectedCategory === category.value }"
        @click="selectedCategory = category.value"
      >
        <el-icon><component :is="category.icon" /></el-icon>
        {{ category.label }}
      </button>
      <span class="updated-at">{{ lastUpdatedText }}</span>
    </section>

    <div class="main-content">
      <div class="left-column">
        <EarthEventFeed
          :events="feedNews"
          :loading="newsLoading"
          :source-label="sourceLabel"
        />
      </div>

      <div class="right-column">
        <EarthHeatPanel :trends="newsTrends" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useProfileStore } from '@/stores/profile'
import EarthEventFeed from '@/components/Home/EarthEventFeed.vue'
import EarthHeatPanel from '@/components/Home/EarthHeatPanel.vue'
import { fetchEarthNews } from '@/api/newsService'

const profileStore = useProfileStore()
const selectedCategory = ref('all')
const newsLoading = ref(false)
const newsEvents = ref([])
const newsTrends = ref([])
const sourceMode = ref('fallback')
const lastUpdatedAt = ref(null)

const categories = [
  { label: '全球', value: 'all', icon: 'Position' },
  { label: '气候', value: 'climate', icon: 'MostlyCloudy' },
  { label: '科技', value: 'science', icon: 'Monitor' },
  { label: '社会', value: 'society', icon: 'UserFilled' },
  { label: '经济', value: 'economy', icon: 'TrendCharts' }
]

const sourceLabel = computed(() => {
  const labels = {
    backend: 'GDELT + News API',
    'gdelt-newsapi': 'GDELT + NewsAPI',
    gdelt: 'GDELT',
    fallback: '演示数据'
  }
  return labels[sourceMode.value] || 'GDELT'
})

const featuredNews = computed(() => newsEvents.value[0])
const feedNews = computed(() => newsEvents.value.slice(1).length ? newsEvents.value.slice(1) : newsEvents.value)

const lastUpdatedText = computed(() => {
  if (!lastUpdatedAt.value) return '等待首次同步'
  return `上次同步 ${lastUpdatedAt.value.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}`
})

const formatTime = (value) => {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '刚刚'

  const diffMinutes = Math.max(0, Math.floor((Date.now() - date.getTime()) / 60000))
  if (diffMinutes < 60) return `${diffMinutes || 1} 分钟前`
  if (diffMinutes < 1440) return `${Math.floor(diffMinutes / 60)} 小时前`
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

const loadEarthNews = async () => {
  newsLoading.value = true
  try {
    const data = await fetchEarthNews({ category: selectedCategory.value })
    newsEvents.value = data.events
    newsTrends.value = data.trends
    sourceMode.value = data.sourceMode
    lastUpdatedAt.value = new Date()
  } finally {
    newsLoading.value = false
  }
}

onMounted(() => {
  profileStore.fetchProfileData()
  loadEarthNews()
})

watch(selectedCategory, () => {
  loadEarthNews()
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.profile-view {
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

.earth-focus {
  min-height: 194px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 32px;
  align-items: center;
  overflow: hidden;
  background:
    linear-gradient(105deg, rgba(255, 255, 255, 0.95), rgba(255, 255, 255, 0.72)),
    linear-gradient(135deg, rgba(102, 194, 138, 0.16), rgba(160, 198, 255, 0.18));
}

.section-kicker {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: $primary-color;
  font-size: 14px;
  font-weight: 600;
}

.focus-content {
  min-width: 0;

  h2 {
    margin: 14px 0 10px;
    color: $text-main;
    font-size: 28px;
    line-height: 1.35;
  }

  p {
    max-width: 820px;
    margin: 0;
    color: $text-regular;
    font-size: 15px;
    line-height: 1.9;
  }
}

.focus-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;

  span {
    padding: 6px 10px;
    border-radius: 999px;
    color: $text-regular;
    background: rgba(255, 255, 255, 0.76);
    font-size: 12px;
  }
}

.focus-panel {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  align-items: stretch;

  .el-button {
    grid-column: 1 / -1;
    justify-self: end;
  }
}

.focus-stat {
  min-height: 76px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 14px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.72);

  span {
    color: $text-secondary;
    font-size: 12px;
  }

  strong {
    margin-top: 8px;
    color: $text-main;
    font-size: 18px;
    line-height: 1.2;
  }
}

.category-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  padding: 6px 0;

  button {
    height: 38px;
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 0 16px;
    border: 1px solid rgba(102, 194, 138, 0.2);
    border-radius: 999px;
    color: $text-regular;
    background: rgba(255, 255, 255, 0.82);
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover,
    &.active {
      color: #fff;
      border-color: $primary-color;
      background: $primary-color;
      box-shadow: 0 8px 20px rgba(102, 194, 138, 0.18);
    }
  }
}

.updated-at {
  margin-left: auto;
  color: $text-secondary;
  font-size: 13px;
}

.main-content {
  display: flex;
  gap: 24px;
  
  .left-column {
    flex: 0 0 calc(72% - 12px);
    display: flex;
    flex-direction: column;
  }

  .right-column {
    flex: 0 0 calc(28% - 12px);
    display: flex;
    flex-direction: column;
    gap: 24px;
  }
}

@media (max-width: 1200px) {
  .earth-focus {
    grid-template-columns: 1fr;
  }

  .main-content {
    flex-direction: column;
    
    .left-column, .right-column {
      flex: 1 1 auto;
    }
  }
}

@media (max-width: 760px) {
  .profile-view {
    gap: 18px;
  }

  .module-card {
    padding: 18px;
  }

  .focus-content h2 {
    font-size: 22px;
  }

  .focus-panel {
    grid-template-columns: 1fr;

    .el-button {
      justify-self: stretch;
    }
  }

  .updated-at {
    width: 100%;
    margin-left: 0;
  }
}
</style>
