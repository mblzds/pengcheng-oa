package com.pengcheng.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.message.entity.MessageCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息分类 Mapper
 */
@Mapper
public interface MessageCategoryMapper extends BaseMapper<MessageCategory> {
}
