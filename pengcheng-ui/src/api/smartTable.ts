import { request } from '@/utils/request'

/** 智能表格 API */
export const smartTableApi = {
  // ==================== 表格 ====================
  listTables(deptId?: number) {
    return request({ url: '/sys/smart-table/list', method: 'get', params: { deptId } })
  },
  getTable(id: number) {
    return request({ url: `/sys/smart-table/${id}`, method: 'get' })
  },
  createTable(data: { name: string; description?: string; icon?: string; visibility?: string; deptId?: number }) {
    return request({ url: '/sys/smart-table', method: 'post', data })
  },
  updateTable(data: { id: number; name?: string; description?: string; icon?: string; visibility?: string }) {
    return request({ url: '/sys/smart-table', method: 'put', data })
  },
  deleteTable(id: number) {
    return request({ url: `/sys/smart-table/${id}`, method: 'delete' })
  },
  createFromTemplate(templateId: number, name: string, deptId?: number) {
    return request({
      url: '/sys/smart-table/from-template',
      method: 'post',
      params: { templateId, name, deptId }
    })
  },

  // ==================== 字段 ====================
  listFields(tableId: number) {
    return request({ url: `/sys/smart-table/${tableId}/fields`, method: 'get' })
  },
  addField(tableId: number, data: any) {
    return request({ url: `/sys/smart-table/${tableId}/fields`, method: 'post', data })
  },
  updateField(tableId: number, data: any) {
    return request({ url: `/sys/smart-table/${tableId}/fields`, method: 'put', data })
  },
  deleteField(tableId: number, fieldId: number) {
    return request({ url: `/sys/smart-table/${tableId}/fields/${fieldId}`, method: 'delete' })
  },
  reorderFields(tableId: number, fieldIds: number[]) {
    return request({ url: `/sys/smart-table/${tableId}/fields/reorder`, method: 'put', data: fieldIds })
  },

  // ==================== 记录 ====================
  listRecords(tableId: number, page = 1, pageSize = 50) {
    return request({ url: `/sys/smart-table/${tableId}/records`, method: 'get', params: { page, pageSize } })
  },
  addRecord(tableId: number, data: Record<string, any>) {
    return request({ url: `/sys/smart-table/${tableId}/records`, method: 'post', data })
  },
  updateRecord(recordId: number, data: Record<string, any>) {
    return request({ url: `/sys/smart-table/records/${recordId}`, method: 'put', data })
  },
  deleteRecord(recordId: number) {
    return request({ url: `/sys/smart-table/records/${recordId}`, method: 'delete' })
  },
  batchDeleteRecords(recordIds: number[]) {
    return request({ url: '/sys/smart-table/records/batch', method: 'delete', data: recordIds })
  },

  // ==================== 视图 ====================
  listViews(tableId: number) {
    return request({ url: `/sys/smart-table/${tableId}/views`, method: 'get' })
  },
  createView(tableId: number, data: { name: string; viewType: string; config?: any }) {
    return request({ url: `/sys/smart-table/${tableId}/views`, method: 'post', data })
  },
  updateView(viewId: number, data: { name?: string; config?: any }) {
    return request({ url: `/sys/smart-table/views/${viewId}`, method: 'put', data })
  },
  deleteView(viewId: number) {
    return request({ url: `/sys/smart-table/views/${viewId}`, method: 'delete' })
  },

  // ==================== 模板 ====================
  listTemplates(category?: string) {
    return request({ url: '/sys/smart-table/templates', method: 'get', params: { category } })
  },
  getTemplate(id: number) {
    return request({ url: `/sys/smart-table/templates/${id}`, method: 'get' })
  },
  createTemplate(data: any) {
    return request({ url: '/sys/smart-table/templates', method: 'post', data })
  },
  updateTemplate(data: any) {
    return request({ url: '/sys/smart-table/templates', method: 'put', data })
  },
  deleteTemplate(id: number) {
    return request({ url: `/sys/smart-table/templates/${id}`, method: 'delete' })
  }
}
