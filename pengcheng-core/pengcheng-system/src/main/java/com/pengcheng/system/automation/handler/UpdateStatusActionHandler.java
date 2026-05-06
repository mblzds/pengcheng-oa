package com.pengcheng.system.automation.handler;

import com.pengcheng.system.automation.entity.AutomationRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * update_status 动作：按 actionConfig 指定的表/字段/值，更新触发行的状态列。
 * <p>
 * actionConfig 要求：
 * <ul>
 *   <li><b>target_table</b>：目标表名（白名单校验）</li>
 *   <li><b>target_field</b>：要更新的列（白名单校验）</li>
 *   <li><b>new_value</b>：新值（字符串或数值）</li>
 *   <li><b>where_column</b>：定位列，默认 "id"</li>
 * </ul>
 * data 中必须包含 {@code where_column} 对应的值。表名/列名使用正则 <code>\w+</code> 校验，
 * 防止 SQL 注入。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateStatusActionHandler implements RuleActionHandler {

    public static final String ACTION_TYPE = "update_status";
    private static final Pattern IDENT = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

    private final JdbcTemplate jdbcTemplate;

    @Override
    public String actionType() {
        return ACTION_TYPE;
    }

    @Override
    public void execute(AutomationRule rule, Map<String, Object> data) {
        Map<String, Object> cfg = rule.getActionConfig();
        if (cfg == null) {
            log.warn("[Automation/update_status] rule={} actionConfig 缺失", rule.getName());
            return;
        }
        String table = str(cfg, "target_table");
        String field = str(cfg, "target_field");
        Object newVal = cfg.get("new_value");
        String whereCol = cfg.getOrDefault("where_column", "id").toString();

        if (!isIdent(table) || !isIdent(field) || !isIdent(whereCol)) {
            log.error("[Automation/update_status] rule={} 非法标识符 table={}, field={}, where={}",
                    rule.getName(), table, field, whereCol);
            return;
        }
        if (data == null || !data.containsKey(whereCol)) {
            log.warn("[Automation/update_status] rule={} 触发上下文缺少 {}", rule.getName(), whereCol);
            return;
        }

        String sql = String.format("UPDATE `%s` SET `%s` = ? WHERE `%s` = ?", table, field, whereCol);
        int affected = jdbcTemplate.update(sql, newVal, data.get(whereCol));
        log.info("[Automation/update_status] rule={} {}.{}={}，影响 {} 行",
                rule.getName(), table, field, newVal, affected);
    }

    private static boolean isIdent(String s) { return s != null && IDENT.matcher(s).matches(); }
    private static String str(Map<String, Object> m, String k) {
        Object v = m.get(k); return v == null ? null : v.toString();
    }
}
