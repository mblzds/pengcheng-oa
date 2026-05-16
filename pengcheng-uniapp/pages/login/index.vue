<template>
	<view class="login-page">
		<!-- 顶部区域 -->
		<view class="login-header">
			<view class="header-bg"></view>
			<view class="logo-area">
				<image class="logo" src="/static/logo.png" mode="aspectFit"></image>
				<text class="app-name">MasterLife</text>
				<text class="app-desc">简洁高效的管理平台</text>
			</view>
		</view>

		<!-- 主体区域 -->
		<view class="login-body">
			<view class="login-card">
				<button v-if="!needBindPhone" class="wx-login-btn" @tap="handleWxLogin" :loading="loading">
					<pc-icon name="weixin-fill" color="#FFFFFF" size="20"></pc-icon>
					<text class="wx-btn-text">微信授权登录</text>
				</button>

				<view v-else class="bind-section">
					<view class="bind-tip">首次登录，需授权手机号绑定到现有账号</view>
					<button class="wx-login-btn" open-type="getPhoneNumber" @getphonenumber="handleBindPhone" :loading="loading">
						<text class="wx-btn-text">授权手机号完成绑定</text>
					</button>
					<view class="bind-cancel" @tap="cancelBind"><text>取消</text></view>
				</view>
			</view>

			<view class="agreement">
				<text class="agree-text">登录即表示同意</text>
				<text class="agree-link">《用户协议》</text>
				<text class="agree-text">和</text>
				<text class="agree-link">《隐私政策》</text>
			</view>
		</view>

		<!-- 隐私协议引导弹窗：当系统检测到 needAuthorization 时弹出 -->
		<view v-if="showPrivacyModal" class="privacy-mask" @tap.stop>
			<view class="privacy-modal">
				<view class="privacy-title">隐私协议</view>
				<view class="privacy-content">
					使用本小程序前需同意
					<text class="privacy-link" @tap="openPrivacyContract">《用户隐私保护指引》</text>
					。同意后才能完成微信登录与手机号绑定。
				</view>
				<view class="privacy-actions">
					<button class="privacy-btn privacy-reject" @tap="rejectPrivacy">拒绝</button>
					<button class="privacy-btn privacy-agree"
							open-type="agreePrivacyAuthorization"
							@agreeprivacyauthorization="onAgreePrivacy">同意并继续</button>
				</view>
			</view>
		</view>
	</view>
</template>

	<script>
		import { wxLogin, getAppRoleCodes } from '../../utils/api.js'
		import { setToken, setUserInfo, getUserInfo } from '../../utils/auth.js'
		import wsClient from '../../utils/websocket.js'

	export default {
		data() {
			return {
				loading: false,
				// 微信登录返回 BIND_REQUIRED(4001) 时进入"授权手机号绑定"分支
				needBindPhone: false,
				// 隐私协议引导弹窗
				showPrivacyModal: false
			}
		},
		methods: {
			async completeLogin(data) {
				setToken(data.token)
				setUserInfo({
					userId: data.userId,
					username: data.nickname || data.username || '',
					nickname: data.nickname || '',
					avatar: data.avatar || '',
					roleCodes: []
				})
				// 顺手拉一次角色列表缓存，供前端按角色显隐 UI（如 AI 助手仅 admin 可见）
				// 失败不阻塞登录流程，最差情况下管理员菜单暂时不显示，用户重新进页面会再拉
				try {
					const r = await getAppRoleCodes()
					const codes = Array.isArray(r.data) ? r.data : []
					const info = getUserInfo() || {}
					info.roleCodes = codes
					setUserInfo(info)
				} catch (e) {
					console.warn('拉取角色列表失败，按非管理员处理：', e)
				}
				wsClient.connect()
				uni.showToast({ title: '登录成功', icon: 'success' })
				setTimeout(() => { uni.switchTab({ url: '/pages/index/index' }) }, 500)
			},

			async handleWxLogin() {
				if (this.loading) return
				this.loading = true
				try {
					// #ifdef MP-WEIXIN
					const loginRes = await new Promise((resolve, reject) => {
						uni.login({ provider: 'weixin', success: resolve, fail: reject })
					})
					const res = await wxLogin({ wxCode: loginRes.code, loginType: 'MINIPROGRAM' })
					this.completeLogin(res.data)
					// #endif
					// #ifndef MP-WEIXIN
					uni.showToast({ title: '请在微信小程序中使用', icon: 'none' })
					// #endif
				} catch (err) {
					console.error('登录失败:', err)
					if (err && err.code === 4001) {
						// openId 未绑定到现有账号；切到"授权手机号绑定"分支
						this.needBindPhone = true
						return
					}
					uni.showToast({ title: err?.message || '登录失败，请重试', icon: 'none' })
				} finally { this.loading = false }
			},

			async handleBindPhone(e) {
				if (this.loading) return
				const phoneCode = e?.detail?.code
				if (!phoneCode) {
					// 拿不到 code 有几种原因，按概率诊断给出具体提示
					const errMsg = (e?.detail?.errMsg || '').toLowerCase()
					// #ifdef MP-WEIXIN
					const needPrivacy = await this.checkPrivacyAuth()
					if (needPrivacy) {
						this.showPrivacyModal = true
						return
					}
					// #endif
					if (errMsg.includes('deny') || errMsg.includes('cancel')) {
						uni.showToast({ title: '已取消授权', icon: 'none' })
					} else {
						uni.showModal({
							title: '获取手机号失败',
							content: '请确认小程序后台已：\n1) 通过企业主体认证\n2) 配置并审核通过《用户隐私保护指引》\n3) 已开通"获取手机号"接口\n4) 接口余额充足',
							showCancel: false
						})
					}
					return
				}
				this.loading = true
				try {
					// 重新拿一个 wxCode（前一次的可能已过期或已被消费）
					const loginRes = await new Promise((resolve, reject) => {
						uni.login({ provider: 'weixin', success: resolve, fail: reject })
					})
					const res = await wxLogin({ wxCode: loginRes.code, phoneCode, loginType: 'MINIPROGRAM' })
					this.needBindPhone = false
					this.completeLogin(res.data)
				} catch (err) {
					console.error('绑定失败:', err)
					if (err && err.code === 4002) {
						uni.showToast({ title: err.message || '该手机号未注册，请联系管理员', icon: 'none', duration: 3000 })
					} else {
						uni.showToast({ title: err?.message || '绑定失败，请重试', icon: 'none' })
					}
				} finally { this.loading = false }
			},

			cancelBind() {
				this.needBindPhone = false
			},

			// 隐私协议是否需要用户授权（true=需要，调用方应弹引导）
			checkPrivacyAuth() {
				// #ifdef MP-WEIXIN
				return new Promise((resolve) => {
					if (typeof wx === 'undefined' || !wx.getPrivacySetting) {
						// 老版本基础库没有该 API，不阻塞流程
						resolve(false)
						return
					}
					wx.getPrivacySetting({
						success: (res) => resolve(!!res.needAuthorization),
						fail: () => resolve(false)
					})
				})
				// #endif
				// #ifndef MP-WEIXIN
				return Promise.resolve(false)
				// #endif
			},

			onAgreePrivacy() {
				this.showPrivacyModal = false
				uni.showToast({ title: '已同意，请再次点击授权手机号', icon: 'none' })
			},

			rejectPrivacy() {
				this.showPrivacyModal = false
				uni.showToast({ title: '未同意隐私协议，无法登录', icon: 'none' })
			},

			openPrivacyContract() {
				// #ifdef MP-WEIXIN
				if (typeof wx !== 'undefined' && wx.openPrivacyContract) {
					wx.openPrivacyContract({
						fail: (err) => console.warn('打开隐私协议失败', err)
					})
				}
				// #endif
			}
		}
	}
