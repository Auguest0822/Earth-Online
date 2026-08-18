import { API_BASE_URL } from './config'

const TOKEN_KEY = 'earth-online:access-token'

export const getAccessToken = () => localStorage.getItem(TOKEN_KEY) || ''
export const setAccessToken = token => localStorage.setItem(TOKEN_KEY, token)
export const clearAccessToken = () => localStorage.removeItem(TOKEN_KEY)

export class ApiError extends Error {
  constructor(message, status, data) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.data = data
  }
}

export const request = async (path, options = {}) => {
  const { method = 'GET', body, params, headers = {}, signal } = options
  const isFormData = body instanceof FormData
  const query = params ? `?${new URLSearchParams(Object.entries(params).filter(([, value]) => value !== undefined && value !== '')).toString()}` : ''
  const isAuthRequest = path === '/auth/login' || path === '/auth/register'
  const token = isAuthRequest ? '' : getAccessToken()
  const response = await fetch(`${API_BASE_URL}${path}${query}`, {
    method,
    signal,
    headers: {
      Accept: 'application/json',
      ...(body !== undefined && !isFormData ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...headers
    },
    body: body === undefined ? undefined : (isFormData ? body : JSON.stringify(body))
  })

  const contentType = response.headers.get('content-type') || ''
  const data = contentType.includes('application/json') ? await response.json() : await response.text()
  if (!response.ok) {
    throw new ApiError(data?.message || data?.error || `请求失败（${response.status}）`, response.status, data)
  }
  return data
}
