import { createRouter, createWebHistory } from 'vue-router'
import { getAccessToken } from '../utils/auth'

const routes = [
  { path: '/', name: 'home', component: () => import('../views/HomeView.vue'), meta: { requiresAuth: true, title: '秒杀广场' } },
  { path: '/login', name: 'login', component: () => import('../views/LoginView.vue'), meta: { guestOnly: true, title: '登录' } },
  { path: '/register', name: 'register', component: () => import('../views/RegisterView.vue'), meta: { guestOnly: true, title: '注册' } },
  { path: '/product/:id', name: 'product-detail', component: () => import('../views/ProductDetailView.vue'), meta: { requiresAuth: true, title: '商品详情' } },
  { path: '/orders', name: 'orders', component: () => import('../views/OrdersView.vue'), meta: { requiresAuth: true, title: '我的订单' } },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({ history: createWebHistory(), routes, scrollBehavior: () => ({ top: 0 }) })

router.beforeEach((to) => {
  const authenticated = Boolean(getAccessToken())
  if (to.meta.requiresAuth && !authenticated) return { name: 'login', query: { redirect: to.fullPath } }
  if (to.meta.guestOnly && authenticated) return { name: 'home' }
  document.title = `${to.meta.title || '用户中心'} - SeckillCore`
  return true
})

export default router
