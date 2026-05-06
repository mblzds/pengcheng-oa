package com.pengcheng.system.automation.handler;

import com.pengcheng.system.automation.entity.AutomationRule;
import com.pengcheng.system.channel.service.ChannelPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * notify 动作：通过 {@link ChannelPushService} 广播到所有启用的推送渠道（钉钉/飞书/企微）。
 * <p>
 * actionConfig 支持：
 * <ul>
 *   <li><b>title</b>：消息标题（可含占位符 {xxx}）</li>
 *   <li><b>template</b>：消息正文（可含占位符 {xxx}）</li>
 *   <li><b>messageType</b>：消息类型，默认 "automation"</li>
 * </ul>
 * 占位符取自触发上下文 {@code data}（如定时扫描到的字段、事件 payload 的字段）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyActionHandler implements RuleActionHandler {

    public static final String ACTION_TYPE = "notify";

    private final ChannelPushService channelPushService;

    @Override
    public String actionType() {
        return ACTION_TYPE;
    }

    @Override
    public void execute(AutomationRule rule, Map<String, Object> data) {
        Map<String, Object> cfg = rule.getActionConfig();
        if (cfg == null) cfg = Map.of();

        String title = render(String.valueOf(cfg.getOrDefault("title", rule.getName())), data);
        String content = render(String.valueOf(cfg.getOrDefault("template", "自动化规则触发")), data);
        String messageType = String.valueOf(cfg.getOrDefault("messageType", "automation"));

        try {
            channelPushService.broadcast(title, content, messageType);
            log.info("[Automation/notify] rule={} 已广播 title={}", rule.getName(), title);
        } catch (Exception e) {
            // 单渠道失败不阻断整条规则，channelPushService 内部已落 push_log 记录详细错误
            log.warn("[Automation/notify] rule={} 广播失败：{}", rule.getName(), e.getMessage());
        }
    }

    /** 把 "{key}" 替换成 data 中对应值；缺省保留原字符串。 */
    static String render(String template, Map<String, Object> data) {
        if (template == null || data == null) return template;
        String out = template;
        for (Map.Entry<String, Object> e : data.entrySet()) {
            out = out.replace("{" + e.getKey() + "}", String.valueOf(e.getValue()));
        }
        return out;
    }
}
