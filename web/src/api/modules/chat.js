import { request } from '../httpClient'

export const listChatMessages = friendPublicId => request('/chat/messages', {
  params: { friendPublicId }
})

export const sendChatMessage = (recipientPublicId, content, clientMessageId, messageType = 'TEXT', imageObjectKey = null) => request('/chat/messages', {
  method: 'POST',
  body: { recipientPublicId, content, clientMessageId, messageType, imageObjectKey }
})

export const listChatUnreadCounts = () => request('/chat/unread-counts')

export const markChatMessagesRead = friendPublicId => request('/chat/messages/read', {
  method: 'POST',
  params: { friendPublicId }
})
