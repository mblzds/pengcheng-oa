package com.pengcheng.pay;

import com.pengcheng.system.helper.SystemConfigHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AlipayVerifyService")
class AlipayVerifyServiceTest {

    private SystemConfigHelper configHelper;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private AlipayVerifyService service;

    @BeforeEach
    void setUp() {
        configHelper = mock(SystemConfigHelper.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new AlipayVerifyService(configHelper, redisTemplate);
    }

    @Test
    @DisplayName("isProcessed 对空 notifyId 返回 false，命中 Redis 返回 true")
    void isProcessedUsesRedisKey() {
        when(redisTemplate.hasKey("pay:alipay:notify:notify-1")).thenReturn(true);

        assertThat(service.isProcessed("")).isFalse();
        assertThat(service.isProcessed("notify-1")).isTrue();
    }

    @Test
    @DisplayName("markProcessed 写入 Redis 并设置 24 小时过期")
    void markProcessedStoresRedisKey() {
        service.markProcessed("notify-2");

        verify(valueOperations).set(
                eq("pay:alipay:notify:notify-2"),
                eq("1"),
                eq(24L),
                eq(TimeUnit.HOURS)
        );
    }

    @Test
    @DisplayName("verifyParams 要求 notify_id 为必填字段")
    void verifyParamsRequiresNotifyId() {
        Map<String, String> paramsWithoutNotifyId = Map.of(
                "out_trade_no", "order-1",
                "trade_no", "trade-1",
                "trade_status", "TRADE_SUCCESS",
                "total_amount", "0.01"
        );
        Map<String, String> paramsWithNotifyId = Map.of(
                "notify_id", "notify-3",
                "out_trade_no", "order-1",
                "trade_no", "trade-1",
                "trade_status", "TRADE_SUCCESS",
                "total_amount", "0.01"
        );

        assertThat(service.verifyParams(paramsWithoutNotifyId)).isFalse();
        assertThat(service.verifyParams(paramsWithNotifyId)).isTrue();
    }
}
