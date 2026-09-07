package com.reditickets.launcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {
        "com.reditickets.interfaces",
        "com.reditickets.user",
        "com.reditickets.product",
        "com.reditickets.order",
        "com.reditickets.seckill",
        "com.reditickets.activity",
        "com.reditickets.common",
        "com.reditickets.launcher"
})
@EnableDiscoveryClient
public class SeckillLauncherApplication {
    public static void main(String[] args) {
        SpringApplication.run(SeckillLauncherApplication.class, args);
    }
}