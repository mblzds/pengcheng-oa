package com.pengcheng.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.db.config.MybatisPlusConfig;
import com.pengcheng.db.interceptor.DataPermissionInterceptor;
import com.pengcheng.realty.payment.entity.PayNotifyLog;
import com.pengcheng.realty.payment.entity.PaymentRequest;
import com.pengcheng.realty.payment.mapper.PayNotifyLogMapper;
import com.pengcheng.realty.payment.mapper.PaymentRequestMapper;
import com.pengcheng.realty.payment.service.PaymentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import org.testcontainers.containers.MySQLContainer;

import jakarta.annotation.Resource;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@SpringBootTest(
        classes = PaymentCallbackIdempotencyTest.TestApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@EnabledIf(
        value = "#{systemProperties['it.mysql.url'] != null || T(org.testcontainers.DockerClientFactory).instance().isDockerAvailable()}",
        loadContext = false
)
@DisplayName("Payment callback idempotency integration")
class PaymentCallbackIdempotencyTest {

    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("pengcheng_test")
            .withUsername("root")
            .withPassword("test123456");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        if (useExternalMysql()) {
            registry.add("spring.datasource.url", () -> requiredSystemProperty("it.mysql.url"));
            registry.add("spring.datasource.username", () -> System.getProperty("it.mysql.username", "root"));
            registry.add("spring.datasource.password", () -> System.getProperty("it.mysql.password", ""));
            registry.add("spring.flyway.enabled", () -> "false");
        } else {
            if (!MYSQL.isRunning()) {
                MYSQL.start();
            }
            registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
            registry.add("spring.datasource.username", MYSQL::getUsername);
            registry.add("spring.datasource.password", MYSQL::getPassword);
            registry.add("spring.flyway.enabled", () -> "true");
            registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        }
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    private static boolean useExternalMysql() {
        String url = System.getProperty("it.mysql.url");
        return url != null && !url.isBlank();
    }

    private static String requiredSystemProperty(String key) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required system property: " + key);
        }
        return value;
    }

    @Resource
    private PaymentService paymentService;

    @Resource
    private PaymentRequestMapper paymentRequestMapper;

    @Resource
    private PayNotifyLogMapper payNotifyLogMapper;

    private String createdOrderNo;

    private String createdNotifyId;

    @AfterEach
    void cleanup() {
        if (createdNotifyId != null) {
            payNotifyLogMapper.delete(
                    new LambdaQueryWrapper<PayNotifyLog>().eq(PayNotifyLog::getNotifyId, createdNotifyId)
            );
            createdNotifyId = null;
        }
        if (createdOrderNo != null) {
            paymentRequestMapper.delete(
                    new LambdaQueryWrapper<PaymentRequest>().eq(PaymentRequest::getOrderNo, createdOrderNo)
            );
            createdOrderNo = null;
        }
    }

    @Test
    @DisplayName("同一 notifyId 的支付回调重复进入时只落一条 pay_notify_log，订单仍保持已付款")
    void duplicateNotifyIdIsIdempotent() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String orderNo = "PAY-IT-" + suffix;
        String notifyId = "notify-it-" + suffix;
        createdOrderNo = orderNo;
        createdNotifyId = notifyId;

        PaymentRequest request = PaymentRequest.builder()
                .orderNo(orderNo)
                .applicantId(1L)
                .requestType(PaymentService.TYPE_EXPENSE)
                .expenseType(1)
                .amount(new BigDecimal("100.00"))
                .status(PaymentService.STATUS_APPROVED)
                .payStatus(PaymentService.PAY_STATUS_UNPAID)
                .description("集成测试订单")
                .build();
        paymentRequestMapper.insert(request);

        boolean first = paymentService.updatePayStatus(orderNo, "TRADE-001", "alipay", 100.0, notifyId, "{\"round\":1}");
        boolean second = paymentService.updatePayStatus(orderNo, "TRADE-001", "alipay", 100.0, notifyId, "{\"round\":2}");

        assertThat(first).isTrue();
        assertThat(second).isTrue();

        PaymentRequest updated = paymentRequestMapper.selectById(request.getId());
        assertThat(updated.getPayStatus()).isEqualTo(PaymentService.PAY_STATUS_PAID);
        assertThat(updated.getThirdTradeNo()).isEqualTo("TRADE-001");
        assertThat(updated.getPayChannel()).isEqualTo("alipay");
        assertThat(updated.getPaidTime()).isNotNull();

        Long notifyCount = payNotifyLogMapper.selectCount(
                new LambdaQueryWrapper<PayNotifyLog>().eq(PayNotifyLog::getNotifyId, notifyId)
        );
        assertThat(notifyCount).isEqualTo(1L);

        PayNotifyLog log = payNotifyLogMapper.selectOne(
                new LambdaQueryWrapper<PayNotifyLog>().eq(PayNotifyLog::getNotifyId, notifyId)
        );
        assertThat(log.getProcessResult()).isEqualTo(PayNotifyLog.RESULT_SUCCESS);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            RedisAutoConfiguration.class,
            RedisRepositoriesAutoConfiguration.class
    })
    @MapperScan({
            "com.pengcheng.realty.payment.mapper",
            "com.pengcheng.realty.customer.mapper",
            "com.pengcheng.realty.alliance.mapper"
    })
    @Import({
            MybatisPlusConfig.class,
            DataPermissionInterceptor.class,
            PaymentService.class
    })
    static class TestApp {

        @Bean
        RedisConnectionFactory redisConnectionFactory() {
            return mock(RedisConnectionFactory.class);
        }
    }
}
