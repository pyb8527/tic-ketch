package com.ticketch.gateway;

import com.ticketch.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API Gateway 애플리케이션 진입점.
 * 담당: 라우팅·JWT 검증·Rate Limit·CORS (포트 8080)
 *
 * <p>스캔 범위를 gateway + security로 한정한다. com.ticketch.common 전체를 스캔하면
 * 서블릿 전용 {@code GlobalExceptionHandler}(@RestControllerAdvice)가 등록되어
 * 리액티브 Gateway에서 기동 실패하므로 제외한다. (BusinessException 등 순수 클래스는 스캔과 무관하게 사용)
 */
@SpringBootApplication(scanBasePackages = {"com.ticketch.gateway", "com.ticketch.security"})
@EnableConfigurationProperties(JwtProperties.class)
@EnableDiscoveryClient
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
