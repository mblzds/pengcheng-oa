package com.pengcheng.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.pengcheng.message.entity.Notification;
import com.pengcheng.message.mapper.NotificationMapper;
import com.pengcheng.message.service.NotificationService;
import com.pengcheng.push.PushServiceFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知消息服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final PushServiceFactory pushServiceFactory;

    /** 通知类型：客户状态变更 */
    private static final int TYPE_CUSTOMER_STATUS = 1;
    /** 通知类型：审批状态变更 */
    private static final int TYPE_APPROVAL_STATUS = 2;
    /** 通知类型：新审批到达 */
    private static final int TYPE_NEW_APPROVAL = 3;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createNotification(Notification notification) {
        if (notification.getReadStatus() == null) {
            notification.setReadStatus(0);
        }
        if (notification.getCreateTime() == null) {
            notification.setCreateTime(LocalDateTime.now());
        }
        notificationMapper.insert(notification);
        log.info("创建通知: userId={}, type={}, bizType={}, bizId={}",
                notification.getUserId(), notification.getType(),
                notification.getBizType(), notification.getBizId());
    }

    @Override
    public int getUnreadCount(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId);
        wrapper.eq(Notification::getReadStatus, 0);
        return Math.toIntExact(notificationMapper.selectCount(wrapper));
    }

    @Override
    public List<Notification> getRecentNotifications(Long userId, int limit) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId);
        wrapper.orderByDesc(Notification::getCreateTime);
        wrapper.last("LIMIT " + limit);
        return notificationMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long notificationId) {
        LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Notification::getId, notificationId);
        wrapper.set(Notification::getReadStatus, 1);
        notificationMapper.update(null, wrapper);
    }

    @Override
    public void notifyCustomerStatusChange(Long customerId, String newStatus, Long userId) {
        String title = "客户状态变更";
        String content = "您关注的客户状态已变更为：" + newStatus;

        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .content(content)
                .type(TYPE_CUSTOMER_STATUS)
                .bizType("customer")
                .bizId(customerId)
                .build();
        createNotification(notification);

        pushSafe(userId, title, content);
        log.info("客户状态变更通知已发送: customerId={}, newStatus={}, userId={}", customerId, newStatus, userId);
    }

    @Override
    public void notifyApprovalStatusChange(Long requestId, String bizType, String newStatus, Long applicantId) {
        String title = "审批状态变更";
        String content = "您的" + formatBizType(bizType) + "申请审批状态已变更为：" + newStatus;

        Notification notification = Notification.builder()
                .userId(applicantId)
                .title(title)
                .content(content)
                .type(TYPE_APPROVAL_STATUS)
                .bizType(bizType)
                .bizId(requestId)
                .build();
        createNotification(notification);

        pushSafe(applicantId, title, content);
        log.info("审批状态变更通知已发送: requestId={}, bizType={}, newStatus={}, applicantId={}",
                requestId, bizType, newStatus, applicantId);
    }

    @Override
    public void notifyNewApproval(Long requestId, String bizType, List<Long> approverIds) {
        String title = "新审批事项";
        String content = "您有一条新的" + formatBizType(bizType) + "审批待处理";

        for (Long approverId : approverIds) {
            Notification notification = Notification.builder()
                    .userId(approverId)
                    .title(title)
                    .content(content)
                    .type(TYPE_NEW_APPROVAL)
                    .bizType(bizType)
                    .bizId(requestId)
                    .build();
            createNotification(notification);

            pushSafe(approverId, title, content);
        }
        log.info("新审批通知已发送: requestId={}, bizType={}, approverIds={}", requestId, bizType, approverIds);
    }

    /**
     * 安全推送，捕获异常避免影响业务流程
     */
    private void pushSafe(Long userId, String title, String content) {
        try {
            pushServiceFactory.pushToUser(String.valueOf(userId), title, content);
        } catch (Exception e) {
            log.warn("推送通知失败: userId={}, title={}, error={}", userId, title, e.getMessage());
        }
    }

    /**
     * 格式化业务类型为中文描述
     */
    private String formatBizType(String bizType) {
        return switch (bizType) {
            case "leave" -> "请假";
            case "compensate" -> "调休";
            case "payment" -> "付款";
            case "commission" -> "佣金";
            default -> bizType;
        };
    }
}
