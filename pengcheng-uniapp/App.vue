<script>
	import { isLoggedIn } from './utils/auth.js'
	import wsClient from './utils/websocket.js'
	import { fetchCryptoConfig } from './utils/crypto.js'

	export default {
		onLaunch: function() {
			console.log('App Launch')
			// 预加载加密配置
			fetchCryptoConfig()
			if (isLoggedIn()) {
				wsClient.connect()
				this.setupGlobalListeners()
				this.checkPendingSubmissions()
				this.setupNetworkListeners()
			} else {
				uni.reLaunch({ url: '/pages/login/index' })
			}
		},
		onShow: function() {
			console.log('App Show')
		},
		onHide: function() {
			console.log('App Hide')
		},
		methods: {
			setupGlobalListeners() {
				// 防止重复绑定
				if (this.noticeHandler) wsClient.off('notice', this.noticeHandler)
				if (this.unreadHandler) wsClient.off('unreadCount', this.unreadHandler)
				if (this.dataChangeHandler) wsClient.off('dataChange', this.dataChangeHandler)

				const updateMessageBadge = (count) => {
					const safeCount = Math.max(0, Number(count) || 0)
					if (safeCount <= 0) {
						uni.removeTabBarBadge({ index: 1 }).catch(() => {})
						uni.removeStorageSync('message_badge_count')
						return
					}
					const text = safeCount > 99 ? '99+' : String(safeCount)
					uni.setTabBarBadge({ index: 1, text }).catch(() => {})
					uni.setStorageSync('message_badge_count', safeCount)
				}

				// 消息通知 Badge 更新
				this.noticeHandler = (payload) => {
					const explicitCount = payload?.unreadCount ?? payload?.count
					if (explicitCount != null) {
						updateMessageBadge(explicitCount)
						return
					}
					const current = Number(uni.getStorageSync('message_badge_count') || 0)
					updateMessageBadge(current + 1)
				}
				wsClient.on('notice', this.noticeHandler)

				this.unreadHandler = (payload) => {
					const count = payload?.unreadCount ?? payload?.count ?? payload?.total
					updateMessageBadge(count)
				}
				wsClient.on('unreadCount', this.unreadHandler)

				// 数据变更提示
				this.dataChangeHandler = () => {
					uni.$emit('app:data-change')
				}
				wsClient.on('dataChange', this.dataChangeHandler)
			},
			setupNetworkListeners() {
				if (this.networkStatusHandler) return
				this.networkStatusHandler = (res) => {
					if (res && res.isConnected) {
						this.checkPendingSubmissions()
					}
				}
				uni.onNetworkStatusChange(this.networkStatusHandler)
			},
			checkPendingSubmissions() {
				try {
					const pending = uni.getStorageSync('pending_submissions') || []
					if (pending.length > 0) {
						uni.showModal({
							title: '未提交的数据',
							content: `您有 ${pending.length} 条未提交的数据，是否重新提交？`,
							success: (res) => {
								if (res.confirm) {
									uni.showToast({ title: '请前往对应页面重新提交', icon: 'none' })
								}
								uni.removeStorageSync('pending_submissions')
							}
						})
					}
				} catch (e) { /* ignore */ }
			}
		}
	}
</script>

<style lang="scss">
	@import "uview-plus/index.scss";

	page {
		background-color: #EDEDED;
		font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif;
		font-size: 28rpx;
		color: #1A1A1A;
		line-height: 1.5;
	}

		.safe-area-bottom {
			padding-bottom: constant(safe-area-inset-bottom);
			padding-bottom: env(safe-area-inset-bottom);
		}

	.container {
		padding: 20rpx;
	}

	.avatar {
		width: 80rpx;
		height: 80rpx;
		border-radius: 8rpx;
		background-color: #E0E0E0;
	}

	.avatar-sm {
		width: 60rpx;
		height: 60rpx;
		border-radius: 6rpx;
	}

	.avatar-lg {
		width: 120rpx;
		height: 120rpx;
		border-radius: 12rpx;
	}

	.avatar-circle {
		border-radius: 50%;
	}

	.divider {
		height: 1rpx;
		background-color: #E5E5E5;
	}

	.card {
		background-color: #FFFFFF;
		border-radius: 12rpx;
		margin: 16rpx;
		padding: 24rpx;
	}

	.btn-primary {
		background: linear-gradient(135deg, #06AD56, #07C160);
		color: #FFFFFF;
		border: none;
		border-radius: 8rpx;
		font-size: 30rpx;
		font-weight: 500;
		padding: 24rpx 0;
		text-align: center;
	}

	.btn-primary:active {
		opacity: 0.85;
	}

	.badge {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		min-width: 32rpx;
		height: 32rpx;
		padding: 0 8rpx;
		border-radius: 32rpx;
		background-color: #FA5151;
		color: #FFFFFF;
		font-size: 20rpx;
		font-weight: 600;
		line-height: 1;
	}

	.loading-wrap {
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 40rpx;
		color: #999999;
		font-size: 26rpx;
	}
</style>
