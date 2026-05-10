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
			<view class="record-item" v-for="item in list" :key="item.id" @tap="openTimeline(item)">
				<view class="record-top">
					<text class="record-type">{{ item.typeName }}<text v-if="item.daysText" class="record-days">  {{ item.daysText }}</text></text>
					<view v-if="!statusFilter" class="record-status" :class="'s-' + item.status">
						<text>{{ getStatusText(item.status) }}</text>
					</view>
				</view>
				<view class="record-info" v-if="item.amount">
					<text class="info-text">金额：¥{{ item.amount }}</text>
				</view>
				<view class="record-info" v-if="item.rangeText">
					<text class="info-text">{{ item.rangeText }}</text>
				</view>
				<view class="record-info" v-if="item.reason">
						<text class="info-text">原因：{{ item.reason }}</text>
					</view>
					<view class="record-info" v-if="item.currentNodeName && (item.status === 1 || item.status === 2)">
					<text class="info-text node-text">当前节点：{{ item.currentNodeName }}</text>
				</view>
				<view class="record-info reject-row" v-if="item.status === 4 && item.rejectReason">
					<text class="info-text reject-text">驳回原因：{{ item.rejectReason }}</text>
				</view>
				<view class="record-bottom">
					<text class="info-text">提交时间：{{ item.createTime || '--' }}</text>
					<view v-if="canCancel(item)" class="cancel-btn" @tap.stop="onCancel(item)">
						<text>取消申请</text>
					</view>
				</view>
			</view>

			<view class="load-tip" v-if="loading"><text>加载中...</text></view>
			<view class="load-tip" v-else-if="noMore && list.length > 0"><text>没有更多了</text></view>
			<view class="empty-state" v-if="!loading && list.length === 0">
				<u-icon name="order" color="#D0D0D0" size="56"></u-icon>
				<text class="empty-text">暂无记录</text>
			</view>
		</scroll-view>

		<!-- 审批流程时间线弹窗 -->
		<u-popup :show="showTimeline" mode="bottom" round="16" closeable @close="showTimeline = false">
			<view class="timeline-popup">
				<view class="timeline-popup-title">{{ timelineTitle }}</view>
				<view v-if="timelineLoading" class="timeline-loading"><text>加载中...</text></view>
				<view v-else-if="timelineHistories.length === 0" class="timeline-empty">
					<text>暂无审批记录</text>
				</view>
				<view v-else class="timeline">
					<view class="timeline-item" v-for="(t, i) in timelineHistories" :key="i">
						<view class="tl-dot" :class="dotClass(t, i)"></view>
						<view class="tl-line" v-if="i < timelineHistories.length - 1"></view>
						<view class="tl-content">
							<view class="tl-header">
								<text class="tl-node">{{ t.nodeName || ('节点 ' + (i + 1)) }}</text>
								<text class="tl-state" :class="stateClass(t, i)">{{ stateLabel(t, i) }}</text>
							</view>
							<text class="tl-approver">{{ t.approverName || '—' }}</text>
							<text class="tl-time" v-if="t.approvalTime">{{ formatDateTime(t.approvalTime) }}</text>
							<text class="tl-reason" v-if="t.remark">备注：{{ t.remark }}</text>
						</view>
					</view>
				</view>
			</view>
		</u-popup>

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
				<view class="fab-menu-item" v-for="t in generalTypes" :key="t.businessType"
					@tap="goGeneral(t)">{{ t.label }}</view>
			</view>
		</view>
	</view>
</template>

