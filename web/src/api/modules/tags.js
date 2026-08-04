import { request } from '../httpClient'

export const listTags = () => request('/tags')
export const createTag = payload => request('/tags', { method: 'POST', body: payload })
export const deleteTag = id => request(`/tags/${id}`, { method: 'DELETE' })
