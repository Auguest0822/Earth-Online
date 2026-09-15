import { defineStore } from 'pinia'
import { getAccessToken } from '@/api/httpClient'
import { connectChatSocket, disconnectChatSocket, isChatSocketConnected } from '@/api/chatSocket'
import { listChatUnreadCounts, markChatMessagesRead } from '@/api/modules/chat'
import { listReceivedFriendRequests } from '@/api/modules/friends'

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
    // 待处理的好友申请数量：和未读聊天一样计入侧边栏“朋友”红点
    pendingFriendRequests: 0,
    connected: false,
    initialized: false
  }),
  getters: {
    totalUnread: state => Object.values(state.unreadByFriend)
      .reduce((total, count) => total + (Number(count) || 0), 0),
    unreadFor: state => friendPublicId => Number(state.unreadByFriend[friendPublicId]) || 0,
    // 侧边栏“朋友”一栏的红点：未读聊天 + 待处理好友申请
    friendsBadge() {
      return this.totalUnread + (Number(this.pendingFriendRequests) || 0)
    }
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
    // 拉取“待处理的好友申请”数量，保证刷新页面后红点数量依然准确
    async refreshPendingFriendRequests() {
      const result = await listReceivedFriendRequests()
      const list = Array.isArray(result) ? result : (result?.content || result?.items || [])
      this.pendingFriendRequests = list.length
      return list
    },
    setPendingFriendRequests(count) {
      this.pendingFriendRequests = Math.max(0, Number(count) || 0)
    },
    async initialize() {
      if (!getAccessToken()) return
      bindSoundUnlockHandlers()
      if (this.initialized && isChatSocketConnected()) return
      this.initialized = true
      try {
        await Promise.all([this.refreshUnreadCounts(), this.refreshPendingFriendRequests()])
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
        onFriendEvent: event => {
          if (event?.type === 'REQUEST_RECEIVED') {
            // 收到好友申请按“消息”处理：响铃提示 + 刷新待处理数量（侧边栏红点）
            playNotificationSound()
            this.refreshPendingFriendRequests().catch(() => {})
          }
          emit('onFriendEvent', event)
        },
        onConnectionChange: connected => { this.connected = connected },
        onReconnect: async () => {
          try {
            await Promise.all([this.refreshUnreadCounts(), this.refreshPendingFriendRequests()])
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
      this.pendingFriendRequests = 0
      receivedMessageIds.clear()
      await disconnectChatSocket()
    }
  }
})
