import { request } from '@/utils/request'

/**
 * 日历事件（与后端 sys_calendar_event 对应）
 * 事件类型: visit / sign / payment / meeting / reminder / custom
 */
export interface CalendarEvent {
  id?: number
  title: string
  description?: string
  eventType: string
  startTime: string
  endTime?: string
  allDay?: boolean
  color?: string
  userId?: number
  customerId?: number
  projectId?: number
  location?: string
  reminderMinutes?: number
  reminderSent?: boolean
  recurrence?: string
  status?: number
  createdAt?: string
  updatedAt?: string
}

/** 创建/更新事件入参 */
export interface CalendarEventForm {
  id?: number
  title: string
  description?: string
  eventType: string
  startTime: string
  endTime?: string
  allDay?: boolean
  color?: string
  location?: string
  reminderMinutes?: number
}

/**
 * 日历/会议 API
 * 会议数据使用 eventType=meeting 的日历事件存储
 */
export const calendarApi = {
  /** 按日期范围查询事件 */
  getEvents(params: { start: string; end: string }): Promise<CalendarEvent[]> {
    return request({ url: '/calendar/events', method: 'get', params })
  },

  /** 按月查询事件 */
  getMonthEvents(year: number, month: number): Promise<CalendarEvent[]> {
    return request({ url: '/calendar/month', method: 'get', params: { year, month } })
  },

  /** 今日事件（工作台今日会议等） */
  getTodayEvents(): Promise<CalendarEvent[]> {
    return request({ url: '/calendar/today', method: 'get' })
  },

  /** 创建事件（预约会议时传 eventType: 'meeting'） */
  createEvent(data: CalendarEventForm): Promise<CalendarEvent> {
    return request({ url: '/calendar/event', method: 'post', data })
  },

  /** 更新事件 */
  updateEvent(data: CalendarEventForm & { id: number }): Promise<void> {
    return request({ url: '/calendar/event', method: 'put', data })
  },

  /** 取消/删除事件 */
  cancelEvent(id: number): Promise<void> {
    return request({ url: `/calendar/event/${id}`, method: 'delete' })
  },

  /** 合并视图（手动事件 + 客户拜访 + 合同节点） */
  getMergedEvents(params: { start: string; end: string }): Promise<CalendarEvent[]> {
    return request({ url: '/calendar/merged', method: 'get', params })
  },

  /** 团队日程 */
  getTeamEvents(params: { start: string; end: string; deptId?: number }): Promise<CalendarEvent[]> {
    return request({ url: '/calendar/team', method: 'get', params })
  }
}
