<template>
  <div class="manage-page">
    <div class="container">
      <div class="panel-card">
        <div class="card-head">
          <h3 class="card-title">用户管理</h3>
          <a-button type="primary" @click="openCreate">
            <template #icon>
              <PlusOutlined />
            </template>
            新增用户
          </a-button>
        </div>

        <!-- 搜索工具条 -->
        <div class="toolbar">
          <a-input-search
            v-model:value="searchParams.userName"
            placeholder="输入用户名"
            allow-clear
            enter-button="搜索"
            size="large"
            @search="doSearch"
          />
        </div>

        <!-- 表格 -->
        <a-table
          class="manage-table"
          :columns="columns"
          :data-source="data"
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
            <template v-if="column.key === 'userAvatar'">
              <a-avatar :size="48" :src="record.userAvatar">
                <template #icon>
                  <UserOutlined />
                </template>
              </a-avatar>
            </template>
            <template v-else-if="column.key === 'userRole'">
              <a-tag :color="record.userRole === 'admin' ? 'green' : 'blue'" class="meta-tag">
                {{ record.userRole === 'admin' ? '管理员' : '普通用户' }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'createTime'">
              {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
            </template>
            <template v-else-if="column.key === 'action'">
              <a-space :size="4" wrap>
                <a-button type="link" size="small" @click="openEdit(record)">修改</a-button>
                <a-popconfirm
                  title="确定要删除这个用户吗？"
                  ok-text="删除"
                  cancel-text="取消"
                  @confirm="doDelete(record.id)"
                >
                  <a-button type="link" size="small" danger>删除</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </div>
    </div>

    <!-- 新增/修改用户弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      :confirm-loading="saving"
      @ok="handleSubmit"
    >
      <a-form layout="vertical" class="edit-form">
        <a-form-item label="用户名" required>
          <a-input v-model:value="userForm.userName" placeholder="请输入用户名" :maxlength="20" />
        </a-form-item>
        <a-form-item v-if="!editingUserId" label="账号" required>
          <a-input v-model:value="userAccount" placeholder="请输入登录账号" :maxlength="20" />
        </a-form-item>
        <a-form-item label="头像链接">
          <a-input v-model:value="userForm.userAvatar" placeholder="请输入头像图片 URL" />
        </a-form-item>
        <a-form-item v-if="editingUserId" label="简介">
          <a-textarea
            v-model:value="userForm.userProfile"
            :rows="2"
            :maxlength="200"
            placeholder="用户简介，可留空"
          />
        </a-form-item>
        <a-form-item label="角色" required>
          <a-select v-model:value="userForm.userRole" style="width: 160px" :options="roleOptions" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, UserOutlined } from '@ant-design/icons-vue'
import { addUser, deleteUser, listUserVoByPage, updateUser } from '@/api/userController.ts'
import dayjs from 'dayjs'

// ==================== 列表数据 ====================
const data = ref<API.UserVO[]>([])
const total = ref(0)
const loading = ref(false)

// 搜索 + 分页条件（类型以 typings.d.ts 为准：current / pageSize）
const searchParams = reactive<API.UserQueryRequest>({
  current: 1,
  pageSize: 10,
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await listUserVoByPage({
      current: searchParams.current,
      pageSize: searchParams.pageSize,
      userName: searchParams.userName?.trim() || undefined,
    })
    if (res.data.code === 0 && res.data.data) {
      data.value = res.data.data.records ?? []
      total.value = res.data.data.total ?? 0
    } else {
      data.value = []
      total.value = 0
      message.error(res.data.message ?? '获取数据失败')
    }
  } catch (error) {
    console.error('加载用户列表失败：', error)
    data.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 分页参数（total 取自 res.data.data.total）
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
  fetchData()
}

const doSearch = () => {
  // 搜索时重置页码
  searchParams.current = 1
  fetchData()
}

// ==================== 新增 / 修改用户 ====================
const modalVisible = ref(false)
const saving = ref(false)
const editingUserId = ref<string | undefined>(undefined)
// userAccount 仅新增时使用（UserUpdateRequest 无此字段，用独立 ref 承载）
const userAccount = ref('')

const roleOptions = [
  { label: '普通用户', value: 'user' },
  { label: '管理员', value: 'admin' },
]

const userForm = reactive<API.UserUpdateRequest>({
  userName: '',
  userAvatar: '',
  userProfile: '',
  userRole: 'user',
})

const modalTitle = computed(() => (editingUserId.value ? '修改用户' : '新增用户'))

const openCreate = () => {
  editingUserId.value = undefined
  userForm.userName = ''
  userForm.userAvatar = ''
  userForm.userProfile = ''
  userForm.userRole = 'user'
  userAccount.value = ''
  modalVisible.value = true
}

const openEdit = (record: API.UserVO) => {
  editingUserId.value = record.id
  userForm.userName = record.userName ?? ''
  userForm.userAvatar = record.userAvatar ?? ''
  userForm.userProfile = record.userProfile ?? ''
  userForm.userRole = record.userRole ?? 'user'
  userAccount.value = ''
  modalVisible.value = true
}

const handleSubmit = async () => {
  if (!userForm.userName?.trim()) {
    message.warning('请输入用户名')
    return
  }
  if (!editingUserId.value && !userAccount.value.trim()) {
    message.warning('请输入账号')
    return
  }
  saving.value = true
  try {
    const res = editingUserId.value
      ? await updateUser({
          id: editingUserId.value,
          userName: userForm.userName,
          userAvatar: userForm.userAvatar,
          userProfile: userForm.userProfile,
          userRole: userForm.userRole,
        })
      : await addUser({
          userName: userForm.userName,
          userAccount: userAccount.value.trim(),
          userAvatar: userForm.userAvatar,
          userRole: userForm.userRole,
        })
    if (res.data.code === 0) {
      message.success(editingUserId.value ? '修改成功' : '新增成功')
      modalVisible.value = false
      fetchData()
    } else {
      message.error(res.data.message ?? '操作失败')
    }
  } catch (error) {
    console.error('保存用户失败：', error)
    message.error('操作失败，请重试')
  } finally {
    saving.value = false
  }
}

// ==================== 删除用户 ====================
const doDelete = async (id: string | undefined) => {
  if (!id) {
    return
  }
  try {
    const res = await deleteUser({ id })
    if (res.data.code === 0) {
      message.success('删除成功')
      fetchData()
    } else {
      message.error(res.data.message ?? '删除失败')
    }
  } catch (error) {
    console.error('删除用户失败：', error)
    message.error('删除失败，请重试')
  }
}

onMounted(() => {
  fetchData()
})

const columns = [
  { title: 'id', dataIndex: 'id', key: 'id', width: 180, ellipsis: true },
  { title: '用户名', dataIndex: 'userName', key: 'userName' },
  { title: '头像', dataIndex: 'userAvatar', key: 'userAvatar', width: 90 },
  { title: '简介', dataIndex: 'userProfile', key: 'userProfile' },
  { title: '角色', dataIndex: 'userRole', key: 'userRole', width: 110 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 130 },
]
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

.meta-tag {
  margin: 0;
}

.edit-form {
  margin-top: 8px;
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
