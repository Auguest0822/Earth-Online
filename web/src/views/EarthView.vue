<template>
  <div class="earth-view">
    <header class="page-heading">
      <div>
        <p>EARTH PULSE</p>
        <h2>地球动态</h2>
        <span>看看此刻，这颗星球正在发生什么</span>
      </div>
      <div class="sync-summary">
        <el-select v-model="dataSource" class="source-select" aria-label="选择数据源" @change="changeDataSource">
          <el-option label="Mock 演示" value="mock" />
          <el-option label="后端接口" value="api" />
          <el-option label="自动切换" value="auto" />
        </el-select>
        <span>数据来源 {{ sourceLabel }}</span>
        <span>{{ updatedText }}</span>
        <el-button :loading="loading" plain type="success" @click="loadNews">
          <el-icon><RefreshRight /></el-icon>
          刷新
        </el-button>
      </div>
    </header>

    <section v-if="featured" class="featured-card module-card">
      <div class="carousel-frame">
        <img :src="featured.image || fallbackImage" :alt="featured.title" @error="replaceBrokenImage" />
        <button class="carousel-arrow prev" aria-label="上一条新闻" @click="showPrevious">‹</button>
        <button class="carousel-arrow next" aria-label="下一条新闻" @click="showNext">›</button>
        <div class="carousel-dots" aria-label="轮播新闻切换">
          <button
            v-for="(_, index) in carouselItems"
            :key="index"
            :class="{ active: index === carouselIndex }"
            :aria-label="`第 ${index + 1} 条新闻`"
            @click="carouselIndex = index"
          />
        </div>
      </div>
      <div class="featured-content">
        <span class="category">{{ featured.category }}</span>
        <h3>{{ featured.title }}</h3>
        <div class="article-meta">
          <span>{{ featured.source }}</span>
          <span>{{ featured.region }}</span>
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
            <img :src="item.image || fallbackImage" :alt="item.title" loading="lazy" @error="replaceBrokenImage" />
            <div>
              <div class="news-tags">
                <span>{{ item.category }}</span>
                <span>{{ item.region }}</span>
              </div>
              <h4 @click="openArticle(item)">{{ item.title }}</h4>
              <div class="article-meta">
                <span>{{ item.source }}</span>
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
        <section class="module-card focus-card">
          <div class="panel-heading compact">
            <div>
              <p>EARTH PULSE</p>
              <h3>今日焦点</h3>
            </div>
            <el-icon><InfoFilled /></el-icon>
          </div>
          <div class="focus-list">
            <div><span>数据来源</span><strong>{{ sourceLabel }}</strong></div>
            <div><span>当前分类</span><strong>{{ currentCategoryLabel }}</strong></div>
            <div><span>新闻数量</span><strong>{{ news.length }} 条</strong></div>
            <div><span>最近更新</span><strong>{{ updatedText.replace('更新于 ', '') }}</strong></div>
          </div>
          <el-button class="focus-refresh" plain type="success" :loading="loading" @click="loadNews">刷新新闻</el-button>
        </section>

        <section class="module-card tips-card">
          <div class="panel-heading compact">
            <div><p>READING GUIDE</p><h3>阅读提示</h3></div>
            <el-icon><Reading /></el-icon>
          </div>
          <ul class="tips-list">
            <li>点击新闻标题或“阅读原文”查看来源页面。</li>
            <li>切换分类可以浏览不同主题的地球动态。</li>
            <li>接口暂时不可用时，页面会显示演示数据。</li>
          </ul>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchEarthNews, getNewsDataSource, setNewsDataSource } from '@/api/newsService'

const categories = [
  { label: '全部', value: 'all' },
  { label: '国际', value: 'guoji' },
  { label: '国内', value: 'guonei' },
  { label: '科技', value: 'keji' },
  { label: '健康', value: 'jiankang' },
  { label: '财经', value: 'caijing' },
  { label: '军事', value: 'junshi' },
  { label: '娱乐', value: 'yule' }
]

const selectedCategory = ref('all')
const dataSource = ref(getNewsDataSource())
const loading = ref(false)
const news = ref([])
const meta = ref({})
const visibleCount = ref(4)
const carouselIndex = ref(0)
let refreshTimer
let carouselTimer
const fallbackImage = 'data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 width=%22900%22 height=%22520%22 viewBox=%220 0 900 520%22%3E%3Crect width=%22900%22 height=%22520%22 fill=%22%23eef8f0%22/%3E%3Ccircle cx=%22450%22 cy=%22225%22 r=%2286%22 fill=%22%23b5dfc2%22/%3E%3Cpath d=%22M365 225h170M450 140v170%22 stroke=%22%23fff%22 stroke-width=%2218%22 stroke-linecap=%22round%22/%3E%3Ctext x=%22450%22 y=%22380%22 text-anchor=%22middle%22 fill=%22%235d9f70%22 font-family=%22Arial%22 font-size=%2230%22%3EEARTH PULSE%3C/text%3E%3C/svg%3E'

