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
        :autosize="{ minRows: 3, maxRows: 7 }"
        maxlength="1500"
        show-word-limit
        placeholder="此刻的想法、心情或正在发生的事…"
      />
      <div class="composer-actions">
        <div class="record-options">
          <div class="event-type-row">
            <span>事件类型</span>
            <el-select v-model="selectedEventType" class="home-event-type" size="small" aria-label="选择事件类型">
              <el-option v-for="type in eventTypeOptions" :key="type" :label="type" :value="type" />
            </el-select>
          </div>
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
        </div>
        <div class="publish-actions">
          <input ref="photoInput" type="file" accept="image/jpeg,image/png,image/webp" multiple hidden @change="handlePhotoSelect" />
          <el-button plain :disabled="selectedPhotos.length >= 9" @click="photoInput?.click()"><el-icon><Picture /></el-icon>添加图片</el-button>
          <el-button plain @click="tagDialogVisible = true"><el-icon><PriceTag /></el-icon>添加标签</el-button>
          <el-button type="success" :loading="publishing" :disabled="!draft.trim()" @click="publishRecord">
            <el-icon><Promotion /></el-icon>发布记录
          </el-button>
        </div>
      </div>
      <div v-if="tags.length" class="attachment-row">
        <el-tag v-for="tag in tags" :key="tag" closable @close="removeTag(tag)"># {{ tag }}</el-tag>
      </div>
      <div v-if="selectedPhotos.length" class="composer-photos">
        <div v-for="(photo, index) in selectedPhotos" :key="photo.preview">
          <img :src="photo.preview" :alt="`待上传图片 ${index + 1}`" />
          <button type="button" @click="removePhoto(index)">×</button>
        </div>
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
              <p>{{ getContentPreview(item.content) }}</p>
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
          <div v-if="nextFutureLetter" class="letter-preview">
            <div class="envelope">✉️</div>
            <div><strong>{{ nextFutureLetter.title }}</strong><span>将在 {{ formatLetterDate(nextFutureLetter.openAt) }} 开启</span><b>{{ nextFutureLetterDays }} <small>天后</small></b></div>
            <el-button type="success" @click="router.push('/future-letters')">写一封信</el-button>
          </div>
          <div v-else class="letter-empty">还没有等待开启的信。<el-button type="success" @click="router.push('/future-letters')">写一封信</el-button></div>
        </section>

        <section class="month-card module-card">
          <div class="module-title"><div><el-icon><Calendar /></el-icon><h2>本月足迹</h2></div></div>
          <div class="month-summary">
            <div><span>本月事记</span><strong>{{ monthRecords.length }}</strong><small>条</small></div>
            <div><span>常见心情</span><strong>{{ topMonthMood.icon }}</strong><small>{{ topMonthMood.label }}</small></div>
            <div><span>常用关键词</span><strong class="keyword-value">{{ topMonthKeyword || '—' }}</strong><small>{{ topMonthKeyword ? '本月出现最多' : '等待记录' }}</small></div>
          </div>
          <button class="text-link month-link" @click="router.push('/timeline')">查看本月记录 →</button>
        </section>
      </aside>
    </div>

    <el-dialog v-model="tagDialogVisible" title="添加标签" width="360px">
      <el-input v-model="tagInput" maxlength="12" placeholder="例如：学习、生活、旅行" @keyup.enter="confirmTag" />
      <template #footer><el-button type="success" @click="confirmTag">添加</el-button></template>
    </el-dialog>

  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useProfileStore } from '@/stores/profile'
import { uploadRecordImage } from '@/api/modules/uploads'

const router = useRouter()
const profileStore = useProfileStore()
const draft = ref('')
const tags = ref([])
const tagInput = ref('')
const tagDialogVisible = ref(false)
const selectedPhotos = ref([])
const photoInput = ref(null)
const publishing = ref(false)
const moods = [
  { label: '开心', icon: '😊' }, { label: '平静', icon: '😌' },
  { label: '思考', icon: '🤔' }, { label: '兴奋', icon: '🥳' },
  { label: '疲惫', icon: '😴' }, { label: '难过', icon: '😭' },
  { label: '烦躁', icon: '😤' }, { label: '期待', icon: '🤩' }
]
const selectedMood = ref(moods[0])
const eventTypeOptions = ['生活日常', '学习成长', '工作 / 学业', '健康运动', '旅行探索', '人际关系', '其他']
const selectedEventType = ref(eventTypeOptions[0])

