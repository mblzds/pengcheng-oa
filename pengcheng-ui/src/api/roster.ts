import request, { downloadBlob } from '@/utils/request'

export interface RosterRowError {
  lineNo: number
  employeeNo: string
  name: string
  reason: string
}

export interface RosterPreviewVO {
  totalRows: number
  validRows: number
  errorRows: number
  deptsToCreate: number
  usersToCreate: number
  usersToUpdate: number
  usersToDeactivate: number
  deptsToCreateList: string[]
  errors: RosterRowError[]
}

export interface RosterImportResultVO {
  deptsCreated: number
  usersCreated: number
  usersUpdated: number
  usersDeactivated: number
  errorRows: number
  errors: RosterRowError[]
  defaultPassword: string
}

export const rosterApi = {
  preview(file: File): Promise<RosterPreviewVO> {
    const fd = new FormData()
    fd.append('file', file)
    return request({ url: '/admin/roster/preview', method: 'post', data: fd, headers: { 'Content-Type': 'multipart/form-data' } })
  },
  importRoster(file: File): Promise<RosterImportResultVO> {
    const fd = new FormData()
    fd.append('file', file)
    return request({ url: '/admin/roster/import', method: 'post', data: fd, headers: { 'Content-Type': 'multipart/form-data' } })
  },
  downloadTemplate(): Promise<void> {
    return downloadBlob('/admin/roster/template', '员工花名册-模板.csv')
  }
}
