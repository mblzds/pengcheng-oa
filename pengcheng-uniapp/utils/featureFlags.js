/**
 * 功能开关 / Feature Flags
 * 用于按版本封存 / 启用产品能力。后端 service / controller / 表结构均保留，
 * V2 切 true 即可解锁对应 UI 入口。
 */
export const FEATURES = Object.freeze({
	/**
	 * 到访录入 / 成交录入
	 * V1 一期客户验收范围仅包含「客户报备」，到访 / 成交 UI 全部隐藏。
	 * - detail.vue: 到访记录卡 + 成交信息卡 + 两个录入弹窗
	 * - list.vue: ?action=visit/deal 自动跳转逻辑
	 */
	ENABLE_VISIT_AND_DEAL: false
})
