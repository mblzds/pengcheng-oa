package com.pengcheng.system.search.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 搜索历史与热门搜索
 */
@Mapper
public interface SearchHistoryMapper {

    @Insert("INSERT INTO sys_search_history (user_id, keyword, scope, result_count) VALUES (#{userId}, #{keyword}, #{scope}, #{resultCount})")
    void saveHistory(@Param("userId") Long userId, @Param("keyword") String keyword, @Param("scope") String scope, @Param("resultCount") int resultCount);

    @Select("SELECT keyword FROM sys_search_history WHERE user_id = #{userId} GROUP BY keyword ORDER BY MAX(created_at) DESC LIMIT #{limit}")
    List<String> getRecentKeywords(@Param("userId") Long userId, @Param("limit") int limit);

    @Insert("INSERT INTO sys_search_hot (keyword, search_count, last_searched_at) VALUES (#{keyword}, 1, NOW()) ON DUPLICATE KEY UPDATE search_count = search_count + 1, last_searched_at = NOW()")
    void incrementHotSearch(@Param("keyword") String keyword);

    @Select("SELECT keyword, search_count AS searchCount FROM sys_search_hot ORDER BY search_count DESC LIMIT #{limit}")
    List<Map<String, Object>> getHotSearches(@Param("limit") int limit);
}
