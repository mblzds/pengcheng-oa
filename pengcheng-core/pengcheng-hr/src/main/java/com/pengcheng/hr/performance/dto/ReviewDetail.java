package com.pengcheng.hr.performance.dto;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;

/**
 * 详细评价
 */
@Data
@Builder
public class ReviewDetail {
    private Integer reviewType;
    private String reviewTypeName;
    private String reviewerName;
    private BigDecimal score;
    private String comment;
    private String strengths;
    private String improvements;
}
