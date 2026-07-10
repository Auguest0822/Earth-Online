<template>
  <div class="capsule-view">
    <section class="page-head">
      <div>
        <h2>时间胶囊</h2>
        <p>给未来的自己写封信，把此刻认真封存起来。</p>
      </div>
      <div class="summary">
        <div class="summary-item">
          <span>{{ sealedCount }}</span>
          <small>等待开启</small>
        </div>
        <div class="summary-item">
          <span>{{ openedCount }}</span>
          <small>已开启</small>
        </div>
        <div class="summary-item">
          <span>{{ draftCount }}</span>
          <small>草稿</small>
        </div>
      </div>
    </section>

    <section class="compose-grid">
      <div class="letter-panel">
        <div class="panel-title">
          <div>
            <span class="eyebrow">Letter</span>
            <h3>写给未来的自己</h3>
          </div>
          <el-icon><EditPen /></el-icon>
        </div>

        <div class="paper">
          <div class="paper-row">
            <label>收件人</label>
            <el-input v-model="form.recipient" placeholder="未来的我" />
          </div>
          <div class="paper-row">
            <label>信件标题</label>
            <el-input v-model="form.title" placeholder="例如：写给一年后的自己" />
          </div>
          <el-input
            v-model="form.content"
            class="letter-input"
            type="textarea"
            :autosize="{ minRows: 9, maxRows: 12 }"
            placeholder="此刻的你想对未来说些什么？"
          />
        </div>

        <div class="mood-area">
          <div class="mood-title">此刻心情</div>
          <div class="mood-list">
            <button
              v-for="mood in moodOptions"
              :key="mood"
              class="mood-chip"
              :class="{ active: form.moods.includes(mood) }"
              @click="toggleMood(mood)"
            >
              {{ mood }}
            </button>
          </div>
          <el-input
            v-model="form.moodText"
            class="mood-input"
            clearable
            placeholder="也可以自己描述此刻心情"
          />
        </div>

        <div class="tag-area">
          <el-tag
            v-for="tag in form.tags"
            :key="tag"
            closable
            type="success"
            @close="removeTag(tag)"
          >
            {{ tag }}
          </el-tag>
          <el-input
            v-if="tagEditing"
            ref="tagInputRef"
            v-model="tagInput"
            class="tag-input"
            size="small"
            placeholder="输入标签"
            @keyup.enter="addTag"
            @blur="addTag"
          />
          <el-button v-else size="small" plain type="success" @click="showTagInput">
            <el-icon><Plus /></el-icon>
            添加标签
          </el-button>
        </div>
      </div>

      <aside class="settings-panel">
        <div class="panel-title compact">
          <div>
            <span class="eyebrow">Settings</span>
            <h3>胶囊设置</h3>
          </div>
          <el-icon><Lock /></el-icon>
        </div>

        <div class="setting-block">
          <label>开启日期</label>
          <el-date-picker
            v-model="form.openAt"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            :disabled-date="disableOpenDate"
            style="width: 100%"
          />
          <div class="quick-dates">
            <button
              v-for="preset in datePresets"
              :key="preset.label"
              @click="applyPreset(preset.days)"
            >
              {{ preset.label }}
            </button>
          </div>
        </div>

        <div class="setting-block">
          <label>隐私状态</label>
          <div class="segmented">
            <button :class="{ active: form.visibility === 'private' }" @click="form.visibility = 'private'">
              仅自己可见
            </button>
            <button :class="{ active: form.visibility === 'shareable' }" @click="form.visibility = 'shareable'">
              到期可分享
            </button>
          </div>
        </div>

        <div class="switch-row">
          <div>
            <strong>到期前允许编辑</strong>
            <span>关闭后封存期间不可修改</span>
          </div>
          <el-switch v-model="form.allowEditBeforeOpen" />
        </div>

        <div class="preview-card">
          <div class="preview-icon">
            <el-icon><MessageBox /></el-icon>
          </div>
          <div>
            <strong>{{ form.title || '给未来的一封信' }}</strong>
            <span>{{ openHint }}</span>
          </div>
        </div>

        <el-button class="seal-button" type="success" size="large" @click="sealCapsule">
          <el-icon><Finished /></el-icon>
          封存胶囊
        </el-button>
      </aside>
    </section>

    <section class="capsule-list-section">
      <div class="list-head">
        <div>
          <h3>我的胶囊</h3>
          <p>每一封信都在自己的时间里慢慢抵达。</p>
        </div>
        <div class="filters">
          <button
            v-for="filter in filters"
            :key="filter.value"
            :class="{ active: activeFilter === filter.value }"
            @click="activeFilter = filter.value"
          >
            {{ filter.label }}
          </button>
        </div>
      </div>

      <div class="capsule-grid">
        <article
          v-for="capsule in filteredCapsules"
          :key="capsule.id"
          class="capsule-card"
          :class="capsule.status"
        >
          <div class="card-top">
            <div class="state-icon">
              <el-icon>
                <component :is="statusMeta[capsule.status].icon" />
              </el-icon>
            </div>
            <span class="state-label">{{ statusMeta[capsule.status].label }}</span>
          </div>
          <h4>{{ capsule.title }}</h4>
          <p>{{ capsule.status === 'sealed' ? '信件已封存，开启前保持沉默。' : capsule.excerpt }}</p>
          <div v-if="capsule.mood" class="capsule-mood">{{ capsule.mood }}</div>
          <div class="card-meta">
            <span>
              <el-icon><Calendar /></el-icon>
              {{ capsule.openAt }}
            </span>
            <strong>{{ capsule.status === 'sealed' ? daysLeft(capsule.openAt) : capsule.action }}</strong>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

