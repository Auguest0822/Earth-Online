import { request } from '../httpClient'

export const getDashboard = () => request('/stats/dashboard')
export const getYearlyStats = year => request('/stats/yearly', { params: { year } })
export const getYearlySummary = year => request('/ai/yearly-summary', { params: { year } })
export const generateYearlySummary = (year, regenerate = false) => request('/ai/yearly-summary', {
  method: 'POST',
  body: { year, regenerate }
})
export const queryYearlyMemory = (year, question) => request('/ai/yearly-memory/query', {
  method: 'POST',
  body: { year, question }
})
