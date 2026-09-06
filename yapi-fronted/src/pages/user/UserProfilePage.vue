<template>
  <div class="profile-page">
    <div class="container">
      <!-- 我的信息 -->
      <div class="panel-card">
        <div class="card-head">
          <h3 class="card-title">我的信息</h3>
          <a-button type="primary" @click="openEdit">
            <template #icon>
              <EditOutlined />
            </template>
            编辑资料
          </a-button>
        </div>
        <div class="profile-body">
          <a-avatar :size="80" :src="loginUserStore.loginUser.userAvatar" class="profile-avatar">
            <template #icon>
              <UserOutlined />
            </template>
          </a-avatar>
          <div class="profile-meta">
            <div class="profile-name-row">
              <span class="profile-name">{{ loginUserStore.loginUser.userName || '未设置用户名' }}</span>
              <a-tag :color="roleInfo(loginUserStore.loginUser.userRole).color" class="meta-tag">
                {{ roleInfo(loginUserStore.loginUser.userRole).text }}
              </a-tag>
            </div>
            <p class="profile-desc">
              {{ loginUserStore.loginUser.userProfile || '这个人很懒，还没有写简介' }}
            </p>
            <div class="profile-extra">
              注册时间：{{ formatTime(loginUserStore.loginUser.createTime) || '-' }}
            </div>
          </div>
        </div>
      </div>

      <!-- 我创建的接口 -->
      <div class="panel-card">
        <div class="card-head">
          <h3 class="card-title">我创建的接口</h3>
          <a-button type="primary" @click="openCreate">
            <template #icon>
              <PlusOutlined />
            </template>
            新增接口
          </a-button>
        </div>
        <a-table
          :columns="createColumns"
          :data-source="createList"
          :loading="createLoading"
          :pagination="false"
          row-key="id"
          size="middle"
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
            <template v-else-if="column.key === 'applicantCount'">
              {{ record.applicantCount ?? '0' }}
            </template>
            <template v-else-if="column.key === 'action'">
              <a-space :size="4" wrap>
                <a-button type="link" size="small" @click="openInterfaceEdit(record)">修改</a-button>
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

      <!-- 我申请的接口 -->
      <div class="panel-card">
        <h3 class="card-title list-title">我申请的接口</h3>
        <a-table
          :columns="applyColumns"
          :data-source="applyList"
          :loading="applyLoading"
          :pagination="false"
          row-key="id"
          size="middle"
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
            <template v-else-if="column.key === 'leftNum'">
              {{ record.leftNum ?? '-' }}
            </template>
          </template>
        </a-table>
      </div>
    </div>

    <!-- 编辑资料弹窗 -->
    <a-modal
      v-model:open="editVisible"
      title="编辑个人信息"
      :confirm-loading="editSaving"
      @ok="handleEditSubmit"
    >
      <a-form layout="vertical" class="edit-form">
        <a-form-item label="用户名">
          <a-input v-model:value="editForm.userName" placeholder="请输入用户名" :maxlength="20" />
        </a-form-item>
        <a-form-item label="头像">
          <a-upload accept="image/*" :show-upload-list="false" :custom-request="onAvatarUpload">
            <div class="avatar-uploader">
              <img
                v-if="editForm.userAvatar"
                :src="editForm.userAvatar"
                alt="头像预览"
                class="avatar-preview"
              />
              <div v-else class="avatar-uploader-placeholder">
                <PlusOutlined />
                <span>点击上传</span>
              </div>
            </div>
          </a-upload>
          <div class="avatar-upload-tip">选择图片后自动上传，点击「确定」保存修改</div>
        </a-form-item>
        <a-form-item label="个人简介">
          <a-textarea
            v-model:value="editForm.userProfile"
            :rows="3"
            :maxlength="200"
            placeholder="介绍一下自己"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 新增/修改接口弹窗 -->
    <a-modal
      v-model:open="interfaceModalVisible"
      :title="interfaceModalTitle"
      :confirm-loading="interfaceSaving"
      :width="560"
      @ok="handleInterfaceSubmit"
    >
      <a-form layout="vertical" class="edit-form">
        <a-form-item label="接口名" required>
          <a-input v-model:value="interfaceForm.interfaceName" placeholder="请输入接口名称" :maxlength="50" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea
            v-model:value="interfaceForm.description"
            :rows="2"
            :maxlength="200"
            placeholder="接口用途说明，可留空"
          />
        </a-form-item>
        <a-form-item label="请求方法" required>
          <a-select v-model:value="interfaceForm.method" style="width: 160px" :options="methodOptions" />
        </a-form-item>
        <a-form-item label="接口地址（url）" required>
          <a-input
            v-model:value="interfaceForm.url"
            placeholder="http://...，如 http://localhost:8123/api/name/user"
          />
        </a-form-item>
        <a-form-item label="请求头">
          <a-textarea
            v-model:value="interfaceForm.requestHeader"
            :rows="2"
            placeholder='JSON 字符串，如 {"Content-Type":"application/json"}，可留空'
          />
        </a-form-item>
        <a-form-item label="请求参数">
          <a-textarea
            v-model:value="interfaceForm.requestParams"
            :rows="2"
            placeholder='JSON 字符串，如 {"username":"yapi"}，可留空'
          />
        </a-form-item>
        <a-form-item label="响应头">
          <a-textarea v-model:value="interfaceForm.responseHeader" :rows="2" placeholder="JSON 字符串，可留空" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { EditOutlined, PlusOutlined, UserOutlined } from '@ant-design/icons-vue'
