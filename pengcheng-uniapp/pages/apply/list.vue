<template>
	<view class="page">
		<!-- 顶部分类 Tab -->
		<view class="tab-bar">
			<view class="tab-item" :class="{ active: activeType === t.value }"
				v-for="t in typeList" :key="t.value" @tap="switchType(t.value)">
				<text>{{ t.label }}</text>
			</view>
		</view>

		<!-- 状态筛选 -->
		<view class="filter-bar">
			<view class="filter-tag" :class="{ active: statusFilter === s.value }"
				v-for="s in statusList" :key="s.value" @tap="switchStatus(s.value)">
				<text>{{ s.label }}</text>
			</view>
		</view>

		<!-- 列表 -->
		<scroll-view scroll-y class="list-scroll" @scrolltolower="loadMore"
			refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
			<view class="update-bar" v-if="showDataUpdated" @tap="refreshFromDataChange">
				<text>数据已更新，点击刷新</text>
			</view>
			<view class="record-item" v-for="item in list" :key="item.id">
				<view class="record-top">
					<text class="record-type">{{ item.typeName }}</text>
					<view class="record-status" :class="'s-' + item.status">
						<text>{{ getStatusText(item.status) }}</text>
					</view>
				</view>
				<view class="record-info" v-if="item.amount">
					<text class="info-text">金额：¥{{ item.amount }}</text>
				</view>
				<view class="record-info" v-if="item.rangeText">
					<text class="info-text">{{ item.rangeText }}</text>
				</view>
				<view class="record-info">
					<text class="info-text">提交时间：{{ item.createTime || '--' }}</text>
				</view>
			</view>

			<view class="load-tip" v-if="loading"><text>加载中...</text></view>
			<view class="load-tip" v-else-if="noMore && list.length > 0"><text>没有更多了</text></view>
			<view class="empty-state" v-if="!loading && list.length === 0">
				<u-icon name="order" color="#D0D0D0" size="56"></u-icon>
				<text class="empty-text">暂无记录</text>
			</view>
		</scroll-view>

		<!-- 新建申请按钮 -->
		<view class="fab-wrap">
			<view class="fab-btn" @tap="showApplyMenu = !showApplyMenu">
				<u-icon name="plus" color="#FFF" size="24"></u-icon>
			</view>
			<view class="fab-menu" v-if="showApplyMenu">
				<view class="fab-menu-item" @tap="goApply('/pages/apply/leave')">请假</view>
				<view class="fab-menu-item" @tap="goApply('/pages/apply/compensate')">调休</view>
				<view class="fab-menu-item" @tap="goApply('/pages/apply/expense')">报销</view>
				<view class="fab-menu-item" @tap="goApply('/pages/apply/advance')">垫佣</view>
				<view class="fab-menu-item" @tap="goApply('/pages/apply/prepay')">预付佣</view>
			</view>
		</view>
	</view>
</template>

<script>
	import { getLeaveList, getPaymentList } from '../../utils/api.js'

	export default {
		data() {
			return {
				activeType: 'all',
				statusFilter: '',
				typeList: [
					{ label: '全部', value: 'all' },
					{ label: '请假', value: 'leave' },
					{ label: '调休', value: 'compensate' },
					{ label: '报销', value: 'expense' },
					{ label: '垫佣', value: 'advance' },
					{ label: '预付佣', value: 'prepay' }
				],
				statusList: [
					{ label: '全部', value: '' },
					{ label: '审批中', value: 'pending' },
					{ label: '已通过', value: 'approved' },
					{ label: '已驳回', value: 'rejected' }
				],
				list: [],
				page: 1,
				pageSize: 20,
				loading: false,
				noMore: false,
				refreshing: false,
				showApplyMenu: false,
				showDataUpdated: false
			}
		},
		onLoad(options) {
			if (options?.type) {
				const validTypes = this.typeList.map(t => t.value)
				if (validTypes.includes(options.type)) {
					this.activeType = options.type
				}
			}
			if (options?.status) {
				const validStatuses = this.statusList.map(s => s.value)
				if (validStatuses.includes(options.status)) {
					this.statusFilter = options.status
				}
			}
		},
		onShow() {
			uni.$on('app:data-change', this.onDataChange)
			this.resetAndLoad()
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
				this.resetAndLoad()
			},
			getStatusText(status) {
				const map = { 1: '审批中', 2: '审批中', 3: '已通过', 4: '已驳回' }
				return map[status] || status || '--'
			},
			switchType(val) {
				this.activeType = val
				this.resetAndLoad()
			},
			switchStatus(val) {
				this.statusFilter = val
				this.resetAndLoad()
			},
			resetAndLoad() {
				this.page = 1; this.noMore = false; this.list = []; this.loadList()
			},
			async loadList() {
				if (this.loading || this.noMore) return
				this.loading = true
				try {
					const rows = await this.fetchMixedRows()
					if (rows.length < this.pageSize) this.noMore = true
					this.list = this.page === 1 ? rows : [...this.list, ...rows]
					this.page++
				} catch (err) { console.error(err) }
				finally { this.loading = false; this.refreshing = false; this.showDataUpdated = false }
			},
			async fetchMixedRows() {
				const type = this.activeType
				const rows = []
				if (type === 'all' || type === 'leave' || type === 'compensate') {
					const leaveType = type === 'all' ? undefined : type
					const leaveStatusMap = { pending: 1, approved: 2, rejected: 3 }
					const leaveStatus = this.statusFilter ? leaveStatusMap[this.statusFilter] : undefined
					const leaveRes = await getLeaveList({
						type: leaveType,
						status: leaveStatus,
						page: this.page,
						pageSize: this.pageSize
					}).catch(() => ({ data: { list: [] } }))
					const leaveRows = leaveRes.data?.list || []
					leaveRows.forEach(r => {
						const typeName = r.type === 'compensate' ? '调休申请' : '请假申请'
						const start = r.startTime ? String(r.startTime).replace('T', ' ').slice(0, 16) : ''
						const end = r.endTime ? String(r.endTime).replace('T', ' ').slice(0, 16) : ''
						rows.push({
							id: `l-${r.type}-${r.id}`,
							typeName,
							status: r.status === 2 ? 3 : (r.status === 3 ? 4 : 1),
							amount: null,
							rangeText: end ? `${start} ~ ${end}` : start,
							createTime: r.createTime
						})
					})
				}

				if (type === 'all' || type === 'expense' || type === 'advance' || type === 'prepay') {
					const paymentTypeMap = { expense: 1, advance: 2, prepay: 3 }
					const requestType = type === 'all' ? undefined : paymentTypeMap[type]
					const statusList = this.statusFilter === 'pending'
						? [1, 2]
						: (this.statusFilter === 'approved' ? [3] : (this.statusFilter === 'rejected' ? [4] : [undefined]))
					for (const payStatus of statusList) {
						const payRes = await getPaymentList({
							type: requestType,
							status: payStatus,
							page: this.page,
							pageSize: this.pageSize
						}).catch(() => ({ data: { list: [] } }))
						const payRows = payRes.data?.list || []
						payRows.forEach(r => {
							const typeNameMap = { 1: '报销申请', 2: '垫佣申请', 3: '预付佣申请' }
							rows.push({
								id: `p-${r.id}-${r.status}`,
								typeName: typeNameMap[r.requestType] || '付款申请',
								status: r.status,
								amount: r.amount,
								rangeText: '',
								createTime: r.createTime
							})
						})
					}
				}

				rows.sort((a, b) => String(b.createTime || '').localeCompare(String(a.createTime || '')))
				return rows.slice(0, this.pageSize)
			},
			loadMore() { this.loadList() },
			onRefresh() { this.refreshing = true; this.resetAndLoad() },
			goApply(url) {
				this.showApplyMenu = false
				uni.navigateTo({ url })
			}
		}
	}
