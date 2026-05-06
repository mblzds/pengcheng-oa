package com.pengcheng.realty.customer.service;

import com.pengcheng.realty.common.exception.InvalidStateTransitionException;
import com.pengcheng.realty.customer.dto.CustomerDealDTO;
import com.pengcheng.realty.customer.dto.CustomerDealUpdateDTO;
import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.entity.CustomerDeal;
import com.pengcheng.realty.customer.mapper.CustomerDealMapper;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CustomerDealService")
class CustomerDealServiceTest {

    private CustomerDealMapper customerDealMapper;
    private RealtyCustomerMapper customerMapper;
    private CustomerDealService service;

    @BeforeEach
    void setUp() {
        customerDealMapper = mock(CustomerDealMapper.class);
        customerMapper = mock(RealtyCustomerMapper.class);
        service = new CustomerDealService(customerDealMapper, customerMapper);
    }

    @Test
    @DisplayName("createDeal 对已到访客户创建成交并更新客户状态")
    void createDealCreatesDealAndUpdatesCustomer() {
        Customer customer = Customer.builder().status(2).build();
        customer.setId(4001L);
        when(customerMapper.selectById(4001L)).thenReturn(customer);
        doAnswer(invocation -> {
            CustomerDeal deal = invocation.getArgument(0);
            deal.setId(5001L);
            return 1;
        }).when(customerDealMapper).insert(any(CustomerDeal.class));

        Long id = service.createDeal(validDealDto());

        assertThat(id).isEqualTo(5001L);
        assertThat(customer.getStatus()).isEqualTo(3);
        verify(customerMapper).updateById(customer);
    }

    @Test
    @DisplayName("createDeal 对未到访客户拒绝成交录入")
    void createDealRejectsInvalidState() {
        Customer customer = Customer.builder().status(1).build();
        customer.setId(4001L);
        when(customerMapper.selectById(4001L)).thenReturn(customer);

        assertThatThrownBy(() -> service.createDeal(validDealDto()))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("不允许录入成交");
    }

    @Test
    @DisplayName("updateDeal 更新网签备案贷款回款状态")
    void updateDealMutatesStatuses() {
        CustomerDeal deal = CustomerDeal.builder().onlineSignStatus(0).filingStatus(0).loanStatus(0).paymentStatus(0).build();
        deal.setId(5001L);
        when(customerDealMapper.selectById(5001L)).thenReturn(deal);

        CustomerDealUpdateDTO dto = CustomerDealUpdateDTO.builder()
                .dealId(5001L)
                .onlineSignStatus(1)
                .filingStatus(1)
                .loanStatus(2)
                .paymentStatus(2)
                .build();

        service.updateDeal(dto);

        assertThat(deal.getOnlineSignStatus()).isEqualTo(1);
        assertThat(deal.getFilingStatus()).isEqualTo(1);
        assertThat(deal.getLoanStatus()).isEqualTo(2);
        assertThat(deal.getPaymentStatus()).isEqualTo(2);
        verify(customerDealMapper).updateById(deal);
    }

    @Test
    @DisplayName("listDealsByCustomerId 返回查询结果")
    void listDealsByCustomerIdReturnsMapperResult() {
        CustomerDeal deal = CustomerDeal.builder().roomNo("A-3-1001").build();
        when(customerDealMapper.selectList(any())).thenReturn(List.of(deal));

        assertThat(service.listDealsByCustomerId(4001L)).containsExactly(deal);
    }

    private CustomerDealDTO validDealDto() {
        return CustomerDealDTO.builder()
                .customerId(4001L)
                .roomNo("A-3-1001")
                .dealAmount(new BigDecimal("8000000"))
                .dealTime(LocalDateTime.of(2026, 4, 20, 10, 0))
                .signStatus(1)
                .subscribeType(2)
                .build();
    }
}
