import axios from 'axios'

const client = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL || '', timeout: 8000 })
client.interceptors.request.use((config) => { const token = localStorage.getItem('seckill_access_token'); if (token) config.headers.Authorization = `Bearer ${token}`; return config })
client.interceptors.response.use((response) => { const result = response.data; if (result?.code !== undefined && result.code !== 200) return Promise.reject(new Error(result.message || '接口返回异常')); return result })
export function probe(path) { const started = performance.now(); return client.get(path).then((result) => ({ result, latency: Math.round(performance.now() - started) })) }
