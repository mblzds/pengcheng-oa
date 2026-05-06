<template>
	<view class="page">
		<!-- 文案生成 -->
		<view class="copywriting-panel">
			<view class="panel-title">营销文案生成</view>
			<view class="panel-form">
				<input class="panel-input" v-model="copyForm.projectId" type="number" placeholder="输入项目ID" />
				<picker :range="copyTypes" range-key="label" @change="onCopyTypeChange">
					<view class="panel-picker">
						<text>{{ copyForm.typeLabel || '选择文案类型' }}</text>
					</view>
				</picker>
				<button class="panel-btn" :disabled="copyLoading" @tap="generateCopywriting">
					{{ copyLoading ? '生成中...' : '生成文案' }}
				</button>
			</view>
			<view class="panel-result" v-if="copyResult">
				<text class="result-text" selectable>{{ copyResult }}</text>
				<view class="result-copy" @tap="copyText(copyResult)">
					<u-icon name="file-text" color="#07C160" size="14"></u-icon>
					<text>复制文案</text>
				</view>
			</view>
		</view>

		<!-- 消息列表 -->
		<scroll-view scroll-y class="chat-list" :scroll-into-view="scrollToId"
			scroll-with-animation>
			<view class="chat-welcome" v-if="messages.length === 0">
				<view class="welcome-icon">
					<u-icon name="integral" color="#07C160" size="48"></u-icon>
				</view>
				<text class="welcome-title">AI 助手</text>
				<text class="welcome-desc">我是您的房产销售智能助手，可以帮您：</text>
				<view class="welcome-tags">
					<view class="tag-item" @tap="quickAsk('帮我分析本月销售数据')">销售数据分析</view>
					<view class="tag-item" @tap="quickAsk('帮我写一段楼盘营销文案')">营销文案生成</view>
					<view class="tag-item" @tap="quickAsk('本月业绩排行是怎样的')">业绩排行查询</view>
					<view class="tag-item" @tap="quickAsk('客户跟进有什么建议')">客户跟进建议</view>
				</view>
			</view>

			<view class="msg-item" v-for="(msg, idx) in messages" :key="idx"
				:id="'msg-' + idx" :class="{ 'msg-user': msg.role === 'user' }">
				<view class="msg-avatar" v-if="msg.role === 'ai'">
					<view class="avatar-ai"><u-icon name="integral" color="#FFF" size="16"></u-icon></view>
				</view>
				<view class="msg-bubble" :class="msg.role === 'user' ? 'bubble-user' : 'bubble-ai'">
					<text class="msg-text" selectable>{{ msg.content }}</text>
					<view class="msg-actions" v-if="msg.role === 'ai'">
						<view class="action-btn" @tap="copyText(msg.content)">
							<u-icon name="file-text" color="#999" size="14"></u-icon>
							<text>复制</text>
						</view>
					</view>
				</view>
				<view class="msg-avatar" v-if="msg.role === 'user'">
					<view class="avatar-user">
						<text>我</text>
					</view>
				</view>
			</view>

			<!-- AI 正在输入 -->
			<view class="msg-item" v-if="aiLoading" id="msg-loading">
				<view class="msg-avatar">
					<view class="avatar-ai"><u-icon name="integral" color="#FFF" size="16"></u-icon></view>
				</view>
				<view class="msg-bubble bubble-ai">
					<view class="typing-dots">
						<view class="dot"></view><view class="dot"></view><view class="dot"></view>
					</view>
				</view>
			</view>

			<!-- 错误重试 -->
			<view class="error-bar" v-if="errorMsg">
				<text class="error-text">{{ errorMsg }}</text>
				<view class="retry-btn" @tap="retryLast"><text>重试</text></view>
			</view>
		</scroll-view>

		<!-- 底部输入框 -->
		<view class="input-bar">
			<input class="chat-input" v-model="inputText" placeholder="输入消息..."
				confirm-type="send" @confirm="sendMessage" :disabled="aiLoading" />
			<view class="send-btn" :class="{ active: inputText.trim() }" @tap="sendMessage">
				<u-icon name="arrow-up" :color="inputText.trim() ? '#FFF' : '#CCC'" size="18"></u-icon>
			</view>
		</view>
	</view>
</template>

