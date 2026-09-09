package com.reditickets.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.reditickets.product", "com.reditickets.common"})
@EnableDiscoveryClient
@MapperScan("com.reditickets.product.mapper")
public class SeckillProductApplication {
    public static void main(String[] args) {
        SpringApplication.run(SeckillProductApplication.class, args);
    }
}