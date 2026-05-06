package com.pengcheng.admin.controller.system;

import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.search.dto.SearchResultDTO;
import com.pengcheng.system.search.service.GlobalSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 全局智能搜索
 */
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class GlobalSearchController {

    private final GlobalSearchService searchService;

    /**
     * 全局搜索
     * @param q     搜索关键词
     * @param scope 搜索范围：all/customer/project/alliance/chat/notice
     */
    @GetMapping
    public Result<SearchResultDTO> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "all") String scope,
            @RequestParam(defaultValue = "5") int maxPerCategory) {
        Long userId = StpUtil.getLoginIdAsLong();
        SearchResultDTO result = searchService.search(q, scope, userId, null, maxPerCategory);
        return Result.ok(result);
    }

    /** 搜索历史 */
    @GetMapping("/history")
    public Result<List<String>> searchHistory(@RequestParam(defaultValue = "10") int limit) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(searchService.getSearchHistory(userId, limit));
    }

    /** 热门搜索 */
    @GetMapping("/hot")
    public Result<List<Map<String, Object>>> hotSearches(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(searchService.getHotSearches(limit));
    }
}
