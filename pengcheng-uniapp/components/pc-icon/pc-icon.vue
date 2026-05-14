<template>
	<image
		v-if="isImg"
		class="pc-icon__img"
		:src="name"
		:mode="imgMode"
		:style="[imgStyle, mergedStyle]"
		@tap="clickHandler"
	></image>
	<text
		v-else
		class="pc-icon__text"
		:style="[textStyle, mergedStyle]"
		:hover-class="hoverClass"
		@tap="clickHandler"
	>{{ icon }}</text>
</template>

<script>
	import icons from 'uview-plus/components/u-icon/icons'
	import { addUnit, addStyle } from 'uview-plus/libs/function/index'

	export default {
		name: 'pc-icon',
		props: {
			name: { type: String, default: '' },
			color: { type: String, default: '#303133' },
			size: { type: [String, Number], default: 16 },
			bold: { type: Boolean, default: false },
			index: { type: [String, Number], default: '' },
			hoverClass: { type: String, default: '' },
			customPrefix: { type: String, default: 'uicon' },
			imgMode: { type: String, default: 'widthFix' },
			width: { type: [String, Number], default: '' },
			height: { type: [String, Number], default: '' },
			top: { type: [String, Number], default: 0 },
			stop: { type: Boolean, default: false },
			customStyle: { type: [Object, String], default: () => ({}) }
		},
		emits: ['click'],
		computed: {
			isImg() {
				return this.name.indexOf('/') !== -1
			},
			icon() {
				if (this.customPrefix !== 'uicon') return ''
				return icons['uicon-' + this.name] || this.name
			},
			mergedStyle() {
				return addStyle(this.customStyle)
			},
			textStyle() {
				const palette = {
					primary: '#2979ff',
					success: '#19be6b',
					error: '#fa3534',
					warning: '#ff9900',
					info: '#909399'
				}
				return {
					fontFamily: 'uicon-iconfont',
					fontSize: addUnit(this.size),
					lineHeight: addUnit(this.size),
					fontWeight: this.bold ? 'bold' : 'normal',
					color: palette[this.color] || this.color,
					display: 'inline-block',
					textAlign: 'center',
					top: addUnit(this.top)
				}
			},
			imgStyle() {
				return {
					width: this.width ? addUnit(this.width) : addUnit(this.size),
					height: this.height ? addUnit(this.height) : addUnit(this.size),
					display: 'inline-block'
				}
			}
		},
		methods: {
			clickHandler(e) {
				this.$emit('click', this.index)
				if (this.stop && e && typeof e.stopPropagation === 'function') {
					e.stopPropagation()
				}
			}
		}
	}
</script>

<style lang="scss" scoped>
	@font-face {
		font-family: 'uicon-iconfont';
		src: url('/static/fonts/uicon-iconfont.ttf') format('truetype');
	}

	.pc-icon__text,
	.pc-icon__img {
		vertical-align: middle;
		flex-shrink: 0;
	}
</style>
