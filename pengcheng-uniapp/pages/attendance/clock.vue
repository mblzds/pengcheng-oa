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

		<!-- 拍照区域（始终可见） -->
		<view class="photo-card">
			<!-- 未拍照：显示拍照入口 -->
			<view v-if="!photoPath" class="photo-placeholder" @tap="takePhoto">
				<view class="camera-icon-wrap">
					<u-icon name="camera" color="#07C160" size="36"></u-icon>
				</view>
				<text class="photo-hint-main">拍照打卡</text>
				<text class="photo-hint-sub">点击拍摄现场照片（可选）</text>
			</view>

			<!-- 已拍照：显示预览 -->
			<view v-else class="photo-preview-wrap">
				<image class="photo-preview" :src="photoPath" mode="aspectFill" @tap="previewPhoto" />
				<view class="photo-actions">
					<view class="photo-tag">
						<u-icon name="checkmark-circle-fill" color="#07C160" size="14"></u-icon>
						<text class="photo-tag-text">已拍照</text>
					</view>
					<view class="retake-btn" @tap="takePhoto">
						<u-icon name="reload" color="#666" size="14"></u-icon>
						<text class="retake-text">重拍</text>
					</view>
				</view>
			</view>
		</view>

		<!-- 打卡按钮 -->
		<view class="clock-action">
			<view class="clock-btn" :class="{ disabled: !canClock || clocking }" @tap="handleClock">
				<u-loading-icon v-if="clocking" color="#FFF" size="28"></u-loading-icon>
				<template v-else>
					<text class="clock-btn-text">{{ clockBtnText }}</text>
					<text class="clock-btn-time">{{ currentTime }}</text>
				</template>
			</view>
		</view>
	</view>
</template>

