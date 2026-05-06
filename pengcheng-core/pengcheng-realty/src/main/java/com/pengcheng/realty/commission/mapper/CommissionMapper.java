package com.pengcheng.realty.commission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.realty.commission.entity.Commission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 佣金主表 Mapper 接口
 */
@Mapper
public interface CommissionMapper extends BaseMapper<Commission> {

}
