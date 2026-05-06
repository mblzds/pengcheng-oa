<template>
  <div class="workspace-container">
    <!-- 顶部欢迎区 -->
    <div class="welcome-header">
      <div class="welcome-left">
        <n-avatar round :size="64" :src="userStore.avatar || undefined">
          {{ userStore.nickname?.charAt(0) || 'U' }}
        </n-avatar>
        <div class="welcome-text">
          <h2 class="greeting">{{ getGreeting() }}，{{ userStore.nickname }} 👋</h2>
          <p class="subtitle">
            今天有 <span class="highlight">{{ todoCount }}</span> 项待办任务，
            <span class="highlight">{{ meetingCount }}</span> 个会议待参加。
          </p>
        </div>
      </div>
      <div class="welcome-right">
        <div class="weather-widget">
          <span class="time">{{ currentTime }}</span>
          <span class="date">{{ currentDate }}</span>
        </div>
      </div>
    </div>

    <div class="workspace-content">
      <!-- 左侧主区域 -->
      <div class="main-area">
        <!-- 快捷应用（按角色动态渲染） -->
        <div class="section-card quick-apps">
          <div class="section-header">
            <span class="title">常用应用</span>
            <n-tag size="tiny" type="info" style="margin-left: 8px">{{ roleLabel }}</n-tag>
            <n-button text size="small" type="primary" style="margin-left: auto">编辑</n-button>
          </div>
          <div class="app-grid">
            <div
              v-for="app in roleApps"
              :key="app.route"
              class="app-item"
              @click="router.push(app.route)"
            >
              <div class="app-icon" :style="app.style">
                <n-icon><component :is="app.icon" /></n-icon>
                <span v-if="app.badge && app.badge > 0" class="app-badge">{{ app.badge > 99 ? '99+' : app.badge }}</span>
              </div>
              <span class="app-name">{{ app.name }}</span>
            </div>
            <div class="app-item">
              <div class="app-icon add-app">
                <n-icon><AddOutline /></n-icon>
              </div>
              <span class="app-name">添加</span>
            </div>
          </div>
        </div>

        <!-- 业务数据概览 -->
        <div class="section-card data-overview">
          <div class="section-header">
            <span class="title">数据概览</span>
            <n-button text size="small" type="primary" @click="router.push('/realty/dashboard')">详细报表</n-button>
          </div>
          <div class="stat-grid">
            <div class="stat-card" style="border-left: 3px solid #18a058">
              <div class="stat-value">{{ bizStats.todaySignCount }}</div>
              <div class="stat-label">今日签约</div>
              <div class="stat-trend" :class="bizStats.signTrend >= 0 ? 'up' : 'down'">
                {{ bizStats.signTrend >= 0 ? '↑' : '↓' }} {{ Math.abs(bizStats.signTrend) }}%
              </div>
            </div>
            <div class="stat-card" style="border-left: 3px solid #2196f3">
              <div class="stat-value">¥{{ formatAmount(bizStats.monthRevenue) }}</div>
              <div class="stat-label">本月业绩</div>
              <div class="stat-trend" :class="bizStats.revenueTrend >= 0 ? 'up' : 'down'">
                {{ bizStats.revenueTrend >= 0 ? '↑' : '↓' }} {{ Math.abs(bizStats.revenueTrend) }}%
              </div>
            </div>
            <div class="stat-card" style="border-left: 3px solid #ff9800">
              <div class="stat-value">{{ bizStats.pendingFollowUp }}</div>
              <div class="stat-label">待跟进客户</div>
            </div>
            <div class="stat-card" style="border-left: 3px solid #d03050">
              <div class="stat-value">{{ bizStats.pendingApproval }}</div>
              <div class="stat-label">待审批</div>
            </div>
          </div>
        </div>

        <!-- AI 洞察 -->
        <div v-if="aiInsights.length > 0" class="section-card ai-insights">
          <div class="section-header">
            <span class="title">🤖 AI 洞察</span>
          </div>
          <div class="insight-list">
            <div v-for="(insight, idx) in aiInsights" :key="idx" class="insight-item" :class="insight.level">
              <div class="insight-icon">{{ insight.icon }}</div>
              <div class="insight-content">{{ insight.content }}</div>
            </div>
          </div>
        </div>

        <!-- 我的待办 -->
        <div class="section-card todo-list">
          <div class="section-header">
            <span class="title">我的待办</span>
            <n-button text size="small" type="primary" @click="router.push('/smart-table')">更多</n-button>
          </div>
          <n-list hoverable clickable>
            <n-list-item v-for="task in todos" :key="task.id">
              <template #prefix>
                <n-checkbox :checked="task.status === 'done'" @update:checked="toggleTask(task)" />
              </template>
              <div class="task-content" :class="{ done: task.status === 'done' }">
                <div class="task-title">{{ task.title }}</div>
                <div class="task-meta">
                  <n-tag :type="getPriorityType(task.priority)" size="tiny" round>{{ getPriorityText(task.priority) }}</n-tag>
                  <span class="task-date" :class="{ overdue: isOverdue(task.dueDate) }">
                    截止: {{ formatDate(task.dueDate) }}
                  </span>
                </div>
              </div>
              <template #suffix>
                <n-button size="tiny" quaternary circle>
                  <template #icon><n-icon><EllipsisHorizontalOutline /></n-icon></template>
                </n-button>
              </template>
            </n-list-item>
            <n-empty v-if="todos.length === 0" description="暂无待办任务" />
          </n-list>
        </div>

        <!-- 我的任务（项目管理） -->
        <div v-if="myTasks.length > 0" class="section-card my-tasks">
          <div class="section-header">
            <span class="title">我的任务</span>
            <n-button text size="small" type="primary" @click="router.push('/project')">更多</n-button>
          </div>
          <n-list hoverable clickable>
            <n-list-item v-for="task in myTasks" :key="task.id" @click="router.push('/project/' + task.projectId)">
              <div class="task-content">
                <div class="task-title">{{ task.title }}</div>
                <div class="task-meta">
                  <n-tag :type="task.status === 'done' ? 'success' : task.status === 'in_progress' ? 'info' : 'default'" size="tiny" round>{{ task.status === 'done' ? '已完成' : task.status === 'in_progress' ? '进行中' : '待处理' }}</n-tag>
                  <span v-if="task.projectName" style="color: #999; font-size: 12px; margin-left: 6px">{{ task.projectName }}</span>
                  <span v-if="task.dueDate" class="task-date" :class="{ overdue: new Date(task.dueDate) < new Date() }">
                    截止: {{ new Date(task.dueDate).toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' }) }}
                  </span>
                </div>
              </div>
            </n-list-item>
          </n-list>
        </div>

        <!-- 最近文档 -->
        <div class="section-card recent-files">
          <div class="section-header">
            <span class="title">最近文档</span>
            <n-button text size="small" type="primary" @click="router.push('/system/file')">更多</n-button>
          </div>
          <n-data-table
            :columns="fileColumns"
            :data="recentFiles"
            :pagination="false"
            size="small"
            :bordered="false"
          />
        </div>
      </div>

      <!-- 右侧侧边栏 -->
      <div class="sidebar-area">
        <!-- 今日日程 -->
        <div class="section-card schedule-card">
          <div class="section-header">
            <span class="title">今日日程</span>
            <n-button size="tiny" circle type="primary" secondary @click="router.push('/meeting')">
              <template #icon><n-icon><AddOutline /></n-icon></template>
            </n-button>
          </div>
          <div class="timeline-wrapper">
            <div v-for="meeting in todayMeetings" :key="meeting.id" class="timeline-item">
              <div class="timeline-time">
                <span class="start">{{ formatTime(meeting.startTime) }}</span>
                <span class="end">{{ formatTime(meeting.endTime) }}</span>
              </div>
              <div class="timeline-content" :class="getMeetingStatusClass(meeting)">
                <div class="meeting-title">{{ meeting.title }}</div>
                <div class="meeting-loc">
                  <n-icon size="12"><LocationOutline /></n-icon>
                  {{ meeting.location }}
                </div>
                <div class="meeting-status">
                  <n-tag size="tiny" :type="getMeetingStatusType(meeting)">{{ getMeetingStatusText(meeting) }}</n-tag>
                </div>
              </div>
            </div>
            <n-empty v-if="todayMeetings.length === 0" description="今日无会议安排" size="small" />
          </div>
        </div>

        <!-- 公告通知 -->
        <div class="section-card notice-card">
          <div class="section-header">
            <span class="title">公告通知</span>
            <n-button text size="small" type="primary" @click="router.push('/message/notice')">更多</n-button>
          </div>
          <div class="notice-list">
            <div v-for="notice in notices" :key="notice.id" class="notice-item" @click="handleNoticeClick(notice)">
              <div class="notice-tag">
                <n-tag size="tiny" :type="notice.type === 1 ? 'info' : 'warning'">{{ notice.type === 1 ? '通知' : '公告' }}</n-tag>
              </div>
              <div class="notice-title">{{ notice.title }}</div>
              <div class="notice-time">{{ formatRelativeTime(notice.time) }}</div>
            </div>
          </div>
        </div>
        
        <!-- 快捷操作 -->
        <div class="section-card quick-actions">
          <div class="action-grid">
            <div class="action-btn" @click="showQrCode = true">
              <n-icon size="20"><ScanOutline /></n-icon>
              <span>扫一扫</span>
            </div>
            <div class="action-btn">
              <n-icon size="20"><ShareOutline /></n-icon>
              <span>分享</span>
            </div>
            <div class="action-btn">
              <n-icon size="20"><SettingsOutline /></n-icon>
              <span>设置</span>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 扫码弹窗 -->
    <n-modal v-model:show="showQrCode" preset="card" title="扫一扫" style="width: 300px">
      <div style="text-align: center; padding: 20px;">
        <n-icon size="100" color="#ccc"><QrCodeOutline /></n-icon>
        <p style="color: #666; margin-top: 10px;">请使用手机 App 扫码</p>
      </div>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { NTag, NButton, NAvatar } from 'naive-ui'
import {
  ChatbubbleOutline,
  GridOutline,
  CalendarOutline,
  FolderOpenOutline,
  BookOutline,
  TimeOutline,
  WalletOutline,
  AddOutline,
  EllipsisHorizontalOutline,
  LocationOutline,
  ScanOutline,
  ShareOutline,
  SettingsOutline,
  QrCodeOutline,
  DocumentTextOutline,
  PeopleOutline,
  BusinessOutline,
  CashOutline,
  StatsChartOutline,
  CheckmarkDoneOutline,
  SparklesOutline,
  RibbonOutline,
  ListOutline,
  ClipboardOutline
} from '@vicons/ionicons5'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'
import { calendarApi } from '@/api/calendar'

const router = useRouter()
const userStore = useUserStore()
const messageStore = useMessageStore()

const currentTime = ref('')
const currentDate = ref('')
const todoCount = ref(0)
const meetingCount = ref(0)
const showQrCode = ref(false)

const bizStats = ref({
  todaySignCount: 0,
  monthRevenue: 0,
  pendingFollowUp: 0,
  pendingApproval: 0,
  signTrend: 0,
  revenueTrend: 0
})

const aiInsights = ref<Array<{ icon: string; content: string; level: string }>>([])

const userRole = computed(() => {
  const user = userStore.user as typeof userStore.user & { roles?: unknown[]; roleIds?: unknown[] }
  const roles = user?.roles || user?.roleIds || userStore.roles || []
  if (Array.isArray(roles)) {
    const roleNames = roles.map((r: any) => typeof r === 'string' ? r : r.roleKey || r.roleName || '')
    if (roleNames.some((r: string) => ['admin', 'super_admin'].includes(r))) return 'admin'
    if (roleNames.some((r: string) => ['manager', 'director'].includes(r))) return 'manager'
    if (roleNames.some((r: string) => ['hr', 'attendance'].includes(r))) return 'hr'
  }
  return 'sales'
})

const roleLabel = computed(() => {
  const map: Record<string, string> = { admin: '管理员', manager: '管理者', hr: '人事', sales: '销售' }
  return map[userRole.value] || '销售'
})

interface AppItem {
  name: string
  route: string
  icon: any
  style: string
  badge?: number
}

const baseApps: AppItem[] = [
  { name: '消息中心', route: '/message/chat', icon: ChatbubbleOutline, style: 'background:#e8f5e9;color:#18a058', badge: messageStore.totalUnread },
  { name: '智能表格', route: '/smart-table', icon: GridOutline, style: 'background:#e3f2fd;color:#2196f3' },
  { name: '项目管理', route: '/project', icon: ClipboardOutline, style: 'background:#e8eaf6;color:#3f51b5' },
  { name: '会议日程', route: '/meeting', icon: CalendarOutline, style: 'background:#fff3e0;color:#ff9800' },
  { name: '云文档', route: '/system/file', icon: FolderOpenOutline, style: 'background:#f3e5f5;color:#9c27b0' },
  { name: '通讯录', route: '/contacts', icon: BookOutline, style: 'background:#e0f2f1;color:#673ab7' },
]

const salesApps: AppItem[] = [
  { name: '客户管理', route: '/realty/customer', icon: PeopleOutline, style: 'background:#e8f5e9;color:#18a058', badge: bizStats.value.pendingFollowUp },
  { name: '项目楼盘', route: '/realty/project', icon: BusinessOutline, style: 'background:#e3f2fd;color:#2196f3' },
  { name: '成交佣金', route: '/realty/commission', icon: CashOutline, style: 'background:#fff3e0;color:#ff9800' },
  { name: '考勤打卡', route: '/realty/attendance', icon: TimeOutline, style: 'background:#fbe9e7;color:#ff5722' },
  { name: 'AI 助手', route: '/ai/chat', icon: SparklesOutline, style: 'background:#ede7f6;color:#673ab7' },
]

const managerApps: AppItem[] = [
  { name: '数据统计', route: '/realty/stats', icon: StatsChartOutline, style: 'background:#e8f5e9;color:#18a058' },
  { name: '付款审批', route: '/realty/payment', icon: WalletOutline, style: 'background:#e8eaf6;color:#3f51b5', badge: bizStats.value.pendingApproval },
  { name: '客户管理', route: '/realty/customer', icon: PeopleOutline, style: 'background:#e3f2fd;color:#2196f3' },
  { name: '联盟商', route: '/realty/alliance', icon: BusinessOutline, style: 'background:#fff3e0;color:#ff9800' },
  { name: 'AI 洞察', route: '/ai/chat', icon: SparklesOutline, style: 'background:#ede7f6;color:#673ab7' },
]

const hrApps: AppItem[] = [
  { name: '绩效考核', route: '/hr', icon: RibbonOutline, style: 'background:#ede7f6;color:#673ab7' },
  { name: '考勤管理', route: '/realty/attendance', icon: TimeOutline, style: 'background:#fbe9e7;color:#ff5722' },
  { name: '用户管理', route: '/system/user', icon: PeopleOutline, style: 'background:#e3f2fd;color:#2196f3' },
  { name: '部门管理', route: '/org/dept', icon: BusinessOutline, style: 'background:#e8f5e9;color:#18a058' },
  { name: '岗位管理', route: '/org/post', icon: CheckmarkDoneOutline, style: 'background:#fff3e0;color:#ff9800' },
]

const adminApps: AppItem[] = [
  { name: '系统配置', route: '/system/config', icon: SettingsOutline, style: 'background:#e8eaf6;color:#3f51b5' },
  { name: 'AI 模型', route: '/ai/config', icon: SparklesOutline, style: 'background:#ede7f6;color:#673ab7' },
  { name: '用户管理', route: '/system/user', icon: PeopleOutline, style: 'background:#e3f2fd;color:#2196f3' },
  { name: '数据统计', route: '/realty/stats', icon: StatsChartOutline, style: 'background:#e8f5e9;color:#18a058' },
  { name: '服务监控', route: '/monitor/server', icon: GridOutline, style: 'background:#fbe9e7;color:#ff5722' },
]

const roleApps = computed(() => {
  const specialApps = { admin: adminApps, manager: managerApps, hr: hrApps, sales: salesApps }[userRole.value] || salesApps
  return [...baseApps, ...specialApps]
})

function formatAmount(amount: number): string {
  if (amount >= 10000) return (amount / 10000).toFixed(1) + '万'
  return amount.toLocaleString()
}

async function loadBizStats() {
  try {
    const { request } = await import('@/utils/request')
    const d: any = await request({ url: '/admin/dashboard/overview', method: 'get' })
    if (d && typeof d === 'object') {
      bizStats.value = {
        todaySignCount: d.todayDealCount || 0,
        monthRevenue: d.totalDealAmount || 0,
        pendingFollowUp: d.pendingFollowUp || 0,
        pendingApproval: d.pendingApproval || 0,
        signTrend: d.dealCountTrend || 0,
        revenueTrend: d.dealAmountTrend || 0
      }
    }
  } catch { /* 后端接口暂未适配时静默处理 */ }
}

async function loadAiInsights() {
  try {
    const { request } = await import('@/utils/request')
    const res: any = await request({ url: '/admin/dashboard/ai-insights', method: 'get' })
    if (Array.isArray(res)) {
      aiInsights.value = res
    }
  } catch { /* 后端接口尚未实现时静默处理 */ }
}

// 问候语
function getGreeting() {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 9) return '早上好'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  if (hour < 22) return '晚上好'
  return '夜深了'
}

// 时间更新
function updateTime() {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  currentDate.value = now.toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' })
}

const todos = ref<any[]>([])
const myTasks = ref<any[]>([])

async function loadTodos() {
  try {
    const { request } = await import('@/utils/request')
    const res = await request({ url: '/todo/list', method: 'get', params: { status: 0 } })
    const list = res?.data ?? res ?? []
    todos.value = list.slice(0, 5).map((t: any) => ({
      id: t.id, title: t.title, status: t.status === 2 ? 'done' : 'todo',
      priority: t.priority === 2 ? 'high' : t.priority === 1 ? 'medium' : 'low',
      dueDate: t.dueDate ? new Date(t.dueDate).getTime() : Date.now() + 86400000 * 7,
    }))
    todoCount.value = list.length
  } catch { /* 接口暂未可用 */ }
}

async function loadMyTasks() {
  try {
    const { request } = await import('@/utils/request')
    const res = await request({ url: '/project/my-tasks', method: 'get', params: { limit: 5 } })
    myTasks.value = res?.data ?? res ?? []
  } catch { /* 接口暂未可用 */ }
}

/** 今日会议：从日历接口拉取，仅展示 eventType=meeting 且未取消的 */
async function loadTodayMeetings() {
  try {
    const list = await calendarApi.getTodayEvents()
    const raw = Array.isArray(list) ? list : []
    const meetings = raw
      .filter((e: any) => e.eventType === 'meeting' && e.status !== 0)
      .map((e: any) => {
        const start = e.startTime ? new Date(e.startTime).getTime() : 0
        const end = e.endTime ? new Date(e.endTime).getTime() : start
        const now = Date.now()
        let status = 0
        if (now < start) status = 0
        else if (end && now > end) status = 2
        else status = 1
        return {
          id: e.id,
          title: e.title || '未命名会议',
          startTime: start,
          endTime: end,
          location: e.location || '线上会议',
          status
        }
      })
    todayMeetings.value = meetings
    meetingCount.value = meetings.length
  } catch {
    todayMeetings.value = []
    meetingCount.value = 0
  }
}

// 最近文件（由 loadRecentFiles 从 /sys/file/page 拉取）
const recentFiles = ref<Array<{ id: number; name: string; size: string; time: string; type: string }>>([])

function formatFileSize(bytes: number): string {
  if (bytes >= 1024 * 1024) return (bytes / 1024 / 1024).toFixed(1) + ' MB'
  if (bytes >= 1024) return (bytes / 1024).toFixed(0) + ' KB'
  return bytes + ' B'
}

async function loadRecentFiles() {
  try {
    const { fileApi } = await import('@/api/system')
    const res = await fileApi.page({ page: 1, pageSize: 5 })
    const list = res?.list ?? []
    recentFiles.value = list.map((f: any) => ({
      id: f.id,
      name: f.originalName || f.fileName || '',
      size: formatFileSize(f.fileSize || 0),
      time: f.createTime ? formatRelativeTime(new Date(f.createTime).getTime()) : '-',
      type: (f.fileSuffix || f.fileType || 'file').replace('.', '')
    }))
  } catch {
    recentFiles.value = []
  }
}

const fileColumns = [
  {
    title: '文件名',
    key: 'name',
    render(row: any) {
      return h('div', { style: 'display: flex; align-items: center; gap: 8px;' }, [
        h(NButton, { size: 'tiny', quaternary: true, circle: true, type: 'primary' }, { icon: () => h(DocumentTextOutline) }),
        h('span', row.name)
      ])
    }
  },
  { title: '大小', key: 'size', width: 80 },
  { title: '修改时间', key: 'time', width: 100 }
]

// 今日会议（由 loadTodayMeetings 从 /calendar/today 接口拉取）
const todayMeetings = ref<Array<{ id: number; title: string; startTime: number; endTime: number; location: string; status: number }>>([])

// 公告（由 loadNotices 从接口拉取）
const notices = ref<Array<{ id: number; title: string; type: number; time: number }>>([])

async function loadNotices() {
  try {
    const { noticeApi } = await import('@/api/message')
    const res = await noticeApi.myNotices({ page: 1, pageSize: 5 })
    const list = res?.list ?? []
    notices.value = list.map((n: any) => ({
      id: n.id,
      title: n.title,
      type: n.noticeType ?? 1,
      time: n.createTime ? new Date(n.createTime).getTime() : Date.now()
    }))
  } catch {
    notices.value = []
  }
}

// 辅助函数
function toggleTask(task: any) {
  task.status = task.status === 'done' ? 'todo' : 'done'
}

function getPriorityType(p: string) {
  return { high: 'error', medium: 'warning', low: 'info' }[p] as any
}

function getPriorityText(p: string) {
  return { high: '高', medium: '中', low: '低' }[p]
}

function formatDate(ts: number) {
  return new Date(ts).toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' })
}

function formatTime(ts: number) {
  return new Date(ts).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function isOverdue(ts: number) {
  return ts < Date.now()
}

function getMeetingStatusClass(m: any) {
  if (m.status === 2) return 'meeting-ended'
  if (m.status === 1) return 'meeting-active'
  return ''
}

function getMeetingStatusType(m: any) {
  if (m.status === 2) return 'default'
  if (m.status === 1) return 'success'
  return 'info'
}

function getMeetingStatusText(m: any) {
  if (m.status === 2) return '已结束'
  if (m.status === 1) return '进行中'
  return '未开始'
}

function formatRelativeTime(ts: number) {
  const diff = Date.now() - ts
  if (diff < 3600000) return Math.ceil(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.ceil(diff / 3600000) + '小时前'
  return Math.ceil(diff / 86400000) + '天前'
}

function handleNoticeClick(notice: any) {
  router.push('/message/notice')
}

let timer: number
onMounted(() => {
  updateTime()
  timer = window.setInterval(updateTime, 1000)
  loadBizStats()
  loadAiInsights()
  loadTodos()
  loadMyTasks()
  loadTodayMeetings()
  loadNotices()
  loadRecentFiles()
})

onUnmounted(() => {
  clearInterval(timer)
})
</script>

<style scoped>
.workspace-container {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100%;
}

.welcome-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  background: #fff;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.welcome-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.welcome-text .greeting {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
}

.welcome-text .subtitle {
  color: #666;
  font-size: 14px;
}

.highlight {
  color: #18a058;
  font-weight: 600;
  font-size: 16px;
}

.weather-widget {
  text-align: right;
}

.weather-widget .time {
  display: block;
  font-size: 32px;
  font-weight: 700;
  color: #333;
  line-height: 1.2;
}

.weather-widget .date {
  color: #999;
  font-size: 14px;
}

.workspace-content {
  display: flex;
  gap: 20px;
}

.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.sidebar-area {
  width: 320px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-header .title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  border-left: 4px solid #18a058;
  padding-left: 12px;
}

/* 快捷应用 */
.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
  gap: 16px;
}

.app-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  transition: transform 0.2s;
}

