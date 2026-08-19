import { request } from '../httpClient'

export const uploadRecordImage = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request('/uploads/images', { method: 'POST', body: formData })
}

export const uploadAvatar = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request('/uploads/avatar', { method: 'POST', body: formData })
}

export const uploadChatImage = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request('/uploads/chat-images', { method: 'POST', body: formData })
}
