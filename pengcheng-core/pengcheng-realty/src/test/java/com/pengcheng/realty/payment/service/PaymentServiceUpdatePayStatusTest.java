package com.pengcheng.realty.payment.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.pengcheng.realty.payment.entity.PayNotifyLog;
import com.pengcheng.realty.payment.entity.PaymentRequest;
import com.pengcheng.realty.payment.mapper.PayNotifyLogMapper;
import com.pengcheng.realty.payment.mapper.PaymentApprovalMapper;
import com.pengcheng.realty.payment.mapper.PaymentRequestMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * P0-1 支付回调修复 · PaymentService.updatePayStatus 分支单元测试
 */
@DisplayName("PaymentService.updatePayStatus — 支付回调核心分支")
class PaymentServiceUpdatePayStatusTest {

    private PaymentRequestMapper paymentRequestMapper;
    private PaymentApprovalMapper paymentApprovalMapper;
    private PayNotifyLogMapper payNotifyLogMapper;
    private ApplicationEventPublisher publisher;
    private PaymentService service;

    @BeforeEach
    void setUp() {
        paymentRequestMapper = mock(PaymentRequestMapper.class);
        paymentApprovalMapper = mock(PaymentApprovalMapper.class);
        payNotifyLogMapper = mock(PayNotifyLogMapper.class);
        publisher = mock(ApplicationEventPublisher.class);
        service = new PaymentService(
                paymentRequestMapper,
                paymentApprovalMapper,
                payNotifyLogMapper,
                mock(com.pengcheng.realty.customer.mapper.CustomerDealMapper.class),
                mock(com.pengcheng.realty.alliance.mapper.AllianceMapper.class),
                publisher);
    }

    private PaymentRequest approvedRequest(String orderNo, BigDecimal amount) {
        PaymentRequest r = PaymentRequest.builder()
                .orderNo(orderNo)
                .amount(amount)
                .status(PaymentService.STATUS_APPROVED)
                .payStatus(PaymentService.PAY_STATUS_UNPAID)
                .build();
        r.setId(1001L);
        return r;
    }

    @Test
    @DisplayName("订单不存在 → 返回 false 并写失败日志")
    void whenOrderNotFound_returnsFalse() {
        when(paymentRequestMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        boolean ok = service.updatePayStatus("PAY001", "2099", "alipay",
                100.0, "N-1", "raw");

        assertThat(ok).isFalse();
        verify(paymentRequestMapper, never()).updateById(any());
        ArgumentCaptor<PayNotifyLog> captor = ArgumentCaptor.forClass(PayNotifyLog.class);
        verify(payNotifyLogMapper).insert(captor.capture());
        assertThat(captor.getValue().getProcessResult()).isEqualTo(PayNotifyLog.RESULT_FAILURE);
    }

    @Test
    @DisplayName("订单已支付 → 返回 true 并标记 duplicate（幂等）")
    void whenAlreadyPaid_returnsTrueAsDuplicate() {
        PaymentRequest r = approvedRequest("PAY002", new BigDecimal("200.00"));
        r.setPayStatus(PaymentService.PAY_STATUS_PAID);
        when(paymentRequestMapper.selectOne(any(Wrapper.class))).thenReturn(r);

        boolean ok = service.updatePayStatus("PAY002", "TRADE-X", "wechat",
                200.0, "N-2", "raw");

        assertThat(ok).isTrue();
        verify(paymentRequestMapper, never()).updateById(any());
        ArgumentCaptor<PayNotifyLog> captor = ArgumentCaptor.forClass(PayNotifyLog.class);
        verify(payNotifyLogMapper).insert(captor.capture());
        assertThat(captor.getValue().getProcessResult()).isEqualTo(PayNotifyLog.RESULT_DUPLICATE);
    }

    @Test
    @DisplayName("未审批通过的订单 → 返回 false 拒绝入账")
    void whenNotApproved_returnsFalse() {
        PaymentRequest r = approvedRequest("PAY003", new BigDecimal("500.00"));
        r.setStatus(PaymentService.STATUS_PENDING);
        when(paymentRequestMapper.selectOne(any(Wrapper.class))).thenReturn(r);

        boolean ok = service.updatePayStatus("PAY003", "TRADE-Y", "alipay",
                500.0, "N-3", "raw");

        assertThat(ok).isFalse();
        verify(paymentRequestMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("金额与订单不一致（>0.01 元）→ 返回 false")
    void whenAmountMismatch_returnsFalse() {
        PaymentRequest r = approvedRequest("PAY004", new BigDecimal("100.00"));
        when(paymentRequestMapper.selectOne(any(Wrapper.class))).thenReturn(r);

        boolean ok = service.updatePayStatus("PAY004", "TRADE-Z", "wechat",
                80.0, "N-4", "raw");

        assertThat(ok).isFalse();
        verify(paymentRequestMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("正常回调 → 落库为已付并发布事件")
    void whenValidCallback_marksPaidAndPublishesEvent() {
        PaymentRequest r = approvedRequest("PAY005", new BigDecimal("1234.56"));
        when(paymentRequestMapper.selectOne(any(Wrapper.class))).thenReturn(r);

        boolean ok = service.updatePayStatus("PAY005", "TRADE-OK", "alipay",
                1234.56, "N-5", "raw");

        assertThat(ok).isTrue();
        ArgumentCaptor<PaymentRequest> pr = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(paymentRequestMapper).updateById(pr.capture());
        PaymentRequest updated = pr.getValue();
        assertThat(updated.getPayStatus()).isEqualTo(PaymentService.PAY_STATUS_PAID);
        assertThat(updated.getPayChannel()).isEqualTo("alipay");
        assertThat(updated.getThirdTradeNo()).isEqualTo("TRADE-OK");
        assertThat(updated.getPaidTime()).isNotNull();
        verify(publisher).publishEvent(any());
    }

    @Test
    @DisplayName("notify_id 唯一索引重复 → 不中断主流程")
    void whenDuplicateNotifyId_swallowed() {
        PaymentRequest r = approvedRequest("PAY006", new BigDecimal("10.00"));
        when(paymentRequestMapper.selectOne(any(Wrapper.class))).thenReturn(r);
        doThrow(new DuplicateKeyException("dup")).when(payNotifyLogMapper).insert(any());

        boolean ok = service.updatePayStatus("PAY006", "TX", "wechat",
                10.0, "N-DUP", "raw");

        assertThat(ok).isTrue();
        verify(paymentRequestMapper).updateById(any());
    }

    @Test
    @DisplayName("generateOrderNo 形如 PAYyyyyMMddHHmmss + 6 位随机")
    void generateOrderNo_followsPattern() {
        String n = service.generateOrderNo();
        assertThat(n).hasSize(23).startsWith("PAY");
        assertThat(n.substring(3)).matches("\\d{20}");
    }
}
