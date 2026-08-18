<template>
  <div v-if="profileStore.userInfo" class="home-view">
    <header class="welcome-row">
      <div>
        <p>{{ greeting }}，{{ displayName }} 🌱</p>
        <h1>今天，也认真生活在地球上</h1>
        <span>每一次记录，都是你与这个星球的温柔对话。</span>
      </div>
      <div class="today-card">
        <strong>{{ todayText }}</strong>
        <span>{{ weekdayText }}</span>
        <small>愿今天有值得记住的瞬间</small>
      </div>
    </header>

    <section class="composer module-card">
      <div class="module-title">
        <div><el-icon><EditPen /></el-icon><h2>记录此刻</h2></div>
        <span>写下今天的一小段生活</span>
      </div>
      <el-input
        v-model="draft"
        type="textarea"
        :autosize="{ minRows: 2, maxRows: 4 }"
        maxlength="240"
        show-word-limit
        placeholder="此刻的想法、心情或正在发生的事…"
      />
      <div class="composer-actions">
        <div class="mood-row">
          <span>心情</span>
          <button
            v-for="item in moods"
            :key="item.label"
            :class="{ active: selectedMood.label === item.label }"
            @click="selectedMood = item"
          >
            {{ item.icon }} {{ item.label }}
          </button>
        </div>
        <div class="publish-actions">
          <el-button plain @click="tagDialogVisible = true"><el-icon><PriceTag /></el-icon>添加标签</el-button>
          <el-button type="success" :disabled="!draft.trim()" @click="publishRecord">
            <el-icon><Promotion /></el-icon>发布记录
          </el-button>
        </div>
      </div>
      <div v-if="tags.length" class="attachment-row">
        <el-tag v-for="tag in tags" :key="tag" closable @close="removeTag(tag)"># {{ tag }}</el-tag>
      </div>
    </section>

    <div class="dashboard-grid">
      <section class="recent-panel module-card">
        <div class="module-title">
          <div><el-icon><Clock /></el-icon><h2>我的最近时光</h2></div>
        </div>
        <div class="timeline-list">
          <article v-for="item in recentRecords" :key="item.id" class="timeline-item">
            <div class="timeline-date">
              <strong>{{ formatDay(item.date) }}</strong>
              <span>{{ formatYear(item.date) }}</span>
            </div>
            <i></i>
            <div class="timeline-content">
              <div class="record-mood">{{ item.moods?.[0] || '🌿' }} {{ moodLabel(item.moods?.[0]) }}</div>
              <p>{{ item.content }}</p>
              <div class="record-bottom">
                <div><span v-for="keyword in item.keywords || []" :key="keyword"># {{ keyword }}</span></div>
                <img v-if="item.photos?.[0]" :src="item.photos[0]" :alt="item.content" />
              </div>
            </div>
          </article>
        </div>
        <button class="text-link full-link" @click="router.push('/timeline')">查看全部记录 →</button>
      </section>

      <aside class="side-stack">
        <section class="future-card module-card">
          <div class="module-title"><div><el-icon><Message /></el-icon><h2>写给未来</h2></div></div>
          <div class="letter-preview">
            <div class="envelope">✉️</div>
            <div><strong>致未来的自己</strong><span>将在 2027-07-11 开启</span><b>365 <small>天后</small></b></div>
            <el-button type="success" @click="router.push('/future-letters')">写一封信</el-button>
          </div>
        </section>

        <section class="year-card module-card">
          <div class="module-title"><div><el-icon><DataLine /></el-icon><h2>年度生活概览</h2></div></div>
          <div class="year-summary">
            <div><span>记录天数</span><strong>{{ recordDays }} <small>天</small></strong><em>共 {{ profileStore.timelines.length }} 条记录</em></div>
            <div class="mood-chart" aria-label="心情趋势">
              <span v-for="(item, index) in chartPoints" :key="index" :style="{ height: `${item}%` }"></span>
            </div>
          </div>
        </section>
      </aside>
    </div>

    <section class="earth-summary module-card">
      <div class="module-title">
        <div><el-icon><Compass /></el-icon><h2>今天的地球</h2></div>
        <button class="text-link" @click="router.push('/earth')">查看全部新闻 →</button>
      </div>
      <div v-loading="newsLoading" class="earth-news-grid">
        <article v-for="item in earthNews" :key="item.id" @click="router.push('/earth')">
          <img :src="item.image" :alt="item.title" />
          <div><span>{{ item.category }}</span><h3>{{ item.title }}</h3><small>{{ item.source }} · {{ formatNewsTime(item.publishedAt) }}</small></div>
        </article>
      </div>
    </section>

    <el-dialog v-model="tagDialogVisible" title="添加标签" width="360px">
      <el-input v-model="tagInput" maxlength="12" placeholder="例如：学习、生活、旅行" @keyup.enter="confirmTag" />
      <template #footer><el-button type="success" @click="confirmTag">添加</el-button></template>
    </el-dialog>

  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useProfileStore } from '@/stores/profile'
