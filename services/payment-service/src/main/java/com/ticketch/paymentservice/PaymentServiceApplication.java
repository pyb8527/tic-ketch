package com.ticketch.paymentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Payment Service 메인 클래스.
 *
 * <p>담당: 목업 결제·RabbitMQ 이벤트 발행
 * <p>포트: 8084 (config-repo/payment-service.yml)
 */
@SpringBootApplication(scanBasePackages = {"com.ticketch.paymentservice", "com.ticketch.common"})
@EnableDiscoveryClient
@EnableFeignClients
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
