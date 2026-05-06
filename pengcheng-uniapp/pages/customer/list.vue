<template>
	<view class="page">
		<!-- 筛选栏 -->
		<view class="filter-bar">
			<input class="filter-input" v-model="filter.surname" placeholder="搜索客户姓氏" @confirm="onSearch" />
			<picker :range="projectList" range-key="projectName" @change="onProjectChange">
				<view class="filter-picker">
					<text>{{ selectedProjectLabel }}</text>
					<u-icon name="arrow-down" color="#999" size="12"></u-icon>
				</view>
			</picker>
			<picker :range="statusList" range-key="label" @change="onStatusChange">
				<view class="filter-picker">
					<text>{{ selectedStatusLabel }}</text>
					<u-icon name="arrow-down" color="#999" size="12"></u-icon>
				</view>
			</picker>
		</view>

		<!-- 列表 -->
		<scroll-view scroll-y class="list-scroll" @scrolltolower="loadMore"
			refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
			<view class="update-bar" v-if="showDataUpdated" @tap="refreshFromDataChange">
				<text>数据已更新，点击刷新</text>
			</view>
			<view class="customer-item" v-for="item in list" :key="item.id" @tap="goDetail(item)">
				<view class="item-top">
					<text class="item-name">{{ item.customerName }}{{ item.phoneMasked ? ' (' + item.phoneMasked + ')' : '' }}</text>
					<view class="item-status" :class="'status-' + item.status">
						<text>{{ getStatusText(item.status) }}</text>
					</view>
				</view>
				<view class="item-info">
					<text class="info-text">报备编号：{{ item.reportNo || '--' }}</text>
				</view>
				<view class="item-info">
					<text class="info-text">报备时间：{{ item.createTime || '--' }}</text>
				</view>
			</view>

			<view class="load-tip" v-if="loading"><text>加载中...</text></view>
			<view class="load-tip" v-else-if="noMore && list.length > 0"><text>没有更多了</text></view>
			<view class="empty-state" v-if="!loading && list.length === 0">
				<u-icon name="order" color="#D0D0D0" size="56"></u-icon>
				<text class="empty-text">暂无客户</text>
			</view>
		</scroll-view>

		<!-- 右下角报备按钮 -->
		<view class="fab-btn" @tap="goReport">
			<u-icon name="plus" color="#FFF" size="24"></u-icon>
		</view>
	</view>
</template>

<script>
	import { getCustomerList, searchCustomerProjects } from '../../utils/api.js'

	export default {
		data() {
			return {
				filter: { surname: '', status: '', projectId: '' },
				statusList: [
					{ label: '全部状态', value: '' },
					{ label: '已报备', value: 1 },
					{ label: '已到访', value: 2 },
					{ label: '已成交', value: 3 }
				],
				projectList: [{ id: '', projectName: '全部项目' }],
				selectedStatusLabel: '全部状态',
				selectedProjectLabel: '全部项目',
				list: [],
				page: 1,
				pageSize: 20,
				loading: false,
				noMore: false,
				refreshing: false,
				showDataUpdated: false
			}
		},
		onShow() {
			uni.$on('app:data-change', this.onDataChange)
			this.resetAndLoad()
		},
		onHide() {
			uni.$off('app:data-change', this.onDataChange)
		},
		onLoad() {
			this.loadProjects()
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
				const map = { 1: '已报备', 2: '已到访', 3: '已成交' }
				return map[status] || status || '--'
			},
			async loadProjects() {
				try {
					const res = await searchCustomerProjects('')
					const projects = res.data || []
					this.projectList = [{ id: '', projectName: '全部项目' }, ...projects]
				} catch (e) {
					this.projectList = [{ id: '', projectName: '全部项目' }]
				}
			},
			onProjectChange(e) {
				const item = this.projectList[e.detail.value]
				if (!item) return
				this.filter.projectId = item.id
				this.selectedProjectLabel = item.projectName
				this.resetAndLoad()
			},
			onStatusChange(e) {
				const item = this.statusList[e.detail.value]
				this.filter.status = item.value
				this.selectedStatusLabel = item.label
				this.resetAndLoad()
			},
			onSearch() {
				this.resetAndLoad()
			},
			resetAndLoad() {
				this.page = 1
				this.noMore = false
				this.list = []
				this.loadList()
			},
			async loadList() {
				if (this.loading || this.noMore) return
				this.loading = true
				try {
					const res = await getCustomerList({
						surname: this.filter.surname || undefined,
						projectId: this.filter.projectId || undefined,
						status: this.filter.status || undefined,
						page: this.page,
						pageSize: this.pageSize
					})
					const rows = res.data?.list || res.data || []
					if (rows.length < this.pageSize) this.noMore = true
					this.list = this.page === 1 ? rows : [...this.list, ...rows]
					this.page++
				} catch (err) {
					console.error(err)
				} finally {
					this.loading = false
					this.refreshing = false
					this.showDataUpdated = false
				}
			},
			loadMore() { this.loadList() },
			onRefresh() { this.refreshing = true; this.resetAndLoad() },
			goDetail(item) {
				uni.navigateTo({ url: `/pages/customer/detail?id=${item.id}` })
			},
			goReport() {
				uni.navigateTo({ url: '/pages/customer/report' })
			}
		}
	}
</script>

<style lang="scss" scoped>
	.page { min-height: 100vh; min-height: 100dvh; background: #F0F0F0; display: flex; flex-direction: column; }
	.filter-bar {
		display: flex; padding: 16rpx 20rpx; background: #FFF; gap: 16rpx;
		border-bottom: 1rpx solid #F0F0F0;
	}
	.filter-input {
		flex: 1; height: 64rpx; background: #F5F5F5; border-radius: 8rpx;
		padding: 0 16rpx; font-size: 26rpx;
	}
	.filter-picker {
		display: flex; align-items: center; height: 64rpx; padding: 0 16rpx;
		background: #F5F5F5; border-radius: 8rpx; gap: 8rpx;
		text { font-size: 24rpx; color: #666; white-space: nowrap; }
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
	.customer-item {
		margin: 16rpx 20rpx 0; background: #FFF; border-radius: 12rpx; padding: 24rpx;
		&:active { background: #FAFAFA; }
	}
	.item-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12rpx; }
	.item-name { font-size: 30rpx; font-weight: 600; color: #1A1A1A; }
	.item-status {
		padding: 4rpx 16rpx; border-radius: 20rpx; font-size: 20rpx;
		&.status-reported { background: #E6F7FF; color: #1890FF; }
		&.status-visited { background: #F6FFED; color: #52C41A; }
		&.status-dealt { background: #FFF7E6; color: #FA8C16; }
		&.status-expired { background: #F5F5F5; color: #999; }
	}
	.item-info { margin-top: 8rpx; }
	.info-text { font-size: 24rpx; color: #999; }
	.load-tip { text-align: center; padding: 24rpx; text { font-size: 24rpx; color: #CCC; } }
	.empty-state {
		display: flex; flex-direction: column; align-items: center; padding: 140rpx 0;
		.empty-text { font-size: 28rpx; color: #999; margin-top: 20rpx; }
	}
	.fab-btn {
		position: fixed; right: 32rpx; bottom: 140rpx;
		width: 96rpx; height: 96rpx; border-radius: 50%;
		background: #07C160; display: flex; align-items: center; justify-content: center;
		box-shadow: 0 8rpx 24rpx rgba(7,193,96,0.4);
	}
</style>
