<template>
  <div class="auth-page">
    <!-- 极淡网格 + 径向光晕背景 -->
    <div class="grid-bg" aria-hidden="true"></div>

    <!-- 注册卡片 -->
    <div class="auth-card">
      <div class="auth-head">
        <div class="auth-badge">
          <ThunderboltOutlined />
          yApiRelay — 接口中转站
        </div>
        <h2 class="auth-title">注册 接口中转站</h2>
        <p class="auth-desc">创建账号，开始浏览和调用接口</p>
      </div>

      <a-form :model="formState" name="basic" autocomplete="off" @finish="handleSubmit">
        <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
          <a-input v-model:value="formState.userAccount" size="large" placeholder="请输入账号">
            <template #prefix>
              <UserOutlined />
            </template>
          </a-input>
        </a-form-item>
        <a-form-item
          name="userPassword"
          :rules="[
            { required: true, message: '请输入密码' },
            { min: 8, message: '密码不能小于 8 位' },
          ]"
        >
          <a-input-password v-model:value="formState.userPassword" size="large" placeholder="请输入密码">
            <template #prefix>
              <LockOutlined />
            </template>
          </a-input-password>
        </a-form-item>
        <a-form-item
          name="checkPassword"
          :rules="[
            { required: true, message: '请确认密码' },
            { min: 8, message: '密码不能小于 8 位' },
            { validator: validateCheckPassword },
          ]"
        >
          <a-input-password v-model:value="formState.checkPassword" size="large" placeholder="请确认密码">
            <template #prefix>
              <LockOutlined />
            </template>
          </a-input-password>
        </a-form-item>
        <div class="tips">
          已有账号？<RouterLink to="/user/login">去登录</RouterLink>
        </div>
        <a-form-item class="submit-item">
          <a-button type="primary" html-type="submit" size="large" block class="btn-primary">注册</a-button>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { userRegister } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import { LockOutlined, ThunderboltOutlined, UserOutlined } from '@ant-design/icons-vue'

const router = useRouter()

const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

/**
 * 验证确认密码
 * @param rule
 * @param value
 * @param callback
 */
const validateCheckPassword = (rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value && value !== formState.userPassword) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: API.UserRegisterRequest) => {
  const res = await userRegister(values)
  // 注册成功，跳转到登录页面
  if (res.data.code === 0) {
    message.success('注册成功')
    router.push({
      path: '/user/login',
      replace: true,
    })
  } else {
    message.error('注册失败，' + res.data.message)
  }
}
</script>

<style scoped>
.auth-page {
  position: relative;
  min-height: 640px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 56px 24px 80px;
  background:
    radial-gradient(ellipse 720px 360px at 50% 0%, rgba(59, 130, 246, 0.1) 0%, transparent 70%),
    #ffffff;
  overflow: hidden;
}

/* 极淡网格背景（与首页一致） */
.grid-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(37, 99, 235, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(37, 99, 235, 0.035) 1px, transparent 1px);
  background-size: 48px 48px;
  -webkit-mask-image: radial-gradient(ellipse 80% 60% at 50% 0%, #000 40%, transparent 100%);
  mask-image: radial-gradient(ellipse 80% 60% at 50% 0%, #000 40%, transparent 100%);
}

/* 居中白底卡片 */
.auth-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 420px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 40px 36px 32px;
  box-shadow:
    0 12px 40px rgba(15, 23, 42, 0.06),
    0 2px 8px rgba(37, 99, 235, 0.04);
}

.auth-head {
  text-align: center;
  margin-bottom: 28px;
}

.auth-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.08);
  color: #2563eb;
  font-size: 14px;
  font-weight: 500;
}

.auth-title {
  margin: 16px 0 8px;
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
}

.auth-desc {
  margin: 0;
  font-size: 15px;
  color: #64748b;
}

/* 输入框：圆角 + 聚焦蓝色 */
.auth-page :deep(.ant-input),
.auth-page :deep(.ant-input-affix-wrapper) {
  border-radius: 10px;
}

.auth-page :deep(.ant-input-affix-wrapper-focused),
.auth-page :deep(.ant-input-affix-wrapper:focus) {
  border-color: #2563eb;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1);
}

.tips {
  margin-bottom: 16px;
  font-size: 13px;
  color: #64748b;
  text-align: right;
}

.tips a {
  color: #2563eb;
  font-weight: 500;
}

.tips a:hover {
  color: #3b82f6;
}

.submit-item {
  margin-bottom: 0;
}

/* 蓝色圆角主按钮（带阴影，与首页一致） */
.btn-primary {
  height: 44px;
  border-radius: 12px;
  font-size: 15px;
  background: #2563eb;
  box-shadow: 0 8px 20px rgba(37, 99, 235, 0.3);
}

.btn-primary:hover {
  background: #3b82f6 !important;
}

@media (max-width: 768px) {
  .auth-card {
    padding: 32px 24px 24px;
  }
}
</style>
