import { request } from '../httpClient'

export const getDashboard = () => request('/stats/dashboard')
export const getYearlyStats = year => request('/stats/yearly', { params: { year } })
export const generateYearlySummary = year => request('/ai/yearly-summary', { method: 'POST', body: { year } })
