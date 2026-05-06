package com.pengcheng.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.ai.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.entity.CustomerVisit;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
import com.pengcheng.realty.customer.mapper.CustomerVisitMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * AI 成交概率分析服务
 * <p>
 * 基于客户报备信息、到访次数、到访间隔、项目热度等维度计算成交概率评分。
 * 评分范围 [0, 1]，结果持久化到客户记录的 dealProbability 字段。
 * <p>
 * 降级策略：AI 服务不可用时，使用规则引擎计算评分。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisService {

    private final RealtyCustomerMapper customerMapper;
    private final CustomerVisitMapper customerVisitMapper;
    private final AiFallbackHandler fallbackHandler;
    private final AiProperties aiProperties;

    /** 客户状态：已报备 */
    private static final int STATUS_REPORTED = 1;
    /** 客户状态：已到访 */
    private static final int STATUS_VISITED = 2;

    /**
     * 计算单个客户的成交概率评分
     *
     * @param customerId 客户ID
     * @return 成交概率评分 [0, 1]
     */
    public BigDecimal calculateDealProbability(Long customerId) {
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在: " + customerId);
        }
        return calculateDealProbability(customer);
    }

    /**
     * 计算客户成交概率评分（内部方法）
     * <p>
     * 评分维度及权重：
     * <ul>
     *   <li>到访次数（30%）：到访越多，成交概率越高</li>
     *   <li>到访间隔（20%）：到访间隔越短，说明客户意向越强</li>
     *   <li>报备时长（20%）：报备后跟进时间越长，积累越多</li>
     *   <li>客户状态（30%）：已到访比已报备概率更高</li>
     * </ul>
     */
    public BigDecimal calculateDealProbability(Customer customer) {
        List<CustomerVisit> visits = getVisits(customer.getId());

        BigDecimal visitCountScore = calculateVisitCountScore(visits.size());
        BigDecimal visitIntervalScore = calculateVisitIntervalScore(visits);
        BigDecimal reportDurationScore = calculateReportDurationScore(customer);
        BigDecimal statusScore = calculateStatusScore(customer.getStatus());

        // 加权求和
        BigDecimal score = visitCountScore.multiply(new BigDecimal("0.30"))
                .add(visitIntervalScore.multiply(new BigDecimal("0.20")))
                .add(reportDurationScore.multiply(new BigDecimal("0.20")))
                .add(statusScore.multiply(new BigDecimal("0.30")));

        // 确保结果在 [0, 1] 范围内
        return clampScore(score);
    }

    /**
     * 计算并持久化客户成交概率评分
     */
    public BigDecimal calculateAndPersist(Long customerId) {
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在: " + customerId);
        }

        BigDecimal probability = fallbackHandler.executeWithFallback(
                () -> calculateDealProbability(customer),
                () -> retainLastScore(customer),
                "成交概率评分"
        );

        customer.setDealProbability(probability);
        customerMapper.updateById(customer);
        return probability;
    }

    /**
     * 批量更新所有活跃客户（已报备或已到访）的成交概率评分
     *
     * @return 更新的客户数量
     */
    public int batchUpdateDealProbability() {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Customer::getStatus, STATUS_REPORTED, STATUS_VISITED);
        List<Customer> activeCustomers = customerMapper.selectList(wrapper);

        int updatedCount = 0;
        for (Customer customer : activeCustomers) {
            try {
                BigDecimal probability = calculateDealProbability(customer);
                customer.setDealProbability(probability);
                customerMapper.updateById(customer);
                updatedCount++;
            } catch (Exception e) {
                log.warn("更新客户 {} 成交概率失败: {}", customer.getId(), e.getMessage());
                // 降级：保留上次评分
                if (aiProperties.isDealProbabilityRetainLast()) {
                    log.info("保留客户 {} 上次评分: {}", customer.getId(), customer.getDealProbability());
                }
            }
        }
        log.info("批量更新成交概率完成，共更新 {}/{} 个客户", updatedCount, activeCustomers.size());
        return updatedCount;
    }

    // ========== 评分维度计算 ==========

    /**
     * 到访次数评分：0次=0.1, 1次=0.4, 2次=0.6, 3次=0.8, 4次及以上=1.0
     */
    BigDecimal calculateVisitCountScore(int visitCount) {
        if (visitCount == 0) return new BigDecimal("0.10");
        if (visitCount == 1) return new BigDecimal("0.40");
        if (visitCount == 2) return new BigDecimal("0.60");
        if (visitCount == 3) return new BigDecimal("0.80");
        return BigDecimal.ONE;
    }

    /**
     * 到访间隔评分：平均间隔越短，评分越高
     * 无到访记录=0.1, <=3天=1.0, <=7天=0.7, <=14天=0.4, >14天=0.2
     */
    BigDecimal calculateVisitIntervalScore(List<CustomerVisit> visits) {
        if (visits.size() < 2) {
            return visits.isEmpty() ? new BigDecimal("0.10") : new BigDecimal("0.50");
        }

        // 计算平均到访间隔（天）
        long totalDays = 0;
        for (int i = 1; i < visits.size(); i++) {
            LocalDateTime prev = visits.get(i - 1).getActualVisitTime();
            LocalDateTime curr = visits.get(i).getActualVisitTime();
            if (prev != null && curr != null) {
                totalDays += Math.abs(ChronoUnit.DAYS.between(prev, curr));
            }
        }
        long avgDays = totalDays / (visits.size() - 1);

        if (avgDays <= 3) return BigDecimal.ONE;
        if (avgDays <= 7) return new BigDecimal("0.70");
        if (avgDays <= 14) return new BigDecimal("0.40");
        return new BigDecimal("0.20");
    }

    /**
     * 报备时长评分：报备后跟进时间越长，说明持续关注
     * <1天=0.2, 1-3天=0.4, 3-7天=0.6, 7-14天=0.8, >14天=0.5（可能冷却）
     */
    BigDecimal calculateReportDurationScore(Customer customer) {
        if (customer.getCreateTime() == null) {
            return new BigDecimal("0.20");
        }
        long days = ChronoUnit.DAYS.between(customer.getCreateTime(), LocalDateTime.now());
        if (days < 1) return new BigDecimal("0.20");
        if (days <= 3) return new BigDecimal("0.40");
        if (days <= 7) return new BigDecimal("0.60");
        if (days <= 14) return new BigDecimal("0.80");
        return new BigDecimal("0.50"); // 超过14天可能冷却
    }

    /**
     * 客户状态评分：已到访=0.7, 已报备=0.3
     */
    BigDecimal calculateStatusScore(Integer status) {
        if (status == null) return BigDecimal.ZERO;
        return switch (status) {
            case STATUS_VISITED -> new BigDecimal("0.70");
            case STATUS_REPORTED -> new BigDecimal("0.30");
            default -> BigDecimal.ZERO;
        };
    }

    // ========== 辅助方法 ==========

    private List<CustomerVisit> getVisits(Long customerId) {
        LambdaQueryWrapper<CustomerVisit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerVisit::getCustomerId, customerId)
                .orderByAsc(CustomerVisit::getActualVisitTime);
        return customerVisitMapper.selectList(wrapper);
    }

    private BigDecimal retainLastScore(Customer customer) {
        if (customer.getDealProbability() != null) {
            return customer.getDealProbability();
        }
        return new BigDecimal("0.10");
    }

    /**
     * 将评分限制在 [0, 1] 范围内
     */
    static BigDecimal clampScore(BigDecimal score) {
        if (score.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;
        if (score.compareTo(BigDecimal.ONE) > 0) return BigDecimal.ONE;
        return score.setScale(4, RoundingMode.HALF_UP);
    }
}
