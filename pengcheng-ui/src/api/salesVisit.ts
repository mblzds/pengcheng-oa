import { request } from '@/utils/request'

export const salesVisitApi = {
  list: (params: any) => request({ url: '/realty/visit/list', method: 'get', params }),
  detail: (id: number) => request({ url: `/realty/visit/${id}`, method: 'get' }),
  create: (data: any) => request({ url: '/realty/visit', method: 'post', data }),
  update: (data: any) => request({ url: '/realty/visit', method: 'put', data }),
  remove: (id: number) => request({ url: `/realty/visit/${id}`, method: 'delete' }),
  saveTags: (id: number, tags: any[]) => request({ url: `/realty/visit/${id}/tags`, method: 'post', data: tags }),
  stats: (userId?: number) => request({ url: '/realty/visit/stats', method: 'get', params: { userId } }),
  ranking: (deptId: number) => request({ url: '/realty/visit/ranking', method: 'get', params: { deptId } }),
  uploadAudio: (id: number, audioUrl: string) => request({ url: `/realty/visit/${id}/audio`, method: 'post', params: { audioUrl } }),
  analyze: (id: number) => request({ url: `/realty/visit/${id}/analyze`, method: 'post' }),
}
