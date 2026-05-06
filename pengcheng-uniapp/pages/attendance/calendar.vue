<template>
	<view class="page">
		<!-- 月份切换 -->
		<view class="month-bar">
			<view class="month-arrow" @tap="prevMonth"><u-icon name="arrow-left" color="#333" size="16"></u-icon></view>
			<text class="month-text">{{ year }}年{{ month }}月</text>
			<view class="month-arrow" @tap="nextMonth"><u-icon name="arrow-right" color="#333" size="16"></u-icon></view>
		</view>

		<!-- 星期头 -->
		<view class="week-header">
			<text class="week-cell" v-for="w in ['日','一','二','三','四','五','六']" :key="w">{{ w }}</text>
		</view>

		<!-- 日历网格 -->
		<view class="calendar-grid">
			<view class="day-cell" v-for="(day, idx) in calendarDays" :key="idx"
				:class="{ empty: !day.date, today: day.isToday }">
				<text class="day-num" v-if="day.date">{{ day.date }}</text>
				<view class="day-dot" v-if="day.status" :class="'dot-' + day.status"></view>
				<text class="day-label" v-if="day.statusLabel">{{ day.statusLabel }}</text>
			</view>
		</view>

		<!-- 图例 -->
		<view class="legend">
			<view class="legend-item" v-for="l in legends" :key="l.key">
				<view class="legend-dot" :class="'dot-' + l.key"></view>
				<text class="legend-text">{{ l.label }}</text>
			</view>
		</view>

		<!-- 月度汇总 -->
		<view class="summary-card">
			<view class="summary-title">月度汇总</view>
			<view class="summary-grid">
				<view class="summary-item" v-for="s in summaryItems" :key="s.label">
					<text class="summary-value">{{ s.value }}</text>
					<text class="summary-label">{{ s.label }}</text>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import { getAttendanceMonthly, getAttendanceRecords } from '../../utils/api.js'

	export default {
		data() {
			const now = new Date()
			return {
				year: now.getFullYear(),
				month: now.getMonth() + 1,
				records: {},
				summary: {},
				legends: [
					{ key: 'normal', label: '正常' },
					{ key: 'late', label: '迟到' },
					{ key: 'early', label: '早退' },
					{ key: 'absent', label: '缺勤' },
					{ key: 'leave', label: '请假' }
				]
			}
		},
		computed: {
			calendarDays() {
				const firstDay = new Date(this.year, this.month - 1, 1).getDay()
				const daysInMonth = new Date(this.year, this.month, 0).getDate()
				const today = new Date()
				const days = []
				for (let i = 0; i < firstDay; i++) days.push({ date: 0 })
				for (let d = 1; d <= daysInMonth; d++) {
					const key = `${this.year}-${String(this.month).padStart(2, '0')}-${String(d).padStart(2, '0')}`
					const rec = this.records[key] || {}
					const isToday = today.getFullYear() === this.year && today.getMonth() + 1 === this.month && today.getDate() === d
					const statusMap = { normal: '正常', late: '迟到', early: '早退', absent: '缺勤', leave: '请假' }
					const isPastDate = new Date(this.year, this.month - 1, d) < new Date(today.getFullYear(), today.getMonth(), today.getDate())
					days.push({
						date: d, isToday,
						status: rec.status || (isPastDate ? 'absent' : ''),
						statusLabel: statusMap[rec.status || (isPastDate ? 'absent' : '')] || ''
					})
				}
				return days
			},
			summaryItems() {
				const s = this.summary
				return [
					{ label: '出勤', value: s.attendanceDays || 0 },
					{ label: '迟到', value: s.lateTimes || 0 },
					{ label: '早退', value: s.earlyLeaveTimes || 0 },
					{ label: '缺勤', value: Math.max((new Date(this.year, this.month, 0).getDate()) - (s.attendanceDays || 0) - (s.leaveDays || 0), 0) },
					{ label: '请假', value: s.leaveDays || 0 }
				]
			}
		},
		onShow() { this.loadData() },
		methods: {
			prevMonth() {
				if (this.month === 1) { this.year--; this.month = 12 } else { this.month-- }
				this.loadData()
			},
			nextMonth() {
				if (this.month === 12) { this.year++; this.month = 1 } else { this.month++ }
				this.loadData()
			},
			async loadData() {
				try {
					const [monthlyRes, recordRes] = await Promise.all([
						getAttendanceMonthly({ year: this.year, month: this.month }),
						getAttendanceRecords({ year: this.year, month: this.month })
					])
					this.summary = monthlyRes.data || {}
					const dailyRecords = recordRes.data || []
					const map = {}
					dailyRecords.forEach(r => {
						if (!r.attendanceDate) return
						let status = ''
						if (r.clockInStatus === 2) {
							status = 'late'
						} else if (r.clockOutStatus === 2) {
							status = 'early'
						} else if (r.clockInTime || r.clockOutTime) {
							status = 'normal'
						}
						map[r.attendanceDate] = { status }
					})
					this.records = map
				} catch (err) { console.error(err) }
			}
		}
	}
</script>

<style lang="scss" scoped>
	.page { min-height: 100vh; min-height: 100dvh; background: #F0F0F0; }
	.month-bar {
		display: flex; align-items: center; justify-content: center;
		padding: 24rpx; background: #FFF; gap: 32rpx;
	}
	.month-arrow { padding: 8rpx; }
	.month-text { font-size: 30rpx; font-weight: 600; color: #1A1A1A; }
	.week-header {
		display: flex; background: #FFF; padding: 12rpx 0; border-bottom: 1rpx solid #F0F0F0;
	}
	.week-cell {
		flex: 1; text-align: center; font-size: 24rpx; color: #999;
	}
	.calendar-grid {
		display: flex; flex-wrap: wrap; background: #FFF; padding: 8rpx 0 16rpx;
	}
	.day-cell {
		width: calc(100% / 7); display: flex; flex-direction: column; align-items: center;
		padding: 12rpx 0; min-height: 80rpx;
		&.today .day-num {
			background: #07C160; color: #FFF; border-radius: 50%;
			width: 48rpx; height: 48rpx; line-height: 48rpx; text-align: center;
		}
	}
	.day-num { font-size: 26rpx; color: #333; }
	.day-dot {
		width: 10rpx; height: 10rpx; border-radius: 50%; margin-top: 4rpx;
		&.dot-normal { background: #52C41A; }
		&.dot-late { background: #FA8C16; }
		&.dot-early { background: #FADB14; }
		&.dot-absent { background: #F5222D; }
		&.dot-leave { background: #1890FF; }
	}
	.day-label { font-size: 16rpx; color: #999; margin-top: 2rpx; }
	.legend {
		display: flex; justify-content: center; gap: 24rpx; padding: 16rpx; background: #FFF;
		border-top: 1rpx solid #F0F0F0;
	}
	.legend-item { display: flex; align-items: center; gap: 6rpx; }
	.legend-dot { width: 14rpx; height: 14rpx; border-radius: 50%; }
	.legend-text { font-size: 20rpx; color: #999; }
	.summary-card {
		margin: 20rpx; background: #FFF; border-radius: 16rpx; padding: 24rpx;
	}
	.summary-title { font-size: 28rpx; font-weight: 600; color: #1A1A1A; margin-bottom: 20rpx; }
	.summary-grid { display: flex; }
	.summary-item {
		flex: 1; text-align: center;
	}
	.summary-value { font-size: 36rpx; font-weight: 700; color: #1A1A1A; display: block; }
	.summary-label { font-size: 22rpx; color: #999; margin-top: 4rpx; display: block; }
</style>
