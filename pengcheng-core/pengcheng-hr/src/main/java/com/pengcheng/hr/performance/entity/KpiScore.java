package com.pengcheng.hr.performance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 考核记录（周期+员工+指标维度，公司级）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("hr_kpi_score")
public class KpiScore extends BaseEntity {

    private Long periodId;
    private Long userId;
    private Long templateId;
    private BigDecimal targetValue;
    private BigDecimal actualValue;
    private BigDecimal score;
    private BigDecimal weightedScore;
    private String remark;
}
