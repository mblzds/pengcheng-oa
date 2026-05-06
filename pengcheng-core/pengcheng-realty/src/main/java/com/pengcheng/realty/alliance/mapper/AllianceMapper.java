package com.pengcheng.realty.alliance.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.pengcheng.common.annotation.DataScope;
import com.pengcheng.realty.alliance.entity.Alliance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 联盟商 Mapper 接口
 * <p>
 * 数据权限规则（通过 @DataScope 注解 + DataPermissionInterceptor 自动注入 WHERE 条件）：
 * <ul>
 *   <li>渠道同事：仅对接的联盟商（通过 allianceAlias = "id" 过滤 channel_user_id）</li>
 *   <li>渠道总监/行政文员/行政总监：全部</li>
 *   <li>联盟商负责人：仅本联盟商（通过 allianceAlias = "id" 过滤 user_id）</li>
 * </ul>
 */
@Mapper
public interface AllianceMapper extends BaseMapper<Alliance> {

    /**
     * 分页查询联盟商列表（带数据权限过滤）
     */
    @Select("SELECT * FROM alliance ${ew.customSqlSegment}")
    @DataScope(allianceAlias = "id")
    IPage<Alliance> selectPageWithScope(IPage<Alliance> page, @Param(Constants.WRAPPER) Wrapper<Alliance> queryWrapper);

    /**
     * 查询联盟商列表（带数据权限过滤）
     */
    @Select("SELECT * FROM alliance ${ew.customSqlSegment}")
    @DataScope(allianceAlias = "id")
    List<Alliance> selectListWithScope(@Param(Constants.WRAPPER) Wrapper<Alliance> queryWrapper);

    /**
     * 根据关联系统账号ID查询联盟商
     */
    default Alliance selectByUserId(Long userId) {
        return selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Alliance>()
                .eq(Alliance::getUserId, userId)
                .last("LIMIT 1"));
    }
}
