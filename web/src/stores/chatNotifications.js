import { defineStore } from 'pinia'
import { getAccessToken } from '@/api/httpClient'
import { connectChatSocket, disconnectChatSocket, isChatSocketConnected } from '@/api/chatSocket'
import { listChatUnreadCounts, markChatMessagesRead } from '@/api/modules/chat'

const listeners = new Set()
const receivedMessageIds = new Set()
let audioContext = null

const emit = (type, payload) => {
  listeners.forEach(listener => listener?.[type]?.(payload))
}

// 不依赖额外音频文件。浏览器要求用户先和页面有过一次交互，
// 否则会自动拦截播放；这种情况下未读红点仍会正常显示。
const playNotificationSound = () => {
  try {
    const AudioContextClass = window.AudioContext || window.webkitAudioContext
    if (!AudioContextClass) return
    audioContext ||= new AudioContextClass()
    const play = () => {
      const oscillator = audioContext.createOscillator()
      const gain = audioContext.createGain()
      oscillator.type = 'sine'
      oscillator.frequency.setValueAtTime(880, audioContext.currentTime)
      gain.gain.setValueAtTime(0.0001, audioContext.currentTime)
      gain.gain.exponentialRampToValueAtTime(0.08, audioContext.currentTime + 0.015)
      gain.gain.exponentialRampToValueAtTime(0.0001, audioContext.currentTime + 0.18)
      oscillator.connect(gain).connect(audioContext.destination)
      oscillator.start()
      oscillator.stop(audioContext.currentTime + 0.19)
    }
    if (audioContext.state === 'suspended') audioContext.resume().then(play).catch(() => {})
    else play()
  } catch {
    // 通知音失败不影响实时消息和未读数。
  }
}

export const useChatNotificationStore = defineStore('chatNotifications', {
  state: () => ({
    unreadByFriend: {},
    connected: false,
    initialized: false
  }),
  getters: {
    totalUnread: state => Object.values(state.unreadByFriend)
      .reduce((total, count) => total + (Number(count) || 0), 0),
    unreadFor: state => friendPublicId => Number(state.unreadByFriend[friendPublicId]) || 0
  },
  actions: {
    subscribe(listener) {
      listeners.add(listener)
      return () => listeners.delete(listener)
    },
    async refreshUnreadCounts() {
      const result = await listChatUnreadCounts()
      const nextCounts = {}
      ;(Array.isArray(result) ? result : []).forEach(item => {
        const count = Number(item?.unreadCount) || 0
        if (item?.friendPublicId && count > 0) nextCounts[item.friendPublicId] = count
      })
      this.unreadByFriend = nextCounts
    },
    async markFriendRead(friendPublicId) {
      if (!friendPublicId) return
      this.unreadByFriend = { ...this.unreadByFriend, [friendPublicId]: 0 }
      await markChatMessagesRead(friendPublicId)
    },
    async initialize() {
      if (!getAccessToken()) return
      if (this.initialized && isChatSocketConnected()) return
      this.initialized = true
      try {
        await this.refreshUnreadCounts()
      } catch {
        // 首次未读数获取失败时，连接建立后仍可继续接收新消息。
      }
      connectChatSocket({
        onMessage: message => {
          if (!message?.mine) {
            const id = String(message.id || '')
            if (id && receivedMessageIds.has(id)) return
            if (id) receivedMessageIds.add(id)
            const friendPublicId = message.sender?.publicId
            if (friendPublicId) {
              this.unreadByFriend = {
                ...this.unreadByFriend,
                [friendPublicId]: this.unreadFor(friendPublicId) + 1
              }
              playNotificationSound()
            }
          }
          emit('onMessage', message)
        },
        onReadReceipt: receipt => emit('onReadReceipt', receipt),
        onFriendEvent: event => emit('onFriendEvent', event),
        onConnectionChange: connected => { this.connected = connected },
        onReconnect: async () => {
          try {
            await this.refreshUnreadCounts()
          } finally {
            emit('onReconnect')
          }
        }
      })
    },
    async stop() {
      this.initialized = false
      this.connected = false
      this.unreadByFriend = {}
      receivedMessageIds.clear()
      await disconnectChatSocket()
    }
  }
})
