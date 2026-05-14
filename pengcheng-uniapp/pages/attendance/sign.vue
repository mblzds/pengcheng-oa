<template>
	<view class="page">
		<!-- 签到前：扫码提示 -->
		<view class="scan-section" v-if="!signResult">
			<view class="scan-icon-wrap">
				<u-icon name="scan" color="#07C160" size="64"></u-icon>
			</view>
			<text class="scan-title">扫码签到</text>
			<text class="scan-desc">请扫描项目现场二维码进行签到</text>
			<button class="scan-btn" @tap="handleScan">
				<u-icon name="scan" color="#FFF" size="18"></u-icon>
				<text class="scan-btn-text">打开扫码</text>
			</button>
		</view>

		<!-- 签到成功结果 -->
		<view class="result-section" v-else>
			<view class="result-card">
				<view class="result-icon">
					<u-icon name="checkmark-circle" color="#07C160" size="48"></u-icon>
				</view>
				<text class="result-title">签到成功</text>
				<view class="result-info">
					<view class="result-row">
						<text class="result-label">项目名称</text>
						<text class="result-value">{{ signResult.projectName || '--' }}</text>
					</view>
					<view class="result-row">
						<text class="result-label">签到时间</text>
						<text class="result-value">{{ formatSignTime(signResult.signTime) }}</text>
					</view>
					<view class="result-row">
						<text class="result-label">位置描述</text>
						<text class="result-value">{{ signResult.locationDesc || '--' }}</text>
					</view>
				</view>
			</view>
			<button class="again-btn" @tap="resetSign">重新签到</button>
		</view>
	</view>
</template>

<script>
	import { signAttendance } from '../../utils/api.js'

	export default {
		data() {
			return {
				signResult: null
			}
		},
		methods: {
			handleScan() {
				uni.scanCode({
					scanType: ['qrCode'],
					success: (scanRes) => {
						const projectCode = scanRes.result
						if (!projectCode) {
							return uni.showToast({ title: '无效的签到二维码', icon: 'none' })
						}
						this.doSign(projectCode)
					},
					fail: () => {
						uni.showToast({ title: '扫码取消', icon: 'none' })
					}
				})
			},
			async doSign(projectCode) {
				uni.showLoading({ title: '签到中...' })
				try {
					const locRes = await new Promise((resolve, reject) => {
						uni.getLocation({
							type: 'gcj02',
							success: resolve,
							fail: () => reject(new Error('获取定位失败，请开启定位权限'))
						})
					})
					const res = await signAttendance({
						projectCode,
						latitude: locRes.latitude,
						longitude: locRes.longitude
					})
					this.signResult = res.data || { projectName: projectCode, signTime: new Date().toISOString(), locationDesc: '' }
				} catch (err) {
					const msg = err.message || '签到失败'
					if (err.code === 400 || msg.includes('无效') || msg.includes('not found')) {
						uni.showToast({ title: '无效的签到二维码', icon: 'none' })
					} else {
						uni.showToast({ title: msg, icon: 'none' })
					}
				} finally {
					uni.hideLoading()
				}
			},
			formatSignTime(value) {
				if (!value) return '--'
				return String(value).replace('T', ' ').slice(0, 19)
			},
			resetSign() {
				this.signResult = null
			}
		}
	}
</script>

<style lang="scss" scoped>
	.page { min-height: 100vh; min-height: 100dvh; background: #F0F0F0; }
	.scan-section {
		display: flex; flex-direction: column; align-items: center;
		padding: 120rpx 40rpx;
	}
	.scan-icon-wrap {
		width: 160rpx; height: 160rpx; border-radius: 50%;
		background: rgba(7,193,96,0.1); display: flex; align-items: center; justify-content: center;
		margin-bottom: 32rpx;
	}
	.scan-title { font-size: 36rpx; font-weight: 600; color: #1A1A1A; margin-bottom: 12rpx; }
	.scan-desc { font-size: 26rpx; color: #999; margin-bottom: 48rpx; }
	.scan-btn {
		display: flex; align-items: center; justify-content: center; gap: 12rpx;
		width: 360rpx; height: 88rpx; background: #07C160; border-radius: 44rpx; border: none;
	}
	.scan-btn-text { font-size: 30rpx; color: #FFF; font-weight: 500; }
	.scan-btn::after { border: none; }

	.result-section { padding: 60rpx 20rpx; }
	.result-card {
		background: #FFF; border-radius: 16rpx; padding: 40rpx 24rpx; text-align: center;
	}
	.result-icon { margin-bottom: 16rpx; }
	.result-title { font-size: 36rpx; font-weight: 600; color: #07C160; margin-bottom: 32rpx; display: block; }
	.result-info { text-align: left; }
	.result-row {
		display: flex; justify-content: space-between; padding: 16rpx 0;
		border-bottom: 1rpx solid #F5F5F5;
		&:last-child { border-bottom: none; }
	}
	.result-label { font-size: 26rpx; color: #999; }
	.result-value { font-size: 26rpx; color: #333; }
	.again-btn {
		margin-top: 32rpx; height: 88rpx; line-height: 88rpx;
		background: #FFF; color: #07C160; font-size: 30rpx; font-weight: 500;
		border-radius: 16rpx; border: none;
	}
	.again-btn::after { border: none; }
</style>
