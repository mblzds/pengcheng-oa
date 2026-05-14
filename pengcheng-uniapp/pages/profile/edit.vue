<template>
	<view class="page">
		<!-- 头像区域 -->
		<view class="section">
			<view class="cell cell-avatar" @tap="handleChangeAvatar">
				<text class="cell-label">头像</text>
				<view class="cell-right">
					<image class="avatar-img" :src="resolveAvatar(form.avatar)" mode="aspectFill"></image>
					<pc-icon name="arrow-right" color="#CCCCCC" size="14"></pc-icon>
				</view>
			</view>
		</view>

		<!-- 基本信息 -->
		<view class="section">
			<view class="cell" @tap="editNickname">
				<text class="cell-label">昵称</text>
				<view class="cell-right">
					<text class="cell-value">{{ form.nickname || '未设置' }}</text>
					<pc-icon name="arrow-right" color="#CCCCCC" size="14"></pc-icon>
				</view>
			</view>
			<view class="cell" @tap="showGenderPicker">
				<text class="cell-label">性别</text>
				<view class="cell-right">
					<text class="cell-value">{{ genderText }}</text>
					<pc-icon name="arrow-right" color="#CCCCCC" size="14"></pc-icon>
				</view>
			</view>
			<view class="cell" @tap="editPhone">
				<text class="cell-label">手机号</text>
				<view class="cell-right">
					<text class="cell-value">{{ form.phone || '未设置' }}</text>
					<pc-icon name="arrow-right" color="#CCCCCC" size="14"></pc-icon>
				</view>
			</view>
			<view class="cell" @tap="editEmail">
				<text class="cell-label">邮箱</text>
				<view class="cell-right">
					<text class="cell-value">{{ form.email || '未设置' }}</text>
					<pc-icon name="arrow-right" color="#CCCCCC" size="14"></pc-icon>
				</view>
			</view>
		</view>

		<!-- 账号信息（只读） -->
		<view class="section">
			<view class="cell">
				<text class="cell-label">用户ID</text>
				<view class="cell-right">
					<text class="cell-value cell-readonly">{{ form.id || '--' }}</text>
				</view>
			</view>
			<view class="cell">
				<text class="cell-label">用户名</text>
				<view class="cell-right">
					<text class="cell-value cell-readonly">{{ form.username || '--' }}</text>
				</view>
			</view>
		</view>

		<!-- 性别选择弹窗（u-action-sheet 在 mp-weixin 体验版 :show 失灵，改自写 fixed 弹层） -->
		<view v-if="genderSheetVisible" class="custom-popup-mask" @tap="genderSheetVisible = false"></view>
		<view v-if="genderSheetVisible" class="custom-popup-sheet">
			<view class="action-sheet" @tap.stop>
				<view class="action-item" v-for="item in genderActions" :key="item.value" @tap="onGenderSelect(item)">
					<text>{{ item.name }}</text>
				</view>
				<view class="action-cancel" @tap="genderSheetVisible = false">
					<text>取消</text>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import { getAppProfile, updateAppProfile, uploadFile } from '../../utils/api.js'
	import { getUserInfo, setUserInfo } from '../../utils/auth.js'
	import { resolveAvatar } from '../../utils/config.js'
	import { ensurePrivacyAuth } from '../../utils/privacy.js'

	export default {
		data() {
			return {
				form: {
					id: '',
					username: '',
					nickname: '',
					avatar: '',
					gender: 0,
					phone: '',
					email: ''
				},
				genderSheetVisible: false,
				genderActions: [
					{ name: '男', value: 1 },
					{ name: '女', value: 2 },
					{ name: '保密', value: 0 }
				]
			}
		},
		computed: {
			genderText() {
				const map = { 0: '保密', 1: '男', 2: '女' }
				return map[this.form.gender] || '保密'
			}
		},
		onLoad() {
			this.loadProfile()
		},
		methods: {
			resolveAvatar,
			async loadProfile() {
				try {
					uni.showLoading({ title: '加载中...' })
					const res = await getAppProfile()
					if (res.data) {
						this.form = {
							id: res.data.id || '',
							username: res.data.username || '',
							nickname: res.data.nickname || '',
							avatar: res.data.avatar || '',
							gender: res.data.gender ?? 0,
							phone: res.data.phone || '',
							email: res.data.email || ''
						}
					}
				} catch (err) {
					uni.showToast({ title: '加载失败', icon: 'none' })
				} finally {
					uni.hideLoading()
				}
			},

			async handleChangeAvatar() {
				const agreed = await ensurePrivacyAuth()
				if (!agreed) return
				uni.chooseImage({
					count: 1,
					sizeType: ['compressed'],
					success: async (res) => {
						uni.showLoading({ title: '上传中...' })
						try {
							const r = await uploadFile(res.tempFilePaths[0])
							const url = r.data?.url || r.data
							await updateAppProfile({ avatar: url })
							this.form.avatar = url
							// 同步更新本地缓存
							const userInfo = getUserInfo() || {}
							userInfo.avatar = url
							setUserInfo(userInfo)
							uni.showToast({ title: '头像已更新', icon: 'success' })
						} catch (e) {
							uni.showToast({ title: '上传失败', icon: 'none' })
						} finally {
							uni.hideLoading()
						}
					}
				})
			},

			editNickname() {
				uni.showModal({
					title: '修改昵称',
					editable: true,
					placeholderText: '请输入新昵称',
					content: this.form.nickname || '',
					success: async (res) => {
						if (res.confirm && res.content && res.content.trim()) {
							await this.saveField({ nickname: res.content.trim() })
							this.form.nickname = res.content.trim()
							// 同步本地缓存
							const userInfo = getUserInfo() || {}
							userInfo.nickname = res.content.trim()
							setUserInfo(userInfo)
						}
					}
				})
			},

			showGenderPicker() {
				this.genderSheetVisible = true
			},

			async onGenderSelect(item) {
				this.genderSheetVisible = false
				await this.saveField({ gender: item.value })
				this.form.gender = item.value
			},

			editPhone() {
				uni.showModal({
					title: '修改手机号',
					editable: true,
					placeholderText: '请输入手机号',
					content: this.form.phone || '',
					success: async (res) => {
						if (res.confirm && res.content) {
							const phone = res.content.trim()
							if (phone && !/^1[3-9]\d{9}$/.test(phone)) {
								uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
								return
							}
							await this.saveField({ phone })
							this.form.phone = phone
						}
					}
				})
			},

			editEmail() {
				uni.showModal({
					title: '修改邮箱',
					editable: true,
					placeholderText: '请输入邮箱地址',
					content: this.form.email || '',
					success: async (res) => {
						if (res.confirm && res.content) {
							const email = res.content.trim()
							if (email && !/^[\w.-]+@[\w.-]+\.\w+$/.test(email)) {
								uni.showToast({ title: '请输入正确的邮箱', icon: 'none' })
								return
							}
							await this.saveField({ email })
							this.form.email = email
						}
					}
				})
			},

			async saveField(data) {
				try {
					uni.showLoading({ title: '保存中...' })
					await updateAppProfile(data)
					uni.showToast({ title: '已更新', icon: 'success' })
				} catch (e) {
					uni.showToast({ title: e.message || '保存失败', icon: 'none' })
					throw e
				} finally {
					uni.hideLoading()
				}
			}
		}
	}
