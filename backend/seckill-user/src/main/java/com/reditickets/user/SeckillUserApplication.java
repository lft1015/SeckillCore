package com.reditickets.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.reditickets.user", "com.reditickets.common"})
@EnableDiscoveryClient
@MapperScan("com.reditickets.user.mapper")
public class SeckillUserApplication {
    public static void main(String[] args) {
        SpringApplication.run(SeckillUserApplication.class, args);
    }
}