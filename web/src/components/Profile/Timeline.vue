<template>
  <div class="timeline-container module-card">
    <div class="module-header">
      <div class="title">
        <h3>我的时间轴</h3>
        <el-icon class="icon"><MagicStick /></el-icon>
      </div>
      <div class="actions">
        <el-select class="filter-select" size="small" placeholder="全部年份" v-model="selectedYear" @change="applyFilters">
          <el-option label="全部年份" value="all" />
          <el-option v-for="year in yearOptions" :key="year" :label="year" :value="String(year)" />
        </el-select>
        <el-select class="filter-select" size="small" placeholder="全部标签" v-model="selectedTag" clearable @change="applyFilters">
          <el-option v-for="tag in tagOptions" :key="tag" :label="tag" :value="tag" />
        </el-select>
        <el-select class="filter-select event-filter" size="small" placeholder="全部事件" v-model="selectedEventType" clearable @change="applyFilters">
          <el-option v-for="type in filterEventTypeOptions" :key="type" :label="type" :value="type" />
        </el-select>
        <el-button class="create-button" type="primary" size="small" @click="handleCreate">
          <el-icon><Plus /></el-icon> 新增
        </el-button>
      </div>
    </div>

    <div class="timeline-wrapper" v-loading="profileStore.timelineLoading">
      <div v-if="groupedTimelines.length" class="timeline-line"></div>
      
      <div v-if="!groupedTimelines.length" class="empty-state">
        <div class="empty-orbit" aria-hidden="true">
          <span></span><i></i><b></b>
        </div>
        <p class="empty-kicker">{{ hasActiveFilters ? '没有匹配的事记' : '从第一条记录开始' }}</p>
        <h4>{{ hasActiveFilters ? '换个筛选条件试试' : '你的时间线还没有开始' }}</h4>
        <p class="empty-copy">{{ hasActiveFilters ? '当前年份、标签或事件类型下没有记录。' : '记录今天的心情、想法或一件小事，让它成为你在地球上的第一段轨迹。' }}</p>
        <div class="empty-actions">
          <el-button v-if="hasActiveFilters" type="success" @click="clearFilters">清除筛选</el-button>
          <el-button v-else type="success" @click="handleCreate"><el-icon><EditPen /></el-icon>记录此刻</el-button>
          <el-button v-if="!hasActiveFilters" plain @click="goHome">去首页看看</el-button>
        </div>
      </div>

      <div class="timeline-item" v-for="group in groupedTimelines" :key="group.key">
        <div class="date-label">
          <span class="year">{{ group.year }}</span>
          <span class="date">{{ group.month }}.{{ group.day }}</span>
        </div>
        
        <div class="dot"></div>
        
        <div class="event-list">
          <div
            class="event-card"
            :class="{ 'search-highlight': highlightedRecordId === String(item.id) }"
            v-for="item in group.items"
            :key="item.id"
            :id="`record-${item.id}`"
            role="button"
            tabindex="0"
            @click="openDetail(item)"
            @keydown.enter="openDetail(item)"
          >
            <div class="card-header">
              <div class="user-meta">
                <img :src="profileStore.userInfo?.avatar || defaultAvatar" class="avatar" alt="用户头像" />
                <div class="meta-text">
                  <span class="name">{{ profileStore.userInfo?.username }}</span>
                  <span class="time">{{ formatRecordDateTime(item) }}</span>
                </div>
              </div>
              <el-button
                text
                type="danger"
                size="small"
                class="delete-checkbox"
                aria-label="删除这条事记"
                @click.stop="handleDelete(item.id)"
              >删除</el-button>
            </div>

            <div class="card-body">
              <div class="info-row">
                <span class="label">事记：</span>
                <span class="value content-preview">{{ getContentPreview(item.content) }}</span>
              </div>
              <div class="info-row" v-if="item.eventType">
                <span class="label">事件类型：</span>
                <span class="value event-type">{{ item.eventType }}</span>
              </div>
              <div class="info-row" v-if="item.keywords && item.keywords.length">
                <span class="label">关键词：</span>
                <span class="value">
                  <span class="keyword" v-for="(kw, i) in item.keywords" :key="i">{{ kw }}</span>
                </span>
              </div>
              <div class="info-row" v-if="item.moods && item.moods.length">
                <span class="label">心情：</span>
                <span class="value moods">
                  <span v-for="(mood, i) in item.moods" :key="i">{{ mood }}</span>
                </span>
              </div>
              
              <div :class="['photos', `photos--${Math.min(item.photos.length, 9)}`]" v-if="item.photos && item.photos.length">
                <img v-for="(photo, i) in item.photos" :key="i" :src="photo" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="记录详情" width="760px" class="record-detail-dialog">
      <article v-if="selectedItem" class="record-detail">
        <header class="detail-header">
          <div>
            <p class="detail-date">{{ selectedItem.date }}</p>
            <h4>{{ selectedItem.title || '这一天的记录' }}</h4>
          </div>
          <span v-if="selectedItem.moods?.length" class="detail-mood">{{ selectedItem.moods.join(' ') }}</span>
        </header>

        <p class="detail-content">{{ selectedItem.content }}</p>

        <div v-if="selectedItem.keywords?.length" class="detail-keywords">
          <el-tag v-for="keyword in selectedItem.keywords" :key="keyword" type="success" effect="plain">
            {{ keyword }}
          </el-tag>
        </div>

        <div v-if="selectedItem.photos?.length" :class="['detail-photos', `detail-photos--${Math.min(selectedItem.photos.length, 9)}`]">
          <el-image
            v-for="(photo, index) in selectedItem.photos"
            :key="photo"
            :src="photo"
            :preview-src-list="selectedItem.photos"
            :initial-index="index"
            fit="cover"
            preview-teleported
          />
        </div>
      </article>
    </el-dialog>

    <div v-if="profileStore.timelinePagination.total > 0" class="timeline-pagination">
      <el-pagination
        background
        :current-page="currentPage"
        :page-size="PAGE_SIZE"
        :total="profileStore.timelinePagination.total"
        layout="total, prev, pager, next"
        @current-change="changePage"
      />
    </div>

    <!-- 弹窗：新增/编辑时间轴 -->
    <el-dialog
      :title="dialogType === 'create' ? '新增时间轴' : '编辑时间轴'"
      v-model="dialogVisible"
      width="min(94vw, 1040px)"
      class="record-dialog"
    >
      <div class="record-stage" :style="{ backgroundImage: `url(${recordBanner})` }">
        <div class="record-banner">
        <div>
          <p>EARTH MOMENT</p>
          <h4>让今天成为你在地球上的一段轨迹</h4>
        </div>
      </div>
      <el-form class="record-form" :model="formData" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="日期" prop="date">
          <el-date-picker v-model="formData.date" type="date" value-format="YYYY-MM-DD" disabled />
        </el-form-item>
        <el-form-item label="事件类型" prop="eventType">
          <el-select v-model="formData.eventType" class="event-type-select" placeholder="请选择事件类型">
            <el-option v-for="type in eventTypeOptions" :key="type" :label="type" :value="type" />
          </el-select>
        </el-form-item>
        <el-form-item label="事记" prop="content">
          <div class="content-input-wrap">
            <el-input
              v-model="formData.content"
              type="textarea"
              placeholder="说说今天发生了什么"
              :maxlength="1500"
              :show-word-limit="true"
              @update:model-value="limitContentLength"
              rows="6"
            />
          </div>
        </el-form-item>
        <el-form-item label="关键词" class="keyword-form-item">
          <div style="display: flex; gap: 8px;">
            <el-input
              v-model="keywordInput"
              placeholder="输入关键词并回车或点击添加"
              size="small"
              :disabled="formData.keywords.length >= 5"
              @keyup.enter="addKeyword"
              style="width: 200px"
            />
            <el-button size="small" :disabled="formData.keywords.length >= 5" @click="addKeyword">添加</el-button>
          </div>
          <div v-if="formData.keywords.length" class="keyword-list">
            <el-tag
              v-for="(kw, index) in formData.keywords"
              :key="index"
              closable
              @close="removeKeyword(index)"
            >
              {{ kw }}
            </el-tag>
          </div>
        </el-form-item>
        <el-form-item label="心情">
          <el-select
            v-model="formData.mood"
            class="mood-select"
            popper-class="mood-popper"
            placeholder="选择或输入emoji表情"
          >
            <el-option v-for="emoji in moodOptions" :key="emoji" :label="emoji" :value="emoji" />
          </el-select>
        </el-form-item>
        <el-form-item label="图片">
          <div class="photo-upload-area">
            <input ref="photoInput" class="photo-input" type="file" accept="image/jpeg,image/png,image/webp" multiple @change="handlePhotoSelect" />
            <el-button plain type="success" :disabled="formData.photos.length >= 9" @click="photoInput?.click()">
              <el-icon><Picture /></el-icon>{{ formData.photos.length ? `继续添加（${formData.photos.length}/9）` : '添加图片' }}
            </el-button>
            <span class="upload-tip">JPG、PNG、WebP，单张不超过 5MB</span>
            <div v-if="formData.photos.length" :class="['photo-grid', `photo-grid--${formData.photos.length}`]">
              <div v-for="(photo, index) in formData.photos" :key="photo.objectKey || photo.preview" class="preview-item">
                <img :src="photo.preview" :alt="`记录图片 ${index + 1}`" />
                <button type="button" :aria-label="`删除图片 ${index + 1}`" @click="removePhoto(index)">×</button>
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import defaultAvatar from '@/assets/picture/default-avatar.png'
import { ref, reactive, computed, nextTick, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useProfileStore } from '@/stores/profile'
import { ElMessage, ElMessageBox } from 'element-plus'
import { uploadRecordImage } from '@/api/modules/uploads'
import recordBanner from '@/assets/picture/timeline-record-banner.png'

