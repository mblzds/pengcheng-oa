package com.pengcheng.realty.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.realty.payment.entity.PayNotifyLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付回调审计日志 Mapper
 */
@Mapper
public interface PayNotifyLogMapper extends BaseMapper<PayNotifyLog> {
}
