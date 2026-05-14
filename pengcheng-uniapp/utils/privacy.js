/**
 * 隐私授权预检工具
 *
 * 自 2023-09-15 起，微信小程序对涉及"用户隐私信息"的 API（如 getLocation、
 * chooseImage、chooseAddress 等）要求在调用前确认用户已同意《用户隐私保护
 * 指引》。开发者工具不强制，体验版/正式版会直接拦截。
 *
 * 用法：
 *   import { ensurePrivacyAuth } from '@/utils/privacy.js'
 *   const ok = await ensurePrivacyAuth()
 *   if (!ok) return
 *   uni.chooseImage({ ... })
 */

export function ensurePrivacyAuth() {
	// #ifdef MP-WEIXIN
	return new Promise((resolve) => {
		if (typeof wx === 'undefined' || !wx.getPrivacySetting) {
			// 老版本基础库没有该 API，按通过处理（微信会在调用时自动弹默认协议）
			resolve(true); return
		}
		wx.getPrivacySetting({
			success: (res) => {
				if (!res.needAuthorization) { resolve(true); return }
				if (!wx.requirePrivacyAuthorize) { resolve(true); return }
				wx.requirePrivacyAuthorize({
					success: () => resolve(true),
					fail: () => resolve(false)
				})
			},
			fail: () => resolve(true)
		})
	})
	// #endif
	// #ifndef MP-WEIXIN
	return Promise.resolve(true)
	// #endif
}