const props = defineProps({
  targetRecordId: { type: String, default: '' }
})

const profileStore = useProfileStore()
const route = useRoute()
const router = useRouter()
const selectedYear = ref('all')
const selectedTag = ref('')
const selectedEventType = ref('')
const currentPage = ref(1)
const PAGE_SIZE = 10
const photoInput = ref(null)
const detailVisible = ref(false)
const selectedItem = ref(null)
const highlightedRecordId = ref('')
let highlightTimer = null

watch(
  () => [props.targetRecordId, profileStore.timelines.length],
  async ([recordId]) => {
    if (!recordId) return
    await nextTick()
    const element = document.getElementById(`record-${recordId}`)
    if (!element) return
    highlightedRecordId.value = String(recordId)
    element.scrollIntoView({ behavior: 'smooth', block: 'center' })
    if (highlightTimer) window.clearTimeout(highlightTimer)
    highlightTimer = window.setTimeout(() => { highlightedRecordId.value = '' }, 3000)
  },
  { immediate: true }
)
onBeforeUnmount(() => {
  if (highlightTimer) window.clearTimeout(highlightTimer)
})

const getToday = () => {
  const today = new Date()
  const year = today.getFullYear()
  const month = String(today.getMonth() + 1).padStart(2, '0')
  const day = String(today.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const getContentPreview = (content) => {
  const normalized = String(content || '').replace(/\s+/g, ' ').trim()
  return normalized.length > 120 ? `${normalized.slice(0, 120)}…` : normalized
}

const openDetail = (item) => {
  selectedItem.value = item
  detailVisible.value = true
}
const moodOptions = ['😊', '😌', '🥰', '🤔', '🥳', '😴', '😢', '😤', '😔', '😰', '😎', '😶', '😡', '😍', '😳', '🤩']

const eventTypeOptions = ['生活日常', '学习成长', '工作 / 学业', '健康运动', '旅行探索', '人际关系', '其他']

const yearOptions = computed(() => profileStore.timelineFilterOptions.years)
const tagOptions = computed(() => profileStore.timelineFilterOptions.tags)
const filterEventTypeOptions = computed(() => profileStore.timelineFilterOptions.eventTypes)
const hasActiveFilters = computed(() => selectedYear.value !== 'all' || Boolean(selectedTag.value) || Boolean(selectedEventType.value))

const parsePositiveInteger = (value, fallback) => {
  const parsed = Number.parseInt(String(value || ''), 10)
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback
}

const loadTimelinePage = async () => {
  try {
    await profileStore.fetchTimelinePage({
      page: currentPage.value,
      pageSize: PAGE_SIZE,
      year: selectedYear.value === 'all' ? '' : selectedYear.value,
      tag: selectedTag.value,
      eventType: selectedEventType.value
    })
  } catch (error) {
    ElMessage.error(error?.message || '时间线加载失败，请稍后重试')
  }
}

const replaceFilterQuery = (page = 1) => {
  const query = { ...route.query }
  delete query.record
  if (selectedYear.value === 'all') delete query.year
  else query.year = selectedYear.value
  if (selectedTag.value) query.tag = selectedTag.value
  else delete query.tag
  if (selectedEventType.value) query.eventType = selectedEventType.value
  else delete query.eventType
  if (page > 1) query.page = String(page)
  else delete query.page
  delete query.pageSize
  return router.replace({ query })
}

const applyFilters = () => replaceFilterQuery(1)
const clearFilters = () => {
  selectedYear.value = 'all'
  selectedTag.value = ''
  selectedEventType.value = ''
  return replaceFilterQuery(1)
}
const changePage = (page) => replaceFilterQuery(page)

watch(
  () => route.query,
  async (query) => {
    selectedYear.value = /^\d{4}$/.test(String(query.year || '')) ? String(query.year) : 'all'
    selectedTag.value = typeof query.tag === 'string' ? query.tag : ''
    selectedEventType.value = typeof query.eventType === 'string' ? query.eventType : ''
    currentPage.value = parsePositiveInteger(query.page, 1)
    await loadTimelinePage()
  },
  { immediate: true }
)

const parseDateParts = (dateStr) => {
  const date = new Date(dateStr)
  if (Number.isNaN(date.getTime())) return null
  return {
    year: date.getFullYear(),
    month: date.getMonth() + 1,
    day: date.getDate()
  }
}

const pad2 = (value) => String(value).padStart(2, '0')

const getRecordTimestamp = (item) => {
  const createdAt = new Date(item?.createdAt).getTime()
  if (Number.isFinite(createdAt)) return createdAt
  const recordDate = new Date(item?.date).getTime()
  return Number.isFinite(recordDate) ? recordDate : 0
}

const formatRecordDateTime = (item) => {
  const date = new Date(item?.createdAt)
  if (Number.isNaN(date.getTime())) return item?.date || ''
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())} ${pad2(date.getHours())}:${pad2(date.getMinutes())}`
}

const groupedTimelines = computed(() => {
  const groups = new Map()
  const sorted = [...profileStore.timelines].sort((a, b) => getRecordTimestamp(b) - getRecordTimestamp(a))

  sorted.forEach((item) => {
    const parts = parseDateParts(item.date)
    if (!parts) return
    const key = `${parts.year}-${pad2(parts.month)}-${pad2(parts.day)}`
    if (!groups.has(key)) {
      groups.set(key, {
        key,
        year: parts.year,
        month: pad2(parts.month),
        day: pad2(parts.day),
        items: []
      })
    }
    groups.get(key).items.push(item)
  })

  return Array.from(groups.values())
})

const dialogVisible = ref(false)
const dialogType = ref('create') // 'create' or 'edit'

const formData = reactive({
  id: '',
  date: '',
  eventType: '生活日常',
  content: '',
  keywords: [],
  mood: '',
  moods: [],
  photos: []
})

const keywordInput = ref('')
const formRef = ref(null)

// maxlength 会限制正常键盘输入；这里额外处理粘贴内容，确保任何输入方式都不能超过 1500 字。
const limitContentLength = (value) => {
  const content = String(value ?? '')
  if (content.length > 1500) {
    formData.content = content.slice(0, 1500)
    return
  }
  formData.content = content
}

const rules = {
  date: [{ required: true, message: '请选择日期', trigger: 'change' }],
  eventType: [{ required: true, message: '请选择事件类型', trigger: 'change' }],
  content: [{ required: true, message: '请输入事记内容', trigger: 'blur' }]
}

const handleCreate = () => {
  dialogType.value = 'create'
  Object.assign(formData, {
    id: '',
    date: getToday(),
    eventType: '生活日常',
    content: '',
    keywords: [],
    mood: '',
    moods: [],
    photos: []
  })
  keywordInput.value = ''
  dialogVisible.value = true
}

const goHome = () => router.push('/')

const handleEdit = (item) => {
  dialogType.value = 'edit'
  Object.assign(formData, JSON.parse(JSON.stringify(item)))
  formData.eventType = item.eventType || '生活日常'
  formData.mood = item.moods?.[0] || ''
  formData.photos = (item.photos || []).map((preview, index) => ({
    preview,
    objectKey: item.photoObjectKeys?.[index] || ''
  }))
  keywordInput.value = ''
  dialogVisible.value = true
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该时间轴记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await profileStore.deleteTimeline(id)
    if (profileStore.timelines.length === 0 && currentPage.value > 1) {
      await replaceFilterQuery(currentPage.value - 1)
    } else {
      await loadTimelinePage()
    }
    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || '删除失败，请重试')
    }
  }
}

const submitForm = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    const photoObjectKeys = []
    for (const photo of formData.photos) {
      if (photo.objectKey) {
        photoObjectKeys.push(photo.objectKey)
      } else {
        const result = await uploadRecordImage(photo.file)
        photoObjectKeys.push(result.objectKey)
      }
    }

    const payload = {
      ...formData,
      photoObjectKeys,
      moods: formData.mood ? [formData.mood] : []
    }

    if (dialogType.value === 'create') {
      await profileStore.addTimeline({ ...payload, id: formData.id || `t-${Date.now()}` })
      ElMessage.success('添加成功')
    } else {
      await profileStore.updateTimeline(payload)
      ElMessage.success('修改成功')
    }
    await loadTimelinePage()
    dialogVisible.value = false
  } catch (error) {
    ElMessage.error(error?.message || '图片或记录保存失败，请重试')
  }
}

const addKeyword = () => {
  if (formData.keywords.length >= 5) {
    ElMessage.warning('最多添加 5 个关键词')
    return
  }
  if (keywordInput.value.trim() && !formData.keywords.includes(keywordInput.value.trim())) {
    formData.keywords.push(keywordInput.value.trim())
  }
  keywordInput.value = ''
}
const removeKeyword = (index) => {
  formData.keywords.splice(index, 1)
}

const handlePhotoSelect = (event) => {
  const files = Array.from(event.target.files || [])
  const remaining = 9 - formData.photos.length

  files.slice(0, remaining).forEach((file) => {
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
      ElMessage.warning('只支持 JPG、PNG、WebP 图片')
      return
    }
    if (file.size > 5 * 1024 * 1024) {
      ElMessage.warning('单张图片不能超过 5MB')
      return
    }
    formData.photos.push({
      preview: URL.createObjectURL(file),
      file,
      objectKey: ''
    })
  })

  if (files.length > remaining) ElMessage.warning('每条记录最多添加 9 张图片')
  event.target.value = ''
}

const removePhoto = (index) => {
  const [removed] = formData.photos.splice(index, 1)
  if (removed?.preview?.startsWith('blob:')) URL.revokeObjectURL(removed.preview)
}
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
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;

  .title {
    display: flex;
    align-items: center;
    gap: 8px;
    h3 { margin: 0; font-size: 18px; color: $text-main; }
    .icon { color: $primary-color; font-size: 18px; }
  }

  .actions {
    display: flex;
    gap: 12px;
    align-items: center;

    .filter-select { width: 118px; }
    .event-filter { width: 132px; }
    .create-button { margin-left: 0; }
  }
}

.timeline-wrapper {
  position: relative;
  padding-left: 80px;

  .timeline-line {
    position: absolute;
    left: 87px;
    top: 10px;
    bottom: 0;
    width: 2px;
    background-color: rgba(102, 194, 138, 0.3);
  }
}

.timeline-item {
  position: relative;
  margin-bottom: 32px;
  display: flex;
  min-height: 120px;

  .date-label {
    position: absolute;
    left: -80px;
    top: -2px;
    width: 60px;
    text-align: right;
    display: flex;
    flex-direction: column;
    
    .year {
      font-size: 16px;
      font-weight: bold;
      color: $text-main;
    }
    .date {
      font-size: 12px;
      color: $text-secondary;
    }
  }

  .dot {
    position: absolute;
    left: 3px;
    top: 4px;
    width: 12px;
    height: 12px;
    border-radius: 50%;
    background-color: $primary-color;
    border: 2px solid #fff;
    box-shadow: 0 0 0 2px rgba(102, 194, 138, 0.3);
    z-index: 2;
  }
}

.event-list {
  margin-left: 32px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
  min-width: 0;
}

.event-card {
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
  overflow: hidden;
  background: #fdfdfd;
  border: 1px solid $border-color;
  border-radius: 14px;
  padding: 18px 20px;
  transition: all 0.25s ease;

  &.search-highlight {
    border-color: $primary-color;
    background: rgba(102,194,138,.1);
    box-shadow: 0 0 0 4px rgba(102,194,138,.14), 0 8px 22px rgba(62,126,85,.12);
  }

  &:hover {
    box-shadow: 0 4px 12px rgba(0,0,0,0.05);
    transform: translateY(-1px);
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;

    .user-meta {
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .avatar {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      object-fit: cover;
    }

    .meta-text {
      display: flex;
      flex-direction: column;
      gap: 2px;
    }

    .name {
      font-size: 14px;
      color: $text-main;
      font-weight: 500;
    }

    .time {
      font-size: 12px;
      color: $text-secondary;
    }

    .delete-checkbox {
      padding: 4px 8px;
    }
  }

  .card-body {
    min-width: 0;

    .info-row {
      display: flex;
      min-width: 0;
      margin-bottom: 10px;
      font-size: 14px;
      .label { flex: 0 0 76px; color: $text-secondary; }
      .value { min-width: 0; color: $text-main; overflow-wrap: anywhere; }
      .event-type { color: $primary-color; }
      
      .keyword {
        margin-right: 12px;
        color: $primary-color;
      }
      
      .moods span {
        margin-right: 8px; font-size: 16px;
      }
    }

    .content-preview {
      display: block;
      max-width: min(100%, 860px);
      overflow: hidden;
      vertical-align: bottom;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .photos {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 8px;
      width: 352px;
      max-width: 100%;
      max-height: 352px;
      margin-top: 16px;
      overflow: hidden;
      img {
        display: block;
        width: 100%;
        min-width: 0;
        aspect-ratio: 1;
        object-fit: cover;
        border-radius: 8px;
      }
    }
    .photos--1 { grid-template-columns: minmax(0, 1fr); width: 180px; }
    .photos--2 { grid-template-columns: repeat(2, minmax(0, 1fr)); width: 296px; }
  }
}

.record-detail {
  .detail-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 18px;
    padding-bottom: 16px;
    border-bottom: 1px solid $border-color;
  }

  .detail-date { margin: 0 0 5px; color: $text-secondary; font-size: 13px; }
  h4 { margin: 0; color: $text-main; font-size: 21px; }
  .detail-mood { font-size: 25px; line-height: 1; }
  .detail-content { margin: 20px 0; color: $text-regular; font-size: 15px; line-height: 1.9; white-space: pre-wrap; word-break: break-word; }
  .detail-keywords { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 18px; }
}

.detail-photos {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;

  :deep(.el-image) { width: 100%; aspect-ratio: 1; border-radius: 10px; cursor: zoom-in; }
}

.detail-photos--1 { grid-template-columns: minmax(0, 460px); }
.detail-photos--2 { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.detail-photos--3 { grid-template-columns: 1.45fr 1fr; }
.detail-photos--3 :deep(.el-image:first-child) { grid-row: span 2; }

.empty-state {
  display: flex;
  min-height: 370px;
  padding: 28px 20px 16px;
  align-items: center;
  flex-direction: column;
  justify-content: center;
  text-align: center;

  .empty-orbit {
    position: relative;
    width: 96px;
    height: 96px;
    margin-bottom: 14px;
    border: 2px dashed rgba(102, 194, 138, .38);
    border-radius: 50%;

    span { position: absolute; inset: 24px; border-radius: 50%; background: rgba(102, 194, 138, .18); }
    i { position: absolute; top: 7px; right: 11px; width: 13px; height: 13px; border-radius: 50%; background: $primary-color; box-shadow: 0 0 0 5px #fff; }
    b { position: absolute; right: 20px; bottom: 17px; width: 7px; height: 7px; border-radius: 50%; background: #f1c879; }
  }

  .empty-kicker { margin: 0 0 6px; color: $primary-color; font-size: 11px; font-weight: 700; letter-spacing: 1px; }
  h4 { margin: 0; color: $text-main; font-size: 20px; }
  .empty-copy { max-width: 420px; margin: 12px 0 18px; color: $text-secondary; font-size: 14px; line-height: 1.7; }
  .empty-actions { display: flex; gap: 10px; }
}

.timeline-pagination {
  display: flex;
  margin-top: 24px;
  justify-content: center;
}

:deep(.record-dialog .el-dialog__header) {
  margin-right: 0;
  padding: 20px 24px 12px;
}

:deep(.record-dialog .el-dialog__body) {
  padding: 0 24px 12px;
}

.record-stage {
  position: relative;
  overflow: hidden;
  padding: 24px;
  border-radius: 16px;
  background-color: #eef8f0;
  background-repeat: no-repeat;
  background-position: center;
  background-size: cover;

  &::before {
    position: absolute;
    inset: 0;
    content: '';
    background: linear-gradient(100deg, rgba(255, 255, 255, .92) 0%, rgba(255, 255, 255, .78) 53%, rgba(255, 255, 255, .5) 100%);
  }
}

.record-banner {
  position: relative;
  z-index: 1;
  display: flex;
  min-height: 84px;
  padding: 4px 4px 18px;
  align-items: center;
  p { margin: 0 0 7px; color: #4fa875; font-size: 11px; font-weight: 700; letter-spacing: 1px; }
  h4 { max-width: 320px; margin: 0; color: $text-main; font-size: 20px; line-height: 1.45; }
}

.record-form {
  position: relative;
  z-index: 1;
  padding: 20px 18px 4px;
  border: 1px solid rgba(255, 255, 255, .78);
  border-radius: 14px;
  background: rgba(255, 255, 255, .72);
  box-shadow: 0 12px 26px rgba(89, 133, 100, .12);
  backdrop-filter: blur(8px);

  :deep(.el-form-item__label) { color: $text-regular; }
  :deep(.el-textarea__inner), :deep(.el-input__wrapper) {
    border-radius: 10px;
    background: rgba(255, 255, 255, .88);
  }
}

.event-type-select {
  width: min(100%, 320px);
}

.content-input-wrap {
  position: relative;
  width: 100%;

  :deep(.el-textarea__inner) { min-height: 148px !important; padding-bottom: 28px; }
}

.mood-select { width: 100%; }
.mood-select :deep(.el-select__selected-item) { font-size: 22px; }
.mood-select :deep(.el-select__placeholder) { font-size: 14px; }
:deep(.keyword-form-item .el-form-item__content) { display: block; }
.keyword-list { display: flex; width: 100%; flex-wrap: wrap; gap: 8px; margin-top: 10px; }

:global(.mood-popper .el-select-dropdown__wrap) { max-height: 260px; }
:global(.mood-popper .el-select-dropdown__list) {
  display: grid;
  grid-template-columns: repeat(6, 42px);
  gap: 7px;
  padding: 10px;
}
:global(.mood-popper .el-select-dropdown__item) {
  display: grid;
  width: 42px;
  height: 42px;
  padding: 0;
  place-items: center;
  border-radius: 10px;
  font-size: 23px;
  line-height: 1;
}
:global(.mood-popper .el-select-dropdown__item:hover),
:global(.mood-popper .el-select-dropdown__item.is-selected) { background: #edf8f1; }

.photo-upload-area { width: 100%; }
.photo-input { display: none; }
.upload-tip { margin-left: 10px; color: $text-secondary; font-size: 12px; }
.photo-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  max-width: 420px;
  margin-top: 12px;
}
.photo-grid--1 { grid-template-columns: minmax(0, 220px); }
.photo-grid--2 { grid-template-columns: repeat(2, minmax(0, 160px)); }
.photo-grid--3 { grid-template-columns: 1.45fr 1fr; }
.preview-item {
  position: relative;
  overflow: hidden;
  min-width: 0;
  aspect-ratio: 1;
  border-radius: 9px;
  background: #f3f7f4;

  img { width: 100%; height: 100%; object-fit: cover; }
  button {
    position: absolute;
    top: 5px;
    right: 5px;
    width: 22px;
    height: 22px;
    border: 0;
    border-radius: 50%;
    color: #fff;
    background: rgba(35, 45, 39, .62);
    cursor: pointer;
  }
}
.photo-grid--1 .preview-item { aspect-ratio: 4 / 3; }
.photo-grid--3 .preview-item:first-child { grid-row: span 2; }

@media (max-width: 640px) {
  .module-card { padding: 16px; }
  .module-header {
    align-items: stretch;
    flex-direction: column;
    gap: 14px;
    margin-bottom: 18px;

    .title h3 { white-space: nowrap; }
    .actions {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      width: 100%;
    }
    .actions .filter-select { width: 100%; }
    .actions .create-button { width: 100%; }
  }

  .timeline-wrapper { padding-left: 0; }
  .timeline-wrapper .timeline-line { left: 6px; }
  .timeline-item {
    padding-left: 26px;
    flex-direction: column;

    .date-label {
      position: static;
      width: auto;
      margin-bottom: 8px;
      align-items: baseline;
      flex-direction: row;
      gap: 6px;
      text-align: left;
    }
    .dot { left: 0; top: 5px; }
  }
  .event-list { width: auto; margin-left: 0; }
  .event-card { padding: 15px 14px; }
  .empty-state {
    min-height: 340px;
    padding: 20px 4px 10px;
  }
  .empty-state .empty-copy { margin-top: 10px; }
  .empty-state .empty-actions {
    width: min(100%, 260px);
    flex-direction: column;
  }
  .empty-state .empty-actions :deep(.el-button) {
    width: 100%;
    margin-left: 0;
  }

  :deep(.record-dialog) { width: calc(100% - 24px) !important; }
  .record-stage { padding: 18px 12px; background-position: 68% center; }
  .record-banner { min-height: 70px; padding: 2px 6px 14px; }
  .record-banner h4 { max-width: 230px; font-size: 17px; }
  .record-form { padding: 16px 12px 2px; }
  .upload-tip { display: block; margin: 8px 0 0; }
  .photo-grid { max-width: 100%; }
  .timeline-pagination { overflow-x: auto; justify-content: flex-start; }
  .timeline-pagination :deep(.el-pagination__total) { display: none; }
}
</style>
