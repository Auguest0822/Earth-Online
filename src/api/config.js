const VALID_SOURCES = ['mock', 'api', 'auto']
const STORAGE_KEY = 'earth-online:app-data-source'

export const getAppDataSource = () => {
  const saved = typeof window === 'undefined' ? '' : window.localStorage.getItem(STORAGE_KEY)
  const source = saved || import.meta.env.VITE_APP_DATA_SOURCE || 'mock'
  return VALID_SOURCES.includes(source) ? source : 'mock'
}

export const setAppDataSource = (source) => {
  if (!VALID_SOURCES.includes(source)) throw new Error(`Unsupported data source: ${source}`)
  window.localStorage.setItem(STORAGE_KEY, source)
}

export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')
