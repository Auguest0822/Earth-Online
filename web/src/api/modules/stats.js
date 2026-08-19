import { request } from '../httpClient'

export const getDashboard = () => request('/stats/dashboard')
export const getYearlyStats = year => request('/stats/yearly', { params: { year } })
export const getYearlySummary = year => request('/ai/yearly-summary', { params: { year } })
export const generateYearlySummary = (year, regenerate = false) => request('/ai/yearly-summary', {
  method: 'POST',
  body: { year, regenerate }
})
