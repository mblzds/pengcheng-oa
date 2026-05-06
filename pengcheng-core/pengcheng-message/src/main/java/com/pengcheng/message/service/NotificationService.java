package com.pengcheng.message.service;

import com.pengcheng.message.entity.Notification;

import java.util.List;

/**
 * 通知消息服务接口
 */
public interface NotificationService {

    /**
     * 创建通知
     *
     * @param notification 通知实体
     */
    void createNotification(Notification notification);

    /**
     * 获取用户未读通知数量
     *
     * @param userId 用户ID
     * @return 未读数量
     */
    int getUnreadCount(Long userId);

    /**
     * 获取用户最近通知列表
     *
     * @param userId 用户ID
     * @param limit  返回条数上限
     * @return 通知列表（按创建时间降序）
     */
    List<Notification> getRecentNotifications(Long userId, int limit);

    /**
     * 标记通知为已读
     *
     * @param notificationId 通知ID
     */
    void markAsRead(Long notificationId);

    /**
     * 客户报备状态变更通知
     * 向相关驻场人员发送推送通知
     *
     * @param customerId 客户ID
     * @param newStatus  新状态描述
     * @param userId     接收通知的用户ID
     */
    void notifyCustomerStatusChange(Long customerId, String newStatus, Long userId);

    /**
     * 审批事项状态变更通知
     * 向申请人发送推送通知
     *
     * @param requestId  申请ID
     * @param bizType    业务类型（leave/payment/commission）
     * @param newStatus  新状态描述
     * @param applicantId 申请人ID
     */
    void notifyApprovalStatusChange(Long requestId, String bizType, String newStatus, Long applicantId);

    /**
     * 新审批事项到达通知
     * 向审批人发送推送通知
     *
     * @param requestId   申请ID
     * @param bizType     业务类型（leave/payment/commission）
     * @param approverIds 审批人ID列表
     */
    void notifyNewApproval(Long requestId, String bizType, List<Long> approverIds);
}
