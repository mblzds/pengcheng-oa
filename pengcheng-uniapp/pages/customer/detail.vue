<template>
	<view class="page">
		<scroll-view scroll-y class="content">
			<!-- 基本信息 -->
			<view class="card">
				<view class="card-title">报备信息</view>
				<view class="info-row">
					<text class="info-label">客户姓氏</text>
					<text class="info-value">{{ report.customerName || '--' }}</text>
				</view>
				<view class="info-row">
					<text class="info-label">联系方式</text>
					<text class="info-value">{{ report.phoneMasked || '--' }}</text>
				</view>
				<view class="info-row">
					<text class="info-label">联盟商</text>
					<text class="info-value">{{ report.allianceName || '--' }}</text>
				</view>
				<view class="info-row">
					<text class="info-label">报备编号</text>
					<text class="info-value">{{ report.reportNo || '--' }}</text>
				</view>
				<view class="info-row">
					<text class="info-label">报备时间</text>
					<text class="info-value">{{ report.createTime || '--' }}</text>
				</view>
				<view class="info-row">
					<text class="info-label">当前状态</text>
					<view class="status-tag" :class="'status-' + report.status">
						<text>{{ getStatusText(report.status) }}</text>
					</view>
				</view>
			</view>

			<!-- 到访记录 -->
			<view class="card">
				<view class="card-header">
					<text class="card-title">到访记录</text>
					<text class="card-action" @tap="showVisitForm = true">+录入到访</text>
				</view>
				<view class="visit-item" v-for="(v, i) in detail.visits || []" :key="i">
					<view class="timeline-dot"></view>
					<view class="visit-info">
						<text class="visit-time">{{ v.actualVisitTime }}</text>
						<text class="visit-desc">到访人数：{{ v.actualVisitCount }}人 | 接待：{{ v.receptionist || '--' }}</text>
					</view>
				</view>
				<view class="empty-tip" v-if="!detail.visits || detail.visits.length === 0">
					<text>暂无到访记录</text>
				</view>
			</view>

			<!-- 成交信息 -->
			<view class="card">
				<view class="card-header">
					<text class="card-title">成交信息</text>
					<text class="card-action" @tap="showDealForm = true">+录入成交</text>
				</view>
				<view v-if="detail.deal">
					<view class="info-row">
						<text class="info-label">成交房号</text>
						<text class="info-value">{{ detail.deal.roomNo || '--' }}</text>
					</view>
					<view class="info-row">
						<text class="info-label">成交金额</text>
						<text class="info-value amount">{{ detail.deal.dealAmount ? '¥' + detail.deal.dealAmount : '--' }}</text>
					</view>
					<view class="info-row">
						<text class="info-label">成交时间</text>
						<text class="info-value">{{ detail.deal.dealTime || '--' }}</text>
					</view>
				</view>
				<view class="empty-tip" v-else>
					<text>暂无成交信息</text>
				</view>
			</view>

			<!-- 跟进历史 -->
			<view class="card" v-if="detail.timeline && detail.timeline.length > 0">
				<view class="card-title">跟进历史</view>
				<view class="timeline">
					<view class="timeline-item" v-for="(t, i) in detail.timeline" :key="i">
						<view class="tl-dot" :class="{ active: i === 0 }"></view>
						<view class="tl-line" v-if="i < detail.timeline.length - 1"></view>
						<view class="tl-content">
							<text class="tl-title">{{ t.action }}</text>
							<text class="tl-time">{{ t.time }}</text>
						</view>
					</view>
				</view>
			</view>
		</scroll-view>

		<!-- 到访表单弹窗 -->
		<u-popup :show="showVisitForm" mode="bottom" round="16" @close="showVisitForm = false">
			<view class="popup-content">
				<view class="popup-title">录入到访</view>
				<view class="form-item">
					<text class="form-label">到访时间</text>
					<picker mode="datetime" @change="e => visitForm.actualVisitTime = e.detail.value">
						<view class="picker-value">
							<text :class="{ placeholder: !visitForm.actualVisitTime }">{{ visitForm.actualVisitTime || '请选择' }}</text>
						</view>
					</picker>
				</view>
				<view class="form-item">
					<text class="form-label">到访人数</text>
					<input class="form-input" v-model="visitForm.actualVisitCount" type="number" placeholder="请输入" />
				</view>
				<view class="form-item">
					<text class="form-label">接待人员</text>
					<input class="form-input" v-model="visitForm.receptionist" placeholder="请输入" />
				</view>
				<button class="popup-btn" @tap="submitVisit">确认提交</button>
			</view>
		</u-popup>

		<!-- 成交表单弹窗 -->
		<u-popup :show="showDealForm" mode="bottom" round="16" @close="showDealForm = false">
			<view class="popup-content">
				<view class="popup-title">录入成交</view>
				<view class="form-item">
					<text class="form-label">成交房号</text>
					<input class="form-input" v-model="dealForm.roomNo" placeholder="请输入" />
				</view>
				<view class="form-item">
					<text class="form-label">成交金额</text>
					<input class="form-input" v-model="dealForm.dealAmount" type="digit" placeholder="请输入" />
				</view>
				<view class="form-item">
					<text class="form-label">成交时间</text>
					<picker mode="datetime" @change="e => dealForm.dealTime = e.detail.value">
						<view class="picker-value">
							<text :class="{ placeholder: !dealForm.dealTime }">{{ dealForm.dealTime || '请选择' }}</text>
						</view>
					</picker>
				</view>
				<view class="form-item">
					<text class="form-label">签约状态</text>
					<picker :range="signStatusList" range-key="label" @change="onSignStatusChange">
						<view class="picker-value">
							<text :class="{ placeholder: !dealForm.signStatusLabel }">{{ dealForm.signStatusLabel || '请选择' }}</text>
						</view>
					</picker>
				</view>
				<view class="form-item">
					<text class="form-label">认购类型</text>
					<picker :range="subscribeTypes" range-key="label" @change="onSubscribeTypeChange">
						<view class="picker-value">
							<text :class="{ placeholder: !dealForm.subscribeTypeLabel }">{{ dealForm.subscribeTypeLabel || '请选择' }}</text>
						</view>
					</picker>
				</view>
				<button class="popup-btn" @tap="submitDeal">确认提交</button>
			</view>
		</u-popup>
	</view>
