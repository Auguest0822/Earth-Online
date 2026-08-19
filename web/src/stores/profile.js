import { defineStore } from 'pinia'
import { createRecord, deleteRecord, listRecords, updateRecord } from '../api/modules/records'
import { getCurrentUser } from '../api/modules/auth'
import { updateProfile } from '../api/modules/profile'
import { listFutureLetters } from '../api/modules/futureLetters'


const MS_PER_DAY = 24 * 60 * 60 * 1000
const isLeapYear = (year) => {
  return (year % 4 === 0 && year % 100 !== 0) || year % 400 === 0
}

const buildBirthday = (birthDate, year) => {
  const month = birthDate.getMonth()
  const day = birthDate.getDate()
  if (month === 1 && day === 29 && !isLeapYear(year)) {
    return new Date(year, 1, 28)
  }
  return new Date(year, month, day)
}

const normalizeDate = (date) => {
  const d = new Date(date)
  d.setHours(0, 0, 0, 0)
  return d
}

const daysBetween = (start, end) => {
  return Math.max(0, Math.floor((normalizeDate(end) - normalizeDate(start)) / MS_PER_DAY))
}

const buildSurvivalStats = (userInfo) => {
  if (!userInfo?.birthDate) return null

  const birth = normalizeDate(new Date(userInfo.birthDate))
  const today = normalizeDate(new Date())
  if (Number.isNaN(birth.getTime())) return null

  let years = today.getFullYear() - birth.getFullYear()
  const thisYearBirthday = buildBirthday(birth, today.getFullYear())
  if (today < thisYearBirthday) {
    years -= 1
  }

  const lastBirthday = buildBirthday(birth, birth.getFullYear() + years)
  const nextBirthday = buildBirthday(birth, birth.getFullYear() + years + 1)

  const survivalDays = daysBetween(birth, today)
  let months = today.getMonth() - lastBirthday.getMonth()
  const dayOffset = today.getDate() - lastBirthday.getDate()
  if (dayOffset < 0) {
    months -= 1
  }
  if (months < 0) {
    months += 12
  }
  const monthAnchor = new Date(lastBirthday)
  monthAnchor.setMonth(monthAnchor.getMonth() + months)
  const durationDays = daysBetween(monthAnchor, today)
  const survivalDuration = `${years}年${months}个月${durationDays}天`

  return {
    survivalDays,
    survivalDuration
  }
}

const getTimelineDate = (item) => {
  if (item?.date) return item.date
  if (item?.year) return `${item.year}-01-01`
  return '1970-01-01'
}

const sortTimelinesByDateDesc = (a, b) => {
  return new Date(getTimelineDate(b)) - new Date(getTimelineDate(a))
}

// API 模式下，关键词来自每一条记录；在前端按出现次数汇总为关键词云数据。
const buildKeywordCloud = (timelines) => {
  const counts = new Map()

  timelines.forEach((timeline) => {
    ;(timeline.keywords || []).forEach((keyword) => {
      const name = String(keyword || '').trim()
      if (!name) return
      counts.set(name, (counts.get(name) || 0) + 1)
    })
  })

  return [...counts.entries()]
    .map(([name, value]) => ({ name, value }))
    .sort((a, b) => b.value - a.value || a.name.localeCompare(b.name, 'zh-CN'))
}

const MOOD_SCORE_MAP = {
  '😊': 9, '😌': 7, '🥰': 8, '🤔': 6, '🥳': 9, '😴': 4,
  '😭': 3, '😤': 3, '😔': 3, '😱': 4, '😎': 8, '😶': 5,
  '😡': 2, '😍': 8, '😳': 6, '🤩': 8
}

const buildEventsData = (timelines) => {
  const counts = new Map()
  timelines.forEach((timeline) => {
    const type = String(timeline.eventType || '生活日常').trim() || '生活日常'
    counts.set(type, (counts.get(type) || 0) + 1)
  })
  return [...counts.entries()].map(([name, value]) => ({ name, value }))
}

