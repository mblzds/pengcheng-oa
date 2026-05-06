package com.pengcheng.system.automation.handler;

import com.pengcheng.system.automation.entity.AutomationRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * assign 动作：把目标行的"负责人"列更新到某个用户 ID。
 * <p>
 * actionConfig：
 * <ul>
 *   <li><b>target_table</b>：目标表</li>
 *   <li><b>owner_column</b>：负责人列，默认 "creator_id"</li>
 *   <li><b>assignee_user_id</b>：固定用户 ID；若不给则使用 data 中 "assignee_user_id"</li>
 *   <li><b>where_column</b>：定位列，默认 "id"</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssignActionHandler implements RuleActionHandler {

    public static final String ACTION_TYPE = "assign";
    private static final Pattern IDENT = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

    private final JdbcTemplate jdbcTemplate;

    @Override
    public String actionType() {
        return ACTION_TYPE;
    }

    @Override
    public void execute(AutomationRule rule, Map<String, Object> data) {
        Map<String, Object> cfg = rule.getActionConfig();
        if (cfg == null) return;
        String table = String.valueOf(cfg.get("target_table"));
        String ownerCol = cfg.getOrDefault("owner_column", "creator_id").toString();
        String whereCol = cfg.getOrDefault("where_column", "id").toString();

        if (!IDENT.matcher(table).matches() || !IDENT.matcher(ownerCol).matches()
                || !IDENT.matcher(whereCol).matches()) {
            log.error("[Automation/assign] rule={} 非法标识符", rule.getName());
            return;
        }

        Object assignee = cfg.get("assignee_user_id");
        if (assignee == null && data != null) assignee = data.get("assignee_user_id");
        if (assignee == null || data == null || !data.containsKey(whereCol)) {
            log.warn("[Automation/assign] rule={} 无可用 assignee 或 where 值", rule.getName());
            return;
        }

        int affected = jdbcTemplate.update(
                String.format("UPDATE `%s` SET `%s` = ? WHERE `%s` = ?", table, ownerCol, whereCol),
                assignee, data.get(whereCol));
        log.info("[Automation/assign] rule={} {}.{}={}，影响 {} 行",
                rule.getName(), table, ownerCol, assignee, affected);
    }
}
