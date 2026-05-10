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
					<u-icon name="weixin-fill" color="#FFFFFF" size="20"></u-icon>
					<text class="wx-btn-text">微信授权登录</text>
				</button>

				<view v-else class="bind-section">
					<view class="bind-tip">首次登录，请用手机号 + 验证码绑定到现有账号</view>
					<view class="phone-form">
						<view class="input-group">
							<view class="input-wrap">
								<u-icon name="phone" color="#BBB" size="18" style="margin-right: 16rpx;"></u-icon>
								<input class="input-field" type="number" maxlength="11"
									v-model="bindPhone" placeholder="请输入手机号" placeholder-class="placeholder" />
							</view>
						</view>
						<view class="input-group">
							<view class="input-wrap">
								<u-icon name="lock" color="#BBB" size="18" style="margin-right: 16rpx;"></u-icon>
								<input class="input-field code-field" type="number" maxlength="6"
									v-model="bindSmsCode" placeholder="请输入验证码" placeholder-class="placeholder" />
								<view class="code-btn" :class="{ 'code-disabled': bindCodeCountdown > 0 }" @tap="handleSendBindCode">
									<text>{{ bindCodeCountdown > 0 ? `${bindCodeCountdown}s` : '获取验证码' }}</text>
								</view>
							</view>
						</view>
					</view>
					<button class="wx-login-btn" @tap="handleBindBySms" :loading="loading">
						<text class="wx-btn-text">提交并绑定</text>
					</button>
					<view class="bind-cancel" @tap="cancelBind"><text>取消</text></view>
				</view>

				<view class="divider-line" v-if="!needBindPhone">
					<view class="line"></view>
					<text class="divider-text">或</text>
					<view class="line"></view>
				</view>

				<view class="toggle-login" v-if="!needBindPhone" @tap="showPhoneLogin = !showPhoneLogin">
					<text>{{ showPhoneLogin ? '返回微信登录' : '手机号登录' }}</text>
				</view>

				<view class="phone-form" v-if="showPhoneLogin">
					<view class="input-group">
						<view class="input-wrap">
							<u-icon name="phone" color="#BBB" size="18" style="margin-right: 16rpx;"></u-icon>
							<input class="input-field" type="number" maxlength="11"
								v-model="phone" placeholder="请输入手机号" placeholder-class="placeholder" />
						</view>
					</view>
					<view class="input-group">
						<view class="input-wrap">
							<u-icon name="lock" color="#BBB" size="18" style="margin-right: 16rpx;"></u-icon>
							<input class="input-field code-field" type="number" maxlength="6"
								v-model="smsCode" placeholder="请输入验证码" placeholder-class="placeholder" />
							<view class="code-btn" :class="{ 'code-disabled': codeCountdown > 0 }" @tap="handleSendCode">
								<text>{{ codeCountdown > 0 ? `${codeCountdown}s` : '获取验证码' }}</text>
							</view>
						</view>
					</view>
					<button class="phone-login-btn" @tap="handlePhoneLogin" :loading="loading">登录</button>
				</view>
			</view>

			<view class="agreement">
				<text class="agree-text">登录即表示同意</text>
				<text class="agree-link">《用户协议》</text>
				<text class="agree-text">和</text>
				<text class="agree-link">《隐私政策》</text>
			</view>
		</view>
	</view>
