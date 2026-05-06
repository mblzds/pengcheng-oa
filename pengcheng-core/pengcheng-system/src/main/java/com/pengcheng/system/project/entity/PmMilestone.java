package com.pengcheng.system.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 里程碑实体
 */
@Data
@TableName("pm_milestone")
public class PmMilestone {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private String name;
    private String description;
    private LocalDate dueDate;
    /** 0-未完成 1-已完成 */
    private Integer status;
    private Integer sortOrder;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
