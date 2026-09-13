<template>
  <main class="register-page">
    <div class="ambient ambient-one"></div><div class="ambient ambient-two"></div>
    <section class="showcase">
      <BrandLogo />
      <div class="showcase-copy">
        <p class="eyebrow"><span></span> 加入 SeckillCore</p>
        <h1>好机会，<br><em>值得马上拥有。</em></h1>
        <p class="description">创建你的专属账号，提前锁定心仪好物，享受更快、更安心的秒杀体验。</p>
        <div class="benefit-list"><div><b>01</b><span>实时掌握活动场次</span></div><div><b>02</b><span>订单状态一目了然</span></div><div><b>03</b><span>安全可靠的交易保障</span></div></div>
      </div>
      <p class="copyright">© 2026 SeckillCore. 简单、快速、可靠。</p>
    </section>
    <section class="form-side">
      <div class="mobile-brand"><BrandLogo compact /></div>
      <div class="register-card">
        <div class="card-heading"><span class="welcome-icon">✨</span><h2>创建新账号</h2><p>填写以下信息，开启你的秒杀之旅</p></div>
        <form novalidate @submit.prevent="handleSubmit">
          <div class="form-grid">
            <div class="field" :class="{ invalid: errors.username }"><label for="username">用户名</label><div class="input-wrap"><svg viewBox="0 0 24 24" fill="none"><path d="M20 21a8 8 0 0 0-16 0M12 13a5 5 0 1 0 0-10 5 5 0 0 0 0 10Z" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></svg><input id="username" v-model.trim="form.username" autocomplete="username" placeholder="4-20位字母、数字或下划线" @input="errors.username = ''"></div><span v-if="errors.username" class="error-text">{{ errors.username }}</span></div>
            <div class="field" :class="{ invalid: errors.phone }"><label for="phone">手机号</label><div class="input-wrap"><svg viewBox="0 0 24 24" fill="none"><rect x="6" y="3" width="12" height="18" rx="2.5" stroke="currentColor" stroke-width="1.7"/><path d="M10 18h4" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></svg><input id="phone" v-model.trim="form.phone" inputmode="numeric" autocomplete="tel" placeholder="请输入手机号" @input="errors.phone = ''"></div><span v-if="errors.phone" class="error-text">{{ errors.phone }}</span></div>
          </div>
          <div class="field" :class="{ invalid: errors.email }"><label for="email">邮箱 <small>选填</small></label><div class="input-wrap"><svg viewBox="0 0 24 24" fill="none"><rect x="3" y="5" width="18" height="14" rx="2" stroke="currentColor" stroke-width="1.7"/><path d="m4 7 8 6 8-6" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></svg><input id="email" v-model.trim="form.email" type="email" autocomplete="email" placeholder="用于接收活动提醒（可选）" @input="errors.email = ''"></div><span v-if="errors.email" class="error-text">{{ errors.email }}</span></div>
          <div class="form-grid">
            <div class="field" :class="{ invalid: errors.password }"><label for="password">设置密码</label><div class="input-wrap"><svg viewBox="0 0 24 24" fill="none"><rect x="4" y="10" width="16" height="11" rx="2" stroke="currentColor" stroke-width="1.7"/><path d="M8 10V7a4 4 0 0 1 8 0v3" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></svg><input id="password" v-model="form.password" :type="showPassword ? 'text' : 'password'" autocomplete="new-password" placeholder="8-20位复杂密码" @input="errors.password = ''"><button class="eye-button" type="button" @click="showPassword = !showPassword"><svg viewBox="0 0 24 24" fill="none"><path d="M2.5 12s3.5-6 9.5-6 9.5 6 9.5 6-3.5 6-9.5 6-9.5-6-9.5-6Z" stroke="currentColor" stroke-width="1.6"/><circle cx="12" cy="12" r="2.6" stroke="currentColor" stroke-width="1.6"/></svg></button></div><span v-if="errors.password" class="error-text">{{ errors.password }}</span></div>
            <div class="field" :class="{ invalid: errors.confirmPassword }"><label for="confirmPassword">确认密码</label><div class="input-wrap"><svg viewBox="0 0 24 24" fill="none"><path d="M6 12.5 10 16l8-8" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/><rect x="3" y="3" width="18" height="18" rx="4" stroke="currentColor" stroke-width="1.5"/></svg><input id="confirmPassword" v-model="form.confirmPassword" :type="showConfirm ? 'text' : 'password'" autocomplete="new-password" placeholder="再次输入密码" @input="errors.confirmPassword = ''"><button class="eye-button" type="button" @click="showConfirm = !showConfirm"><svg viewBox="0 0 24 24" fill="none"><path d="M2.5 12s3.5-6 9.5-6 9.5 6 9.5 6-3.5 6-9.5 6-9.5-6-9.5-6Z" stroke="currentColor" stroke-width="1.6"/><circle cx="12" cy="12" r="2.6" stroke="currentColor" stroke-width="1.6"/></svg></button></div><span v-if="errors.confirmPassword" class="error-text">{{ errors.confirmPassword }}</span></div>
          </div>
          <p class="password-tip">密码需包含大写字母、小写字母、数字和特殊字符</p>
          <label class="terms"><input v-model="form.agreed" type="checkbox"><span class="checkbox"></span><span>我已阅读并同意 <a href="#">服务条款</a> 和 <a href="#">隐私政策</a></span></label>
          <p v-if="submitError" class="submit-error" role="alert">{{ submitError }}</p>
          <button class="submit-button" type="submit" :disabled="loading"><span v-if="loading" class="spinner"></span>{{ loading ? '正在创建...' : '创建账号' }}<svg v-if="!loading" viewBox="0 0 24 24" fill="none"><path d="m9 18 6-6-6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg></button>
        </form>
        <p class="login-link">已经有账号？ <button type="button" @click="router.push('/login')">返回登录</button></p>
      </div>
    </section>
    <Transition name="toast"><div v-if="notice" class="toast">{{ notice }}</div></Transition>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import BrandLogo from '../components/BrandLogo.vue'
