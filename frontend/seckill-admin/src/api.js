import axios from 'axios'

const client = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL || '', timeout: 10000, headers: { 'Content-Type': 'application/json' } })
client.interceptors.request.use((config) => { const token = localStorage.getItem('seckill_access_token'); if (token) config.headers.Authorization = `Bearer ${token}`; return config })
client.interceptors.response.use((response) => { const result = response.data; if (result?.code !== undefined && result.code !== 200) return Promise.reject(new Error(result.message || '请求失败')); return result }, (error) => Promise.reject(new Error(error.response?.data?.message || (error.request ? '无法连接到服务，请确认后端已启动' : error.message))))
export const getProducts = (params = {}) => client.get('/api/v1/product/list', { params })
export const getActivities = (params = {}) => client.get('/api/v1/activity/list', { params })
export const getUsers = (params = {}) => client.get('/api/v1/users', { params })
export const updateUserStatus = (id, status) => client.put(`/api/v1/users/${id}/status`, { userId: id, status })
