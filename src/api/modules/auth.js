import { clearAccessToken, request, setAccessToken } from '../httpClient'

export const register = payload => request('/auth/register', { method: 'POST', body: payload })
export const login = async payload => {
  const data = await request('/auth/login', { method: 'POST', body: payload })
  if (data.accessToken) setAccessToken(data.accessToken)
  return data
}
export const getCurrentUser = () => request('/auth/me')
export const logout = () => {
  clearAccessToken()
  return Promise.resolve()
}
