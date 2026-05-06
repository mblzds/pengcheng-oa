package com.pengcheng.system.hr.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考核周期实体
 */
@Data
@TableName("hr_kpi_period")
public class KpiPeriod {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 周期名称 */
    private String name;

    /** 1-月度 2-季度 3-年度 */
    private Integer periodType;

    private Integer year;
    /** 季度 1-4 */
    private Integer quarter;
    /** 月份 1-12 */
    private Integer month;

    private LocalDate startDate;
    private LocalDate endDate;

    /** 1-未开始 2-考核中 3-已结束 */
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
