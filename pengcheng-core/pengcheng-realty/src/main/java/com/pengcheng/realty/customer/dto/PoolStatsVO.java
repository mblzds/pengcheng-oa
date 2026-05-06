package com.pengcheng.realty.customer.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 公海池统计数据 VO
 */
@Data
@Builder
public class PoolStatsVO {

    /**
     * 公海池客户总数
     */
    private Integer total;

    /**
     * 今日新增
     */
    private Integer todayNew;

    /**
     * 今日领取
     */
    private Integer todayClaimed;

    /**
     * 今日回收
     */
    private Integer todayRecycled;
}
