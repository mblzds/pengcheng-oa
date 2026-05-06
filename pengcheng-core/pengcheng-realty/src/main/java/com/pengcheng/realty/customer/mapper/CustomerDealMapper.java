package com.pengcheng.realty.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.realty.customer.entity.CustomerDeal;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户成交记录 Mapper 接口
 */
@Mapper
public interface CustomerDealMapper extends BaseMapper<CustomerDeal> {

}
