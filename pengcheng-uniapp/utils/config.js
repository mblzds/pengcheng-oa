/**
 * Runtime config helpers for mobile/mini-program.
 * Keep API base URL configurable to avoid hardcoded localhost in real devices.
 */

const API_BASE_URL_KEY = 'https://www.jiashunpingtai.com/api'
const DEFAULT_API_BASE_URL = 'https://www.jiashunpingtai.com'

const normalizeBaseUrl = (value) => {
  if (!value || typeof value !== 'string') return ''
  const trimmed = value.trim()
  if (!trimmed) return ''
  return trimmed.replace(/\/+$/, '')
}

export const getApiBaseUrl = () => {
  const custom = normalizeBaseUrl(uni.getStorageSync(API_BASE_URL_KEY))
  return custom || DEFAULT_API_BASE_URL
}

export const setApiBaseUrl = (value) => {
  const normalized = normalizeBaseUrl(value)
  if (!normalized) return false
  uni.setStorageSync(API_BASE_URL_KEY, normalized)
  return true
}

export const resetApiBaseUrl = () => {
  uni.removeStorageSync(API_BASE_URL_KEY)
}

export const joinBaseUrl = (path = '') => {
  if (!path) return getApiBaseUrl()
  if (/^https?:\/\//.test(path)) return path
  const prefix = path.startsWith('/') ? '' : '/'
  return `${getApiBaseUrl()}${prefix}${path}`
}

/**
 * 解析头像 URL：空 → 默认头像；绝对 URL → 原样返回；相对路径 → 拼接 API 基地址
 * 用于所有 <image :src="..."> 头像展示，避免相对路径在小程序/原生 App 中无法解析
 */
export const resolveAvatar = (path) => {
  if (!path) return '/static/default-avatar.png'
  return joinBaseUrl(path)
}

/**
 * 解析后端文件 URL（票据图、聊天图片、聊天文件等）：
 *   - 空 → 空串
 *   - 本地预览路径 (wxfile/file/blob) → 原样
 *   - http(s) 绝对地址 → 原样
 *   - 相对路径（如 /api/files/...） → 拼上 API 基地址
 * 与 resolveAvatar 不同的是不会回退到默认头像，空值就是空值。
 */
export const resolveFileUrl = (path) => {
  if (!path) return ''
  if (/^(wxfile|file|blob|https?):/i.test(path)) return path
  return joinBaseUrl(path)
}

