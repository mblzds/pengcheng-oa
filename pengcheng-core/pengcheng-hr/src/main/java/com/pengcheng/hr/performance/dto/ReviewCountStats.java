package com.pengcheng.hr.performance.dto;

import lombok.Data;
import lombok.Builder;

/**
 * 评估人数统计
 */
@Data
@Builder
public class ReviewCountStats {
    private Integer selfCount;
    private Integer managerCount;
    private Integer peerCount;
    private Integer subordinateCount;
    private Integer totalCount;
}
