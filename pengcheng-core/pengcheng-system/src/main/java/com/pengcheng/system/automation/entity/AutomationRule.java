package com.pengcheng.system.automation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 自动化规则实体
 */
@Data
@TableName(value = "sys_automation_rule", autoResultMap = true)
public class AutomationRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String description;

    /**
     * time_based / event_based / condition_based
     */
    private String triggerType;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> triggerConfig;

    /**
     * notify / assign / update_status / create_task
     */
    private String actionType;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> actionConfig;

    private Boolean enabled;
    private Integer priority;
    private Long createdBy;
    private LocalDateTime lastTriggeredAt;
    private Integer triggerCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
