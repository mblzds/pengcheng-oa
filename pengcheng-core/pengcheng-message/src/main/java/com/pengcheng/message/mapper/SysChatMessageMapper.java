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

    /**
     * 搜索私聊聊天记录：按"对方用户"聚合，每个对方只取最新匹配条 + 总匹配条数。
     * 用于消息页搜索框的"聊天记录"分区。
     */
    @Select("SELECT t.other_id AS id, t.msg_id AS msgId, t.content, t.send_time AS sendTime, " +
            "       t.match_count AS matchCount, t.sender_id AS senderId, " +
            "       u.nickname, u.username, u.avatar " +
            "FROM ( " +
            "  SELECT " +
            "    id AS msg_id, " +
            "    CASE WHEN sender_id = #{userId} THEN receiver_id ELSE sender_id END AS other_id, " +
            "    sender_id, content, send_time, " +
            "    ROW_NUMBER() OVER (PARTITION BY CASE WHEN sender_id = #{userId} THEN receiver_id ELSE sender_id END ORDER BY send_time DESC) AS rn, " +
            "    COUNT(*)     OVER (PARTITION BY CASE WHEN sender_id = #{userId} THEN receiver_id ELSE sender_id END) AS match_count " +
            "  FROM sys_chat_message " +
            "  WHERE (sender_id = #{userId} OR receiver_id = #{userId}) " +
            "    AND msg_type = 1 AND COALESCE(recalled, 0) = 0 " +
            "    AND content LIKE CONCAT('%', #{kw}, '%') " +
            ") t " +
            "LEFT JOIN sys_user u ON u.id = t.other_id " +
            "WHERE t.rn = 1 " +
            "ORDER BY t.send_time DESC " +
            "LIMIT #{limit}")
    java.util.List<java.util.Map<String, Object>> searchPrivateLatest(@Param("userId") Long userId,
                                                                       @Param("kw") String kw,
                                                                       @Param("limit") int limit);
}
