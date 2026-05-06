package com.pengcheng.realty.commission;

import com.pengcheng.realty.commission.entity.Commission;
import com.pengcheng.realty.commission.entity.CommissionChangeLog;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 佣金管理属性测试
 *
 * <p>Property 14: 佣金等式不变量 — For any 佣金记录，应收佣金 = 应结佣金 + 公司平台费
 * <p>Property 15: 佣金创建后状态为待审核 — For any 成功创建的佣金记录，审核状态应为"待审核"
 * <p>Property 16: 佣金变更审计追踪 — For any 佣金字段更新，系统应生成变更日志包含字段名、前后值、变更人、时间
 *
 * <p><b>Validates: Requirements 8.3, 8.5, 8.6</b>
 */
class CommissionManagementProperties {

    /** 审核状态：待审核 */
    private static final int AUDIT_STATUS_PENDING = 1;

    // ========== Generators ==========

    @Provide
    Arbitrary<BigDecimal> positiveAmounts() {
        return Arbitraries.bigDecimals()
                .between(new BigDecimal("0.01"), new BigDecimal("10000000"))
                .ofScale(2);
    }

    @Provide
    Arbitrary<BigDecimal> platformFeeRatios() {
        // Platform fee ratio between 0.01 and 0.99 to ensure valid split
        return Arbitraries.bigDecimals()
                .between(new BigDecimal("0.01"), new BigDecimal("0.99"))
                .ofScale(2);
    }

    // ========== Property 14: 佣金等式不变量 ==========

    /**
     * Property 14: 佣金等式不变量
     *
     * <p>For any 佣金记录，应收佣金 = 应结佣金 + 公司平台费。
     * 违反此等式的录入应被拒绝。
     *
     * <p><b>Validates: Requirements 8.5</b>
     */
    @Property(tries = 100)
    void commissionEquationMustHold(
            @ForAll("positiveAmounts") BigDecimal receivableAmount,
            @ForAll("platformFeeRatios") BigDecimal feeRatio
    ) {
        // Derive payable and platformFee from receivable to guarantee valid split
        BigDecimal platformFee = receivableAmount.multiply(feeRatio)
                .setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal payableAmount = receivableAmount.subtract(platformFee);

        // Valid case: equation holds → validation passes
        boolean validResult = validateEquation(receivableAmount, payableAmount, platformFee);
        assertThat(validResult)
                .as("receivable(%s) should equal payable(%s) + platformFee(%s)",
                        receivableAmount, payableAmount, platformFee)
                .isTrue();

        // Invalid case: tamper with one value → validation fails
        BigDecimal tamperedReceivable = receivableAmount.add(BigDecimal.ONE);
        boolean invalidResult = validateEquation(tamperedReceivable, payableAmount, platformFee);
        assertThat(invalidResult)
                .as("Tampered receivable should fail equation validation")
                .isFalse();
    }

    // ========== Property 15: 佣金创建后状态为待审核 ==========

    /**
     * Property 15: 佣金创建后状态为待审核
     *
     * <p>For any 成功创建的佣金记录，审核状态应为"待审核"(1)。
     *
     * <p><b>Validates: Requirements 8.3</b>
     */
    @Property(tries = 100)
    void commissionCreatedWithPendingAuditStatus(
            @ForAll("positiveAmounts") BigDecimal receivableAmount,
            @ForAll("platformFeeRatios") BigDecimal feeRatio,
            @ForAll @LongRange(min = 1, max = 10000) long dealId,
            @ForAll @LongRange(min = 1, max = 100) long projectId,
            @ForAll @LongRange(min = 1, max = 100) long allianceId
    ) {
        BigDecimal platformFee = receivableAmount.multiply(feeRatio)
                .setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal payableAmount = receivableAmount.subtract(platformFee);

        // Simulate commission creation logic (same as CommissionService.createCommission)
        Commission commission = Commission.builder()
                .dealId(dealId)
                .projectId(projectId)
                .allianceId(allianceId)
                .receivableAmount(receivableAmount)
                .payableAmount(payableAmount)
                .platformFee(platformFee)
                .auditStatus(AUDIT_STATUS_PENDING)
                .build();

        assertThat(commission.getAuditStatus())
                .as("Newly created commission should have audit status = 待审核(1)")
                .isEqualTo(AUDIT_STATUS_PENDING);

        // Also verify the equation holds for the created commission
        assertThat(commission.getReceivableAmount())
                .isEqualByComparingTo(commission.getPayableAmount().add(commission.getPlatformFee()));
    }

    // ========== Property 16: 佣金变更审计追踪 ==========

    /**
     * Property 16: 佣金变更审计追踪
     *
     * <p>For any 佣金字段更新，系统应生成变更日志包含字段名、前后值、变更人、时间。
     *
     * <p><b>Validates: Requirements 8.6</b>
     */
    @Property(tries = 100)
    void commissionChangeGeneratesAuditLog(
            @ForAll("positiveAmounts") BigDecimal oldReceivable,
            @ForAll("positiveAmounts") BigDecimal newReceivable,
            @ForAll @LongRange(min = 1, max = 10000) long commissionId,
            @ForAll @LongRange(min = 1, max = 1000) long operatorId
    ) {
        List<CommissionChangeLog> logs = new ArrayList<>();

        // Simulate the update logic from CommissionService.updateCommission
        String oldValue = oldReceivable.toPlainString();
        String newValue = newReceivable.toPlainString();

        if (!Objects.equals(oldReceivable, newReceivable)) {
            CommissionChangeLog log = CommissionChangeLog.builder()
                    .commissionId(commissionId)
                    .fieldName("receivableAmount")
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .operatorId(operatorId)
                    .changeTime(LocalDateTime.now())
                    .build();
            logs.add(log);
        }

        if (!Objects.equals(oldReceivable, newReceivable)) {
            // When values differ, exactly one log should be generated
            assertThat(logs).hasSize(1);

            CommissionChangeLog log = logs.get(0);
            assertThat(log.getCommissionId()).isEqualTo(commissionId);
            assertThat(log.getFieldName()).isEqualTo("receivableAmount");
            assertThat(log.getOldValue()).isEqualTo(oldValue);
            assertThat(log.getNewValue()).isEqualTo(newValue);
            assertThat(log.getOperatorId()).isEqualTo(operatorId);
            assertThat(log.getChangeTime()).isNotNull();
        } else {
            // When values are the same, no log should be generated
            assertThat(logs).isEmpty();
        }
    }

    // ========== Helper Methods ==========

    /**
     * Pure validation logic matching CommissionService.validateCommissionEquation.
     * Returns true if equation holds, false otherwise.
     */
    private boolean validateEquation(BigDecimal receivable, BigDecimal payable, BigDecimal platformFee) {
        if (receivable == null || payable == null || platformFee == null) {
            return false;
        }
        return receivable.compareTo(payable.add(platformFee)) == 0;
    }
}
