package com.pengcheng.system.visit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.system.visit.entity.SalesVisit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 销售拜访记录 Mapper
 */
@Mapper
public interface SalesVisitMapper extends BaseMapper<SalesVisit> {

    /** 统计用户拜访数据 */
    @Select("""
        SELECT visit_type, COUNT(*) AS cnt
        FROM sys_sales_visit
        WHERE user_id = #{userId} AND deleted = 0
          AND visit_time >= DATE_SUB(NOW(), INTERVAL 30 DAY)
        GROUP BY visit_type
        """)
    List<Map<String, Object>> countByTypeLastMonth(Long userId);

    /** 团队拜访排行 */
    @Select("""
        SELECT user_id, COUNT(*) AS visit_count
        FROM sys_sales_visit
        WHERE dept_id = #{deptId} AND deleted = 0
          AND visit_time >= DATE_SUB(NOW(), INTERVAL 30 DAY)
        GROUP BY user_id
        ORDER BY visit_count DESC
        LIMIT 10
        """)
    List<Map<String, Object>> teamRanking(Long deptId);
}