const moodOptions = ['期待', '平静', '勇敢', '迷茫', '快乐']
const datePresets = [
  { label: '明天', days: 1 },
  { label: '一个月后', days: 30 },
  { label: '一年后', days: 365 }
]
const filters = [
  { label: '全部', value: 'all' },
  { label: '等待开启', value: 'sealed' },
  { label: '已开启', value: 'opened' },
  { label: '草稿', value: 'draft' }
]
const statusMeta = {
  sealed: { label: '等待开启', icon: 'Lock' },
  opened: { label: '已开启', icon: 'Unlock' },
  draft: { label: '草稿', icon: 'EditPen' }
}

const formatLocalDate = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const getFutureDate = (days) => {
  const date = new Date()
  date.setDate(date.getDate() + days)
  return formatLocalDate(date)
}

const getDateStart = (date) => {
  const result = new Date(date)
  result.setHours(0, 0, 0, 0)
  return result
}

const getTomorrowStart = () => {
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  return getDateStart(tomorrow)
}

const disableOpenDate = (date) => {
  return getDateStart(date) < getTomorrowStart()
}

const isBeforeTomorrow = (dateStr) => {
  return getDateStart(dateStr) < getTomorrowStart()
}

const form = reactive({
  recipient: '未来的我',
  title: '',
  content: '',
  moods: ['期待'],
  moodText: '',
  tags: ['成长', '给自己'],
  openAt: getFutureDate(365),
  visibility: 'private',
  allowEditBeforeOpen: true
})

const capsules = ref([
  {
    id: 1,
    title: '写给一年后的自己',
    excerpt: '希望你仍然记得现在选择出发的原因。',
    openAt: getFutureDate(128),
    status: 'sealed',
    action: '等待中'
  },
  {
    id: 2,
    title: '毕业旅行后的留言',
    excerpt: '那段路很自由，也让我知道自己可以独自完成很多事。',
    openAt: '2025-06-02',
    status: 'opened',
    action: '打开信件'
  },
  {
    id: 3,
    title: '还没写完的夜晚',
    excerpt: '今天只写了一半，剩下的话想等心情更清楚一点。',
    openAt: getFutureDate(60),
    status: 'draft',
    action: '继续编辑'
  }
])

const activeFilter = ref('all')
const tagEditing = ref(false)
const tagInput = ref('')
const tagInputRef = ref(null)

