package com.ticketch.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * CORS(Cross-Origin Resource Sharing) 설정을 담당하는 리액티브 구성 클래스.
 * 모든 출처, 메서드, 헤더를 허용하고 자격증명을 함께 전송할 수 있도록 구성한다.
 */
@Configuration
public class CorsConfig {

    /**
     * 리액티브 CORS 웹 필터를 생성하여 모든 요청에 CORS 정책을 적용한다.
     *
     * @return 구성된 CorsWebFilter 빈
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.addAllowedOriginPattern("*");
        cfg.addAllowedMethod("*");
        cfg.addAllowedHeader("*");
        cfg.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return new CorsWebFilter(source);
    }
}
