/**
 * 桌面通知 + 声音提示工具
 *
 * 使用 Web Notification API 实现新消息桌面推送，
 * 配合 AudioContext 播放提示音，无需额外音频文件。
 */

let notificationPermission: NotificationPermission = 'default'
let audioContext: AudioContext | null = null

/**
 * 请求通知权限（应在用户交互后调用）
 */
export async function requestNotificationPermission(): Promise<boolean> {
  if (!('Notification' in window)) {
    console.warn('[Notification] 浏览器不支持桌面通知')
    return false
  }

  if (Notification.permission === 'granted') {
    notificationPermission = 'granted'
    return true
  }

  if (Notification.permission === 'denied') {
    notificationPermission = 'denied'
    return false
  }

  const permission = await Notification.requestPermission()
  notificationPermission = permission
  return permission === 'granted'
}

/**
 * 发送桌面通知
 */
export function showDesktopNotification(title: string, options?: {
  body?: string
  icon?: string
  tag?: string
  onClick?: () => void
}): Notification | null {
  if (notificationPermission !== 'granted') return null
  if (document.hasFocus()) return null

  try {
    const notification = new Notification(title, {
      body: options?.body || '',
      icon: options?.icon || '/favicon.ico',
      tag: options?.tag || 'pengcheng-msg',
      silent: true
    })

    notification.onclick = () => {
      window.focus()
      notification.close()
      options?.onClick?.()
    }

    setTimeout(() => notification.close(), 8000)
    return notification
  } catch (e) {
    console.warn('[Notification] 创建通知失败', e)
    return null
  }
}

/**
 * 播放消息提示音（使用 Web Audio API 合成，无需音频文件）
 */
export function playNotificationSound(type: 'message' | 'mention' | 'notice' = 'message') {
  try {
    if (!audioContext) {
      audioContext = new AudioContext()
    }

    const ctx = audioContext
    if (ctx.state === 'suspended') {
      ctx.resume()
    }

    const oscillator = ctx.createOscillator()
    const gainNode = ctx.createGain()

    oscillator.connect(gainNode)
    gainNode.connect(ctx.destination)

    gainNode.gain.setValueAtTime(0.15, ctx.currentTime)

    switch (type) {
      case 'mention':
        oscillator.frequency.setValueAtTime(880, ctx.currentTime)
        oscillator.frequency.setValueAtTime(1100, ctx.currentTime + 0.1)
        oscillator.frequency.setValueAtTime(880, ctx.currentTime + 0.2)
        gainNode.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.4)
        oscillator.start(ctx.currentTime)
        oscillator.stop(ctx.currentTime + 0.4)
        break
      case 'notice':
        oscillator.frequency.setValueAtTime(660, ctx.currentTime)
        oscillator.frequency.setValueAtTime(880, ctx.currentTime + 0.15)
        gainNode.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.3)
        oscillator.start(ctx.currentTime)
        oscillator.stop(ctx.currentTime + 0.3)
        break
      default:
        oscillator.frequency.setValueAtTime(800, ctx.currentTime)
        gainNode.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.15)
        oscillator.start(ctx.currentTime)
        oscillator.stop(ctx.currentTime + 0.15)
    }
  } catch (e) {
    console.warn('[Sound] 播放提示音失败', e)
  }
}

/**
 * 通知设置（存储在 localStorage）
 */
export interface NotificationSettings {
  desktopEnabled: boolean
  soundEnabled: boolean
  muteUntil: number | null
}

const SETTINGS_KEY = 'pengcheng_notification_settings'

export function getNotificationSettings(): NotificationSettings {
  try {
    const stored = localStorage.getItem(SETTINGS_KEY)
    if (stored) return JSON.parse(stored)
  } catch { /* ignore */ }
  return { desktopEnabled: true, soundEnabled: true, muteUntil: null }
}

export function saveNotificationSettings(settings: NotificationSettings) {
  localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings))
}

export function isMuted(): boolean {
  const settings = getNotificationSettings()
  if (settings.muteUntil && Date.now() < settings.muteUntil) return true
  return false
}

/**
 * 统一通知触发入口
 */
export function triggerNotification(title: string, body: string, type: 'message' | 'mention' | 'notice' = 'message', onClick?: () => void) {
  if (isMuted()) return

  const settings = getNotificationSettings()

  if (settings.soundEnabled) {
    playNotificationSound(type)
  }

  if (settings.desktopEnabled) {
    showDesktopNotification(title, { body, onClick })
  }
}
