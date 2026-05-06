/**
 * 通知消息跳转工具
 */

/**
 * 根据通知业务类型跳转对应页面
 * @param {Object} notice 通知对象
 * @returns {boolean} 是否已执行跳转
 */
export const navigateByNotice = (notice) => {
  if (!notice) return false
  const bizType = notice.bizType
  const bizId = notice.bizId
  const subType = notice.subType || notice.typeCode || notice.approvalType

  if (bizType === 'customer' && bizId) {
    uni.navigateTo({ url: `/pages/customer/detail?id=${bizId}` })
    return true
  }

  if (bizType === 'approval' && bizId && subType) {
    uni.navigateTo({ url: `/pages/approval/detail?id=${bizId}&type=${subType}` })
    return true
  }

  if (bizType === 'approval') {
    uni.navigateTo({ url: '/pages/approval/list' })
    return true
  }

  if (bizType === 'payment' || bizType === 'apply') {
    uni.navigateTo({ url: '/pages/apply/list' })
    return true
  }

  return false
}

