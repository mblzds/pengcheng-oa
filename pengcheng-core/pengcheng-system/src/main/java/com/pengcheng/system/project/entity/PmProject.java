package com.pengcheng.system.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 项目实体
 */
@Data
@TableName("pm_project")
public class PmProject {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String description;
    private Long ownerId;
    /** 1-未开始 2-进行中 3-已暂停 4-已完成 5-已归档 */
    private Integer status;
    private LocalDate startDate;
    private LocalDate endDate;
    /** private/dept/all */
    private String visibility;
    private String color;
    private Integer sortOrder;

    private Long createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
