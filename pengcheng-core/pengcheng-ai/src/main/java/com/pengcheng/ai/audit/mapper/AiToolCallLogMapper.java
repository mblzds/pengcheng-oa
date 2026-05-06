package com.pengcheng.ai.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.ai.audit.entity.AiToolCallLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 工具调用审计 Mapper
 */
@Mapper
public interface AiToolCallLogMapper extends BaseMapper<AiToolCallLog> {
}

