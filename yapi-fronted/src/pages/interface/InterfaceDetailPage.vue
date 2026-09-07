<template>
  <div class="detail-page">
    <div class="container">
      <!-- 接口不存在 -->
      <a-result v-if="notFound" status="404" title="接口不存在">
        <template #extra>
          <a-button type="primary" @click="router.push('/interfaces')">返回接口市场</a-button>
        </template>
      </a-result>

      <template v-else>
        <!-- 返回栏 -->
        <div class="back-bar">
          <a-button type="text" @click="router.push('/interfaces')">
            <template #icon>
              <ArrowLeftOutlined />
            </template>
            返回接口市场
          </a-button>
        </div>

        <a-spin :spinning="loading">
          <!-- 接口信息卡片 -->
          <div class="info-card">
            <div class="card-head">
              <div class="head-left">
                <a-tag class="tag" :style="methodBadgeStyle(interfaceInfo.method)">
                  {{ interfaceInfo.method ?? '-' }}
                </a-tag>
                <h2 class="interface-name">{{ interfaceInfo.interfaceName }}</h2>
                <a-tag :color="statusInfo(interfaceInfo.status).color" class="tag">
                  {{ statusInfo(interfaceInfo.status).text }}
                </a-tag>
              </div>
              <a-button type="primary" :disabled="applied" :loading="applying" @click="handleApply">
                {{ applied ? '已申请' : '申请开通' }}
              </a-button>
            </div>

            <p class="interface-desc">{{ interfaceInfo.description || '暂无描述' }}</p>

            <a-descriptions :column="{ xs: 1, sm: 2 }" size="middle" class="info-desc">
              <a-descriptions-item label="请求方法">{{ interfaceInfo.method ?? '-' }}</a-descriptions-item>
              <a-descriptions-item label="接口地址">
                <span v-if="applied" class="path-value">{{ interfaceInfo.path ?? '-' }}</span>
                <span v-else class="path-hidden">申请后可见</span>
              </a-descriptions-item>
              <a-descriptions-item label="累计调用次数">{{ interfaceInfo.totalNum ?? 0 }}</a-descriptions-item>
              <a-descriptions-item label="剩余调用次数">
                {{ applied ? (appliedLeftNum ?? 0) : '-' }}
              </a-descriptions-item>
            </a-descriptions>

            <div class="code-block">
              <div class="code-block-head">
                <span class="code-block-title">请求头</span>
                <a-tag class="sample-tag">示例数据</a-tag>
              </div>
              <pre class="code-content">{{ formatJson(interfaceInfo.requestHeader) }}</pre>
            </div>
            <div class="code-block">
              <div class="code-block-head">
                <span class="code-block-title">请求参数</span>
                <a-tag class="sample-tag">示例数据</a-tag>
              </div>
              <pre class="code-content">{{ formatJson(interfaceInfo.requestParams) }}</pre>
            </div>
            <div class="code-block">
              <div class="code-block-head">
                <span class="code-block-title">响应头</span>
                <a-tag class="sample-tag">示例数据</a-tag>
              </div>
              <pre class="code-content">{{ formatJson(interfaceInfo.responseHeader) }}</pre>
            </div>
          </div>

          <!-- 在线调用测试卡片 -->
          <div class="invoke-card">
            <h3 class="card-title">
              <PlayCircleOutlined class="card-title-icon" />
              在线调用测试
            </h3>
            <a-form layout="vertical">
              <a-form-item label="请求方法">
                <a-tag :color="methodTagColor(interfaceInfo.method)" class="invoke-method-tag">
                  {{ interfaceInfo.method ?? '-' }}
                </a-tag>
              </a-form-item>
              <a-form-item label="请求参数（JSON 字符串）">
                <a-textarea
                  v-model:value="invokeForm.requestParams"
                  :rows="4"
                  :placeholder="interfaceInfo.requestParams || '请输入请求参数，JSON 格式'"
                />
              </a-form-item>
              <a-button type="primary" :loading="invoking" @click="handleInvoke">
                <template #icon>
                  <ThunderboltOutlined />
                </template>
                调用
              </a-button>
            </a-form>

            <div v-if="invokeResult !== undefined" class="code-block result-block">
              <div class="code-block-title">返回结果</div>
              <pre class="code-content">{{ formatJson(invokeResult) }}</pre>
            </div>
          </div>
        </a-spin>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowLeftOutlined, PlayCircleOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'
