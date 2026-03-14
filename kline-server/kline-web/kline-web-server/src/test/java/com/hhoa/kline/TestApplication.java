package com.hhoa.kline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

/** 测试用的Spring Boot应用 使用原生的Spring自动注入，包括真实的FileServiceImpl */
@SpringBootApplication(
        scanBasePackages = {
            "com.hhoa.kline.web",
        })
@SpringBootTest
@ActiveProfiles("test")
@Configuration
@EntityScan(basePackages = "com.hhoa.kline.web")
@EnableJpaRepositories(basePackages = "com.hhoa.kline")
public class TestApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
