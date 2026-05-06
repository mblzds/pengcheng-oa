package com.pengcheng.hr.performance.dto;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;

/**
 * 360 度评估请求 DTO
 */
@Data
@Builder
public class KpiReview360DTO {
    private Long periodId;
    private Long userId;
    private Integer reviewType;
    private BigDecimal totalScore;
    private String comment;
    private String strengths;
    private String improvements;
}
