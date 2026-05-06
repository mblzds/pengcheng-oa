package com.pengcheng.system.automation.handler;

import com.pengcheng.system.automation.entity.AutomationRule;

import java.util.Map;

/**
 * 自动化规则动作 SPI。
 * <p>
 * 每个实现声明 {@link #actionType()} 作为 {@code sys_automation_rule.action_type} 的匹配键。
 * 由 Spring 自动发现并注册到 {@code AutomationService} 的动作分发表。
 * <p>
 * 新增动作 = 新增一个 {@code @Component} 实现，无需改动分发代码。
 */
public interface RuleActionHandler {

    /** 动作类型标识，对应 action_type 列枚举值 */
    String actionType();

    /**
     * 执行动作。
     *
     * @param rule 当前触发的规则（含 actionConfig）
     * @param data 触发上下文（定时扫描的数据行、或事件 payload）
     */
    void execute(AutomationRule rule, Map<String, Object> data);
}
