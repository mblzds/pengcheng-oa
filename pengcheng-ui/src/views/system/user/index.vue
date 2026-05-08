<template>
  <div class="page-container">
    <!-- 用户列表 -->
    <n-card class="user-list-card" size="small">
      <!-- 搜索表单 -->
      <div class="search-form">
        <n-form inline :model="searchForm" label-placement="left">
          <n-form-item label="用户名">
            <n-input v-model:value="searchForm.username" placeholder="请输入用户名" clearable />
          </n-form-item>
          <n-form-item label="用户类型">
            <n-select
              v-model:value="searchForm.userType"
              placeholder="请选择用户类型"
              :options="userTypeOptions"
              clearable
              style="width: 140px"
            />
          </n-form-item>
          <n-form-item label="状态">
            <n-select
              v-model:value="searchForm.status"
              placeholder="请选择状态"
              :options="statusOptions"
              clearable
              style="width: 120px"
            />
          </n-form-item>
          <n-form-item>
            <n-space>
              <n-button type="primary" @click="handleSearch">
                <template #icon><n-icon><SearchOutline /></n-icon></template>
                搜索
              </n-button>
              <n-button @click="handleReset">
                <template #icon><n-icon><RefreshOutline /></n-icon></template>
                重置
              </n-button>
            </n-space>
          </n-form-item>
        </n-form>
      </div>

      <!-- 工具栏 -->
      <div class="table-toolbar">
        <n-button v-if="hasPermission('sys:user:add')" type="primary" @click="handleAdd">
          <template #icon><n-icon><AddOutline /></n-icon></template>
          新增用户
        </n-button>
      </div>

      <!-- 表格 -->
      <n-data-table
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :row-key="(row: SysUser) => row.id"
        remote
      />

      <div class="pagination-container">
        <span class="pg-info">共 {{ pagination.itemCount }} 条</span>
        <span class="pg-sep">|</span>
        <n-pagination
          v-model:page="pagination.page"
          :page-size="pagination.pageSize"
          :item-count="pagination.itemCount"
          @update:page="handlePageChange"
        >
          <template #prev><span class="pg-step">上一页</span></template>
          <template #next><span class="pg-step">下一页</span></template>
        </n-pagination>
        <span class="pg-sep">|</span>
        <span class="pg-jumper">
          跳至
          <n-input-number
            v-model:value="jumperValue"
            :min="1"
            :max="pageCount || 1"
            :show-button="false"
            size="small"
            style="width: 56px; margin: 0 4px"
            @keydown.enter="handleJump"
            @blur="handleJump"
          />
          页
        </span>
        <span class="pg-sep">|</span>
        <n-select
          :value="pagination.pageSize"
          :options="pageSizeOptions"
          size="small"
          style="width: 96px"
          @update:value="handlePageSizeChange"
        />
      </div>
    </n-card>

    <!-- 新增/编辑弹窗 -->
    <n-modal
      v-model:show="modalVisible"
      :title="modalTitle"
      preset="card"
      style="width: 600px"
      :mask-closable="false"
    >
      <n-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-placement="left"
        label-width="80"
        class="modal-form"
      >
        <n-form-item label="用户名" path="username">
          <n-input v-model:value="formData.username" placeholder="请输入用户名" :disabled="!!formData.id" />
        </n-form-item>
        <n-form-item v-if="!formData.id" label="密码" path="password">
          <n-input v-model:value="formData.password" type="password" placeholder="请输入密码，留空默认123456" show-password-on="click" />
        </n-form-item>
        <n-form-item label="角色" path="roleId">
          <n-select
            v-model:value="roleId"
            :options="roleOptions"
            placeholder="请选择角色"
            clearable
          />
        </n-form-item>
        <n-form-item label="归属部门" path="deptId">
          <n-tree-select
            v-model:value="formData.deptId"
            :options="deptOptions"
            key-field="id"
            label-field="deptName"
            children-field="children"
            placeholder="请选择归属部门"
            clearable
            default-expand-all
          />
        </n-form-item>
        <n-form-item label="邮箱" path="email">
          <n-input v-model:value="formData.email" placeholder="请输入邮箱" />
        </n-form-item>
        <n-form-item label="手机号" path="phone">
          <n-input v-model:value="formData.phone" placeholder="请输入手机号" />
        </n-form-item>
        <n-form-item label="性别" path="gender">
          <n-radio-group v-model:value="formData.gender">
            <n-radio :value="1">男</n-radio>
            <n-radio :value="2">女</n-radio>
            <n-radio :value="0">未知</n-radio>
          </n-radio-group>
        </n-form-item>
        <n-form-item label="用户类型" path="userType">
          <n-select
            v-model:value="formData.userType"
            :options="userTypeOptions"
            placeholder="请选择用户类型"
          />
        </n-form-item>
        <n-form-item label="岗位" path="postIds">
          <n-select
            v-model:value="postIds"
            multiple
            :options="postOptions"
            placeholder="请选择岗位"
          />
        </n-form-item>
        <n-form-item label="状态" path="status">
          <n-switch v-model:value="formData.status" :checked-value="1" :unchecked-value="0">
            <template #checked>启用</template>
            <template #unchecked>禁用</template>
          </n-switch>
        </n-form-item>

      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="modalVisible = false">取消</n-button>
          <n-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted, computed, type HTMLAttributes } from 'vue'
