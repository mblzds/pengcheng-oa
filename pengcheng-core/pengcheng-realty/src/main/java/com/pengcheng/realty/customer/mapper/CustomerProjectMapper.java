package com.pengcheng.realty.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.realty.customer.entity.CustomerProject;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户-项目关联 Mapper 接口
 */
@Mapper
public interface CustomerProjectMapper extends BaseMapper<CustomerProject> {

}