const buildMoodTrend = (timelines) => {
  const grouped = new Map()
  timelines.forEach((timeline) => {
    const mood = timeline.mood || timeline.moods?.[0]
    const score = MOOD_SCORE_MAP[mood]
    const year = new Date(timeline.date).getFullYear()
    if (!Number.isFinite(score) || Number.isNaN(year)) return
    const current = grouped.get(year) || { total: 0, count: 0 }
    current.total += score
    current.count += 1
    grouped.set(year, current)
  })
  return [...grouped.entries()]
    .sort(([a], [b]) => a - b)
    .map(([year, item]) => ({ year: String(year), score: Number((item.total / item.count).toFixed(1)) }))
}

const toRecordPayload = (timeline) => ({
  title: timeline.title || '',
  content: timeline.content,
  date: timeline.date,
  eventType: timeline.eventType || '生活日常',
  mood: timeline.mood || timeline.moods?.[0] || '',
  keywords: timeline.keywords || [],
  // blob: 地址只适合本地预览；MinIO 上传完成后才会把真实 URL 放进 photos。
  photos: timeline.photoObjectKeys || []
})

const toTimeline = (record) => ({
  ...record,
  date: record.date || record.createdAt || new Date().toISOString(),
  moods: record.moods || (record.mood ? [record.mood] : []),
  keywords: record.keywords || [],
  photos: record.photos || [],
  photoObjectKeys: record.photoObjectKeys || []
})

export const useProfileStore = defineStore('profile', {
  state: () => ({
    userInfo: null,
    timelines: [],
    eventsData: [],
    moodTrend: [],
    keywords: [],
    futureLetters: []
  }),
  getters: {
    userStats: (state) => buildSurvivalStats(state.userInfo)
  },
  actions: {
    clearUserSession() {
      this.userInfo = null
      this.timelines = []
      this.eventsData = []
      this.moodTrend = []
      this.keywords = []
      this.futureLetters = []
    },
    async fetchProfileData() {
      const [user, records, futureLetters] = await Promise.all([
        getCurrentUser(),
        listRecords({ page: 1, pageSize: 100 }),
        listFutureLetters()
      ])
      this.userInfo = {
        id: user.id,
        publicId: user.publicId,
        username: user.username,
        email: user.email,
        avatar: user.avatar,
        slogan: user.slogan,
        birthDate: user.birthDate
      }
      this.timelines = (records.items || records).map(toTimeline).sort(sortTimelinesByDateDesc)
      this.eventsData = buildEventsData(this.timelines)
      this.moodTrend = buildMoodTrend(this.timelines)
      this.keywords = buildKeywordCloud(this.timelines)
      this.futureLetters = futureLetters.items || futureLetters
      return { userInfo: this.userInfo, timelines: this.timelines }
    },
    async addTimeline(timeline) {
      const payload = {
        id: timeline.id || `t-${Date.now()}`,
        ...timeline
      }
      this.timelines.push(payload)
      this.timelines.sort(sortTimelinesByDateDesc)
      try {
        const saved = await createRecord(toRecordPayload(timeline))
        this.timelines = this.timelines.map(item => item.id === payload.id ? toTimeline(saved) : item)
        this.keywords = buildKeywordCloud(this.timelines)
        this.eventsData = buildEventsData(this.timelines)
        this.moodTrend = buildMoodTrend(this.timelines)
      } catch (error) {
        this.timelines = this.timelines.filter(item => item.id !== payload.id)
        throw error
      }
      return payload
    },
    async updateTimeline(timeline) {
      const saved = await updateRecord(timeline.id, toRecordPayload(timeline))
      this.timelines = this.timelines.map(item => item.id === timeline.id ? toTimeline(saved) : item)
      this.keywords = buildKeywordCloud(this.timelines)
      this.eventsData = buildEventsData(this.timelines)
      this.moodTrend = buildMoodTrend(this.timelines)
    },
    async deleteTimeline(id) {
      await deleteRecord(id)
      this.timelines = this.timelines.filter(t => t.id !== id)
      this.keywords = buildKeywordCloud(this.timelines)
      this.eventsData = buildEventsData(this.timelines)
      this.moodTrend = buildMoodTrend(this.timelines)
    },
    async updateUserInfo(userInfo) {
  const saved = await updateProfile(userInfo)
  this.userInfo = {
    ...(this.userInfo || {}),
    ...saved
  }
}
  }
})