import { fetchEarthNews } from '@/api/newsService'

const router = useRouter()
const profileStore = useProfileStore()
const draft = ref('')
const tags = ref([])
const tagInput = ref('')
const tagDialogVisible = ref(false)
const newsLoading = ref(false)
const earthNews = ref([])
const moods = [
  { label: '开心', icon: '😊' }, { label: '平静', icon: '😌' },
  { label: '感恩', icon: '🙂' }, { label: '思考', icon: '🤔' },
  { label: '疲惫', icon: '🥱' }
]
const selectedMood = ref(moods[0])

const now = new Date()
const greeting = computed(() => now.getHours() < 12 ? '早上好' : now.getHours() < 18 ? '下午好' : '晚上好')
const displayName = computed(() => (profileStore.userInfo?.username || '地球居民').replace(/🌱/g, '').trim())
const todayText = now.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
const weekdayText = now.toLocaleDateString('zh-CN', { weekday: 'long' })
const recentRecords = computed(() => profileStore.timelines.slice(0, 3))
const recordDays = computed(() => new Set(profileStore.timelines.map(item => item.date)).size)
const chartPoints = computed(() => profileStore.moodTrend.length
  ? profileStore.moodTrend.map(item => Math.max(30, Math.round(item.score * 10)))
  : [48, 60, 54, 72, 64, 82])

const formatDay = value => new Date(value).toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' }).replace('/', '-')
const formatYear = value => new Date(value).getFullYear()
const moodLabel = icon => moods.find(item => item.icon === icon)?.label || '记录'
const formatNewsTime = value => {
  const minutes = Math.max(1, Math.floor((Date.now() - new Date(value).getTime()) / 60000))
  return minutes < 60 ? `${minutes} 分钟前` : `${Math.floor(minutes / 60)} 小时前`
}

const confirmTag = () => {
  const value = tagInput.value.trim()
  if (value && !tags.value.includes(value)) tags.value.push(value)
  tagInput.value = ''
  tagDialogVisible.value = false
}
const removeTag = tag => { tags.value = tags.value.filter(item => item !== tag) }
const publishRecord = () => {
  const date = new Date().toISOString().slice(0, 10)
  profileStore.addTimeline({
    date,
    content: draft.value.trim(),
    keywords: [...tags.value],
    moods: [selectedMood.value.icon]
  })
  draft.value = ''
  tags.value = []
  ElMessage.success('这一刻已经记录下来')
}

const loadNews = async () => {
  newsLoading.value = true
  try {
    const data = await fetchEarthNews()
    earthNews.value = (data.events || []).slice(0, 4)
  } finally { newsLoading.value = false }
}

