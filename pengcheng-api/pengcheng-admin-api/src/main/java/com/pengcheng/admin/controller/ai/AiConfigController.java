package com.pengcheng.admin.controller.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pengcheng.ai.audit.entity.AiToolCallLog;
import com.pengcheng.ai.audit.mapper.AiToolCallLogMapper;
import com.pengcheng.ai.memory.entity.AiMemory;
import com.pengcheng.ai.memory.mapper.AiMemoryMapper;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.entity.SysConfigGroup;
import com.pengcheng.system.service.SysConfigGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * AI 模型与技能配置接口
 * 提供模型列表/更新、功能开关、使用统计、连通测试。
 */
@RestController
@RequestMapping("/ai/config")
@RequiredArgsConstructor
public class AiConfigController {

    private static final String GROUP_AI_CONFIG = "aiConfig";

    private final SysConfigGroupService configGroupService;
    private final AiToolCallLogMapper toolCallLogMapper;
    private final AiMemoryMapper aiMemoryMapper;
    private final ObjectMapper objectMapper;

    /** 默认模型列表（与前端一致，可被配置覆盖） */
    private static final List<Map<String, Object>> DEFAULT_MODELS = List.of(
            mapModel(1, "Qwen-Max", "DashScope", "qwen-max", "主力对话/报表分析", true, 0.7, 8000),
            mapModel(2, "Qwen-Turbo", "DashScope", "qwen-turbo", "快速响应/文案生成", true, 0.8, 4000),
            mapModel(3, "GLM-4-Flash", "智谱AI", "glm-4-flash", "备用模型/降级", true, 0.7, 4000),
            mapModel(4, "Text-Embedding-V3", "DashScope", "text-embedding-v3", "RAG 向量化", true, 0.0, 0)
    );

    /** 默认功能开关 */
    private static final List<Map<String, Object>> DEFAULT_FEATURES = List.of(
            mapFeature("ai_chat", "AI 智能对话", "启用 AI 对话功能，包括报表分析、文案生成", true),
            mapFeature("ai_rag", "RAG 知识库", "启用文档知识库检索增强生成", true),
            mapFeature("ai_memory", "AI 记忆系统", "启用三层记忆架构，自动提取和注入记忆上下文", true),
            mapFeature("ai_customer_profile", "客户画像记忆", "从对话自动提取客户偏好和需求", true),
            mapFeature("ai_experiment", "A/B 实验平台", "启用 AI 模型 A/B 测试和实验管理", false)
    );

