import { request } from '@/utils/request'

// ==================== 微信小程序 ====================

export interface MiniProgramLoginResult {
  openId: string
  unionId?: string
}

export interface PhoneResult {
  phone: string
}

// ==================== 微信公众号 ====================

export interface MpOAuthResult {
  openId: string
  unionId?: string
  nickname?: string
  headImgUrl?: string
  sex?: number
}

export const wechatApi = {
  // 小程序登录
  miniProgramLogin(code: string): Promise<MiniProgramLoginResult> {
    return request({ url: '/wechat/miniprogram/login', method: 'post', data: { code } })
  },

  getMiniProgramPhone(code: string): Promise<PhoneResult> {
    return request({ url: '/wechat/miniprogram/phone', method: 'post', data: { code } })
  },

  getOAuthUrl(redirectUri: string, state?: string, scope?: string): Promise<string> {
    return request({ 
      url: '/wechat/mp/oauth-url', 
      method: 'get', 
      params: { redirectUri, state: state || '', scope: scope || 'snsapi_userinfo' } 
    })
  },

  mpOAuthLogin(code: string): Promise<MpOAuthResult> {
    return request({ url: '/wechat/mp/oauth-login', method: 'post', data: { code } })
  },

  syncMenu(menuConfig: string): Promise<void> {
    return request({ url: '/wechat/mp/menu/sync', method: 'post', data: { menuConfig } })
  },

  getMenu(): Promise<string> {
    return request({ url: '/wechat/mp/menu', method: 'get' })
  },

  deleteMenu(): Promise<void> {
    return request({ url: '/wechat/mp/menu', method: 'delete' })
  }
}
