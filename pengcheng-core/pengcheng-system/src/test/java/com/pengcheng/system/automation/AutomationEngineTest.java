package com.pengcheng.system.automation;

import com.pengcheng.system.automation.entity.AutomationRule;
import com.pengcheng.system.automation.handler.*;
import com.pengcheng.system.channel.entity.ChannelConfig;
import com.pengcheng.system.channel.service.ChannelPushService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * 手写 ChannelPushService 的测试替身，避免 Mockito 在 JDK 17.0.18
 * 下对该 Service 生成 byte-buddy 代理失败的已知问题。
 */
class RecordingChannelPushService extends ChannelPushService {
    final List<Object[]> calls = new ArrayList<>();
    RuntimeException throwOnBroadcast;
    RecordingChannelPushService() { super(null, null, null); }
    @Override
    public List<ChannelConfig> getAllChannels() { return List.of(); }
    @Override
    public void broadcast(String title, String content, String messageType) {
        calls.add(new Object[]{title, content, messageType});
        if (throwOnBroadcast != null) throw throwOnBroadcast;
    }
}

/**
 * 同样手写 JdbcTemplate 替身：记录 (sql, args) 供断言。
 * 可选模拟一次失败（throwOnNext = ...）。
 */
class RecordingJdbcTemplate extends JdbcTemplate {
    final List<Object[]> calls = new ArrayList<>();
    RuntimeException throwOnNext;
    int returnRows = 1;
    RecordingJdbcTemplate() { super(); }
    @Override
    public int update(String sql, Object... args) {
        calls.add(concat(sql, args));
        if (throwOnNext != null) { RuntimeException e = throwOnNext; throwOnNext = null; throw e; }
        return returnRows;
    }
    private static Object[] concat(String sql, Object... args) {
        Object[] out = new Object[args.length + 1];
        out[0] = sql;
        System.arraycopy(args, 0, out, 1, args.length);
        return out;
    }
}

/**
 * P1-2 自动化规则引擎：SPI + 4 个动作处理器的分发与执行
 */
@DisplayName("AutomationEngine — 动作 SPI")
class AutomationEngineTest {

    private AutomationRule ruleOf(String actionType, Map<String, Object> actionConfig) {
        AutomationRule r = new AutomationRule();
        r.setId(1L);
        r.setName("test rule");
        r.setEnabled(true);
        r.setTriggerType("time_based");
        r.setActionType(actionType);
        r.setActionConfig(actionConfig);
        return r;
    }

    // ------- NotifyActionHandler -------

    @Test
    @DisplayName("notify：渲染占位符后调 ChannelPushService.broadcast")
    void notify_broadcastsWithRenderedText() {
        RecordingChannelPushService push = new RecordingChannelPushService();
        NotifyActionHandler h = new NotifyActionHandler(push);

        Map<String, Object> cfg = Map.of(
                "title", "[跟进] 客户 {customer_name}",
                "template", "{customer_name} 已 {days} 天未跟进",
                "messageType", "automation");
        Map<String, Object> data = Map.of("customer_name", "张先生", "days", 9);

        h.execute(ruleOf("notify", cfg), data);

        assertThat(push.calls).hasSize(1);
        Object[] args = push.calls.get(0);
        assertThat(args[0]).isEqualTo("[跟进] 客户 张先生");
        assertThat(args[1]).isEqualTo("张先生 已 9 天未跟进");
        assertThat(args[2]).isEqualTo("automation");
    }

    @Test
    @DisplayName("notify：broadcast 抛异常 → 规则不崩（日志降级）")
    void notify_swallowsBroadcastErrors() {
        RecordingChannelPushService push = new RecordingChannelPushService();
        push.throwOnBroadcast = new RuntimeException("network down");
        NotifyActionHandler h = new NotifyActionHandler(push);

        assertThatCode(() -> h.execute(ruleOf("notify", Map.of()), new HashMap<>()))
                .doesNotThrowAnyException();
    }

    // ------- UpdateStatusActionHandler -------

