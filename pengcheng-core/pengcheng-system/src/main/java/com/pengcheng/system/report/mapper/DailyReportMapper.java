package com.pengcheng.system.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.system.report.entity.DailyReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DailyReportMapper extends BaseMapper<DailyReport> {

    @Select("SELECT * FROM sys_daily_report WHERE user_id = #{userId} AND report_date = #{reportDate}")
    DailyReport findByUserAndDate(Long userId, String reportDate);
}
