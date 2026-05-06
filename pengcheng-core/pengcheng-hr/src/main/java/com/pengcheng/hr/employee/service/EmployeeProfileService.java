package com.pengcheng.hr.employee.service;

import com.pengcheng.hr.employee.entity.EmployeeProfile;

/**
 * 员工档案服务（公司级）
 */
public interface EmployeeProfileService {

    EmployeeProfile getByUserId(Long userId);

    void saveOrUpdate(EmployeeProfile profile);
}
