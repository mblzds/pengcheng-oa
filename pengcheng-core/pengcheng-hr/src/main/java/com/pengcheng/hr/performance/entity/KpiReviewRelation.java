package com.pengcheng.hr.performance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评估关系（上下级）— 对应 V33__360_review.sql 的 kpi_review_relation 表
 */
@Data
@TableName("kpi_review_relation")
public class KpiReviewRelation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户 ID（被评估人） */
    private Long userId;

    /** 上级 ID */
    private Long managerId;

    /** 考核周期 ID */
    private Long periodId;

    /** 状态：1-有效 0-无效 */
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
