package com.pengcheng.realty.customer.service;

import com.pengcheng.realty.common.exception.InvalidStateTransitionException;
import com.pengcheng.realty.customer.dto.CustomerVisitDTO;
import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.entity.CustomerVisit;
import com.pengcheng.realty.customer.mapper.CustomerVisitMapper;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CustomerVisitService")
class CustomerVisitServiceTest {

    private CustomerVisitMapper customerVisitMapper;
    private RealtyCustomerMapper customerMapper;
    private CustomerVisitService service;

    @BeforeEach
    void setUp() {
        customerVisitMapper = mock(CustomerVisitMapper.class);
        customerMapper = mock(RealtyCustomerMapper.class);
        service = new CustomerVisitService(customerVisitMapper, customerMapper);
    }

    @Test
    @DisplayName("createVisit 首次到访会创建记录并把客户置为已到访")
    void createVisitCreatesRecordAndTransitionsCustomer() {
        Customer customer = Customer.builder().status(1).build();
        customer.setId(1001L);
        when(customerMapper.selectById(1001L)).thenReturn(customer);
        doAnswer(invocation -> {
            CustomerVisit visit = invocation.getArgument(0);
            visit.setId(9001L);
            return 1;
        }).when(customerVisitMapper).insert(any(CustomerVisit.class));

        Long id = service.createVisit(validDto());

        assertThat(id).isEqualTo(9001L);
        assertThat(customer.getStatus()).isEqualTo(2);
        verify(customerMapper).updateById(customer);
    }

    @Test
    @DisplayName("createVisit 非报备/到访状态客户拒绝录入")
    void createVisitRejectsInvalidState() {
        Customer customer = Customer.builder().status(3).build();
        customer.setId(1001L);
        when(customerMapper.selectById(1001L)).thenReturn(customer);

        assertThatThrownBy(() -> service.createVisit(validDto()))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("不允许录入到访");
    }

    @Test
    @DisplayName("listVisitsByCustomerId 返回到访记录")
    void listVisitsByCustomerIdReturnsMapperResult() {
        CustomerVisit visit = CustomerVisit.builder().receptionist("赵驻场").build();
        when(customerVisitMapper.selectList(any())).thenReturn(List.of(visit));

        assertThat(service.listVisitsByCustomerId(1001L)).containsExactly(visit);
    }

    private CustomerVisitDTO validDto() {
        return CustomerVisitDTO.builder()
                .customerId(1001L)
                .actualVisitTime(LocalDateTime.of(2026, 4, 22, 10, 0))
                .actualVisitCount(2)
                .receptionist("赵驻场")
                .remark("首次到访")
                .build();
    }
}
