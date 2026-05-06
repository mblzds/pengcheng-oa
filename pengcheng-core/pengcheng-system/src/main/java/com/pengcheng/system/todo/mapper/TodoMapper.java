package com.pengcheng.system.todo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.system.todo.entity.Todo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TodoMapper extends BaseMapper<Todo> {

    @Select("SELECT COUNT(*) FROM sys_todo WHERE user_id = #{userId} AND status IN (0, 1)")
    int countPending(Long userId);
}
