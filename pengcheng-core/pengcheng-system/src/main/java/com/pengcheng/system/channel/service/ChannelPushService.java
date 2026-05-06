package com.pengcheng.system.channel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.system.channel.entity.ChannelConfig;
import com.pengcheng.system.channel.entity.ChannelPushLog;
import com.pengcheng.system.channel.mapper.ChannelConfigMapper;
import com.pengcheng.system.channel.mapper.ChannelPushLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * 统一多渠道推送服务
 * 支持钉钉机器人、飞书机器人、企业微信 Webhook 推送消息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelPushService {

    private final ChannelConfigMapper configMapper;
    private final ChannelPushLogMapper logMapper;
    private final RestTemplate restTemplate;

    public List<ChannelConfig> getAllChannels() {
        return configMapper.selectList(
            new LambdaQueryWrapper<ChannelConfig>().orderByAsc(ChannelConfig::getId));
    }

    public List<ChannelConfig> getEnabledChannels() {
        return configMapper.selectList(
            new LambdaQueryWrapper<ChannelConfig>().eq(ChannelConfig::getEnabled, 1));
    }

    public void saveChannel(ChannelConfig config) {
        if (config.getId() != null) {
            configMapper.updateById(config);
        } else {
            configMapper.insert(config);
        }
    }

    public void toggleChannel(Long id, boolean enabled) {
        ChannelConfig config = new ChannelConfig();
        config.setId(id);
        config.setEnabled(enabled ? 1 : 0);
        configMapper.updateById(config);
    }

    public void deleteChannel(Long id) {
        configMapper.deleteById(id);
    }

    /**
     * 向所有启用渠道广播消息
     */
    public void broadcast(String title, String content, String messageType) {
        List<ChannelConfig> channels = getEnabledChannels();
        for (ChannelConfig channel : channels) {
            pushToChannel(channel, title, content, messageType);
        }
    }

    /**
     * 向指定渠道推送消息
     */
    public void pushToChannel(ChannelConfig channel, String title, String content, String messageType) {
        ChannelPushLog pushLog = new ChannelPushLog();
        pushLog.setChannelId(channel.getId());
        pushLog.setMessageType(messageType);
        pushLog.setContent(content);
        pushLog.setTarget(channel.getChannelName());

        try {
            String payload = buildPayload(channel.getChannelType(), title, content);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<String> resp = restTemplate.postForEntity(
                channel.getWebhookUrl(), entity, String.class);

            if (resp.getStatusCode().is2xxSuccessful()) {
                pushLog.setStatus(1);
                log.info("推送成功: channel={}, type={}", channel.getChannelName(), messageType);
            } else {
                pushLog.setStatus(2);
                pushLog.setErrorMsg("HTTP " + resp.getStatusCode());
            }
        } catch (Exception e) {
            pushLog.setStatus(2);
            pushLog.setErrorMsg(e.getMessage());
            log.error("推送失败: channel={}", channel.getChannelName(), e);
        }

        logMapper.insert(pushLog);
    }

    private String buildPayload(String channelType, String title, String content) {
        return switch (channelType) {
            case "dingtalk" -> buildDingtalkPayload(title, content);
            case "feishu" -> buildFeishuPayload(title, content);
            case "wecom" -> buildWecomPayload(title, content);
            default -> "{\"text\":\"" + title + ": " + content + "\"}";
        };
    }

    private String buildDingtalkPayload(String title, String content) {
        return """
            {"msgtype":"markdown","markdown":{"title":"%s","text":"### %s\\n%s"}}
            """.formatted(title, title, content).trim();
    }

    private String buildFeishuPayload(String title, String content) {
        return """
            {"msg_type":"interactive","card":{"header":{"title":{"tag":"plain_text","content":"%s"}},"elements":[{"tag":"markdown","content":"%s"}]}}
            """.formatted(title, content).trim();
    }

    private String buildWecomPayload(String title, String content) {
        return """
            {"msgtype":"markdown","markdown":{"content":"### %s\\n%s"}}
            """.formatted(title, content).trim();
    }

    public List<ChannelPushLog> getRecentLogs(int limit) {
        return logMapper.selectList(
            new LambdaQueryWrapper<ChannelPushLog>()
                .orderByDesc(ChannelPushLog::getSentAt)
                .last("LIMIT " + limit));
    }

    /**
     * 测试渠道连通性
     */
    public boolean testChannel(Long channelId) {
        ChannelConfig channel = configMapper.selectById(channelId);
        if (channel == null || channel.getWebhookUrl() == null || channel.getWebhookUrl().isBlank()) {
            return false;
        }
        try {
            pushToChannel(channel, "连通性测试", "这是一条测试消息，来自鹏程地产系统", "test");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