const now = new Date()
const greeting = computed(() => now.getHours() < 12 ? '早上好' : now.getHours() < 18 ? '下午好' : '晚上好')
const displayName = computed(() => (profileStore.userInfo?.username || '地球居民').replace(/🌱/g, '').trim())
const todayText = now.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
const weekdayText = now.toLocaleDateString('zh-CN', { weekday: 'long' })
const recentRecords = computed(() => profileStore.timelines.slice(0, 3))
const nextFutureLetter = computed(() => profileStore.futureLetters
  .filter(letter => letter.status === 'sealed' && new Date(letter.openAt).getTime() > Date.now())
  .sort((a, b) => new Date(a.openAt) - new Date(b.openAt))[0] || null)
const nextFutureLetterDays = computed(() => nextFutureLetter.value
  ? Math.max(1, Math.ceil((new Date(nextFutureLetter.value.openAt) - Date.now()) / 86400000))
  : 0)
const monthRecords = computed(() => profileStore.timelines.filter(item => {
  const date = new Date(item.date)
  return date.getFullYear() === now.getFullYear() && date.getMonth() === now.getMonth()
}))
const topMonthMood = computed(() => {
  const counts = new Map()
  monthRecords.value.forEach(item => {
    const icon = item.moods?.[0]
    if (icon) counts.set(icon, (counts.get(icon) || 0) + 1)
  })
  const icon = [...counts.entries()].sort((a, b) => b[1] - a[1])[0]?.[0] || '🌿'
  return { icon, label: moodLabel(icon) }
})
const topMonthKeyword = computed(() => {
  const counts = new Map()
  monthRecords.value.forEach(item => (item.keywords || []).forEach(keyword => counts.set(keyword, (counts.get(keyword) || 0) + 1)))
  return [...counts.entries()].sort((a, b) => b[1] - a[1])[0]?.[0] || ''
})

const formatDay = value => new Date(value).toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' }).replace('/', '-')
const formatYear = value => new Date(value).getFullYear()
const formatLetterDate = value => new Date(value).toLocaleDateString('zh-CN')
const getContentPreview = (content, maxLength = 84) => {
  const normalized = String(content || '').replace(/\s+/g, ' ').trim()
  return normalized.length > maxLength ? `${normalized.slice(0, maxLength)}…` : normalized
}
const moodLabel = icon => moods.find(item => item.icon === icon)?.label || '记录'
const confirmTag = () => {
  const value = tagInput.value.trim()
  if (value && !tags.value.includes(value)) tags.value.push(value)
  tagInput.value = ''
  tagDialogVisible.value = false
}
const removeTag = tag => { tags.value = tags.value.filter(item => item !== tag) }
const releasePhoto = photo => URL.revokeObjectURL(photo.preview)
const removePhoto = index => {
  releasePhoto(selectedPhotos.value[index])
  selectedPhotos.value.splice(index, 1)
}
const handlePhotoSelect = event => {
  const files = Array.from(event.target.files || [])
  event.target.value = ''
  const remaining = 9 - selectedPhotos.value.length
  for (const file of files.slice(0, remaining)) {
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
      ElMessage.warning('只支持 JPG、PNG、WebP 图片')
      continue
    }
    if (file.size > 5 * 1024 * 1024) {
      ElMessage.warning('单张图片不能超过 5MB')
      continue
    }
    selectedPhotos.value.push({ file, preview: URL.createObjectURL(file) })
  }
  if (files.length > remaining) ElMessage.warning('每条记录最多添加 9 张图片')
}
const publishRecord = async () => {
  const date = new Date().toISOString().slice(0, 10)
  publishing.value = true
  try {
    const uploaded = []
    for (const photo of selectedPhotos.value) uploaded.push(await uploadRecordImage(photo.file))
    await profileStore.addTimeline({
      date,
      eventType: selectedEventType.value,
      content: draft.value.trim(),
      keywords: [...tags.value],
      moods: [selectedMood.value.icon],
      photoObjectKeys: uploaded.map(item => item.objectKey)
    })
    draft.value = ''
    selectedEventType.value = eventTypeOptions[0]
    tags.value = []
    selectedPhotos.value.forEach(releasePhoto)
    selectedPhotos.value = []
    ElMessage.success('这一刻已经记录下来')
  } catch (error) {
    ElMessage.error(error.message || '记录保存失败')
  } finally {
    publishing.value = false
  }
}

