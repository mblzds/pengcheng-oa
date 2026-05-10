<template>
	<view class="page">
		<view class="card" v-if="notice">
			<view class="title">{{ notice.title || '系统通知' }}</view>
			<view class="meta">
				<text class="meta-item" v-if="notice.publishTime || notice.createTime">{{ notice.publishTime || notice.createTime }}</text>
				<text class="meta-item" v-if="notice.creatorName">{{ notice.creatorName }}</text>
				<text class="meta-tag" v-if="typeLabel">{{ typeLabel }}</text>
			</view>
			<view class="content">
				<rich-text v-if="hasHtml" :nodes="resolvedContent"></rich-text>
				<text v-else class="plain">{{ notice.content || '（无正文）' }}</text>
			</view>
		</view>
		<view class="empty" v-else-if="!loading">
			<text>未找到该通知</text>
		</view>
	</view>
</template>

<script>
	import { getNoticeDetail, readNotice } from '../../utils/api.js'
	import { joinBaseUrl } from '../../utils/config.js'

	// 富文本里的 <img src="/api/files/..."> 是后端返回的相对路径，
	// 在小程序 <rich-text> 里会被解析到 devtools/页面 origin 上而非后端，
	// 这里把站内相对路径改写为带后端域名的绝对 URL。
	function resolveHtmlAssetUrls(html) {
		if (!html) return ''
		return html.replace(/(<img\b[^>]*?\bsrc\s*=\s*)(["'])([^"']+)\2/gi, (match, prefix, quote, url) => {
			if (/^(https?:|data:|wxfile:|file:|blob:)/i.test(url)) return match
			return `${prefix}${quote}${joinBaseUrl(url)}${quote}`
		})
	}

	export default {
		data() {
			return {
				notice: null,
				loading: true
			}
		},
		computed: {
			hasHtml() {
				const c = this.notice?.content || ''
				return /<[a-z][^>]*>/i.test(c)
			},
			resolvedContent() {
				return resolveHtmlAssetUrls(this.notice?.content || '')
			},
			typeLabel() {
				const map = { 1: '通知', 2: '公告', 3: '系统', 4: '业务' }
				return this.notice?.type ? (map[this.notice.type] || '') : ''
			}
		},
		onLoad(options) {
			const id = options?.id
			if (!id) {
				this.loading = false
				return
			}
			this.fetchDetail(id)
			readNotice(id).catch(() => {})
		},
		methods: {
			async fetchDetail(id) {
				try {
					const res = await getNoticeDetail(id)
					this.notice = res.data || null
				} catch (err) {
					console.error('加载通知详情失败:', err)
				} finally {
					this.loading = false
				}
			}
		}
	}
</script>

<style lang="scss" scoped>
	.page { min-height: 100vh; min-height: 100dvh; background: #F0F0F0; padding: 20rpx; }
	.card {
		background: #FFF; border-radius: 16rpx; padding: 32rpx 28rpx;
	}
	.title {
		font-size: 34rpx; font-weight: 600; color: #1A1A1A; line-height: 1.5;
	}
	.meta {
		display: flex; flex-wrap: wrap; align-items: center; gap: 16rpx;
		margin-top: 16rpx; padding-bottom: 24rpx; border-bottom: 1rpx solid #F0F0F0;
		.meta-item { font-size: 24rpx; color: #999; }
		.meta-tag {
			font-size: 22rpx; color: #07C160; background: rgba(7, 193, 96, 0.08);
			padding: 4rpx 12rpx; border-radius: 8rpx;
		}
	}
	.content {
		margin-top: 24rpx;
		font-size: 28rpx; color: #333; line-height: 1.7;
		.plain { white-space: pre-wrap; word-break: break-word; }
	}
	.empty {
		padding: 120rpx 0; text-align: center;
		text { font-size: 26rpx; color: #999; }
	}
</style>
