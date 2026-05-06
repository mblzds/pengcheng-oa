<template>
	<view class="page">
		<!-- 自定义顶部 -->
		<view class="header-wrap">
			<view class="status-bar" :style="{ height: statusBarHeight + 'px' }"></view>
			<view class="nav-bar">
				<text class="nav-title">工作台</text>
			</view>
		</view>

		<scroll-view scroll-y class="content" refresher-enabled :refresher-triggered="refreshing"
			@refresherrefresh="onRefresh">
			<view class="update-bar" v-if="showDataUpdated" @tap="refreshFromDataChange">
				<text>数据已更新，点击刷新</text>
			</view>
			<!-- 统计卡片 -->
			<view class="stats-section">
				<view class="stats-card" v-for="(card, idx) in statsCards" :key="idx"
					:style="{ background: card.color }">
					<text class="stats-value">{{ card.value }}</text>
					<text class="stats-label">{{ card.label }}</text>
				</view>
			</view>

			<!-- 九宫格快捷入口 -->
			<view class="grid-section">
				<view class="section-title">快捷入口</view>
				<view class="grid-wrap">
					<view class="grid-item" v-for="(entry, idx) in quickEntries" :key="idx"
						@tap="navigateTo(entry)">
						<view class="grid-icon-wrap" :style="{ background: entry.bgColor }">
							<u-icon :name="entry.icon" color="#FFF" size="22"></u-icon>
							<view class="grid-badge" v-if="entry.badge > 0">
								<text>{{ entry.badge > 99 ? '99+' : entry.badge }}</text>
							</view>
						</view>
						<text class="grid-text">{{ entry.name }}</text>
					</view>
				</view>
			</view>

			<!-- 最近通知 -->
			<view class="notice-section">
				<view class="section-header">
					<text class="section-title">系统通知</text>
					<text class="section-more" @tap="goNoticeList">更多</text>
				</view>
				<view class="notice-list" v-if="notices.length > 0">
					<view class="notice-item" v-for="item in notices" :key="item.id" @tap="onNoticeClick(item)">
						<view class="notice-dot" :class="{ unread: !item.readFlag }"></view>
						<view class="notice-content">
							<text class="notice-title">{{ item.title }}</text>
							<text class="notice-time">{{ item.createTime }}</text>
						</view>
					</view>
				</view>
				<view class="empty-notice" v-else>
					<text class="empty-text">暂无通知</text>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script>
	import { getWorkbench, readNotice } from '../../utils/api.js'
	import { checkLogin } from '../../utils/auth.js'
	import { navigateByNotice } from '../../utils/notice.js'

	export default {
		data() {
			return {
				statusBarHeight: 20,
				refreshing: false,
				showDataUpdated: false,
				statsCards: [],
				pendingApprovalCount: 0,
				notices: [],
				quickEntries: []
			}
		},
		onLoad() {
			const sysInfo = uni.getSystemInfoSync()
			this.statusBarHeight = sysInfo.statusBarHeight || 20
		},
		onShow() {
			if (!checkLogin()) return
			uni.$on('app:data-change', this.onDataChange)
			this.loadData()
		},
		onHide() {
			uni.$off('app:data-change', this.onDataChange)
		},
		methods: {
			onDataChange() {
				this.showDataUpdated = true
			},
			refreshFromDataChange() {
				this.showDataUpdated = false
				this.loadData()
			},
			initQuickEntries() {
				this.quickEntries = [
					{ name: '客户报备', icon: 'edit-pen', bgColor: '#07C160', url: '/pages/customer/report' },
					{ name: '客户列表', icon: 'order', bgColor: '#1890FF', url: '/pages/customer/list' },
					{ name: '到访录入', icon: 'calendar', bgColor: '#722ED1', url: '/pages/customer/detail?action=visit' },
					{ name: '考勤打卡', icon: 'map', bgColor: '#FA8C16', url: '/pages/attendance/clock' },
					{ name: '扫码签到', icon: 'scan', bgColor: '#13C2C2', url: '/pages/attendance/sign' },
					{ name: '付款申请', icon: 'rmb-circle', bgColor: '#F5222D', url: '/pages/apply/list' },
					{ name: '审批中心', icon: 'checkbox-mark', bgColor: '#EB2F96', url: '/pages/approval/list', badge: this.pendingApprovalCount },
					{ name: 'AI助手', icon: 'integral', bgColor: '#2F54EB', url: '/pages/ai/chat' }
				]
			},
			async loadData() {
				try {
					const res = await getWorkbench()
					const data = res.data || {}
					this.statsCards = (data.statsCards || []).map((c, i) => ({
						...c,
						color: ['linear-gradient(135deg,#07C160,#06AD56)', 'linear-gradient(135deg,#1890FF,#096DD9)', 'linear-gradient(135deg,#FA8C16,#D46B08)', 'linear-gradient(135deg,#722ED1,#531DAB)', 'linear-gradient(135deg,#F5222D,#CF1322)'][i % 5]
					}))
					this.pendingApprovalCount = data.pendingApprovalCount || 0
					this.notices = (data.recentNotices || []).slice(0, 5)
					this.initQuickEntries()
				} catch (err) {
					console.error('加载工作台数据失败', err)
					this.initQuickEntries()
				} finally {
					this.refreshing = false
					this.showDataUpdated = false
				}
			},
			onRefresh() {
				this.refreshing = true
				this.loadData()
			},
			navigateTo(entry) {
				uni.navigateTo({ url: entry.url })
			},
			goNoticeList() {
				uni.switchTab({ url: '/pages/index/index' })
			},
			onNoticeClick(item) {
				if (item.id) {
					readNotice(item.id).catch(() => {})
				}
				if (!navigateByNotice(item)) {
					uni.showToast({ title: '暂无可跳转页面', icon: 'none' })
				}
			}
		}
	}
