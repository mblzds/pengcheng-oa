package com.pengcheng.realty.commission;

import com.pengcheng.realty.commission.service.CommissionCalculator;
import com.pengcheng.realty.project.entity.ProjectCommissionRule;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 佣金计算引擎属性测试
 *
 * <p>Property 28: 佣金计算引擎正确性 — For any 成交金额和项目佣金规则，
 * 基础佣金 = 成交金额 × 基础比例，跳点佣金根据套数和规则正确计算
 *
 * <p><b>Validates: Requirements 16.1</b>
 */
class CommissionCalculatorProperties {

    private final CommissionCalculator calculator = new CommissionCalculator();

    // ========== Generators ==========

    @Provide
    Arbitrary<BigDecimal> dealAmounts() {
        return Arbitraries.bigDecimals()
                .between(new BigDecimal("1000"), new BigDecimal("50000000"))
                .ofScale(2);
    }

    @Provide
    Arbitrary<BigDecimal> baseRates() {
        return Arbitraries.bigDecimals()
                .between(new BigDecimal("0.001"), new BigDecimal("0.10"))
                .ofScale(4);
    }

    @Provide
    Arbitrary<BigDecimal> rewardAmounts() {
        return Arbitraries.bigDecimals()
                .between(BigDecimal.ZERO, new BigDecimal("100000"))
                .ofScale(2);
    }

    @Provide
    Arbitrary<Integer> dealCounts() {
        return Arbitraries.integers().between(1, 100);
    }

    // ========== Property 28: 基础佣金 = 成交金额 × 基础比例 ==========

    /**
     * Property 28 (base commission): For any deal amount and base rate,
     * the calculated base commission must equal dealAmount × baseRate (rounded to 2 decimal places).
     *
     * <p><b>Validates: Requirements 16.1</b>
     */
    @Property(tries = 100)
    void baseCommissionEqualsAmountTimesRate(
            @ForAll("dealAmounts") BigDecimal dealAmount,
            @ForAll("baseRates") BigDecimal baseRate,
            @ForAll("rewardAmounts") BigDecimal cashReward,
            @ForAll("rewardAmounts") BigDecimal firstDealReward,
            @ForAll("rewardAmounts") BigDecimal platformReward
    ) {
        ProjectCommissionRule rule = ProjectCommissionRule.builder()
                .projectId(1L)
                .baseRate(baseRate)
                .cashReward(cashReward)
                .firstDealReward(firstDealReward)
                .platformReward(platformReward)
                .version(1)
                .status(1)
                .build();

        CommissionCalculator.CalcResult result = calculator.calculate(rule, dealAmount, 1);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDetail()).isNotNull();

        BigDecimal expectedBase = dealAmount.multiply(baseRate).setScale(2, RoundingMode.HALF_UP);
        assertThat(result.getDetail().getBaseCommission())
                .as("Base commission should equal dealAmount(%s) × baseRate(%s) = %s",
                        dealAmount, baseRate, expectedBase)
                .isEqualByComparingTo(expectedBase);

        // Fixed rewards should be passed through directly
        assertThat(result.getDetail().getCashReward()).isEqualByComparingTo(cashReward);
        assertThat(result.getDetail().getFirstDealReward()).isEqualByComparingTo(firstDealReward);
        assertThat(result.getDetail().getPlatformReward()).isEqualByComparingTo(platformReward);
    }

    // ========== Property 28: 跳点佣金根据套数和规则正确计算 ==========

    /**
     * Property 28 (jump point commission): For any deal amount, deal count, and jump point rules,
     * the jump point commission should be calculated using the highest applicable rate tier.
     *
     * <p><b>Validates: Requirements 16.1</b>
     */
    @Property(tries = 100)
    void jumpPointCommissionCalculatedCorrectly(
            @ForAll("dealAmounts") BigDecimal dealAmount,
            @ForAll("dealCounts") int dealCount
    ) {
        // Define a two-tier jump point rule: >=10 → 1%, >=20 → 1.5%
        String jumpPointRules = "[{\"threshold\":10,\"rate\":\"0.01\"},{\"threshold\":20,\"rate\":\"0.015\"}]";

        ProjectCommissionRule rule = ProjectCommissionRule.builder()
                .projectId(1L)
                .baseRate(new BigDecimal("0.02"))
                .jumpPointRules(jumpPointRules)
                .cashReward(BigDecimal.ZERO)
                .firstDealReward(BigDecimal.ZERO)
                .platformReward(BigDecimal.ZERO)
                .version(1)
                .status(1)
                .build();

        CommissionCalculator.CalcResult result = calculator.calculate(rule, dealAmount, dealCount);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDetail()).isNotNull();

        // Determine expected jump point commission
        BigDecimal expectedRate;
        if (dealCount >= 20) {
            expectedRate = new BigDecimal("0.015");
        } else if (dealCount >= 10) {
            expectedRate = new BigDecimal("0.01");
        } else {
            expectedRate = BigDecimal.ZERO;
        }
        BigDecimal expectedJumpPoint = dealAmount.multiply(expectedRate).setScale(2, RoundingMode.HALF_UP);

        assertThat(result.getDetail().getJumpPointCommission())
                .as("Jump point commission for dealCount=%d should use rate=%s", dealCount, expectedRate)
                .isEqualByComparingTo(expectedJumpPoint);
    }

    // ========== Property 28: 规则不完整时标记需人工确认 ==========

    /**
     * Property 28 (incomplete rules): When the commission rule has missing base rate,
     * the result should flag it for manual confirmation.
     *
     * <p><b>Validates: Requirements 16.3</b>
     */
    @Property(tries = 100)
    void incompleteRuleFlagsManualConfirmation(
            @ForAll("dealAmounts") BigDecimal dealAmount,
            @ForAll("dealCounts") int dealCount
    ) {
        // Rule with null base rate
        ProjectCommissionRule rule = ProjectCommissionRule.builder()
                .projectId(1L)
                .baseRate(null)
                .cashReward(BigDecimal.ZERO)
                .firstDealReward(BigDecimal.ZERO)
                .platformReward(BigDecimal.ZERO)
                .version(1)
                .status(1)
                .build();

        CommissionCalculator.CalcResult result = calculator.calculate(rule, dealAmount, dealCount);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getManualConfirmItems())
                .as("Missing base rate should be flagged for manual confirmation")
                .isNotEmpty();
        assertThat(result.getDetail().getBaseCommission())
                .as("Base commission should be zero when rate is missing")
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ========== Property 28: null 规则返回失败 ==========

    /**
     * Property 28 (null rule): When no commission rule exists, calculation should fail gracefully.
     *
     * <p><b>Validates: Requirements 16.3</b>
     */
    @Property(tries = 100)
    void nullRuleReturnsFailure(
            @ForAll("dealAmounts") BigDecimal dealAmount,
            @ForAll("dealCounts") int dealCount
    ) {
        CommissionCalculator.CalcResult result = calculator.calculate(null, dealAmount, dealCount);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("佣金规则不存在");
    }
}
