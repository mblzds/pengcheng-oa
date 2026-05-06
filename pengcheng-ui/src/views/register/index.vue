<template>
  <div class="register-page">
    <div class="register-container">
      <div class="register-card">
        <!-- Logo 和标题 -->
        <div class="register-header">
          <div class="logo-section">
            <img v-if="siteLogo" :src="siteLogo" class="logo-img" alt="Logo" />
            <div v-else class="logo-icon">{{ siteName.charAt(0) }}</div>
            <span class="logo-text">{{ siteName }}</span>
          </div>
          <h1 class="register-title">用户注册</h1>
          <p class="register-desc">创建您的账号，开始使用系统</p>
        </div>

        <!-- 注册表单 -->
        <n-form ref="formRef" :model="formData" :rules="rules" label-placement="left" label-width="80">
          <n-form-item path="username" label="用户名">
            <n-input v-model:value="formData.username" placeholder="4-20位字母数字下划线" :maxlength="20">
              <template #prefix><n-icon :component="PersonOutline" /></template>
            </n-input>
          </n-form-item>
          <n-form-item path="password" label="密码">
            <n-input v-model:value="formData.password" type="password" placeholder="请输入密码" show-password-on="click" :maxlength="20">
              <template #prefix><n-icon :component="LockClosedOutline" /></template>
            </n-input>
          </n-form-item>
          <n-form-item path="confirmPassword" label="确认密码">
            <n-input v-model:value="formData.confirmPassword" type="password" placeholder="请再次输入密码" show-password-on="click" :maxlength="20">
              <template #prefix><n-icon :component="LockClosedOutline" /></template>
            </n-input>
          </n-form-item>
          <n-form-item path="nickname" label="昵称">
            <n-input v-model:value="formData.nickname" placeholder="请输入昵称（可选）" :maxlength="20">
              <template #prefix><n-icon :component="PersonCircleOutline" /></template>
            </n-input>
          </n-form-item>
          <n-form-item v-if="verifyEmail" path="email" label="邮箱">
            <n-input v-model:value="formData.email" placeholder="请输入邮箱" :maxlength="50">
              <template #prefix><n-icon :component="MailOutline" /></template>
            </n-input>
          </n-form-item>
          <n-form-item v-if="verifyPhone" path="phone" label="手机号">
            <n-input v-model:value="formData.phone" placeholder="请输入手机号" :maxlength="11">
              <template #prefix><n-icon :component="CallOutline" /></template>
            </n-input>
          </n-form-item>
          <n-form-item v-if="captchaEnabled" path="code" label="验证码">
            <div class="captcha-row">
              <n-input v-model:value="formData.code" placeholder="请输入验证码" :maxlength="6" />
              <img v-if="captchaImg" :src="captchaImg" class="captcha-img" @click="loadCaptcha" title="点击刷新" />
              <n-spin v-else :size="20" />
            </div>
          </n-form-item>
          <n-form-item :show-label="false">
            <n-button type="primary" block :loading="loading" @click="handleRegister">注 册</n-button>
          </n-form-item>
        </n-form>

        <!-- 底部链接 -->
        <div class="register-footer">
          <span>已有账号？</span>
          <a class="login-link" @click="goLogin">立即登录</a>
        </div>
      </div>
    </div>

    <!-- 底部版权 -->
    <div class="page-footer">
      <span>{{ copyright }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage, type FormInst, type FormRules } from 'naive-ui'
import { PersonOutline, LockClosedOutline, PersonCircleOutline, MailOutline, CallOutline } from '@vicons/ionicons5'
import { useSiteStore } from '@/stores/site'
import { authApi } from '@/api/auth'
import { configGroupApi } from '@/api/org'

const router = useRouter()
const message = useMessage()
const siteStore = useSiteStore()

// 站点配置
const siteName = computed(() => siteStore.siteName || 'MasterLife')
const siteLogo = computed(() => siteStore.siteLogo)
const copyright = computed(() => siteStore.copyright || '版权所有@朋诚科技')

