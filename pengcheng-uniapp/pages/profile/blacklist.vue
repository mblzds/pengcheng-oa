<template>
	<view class="page">
		<scroll-view scroll-y class="list-wrap">
			<view class="item" v-for="item in blacklist" :key="item.id" @tap="openChat(item)">
				<image class="avatar" :src="item.blockedUserAvatar || '/static/default-avatar.png'" mode="aspectFill"></image>
				<view class="info">
					<text class="name">{{ item.blockedUserName || `用户${item.blockedUserId}` }}</text>
					<text class="time">{{ formatTime(item.createTime) }}</text>
				</view>
				<button class="unblock-btn" @tap.stop="handleUnblock(item)">解除</button>
			</view>

			<view class="empty" v-if="!loading && blacklist.length === 0">
				<u-icon name="minus-circle" color="#CFCFCF" size="56"></u-icon>
				<text class="empty-text">暂无黑名单用户</text>
			</view>
		</scroll-view>
	</view>
</template>

<script>
import { getBlacklist, unblockUser } from '../../utils/api.js'

export default {
	data() {
		return {
			blacklist: [],
			loading: false
		}
	},
	onShow() {
		this.loadBlacklist()
	},
	methods: {
		async loadBlacklist() {
			this.loading = true
			try {
				const res = await getBlacklist()
				this.blacklist = Array.isArray(res.data) ? res.data : []
			} catch (e) {
				uni.showToast({ title: '加载失败', icon: 'none' })
			} finally {
				this.loading = false
			}
		},
		handleUnblock(item) {
			uni.showModal({
				title: '解除黑名单',
				content: `确认解除 ${item.blockedUserName || `用户${item.blockedUserId}`} 吗？`,
				success: async (res) => {
					if (!res.confirm) return
					try {
						await unblockUser(item.blockedUserId)
						uni.showToast({ title: '已解除', icon: 'success' })
						this.blacklist = this.blacklist.filter(user => user.id !== item.id)
					} catch (e) {
						uni.showToast({ title: '操作失败', icon: 'none' })
					}
				}
			})
		},
		openChat(item) {
			if (!item.blockedUserId) return
			uni.navigateTo({
				url: `/pages/chat/index?targetId=${item.blockedUserId}&name=${encodeURIComponent(item.blockedUserName || '聊天')}&avatar=${encodeURIComponent(item.blockedUserAvatar || '')}`
			})
		},
		formatTime(value) {
			if (!value) return ''
			try {
				const date = new Date(value.replace(' ', 'T'))
				if (Number.isNaN(date.getTime())) return value
				const y = date.getFullYear()
				const m = String(date.getMonth() + 1).padStart(2, '0')
				const d = String(date.getDate()).padStart(2, '0')
				const hh = String(date.getHours()).padStart(2, '0')
				const mm = String(date.getMinutes()).padStart(2, '0')
				return `拉黑于 ${y}-${m}-${d} ${hh}:${mm}`
			} catch (e) {
				return value
			}
		}
	}
}
</script>

<style lang="scss" scoped>
.page {
	min-height: 100vh;
	min-height: 100dvh;
	background: #F0F0F0;
}

.list-wrap {
	height: 100%;
}

.item {
	display: flex;
	align-items: center;
	padding: 22rpx 24rpx;
	background: #FFF;
	border-bottom: 1rpx solid #F2F2F2;
}

.avatar {
	width: 72rpx;
	height: 72rpx;
	border-radius: 8rpx;
	background: #E0E0E0;
	margin-right: 16rpx;
	flex-shrink: 0;
}

.info {
	flex: 1;
	min-width: 0;
}

.name {
	display: block;
	font-size: 28rpx;
	color: #1A1A1A;
}

.time {
	display: block;
	margin-top: 4rpx;
	font-size: 22rpx;
	color: #999;
}

.unblock-btn {
	height: 56rpx;
	line-height: 56rpx;
	padding: 0 22rpx;
	background: #FFF5F5;
	color: #FA5151;
	border: 1rpx solid #FFD6D6;
	border-radius: 28rpx;
	font-size: 24rpx;
}

.unblock-btn::after {
	border: none;
}

.empty {
	padding: 180rpx 40rpx 0;
	display: flex;
	flex-direction: column;
	align-items: center;
}

.empty-text {
	margin-top: 14rpx;
	font-size: 26rpx;
	color: #999;
}
</style>

