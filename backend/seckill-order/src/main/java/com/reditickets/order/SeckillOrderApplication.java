package com.reditickets.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.reditickets.order", "com.reditickets.common"})
@EnableDiscoveryClient
@MapperScan("com.reditickets.order.mapper")
public class SeckillOrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(SeckillOrderApplication.class, args);
    }
}