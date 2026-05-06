package com.pengcheng.realty.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.realty.payment.entity.PaymentRequest;
import org.apache.ibatis.annotations.Mapper;

/**
 * 付款申请 Mapper 接口
 */
@Mapper
public interface PaymentRequestMapper extends BaseMapper<PaymentRequest> {

}
