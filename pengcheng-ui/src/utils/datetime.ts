/**
 * 统一的日期时间格式化工具
 *
 * 项目里历史上散落了 8+ 处各自实现的 formatDateTime，行为略有差异（空值兜底、
 * 异常值处理）。后续新页面统一从这里 import。老页面有空逐步迁过来。
 */

type DateInput = string | number | Date | null | undefined

function toDate(value: DateInput): Date | null {
  if (value == null || value === '') return null
  if (value instanceof Date) {
    return Number.isNaN(value.getTime()) ? null : value
  }
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? null : d
}

const pad2 = (n: number) => String(n).padStart(2, '0')

/** YYYY-MM-DD HH:mm:ss；空值或无效值返回 fallback（默认 '-'） */
export function formatDateTime(value: DateInput, fallback = '-'): string {
  const d = toDate(value)
  if (!d) return fallback
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`
}

/** YYYY-MM-DD；空值或无效值返回 fallback（默认 '-'） */
export function formatDate(value: DateInput, fallback = '-'): string {
  const d = toDate(value)
  if (!d) return fallback
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`
}

/** HH:mm；空值或无效值返回 fallback（默认 '-'） */
export function formatTime(value: DateInput, fallback = '-'): string {
  const d = toDate(value)
  if (!d) return fallback
  return `${pad2(d.getHours())}:${pad2(d.getMinutes())}`
}

/** YYYY-MM-DD HH:mm（不含秒）；适合"申请时间""创建时间"这类列表展示 */
export function formatDateTimeMinute(value: DateInput, fallback = '-'): string {
  const d = toDate(value)
  if (!d) return fallback
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(d.getMinutes())}`
}
