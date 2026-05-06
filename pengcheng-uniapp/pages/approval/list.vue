<template>
	<view class="page">
		<scroll-view scroll-y class="content" refresher-enabled :refresher-triggered="refreshing"
			@refresherrefresh="onRefresh">
			<view class="update-bar" v-if="showDataUpdated" @tap="refreshFromDataChange">
				<text>数据已更新，点击刷新</text>
			</view>
			<!-- 分类统计 -->
			<view class="type-grid" v-if="typeCounts.length > 0">
				<view class="type-item" v-for="t in typeCounts" :key="t.type"
					:class="{ active: activeType === t.type }" @tap="switchType(t.type)">
					<text class="type-count">{{ t.count }}</text>
					<text class="type-name">{{ t.name }}</text>
				</view>
			</view>

			<!-- 待审批列表 -->
			<view class="section-title">
				<text>{{ activeType === 'all' ? '全部待审批' : activeTypeName }}</text>
			</view>
			<view class="approval-item" v-for="item in filteredList" :key="item.id + '-' + item.type"
				@tap="goDetail(item)">
				<view class="item-left">
					<view class="item-avatar" :style="{ background: getTypeColor(item.type) }">
						<text>{{ getTypeIcon(item.type) }}</text>
					</view>
				</view>
				<view class="item-center">
					<text class="item-title">{{ item.summary || item.typeName }}</text>
					<text class="item-desc">{{ item.applicantName }} · {{ formatDateTime(item.applyTime) }}</text>
				</view>
				<view class="item-right">
					<u-icon name="arrow-right" color="#CCC" size="14"></u-icon>
				</view>
			</view>

			<view class="empty-state" v-if="!loading && filteredList.length === 0">
				<u-icon name="checkbox-mark" color="#D0D0D0" size="56"></u-icon>
				<text class="empty-text">暂无待审批事项</text>
			</view>
		</scroll-view>
	</view>
</template>

<script>
	import { getApprovalPending } from '../../utils/api.js'

	export default {
		data() {
			return {
				allList: [],
				activeType: 'all',
				loading: false,
				refreshing: false,
				showDataUpdated: false
			}
		},
		computed: {
			typeCounts() {
				const map = {}
				this.allList.forEach(item => {
					const t = item.type || 'other'
					if (!map[t]) map[t] = { type: t, name: this.getTypeName(t), count: 0 }
					map[t].count++
				})
				const all = { type: 'all', name: '全部', count: this.allList.length }
				return [all, ...Object.values(map)]
			},
			activeTypeName() {
				const found = this.typeCounts.find(t => t.type === this.activeType)
				return found ? found.name : '全部'
			},
			filteredList() {
				if (this.activeType === 'all') return this.allList
				return this.allList.filter(i => i.type === this.activeType)
			}
		},
		onShow() {
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
			getTypeName(type) {
				const map = { leave: '请假', compensate: '调休', expense: '报销', advance: '垫佣', prepay: '预付佣', commission: '佣金审核' }
				return map[type] || type
			},
			formatDateTime(value) {
				if (!value) return '--'
				return String(value).replace('T', ' ').slice(0, 16)
			},
			getTypeColor(type) {
				const map = { leave: '#1890FF', compensate: '#13C2C2', expense: '#FA8C16', advance: '#722ED1', prepay: '#EB2F96', commission: '#F5222D' }
				return map[type] || '#999'
			},
			getTypeIcon(type) {
				const map = { leave: '假', compensate: '调', expense: '报', advance: '垫', prepay: '预', commission: '佣' }
				return map[type] || '审'
			},
			switchType(type) { this.activeType = type },
			async loadData() {
				this.loading = true
				try {
					const res = await getApprovalPending()
					const data = res.data || {}
					this.allList = [
						...(data.leaveItems || []),
						...(data.paymentItems || []),
						...(data.commissionItems || [])
					].sort((a, b) => String(b.applyTime || '').localeCompare(String(a.applyTime || '')))
				} catch (err) { console.error(err) }
				finally { this.loading = false; this.refreshing = false; this.showDataUpdated = false }
			},
			onRefresh() { this.refreshing = true; this.loadData() },
			goDetail(item) {
				uni.navigateTo({ url: `/pages/approval/detail?id=${item.id}&type=${item.type}` })
			}
		}
	}
</script>

<style lang="scss" scoped>
	.page { min-height: 100vh; min-height: 100dvh; background: #F0F0F0; }
	.content { padding-bottom: 40rpx; }
	.update-bar {
		margin: 12rpx 20rpx 0;
		background: #FFF7E6;
		border: 1rpx solid #FFD591;
		border-radius: 10rpx;
		padding: 12rpx 16rpx;
		text-align: center;
		text { font-size: 24rpx; color: #D46B08; }
	}
	.type-grid {
		display: flex; flex-wrap: wrap; margin: 20rpx 20rpx 0;
		background: #FFF; border-radius: 16rpx; padding: 16rpx;
	}
	.type-item {
		width: 25%; text-align: center; padding: 16rpx 0; border-radius: 12rpx;
		&.active { background: rgba(7,193,96,0.08); }
	}
	.type-count { font-size: 36rpx; font-weight: 700; color: #1A1A1A; display: block; }
	.type-name { font-size: 22rpx; color: #999; margin-top: 4rpx; display: block; }
	.section-title {
		padding: 24rpx 20rpx 12rpx; font-size: 26rpx; font-weight: 600; color: #666;
	}
	.approval-item {
		display: flex; align-items: center; margin: 0 20rpx 16rpx;
		background: #FFF; border-radius: 12rpx; padding: 24rpx;
		&:active { background: #FAFAFA; }
	}
	.item-left { margin-right: 20rpx; }
	.item-avatar {
		width: 72rpx; height: 72rpx; border-radius: 16rpx;
		display: flex; align-items: center; justify-content: center;
		text { font-size: 28rpx; color: #FFF; font-weight: 600; }
	}
	.item-center { flex: 1; min-width: 0; }
	.item-title { font-size: 28rpx; color: #1A1A1A; font-weight: 500; display: block;
		overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
	.item-desc { font-size: 22rpx; color: #BBB; margin-top: 6rpx; display: block; }
	.item-right { flex-shrink: 0; margin-left: 12rpx; }
	.empty-state {
		display: flex; flex-direction: column; align-items: center; padding: 140rpx 0;
		.empty-text { font-size: 28rpx; color: #999; margin-top: 20rpx; }
	}
</style>
