<template>
  <div class="earth-view">
    <header class="page-heading">
      <div>
        <p>EARTH PULSE</p>
        <h2>地球动态</h2>
        <span>看看此刻，这颗星球正在发生什么</span>
      </div>
      <div class="sync-summary">
        <span>数据来源 {{ sourceLabel }}</span>
        <span>{{ updatedText }}</span>
        <el-button :loading="loading" plain type="success" @click="loadNews">
          <el-icon><RefreshRight /></el-icon>
          刷新
        </el-button>
      </div>
    </header>

    <section v-if="featured" class="featured-card module-card">
      <img :src="featured.image" :alt="featured.title" />
      <div class="featured-content">
        <span class="category">{{ featured.category }}</span>
        <h3>{{ featured.title }}</h3>
        <p>{{ featured.summary }}</p>
        <div class="article-meta">
          <span>{{ featured.source }}</span>
          <span>{{ featured.region }}</span>
          <span>{{ formatTime(featured.publishedAt) }}</span>
        </div>
        <el-button type="success" plain @click="openArticle(featured)">阅读原文</el-button>
      </div>
    </section>

    <nav class="category-filter" aria-label="新闻分类">
      <button
        v-for="item in categories"
        :key="item.value"
        :class="{ active: selectedCategory === item.value }"
        @click="selectedCategory = item.value"
      >
        {{ item.label }}
      </button>
    </nav>

    <div class="content-grid">
      <section class="news-panel module-card">
        <div class="panel-heading">
          <div>
            <p>地球大事件</p>
            <h3>正在发生</h3>
          </div>
          <span>共 {{ news.length }} 条</span>
        </div>

        <div v-loading="loading" class="news-list">
          <article v-for="item in visibleNews" :key="item.id" class="news-item">
            <img :src="item.image" :alt="item.title" loading="lazy" />
            <div>
              <div class="news-tags">
                <span>{{ item.category }}</span>
                <span>{{ item.region }}</span>
              </div>
              <h4 @click="openArticle(item)">{{ item.title }}</h4>
              <p>{{ item.summary }}</p>
              <div class="article-meta">
                <span>{{ item.source }}</span>
                <span>{{ formatTime(item.publishedAt) }}</span>
              </div>
            </div>
          </article>
          <el-empty v-if="!loading && !visibleNews.length" description="暂时没有这一分类的新闻" />
        </div>

        <button v-if="news.length > visibleCount" class="load-more" @click="visibleCount += 4">
          加载更多
        </button>
      </section>

      <aside class="insight-column">
        <section class="module-card stats-card">
          <div class="panel-heading compact">
            <div>
              <p>按本次新闻统计</p>
              <h3>分类关注度</h3>
            </div>
            <el-icon><DataAnalysis /></el-icon>
          </div>
          <div class="stat-list">
            <div v-for="item in categoryStats" :key="item.name" class="stat-item">
              <div><span>{{ item.name }}</span><strong>{{ item.count }} 条</strong></div>
              <div class="bar"><i :style="{ width: `${item.percentage}%` }"></i></div>
            </div>
          </div>
        </section>

        <section class="module-card stats-card">
          <div class="panel-heading compact">
            <div>
              <p>按新闻地区字段统计</p>
              <h3>新闻地区分布</h3>
            </div>
            <el-icon><LocationInformation /></el-icon>
          </div>
          <div class="region-list">
            <div v-for="(item, index) in regionStats" :key="item.name" class="region-item">
              <span class="rank">{{ index + 1 }}</span>
              <span class="region-name">{{ item.name }}</span>
              <div class="mini-bar"><i :style="{ width: `${item.percentage}%` }"></i></div>
              <strong>{{ item.count }} 条</strong>
            </div>
          </div>
        </section>

        <section class="module-card data-card">
          <div class="panel-heading compact">
            <div><h3>数据说明</h3></div>
            <el-tag :type="statusType" effect="light">{{ statusText }}</el-tag>
          </div>
          <dl>
            <div><dt>数据来源</dt><dd>{{ sourceLabel }}</dd></div>
            <div><dt>更新时间</dt><dd>{{ exactUpdatedText }}</dd></div>
            <div><dt>更新频率</dt><dd>每 {{ meta.cacheMinutes || 10 }} 分钟</dd></div>
            <div><dt>缓存状态</dt><dd>{{ meta.cached ? '使用缓存' : '刚刚同步' }}</dd></div>
          </dl>
          <p class="data-note">{{ dataNote }}</p>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchEarthNews } from '@/api/newsService'

