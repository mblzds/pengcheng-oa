package com.pengcheng.realty.customer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.entity.CustomerPoolEventLog;
import com.pengcheng.realty.customer.mapper.CustomerPoolEventLogMapper;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
import com.pengcheng.system.service.SysConfigGroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CustomerPoolService")
class CustomerPoolServiceTest {

    private RealtyCustomerMapper customerMapper;
    private CustomerPoolEventLogMapper customerPoolEventLogMapper;
    private SysConfigGroupService configGroupService;
    private ObjectMapper objectMapper;
    private CustomerPoolService service;

    @BeforeEach
    void setUp() {
        customerMapper = mock(RealtyCustomerMapper.class);
        customerPoolEventLogMapper = mock(CustomerPoolEventLogMapper.class);
        configGroupService = mock(SysConfigGroupService.class);
        objectMapper = new ObjectMapper();
        service = new CustomerPoolService(customerMapper, customerPoolEventLogMapper, configGroupService, objectMapper);
    }

    @Test
    @DisplayName("updateRecycleConfig 先持久化再刷新内存")
    void updateConfigPersistsBeforeUpdatingMemory() throws Exception {
        service.updateRecycleConfig(10, 40);

        Customer privateReported = customer(1L, 2, 1);
        privateReported.setCreateTime(LocalDateTime.now().minusDays(41));
        privateReported.setLastFollowTime(LocalDateTime.now().minusDays(11));

        ArgumentCaptor<String> configValueCaptor = ArgumentCaptor.forClass(String.class);
        verify(configGroupService).saveConfig(org.mockito.ArgumentMatchers.eq(CustomerPoolService.CONFIG_GROUP_CODE), configValueCaptor.capture());
        JsonNode savedConfig = objectMapper.readTree(configValueCaptor.getValue());
        assertThat(savedConfig.get("noFollowDays").asInt()).isEqualTo(10);
        assertThat(savedConfig.get("noVisitDays").asInt()).isEqualTo(40);
        assertThat(service.getNoFollowDays()).isEqualTo(10);
        assertThat(service.getNoVisitDays()).isEqualTo(40);
        assertThat(CustomerPoolService.shouldRecycle(privateReported, LocalDateTime.now(), 10, 40)).isTrue();
    }

    @Test
    @DisplayName("updateRecycleConfig 持久化失败时不切换内存")
    void updateConfigDoesNotSwitchMemoryWhenPersistFails() {
        doThrow(new IllegalStateException("db down"))
                .when(configGroupService)
                .saveConfig(any(), any());

        assertThatThrownBy(() -> service.updateRecycleConfig(12, 45))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("保存公海池回收规则失败");
        assertThat(service.getNoFollowDays()).isEqualTo(7);
        assertThat(service.getNoVisitDays()).isEqualTo(30);
    }

    @Test
    @DisplayName("claimFromPublicPool 将客户转为私海、重置保护期并写事件日志")
    void claimFromPublicPoolClaimsCustomer() {
        Customer customer = customer(2001L, CustomerPoolService.POOL_PUBLIC, 1);
        when(customerMapper.selectById(2001L)).thenReturn(customer);

        service.claimFromPublicPool(2001L, 88L);

        assertThat(customer.getPoolType()).isEqualTo(CustomerPoolService.POOL_PRIVATE);
        assertThat(customer.getCreatorId()).isEqualTo(88L);
        assertThat(customer.getProtectionExpireTime()).isNotNull();
        assertThat(customer.getLastFollowTime()).isNotNull();
        verify(customerMapper).updateById(customer);
        ArgumentCaptor<CustomerPoolEventLog> eventCaptor = ArgumentCaptor.forClass(CustomerPoolEventLog.class);
        verify(customerPoolEventLogMapper).insert(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getCustomerId()).isEqualTo(2001L);
        assertThat(eventCaptor.getValue().getOperatorId()).isEqualTo(88L);
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo(CustomerPoolEventLog.EVENT_TYPE_CLAIM);
    }

    @Test
    @DisplayName("claimFromPublicPool 对非公海客户拒绝领取")
    void claimFromPublicPoolRejectsNonPublicCustomer() {
        Customer customer = customer(3001L, CustomerPoolService.POOL_PRIVATE, 1);
        when(customerMapper.selectById(3001L)).thenReturn(customer);

        assertThatThrownBy(() -> service.claimFromPublicPool(3001L, 88L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("不在公海池");
    }

    @Test
    @DisplayName("recycleToPublicPool 回收成功后写回收事件日志")
    void recycleToPublicPoolWritesRecycleEventLog() {
        Customer customer = customer(4001L, CustomerPoolService.POOL_PRIVATE, 1);
        customer.setUpdateTime(LocalDateTime.now().minusMinutes(5));

        PageStub page = new PageStub();
        page.setRecords(java.util.List.of(customer));
        when(customerMapper.selectPage(any(), any())).thenReturn(page);
        when(customerMapper.update(any(), any())).thenReturn(1);

        int recycled = service.recycleToPublicPool();

        assertThat(recycled).isEqualTo(1);
        ArgumentCaptor<CustomerPoolEventLog> eventCaptor = ArgumentCaptor.forClass(CustomerPoolEventLog.class);
        verify(customerPoolEventLogMapper).insert(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getCustomerId()).isEqualTo(4001L);
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo(CustomerPoolEventLog.EVENT_TYPE_RECYCLE);
        assertThat(eventCaptor.getValue().getEventSource()).isEqualTo(CustomerPoolEventLog.EVENT_SOURCE_AUTO);
    }

    private Customer customer(Long id, int poolType, int status) {
        Customer customer = Customer.builder()
                .poolType(poolType)
                .status(status)
                .build();
        customer.setId(id);
        customer.setCreateTime(LocalDateTime.now().minusDays(50));
        customer.setLastFollowTime(LocalDateTime.now().minusDays(20));
        return customer;
    }

    private static class PageStub extends com.baomidou.mybatisplus.extension.plugins.pagination.Page<Customer> {
        PageStub() {
            super(1, 100);
        }
    }
}
