package com.pengcheng.system.quality.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 销售质检评分实体
 * 包含 6 个维度评分 + 综合评分 + AI 评语
 */
@Data
@TableName("sys_sales_quality_score")
public class SalesQualityScore {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate scoreDate;
    /** 沟通完整性 0-100 */
    private Integer communicationScore;
    /** 需求挖掘 0-100 */
    private Integer demandMiningScore;
    /** 异议处理 0-100 */
    private Integer objectionHandlingScore;
    /** 闭合能力 0-100 */
    private Integer closingAbilityScore;
    /** 跟进频率 0-100 */
    private Integer followUpFrequencyScore;
    /** 响应时效 0-100 */
    private Integer responseTimeScore;
    /** 综合评分 0-100 */
    private Integer overallScore;
    private String aiComment;
    private String aiSuggestion;
    private Integer evaluatedRecords;
    private LocalDateTime createdAt;
}
