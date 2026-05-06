package com.pengcheng.realty.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 业绩排行榜 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingVO {

    /** 项目业绩排行榜 */
    private List<ProjectRankItem> projectRanking;

    /** 联盟商业绩排行榜 */
    private List<AllianceRankItem> allianceRanking;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectRankItem {
        private Long projectId;
        private String projectName;
        /** 成交数量 */
        private Long dealCount;
        /** 成交金额 */
        private BigDecimal dealAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AllianceRankItem {
        private Long allianceId;
        private String companyName;
        /** 上客数量（报备数） */
        private Long customerCount;
        /** 成交数量 */
        private Long dealCount;
    }
}
