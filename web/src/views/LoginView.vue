<template>
  <main class="auth-page" :class="{ 'is-register': activeTab === 'register' }">
    <div class="background-space" aria-hidden="true"></div>

    <header class="mobile-hero">
      <img :src="mobileAuthEarth" alt="" aria-hidden="true" />
      <h1>Earth Online</h1>
      <p>记录属于你的地球时光</p>
    </header>

    <section class="auth-shell" aria-label="账户入口">
      <header class="auth-heading">
        <p class="eyebrow">{{ activeTab === 'login' ? 'WELCOME BACK' : 'JOIN THE WORLD' }}</p>
        <h2>{{ activeTab === 'login' ? '欢迎来到地球online' : '加入地球online' }}</h2>
        <p>{{ activeTab === 'login' ? '继续记录你的故事与珍贵时刻' : '从今天开始，收藏生活里的每个瞬间' }}</p>
      </header>

      <div class="auth-tabs" role="tablist">
        <button v-for="tab in tabs" :key="tab.value" type="button" class="tab-item"
          :class="{ active: activeTab === tab.value }" role="tab"
          :aria-selected="activeTab === tab.value" @click="switchTab(tab.value)">
          {{ tab.label }}
        </button>
      </div>

      <Transition name="form-switch" mode="out-in">
        <form v-if="activeTab === 'login'" key="login" class="auth-form" autocomplete="on" @submit.prevent="handleLogin">
          <el-alert v-if="route.query.expired === '1'" title="登录状态已过期，请重新登录"
            type="warning" :closable="false" show-icon />
          <FieldLabel for-id="login-user" text="用户名" />
          <el-input id="login-user" v-model="loginForm.username" name="username" autocomplete="username"
            placeholder="输入你的用户名" size="large" @click="chooseBrowserCredential">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
          <FieldLabel for-id="login-password" text="密码" />
          <el-input id="login-password" v-model="loginForm.password" name="password" autocomplete="current-password"
            type="password" placeholder="输入你的密码" size="large" show-password>
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
          <p v-if="loginError" class="form-error" role="alert">{{ loginError }}</p>
          <SubmitButton :loading="submitting" text="登录" loading-text="正在登录…" />
        </form>

        <form v-else key="register" class="auth-form register-form" autocomplete="on" @submit.prevent="handleRegister">
          <FieldLabel for-id="register-user" text="用户名" />
          <el-input id="register-user" v-model="registerForm.username" name="username" autocomplete="username"
            maxlength="20" placeholder="3–20 位中文、字母、数字或下划线" size="large"><template #prefix><el-icon><User /></el-icon></template></el-input>
          <FieldLabel for-id="register-email" text="邮箱" />
          <el-input id="register-email" v-model="registerForm.email" name="email" autocomplete="email" type="text" inputmode="email"
            maxlength="20" placeholder="输入常用邮箱（最多 20 位）" size="large"><template #prefix><el-icon><Message /></el-icon></template></el-input>
          <div class="password-grid">
            <div><FieldLabel for-id="register-password" text="密码" />
              <el-input id="register-password" v-model="registerForm.password" name="password" autocomplete="new-password"
                type="password" maxlength="20" placeholder="8–20 位，含字母和数字" size="large" show-password><template #prefix><el-icon><Lock /></el-icon></template></el-input>
            </div>
            <div><FieldLabel for-id="register-confirm" text="确认密码" />
              <el-input id="register-confirm" v-model="registerForm.confirmPassword" name="password-confirmation"
                autocomplete="new-password" type="password" maxlength="20" placeholder="再次输入" size="large" show-password>
                <template #prefix><el-icon><Lock /></el-icon></template></el-input>
            </div>
          </div>
          <SubmitButton :loading="submitting" text="注册" loading-text="正在注册…" />
        </form>
      </Transition>
    </section>
    <p class="page-credit"><span aria-hidden="true">🌱</span> 开始记录生活</p>
  </main>
</template>

