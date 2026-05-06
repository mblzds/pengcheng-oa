package com.pengcheng.realty.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.realty.payment.entity.PaymentApproval;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审批记录 Mapper 接口
 */
@Mapper
public interface PaymentApprovalMapper extends BaseMapper<PaymentApproval> {

}
