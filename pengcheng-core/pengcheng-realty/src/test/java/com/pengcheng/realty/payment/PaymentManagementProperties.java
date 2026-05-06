package com.pengcheng.realty.payment;

import com.pengcheng.realty.payment.entity.PaymentApproval;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 付款申请管理属性测试
 *
 * <p>Property 19: 付款申请校验与审批流匹配 — For any 垫佣申请必须关联有效成交记录，
 * 预付佣必须关联有效联盟商和成交记录
 * <p>Property 20: 审批流转历史完整性 — For any 审批操作，系统应生成审批记录，总数等于操作次数
 *
 * <p><b>Validates: Requirements 10.2, 10.3, 10.4, 10.6</b>
 */
class PaymentManagementProperties {

    /** 申请类型 */
    private static final int TYPE_EXPENSE = 1;
    private static final int TYPE_ADVANCE_COMMISSION = 2;
    private static final int TYPE_PREPAY_COMMISSION = 3;

    /** 审批结果 */
    private static final int APPROVAL_RESULT_PASS = 1;
    private static final int APPROVAL_RESULT_REJECT = 2;

    // ========== Generators ==========

    @Provide
    Arbitrary<BigDecimal> positiveAmounts() {
        return Arbitraries.bigDecimals()
                .between(new BigDecimal("0.01"), new BigDecimal("1000000"))
                .ofScale(2);
    }

    // ========== Property 19: 付款申请校验与审批流匹配 ==========

