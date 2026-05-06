package com.pengcheng.hr.employee.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.hr.employee.entity.EmployeeChange;

/**
 * 人事异动服务（公司级）
 */
public interface EmployeeChangeService {

    IPage<EmployeeChange> page(Page<EmployeeChange> page, Long userId, Integer changeType, Integer status);

    EmployeeChange getById(Long id);

    Long create(EmployeeChange change);

    /** 异动生效；离职类型同步 sys_user.is_quit */
    void setEffective(Long id);
}
