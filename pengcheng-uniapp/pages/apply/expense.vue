<template>
	<view class="page">
		<scroll-view scroll-y class="form-scroll">
			<view class="form-section">
				<view class="form-item">
					<text class="form-label">报销类型 <text class="required">*</text></text>
					<picker :range="expenseTypes" range-key="label" @change="onExpenseTypeChange">
						<view class="picker-value">
							<text :class="{ placeholder: !form.expenseTypeLabel }">{{ form.expenseTypeLabel || '请选择报销类型' }}</text>
							<u-icon name="arrow-right" color="#CCC" size="14"></u-icon>
						</view>
					</picker>
				</view>
				<view class="form-item">
					<text class="form-label">报销金额 <text class="required">*</text></text>
					<input class="form-input" v-model="form.amount" type="digit" placeholder="请输入金额" />
				</view>
				<view class="form-item">
					<text class="form-label">发生时间 <text class="required">*</text></text>
					<picker mode="datetime" @change="e => form.occurTime = e.detail.value">
						<view class="picker-value">
							<text :class="{ placeholder: !form.occurTime }">{{ form.occurTime || '请选择发生时间' }}</text>
							<u-icon name="arrow-right" color="#CCC" size="14"></u-icon>
						</view>
					</picker>
				</view>
				<view class="form-item">
					<text class="form-label">费用说明</text>
					<textarea class="form-textarea" v-model="form.description" placeholder="请描述费用详情" maxlength="200"></textarea>
				</view>
				<view class="form-item">
					<text class="form-label">票据附件</text>
					<view class="image-list">
						<view class="image-item" v-for="(img, i) in attachments" :key="i">
							<image :src="img" mode="aspectFill" class="att-image"></image>
							<view class="image-del" @tap="removeImage(i)"><u-icon name="close" color="#FFF" size="10"></u-icon></view>
						</view>
						<view class="image-add" @tap="chooseImage" v-if="attachments.length < 9">
							<u-icon name="plus" color="#CCC" size="24"></u-icon>
						</view>
					</view>
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
	import { applyExpense, uploadFile } from '../../utils/api.js'

	export default {
		data() {
			return {
				form: { expenseType: '', expenseTypeLabel: '', amount: '', occurTime: '', description: '', attachments: [] },
				expenseTypes: [
					{ label: '交通费', value: 1 },
					{ label: '餐饮费', value: 2 },
					{ label: '住宿费', value: 3 },
					{ label: '办公用品', value: 4 },
					{ label: '其他', value: 5 }
				],
				attachments: [],
				submitting: false
			}
		},
		methods: {
			onExpenseTypeChange(e) {
				const item = this.expenseTypes[e.detail.value]
				if (!item) return
				this.form.expenseType = item.value
				this.form.expenseTypeLabel = item.label
			},
			chooseImage() {
				uni.chooseImage({
					count: 9 - this.attachments.length,
					sizeType: ['compressed'],
					success: async (res) => {
						for (const path of res.tempFilePaths) {
							try {
								const r = await uploadFile(path)
								const uploaded = r.data
								this.attachments.push(typeof uploaded === 'string' ? uploaded : (uploaded?.url || path))
							} catch (e) {
								this.attachments.push(path)
							}
						}
					}
				})
			},
			removeImage(idx) {
				this.attachments.splice(idx, 1)
			},
			async handleSubmit() {
				if (!this.form.expenseType) return uni.showToast({ title: '请选择报销类型', icon: 'none' })
				if (!this.form.amount) return uni.showToast({ title: '请输入报销金额', icon: 'none' })
				if (!this.form.occurTime) return uni.showToast({ title: '请选择发生时间', icon: 'none' })
				this.submitting = true
				try {
					await applyExpense({
						expenseType: this.form.expenseType,
						amount: this.form.amount,
						occurTime: this.form.occurTime.replace(' ', 'T'),
						description: this.form.description?.trim(),
						attachments: this.attachments
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
	.image-list { display: flex; flex-wrap: wrap; gap: 16rpx; }
	.image-item { position: relative; }
	.att-image { width: 160rpx; height: 160rpx; border-radius: 8rpx; background: #F5F5F5; }
	.image-del {
		position: absolute; top: -8rpx; right: -8rpx; width: 32rpx; height: 32rpx;
		border-radius: 50%; background: rgba(0,0,0,0.5);
		display: flex; align-items: center; justify-content: center;
	}
	.image-add {
		width: 160rpx; height: 160rpx; border-radius: 8rpx;
		border: 2rpx dashed #DDD; display: flex; align-items: center; justify-content: center;
	}
	.btn-wrap { padding: 20rpx 24rpx 40rpx; background: #FFF; }
	.submit-btn {
		height: 88rpx; line-height: 88rpx; background: #07C160; color: #FFF;
		font-size: 30rpx; font-weight: 500; border-radius: 16rpx; border: none;
		&[disabled] { opacity: 0.6; }
	}
	.submit-btn::after { border: none; }
</style>