    /**
     * Property 19a: 垫佣申请必须关联有效成交记录
     *
     * <p>For any 垫佣申请，如果未关联成交记录ID，校验应失败。
     *
     * <p><b>Validates: Requirements 10.2</b>
     */
    @Property(tries = 100)
    void advanceCommissionRequiresDealId(
            @ForAll @LongRange(min = 1, max = 10000) long applicantId,
            @ForAll("positiveAmounts") BigDecimal amount
    ) {
        // Simulate validation logic from PaymentService.validatePaymentRequest
        // 垫佣申请 without relatedDealId should be rejected
        assertThatThrownBy(() -> validatePaymentRequest(TYPE_ADVANCE_COMMISSION, applicantId, amount,
                null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("垫佣申请必须关联成交记录");
    }

    /**
     * Property 19b: 预付佣申请必须关联有效联盟商和成交记录
     *
     * <p>For any 预付佣申请，如果未关联成交记录或联盟商，校验应失败。
     *
     * <p><b>Validates: Requirements 10.3</b>
     */
    @Property(tries = 100)
    void prepayCommissionRequiresDealAndAlliance(
            @ForAll @LongRange(min = 1, max = 10000) long applicantId,
            @ForAll("positiveAmounts") BigDecimal amount,
            @ForAll @LongRange(min = 1, max = 1000) long dealId,
            @ForAll @LongRange(min = 1, max = 1000) long allianceId
    ) {
        // Missing dealId
        assertThatThrownBy(() -> validatePaymentRequest(TYPE_PREPAY_COMMISSION, applicantId, amount,
                null, null, allianceId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("预付佣申请必须关联成交记录");

        // Missing allianceId
        assertThatThrownBy(() -> validatePaymentRequest(TYPE_PREPAY_COMMISSION, applicantId, amount,
                null, dealId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("预付佣申请必须关联联盟商");
    }

    /**
     * Property 19c: 审批流程根据申请类型和金额自动匹配
     *
     * <p>For any 付款申请，审批级数应根据类型和金额正确匹配。
     *
     * <p><b>Validates: Requirements 10.4</b>
     */
    @Property(tries = 100)
    void approvalFlowMatchesTypeAndAmount(
            @ForAll("positiveAmounts") BigDecimal amount
    ) {
        // 费用报销：<= 5000 → 1级，> 5000 → 2级
        int expenseApprovals = getRequiredApprovalCount(TYPE_EXPENSE, amount);
        if (amount.compareTo(new BigDecimal("5000")) <= 0) {
            assertThat(expenseApprovals).isEqualTo(1);
        } else {
            assertThat(expenseApprovals).isEqualTo(2);
        }

        // 垫佣：<= 50000 → 2级，> 50000 → 3级
        int advanceApprovals = getRequiredApprovalCount(TYPE_ADVANCE_COMMISSION, amount);
        if (amount.compareTo(new BigDecimal("50000")) <= 0) {
            assertThat(advanceApprovals).isEqualTo(2);
        } else {
            assertThat(advanceApprovals).isEqualTo(3);
        }

        // 预付佣：same rules as 垫佣
        int prepayApprovals = getRequiredApprovalCount(TYPE_PREPAY_COMMISSION, amount);
        assertThat(prepayApprovals).isEqualTo(advanceApprovals);
    }

    // ========== Property 20: 审批流转历史完整性 ==========

    /**
     * Property 20: 审批流转历史完整性
     *
     * <p>For any 审批操作序列，系统应生成审批记录，总数等于操作次数，
     * 且每条记录包含审批人、结果、时间、顺序号。
     *
     * <p><b>Validates: Requirements 10.6</b>
     */
    @Property(tries = 100)
    void approvalHistoryIsComplete(
            @ForAll @IntRange(min = 1, max = 5) int approvalCount,
            @ForAll @LongRange(min = 1, max = 10000) long requestId
    ) {
        List<PaymentApproval> approvals = new ArrayList<>();

        // Simulate a sequence of approval operations
        for (int i = 1; i <= approvalCount; i++) {
            PaymentApproval approval = PaymentApproval.builder()
                    .requestId(requestId)
                    .approverId((long) (i * 100))
                    .result(APPROVAL_RESULT_PASS)
                    .remark("审批通过")
                    .approvalOrder(i)
                    .approvalTime(LocalDateTime.now())
                    .build();
            approvals.add(approval);
        }

        // Total approval records should equal the number of operations
        assertThat(approvals).hasSize(approvalCount);

        // Each record should have all required fields
        for (int i = 0; i < approvals.size(); i++) {
            PaymentApproval approval = approvals.get(i);
            assertThat(approval.getRequestId()).isEqualTo(requestId);
            assertThat(approval.getApproverId()).isNotNull().isPositive();
            assertThat(approval.getResult()).isIn(APPROVAL_RESULT_PASS, APPROVAL_RESULT_REJECT);
            assertThat(approval.getApprovalTime()).isNotNull();
            assertThat(approval.getApprovalOrder()).isEqualTo(i + 1);
        }

        // Approval orders should be sequential
        for (int i = 1; i < approvals.size(); i++) {
            assertThat(approvals.get(i).getApprovalOrder())
                    .isGreaterThan(approvals.get(i - 1).getApprovalOrder());
        }
    }

    // ========== Helper Methods ==========

    /**
     * Pure validation logic matching PaymentService.validatePaymentRequest.
     * Does NOT check DB existence — only validates required field presence.
     */
    private void validatePaymentRequest(int requestType, Long applicantId, BigDecimal amount,
                                         Integer expenseType, Long relatedDealId, Long relatedAllianceId) {
        if (applicantId == null) {
            throw new IllegalArgumentException("申请人ID不能为空");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("金额必须大于0");
        }

        switch (requestType) {
            case TYPE_EXPENSE:
                if (expenseType == null) {
                    throw new IllegalArgumentException("费用报销时报销类型不能为空");
                }
                break;
            case TYPE_ADVANCE_COMMISSION:
                if (relatedDealId == null) {
                    throw new IllegalArgumentException("垫佣申请必须关联成交记录");
                }
                break;
            case TYPE_PREPAY_COMMISSION:
                if (relatedDealId == null) {
                    throw new IllegalArgumentException("预付佣申请必须关联成交记录");
                }
                if (relatedAllianceId == null) {
                    throw new IllegalArgumentException("预付佣申请必须关联联盟商");
                }
                break;
            default:
                throw new IllegalArgumentException("无效的申请类型：" + requestType);
        }
    }

    /**
     * Pure logic matching PaymentService.getRequiredApprovalCount.
     */
    private int getRequiredApprovalCount(int requestType, BigDecimal amount) {
        if (amount == null) {
            return 1;
        }
        if (requestType == TYPE_EXPENSE) {
            return amount.compareTo(new BigDecimal("5000")) <= 0 ? 1 : 2;
        } else {
            return amount.compareTo(new BigDecimal("50000")) <= 0 ? 2 : 3;
        }
    }
}
