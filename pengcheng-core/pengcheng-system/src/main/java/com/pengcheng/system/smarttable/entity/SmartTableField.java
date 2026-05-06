package com.pengcheng.system.smarttable.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 智能表格字段定义
 */
@Data
@TableName(value = "smart_table_field", autoResultMap = true)
public class SmartTableField implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tableId;

    private String name;

    private String fieldKey;

    /**
     * 字段类型：text/number/select/multi_select/date/datetime/checkbox/
     * url/email/phone/rating/progress/member/attachment
     */
    private String fieldType;

    private Boolean required;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> options;

    private String defaultValue;

    private Integer sortOrder;

    private Integer width;

    private Boolean hidden;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