// 注册配置
const captchaEnabled = ref(false)
const verifyEmail = ref(false)
const verifyPhone = ref(false)

// 验证码
const captchaImg = ref('')
const captchaUuid = ref('')

// 加载配置
async function loadConfig() {
  try {
    const config = await configGroupApi.getPublicConfig()
    captchaEnabled.value = config.login?.captchaEnabled || false
    verifyEmail.value = config.register?.verifyEmail || false
    verifyPhone.value = config.register?.verifyPhone || false
    
    // 检查是否开放注册
    if (!config.register?.enabled) {
      message.warning('系统暂未开放注册')
      router.push('/login')
      return
    }
    
    if (captchaEnabled.value) {
      await loadCaptcha()
    }
  } catch (error) {
    console.error('加载配置失败', error)
  }
}

// 加载验证码
async function loadCaptcha() {
  try {
    const result = await authApi.getCaptcha()
    captchaImg.value = result.img
    captchaUuid.value = result.uuid
  } catch (error) {
    console.error('获取验证码失败', error)
  }
}

onMounted(() => {
  if (!siteStore.loaded) {
    siteStore.loadConfig()
  }
  loadConfig()
})

const formRef = ref<FormInst | null>(null)
const loading = ref(false)

const formData = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  email: '',
  phone: '',
  code: ''
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{4,20}$/, message: '用户名只能包含字母、数字、下划线，长度4-20位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_: any, value: string) => {
        if (value !== formData.password) {
          return new Error('两次输入的密码不一致')
        }
        return true
      },
      trigger: 'blur'
    }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ]
}

async function handleRegister() {
  try {
    await formRef.value?.validate()
    loading.value = true
    
    const registerData: any = {
      username: formData.username,
      password: formData.password,
      nickname: formData.nickname || undefined,
      email: formData.email || undefined,
      phone: formData.phone || undefined
    }
    
    if (captchaEnabled.value) {
      registerData.uuid = captchaUuid.value
      registerData.code = formData.code
    }
    
    const result = await authApi.register(registerData)
    if (result === 'needAudit') {
      message.success('注册成功，请等待管理员审核通过后再登录')
    } else {
      message.success('注册成功，请登录')
    }
    router.push('/login')
  } catch (error: any) {
    // 刷新验证码
    if (captchaEnabled.value) {
      loadCaptcha()
    }
  } finally {
    loading.value = false
  }
}

function goLogin() {
  router.push('/login')
}
</script>

<style lang="scss" scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
  position: relative;
}

.register-container {
  width: 100%;
  max-width: 520px;
  padding: 20px;
}

.register-card {
  background: #fff;
  border-radius: 16px;
  padding: 48px 40px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.08);
}

.register-header {
  text-align: center;
  margin-bottom: 32px;
}

.logo-section {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 24px;
}

.logo-img {
  width: 40px;
  height: 40px;
  object-fit: contain;
  border-radius: 10px;
}

.logo-icon {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #111827 0%, #374151 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
  font-weight: 700;
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
}

.register-title {
  font-size: 24px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 8px;
}

.register-desc {
  font-size: 14px;
  color: #6B7280;
}

.captcha-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  
  .n-input {
    flex: 1;
  }
}

.captcha-img {
  height: 40px;
  border-radius: 6px;
  cursor: pointer;
  border: 1px solid #E5E7EB;
  
  &:hover {
    opacity: 0.8;
  }
}

.register-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: #6B7280;
}

.login-link {
  color: #111827;
  cursor: pointer;
  font-weight: 500;
  
  &:hover {
    text-decoration: underline;
  }
}

.page-footer {
  position: absolute;
  bottom: 20px;
  left: 0;
  right: 0;
  text-align: center;
  color: #9CA3AF;
  font-size: 13px;
}
</style>
