<template>
	<view class="page">
		<scroll-view scroll-y class="form-scroll">
			<view class="form-section">
				<view class="form-item">
					<text class="form-label">联盟商 <text class="required">*</text></text>
					<picker :range="allianceList" range-key="companyName" @change="onAllianceChange">
						<view class="picker-value">
							<text :class="{ placeholder: !form.allianceId }">{{ selectedAllianceName || '请选择联盟商' }}</text>
							<u-icon name="arrow-right" color="#CCC" size="14"></u-icon>
						</view>
					</picker>
				</view>
				<view class="form-item">
					<text class="form-label">成交记录 <text class="required">*</text></text>
					<picker :range="dealList" range-key="label" @change="onDealChange">
						<view class="picker-value">
							<text :class="{ placeholder: !form.dealId }">{{ selectedDealLabel || '请选择成交记录' }}</text>
							<u-icon name="arrow-right" color="#CCC" size="14"></u-icon>
						</view>
					</picker>
				</view>
				<view class="form-item">
					<text class="form-label">预付金额 <text class="required">*</text></text>
					<input class="form-input" v-model="form.amount" type="digit" placeholder="请输入预付金额" />
				</view>
				<view class="form-item">
					<text class="form-label">预付原因</text>
					<textarea class="form-textarea" v-model="form.reason" placeholder="请输入预付原因" maxlength="200"></textarea>
				</view>
			</view>
		</scroll-view>
		<view class="btn-wrap">
			<button class="submit-btn" :disabled="submitting" @tap="handleSubmit">
				{{ submitting ? '提交中...' : '提交申请' }}
			</button>
		</view>
	</view>
</template>

<script>
	import { applyPrepay, getCustomerDeals, searchCustomerAlliances } from '../../utils/api.js'

	export default {
		data() {
			return {
				form: { allianceId: '', dealId: '', amount: '', reason: '' },
				allianceList: [],
				dealList: [],
				selectedAllianceName: '',
				selectedDealLabel: '',
				submitting: false
			}
		},
		onLoad() { this.loadAlliances(); this.loadDeals() },
		methods: {
			async loadAlliances() {
				try {
					const res = await searchCustomerAlliances('')
					this.allianceList = res.data || []
				} catch (e) { this.allianceList = [] }
			},
			async loadDeals() {
				try {
					const res = await getCustomerDeals()
					this.dealList = (res.data || []).map(d => ({
						...d, label: `${d.customerName || '客户'} - ${d.roomNo || '--'} ¥${d.dealAmount || 0}`
					}))
				} catch (e) { this.dealList = [] }
			},
			onAllianceChange(e) {
				const item = this.allianceList[e.detail.value]
				if (item) { this.form.allianceId = item.id; this.selectedAllianceName = item.companyName }
			},
			onDealChange(e) {
				const item = this.dealList[e.detail.value]
				if (item) { this.form.dealId = item.id; this.selectedDealLabel = item.label }
			},
			async handleSubmit() {
				if (!this.form.allianceId) return uni.showToast({ title: '请选择联盟商', icon: 'none' })
				if (!this.form.dealId) return uni.showToast({ title: '请选择成交记录', icon: 'none' })
				if (!this.form.amount) return uni.showToast({ title: '请输入预付金额', icon: 'none' })
				this.submitting = true
				try {
					await applyPrepay({
						allianceId: this.form.allianceId,
						dealId: this.form.dealId,
						amount: this.form.amount,
						reason: this.form.reason?.trim()
					})
					uni.showToast({ title: '提交成功', icon: 'success' })
					setTimeout(() => uni.redirectTo({ url: '/pages/apply/list' }), 1200)
				} catch (err) { console.error(err) }
				finally { this.submitting = false }
			}
		}
	}
</script>

<style lang="scss" scoped>
	.page { min-height: 100vh; min-height: 100dvh; background: #F0F0F0; display: flex; flex-direction: column; }
	.form-scroll { flex: 1; }
	.form-section { margin: 20rpx; background: #FFF; border-radius: 16rpx; padding: 8rpx 24rpx; }
	.form-item { padding: 24rpx 0; border-bottom: 1rpx solid #F5F5F5; &:last-child { border-bottom: none; } }
	.form-label { font-size: 26rpx; color: #666; margin-bottom: 12rpx; display: block; }
	.required { color: #F5222D; }
	.form-input {
		height: 72rpx; font-size: 28rpx; color: #1A1A1A;
		border: 1rpx solid #E8E8E8; border-radius: 8rpx; padding: 0 16rpx;
	}
	.picker-value {
		height: 72rpx; display: flex; align-items: center; justify-content: space-between;
		border: 1rpx solid #E8E8E8; border-radius: 8rpx; padding: 0 16rpx;
		font-size: 28rpx; color: #1A1A1A;
	}
	.placeholder { color: #C0C0C0; }
	.form-textarea {
		width: 100%; min-height: 120rpx; font-size: 28rpx; color: #1A1A1A;
		border: 1rpx solid #E8E8E8; border-radius: 8rpx; padding: 16rpx; box-sizing: border-box;
	}
	.btn-wrap { padding: 20rpx 24rpx 40rpx; background: #FFF; }
	.submit-btn {
		height: 88rpx; line-height: 88rpx; background: #07C160; color: #FFF;
		font-size: 30rpx; font-weight: 500; border-radius: 16rpx; border: none;
		&[disabled] { opacity: 0.6; }
	}
	.submit-btn::after { border: none; }
</style>
