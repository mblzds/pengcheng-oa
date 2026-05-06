<template>
	<view class="page">
		<scroll-view scroll-y class="content">
			<!-- 申请信息 -->
			<view class="card">
				<view class="card-title">申请信息</view>
				<view class="info-row" v-for="(item, idx) in infoFields" :key="idx">
					<text class="info-label">{{ item.label }}</text>
					<text class="info-value">{{ item.value }}</text>
				</view>
			</view>

			<!-- 审批流转时间线 -->
			<view class="card" v-if="detail.histories && detail.histories.length > 0">
				<view class="card-title">审批流程</view>
				<view class="timeline">
					<view class="timeline-item" v-for="(t, i) in detail.histories" :key="i">
						<view class="tl-dot" :class="{ active: i === 0, approved: t.result === 1, rejected: t.result === 2 }"></view>
						<view class="tl-line" v-if="i < detail.histories.length - 1"></view>
						<view class="tl-content">
							<text class="tl-title">{{ t.approverName || '审批人' }} - {{ t.result === 1 ? '通过' : '驳回' }}</text>
							<text class="tl-time">{{ formatDateTime(t.approvalTime) }}</text>
							<text class="tl-reason" v-if="t.reason">原因：{{ t.reason }}</text>
							<text class="tl-reason" v-if="t.remark">备注：{{ t.remark }}</text>
						</view>
					</view>
				</view>
			</view>
		</scroll-view>

		<!-- 底部审批按钮 -->
		<view class="action-bar" v-if="canApprove">
			<button class="reject-btn" @tap="showRejectModal = true">驳回</button>
			<button class="approve-btn" @tap="handleApprove(true)">通过</button>
		</view>

		<!-- 驳回原因弹窗 -->
		<u-popup :show="showRejectModal" mode="bottom" round="16" @close="showRejectModal = false">
			<view class="popup-content">
				<view class="popup-title">驳回原因</view>
				<textarea class="reject-textarea" v-model="rejectReason" placeholder="请输入驳回原因" maxlength="200"></textarea>
				<button class="popup-btn" @tap="handleApprove(false)">确认驳回</button>
			</view>
		</u-popup>
	</view>
</template>

<script>
	import { getApprovalDetail, submitApproval } from '../../utils/api.js'

	export default {
		data() {
			return {
				approvalId: '',
				approvalType: '',
				detail: {},
				canApprove: true,
				showRejectModal: false,
				rejectReason: '',
				submitting: false
			}
		},
		computed: {
			infoFields() {
				const d = this.detail
				const fields = []
				if (d.type) fields.push({ label: '申请类型', value: this.getTypeName(d.type) })
				if (d.applicantName) fields.push({ label: '申请人', value: d.applicantName })
				if (d.amount) fields.push({ label: '金额', value: '¥' + d.amount })
				if (d.summary) fields.push({ label: '摘要', value: d.summary })
				if (d.applyTime) fields.push({ label: '提交时间', value: this.formatDateTime(d.applyTime) })
				if (d.status != null) fields.push({ label: '状态', value: this.getStatusText(d.status) })
				return fields
			}
		},
		onLoad(opts) {
			this.approvalId = opts.id
			this.approvalType = opts.type
		},
		onShow() { this.loadDetail() },
		methods: {
			getTypeName(type) {
				const map = { leave: '请假', compensate: '调休', expense: '报销', advance: '垫佣', prepay: '预付佣', commission: '佣金审核' }
				return map[type] || type
			},
			getStatusText(status) {
				const map = { 1: '待审批', 2: '审批中', 3: '已通过', 4: '已驳回' }
				return map[status] || String(status)
			},
			formatDateTime(value) {
				if (!value) return '--'
				return String(value).replace('T', ' ').slice(0, 16)
			},
			async loadDetail() {
				try {
					const res = await getApprovalDetail(this.approvalId, this.approvalType)
					this.detail = res.data || {}
					this.canApprove = [1, 2].includes(this.detail.status)
				} catch (err) { console.error(err) }
			},
			async handleApprove(approved) {
				if (this.submitting) return
				if (!approved && !this.rejectReason.trim()) {
					return uni.showToast({ title: '请输入驳回原因', icon: 'none' })
				}
				this.submitting = true
				try {
					await submitApproval(this.approvalId, {
						approved,
						reason: approved ? '' : this.rejectReason,
						type: this.approvalType
					})
					uni.showToast({ title: approved ? '已通过' : '已驳回', icon: 'success' })
					this.showRejectModal = false
					setTimeout(() => uni.navigateBack(), 1500)
				} catch (err) {
					uni.showToast({ title: '操作失败', icon: 'none' })
				} finally { this.submitting = false }
			}
		}
	}
