<template>
	<view class="page">
		<scroll-view scroll-y class="list-scroll" @scrolltolower="loadMore"
			refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
			<view class="deal-item" v-for="item in list" :key="item.dealId" @tap="goCustomerDetail(item)">
				<view class="item-top">
					<text class="project-name">{{ item.projectName || '未关联项目' }}</text>
					<text class="deal-amount">¥ {{ formatAmount(item.dealAmount) }}</text>
				</view>

				<view class="item-row">
					<text class="row-label">客户</text>
					<text class="row-value">{{ item.customerName || '--' }}</text>
					<text class="row-phone" v-if="item.phoneMasked">{{ item.phoneMasked }}</text>
				</view>

				<view class="item-row">
					<text class="row-label">中介</text>
					<text class="row-value">{{ item.allianceName || '直客' }}</text>
					<text class="row-extra" v-if="item.agentName">
						· {{ item.agentName }}{{ item.agentPhone ? ' / ' + item.agentPhone : '' }}
					</text>
				</view>

				<view class="item-row">
					<text class="row-label">成交</text>
					<text class="row-value">{{ formatDateTime(item.dealTime) }}</text>
					<text class="row-extra" v-if="item.roomNo">· {{ item.roomNo }}</text>
				</view>

				<view class="item-tags">
					<view class="tag" :class="signTagClass(item.signStatus)">{{ signStatusText(item.signStatus) }}</view>
					<view class="tag" :class="payTagClass(item.paymentStatus)">{{ paymentStatusText(item.paymentStatus) }}</view>
					<view class="tag tag-default" v-if="item.subscribeType">{{ subscribeTypeText(item.subscribeType) }}</view>
				</view>
			</view>

			<view class="load-tip" v-if="loading"><text>加载中...</text></view>
			<view class="load-tip" v-else-if="noMore && list.length > 0"><text>没有更多了</text></view>
			<view class="empty-state" v-if="!loading && list.length === 0">
				<pc-icon name="order" color="#D0D0D0" size="56"></pc-icon>
				<text class="empty-text">暂无成交记录</text>
				<text class="empty-sub">仅展示你有权查看的客户的成交记录</text>
			</view>
		</scroll-view>
	</view>
</template>

<script>
	import { getDealsPage } from '../../utils/api.js'

	export default {
		data() {
			return {
				list: [],
				page: 1,
				pageSize: 20,
				total: 0,
				loading: false,
				noMore: false,
				refreshing: false
			}
		},
		onShow() {
			if (this.list.length === 0) this.resetAndLoad()
		},
		methods: {
			async resetAndLoad() {
				this.page = 1
				this.noMore = false
				this.list = []
				await this.loadData()
			},
			async onRefresh() {
				this.refreshing = true
				try { await this.resetAndLoad() } finally { this.refreshing = false }
			},
			async loadMore() {
				if (this.loading || this.noMore) return
				this.page += 1
				await this.loadData(true)
			},
			async loadData(append = false) {
				if (this.loading) return
				this.loading = true
				try {
					const res = await getDealsPage({ page: this.page, size: this.pageSize })
					// 后端返回 MyBatis-Plus IPage：{records, total, current, size}
					// request.js 已剥掉外层 Result，res.data 就是 IPage 对象
					const data = res.data || {}
					const records = Array.isArray(data.records) ? data.records : []
					this.total = Number(data.total) || 0
					if (append) this.list = this.list.concat(records)
					else this.list = records
					if (this.list.length >= this.total || records.length < this.pageSize) this.noMore = true
				} catch (e) {
					if (append) this.page -= 1
					uni.showToast({ title: e.message || '加载失败', icon: 'none' })
				} finally {
					this.loading = false
				}
			},
			goCustomerDetail(item) {
				if (!item.customerId) return
				uni.navigateTo({ url: `/pages/customer/detail?id=${item.customerId}` })
			},
			formatAmount(v) {
				if (v == null || v === '') return '--'
				const n = Number(v)
				if (!isFinite(n)) return '--'
				// 万元单位更紧凑
				return n >= 10000 ? (n / 10000).toFixed(2) + ' 万' : n.toFixed(2)
			},
			formatDateTime(v) {
				if (!v) return '--'
				return String(v).replace('T', ' ').slice(0, 16)
			},
			signStatusText(s) {
				return { 1: '已签约', 2: '未签约' }[s] || '签约待录'
			},
			signTagClass(s) {
				if (s === 1) return 'tag-success'
				if (s === 2) return 'tag-warning'
				return 'tag-default'
			},
			paymentStatusText(s) {
				return { 0: '未回款', 1: '部分回款', 2: '全部回款' }[s] || '回款未录'
			},
			payTagClass(s) {
				if (s === 2) return 'tag-success'
				if (s === 1) return 'tag-info'
				return 'tag-warning'
			},
			subscribeTypeText(t) {
				return { 1: '小订', 2: '大定' }[t] || ''
			}
		}
	}
</script>

<style lang="scss" scoped>
	.page { min-height: 100vh; min-height: 100dvh; background: #F0F0F0; }
	.list-scroll { height: 100vh; }

	.deal-item {
		background: #FFF; margin: 16rpx 20rpx; border-radius: 16rpx; padding: 24rpx;
		&:active { background: #FAFAFA; }
	}
	.item-top {
		display: flex; align-items: center; justify-content: space-between; margin-bottom: 16rpx;
	}
	.project-name {
		font-size: 30rpx; font-weight: 600; color: #1A1A1A; flex: 1;
		overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
	}
	.deal-amount {
		font-size: 32rpx; font-weight: 700; color: #F5222D; flex-shrink: 0; margin-left: 16rpx;
	}

	.item-row {
		display: flex; align-items: center; padding: 6rpx 0; font-size: 26rpx; flex-wrap: wrap;
	}
	.row-label {
		color: #999; min-width: 80rpx; margin-right: 8rpx;
	}
	.row-value { color: #333; }
	.row-phone { color: #07C160; margin-left: 12rpx; }
	.row-extra { color: #999; margin-left: 8rpx; font-size: 24rpx; }

	.item-tags {
		display: flex; flex-wrap: wrap; gap: 12rpx; margin-top: 16rpx;
	}
	.tag {
		padding: 4rpx 16rpx; border-radius: 6rpx; font-size: 22rpx;
	}
	.tag-success { background: rgba(7,193,96,0.1); color: #07C160; }
	.tag-warning { background: rgba(250,140,22,0.1); color: #FA8C16; }
	.tag-info    { background: rgba(24,144,255,0.1); color: #1890FF; }
	.tag-default { background: #F5F5F5; color: #666; }

	.load-tip { text-align: center; padding: 24rpx 0; color: #999; font-size: 24rpx; }
	.empty-state {
		display: flex; flex-direction: column; align-items: center; padding: 160rpx 0;
	}
	.empty-text { color: #999; margin-top: 16rpx; font-size: 26rpx; }
	.empty-sub { color: #C0C0C0; margin-top: 8rpx; font-size: 22rpx; }
</style>
