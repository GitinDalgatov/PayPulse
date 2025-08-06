package com.paypulse.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class ApiGatewayApplication {

    public static void main(String[] args) {
        log.info("Starting PayPulse API Gateway...");
        SpringApplication.run(ApiGatewayApplication.class, args);
        log.info("PayPulse API Gateway started successfully!");
    }
}