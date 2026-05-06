package com.pengcheng.realty.customer.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.pengcheng.common.annotation.DataScope;
import com.pengcheng.realty.customer.entity.Customer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 客户 Mapper 接口
 * <p>
 * 数据权限规则（通过 @DataScope 注解 + DataPermissionInterceptor 自动注入 WHERE 条件）：
 * <ul>
 *   <li>驻场：仅负责项目的客户（通过 projectAlias 过滤）</li>
 *   <li>渠道：仅对接联盟商的客户（通过 allianceAlias 过滤）</li>
 *   <li>驻场总监/渠道总监/行政总监/行政文员：全部</li>
 *   <li>联盟商负责人：仅本联盟商客户（通过 allianceAlias 过滤）</li>
 * </ul>
 */
@Mapper
public interface RealtyCustomerMapper extends BaseMapper<Customer> {

    /**
     * 分页查询客户列表（带数据权限过滤）
     */
    @Select("SELECT * FROM customer ${ew.customSqlSegment}")
    @DataScope(allianceAlias = "alliance_id", projectAlias = "id")
    IPage<Customer> selectPageWithScope(IPage<Customer> page, @Param(Constants.WRAPPER) Wrapper<Customer> queryWrapper);

    /**
     * 查询客户列表（带数据权限过滤）
     */
    @Select("SELECT * FROM customer ${ew.customSqlSegment}")
    @DataScope(allianceAlias = "alliance_id", projectAlias = "id")
    List<Customer> selectListWithScope(@Param(Constants.WRAPPER) Wrapper<Customer> queryWrapper);
}
