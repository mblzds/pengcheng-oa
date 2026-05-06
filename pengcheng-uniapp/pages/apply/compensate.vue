<template>
	<view class="page">
		<scroll-view scroll-y class="form-scroll">
			<view class="form-section">
				<view class="form-item">
					<text class="form-label">调休日期 <text class="required">*</text></text>
					<picker mode="date" @change="e => form.compensateDate = e.detail.value">
						<view class="picker-value">
							<text :class="{ placeholder: !form.compensateDate }">{{ form.compensateDate || '请选择调休日期' }}</text>
							<u-icon name="arrow-right" color="#CCC" size="14"></u-icon>
						</view>
					</picker>
				</view>
				<view class="form-item">
					<text class="form-label">调休原因 <text class="required">*</text></text>
					<textarea class="form-textarea" v-model="form.reason" placeholder="请输入调休原因" maxlength="200"></textarea>
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
	import { applyCompensate } from '../../utils/api.js'

	export default {
		data() {
			return {
				form: { compensateDate: '', reason: '' },
				submitting: false
			}
		},
		methods: {
			async handleSubmit() {
				if (!this.form.compensateDate) return uni.showToast({ title: '请选择调休日期', icon: 'none' })
				if (!this.form.reason.trim()) return uni.showToast({ title: '请输入调休原因', icon: 'none' })
				this.submitting = true
				try {
					await applyCompensate({
						compensateDate: this.form.compensateDate,
						reason: this.form.reason.trim()
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
	.picker-value {
		height: 72rpx; display: flex; align-items: center; justify-content: space-between;
		border: 1rpx solid #E8E8E8; border-radius: 8rpx; padding: 0 16rpx;
		font-size: 28rpx; color: #1A1A1A;
	}
	.placeholder { color: #C0C0C0; }
	.form-textarea {
		width: 100%; min-height: 160rpx; font-size: 28rpx; color: #1A1A1A;
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
