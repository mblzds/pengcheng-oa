import { request } from '@/utils/request'

/** 项目 */
export const projectApi = {
  list: (params: { page?: number; size?: number; scope?: string; status?: number }) =>
    request({ url: '/project/list', method: 'get', params }),
  get: (id: number) => request({ url: `/project/${id}`, method: 'get' }),
  create: (data: any) => request({ url: '/project', method: 'post', data }),
  update: (id: number, data: any) => request({ url: `/project/${id}`, method: 'put', data }),
  remove: (id: number) => request({ url: `/project/${id}`, method: 'delete' }),
  members: (id: number) => request({ url: `/project/${id}/members`, method: 'get' }),
  addMember: (id: number, userId: number, role: string) =>
    request({ url: `/project/${id}/members`, method: 'post', params: { userId, role } }),
  updateMemberRole: (id: number, userId: number, role: string) =>
    request({ url: `/project/${id}/members/${userId}`, method: 'put', params: { role } }),
  removeMember: (id: number, userId: number) =>
    request({ url: `/project/${id}/members/${userId}`, method: 'delete' }),
  stats: (id: number) => request({ url: `/project/${id}/stats`, method: 'get' }),
  /** V24 看板状态列配置 */
  statusColumns: (id: number) => request<{ data?: Array<{ id: number; name: string; statusValue: string; sortOrder: number; isDone: number }> }>({ url: `/project/${id}/status-columns`, method: 'get' }),
  createStatusColumn: (id: number, data: { name: string; statusValue: string; sortOrder?: number; isDone?: number }) =>
    request({ url: `/project/${id}/status-columns`, method: 'post', data }),
  updateStatusColumn: (columnId: number, data: { name?: string; statusValue?: string; sortOrder?: number; isDone?: number }) =>
    request({ url: `/project/status-columns/${columnId}`, method: 'put', data }),
  removeStatusColumn: (columnId: number) =>
    request({ url: `/project/status-columns/${columnId}`, method: 'delete' }),
  updateStatusColumnsOrder: (id: number, columnIds: number[]) =>
    request({ url: `/project/${id}/status-columns/order`, method: 'put', data: columnIds }),
}

/** 任务 */
export const projectTaskApi = {
  page: (projectId: number, params: any) =>
    request({ url: `/project/${projectId}/tasks`, method: 'get', params }),
  tree: (projectId: number) => request({ url: `/project/${projectId}/tasks/tree`, method: 'get' }),
  get: (taskId: number) => request({ url: `/project/task/${taskId}`, method: 'get' }),
  create: (projectId: number, data: any) =>
    request({ url: `/project/${projectId}/tasks`, method: 'post', data }),
  update: (taskId: number, data: any) =>
    request({ url: `/project/task/${taskId}`, method: 'put', data }),
  remove: (taskId: number) => request({ url: `/project/task/${taskId}`, method: 'delete' }),
  updateStatus: (taskId: number, status: string) =>
    request({ url: `/project/task/${taskId}/status`, method: 'put', params: { status } }),
  updateAssignee: (taskId: number, assigneeId: number) =>
    request({ url: `/project/task/${taskId}/assignee`, method: 'put', params: { assigneeId } }),
  updateProgress: (taskId: number, progress: number) =>
    request({ url: `/project/task/${taskId}/progress`, method: 'put', params: { progress } }),
  dependencies: (taskId: number) =>
    request({ url: `/project/task/${taskId}/dependencies`, method: 'get' }),
  addDependency: (taskId: number, dependsOnTaskId: number, type?: string) =>
    request({ url: `/project/task/${taskId}/dependencies`, method: 'post', params: { dependsOnTaskId, type } }),
  removeDependency: (taskId: number, depId: number) =>
    request({ url: `/project/task/${taskId}/dependencies/${depId}`, method: 'delete' }),
  board: (projectId: number) => request({ url: `/project/${projectId}/board`, method: 'get' }),
  gantt: (projectId: number) => request({ url: `/project/${projectId}/gantt`, method: 'get' }),
  calendar: (projectId: number) => request({ url: `/project/${projectId}/calendar`, method: 'get' }),
}

/** 里程碑 */
export const projectMilestoneApi = {
  list: (projectId: number) => request({ url: `/project/${projectId}/milestones`, method: 'get' }),
  create: (projectId: number, data: any) =>
    request({ url: `/project/${projectId}/milestones`, method: 'post', data }),
  update: (id: number, data: any) =>
    request({ url: `/project/milestone/${id}`, method: 'put', data }),
  remove: (id: number) => request({ url: `/project/milestone/${id}`, method: 'delete' }),
  setComplete: (id: number, complete: boolean) =>
    request({ url: `/project/milestone/${id}/complete`, method: 'put', params: { complete } }),
}