const carouselItems = computed(() => news.value.slice(0, 5))
const featured = computed(() => carouselItems.value[carouselIndex.value] || news.value[0])
const visibleNews = computed(() => news.value.slice(1, visibleCount.value + 1))
const sourceLabel = computed(() => meta.value.sources?.join(' + ') || '本地演示数据')
const currentCategoryLabel = computed(() => categories.find((item) => item.value === selectedCategory.value)?.label || '全部')
const statusText = computed(() => meta.value.dataStatus === 'error'
  ? '接口异常'
  : meta.value.dataStatus === 'fallback' ? '演示模式' : '数据正常')
const statusType = computed(() => meta.value.dataStatus === 'normal' ? 'success' : 'warning')
const dataNote = computed(() => meta.value.message || '分类与地区数据均根据本次同步到的新闻列表统计，不代表全网热度。')

const updatedDate = computed(() => {
  const date = new Date(meta.value.updatedAt)
  return Number.isNaN(date.getTime()) ? null : date
})
const updatedText = computed(() => updatedDate.value
  ? `更新于 ${updatedDate.value.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}`
  : '等待同步')

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

const replaceBrokenImage = (event) => {
  if (event.target.src !== fallbackImage) event.target.src = fallbackImage
}

const showNext = () => {
  if (carouselItems.value.length) carouselIndex.value = (carouselIndex.value + 1) % carouselItems.value.length
}
const showPrevious = () => {
  if (carouselItems.value.length) carouselIndex.value = (carouselIndex.value - 1 + carouselItems.value.length) % carouselItems.value.length
}

const loadNews = async () => {
  loading.value = true
  try {
    const data = await fetchEarthNews({ category: selectedCategory.value, source: dataSource.value })
    news.value = data.events || []
    meta.value = data.meta || {}
    visibleCount.value = 4
    carouselIndex.value = 0
  } finally {
    loading.value = false
  }
}

const changeDataSource = () => {
  setNewsDataSource(dataSource.value)
  loadNews()
}

watch(selectedCategory, loadNews)
onMounted(() => {
  loadNews()
  refreshTimer = window.setInterval(loadNews, 10 * 60 * 1000)
  carouselTimer = window.setInterval(showNext, 5000)
})
onBeforeUnmount(() => {
  window.clearInterval(refreshTimer)
  window.clearInterval(carouselTimer)
})
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
.source-select { width: 118px; }
.featured-card { display: grid; grid-template-columns: 38% minmax(0,1fr); gap: 24px; padding: 14px; }
.carousel-frame { position: relative; min-width: 0; }
.carousel-frame > img { width: 100%; height: 210px; object-fit: cover; border-radius: 12px; }
.carousel-arrow { position: absolute; top: 50%; width: 32px; height: 32px; border: 0; border-radius: 50%; color: #fff; background: rgba(0,0,0,.35); font-size: 24px; line-height: 28px; cursor: pointer; transform: translateY(-50%); }
.carousel-arrow:hover { background: rgba(0,0,0,.58); }
.carousel-arrow.prev { left: 12px; }
.carousel-arrow.next { right: 12px; }
.carousel-dots { position: absolute; right: 0; bottom: 12px; left: 0; display: flex; justify-content: center; gap: 6px; }
.carousel-dots button { width: 7px; height: 7px; padding: 0; border: 0; border-radius: 50%; background: rgba(255,255,255,.65); cursor: pointer; }
.carousel-dots button.active { width: 20px; border-radius: 999px; background: #fff; }
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
.focus-list { display: flex; flex-direction: column; gap: 13px; }
.focus-list > div { display: flex; justify-content: space-between; gap: 12px; color: $text-secondary; font-size: 13px; }
.focus-list strong { color: $text-main; font-weight: 500; text-align: right; }
.focus-refresh { width: 100%; margin-top: 18px; }
.tips-list { display: flex; flex-direction: column; gap: 13px; margin: 0; padding: 0; list-style: none; color: $text-regular; font-size: 13px; line-height: 1.65; }
.tips-list li { position: relative; padding-left: 16px; }
.tips-list li::before { position: absolute; top: .6em; left: 0; width: 6px; height: 6px; border-radius: 50%; background: $primary-color; content: ''; }
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
