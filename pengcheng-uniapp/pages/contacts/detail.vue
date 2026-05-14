<template>
	<view class="page">
		<view class="header">
			<image v-if="avatar" class="avatar" :src="resolveAvatar(avatar)" mode="aspectFill"></image>
			<view v-else class="avatar avatar-text" :style="{ background: avatarColor }">
				<text>{{ firstChar }}</text>
			</view>
			<view class="name-block">
				<text class="name">{{ displayName }}</text>
				<text v-if="username && username !== displayName" class="sub-name">用户名：{{ username }}</text>
			</view>
		</view>

		<view class="info-card" v-if="hasInfoRows">
			<view class="info-row" v-if="employeeNo">
				<text class="info-label">工号</text>
				<text class="info-value">{{ employeeNo }}</text>
			</view>
			<view class="info-row" v-if="phone" @tap="callPhone">
				<text class="info-label">手机号</text>
				<view class="info-value-with-arrow">
					<text class="info-value link">{{ phone }}</text>
					<pc-icon name="arrow-right" color="#CCC" size="14"></pc-icon>
				</view>
			</view>
		</view>

		<view class="bottom-bar safe-area-bottom">
			<button class="send-btn" @tap="sendMessage">发送消息</button>
		</view>
	</view>
</template>

<script>
	import { resolveAvatar } from '../../utils/config.js'

	const AVATAR_COLORS = ['#25B7D3', '#F56C6C', '#E6A23C', '#67C23A', '#409EFF', '#9B59B6', '#1ABC9C', '#E74C3C', '#3498DB', '#2ECC71']

	export default {
		data() {
			return {
				id: '',
				nickname: '',
				username: '',
				avatar: '',
				employeeNo: '',
				phone: ''
			}
		},
		computed: {
			displayName() {
				return this.nickname || this.username || '联系人'
			},
			firstChar() {
				const n = this.displayName
				return n ? n.charAt(0).toUpperCase() : '?'
			},
			avatarColor() {
				const n = this.displayName
				if (!n) return AVATAR_COLORS[0]
				let hash = 0
				for (let i = 0; i < n.length; i++) hash = n.charCodeAt(i) + ((hash << 5) - hash)
				return AVATAR_COLORS[Math.abs(hash) % AVATAR_COLORS.length]
			},
			hasInfoRows() {
				return Boolean(this.employeeNo || this.phone)
			}
		},
		onLoad(options) {
			this.id = options?.id || ''
			this.nickname = decodeURIComponent(options?.nickname || '')
			this.username = decodeURIComponent(options?.username || '')
			this.avatar = decodeURIComponent(options?.avatar || '')
			this.employeeNo = decodeURIComponent(options?.employeeNo || '')
			this.phone = decodeURIComponent(options?.phone || '')
			uni.setNavigationBarTitle({ title: '详细资料' })
		},
		methods: {
			resolveAvatar,
			callPhone() {
				if (!this.phone) return
				uni.makePhoneCall({ phoneNumber: String(this.phone) })
			},
			sendMessage() {
				if (!this.id) return
				// 用 redirectTo 替换当前详情页，从聊天页返回时直接回通讯录，
				// 与微信"详细资料 → 发送消息 → 聊天页 → 返回通讯录"的栈行为一致
				uni.redirectTo({
					url: `/pages/chat/index?targetId=${this.id}&name=${encodeURIComponent(this.displayName)}&avatar=${encodeURIComponent(this.avatar || '')}`
				})
			}
		}
	}
</script>

<style lang="scss" scoped>
	.page {
		min-height: 100vh; min-height: 100dvh;
		background: #EDEDED;
		padding-bottom: 160rpx;
	}

	.header {
		display: flex; align-items: center;
		padding: 40rpx 32rpx;
		background: #FFF;
		border-bottom: 1rpx solid #F0F0F0;
	}
	.avatar {
		width: 128rpx; height: 128rpx; border-radius: 12rpx; background: #E0E0E0; flex-shrink: 0;
	}
	.avatar-text {
		display: flex; align-items: center; justify-content: center;
		text { font-size: 56rpx; color: #FFF; font-weight: 600; }
	}
	.name-block { margin-left: 28rpx; flex: 1; min-width: 0; }
	.name { font-size: 36rpx; color: #1A1A1A; font-weight: 600; display: block; }
	.sub-name { font-size: 24rpx; color: #999; display: block; margin-top: 12rpx; }

	.info-card {
		margin-top: 16rpx;
		background: #FFF;
	}
	.info-row {
		display: flex; align-items: center; justify-content: space-between;
		padding: 28rpx 32rpx;
		border-bottom: 1rpx solid #F0F0F0;
		&:last-child { border-bottom: none; }
		&:active { background: #ECECEC; }
	}
	.info-label { font-size: 28rpx; color: #1A1A1A; flex-shrink: 0; margin-right: 16rpx; }
	.info-value { font-size: 28rpx; color: #555; text-align: right; white-space: nowrap; }
	.info-value-with-arrow { display: flex; align-items: center; gap: 8rpx; flex-shrink: 0; }
	.info-value.link { color: #07C160; }

	.bottom-bar {
		position: fixed; left: 0; right: 0; bottom: 0;
		padding: 20rpx 28rpx calc(20rpx + env(safe-area-inset-bottom));
		background: #FFF;
		border-top: 1rpx solid #F0F0F0;
	}
	.send-btn {
		height: 84rpx; line-height: 84rpx;
		background: linear-gradient(135deg, #059B4B, #07C160);
		color: #FFF;
		font-size: 30rpx;
		font-weight: 600;
		border-radius: 12rpx;
		border: none;
		&::after { border: none; }
		&:active { opacity: 0.9; }
	}
</style>
