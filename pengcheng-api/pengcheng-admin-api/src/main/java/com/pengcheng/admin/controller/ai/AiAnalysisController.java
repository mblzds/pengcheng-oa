package com.pengcheng.admin.controller.ai;

import com.pengcheng.ai.service.AiAnalysisService;
import com.pengcheng.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * AI 成交概率分析控制器
 */
@RestController
@RequestMapping("/admin/ai/analysis")
@RequiredArgsConstructor
public class AiAnalysisController {

    private final AiAnalysisService aiAnalysisService;

    /**
     * 查询客户成交概率评分
     *
     * @param customerId 客户ID
     * @return 成交概率评分 [0, 1]
     */
    @GetMapping("/probability")
    public Result<BigDecimal> getProbability(@RequestParam Long customerId) {
        BigDecimal probability = aiAnalysisService.calculateAndPersist(customerId);
        return Result.ok(probability);
    }
}
