package com.pengcheng.system.smarttable.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 智能表格记录（行数据）
 */
@Data
@TableName(value = "smart_table_record", autoResultMap = true)
public class SmartTableRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tableId;

    /**
     * 行数据（JSON 格式，key 对应 field_key）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> data;

    private Long creatorId;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