</template>

<script>
	import { getCustomerDetail, addCustomerVisit, addCustomerDeal } from '../../utils/api.js'

	export default {
		data() {
			return {
				customerId: '',
				detail: {},
				report: {},
				showVisitForm: false,
				showDealForm: false,
				visitForm: { actualVisitTime: '', actualVisitCount: '', receptionist: '' },
				dealForm: { roomNo: '', dealAmount: '', dealTime: '', signStatus: '', subscribeType: '', signStatusLabel: '', subscribeTypeLabel: '' },
				signStatusList: [{ label: '已签约', value: 1 }, { label: '未签约', value: 2 }],
				subscribeTypes: [{ label: '小订', value: 1 }, { label: '大定', value: 2 }]
			}
		},
		onLoad(opts) {
			this.customerId = opts.id
			if (opts.action === 'visit') this.showVisitForm = true
		},
		onShow() {
			if (this.customerId) this.loadDetail()
		},
		methods: {
			getStatusText(status) {
				const map = { 1: '已报备', 2: '已到访', 3: '已成交' }
				return map[status] || status || '--'
			},
			async loadDetail() {
				try {
					const res = await getCustomerDetail(this.customerId)
					this.detail = res.data || {}
					this.report = this.detail.reportInfo || {}
				} catch (err) { console.error(err) }
			},
			onSignStatusChange(e) {
				const item = this.signStatusList[e.detail.value]
				if (!item) return
				this.dealForm.signStatus = item.value
				this.dealForm.signStatusLabel = item.label
			},
			onSubscribeTypeChange(e) {
				const item = this.subscribeTypes[e.detail.value]
				if (!item) return
				this.dealForm.subscribeType = item.value
				this.dealForm.subscribeTypeLabel = item.label
			},
			async submitVisit() {
				if (!this.visitForm.actualVisitTime) return uni.showToast({ title: '请选择到访时间', icon: 'none' })
				try {
					await addCustomerVisit({
						customerId: Number(this.customerId),
						actualVisitTime: this.visitForm.actualVisitTime.replace(' ', 'T'),
						actualVisitCount: Number(this.visitForm.actualVisitCount || 1),
						receptionist: this.visitForm.receptionist
					})
					uni.showToast({ title: '录入成功', icon: 'success' })
					this.showVisitForm = false
					this.visitForm = { actualVisitTime: '', actualVisitCount: '', receptionist: '' }
					this.loadDetail()
				} catch (err) { console.error(err) }
			},
			async submitDeal() {
				if (!this.dealForm.roomNo) return uni.showToast({ title: '请输入成交房号', icon: 'none' })
				if (!this.dealForm.dealAmount) return uni.showToast({ title: '请输入成交金额', icon: 'none' })
				if (!this.dealForm.dealTime || !this.dealForm.signStatus || !this.dealForm.subscribeType) {
					return uni.showToast({ title: '请补全成交信息', icon: 'none' })
				}
				try {
					await addCustomerDeal({
						customerId: Number(this.customerId),
						roomNo: this.dealForm.roomNo,
						dealAmount: this.dealForm.dealAmount,
						dealTime: this.dealForm.dealTime.replace(' ', 'T'),
						signStatus: this.dealForm.signStatus,
						subscribeType: this.dealForm.subscribeType
					})
					uni.showToast({ title: '录入成功', icon: 'success' })
					this.showDealForm = false
					this.dealForm = { roomNo: '', dealAmount: '', dealTime: '', signStatus: '', subscribeType: '', signStatusLabel: '', subscribeTypeLabel: '' }
					this.loadDetail()
				} catch (err) {
					if (err.code === 400) {
						uni.showModal({ title: '操作失败', content: '当前客户状态不允许此操作', showCancel: false })
					}
				}
			}
		}
	}