import {
  applyInterface,
  getInterfaceById,
  invokeInterface,
  listUserInterfaceApply,
} from '@/api/interfaceInfoController.ts'
import { userLoginUserStore } from '@/stores/loginUser.ts'

const route = useRoute()
const router = useRouter()
const loginUserStore = userLoginUserStore()

// 雪花 ID 全程保持字符串，避免 Number 精度丢失
const interfaceId = route.params.id as string

const interfaceInfo = ref<API.InterfaceInfoVO>({})
const loading = ref(true)
const notFound = ref(false)
const applied = ref(false)
// 当前接口在「我申请的」列表里的剩余调用次数（getInterfaceById 返回的 leftNum 为 null，不可用）
const appliedLeftNum = ref<number | undefined>(undefined)
const applying = ref(false)
const invoking = ref(false)
const invokeResult = ref<string | undefined>(undefined)

// 在线调用表单（请求参数默认留空，不预填；请求方法固定使用接口本身的 method，不允许修改）
const invokeForm = reactive<API.InterfaceInfoInvokeRequest>({
  requestParams: '',
})

// 请求方法标签颜色
const methodColor = (method?: string): string => {
  const map: Record<string, string> = {
    GET: '#3B82F6',
    POST: '#22C55E',
    PUT: '#F59E0B',
    DELETE: '#EF4444',
  }
  return map[(method ?? '').toUpperCase()] ?? '#94A3B8'
}

// 请求方法填充色徽章内联样式
const methodBadgeStyle = (method?: string): Record<string, string> => ({
  background: methodColor(method),
  color: '#ffffff',
  border: 'none',
  fontWeight: '600',
})

// 请求方法标签预设色（浅色底 + 彩色字：GET 蓝 / POST 绿 / PUT 橙 / DELETE 红）
const methodTagColor = (method?: string): string => {
  const map: Record<string, string> = {
    GET: 'blue',
    POST: 'green',
    PUT: 'orange',
    DELETE: 'red',
  }
  return map[(method ?? '').toUpperCase()] ?? 'default'
}

// 接口状态文案与颜色（0=关闭 1=发布 2=管理员下架）
const statusInfo = (status?: number): { text: string; color: string } => {
  const map: Record<number, { text: string; color: string }> = {
    0: { text: '已下线', color: 'default' },
    1: { text: '已上线', color: 'green' },
    2: { text: '已下架', color: 'red' },
  }
  return map[status ?? -1] ?? { text: '未知', color: 'default' }
}

// JSON 字符串美化展示，非 JSON 原样返回
const formatJson = (value?: string): string => {
  if (!value) return '暂无数据'
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

// 加载接口详情（getInterfaceById 返回 InterfaceInfoVO）
const loadInterface = async () => {
  loading.value = true
  try {
    const res = await getInterfaceById({ id: interfaceId })
    if (res.data.code === 0 && res.data.data) {
      const info = res.data.data
      interfaceInfo.value = info
      // 接口自带的示例请求参数直接回填到输入框；为空则保持空框（placeholder 兜底提示）
      invokeForm.requestParams = info.requestParams ?? ''
    } else {
      notFound.value = true
    }
  } catch (error) {
    console.error('加载接口详情失败：', error)
    notFound.value = true
  } finally {
    loading.value = false
  }
}

// 查询当前用户是否已申请该接口，并从中取该接口的剩余调用次数 leftNum
const loadApplied = async () => {
  try {
    const res = await listUserInterfaceApply()
    if (res.data.code === 0 && res.data.data) {
      const record = res.data.data.find((item) => item.id === interfaceId)
      applied.value = !!record
      appliedLeftNum.value = record?.leftNum ?? 0
    }
  } catch (error) {
    console.error('查询申请状态失败：', error)
  }
}

// 申请开通
const handleApply = async () => {
  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    await router.push({ path: '/user/login', query: { redirect: route.fullPath } })
    return
  }
  applying.value = true
  try {
    const res = await applyInterface({ interfaceId })
    if (res.data.code === 0) {
      message.success('申请成功')
      applied.value = true
      // 申请成功后重新拉取申请列表，回填剩余调用次数
      loadApplied()
    } else {
      message.error(res.data.message ?? '申请失败')
    }
  } catch (error) {
    console.error('申请失败：', error)
    message.error('申请失败，请重试')
  } finally {
    applying.value = false
  }
}

