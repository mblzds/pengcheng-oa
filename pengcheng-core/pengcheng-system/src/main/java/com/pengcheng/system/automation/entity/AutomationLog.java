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
 * 自动化规则执行日志
 */
@Data
@TableName(value = "sys_automation_log", autoResultMap = true)
public class AutomationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ruleId;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> triggerData;

    private String actionResult;
    private Integer status;
    private LocalDateTime executedAt;
}
