package com.pengcheng.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.common.annotation.DataScope;
import com.pengcheng.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;

/**
 * 用户Mapper
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 分页查询用户
     */
    @DataScope(deptAlias = "dept_id", userAlias = "create_by")
    IPage<SysUser> selectUserPage(IPage<SysUser> page, @Param(Constants.WRAPPER) Wrapper<SysUser> queryWrapper);

    /**
     * 根据用户ID获取角色编码列表
     */
    @Select("SELECT r.code FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.status = 1 AND r.deleted = 0")
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID获取权限标识列表
     */
    @Select("SELECT DISTINCT m.permission FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND m.status = 1 AND m.deleted = 0 AND m.permission IS NOT NULL AND m.permission != ''")
    List<String> selectPermissionsByUserId(@Param("userId") Long userId);

    /**
     * 唯一性检测：跨软删全表统计 username。
     * 唯一索引 uk_username 不区分 deleted，软删行也会占用索引位，必须包含进来。
     */
    @Select("<script>SELECT COUNT(*) FROM sys_user WHERE username = #{username}" +
            "<if test='excludeId != null'> AND id != #{excludeId}</if></script>")
    long countByUsernameAll(@Param("username") String username, @Param("excludeId") Long excludeId);

    /** 唯一性检测：跨软删全表统计 phone。 */
    @Select("<script>SELECT COUNT(*) FROM sys_user WHERE phone = #{phone}" +
            "<if test='excludeId != null'> AND id != #{excludeId}</if></script>")
    long countByPhoneAll(@Param("phone") String phone, @Param("excludeId") Long excludeId);

    /** 唯一性检测：跨软删全表统计 open_id。 */
    @Select("<script>SELECT COUNT(*) FROM sys_user WHERE open_id = #{openId}" +
            "<if test='excludeId != null'> AND id != #{excludeId}</if></script>")
    long countByOpenIdAll(@Param("openId") String openId, @Param("excludeId") Long excludeId);
}