const sealedCount = computed(() => capsules.value.filter(item => item.status === 'sealed').length)
const openedCount = computed(() => capsules.value.filter(item => item.status === 'opened').length)
const draftCount = computed(() => capsules.value.filter(item => item.status === 'draft').length)
const filteredCapsules = computed(() => {
  if (activeFilter.value === 'all') return capsules.value
  return capsules.value.filter(item => item.status === activeFilter.value)
})
const moodSummary = computed(() => {
  return [...form.moods, form.moodText.trim()].filter(Boolean).join(' · ')
})
const openHint = computed(() => {
  return form.openAt ? `将在 ${form.openAt} 开启` : '选择一个未来日期'
})

const applyPreset = (days) => {
  form.openAt = getFutureDate(days)
}

const toggleMood = (mood) => {
  if (form.moods.includes(mood)) {
    form.moods = form.moods.filter(item => item !== mood)
    return
  }
  form.moods.push(mood)
}

const daysLeft = (dateStr) => {
  const today = new Date()
  const openDate = new Date(dateStr)
  today.setHours(0, 0, 0, 0)
  openDate.setHours(0, 0, 0, 0)
  const diff = Math.ceil((openDate - today) / (24 * 60 * 60 * 1000))
  return diff > 0 ? `还有 ${diff} 天` : '可以开启'
}

const showTagInput = async () => {
  tagEditing.value = true
  await nextTick()
  tagInputRef.value?.focus()
}

const addTag = () => {
  const value = tagInput.value.trim()
  if (value && !form.tags.includes(value)) {
    form.tags.push(value)
  }
  tagInput.value = ''
  tagEditing.value = false
}

const removeTag = (tag) => {
  form.tags = form.tags.filter(item => item !== tag)
}

const sealCapsule = () => {
  if (!form.title.trim()) {
    ElMessage.warning('请先写下信件标题')
    return
  }
  if (!form.content.trim()) {
    ElMessage.warning('请先写下信件内容')
    return
  }
  if (!form.openAt) {
    ElMessage.warning('请选择开启日期')
    return
  }
  if (isBeforeTomorrow(form.openAt)) {
    ElMessage.warning('开启日期请从明天开始选择')
    form.openAt = getFutureDate(1)
    return
  }

  capsules.value.unshift({
    id: Date.now(),
    title: form.title,
    excerpt: form.content.slice(0, 36),
    mood: moodSummary.value,
    openAt: form.openAt,
    status: 'sealed',
    action: '等待中'
  })

  form.title = ''
  form.content = ''
  form.moods = ['期待']
  form.moodText = ''
  form.tags = ['成长', '给自己']
  form.openAt = getFutureDate(365)
  ElMessage.success('时间胶囊已封存')
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.capsule-view {
  --capsule-green: #66c28a;
  --capsule-green-dark: #3f9b68;
  --capsule-mint: #edf8f1;
  --capsule-blue: #5f8ed8;
  --capsule-amber: #c59b4a;
  --capsule-paper: #fffaf0;
  --capsule-ink: #2f3b35;

  position: relative;
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding-bottom: 8px;
}

.page-head {
  position: relative;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding: 24px 28px;
  overflow: hidden;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.94) 0%, rgba(242, 250, 245, 0.9) 100%),
    radial-gradient(circle at 12px 12px, rgba(102, 194, 138, 0.16) 1px, transparent 1.5px);
  background-size: auto, 18px 18px;
  border: 1px solid rgba(102, 194, 138, 0.16);
  border-radius: $radius-base;
  box-shadow: $shadow-base;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    width: 6px;
    height: 100%;
    background: linear-gradient(180deg, var(--capsule-green), #9ed8b3);
  }

  h2 {
    margin: 0 0 8px;
    color: var(--capsule-ink);
    font-size: 28px;
    line-height: 1.2;
    font-weight: 800;
  }

  p {
    margin: 0;
    color: $text-secondary;
    font-size: 14px;
  }
}

.summary {
  display: flex;
  gap: 12px;
}

