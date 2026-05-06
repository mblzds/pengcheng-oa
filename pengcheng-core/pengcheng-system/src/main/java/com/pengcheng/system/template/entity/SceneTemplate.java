package com.pengcheng.system.template.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 销售场景模板（楼盘推介/需求分析/竞品对比/踩盘报告）
 */
@Data
@TableName(value = "sys_scene_template", autoResultMap = true)
public class SceneTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    /** visit_memo / demand_analysis / competitor / survey */
    private String category;
    private String description;
    /** Markdown 内容，含 {{placeholder}} 占位符 */
    private String templateContent;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> fields;
    private String icon;
    private Integer sortOrder;
    private Integer usageCount;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
