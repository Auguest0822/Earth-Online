import { request } from '../httpClient'

export const listFutureLetters = params => request('/future-letters', { params })
export const getFutureLetter = id => request(`/future-letters/${id}`)
export const createFutureLetter = payload => request('/future-letters', { method: 'POST', body: payload })
export const updateFutureLetter = (id, payload) => request(`/future-letters/${id}`, { method: 'PATCH', body: payload })
export const deleteFutureLetter = id => request(`/future-letters/${id}`, { method: 'DELETE' })
export const openFutureLetter = id => request(`/future-letters/${id}/open`, { method: 'POST' })
