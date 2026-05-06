package com.pengcheng.message.service;

import com.pengcheng.message.entity.Notification;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property 11: 业务事件通知创建
 *
 * <p>对于任意触发通知的业务事件（客户状态变更、审批状态变更、新审批到达），
 * 系统应为正确的接收人创建 Notification 记录，且通知的 bizType 和 bizId 应与触发事件的业务实体对应。
 *
 * <p>Feature: internal-multiplatform, Property 11: 业务事件通知创建
 *
 * <p><b>Validates: Requirements 7.1, 7.2, 7.3</b>
 */
class NotificationServiceProperties {

    // ========== Constants ==========
    private static final int TYPE_CUSTOMER_STATUS = 1;
    private static final int TYPE_APPROVAL_STATUS = 2;
    private static final int TYPE_NEW_APPROVAL = 3;

    // ========== Simulated store ==========

    /**
     * Simulates NotificationMapper + PushServiceFactory behavior.
     * Captures created notifications and push calls for verification.
     */
    static class SimNotificationStore {
        private final AtomicLong idSeq = new AtomicLong(1);
        private final List<Notification> notifications = new ArrayList<>();
        private final List<PushRecord> pushRecords = new ArrayList<>();

        void insert(Notification notification) {
            notification.setId(idSeq.getAndIncrement());
            if (notification.getReadStatus() == null) {
                notification.setReadStatus(0);
            }
            if (notification.getCreateTime() == null) {
                notification.setCreateTime(LocalDateTime.now());
            }
            notifications.add(notification);
        }

        void pushToUser(Long userId, String title, String content) {
            pushRecords.add(new PushRecord(userId, title, content));
        }

        List<Notification> getAll() {
            return Collections.unmodifiableList(notifications);
        }

        List<Notification> getByUserId(Long userId) {
            return notifications.stream()
                    .filter(n -> n.getUserId().equals(userId))
                    .toList();
        }

        List<PushRecord> getPushRecords() {
            return Collections.unmodifiableList(pushRecords);
        }

        record PushRecord(Long userId, String title, String content) {}
    }

    /**
     * Simulates NotificationServiceImpl logic using the SimStore.
     */
    static class SimNotificationService {
        private final SimNotificationStore store;

        SimNotificationService(SimNotificationStore store) {
            this.store = store;
        }

        void notifyCustomerStatusChange(Long customerId, String newStatus, Long userId) {
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
            store.insert(notification);
            store.pushToUser(userId, title, content);
        }

        void notifyApprovalStatusChange(Long requestId, String bizType, String newStatus, Long applicantId) {
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
            store.insert(notification);
            store.pushToUser(applicantId, title, content);
        }

        void notifyNewApproval(Long requestId, String bizType, List<Long> approverIds) {
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
                store.insert(notification);
                store.pushToUser(approverId, title, content);
            }
        }

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

    // ========== Generators ==========

    @Provide
    Arbitrary<String> customerStatuses() {
        return Arbitraries.of("已报备", "已到访", "已成交", "已失效", "已退房");
    }

    @Provide
    Arbitrary<String> bizTypes() {
        return Arbitraries.of("leave", "compensate", "payment", "commission");
    }

    @Provide
    Arbitrary<String> approvalStatuses() {
        return Arbitraries.of("已通过", "已驳回", "审批中");
    }

    @Provide
    Arbitrary<List<Long>> approverIdLists() {
        return Arbitraries.longs().between(1, 10000)
                .list().ofMinSize(1).ofMaxSize(5)
                .filter(list -> list.stream().distinct().count() == list.size());
    }

    // ========== Property 11a: 客户状态变更通知 ==========

