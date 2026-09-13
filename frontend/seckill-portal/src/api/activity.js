import http from './http'

export function getActivities(params = {}) { return http.get('/api/v1/activity/list', { params }) }
export function getSeckillPage(activityId) { return http.get(`/api/v1/bff/seckill-page/${activityId}`) }
