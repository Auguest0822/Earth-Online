import { API_BASE_URL } from './config'

//token存取
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
//判断请求是否需要携带token
  const isAuthRequest = path === '/auth/login' || path === '/auth/register'
  const token = isAuthRequest ? '' : getAccessToken()

  let response
  //发送请求
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
  //响应体解析
  const contentType = response.headers.get('content-type') || ''
  const data = contentType.includes('application/json') ? await response.json() : await response.text()
  //401全局登出
  if (!response.ok) {
    const isLoginPage = window.location.pathname === '/login'
    // Only an existing token can expire. A visitor without a token should be
    // handled as "not logged in" and must not see the session-expired notice.
    if (response.status === 401 && token && !isAuthRequest && !isLoginPage && !redirectingToLogin) {
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
