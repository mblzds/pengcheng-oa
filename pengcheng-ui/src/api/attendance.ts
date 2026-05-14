import { request } from '@/utils/request'

/** 考勤记录（与后端 AttendanceRecord 对应） */
export interface AttendanceRecordItem {
  id?: number
  userId?: number
  userName?: string
  employeeNo?: string
  deptName?: string
  attendanceDate?: string
  clockInTime?: string
  clockInLocation?: string
  clockInPhoto?: string
  clockInStatus?: number
  clockOutTime?: string
  clockOutLocation?: string
  clockOutPhoto?: string
  clockOutStatus?: number
  /** 考勤豁免原因：请假/调休审批通过联动写入。如 "leave-病假" / "compensate" */
  exemptReason?: string
}

/** 月度汇总（与后端 AttendanceMonthlyVO 对应） */
export interface AttendanceMonthlyVO {
  userId: number
  /** 仅 batch 接口填充，单人 monthly 详情可为空 */
  nickname?: string
  /** 仅 batch 接口填充 */
  deptName?: string
  year: number
  month: number
  attendanceDays: number
  lateTimes: number
  earlyLeaveTimes: number
  leaveDays: number
  compensateDays?: number
  expectedWorkdays?: number
  absentDays?: number
  overtimeHours?: number
}

/** 请假/调休（与后端实体字段对应） */
export interface LeaveRequestItem {
  id?: number
  userId?: number
  userName?: string
  employeeNo?: string
  deptName?: string
  leaveType?: number
  startTime?: string
  endTime?: string
  reason?: string
  status?: number
  createTime?: string
  currentNodeName?: string
}

export interface CompensateRequestItem {
  id?: number
  userId?: number
  userName?: string
  employeeNo?: string
  deptName?: string
  compensateDate?: string
  reason?: string
  status?: number
  createTime?: string
  currentNodeName?: string
}

/** 考勤/请假/调休（公司级假勤，接口 /admin/attendance） */
export const attendanceApi = {
  records(params: { userId?: number; startDate?: string; endDate?: string }) {
    return request<AttendanceRecordItem[]>({ url: '/admin/attendance/records', method: 'get', params })
  },
  monthly(params: { userId: number; year: number; month: number }) {
    return request<AttendanceMonthlyVO>({ url: '/admin/attendance/monthly', method: 'get', params })
  },
  /** 批量月度汇总：后端按当前登录角色自动决定范围；deptIds / userIds 可进一步聚焦 */
  monthlyBatch(params: { year: number; month: number; deptIds?: number[]; userIds?: number[] }) {
    return request<AttendanceMonthlyVO[]>({
      url: '/admin/attendance/monthly/batch',
      method: 'get',
      params,
      paramsSerializer: { indexes: null }  // axios v1: deptIds=1&deptIds=2 而不是 deptIds[]=1
    } as any)
  },
  leaveList(params: { userId?: number; status?: number }) {
    return request<LeaveRequestItem[]>({ url: '/admin/attendance/leave/list', method: 'get', params })
  },
  compensateList(params: { userId?: number; status?: number }) {
    return request<CompensateRequestItem[]>({ url: '/admin/attendance/compensate/list', method: 'get', params })
  },
}
