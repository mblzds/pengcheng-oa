package com.pengcheng.system.template.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_scene_template_usage")
public class SceneTemplateUsage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long templateId;
    private Long userId;
    private String filledContent;
    private Long customerId;
    private Long projectId;
    private LocalDateTime createdAt;
}
