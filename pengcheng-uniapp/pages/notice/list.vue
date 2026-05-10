<template>
	<view class="page">
		<view class="topbar" v-if="list.length > 0">
			<text class="count">共 {{ total }} 条</text>
			<text class="action" v-if="hasUnread" @tap="markAllRead">全部已读</text>
		</view>

		<scroll-view scroll-y class="list"
			refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh"
			@scrolltolower="loadMore">

			<view class="empty" v-if="!loading && list.length === 0">
				<u-icon name="bell" color="#D0D0D0" size="56"></u-icon>
				<text class="empty-text">暂无系统通知</text>
			</view>

			<view class="item" v-for="n in list" :key="n.id" @tap="openDetail(n)">
				<view class="dot" :class="{ unread: isUnread(n) }"></view>
				<view class="content">
					<text class="title" :class="{ read: !isUnread(n) }">{{ n.title || '系统通知' }}</text>
					<text class="time">{{ formatTime(n.createTime || n.publishTime) }}</text>
				</view>
				<u-icon name="arrow-right" color="#CCC" size="14"></u-icon>
			</view>

			<view class="footer" v-if="list.length > 0">
				<text v-if="loadingMore">加载中...</text>
				<text v-else-if="hasMore">上拉加载更多</text>
				<text v-else>—— 没有更多了 ——</text>
			</view>
		</scroll-view>
	</view>
</template>

<script>
	import { getNoticeList, readNotice, markAllNoticesRead } from '../../utils/api.js'
	import { checkLogin } from '../../utils/auth.js'

	export default {
		data() {
			return {
				list: [],
				page: 1,
				pageSize: 20,
				total: 0,
				loading: false,
				loadingMore: false,
				refreshing: false
			}
		},
		computed: {
			hasMore() {
				return this.list.length < this.total
			},
			hasUnread() {
				return this.list.some(n => this.isUnread(n))
			}
		},
		onLoad() {
			uni.setNavigationBarTitle({ title: '系统通知' })
		},
		onShow() {
			if (!checkLogin()) return
			this.loadFirstPage()
		},
		methods: {
			isUnread(n) {
				return n.readFlag === false || n.readStatus === 0
			},
			async loadFirstPage() {
				this.loading = true
				this.page = 1
				try {
					const res = await getNoticeList(1, this.pageSize)
					const data = res.data || {}
					this.list = data.list || []
					this.total = Number(data.total ?? this.list.length) || 0
				} catch (e) {
					console.error(e)
				} finally {
					this.loading = false
					this.refreshing = false
				}
			},
			async loadMore() {
				if (this.loadingMore || !this.hasMore) return
				this.loadingMore = true
				try {
					const next = this.page + 1
					const res = await getNoticeList(next, this.pageSize)
					const data = res.data || {}
					const more = data.list || []
					if (more.length > 0) {
						this.list = [...this.list, ...more]
						this.page = next
					}
					this.total = Number(data.total ?? this.total) || this.total
				} catch (e) {
					console.error(e)
				} finally {
					this.loadingMore = false
				}
			},
			onRefresh() {
				this.refreshing = true
				this.loadFirstPage()
			},
			async markAllRead() {
				try {
					await markAllNoticesRead()
					this.list = this.list.map(n => ({ ...n, readFlag: true, readStatus: 1 }))
					uni.showToast({ title: '已全部标记为已读', icon: 'none' })
				} catch (e) {
					uni.showToast({ title: '操作失败', icon: 'none' })
				}
			},
			openDetail(n) {
				if (!n.id) return
				if (this.isUnread(n)) {
					readNotice(n.id).catch(() => {})
					n.readFlag = true
					n.readStatus = 1
					this.list = [...this.list]
				}
				uni.navigateTo({ url: `/pages/notice/detail?id=${n.id}` })
			},
			formatTime(time) {
				if (!time) return ''
				return String(time).replace('T', ' ').slice(0, 16)
			}
		}
	}
</script>

<style lang="scss" scoped>
	.page {
		min-height: 100vh; min-height: 100dvh; height: 100vh; height: 100dvh;
		display: flex; flex-direction: column; background: #F0F0F0;
	}

	.topbar {
		display: flex; align-items: center; justify-content: space-between;
		padding: 16rpx 28rpx;
		background: #FFF;
		border-bottom: 1rpx solid #F0F0F0;
		.count { font-size: 24rpx; color: #999; }
		.action { font-size: 26rpx; color: #07C160; padding: 8rpx 0; }
	}

	.list { flex: 1; overflow: hidden; }

	.empty {
		display: flex; flex-direction: column; align-items: center;
		padding: 160rpx 40rpx;
		.empty-text { font-size: 26rpx; color: #999; margin-top: 20rpx; }
	}

	.item {
		display: flex; align-items: center;
		padding: 28rpx 28rpx;
		background: #FFF;
		border-bottom: 1rpx solid #F0F0F0;
		&:active { background: #ECECEC; }
	}
	.dot {
		width: 14rpx; height: 14rpx; border-radius: 50%;
		background: transparent;
		margin-right: 16rpx; flex-shrink: 0;
		&.unread { background: #FA5151; }
	}
	.content { flex: 1; min-width: 0; }
	.title {
		font-size: 28rpx; color: #1A1A1A; font-weight: 500;
		display: block;
		overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
		&.read { font-weight: 400; color: #555; }
	}
	.time {
		font-size: 22rpx; color: #B0B0B0; display: block; margin-top: 8rpx;
	}

	.footer {
		text-align: center; padding: 32rpx 0;
		text { font-size: 22rpx; color: #B0B0B0; }
	}
</style>