</script>

<style lang="scss" scoped>
	.page { min-height: 100vh; min-height: 100dvh; background: #F0F0F0; display: flex; flex-direction: column; }
	.tab-bar {
		display: flex; background: #FFF; padding: 0 8rpx; border-bottom: 1rpx solid #F0F0F0;
		overflow-x: auto; white-space: nowrap;
	}
	.tab-item {
		padding: 20rpx 20rpx; font-size: 26rpx; color: #666; position: relative; flex-shrink: 0;
		&.active { color: #07C160; font-weight: 600;
			&::after { content: ''; position: absolute; bottom: 0; left: 20%; right: 20%; height: 4rpx; background: #07C160; border-radius: 2rpx; }
		}
	}
	.filter-bar {
		display: flex; padding: 16rpx 20rpx; gap: 16rpx; background: #FFF;
	}
	.filter-tag {
		padding: 8rpx 24rpx; border-radius: 24rpx; background: #F5F5F5;
		font-size: 24rpx; color: #666;
		&.active { background: rgba(7,193,96,0.1); color: #07C160; }
	}
	.list-scroll { flex: 1; }
	.update-bar {
		margin: 12rpx 20rpx 0;
		background: #FFF7E6;
		border: 1rpx solid #FFD591;
		border-radius: 10rpx;
		padding: 12rpx 16rpx;
		text-align: center;
		text { font-size: 24rpx; color: #D46B08; }
	}
	.record-item {
		margin: 16rpx 20rpx 0; background: #FFF; border-radius: 12rpx; padding: 24rpx;
	}
	.record-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12rpx; }
	.record-type { font-size: 28rpx; font-weight: 600; color: #1A1A1A; }
	.record-status {
		padding: 4rpx 16rpx; border-radius: 20rpx; font-size: 20rpx;
		&.s-pending, &.s-1, &.s-2 { background: #FFF7E6; color: #FA8C16; }
		&.s-approved, &.s-3 { background: #F6FFED; color: #52C41A; }
		&.s-rejected, &.s-4 { background: #FFF1F0; color: #F5222D; }
	}
	.record-info { margin-top: 8rpx; }
	.info-text { font-size: 24rpx; color: #999; }
	.load-tip { text-align: center; padding: 24rpx; text { font-size: 24rpx; color: #CCC; } }
	.empty-state {
		display: flex; flex-direction: column; align-items: center; padding: 140rpx 0;
		.empty-text { font-size: 28rpx; color: #999; margin-top: 20rpx; }
	}
	.fab-wrap { position: fixed; right: 32rpx; bottom: 140rpx; }
	.fab-btn {
		width: 96rpx; height: 96rpx; border-radius: 50%;
		background: #07C160; display: flex; align-items: center; justify-content: center;
		box-shadow: 0 8rpx 24rpx rgba(7,193,96,0.4);
	}
	.fab-menu {
		position: absolute; bottom: 110rpx; right: 0;
		background: #FFF; border-radius: 12rpx; padding: 8rpx 0;
		box-shadow: 0 4rpx 20rpx rgba(0,0,0,0.12); min-width: 180rpx;
	}
	.fab-menu-item {
		padding: 20rpx 32rpx; font-size: 26rpx; color: #333;
		&:active { background: #F5F5F5; }
	}
</style>
