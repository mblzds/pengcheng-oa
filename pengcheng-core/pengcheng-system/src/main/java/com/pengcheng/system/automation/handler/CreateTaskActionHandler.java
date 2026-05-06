package com.pengcheng.system.automation.handler;

import com.pengcheng.system.automation.entity.AutomationRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * create_task 动作：向 sys_todo 表插入一条待办，由待办模块消费。
 * <p>
 * actionConfig：
 * <ul>
 *   <li><b>title</b>：待办标题（可含占位符）</li>
 *   <li><b>content</b>：待办内容（可含占位符）</li>
 *   <li><b>assignee</b>：执行人 user_id（或从 data.assignee_user_id 读取）</li>
 *   <li><b>priority</b>：优先级 1-低 2-中 3-高，默认 2</li>
 * </ul>
 * 若 sys_todo 表不存在或字段不匹配，只会 warn 不抛异常（保证规则引擎不因单个动作失败中断）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreateTaskActionHandler implements RuleActionHandler {

    public static final String ACTION_TYPE = "create_task";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public String actionType() {
        return ACTION_TYPE;
    }

    @Override
    public void execute(AutomationRule rule, Map<String, Object> data) {
        Map<String, Object> cfg = rule.getActionConfig();
        if (cfg == null) {
            log.warn("[Automation/create_task] rule={} actionConfig 缺失", rule.getName());
            return;
        }
        String title = NotifyActionHandler.render(String.valueOf(cfg.getOrDefault("title", rule.getName())), data);
        String content = NotifyActionHandler.render(String.valueOf(cfg.getOrDefault("content", "")), data);
        Object assignee = cfg.get("assignee");
        if (assignee == null && data != null) assignee = data.get("assignee_user_id");
        int priority = cfg.get("priority") instanceof Number n ? n.intValue() : 2;

        try {
            jdbcTemplate.update(
                    "INSERT INTO sys_todo (title, content, assignee_id, priority, status, create_time) " +
                            "VALUES (?, ?, ?, ?, 0, NOW())",
                    title, content, assignee, priority);
            log.info("[Automation/create_task] rule={} 已建待办 title={}", rule.getName(), title);
        } catch (Exception e) {
            // 待办表 schema 可能随版本演进；失败不阻断其他动作执行
            log.warn("[Automation/create_task] rule={} 创建待办失败：{}", rule.getName(), e.getMessage());
        }
    }
}
