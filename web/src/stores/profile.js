import { defineStore } from 'pinia'
import { getProfileData } from '../mock/index'
import { getAppDataSource } from '../api/config'
import { createRecord, deleteRecord, listRecords, updateRecord } from '../api/modules/records'
import { getCurrentUser } from '../api/modules/auth'
import { updateProfile } from '../api/modules/profile'


const MS_PER_DAY = 24 * 60 * 60 * 1000
const USER_INFO_STORAGE_KEY = 'earth-online:user-info'

const loadSavedUserInfo = () => {
  if (typeof window === 'undefined') return null

  try {
    const raw = window.localStorage.getItem(USER_INFO_STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

const saveUserInfo = (userInfo) => {
  if (typeof window === 'undefined') return

  const savedUserInfo = {
    avatar: userInfo.avatar,
    username: userInfo.username,
    email: userInfo.email,
    slogan: userInfo.slogan,
    birthDate: userInfo.birthDate
  }

  try {
    window.localStorage.setItem(USER_INFO_STORAGE_KEY, JSON.stringify(savedUserInfo))
  } catch {
    const { avatar, ...fallbackUserInfo } = savedUserInfo
    try {
      window.localStorage.setItem(USER_INFO_STORAGE_KEY, JSON.stringify(fallbackUserInfo))
    } catch {
      // 本地存储失败时，仍保留当前页面内的资料更新。
    }
  }
}

const clearSavedUserInfo = () => {
  if (typeof window === 'undefined') return
  window.localStorage.removeItem(USER_INFO_STORAGE_KEY)
}

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

const toRecordPayload = (timeline) => ({
  title: timeline.title || '',
  content: timeline.content,
  date: timeline.date,
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
    userInfo: loadSavedUserInfo(),
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
      clearSavedUserInfo()
      this.userInfo = null
      this.timelines = []
      this.eventsData = []
      this.moodTrend = []
      this.keywords = []
      this.futureLetters = []
    },
    async fetchProfileData() {
      const source = getAppDataSource()
      if (source !== 'mock') {
        try {
          const [user, records] = await Promise.all([
            getCurrentUser(), listRecords({ page: 1, pageSize: 100 })
          ])
          this.userInfo = {
            ...(this.userInfo || {}),
            id: user.id,
            username: user.username,
            email: user.email,
            avatar: user.avatar,
            slogan: user.slogan,
            birthDate: user.birthDate
          }
          this.timelines = (records.items || records).map(toTimeline).sort(sortTimelinesByDateDesc)
          this.eventsData = []
          this.moodTrend = []
          this.keywords = buildKeywordCloud(this.timelines)
          this.futureLetters = []
          return { userInfo: this.userInfo, timelines: this.timelines }
        } catch (error) {
          if (source === 'api') throw error
          console.warn('[Earth Online] business API unavailable, using mock data:', error)
        }
      }

      return new Promise(resolve => {
        setTimeout(() => {
          const data = getProfileData()
          const savedUserInfo = loadSavedUserInfo()
          this.userInfo = {
            ...data.userInfo,
            ...(savedUserInfo || {}),
            ...(this.userInfo || {})
          }
          this.timelines = data.timelines
          this.eventsData = data.eventsData
          this.moodTrend = data.moodTrend
          this.keywords = data.keywords
          this.futureLetters = data.futureLetters
          resolve(data)
        }, 500)
      })
    },
    async fetchMockTimelineData() {
      const data = getProfileData()
      this.timelines = data.timelines
      this.eventsData = data.eventsData
      this.moodTrend = data.moodTrend
      this.keywords = data.keywords
      this.futureLetters = data.futureLetters
      return data
    },
    async addTimeline(timeline) {
      const source = getAppDataSource()
      const payload = {
        id: timeline.id || `t-${Date.now()}`,
        ...timeline
      }
      this.timelines.push(payload)
      this.timelines.sort(sortTimelinesByDateDesc)
      if (source !== 'mock') {
        try {
          const saved = await createRecord(toRecordPayload(timeline))
          this.timelines = this.timelines.map(item => item.id === payload.id ? toTimeline(saved) : item)
          this.keywords = buildKeywordCloud(this.timelines)
        } catch (error) {
          if (source === 'api') this.timelines = this.timelines.filter(item => item.id !== payload.id)
          if (source === 'api') throw error
          console.warn('[Earth Online] record create fallback:', error)
        }
      }
      return payload
    },
    async updateTimeline(timeline) {
      const index = this.timelines.findIndex(t => t.id === timeline.id)
      if (index !== -1) {
        this.timelines[index] = { ...this.timelines[index], ...timeline }
        this.timelines.sort(sortTimelinesByDateDesc)
      }
      if (getAppDataSource() !== 'mock') {
        const saved = await updateRecord(timeline.id, toRecordPayload(timeline))
        this.timelines = this.timelines.map(item => item.id === timeline.id ? toTimeline(saved) : item)
        this.keywords = buildKeywordCloud(this.timelines)
      }
    },
    async deleteTimeline(id) {
      this.timelines = this.timelines.filter(t => t.id !== id)
      this.keywords = buildKeywordCloud(this.timelines)
      if (getAppDataSource() !== 'mock') await deleteRecord(id)
    },
    async updateUserInfo(userInfo) {
  const source = getAppDataSource()

  if (source !== 'mock') {
    const saved = await updateProfile(userInfo)

    this.userInfo = {
      ...(this.userInfo || {}),
      ...saved
    }
  } else {
    this.userInfo = {
      ...(this.userInfo || {}),
      ...userInfo
    }
  }

  saveUserInfo(this.userInfo)
}
  }
})
