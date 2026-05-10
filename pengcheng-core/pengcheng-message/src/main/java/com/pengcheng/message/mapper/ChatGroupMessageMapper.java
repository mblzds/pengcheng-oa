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
}