onMounted(async () => {
  await profileStore.fetchProfileData()
})
onBeforeUnmount(() => selectedPhotos.value.forEach(releasePhoto))
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
.record-options { display:flex; flex:1; flex-wrap:wrap; align-items:center; gap:10px 16px; min-width:0; }
.event-type-row,.mood-row,.publish-actions { display: flex; align-items: center; gap: 8px; }
.event-type-row > span,.mood-row > span { flex:none; color: $text-regular; font-size: 13px; }
.home-event-type { width: 132px; }
.mood-row button { padding: 6px 10px; border: 1px solid transparent; border-radius: 999px; color: $text-regular; background: #fbf7ef; cursor: pointer; }
.mood-row button.active { color: #3f9b68; border-color: rgba(102,194,138,.25); background: #edf8f1; }
.attachment-row { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 12px; }
.composer-photos { display:flex; flex-wrap:wrap; gap:10px; margin-top:12px; }
.composer-photos > div { position:relative; width:82px; height:82px; }
.composer-photos img { width:100%; height:100%; object-fit:cover; border-radius:9px; }
.composer-photos button { position:absolute; top:-6px; right:-6px; width:22px; height:22px; padding:0; border:0; border-radius:50%; color:#fff; background:#e05252; cursor:pointer; }
.dashboard-grid { display: grid; grid-template-columns: minmax(0,1.35fr) minmax(360px,.9fr); gap: 16px; align-items: start; }
.recent-panel { min-height: 386px; }.side-stack { display: grid; grid-template-rows: 176px 176px; align-self: start; gap: 16px; }
.future-card,.month-card { min-height: 0; display: flex; flex-direction: column; justify-content: center; }
.timeline-list { display: flex; flex-direction: column; }.timeline-item { display: grid; grid-template-columns: 56px 10px 1fr; gap: 10px; min-height: 86px; }
.timeline-date strong,.timeline-date span { display: block; }.timeline-date strong { color: $text-main; font-size: 13px; }.timeline-date span { color: $text-secondary; font-size: 11px; }
.timeline-item > i { position: relative; width: 2px; height: 100%; background: rgba(102,194,138,.2); }.timeline-item > i::before { content:''; position:absolute; left:-3px; width:8px; height:8px; border-radius:50%; background:$primary-color; }
.timeline-content { padding: 0 0 12px; }.record-mood { color: #3f9b68; font-size: 12px; }.timeline-content p { display:-webkit-box; overflow:hidden; margin:6px 0; color:$text-regular; font-size:13px; line-height:1.55; -webkit-line-clamp:2; -webkit-box-orient:vertical; }
.record-bottom { display:flex; justify-content:space-between; align-items:center; gap:10px; }.record-bottom span { margin-right:6px; color:#4fa875; background:#edf8f1; padding:3px 7px; border-radius:999px; font-size:10px; }.record-bottom img { width:72px; height:42px; object-fit:cover; border-radius:6px; }
.text-link { border:0; color:#43a36b; background:transparent; cursor:pointer; }.full-link { width:100%; padding:10px; background:rgba(102,194,138,.07); border-radius:8px; }
.letter-preview { display:flex; align-items:center; gap:14px; }.envelope { display:grid; place-items:center; width:88px; height:62px; border-radius:10px; background:#fff5e7; font-size:28px; }
.letter-preview > div:nth-child(2) { flex:1; }.letter-preview strong,.letter-preview span { display:block; }.letter-preview span { margin-top:4px; color:$text-secondary; font-size:11px; }.letter-preview b { display:block; margin-top:5px; color:#43a36b; font-size:23px; }.letter-preview b small { font-size:11px; font-weight:400; }
.letter-empty { display:flex; align-items:center; justify-content:space-between; gap:12px; color:$text-secondary; font-size:13px; }
.month-summary { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:10px; }
.month-summary > div { min-width:0; padding:9px 10px; border-radius:9px; background:#f5faf6; }
.month-summary span,.month-summary small { display:block; overflow:hidden; color:$text-secondary; font-size:10px; text-overflow:ellipsis; white-space:nowrap; }
.month-summary strong { display:inline-block; margin:3px 4px 2px 0; color:#43a36b; font-size:22px; line-height:1.1; }
.month-summary .keyword-value { max-width:100%; overflow:hidden; font-size:15px; text-overflow:ellipsis; white-space:nowrap; }
.month-link { align-self:flex-end; margin-top:10px; font-size:12px; }
@media (max-width:1200px) { .composer-actions{align-items:flex-start;flex-direction:column}.dashboard-grid{grid-template-columns:1fr} }
@media (max-width:760px) { .welcome-row{align-items:flex-start;flex-direction:column}.today-card{text-align:left}.mood-row,.publish-actions{flex-wrap:wrap}.side-stack{display:flex;flex-direction:column} }
</style>
