package com.hhoa.kline.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication(
        scanBasePackages = {
            "com.hhoa.kline.web",
        })
@Configuration
@EntityScan(basePackages = "com.hhoa.kline.web")
public class KlineApplication {
    public static void main(String[] args) {
        SpringApplication.run(KlineApplication.class, args);
    }
}
