package com.pengcheng.realty.commission.service;

import com.pengcheng.common.event.DataChangeEvent;
import com.pengcheng.realty.commission.dto.CommissionDetailDTO;
import com.pengcheng.realty.commission.entity.Commission;
import com.pengcheng.realty.commission.entity.CommissionDetail;
import com.pengcheng.realty.commission.mapper.CommissionChangeLogMapper;
import com.pengcheng.realty.commission.mapper.CommissionDetailMapper;
import com.pengcheng.realty.commission.mapper.CommissionMapper;
import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.entity.CustomerDeal;
import com.pengcheng.realty.customer.entity.CustomerProject;
import com.pengcheng.realty.customer.mapper.CustomerDealMapper;
import com.pengcheng.realty.customer.mapper.CustomerProjectMapper;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
import com.pengcheng.realty.project.entity.ProjectCommissionRule;
import com.pengcheng.realty.project.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CommissionService auto settlement")
class CommissionServiceAutoSettlementTest {

    private CommissionMapper commissionMapper;
    private CommissionDetailMapper commissionDetailMapper;
    private CommissionChangeLogMapper commissionChangeLogMapper;
    private CustomerDealMapper customerDealMapper;
    private RealtyCustomerMapper customerMapper;
    private CustomerProjectMapper customerProjectMapper;
    private ProjectService projectService;
    private CommissionCalculator commissionCalculator;
    private ApplicationEventPublisher eventPublisher;
    private CommissionService service;

    @BeforeEach
    void setUp() {
        commissionMapper = mock(CommissionMapper.class);
        commissionDetailMapper = mock(CommissionDetailMapper.class);
        commissionChangeLogMapper = mock(CommissionChangeLogMapper.class);
        customerDealMapper = mock(CustomerDealMapper.class);
        customerMapper = mock(RealtyCustomerMapper.class);
        customerProjectMapper = mock(CustomerProjectMapper.class);
        projectService = mock(ProjectService.class);
        commissionCalculator = mock(CommissionCalculator.class);
        eventPublisher = mock(ApplicationEventPublisher.class);

        service = new CommissionService(
                commissionMapper,
                commissionDetailMapper,
                commissionChangeLogMapper,
                customerDealMapper,
                customerMapper,
                customerProjectMapper,
                projectService,
                commissionCalculator,
                eventPublisher
        );
    }

    @Test
    @DisplayName("autoCreatePendingCommissions 对满足条件的成交生成待审核佣金单")
    void autoCreatePendingCommissionsCreatesPendingCommission() {
        CustomerDeal deal = CustomerDeal.builder()
                .customerId(40001L)
                .dealAmount(new BigDecimal("8000000"))
                .dealTime(LocalDateTime.of(2026, 4, 20, 12, 0))
                .paymentStatus(2)
                .build();
        deal.setId(50001L);
        Customer customer = Customer.builder().allianceId(10001L).build();
        customer.setId(40001L);
        CustomerProject relation = CustomerProject.builder().customerId(40001L).projectId(20001L).build();
        ProjectCommissionRule rule = ProjectCommissionRule.builder().projectId(20001L).baseRate(new BigDecimal("0.018")).build();
        CommissionDetailDTO detail = CommissionDetailDTO.builder()
                .baseCommission(new BigDecimal("100000"))
                .jumpPointCommission(new BigDecimal("20000"))
                .cashReward(new BigDecimal("3000"))
                .firstDealReward(new BigDecimal("5000"))
                .platformReward(new BigDecimal("1000"))
                .build();

        when(customerDealMapper.selectList(any())).thenReturn(List.of(deal));
        when(commissionMapper.selectCount(any())).thenReturn(0L);
        when(customerMapper.selectById(40001L)).thenReturn(customer);
        when(customerProjectMapper.selectList(any())).thenReturn(List.of(relation), List.of(relation));
        when(projectService.getActiveCommissionRule(20001L)).thenReturn(rule);
        when(customerDealMapper.selectCount(any())).thenReturn(1L);
        when(commissionCalculator.calculate(rule, deal.getDealAmount(), 1))
                .thenReturn(CommissionCalculator.CalcResult.builder()
                        .success(true)
                        .detail(detail)
                        .manualConfirmItems(List.of("跳点佣金需复核"))
                        .message("ok")
                        .build());
        doAnswer(invocation -> {
            Commission commission = invocation.getArgument(0);
            commission.setId(90001L);
            return 1;
        }).when(commissionMapper).insert(any(Commission.class));

        int created = service.autoCreatePendingCommissions();

        assertThat(created).isEqualTo(1);

        ArgumentCaptor<Commission> commissionCaptor = ArgumentCaptor.forClass(Commission.class);
        verify(commissionMapper).insert(commissionCaptor.capture());
        Commission saved = commissionCaptor.getValue();
        assertThat(saved.getDealId()).isEqualTo(50001L);
        assertThat(saved.getProjectId()).isEqualTo(20001L);
        assertThat(saved.getAllianceId()).isEqualTo(10001L);
        assertThat(saved.getAuditStatus()).isEqualTo(1);
        assertThat(saved.getPayableAmount()).isEqualByComparingTo("128000");
        assertThat(saved.getPlatformFee()).isEqualByComparingTo("1000");
        assertThat(saved.getReceivableAmount()).isEqualByComparingTo("129000");
        assertThat(saved.getAuditRemark()).contains("月末自动生成").contains("跳点佣金需复核");

        ArgumentCaptor<CommissionDetail> detailCaptor = ArgumentCaptor.forClass(CommissionDetail.class);
        verify(commissionDetailMapper).insert(detailCaptor.capture());
        assertThat(detailCaptor.getValue().getCommissionId()).isEqualTo(90001L);
        assertThat(detailCaptor.getValue().getPlatformReward()).isEqualByComparingTo("1000");

        ArgumentCaptor<DataChangeEvent> eventCaptor = ArgumentCaptor.forClass(DataChangeEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getBizType()).isEqualTo("commission");
        assertThat(eventCaptor.getValue().getBizId()).isEqualTo(90001L);
    }

    @Test
    @DisplayName("autoCreatePendingCommissions 已存在佣金记录时跳过")
    void autoCreatePendingCommissionsSkipsExistingDeal() {
        CustomerDeal deal = CustomerDeal.builder().paymentStatus(2).build();
        deal.setId(50002L);
        when(customerDealMapper.selectList(any())).thenReturn(List.of(deal));
        when(commissionMapper.selectCount(any())).thenReturn(1L);

        int created = service.autoCreatePendingCommissions();

        assertThat(created).isZero();
        verify(commissionMapper, never()).insert(any(Commission.class));
        verify(commissionDetailMapper, never()).insert(any(CommissionDetail.class));
    }

    @Test
    @DisplayName("checkSettlementTrigger 支持全款到账或按揭放款")
    void checkSettlementTriggerRecognizesQualifiedDeals() {
        when(customerDealMapper.selectById(1L)).thenReturn(CustomerDeal.builder().paymentStatus(2).loanStatus(0).build());
        when(customerDealMapper.selectById(2L)).thenReturn(CustomerDeal.builder().paymentStatus(0).loanStatus(2).build());
        when(customerDealMapper.selectById(3L)).thenReturn(CustomerDeal.builder().paymentStatus(1).loanStatus(1).build());

        assertThat(service.checkSettlementTrigger(1L)).isTrue();
        assertThat(service.checkSettlementTrigger(2L)).isTrue();
        assertThat(service.checkSettlementTrigger(3L)).isFalse();
    }

}