</template>

	<script>
		import { wxLogin, sendSmsCode } from '../../utils/api.js'
		import { setToken, setUserInfo } from '../../utils/auth.js'
		import wsClient from '../../utils/websocket.js'

	export default {
		data() {
			return {
				loading: false,
				showPhoneLogin: false,
				phone: '', smsCode: '', codeCountdown: 0, codeTimer: null,
				// 微信登录返回 BIND_REQUIRED(4001) 时进入"短信验证码绑定"分支
				needBindPhone: false,
				bindPhone: '', bindSmsCode: '', bindCodeCountdown: 0, bindCodeTimer: null
			}
		},
		onUnload() {
			if (this.codeTimer) clearInterval(this.codeTimer)
			if (this.bindCodeTimer) clearInterval(this.bindCodeTimer)
		},
		methods: {
			completeLogin(data) {
				setToken(data.token)
				setUserInfo({
					userId: data.userId,
					username: data.nickname || data.username || '',
					nickname: data.nickname || '',
					avatar: data.avatar || ''
				})
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

			async handleSendBindCode() {
				if (this.bindCodeCountdown > 0) return
				if (!this.bindPhone || !/^1[3-9]\d{9}$/.test(this.bindPhone)) {
					uni.showToast({ title: '请输入正确的手机号', icon: 'none' }); return
				}
				try {
					await sendSmsCode({ phone: this.bindPhone })
					uni.showToast({ title: '验证码已发送', icon: 'success' })
					this.bindCodeCountdown = 60
					this.bindCodeTimer = setInterval(() => {
						this.bindCodeCountdown--
						if (this.bindCodeCountdown <= 0) clearInterval(this.bindCodeTimer)
					}, 1000)
				} catch (err) { console.error('发送验证码失败:', err) }
			},

			async handleBindBySms() {
				if (this.loading) return
				if (!this.bindPhone || !/^1[3-9]\d{9}$/.test(this.bindPhone)) {
					return uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
				}
				if (!this.bindSmsCode || this.bindSmsCode.length < 4) {
					return uni.showToast({ title: '请输入验证码', icon: 'none' })
				}
				this.loading = true
				try {
					// 重新拿 wxCode：前一次的可能已经过期或被消费
					const loginRes = await new Promise((resolve, reject) => {
						uni.login({ provider: 'weixin', success: resolve, fail: reject })
					})
					const res = await wxLogin({
						wxCode: loginRes.code,
						phone: this.bindPhone,
						smsCode: this.bindSmsCode,
						loginType: 'MINIPROGRAM'
					})
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
				this.bindPhone = ''
				this.bindSmsCode = ''
			},

			async handleSendCode() {
				if (this.codeCountdown > 0) return
				if (!this.phone || !/^1[3-9]\d{9}$/.test(this.phone)) {
					uni.showToast({ title: '请输入正确的手机号', icon: 'none' }); return
				}
				try {
					await sendSmsCode({ phone: this.phone })
					uni.showToast({ title: '验证码已发送', icon: 'success' })
					this.codeCountdown = 60
					this.codeTimer = setInterval(() => {
						this.codeCountdown--
						if (this.codeCountdown <= 0) clearInterval(this.codeTimer)
					}, 1000)
				} catch (err) { console.error('发送验证码失败:', err) }
			},

			async handlePhoneLogin() {
				if (this.loading) return
				if (!this.phone || !/^1[3-9]\d{9}$/.test(this.phone)) {
					uni.showToast({ title: '请输入正确的手机号', icon: 'none' }); return
				}
				if (!this.smsCode || this.smsCode.length < 4) {
					uni.showToast({ title: '请输入验证码', icon: 'none' }); return
				}
				this.loading = true
				try {
					const res = await wxLogin({ phone: this.phone, smsCode: this.smsCode, loginType: 'SMS' })
					this.completeLogin(res.data)
				} catch (err) { console.error('登录失败:', err) }
				finally { this.loading = false }
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

	.divider-line { display: flex; align-items: center; margin: 32rpx 0 20rpx; }
	.divider-line .line { flex: 1; height: 1rpx; background: #EBEBEB; }
	.divider-text { padding: 0 20rpx; font-size: 22rpx; color: #BBB; }

	.toggle-login {
		text-align: center; padding: 8rpx 0;
		text { font-size: 26rpx; color: #07C160; }
	}

	.phone-form {
		margin-top: 24rpx;
		.input-group { margin-bottom: 20rpx; }
		.input-wrap {
			display: flex; align-items: center;
			height: 84rpx; background: #F8F8F8; border-radius: 42rpx;
			padding: 0 28rpx; border: 1rpx solid #EBEBEB;
		}
		.input-field { flex: 1; height: 84rpx; font-size: 28rpx; color: #333; }
		.code-field { flex: 1; }
		.code-btn {
			padding: 0 20rpx; height: 52rpx; line-height: 52rpx;
			border-left: 1rpx solid #E0E0E0; margin-left: 16rpx; padding-left: 20rpx;
			text { font-size: 24rpx; color: #07C160; }
		}
		.code-disabled text { color: #BBB; }
	}

	.phone-login-btn {
		height: 88rpx; background: linear-gradient(135deg, #06AD56, #07C160);
		color: #FFF; font-size: 30rpx; font-weight: 500;
		border-radius: 44rpx; border: none; margin-top: 8rpx;
	}
	.phone-login-btn::after { border: none; }

	.agreement {
		display: flex; align-items: center; justify-content: center;
		flex-wrap: wrap; padding: 32rpx 0;
		.agree-text { font-size: 22rpx; color: #BBB; }
		.agree-link { font-size: 22rpx; color: #07C160; }
	}

	.placeholder { color: #CCC; }
</style>
