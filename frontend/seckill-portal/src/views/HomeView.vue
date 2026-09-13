<template>
  <div class="app-shell">
    <header class="topbar"><BrandLogo compact /><nav><a class="active" href="#">秒杀广场</a><a href="#">我的订单</a></nav><div class="user-area"><span class="avatar">{{ userInitial }}</span><span>{{ user.username || '用户' }}</span><button type="button" @click="handleLogout">退出</button></div></header>
    <main class="content">
      <section class="hero-card"><div><p>SECKILLCORE</p><h1>限时好物，即刻开抢</h1><span>基础框架已就绪，商品与活动模块可从这里继续接入。</span></div><div class="hero-mark">⚡</div></section>
      <section class="section-heading"><div><h2>正在进行</h2><p>热门秒杀活动</p></div><span class="status"><i></i> 实时更新</span></section>
      <div class="empty-card"><div class="empty-icon">🛍️</div><h3>商品列表即将上线</h3><p>前端路由、身份认证和页面布局已经完成。</p></div>
    </main>
  </div>
</template>
<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import BrandLogo from '../components/BrandLogo.vue'
import { logout } from '../api/auth'
import { clearSession, getCurrentUser } from '../utils/auth'
const router = useRouter(); const user = getCurrentUser(); const userInitial = computed(() => (user.username || 'U').slice(0, 1).toUpperCase())
async function handleLogout() { try { await logout() } catch {} clearSession(); await router.replace('/login') }
</script>
<style scoped>
.app-shell{min-height:100vh;background:#f5f8fc}.topbar{position:sticky;z-index:10;top:0;display:flex;height:68px;padding:0 max(24px,calc((100vw - 1180px)/2));align-items:center;gap:54px;border-bottom:1px solid #e8edf5;background:rgba(255,255,255,.92);backdrop-filter:blur(12px)}nav{display:flex;align-self:stretch;gap:32px}nav a{position:relative;display:flex;align-items:center;color:var(--text-muted);font-size:14px;text-decoration:none}nav a.active{color:var(--primary);font-weight:650}nav a.active::after{content:'';position:absolute;right:0;bottom:-1px;left:0;height:2px;border-radius:4px;background:var(--primary)}.user-area{display:flex;margin-left:auto;align-items:center;gap:9px;color:#526078;font-size:13px}.avatar{display:grid;width:34px;height:34px;place-items:center;border-radius:50%;color:#fff;font-weight:700;background:linear-gradient(145deg,#66a0f7,#3476de)}.user-area button{margin-left:8px;padding:6px 10px;border:0;color:#8b99ad;background:none;cursor:pointer}.content{max-width:1180px;margin:0 auto;padding:42px 24px 72px}.hero-card{position:relative;display:flex;min-height:230px;padding:45px 52px;align-items:center;justify-content:space-between;overflow:hidden;border-radius:24px;color:#fff;background:linear-gradient(120deg,#2f72e7,#6aa7fa);box-shadow:0 18px 36px rgba(47,114,231,.17)}.hero-card::after{content:'';position:absolute;width:320px;height:320px;right:-70px;top:-150px;border:60px solid rgba(255,255,255,.08);border-radius:50%}.hero-card p{margin:0 0 12px;font-size:12px;font-weight:700;letter-spacing:3px;opacity:.75}.hero-card h1{margin:0 0 15px;font-size:clamp(30px,4vw,46px);letter-spacing:-1px}.hero-card span{font-size:14px;opacity:.78}.hero-mark{position:relative;z-index:1;display:grid;width:100px;height:100px;place-items:center;border:1px solid rgba(255,255,255,.22);border-radius:28px;font-size:46px;background:rgba(255,255,255,.12);transform:rotate(6deg)}.section-heading{display:flex;margin:42px 0 18px;align-items:center;justify-content:space-between}.section-heading h2{margin:0 0 5px;color:var(--text-strong);font-size:22px}.section-heading p{margin:0;color:var(--text-light);font-size:13px}.status{color:#6c7b91;font-size:12px}.status i{display:inline-block;width:7px;height:7px;margin-right:5px;border-radius:50%;background:#47c786;box-shadow:0 0 0 4px rgba(71,199,134,.12)}.empty-card{padding:65px 24px;border:1px solid #e7edf5;border-radius:18px;text-align:center;background:#fff}.empty-icon{display:grid;width:60px;height:60px;margin:0 auto 18px;place-items:center;border-radius:18px;font-size:27px;background:var(--primary-soft)}.empty-card h3{margin:0 0 9px;color:#34425a;font-size:17px}.empty-card p{margin:0;color:var(--text-light);font-size:13px}@media(max-width:700px){nav{display:none}.topbar{padding:0 18px}.user-area>span:not(.avatar){display:none}.content{padding:24px 16px}.hero-card{padding:34px 28px}.hero-mark{display:none}}
</style>
