package com.pengcheng.system.smarttable.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 智能表格模板
 */
@Data
@TableName(value = "smart_table_template", autoResultMap = true)
public class SmartTableTemplate implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    /**
     * 分类：general/realty/sales/hr/finance
     */
    private String category;

    private String icon;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> fieldsConfig;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> sampleData;

    private Boolean builtIn;

    private Integer usageCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
