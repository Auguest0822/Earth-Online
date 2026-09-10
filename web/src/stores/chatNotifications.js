import { defineStore } from 'pinia'
import { getAccessToken } from '@/api/httpClient'
import { connectChatSocket, disconnectChatSocket, isChatSocketConnected } from '@/api/chatSocket'
import { listChatUnreadCounts, markChatMessagesRead } from '@/api/modules/chat'

const listeners = new Set()
const receivedMessageIds = new Set()
let audioContext = null
let soundUnlockHandlersBound = false

const emit = (type, payload) => {
  listeners.forEach(listener => listener?.[type]?.(payload))
}

// 不依赖额外音频文件，用 WebAudio 合成一段很短的提示音。
const ensureAudioContext = () => {
  const AudioContextClass = window.AudioContext || window.webkitAudioContext
  if (!AudioContextClass) return null
  audioContext ||= new AudioContextClass()
  return audioContext
}

const playNotificationSound = () => {
  try {
    const context = ensureAudioContext()
    if (!context) return
    const play = () => {
      const oscillator = context.createOscillator()
      const gain = context.createGain()
      oscillator.type = 'sine'
      oscillator.frequency.setValueAtTime(880, context.currentTime)
      gain.gain.setValueAtTime(0.0001, context.currentTime)
      gain.gain.exponentialRampToValueAtTime(0.08, context.currentTime + 0.015)
      gain.gain.exponentialRampToValueAtTime(0.0001, context.currentTime + 0.18)
      oscillator.connect(gain).connect(context.destination)
      oscillator.start()
      oscillator.stop(context.currentTime + 0.19)
    }
    // Chrome/Edge 在用户交互前会把 AudioContext 置为 suspended 并拦截播放。
    // 这里先尝试恢复再播放，被浏览器拦截时保持静默，未读红点仍然正常显示。
    if (context.state === 'running') play()
    else context.resume().then(play).catch(() => {})
  } catch {
    // 通知音失败不影响实时消息和未读数。
  }
}

// 页面第一次交互时解锁音频上下文，之后收到实时消息才能发出提示音。
const unlockNotificationSound = () => {
  try {
    const context = ensureAudioContext()
    if (context && context.state !== 'running') context.resume().catch(() => {})
  } catch {
    // 忽略：无法创建 AudioContext 时不影响消息接收。
  }
}

const bindSoundUnlockHandlers = () => {
  if (soundUnlockHandlersBound) return
  soundUnlockHandlersBound = true
  // 捕获阶段监听，避免子元素 stopPropagation 使解锁失效。
  const options = { capture: true, passive: true, once: true }
  ;['pointerdown', 'keydown', 'touchstart'].forEach(type => {
    window.addEventListener(type, unlockNotificationSound, options)
  })
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
      bindSoundUnlockHandlers()
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
