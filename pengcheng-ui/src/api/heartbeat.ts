import { request } from '@/utils/request'

/** AI 巡检告警 API */
export const heartbeatApi = {
  list(params: { page?: number; pageSize?: number; severity?: string; handled?: boolean }) {
    return request({ url: '/sys/heartbeat/list', method: 'get', params })
  },
  stats() {
    return request({ url: '/sys/heartbeat/stats', method: 'get' })
  },
  handle(id: number) {
    return request({ url: `/sys/heartbeat/handle/${id}`, method: 'post' })
  },
  batchHandle(ids: number[]) {
    return request({ url: '/sys/heartbeat/batch-handle', method: 'post', data: ids })
  },
  run() {
    return request({ url: '/sys/heartbeat/run', method: 'post' })
  }
}
