package com.ticketch.reservationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Reservation Service 메인 클래스.
 *
 * <p>담당: 좌석 분산 선점·TTL·대기열
 * <p>포트: 8083 (config-repo/reservation-service.yml)
 */
@SpringBootApplication(scanBasePackages = {"com.ticketch.reservationservice", "com.ticketch.common"})
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
public class ReservationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReservationServiceApplication.class, args);
    }
}