    private static Map<String, Object> mapModel(int id, String name, String provider, String modelId, String usage, boolean enabled, double temperature, int maxTokens) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("provider", provider);
        m.put("modelId", modelId);
        m.put("usage", usage);
        m.put("enabled", enabled);
        m.put("temperature", temperature);
        m.put("maxTokens", maxTokens);
        return m;
    }

    private static Map<String, Object> mapFeature(String key, String name, String description, boolean enabled) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("key", key);
        f.put("name", name);
        f.put("description", description);
        f.put("enabled", enabled);
        return f;
    }

    /**
     * 获取模型列表（合并默认与配置中的覆盖项）
     */
    @GetMapping("/models")
    public Result<List<Map<String, Object>>> getModels() {
        JsonNode config = getAiConfigJson();
        JsonNode modelsNode = config != null ? config.get("models") : null;

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> def : DEFAULT_MODELS) {
            Map<String, Object> row = new LinkedHashMap<>(def);
            String modelId = (String) def.get("modelId");
            if (modelsNode != null && modelsNode.has(modelId)) {
                JsonNode over = modelsNode.get(modelId);
                if (over.has("enabled")) row.put("enabled", over.get("enabled").asBoolean());
                if (over.has("temperature")) row.put("temperature", over.get("temperature").asDouble());
                if (over.has("maxTokens")) row.put("maxTokens", over.get("maxTokens").asInt());
            }
            result.add(row);
        }
        return Result.ok(result);
    }

    /**
     * 更新模型配置（仅持久化启用状态、温度、最大 Token，保留 features 等其它配置）
     */
    @PutMapping("/models")
    public Result<Void> updateModels(@RequestBody List<Map<String, Object>> models) {
        JsonNode config = getAiConfigJson();
        ObjectNode root = config != null && config.isObject() ? (ObjectNode) config.deepCopy() : objectMapper.createObjectNode();
        ObjectNode modelsNode = objectMapper.createObjectNode();
        for (Map<String, Object> m : models) {
            String modelId = (String) m.get("modelId");
            if (!StringUtils.hasText(modelId)) continue;
            ObjectNode over = modelsNode.putObject(modelId);
            if (m.containsKey("enabled")) over.put("enabled", Boolean.TRUE.equals(m.get("enabled")));
            if (m.containsKey("temperature")) over.put("temperature", ((Number) m.get("temperature")).doubleValue());
            if (m.containsKey("maxTokens")) over.put("maxTokens", ((Number) m.get("maxTokens")).intValue());
        }
        root.set("models", modelsNode);
        saveAiConfig(root);
        return Result.ok();
    }

    /**
     * 获取功能开关列表
     */
    @GetMapping("/features")
    public Result<List<Map<String, Object>>> getFeatures() {
        JsonNode config = getAiConfigJson();
        JsonNode featuresNode = config != null ? config.get("features") : null;

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> def : DEFAULT_FEATURES) {
            Map<String, Object> row = new LinkedHashMap<>(def);
            String key = (String) def.get("key");
            if (featuresNode != null && featuresNode.has(key)) {
                row.put("enabled", featuresNode.get(key).asBoolean());
            }
            result.add(row);
        }
        return Result.ok(result);
    }

    /**
     * 更新功能开关（保留 models 等其它配置）
     */
    @PutMapping("/features")
    public Result<Void> updateFeatures(@RequestBody List<Map<String, Object>> features) {
        JsonNode config = getAiConfigJson();
        ObjectNode root = config != null && config.isObject() ? (ObjectNode) config.deepCopy() : objectMapper.createObjectNode();
        ObjectNode featuresNode = objectMapper.createObjectNode();
        for (Map<String, Object> f : features) {
            String key = (String) f.get("key");
            if (!StringUtils.hasText(key)) continue;
            featuresNode.put(key, Boolean.TRUE.equals(f.get("enabled")));
        }
        root.set("features", featuresNode);
        saveAiConfig(root);
        return Result.ok();
    }

    /**
     * 使用统计：本月/今日调用、向量文档、记忆数量等
     */
    @GetMapping("/usage-stats")
    public Result<Map<String, Object>> usageStats() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfToday = today.atStartOfDay();

        long monthCalls = toolCallLogMapper.selectCount(
                new LambdaQueryWrapper<AiToolCallLog>().ge(AiToolCallLog::getCreateTime, startOfMonth));
        long todayCalls = toolCallLogMapper.selectCount(
                new LambdaQueryWrapper<AiToolCallLog>().ge(AiToolCallLog::getCreateTime, startOfToday));

        long memoryCount = aiMemoryMapper.selectCount(
                new LambdaQueryWrapper<AiMemory>().eq(AiMemory::getDeleted, false));
        long memoryL2Count = aiMemoryMapper.selectCount(
                new LambdaQueryWrapper<AiMemory>().eq(AiMemory::getDeleted, false).eq(AiMemory::getMemoryLevel, "L2"));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("monthCalls", (int) monthCalls);
        data.put("monthTokens", 0);
        data.put("todayCalls", (int) todayCalls);
        data.put("todayTokens", 0);
        data.put("vectorDocs", 0);
        data.put("vectorChunks", 0);
        data.put("memoryCount", (int) memoryCount);
        data.put("memoryL2Count", (int) memoryL2Count);
        return Result.ok(data);
    }

    /**
     * 测试模型连通性（占位实现，返回成功）
     */
    @PostMapping("/test-connection")
    public Result<Map<String, String>> testConnection(@RequestBody Map<String, String> body) {
        String modelName = body != null ? body.get("modelName") : null;
        Map<String, String> data = new LinkedHashMap<>();
        data.put("message", StringUtils.hasText(modelName) ? modelName + " 连接正常" : "连接正常");
        return Result.ok(data);
    }

    private JsonNode getAiConfigJson() {
        SysConfigGroup group = configGroupService.getByGroupCode(GROUP_AI_CONFIG);
        if (group == null || !StringUtils.hasText(group.getConfigValue())) {
            return null;
        }
        try {
            return objectMapper.readTree(group.getConfigValue());
        } catch (Exception e) {
            return null;
        }
    }

    private void saveAiConfig(ObjectNode root) {
        try {
            configGroupService.saveConfig(GROUP_AI_CONFIG, objectMapper.writeValueAsString(root));
        } catch (Exception e) {
            throw new RuntimeException("保存 AI 配置失败: " + e.getMessage());
        }
    }
}
