<template>
  <div class="page-container">
    <div class="contacts-layout">
      <!-- 左侧部门树 -->
      <div class="dept-sidebar">
        <div class="sidebar-header">
          <span class="title">组织架构</span>
        </div>
        <div class="dept-search">
          <n-input v-model:value="deptSearch" placeholder="搜索部门" clearable size="small">
            <template #prefix>
              <n-icon><SearchOutline /></n-icon>
            </template>
          </n-input>
        </div>
        <div class="dept-tree-wrapper">
          <n-tree
            block-line
            :data="deptData"
            :pattern="deptSearch"
            :default-expand-all="true"
            :selected-keys="selectedDeptKeys"
            key-field="id"
            label-field="deptName"
            children-field="children"
            selectable
            @update:selected-keys="handleDeptSelect"
          />
        </div>
      </div>

      <!-- 右侧用户列表 -->
      <div class="user-main">
        <div class="main-header">
          <div class="breadcrumb">
            <span class="current-dept">{{ selectedDeptName || '全公司' }}</span>
            <span class="user-count">({{ pagination.itemCount }}人)</span>
          </div>
          <div class="actions">
            <n-input v-model:value="userSearch" placeholder="搜索成员" clearable size="small" @keyup.enter="handleSearch">
              <template #prefix>
                <n-icon><SearchOutline /></n-icon>
              </template>
            </n-input>
          </div>
        </div>

        <div class="user-content">
          <n-spin :show="loading">
            <div v-if="userList.length > 0" class="user-grid">
              <div v-for="user in userList" :key="user.id" class="user-card">
                <div class="user-card-header">
                  <n-avatar round :size="48" :src="user.avatar || undefined" :color="'#18a058'">
                    {{ user.nickname?.charAt(0) || 'U' }}
                  </n-avatar>
                  <div class="user-card-info">
                    <div class="user-name">{{ user.nickname }}</div>
                    <div class="user-post">{{ user.postNames || '普通成员' }}</div>
                  </div>
                </div>
                <div class="user-card-body">
                  <div class="info-item">
                    <n-icon><CallOutline /></n-icon>
                    <span>{{ user.phone || '暂无电话' }}</span>
                  </div>
                  <div class="info-item">
                    <n-icon><MailOutline /></n-icon>
                    <span>{{ user.email || '暂无邮箱' }}</span>
                  </div>
                </div>
                <div class="user-card-footer">
                  <n-button block type="primary" secondary @click="handleMessage(user)">
                    <template #icon>
                      <n-icon><ChatbubbleOutline /></n-icon>
                    </template>
                    发消息
                  </n-button>
                </div>
              </div>
            </div>
            <n-empty v-else description="暂无成员" class="empty-state" />
          </n-spin>
          
          <div class="pagination-wrapper" v-if="pagination.itemCount > 0">
            <n-pagination
              v-model:page="pagination.page"
              v-model:page-size="pagination.pageSize"
              :item-count="pagination.itemCount"
              :page-sizes="[12, 24, 60]"
              show-size-picker
              @update:page="handlePageChange"
              @update:page-size="handlePageChange"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { SearchOutline, CallOutline, MailOutline, ChatbubbleOutline } from '@vicons/ionicons5'
import { deptApi, type SysDept } from '@/api/org'
import { userApi, type SysUser } from '@/api/system'

const router = useRouter()

// 部门相关
const deptData = ref<SysDept[]>([])
const deptSearch = ref('')
const selectedDeptKeys = ref<number[]>([])
const selectedDeptName = ref('')

// 用户相关
const loading = ref(false)
const userList = ref<SysUser[]>([])
const userSearch = ref('')
const pagination = reactive({
  page: 1,
  pageSize: 24,
  itemCount: 0
})

// 加载部门树
async function loadDeptTree() {
  try {
    const res = await deptApi.tree()
    deptData.value = [{ id: 0, deptName: '全公司', children: res } as any]
    // 默认选中第一个
    if (res.length > 0) {
      // selectedDeptKeys.value = [res[0].id!]
      // selectedDeptName.value = res[0].deptName
      // handleDeptSelect([res[0].id!])
      
      // 默认选中全公司
      selectedDeptKeys.value = [0]
      selectedDeptName.value = '全公司'
      loadUsers()
    }
  } catch (error) {
    console.error(error)
  }
}

// 选择部门
function handleDeptSelect(keys: number[], option?: any[]) {
  if (keys.length > 0) {
    selectedDeptKeys.value = keys
    // 从树数据中查找名称（简单处理，实际可能需要递归查找）
    // 这里暂时简化，因为option参数在n-tree中不一定直接可用，或者需要配置
    // 我们直接重置页码并加载
    if (keys[0] === 0) {
      selectedDeptName.value = '全公司'
    } else {
      const findName = (nodes: any[]): string => {
        for (const node of nodes) {
          if (node.id === keys[0]) return node.deptName || node.label || ''
          if (node.children?.length) {
            const found = findName(node.children)
            if (found) return found
          }
        }
        return ''
      }
      selectedDeptName.value = findName(deptData.value) || '部门'
    }
    pagination.page = 1
    loadUsers()
  }
}

// 加载用户
async function loadUsers() {
  loading.value = true
  try {
    const deptId = selectedDeptKeys.value[0] === 0 ? undefined : selectedDeptKeys.value[0]
    const res = await userApi.page({
      page: pagination.page,
      pageSize: pagination.pageSize,
      deptId: deptId,
      username: userSearch.value || undefined
    })
    userList.value = res.list
    pagination.itemCount = Number(res.total)
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 搜索
function handleSearch() {
  pagination.page = 1
  loadUsers()
}

// 分页
function handlePageChange() {
  loadUsers()
}

// 发消息
function handleMessage(user: SysUser) {
  router.push({
    path: '/message/chat',
    query: { userId: user.id }
  })
}

onMounted(() => {
  loadDeptTree()
})
</script>

<style scoped>
.page-container {
  height: calc(100vh - 92px);
  background: #f5f7fa;
}

.contacts-layout {
  display: flex;
  height: 100%;
  gap: 16px;
  padding: 16px;
}

.dept-sidebar {
  width: 280px;
  background: #fff;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  padding: 16px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
}

.sidebar-header {
  margin-bottom: 16px;
}

.title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.dept-search {
  margin-bottom: 12px;
}

.dept-tree-wrapper {
  flex: 1;
  overflow-y: auto;
}

.user-main {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.main-header {
  padding: 16px 24px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.current-dept {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-right: 8px;
}

.user-count {
  color: #999;
  font-size: 14px;
}

.user-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.user-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  flex: 1;
}

.user-card {
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 20px;
  transition: all 0.3s;
  background: #fff;
}

.user-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border-color: transparent;
  transform: translateY(-2px);
}

.user-card-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.user-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.user-post {
  font-size: 12px;
  color: #666;
  background: #f5f7fa;
  padding: 2px 8px;
  border-radius: 4px;
  display: inline-block;
}

.user-card-body {
  margin-bottom: 20px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #666;
  font-size: 13px;
  margin-bottom: 8px;
}

.user-card-footer {
  margin-top: auto;
}

.empty-state {
  margin-top: 100px;
}

.pagination-wrapper {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}
</style>