import { register } from '../api/auth'

const router = useRouter()
const form = reactive({ username: '', phone: '', email: '', password: '', confirmPassword: '', agreed: false })
const errors = reactive({ username: '', phone: '', email: '', password: '', confirmPassword: '' })
const loading = ref(false); const submitError = ref(''); const notice = ref(''); const showPassword = ref(false); const showConfirm = ref(false); let noticeTimer
const usernamePattern = /^[a-zA-Z0-9_]{4,20}$/; const phonePattern = /^1[3-9]\d{9}$/; const emailPattern = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/; const passwordPattern = /^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>/?`~]).{8,20}$/
function validate() {
  errors.username = usernamePattern.test(form.username) ? '' : '用户名需为4-20位字母、数字或下划线'
  errors.phone = phonePattern.test(form.phone) ? '' : '请输入正确的11位手机号'
  errors.email = !form.email || emailPattern.test(form.email) ? '' : '请输入正确的邮箱地址'
  errors.password = passwordPattern.test(form.password) ? '' : '密码需为8-20位且包含大小写字母、数字和特殊字符'
  errors.confirmPassword = form.confirmPassword === form.password && form.confirmPassword ? '' : '两次输入的密码不一致'
  return !Object.values(errors).some(Boolean) && form.agreed
}
function showNotice(message) { notice.value = message; window.clearTimeout(noticeTimer); noticeTimer = window.setTimeout(() => { notice.value = '' }, 2600) }
async function handleSubmit() {
  submitError.value = ''; if (!validate()) { if (!form.agreed) showNotice('请先同意服务条款和隐私政策'); return }; if (loading.value) return; loading.value = true
  try { await register({ username: form.username, password: form.password, confirmPassword: form.confirmPassword, phone: form.phone, email: form.email || null }); await router.replace({ name: 'login', query: { registered: '1' } }) } catch (error) { submitError.value = error.message } finally { loading.value = false }
}
</script>

<style scoped>
.register-page{position:relative;display:grid;min-height:100vh;grid-template-columns:minmax(420px,1.02fr) minmax(500px,.98fr);overflow:hidden;background:#f9fbff}.ambient{position:absolute;border-radius:50%;pointer-events:none}.ambient-one{right:40%;top:-180px;width:420px;height:420px;background:rgba(191,219,254,.28)}.ambient-two{right:-100px;bottom:-180px;width:420px;height:420px;background:rgba(219,234,254,.35)}.showcase{position:relative;z-index:1;display:flex;flex-direction:column;padding:48px clamp(52px,6vw,104px);overflow:hidden;background:linear-gradient(145deg,#eef6ff 0%,#e8f2ff 55%,#f3f8ff 100%)}.showcase::before,.showcase::after{content:'';position:absolute;border:1px solid rgba(76,137,238,.09);border-radius:50%}.showcase::before{width:510px;height:510px;left:-260px;bottom:-200px}.showcase::after{width:330px;height:330px;right:-185px;top:8%}.showcase-copy{position:relative;z-index:1;margin:auto 0;max-width:540px;padding:60px 0}.eyebrow{display:flex;align-items:center;gap:10px;margin-bottom:22px;color:var(--primary);font-size:14px;font-weight:650;letter-spacing:2px}.eyebrow span{width:28px;height:2px;border-radius:4px;background:var(--primary)}h1{margin:0;color:#17233d;font-size:clamp(43px,4.2vw,65px);font-weight:760;line-height:1.22;letter-spacing:-2.8px}h1 em{color:var(--primary);font-style:normal}.description{max-width:460px;margin:28px 0 38px;color:var(--text-muted);font-size:17px;line-height:1.9}.benefit-list{display:grid;gap:17px}.benefit-list div{display:flex;align-items:center;gap:16px;color:#53647c;font-size:14px}.benefit-list b{display:grid;width:32px;height:32px;place-items:center;border-radius:9px;color:var(--primary);font-size:11px;letter-spacing:1px;background:rgba(255,255,255,.7)}.copyright{position:relative;z-index:1;margin:0;color:#9aaac1;font-size:12px}.form-side{position:relative;z-index:2;display:grid;place-items:center;padding:46px 7vw;background:rgba(255,255,255,.72);backdrop-filter:blur(10px)}.mobile-brand{display:none}.register-card{width:100%;max-width:570px}.card-heading{margin-bottom:26px}.welcome-icon{display:inline-grid;width:38px;height:38px;margin-bottom:12px;place-items:center;border-radius:12px;font-size:19px;background:var(--primary-soft)}.card-heading h2{margin:0 0 9px;color:var(--text-strong);font-size:30px;letter-spacing:-.8px}.card-heading p{margin:0;color:var(--text-muted);font-size:14px}.form-grid{display:grid;grid-template-columns:1fr 1fr;gap:18px}.field{margin-bottom:17px}.field label{display:block;margin-bottom:8px;color:#34425b;font-size:13px;font-weight:650}.field label small{margin-left:4px;color:#a3afbf;font-size:11px;font-weight:400}.input-wrap{position:relative;display:flex;align-items:center}.input-wrap>svg{position:absolute;left:14px;width:19px;height:19px;color:#9aabc2;pointer-events:none}.input-wrap input{width:100%;height:48px;padding:0 43px;border:1px solid var(--border);border-radius:11px;outline:none;color:var(--text-strong);font:inherit;font-size:13px;background:#fbfdff;transition:border .2s,box-shadow .2s}.input-wrap input::placeholder{color:#afbbcc}.input-wrap input:focus{border-color:#72a4f2;background:#fff;box-shadow:0 0 0 4px rgba(47,113,235,.09)}.invalid .input-wrap input{border-color:#eb6b6b}.eye-button{position:absolute;right:8px;display:grid;width:32px;height:32px;padding:6px;place-items:center;border:0;border-radius:8px;color:#96a7bd;background:transparent;cursor:pointer}.eye-button:hover{color:var(--primary);background:var(--primary-soft)}.eye-button svg{width:19px;height:19px}.error-text{display:block;margin-top:5px;color:#dc4b4b;font-size:11px;line-height:1.4}.password-tip{margin:-5px 0 17px;color:#9aa9bc;font-size:11px}.terms{display:flex;align-items:flex-start;gap:9px;color:#76869c;font-size:12px;line-height:1.5;cursor:pointer}.terms input{position:absolute;opacity:0}.checkbox{display:grid;flex:0 0 17px;width:17px;height:17px;margin-top:1px;place-items:center;border:1px solid #bdcadb;border-radius:5px;background:#fff}.terms input:checked+.checkbox{border-color:var(--primary);background:var(--primary)}.terms input:checked+.checkbox::after{content:'';width:7px;height:4px;border-bottom:2px solid #fff;border-left:2px solid #fff;transform:translateY(-1px) rotate(-45deg)}.terms a,.login-link button{color:var(--primary);text-decoration:none}.submit-error{margin:13px 0 0;padding:9px 11px;border-radius:8px;color:#b43d3d;font-size:12px;background:#fff1f1}.submit-button{display:flex;width:100%;height:50px;margin-top:19px;align-items:center;justify-content:center;gap:10px;border:0;border-radius:11px;color:#fff;font:inherit;font-size:14px;font-weight:650;background:linear-gradient(115deg,#2c70e9,#4388f3);box-shadow:0 10px 22px rgba(44,112,233,.2);cursor:pointer;transition:transform .2s,box-shadow .2s}.submit-button:hover:not(:disabled){transform:translateY(-1px);box-shadow:0 14px 28px rgba(44,112,233,.27)}.submit-button:disabled{cursor:wait;opacity:.72}.submit-button svg{width:19px;height:19px}.spinner{width:17px;height:17px;border:2px solid rgba(255,255,255,.45);border-top-color:#fff;border-radius:50%;animation:spin .8s linear infinite}.login-link{margin:21px 0 0;color:#9aa7b8;font-size:12px;text-align:center}.login-link button{padding:0;border:0;font:inherit;background:none;cursor:pointer}.toast{position:fixed;z-index:20;left:50%;bottom:30px;padding:11px 18px;border-radius:10px;color:#fff;font-size:13px;background:rgba(27,39,61,.9);box-shadow:0 8px 24px rgba(23,35,55,.18);transform:translateX(-50%)}.toast-enter-active,.toast-leave-active{transition:opacity .2s,transform .2s}.toast-enter-from,.toast-leave-to{opacity:0;transform:translate(-50%,8px)}@keyframes spin{to{transform:rotate(360deg)}}
@media(max-width:920px){.register-page{grid-template-columns:1fr}.showcase{display:none}.form-side{align-content:center;min-height:100vh;padding:40px 24px}.mobile-brand{display:block;position:absolute;left:24px;top:24px}}@media(max-width:600px){.form-side{padding:91px 22px 32px}.form-grid{grid-template-columns:1fr;gap:0}.register-card{max-width:430px}.card-heading{margin-bottom:26px}.card-heading h2{font-size:28px}}
</style>
