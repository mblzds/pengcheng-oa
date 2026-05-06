package com.pengcheng.system.template.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.system.template.entity.SceneTemplate;
import com.pengcheng.system.template.entity.SceneTemplateUsage;
import com.pengcheng.system.template.mapper.SceneTemplateMapper;
import com.pengcheng.system.template.mapper.SceneTemplateUsageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 销售场景模板服务
 * 管理模板 CRUD、填充模板内容、记录使用历史
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SceneTemplateService {

    private final SceneTemplateMapper templateMapper;
    private final SceneTemplateUsageMapper usageMapper;

    public List<SceneTemplate> getAllTemplates() {
        return templateMapper.selectList(
            new LambdaQueryWrapper<SceneTemplate>().orderByAsc(SceneTemplate::getSortOrder));
    }

    public List<SceneTemplate> getByCategory(String category) {
        return templateMapper.selectList(
            new LambdaQueryWrapper<SceneTemplate>().eq(SceneTemplate::getCategory, category)
                .orderByAsc(SceneTemplate::getSortOrder));
    }

    public SceneTemplate getTemplate(Long id) {
        return templateMapper.selectById(id);
    }

    public SceneTemplate createTemplate(SceneTemplate template) {
        templateMapper.insert(template);
        return template;
    }

    public void updateTemplate(SceneTemplate template) {
        templateMapper.updateById(template);
    }

    public void deleteTemplate(Long id) {
        templateMapper.deleteById(id);
    }

    /**
     * 填充模板，替换 {{placeholder}} 占位符
     */
    @Transactional
    public String fillTemplate(Long templateId, Map<String, String> data, Long userId, Long customerId, Long projectId) {
        SceneTemplate template = templateMapper.selectById(templateId);
        if (template == null) return null;

        String content = template.getTemplateContent();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            content = content.replace("{{" + entry.getKey() + "}}", entry.getValue() != null ? entry.getValue() : "");
        }
        content = content.replaceAll("\\{\\{\\w+}}", "");

        SceneTemplateUsage usage = new SceneTemplateUsage();
        usage.setTemplateId(templateId);
        usage.setUserId(userId);
        usage.setFilledContent(content);
        usage.setCustomerId(customerId);
        usage.setProjectId(projectId);
        usageMapper.insert(usage);

        template.setUsageCount(template.getUsageCount() != null ? template.getUsageCount() + 1 : 1);
        templateMapper.updateById(template);

        return content;
    }

    public List<SceneTemplateUsage> getUserUsages(Long userId, int limit) {
        return usageMapper.selectList(
            new LambdaQueryWrapper<SceneTemplateUsage>()
                .eq(SceneTemplateUsage::getUserId, userId)
                .orderByDesc(SceneTemplateUsage::getCreatedAt)
                .last("LIMIT " + limit));
    }
}
