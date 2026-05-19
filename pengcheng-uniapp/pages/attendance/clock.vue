<template>
	<view class="page">
		<view class="header-wrap">
			<view class="status-bar" :style="{ height: statusBarHeight + 'px' }"></view>
			<view class="nav-bar">
				<view class="nav-back" @tap="goBack"><pc-icon name="arrow-left" color="#FFF" size="18"></pc-icon></view>
				<text class="nav-title">{{ mode === 'sign' ? '考勤签到' : '考勤打卡' }}</text>
				<view class="nav-placeholder"></view>
			</view>
			<!-- 打卡 / 签到 Tab 切换（参考钉钉的二级 Tab） -->
			<view class="mode-tabs">
				<view class="mode-tab" :class="{ active: mode === 'clock' }" @tap="switchMode('clock')">
					<text class="mode-text">打卡</text>
				</view>
				<view class="mode-tab" :class="{ active: mode === 'sign' }" @tap="switchMode('sign')">
					<text class="mode-text">签到</text>
				</view>
			</view>
		</view>

		<!-- ========== 打卡模式 ========== -->
		<block v-if="mode === 'clock'">
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

			<view class="map-section">
				<map class="clock-map" :latitude="latitude" :longitude="longitude" :markers="markers"
					:scale="16" show-location></map>
				<view class="location-tip" v-if="locationError">
					<pc-icon name="info-circle" color="#F5222D" size="14"></pc-icon>
					<text class="tip-text">{{ locationError }}</text>
				</view>
			</view>

			<view class="photo-card">
				<view v-if="!photoPath" class="photo-placeholder" @tap="takePhoto">
					<view class="camera-icon-wrap">
						<pc-icon name="camera" color="#07C160" size="36"></pc-icon>
					</view>
					<text class="photo-hint-main">拍照打卡</text>
					<text class="photo-hint-sub">点击拍摄现场照片（可选）</text>
				</view>
				<view v-else class="photo-preview-wrap">
					<image class="photo-preview" :src="photoPath" mode="aspectFill" @tap="previewPhoto" />
					<view class="photo-actions">
						<view class="photo-tag">
							<pc-icon name="checkmark-circle-fill" color="#07C160" size="14"></pc-icon>
							<text class="photo-tag-text">已拍照</text>
						</view>
						<view class="retake-btn" @tap="takePhoto">
							<pc-icon name="reload" color="#666" size="14"></pc-icon>
							<text class="retake-text">重拍</text>
						</view>
					</view>
				</view>
			</view>

			<view class="clock-action">
				<view class="clock-btn" :class="{ disabled: !canClock || clocking }" @tap="handleClock">
					<u-loading-icon v-if="clocking" color="#FFF" size="28"></u-loading-icon>
					<template v-else>
						<text class="clock-btn-text">{{ clockBtnText }}</text>
						<text class="clock-btn-time">{{ currentTime }}</text>
					</template>
				</view>
			</view>
		</block>

		<!-- ========== 签到模式（参考钉钉签到：拍照必填，不显示位置） ========== -->
		<block v-else>
			<!-- 签到说明卡片：不显示位置文字，只提示拍照 -->
			<view class="sign-hint-card">
				<pc-icon name="info-circle" color="#07C160" size="16"></pc-icon>
				<text class="sign-hint-text">签到需拍摄现场照片，地点由系统自动记录</text>
			</view>

			<!-- 拍照区（签到必拍） -->
			<view class="photo-card">
				<view v-if="!signPhotoPath" class="photo-placeholder big" @tap="takeSignPhoto">
					<view class="camera-icon-wrap big">
						<pc-icon name="camera" color="#07C160" size="48"></pc-icon>
					</view>
					<text class="photo-hint-main">点击拍照</text>
					<text class="photo-hint-sub">签到必须拍摄现场照片</text>
				</view>
				<view v-else class="photo-preview-wrap">
					<image class="photo-preview" :src="signPhotoPath" mode="aspectFill" @tap="previewSignPhoto" />
					<view class="photo-actions">
						<view class="photo-tag">
							<pc-icon name="checkmark-circle-fill" color="#07C160" size="14"></pc-icon>
							<text class="photo-tag-text">已拍照</text>
						</view>
						<view class="retake-btn" @tap="takeSignPhoto">
							<pc-icon name="reload" color="#666" size="14"></pc-icon>
							<text class="retake-text">重拍</text>
						</view>
					</view>
				</view>
			</view>

			<!-- 备注输入框 -->
			<view class="remark-card">
				<text class="remark-label">备注（可选）</text>
				<textarea class="remark-input" v-model="signRemark" placeholder="补充说明，如外勤事由..."
					maxlength="200" :auto-height="true" :show-confirm-bar="false" />
				<text class="remark-counter">{{ (signRemark || '').length }}/200</text>
			</view>

			<!-- 签到按钮 -->
			<view class="clock-action">
				<view class="clock-btn" :class="{ disabled: !canSign || signing }" @tap="handleSign">
					<u-loading-icon v-if="signing" color="#FFF" size="28"></u-loading-icon>
					<template v-else>
						<text class="clock-btn-text">签到</text>
						<text class="clock-btn-time">{{ currentTime }}</text>
					</template>
				</view>
			</view>
		</block>
	</view>