<script>
	import { aiChat, aiCopywriting } from '../../utils/api.js'

	export default {
		data() {
			return {
				messages: [],
				inputText: '',
				aiLoading: false,
				errorMsg: '',
				lastInput: '',
				conversationId: '',
				scrollToId: '',
				copyForm: { projectId: '', type: '', typeLabel: '' },
				copyTypes: [
					{ label: '通用', value: 'general' },
					{ label: '朋友圈', value: 'moments' },
					{ label: '短视频', value: 'video' }
				],
				copyLoading: false,
				copyResult: ''
			}
		},
		methods: {
			onCopyTypeChange(e) {
				const item = this.copyTypes[e.detail.value]
				if (!item) return
				this.copyForm.type = item.value
				this.copyForm.typeLabel = item.label
			},
			async generateCopywriting() {
				if (!this.copyForm.projectId) {
					return uni.showToast({ title: '请输入项目ID', icon: 'none' })
				}
				this.copyLoading = true
				try {
					const res = await aiCopywriting({
						projectId: Number(this.copyForm.projectId),
						type: this.copyForm.type || 'general'
					})
					this.copyResult = res.data?.content || '暂无文案内容'
				} catch (err) {
					this.errorMsg = 'AI服务暂时不可用，请稍后重试'
				} finally {
					this.copyLoading = false
				}
			},
			quickAsk(text) {
				this.inputText = text
				this.sendMessage()
			},
			async sendMessage() {
				const text = this.inputText.trim()
				if (!text || this.aiLoading) return

				this.inputText = ''
				this.errorMsg = ''
				this.lastInput = text
				this.messages.push({ role: 'user', content: text })
				this.scrollToBottom()
				this.aiLoading = true

				try {
					const res = await aiChat({
						message: text,
						conversationId: this.conversationId
					})
					const reply = res.data?.reply || res.data?.content || res.data || '暂无回复'
					if (res.data?.conversationId) this.conversationId = res.data.conversationId
					this.messages.push({ role: 'ai', content: reply })
				} catch (err) {
					this.errorMsg = 'AI服务暂时不可用，请稍后重试'
				} finally {
					this.aiLoading = false
					this.scrollToBottom()
				}
			},
			retryLast() {
				if (this.lastInput) {
					this.inputText = this.lastInput
					this.errorMsg = ''
					this.sendMessage()
				}
			},
			copyText(text) {
				uni.setClipboardData({
					data: text,
					success: () => uni.showToast({ title: '已复制', icon: 'success' })
				})
			},
			scrollToBottom() {
				this.$nextTick(() => {
					const id = this.aiLoading ? 'msg-loading' : `msg-${this.messages.length - 1}`
					this.scrollToId = id
				})
			}
		}
	}
</script>

