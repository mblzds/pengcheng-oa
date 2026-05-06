/**
 * 网络请求封装
 * 统一处理请求头、Token、错误码等
 */
import { isAesEncryptedData, decryptResponseData } from './crypto.js'
import { getApiBaseUrl, joinBaseUrl } from './config.js'

const CACHEABLE_POST_ENDPOINTS = new Set([
  '/api/app/customer/report',
  '/api/app/customer/visit',
  '/api/app/customer/deal',
  '/api/app/attendance/clock',
  '/api/app/attendance/sign',
  '/api/app/leave/apply',
  '/api/app/leave/compensate',
  '/api/app/payment/expense',
  '/api/app/payment/advance',
  '/api/app/payment/prepay'
])
const PENDING_SUBMISSION_KEY = 'pending_submissions'
const MAX_PENDING_SUBMISSIONS = 20

const isCacheableSubmission = (options) => {
  return options?.method === 'POST' && options?.url && CACHEABLE_POST_ENDPOINTS.has(options.url)
}

const cachePendingSubmission = (options) => {
  try {
    const pending = uni.getStorageSync(PENDING_SUBMISSION_KEY) || []
    const signature = `${options.url}|${JSON.stringify(options.data || {})}`
    const next = pending.filter(item => item.signature !== signature)
    next.push({
      signature,
      url: options.url,
      data: options.data || {},
      time: Date.now()
    })
    if (next.length > MAX_PENDING_SUBMISSIONS) {
      next.splice(0, next.length - MAX_PENDING_SUBMISSIONS)
    }
    uni.setStorageSync(PENDING_SUBMISSION_KEY, next)
  } catch (e) {
    // ignore cache failure
  }
}

/**
 * 通用请求方法
 */
const request = (options) => {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    const header = {
      'Content-Type': 'application/json',
      ...options.header
    }
    if (token) {
      header['Authorization'] = token
    }

    uni.request({
      url: joinBaseUrl(options.url),
      method: options.method || 'GET',
      data: options.data,
      header,
      success: (res) => {
        const rejectWith = (message, payload = {}) => {
          const err = new Error(message || '请求失败')
          Object.assign(err, payload)
          reject(err)
        }

        if (res.statusCode === 200) {
          const data = res.data
          // 后端统一返回格式 { code, msg, data }
          if (data.code === 200 || data.code === 0) {
            // 检查响应数据是否是AES加密的，自动解密
            if (isAesEncryptedData(data.data)) {
              decryptResponseData(data.data)
                .then((decrypted) => {
                  data.data = decrypted
                  resolve(data)
                })
                .catch(() => {
                  resolve(data) // 解密失败返回原始数据
                })
            } else {
              resolve(data)
            }
          } else if (data.code === 401) {
            // Token过期，跳转登录
            uni.removeStorageSync('token')
            uni.removeStorageSync('userInfo')
            uni.reLaunch({ url: '/pages/login/index' })
            rejectWith(data.msg || '登录已过期', { code: data.code, statusCode: res.statusCode })
          } else {
            uni.showToast({ title: data.msg || '请求失败', icon: 'none' })
            rejectWith(data.msg || '请求失败', { code: data.code, statusCode: res.statusCode, data })
          }
        } else if (res.statusCode === 401) {
          uni.removeStorageSync('token')
          uni.removeStorageSync('userInfo')
          uni.reLaunch({ url: '/pages/login/index' })
          rejectWith('登录已过期', { statusCode: res.statusCode })
        } else {
          const body = res.data || {}
          const message = body.msg || body.message || `请求失败(${res.statusCode})`
          uni.showToast({ title: message, icon: 'none' })
          rejectWith(message, { code: body.code, statusCode: res.statusCode, data: body })
        }
      },
      fail: (err) => {
        uni.showToast({ title: '网络异常，请检查网络连接', icon: 'none' })
        // 仅缓存关键表单提交，避免敏感请求被误缓存
        if (isCacheableSubmission(options)) {
          cachePendingSubmission(options)
        }
        reject(err)
      }
    })
  })
}

/**
 * GET 请求
 */
export const get = (url, data) => request({ url, method: 'GET', data })

/**
 * POST 请求
 */
export const post = (url, data) => request({ url, method: 'POST', data })

/**
 * PUT 请求
 */
export const put = (url, data) => request({ url, method: 'PUT', data })

/**
 * DELETE 请求
 */
export const del = (url, data) => request({ url, method: 'DELETE', data })

/**
 * 文件上传
 */
export const upload = (url, filePath, name = 'file') => {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    uni.uploadFile({
      url: joinBaseUrl(url),
      filePath,
      name,
      header: {
        'Authorization': token || ''
      },
      success: (res) => {
        if (res.statusCode === 200) {
          const data = JSON.parse(res.data)
          if (data.code === 200 || data.code === 0) {
            // 检查上传响应数据是否加密
            if (isAesEncryptedData(data.data)) {
              decryptResponseData(data.data)
                .then((decrypted) => {
                  data.data = decrypted
                  resolve(data)
                })
                .catch(() => resolve(data))
            } else {
              resolve(data)
            }
          } else {
            reject(new Error(data.msg || '上传失败'))
          }
        } else {
          reject(new Error('上传失败'))
        }
      },
      fail: reject
    })
  })
}

export const getBaseUrl = () => getApiBaseUrl()
// Backward compatible snapshot value (prefer getBaseUrl/joinBaseUrl in new code).
export const BASE_URL = getApiBaseUrl()
export default request