</template>

<script>
	import { clockAttendance, getAttendanceRecords, uploadAttendancePhoto, quickSign } from '../../utils/api.js'
	import { gcj02ToWgs84 } from '../../utils/coordTransform.js'
	import { ensurePrivacyAuth } from '../../utils/privacy.js'

	export default {
		data() {
			return {
				statusBarHeight: 20,
				mode: 'clock', // 'clock' | 'sign'

				// 打卡
				latitude: 39.908823,
				longitude: 116.397470,
				markers: [],
				clockInTime: '',
				clockOutTime: '',
				canClock: false,
				locationError: '',
				clocking: false,
				photoPath: '',
				photoUrl: '',

				// 签到（独立状态，不与打卡共用，避免切回打卡丢失打卡照片）
				signPhotoPath: '',
				signPhotoUrl: '',
				signRemark: '',
				signing: false,
				// 签到位置悄悄获取但不显示，按需在提交时用
				signLatitude: null,
				signLongitude: null,

				currentTime: '',
				timer: null
			}
		},
		computed: {
			clockBtnText() {
				if (this.locationError) return '定位失败'
				return this.clockInTime ? '下班打卡' : '上班打卡'
			},
			// 签到必须有照片才能提交；位置可以为空（让百度逆地理失败时降级为空地点）
			canSign() {
				return !!this.signPhotoPath
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

			switchMode(m) {
				if (this.mode === m) return
				this.mode = m
			},

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

			async getLocation() {
				this.locationError = ''
				const agreed = await ensurePrivacyAuth()
				if (!agreed) {
					this.canClock = false
					this.markers = []
					this.locationError = '未同意隐私协议，无法获取定位'
					return
				}
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
						// 顺便缓存一份给签到用（签到不显示，但提交时需要 WGS-84 经纬度）
						const [wgsLng, wgsLat] = gcj02ToWgs84(res.longitude, res.latitude)
						this.signLatitude = wgsLat
						this.signLongitude = wgsLng
					},
					fail: (err) => {
						this.canClock = false
						this.markers = []
						const msg = err && err.errMsg ? err.errMsg : ''
						if (msg.includes('auth deny') || msg.includes('auth denied')) {
							this.locationError = '请在设置中开启定位权限'
							uni.showModal({
								title: '需要定位权限',
								content: '考勤打卡/签到需要获取你的位置信息，请在小程序设置中开启「位置信息」权限',
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

			// ===== 打卡相关 =====
			async takePhoto() {
				const agreed = await ensurePrivacyAuth()
				if (!agreed) return
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
					this.photoUrl = ''
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

			async handleClock() {
				if (!this.canClock || this.clocking) return
				this.clocking = true
				try {
					if (this.photoPath && !this.photoUrl) {
						uni.showLoading({ title: '上传照片…', mask: true })
						const uploadRes = await uploadAttendancePhoto(this.photoPath)
						uni.hideLoading()
						this.photoUrl = uploadRes.data || ''
					}

					const now = new Date()
					const pad = v => String(v).padStart(2, '0')
					const clockTime = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`

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
					this.photoPath = ''
					this.photoUrl = ''
					this.loadTodayRecords()
				} catch (err) {
					uni.hideLoading()
					uni.showToast({ title: err.message || '打卡失败', icon: 'none' })
					this.photoUrl = ''
				} finally {
					this.clocking = false
				}
			},

			// ===== 签到相关 =====
			async takeSignPhoto() {
				const agreed = await ensurePrivacyAuth()
				if (!agreed) return
				try {
					const res = await new Promise((resolve, reject) => {
						uni.chooseImage({
							count: 1,
							sizeType: ['compressed'],
							sourceType: ['camera'], // 强制走相机，杜绝相册作弊
							success: resolve,
							fail: reject
						})
					})
					this.signPhotoPath = res.tempFilePaths[0]
					this.signPhotoUrl = ''
				} catch (err) {
					if (!err?.errMsg?.includes('cancel')) {
						uni.showToast({ title: '拍照失败，请重试', icon: 'none' })
					}
				}
			},

			previewSignPhoto() {
				if (this.signPhotoPath) {
					uni.previewImage({ urls: [this.signPhotoPath], current: this.signPhotoPath })
				}
			},

			async handleSign() {
				if (!this.canSign || this.signing) return
				this.signing = true
				try {
					// 1. 上传照片
					if (!this.signPhotoUrl) {
						uni.showLoading({ title: '上传照片…', mask: true })
						const uploadRes = await uploadAttendancePhoto(this.signPhotoPath)
						uni.hideLoading()
						this.signPhotoUrl = uploadRes.data || ''
						if (!this.signPhotoUrl) {
							throw new Error('照片上传失败')
						}
					}
					// 2. 兜底再拿一次定位（onShow 可能因权限拿不到）
					if (this.signLatitude == null || this.signLongitude == null) {
						try {
							const locRes = await new Promise((resolve, reject) => {
								uni.getLocation({ type: 'gcj02', success: resolve, fail: reject })
							})
							const [wgsLng, wgsLat] = gcj02ToWgs84(locRes.longitude, locRes.latitude)
							this.signLatitude = wgsLat
							this.signLongitude = wgsLng
						} catch (_) {
							// 定位失败不阻塞签到，地点字段交给后端记 null
						}
					}
					// 3. 提交签到 —— 仅上报 GPS，逆地理交后端读系统配置里的 AK 完成
					await quickSign({
						photoUrl: this.signPhotoUrl,
						latitude: this.signLatitude,
						longitude: this.signLongitude,
						remark: this.signRemark || undefined
					})
					uni.showToast({ title: '签到成功', icon: 'success' })
					// 重置，方便连续签到
					this.signPhotoPath = ''
					this.signPhotoUrl = ''
					this.signRemark = ''
				} catch (err) {
					uni.hideLoading()
					uni.showToast({ title: err.message || '签到失败', icon: 'none' })
					this.signPhotoUrl = ''
				} finally {
					this.signing = false
				}
			}
		}
	}
</script>

<style lang="scss" scoped>
	.page { min-height: 100vh; background: #F0F0F0; display: flex; flex-direction: column; }

	.header-wrap { background: linear-gradient(160deg, #059B4B 0%, #07C160 45%, #2BD373 100%); padding-bottom: 8rpx; }
	.status-bar { width: 100%; }
	.nav-bar {
		height: 88rpx; display: flex; align-items: center; justify-content: space-between; padding: 0 24rpx;
	}
	.nav-back { width: 60rpx; }
	.nav-title { font-size: 34rpx; font-weight: 600; color: #FFF; }
	.nav-placeholder { width: 60rpx; }

	/* 打卡 / 签到 Tab */
	.mode-tabs {
		display: flex; justify-content: center; gap: 60rpx; padding: 0 0 24rpx;
	}
	.mode-tab {
		position: relative; padding: 12rpx 0;
		.mode-text { font-size: 30rpx; color: rgba(255,255,255,0.7); }
		&.active {
			.mode-text { color: #FFF; font-weight: 600; font-size: 32rpx; }
			&::after {
				content: ''; position: absolute; bottom: 0; left: 50%;
				width: 40rpx; height: 4rpx; background: #FFF;
				transform: translateX(-50%); border-radius: 2rpx;
			}
		}
	}

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

	.sign-hint-card {
		margin: 20rpx; padding: 20rpx 24rpx; background: rgba(7,193,96,0.08); border-radius: 12rpx;
		display: flex; align-items: center; gap: 12rpx;
	}
	.sign-hint-text { font-size: 24rpx; color: #07C160; flex: 1; }

	.photo-card {
		margin: 20rpx; background: #FFF; border-radius: 16rpx; overflow: hidden;
	}
	.photo-placeholder {
		display: flex; flex-direction: column; align-items: center; justify-content: center;
		padding: 40rpx 0;
		&.big { padding: 80rpx 0; }
		&:active { background: #F8F8F8; }
	}
	.camera-icon-wrap {
		width: 100rpx; height: 100rpx; border-radius: 50%;
		background: rgba(7, 193, 96, 0.08);
		display: flex; align-items: center; justify-content: center;
		margin-bottom: 16rpx;
		&.big { width: 140rpx; height: 140rpx; margin-bottom: 24rpx; }
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

	.remark-card {
		margin: 20rpx; background: #FFF; border-radius: 16rpx; padding: 24rpx;
	}
	.remark-label { font-size: 26rpx; color: #666; display: block; margin-bottom: 12rpx; }
	.remark-input {
		width: 100%; min-height: 80rpx; font-size: 28rpx; color: #1A1A1A;
		background: #F8F8F8; border-radius: 8rpx; padding: 16rpx; box-sizing: border-box;
	}
	.remark-counter {
		display: block; text-align: right; font-size: 22rpx; color: #999; margin-top: 8rpx;
	}

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
