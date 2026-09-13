import http from './http'

export function executeSeckill(payload) { return http.post('/api/seckill/execute', payload) }
export function getSeckillResult(id) { return http.get(`/api/seckill/result/${id}`) }
