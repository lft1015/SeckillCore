package com.reditickets.activity;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.reditickets.activity", "com.reditickets.common"})
@EnableDiscoveryClient
@MapperScan("com.reditickets.activity.mapper")
public class SeckillActivityApplication {
    public static void main(String[] args) {
        SpringApplication.run(SeckillActivityApplication.class, args);
    }
}