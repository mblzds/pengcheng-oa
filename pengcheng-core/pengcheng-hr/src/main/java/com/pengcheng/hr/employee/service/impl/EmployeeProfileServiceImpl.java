package com.pengcheng.hr.employee.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.hr.employee.entity.EmployeeProfile;
import com.pengcheng.hr.employee.mapper.EmployeeProfileMapper;
import com.pengcheng.hr.employee.service.EmployeeProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployeeProfileServiceImpl implements EmployeeProfileService {

    private final EmployeeProfileMapper employeeProfileMapper;

    @Override
    public EmployeeProfile getByUserId(Long userId) {
        if (userId == null) return null;
        return employeeProfileMapper.selectOne(
                new LambdaQueryWrapper<EmployeeProfile>().eq(EmployeeProfile::getUserId, userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdate(EmployeeProfile profile) {
        if (profile == null || profile.getUserId() == null) {
            throw new IllegalArgumentException("员工ID不能为空");
        }
        EmployeeProfile existing = getByUserId(profile.getUserId());
        if (existing != null) {
            profile.setId(existing.getId());
            employeeProfileMapper.updateById(profile);
        } else {
            employeeProfileMapper.insert(profile);
        }
    }
}
