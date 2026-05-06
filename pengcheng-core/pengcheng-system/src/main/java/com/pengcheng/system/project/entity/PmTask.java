package com.pengcheng.system.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务实体
 */
@Data
@TableName("pm_task")
public class PmTask {
    /** 子任务（树形结构用，非表字段） */
    private transient List<PmTask> children;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private Long parentId;
    private String title;
    private String description;
    private Long assigneeId;
    private String status;
    /** 0-无 1-低 2-中 3-高 4-紧急 */
    private Integer priority;
    private Integer progress;
    private LocalDate startDate;
    private LocalDate dueDate;
    private BigDecimal estimatedHours;
    private BigDecimal actualHours;
    private Integer sortOrder;

    private Long createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
