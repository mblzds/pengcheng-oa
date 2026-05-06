package com.pengcheng.job.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pengcheng.common.result.PageResult;
import com.pengcheng.job.entity.SysJobLog;
import com.pengcheng.job.mapper.SysJobLogMapper;
import com.pengcheng.job.service.SysJobLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 定时任务日志服务实现
 */
@Service
@RequiredArgsConstructor
public class SysJobLogServiceImpl extends ServiceImpl<SysJobLogMapper, SysJobLog> implements SysJobLogService {

    @Override
    public PageResult<SysJobLog> page(Integer page, Integer pageSize, String jobName, String jobGroup, Integer status) {
        Page<SysJobLog> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<SysJobLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(jobName), SysJobLog::getJobName, jobName)
                .eq(StringUtils.hasText(jobGroup), SysJobLog::getJobGroup, jobGroup)
                .eq(status != null, SysJobLog::getStatus, status)
                .orderByDesc(SysJobLog::getStartTime);
        return PageResult.of(this.page(pageParam, wrapper));
    }

    @Override
    public void clean() {
        this.remove(new LambdaQueryWrapper<>());
    }
}
