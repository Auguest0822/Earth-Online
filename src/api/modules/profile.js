import { request } from '../httpClient'

export const getProfile = () => request('/profile')
export const updateProfile = payload => request('/profile', { method: 'PATCH', body: payload })
export const uploadAvatar = file => {
  const formData = new FormData()
  formData.append('avatar', file)
  return fetch(`${import.meta.env.VITE_API_BASE_URL || '/api'}/profile/avatar`, {
    method: 'POST', body: formData
  }).then(async response => {
    const data = await response.json()
    if (!response.ok) throw new Error(data.message || '头像上传失败')
    return data
  })
}
