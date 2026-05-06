package com.pengcheng.admin.integration.kpi;

import com.pengcheng.hr.performance.provider.KpiDataSourceProvider;
import com.pengcheng.system.quality.service.SalesQualityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 销售质检数据源：按周期内质检综合分平均值，供绩效 auto_quality 指标建议值
 */
@Component
@RequiredArgsConstructor
public class KpiQualityDataProvider implements KpiDataSourceProvider {

    public static final String DATA_SOURCE = "auto_quality";

    private final SalesQualityService salesQualityService;

    @Override
    public String getDataSource() {
        return DATA_SOURCE;
    }

    @Override
    public BigDecimal getActualValue(LocalDate startDate, LocalDate endDate, Long userId, String templateCode) {
        if (userId == null || startDate == null || endDate == null) return null;
        Double avg = salesQualityService.getAverageOverallScoreInRange(userId, startDate, endDate);
        return avg == null ? null : BigDecimal.valueOf(avg);
    }
}