</script>

<style lang="scss" scoped>
	.login-page { min-height: 100vh; min-height: 100dvh; background: #F5F5F5; position: relative; }

	.login-header { position: relative; height: 600rpx; }
	.header-bg {
		position: absolute; top: 0; left: 0; right: 0; height: 520rpx;
		background: linear-gradient(160deg, #06AD56 0%, #07C160 50%, #2BD373 100%);
		border-radius: 0 0 60rpx 60rpx;
	}
	.logo-area {
		position: relative; z-index: 1;
		display: flex; flex-direction: column; align-items: center; padding-top: 200rpx;
	}
	.logo {
		width: 130rpx; height: 130rpx; border-radius: 28rpx;
		background: #FFF; box-shadow: 0 8rpx 32rpx rgba(0,0,0,0.15);
	}
	.app-name { font-size: 44rpx; font-weight: 700; color: #FFF; margin-top: 24rpx; letter-spacing: 4rpx; }
	.app-desc { font-size: 24rpx; color: rgba(255,255,255,0.7); margin-top: 10rpx; letter-spacing: 2rpx; }

	.login-body { padding: 0 40rpx; margin-top: -40rpx; }
	.login-card {
		background: #FFF; border-radius: 20rpx; padding: 48rpx 36rpx;
		box-shadow: 0 4rpx 24rpx rgba(0,0,0,0.06);
	}

	.wx-login-btn {
		display: flex; align-items: center; justify-content: center;
		height: 88rpx; background: linear-gradient(135deg, #06AD56, #07C160);
		color: #FFF; font-size: 30rpx; font-weight: 500;
		border-radius: 44rpx; border: none;
	}
	.wx-login-btn::after { border: none; }
	.wx-btn-text { margin-left: 10rpx; }

	.bind-section {
		display: flex; flex-direction: column; align-items: stretch; gap: 20rpx;
		.bind-tip { font-size: 26rpx; color: #666; text-align: center; line-height: 1.6; padding: 8rpx 0 12rpx; }
		.bind-cancel { text-align: center; padding: 8rpx 0; text { font-size: 26rpx; color: #999; } }
	}

	.agreement {
		display: flex; align-items: center; justify-content: center;
		flex-wrap: wrap; padding: 32rpx 0;
		.agree-text { font-size: 22rpx; color: #BBB; }
		.agree-link { font-size: 22rpx; color: #07C160; }
	}

	.privacy-mask {
		position: fixed; inset: 0; background: rgba(0,0,0,0.5);
		display: flex; align-items: center; justify-content: center;
		z-index: 999;
	}
	.privacy-modal {
		width: 600rpx; background: #FFF; border-radius: 16rpx;
		padding: 48rpx 36rpx 32rpx;
	}
	.privacy-title { font-size: 32rpx; font-weight: 600; color: #333; text-align: center; }
	.privacy-content {
		font-size: 26rpx; color: #555; line-height: 1.6;
		margin: 28rpx 0 36rpx;
	}
	.privacy-link { color: #07C160; }
	.privacy-actions { display: flex; gap: 20rpx; }
	.privacy-btn {
		flex: 1; height: 80rpx; line-height: 80rpx; border-radius: 12rpx;
		font-size: 28rpx; padding: 0;
	}
	.privacy-btn::after { border: none; }
	.privacy-reject { background: #F2F2F2; color: #666; }
	.privacy-agree { background: #07C160; color: #FFF; }
</style>
