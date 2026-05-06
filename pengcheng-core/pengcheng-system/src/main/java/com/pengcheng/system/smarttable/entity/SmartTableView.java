package com.pengcheng.system.smarttable.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 智能表格视图
 */
@Data
@TableName(value = "smart_table_view", autoResultMap = true)
public class SmartTableView implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tableId;

    private String name;

    /**
     * 视图类型：grid/kanban/gantt/calendar
     */
    private String viewType;

    /**
     * 视图配置（筛选、排序、分组、隐藏列等）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> config;

    private Boolean isDefault;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
