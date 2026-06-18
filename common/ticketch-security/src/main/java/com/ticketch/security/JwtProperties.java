package com.ticketch.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 설정값 바인딩 객체.
 *
 * <p>Config Server의 {@code jwt.*} 프로퍼티를 주입받는다.
 * 사용 시 {@code @EnableConfigurationProperties(JwtProperties.class)}를 메인 클래스에 선언해야 한다.
 *
 * <pre>
 * jwt:
 *   secret: ...           # HMAC-SHA 서명 키 (256비트 이상 권장)
 *   access-expiry: 900000  # AccessToken 유효기간 (ms, 기본 15분)
 *   refresh-expiry: 604800000  # RefreshToken 유효기간 (ms, 기본 7일)
 * </pre>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;
    /** AccessToken 유효기간 (밀리초) */
    private long accessExpiry;
    /** RefreshToken 유효기간 (밀리초) */
    private long refreshExpiry;
}
