package com.pengcheng.system.doc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.system.doc.entity.Doc;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface DocMapper extends BaseMapper<Doc> {

    @Select("SELECT * FROM sys_doc WHERE deleted = 0 AND MATCH(title, content) AGAINST(#{keyword} IN BOOLEAN MODE) AND space_id = #{spaceId} LIMIT #{limit}")
    List<Doc> searchInSpace(Long spaceId, String keyword, int limit);
}
