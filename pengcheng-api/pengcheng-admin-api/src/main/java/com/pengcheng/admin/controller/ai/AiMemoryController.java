package com.pengcheng.admin.controller.ai;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.ai.memory.entity.AiMemory;
import com.pengcheng.ai.memory.mapper.AiMemoryMapper;
import com.pengcheng.ai.memory.service.MemoryService;
import com.pengcheng.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 记忆管理接口
 */
@RestController
@RequestMapping("/ai/memory")
@RequiredArgsConstructor
public class AiMemoryController {

    private final MemoryService memoryService;
    private final AiMemoryMapper aiMemoryMapper;

    /**
     * 分页查询记忆列表
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String level) {

        Page<AiMemory> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<AiMemory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMemory::getDeleted, false);

        if (StringUtils.hasText(type)) {
            wrapper.eq(AiMemory::getMemoryType, type);
        }
        if (StringUtils.hasText(level)) {
            wrapper.eq(AiMemory::getMemoryLevel, level);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(AiMemory::getContent, keyword);
        }
        wrapper.orderByDesc(AiMemory::getCreatedAt);

        Page<AiMemory> result = aiMemoryMapper.selectPage(pageParam, wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getRecords());
        data.put("total", result.getTotal());
        return Result.ok(data);
    }

    /**
     * 统计信息
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        LambdaQueryWrapper<AiMemory> base = new LambdaQueryWrapper<>();
        base.eq(AiMemory::getDeleted, false);

        long total = aiMemoryMapper.selectCount(new LambdaQueryWrapper<AiMemory>().eq(AiMemory::getDeleted, false));
        long l2Count = aiMemoryMapper.selectCount(new LambdaQueryWrapper<AiMemory>().eq(AiMemory::getDeleted, false).eq(AiMemory::getMemoryLevel, "L2"));
        long l1Count = aiMemoryMapper.selectCount(new LambdaQueryWrapper<AiMemory>().eq(AiMemory::getDeleted, false).eq(AiMemory::getMemoryLevel, "L1"));
        long profileCount = aiMemoryMapper.selectCount(new LambdaQueryWrapper<AiMemory>().eq(AiMemory::getDeleted, false).eq(AiMemory::getMemoryType, "profile"));

        Map<String, Object> data = new HashMap<>();
        data.put("total", total);
        data.put("l2Count", l2Count);
        data.put("l1Count", l1Count);
        data.put("profileCount", profileCount);
        return Result.ok(data);
    }

    /**
     * 手动升级为长期记忆
     */
    @PostMapping("/{id}/promote")
    public Result<Void> promote(@PathVariable Long id) {
        memoryService.promoteToLongTerm(id);
        return Result.ok();
    }

    /**
     * 删除记忆
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        memoryService.deleteMemory(id, userId);
        return Result.ok();
    }

    /**
     * 搜索记忆（FULLTEXT）
     */
    @GetMapping("/search")
    public Result<?> search(@RequestParam String keyword, @RequestParam(defaultValue = "20") int limit) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(memoryService.searchMemories(userId, keyword, limit));
    }
}
