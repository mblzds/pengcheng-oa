package com.pengcheng.realty.commission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.realty.commission.entity.CommissionChangeLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 佣金变更日志 Mapper 接口
 */
@Mapper
public interface CommissionChangeLogMapper extends BaseMapper<CommissionChangeLog> {

}