.summary-item {
  min-width: 94px;
  padding: 14px 16px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(102, 194, 138, 0.16);
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(72, 128, 91, 0.08);
  backdrop-filter: blur(8px);

  span {
    display: block;
    color: var(--capsule-green-dark);
    font-size: 24px;
    font-weight: 700;
  }

  small {
    color: $text-secondary;
    font-size: 12px;
  }
}

.compose-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 24px;
}

.letter-panel,
.settings-panel,
.capsule-list-section {
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(102, 194, 138, 0.12);
  border-radius: $radius-base;
  box-shadow: 0 14px 36px rgba(72, 128, 91, 0.08);
  backdrop-filter: blur(10px);
}

.letter-panel {
  position: relative;
  padding: 26px;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 26px;
    right: 26px;
    height: 3px;
    background: linear-gradient(90deg, var(--capsule-green), #b7dfc5, transparent);
    border-radius: 999px;
  }
}

.settings-panel {
  position: sticky;
  top: 0;
  padding: 22px;
  align-self: start;
}

.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;

  h3 {
    margin: 4px 0 0;
    color: $text-main;
    font-size: 18px;
  }

  .el-icon {
    color: var(--capsule-green);
    font-size: 22px;
  }

  &.compact {
    margin-bottom: 24px;
  }
}

.eyebrow {
  color: var(--capsule-green-dark);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
  text-transform: uppercase;
}

