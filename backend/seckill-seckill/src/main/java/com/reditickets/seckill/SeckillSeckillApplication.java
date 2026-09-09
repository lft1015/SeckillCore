package com.reditickets.seckill;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.reditickets.seckill", "com.reditickets.common"})
@EnableDiscoveryClient
@MapperScan("com.reditickets.seckill.mapper")
public class SeckillSeckillApplication {
    public static void main(String[] args) {
        SpringApplication.run(SeckillSeckillApplication.class, args);
    }
}