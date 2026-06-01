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
      <form v-if="activeTab === 'login'" class="auth-form" @submit.prevent="handleLogin">
        <div class="form-item">
          <el-input v-model="loginForm.username" placeholder="请输入用户名" size="large">
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </div>
        <div class="form-item">
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" size="large" show-password>
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </div>
        <div class="form-extra">
          <el-checkbox v-model="loginForm.remember">记住密码</el-checkbox>
          <a href="#">忘记密码？</a>
        </div>
        <el-button type="success" size="large" native-type="submit" round class="submit-btn">
          登 录
        </el-button>
      </form>

      <!-- 注册表单 -->
      <form v-if="activeTab === 'register'" class="auth-form" @submit.prevent="handleRegister">
        <div class="form-item">
          <el-input v-model="registerForm.username" placeholder="请输入用户名" size="large">
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </div>
        <div class="form-item">
          <el-input v-model="registerForm.email" placeholder="请输入邮箱" size="large">
            <template #prefix>
              <el-icon><Message /></el-icon>
            </template>
          </el-input>
        </div>
        <div class="form-item">
          <el-input v-model="registerForm.password" type="password" placeholder="请输入密码" size="large" show-password>
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </div>
        <div class="form-item">
          <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请确认密码" size="large" show-password>
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
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()
const activeTab = ref('login')

const loginForm = reactive({
  username: '',
  password: '',
  remember: false
})

const registerForm = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const handleLogin = () => {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  ElMessage.success('登录成功')
  setTimeout(() => {
    router.push('/')
  }, 800)
}

const handleRegister = () => {
  if (!registerForm.username || !registerForm.email || !registerForm.password) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    ElMessage.warning('两次密码输入不一致')
    return
  }
  ElMessage.success('注册成功，已自动登录')
  setTimeout(() => {
    router.push('/')
  }, 800)
}
</script>

<style lang="scss" scoped>
.auth-page {
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-image: url('https://images.unsplash.com/photo-1501854140801-50d01698950b?auto=format&fit=crop&w=1950&q=80');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.auth-card {
  width: 420px;
  padding: 48px 40px;
  
  /* ----- 毛玻璃效果 ----- */
  background: rgba(255, 255, 255, 0.35);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08), 0 2px 8px rgba(0, 0, 0, 0.04);
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
    color: #aaa;
    cursor: pointer;
    padding: 8px 28px;
    transition: all 0.3s ease;
    border-radius: 12px;

    &:hover {
      color: #66C28A;
    }

    &.active {
      color: #66C28A;
      font-weight: 700;
      background: rgba(102, 194, 138, 0.12);
    }
  }

  .tab-divider {
    width: 1px;
    height: 20px;
    background: rgba(0, 0, 0, 0.12);
  }
}

.auth-form {
  .form-item {
    margin-bottom: 20px;

    :deep(.el-input__wrapper) {
      border-radius: 14px;
      background: rgba(255, 255, 255, 0.6);
      box-shadow: 0 0 0 1px rgba(102, 194, 138, 0.15) inset;
      transition: all 0.3s;

      &:hover {
        box-shadow: 0 0 0 1px rgba(102, 194, 138, 0.3) inset;
      }

      &.is-focus {
        box-shadow: 0 0 0 1px #66C28A inset;
        background: rgba(255, 255, 255, 0.85);
      }
    }
  }

  .form-extra {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 13px;
    margin-bottom: 28px;

    :deep(.el-checkbox__label) {
      color: #666;
    }

    a {
      color: #66C28A;
      text-decoration: none;
      &:hover {
        text-decoration: underline;
      }
    }
  }

  .submit-btn {
    width: 100%;
    height: 48px;
    font-size: 16px;
    letter-spacing: 6px;
    background: linear-gradient(135deg, #66C28A, #51A874);
    border: none;
    border-radius: 14px;
    box-shadow: 0 6px 20px rgba(102, 194, 138, 0.3);
    transition: all 0.3s;

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 8px 28px rgba(102, 194, 138, 0.4);
    }
  }
}
</style>
