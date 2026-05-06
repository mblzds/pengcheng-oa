<template>
	<view class="page">
		<view class="header-wrap">
			<view class="status-bar" :style="{ height: statusBarHeight + 'px' }"></view>
			<view class="nav-bar">
				<view class="nav-back" @tap="goBack"><u-icon name="arrow-left" color="#FFF" size="18"></u-icon></view>
				<text class="nav-title">考勤打卡</text>
				<view class="nav-placeholder"></view>
			</view>
		</view>

		<!-- 当日打卡状态 -->
		<view class="status-card">
			<view class="status-row">
				<view class="status-item">
					<text class="status-label">上班打卡</text>
					<text class="status-time">{{ clockInTime || '未打卡' }}</text>
				</view>
				<view class="status-divider"></view>
				<view class="status-item">
					<text class="status-label">下班打卡</text>
					<text class="status-time">{{ clockOutTime || '未打卡' }}</text>
				</view>
			</view>
		</view>

		<!-- 地图 -->
		<view class="map-section">
			<map class="clock-map" :latitude="latitude" :longitude="longitude" :markers="markers"
				:scale="16" show-location></map>
			<view class="location-tip" v-if="locationError">
				<u-icon name="info-circle" color="#F5222D" size="14"></u-icon>
				<text class="tip-text">{{ locationError }}</text>
			</view>
		</view>

		<!-- 拍照预览 -->
		<view class="camera-section" v-if="photoPath">
			<image class="photo-preview" :src="photoPath" mode="aspectFill" />
			<view class="photo-actions">
				<n-button size="small" @tap="retakePhoto">重拍</n-button>
			</view>
		</view>

		<!-- 打卡按钮 -->
		<view class="clock-action">
			<view class="clock-btn" :class="{ disabled: !canClock }" @tap="handleClock">
				<text class="clock-btn-text">{{ clockBtnText }}</text>
				<text class="clock-btn-time">{{ currentTime }}</text>
			</view>
		</view>
	</view>
	
	<!-- 拍照提示弹窗 -->
	<n-modal v-model:show="showCameraTip" preset="dialog" title="拍照打卡" :closable="false">
		<text>是否需要拍照记录打卡？</text>
		<template #action>
			<n-button @click="confirmClock(false)">跳过拍照</n-button>
			<n-button type="primary" @click="takePhotoAndClock">拍照打卡</n-button>
		</template>
	</n-modal>
</template>