</script>

<style lang="scss" scoped>
	.page {
		min-height: 100vh; min-height: 100dvh;
		background: #F0F0F0;
		padding-top: 20rpx;
	}

	/* 自写底部弹层（替代 u-action-sheet / u-popup，绕开 mp-weixin :show 失灵） */
	.custom-popup-mask {
		position: fixed; left: 0; right: 0; top: 0; bottom: 0;
		background: rgba(0, 0, 0, 0.5); z-index: 998;
	}
	.custom-popup-sheet {
		position: fixed; left: 0; right: 0; bottom: 0;
		background: #F5F5F5; border-top-left-radius: 24rpx; border-top-right-radius: 24rpx;
		z-index: 999; padding-bottom: env(safe-area-inset-bottom);
	}
	.action-sheet { padding: 0; }
	.action-item {
		height: 100rpx; line-height: 100rpx; text-align: center;
		font-size: 32rpx; color: #1A1A1A; background: #FFF;
		border-bottom: 1rpx solid #F2F2F2;
		&:active { background: #F7F7F7; }
	}
	.action-cancel {
		height: 100rpx; line-height: 100rpx; text-align: center;
		font-size: 32rpx; color: #666; background: #FFF;
		margin-top: 12rpx;
		&:active { background: #F7F7F7; }
	}

	.section {
		margin: 0 24rpx 20rpx;
		background: #FFFFFF;
		border-radius: 16rpx;
		overflow: hidden;
		box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.03);
	}

	.cell {
		display: flex;
		align-items: center;
		padding: 28rpx 28rpx;
		position: relative;

		&:active {
			background: #F7F7F7;
		}

		& + .cell::before {
			content: '';
			position: absolute;
			top: 0;
			left: 28rpx;
			right: 28rpx;
			height: 1rpx;
			background: #F2F2F2;
		}
	}

	.cell-avatar {
		padding: 20rpx 28rpx;
	}

	.cell-label {
		font-size: 28rpx;
		color: #1A1A1A;
		flex-shrink: 0;
		width: 140rpx;
	}

	.cell-right {
		flex: 1;
		display: flex;
		align-items: center;
		justify-content: flex-end;
	}

	.cell-value {
		font-size: 28rpx;
		color: #666666;
		margin-right: 12rpx;
		max-width: 400rpx;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.cell-readonly {
		color: #BBBBBB;
	}

	.avatar-img {
		width: 96rpx;
		height: 96rpx;
		border-radius: 50%;
		background: #E0E0E0;
		margin-right: 12rpx;
	}
</style>
