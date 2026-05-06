package com.pengcheng.realty.commission.provider;

import com.pengcheng.hr.performance.provider.KpiDataSourceProvider;
import com.pengcheng.realty.commission.service.CommissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 佣金数据源：按周期内已审核通过应结佣金汇总，供绩效 auto_commission 指标建议值
 */
@Component
@RequiredArgsConstructor
public class KpiCommissionDataProvider implements KpiDataSourceProvider {

    public static final String DATA_SOURCE = "auto_commission";

    private final CommissionService commissionService;

    @Override
    public String getDataSource() {
        return DATA_SOURCE;
    }

    @Override
    public BigDecimal getActualValue(LocalDate startDate, LocalDate endDate, Long userId, String templateCode) {
        if (userId == null || startDate == null || endDate == null) return null;
        return commissionService.sumPayableByUserIdAndDateRange(userId, startDate, endDate);
    }
}
