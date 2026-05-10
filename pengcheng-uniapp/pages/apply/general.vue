<template>
	<view class="page">
		<scroll-view scroll-y class="form-scroll">
			<view class="form-section">
				<view class="form-item">
					<text class="form-label">类型</text>
					<view class="form-readonly">
						<text>{{ typeLabel || '通用申请' }}</text>
					</view>
				</view>
				<view class="form-item">
					<text class="form-label">标题 <text class="required">*</text></text>
					<input class="form-input" v-model="form.title" placeholder="请输入申请标题" maxlength="120" />
				</view>
				<view class="form-item">
					<text class="form-label">说明</text>
					<textarea class="form-textarea" v-model="form.description" placeholder="请补充详细说明（选填）" maxlength="500"></textarea>
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
	import { applyGeneral } from '../../utils/api.js'

	export default {
		data() {
			return {
				businessType: '',
				typeLabel: '',
				form: { title: '', description: '' },
				submitting: false
			}
		},
		onLoad(options) {
			this.businessType = options?.type || ''
			this.typeLabel = options?.label ? decodeURIComponent(options.label) : ''
			uni.setNavigationBarTitle({ title: this.typeLabel || '通用申请' })
		},
		methods: {
			async handleSubmit() {
				if (!this.businessType) return uni.showToast({ title: '业务类型缺失', icon: 'none' })
				if (!this.form.title.trim()) return uni.showToast({ title: '请输入标题', icon: 'none' })
				this.submitting = true
				try {
					await applyGeneral({
						businessType: this.businessType,
						title: this.form.title.trim(),
						description: this.form.description?.trim() || ''
					})
					uni.showToast({ title: '提交成功', icon: 'success' })
					setTimeout(() => uni.redirectTo({ url: '/pages/apply/list' }), 1200)
				} catch (err) {
					const msg = (err && (err.msg || err.message)) || '提交失败'
					uni.showToast({ title: msg, icon: 'none' })
				} finally {
					this.submitting = false
				}
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
	.form-readonly {
		height: 72rpx; display: flex; align-items: center;
		padding: 0 16rpx; background: #FAFAFA; border-radius: 8rpx;
		font-size: 28rpx; color: #1A1A1A;
	}
	.form-input {
		height: 72rpx; padding: 0 16rpx; font-size: 28rpx; color: #1A1A1A;
		border: 1rpx solid #E8E8E8; border-radius: 8rpx; box-sizing: border-box;
	}
	.form-textarea {
		width: 100%; min-height: 200rpx; font-size: 28rpx; color: #1A1A1A;
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
