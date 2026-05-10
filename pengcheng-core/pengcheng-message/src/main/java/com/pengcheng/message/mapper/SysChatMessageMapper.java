package com.pengcheng.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.message.entity.SysChatMessage;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 聊天消息 Mapper
 */
@Mapper
public interface SysChatMessageMapper extends BaseMapper<SysChatMessage> {

    /**
     * 获取用户未读消息数量
     */
    @Select("SELECT COUNT(*) FROM sys_chat_message WHERE receiver_id = #{userId} AND is_read = 0")
    int selectUnreadCount(@Param("userId") Long userId);
    
    /**
     * 获取两个用户之间的最新一条消息
     */
    @Select("SELECT * FROM sys_chat_message " +
            "WHERE (sender_id = #{userId} AND receiver_id = #{targetId}) " +
            "   OR (sender_id = #{targetId} AND receiver_id = #{userId}) " +
            "ORDER BY send_time DESC LIMIT 1")
    SysChatMessage selectLatestMessage(@Param("userId") Long userId, @Param("targetId") Long targetId);

    /**
     * 物理批量删除 send_time 早于 cutoff 的私聊消息（按 LIMIT batchSize 分批，避免大事务）
     * 由 ChatRetentionService 在循环里反复调用直到返回 < batchSize
     */
    @Delete("DELETE FROM sys_chat_message WHERE send_time < #{cutoff} LIMIT #{batchSize}")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff, @Param("batchSize") int batchSize);
}
