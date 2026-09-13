import http from './http'

export function login(payload) { return http.post('/api/v1/auth/login', payload) }
export function logout() { return http.delete('/api/v1/auth/session') }
export function register(payload) { return http.post('/api/v1/users', payload) }
