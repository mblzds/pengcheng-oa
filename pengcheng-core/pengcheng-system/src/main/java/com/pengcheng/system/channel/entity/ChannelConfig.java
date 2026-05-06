package com.pengcheng.system.channel.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 推送渠道配置（钉钉/飞书/企业微信）
 */
@Data
@TableName(value = "sys_channel_config", autoResultMap = true)
public class ChannelConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** dingtalk / feishu / wecom / email */
    private String channelType;
    private String channelName;
    private String webhookUrl;
    private String appKey;
    private String appSecret;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extraConfig;
    private Integer enabled;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
