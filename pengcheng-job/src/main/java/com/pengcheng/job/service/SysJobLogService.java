package com.pengcheng.job.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pengcheng.common.result.PageResult;
import com.pengcheng.job.entity.SysJobLog;

/**
 * 定时任务日志服务接口
 */
public interface SysJobLogService extends IService<SysJobLog> {

    /**
     * 分页查询
     */
    PageResult<SysJobLog> page(Integer page, Integer pageSize, String jobName, String jobGroup, Integer status);

    /**
     * 清空日志
     */
    void clean();
}
