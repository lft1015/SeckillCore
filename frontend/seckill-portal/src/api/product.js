import http from './http'

export function getProducts(params = {}) { return http.get('/api/v1/product/list', { params }) }
export function getProduct(id) { return http.get(`/api/v1/product/detail/${id}`) }