import { userLoginUserStore } from '@/stores/loginUser.ts'
import { updateMyUser } from '@/api/userController.ts'
import { imageUpload } from '@/api/imageController.ts'
import {
  addInterfaceInfo,
  deleteInterface,
  listInterfaceCreate,
  listUserInterfaceApply,
  offlineInterface,
  publishInterface,
  updateInterfaceInfo,
} from '@/api/interfaceInfoController.ts'
import { formatTime } from '@/utils/time.ts'

const router = useRouter()
const loginUserStore = userLoginUserStore()

// ==================== 我创建的接口 ====================
const createList = ref<API.InterfaceInfoVO[]>([])
const createLoading = ref(false)
const createColumns = [
  { title: '接口名', dataIndex: 'interfaceName', key: 'interfaceName' },
  { title: '描述', dataIndex: 'description', key: 'description' },
  { title: '请求方法', dataIndex: 'method', key: 'method', width: 110 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '申请人数', dataIndex: 'applicantCount', key: 'applicantCount', width: 110 },
  { title: '操作', key: 'action', width: 200 },
]

const loadCreateList = async () => {
  createLoading.value = true
  try {
    const res = await listInterfaceCreate()
    if (res.data.code === 0 && res.data.data) {
      createList.value = res.data.data
    } else {
      createList.value = []
    }
  } catch (error) {
    console.error('加载我创建的接口失败：', error)
    createList.value = []
  } finally {
    createLoading.value = false
  }
}

// ==================== 我创建的接口：新增 / 修改 / 删除 / 上线 / 下线 ====================
const methodOptions = ['GET', 'POST', 'PUT', 'DELETE'].map((m) => ({ label: m, value: m }))

const interfaceModalVisible = ref(false)
const interfaceSaving = ref(false)
// 修改时的记录 id / path（新增时为空；path 不开放编辑，修改时原样带回）
const editingInterfaceId = ref<string | undefined>(undefined)
const editingInterfacePath = ref<string | undefined>(undefined)
const interfaceForm = reactive<API.InterfaceInfoUpdateRequest>({
  interfaceName: '',
  description: '',
  method: 'GET',
  url: '',
  requestHeader: '',
  requestParams: '',
  responseHeader: '',
})

const interfaceModalTitle = computed(() => (editingInterfaceId.value ? '修改接口' : '新增接口'))

const resetInterfaceForm = () => {
  interfaceForm.interfaceName = ''
  interfaceForm.description = ''
  interfaceForm.method = 'GET'
  interfaceForm.url = ''
  interfaceForm.requestHeader = ''
  interfaceForm.requestParams = ''
  interfaceForm.responseHeader = ''
}

// 新增
const openCreate = () => {
  editingInterfaceId.value = undefined
  editingInterfacePath.value = undefined
  resetInterfaceForm()
  interfaceModalVisible.value = true
}

// 修改：回填当前记录
const openInterfaceEdit = (record: API.InterfaceInfoVO) => {
  editingInterfaceId.value = record.id
  editingInterfacePath.value = record.path
  interfaceForm.interfaceName = record.interfaceName ?? ''
  interfaceForm.description = record.description ?? ''
  interfaceForm.method = record.method ?? 'GET'
  interfaceForm.url = record.url ?? ''
  interfaceForm.requestHeader = record.requestHeader ?? ''
  interfaceForm.requestParams = record.requestParams ?? ''
  interfaceForm.responseHeader = record.responseHeader ?? ''
  interfaceModalVisible.value = true
}

