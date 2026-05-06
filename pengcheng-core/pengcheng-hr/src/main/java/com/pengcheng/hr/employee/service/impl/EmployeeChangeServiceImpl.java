package com.pengcheng.hr.employee.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.hr.employee.entity.EmployeeChange;
import com.pengcheng.hr.employee.mapper.EmployeeChangeMapper;
import com.pengcheng.hr.employee.service.EmployeeChangeService;
import com.pengcheng.system.entity.SysUser;
import com.pengcheng.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployeeChangeServiceImpl implements EmployeeChangeService {

    private final EmployeeChangeMapper employeeChangeMapper;
    private final SysUserService sysUserService;

    @Override
    public IPage<EmployeeChange> page(Page<EmployeeChange> page, Long userId, Integer changeType, Integer status) {
        LambdaQueryWrapper<EmployeeChange> q = new LambdaQueryWrapper<>();
        if (userId != null) q.eq(EmployeeChange::getUserId, userId);
        if (changeType != null) q.eq(EmployeeChange::getChangeType, changeType);
        if (status != null) q.eq(EmployeeChange::getStatus, status);
        q.orderByDesc(EmployeeChange::getChangeDate);
        return employeeChangeMapper.selectPage(page, q);
    }

    @Override
    public EmployeeChange getById(Long id) {
        return employeeChangeMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(EmployeeChange change) {
        if (change == null || change.getUserId() == null || change.getChangeType() == null || change.getChangeDate() == null) {
            throw new IllegalArgumentException("用户ID、异动类型、异动日期不能为空");
        }
        if (change.getStatus() == null) change.setStatus(EmployeeChange.STATUS_DRAFT);
        employeeChangeMapper.insert(change);
        return change.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setEffective(Long id) {
        EmployeeChange change = employeeChangeMapper.selectById(id);
        if (change == null) throw new IllegalArgumentException("异动记录不存在");
        if (change.getStatus() == EmployeeChange.STATUS_EFFECTIVE) return;
        change.setStatus(EmployeeChange.STATUS_EFFECTIVE);
        employeeChangeMapper.updateById(change);
        if (change.getChangeType() == EmployeeChange.TYPE_LEAVE) {
            SysUser user = sysUserService.getById(change.getUserId());
            if (user != null) {
                user.setIsQuit(1);
                sysUserService.updateById(user);
            }
        }
    }
}
