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
        <el-empty description="暂无记录" />
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
                  <span class="name">{{ profileStore.userInfo?.nickname }}</span>
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
              
              <div class="photos" v-if="item.photos && item.photos.length">
                <img v-for="(photo, i) in item.photos" :key="i" :src="photo" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="load-more">
      <el-button type="success" plain round size="small">
        <el-icon><ArrowDown /></el-icon> 查看更多年份
      </el-button>
    </div>

    <!-- 弹窗：新增/编辑时间轴 -->
    <el-dialog
      :title="dialogType === 'create' ? '新增时间轴' : '编辑时间轴'"
      v-model="dialogVisible"
      width="500px"
    >
      <el-form :model="formData" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="日期" prop="date">
          <el-date-picker v-model="formData.date" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="事记" prop="content">
          <el-input type="textarea" v-model="formData.content" placeholder="说说今天发生了什么" rows="3" />
        </el-form-item>
        <el-form-item label="关键词">
          <div style="display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 8px;">
            <el-tag
              v-for="(kw, index) in formData.keywords"
              :key="index"
              closable
              @close="removeKeyword(index)"
            >
              {{ kw }}
            </el-tag>
          </div>
          <div style="display: flex; gap: 8px;">
            <el-input
              v-model="keywordInput"
              placeholder="输入关键词并回车或点击添加"
              size="small"
              @keyup.enter="addKeyword"
              style="width: 200px"
            />
            <el-button size="small" @click="addKeyword">添加</el-button>
          </div>
        </el-form-item>
        <el-form-item label="心情">
          <el-select
            v-model="formData.moods"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="选择或输入emoji表情"
          >
            <el-option label="😊" value="😊" />
            <el-option label="😁" value="😁" />
            <el-option label="🤩" value="🤩" />
            <el-option label="🧐" value="🧐" />
            <el-option label="😎" value="😎" />
            <el-option label="😍" value="😍" />
            <el-option label="🥰" value="🥰" />
          </el-select>
        </el-form-item>
      </el-form>
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
import { useProfileStore } from '@/store/profile'
import { ElMessage, ElMessageBox } from 'element-plus'

const props = defineProps({
  timelines: Array
})

const profileStore = useProfileStore()
const selectedYear = ref('all')

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
    moods: [],
    photos: []
  })
  keywordInput.value = ''
  dialogVisible.value = true
}

const handleEdit = (item) => {
  dialogType.value = 'edit'
  Object.assign(formData, JSON.parse(JSON.stringify(item)))
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
          id: formData.id || `t-${Date.now()}`
        })
        ElMessage.success('添加成功')
      } else {
        profileStore.updateTimeline({ ...formData })
        ElMessage.success('修改成功')
      }
      dialogVisible.value = false
    }
  })
}

const addKeyword = () => {
  if (keywordInput.value.trim() && !formData.keywords.includes(keywordInput.value.trim())) {
    formData.keywords.push(keywordInput.value.trim())
  }
  keywordInput.value = ''
}
const removeKeyword = (index) => {
  formData.keywords.splice(index, 1)
}
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

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
      display: flex; gap: 12px; margin-top: 16px;
      img {
        width: 80px; height: 60px; object-fit: cover;
        border-radius: 8px;
      }
    }
  }
}

.empty-state {
  padding: 24px 0 12px;
}

.load-more {
  text-align: center;
  margin-top: 20px;
  
  .el-button {
    width: 200px;
  }
}
</style>
