const ACCESS_TOKEN_KEY = 'seckill_access_token'
const REFRESH_TOKEN_KEY = 'seckill_refresh_token'
const USER_KEY = 'seckill_user'
const REMEMBERED_ACCOUNT_KEY = 'seckill_remembered_account'

export const getAccessToken = () => localStorage.getItem(ACCESS_TOKEN_KEY)
export function saveSession(data) {
  localStorage.setItem(ACCESS_TOKEN_KEY, data.token)
  localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken || '')
  localStorage.setItem(USER_KEY, JSON.stringify({ userId: data.userId, username: data.username, avatar: data.avatar }))
}
export function getCurrentUser() { try { return JSON.parse(localStorage.getItem(USER_KEY)) || {} } catch { return {} } }
export function clearSession() { [ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY, USER_KEY].forEach((key) => localStorage.removeItem(key)) }
export const getRememberedAccount = () => localStorage.getItem(REMEMBERED_ACCOUNT_KEY) || ''
export function rememberAccount(account) { if (account) localStorage.setItem(REMEMBERED_ACCOUNT_KEY, account); else localStorage.removeItem(REMEMBERED_ACCOUNT_KEY) }
