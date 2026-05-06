package com.pengcheng.realty.receivable.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.pengcheng.realty.customer.entity.CustomerDeal;
import com.pengcheng.realty.customer.mapper.CustomerDealMapper;
import com.pengcheng.realty.receivable.dto.ReceivablePlanCreateDTO;
import com.pengcheng.realty.receivable.dto.ReceivableRecordCreateDTO;
import com.pengcheng.realty.receivable.entity.ReceivableAlert;
import com.pengcheng.realty.receivable.entity.ReceivablePlan;
import com.pengcheng.realty.receivable.mapper.ReceivableAlertMapper;
import com.pengcheng.realty.receivable.mapper.ReceivablePlanMapper;
import com.pengcheng.realty.receivable.mapper.ReceivableRecordMapper;
import com.pengcheng.realty.receivable.vo.ReceivableStatsVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * P0-2 回款模块单元测试（分期/登记/状态机/逾期扫描/告警去重/统计）
 */
@DisplayName("ReceivableService — 回款核心")
class ReceivableServiceTest {

    private ReceivablePlanMapper planMapper;
    private ReceivableRecordMapper recordMapper;
    private ReceivableAlertMapper alertMapper;
    private CustomerDealMapper customerDealMapper;
    private ApplicationEventPublisher publisher;
    private ReceivableService service;

    @BeforeEach
    void setUp() {
        planMapper = mock(ReceivablePlanMapper.class);
        recordMapper = mock(ReceivableRecordMapper.class);
        alertMapper = mock(ReceivableAlertMapper.class);
        customerDealMapper = mock(CustomerDealMapper.class);
        publisher = mock(ApplicationEventPublisher.class);
        service = new ReceivableService(planMapper, recordMapper, alertMapper,
                customerDealMapper, publisher);
    }

    private CustomerDeal deal(long id, BigDecimal amount) {
        CustomerDeal d = new CustomerDeal();
        d.setId(id);
        d.setDealAmount(amount);
        return d;
    }

    private ReceivablePlanCreateDTO.Item item(int no, String date, String amt) {
        ReceivablePlanCreateDTO.Item it = new ReceivablePlanCreateDTO.Item();
        it.setPeriodNo(no);
        it.setDueDate(LocalDate.parse(date));
        it.setDueAmount(new BigDecimal(amt));
        return it;
    }

    // ---------- 1. createPlan ----------