<script setup>
import { defineComponent, h, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { login, register } from '@/api/modules/auth'
import { useProfileStore } from '@/stores/profile'
import mobileAuthEarth from '@/assets/picture/mobile-auth-earth.png'

const FieldLabel = defineComponent({ props: ['forId', 'text'], setup: p => () => h('label', { class: 'field-label', for: p.forId }, p.text) })
const SubmitButton = defineComponent({ props: ['loading', 'text', 'loadingText'], setup: p => () => h('button',
  { class: 'submit-btn', type: 'submit', disabled: p.loading }, [h('span', p.loading ? p.loadingText : p.text), p.loading ? null : h('span', { class: 'button-arrow', 'aria-hidden': 'true' }, '→')])
})

const router = useRouter()
const route = useRoute()
const profileStore = useProfileStore()
const tabs = [{ value: 'login', label: '登录' }, { value: 'register', label: '注册' }]
const activeTab = ref('login')
const submitting = ref(false)
const loginError = ref('')
const credentialLoading = ref(false)
const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', email: '', password: '', confirmPassword: '' })

const switchTab = tab => { activeTab.value = tab; loginError.value = '' }
const loadBrowserCredential = async (mediation = 'optional') => {
  if (!window.isSecureContext || !window.PasswordCredential || !navigator.credentials?.get || credentialLoading.value) return
  credentialLoading.value = true
  try {
    const credential = await navigator.credentials.get({ password: true, mediation })
    if (credential?.type === 'password') {
      loginForm.username = credential.id || ''
      loginForm.password = credential.password || ''
    }
  } catch (error) { console.debug('浏览器未提供已保存的登录凭据', error) }
  finally { credentialLoading.value = false }
}
const chooseBrowserCredential = () => loadBrowserCredential('required')
const saveBrowserCredential = async (username, password) => {
  if (!window.isSecureContext || !window.PasswordCredential || !navigator.credentials?.store) return
  try { await navigator.credentials.store(new window.PasswordCredential({ id: username, name: username, password })) }
  catch (error) { console.debug('浏览器未保存登录凭据', error) }
}
const finishSession = async (username, password) => {
  await saveBrowserCredential(username, password)
  profileStore.clearUserSession()
  await profileStore.fetchProfileData()
}
const handleLogin = async () => {
  loginError.value = ''
  if (!loginForm.username || !loginForm.password) { loginError.value = '请输入用户名和密码'; return }
  submitting.value = true
  try {
    await login({ username: loginForm.username, password: loginForm.password })
    await finishSession(loginForm.username, loginForm.password)
    ElMessage.success('欢迎回来')
    const requested = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    const redirect = requested.startsWith('/') && !requested.startsWith('/login') ? requested : '/'
    setTimeout(() => router.push(redirect), 600)
  } catch (error) {
    loginError.value = error.status === 401 || error.status === 403 ? '用户名或密码有误，请重新输入' : (error.message || '登录失败，请稍后重试')
    submitting.value = false
  }
}
const handleRegister = async () => {
  const username = registerForm.username.trim()
  const email = registerForm.email.trim().toLowerCase()
  const password = registerForm.password
  if (!username || !email || !password) { ElMessage.warning('请填写完整信息'); return }
  if (!/^[\p{L}\p{N}_]{3,20}$/u.test(username)) { ElMessage.warning('用户名需为 3–20 位中文、字母、数字或下划线'); return }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) || email.length > 20) { ElMessage.warning('邮箱格式错误'); return }
  if (password.length < 8 || password.length > 20 || !/[\p{L}]/u.test(password) || !/\d/.test(password)) { ElMessage.warning('密码需为 8–20 位，且同时包含字母和数字'); return }
  if (registerForm.password !== registerForm.confirmPassword) { ElMessage.warning('两次密码输入不一致'); return }
  submitting.value = true
  try {
    await register({ username, password, email })
    await login({ username, password })
    await finishSession(username, password)
    ElMessage.success('注册成功，欢迎来到 Earth Online')
    setTimeout(() => router.push('/'), 600)
  } catch (error) { ElMessage.error(error.message || '注册失败，请稍后重试'); submitting.value = false }
}
onMounted(loadBrowserCredential)
</script>