<script>
	import {
		getLeaveList, getPaymentList, cancelLeave, cancelPayment, getApprovalDetail,
		getGeneralTypes, getGeneralList, cancelGeneral
	} from '../../utils/api.js'

	export default {
		data() {
			return {
				activeType: 'all',
				statusFilter: '',
				builtinTypeList: [
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
					{ label: '已驳回', value: 'rejected' },
					{ label: '已撤销', value: 'cancelled' }
				],
				list: [],
				page: 1,
				pageSize: 20,
				loading: false,
				noMore: false,
				refreshing: false,
				showApplyMenu: false,
				showDataUpdated: false,
				generalTypes: [],
				showTimeline: false,
				timelineLoading: false,
				timelineTitle: '审批流程',
				timelineHistories: []
			}
		},
		computed: {
			typeList() {
				return [
					...this.builtinTypeList,
					...this.generalTypes.map(t => ({ label: t.label, value: t.businessType }))
				]
			}
		},
		onLoad(options) {
			if (options?.type) {
				// 这里 typeList 可能未拉到通用类型；若是 general key 也直接信任
				this.activeType = options.type
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
			this.loadGeneralTypes()
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
				const map = { 1: '审批中', 2: '审批中', 3: '已通过', 4: '已驳回', 5: '已撤销' }
				return map[status] || status || '--'
			},
			canCancel(item) {
				// 仅本人发起且仍在审批中的申请允许撤销（列表本身已按当前用户过滤）
				return item && (item.status === 1 || item.status === 2) && item.cancelKind
			},
			async loadGeneralTypes() {
				try {
					const res = await getGeneralTypes()
					this.generalTypes = (res && res.data) || []
				} catch (err) {
					this.generalTypes = []
				}
			},
			goGeneral(t) {
				this.showApplyMenu = false
				const label = encodeURIComponent(t.label || t.businessType)
				uni.navigateTo({ url: `/pages/apply/general?type=${t.businessType}&label=${label}` })
			},
			// 审批时间线 ==========
			async openTimeline(item) {
				if (!item || !item.detailType || !item.rawId) return
				this.timelineTitle = `${item.typeName} · 审批流程`
				this.timelineHistories = []
				this.timelineLoading = true
				this.showTimeline = true
				try {
					const res = await getApprovalDetail(item.rawId, item.detailType)
					this.timelineHistories = (res && res.data && res.data.histories) || []
				} catch (err) {
					uni.showToast({ title: '审批流程加载失败', icon: 'none' })
				} finally {
					this.timelineLoading = false
				}
			},
			currentPendingIndex() {
				return this.timelineHistories.findIndex(h => h.result == null)
			},
			dotClass(t, i) {
				if (t.result === 1) return 'approved'
				if (t.result === 2) return 'rejected'
				if (i === this.currentPendingIndex()) return 'active'
				return ''
			},
			stateClass(t, i) {
				if (t.result === 1) return 's-approved'
				if (t.result === 2) return 's-rejected'
				if (i === this.currentPendingIndex()) return 's-active'
				return 's-future'
			},
			stateLabel(t, i) {
				if (t.result === 1) return '已通过'
				if (t.result === 2) return '已驳回'
				if (i === this.currentPendingIndex()) return '审批中'
				return '未审批'
			},
			formatDateTime(value) {
				if (!value) return '--'
				return String(value).replace('T', ' ').slice(0, 16)
			},
			async onCancel(item) {
				const ok = await new Promise(resolve => {
					uni.showModal({
						title: '确认撤销',
						content: `确定撤销这条「${item.typeName}」申请吗？撤销后审批人将不再收到此审批。`,
						confirmText: '撤销',
						confirmColor: '#F5222D',
						success: r => resolve(r.confirm),
						fail: () => resolve(false)
					})
				})
				if (!ok) return
				uni.showLoading({ title: '撤销中...', mask: true })
				try {
					if (item.cancelKind === 'leave' || item.cancelKind === 'compensate') {
						await cancelLeave(item.rawId, item.cancelKind)
					} else if (item.cancelKind === 'payment') {
						await cancelPayment(item.rawId)
					} else if (item.cancelKind === 'general') {
						await cancelGeneral(item.rawId)
					}
					uni.hideLoading()
					uni.showToast({ title: '已撤销', icon: 'success' })
					this.resetAndLoad()
				} catch (err) {
					uni.hideLoading()
					const msg = (err && (err.msg || err.message)) || '撤销失败'
					uni.showToast({ title: msg, icon: 'none' })
				}
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
					const leaveStatusMap = { pending: 1, approved: 2, rejected: 3, cancelled: 4 }
					const leaveStatus = this.statusFilter ? leaveStatusMap[this.statusFilter] : undefined
					const leaveRes = await getLeaveList({
						type: leaveType,
						status: leaveStatus,
						page: this.page,
						pageSize: this.pageSize
					}).catch(() => ({ data: { list: [] } }))
					const leaveRows = leaveRes.data?.list || []
					const leaveSubTypeMap = { 1: '事假', 2: '病假', 3: '年假', 4: '婚假', 5: '产假' }
					const showLeavePrefix = type === 'all'  // 在"全部类型"tab 才显示"请假·"前缀
					leaveRows.forEach(r => {
						let typeName
						if (r.type === 'compensate') {
							typeName = showLeavePrefix ? '调休申请' : '调休'
						} else {
							const sub = leaveSubTypeMap[r.leaveType] || '请假'
							typeName = showLeavePrefix ? `请假·${sub}` : sub
						}
						const start = r.startTime ? String(r.startTime).replace('T', ' ').slice(0, 16) : ''
						const end = r.endTime ? String(r.endTime).replace('T', ' ').slice(0, 16) : ''
						// 紧凑时间："05-09 09:00 → 05-10 18:00"
						const compact = (s) => s ? s.slice(5) : ''
						const rangeText = end ? `${compact(start)} → ${compact(end)}` : compact(start)
						// 请假天数：直接用后端按考勤工时算好的 days 字段
						// （旧版按 24h wall-clock 算 5/9 00:00~18:30 会得 0.8 天，明显不符直觉）
						let daysText = ''
						if (r.type === 'leave' && typeof r.days === 'number') {
							daysText = `${r.days}天`
						}
						// 后端 status: 1 审批中 / 2 已通过 / 3 已驳回 / 4 已撤销
						// UI status: 1 审批中 / 3 已通过 / 4 已驳回 / 5 已撤销（与付款单 1/2/3/4/5 对齐）
						const leaveUiStatus = ({ 1: 1, 2: 3, 3: 4, 4: 5 })[r.status] || 1
						rows.push({
							id: `l-${r.type}-${r.id}`,
							rawId: r.id,
							cancelKind: r.type, // 'leave' 或 'compensate'
							detailType: r.type, // getApprovalDetail 用
							typeName,
							daysText,
							status: leaveUiStatus,
							amount: null,
							rangeText,
							reason: r.reason || '',
							createTime: r.createTime,
							currentNodeName: r.currentNodeName || '',
							rejectReason: r.rejectReason || ''
						})
					})
				}

				if (type === 'all' || type === 'expense' || type === 'advance' || type === 'prepay') {
					const paymentTypeMap = { expense: 1, advance: 2, prepay: 3 }
					const requestType = type === 'all' ? undefined : paymentTypeMap[type]
					const statusList = this.statusFilter === 'pending'
						? [1, 2]
						: (this.statusFilter === 'approved' ? [3]
							: (this.statusFilter === 'rejected' ? [4]
								: (this.statusFilter === 'cancelled' ? [5] : [undefined])))
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
							let rejectReason = ''
							if (r.status === 4 && Array.isArray(r.approvals)) {
								const rejected = r.approvals.find(a => a.result === 2)
								if (rejected && rejected.remark) rejectReason = rejected.remark
							}
							const paymentDetailType = ({ 1: 'expense', 2: 'advance', 3: 'prepay' })[r.requestType] || 'expense'
							rows.push({
								id: `p-${r.id}-${r.status}`,
								rawId: r.id,
								cancelKind: 'payment',
								detailType: paymentDetailType, // getApprovalDetail 用 expense/advance/prepay
								typeName: typeNameMap[r.requestType] || '付款申请',
								daysText: '',
								status: r.status,
								amount: r.amount,
								rangeText: '',
								reason: r.description || '',
								createTime: r.createTime,
								currentNodeName: r.currentNodeName || '',
								rejectReason
							})
						})
					}
				}

				// 通用审批：在"全部类型"或当前 activeType 命中已注册的非内置类型时拉
				const isGeneralActive = type !== 'all'
					&& !['leave', 'compensate', 'expense', 'advance', 'prepay'].includes(type)
				if (type === 'all' || isGeneralActive) {
					const genStatusMap = { pending: 1, approved: 2, rejected: 3, cancelled: 4 }
					const genStatus = this.statusFilter ? genStatusMap[this.statusFilter] : undefined
					const genRes = await getGeneralList({
						businessType: isGeneralActive ? type : undefined,
						status: genStatus,
						page: this.page,
						pageSize: this.pageSize
					}).catch(() => ({ data: { list: [] } }))
					const genRows = genRes.data?.list || []
					// 通用类型 label 用 generalTypes 里的映射，回退到原 key
					const labelOf = (key) => {
						const t = this.generalTypes.find(g => g.businessType === key)
						return t ? t.label : key
					}
					// 后端 status 1/2/3/4 直接对应 UI 1/3/4/5（与请假同形，2 没用到）
					const genUiStatus = (s) => ({ 1: 1, 2: 3, 3: 4, 4: 5 })[s] || 1
					genRows.forEach(r => {
						rows.push({
							id: `g-${r.businessType}-${r.id}`,
							rawId: r.id,
							cancelKind: 'general',
							detailType: r.businessType,
							typeName: labelOf(r.businessType),
							daysText: '',
							status: genUiStatus(r.status),
							amount: null,
							rangeText: '',
							reason: r.description || '',
							createTime: r.createTime,
							currentNodeName: '',
							rejectReason: ''
						})
					})
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
	.record-days { font-size: 24rpx; font-weight: 400; color: #666; margin-left: 8rpx; }
	.record-status {
		padding: 4rpx 16rpx; border-radius: 20rpx; font-size: 20rpx;
		&.s-pending, &.s-1, &.s-2 { background: #FFF7E6; color: #FA8C16; }
		&.s-approved, &.s-3 { background: #F6FFED; color: #52C41A; }
		&.s-rejected, &.s-4 { background: #FFF1F0; color: #F5222D; }
		&.s-cancelled, &.s-5 { background: #F5F5F5; color: #999; }
	}
	.record-info { margin-top: 8rpx; }
	.record-bottom {
		margin-top: 12rpx; display: flex; justify-content: space-between; align-items: center;
	}
	.cancel-btn {
		padding: 8rpx 24rpx; border-radius: 24rpx;
		background: #FFF1F0; border: 1rpx solid #FFCCC7;
		text { font-size: 24rpx; color: #F5222D; }
		&:active { background: #FFE4E1; }
	}
	.info-text { font-size: 24rpx; color: #999; }
	.info-text.node-text { color: #FA8C16; }
	.reject-row {
		background: #FFF1F0; border: 1rpx solid #FFCCC7; border-radius: 8rpx;
		padding: 12rpx 16rpx; margin-top: 12rpx;
	}
	.reject-row .reject-text { color: #F5222D; }
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

	/* 审批流程时间线弹窗（与审批人侧 approval/detail.vue 同视觉规范） */
	.timeline-popup { padding: 32rpx 32rpx 48rpx; max-height: 80vh; }
	.timeline-popup-title {
		font-size: 32rpx; font-weight: 600; color: #1A1A1A;
		text-align: center; margin-bottom: 32rpx;
	}
	.timeline-loading, .timeline-empty {
		text-align: center; padding: 60rpx 0;
		text { font-size: 26rpx; color: #999; }
	}
	.timeline { padding-left: 8rpx; max-height: 60vh; overflow-y: auto; }
	.timeline-item { display: flex; position: relative; padding-bottom: 24rpx; }
	.tl-dot {
		width: 18rpx; height: 18rpx; border-radius: 50%; background: #DDD;
		margin-right: 20rpx; margin-top: 8rpx; flex-shrink: 0; z-index: 1;
		&.active { background: #FA8C16; box-shadow: 0 0 0 4rpx rgba(250, 140, 22, 0.2); }
		&.approved { background: #52C41A; }
		&.rejected { background: #F5222D; }
	}
	.tl-line {
		position: absolute; left: 8rpx; top: 26rpx; bottom: 0; width: 2rpx; background: #E8E8E8;
	}
	.tl-content { flex: 1; min-width: 0; }
	.tl-header {
		display: flex; align-items: center; justify-content: space-between; gap: 12rpx;
	}
	.tl-node { font-size: 28rpx; color: #1A1A1A; font-weight: 500; }
	.tl-state {
		font-size: 22rpx; padding: 2rpx 12rpx; border-radius: 16rpx; flex-shrink: 0;
		&.s-approved { background: #F6FFED; color: #52C41A; }
		&.s-rejected { background: #FFF1F0; color: #F5222D; }
		&.s-active   { background: #FFF7E6; color: #FA8C16; }
		&.s-future   { background: #F5F5F5; color: #999; }
	}
	.tl-approver { font-size: 24rpx; color: #666; margin-top: 6rpx; display: block; }
	.tl-time { font-size: 22rpx; color: #BBB; margin-top: 4rpx; display: block; }
	.tl-reason { font-size: 24rpx; color: #999; margin-top: 4rpx; display: block; }
</style>
