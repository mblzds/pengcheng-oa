package com.pengcheng.admin.entity.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 实验配置变更审计实体。
 */
@Data
@TableName("ai_experiment_config_audit")
public class AiExperimentConfigAudit {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String groupCode;

    private String changeType;

    private String source;

    private Long operatorId;

    private String operatorName;

    private Long rollbackFromAuditId;

    private String previousConfigValue;

    private String configValue;

    private String remark;

    private LocalDateTime createTime;
}
