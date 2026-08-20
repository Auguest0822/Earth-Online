<template>
  <div class="auth-page">
    <div class="auth-card">
      <!-- 切换 Tab -->
      <div class="auth-tabs">
        <span
          class="tab-item"
          :class="{ active: activeTab === 'login' }"
          @click="activeTab = 'login'"
        >登录</span>
        <span class="tab-divider"></span>
        <span
          class="tab-item"
          :class="{ active: activeTab === 'register' }"
          @click="activeTab = 'register'"
        >注册</span>
      </div>

      <!-- 登录表单 -->
      <form v-if="activeTab === 'login'" class="auth-form" autocomplete="on" @submit.prevent="handleLogin">
        <el-alert
          v-if="route.query.expired === '1'"
          title="登录状态已过期，请重新登录"
          type="warning"
          :closable="false"
          show-icon
        />
        <div class="form-item">
          <el-input
            v-model="loginForm.username"
            name="username"
            autocomplete="username"
            placeholder="请输入用户名"
            size="large"
            @click="chooseBrowserCredential"
          >
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </div>
        <div class="form-item">
          <el-input
            v-model="loginForm.password"
            name="password"
            autocomplete="current-password"
            type="password"
            placeholder="请输入密码"
            size="large"
            show-password
          >
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </div>
        <div v-if="loginError" class="login-error">{{ loginError }}</div>
        <el-button type="success" size="large" native-type="submit" round class="submit-btn">
          登 录
        </el-button>
      </form>

      <!-- 注册表单 -->
      <form v-if="activeTab === 'register'" class="auth-form" autocomplete="on" @submit.prevent="handleRegister">
        <div class="form-item">
          <el-input v-model="registerForm.username" name="username" autocomplete="username" placeholder="请输入用户名" size="large">
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </div>
        <div class="form-item">
          <el-input v-model="registerForm.email" name="email" autocomplete="email" type="email" placeholder="请输入邮箱" size="large">
            <template #prefix>
              <el-icon><Message /></el-icon>
            </template>
          </el-input>
        </div>
        <div class="form-item">
          <el-input v-model="registerForm.password" name="password" autocomplete="new-password" type="password" placeholder="请输入密码" size="large" show-password>
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </div>
        <div class="form-item">
          <el-input v-model="registerForm.confirmPassword" name="password-confirmation" autocomplete="new-password" type="password" placeholder="请确认密码" size="large" show-password>
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </div>
        <el-button type="success" size="large" native-type="submit" round class="submit-btn">
          注 册
        </el-button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { login, register } from '@/api/modules/auth'
import { useProfileStore } from '@/stores/profile'

const router = useRouter()
const route = useRoute()
const profileStore = useProfileStore()
const activeTab = ref('login')
const loginError = ref('')

const loginForm = reactive({
  username: '',
  password: ''
})

const registerForm = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const credentialLoading = ref(false)

const loadBrowserCredential = async (mediation = 'optional') => {
  if (!window.isSecureContext || !window.PasswordCredential || !navigator.credentials?.get) return
  if (credentialLoading.value) return
  credentialLoading.value = true
  try {
    const credential = await navigator.credentials.get({
      password: true,
      mediation
    })
    if (credential?.type === 'password') {
      loginForm.username = credential.id || ''
      loginForm.password = credential.password || ''
    }
  } catch (error) {
    console.debug('浏览器未提供已保存的登录凭据', error)
  } finally {
    credentialLoading.value = false
  }
}

const chooseBrowserCredential = () => loadBrowserCredential('required')

const saveBrowserCredential = async (username, password) => {
  if (!window.isSecureContext || !window.PasswordCredential || !navigator.credentials?.store) return
  try {
    const credential = new window.PasswordCredential({
      id: username,
      name: username,
      password
    })
    await navigator.credentials.store(credential)
  } catch (error) {
    console.debug('浏览器未保存登录凭据', error)
  }
}

const handleLogin = async () => {
  loginError.value = ''
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  try {
    await login({ username: loginForm.username, password: loginForm.password })
    await saveBrowserCredential(loginForm.username, loginForm.password)
    profileStore.clearUserSession()
    await profileStore.fetchProfileData()
  } catch (error) {
    loginError.value = error.status === 401 || error.status === 403
      ? '用户名或密码有误'
      : (error.message || '登录失败')
    return
  }
  ElMessage.success('登录成功')
  setTimeout(() => {
    const requestedRedirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    const redirect = requestedRedirect.startsWith('/') && !requestedRedirect.startsWith('/login')
      ? requestedRedirect : '/'
    router.push(redirect)
  }, 800)
}

const handleRegister = async () => {
  if (!registerForm.username || !registerForm.email || !registerForm.password) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    ElMessage.warning('两次密码输入不一致')
    return
  }
  try {
    await register({
        username: registerForm.username,
        password: registerForm.password,
        email: registerForm.email
    })
    await login({ username: registerForm.username, password: registerForm.password })
    await saveBrowserCredential(registerForm.username, registerForm.password)
    profileStore.clearUserSession()
    await profileStore.fetchProfileData()
  } catch (error) {
    ElMessage.error(error.message || '注册失败')
    return
  }
  ElMessage.success('注册成功，已自动登录')
  setTimeout(() => {
    router.push('/')
  }, 800)
}

onMounted(loadBrowserCredential)
</script>

<style lang="scss" scoped>
.login-error {
  margin: -8px 0 16px;
  color: #e05252;
  font-size: 13px;
  text-align: left;
}

.auth-page {
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-image: url('@/assets/picture/login.jpg');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.auth-card {
  width: 420px;
  padding: 48px 40px;
  
  background: #edf6ec;
  border: 1px solid #c5dcc6;
  border-radius: 20px;
  box-shadow: 0 18px 48px rgba(54, 91, 61, 0.2), 0 3px 12px rgba(66, 105, 72, 0.1);
}

.auth-tabs {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 36px;
  gap: 0;

  .tab-item {
    font-size: 20px;
    font-weight: 500;
    color: #829184;
    cursor: pointer;
    padding: 8px 28px;
    transition: all 0.3s ease;
    border-radius: 12px;

    &:hover {
      color: #4f9d70;
    }

    &.active {
      color: #3f9164;
      font-weight: 700;
      background: #dcebdc;
    }
  }

  .tab-divider {
    width: 1px;
    height: 20px;
    background: #c9d8c9;
  }
}

.auth-form {
  .form-item {
    margin-bottom: 20px;

    :deep(.el-input__wrapper) {
      border-radius: 14px;
      background: #fbfdf8;
      box-shadow: 0 0 0 1px #c5d8c7 inset;
      transition: all 0.3s;

      &:hover {
        box-shadow: 0 0 0 1px #8fba9f inset;
      }

      &.is-focus {
        box-shadow: 0 0 0 2px #69ad83 inset;
        background: #fffef9;
      }
    }
  }

  .submit-btn {
    width: 100%;
    height: 48px;
    font-size: 16px;
    letter-spacing: 6px;
    background: linear-gradient(135deg, #69b083, #438d68);
    border: none;
    border-radius: 14px;
    box-shadow: 0 7px 20px rgba(54, 112, 82, 0.25);
    transition: all 0.3s;

    &:hover {
      background: linear-gradient(135deg, #76b98e, #397d5c);
      transform: translateY(-1px);
      box-shadow: 0 9px 28px rgba(48, 103, 75, 0.32);
    }
  }
}
</style>