</script>

<style lang="scss" scoped>
	.page { min-height: 100vh; min-height: 100dvh; background: #F0F0F0; display: flex; flex-direction: column; }
	.content { flex: 1; padding-bottom: 40rpx; }
	.card { margin: 20rpx 20rpx 0; background: #FFF; border-radius: 16rpx; padding: 24rpx; }
	.card-title { font-size: 28rpx; font-weight: 600; color: #1A1A1A; margin-bottom: 16rpx; }
	.info-row {
		display: flex; justify-content: space-between; padding: 12rpx 0;
		border-bottom: 1rpx solid #F8F8F8;
		&:last-child { border-bottom: none; }
	}
	.info-label { font-size: 26rpx; color: #999; flex-shrink: 0; }
	.info-value { font-size: 26rpx; color: #333; text-align: right; flex: 1; margin-left: 20rpx;
		overflow: hidden; text-overflow: ellipsis; }

	/* 时间线 */
	.timeline { padding-left: 8rpx; }
	.timeline-item { display: flex; position: relative; padding-bottom: 24rpx; }
	.tl-dot {
		width: 16rpx; height: 16rpx; border-radius: 50%; background: #DDD;
		margin-right: 20rpx; margin-top: 6rpx; flex-shrink: 0; z-index: 1;
		&.active { background: #FA8C16; }
		&.approved { background: #52C41A; }
		&.rejected { background: #F5222D; }
	}
	.tl-line {
		position: absolute; left: 7rpx; top: 22rpx; bottom: 0; width: 2rpx; background: #E8E8E8;
	}
	.tl-content { flex: 1; }
	.tl-title { font-size: 26rpx; color: #333; display: block; }
	.tl-time { font-size: 22rpx; color: #BBB; margin-top: 4rpx; display: block; }
	.tl-reason { font-size: 24rpx; color: #F5222D; margin-top: 4rpx; display: block; }

	/* 底部操作 */
	.action-bar {
		display: flex; padding: 20rpx 24rpx 40rpx; background: #FFF; gap: 24rpx;
		box-shadow: 0 -2rpx 12rpx rgba(0,0,0,0.04);
	}
	.reject-btn, .approve-btn {
		flex: 1; height: 88rpx; line-height: 88rpx; font-size: 30rpx;
		font-weight: 500; border-radius: 16rpx; border: none;
	}
	.reject-btn { background: #FFF; color: #F5222D; border: 2rpx solid #F5222D; }
	.reject-btn::after { border: none; }
	.approve-btn { background: #07C160; color: #FFF; }
	.approve-btn::after { border: none; }

	/* 弹窗 */
	.popup-content { padding: 32rpx 24rpx 48rpx; }
	.popup-title { font-size: 32rpx; font-weight: 600; color: #1A1A1A; text-align: center; margin-bottom: 32rpx; }
	.reject-textarea {
		width: 100%; min-height: 200rpx; font-size: 28rpx; color: #1A1A1A;
		border: 1rpx solid #E8E8E8; border-radius: 8rpx; padding: 16rpx; box-sizing: border-box;
	}
	.popup-btn {
		margin-top: 32rpx; height: 88rpx; line-height: 88rpx;
		background: #F5222D; color: #FFF; font-size: 30rpx; font-weight: 500;
		border-radius: 16rpx; border: none;
	}
	.popup-btn::after { border: none; }
</style>
