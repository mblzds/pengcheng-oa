package com.pengcheng.realty.commission.service;

import com.pengcheng.realty.commission.dto.CommissionDetailDTO;
import com.pengcheng.realty.project.entity.ProjectCommissionRule;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 佣金计算引擎
 * <p>
 * 根据项目佣金规则自动计算各项佣金：
 * <ul>
 *   <li>基础佣金 = 成交金额 × 基础佣金比例</li>
 *   <li>跳点佣金根据累计成交套数和跳点规则计算</li>
 *   <li>现金奖、开单奖、平台奖励按项目规则计算</li>
 * </ul>
 */
@Slf4j
@Component
public class CommissionCalculator {

    /**
     * 根据项目佣金规则计算各项佣金
     *
     * @param rule       项目佣金规则
     * @param dealAmount 成交金额
     * @param dealCount  该项目累计成交套数（含本次）
     * @return 计算结果，包含各项佣金明细和需人工确认的项
     */
    public CalcResult calculate(ProjectCommissionRule rule, BigDecimal dealAmount, int dealCount) {
        if (rule == null) {
            return CalcResult.builder()
                    .success(false)
                    .message("项目佣金规则不存在，请手动录入佣金")
                    .build();
        }
        if (dealAmount == null || dealAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return CalcResult.builder()
                    .success(false)
                    .message("成交金额无效，请手动录入佣金")
                    .build();
        }

        List<String> manualConfirmItems = new ArrayList<>();

        // 基础佣金
        BigDecimal baseCommission = BigDecimal.ZERO;
        if (rule.getBaseRate() != null && rule.getBaseRate().compareTo(BigDecimal.ZERO) > 0) {
            baseCommission = dealAmount.multiply(rule.getBaseRate()).setScale(2, RoundingMode.HALF_UP);
        } else {
            manualConfirmItems.add("基础佣金比例未设置，请人工确认基础佣金金额");
        }

        // 跳点佣金
        BigDecimal jumpPointCommission = BigDecimal.ZERO;
        if (rule.getJumpPointRules() != null && !rule.getJumpPointRules().isBlank()) {
            jumpPointCommission = calculateJumpPointCommission(rule.getJumpPointRules(), dealAmount, dealCount);
            if (jumpPointCommission.compareTo(BigDecimal.ZERO) < 0) {
                // 解析失败
                jumpPointCommission = BigDecimal.ZERO;
                manualConfirmItems.add("跳点规则格式异常，请人工确认跳点佣金金额");
            }
        }

        // 现金奖
        BigDecimal cashReward = BigDecimal.ZERO;
        if (rule.getCashReward() != null && rule.getCashReward().compareTo(BigDecimal.ZERO) > 0) {
            cashReward = rule.getCashReward();
        }

        // 开单奖
        BigDecimal firstDealReward = BigDecimal.ZERO;
        if (rule.getFirstDealReward() != null && rule.getFirstDealReward().compareTo(BigDecimal.ZERO) > 0) {
            firstDealReward = rule.getFirstDealReward();
        }

        // 平台奖励
        BigDecimal platformReward = BigDecimal.ZERO;
        if (rule.getPlatformReward() != null && rule.getPlatformReward().compareTo(BigDecimal.ZERO) > 0) {
            platformReward = rule.getPlatformReward();
        }

        CommissionDetailDTO detail = CommissionDetailDTO.builder()
                .baseCommission(baseCommission)
                .jumpPointCommission(jumpPointCommission)
                .cashReward(cashReward)
                .firstDealReward(firstDealReward)
                .platformReward(platformReward)
                .build();

        return CalcResult.builder()
                .success(true)
                .detail(detail)
                .manualConfirmItems(manualConfirmItems)
                .message(manualConfirmItems.isEmpty() ? "佣金计算完成" : "佣金计算完成，部分项需人工确认")
                .build();
    }

    /**
     * 计算跳点佣金
     * <p>
     * 跳点规则 JSON 格式: [{"threshold":10,"rate":"0.01"},{"threshold":20,"rate":"0.015"}]
     * 含义：累计成交达到 threshold 套后，额外佣金比例为 rate。
     * 取满足条件的最高档位。
     *
     * @param jumpPointRulesJson 跳点规则 JSON
     * @param dealAmount         成交金额
     * @param dealCount          累计成交套数
     * @return 跳点佣金金额，解析失败返回 -1
     */
    BigDecimal calculateJumpPointCommission(String jumpPointRulesJson, BigDecimal dealAmount, int dealCount) {
        try {
            List<JumpPointRule> rules = parseJumpPointRules(jumpPointRulesJson);
            if (rules.isEmpty()) {
                return BigDecimal.ZERO;
            }

            // 按 threshold 降序排列，取第一个满足条件的
            BigDecimal applicableRate = BigDecimal.ZERO;
            for (JumpPointRule rule : rules) {
                if (dealCount >= rule.getThreshold()) {
                    if (rule.getRate().compareTo(applicableRate) > 0) {
                        applicableRate = rule.getRate();
                    }
                }
            }

            return dealAmount.multiply(applicableRate).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.warn("跳点规则解析失败: {}", e.getMessage());
            return BigDecimal.valueOf(-1);
        }
    }

    /**
     * 解析跳点规则 JSON
     */
    List<JumpPointRule> parseJumpPointRules(String json) {
        List<JumpPointRule> rules = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return rules;
        }

        // Simple JSON array parsing without external library dependency
        String trimmed = json.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            throw new IllegalArgumentException("跳点规则格式错误：应为JSON数组");
        }

        String inner = trimmed.substring(1, trimmed.length() - 1).trim();
        if (inner.isEmpty()) {
            return rules;
        }

        // Split by "},{"
        String[] parts = inner.split("\\}\\s*,\\s*\\{");
        for (String part : parts) {
            String cleaned = part.replace("{", "").replace("}", "").trim();
            Integer threshold = null;
            BigDecimal rate = null;

            String[] fields = cleaned.split(",");
            for (String field : fields) {
                String[] kv = field.split(":");
                if (kv.length == 2) {
                    String key = kv[0].trim().replace("\"", "");
                    String value = kv[1].trim().replace("\"", "");
                    if ("threshold".equals(key)) {
                        threshold = Integer.parseInt(value);
                    } else if ("rate".equals(key)) {
                        rate = new BigDecimal(value);
                    }
                }
            }

            if (threshold != null && rate != null) {
                rules.add(JumpPointRule.builder().threshold(threshold).rate(rate).build());
            }
        }

        return rules;
    }

    /**
     * 佣金计算结果
     */
    @Data
    @Builder
    public static class CalcResult {
        /** 是否计算成功 */
        private boolean success;
        /** 佣金明细 */
        private CommissionDetailDTO detail;
        /** 需人工确认的项 */
        private List<String> manualConfirmItems;
        /** 结果消息 */
        private String message;
    }

    /**
     * 跳点规则
     */
    @Data
    @Builder
    public static class JumpPointRule {
        /** 成交套数阈值 */
        private int threshold;
        /** 额外佣金比例 */
        private BigDecimal rate;
    }
}
