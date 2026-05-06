package com.pengcheng.system.hr.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * KPI 指标模板实体
 */
@Data
@TableName("hr_kpi_template")
public class KpiTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 指标名称 */
    private String name;

    /** 编码 */
    private String code;

    /** 1-销售业绩 2-考勤 3-过程质量 4-综合 */
    private Integer category;

    /** 权重 0-100 */
    private BigDecimal weight;

    /** manual, auto_commission, auto_attendance, auto_quality */
    private String dataSource;

    /** 计算公式或说明 */
    private String formula;

    private Integer sortOrder;

    /** 0-停用 1-启用 */
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