</script>

<style lang="scss" scoped>
	.page { min-height: 100vh; min-height: 100dvh; background: #F0F0F0; }
	.content { padding-bottom: 40rpx; }
	.card {
		margin: 20rpx 20rpx 0; background: #FFF; border-radius: 16rpx; padding: 24rpx;
	}
	.card-header { display: flex; justify-content: space-between; align-items: center; }
	.card-title { font-size: 28rpx; font-weight: 600; color: #1A1A1A; margin-bottom: 16rpx; }
	.card-action { font-size: 26rpx; color: #07C160; margin-bottom: 16rpx; }
	.info-row {
		display: flex; justify-content: space-between; align-items: center; padding: 12rpx 0;
		border-bottom: 1rpx solid #F8F8F8;
		&:last-child { border-bottom: none; }
	}
	.info-label { font-size: 26rpx; color: #999; }
	.info-value { font-size: 26rpx; color: #333; }
	.amount { color: #F5222D; font-weight: 600; }
	.status-tag {
		padding: 4rpx 16rpx; border-radius: 20rpx; font-size: 20rpx;
		&.status-reported { background: #E6F7FF; color: #1890FF; }
		&.status-visited { background: #F6FFED; color: #52C41A; }
		&.status-dealt { background: #FFF7E6; color: #FA8C16; }
		&.status-expired { background: #F5F5F5; color: #999; }
	}
	.visit-item {
		display: flex; align-items: flex-start; padding: 16rpx 0;
	}
	.timeline-dot {
		width: 16rpx; height: 16rpx; border-radius: 50%; background: #07C160;
		margin-right: 16rpx; margin-top: 6rpx; flex-shrink: 0;
	}
	.visit-info { flex: 1; }
	.visit-time { font-size: 26rpx; color: #333; display: block; }
	.visit-desc { font-size: 22rpx; color: #999; margin-top: 4rpx; display: block; }
	.empty-tip { padding: 24rpx 0; text-align: center; text { font-size: 24rpx; color: #CCC; } }

	/* 时间线 */
	.timeline { padding-left: 8rpx; }
	.timeline-item { display: flex; position: relative; padding-bottom: 24rpx; }
	.tl-dot {
		width: 16rpx; height: 16rpx; border-radius: 50%; background: #DDD;
		margin-right: 20rpx; margin-top: 6rpx; flex-shrink: 0; z-index: 1;
		&.active { background: #07C160; }
	}
	.tl-line {
		position: absolute; left: 7rpx; top: 22rpx; bottom: 0; width: 2rpx; background: #E8E8E8;
	}
	.tl-content { flex: 1; }
	.tl-title { font-size: 26rpx; color: #333; display: block; }
	.tl-time { font-size: 22rpx; color: #BBB; margin-top: 4rpx; display: block; }

	/* 弹窗表单 */
	.popup-content { padding: 32rpx 24rpx 48rpx; }
	.popup-title { font-size: 32rpx; font-weight: 600; color: #1A1A1A; text-align: center; margin-bottom: 32rpx; }
	.form-item { margin-bottom: 24rpx; }
	.form-label { font-size: 26rpx; color: #666; margin-bottom: 8rpx; display: block; }
	.form-input {
		height: 72rpx; font-size: 28rpx; color: #1A1A1A;
		border: 1rpx solid #E8E8E8; border-radius: 8rpx; padding: 0 16rpx;
	}
	.picker-value {
		height: 72rpx; display: flex; align-items: center;
		border: 1rpx solid #E8E8E8; border-radius: 8rpx; padding: 0 16rpx;
		font-size: 28rpx; color: #1A1A1A;
	}
	.placeholder { color: #C0C0C0; }
	.popup-btn {
		margin-top: 32rpx; height: 88rpx; line-height: 88rpx;
		background: #07C160; color: #FFF; font-size: 30rpx; font-weight: 500;
		border-radius: 16rpx; border: none;
	}
	.popup-btn::after { border: none; }
</style>