import { useRoute } from 'vue-router'
import { NButton, NTag, NSpace, NDropdown, NPagination, useMessage, useDialog, type DataTableColumns, type FormInst, type FormRules, type TreeOption } from 'naive-ui'
import { SearchOutline, RefreshOutline, AddOutline, ChevronDownOutline } from '@vicons/ionicons5'
import { userApi, roleApi, postApi, type SysUser, type SysRole } from '@/api/system'
import { deptApi, type SysDept } from '@/api/org'
import { useUserStore } from '@/stores/user'

const message = useMessage()
const dialog = useDialog()
const userStore = useUserStore()
const route = useRoute()

// 权限检查
const hasPermission = (permission: string) => userStore.hasPermission(permission)

// ==================== 部门选择 ====================
const deptOptions = ref<SysDept[]>([])
const selectedDeptId = ref<number | undefined>(undefined)

// 加载部门树（用于下拉选择）
async function loadDeptOptions() {
  try {
    const tree = await deptApi.tree()
    deptOptions.value = tree
  } catch (error) {
    // 错误已在拦截器处理
  }
}

// ==================== 搜索表单 ====================
const searchForm = reactive({
  username: '',
  status: null as number | null,
  userType: null as string | null
})

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 },
  { label: '待审核', value: 2 },
  { label: '审核拒绝', value: 3 }
]

// ==================== 表格 ====================
const tableData = ref<SysUser[]>([])
const loading = ref(false)
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0
})

const pageCount = computed(() => Math.ceil(pagination.itemCount / pagination.pageSize))

// 自定义分页布局所需状态
const jumperValue = ref<number | null>(null)
const pageSizeOptions = [
  { label: '每页 10 条', value: 10 },
  { label: '每页 20 条', value: 20 },
  { label: '每页 50 条', value: 50 },
  { label: '每页 100 条', value: 100 }
]
function handleJump() {
  const v = jumperValue.value
  if (v && v >= 1 && v <= pageCount.value && v !== pagination.page) {
    pagination.page = v
    loadData()
  }
  jumperValue.value = null
}

const roleOptions = ref<Array<{ label: string; value: number }>>([])

const userTypeOptions = [
  { label: '后台管理员', value: 'admin' },
  { label: 'PC前台用户', value: 'pc' },
  { label: 'App/小程序用户', value: 'app' }
]