const categories = [
  { label: '全部', value: 'all' },
  { label: '气候', value: 'climate' },
  { label: '科技', value: 'science' },
  { label: '社会', value: 'society' },
  { label: '经济', value: 'economy' }
]

const selectedCategory = ref('all')
const loading = ref(false)
const news = ref([])
const categoryStats = ref([])
const regionStats = ref([])
const meta = ref({})
const visibleCount = ref(4)
let refreshTimer

const featured = computed(() => news.value[0])
const visibleNews = computed(() => news.value.slice(1, visibleCount.value + 1))
const sourceLabel = computed(() => meta.value.sources?.join(' + ') || '本地演示数据')
const statusText = computed(() => meta.value.dataStatus === 'fallback' ? '演示模式' : '数据正常')
const statusType = computed(() => meta.value.dataStatus === 'fallback' ? 'warning' : 'success')
const dataNote = computed(() => meta.value.message || '分类与地区数据均根据本次同步到的新闻列表统计，不代表全网热度。')

const updatedDate = computed(() => {
  const date = new Date(meta.value.updatedAt)
  return Number.isNaN(date.getTime()) ? null : date
})
const updatedText = computed(() => updatedDate.value
  ? `更新于 ${updatedDate.value.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}`
  : '等待同步')
const exactUpdatedText = computed(() => updatedDate.value
  ? updatedDate.value.toLocaleString('zh-CN', { hour12: false })
  : '尚未更新')

const formatTime = (value) => {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '刚刚'
  const minutes = Math.max(0, Math.floor((Date.now() - date.getTime()) / 60000))
  if (minutes < 60) return `${minutes || 1} 分钟前`
  if (minutes < 1440) return `${Math.floor(minutes / 60)} 小时前`
  return date.toLocaleDateString('zh-CN')
}

const openArticle = (item) => {
  if (/^https?:\/\//.test(item.url || '')) {
    window.open(item.url, '_blank', 'noopener,noreferrer')
    return
  }
  ElMessage.info('当前是演示新闻，暂无外部原文链接')
}

const loadNews = async () => {
  loading.value = true
  try {
    const data = await fetchEarthNews({ category: selectedCategory.value })
    news.value = data.events || []
    categoryStats.value = data.categoryStats || []
    regionStats.value = data.regionStats || []
    meta.value = data.meta || {}
    visibleCount.value = 4
  } finally {
    loading.value = false
  }
}