// 提交表单：新增调 addInterfaceInfo，修改调 updateInterfaceInfo
const handleInterfaceSubmit = async () => {
  if (!interfaceForm.interfaceName?.trim()) {
    message.warning('请输入接口名')
    return
  }
  if (!interfaceForm.url?.trim()) {
    message.warning('请输入接口地址')
    return
  }
  interfaceSaving.value = true
  try {
    const res = editingInterfaceId.value
      ? await updateInterfaceInfo({
          id: editingInterfaceId.value,
          // 修改时把原 path 一并带回，后端不支持在此编辑 path
          path: editingInterfacePath.value,
          interfaceName: interfaceForm.interfaceName,
          description: interfaceForm.description,
          method: interfaceForm.method,
          url: interfaceForm.url,
          requestHeader: interfaceForm.requestHeader,
          requestParams: interfaceForm.requestParams,
          responseHeader: interfaceForm.responseHeader,
        })
      : await addInterfaceInfo({
          // 新增不传 path，由后端自动生成
          interfaceName: interfaceForm.interfaceName,
          description: interfaceForm.description,
          method: interfaceForm.method,
          url: interfaceForm.url,
          requestHeader: interfaceForm.requestHeader,
          requestParams: interfaceForm.requestParams,
          responseHeader: interfaceForm.responseHeader,
        })
    if (res.data.code === 0) {
      message.success(editingInterfaceId.value ? '修改成功' : '新增成功')
      interfaceModalVisible.value = false
      loadCreateList()
    } else {
      message.error(res.data.message ?? '操作失败')
    }
  } catch (error) {
    console.error('保存接口失败：', error)
    message.error('操作失败，请重试')
  } finally {
    interfaceSaving.value = false
  }
}

// 删除（弹窗已二次确认）
const handleDelete = async (record: API.InterfaceInfoVO) => {
  try {
    const res = await deleteInterface({ id: record.id })
    if (res.data.code === 0) {
      message.success('删除成功')
      loadCreateList()
    } else {
      message.error(res.data.message ?? '删除失败')
    }
  } catch (error) {
    console.error('删除接口失败：', error)
    message.error('删除失败，请重试')
  }
}

// 上线（status=0 已下线 / status=2 管理员下架 时显示）
const handlePublish = async (record: API.InterfaceInfoVO) => {
  try {
    const res = await publishInterface({ id: record.id })
    if (res.data.code === 0) {
      message.success('上线成功')
      loadCreateList()
    } else {
      message.error(res.data.message ?? '上线失败')
    }
  } catch (error) {
    console.error('上线失败：', error)
    message.error('上线失败，请重试')
  }
}

// 下线（status=1 已上线时显示）
const handleOffline = async (record: API.InterfaceInfoVO) => {
  try {
    const res = await offlineInterface({ id: record.id })
    if (res.data.code === 0) {
      message.success('下线成功')
      loadCreateList()
    } else {
      message.error(res.data.message ?? '下线失败')
    }
  } catch (error) {
    console.error('下线失败：', error)
    message.error('下线失败，请重试')
  }
}

// ==================== 我申请的接口 ====================
const applyList = ref<API.InterfaceInfoVO[]>([])
const applyLoading = ref(false)
const applyColumns = [
  { title: '接口名', dataIndex: 'interfaceName', key: 'interfaceName' },
  { title: '描述', dataIndex: 'description', key: 'description' },
  { title: '请求方法', dataIndex: 'method', key: 'method', width: 110 },
  { title: '剩余调用次数', dataIndex: 'leftNum', key: 'leftNum', width: 130 },
]

const loadApplyList = async () => {
  applyLoading.value = true
  try {
    const res = await listUserInterfaceApply()
    if (res.data.code === 0 && res.data.data) {
      applyList.value = res.data.data
    } else {
      applyList.value = []
    }
  } catch (error) {
    console.error('加载我申请的接口失败：', error)
    applyList.value = []
  } finally {
    applyLoading.value = false
  }
}

// ==================== 编辑资料 ====================
const editVisible = ref(false)
const editSaving = ref(false)
const editForm = reactive<API.UserUpdateMyRequest>({
  userName: '',
  userAvatar: '',
  userProfile: '',
})