.app-item:hover {
  transform: translateY(-2px);
}

.app-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-bottom: 8px;
  position: relative;
}
.app-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 16px;
  height: 16px;
  line-height: 16px;
  text-align: center;
  font-size: 10px;
  color: #fff;
  background: #d03050;
  border-radius: 8px;
  padding: 0 4px;
}

.app-icon.add-app {
  background: #f5f5f5;
  color: #999;
  border: 1px dashed #ccc;
}

.app-name {
  font-size: 12px;
  color: #666;
}

/* 待办列表 */
.task-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.task-content.done .task-title {
  text-decoration: line-through;
  color: #999;
}

.task-title {
  font-size: 14px;
  color: #333;
}

.task-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.task-date {
  font-size: 12px;
  color: #999;
}

.task-date.overdue {
  color: #d03050;
}

/* 日程 */
.timeline-wrapper {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.timeline-item {
  display: flex;
  gap: 12px;
}

.timeline-time {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  min-width: 45px;
  color: #333;
  font-weight: 500;
  font-size: 13px;
}

.timeline-time .end {
  color: #999;
  font-size: 12px;
  font-weight: normal;
}

.timeline-content {
  flex: 1;
  background: #f9f9f9;
  border-radius: 8px;
  padding: 10px;
  border-left: 3px solid #ccc;
}

.timeline-content.meeting-active {
  background: #e8f5e9;
  border-left-color: #18a058;
}

.timeline-content.meeting-ended {
  opacity: 0.7;
}

.meeting-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}

