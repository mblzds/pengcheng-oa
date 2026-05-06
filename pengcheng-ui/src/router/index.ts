import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import type { MenuInfo } from '@/api/auth'

const modules = import.meta.glob('/src/views/**/*.vue')

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/register/index.vue'),
    meta: { title: '注册', requiresAuth: false }
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页', icon: 'HomeOutline' }
      },

      // ========== 1. 房产业务 ==========
      {
        path: 'realty/customer',
        name: 'RealtyCustomer',
        component: () => import('@/views/realty/customer/CustomerManage.vue'),
        meta: { title: '客户管理', icon: 'PeopleOutline' }
      },
      {
        path: 'realty/customer-pool',
        name: 'CustomerPool',
        component: () => import('@/views/realty/customer/CustomerPool.vue'),
        meta: { title: '客户公海池', icon: 'PeopleOutline' }
      },
      {
        path: 'realty/alliance',
        name: 'RealtyAlliance',
        component: () => import('@/views/realty/alliance/AllianceManage.vue'),
        meta: { title: '联盟商管理', icon: 'BusinessOutline' }
      },
      {
        path: 'realty/project',
        name: 'RealtyProject',
        component: () => import('@/views/realty/project/ProjectManage.vue'),
        meta: { title: '项目楼盘', icon: 'BusinessOutline' }
      },
      {
        path: 'realty/commission',
        name: 'RealtyCommission',
        component: () => import('@/views/realty/commission/CommissionManage.vue'),
        meta: { title: '成交佣金', icon: 'CashOutline' }
      },
      {
        path: 'realty/payment',
        name: 'RealtyPayment',
        component: () => import('@/views/realty/payment/PaymentManage.vue'),
        meta: { title: '付款申请', icon: 'WalletOutline' }
      },
      {
        path: 'realty/receivable',
        name: 'RealtyReceivable',
        component: () => import('@/views/realty/receivable/index.vue'),
        meta: { title: '回款管理', icon: 'CashOutline' }
      },
      {
        path: 'realty/visit',
        name: 'SalesVisit',
        component: () => import('@/views/realty/visit/index.vue'),
        meta: { title: '拜访记录', icon: 'WalkOutline' }
      },
      {
        path: 'realty/calendar',
        name: 'RealtyCalendar',
        component: () => import('@/views/realty/calendar/index.vue'),
        meta: { title: '销售日历', icon: 'CalendarOutline' }
      },
      {
        path: 'realty/report',
        name: 'RealtyReport',
        component: () => import('@/views/realty/report/index.vue'),
        meta: { title: 'AI 日报', icon: 'NewspaperOutline' }
      },
      {
        path: 'realty/quality',
        name: 'RealtyQuality',
        component: () => import('@/views/realty/quality/index.vue'),
        meta: { title: '销售质检', icon: 'ShieldCheckmarkOutline' }
      },
      {
        path: 'realty/templates',
        name: 'RealtyTemplates',
        component: () => import('@/views/realty/templates/index.vue'),
        meta: { title: '场景模板', icon: 'DocumentsOutline' }
      },
      {
        path: 'realty/analysis',
        name: 'RealtyAnalysis',
        component: () => import('@/views/realty/analysis/index.vue'),
        meta: { title: '经营分析', icon: 'BarChartOutline' }
      },
      {
        path: 'realty/stats',
        name: 'RealtyStats',
        component: () => import('@/views/realty/stats/DashboardPage.vue'),
        meta: { title: '数据统计', icon: 'StatsChartOutline' }
      },

      // ========== 2. 智能助手 ==========
      {
        path: 'ai/chat',
        name: 'AIChat',
        component: () => import('@/views/ai/chat/index.vue'),
        meta: { title: 'AI 助手', icon: 'ChatbubbleOutline' }
      },
      {
        path: 'ai/knowledge',
        name: 'AiKnowledge',
        component: () => import('@/views/ai/knowledge/AiKnowledge.vue'),
        meta: { title: '知识库管理', icon: 'LibraryOutline' }
      },
      {
        path: 'ai/experiment',
        name: 'AiExperiment',
        component: () => import('@/views/ai/experiment/AiExperiment.vue'),
        meta: { title: 'AI 实验', icon: 'PulseOutline' }
      },
      {
        path: 'ai/config',
        name: 'AIConfig',
        component: () => import('@/views/ai/config/index.vue'),
        meta: { title: '模型与技能', icon: 'SettingsOutline' }
      },
      {
        path: 'ai/memory',
        name: 'AiMemory',
        component: () => import('@/views/ai/memory/index.vue'),
        meta: { title: 'AI 记忆', icon: 'BulbOutline' }
      },
      {
        path: 'ai/skills',
        name: 'AiSkills',
        component: () => import('@/views/ai/skills/index.vue'),
        meta: { title: 'Skill 管理', icon: 'HammerOutline' }
      },
      {
        path: 'ai/mcp',
        name: 'AiMcp',
        component: () => import('@/views/ai/mcp/index.vue'),
        meta: { title: 'MCP 工具', icon: 'ExtensionPuzzleOutline' }
      },

      // ========== 3. 协作办公 ==========
      {
        path: 'message/chat',
        name: 'MessageChat',
        component: () => import('@/views/message/chat/index.vue'),
        meta: { title: '即时聊天', icon: 'ChatbubbleOutline' }
      },
      {
        path: 'contacts',
        name: 'Contacts',
        component: () => import('@/views/contacts/index.vue'),
        meta: { title: '通讯录', icon: 'BookOutline' }
      },
      {
        path: 'message/notice',
        name: 'MessageNotice',
        component: () => import('@/views/message/notice/index.vue'),
        meta: { title: '系统通知', icon: 'NotificationsOutline' }
      },
      {
        path: 'meeting/calendar',
        name: 'MeetingCalendar',
        component: () => import('@/views/meeting/MeetingCalendar.vue'),
        meta: { title: '会议日历', icon: 'CalendarOutline' }
      },
      {
        path: 'doc',
        name: 'DocSpace',
        component: () => import('@/views/doc/index.vue'),
        meta: { title: '云文档', icon: 'DocumentTextOutline' }
      },
      {
        path: 'smart-table',
        name: 'SmartTable',
        component: () => import('@/views/smarttable/index.vue'),
        meta: { title: '智能表格', icon: 'AppsOutline' }
      },
      {
        path: 'smart-table/template-mgmt',
        name: 'SmartTableTemplates',
        component: () => import('@/views/smarttable/templates.vue'),
        meta: { title: '表格模板管理', icon: 'CopyOutline' }
      },
      {
        path: 'todo',
        name: 'Todo',
        component: () => import('@/views/todo/index.vue'),
        meta: { title: '待办事项', icon: 'CheckboxOutline' }
      },
      {
        path: 'project',
        name: 'ProjectList',
        component: () => import('@/views/project/index.vue'),
        meta: { title: '项目管理', icon: 'FolderOpenOutline' }
      },
      {
        path: 'project/:id',
        name: 'ProjectDetail',
        component: () => import('@/views/project/detail.vue'),
        meta: { title: '项目详情', hideInMenu: true }
      },

      // ========== 4. 人事管理 ==========
      {
        path: 'hr',
        name: 'HrManage',
        component: () => import('@/views/hr/index.vue'),
        meta: { title: '人事档案', icon: 'PersonOutline' }
      },
      {
        path: 'realty/attendance',
        name: 'RealtyAttendance',
        component: () => import('@/views/realty/attendance/AttendanceManage.vue'),
        meta: { title: '考勤打卡', icon: 'TimeOutline' }
      },
      {
        path: 'hr/performance',
        name: 'HrPerformance',
        component: () => import('@/views/hr/performance.vue'),
        meta: { title: '绩效考核', icon: 'StatsChartOutline' }
      },
      {
        path: 'hr/review-360',
        name: 'HrReview360',
        component: () => import('@/views/hr/Review360.vue'),
        meta: { title: '360度评估', icon: 'PeopleCircleOutline' }
      },

      // ========== 5. 组织管理 ==========
      {
        path: 'org/dept',
        name: 'OrgDept',
        component: () => import('@/views/org/dept/index.vue'),
        meta: { title: '部门管理', icon: 'GitNetworkOutline' }
      },
      {
        path: 'org/post',
        name: 'OrgPost',
        component: () => import('@/views/org/post/index.vue'),
        meta: { title: '岗位管理', icon: 'IdCardOutline' }
      },

      // ========== 6. 系统管理 ==========
      {
        path: 'system/user',
        name: 'SystemUser',
        component: () => import('@/views/system/user/index.vue'),
        meta: { title: '用户管理', icon: 'PersonOutline' }
      },
      {
        path: 'system/role',
        name: 'SystemRole',
        component: () => import('@/views/system/role/index.vue'),
        meta: { title: '角色管理', icon: 'PeopleOutline' }
      },
      {
        path: 'system/menu',
        name: 'SystemMenu',
        component: () => import('@/views/system/menu/index.vue'),
        meta: { title: '菜单管理', icon: 'MenuOutline' }
      },
      {
        path: 'system/dict',
        name: 'SystemDict',
        component: () => import('@/views/system/dict/index.vue'),
        meta: { title: '字典管理', icon: 'BookOutline' }
      },
      {
        path: 'system/config',
        name: 'SystemConfig',
        component: () => import('@/views/system/config/index.vue'),
        meta: { title: '系统配置', icon: 'SettingsSharp' }
      },
      {
        path: 'system/file',
        name: 'SystemFile',
        component: () => import('@/views/system/file/index.vue'),
        meta: { title: '文件列表', icon: 'FolderOutline' }
      },
      {
        path: 'system/file-config',
        name: 'SystemFileConfig',
        component: () => import('@/views/system/file-config/index.vue'),
        meta: { title: '文件配置', icon: 'CloudOutline' }
      },
      {
        path: 'system/channel',
        name: 'SystemChannel',
        component: () => import('@/views/system/channel/index.vue'),
        meta: { title: '渠道推送', icon: 'SendOutline' }
      },
      {
        path: 'system/automation',
        name: 'SystemAutomation',
        component: () => import('@/views/system/automation/AutomationRule.vue'),
        meta: { title: '自动化规则', icon: 'FlashOutline' }
      },

      // ========== 7. 系统监控 ==========
      {
        path: 'monitor/online',
        name: 'MonitorOnline',
        component: () => import('@/views/monitor/online/index.vue'),
        meta: { title: '在线用户', icon: 'PeopleCircleOutline' }
      },
      {
        path: 'monitor/job',
        name: 'MonitorJob',
        component: () => import('@/views/monitor/job/index.vue'),
        meta: { title: '定时任务', icon: 'TimerOutline' }
      },
      {
        path: 'monitor/cache',
        name: 'MonitorCache',
        component: () => import('@/views/monitor/cache/index.vue'),
        meta: { title: '缓存监控', icon: 'ServerOutline' }
      },
      {
        path: 'monitor/server',
        name: 'MonitorServer',
        component: () => import('@/views/monitor/server/index.vue'),
        meta: { title: '服务监控', icon: 'DesktopOutline' }
      },
      {
        path: 'monitor/server-manager',
        name: 'ServerManager',
        component: () => import('@/views/monitor/server-manager/index.vue'),
        meta: { title: '服务器管理', icon: 'ServerOutline' }
      },
      {
        path: 'monitor/heartbeat',
        name: 'MonitorHeartbeat',
        component: () => import('@/views/monitor/heartbeat/index.vue'),
        meta: { title: 'AI 巡检', icon: 'PulseOutline' }
      },

      // ========== 8. 系统日志 ==========
      {
        path: 'log/operlog',
        name: 'LogOper',
        component: () => import('@/views/log/operlog/index.vue'),
        meta: { title: '操作日志', icon: 'ListOutline' }
      },
      {
        path: 'log/loginlog',
        name: 'LogLogin',
        component: () => import('@/views/log/loginlog/index.vue'),
        meta: { title: '登录日志', icon: 'LogInOutline' }
      },

      // ========== 9. 开发工具 ==========
      {
        path: 'tool/gen',
        name: 'ToolGen',
        component: () => import('@/views/tool/gen/index.vue'),
        meta: { title: '代码生成', icon: 'CodeSlashOutline' }
      },

      // ========== 辅助路由 ==========
      {
        path: 'redirect/:path(.*)',
        name: 'Redirect',
        component: () => import('@/views/redirect/index.vue'),
        meta: { title: '重定向', requiresAuth: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '404', requiresAuth: false }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

const addedRouteNames = new Set<string>()

/**
 * 根据菜单动态添加新路由（只添加静态路由中没有的）
 */
export function addDynamicRoutes(menus: MenuInfo[]) {
  const isDev = import.meta.env.DEV

  const addRoutes = (menuList: MenuInfo[]) => {
    for (const menu of menuList) {
      if (menu.type === 2 && menu.path && menu.component) {
        const routeName = 'Dynamic-' + menu.id

        const existingRoutes = router.getRoutes()
        const menuPath = menu.path.startsWith('/') ? menu.path.slice(1) : menu.path
        const pathExists = existingRoutes.some(r => r.path === '/' + menuPath || r.path === menuPath)

        if (pathExists || addedRouteNames.has(routeName)) continue

        const componentName = menu.component.startsWith('/') ? menu.component.slice(1) : menu.component
        const componentPath = `/src/views/${componentName}.vue`
        const component = modules[componentPath]

        if (component) {
          router.addRoute('Layout', {
            path: menuPath,
            name: routeName,
            component: component,
            meta: {
              title: menu.name,
              icon: menu.icon,
              permission: menu.permission
            }
          })
          addedRouteNames.add(routeName)
        } else if (isDev) {
          console.warn(`[动态路由] 组件不存在: ${componentPath}`)
        }
      }

      if (menu.children && menu.children.length > 0) {
        addRoutes(menu.children)
      }
    }
  }

  addRoutes(menus)
}

/**
 * 重置动态路由
 */
export function resetRouter() {
  addedRouteNames.forEach(name => {
    if (router.hasRoute(name)) {
      router.removeRoute(name)
    }
  })
  addedRouteNames.clear()
}

router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()

  document.title = `${to.meta.title || ''} - MasterLife`

  if (to.meta.requiresAuth === false) {
    next()
    return
  }

  if (!userStore.token) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }

  if (!userStore.user) {
    try {
      await userStore.getInfo()
      addDynamicRoutes(userStore.menus)
      next({ ...to, replace: true })
      return
    } catch (error) {
      userStore.logout()
      next({ name: 'Login' })
      return
    }
  }

  next()
})

export default router
