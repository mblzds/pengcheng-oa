package com.pengcheng;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * MasterLife 启动类
 */
@SpringBootApplication
@EnableScheduling
public class PengchengApplication {

    public static void main(String[] args) {
        SpringApplication.run(PengchengApplication.class, args);
    }
}
