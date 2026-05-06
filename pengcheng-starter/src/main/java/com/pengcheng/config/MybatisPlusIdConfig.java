package com.pengcheng.config;

import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus ID 生成器配置
 * 显式指定 workerId / datacenterId，避免在受限环境中自动探测本机网络信息导致启动失败
 */
@Configuration
public class MybatisPlusIdConfig {

    @Bean
    public IdentifierGenerator identifierGenerator() {
        return new DefaultIdentifierGenerator(1L, 1L);
    }
}

