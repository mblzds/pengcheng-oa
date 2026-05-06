package com.pengcheng.admin.controller.ai;

import com.pengcheng.ai.service.AiContentService;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.annotation.Log;
import com.pengcheng.system.annotation.Log.BusinessType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * AI 营销文案生成控制器
 */
@RestController
@RequestMapping("/admin/ai/content")
@RequiredArgsConstructor
public class AiContentController {

    private final AiContentService aiContentService;

    /**
     * 生成营销文案
     *
     * @param request 文案生成请求
     * @return 生成的营销文案
     */
    @PostMapping("/marketing")
    @Log(title = "营销文案生成", businessType = BusinessType.OTHER)
    public Result<String> generateMarketing(@RequestBody MarketingRequest request) {
        if (request.keywords() == null || request.keywords().isBlank()) {
            return Result.fail("关键词不能为空");
        }
        String content = aiContentService.generateMarketingContent(request.keywords(), request.channel());
        return Result.ok(content);
    }

    /**
     * 营销文案请求
     *
     * @param keywords 关键词
     * @param channel  目标渠道: wechat_moments(朋友圈), sms(短信), general(通用)
     */
    public record MarketingRequest(String keywords, String channel) {}
}
