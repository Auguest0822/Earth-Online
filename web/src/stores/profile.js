import { defineStore } from 'pinia'
import { getProfileData } from '../mock/index'
import { getAppDataSource } from '../api/config'
import { getProfile, updateProfile } from '../api/modules/profile'
import { createRecord, deleteRecord, listRecords, updateRecord } from '../api/modules/records'
import { getDashboard } from '../api/modules/stats'

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
    nickname: userInfo.nickname,
    slogan: userInfo.slogan,
    birthDate: userInfo.birthDate,
    level: userInfo.level
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
  const daysIntoLevel = daysBetween(lastBirthday, today)
  const daysInLevel = Math.max(1, daysBetween(lastBirthday, nextBirthday))
  const experience = Math.min(100, Math.round((daysIntoLevel / daysInLevel) * 100))
  const daysLeft = Math.max(0, daysInLevel - daysIntoLevel)

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
    level: years,
    experience,
    survivalDays,
    survivalDuration,
    nextLevelExp: `还需 ${daysLeft} 天 升级 Lv.${years + 1}`
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
    async fetchProfileData() {
      const source = getAppDataSource()
      if (source !== 'mock') {
        try {
          const [profile, records, dashboard] = await Promise.all([
            getProfile(), listRecords({ page: 1, pageSize: 100 }), getDashboard()
          ])
          this.userInfo = profile
          this.timelines = (records.items || records).sort(sortTimelinesByDateDesc)
          this.eventsData = dashboard.eventsData || []
          this.moodTrend = dashboard.moodTrend || []
          this.keywords = dashboard.keywords || []
          this.futureLetters = dashboard.futureLetters || []
          return { userInfo: profile, timelines: this.timelines, ...dashboard }
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
          const saved = await createRecord(timeline)
          this.timelines = this.timelines.map(item => item.id === payload.id ? saved : item)
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
      if (getAppDataSource() !== 'mock') await updateRecord(timeline.id, timeline)
    },
    async deleteTimeline(id) {
      this.timelines = this.timelines.filter(t => t.id !== id)
      if (getAppDataSource() !== 'mock') await deleteRecord(id)
    },
    async updateUserInfo(userInfo) {
      this.userInfo = {
        ...(this.userInfo || {}),
        ...userInfo
      }
      saveUserInfo(this.userInfo)
      if (getAppDataSource() !== 'mock') await updateProfile(userInfo)
    }
  }
})
