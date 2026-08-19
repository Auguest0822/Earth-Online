import { request } from '../httpClient'

export const listChatMessages = friendPublicId => request('/chat/messages', {
  params: { friendPublicId }
})

export const sendChatMessage = (recipientPublicId, content) => request('/chat/messages', {
  method: 'POST',
  body: { recipientPublicId, content }
})