<style lang="scss" scoped>
.auth-page {
  --forest:#174f40; --green:#2f8061;
  position:relative; width:100%; min-height:100dvh; overflow:auto; isolation:isolate;
  display:grid; grid-template-columns:minmax(420px,1.28fr) minmax(440px,.72fr);
  background:#bde6e4 url('@/assets/picture/auth-world-v2.png') center/cover no-repeat;
  color:var(--forest);
}
.auth-page::after { content:''; position:fixed; inset:0; z-index:-1; pointer-events:none; background:linear-gradient(90deg,rgba(10,76,78,.12),transparent 46%,rgba(244,249,236,.12)); }
.background-space { min-height:100dvh; }
.mobile-hero { display:none; }
.eyebrow { margin:0 0 13px; font-size:11px!important; font-weight:750; letter-spacing:.24em; }
.auth-shell { align-self:center; width:min(420px,calc(100% - 56px)); max-height:calc(100dvh - 56px); overflow-y:auto; margin:28px auto; padding:38px 36px; border:1px solid rgba(255,255,255,.38); border-radius:24px; background:linear-gradient(145deg,rgba(245,252,248,.48),rgba(235,248,244,.24)); box-shadow:0 24px 65px rgba(18,75,72,.13),inset 0 1px rgba(255,255,255,.62); backdrop-filter:blur(18px) saturate(1.08); scrollbar-width:thin; }
.auth-heading { margin-bottom:25px; }
.auth-heading .eyebrow { color:#5e997c; margin-bottom:9px; }
.auth-heading h2 { margin:0; color:#153f39; font-size:clamp(24px,2vw,29px); letter-spacing:-.03em; text-shadow:0 1px rgba(255,255,255,.35); }
.auth-heading>p:last-child { margin:10px 0 0; color:#57776f; font-size:13px; }
.auth-tabs { display:grid; grid-template-columns:1fr 1fr; gap:4px; margin-bottom:26px; padding:4px; border:1px solid rgba(255,255,255,.28); border-radius:13px; background:rgba(39,103,89,.1); }
.tab-item { height:38px; border:0; border-radius:10px; background:transparent; color:#778b82; font-family:inherit; font-size:13px; font-weight:650; cursor:pointer; transition:.18s; }
.tab-item.active { background:rgba(255,255,255,.62); color:#22674f; box-shadow:0 3px 12px rgba(28,83,65,.08); }
.auth-form { display:flex; flex-direction:column; }
.auth-form :deep(.el-alert) { margin-bottom:18px; border-radius:11px; }
.field-label { margin:0 0 8px 2px; color:#426257; font-size:12px; font-weight:650; }
.auth-form :deep(.el-input) { margin-bottom:18px; }
.auth-form :deep(.el-input__wrapper) { min-height:46px; border-radius:12px; background:rgba(255,255,255,.52); box-shadow:0 0 0 1px rgba(34,91,76,.18) inset; backdrop-filter:blur(8px); transition:.18s; }
.auth-form :deep(.el-input__wrapper:hover) { box-shadow:0 0 0 1px rgba(47,128,97,.45) inset; }
.auth-form :deep(.el-input__wrapper.is-focus) { background:#fff; box-shadow:0 0 0 2px rgba(47,128,97,.68) inset,0 7px 20px rgba(47,128,97,.08); }
.auth-form :deep(.el-input__prefix) { color:#66907e; }
.form-error { margin:-7px 2px 13px; color:#c44f4f; font-size:12px; }
.submit-btn { width:100%; height:48px; min-height:48px; max-height:48px; padding:0 20px; border:1px solid rgba(255,255,255,.2); border-radius:12px; display:flex; align-items:center; justify-content:center; gap:12px; color:#fff; background:rgba(31,105,79,.78); box-shadow:0 9px 22px rgba(31,105,79,.18); backdrop-filter:blur(8px); font-family:inherit; font-size:14px; font-weight:650; letter-spacing:.04em; cursor:pointer; overflow:hidden; transition:.18s; }
.button-arrow { display:inline-block; flex:0 0 auto; font-size:19px; line-height:1; transform:translateY(-1px); }
.submit-btn:hover:not(:disabled) { transform:translateY(-2px); box-shadow:0 14px 30px rgba(31,105,79,.31); }
.submit-btn:disabled { opacity:.7; cursor:wait; }
.password-grid { display:grid; grid-template-columns:1fr 1fr; gap:12px; }
.password-grid>div { min-width:0; display:flex; flex-direction:column; }
.page-credit { display:none; }
.form-switch-enter-active,.form-switch-leave-active { transition:.17s; }
.form-switch-enter-from { opacity:0; transform:translateX(8px); }
.form-switch-leave-to { opacity:0; transform:translateX(-8px); }
@media(max-width:1024px) {
  .auth-page { grid-template-columns:minmax(300px,.85fr) minmax(420px,1fr); background-position:42% center; }
  .auth-shell { width:min(400px,calc(100% - 40px)); }
}
@media(max-width:720px) {
  .auth-page {
    display:flex;
    min-height:100dvh;
    padding:max(22px,env(safe-area-inset-top)) clamp(22px,7vw,34px) max(22px,env(safe-area-inset-bottom));
    align-items:center;
    flex-direction:column;
    justify-content:flex-start;
    background:linear-gradient(180deg,#e8f6f0 0%,#f7faf2 43%,#fdfaf1 100%);
  }
  .auth-page::after {
    position:absolute;
    background:
      radial-gradient(circle at 12% 18%,rgba(255,255,255,.72) 0 5%,transparent 5.4%),
      radial-gradient(circle at 90% 24%,rgba(255,255,255,.54) 0 4%,transparent 4.4%);
  }
  .background-space { display:none; }
  .mobile-hero {
    position:relative;
    z-index:1;
    display:flex;
    width:100%;
    max-width:440px;
    align-items:center;
    flex-direction:column;
    text-align:center;
  }
  .mobile-hero img { width:min(88vw,370px); height:clamp(150px,24dvh,230px); object-fit:contain; }
  .mobile-hero h1 { margin:-9px 0 0; color:#173f33; font-family:Georgia,'Times New Roman',serif; font-size:clamp(36px,11vw,50px); font-weight:500; letter-spacing:-.035em; }
  .mobile-hero p { margin:10px 0 23px; color:#75867e; font-size:13px; letter-spacing:.24em; }
  .auth-shell {
    position:relative;
    z-index:2;
    width:100%;
    max-width:440px;
    max-height:none;
    margin:0;
    padding:0;
    overflow:visible;
    border:0;
    border-radius:0;
    background:transparent;
    box-shadow:none;
    backdrop-filter:none;
    -webkit-backdrop-filter:none;
  }
  .auth-heading { display:none; }
  .auth-tabs { position:relative; grid-template-columns:1fr 1fr; gap:0; margin:0 0 29px; padding:0; border:0; border-radius:0; background:transparent; }
  .tab-item { position:relative; height:45px; border-radius:0; color:#8a9691; font-size:17px; font-weight:600; }
  .tab-item.active { color:#236b4c; background:transparent; box-shadow:none; }
  .tab-item.active::after { content:''; position:absolute; right:25%; bottom:0; left:25%; height:3px; border-radius:999px; background:#5ca77a; }
  .field-label { margin:0 0 9px 5px; color:#34483e; font-size:14px; }
  .auth-form :deep(.el-alert) { margin-bottom:16px; }
  .auth-form :deep(.el-input) { margin-bottom:20px; }
  .auth-form :deep(.el-input__wrapper) { min-height:54px; padding:0 17px; border-radius:16px; background:#fff; box-shadow:0 0 0 1px #d8e2dc inset,0 7px 20px rgba(39,91,63,.035); backdrop-filter:none; }
  .auth-form :deep(.el-input__wrapper:hover) { box-shadow:0 0 0 1px #a9c7b3 inset,0 7px 20px rgba(39,91,63,.045); }
  .auth-form :deep(.el-input__wrapper.is-focus) { background:#fff; box-shadow:0 0 0 2px #65aa7e inset,0 8px 22px rgba(39,91,63,.07); }
  .auth-form :deep(.el-input__prefix) { color:#74827c; font-size:19px; }
  .auth-form :deep(.el-input__inner) { color:#263d31; font-size:15px; }
  .submit-btn { height:54px; min-height:54px; max-height:54px; margin-top:4px; border:0; border-radius:18px; background:linear-gradient(135deg,#5cac78,#438f62); box-shadow:0 10px 24px rgba(54,132,85,.18); backdrop-filter:none; font-size:16px; }
  .page-credit { display:flex; margin:auto 0 0; padding-top:27px; align-items:center; gap:8px; color:#55966d; font-size:13px; letter-spacing:.15em; }
}
@media(max-width:430px) {
  .password-grid { grid-template-columns:1fr; gap:0; }
  .auth-page.is-register .mobile-hero img { height:125px; }
  .auth-page.is-register .mobile-hero h1 { font-size:32px; }
  .auth-page.is-register .mobile-hero p { margin:4px 0 13px; }
  .auth-page.is-register .auth-tabs { margin-bottom:18px; }
  .auth-page.is-register .auth-form :deep(.el-input) { margin-bottom:13px; }
}
@media(max-height:720px) and (min-width:721px) {
  .auth-shell { padding-top:27px; padding-bottom:27px; } .auth-heading { margin-bottom:17px; }
  .auth-heading>p:last-child { display:none; } .auth-tabs { margin-bottom:17px; }
  .auth-form :deep(.el-input) { margin-bottom:12px; }
}
@media(prefers-reduced-motion:reduce) { *,*::before,*::after { transition-duration:.01ms!important; } }
</style>
