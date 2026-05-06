package com.pengcheng.hr.performance.dto;

import lombok.Data;
import lombok.Builder;

/**
 * 360 度评估权重配置
 */
@Data
@Builder
public class Kpi360WeightConfig {
    @Builder.Default
    private Double selfWeight = 0.1;
    @Builder.Default
    private Double managerWeight = 0.4;
    @Builder.Default
    private Double peerWeight = 0.3;
    @Builder.Default
    private Double subordinateWeight = 0.2;
    @Builder.Default
    private Integer minReviewers = 3;
    @Builder.Default
    private Boolean anonymous = true;
}
