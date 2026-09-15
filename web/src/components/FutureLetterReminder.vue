<template>
  <el-dialog
    v-model="visible"
    width="min(92vw, 560px)"
    class="future-letter-reminder"
    :close-on-click-modal="false"
    :close-on-press-escape="!reading"
    :show-close="!reading"
    @closed="handleClosed"
  >
    <template #header>
      <div class="reminder-header">
        <span class="reminder-icon" aria-hidden="true">✉</span>
        <strong>{{ reading ? '来自过去的你' : '有一封信可以打开了' }}</strong>
      </div>
    </template>

    <div v-if="reading && openedLetter" class="reminder-letter">
      <h4>{{ openedLetter.title }}</h4>
      <p class="reminder-meta">
        {{ formatDate(openedLetter.openAt) }} 开启<template v-if="openedLetter.mood"> · 当时心情 {{ openedLetter.mood }}</template>
      </p>
      <div class="reminder-content">{{ openedLetter.content }}</div>
    </div>

    <div v-else-if="current" class="reminder-body">
      <p class="reminder-line">
        你在 <strong>{{ formatDate(current.createdAt) }}</strong> 写下一封信，
        约定 <strong>{{ formatDate(current.openAt) }}</strong> 打开，现在已经到时间了。
      </p>
      <h4>{{ current.title }}</h4>
      <p class="reminder-hint">要现在读吗？读完之后今天再打开网页就不会再提醒你了。</p>
    </div>

    <template #footer>
      <template v-if="reading">
        <el-button type="primary" @click="visible = false">收好了</el-button>
      </template>
      <template v-else>
        <el-button @click="visible = false">稍后再说</el-button>
        <el-button type="primary" :loading="opening" @click="readLetter">现在阅读</el-button>
      </template>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listFutureLetters, openFutureLetter } from '@/api/modules/futureLetters'
import { getAccessToken } from '@/api/httpClient'

const queue = ref([])          // 已到期但还没打开的信（可能有多封）
const visible = ref(false)
const opening = ref(false)
const reading = ref(false)     // true 表示正在读刚打开的信
const openedLetter = ref(null)

const current = computed(() => queue.value[0] || null)

const formatDate = value => {
  if (!value) return ''
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? ''
    : new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' }).format(date)
}

// 判定标准与后端 open() 保持一致：已封存（sealed）且已到达开启时间
const isDueLetter = letter => letter?.status === 'sealed'
  && Boolean(letter?.openAt)
  && new Date(letter.openAt).getTime() <= Date.now()

const loadDueLetters = async () => {
  if (!getAccessToken()) return
  try {
    const result = await listFutureLetters()
    const list = Array.isArray(result) ? result : (result?.content || result?.items || [])
    queue.value = list.filter(isDueLetter)
    if (queue.value.length) visible.value = true
  } catch {
    // 后台不可用时静默跳过，不打扰用户
  }
}

const readLetter = async () => {
  if (!current.value || opening.value) return
  opening.value = true
  try {
    // 打开后后端把状态改为 opened，因此当天再次登入不会再弹（队列里也不会再有它）
    openedLetter.value = await openFutureLetter(current.value.id)
    reading.value = true
  } catch (error) {
    ElMessage.error(error.message || '打开信件失败')
  } finally {
    opening.value = false
  }
}

const handleClosed = () => {
  const wasReading = reading.value
  reading.value = false
  openedLetter.value = null
  if (wasReading) {
    // 这封已经读过了，若还有其它到期信件就继续提示
    queue.value = queue.value.slice(1)
    if (queue.value.length) window.setTimeout(() => { visible.value = true }, 260)
  }
}

onMounted(loadDueLetters)
</script>

<style lang="scss">
.future-letter-reminder {
  .reminder-header {
    display: flex;
    align-items: center;
    gap: 9px;
    color: #2d4638;
    font-size: 16px;
  }

  .reminder-icon {
    display: grid;
    width: 26px;
    height: 26px;
    place-items: center;
    border-radius: 50%;
    color: #4fa875;
    background: #eaf7ef;
    font-size: 13px;
  }

  .reminder-body {
    color: #555;

    .reminder-line {
      margin: 0 0 12px;
      font-size: 14px;
      line-height: 1.8;

      strong { color: #2d4638; }
    }

    h4 { margin: 0 0 10px; color: #2d4638; font-size: 17px; }
    .reminder-hint { margin: 0; color: #8a9a90; font-size: 12px; }
  }

  .reminder-letter {
    h4 { margin: 0 0 6px; color: #2d4638; font-size: 18px; }
    .reminder-meta { margin: 0 0 14px; color: #8a9a90; font-size: 12px; }

    .reminder-content {
      max-height: 52vh;
      overflow-y: auto;
      padding: 14px;
      color: #4b5f53;
      background: #f7fbf8;
      border: 1px solid #e4eee7;
      border-radius: 12px;
      font-size: 14px;
      line-height: 1.9;
      white-space: pre-wrap;
      word-break: break-word;
    }
  }
}
</style>