.paper {
  position: relative;
  padding: 24px 26px 26px 42px;
  background:
    linear-gradient(90deg, rgba(102, 194, 138, 0.26) 1px, transparent 1px) 28px 0 / 1px 100% no-repeat,
    repeating-linear-gradient(transparent 0 43px, rgba(140, 118, 78, 0.1) 44px),
    linear-gradient(180deg, #fffdf7 0%, var(--capsule-paper) 100%);
  border: 1px solid #efe4cf;
  border-radius: 14px;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.72),
    0 12px 28px rgba(140, 118, 78, 0.08);

  &::after {
    content: '';
    position: absolute;
    right: 0;
    bottom: 0;
    width: 42px;
    height: 42px;
    background: linear-gradient(135deg, rgba(240, 230, 212, 0) 45%, #eadbc2 46%, #f5ead7 100%);
    border-radius: 12px 0 12px 0;
  }

  &::before {
    content: '';
    position: absolute;
    top: 22px;
    left: 14px;
    width: 8px;
    height: calc(100% - 44px);
    background:
      radial-gradient(circle, rgba(102, 194, 138, 0.18) 0 2px, transparent 2.5px) 0 0 / 8px 32px;
  }
}

.paper-row {
  display: grid;
  grid-template-columns: 74px minmax(0, 1fr);
  align-items: center;
  gap: 14px;
  margin-bottom: 14px;

  label {
    color: $text-secondary;
    font-size: 13px;
  }

  :deep(.el-input__wrapper) {
    background: transparent;
    box-shadow: none;
    border-bottom: 1px solid rgba(102, 194, 138, 0.22);
    border-radius: 0;

    &.is-focus {
      border-bottom-color: var(--capsule-green);
    }
  }
}

.letter-input {
  margin-top: 8px;

  :deep(.el-textarea__inner) {
    padding: 12px 0;
    background: transparent;
    border: 0;
    box-shadow: none;
    color: $text-main;
    font-size: 15px;
    line-height: 1.9;
    resize: none;

    &::placeholder {
      color: rgba(102, 102, 102, 0.46);
    }
  }
}

.mood-area,
.tag-area {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.mood-title {
  margin-right: 2px;
  color: $text-secondary;
  font-size: 13px;
}

.mood-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.mood-chip,
.quick-dates button,
.segmented button,
.filters button {
  border: 0;
  cursor: pointer;
  font-family: inherit;
}

.mood-chip {
  padding: 7px 14px;
  color: $text-regular;
  background: #f6f8f5;
  border: 1px solid transparent;
  border-radius: 999px;
  transition: color 0.2s ease, background 0.2s ease, transform 0.2s ease, border-color 0.2s ease;

  &:hover {
    color: var(--capsule-green-dark);
    border-color: rgba(102, 194, 138, 0.22);
    transform: translateY(-1px);
  }

  &.active {
    color: var(--capsule-green-dark);
    background: var(--capsule-mint);
    border-color: rgba(102, 194, 138, 0.28);
    font-weight: 600;
  }
}

.mood-input {
  width: min(100%, 260px);

  :deep(.el-input__wrapper) {
    min-height: 34px;
    background: #fbfdfb;
    border-radius: 999px;
    box-shadow: 0 0 0 1px rgba(102, 194, 138, 0.14) inset;

    &:hover,
    &.is-focus {
      box-shadow: 0 0 0 1px rgba(102, 194, 138, 0.32) inset;
    }
  }
}

.tag-area {
  :deep(.el-tag) {
    border-color: rgba(102, 194, 138, 0.22);
    background: #f2faf5;
    color: var(--capsule-green-dark);
    border-radius: 999px;
  }
}

.capsule-mood {
  display: inline-flex;
  width: fit-content;
  max-width: 100%;
  padding: 5px 10px;
  margin-bottom: 12px;
  overflow-wrap: anywhere;
  color: var(--capsule-green-dark);
  background: var(--capsule-mint);
  border-radius: 999px;
  font-size: 12px;
  line-height: 1.5;
}

.tag-input {
  width: 110px;
}

.setting-block {
  margin-bottom: 22px;

  > label {
    display: block;
    margin-bottom: 10px;
    color: $text-main;
    font-size: 14px;
    font-weight: 600;
  }
}

.setting-block {
  :deep(.el-input__wrapper) {
    min-height: 38px;
    background: #fbfdfb;
    border-radius: 10px;
    box-shadow: 0 0 0 1px rgba(102, 194, 138, 0.18) inset;

    &:hover,
    &.is-focus {
      box-shadow: 0 0 0 1px var(--capsule-green) inset;
    }
  }
}

.quick-dates {
  display: flex;
  gap: 8px;
  margin-top: 10px;

  button {
    flex: 1;
    height: 32px;
    color: $text-regular;
    background: #f6f8f5;
    border: 1px solid transparent;
    border-radius: 8px;
    transition: all 0.2s ease;

    &:hover {
      color: var(--capsule-green-dark);
      background: var(--capsule-mint);
      border-color: rgba(102, 194, 138, 0.22);
    }
  }
}

.segmented {
  display: grid;
  grid-template-columns: 1fr 1fr;
  padding: 4px;
  background: #f4f7f3;
  border-radius: 10px;
  border: 1px solid rgba(102, 194, 138, 0.12);

  button {
    min-height: 34px;
    color: $text-regular;
    background: transparent;
    border-radius: 8px;

    &.active {
      color: var(--capsule-green-dark);
      background: #fff;
      box-shadow: 0 2px 8px rgba(102, 194, 138, 0.12);
      font-weight: 600;
    }
  }
}

.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
  padding: 14px;
  background: #fbfdfb;
  border: 1px solid rgba(102, 194, 138, 0.12);
  border-radius: 12px;

  strong,
  span {
    display: block;
  }

  strong {
    margin-bottom: 4px;
    color: $text-main;
    font-size: 14px;
  }

  span {
    color: $text-secondary;
    font-size: 12px;
  }
}

.preview-card {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 20px;
  padding: 14px;
  background:
    linear-gradient(135deg, #fbfdfb 0%, #f2faf5 100%);
  border: 1px solid rgba(102, 194, 138, 0.14);
  border-radius: 12px;

  strong,
  span {
    display: block;
  }

  strong {
    margin-bottom: 4px;
    color: $text-main;
    font-size: 14px;
  }

  span {
    color: $text-secondary;
    font-size: 12px;
  }
}

.preview-icon,
.state-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--capsule-green-dark);
  background: rgba(102, 194, 138, 0.14);
  border-radius: 10px;
}

