package com.pengcheng.system.smarttable.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 智能表格
 */
@Data
@TableName("smart_table")
public class SmartTable implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private String icon;

    private Long templateId;

    private Long ownerId;

    private Long deptId;

    /**
     * 可见范围：private/dept/all
     */
    private String visibility;

    private Integer recordCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private String ownerName;

    @TableField(exist = false)
    private Integer fieldCount;
}
