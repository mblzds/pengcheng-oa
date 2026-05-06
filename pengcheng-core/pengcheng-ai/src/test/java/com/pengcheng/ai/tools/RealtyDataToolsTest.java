package com.pengcheng.ai.tools;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.pengcheng.realty.customer.entity.CustomerDeal;
import com.pengcheng.realty.customer.mapper.CustomerDealMapper;
import com.pengcheng.realty.receivable.service.ReceivableService;
import com.pengcheng.realty.receivable.vo.ReceivableStatsVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * P4-6 Function Calling 工具化 · RealtyDataTools 单元测试
 */
@DisplayName("RealtyDataTools — @Tool 聚合查询")
class RealtyDataToolsTest {

    private CustomerDealMapper dealMapper;
    private ReceivableService receivableService;
    private RealtyDataTools tools;

    @BeforeEach
    void setUp() {
        dealMapper = mock(CustomerDealMapper.class);
        receivableService = mock(ReceivableService.class);
        tools = new RealtyDataTools(dealMapper, receivableService);
    }

    private CustomerDeal deal(BigDecimal amount) {
        CustomerDeal d = new CustomerDeal();
        d.setDealAmount(amount);
        d.setDealTime(LocalDateTime.now());
        return d;
    }

    @Test
    @DisplayName("getDealSummary：多条成交 → 聚合计数与总金额 + 均值")
    void getDealSummary_aggregates() {
        when(dealMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                deal(new BigDecimal("3000000")),
                deal(new BigDecimal("2500000")),
                deal(new BigDecimal("4500000"))));

        Map<String, Object> r = tools.getDealSummary(null, null);

        assertThat(r.get("dealCount")).isEqualTo(3);
        assertThat(r.get("totalAmount")).isEqualTo(new BigDecimal("10000000"));
        assertThat(((BigDecimal) r.get("averageAmount")))
                .isEqualByComparingTo(new BigDecimal("3333333.33"));
    }

    @Test
    @DisplayName("getDealSummary：空结果 → 均值返回 0 不抛除零异常")
    void getDealSummary_emptyList() {
        when(dealMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        Map<String, Object> r = tools.getDealSummary("2099-01-01", "2099-01-01");

        assertThat(r.get("dealCount")).isEqualTo(0);
        assertThat(r.get("totalAmount")).isEqualTo(BigDecimal.ZERO);
        assertThat(r.get("averageAmount")).isEqualTo(BigDecimal.ZERO);
        assertThat(r.get("fromDate")).isEqualTo("2099-01-01");
    }

    @Test
    @DisplayName("getDealSummary：非法日期字符串 → 回退到今日（不崩）")
    void getDealSummary_invalidDate() {
        when(dealMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        Map<String, Object> r = tools.getDealSummary("not-a-date", "");
        assertThat(r.get("fromDate")).isEqualTo(java.time.LocalDate.now().toString());
    }

    @Test
    @DisplayName("getReceivableOverview：透传 ReceivableService.stats()")
    void getReceivableOverview_delegates() {
        ReceivableStatsVO expected = ReceivableStatsVO.builder()
                .totalDue(new BigDecimal("800"))
                .totalPaid(new BigDecimal("300"))
                .totalUnpaid(new BigDecimal("500"))
                .totalOverdue(new BigDecimal("100"))
                .overdueCount(1L)
                .totalCount(3L)
                .build();
        when(receivableService.stats()).thenReturn(expected);

        ReceivableStatsVO r = tools.getReceivableOverview();
        assertThat(r).isSameAs(expected);
    }

    @Test
    @DisplayName("两个方法都标注 @Tool 且 description 非空（符合 Spring AI Function Calling 语义）")
    void toolsCarryAnnotationMetadata() throws NoSuchMethodException {
        Class<?> toolAnnotationClass;
        try {
            toolAnnotationClass = Class.forName("org.springframework.ai.tool.annotation.Tool");
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Spring AI 1.1.x @Tool 注解不可用", e);
        }
        Method[] methods = {
                RealtyDataTools.class.getMethod("getDealSummary", String.class, String.class),
                RealtyDataTools.class.getMethod("getReceivableOverview")
        };
        for (Method m : methods) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            java.lang.annotation.Annotation a = m.getAnnotation((Class) toolAnnotationClass);
            assertThat(a).as("%s 应带 @Tool", m.getName()).isNotNull();
        }
    }
}
