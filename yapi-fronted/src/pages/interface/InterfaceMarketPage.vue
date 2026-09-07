<template>
  <div class="market-page">
    <div class="container">
      <!-- 顶部工具条 -->
      <div class="toolbar-card">
        <div class="page-head">
          <h2 class="page-title">接口市场</h2>
          <p class="page-desc">浏览平台已上线的接口，申请开通后即可在线调用</p>
        </div>
        <div class="toolbar">
          <a-input-search
            v-model:value="searchKeyword"
            placeholder="搜索接口名称"
            allow-clear
            enter-button="搜索"
            size="large"
            @search="handleSearch"
          />
          <a-select
            v-model:value="methodFilter"
            size="large"
            class="method-select"
            placeholder="全部方法"
            allow-clear
            :options="methodOptions"
            @change="handleSearch"
          />
        </div>
      </div>

      <!-- 接口卡片网格 -->
      <a-spin :spinning="loading" wrapper-class-name="list-spin">
        <div v-if="!loading && interfaceList.length === 0" class="empty-wrap">
          <a-empty description="暂无接口数据（后端服务启动后自动展示）" />
        </div>
        <div v-else class="card-grid">
          <div
            v-for="(item, index) in interfaceList"
            :key="item.id ?? index"
            class="interface-card"
            @click="goDetail(item)"
          >
            <div class="card-top">
              <a-tag :color="methodColor(item.method)" class="tag">{{ item.method ?? '-' }}</a-tag>
              <a-tag :color="statusInfo(item.status).color" class="tag">
                {{ statusInfo(item.status).text }}
              </a-tag>
            </div>
            <h3 class="card-name">{{ item.interfaceName }}</h3>
            <p class="card-desc">{{ item.description || '暂无描述' }}</p>
            <div class="card-bottom">
              <span class="detail-link">
                查看详情
                <ArrowRightOutlined />
              </span>
            </div>
          </div>
        </div>
      </a-spin>

      <!-- 分页 -->
      <div v-if="total > 0" class="pagination-wrap">
        <a-pagination
          v-model:current="queryParams.current"
          :total="total"
          :page-size="queryParams.pageSize"
          :show-size-changer="false"
          :show-total="(t: number) => `共 ${t} 个接口`"
          @change="loadInterfaces"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowRightOutlined } from '@ant-design/icons-vue'
import { listInterfaceByPage } from '@/api/interfaceInfoController.ts'

const router = useRouter()

const interfaceList = ref<API.InterfaceInfo[]>([])
const total = ref(0)
const loading = ref(false)
const searchKeyword = ref('')
const methodFilter = ref<string | undefined>(undefined)

const queryParams = reactive({
  current: 1,
  pageSize: 9,
})

// 方法筛选选项（「全部方法」用 select 的 placeholder 表达，不设空值选项）
const methodOptions = [
  { label: 'GET', value: 'GET' },
  { label: 'POST', value: 'POST' },
  { label: 'PUT', value: 'PUT' },
  { label: 'DELETE', value: 'DELETE' },
]

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

// 接口状态文案与颜色（0=关闭 1=发布 2=管理员下架）
const statusInfo = (status?: number): { text: string; color: string } => {
  const map: Record<number, { text: string; color: string }> = {
    0: { text: '已下线', color: 'default' },
    1: { text: '已上线', color: 'green' },
    2: { text: '已下架', color: 'red' },
  }
  return map[status ?? -1] ?? { text: '未知', color: 'default' }
}

// 加载接口分页列表
const loadInterfaces = async () => {
  loading.value = true
  try {
    const res = await listInterfaceByPage({
      current: queryParams.current,
      pageSize: queryParams.pageSize,
      interfaceName: searchKeyword.value.trim() || undefined,
      method: methodFilter.value || undefined,
      sortField: 'createTime',
      sortOrder: 'desc',
    })
    if (res.data.code === 0 && res.data.data) {
      interfaceList.value = res.data.data.records ?? []
      total.value = res.data.data.total ?? 0
    } else {
      interfaceList.value = []
      total.value = 0
      message.error(res.data.message ?? '加载接口列表失败')
    }
  } catch (error) {
    console.error('加载接口列表失败：', error)
    interfaceList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 搜索 / 筛选
const handleSearch = () => {
  queryParams.current = 1
  loadInterfaces()
}

// 跳转详情
const goDetail = (item: API.InterfaceInfo) => {
  if (item.id != null) {
    router.push(`/interface/${item.id}`)
  }
}

onMounted(() => {
  loadInterfaces()
})
</script>

<style scoped>
.market-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 32px 0 64px;
}

.container {
  max-width: 1120px;
  margin: 0 auto;
  padding: 0 24px;
}

/* 顶部工具条 */
.toolbar-card {
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  padding: 28px 32px;
  margin-bottom: 24px;
}

.page-title {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
}

.page-desc {
  margin: 0 0 20px;
  font-size: 14px;
  color: #64748b;
}

.toolbar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar :deep(.ant-input-search) {
  flex: 1;
  max-width: 480px;
}

.toolbar :deep(.ant-input-search .ant-input) {
  border-radius: 10px;
}

.toolbar :deep(.ant-input-search .ant-btn) {
  border-radius: 0 10px 10px 0;
  background: #3b82f6;
}

.toolbar :deep(.ant-input-search .ant-btn:hover) {
  background: #2563eb;
}

.method-select {
  width: 160px;
}

/* 卡片网格 */
.list-spin {
  display: block;
  min-height: 300px;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.empty-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
}

.interface-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 24px;
  cursor: pointer;
  transition: all 0.25s ease;
  display: flex;
  flex-direction: column;
}

.interface-card:hover {
  transform: translateY(-3px);
  border-color: #bfdbfe;
  box-shadow: 0 12px 32px rgba(59, 130, 246, 0.12);
}

.card-top {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
}

.tag {
  border-radius: 8px;
  border: none;
  margin: 0;
}

.card-name {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-desc {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  color: #64748b;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}

.card-bottom {
  margin-top: 18px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.detail-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #3b82f6;
  font-size: 14px;
  font-weight: 500;
}

.interface-card:hover .detail-link {
  gap: 8px;
}

/* 分页 */
.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

@media (max-width: 768px) {
  .toolbar-card {
    padding: 20px;
  }

  .toolbar :deep(.ant-input-search) {
    max-width: 100%;
  }
}
</style>
