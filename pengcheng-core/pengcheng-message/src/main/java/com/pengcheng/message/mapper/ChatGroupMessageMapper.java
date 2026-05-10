package com.pengcheng.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.message.entity.ChatGroupMessage;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 群消息Mapper
 */
@Mapper
public interface ChatGroupMessageMapper extends BaseMapper<ChatGroupMessage> {

    /**
     * 获取群的最新一条消息
     */
    @Select("SELECT * FROM sys_chat_group_message WHERE group_id = #{groupId} ORDER BY send_time DESC LIMIT 1")
    ChatGroupMessage selectLatestMessage(@Param("groupId") Long groupId);

    /** 物理批量删除 send_time 早于 cutoff 的群聊消息；分批控制锁范围 */
    @Delete("DELETE FROM sys_chat_group_message WHERE send_time < #{cutoff} LIMIT #{batchSize}")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff, @Param("batchSize") int batchSize);

    /**
     * 搜索群聊聊天记录：按"群"聚合，每个群只取最新匹配条 + 总匹配条数 + 群基础信息。
     */
    @Select("SELECT t.group_id AS id, t.msg_id AS msgId, t.content, t.send_time AS sendTime, " +
            "       t.match_count AS matchCount, t.sender_id AS senderId, t.sender_name AS senderName, " +
            "       g.name, g.avatar " +
            "FROM ( " +
            "  SELECT id AS msg_id, group_id, sender_id, sender_name, content, send_time, " +
            "         ROW_NUMBER() OVER (PARTITION BY group_id ORDER BY send_time DESC) AS rn, " +
            "         COUNT(*)     OVER (PARTITION BY group_id) AS match_count " +
            "  FROM sys_chat_group_message " +
            "  WHERE group_id IN (SELECT group_id FROM sys_chat_group_member WHERE user_id = #{userId}) " +
            "    AND msg_type = 1 AND COALESCE(recalled, 0) = 0 " +
            "    AND content LIKE CONCAT('%', #{kw}, '%') " +
            ") t " +
            "JOIN sys_chat_group g ON g.id = t.group_id " +
            "WHERE t.rn = 1 " +
            "ORDER BY t.send_time DESC " +
            "LIMIT #{limit}")
    java.util.List<java.util.Map<String, Object>> searchGroupLatest(@Param("userId") Long userId,
                                                                     @Param("kw") String kw,
                                                                     @Param("limit") int limit);
}
