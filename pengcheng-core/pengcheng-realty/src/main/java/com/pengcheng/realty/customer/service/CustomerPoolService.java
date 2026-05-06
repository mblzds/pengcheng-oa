package com.pengcheng.realty.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengcheng.realty.customer.dto.PoolRecycleConfigDTO;
import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.entity.CustomerPoolEventLog;
import com.pengcheng.realty.customer.mapper.CustomerPoolEventLogMapper;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
import com.pengcheng.system.entity.SysConfigGroup;
import com.pengcheng.system.service.SysConfigGroupService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 客户公海/私海池管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerPoolService {

    static final String CONFIG_GROUP_CODE = "customerPoolConfig";

    private final RealtyCustomerMapper customerMapper;
    private final CustomerPoolEventLogMapper customerPoolEventLogMapper;
    private final SysConfigGroupService configGroupService;
    private final ObjectMapper objectMapper;

    /** 池类型：公海 */
    public static final int POOL_PUBLIC = 1;
    /** 池类型：私海 */
    public static final int POOL_PRIVATE = 2;

    /** 客户状态：已报备 */
    private static final int STATUS_REPORTED = 1;
    /** 客户状态：已成交 */
    private static final int STATUS_DEAL = 3;

    /** 默认无跟进回收天数 */
    private static final int DEFAULT_NO_FOLLOW_DAYS = 7;
    /** 默认未到访回收天数 */
    private static final int DEFAULT_NO_VISIT_DAYS = 30;
    /** 默认保护期天数 */
    private static final int DEFAULT_PROTECTION_DAYS = 3;
    /** 分批处理每批大小 */
    private static final int BATCH_SIZE = 100;

    /** 可配置的回收规则参数 */
    private volatile int noFollowDays = DEFAULT_NO_FOLLOW_DAYS;
    private volatile int noVisitDays = DEFAULT_NO_VISIT_DAYS;

    @PostConstruct
    void loadRecycleConfig() {
        PoolRecycleConfigDTO config = readRecycleConfig();
        this.noFollowDays = normalizeConfigValue(config.getNoFollowDays(), DEFAULT_NO_FOLLOW_DAYS);
        this.noVisitDays = normalizeConfigValue(config.getNoVisitDays(), DEFAULT_NO_VISIT_DAYS);
        log.info("已加载公海池回收规则: noFollowDays={}, noVisitDays={}", noFollowDays, noVisitDays);
    }

    /**
     * 获取当前无跟进回收天数配置
     */
    public int getNoFollowDays() {
        return noFollowDays;
    }

    /**
     * 获取当前未到访回收天数配置
     */
    public int getNoVisitDays() {
        return noVisitDays;
    }

    /**
     * 更新回收规则配置
     */
    public void updateRecycleConfig(Integer noFollowDays, Integer noVisitDays) {
        PoolRecycleConfigDTO currentConfig = readRecycleConfig();
        PoolRecycleConfigDTO targetConfig = PoolRecycleConfigDTO.builder()
                .noFollowDays(resolveConfigValue(noFollowDays, currentConfig.getNoFollowDays(), DEFAULT_NO_FOLLOW_DAYS))
                .noVisitDays(resolveConfigValue(noVisitDays, currentConfig.getNoVisitDays(), DEFAULT_NO_VISIT_DAYS))
                .protectionDays(resolveConfigValue(null, currentConfig.getProtectionDays(), DEFAULT_PROTECTION_DAYS))
                .autoRecycleEnabled(currentConfig.getAutoRecycleEnabled() != null ? currentConfig.getAutoRecycleEnabled() : Boolean.TRUE)
                .build();

        try {
            configGroupService.saveConfig(CONFIG_GROUP_CODE, objectMapper.writeValueAsString(targetConfig));
        } catch (Exception e) {
            throw new IllegalStateException("保存公海池回收规则失败", e);
        }

        this.noFollowDays = targetConfig.getNoFollowDays();
        this.noVisitDays = targetConfig.getNoVisitDays();
        log.info("回收规则已更新: noFollowDays={}, noVisitDays={}", this.noFollowDays, this.noVisitDays);
    }

    /**
     * 执行公海池自动回收
     * <p>
     * 分批处理私海池中满足回收条件的客户：
     * <ul>
     *   <li>超过 noFollowDays 天无跟进记录</li>
     *   <li>超过 noVisitDays 天未到访（状态仍为已报备）</li>
     * </ul>
     * 已成交客户不参与回收。使用乐观锁（基于 updateTime）避免并发冲突。
     *
     * @return 本次回收的客户数量
     */
    @Transactional
    public int recycleToPublicPool() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime noFollowThreshold = now.minusDays(noFollowDays);
        LocalDateTime noVisitThreshold = now.minusDays(noVisitDays);

        int totalRecycled = 0;
        int page = 1;

        while (true) {
            // 查询私海池中满足回收条件的客户（分批）
            LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Customer::getPoolType, POOL_PRIVATE)
                    .ne(Customer::getStatus, STATUS_DEAL) // 已成交不回收
                    .and(w -> w
                            // 条件1：超过无跟进天数
                            .lt(Customer::getLastFollowTime, noFollowThreshold)
                            .or()
                            // 条件2：状态为已报备且超过未到访天数
                            .nested(inner -> inner
                                    .eq(Customer::getStatus, STATUS_REPORTED)
                                    .lt(Customer::getCreateTime, noVisitThreshold)
                            )
                    );

            Page<Customer> pageResult = customerMapper.selectPage(
                    new Page<>(page, BATCH_SIZE), wrapper);
            List<Customer> candidates = pageResult.getRecords();

            if (candidates.isEmpty()) {
                break;
            }

            for (Customer customer : candidates) {
                // 使用乐观锁：基于 updateTime 确保并发安全
                UpdateWrapper<Customer> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("id", customer.getId())
                        .eq("pool_type", POOL_PRIVATE) // 确保仍在私海
                        .eq("update_time", customer.getUpdateTime()) // 乐观锁
                        .set("pool_type", POOL_PUBLIC);

                int updated = customerMapper.update(null, updateWrapper);
                if (updated > 0) {
                    customerPoolEventLogMapper.insert(CustomerPoolEventLog.builder()
                            .customerId(customer.getId())
                            .eventType(CustomerPoolEventLog.EVENT_TYPE_RECYCLE)
                            .eventSource(CustomerPoolEventLog.EVENT_SOURCE_AUTO)
                            .eventTime(now)
                            .remark("auto recycle to public pool")
                            .build());
                    totalRecycled++;
                    log.debug("客户已回收至公海池: customerId={}", customer.getId());
                }
            }

            if (candidates.size() < BATCH_SIZE) {
                break;
            }
            page++;
        }

        log.info("公海池回收完成，共回收 {} 个客户", totalRecycled);
        return totalRecycled;
    }

    /**
     * 从公海池领取客户
     * <p>
     * 将客户从公海池转入领取人的私海池，并重置保护期。
     *
     * @param customerId 客户ID
     * @param userId     领取人用户ID
     */
    @Transactional
    public void claimFromPublicPool(Long customerId, Long userId) {
        if (customerId == null) {
            throw new IllegalArgumentException("客户ID不能为空");
        }
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在");
        }
        if (customer.getPoolType() != POOL_PUBLIC) {
            throw new IllegalStateException("该客户不在公海池中，无法领取");
        }

        LocalDateTime now = LocalDateTime.now();
        customer.setPoolType(POOL_PRIVATE);
        customer.setProtectionExpireTime(now.plusDays(DEFAULT_PROTECTION_DAYS));
        customer.setLastFollowTime(now);
        customer.setCreatorId(userId);

        customerMapper.updateById(customer);
        customerPoolEventLogMapper.insert(CustomerPoolEventLog.builder()
                .customerId(customerId)
                .eventType(CustomerPoolEventLog.EVENT_TYPE_CLAIM)
                .eventSource(CustomerPoolEventLog.EVENT_SOURCE_MANUAL)
                .operatorId(userId)
                .eventTime(now)
                .remark("claim from public pool")
                .build());
        log.info("客户已从公海池领取: customerId={}, userId={}", customerId, userId);
    }

    private PoolRecycleConfigDTO readRecycleConfig() {
        try {
            SysConfigGroup configGroup = configGroupService.getByGroupCode(CONFIG_GROUP_CODE);
            if (configGroup == null || configGroup.getConfigValue() == null || configGroup.getConfigValue().isBlank()) {
                return defaultConfig();
            }

            PoolRecycleConfigDTO config = objectMapper.readValue(configGroup.getConfigValue(), PoolRecycleConfigDTO.class);
            if (config == null) {
                return defaultConfig();
            }
            if (config.getProtectionDays() == null) {
                config.setProtectionDays(DEFAULT_PROTECTION_DAYS);
            }
            if (config.getAutoRecycleEnabled() == null) {
                config.setAutoRecycleEnabled(Boolean.TRUE);
            }
            return config;
        } catch (Exception e) {
            log.warn("读取公海池回收规则失败，回退默认配置", e);
            return defaultConfig();
        }
    }

    private PoolRecycleConfigDTO defaultConfig() {
        return PoolRecycleConfigDTO.builder()
                .noFollowDays(DEFAULT_NO_FOLLOW_DAYS)
                .noVisitDays(DEFAULT_NO_VISIT_DAYS)
                .protectionDays(DEFAULT_PROTECTION_DAYS)
                .autoRecycleEnabled(Boolean.TRUE)
                .build();
    }

    private int resolveConfigValue(Integer incomingValue, Integer storedValue, int defaultValue) {
        if (incomingValue != null && incomingValue > 0) {
            return incomingValue;
        }
        return normalizeConfigValue(storedValue, defaultValue);
    }

    private int normalizeConfigValue(Integer value, int defaultValue) {
        return value != null && value > 0 ? value : defaultValue;
    }

    /**
     * 判断客户是否应被回收至公海池
     * <p>
     * 纯逻辑判断方法，便于属性测试验证。
     *
     * @param customer          客户实体
     * @param now               当前时间
     * @param noFollowDays      无跟进天数阈值
     * @param noVisitDays       未到访天数阈值
     * @return true 表示应回收
     */
    public static boolean shouldRecycle(Customer customer, LocalDateTime now,
                                        int noFollowDays, int noVisitDays) {
        if (customer == null) {
            return false;
        }
        // 不在私海池的不回收
        if (customer.getPoolType() != POOL_PRIVATE) {
            return false;
        }
        // 已成交不回收
        if (customer.getStatus() == STATUS_DEAL) {
            return false;
        }

        // 条件1：超过无跟进天数
        if (customer.getLastFollowTime() != null
                && customer.getLastFollowTime().isBefore(now.minusDays(noFollowDays))) {
            return true;
        }

        // 条件2：状态为已报备且超过未到访天数（基于创建时间）
        if (customer.getStatus() == STATUS_REPORTED
                && customer.getCreateTime() != null
                && customer.getCreateTime().isBefore(now.minusDays(noVisitDays))) {
            return true;
        }

        return false;
    }
}