const columns: DataTableColumns<SysUser> = [
  { title: 'ID', key: 'id', width: 60 },
  { title: '用户名', key: 'username', width: 100 },
  { title: '部门', key: 'deptName', width: 100, render(row) {
    return row.deptName || '-'
  }},
  { title: '岗位', key: 'postNames', width: 150, render(row) {
    return row.postNames || '-'
  }},
  {
    title: '用户类型',
    key: 'userType',
    width: 110,
    render(row) {
      const typeMap: Record<string, { type: 'info' | 'success' | 'warning'; label: string }> = {
        admin: { type: 'info', label: '后台管理员' },
        pc: { type: 'success', label: 'PC前台' },
        app: { type: 'warning', label: 'App/小程序' }
      }
      const t = typeMap[row.userType || 'admin'] || { type: 'info', label: row.userType || '未知' }
      return h(NTag, { type: t.type, size: 'small' }, { default: () => t.label })
    }
  },
  { title: '手机号', key: 'phone', width: 120 , render(row) {
      return row.phone || '-'
    }},
  {
    title: '离职',
    key: 'isQuit',
    width: 80,
    render(row) {
      const quit = row.isQuit === 1
      return h(NTag, { type: quit ? 'error' : 'success', size: 'small' }, { default: () => (quit ? '是' : '否') })
    }
  },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render(row) {
      const statusMap: Record<number, { type: 'success' | 'error' | 'warning' | 'info'; label: string }> = {
        0: { type: 'error', label: '禁用' },
        1: { type: 'success', label: '启用' },
        2: { type: 'warning', label: '待审核' },
        3: { type: 'error', label: '审核拒绝' }
      }
      const status = statusMap[row.status] || { type: 'info', label: '未知' }
      return h(NTag, { type: status.type, size: 'small' }, { default: () => status.label })
    }
  },
  { title: '创建时间', key: 'createTime', width: 110, render(row) {
    return row.createTime ? row.createTime.slice(0, 10) : '-'
  }},
  {
    title: '操作',
    key: 'actions',
    width: 240,
    fixed: 'right',
    render(row) {
      const buttons = []
      // 待审核状态显示审核按钮
      if (row.status === 2 && hasPermission('sys:user:edit')) {
        buttons.push(
          h(NButton, { size: 'small', type: 'success', onClick: () => handleApprove(row) }, { default: () => '通过' })
        )
        buttons.push(
          h(NButton, { size: 'small', type: 'error', onClick: () => handleReject(row) }, { default: () => '拒绝' })
        )
      }
      if (hasPermission('sys:user:edit')) {
        buttons.push(
          h(NButton, { size: 'small', onClick: () => handleEdit(row) }, { default: () => '编辑' })
        )
      }
      if (hasPermission('sys:user:delete')) {
        buttons.push(
          h(NButton, { size: 'small', type: 'error', onClick: () => handleDelete(row) }, { default: () => '删除' })
        )
      }

      // 更多操作
      if (hasPermission('sys:user:edit')) {
        const moreOptions = []

        // 重置密码移入更多
        if (row.status !== 2) {
          moreOptions.push({
            label: '重置密码',
            key: 'resetPassword'
          })
        }

        moreOptions.push({
          label: row.isQuit === 1 ? '取消离职' : '离职',
          key: 'toggleQuit'
        })

        buttons.push(
          h(
            NDropdown,
            {
              trigger: 'click',
              options: moreOptions,
              onSelect: (key) => {
                if (key === 'toggleQuit') {
                  handleToggleQuit(row)
                } else if (key === 'resetPassword') {
                  handleResetPassword(row)
                }
              }
            },
            {
              default: () =>
                h(
                  NButton,
                  { size: 'small' },
                  {
                    default: () => '更多',
                    icon: () => h(ChevronDownOutline)
                  }
                )
            }
          )
        )
      }

      return buttons.length > 0 ? h(NSpace, null, { default: () => buttons }) : '-'
    }
  }
]

// ==================== 弹窗 ====================
const modalVisible = ref(false)
const modalTitle = ref('新增用户')
const formRef = ref<FormInst | null>(null)
const submitLoading = ref(false)
const roleId = ref<number | null>(null)
const postIds = ref<number[]>([])
const postOptions = ref<Array<{ label: string; value: number }>>([])

const formData = reactive<SysUser>({
  id: undefined,
  deptId: null,
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  gender: 0,
  status: 1,
  userType: 'admin',
  remark: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  deptId: [
    {
      required: true,
      type: 'number',
      message: '请选择归属部门',
      trigger: ['change', 'blur']
    }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: ['input', 'blur'] },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: ['input', 'blur'] }
  ],
  roleId: [
    {
      required: true,
      validator() {
        if (roleId.value == null) {
          return new Error('请选择角色')
        }
        return true
      },
      trigger: ['change', 'blur']
    }
  ]
}