.meeting-loc {
  font-size: 12px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 6px;
}

/* 公告 */
.notice-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notice-item {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.notice-item:hover .notice-title {
  color: #18a058;
}

.notice-title {
  flex: 1;
  font-size: 13px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notice-time {
  font-size: 12px;
  color: #999;
}

/* 快捷操作 */
.action-grid {
  display: flex;
  justify-content: space-around;
}

.action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #666;
  font-size: 12px;
}

.action-btn:hover {
  color: #18a058;
}

@media (max-width: 900px) {
  .workspace-content {
    flex-direction: column;
  }
  
  .sidebar-area {
    width: 100%;
  }
  
  .welcome-header {
    flex-direction: column;
    text-align: center;
    gap: 16px;
  }
  
  .welcome-left {
    flex-direction: column;
  }
  
  .weather-widget {
    text-align: center;
  }
}

/* 业务数据概览 */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.stat-card {
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
}
.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #333;
}
.stat-label {
  font-size: 13px;
  color: #999;
  margin-top: 4px;
}
.stat-trend {
  font-size: 12px;
  margin-top: 4px;
}
.stat-trend.up {
  color: #18a058;
}
.stat-trend.down {
  color: #d03050;
}

/* AI 洞察 */
.insight-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.insight-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 13px;
}
.insight-item.info {
  background: #e8f5e9;
  color: #2e7d32;
}
.insight-item.warn {
  background: #fff3e0;
  color: #e65100;
}
.insight-item.danger {
  background: #fbe9e7;
  color: #c62828;
}
.insight-icon {
  font-size: 16px;
  flex-shrink: 0;
}
.insight-content {
  flex: 1;
  line-height: 1.5;
}

@media (max-width: 768px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
