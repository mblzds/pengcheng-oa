package com.pengcheng.admin.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengcheng.common.event.DataChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 数据变更 WebSocket 广播监听器
 * <p>
 * 监听 pengcheng-core Service 层发布的 {@link DataChangeEvent}，
 * 通过 WebSocket 向所有已连接客户端广播数据变更消息。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataChangeWebSocketListener {

    private final MessageWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    @EventListener
    public void onDataChange(DataChangeEvent event) {
        try {
            Map<String, Object> message = Map.of(
                    "type", "dataChange",
                    "changeType", event.getChangeType(),
                    "bizType", event.getBizType(),
                    "bizId", event.getBizId(),
                    "time", System.currentTimeMillis()
            );
            String json = objectMapper.writeValueAsString(message);
            webSocketHandler.broadcastMessage(json);
            log.debug("广播数据变更事件: changeType={}, bizType={}, bizId={}",
                    event.getChangeType(), event.getBizType(), event.getBizId());
        } catch (Exception e) {
            log.error("广播数据变更事件失败", e);
        }
    }
}
