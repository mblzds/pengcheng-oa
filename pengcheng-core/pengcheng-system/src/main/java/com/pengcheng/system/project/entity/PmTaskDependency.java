package com.pengcheng.system.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务依赖实体
 */
@Data
@TableName("pm_task_dependency")
public class PmTaskDependency {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;
    private Long dependsOnTaskId;
    /** fs/ff/ss/sf */
    private String type;
    private LocalDateTime createTime;
}
