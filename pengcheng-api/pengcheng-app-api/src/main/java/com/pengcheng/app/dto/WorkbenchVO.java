package com.pengcheng.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 工作台数据响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkbenchVO {

    /** 当前用户角色编码 */
    private String roleCode;

    /** 统计卡片列表 */
    private List<StatsCard> statsCards;

    /** 九宫格快捷入口 */
    private List<QuickEntry> quickEntries;

    /** 最近5条通知 */
    private List<NotificationVO> recentNotices;

    /** 待审批数量 */
    private Integer pendingApprovalCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatsCard {
        /** 卡片标题 */
        private String label;
        /** 数值 */
        private Integer value;
        /** 图标名称 */
        private String icon;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuickEntry {
        /** 入口名称 */
        private String label;
        /** 图标 */
        private String icon;
        /** 跳转路径 */
        private String path;
    }
}
