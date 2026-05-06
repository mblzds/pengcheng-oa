package com.pengcheng.realty.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.realty.customer.entity.CustomerVisit;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户到访记录 Mapper 接口
 */
@Mapper
public interface CustomerVisitMapper extends BaseMapper<CustomerVisit> {

}