.preview-icon {
  width: 40px;
  height: 40px;
  font-size: 20px;
}

.seal-button {
  width: 100%;
  height: 44px;
  border-radius: 12px;
  border: 0;
  background: linear-gradient(135deg, var(--capsule-green) 0%, var(--capsule-green-dark) 100%);
  box-shadow: 0 10px 22px rgba(102, 194, 138, 0.26);

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 12px 26px rgba(102, 194, 138, 0.32);
  }
}

.capsule-list-section {
  padding: 24px;
  overflow: hidden;
}

.list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 20px;

  h3 {
    margin: 0 0 6px;
    color: $text-main;
    font-size: 18px;
  }

  p {
    margin: 0;
    color: $text-secondary;
    font-size: 13px;
  }
}

.filters {
  display: flex;
  gap: 8px;
  padding: 4px;
  background: #f4f7f3;
  border-radius: 10px;
  border: 1px solid rgba(102, 194, 138, 0.12);

  button {
    min-width: 72px;
    height: 32px;
    color: $text-regular;
    background: transparent;
    border-radius: 8px;

    &.active {
      color: var(--capsule-green-dark);
      background: #fff;
      font-weight: 600;
      box-shadow: 0 2px 8px rgba(102, 194, 138, 0.12);
    }
  }
}

.capsule-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.capsule-card {
  position: relative;
  overflow: hidden;
  min-height: 184px;
  padding: 20px;
  background: #fff;
  border: 1px solid $border-color;
  border-radius: 12px;
  transition: transform 0.24s ease, box-shadow 0.24s ease, border-color 0.24s ease;

  &::before {
    content: '';
    position: absolute;
    inset: 0 0 auto 0;
    height: 4px;
    background: var(--capsule-green);
    opacity: 0.72;
  }

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 22px rgba(102, 194, 138, 0.12);
    border-color: rgba(102, 194, 138, 0.32);
  }

  &.sealed {
    background: linear-gradient(180deg, #fff 0%, #f8fbf6 100%);
    border-color: rgba(102, 194, 138, 0.28);
  }

  &.opened {
    &::before {
      background: var(--capsule-blue);
    }

    .state-icon {
      color: var(--capsule-blue);
      background: rgba(95, 142, 216, 0.12);
    }

    .state-label {
      color: var(--capsule-blue);
      background: rgba(95, 142, 216, 0.1);
    }
  }

  &.draft {
    &::before {
      background: var(--capsule-amber);
    }

    .state-icon {
      color: var(--capsule-amber);
      background: rgba(197, 155, 74, 0.13);
    }

    .state-label {
      color: #a77d25;
      background: rgba(197, 155, 74, 0.12);
    }
  }

  h4 {
    margin: 18px 0 10px;
    color: var(--capsule-ink);
    font-size: 16px;
    line-height: 1.45;
  }

  p {
    min-height: 44px;
    margin: 0 0 18px;
    color: $text-regular;
    font-size: 13px;
    line-height: 1.7;
  }
}

.card-top,
.card-meta,
.card-meta span {
  display: flex;
  align-items: center;
}

.card-top {
  justify-content: space-between;
}

.state-icon {
  width: 36px;
  height: 36px;
}

.state-label {
  padding: 5px 10px;
  color: var(--capsule-green-dark);
  background: rgba(102, 194, 138, 0.12);
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.card-meta {
  justify-content: space-between;
  color: $text-secondary;
  font-size: 12px;

  span {
    gap: 5px;
  }

  strong {
    color: var(--capsule-green-dark);
    font-size: 13px;
  }
}

@media (max-width: 1200px) {
  .compose-grid {
    grid-template-columns: 1fr;
  }

  .capsule-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .page-head,
  .list-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .summary,
  .filters {
    width: 100%;
    overflow-x: auto;
  }

  .summary-item {
    flex: 1;
  }

  .paper-row {
    grid-template-columns: 1fr;
    gap: 6px;
  }

  .capsule-grid {
    grid-template-columns: 1fr;
  }
}
</style>
