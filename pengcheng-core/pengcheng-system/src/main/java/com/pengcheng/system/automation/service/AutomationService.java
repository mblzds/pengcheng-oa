package com.pengcheng.system.automation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.system.automation.entity.AutomationLog;
import com.pengcheng.system.automation.entity.AutomationRule;
import com.pengcheng.system.automation.handler.RuleActionHandler;
import com.pengcheng.system.automation.mapper.AutomationLogMapper;
import com.pengcheng.system.automation.mapper.AutomationRuleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 自动化规则引擎服务
 * <p>
 * 支持基于时间、事件、条件三种触发方式，执行通知、分配、状态更新、任务创建等动作。
 */
@Slf4j
@Service
public class AutomationService {

    private final AutomationRuleMapper ruleMapper;
    private final AutomationLogMapper logMapper;
    private final JdbcTemplate jdbcTemplate;

    /** 动作分发表：actionType -> handler，由 Spring 自动收集所有 {@link RuleActionHandler} 实现 */
    private final Map<String, RuleActionHandler> handlers;

    public AutomationService(AutomationRuleMapper ruleMapper,
                             AutomationLogMapper logMapper,
                             JdbcTemplate jdbcTemplate,
                             List<RuleActionHandler> handlerList) {
        this.ruleMapper = ruleMapper;
        this.logMapper = logMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(RuleActionHandler::actionType, Function.identity()));
        log.info("[Automation] 已注册 {} 个动作处理器：{}", handlers.size(), handlers.keySet());
    }

    /**
     * 获取所有启用的规则
     */
    public List<AutomationRule> getEnabledRules() {
        return ruleMapper.selectList(new LambdaQueryWrapper<AutomationRule>()
                .eq(AutomationRule::getEnabled, true)
                .orderByDesc(AutomationRule::getPriority));
    }

    /**
     * 获取所有规则
     */
    public List<AutomationRule> getAllRules() {
        return ruleMapper.selectList(new LambdaQueryWrapper<AutomationRule>()
                .orderByDesc(AutomationRule::getPriority));
    }

    /**
     * 创建规则
     */
    public AutomationRule createRule(AutomationRule rule) {
        rule.setEnabled(true);
        rule.setTriggerCount(0);
        ruleMapper.insert(rule);
        return rule;
    }

    /**
     * 更新规则
     */
    public void updateRule(AutomationRule rule) {
        ruleMapper.updateById(rule);
    }

    /**
     * 启用/禁用规则
     */
    public void toggleRule(Long ruleId, boolean enabled) {
        AutomationRule rule = new AutomationRule();
        rule.setId(ruleId);
        rule.setEnabled(enabled);
        ruleMapper.updateById(rule);
    }

    /**
     * 删除规则
     */
    public void deleteRule(Long ruleId) {
        ruleMapper.deleteById(ruleId);
    }

    /**
     * 执行定时触发类规则（由调度器调用）
     */
    public void executeTimeBasedRules() {
        List<AutomationRule> rules = ruleMapper.selectList(new LambdaQueryWrapper<AutomationRule>()
                .eq(AutomationRule::getEnabled, true)
                .eq(AutomationRule::getTriggerType, "time_based"));

        for (AutomationRule rule : rules) {
            try {
                executeTimeRule(rule);
            } catch (Exception e) {
                log.error("执行自动化规则失败: rule={}", rule.getName(), e);
                logExecution(rule.getId(), null, "执行失败: " + e.getMessage(), 0);
            }
        }
    }

    /**
     * 执行事件触发类规则
     */
    public void executeEventRules(String eventName, Map<String, Object> eventData) {
        List<AutomationRule> rules = ruleMapper.selectList(new LambdaQueryWrapper<AutomationRule>()
                .eq(AutomationRule::getEnabled, true)
                .eq(AutomationRule::getTriggerType, "event_based"));

        for (AutomationRule rule : rules) {
            Map<String, Object> config = rule.getTriggerConfig();
            if (config != null && eventName.equals(config.get("event"))) {
                try {
                    executeAction(rule, eventData);
                    logExecution(rule.getId(), eventData, "执行成功", 1);
                    incrementTriggerCount(rule.getId());
                } catch (Exception e) {
                    log.error("执行事件规则失败: rule={}, event={}", rule.getName(), eventName, e);
                    logExecution(rule.getId(), eventData, "执行失败: " + e.getMessage(), 0);
                }
            }
        }
    }

    private void executeTimeRule(AutomationRule rule) {
        Map<String, Object> config = rule.getTriggerConfig();
        if (config == null) return;

        String targetTable = (String) config.get("target_table");
        String checkField = (String) config.get("check_field");
        Object intervalDays = config.get("interval_days");
        Object advanceDays = config.get("advance_days");

        if (targetTable == null || checkField == null) return;

        String sql;
        if (intervalDays != null) {
            sql = String.format(
                    "SELECT id, customer_name FROM %s WHERE deleted = 0 AND %s < DATE_SUB(NOW(), INTERVAL %d DAY)",
                    targetTable, checkField, ((Number) intervalDays).intValue());
        } else if (advanceDays != null) {
            sql = String.format(
                    "SELECT id, customer_name FROM %s WHERE deleted = 0 AND %s BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL %d DAY)",
                    targetTable, checkField, ((Number) advanceDays).intValue());
        } else {
            return;
        }

        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            if (!rows.isEmpty()) {
                log.info("自动化规则 [{}] 触发，匹配 {} 条记录", rule.getName(), rows.size());
                for (Map<String, Object> row : rows) {
                    executeAction(rule, row);
                }
                logExecution(rule.getId(), Map.of("matched", rows.size()), "触发成功，匹配" + rows.size() + "条", 1);
                incrementTriggerCount(rule.getId());
            }
        } catch (Exception e) {
            log.warn("查询规则目标数据失败: rule={}, sql={}", rule.getName(), sql, e);
        }
    }

    /**
     * 执行规则动作 — 通过 SPI 分发表派发到具体 {@link RuleActionHandler}。
     * 未知动作类型仅记录告警，不抛异常，避免阻塞其他规则。
     */
    private void executeAction(AutomationRule rule, Map<String, Object> data) {
        RuleActionHandler h = handlers.get(rule.getActionType());
        if (h == null) {
            log.warn("[Automation] 未知动作类型: {}, rule={}", rule.getActionType(), rule.getName());
            return;
        }
        h.execute(rule, data == null ? new HashMap<>() : data);
    }

    private void logExecution(Long ruleId, Map<String, Object> triggerData, String result, int status) {
        AutomationLog logEntry = new AutomationLog();
        logEntry.setRuleId(ruleId);
        logEntry.setTriggerData(triggerData);
        logEntry.setActionResult(result);
        logEntry.setStatus(status);
        logMapper.insert(logEntry);
    }

    private void incrementTriggerCount(Long ruleId) {
        jdbcTemplate.update("UPDATE sys_automation_rule SET trigger_count = trigger_count + 1, last_triggered_at = NOW() WHERE id = ?", ruleId);
    }

    /**
     * 获取规则执行日志
     */
    public List<AutomationLog> getRuleLogs(Long ruleId, int limit) {
        return logMapper.selectList(new LambdaQueryWrapper<AutomationLog>()
                .eq(AutomationLog::getRuleId, ruleId)
                .orderByDesc(AutomationLog::getExecutedAt)
                .last("LIMIT " + limit));
    }
}