</script>

<style lang="scss" scoped>
	.page { min-height: 100vh; min-height: 100dvh; background: #F0F0F0; }

	.header-wrap {
		background: linear-gradient(160deg, #059B4B 0%, #07C160 45%, #2BD373 100%);
		padding-bottom: 24rpx;
	}
	.status-bar { width: 100%; }
	.nav-bar {
		height: 88rpx; display: flex; align-items: center; justify-content: center;
	}
	.nav-title { font-size: 34rpx; font-weight: 600; color: #FFF; letter-spacing: 2rpx; }

	/* 统计卡片 */
	.stats-section {
		display: flex; flex-wrap: wrap; padding: 20rpx 20rpx 0; gap: 16rpx;
	}
	.stats-card {
		flex: 1; min-width: 200rpx; border-radius: 16rpx; padding: 24rpx 20rpx;
		display: flex; flex-direction: column;
		box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.08);
	}
	.stats-value { font-size: 44rpx; font-weight: 700; color: #FFF; }
	.stats-label { font-size: 22rpx; color: rgba(255,255,255,0.85); margin-top: 4rpx; }

	/* 九宫格 */
	.grid-section {
		margin: 20rpx 20rpx 0; background: #FFF; border-radius: 16rpx;
		padding: 24rpx 16rpx 12rpx;
	}
	.section-title { font-size: 28rpx; font-weight: 600; color: #1A1A1A; padding: 0 8rpx 16rpx; }
	.grid-wrap { display: flex; flex-wrap: wrap; }
	.grid-item {
		width: 25%; display: flex; flex-direction: column; align-items: center;
		padding: 16rpx 0;
	}
	.grid-icon-wrap {
		width: 88rpx; height: 88rpx; border-radius: 20rpx;
		display: flex; align-items: center; justify-content: center;
		position: relative;
	}
	.grid-badge {
		position: absolute; top: -8rpx; right: -8rpx;
		min-width: 32rpx; height: 32rpx; padding: 0 8rpx; border-radius: 32rpx;
		background: #FA5151; border: 3rpx solid #FFF;
		display: flex; align-items: center; justify-content: center;
		text { font-size: 18rpx; color: #FFF; font-weight: 600; line-height: 1; }
	}
	.grid-text { font-size: 22rpx; color: #666; margin-top: 10rpx; }

	/* 通知区域 */
	.notice-section {
		margin: 20rpx 20rpx 0; background: #FFF; border-radius: 16rpx;
		padding: 24rpx; margin-bottom: 40rpx;
	}
	.section-header {
		display: flex; justify-content: space-between; align-items: center;
		margin-bottom: 16rpx;
	}
	.section-more { font-size: 24rpx; color: #07C160; }
	.notice-item {
		display: flex; align-items: flex-start; padding: 16rpx 0;
		border-bottom: 1rpx solid #F5F5F5;
		&:last-child { border-bottom: none; }
	}
	.notice-dot {
		width: 14rpx; height: 14rpx; border-radius: 50%;
		background: #DDD; margin-top: 10rpx; margin-right: 16rpx; flex-shrink: 0;
		&.unread { background: #FA5151; }
	}
	.notice-content { flex: 1; min-width: 0; }
	.notice-title {
		font-size: 26rpx; color: #333;
		overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
		display: block;
	}
	.notice-time { font-size: 22rpx; color: #BBB; margin-top: 4rpx; display: block; }
	.empty-notice { padding: 40rpx 0; text-align: center; }
	.empty-text { font-size: 24rpx; color: #CCC; }

	.content { flex: 1; }
	.update-bar {
		margin: 12rpx 20rpx 0;
		background: #FFF7E6;
		border: 1rpx solid #FFD591;
		border-radius: 10rpx;
		padding: 12rpx 16rpx;
		text-align: center;
		text { font-size: 24rpx; color: #D46B08; }
	}
</style>
