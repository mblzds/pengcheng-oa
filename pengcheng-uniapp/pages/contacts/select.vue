<template>
	<view class="page">
		<view class="header">
			<view class="search-wrap">
				<u-icon name="search" color="#999" size="28"></u-icon>
				<input class="search-input" v-model="searchText" placeholder="搜索姓名 / 工号 / 手机号"
					placeholder-class="placeholder" />
			</view>
			<text class="selected-count">已选 {{ selectedIds.length }} 人</text>
		</view>

		<scroll-view scroll-y class="user-list">
			<view class="user-item" v-for="user in filteredUsers" :key="user.id" @tap="toggleSelect(user)">
				<view class="check-box" :class="{ checked: isSelected(user.id) }">
					<u-icon v-if="isSelected(user.id)" name="checkmark" color="#FFFFFF" size="22"></u-icon>
				</view>
				<image v-if="user.avatar" class="user-avatar" :src="resolveAvatar(user.avatar)" mode="aspectFill"></image>
				<view v-else class="user-avatar avatar-text" :style="{ background: avatarColor(user.nickname || user.username) }">
					<text>{{ firstChar(user.nickname || user.username) }}</text>
				</view>
				<view class="user-info">
					<text class="user-name">{{ user.nickname || user.username }}</text>
					<text v-if="user.employeeNo" class="user-sub">工号 {{ user.employeeNo }}</text>
				</view>
			</view>
			<view class="empty-state" v-if="filteredUsers.length === 0 && !loading">
				<text class="empty-text">{{ searchText.trim() ? '没有匹配的联系人' : '暂无可选联系人' }}</text>
			</view>
		</scroll-view>

		<view class="bottom-bar">
			<button class="confirm-btn" :class="{ 'btn-disabled': selectedIds.length === 0 }"
				:disabled="selectedIds.length === 0" @tap="handleConfirm">
				确认{{ selectedIds.length > 0 ? ` (${selectedIds.length})` : '' }}
			</button>
		</view>
	</view>
</template>

<script>
	import { getChatUsers } from '../../utils/api.js'
	import { resolveAvatar } from '../../utils/config.js'

	const AVATAR_COLORS = ['#25B7D3', '#F56C6C', '#E6A23C', '#67C23A', '#409EFF', '#9B59B6', '#1ABC9C', '#E74C3C', '#3498DB', '#2ECC71']

	function matchAny(kw, ...fields) {
		for (const f of fields) {
			if (f != null && String(f).toLowerCase().includes(kw)) return true
		}
		return false
	}

	export default {
		data() {
			return {
				users: [],
				selectedIds: [],
				excludeIds: [],
				searchText: '',
				loading: false
			}
		},
		computed: {
			filteredUsers() {
				const kw = (this.searchText || '').trim().toLowerCase()
				if (!kw) return this.users
				return this.users.filter(u => matchAny(kw, u.nickname, u.username, u.employeeNo, u.phone))
			}
		},
		onLoad(options) {
			const excludeRaw = decodeURIComponent(options?.excludeUserIds || '')
			this.excludeIds = excludeRaw
				.split(',')
				.map(item => Number(item))
				.filter(id => Number.isFinite(id) && id > 0)
			const title = decodeURIComponent(options?.title || '选择成员')
			uni.setNavigationBarTitle({ title })
			this.loadUsers()
		},
		methods: {
			resolveAvatar,
			firstChar(name) {
				if (!name) return '?'
				return String(name).charAt(0).toUpperCase()
			},
			avatarColor(name) {
				if (!name) return AVATAR_COLORS[0]
				let hash = 0
				for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash)
				return AVATAR_COLORS[Math.abs(hash) % AVATAR_COLORS.length]
			},
			async loadUsers() {
				this.loading = true
				try {
					const res = await getChatUsers()
					const data = (res.data || []).filter(u => !this.excludeIds.includes(Number(u.id)))
					this.users = data
				} catch (e) {
					console.error('加载联系人失败:', e)
					uni.showToast({ title: '加载联系人失败', icon: 'none' })
				} finally {
					this.loading = false
				}
			},
			isSelected(id) {
				return this.selectedIds.includes(Number(id))
			},
			toggleSelect(user) {
				const id = Number(user.id)
				if (!id) return
				const idx = this.selectedIds.indexOf(id)
				if (idx > -1) this.selectedIds.splice(idx, 1)
				else this.selectedIds.push(id)
			},
			handleConfirm() {
				if (this.selectedIds.length === 0) return
				const selectedUsers = this.users.filter(u => this.selectedIds.includes(Number(u.id)))
				const channel = this.getOpenerEventChannel && this.getOpenerEventChannel()
				if (channel) channel.emit('selectedUsers', selectedUsers)
				uni.navigateBack()
			}
		}
	}
</script>

<style lang="scss" scoped>
	.page {
		min-height: 100vh; min-height: 100dvh; height: 100vh; height: 100dvh;
		display: flex; flex-direction: column; background: #EDEDED;
	}
	.header {
		padding: 16rpx 24rpx 8rpx;
		background: #FFF;
		border-bottom: 1rpx solid #F0F0F0;
	}
	.search-wrap {
		display: flex; align-items: center;
		height: 64rpx;
		background: #F5F5F5; border-radius: 8rpx; padding: 0 16rpx;
	}
	.search-input { flex: 1; height: 64rpx; font-size: 26rpx; color: #1A1A1A; margin-left: 8rpx; }
	.placeholder { color: #B0B0B0; font-size: 26rpx; }
	.selected-count { font-size: 22rpx; color: #07C160; padding: 12rpx 4rpx 0; display: block; }

	.user-list { flex: 1; overflow: hidden; }
	.user-item {
		display: flex; align-items: center; padding: 20rpx 28rpx;
		background: #FFF; border-bottom: 1rpx solid #F0F0F0;
		&:active { background: #F0F0F0; }
	}
	.check-box {
		width: 40rpx; height: 40rpx; border-radius: 50%; border: 3rpx solid #CCC;
		display: flex; align-items: center; justify-content: center;
		margin-right: 20rpx; flex-shrink: 0;
		&.checked { background: #07C160; border-color: #07C160; }
	}
	.user-avatar { width: 76rpx; height: 76rpx; border-radius: 8rpx; margin-right: 18rpx; background: #E0E0E0; flex-shrink: 0; }
	.avatar-text {
		display: flex; align-items: center; justify-content: center;
		text { font-size: 32rpx; color: #FFF; font-weight: 600; }
	}
	.user-info { flex: 1; min-width: 0; }
	.user-name { font-size: 28rpx; color: #1A1A1A; display: block; }
	.user-sub { font-size: 22rpx; color: #999; display: block; margin-top: 4rpx; }

	.empty-state { padding: 120rpx 40rpx; text-align: center; }
	.empty-text { font-size: 26rpx; color: #999; }

	.bottom-bar {
		padding: 20rpx 28rpx calc(20rpx + env(safe-area-inset-bottom));
		background: #FFF; border-top: 1rpx solid #F0F0F0;
	}
	.confirm-btn {
		height: 84rpx; line-height: 84rpx;
		background: linear-gradient(135deg, #059B4B, #07C160);
		color: #FFF; font-size: 30rpx; font-weight: 600; border-radius: 12rpx; border: none;
		&::after { border: none; }
		&:active { opacity: 0.9; }
	}
	.btn-disabled { background: #C0C0C0 !important; }
</style>
