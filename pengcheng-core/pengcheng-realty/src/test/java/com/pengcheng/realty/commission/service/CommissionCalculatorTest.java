package com.pengcheng.realty.commission.service;

import com.pengcheng.realty.project.entity.ProjectCommissionRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CommissionCalculator")
class CommissionCalculatorTest {

    private final CommissionCalculator calculator = new CommissionCalculator();

    @Test
    @DisplayName("calculate 对无效成交金额返回失败结果")
    void calculateRejectsInvalidDealAmount() {
        CommissionCalculator.CalcResult result = calculator.calculate(
                ProjectCommissionRule.builder().baseRate(new BigDecimal("0.018")).build(),
                BigDecimal.ZERO,
                1
        );

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("成交金额无效");
    }

    @Test
    @DisplayName("calculate 在规则完整时返回所有佣金明细")
    void calculateBuildsFullCommissionDetail() {
        ProjectCommissionRule rule = ProjectCommissionRule.builder()
                .baseRate(new BigDecimal("0.018"))
                .jumpPointRules("[{\"threshold\":5,\"rate\":\"0.003\"},{\"threshold\":10,\"rate\":\"0.005\"}]")
                .cashReward(new BigDecimal("3000"))
                .firstDealReward(new BigDecimal("5000"))
                .platformReward(new BigDecimal("1000"))
                .build();

        CommissionCalculator.CalcResult result = calculator.calculate(rule, new BigDecimal("8000000"), 10);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getManualConfirmItems()).isEmpty();
        assertThat(result.getMessage()).contains("佣金计算完成");
        assertThat(result.getDetail().getBaseCommission()).isEqualByComparingTo("144000.00");
        assertThat(result.getDetail().getJumpPointCommission()).isEqualByComparingTo("40000.00");
        assertThat(result.getDetail().getCashReward()).isEqualByComparingTo("3000");
        assertThat(result.getDetail().getFirstDealReward()).isEqualByComparingTo("5000");
        assertThat(result.getDetail().getPlatformReward()).isEqualByComparingTo("1000");
    }

    @Test
    @DisplayName("calculate 对非法跳点规则回退为人工确认")
    void calculateFallsBackWhenJumpPointRuleInvalid() {
        ProjectCommissionRule rule = ProjectCommissionRule.builder()
                .baseRate(new BigDecimal("0.018"))
                .jumpPointRules("{bad-json}")
                .build();

        CommissionCalculator.CalcResult result = calculator.calculate(rule, new BigDecimal("1000000"), 3);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDetail().getJumpPointCommission()).isEqualByComparingTo("0");
        assertThat(result.getManualConfirmItems()).contains("跳点规则格式异常，请人工确认跳点佣金金额");
        assertThat(result.getMessage()).contains("部分项需人工确认");
    }

    @Test
    @DisplayName("parseJumpPointRules 支持空数组和非法格式")
    void parseJumpPointRulesHandlesEmptyAndInvalidInput() {
        assertThat(calculator.parseJumpPointRules("[]")).isEmpty();
        assertThat(calculator.parseJumpPointRules("   ")).isEmpty();

        assertThatThrownBy(() -> calculator.parseJumpPointRules("{\"threshold\":5}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JSON数组");
    }

    @Test
    @DisplayName("calculateJumpPointCommission 对无命中档位返回零")
    void calculateJumpPointCommissionReturnsZeroWhenThresholdNotReached() {
        BigDecimal result = calculator.calculateJumpPointCommission(
                "[{\"threshold\":5,\"rate\":\"0.003\"}]",
                new BigDecimal("800000"),
                2
        );

        assertThat(result).isEqualByComparingTo("0.00");
    }
}