watch(selectedCategory, loadNews)
onMounted(() => {
  loadNews()
  refreshTimer = window.setInterval(loadNews, 10 * 60 * 1000)
})
onBeforeUnmount(() => window.clearInterval(refreshTimer))
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.earth-view { display: flex; flex-direction: column; gap: 20px; }
.module-card { padding: 22px; background: rgba(255,255,255,.96); border: 1px solid rgba(102,194,138,.1); border-radius: $radius-base; box-shadow: $shadow-base; }
.page-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; }
.page-heading p, .panel-heading p { margin: 0 0 5px; color: $primary-color; font-size: 12px; font-weight: 700; }
.page-heading h2 { margin: 0 0 6px; color: $text-main; font-size: 28px; }
.page-heading > div > span { color: $text-regular; font-size: 14px; }
.sync-summary { display: flex; align-items: center; gap: 14px; color: $text-secondary; font-size: 12px; }
.featured-card { display: grid; grid-template-columns: 38% minmax(0,1fr); gap: 24px; padding: 14px; }
.featured-card > img { width: 100%; height: 210px; object-fit: cover; border-radius: 12px; }
.featured-content { padding: 10px 12px 10px 0; }
.featured-content h3 { margin: 10px 0; color: $text-main; font-size: 24px; line-height: 1.4; }
.featured-content p { margin: 0 0 14px; color: $text-regular; line-height: 1.75; }
.category { color: $primary-color; font-size: 13px; font-weight: 600; }
.article-meta { display: flex; flex-wrap: wrap; gap: 12px; margin: 10px 0 16px; color: $text-secondary; font-size: 12px; }
.category-filter { display: flex; gap: 10px; }
.category-filter button { padding: 9px 18px; border: 1px solid rgba(102,194,138,.2); border-radius: 999px; color: $text-regular; background: #fff; cursor: pointer; }
.category-filter button.active { color: #fff; border-color: $primary-color; background: $primary-color; }
.content-grid { display: grid; grid-template-columns: minmax(0, 2.2fr) minmax(300px, .9fr); gap: 20px; align-items: start; }
.panel-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 16px; }
.panel-heading h3 { margin: 0; color: $text-main; font-size: 18px; }
.panel-heading > span { color: $text-secondary; font-size: 12px; }
.panel-heading.compact { margin-bottom: 18px; }
.panel-heading.compact .el-icon { color: $primary-color; font-size: 20px; }
.news-list { min-height: 300px; }
.news-item { display: grid; grid-template-columns: 170px minmax(0,1fr); gap: 16px; padding: 16px 0; border-top: 1px solid $border-color; }
.news-item:first-child { padding-top: 0; border-top: 0; }
.news-item img { width: 170px; height: 112px; object-fit: cover; border-radius: 10px; }
.news-item h4 { margin: 7px 0; color: $text-main; font-size: 16px; cursor: pointer; }
.news-item h4:hover { color: $primary-color; }
.news-item p { display: -webkit-box; overflow: hidden; margin: 0; color: $text-regular; font-size: 13px; line-height: 1.65; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.news-tags { display: flex; gap: 8px; color: $primary-color; font-size: 12px; }
.load-more { width: 100%; padding: 11px; border: 0; border-radius: 10px; color: $primary-color; background: rgba(102,194,138,.08); cursor: pointer; }
.insight-column { display: flex; flex-direction: column; gap: 20px; }
.stat-list { display: flex; flex-direction: column; gap: 15px; }
.stat-item > div:first-child { display: flex; justify-content: space-between; margin-bottom: 7px; color: $text-regular; font-size: 13px; }
.stat-item strong { color: $primary-color; }
.bar, .mini-bar { overflow: hidden; border-radius: 999px; background: rgba(102,194,138,.12); }
.bar { height: 7px; }
.bar i, .mini-bar i { display: block; height: 100%; border-radius: inherit; background: linear-gradient(90deg,$primary-color,$secondary-color); }
.region-list { display: flex; flex-direction: column; gap: 13px; }
.region-item { display: grid; grid-template-columns: 24px 72px 1fr 38px; gap: 8px; align-items: center; font-size: 12px; }
.rank { color: $primary-color; font-weight: 700; }
.region-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: $text-regular; }
.mini-bar { height: 6px; }
.region-item strong { color: $text-main; font-size: 12px; text-align: right; }
.data-card dl { margin: 0; }
.data-card dl div { display: flex; justify-content: space-between; gap: 18px; padding: 8px 0; }
.data-card dt { color: $text-secondary; }
.data-card dd { margin: 0; color: $text-main; text-align: right; }
.data-note { margin: 14px 0 0; padding-top: 14px; border-top: 1px solid $border-color; color: $text-secondary; font-size: 12px; line-height: 1.7; }

@media (max-width: 1100px) {
  .content-grid { grid-template-columns: 1fr; }
  .insight-column { display: grid; grid-template-columns: repeat(2, minmax(0,1fr)); }
}
@media (max-width: 760px) {
  .page-heading, .sync-summary { align-items: flex-start; flex-direction: column; }
  .featured-card, .news-item { grid-template-columns: 1fr; }
  .featured-card > img, .news-item img { width: 100%; }
  .insight-column { display: flex; }
  .category-filter { overflow-x: auto; }
}
</style>
