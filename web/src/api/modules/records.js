import { request } from '../httpClient'

export const listRecords = params => request('/records', { params })
export const getRecord = id => request(`/records/${id}`)
export const createRecord = payload => request('/records', { method: 'POST', body: payload })
export const updateRecord = (id, payload) => request(`/records/${id}`, { method: 'PATCH', body: payload })
export const deleteRecord = id => request(`/records/${id}`, { method: 'DELETE' })
