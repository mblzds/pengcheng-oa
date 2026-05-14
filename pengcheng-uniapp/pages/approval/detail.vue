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

			<!-- 票据附件：仅付款类（报销/垫佣/预付佣）有 -->
			<view class="card" v-if="detail.attachments && detail.attachments.length > 0">
				<view class="card-title">票据附件</view>
				<view class="att-grid">
					<view class="att-item" v-for="(p, i) in detail.attachments" :key="i" @tap="previewAttachment(i)">
						<image :src="resolveImg(p)" mode="aspectFill" class="att-img"></image>
					</view>
				</view>
			</view>

			<!-- 审批流转时间线（含已审批节点 + 当前节点 + 未来节点占位） -->
			<view class="card" v-if="detail.histories && detail.histories.length > 0">
				<view class="card-title">审批流程</view>
				<view class="timeline">
					<view class="timeline-item" v-for="(t, i) in detail.histories" :key="i">
						<view class="tl-dot" :class="dotClass(t, i)"></view>
						<view class="tl-line" v-if="i < detail.histories.length - 1"></view>
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
		</scroll-view>

		<!-- 底部审批按钮 -->
		<view class="action-bar" v-if="canApprove">
			<button class="reject-btn" @tap="showRejectModal = true">驳回</button>
			<button class="approve-btn" @tap="handleApprove(true)">通过</button>
		</view>

		<!-- 驳回原因弹窗（u-popup 在 mp-weixin 体验版 :show 失灵，改自写 fixed 弹层） -->
		<view v-if="showRejectModal" class="custom-popup-mask" @tap="showRejectModal = false"></view>
		<view v-if="showRejectModal" class="custom-popup-sheet">
			<view class="popup-content" @tap.stop>
				<view class="popup-title">驳回原因</view>
				<textarea class="reject-textarea" v-model="rejectReason" placeholder="请输入驳回原因" maxlength="200"></textarea>
				<button class="popup-btn" @tap="handleApprove(false)">确认驳回</button>
			</view>
		</view>
	</view>
</template>

<script>
	import { getApprovalDetail, submitApproval } from '../../utils/api.js'
	import { joinBaseUrl } from '../../utils/config.js'

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
			// 时间线节点：当前未审批节点 = 数组中最早 result==null 的项
			currentPendingIndex() {
				if (!this.detail.histories) return -1
				return this.detail.histories.findIndex(h => h.result == null)
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
			// 后端本地存储返回相对路径 /api/files/...，要拼基地址；http(s) 直接透传
			resolveImg(p) {
				if (!p) return ''
				if (/^(wxfile|file|blob|https?):/i.test(p)) return p
				return joinBaseUrl(p)
			},
			previewAttachment(idx) {
				const list = (this.detail.attachments || []).map(p => this.resolveImg(p))
				if (list.length === 0) return
				uni.previewImage({ urls: list, current: list[idx] || list[0] })
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
	/* 自写底部弹层（替代 u-popup，绕开 mp-weixin :show 失灵） */
	.custom-popup-mask {
		position: fixed; left: 0; right: 0; top: 0; bottom: 0;
		background: rgba(0, 0, 0, 0.5); z-index: 998;
	}
	.custom-popup-sheet {
		position: fixed; left: 0; right: 0; bottom: 0;
		background: #FFF; border-top-left-radius: 24rpx; border-top-right-radius: 24rpx;
		z-index: 999; max-height: 85vh; overflow-y: auto;
		padding-bottom: env(safe-area-inset-bottom);
	}

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

	/* 票据附件九宫格（沿用 expense 提交页的布局比例） */
	.att-grid { display: flex; flex-wrap: wrap; gap: 12rpx; }
	.att-item {
		width: 200rpx; height: 200rpx; border-radius: 8rpx; overflow: hidden;
		background: #F5F5F5;
	}
	.att-img { width: 100%; height: 100%; }
</style>