// ==================== 数据加载 ====================
async function loadData() {
  loading.value = true
  try {
    const res = await userApi.page({
      page: pagination.page,
      pageSize: pagination.pageSize,
      username: searchForm.username || undefined,
      status: searchForm.status ?? undefined,
      userType: searchForm.userType || undefined,
      deptId: selectedDeptId.value
    })
    tableData.value = res.list
    pagination.itemCount = Number(res.total)
  } catch (error) {
    // 错误已在拦截器处理
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  try {
    const roles = await roleApi.list()
    roleOptions.value = roles.map((role: SysRole) => ({
      label: role.name,
      value: role.id!
    }))
  } catch (error) {
    // 错误已在拦截器处理
  }
}

async function loadPostOptions() {
  try {
    const posts = await postApi.list()
    postOptions.value = posts.map(p => ({
      label: p.postName,
      value: p.id!
    }))
  } catch (error) {
    // 错误已在拦截器处理
  }
}

// ==================== 操作方法 ====================
function handleSearch() {
  pagination.page = 1
  loadData()
}

function handleReset() {
  searchForm.username = ''
  searchForm.status = null
  searchForm.userType = null
  selectedDeptId.value = undefined
  handleSearch()
}

function handlePageChange(page: number) {
  pagination.page = page
  loadData()
}

function handlePageSizeChange(pageSize: number) {
  pagination.pageSize = pageSize
  pagination.page = 1
  loadData()
}

function handleAdd() {
  modalTitle.value = '新增用户'
  Object.assign(formData, {
    id: undefined,
    deptId: null,
    username: '',
    password: '',
    nickname: '',
    email: '',
    phone: '',
    gender: 0,
    status: 1,
    userType: 'admin',
    remark: ''
  })
  roleId.value = null
  postIds.value = []
  modalVisible.value = true
}

async function handleEdit(row: SysUser) {
  modalTitle.value = '编辑用户'
  try {
    const res = await userApi.detail(row.id!)
    Object.assign(formData, res.user)
    roleId.value = res.roleIds?.[0] ?? null
    postIds.value = res.postIds
    modalVisible.value = true
  } catch (error) {
    // 错误已在拦截器处理
  }
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
    submitLoading.value = true

    if (!formData.id && !formData.nickname?.trim()) {
      formData.nickname = formData.username
    }

    const data = {
      user: { ...formData },
      roleIds: roleId.value != null ? [roleId.value] : [],
      postIds: postIds.value
    }

    if (formData.id) {
      await userApi.update(data)
      message.success('更新成功')
    } else {
      await userApi.create(data)
      message.success('创建成功')
    }

    modalVisible.value = false
    loadData()
  } catch (error) {
    // 错误已在拦截器处理
  } finally {
    submitLoading.value = false
  }
}

function handleDelete(row: SysUser) {
  dialog.warning({
    title: '提示',
    content: `确定要删除用户"${row.username}"吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await userApi.delete(row.id!)
        message.success('删除成功')
        loadData()
      } catch (error) {
        // 错误已在拦截器处理
      }
    }
  })
}

// 审核通过
function handleApprove(row: SysUser) {
  dialog.success({
    title: '审核通过',
    content: `确定通过用户"${row.username}"的注册申请吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await userApi.approve(row.id!)
        message.success('审核通过')
        loadData()
      } catch (error) {
        // 错误已在拦截器处理
      }
    }
  })
}

// 审核拒绝
function handleReject(row: SysUser) {
  dialog.warning({
    title: '审核拒绝',
    content: `确定拒绝用户"${row.username}"的注册申请吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await userApi.reject(row.id!)
        message.success('已拒绝')
        loadData()
      } catch (error) {
        // 错误已在拦截器处理
      }
    }
  })
}

function handleResetPassword(row: SysUser) {
  dialog.warning({
    title: '提示',
    content: `确定要重置用户"${row.username}"的密码吗？重置后密码为123456`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await userApi.resetPassword(row.id!)
        message.success('密码重置成功')
      } catch (error) {
        // 错误已在拦截器处理
      }
    }
  })
}

function handleToggleQuit(row: SysUser) {
  const nextQuit = row.isQuit === 1 ? 0 : 1
  dialog.warning({
    title: '提示',
    content: nextQuit === 1 ? `确定将用户"${row.username}"设置为离职吗？` : `确定将用户"${row.username}"取消离职吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await userApi.toggleQuit(row.id!)
        message.success(nextQuit === 1 ? '已设置离职' : '已取消离职')
        loadData()
      } catch (error) {
        // 错误已在拦截器处理
      }
    }
  })
}

onMounted(() => {
  const deptId = route.query.deptId
  if (deptId) {
    selectedDeptId.value = Number(deptId)
  }
  loadDeptOptions()
  loadData()
  loadRoles()
  loadPostOptions()
})
</script>

<style scoped>
.user-list-card {
  height: calc(100vh - 160px);
}

/* 自建分页布局：共N条 | 上一页 1 2 3 下一页 | 跳至N页 | 每页N条 */
.pagination-container {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 14px;
  margin-top: 12px;
  font-size: 14px;
}
.pagination-container .pg-info,
.pagination-container .pg-jumper {
  display: inline-flex;
  align-items: center;
}
.pagination-container .pg-sep {
  color: #d1d5db;
  user-select: none;
}
.pagination-container .pg-step {
  padding: 0 6px;
  font-size: 13px;
}
</style>
