package com.pengcheng.admin.controller.realty;

import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.ai.service.AiLlmService;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.template.entity.SceneTemplate;
import com.pengcheng.system.template.entity.SceneTemplateUsage;
import com.pengcheng.system.template.service.SceneTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 销售场景模板接口
 */
@RestController
@RequestMapping("/template")
@RequiredArgsConstructor
public class SceneTemplateController {

    private final SceneTemplateService templateService;
    @Autowired(required = false)
    private AiLlmService aiLlmService;

    @GetMapping("/list")
    public Result<List<SceneTemplate>> list(@RequestParam(required = false) String category) {
        if (category != null && !category.isEmpty()) {
            return Result.ok(templateService.getByCategory(category));
        }
        return Result.ok(templateService.getAllTemplates());
    }

    @GetMapping("/{id}")
    public Result<SceneTemplate> detail(@PathVariable Long id) {
        return Result.ok(templateService.getTemplate(id));
    }

    @PostMapping("/create")
    public Result<SceneTemplate> create(@RequestBody SceneTemplate template) {
        template.setCreatedBy(StpUtil.getLoginIdAsLong());
        return Result.ok(templateService.createTemplate(template));
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody SceneTemplate template) {
        templateService.updateTemplate(template);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return Result.ok();
    }

    @PostMapping("/fill")
    public Result<Map<String, Object>> fill(@RequestBody Map<String, Object> body) {
        Long templateId = Long.valueOf(body.get("templateId").toString());
        @SuppressWarnings("unchecked")
        Map<String, String> data = (Map<String, String>) body.get("data");
        Long customerId = body.get("customerId") != null ? Long.valueOf(body.get("customerId").toString()) : null;
        Long projectId = body.get("projectId") != null ? Long.valueOf(body.get("projectId").toString()) : null;
        Long userId = StpUtil.getLoginIdAsLong();

        String filled = templateService.fillTemplate(templateId, data, userId, customerId, projectId);
        return Result.ok(Map.of("content", filled != null ? filled : ""));
    }

    /** 根据客户画像智能预填模板字段 */
    @PostMapping("/smart-fill")
    public Result<Map<String, String>> smartFill(@RequestBody Map<String, Object> body) {
        if (aiLlmService == null) return Result.fail("AI 服务未启用");
        Long templateId = Long.valueOf(body.get("templateId").toString());
        String customerProfile = (String) body.getOrDefault("customerProfile", "");
        SceneTemplate tpl = templateService.getTemplate(templateId);
        if (tpl == null) return Result.fail("模板不存在");

        String fields = tpl.getTemplateContent() != null ? tpl.getTemplateContent() : "";
        String json = aiLlmService.smartFillTemplate(fields, customerProfile);
        if (json == null) return Result.ok(Map.of());
        try {
            String cleaned = json.trim();
            int s = cleaned.indexOf('{'), e = cleaned.lastIndexOf('}');
            if (s < 0 || e < 0) return Result.ok(Map.of());
            cleaned = cleaned.substring(s, e + 1);
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, String> result = om.readValue(cleaned, Map.class);
            return Result.ok(result);
        } catch (Exception ex) {
            return Result.ok(Map.of());
        }
    }

    @GetMapping("/usages")
    public Result<List<SceneTemplateUsage>> usages(@RequestParam(defaultValue = "20") int limit) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(templateService.getUserUsages(userId, limit));
    }
}
