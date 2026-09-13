import axios from 'axios'
import { clearSession, getAccessToken } from '../utils/auth'

const http = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL || '', timeout: 10000, headers: { 'Content-Type': 'application/json' } })

http.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use((response) => {
  const result = response.data
  if (result?.code !== undefined && result.code !== 200) return Promise.reject(new Error(result.message || '请求处理失败'))
  return result
}, (error) => {
  if (error.response?.status === 401) {
    clearSession()
    if (window.location.pathname !== '/login') window.location.assign('/login')
  }
  const message = error.response?.data?.message || (error.code === 'ECONNABORTED' ? '请求超时，请稍后重试' : '') || (error.request ? '无法连接到服务，请确认后端已启动' : '') || error.message || '网络异常，请稍后重试'
  return Promise.reject(new Error(message))
})

export default http