<script>
	import { clockAttendance, getAttendanceRecords } from '../../utils/api.js'

	export default {
		data() {
			return {
				statusBarHeight: 20,
				latitude: 39.908823,
				longitude: 116.397470,
				markers: [],
				clockInTime: '',
				clockOutTime: '',
				currentTime: '',
				canClock: false,
				locationError: '',
				clocking: false,
				timer: null,
				photoPath: '',
				showCameraTip: false,
				pendingClockType: ''
			}
		},
		computed: {
			clockBtnText() {
				if (this.locationError) return '定位失败'
				return this.clockInTime ? '下班打卡' : '上班打卡'
			}
		},
		onLoad() {
			const sysInfo = uni.getSystemInfoSync()
			this.statusBarHeight = sysInfo.statusBarHeight || 20
		},
		onShow() {
			this.getLocation()
			this.loadTodayRecords()
			this.startTimer()
		},
		onHide() {
			this.stopTimer()
		},
		methods: {
			goBack() { uni.navigateBack() },
			startTimer() {
				this.updateTime()
				this.timer = setInterval(() => this.updateTime(), 1000)
			},
			stopTimer() {
				if (this.timer) { clearInterval(this.timer); this.timer = null }
			},
			updateTime() {
				const now = new Date()
				this.currentTime = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`
			},
			getLocation() {
				this.locationError = ''
				uni.getLocation({
					type: 'gcj02',
					success: (res) => {
						this.latitude = res.latitude
						this.longitude = res.longitude
						this.canClock = true
						this.markers = [{
							id: 1, latitude: res.latitude, longitude: res.longitude,
							iconPath: '/static/tabbar/workbench-active.png',
							width: 30, height: 30
						}]
					},
					fail: () => {
						this.locationError = '请开启定位权限'
						this.canClock = false
					}
				})
			},
			async loadTodayRecords() {
				try {
					const now = new Date()
					const res = await getAttendanceRecords({
						year: now.getFullYear(),
						month: now.getMonth() + 1
					})
					const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
					const records = res.data || []
					const todayRecord = records.find(r => r.attendanceDate === today)
					if (todayRecord) {
						this.clockInTime = (todayRecord.clockInTime || '').replace('T', ' ').slice(11, 16)
						this.clockOutTime = (todayRecord.clockOutTime || '').replace('T', ' ').slice(11, 16)
					} else {
						this.clockInTime = ''
						this.clockOutTime = ''
					}
				} catch (err) { console.error(err) }
			},
			async handleClock() {
				if (!this.canClock || this.clocking) return
				this.pendingClockType = this.clockInTime ? 'out' : 'in'
				this.showCameraTip = true
			},
			async confirmClock(withPhoto) {
				this.showCameraTip = false
				if (withPhoto) {
					this.takePhotoAndClock()
				} else {
					await this.submitClock(null)
				}
			},
			async takePhotoAndClock() {
				try {
					const res = await new Promise((resolve, reject) => {
						uni.chooseImage({
							count: 1,
							sizeType: ['compressed'],
							sourceType: ['camera'],
							success: resolve,
							fail: reject
						})
					})
					this.photoPath = res.tempFilePaths[0]
					await this.submitClock(res.tempFiles[0])
				} catch (err) {
					uni.showToast({ title: '拍照取消', icon: 'none' })
				}
			},
			async submitClock(photoFile) {
				if (this.clocking) return
				this.clocking = true
				try {
					const now = new Date()
					const formData = {
						type: this.pendingClockType,
						latitude: this.latitude,
						longitude: this.longitude,
						clockTime: `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}T${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`
					}
					if (photoFile) {
						await this.uploadAndClock(formData, photoFile)
					} else {
						await clockAttendance(formData)
					}
					uni.showToast({ title: '打卡成功', icon: 'success' })
					this.photoPath = ''
					this.loadTodayRecords()
				} catch (err) {
					uni.showToast({ title: err.message || '打卡失败', icon: 'none' })
				} finally {
					this.clocking = false
				}
			},
			async uploadAndClock(formData, photoFile) {
				return new Promise((resolve, reject) => {
					uni.uploadFile({
						url: '/api/attendance/clock',
						filePath: photoFile.path,
						name: 'photo',
						formData: formData,
						success: (res) => {
							const data = JSON.parse(res.data)
							if (data.code === 200) resolve(data)
							else reject(new Error(data.message || '打卡失败'))
						},
						fail: (err) => reject(new Error('上传失败'))
					})
				})
			},
			retakePhoto() {
				this.photoPath = ''
				this.takePhotoAndClock()
			},
			goBack() { uni.navigateBack() },
		}
	}
</script>

<style lang="scss" scoped>
	.page { min-height: 100vh; min-height: 100dvh; background: #F0F0F0; display: flex; flex-direction: column; }
	.header-wrap { background: linear-gradient(160deg, #059B4B 0%, #07C160 45%, #2BD373 100%); }
	.status-bar { width: 100%; }
	.nav-bar {
		height: 88rpx; display: flex; align-items: center; justify-content: space-between; padding: 0 24rpx;
	}
	.nav-back { width: 60rpx; }
	.nav-title { font-size: 34rpx; font-weight: 600; color: #FFF; }
	.nav-placeholder { width: 60rpx; }

	.status-card {
		margin: 20rpx; background: #FFF; border-radius: 16rpx; padding: 32rpx;
	}
	.status-row { display: flex; align-items: center; }
	.status-item { flex: 1; text-align: center; }
	.status-label { font-size: 24rpx; color: #999; display: block; margin-bottom: 8rpx; }
	.status-time { font-size: 32rpx; font-weight: 600; color: #1A1A1A; }
	.status-divider { width: 1rpx; height: 60rpx; background: #E8E8E8; }

	.map-section { margin: 0 20rpx; border-radius: 16rpx; overflow: hidden; position: relative; }
	.clock-map { width: 100%; height: 400rpx; }
	.location-tip {
		position: absolute; bottom: 16rpx; left: 16rpx; right: 16rpx;
		background: rgba(245,34,45,0.1); border-radius: 8rpx; padding: 12rpx 16rpx;
		display: flex; align-items: center; gap: 8rpx;
	}
	.tip-text { font-size: 24rpx; color: #F5222D; }

	.clock-action {
		flex: 1; display: flex; align-items: center; justify-content: center; padding: 40rpx 0;
	}
	.clock-btn {
		width: 280rpx; height: 280rpx; border-radius: 50%;
		background: linear-gradient(180deg, #07C160, #059B4B);
		display: flex; flex-direction: column; align-items: center; justify-content: center;
		box-shadow: 0 8rpx 40rpx rgba(7,193,96,0.4);
		&.disabled { background: linear-gradient(180deg, #CCC, #AAA); box-shadow: none; }
		&:active { transform: scale(0.96); }
	}
	.clock-btn-text { font-size: 30rpx; color: #FFF; font-weight: 600; }
	.clock-btn-time { font-size: 40rpx; color: #FFF; font-weight: 700; margin-top: 8rpx; }
	
	.camera-section {
		margin: 20rpx; background: #FFF; border-radius: 16rpx; overflow: hidden;
	}
	.photo-preview { width: 100%; height: 400rpx; }
	.photo-actions {
		display: flex; justify-content: flex-end; padding: 12rpx;
	}
</style>