const openEdit = () => {
  editForm.userName = loginUserStore.loginUser.userName ?? ''
  editForm.userAvatar = loginUserStore.loginUser.userAvatar ?? ''
  editForm.userProfile = loginUserStore.loginUser.userProfile ?? ''
  editVisible.value = true
}

// 头像上传（a-upload 的 custom-request）：调 imageUpload，不走 action 直连后端
const onAvatarUpload = async ({ file, onSuccess, onError }: any) => {
  try {
    const res = await imageUpload(file)
    if (res.data.code === 0) {
      // 上传成功：把返回的图片 URL 写入表单，提交时才保存到后端
      editForm.userAvatar = res.data.data
      onSuccess?.(res.data, file)
      message.success('头像上传成功')
    } else {
      onError?.(new Error(res.data.message ?? '头像上传失败'))
      message.error(res.data.message ?? '头像上传失败')
    }
  } catch (error: any) {
    onError?.(error)
    message.error('头像上传失败，请重试')
  }
}

const handleEditSubmit = async () => {
  if (!editForm.userName?.trim()) {
    message.warning('用户名不能为空')
    return
  }
  editSaving.value = true
  try {
    const res = await updateMyUser({
      userName: editForm.userName,
      userAvatar: editForm.userAvatar,
      userProfile: editForm.userProfile,
    })
    if (res.data.code === 0) {
      message.success('修改成功')
      editVisible.value = false
      // 重新拉取登录用户信息，刷新页面展示
      await loginUserStore.fetchLoginUser()
    } else {
      message.error(res.data.message ?? '修改失败')
    }
  } catch (error) {
    console.error('修改个人信息失败：', error)
    message.error('修改失败，请重试')
  } finally {
    editSaving.value = false
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

// 接口状态文案与颜色（0=关闭 1=发布 2=管理员下架）
const statusInfo = (status?: number): { text: string; color: string } => {
  const map: Record<number, { text: string; color: string }> = {
    0: { text: '已下线', color: 'default' },
    1: { text: '已上线', color: 'green' },
    2: { text: '已下架', color: 'red' },
  }
  return map[status ?? -1] ?? { text: '未知', color: 'default' }
}

// 用户角色文案与颜色（user=普通用户 admin=管理员）
const roleInfo = (role?: string): { text: string; color: string } => {
  const map: Record<string, { text: string; color: string }> = {
    admin: { text: '管理员', color: 'geekblue' },
    user: { text: '普通用户', color: 'default' },
  }
  return map[role ?? ''] ?? { text: role || '-', color: 'default' }
}

// 跳转接口详情
const goDetail = (record: API.InterfaceInfoVO) => {
  if (record.id != null) {
    router.push(`/interface/${record.id}`)
  }
}

onMounted(async () => {
  // 拉取登录用户（未登录时列表接口会触发 request.ts 的 40100 跳转登录页）
  await loginUserStore.fetchLoginUser()
  loadCreateList()
  loadApplyList()
})
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 32px 0 64px;
}

.container {
  max-width: 1120px;
  margin: 0 auto;
  padding: 0 24px;
}

/* 面板卡片 */
.panel-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 28px 32px;
  margin-bottom: 24px;
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

.list-title {
  margin-bottom: 16px;
}

/* 我的信息 */
.profile-body {
  display: flex;
  align-items: flex-start;
  gap: 24px;
}

.profile-avatar {
  flex-shrink: 0;
  border: 3px solid #eff6ff;
  background: #eff6ff;
}

.profile-name-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.profile-name {
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
}

.profile-desc {
  margin: 10px 0 12px;
  font-size: 14px;
  line-height: 1.7;
  color: #64748b;
}

.profile-extra {
  font-size: 13px;
  color: #94a3b8;
}

/* 表格相关 */
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

.edit-form {
  margin-top: 8px;
}

/* 头像上传区域（点击上传） */
.avatar-uploader {
  width: 96px;
  height: 96px;
  border: 1px dashed #bfdbfe;
  border-radius: 12px;
  background: #eff6ff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.25s ease;
}

.avatar-uploader:hover {
  border-color: #2563eb;
  background: #dbeafe;
}

.avatar-uploader-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: #2563eb;
  font-size: 12px;
}

.avatar-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.avatar-upload-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #94a3b8;
}

@media (max-width: 768px) {
  .panel-card {
    padding: 20px;
  }

  .profile-body {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .profile-name-row {
    justify-content: center;
  }
}
</style>