onMounted(async () => {
  await profileStore.fetchProfileData()
  loadNews()
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.home-view { display: flex; flex-direction: column; gap: 16px; padding-bottom: 8px; }
.module-card { padding: 20px; background: rgba(255,255,255,.96); border: 1px solid rgba(102,194,138,.09); border-radius: $radius-base; box-shadow: $shadow-base; }
.welcome-row { display: flex; align-items: center; justify-content: space-between; min-height: 116px; padding: 4px 14px; }
.welcome-row p { margin: 0 0 8px; color: $text-main; font-weight: 600; }
.welcome-row h1 { margin: 0 0 8px; color: $text-main; font-size: 30px; letter-spacing: -.5px; }
.welcome-row > div > span { color: $text-regular; font-size: 14px; }
.today-card { display: grid; grid-template-columns: auto auto; gap: 4px 12px; padding: 14px 18px; text-align: right; }
.today-card strong { color: $text-main; }.today-card span,.today-card small { color: $text-secondary; }.today-card small { grid-column: 1/-1; }
.module-title { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.module-title > div { display: flex; align-items: center; gap: 8px; }.module-title .el-icon { color: $primary-color; font-size: 19px; }
.module-title h2 { margin: 0; color: $text-main; font-size: 17px; }.module-title > span { color: $text-secondary; font-size: 12px; }
.composer :deep(.el-textarea__inner) { padding: 14px; border-radius: 10px; box-shadow: 0 0 0 1px $border-color inset; resize: none; }
.composer-actions { display: flex; justify-content: space-between; gap: 16px; margin-top: 12px; }
.mood-row,.publish-actions { display: flex; align-items: center; gap: 8px; }.mood-row > span { color: $text-regular; font-size: 13px; }
.mood-row button { padding: 6px 10px; border: 1px solid transparent; border-radius: 999px; color: $text-regular; background: #fbf7ef; cursor: pointer; }
.mood-row button.active { color: #3f9b68; border-color: rgba(102,194,138,.25); background: #edf8f1; }
.attachment-row { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 12px; }
.dashboard-grid { display: grid; grid-template-columns: minmax(0,1.35fr) minmax(360px,.9fr); gap: 16px; align-items: stretch; }
.recent-panel { min-height: 386px; }.side-stack { display: grid; grid-template-rows: repeat(2,minmax(0,1fr)); gap: 16px; }
.future-card,.year-card { display: flex; flex-direction: column; justify-content: center; }
.timeline-list { display: flex; flex-direction: column; }.timeline-item { display: grid; grid-template-columns: 56px 10px 1fr; gap: 10px; min-height: 86px; }
.timeline-date strong,.timeline-date span { display: block; }.timeline-date strong { color: $text-main; font-size: 13px; }.timeline-date span { color: $text-secondary; font-size: 11px; }
.timeline-item > i { position: relative; width: 2px; height: 100%; background: rgba(102,194,138,.2); }.timeline-item > i::before { content:''; position:absolute; left:-3px; width:8px; height:8px; border-radius:50%; background:$primary-color; }
.timeline-content { padding: 0 0 12px; }.record-mood { color: #3f9b68; font-size: 12px; }.timeline-content p { margin: 6px 0; color:$text-regular; font-size:13px; line-height:1.55; }
.record-bottom { display:flex; justify-content:space-between; align-items:center; gap:10px; }.record-bottom span { margin-right:6px; color:#4fa875; background:#edf8f1; padding:3px 7px; border-radius:999px; font-size:10px; }.record-bottom img { width:72px; height:42px; object-fit:cover; border-radius:6px; }
.text-link { border:0; color:#43a36b; background:transparent; cursor:pointer; }.full-link { width:100%; padding:10px; background:rgba(102,194,138,.07); border-radius:8px; }
.letter-preview,.year-summary { display:flex; align-items:center; gap:14px; }.envelope { display:grid; place-items:center; width:88px; height:62px; border-radius:10px; background:#fff5e7; font-size:28px; }
.letter-preview > div:nth-child(2) { flex:1; }.letter-preview strong,.letter-preview span { display:block; }.letter-preview span { margin-top:4px; color:$text-secondary; font-size:11px; }.letter-preview b { display:block; margin-top:5px; color:#43a36b; font-size:23px; }.letter-preview b small { font-size:11px; font-weight:400; }
.year-summary > div:first-child { width:110px; }.year-summary span,.year-summary em { display:block; color:$text-secondary; font-size:11px; font-style:normal; }.year-summary strong { display:block; margin:5px 0; color:#43a36b; font-size:26px; }.year-summary strong small { font-size:11px; }
.mood-chart { flex:1; height:64px; display:flex; align-items:flex-end; gap:8px; border-bottom:1px solid $border-color; }.mood-chart span { flex:1; min-height:20%; border-radius:5px 5px 0 0; background:linear-gradient(#8bd0a5,#dff2e6); }
.earth-summary { padding-bottom:16px; }.earth-news-grid { display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); min-height:90px; }.earth-news-grid article { display:grid; grid-template-columns:92px 1fr; gap:10px; padding:0 14px; border-left:1px solid $border-color; cursor:pointer; }.earth-news-grid article:first-child { padding-left:0; border-left:0; }.earth-news-grid img { width:92px; height:70px; border-radius:8px; object-fit:cover; }.earth-news-grid span { color:$primary-color; font-size:10px; }.earth-news-grid h3 { display:-webkit-box; overflow:hidden; margin:4px 0; color:$text-main; font-size:12px; line-height:1.4; -webkit-line-clamp:2; -webkit-box-orient:vertical; }.earth-news-grid small { color:$text-secondary; font-size:9px; }
@media (max-width:1200px) { .composer-actions{align-items:flex-start;flex-direction:column}.dashboard-grid{grid-template-columns:1fr}.earth-news-grid{grid-template-columns:repeat(2,1fr);gap:16px}.earth-news-grid article:nth-child(3){border-left:0} }
@media (max-width:760px) { .welcome-row{align-items:flex-start;flex-direction:column}.today-card{text-align:left}.mood-row,.publish-actions{flex-wrap:wrap}.side-stack{display:flex;flex-direction:column}.earth-news-grid{grid-template-columns:1fr}.earth-news-grid article{padding:12px 0;border-left:0;border-top:1px solid $border-color} }
</style>
