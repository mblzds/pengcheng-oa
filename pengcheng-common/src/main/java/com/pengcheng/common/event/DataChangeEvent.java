package com.pengcheng.common.event;

import org.springframework.context.ApplicationEvent;

/**
 * 数据变更事件
 * <p>
 * 由 pengcheng-core Service 层在关键写操作后发布，
 * 由 API 层的 WebSocket 监听器接收并广播给已连接的客户端。
 */
public class DataChangeEvent extends ApplicationEvent {

    private final String changeType;
    private final String bizType;
    private final Long bizId;

    /**
     * @param source     事件源
     * @param changeType 变更类型：create / update / delete
     * @param bizType    业务类型：customer / attendance / payment / commission
     * @param bizId      业务实体ID
     */
    public DataChangeEvent(Object source, String changeType, String bizType, Long bizId) {
        super(source);
        this.changeType = changeType;
        this.bizType = bizType;
        this.bizId = bizId;
    }

    public String getChangeType() {
        return changeType;
    }

    public String getBizType() {
        return bizType;
    }

    public Long getBizId() {
        return bizId;
    }
}
