package com.pengcheng.system.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目看板状态列配置（V24 可选扩展）
 * 支持项目自定义看板列，如「需求评审」「开发中」「测试」
 */
@Data
@TableName("pm_project_status_column")
public class PmProjectStatusColumn {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    /** 列名（展示用） */
    private String name;
    /** 对应 pm_task.status 取值 */
    private String statusValue;
    /** 看板列顺序 */
    private Integer sortOrder;
    /** 是否视为已完成列（用于统计） */
    private Integer isDone;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
