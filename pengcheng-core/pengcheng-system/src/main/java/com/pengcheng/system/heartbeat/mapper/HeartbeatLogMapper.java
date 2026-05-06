package com.pengcheng.system.heartbeat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.system.heartbeat.entity.HeartbeatLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * AI 巡检记录 Mapper
 */
@Mapper
public interface HeartbeatLogMapper extends BaseMapper<HeartbeatLog> {

    @Select("SELECT COUNT(*) FROM sys_ai_heartbeat_log WHERE handled = 0 AND user_id = #{userId}")
    int countUnhandled(Long userId);
}
