package com.pengcheng.system.search.service;

import com.pengcheng.system.search.dto.SearchResultDTO;

import java.util.List;
import java.util.Map;

/**
 * 全局智能搜索服务
 */
public interface GlobalSearchService {

    /**
     * 全局搜索
     * @param keyword  搜索关键词
     * @param scope    搜索范围：all/customer/project/alliance/chat/notice
     * @param userId   当前用户 ID（用于权限过滤）
     * @param deptId   当前用户部门 ID
     * @param maxPerCategory 每个分类最多返回条数
     */
    SearchResultDTO search(String keyword, String scope, Long userId, Long deptId, int maxPerCategory);

    /**
     * 获取搜索历史
     */
    List<String> getSearchHistory(Long userId, int limit);

    /**
     * 获取热门搜索
     */
    List<Map<String, Object>> getHotSearches(int limit);
}
