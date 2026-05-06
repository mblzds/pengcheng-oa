package com.pengcheng.hr.performance.dto;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.util.List;

/**
 * 360 度评估结果 VO
 */
@Data
@Builder
public class KpiReview360ResultVO {
    private Long userId;
    private String userName;
    private BigDecimal selfScore;
    private BigDecimal managerScore;
    private BigDecimal peerScore;
    private BigDecimal subordinateScore;
    private BigDecimal finalScore;
    private String grade;
    private ReviewCountStats stats;
    private List<ReviewDetail> reviews;
}
