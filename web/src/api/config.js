const STORAGE_KEY = 'earth-online:app-data-source'

export const getAppDataSource = () => 'api'

export const setAppDataSource = () => {
  if (typeof window !== 'undefined') {
    window.localStorage.setItem(STORAGE_KEY, 'api')
  }
}

/*

// 兼容旧模块：全站业务数据统一保持 API 模式。
export const setAppDataSource = () => {
  if (typeof window !== 'undefined') window.localStorage.setItem(STORAGE_KEY, 'api')
}

/*

// 除新闻页外，业务模块固定使用真实后端接口。
export const getAppDataSource = () => 'api'

// 业务模块默认且固定使用真实后端。新闻页有自己的 newsService，仍可单独选择 Mock/API。
export const getAppDataSource = () => {

export const setAppDataSource = () => {
  if (typeof window !== 'undefined') window.localStorage.setItem(STORAGE_KEY, 'api')
  // 清理旧版「全局 Mock 模式」遗留设置，避免用户资料等页面显示演示数据。
  if (typeof window !== 'undefined') window.localStorage.setItem(STORAGE_KEY, 'api')
}

*/

export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')