<script>
	import { clockAttendance, getAttendanceRecords, uploadAttendancePhoto } from '../../utils/api.js'
	import { gcj02ToWgs84 } from '../../utils/coordTransform.js'

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
				photoUrl: ''
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
					fail: (err) => {
						// 定位失败禁止打卡：必须先拿到位置后端才能判断是否在合规范围
						this.canClock = false
						this.markers = []
						const msg = err && err.errMsg ? err.errMsg : ''
						if (msg.includes('auth deny') || msg.includes('auth denied')) {
							this.locationError = '请在设置中开启定位权限'
							uni.showModal({
								title: '需要定位权限',
								content: '考勤打卡需要获取你的位置信息，请在小程序设置中开启「位置信息」权限',
								confirmText: '去开启',
								cancelText: '取消',
								success: (r) => {
									if (r.confirm) {
										uni.openSetting({
											success: (s) => {
												if (s.authSetting && s.authSetting['scope.userLocation']) {
													this.getLocation()
												}
											}
										})
									}
								}
							})
						} else {
							this.locationError = '定位失败，请检查手机定位是否开启'
						}
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
						this.clockInTime = (todayRecord.clockInTime || '').slice(11, 16)
						this.clockOutTime = (todayRecord.clockOutTime || '').slice(11, 16)
					} else {
						this.clockInTime = ''
						this.clockOutTime = ''
					}
				} catch (err) { console.error(err) }
			},

			// 拍照（独立操作，不触发打卡）
			async takePhoto() {
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
					this.photoUrl = '' // 重置，打卡时再上传
				} catch (err) {
					if (!err?.errMsg?.includes('cancel')) {
						uni.showToast({ title: '拍照失败，请重试', icon: 'none' })
					}
				}
			},

			previewPhoto() {
				if (this.photoPath) {
					uni.previewImage({ urls: [this.photoPath], current: this.photoPath })
				}
			},

			// 根据后端返回的状态做差异化提示
			//   clockInStatus / clockOutStatus: 1=正常 2=迟到/早退
			//   locationStatus: 1=合规 2=超出范围 NULL=未启用位置校验
			handleClockResult(record, isClockIn) {
				const tags = []
				if (isClockIn && record?.clockInStatus === 2) tags.push('迟到')
				if (!isClockIn && record?.clockOutStatus === 2) tags.push('早退')
				if (record?.locationStatus === 2) tags.push('位置超出范围')

				if (tags.length === 0) {
					uni.showToast({ title: '打卡成功', icon: 'success' })
				} else {
					uni.showModal({
						title: '已记录打卡',
						content: `存在异常：${tags.join('、')}。已为你记入考勤，可联系管理员说明情况。`,
						showCancel: false,
						confirmText: '我知道了'
					})
				}
			},

			// 打卡：若有本地照片则先上传再提交，否则直接提交
			async handleClock() {
				if (!this.canClock || this.clocking) return
				this.clocking = true
				try {
					// 有照片且尚未上传，先上传
					if (this.photoPath && !this.photoUrl) {
						uni.showLoading({ title: '上传照片…', mask: true })
						const uploadRes = await uploadAttendancePhoto(this.photoPath)
						uni.hideLoading()
						this.photoUrl = uploadRes.data || ''
					}

					const now = new Date()
					const pad = v => String(v).padStart(2, '0')
					const clockTime = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`

					// 设备 GPS 由 uni.getLocation 以 GCJ-02 取出（用于 <map> 渲染），
					// 上报后端必须转回 WGS-84 以匹配后台合规位置存储坐标系
					const [wgsLng, wgsLat] = gcj02ToWgs84(this.longitude, this.latitude)

					const isClockIn = !this.clockInTime
					const res = await clockAttendance({
						type: isClockIn ? 'in' : 'out',
						latitude: wgsLat,
						longitude: wgsLng,
						clockTime,
						photoUrl: this.photoUrl || undefined
					})

					this.handleClockResult(res?.data, isClockIn)
					// 打卡成功后清空照片，供下次重新拍
					this.photoPath = ''
					this.photoUrl = ''
					this.loadTodayRecords()
				} catch (err) {
					uni.hideLoading()
					uni.showToast({ title: err.message || '打卡失败', icon: 'none' })
					// 上传失败时重置 URL，保留本地预览供重试
					this.photoUrl = ''
				} finally {
					this.clocking = false
				}
			}
		}
	}
</script>

<style lang="scss" scoped>
	.page { min-height: 100vh; background: #F0F0F0; display: flex; flex-direction: column; }

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
	.clock-map { width: 100%; height: 360rpx; }
	.location-tip {
		position: absolute; bottom: 16rpx; left: 16rpx; right: 16rpx;
		background: rgba(245,34,45,0.1); border-radius: 8rpx; padding: 12rpx 16rpx;
		display: flex; align-items: center; gap: 8rpx;
	}
	.tip-text { font-size: 24rpx; color: #F5222D; }

	/* 拍照区域 */
	.photo-card {
		margin: 20rpx; background: #FFF; border-radius: 16rpx; overflow: hidden;
	}

	.photo-placeholder {
		display: flex; flex-direction: column; align-items: center; justify-content: center;
		padding: 40rpx 0;
		&:active { background: #F8F8F8; }
	}
	.camera-icon-wrap {
		width: 100rpx; height: 100rpx; border-radius: 50%;
		background: rgba(7, 193, 96, 0.08);
		display: flex; align-items: center; justify-content: center;
		margin-bottom: 16rpx;
	}
	.photo-hint-main { font-size: 28rpx; color: #1A1A1A; font-weight: 500; }
	.photo-hint-sub { font-size: 22rpx; color: #999; margin-top: 8rpx; }

	.photo-preview-wrap { position: relative; }
	.photo-preview { width: 100%; height: 380rpx; display: block; }
	.photo-actions {
		display: flex; align-items: center; justify-content: space-between;
		padding: 16rpx 24rpx;
	}
	.photo-tag {
		display: flex; align-items: center; gap: 8rpx;
	}
	.photo-tag-text { font-size: 24rpx; color: #07C160; }
	.retake-btn {
		display: flex; align-items: center; gap: 8rpx;
		padding: 10rpx 20rpx; border: 1rpx solid #E0E0E0; border-radius: 40rpx;
		&:active { background: #F5F5F5; }
	}
	.retake-text { font-size: 24rpx; color: #666; }

	/* 打卡按钮 */
	.clock-action {
		flex: 1; display: flex; align-items: center; justify-content: center; padding: 40rpx 0;
	}
	.clock-btn {
		width: 280rpx; height: 280rpx; border-radius: 50%;
		background: linear-gradient(180deg, #07C160, #059B4B);
		display: flex; flex-direction: column; align-items: center; justify-content: center;
		box-shadow: 0 8rpx 40rpx rgba(7,193,96,0.4);
		&.disabled { background: linear-gradient(180deg, #CCC, #AAA); box-shadow: none; }
		&:active:not(.disabled) { transform: scale(0.96); }
	}
	.clock-btn-text { font-size: 30rpx; color: #FFF; font-weight: 600; }
	.clock-btn-time { font-size: 40rpx; color: #FFF; font-weight: 700; margin-top: 8rpx; }
</style>
