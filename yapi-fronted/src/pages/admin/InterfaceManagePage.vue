<template>
  <div class="manage-page">
    <div class="container">
      <div class="panel-card">
        <div class="card-head">
          <h3 class="card-title">接口管理</h3>
        </div>

        <!-- 筛选工具条 -->
        <div class="toolbar">
          <a-input-search
            v-model:value="nameKeyword"
            placeholder="搜索接口名"
            allow-clear
            enter-button="搜索"
            size="large"
            @search="doSearch"
          />
          <a-select
            v-model:value="statusFilter"
            size="large"
            class="status-select"
            placeholder="全部状态"
            allow-clear
            :options="statusOptions"
            @change="doSearch"
          />
        </div>

        <!-- 表格 -->
        <a-table
          class="manage-table"
          :columns="columns"
          :data-source="dataList"
          :loading="loading"
          :pagination="pagination"
          row-key="id"
          size="middle"
          @change="onTableChange"
        >
          <template #emptyText>
            <a-empty description="暂无数据" />
          </template>
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'interfaceName'">
              <a class="name-link" @click="goDetail(record)">{{ record.interfaceName }}</a>
            </template>
            <template v-else-if="column.key === 'description'">
              <span class="desc-cell">{{ record.description || '暂无描述' }}</span>
            </template>
            <template v-else-if="column.key === 'method'">
              <a-tag :color="methodTagColor(record.method)" class="meta-tag">{{ record.method ?? '-' }}</a-tag>
            </template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="statusInfo(record.status).color" class="meta-tag">
                {{ statusInfo(record.status).text }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'userId'">
              <span class="id-cell">{{ record.userId ?? '-' }}</span>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-space :size="4" wrap>
                <a-button v-if="record.status === 1" type="link" size="small" @click="handleOffline(record)">
                  下线
                </a-button>
                <a-button v-else type="link" size="small" @click="handlePublish(record)">上线</a-button>
                <a-popconfirm
                  title="确定删除该接口吗？删除后不可恢复"
                  ok-text="删除"
                  cancel-text="取消"
                  @confirm="handleDelete(record)"
                >
                  <a-button type="link" size="small" danger>删除</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  deleteInterface,
  listInterfaceByPageByAdmin,
  offlineInterface,
  publishInterface,
} from '@/api/interfaceInfoController.ts'

const router = useRouter()

// ==================== 列表数据 ====================
const dataList = ref<API.InterfaceInfo[]>([])
const total = ref(0)
const loading = ref(false)
const nameKeyword = ref('')
const statusFilter = ref<number | undefined>(undefined)

const searchParams = reactive<API.InterfaceInfoQueryRequest>({
  current: 1,
  pageSize: 10,
})

const statusOptions = [
  { label: '已上线', value: 1 },
  { label: '已下线', value: 0 },
  { label: '已下架', value: 2 },
]

const columns = [
  { title: '接口名', dataIndex: 'interfaceName', key: 'interfaceName' },
  { title: '描述', dataIndex: 'description', key: 'description' },
  { title: '请求方法', dataIndex: 'method', key: 'method', width: 110 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '创建人', dataIndex: 'userId', key: 'userId', width: 200 },
  { title: '操作', key: 'action', width: 170 },
]

// 加载所有接口（含下线/下架），支持接口名 + 状态筛选
const loadList = async () => {
  loading.value = true
  try {
    const res = await listInterfaceByPageByAdmin({
      current: searchParams.current,
      pageSize: searchParams.pageSize,
      interfaceName: nameKeyword.value.trim() || undefined,
      status: statusFilter.value,
      sortField: 'createTime',
      sortOrder: 'desc',
    })
    if (res.data.code === 0 && res.data.data) {
      dataList.value = res.data.data.records ?? []
      total.value = res.data.data.total ?? 0
    } else {
      dataList.value = []
      total.value = 0
      message.error(res.data.message ?? '加载接口列表失败')
    }
  } catch (error) {
    console.error('加载接口列表失败：', error)
    dataList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const pagination = computed(() => ({
  current: searchParams.current,
  pageSize: searchParams.pageSize,
  total: total.value,
  showSizeChanger: true,
  showTotal: (t: number) => `共 ${t} 条`,
}))

const onTableChange = (page: { current: number; pageSize: number }) => {
  searchParams.current = page.current
  searchParams.pageSize = page.pageSize
  loadList()
}

const doSearch = () => {
  searchParams.current = 1
  loadList()
}

// ==================== 上线 / 下线 / 删除 ====================
const handlePublish = async (record: API.InterfaceInfo) => {
  try {
    const res = await publishInterface({ id: record.id })
    if (res.data.code === 0) {
      message.success('上线成功')
      loadList()
    } else {
      message.error(res.data.message ?? '上线失败')
    }
  } catch (error) {
    console.error('上线失败：', error)
    message.error('上线失败，请重试')
  }
}

const handleOffline = async (record: API.InterfaceInfo) => {
  try {
    const res = await offlineInterface({ id: record.id })
    if (res.data.code === 0) {
      message.success('下线成功')
      loadList()
    } else {
      message.error(res.data.message ?? '下线失败')
    }
  } catch (error) {
    console.error('下线失败：', error)
    message.error('下线失败，请重试')
  }
}

const handleDelete = async (record: API.InterfaceInfo) => {
  try {
    const res = await deleteInterface({ id: record.id })
    if (res.data.code === 0) {
      message.success('删除成功')
      loadList()
    } else {
      message.error(res.data.message ?? '删除失败')
    }
  } catch (error) {
    console.error('删除接口失败：', error)
    message.error('删除失败，请重试')
  }
}

// ==================== 通用展示映射 ====================
// 请求方法标签预设色（浅色底 + 彩色字）
const methodTagColor = (method?: string): string => {
  const map: Record<string, string> = {
    GET: 'blue',
    POST: 'green',
    PUT: 'orange',
    DELETE: 'red',
  }
  return map[(method ?? '').toUpperCase()] ?? 'default'
}

// 接口状态文案与颜色（0=关闭/已下线 1=发布/已上线 2=管理员下架）
const statusInfo = (status?: number): { text: string; color: string } => {
  const map: Record<number, { text: string; color: string }> = {
    0: { text: '已下线', color: 'default' },
    1: { text: '已上线', color: 'green' },
    2: { text: '已下架', color: 'red' },
  }
  return map[status ?? -1] ?? { text: '未知', color: 'default' }
}

// 跳转接口详情
const goDetail = (record: API.InterfaceInfo) => {
  if (record.id != null) {
    router.push(`/interface/${record.id}`)
  }
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.manage-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 32px 0 64px;
}

.container {
  max-width: 1120px;
  margin: 0 auto;
  padding: 0 24px;
}

.panel-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 28px 32px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.card-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.toolbar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.toolbar :deep(.ant-input-search) {
  flex: 1;
  max-width: 480px;
}

.toolbar :deep(.ant-input-search .ant-btn) {
  background: #3b82f6;
  border-radius: 0 10px 10px 0;
}

.toolbar :deep(.ant-input-search .ant-btn:hover) {
  background: #2563eb;
}

.status-select {
  width: 160px;
}

.meta-tag {
  margin: 0;
}

.name-link {
  color: #2563eb;
  font-weight: 500;
  cursor: pointer;
}

.name-link:hover {
  text-decoration: underline;
}

.desc-cell {
  color: #64748b;
}

.id-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
  color: #64748b;
}

@media (max-width: 768px) {
  .panel-card {
    padding: 20px;
  }

  .toolbar :deep(.ant-input-search) {
    max-width: 100%;
  }
}
</style>