// 在线调用
const handleInvoke = async () => {
  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    await router.push({ path: '/user/login', query: { redirect: route.fullPath } })
    return
  }
  if (!applied.value) {
    message.warning('请先申请开通该接口')
    return
  }
  invoking.value = true
  try {
    const res = await invokeInterface({
      // 调用地址：传接口的 path；请求方法固定用接口本身的 method
      path: interfaceInfo.value.path ?? '',
      method: interfaceInfo.value.method,
      requestParams: invokeForm.requestParams,
    })
    if (res.data.code === 0) {
      invokeResult.value = res.data.data ?? ''
      message.success('调用成功')
    } else {
      message.error(res.data.message ?? '调用失败')
    }
  } catch (error) {
    console.error('调用失败：', error)
    message.error('调用失败，请重试')
  } finally {
    invoking.value = false
  }
}

onMounted(async () => {
  loadInterface()
  // 先静默获取登录态，已登录才查询申请状态
  try {
    await loginUserStore.fetchLoginUser()
    if (loginUserStore.loginUser.id) {
      loadApplied()
    }
  } catch (error) {
    console.error('获取登录态失败：', error)
  }
})
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 32px 0 64px;
}

.container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 0 24px;
}

.back-bar {
  margin-bottom: 16px;
}

.back-bar :deep(.ant-btn) {
  color: #3b82f6;
  padding-left: 0;
}

/* 卡片通用 */
.info-card,
.invoke-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 32px;
  margin-bottom: 24px;
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.head-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.tag {
  border-radius: 8px;
  border: none;
  margin: 0;
}

.interface-name {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  color: #0f172a;
}

.interface-desc {
  margin: 0 0 24px;
  font-size: 15px;
  line-height: 1.7;
  color: #64748b;
}

.info-desc {
  margin-bottom: 24px;
}

/* 接口地址：已申请展示真实 path，未申请展示占位 */
.path-value {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 13px;
  color: #0f172a;
  word-break: break-all;
}

.path-hidden {
  color: #94a3b8;
  font-style: italic;
}

/* 代码块 */
.code-block {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 16px;
}

.code-block:last-child {
  margin-bottom: 0;
}

.code-block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.code-block-title {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
}

/* 示例数据角标（Swagger/Knife4j 风格） */
.sample-tag {
  margin: 0;
  padding: 0 8px;
  font-size: 12px;
  line-height: 20px;
  border-radius: 6px;
  border: 1px solid #bfdbfe;
  background: #eff6ff;
  color: #2563eb;
}

.code-content {
  margin: 0;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 13px;
  line-height: 1.6;
  color: #334155;
  white-space: pre-wrap;
  word-break: break-all;
}

/* 在线调用 */
.card-title {
  margin: 0 0 24px;
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-title-icon {
  color: #3b82f6;
}

/* 在线调用表单里的方法标签：浅色底 + 彩色字，简洁小巧 */
.invoke-method-tag {
  margin: 0;
  padding: 0 10px;
  font-size: 13px;
  line-height: 22px;
  border-radius: 6px;
}

.result-block {
  margin: 24px 0 0;
}

@media (max-width: 768px) {
  .info-card,
  .invoke-card {
    padding: 20px;
  }

  .interface-name {
    font-size: 22px;
  }
}
</style>
