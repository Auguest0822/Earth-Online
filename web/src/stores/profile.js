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
  const getTimestamp = (item) => {
    const createdAt = new Date(item?.createdAt).getTime()
    if (Number.isFinite(createdAt)) return createdAt
    return new Date(getTimelineDate(item)).getTime()
  }
  return getTimestamp(b) - getTimestamp(a)
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

const buildEventsData = (timelines) => {
  const counts = new Map()
  timelines.forEach((timeline) => {
    const type = String(timeline.eventType || '生活日常').trim() || '生活日常'
    counts.set(type, (counts.get(type) || 0) + 1)
  })
  return [...counts.entries()].map(([name, value]) => ({ name, value }))
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
    backendAvailable: true,
    backendMessage: '',
    profileLoading: false,
    timelineLoading: false,
    timelines: [],
    timelinePagination: { page: 1, pageSize: 10, total: 0, totalPages: 0 },
    timelineFilterOptions: { years: [], tags: [], eventTypes: [] },
    eventsData: [],
    keywords: [],
    futureLetters: []
  }),
  getters: {
    userStats: (state) => buildSurvivalStats(state.userInfo)
  },
  actions: {
    clearUserSession() {
      this.userInfo = null
      this.backendAvailable = true
      this.backendMessage = ''
      this.timelines = []
      this.timelinePagination = { page: 1, pageSize: 10, total: 0, totalPages: 0 }
      this.timelineFilterOptions = { years: [], tags: [], eventTypes: [] }
      this.eventsData = []
      this.keywords = []
      this.futureLetters = []
    },
    async fetchCurrentUser() {
      const user = await getCurrentUser()
      this.userInfo = {
        id: user.id,
        publicId: user.publicId,
        username: user.username,
        email: user.email,
        avatar: user.avatar,
        slogan: user.slogan,
        birthDate: user.birthDate
      }
      return this.userInfo
    },
    async fetchTimelinePage(params = {}) {
      this.timelineLoading = true
      try {
        const result = await listRecords(params)
        this.timelines = (result.items || []).map(toTimeline).sort(sortTimelinesByDateDesc)
        this.timelinePagination = {
          page: result.page || 1,
          pageSize: result.pageSize || 10,
          total: result.total || 0,
          totalPages: result.totalPages || 0
        }
        this.timelineFilterOptions = {
          years: result.years || [],
          tags: result.tags || [],
          eventTypes: result.eventTypes || []
        }
        return result
      } finally {
        this.timelineLoading = false
      }
    },
    async fetchProfileData() {
      if (this.profileLoading) return { available: this.backendAvailable }
      this.profileLoading = true
      const [userResult, recordsResult, lettersResult] = await Promise.allSettled([
        getCurrentUser(),
        listRecords({ page: 1, pageSize: 100 }),
        listFutureLetters()
      ])

      const failures = [userResult, recordsResult, lettersResult].filter(result => result.status === 'rejected')
      this.backendAvailable = failures.length === 0
      this.backendMessage = failures.length ? '后台服务暂时不可用，当前展示离线页面，数据操作将在服务恢复后可用。' : ''

      if (userResult.status === 'fulfilled') {
        const user = userResult.value
        this.userInfo = {
          id: user.id,
          publicId: user.publicId,
          username: user.username,
          email: user.email,
          avatar: user.avatar,
          slogan: user.slogan,
          birthDate: user.birthDate
        }
      } else if (!this.userInfo) {
        this.userInfo = {
          username: '地球居民',
          email: '',
          avatar: '/default-avatar.png',
          slogan: '后台服务恢复后将自动加载你的资料。',
          birthDate: null,
          offline: true
        }
      }

      const records = recordsResult.status === 'fulfilled' ? recordsResult.value : []
      const futureLetters = lettersResult.status === 'fulfilled' ? lettersResult.value : []
      this.timelines = (records.items || records).map(toTimeline).sort(sortTimelinesByDateDesc)
      this.eventsData = buildEventsData(this.timelines)
      this.keywords = buildKeywordCloud(this.timelines)
      this.futureLetters = futureLetters.items || futureLetters
      this.profileLoading = false
      return { userInfo: this.userInfo, timelines: this.timelines, available: this.backendAvailable }
    },
    async addTimeline(timeline) {
      const payload = {
        id: timeline.id || `t-${Date.now()}`,
        ...timeline,
        createdAt: timeline.createdAt || new Date().toISOString()
      }
      this.timelines.push(payload)
      this.timelines.sort(sortTimelinesByDateDesc)
      try {
        const saved = await createRecord(toRecordPayload(timeline))
        this.timelines = this.timelines.map(item => item.id === payload.id ? toTimeline(saved) : item)
        this.timelines.sort(sortTimelinesByDateDesc)
        this.keywords = buildKeywordCloud(this.timelines)
        this.eventsData = buildEventsData(this.timelines)
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
    },
    async deleteTimeline(id) {
      await deleteRecord(id)
      this.timelines = this.timelines.filter(t => t.id !== id)
      this.keywords = buildKeywordCloud(this.timelines)
      this.eventsData = buildEventsData(this.timelines)
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
