package com.pengcheng.hr.performance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 360 度评估实体
 */
@Data
@TableName("kpi_review_360")
public class KpiReview360 implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 考核周期 ID
     */
    @TableField("period_id")
    private Long periodId;

    /**
     * 被评估人 ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 评估人 ID
     */
    @TableField("reviewer_id")
    private Long reviewerId;

    /**
     * 评估类型：1-自评 2-上级 3-同事 4-下级
     */
    @TableField("review_type")
    private Integer reviewType;

    /**
     * 总分
     */
    @TableField("total_score")
    private BigDecimal totalScore;

    /**
     * 评价意见
     */
    @TableField("comment")
    private String comment;

    /**
     * 优点/长处
     */
    @TableField("strengths")
    private String strengths;

    /**
     * 待改进项
     */
    @TableField("improvements")
    private String improvements;

    /**
     * 状态：1-待评估 2-已完成
     */
    @TableField("status")
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
