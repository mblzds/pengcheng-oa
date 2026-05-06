/**
 * Runtime config helpers for mobile/mini-program.
 * Keep API base URL configurable to avoid hardcoded localhost in real devices.
 */

const API_BASE_URL_KEY = 'api_base_url'
const DEFAULT_API_BASE_URL = 'http://localhost:8080'

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

