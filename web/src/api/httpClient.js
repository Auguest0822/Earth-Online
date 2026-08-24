import { API_BASE_URL } from './config'

const TOKEN_KEY = 'earth-online:access-token'
let redirectingToLogin = false

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

export const isServiceUnavailableError = error => error?.status === 0 || error?.status >= 500

export const request = async (path, options = {}) => {
  const { method = 'GET', body, params, headers = {}, signal } = options
  const isFormData = body instanceof FormData
  const query = params ? `?${new URLSearchParams(Object.entries(params).filter(([, value]) => value !== undefined && value !== '')).toString()}` : ''
  const isAuthRequest = path === '/auth/login' || path === '/auth/register'
  const token = isAuthRequest ? '' : getAccessToken()
  let response
  try {
    response = await fetch(`${API_BASE_URL}${path}${query}`, {
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
  } catch (error) {
    if (error?.name === 'AbortError') throw error
    throw new ApiError('无法连接服务器，请检查网络或稍后重试', 0, null)
  }

  const contentType = response.headers.get('content-type') || ''
  const data = contentType.includes('application/json') ? await response.json() : await response.text()
  if (!response.ok) {
    const isLoginPage = window.location.pathname === '/login'
    if (response.status === 401 && !isAuthRequest && !isLoginPage && !redirectingToLogin) {
      redirectingToLogin = true
      clearAccessToken()
      const redirect = window.location.pathname
      window.location.replace(`/login?expired=1&redirect=${encodeURIComponent(redirect)}`)
    }
    const message = response.status >= 500
      ? '后台服务暂时不可用，请稍后重试'
      : (data?.message || data?.error || `请求失败（${response.status}）`)
    throw new ApiError(message, response.status, data)
  }
  return data
}
