package com.pengcheng.system.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 全局搜索结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultDTO {

    /** 搜索关键词 */
    private String keyword;

    /** 总结果数 */
    private int totalCount;

    /** 各分类结果 */
    private List<CategoryResult> categories;

    /** 搜索耗时(ms) */
    private long costMs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryResult {

        /** 分类：customer/project/alliance/chat/notice/knowledge */
        private String scope;

        /** 分类显示名 */
        private String label;

        /** 该分类结果数 */
        private int count;

        /** 结果列表（最多返回前5条，全量需按 scope 单独查询） */
        private List<SearchItem> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchItem {

        /** 记录 ID */
        private Long id;

        /** 标题/名称 */
        private String title;

        /** 摘要/匹配片段 */
        private String snippet;

        /** 类型标签 */
        private String type;

        /** 跳转路由 */
        private String route;

        /** 更新时间 */
        private LocalDateTime updatedAt;

        /** 附加信息 */
        private Map<String, Object> extra;
    }
}
