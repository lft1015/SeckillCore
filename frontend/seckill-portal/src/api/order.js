import http from './http'

export function getOrders(params = {}) { return http.get('/api/v1/order/list', { params }) }
export function getOrder(id) { return http.get(`/api/v1/order/detail/${id}`) }