    @Test
    @DisplayName("update_status：生成参数化 UPDATE 且受白名单保护")
    void updateStatus_runsParameterizedUpdate() {
        RecordingJdbcTemplate jt = new RecordingJdbcTemplate();
        UpdateStatusActionHandler h = new UpdateStatusActionHandler(jt);
        Map<String, Object> cfg = Map.of(
                "target_table", "customer",
                "target_field", "status",
                "new_value", 3,
                "where_column", "id");
        h.execute(ruleOf("update_status", cfg), Map.of("id", 40001L));

        assertThat(jt.calls).hasSize(1);
        Object[] call = jt.calls.get(0);
        assertThat(call[0]).isEqualTo("UPDATE `customer` SET `status` = ? WHERE `id` = ?");
        assertThat(call[1]).isEqualTo(3);
        assertThat(call[2]).isEqualTo(40001L);
    }

    @Test
    @DisplayName("update_status：非法标识符 → 拒绝执行（SQL 注入防护）")
    void updateStatus_rejectsMaliciousIdent() {
        RecordingJdbcTemplate jt = new RecordingJdbcTemplate();
        UpdateStatusActionHandler h = new UpdateStatusActionHandler(jt);
        Map<String, Object> cfg = Map.of(
                "target_table", "customer; DROP TABLE customer;--",
                "target_field", "status",
                "new_value", 3);

        h.execute(ruleOf("update_status", cfg), Map.of("id", 1L));
        assertThat(jt.calls).isEmpty();
    }

    @Test
    @DisplayName("update_status：缺少 where 值 → 不执行 SQL")
    void updateStatus_noWhereValue() {
        RecordingJdbcTemplate jt = new RecordingJdbcTemplate();
        UpdateStatusActionHandler h = new UpdateStatusActionHandler(jt);
        Map<String, Object> cfg = Map.of(
                "target_table", "customer",
                "target_field", "status",
                "new_value", 1);
        h.execute(ruleOf("update_status", cfg), Map.of());
        assertThat(jt.calls).isEmpty();
    }

    // ------- AssignActionHandler -------

    @Test
    @DisplayName("assign：把负责人列更新到 data.assignee_user_id")
    void assign_writesOwner() {
        RecordingJdbcTemplate jt = new RecordingJdbcTemplate();
        AssignActionHandler h = new AssignActionHandler(jt);
        Map<String, Object> cfg = Map.of(
                "target_table", "customer",
                "owner_column", "creator_id",
                "where_column", "id");
        h.execute(ruleOf("assign", cfg), Map.of("id", 40003L, "assignee_user_id", 5L));

        assertThat(jt.calls).hasSize(1);
        Object[] call = jt.calls.get(0);
        assertThat(call[0]).isEqualTo("UPDATE `customer` SET `creator_id` = ? WHERE `id` = ?");
        assertThat(call[1]).isEqualTo(5L);
        assertThat(call[2]).isEqualTo(40003L);
    }

    // ------- CreateTaskActionHandler -------

    @Test
    @DisplayName("create_task：拼 INSERT，标题/内容含渲染后的字段")
    void createTask_insertsOrLogs() {
        RecordingJdbcTemplate jt = new RecordingJdbcTemplate();
        CreateTaskActionHandler h = new CreateTaskActionHandler(jt);
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("title", "跟进 {customer_name}");
        cfg.put("content", "客户 {customer_name} 超期");
        cfg.put("assignee", 2L);
        cfg.put("priority", 3);

        h.execute(ruleOf("create_task", cfg), Map.of("customer_name", "李先生"));

        assertThat(jt.calls).hasSize(1);
        Object[] call = jt.calls.get(0);
        assertThat((String) call[0]).contains("INSERT INTO sys_todo");
        assertThat(call[1]).isEqualTo("跟进 李先生");
        assertThat(call[2]).isEqualTo("客户 李先生 超期");
        assertThat(call[3]).isEqualTo(2L);
        assertThat(call[4]).isEqualTo(3);
    }

    @Test
    @DisplayName("create_task：表不存在时 warn 不抛")
    void createTask_swallowsSqlFailure() {
        RecordingJdbcTemplate jt = new RecordingJdbcTemplate();
        jt.throwOnNext = new RuntimeException("table not found");
        CreateTaskActionHandler h = new CreateTaskActionHandler(jt);
        assertThatCode(() -> h.execute(ruleOf("create_task",
                Map.of("title", "t", "content", "c", "assignee", 1L, "priority", 2)),
                Map.of())).doesNotThrowAnyException();
    }
}
