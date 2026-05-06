package com.pengcheng.system.hr.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 考核记录实体（周期+员工+指标维度）
 */
@Data
@TableName("hr_kpi_score")
public class KpiScore {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long periodId;
    private Long userId;
    private Long templateId;

    private BigDecimal targetValue;
    private BigDecimal actualValue;
    private BigDecimal score;
    private BigDecimal weightedScore;

    private String remark;

    private Long createBy;
    private Long updateBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
