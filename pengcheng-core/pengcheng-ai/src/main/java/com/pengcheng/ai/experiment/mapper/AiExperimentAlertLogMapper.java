package com.pengcheng.ai.experiment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.ai.experiment.entity.AiExperimentAlertLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 实验告警日志 Mapper
 */
@Mapper
public interface AiExperimentAlertLogMapper extends BaseMapper<AiExperimentAlertLog> {
}
