package com.pengcheng.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pengcheng.system.entity.SysUserRole;
import com.pengcheng.system.mapper.SysUserRoleMapper;
import com.pengcheng.system.service.SysUserRoleService;
import org.springframework.stereotype.Service;

/**
 * 用户角色关联服务实现
 */
@Service
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleMapper, SysUserRole> implements SysUserRoleService {

    @Override
    public void deleteByUserId(Long userId) {
        this.remove(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
    }

    @Override
    public void deleteByRoleId(Long roleId) {
        this.remove(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, roleId));
    }
}
