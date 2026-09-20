package com.example.pipelineservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableFeignClients
@EnableAsync
@EnableMethodSecurity
public class PipelineServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PipelineServiceApplication.class, args);
    }

}