<style lang="scss" scoped>
		.page { min-height: 100vh; min-height: 100dvh; height: 100vh; height: 100dvh; display: flex; flex-direction: column; background: #F0F0F0; }
	.chat-list { flex: 1; padding: 20rpx; }
	.copywriting-panel {
		margin: 20rpx 20rpx 0; background: #FFF; border-radius: 16rpx; padding: 20rpx;
	}
	.panel-title { font-size: 26rpx; color: #333; font-weight: 600; margin-bottom: 12rpx; }
	.panel-form { display: flex; gap: 12rpx; align-items: center; }
	.panel-input {
		flex: 1; height: 64rpx; border: 1rpx solid #E8E8E8; border-radius: 8rpx;
		padding: 0 16rpx; font-size: 24rpx; background: #FAFAFA;
	}
	.panel-picker {
		height: 64rpx; padding: 0 16rpx; border: 1rpx solid #E8E8E8; border-radius: 8rpx;
		display: flex; align-items: center; font-size: 24rpx; color: #666; background: #FAFAFA;
	}
	.panel-btn {
		height: 64rpx; line-height: 64rpx; padding: 0 20rpx; font-size: 24rpx;
		background: #07C160; color: #FFF; border-radius: 8rpx; border: none;
	}
	.panel-btn::after { border: none; }
	.panel-result {
		margin-top: 12rpx; background: #F8FFF9; border: 1rpx solid #D9F7E6; border-radius: 8rpx; padding: 12rpx;
	}
	.result-text { font-size: 24rpx; color: #333; line-height: 1.6; display: block; }
	.result-copy {
		display: inline-flex; align-items: center; gap: 6rpx; margin-top: 10rpx;
		padding: 6rpx 12rpx; background: #FFF; border-radius: 20rpx; border: 1rpx solid #B7EACF;
		text { font-size: 22rpx; color: #07C160; }
	}

	/* 欢迎 */
	.chat-welcome {
		display: flex; flex-direction: column; align-items: center; padding: 60rpx 20rpx;
	}
	.welcome-icon {
		width: 120rpx; height: 120rpx; border-radius: 50%;
		background: rgba(7,193,96,0.1); display: flex; align-items: center; justify-content: center;
		margin-bottom: 20rpx;
	}
	.welcome-title { font-size: 36rpx; font-weight: 700; color: #1A1A1A; margin-bottom: 8rpx; }
	.welcome-desc { font-size: 26rpx; color: #999; margin-bottom: 32rpx; }
	.welcome-tags { display: flex; flex-wrap: wrap; gap: 16rpx; justify-content: center; }
	.tag-item {
		padding: 12rpx 24rpx; border-radius: 24rpx; background: #FFF;
		font-size: 24rpx; color: #07C160; border: 1rpx solid rgba(7,193,96,0.3);
		&:active { background: rgba(7,193,96,0.05); }
	}

	/* 消息 */
	.msg-item {
		display: flex; align-items: flex-start; margin-bottom: 24rpx;
		&.msg-user { flex-direction: row-reverse; }
	}
	.msg-avatar { flex-shrink: 0; }
	.avatar-ai {
		width: 56rpx; height: 56rpx; border-radius: 50%; background: #07C160;
		display: flex; align-items: center; justify-content: center; margin-right: 16rpx;
	}
	.avatar-user {
		width: 56rpx; height: 56rpx; border-radius: 50%; background: #1890FF;
		display: flex; align-items: center; justify-content: center; margin-left: 16rpx;
		text { font-size: 24rpx; color: #FFF; font-weight: 600; }
	}
	.msg-bubble {
		max-width: 70%; border-radius: 16rpx; padding: 20rpx;
		&.bubble-ai { background: #FFF; }
		&.bubble-user { background: #07C160; }
	}
	.msg-text { font-size: 28rpx; line-height: 1.6; word-break: break-all; }
	.bubble-ai .msg-text { color: #333; }
	.bubble-user .msg-text { color: #FFF; }
	.msg-actions {
		display: flex; margin-top: 12rpx; padding-top: 12rpx; border-top: 1rpx solid #F0F0F0;
	}
	.action-btn {
		display: flex; align-items: center; gap: 4rpx; padding: 4rpx 12rpx;
		text { font-size: 22rpx; color: #999; }
		&:active { opacity: 0.7; }
	}

	/* 加载动画 */
	.typing-dots { display: flex; gap: 8rpx; padding: 8rpx 0; }
	.dot {
		width: 12rpx; height: 12rpx; border-radius: 50%; background: #CCC;
		animation: blink 1.4s infinite both;
		&:nth-child(2) { animation-delay: 0.2s; }
		&:nth-child(3) { animation-delay: 0.4s; }
	}
	@keyframes blink { 0%, 80%, 100% { opacity: 0.3; } 40% { opacity: 1; } }

	/* 错误 */
	.error-bar {
		display: flex; align-items: center; justify-content: center; gap: 16rpx;
		padding: 16rpx; margin-top: 16rpx;
	}
	.error-text { font-size: 24rpx; color: #F5222D; }
	.retry-btn {
		padding: 6rpx 24rpx; border-radius: 20rpx; background: #FFF;
		border: 1rpx solid #F5222D;
		text { font-size: 24rpx; color: #F5222D; }
	}

	/* 输入框 */
	.input-bar {
		display: flex; align-items: center; padding: 16rpx 20rpx 32rpx;
		background: #FFF; gap: 16rpx; box-shadow: 0 -2rpx 12rpx rgba(0,0,0,0.04);
	}
	.chat-input {
		flex: 1; height: 72rpx; background: #F5F5F5; border-radius: 36rpx;
		padding: 0 24rpx; font-size: 28rpx;
	}
	.send-btn {
		width: 72rpx; height: 72rpx; border-radius: 50%; background: #E8E8E8;
		display: flex; align-items: center; justify-content: center; flex-shrink: 0;
		&.active { background: #07C160; }
	}
</style>
