<template>
	<view class="page">
		<scroll-view scroll-y class="form-scroll">
			<view class="form-section">
				<view class="form-item">
					<text class="form-label">带看项目 <text class="required">*</text></text>
					<input class="form-input form-search" v-model="projectKeyword" placeholder="输入项目关键字并回车搜索"
						@confirm="onProjectKeywordSearch" />
					<picker :range="projectList" range-key="projectName" @change="onProjectChange">
						<view class="picker-value">
							<text :class="{ placeholder: !form.projectIds.length }">{{ selectedProjectName || '请选择项目' }}</text>
							<u-icon name="arrow-right" color="#CCC" size="14"></u-icon>
						</view>
					</picker>
				</view>
				<view class="form-item">
					<text class="form-label">客户姓氏 <text class="required">*</text></text>
					<input class="form-input" v-model="form.customerName" placeholder="请输入客户姓氏" />
				</view>
				<view class="form-item">
					<text class="form-label">联系方式 <text class="required">*</text></text>
					<input class="form-input" v-model="form.phone" type="number" placeholder="请输入联系电话" maxlength="11" />
				</view>
				<view class="form-item">
					<text class="form-label">带看人数</text>
					<input class="form-input" v-model="form.visitCount" type="number" placeholder="请输入带看人数" />
				</view>
				<view class="form-item">
					<text class="form-label">带看时间 <text class="required">*</text></text>
					<picker mode="datetime" @change="onDateChange">
						<view class="picker-value">
							<text :class="{ placeholder: !form.visitTime }">{{ form.visitTime || '请选择日期时间' }}</text>
							<u-icon name="arrow-right" color="#CCC" size="14"></u-icon>
						</view>
					</picker>
				</view>
				<view class="form-item">
					<text class="form-label">带看公司 <text class="required">*</text></text>
					<input class="form-input form-search" v-model="allianceKeyword" placeholder="输入公司关键字并回车搜索"
						@confirm="onAllianceKeywordSearch" />
					<picker :range="allianceList" range-key="companyName" @change="onAllianceChange">
						<view class="picker-value">
							<text :class="{ placeholder: !form.allianceId }">{{ selectedAllianceName || '请选择联盟商' }}</text>
							<u-icon name="arrow-right" color="#CCC" size="14"></u-icon>
						</view>
					</picker>
				</view>
				<view class="form-item">
					<text class="form-label">经纪人姓名 <text class="required">*</text></text>
					<input class="form-input" v-model="form.agentName" placeholder="请输入经纪人姓名" />
				</view>
				<view class="form-item">
					<text class="form-label">经纪人联系方式 <text class="required">*</text></text>
					<input class="form-input" v-model="form.agentPhone" type="number" placeholder="请输入经纪人电话" maxlength="11" />
				</view>
			</view>
		</scroll-view>

		<view class="btn-wrap">
			<button class="submit-btn" :disabled="submitting" @tap="handleSubmit">
				{{ submitting ? '提交中...' : '提交报备' }}
			</button>
		</view>
	</view>
</template>

<script>
	import { reportCustomer, searchCustomerProjects, searchCustomerAlliances } from '../../utils/api.js'

	export default {
		data() {
			return {
				form: {
					projectIds: [],
					customerName: '',
					phone: '',
					visitCount: '',
					visitTime: '',
					allianceId: '',
					agentName: '',
					agentPhone: ''
				},
				projectList: [],
				allianceList: [],
				projectKeyword: '',
				allianceKeyword: '',
				selectedProjectName: '',
				selectedAllianceName: '',
				submitting: false
			}
		},
		onLoad() {
			this.loadProjects()
			this.loadAlliances()
		},
		methods: {
			async loadProjects(keyword = '') {
				try {
					const res = await searchCustomerProjects(keyword)
					this.projectList = res.data || []
				} catch (e) { this.projectList = [] }
			},
			async loadAlliances(keyword = '') {
				try {
					const res = await searchCustomerAlliances(keyword)
					this.allianceList = res.data || []
				} catch (e) { this.allianceList = [] }
			},
			onProjectChange(e) {
				const idx = e.detail.value
				const item = this.projectList[idx]
				if (item) {
					this.form.projectIds = [item.id]
					this.selectedProjectName = item.projectName
				}
			},
			onAllianceChange(e) {
				const idx = e.detail.value
				const item = this.allianceList[idx]
				if (item) {
					this.form.allianceId = item.id
					this.selectedAllianceName = item.companyName
				}
			},
			onDateChange(e) {
				this.form.visitTime = e.detail.value
			},
			onProjectKeywordSearch() {
				this.loadProjects(this.projectKeyword.trim())
			},
			onAllianceKeywordSearch() {
				this.loadAlliances(this.allianceKeyword.trim())
			},
			async handleSubmit() {
				if (!this.form.projectIds.length) return uni.showToast({ title: '请选择带看项目', icon: 'none' })
				if (!this.form.customerName.trim()) return uni.showToast({ title: '请输入客户姓氏', icon: 'none' })
				if (!this.form.phone.trim()) return uni.showToast({ title: '请输入联系方式', icon: 'none' })
				if (!this.form.visitTime) return uni.showToast({ title: '请选择带看时间', icon: 'none' })
				if (!this.form.allianceId) return uni.showToast({ title: '请选择带看公司', icon: 'none' })
				if (!this.form.agentName.trim()) return uni.showToast({ title: '请输入经纪人姓名', icon: 'none' })
				if (!this.form.agentPhone.trim()) return uni.showToast({ title: '请输入经纪人联系方式', icon: 'none' })

				this.submitting = true
				try {
					const res = await reportCustomer({
						projectIds: this.form.projectIds,
						customerName: this.form.customerName.trim(),
						phone: this.form.phone.trim(),
						visitCount: Number(this.form.visitCount || 1),
						visitTime: this.form.visitTime.replace(' ', 'T'),
						allianceId: this.form.allianceId,
						agentName: this.form.agentName.trim(),
						agentPhone: this.form.agentPhone.trim()
					})
					uni.showModal({
						title: '报备成功',
						content: `报备编号：${res.data?.reportNo || ''}`,
						showCancel: false,
						success: () => {
							uni.redirectTo({ url: '/pages/customer/list' })
						}
					})
				} catch (err) {
					if (err.code === 409) {
						uni.showModal({ title: '报备失败', content: '该客户已被报备，不可重复报备', showCancel: false })
					}
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
	.form-item {
		padding: 24rpx 0; border-bottom: 1rpx solid #F5F5F5;
		&:last-child { border-bottom: none; }
	}
	.form-label { font-size: 26rpx; color: #666; margin-bottom: 12rpx; display: block; }
	.required { color: #F5222D; }
	.form-input {
		height: 72rpx; font-size: 28rpx; color: #1A1A1A;
		border: 1rpx solid #E8E8E8; border-radius: 8rpx; padding: 0 16rpx;
	}
	.form-search { margin-bottom: 12rpx; }
	.picker-value {
		height: 72rpx; display: flex; align-items: center; justify-content: space-between;
		border: 1rpx solid #E8E8E8; border-radius: 8rpx; padding: 0 16rpx;
		font-size: 28rpx; color: #1A1A1A;
	}
	.placeholder { color: #C0C0C0; }
	.btn-wrap { padding: 20rpx 24rpx 40rpx; background: #FFF; }
	.submit-btn {
		height: 88rpx; line-height: 88rpx; background: #07C160; color: #FFF;
		font-size: 30rpx; font-weight: 500; border-radius: 16rpx; border: none;
		&[disabled] { opacity: 0.6; }
	}
	.submit-btn::after { border: none; }
</style>
