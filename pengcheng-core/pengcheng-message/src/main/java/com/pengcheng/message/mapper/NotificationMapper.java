package com.pengcheng.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.message.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统通知消息 Mapper
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