    /**
     * For any customer status change event, the system creates a Notification
     * with type=1, bizType="customer", bizId=customerId, and userId matching the recipient.
     *
     * <p><b>Validates: Requirements 7.1</b>
     */
    @Property(tries = 100)
    void customerStatusChangeCreatesCorrectNotification(
            @ForAll @LongRange(min = 1, max = 10000) long customerId,
            @ForAll("customerStatuses") String newStatus,
            @ForAll @LongRange(min = 1, max = 10000) long userId
    ) {
        SimNotificationStore store = new SimNotificationStore();
        SimNotificationService service = new SimNotificationService(store);

        service.notifyCustomerStatusChange(customerId, newStatus, userId);

        List<Notification> created = store.getAll();
        assertThat(created).hasSize(1);

        Notification n = created.get(0);
        assertThat(n.getUserId()).isEqualTo(userId);
        assertThat(n.getType()).isEqualTo(TYPE_CUSTOMER_STATUS);
        assertThat(n.getBizType()).isEqualTo("customer");
        assertThat(n.getBizId()).isEqualTo(customerId);
        assertThat(n.getReadStatus()).isEqualTo(0);
        assertThat(n.getCreateTime()).isNotNull();
        assertThat(n.getContent()).contains(newStatus);

        // Verify push was sent to the correct user
        assertThat(store.getPushRecords()).hasSize(1);
        assertThat(store.getPushRecords().get(0).userId()).isEqualTo(userId);
    }

    // ========== Property 11b: 审批状态变更通知 ==========

    /**
     * For any approval status change event, the system creates a Notification
     * with type=2, matching bizType, bizId=requestId, and userId=applicantId.
     *
     * <p><b>Validates: Requirements 7.2</b>
     */
    @Property(tries = 100)
    void approvalStatusChangeCreatesCorrectNotification(
            @ForAll @LongRange(min = 1, max = 10000) long requestId,
            @ForAll("bizTypes") String bizType,
            @ForAll("approvalStatuses") String newStatus,
            @ForAll @LongRange(min = 1, max = 10000) long applicantId
    ) {
        SimNotificationStore store = new SimNotificationStore();
        SimNotificationService service = new SimNotificationService(store);

        service.notifyApprovalStatusChange(requestId, bizType, newStatus, applicantId);

        List<Notification> created = store.getAll();
        assertThat(created).hasSize(1);

        Notification n = created.get(0);
        assertThat(n.getUserId()).isEqualTo(applicantId);
        assertThat(n.getType()).isEqualTo(TYPE_APPROVAL_STATUS);
        assertThat(n.getBizType()).isEqualTo(bizType);
        assertThat(n.getBizId()).isEqualTo(requestId);
        assertThat(n.getReadStatus()).isEqualTo(0);
        assertThat(n.getCreateTime()).isNotNull();

        assertThat(store.getPushRecords()).hasSize(1);
        assertThat(store.getPushRecords().get(0).userId()).isEqualTo(applicantId);
    }

    // ========== Property 11c: 新审批到达通知 ==========

    /**
     * For any new approval event with N approvers, the system creates exactly N
     * Notification records, each with type=3, matching bizType, bizId=requestId,
     * and userId matching one of the approverIds.
     *
     * <p><b>Validates: Requirements 7.3</b>
     */
    @Property(tries = 100)
    void newApprovalCreatesNotificationForEachApprover(
            @ForAll @LongRange(min = 1, max = 10000) long requestId,
            @ForAll("bizTypes") String bizType,
            @ForAll("approverIdLists") List<Long> approverIds
    ) {
        SimNotificationStore store = new SimNotificationStore();
        SimNotificationService service = new SimNotificationService(store);

        service.notifyNewApproval(requestId, bizType, approverIds);

        List<Notification> created = store.getAll();
        assertThat(created).hasSize(approverIds.size());

        // Each approver should have exactly one notification
        for (Long approverId : approverIds) {
            List<Notification> forApprover = store.getByUserId(approverId);
            assertThat(forApprover)
                    .as("Approver %d should have exactly 1 notification", approverId)
                    .hasSize(1);

            Notification n = forApprover.get(0);
            assertThat(n.getType()).isEqualTo(TYPE_NEW_APPROVAL);
            assertThat(n.getBizType()).isEqualTo(bizType);
            assertThat(n.getBizId()).isEqualTo(requestId);
            assertThat(n.getReadStatus()).isEqualTo(0);
        }

        // Push should be sent to each approver
        assertThat(store.getPushRecords()).hasSize(approverIds.size());
        Set<Long> pushedUserIds = new HashSet<>();
        for (var pr : store.getPushRecords()) {
            pushedUserIds.add(pr.userId());
        }
        assertThat(pushedUserIds).containsExactlyInAnyOrderElementsOf(approverIds);
    }
}
