package com.ticketch.gateway;

import com.ticketch.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API Gateway 애플리케이션 진입점.
 * 담당: 라우팅·JWT 검증·Rate Limit·CORS (포트 8080)
 */
@SpringBootApplication(scanBasePackages = "com.ticketch")
@EnableConfigurationProperties(JwtProperties.class)
@EnableDiscoveryClient
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
