<template>
  <div class="timeline-container module-card">
    <div class="module-header">
      <div class="title">
        <h3>我的时间轴</h3>
        <el-icon class="icon"><MagicStick /></el-icon>
      </div>
      <div class="actions">
        <el-button-group>
          <el-button type="success" plain size="small" class="active-btn">
            <el-icon><Operation /></el-icon> 时间轴视图
          </el-button>
          <el-button type="success" plain size="small">
            <el-icon><Calendar /></el-icon> 日历视图
          </el-button>
        </el-button-group>
        <el-select style="width: 100px" size="small" placeholder="全部年份" v-model="selectedYear">
          <el-option label="全部年份" value="all" />
          <el-option v-for="year in yearOptions" :key="year" :label="year" :value="String(year)" />
        </el-select>
        <el-button type="primary" size="small" @click="handleCreate" style="margin-left: 8px;">
          <el-icon><Plus /></el-icon> 新增
        </el-button>
      </div>
    </div>

    <div class="timeline-wrapper">
      <div class="timeline-line"></div>
      
      <div v-if="!groupedTimelines.length" class="empty-state">
        <div class="empty-orbit" aria-hidden="true">
          <span></span><i></i><b></b>
        </div>
        <p class="empty-kicker">从第一条记录开始</p>
        <h4>你的时间线还没有开始</h4>
        <p class="empty-copy">记录今天的心情、想法或一件小事，让它成为你在地球上的第一段轨迹。</p>
        <div class="empty-actions">
          <el-button type="success" @click="handleCreate"><el-icon><EditPen /></el-icon>记录此刻</el-button>
          <el-button plain @click="goHome">去首页看看</el-button>
        </div>
      </div>

      <div class="timeline-item" v-for="group in groupedTimelines" :key="group.key">
        <div class="date-label">
          <span class="year">{{ group.year }}</span>
          <span class="date">{{ group.month }}.{{ group.day }}</span>
        </div>
        
        <div class="dot"></div>
        
        <div class="event-list">
          <div class="event-card" v-for="item in group.items" :key="item.id">
            <div class="card-header">
              <div class="user-meta">
                <img :src="profileStore.userInfo?.avatar" class="avatar" />
                <div class="meta-text">
                  <span class="name">{{ profileStore.userInfo?.username }}</span>
                  <span class="time">{{ group.year }}-{{ pad2(group.month) }}-{{ pad2(group.day) }}</span>
                </div>
              </div>
              <el-dropdown trigger="click">
                <span class="el-dropdown-link" style="display: flex; align-items: center; gap: 4px; cursor: pointer;">
                  <el-icon><MoreFilled /></el-icon>
                </span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="handleEdit(item)">编辑</el-dropdown-item>
                    <el-dropdown-item divided type="danger" @click="handleDelete(item.id)">删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>

            <div class="card-body">
              <div class="info-row">
                <span class="label">事记：</span>
                <span class="value">{{ item.content }}</span>
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

    <div v-if="groupedTimelines.length" class="load-more">
      <el-button type="success" plain round size="small">
        <el-icon><ArrowDown /></el-icon> 查看更多年份
      </el-button>
    </div>

    <!-- 弹窗：新增/编辑时间轴 -->
    <el-dialog
      :title="dialogType === 'create' ? '新增时间轴' : '编辑时间轴'"
      v-model="dialogVisible"
      width="760px"
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
          <el-date-picker v-model="formData.date" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="事记" prop="content">
          <el-input type="textarea" v-model="formData.content" placeholder="说说今天发生了什么" rows="3" />
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
              <div v-for="(photo, index) in formData.photos" :key="photo" class="preview-item">
                <img :src="photo" :alt="`记录图片 ${index + 1}`" />
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
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useProfileStore } from '@/stores/profile'
import { ElMessage, ElMessageBox } from 'element-plus'
import recordBanner from '@/assets/picture/timeline-record-banner.png'

const props = defineProps({
  timelines: Array
})

const profileStore = useProfileStore()
const router = useRouter()
const selectedYear = ref('all')
const photoInput = ref(null)
const moodOptions = ['😊', '😌', '🥰', '🤔', '🥳', '😴', '😢', '😤', '😔', '😰', '😎', '😶', '😡', '😍', '😳', '🤩']

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

