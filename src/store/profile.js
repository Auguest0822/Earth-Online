import { defineStore } from 'pinia'
import { getProfileData } from '../mock/index'

export const useProfileStore = defineStore('profile', {
  state: () => ({
    userInfo: null,
    timelines: [],
    eventsData: [],
    moodTrend: [],
    keywords: [],
    achievements: [],
    capsules: []
  }),
  actions: {
    async fetchProfileData() {
      // 模拟API请求
      return new Promise(resolve => {
        setTimeout(() => {
          const data = getProfileData()
          this.userInfo = data.userInfo
          this.timelines = data.timelines
          this.eventsData = data.eventsData
          this.moodTrend = data.moodTrend
          this.keywords = data.keywords
          this.achievements = data.achievements
          this.capsules = data.capsules
          resolve(data)
        }, 500)
      })
    }
  }
})
