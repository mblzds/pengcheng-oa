import { request } from '@/utils/request'

/** 考勤记录（与后端 AttendanceRecord 对应） */
export interface AttendanceRecordItem {
  id?: number
  userId?: number
  attendanceDate?: string
  clockInTime?: string
  clockInLocation?: string
  clockInStatus?: number
  clockOutTime?: string
  clockOutLocation?: string
  clockOutStatus?: number
}

/** 月度汇总（与后端 AttendanceMonthlyVO 对应） */
export interface AttendanceMonthlyVO {
  userId: number
  year: number
  month: number
  attendanceDays: number
  lateTimes: number
  earlyLeaveTimes: number
  leaveDays: number
  overtimeHours?: number
}

/** 请假/调休（与后端实体字段对应） */
export interface LeaveRequestItem {
  id?: number
  userId?: number
  leaveType?: number
  startTime?: string
  endTime?: string
  reason?: string
  status?: number
  createTime?: string
}

export interface CompensateRequestItem {
  id?: number
  userId?: number
  compensateDate?: string
  reason?: string
  status?: number
  createTime?: string
}

/** 考勤/请假/调休（公司级假勤，接口 /admin/attendance） */
export const attendanceApi = {
  records(params: { userId?: number; startDate?: string; endDate?: string }) {
    return request<AttendanceRecordItem[]>({ url: '/admin/attendance/records', method: 'get', params })
  },
  monthly(params: { userId: number; year: number; month: number }) {
    return request<AttendanceMonthlyVO>({ url: '/admin/attendance/monthly', method: 'get', params })
  },
  leaveList(params: { userId?: number; status?: number }) {
    return request<LeaveRequestItem[]>({ url: '/admin/attendance/leave/list', method: 'get', params })
  },
  compensateList(params: { userId?: number; status?: number }) {
    return request<CompensateRequestItem[]>({ url: '/admin/attendance/compensate/list', method: 'get', params })
  },
}
