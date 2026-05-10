import request from '@/utils/request'

export interface HolidayCalendarItem {
  id?: number
  holidayDate: string  // 'YYYY-MM-DD'
  /** 1 法定节假日（休）/ 2 调休补班（上） */
  type: number
  label: string
  note?: string | null
  createTime?: string
  updateTime?: string
}

export const holidayApi = {
  list(year: number): Promise<HolidayCalendarItem[]> {
    return request({ url: '/admin/holiday-calendar', method: 'get', params: { year } })
  },
  save(item: HolidayCalendarItem): Promise<number> {
    return request({ url: '/admin/holiday-calendar', method: 'post', data: item })
  },
  update(id: number, item: HolidayCalendarItem): Promise<void> {
    return request({ url: `/admin/holiday-calendar/${id}`, method: 'put', data: item })
  },
  remove(id: number): Promise<void> {
    return request({ url: `/admin/holiday-calendar/${id}`, method: 'delete' })
  }
}
