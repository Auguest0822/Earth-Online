import { Client } from '@stomp/stompjs'
import { API_BASE_URL } from './config'
import { getAccessToken } from './httpClient'

const CHAT_ENDPOINT = '/ws/chat'
const CHAT_QUEUE = '/user/queue/chat/messages'
const CHAT_READ_RECEIPT_QUEUE = '/user/queue/chat/read-receipts'
const FRIEND_EVENTS_QUEUE = '/user/queue/friends/events'
const CHAT_DESTINATION = '/app/chat/messages'

const resolveBrokerUrl = () => {
  const apiUrl = new URL(API_BASE_URL, window.location.origin)
  const basePath = apiUrl.pathname.replace(/\/api\/?$/, '')
  apiUrl.protocol = apiUrl.protocol === 'https:' ? 'wss:' : 'ws:'
  apiUrl.pathname = `${basePath}${CHAT_ENDPOINT}`.replace(/\/{2,}/g, '/')
  apiUrl.search = ''
  apiUrl.hash = ''
  return apiUrl.toString()
}

let client = null

export const connectChatSocket = ({ onMessage, onReadReceipt, onFriendEvent, onConnectionChange, onReconnect }) => {
  if (client?.active) return
  let hasConnected = false

  client = new Client({
    brokerURL: resolveBrokerUrl(),
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    beforeConnect: () => {
      const token = getAccessToken()
      client.connectHeaders = token ? { Authorization: `Bearer ${token}` } : {}
    },
    onConnect: () => {
      const reconnected = hasConnected
      hasConnected = true
      onConnectionChange?.(true)
      client.subscribe(CHAT_QUEUE, frame => {
        try {
          onMessage(JSON.parse(frame.body))
        } catch (error) {
          console.error('无法解析实时聊天消息', error)
        }
      })
      client.subscribe(CHAT_READ_RECEIPT_QUEUE, frame => {
        try {
          onReadReceipt?.(JSON.parse(frame.body))
        } catch (error) {
          console.error('无法解析聊天已读回执', error)
        }
      })
      client.subscribe(FRIEND_EVENTS_QUEUE, frame => {
        try {
          onFriendEvent?.(JSON.parse(frame.body))
        } catch (error) {
          console.error('无法解析好友状态事件', error)
        }
      })
      if (reconnected) onReconnect?.()
    },
    onWebSocketClose: () => onConnectionChange?.(false),
    onWebSocketError: event => {
      onConnectionChange?.(false)
      console.error('聊天 WebSocket 连接失败。请检查服务器的 CHAT_WEBSOCKET_ALLOWED_ORIGIN_PATTERNS 配置。', event)
    },
    onStompError: frame => {
      onConnectionChange?.(false)
      console.error('聊天 WebSocket 错误', frame.headers.message || frame.body)
    }
  })

  client.activate()
}

export const isChatSocketConnected = () => Boolean(client?.connected)

export const sendChatSocketMessage = (recipientPublicId, content, clientMessageId, messageType = 'TEXT', imageObjectKey = null) => {
  if (!client?.connected) return false
  client.publish({
    destination: CHAT_DESTINATION,
    body: JSON.stringify({ recipientPublicId, content, clientMessageId, messageType, imageObjectKey })
  })
  return true
}

export const disconnectChatSocket = async () => {
  const activeClient = client
  client = null
  if (activeClient?.active) await activeClient.deactivate()
}
