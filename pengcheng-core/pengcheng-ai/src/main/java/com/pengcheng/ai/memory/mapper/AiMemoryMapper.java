package com.pengcheng.ai.memory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.ai.memory.entity.AiMemory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AiMemoryMapper extends BaseMapper<AiMemory> {

    /**
     * FULLTEXT 搜索记忆
     */
    @Select("""
        SELECT *, MATCH(content) AGAINST(#{keyword} IN BOOLEAN MODE) AS relevance
        FROM ai_memory
        WHERE deleted = 0 AND user_id = #{userId}
          AND MATCH(content) AGAINST(#{keyword} IN BOOLEAN MODE)
        ORDER BY relevance DESC
        LIMIT #{limit}
        """)
    List<AiMemory> searchByFulltext(@Param("userId") Long userId, @Param("keyword") String keyword, @Param("limit") int limit);

    /**
     * 查找客户相关记忆
     */
    @Select("SELECT * FROM ai_memory WHERE deleted = 0 AND customer_id = #{customerId} ORDER BY importance DESC, updated_at DESC LIMIT #{limit}")
    List<AiMemory> findByCustomer(@Param("customerId") Long customerId, @Param("limit") int limit);

    /**
     * 更新访问统计
     */
    @Update("UPDATE ai_memory SET access_count = access_count + 1, last_accessed_at = NOW() WHERE id = #{id}")
    void incrementAccess(@Param("id") Long id);

    /**
     * 查找过期的 L1 短期记忆
     */
    @Select("SELECT * FROM ai_memory WHERE deleted = 0 AND memory_level = 'L1' AND expires_at < NOW()")
    List<AiMemory> findExpiredL1Memories();
}