const filteredTimelines = computed(() => {
  if (selectedYear.value === 'all') {
    return props.timelines
  }
  return props.timelines.filter((item) => {
    const parts = parseDateParts(item.date)
    return parts ? String(parts.year) === selectedYear.value : false
  })
})

const yearOptions = computed(() => {
  const years = new Set()
  props.timelines.forEach((item) => {
    const parts = parseDateParts(item.date)
    if (parts) years.add(parts.year)
  })
  return Array.from(years).sort((a, b) => b - a)
})

const groupedTimelines = computed(() => {
  const groups = new Map()
  const sorted = [...filteredTimelines.value].sort((a, b) => {
    return new Date(b.date) - new Date(a.date)
  })

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
  content: '',
  keywords: [],
  mood: '',
  moods: [],
  photos: []
})

const keywordInput = ref('')
const formRef = ref(null)

const rules = {
  date: [{ required: true, message: '请选择日期', trigger: 'change' }],
  content: [{ required: true, message: '请输入事记内容', trigger: 'blur' }]
}

const handleCreate = () => {
  dialogType.value = 'create'
  Object.assign(formData, {
    id: '',
    date: new Date().toISOString().slice(0, 10),
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
  formData.mood = item.moods?.[0] || ''
  keywordInput.value = ''
  dialogVisible.value = true
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确定要删除该时间轴记录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    profileStore.deleteTimeline(id)
    ElMessage.success('删除成功')
  }).catch(() => {})
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate((valid) => {
    if (valid) {
      if (dialogType.value === 'create') {
        profileStore.addTimeline({
          ...formData,
          moods: formData.mood ? [formData.mood] : [],
          id: formData.id || `t-${Date.now()}`
        })
        ElMessage.success('添加成功')
      } else {
        profileStore.updateTimeline({ ...formData, moods: formData.mood ? [formData.mood] : [] })
        ElMessage.success('修改成功')
      }
      dialogVisible.value = false
    }
  })
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
    formData.photos.push(URL.createObjectURL(file))
  })

  if (files.length > remaining) ElMessage.warning('每条记录最多添加 9 张图片')
  event.target.value = ''
}

const removePhoto = (index) => {
  const [removed] = formData.photos.splice(index, 1)
  if (removed?.startsWith('blob:')) URL.revokeObjectURL(removed)
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
    
    .active-btn {
      background-color: rgba(102, 194, 138, 0.1);
    }
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
}

.event-card {
  background: #fdfdfd;
  border: 1px solid $border-color;
  border-radius: 14px;
  padding: 18px 20px;
  transition: all 0.25s ease;

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
  }

  .card-body {
    .info-row {
      margin-bottom: 10px;
      font-size: 14px;
      .label { color: $text-secondary; width: 70px; display: inline-block; }
      .value { color: $text-main; }
      
      .keyword {
        margin-right: 12px;
        color: $primary-color;
      }
      
      .moods span {
        margin-right: 8px; font-size: 16px;
      }
    }

    .photos {
      display: grid; grid-template-columns: repeat(3, minmax(0, 96px)); gap: 8px; margin-top: 16px;
      img {
        width: 96px; height: 76px; object-fit: cover;
        border-radius: 8px;
      }
    }
    .photos--1 { grid-template-columns: 260px; img { width: 260px; height: 180px; } }
    .photos--2 { grid-template-columns: repeat(2, 150px); img { width: 150px; height: 120px; } }
    .photos--3 {
      grid-template-columns: 1.45fr 1fr;
      max-width: 330px;
      img { width: 100%; }
      img:first-child { grid-row: span 2; height: 160px; }
    }
  }
}

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

.load-more {
  text-align: center;
  margin-top: 20px;
  
  .el-button {
    width: 200px;
  }
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
  :deep(.record-dialog) { width: calc(100% - 24px) !important; }
  .record-stage { padding: 18px 12px; background-position: 68% center; }
  .record-banner { min-height: 70px; padding: 2px 6px 14px; }
  .record-banner h4 { max-width: 230px; font-size: 17px; }
  .record-form { padding: 16px 12px 2px; }
  .upload-tip { display: block; margin: 8px 0 0; }
  .photo-grid { max-width: 100%; }
}
</style>
