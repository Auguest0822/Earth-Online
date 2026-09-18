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
// 读取上一次的问答缓存：没有缓存时后端返回 204，request 会得到空字符串
export const getYearlyMemoryAnswer = year => request('/ai/yearly-memory/answer', { params: { year } })