    @Test
    @DisplayName("成交不存在 → 抛异常")
    void createPlan_dealNotFound() {
        when(customerDealMapper.selectById(1L)).thenReturn(null);
        ReceivablePlanCreateDTO dto = new ReceivablePlanCreateDTO();
        dto.setDealId(1L);
        dto.setItems(List.of(item(1, "2099-01-01", "100")));

        assertThatThrownBy(() -> service.createPlan(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("成交记录不存在");
    }

    @Test
    @DisplayName("期号重复 → 抛异常")
    void createPlan_duplicatePeriod() {
        when(customerDealMapper.selectById(1L)).thenReturn(deal(1L, new BigDecimal("200")));
        ReceivablePlanCreateDTO dto = new ReceivablePlanCreateDTO();
        dto.setDealId(1L);
        dto.setItems(Arrays.asList(
                item(1, "2099-01-01", "100"),
                item(1, "2099-02-01", "100")));
        assertThatThrownBy(() -> service.createPlan(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("期号重复");
    }

    @Test
    @DisplayName("正常创建 2 期 → 全部 insert，返回 id 列表")
    void createPlan_ok() {
        when(customerDealMapper.selectById(10L)).thenReturn(deal(10L, new BigDecimal("300")));
        ReceivablePlanCreateDTO dto = new ReceivablePlanCreateDTO();
        dto.setDealId(10L);
        dto.setItems(Arrays.asList(
                item(1, "2099-06-01", "100"),
                item(2, "2099-07-01", "200")));

        service.createPlan(dto);
        verify(planMapper, times(2)).insert(any());
        verify(publisher).publishEvent(any());
    }

    @Test
    @DisplayName("到期日早于今日 → 初始状态为 OVERDUE")
    void createPlan_pastDueInitialOverdue() {
        when(customerDealMapper.selectById(10L)).thenReturn(deal(10L, new BigDecimal("100")));
        ReceivablePlanCreateDTO dto = new ReceivablePlanCreateDTO();
        dto.setDealId(10L);
        dto.setItems(List.of(item(1, "2000-01-01", "100")));

        service.createPlan(dto);
        ArgumentCaptor<ReceivablePlan> cap = ArgumentCaptor.forClass(ReceivablePlan.class);
        verify(planMapper).insert(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo(ReceivablePlan.STATUS_OVERDUE);
    }

    // ---------- 2. registerRecord ----------

    @Test
    @DisplayName("部分到账 → status=PARTIAL，累计 paidAmount 正确")
    void registerRecord_partial() {
        ReceivablePlan plan = ReceivablePlan.builder()
                .dealId(1L).periodNo(1)
                .dueDate(LocalDate.now().plusDays(30))
                .dueAmount(new BigDecimal("1000"))
                .paidAmount(BigDecimal.ZERO)
                .status(ReceivablePlan.STATUS_PENDING)
                .build();
        plan.setId(100L);
        when(planMapper.selectById(100L)).thenReturn(plan);

        ReceivableRecordCreateDTO dto = new ReceivableRecordCreateDTO();
        dto.setPlanId(100L);
        dto.setAmount(new BigDecimal("300"));
        dto.setPaidDate(LocalDate.now());

        service.registerRecord(dto);

        ArgumentCaptor<ReceivablePlan> cap = ArgumentCaptor.forClass(ReceivablePlan.class);
        verify(planMapper).updateById(cap.capture());
        assertThat(cap.getValue().getPaidAmount()).isEqualByComparingTo("300");
        assertThat(cap.getValue().getStatus()).isEqualTo(ReceivablePlan.STATUS_PARTIAL);
    }

    @Test
    @DisplayName("全部到账 → status=PAID，关闭该分期的未处理告警")
    void registerRecord_fullyPaid_closesAlerts() {
        ReceivablePlan plan = ReceivablePlan.builder()
                .dealId(1L).periodNo(1)
                .dueDate(LocalDate.now().minusDays(5))
                .dueAmount(new BigDecimal("1000"))
                .paidAmount(new BigDecimal("500"))
                .status(ReceivablePlan.STATUS_OVERDUE)
                .build();
        plan.setId(200L);
        when(planMapper.selectById(200L)).thenReturn(plan);
        ReceivableAlert openAlert = ReceivableAlert.builder()
                .id(9L).planId(200L)
                .alertType(ReceivableAlert.TYPE_OVERDUE)
                .handled(ReceivableAlert.HANDLED_NO).notifyCount(1)
                .build();
        when(alertMapper.selectList(any(Wrapper.class))).thenReturn(List.of(openAlert));

        ReceivableRecordCreateDTO dto = new ReceivableRecordCreateDTO();
        dto.setPlanId(200L);
        dto.setAmount(new BigDecimal("500"));

        service.registerRecord(dto);

        ArgumentCaptor<ReceivablePlan> cap = ArgumentCaptor.forClass(ReceivablePlan.class);
        verify(planMapper).updateById(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo(ReceivablePlan.STATUS_PAID);
        verify(alertMapper).updateById(argThat(a ->
                ((ReceivableAlert) a).getHandled() == ReceivableAlert.HANDLED_YES));
    }

    @Test
    @DisplayName("金额 <= 0 → 抛异常")
    void registerRecord_invalidAmount() {
        ReceivableRecordCreateDTO dto = new ReceivableRecordCreateDTO();
        dto.setPlanId(1L);
        dto.setAmount(BigDecimal.ZERO);
        assertThatThrownBy(() -> service.registerRecord(dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- 3. resolveStatus ----------

    @Test
    @DisplayName("状态机：全量覆盖 PAID/PARTIAL/OVERDUE/PENDING/NOT_DUE")
    void resolveStatus_allBranches() {
        LocalDate today = LocalDate.now();
        ReceivablePlan p = ReceivablePlan.builder()
                .dueAmount(new BigDecimal("100")).paidAmount(new BigDecimal("100"))
                .dueDate(today.plusDays(5)).build();
        assertThat(service.resolveStatus(p, today)).isEqualTo(ReceivablePlan.STATUS_PAID);

        p.setPaidAmount(new BigDecimal("50"));
        p.setDueDate(today.plusDays(5));
        assertThat(service.resolveStatus(p, today)).isEqualTo(ReceivablePlan.STATUS_PARTIAL);

        p.setDueDate(today.minusDays(1));
        assertThat(service.resolveStatus(p, today)).isEqualTo(ReceivablePlan.STATUS_OVERDUE);

        p.setPaidAmount(BigDecimal.ZERO);
        p.setDueDate(today.minusDays(1));
        assertThat(service.resolveStatus(p, today)).isEqualTo(ReceivablePlan.STATUS_OVERDUE);

        p.setDueDate(today);
        assertThat(service.resolveStatus(p, today)).isEqualTo(ReceivablePlan.STATUS_PENDING);

        p.setDueDate(today.plusDays(10));
        assertThat(service.resolveStatus(p, today)).isEqualTo(ReceivablePlan.STATUS_NOT_DUE);
    }

    // ---------- 4. runOverdueCheck ----------

    @Test
    @DisplayName("逾期扫描：逾期 1 条 → 新增告警；即将到期 1 条 → 写 UPCOMING 告警")
    void runOverdueCheck_writesAlerts() {
        LocalDate today = LocalDate.now();
        ReceivablePlan overdue = ReceivablePlan.builder()
                .dueAmount(new BigDecimal("100")).paidAmount(BigDecimal.ZERO)
                .dueDate(today.minusDays(3))
                .status(ReceivablePlan.STATUS_PENDING).build();
        overdue.setId(1L);
        ReceivablePlan upcoming = ReceivablePlan.builder()
                .dueAmount(new BigDecimal("200")).paidAmount(BigDecimal.ZERO)
                .dueDate(today.plusDays(2))
                .status(ReceivablePlan.STATUS_NOT_DUE).build();
        upcoming.setId(2L);

        when(planMapper.selectList(any(Wrapper.class))).thenReturn(List.of(overdue, upcoming));
        when(alertMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        int[] r = service.runOverdueCheck();

        assertThat(r[0]).isEqualTo(1);
        assertThat(r[1]).isEqualTo(1);
        verify(alertMapper, times(2)).insert(any());
    }

    @Test
    @DisplayName("同一分期重复触发 → 告警 notify_count 累加，不 insert")
    void runOverdueCheck_reuseExistingAlert() {
        LocalDate today = LocalDate.now();
        ReceivablePlan overdue = ReceivablePlan.builder()
                .dueAmount(new BigDecimal("100")).paidAmount(BigDecimal.ZERO)
                .dueDate(today.minusDays(3))
                .status(ReceivablePlan.STATUS_OVERDUE).build();
        overdue.setId(1L);
        when(planMapper.selectList(any(Wrapper.class))).thenReturn(List.of(overdue));

        ReceivableAlert existing = ReceivableAlert.builder()
                .id(99L).planId(1L).alertType(ReceivableAlert.TYPE_OVERDUE)
                .notifyCount(2).handled(ReceivableAlert.HANDLED_NO).build();
        when(alertMapper.selectOne(any(Wrapper.class))).thenReturn(existing);

        service.runOverdueCheck();

        verify(alertMapper, never()).insert(any());
        ArgumentCaptor<ReceivableAlert> cap = ArgumentCaptor.forClass(ReceivableAlert.class);
        verify(alertMapper).updateById(cap.capture());
        assertThat(cap.getValue().getNotifyCount()).isEqualTo(3);
    }

    // ---------- 5. stats ----------

    @Test
    @DisplayName("stats 聚合：应收 / 已收 / 未收 / 逾期")
    void stats_aggregation() {
        ReceivablePlan a = ReceivablePlan.builder()
                .dueAmount(new BigDecimal("100")).paidAmount(new BigDecimal("100"))
                .status(ReceivablePlan.STATUS_PAID).build();
        ReceivablePlan b = ReceivablePlan.builder()
                .dueAmount(new BigDecimal("200")).paidAmount(new BigDecimal("50"))
                .status(ReceivablePlan.STATUS_OVERDUE).build();
        ReceivablePlan c = ReceivablePlan.builder()
                .dueAmount(new BigDecimal("300")).paidAmount(BigDecimal.ZERO)
                .status(ReceivablePlan.STATUS_PENDING).build();
        when(planMapper.selectList(any(Wrapper.class))).thenReturn(List.of(a, b, c));

        ReceivableStatsVO vo = service.stats();

        assertThat(vo.getTotalDue()).isEqualByComparingTo("600");
        assertThat(vo.getTotalPaid()).isEqualByComparingTo("150");
        assertThat(vo.getTotalUnpaid()).isEqualByComparingTo("450");
        assertThat(vo.getTotalOverdue()).isEqualByComparingTo("150"); // 200-50
        assertThat(vo.getOverdueCount()).isEqualTo(1);
        assertThat(vo.getTotalCount()).isEqualTo(3);
    }
}
