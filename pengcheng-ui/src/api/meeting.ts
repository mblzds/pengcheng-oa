import { request } from '@/utils/request'

export interface MeetingParticipant {
  id: number
  name: string
  avatar?: string
}

export interface MeetingMinutes {
  id?: number
  content?: string
  conclusions?: string
  actionItems?: string
  status?: number
  creatorId?: number
  createTime?: string
  updateTime?: string
}

export interface MeetingFile {
  id: number
  fileId?: number
  name: string
  type?: string
  size?: number
  url?: string
  createTime?: string
}

export interface MeetingCalendarItem {
  id: number
  title: string
  description?: string
  type: number
  startTime: string
  endTime: string
  location?: string
  meetingUrl?: string
  reminderMinutes?: number
  status: number
  organizerId?: number
  organizerName?: string
  participantIds: number[]
  participants: MeetingParticipant[]
  minutes?: MeetingMinutes
  files: MeetingFile[]
}

export interface MeetingSavePayload {
  title: string
  description?: string
  type: number
  startTime: string
  endTime: string
  location?: string
  meetingUrl?: string
  reminderMinutes?: number
  participantIds: number[]
}

export interface MeetingReminderConfig {
  defaultReminder: number
  internalNotification: boolean
  email: boolean
}

export const meetingApi = {
  getMonthMeetings(year: number, month: number): Promise<MeetingCalendarItem[]> {
    return request({ url: '/meeting/calendar/month', method: 'get', params: { year, month } } as any)
  },

  getDayMeetings(date: string): Promise<MeetingCalendarItem[]> {
    return request({ url: '/meeting/calendar/day', method: 'get', params: { date } } as any)
  },

  getMeetingDetail(id: number): Promise<MeetingCalendarItem> {
    return request({ url: `/meeting/calendar/${id}`, method: 'get' } as any)
  },

  createMeeting(data: MeetingSavePayload): Promise<MeetingCalendarItem> {
    return request({ url: '/meeting/calendar', method: 'post', data } as any)
  },

  updateMeeting(id: number, data: MeetingSavePayload): Promise<MeetingCalendarItem> {
    return request({ url: `/meeting/calendar/${id}`, method: 'put', data } as any)
  },

  cancelMeeting(id: number): Promise<void> {
    return request({ url: `/meeting/calendar/${id}`, method: 'delete' } as any)
  },

  saveMinutes(id: number, data: MeetingMinutes): Promise<MeetingMinutes> {
    return request({ url: `/meeting/calendar/${id}/minutes`, method: 'put', data } as any)
  },

  getReminderConfig(): Promise<MeetingReminderConfig> {
    return request({ url: '/meeting/calendar/config/reminder', method: 'get' } as any)
  },

  saveReminderConfig(data: MeetingReminderConfig): Promise<void> {
    return request({ url: '/meeting/calendar/config/reminder', method: 'post', data } as any)
  }
}
