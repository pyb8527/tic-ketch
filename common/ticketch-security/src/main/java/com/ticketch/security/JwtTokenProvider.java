package com.ticketch.security;

import com.ticketch.common.exception.BusinessException;
import com.ticketch.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 토큰 생성 · 파싱 · 검증 유틸리티.
 *
 * <p>알고리즘: HMAC-SHA512 (JJWT 0.12)
 * <p>토큰 클레임 구조:
 * <pre>
 * {
 *   "jti": "UUID",          // 토큰 고유 식별자 (블랙리스트용)
 *   "sub": "userId",        // 사용자 ID
 *   "email": "...",
 *   "role": "USER|ADMIN",
 *   "iat": ...,
 *   "exp": ...
 * }
 * </pre>
 *
 * <p>User Service가 발급하며, API Gateway와 각 서비스의 JWT 필터에서 검증한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    /**
     * 애플리케이션 기동 시 secret을 Base64 디코딩하여 HMAC 키 초기화.
     * secret이 null이면 NullPointerException 발생 → Config Server 미연결 확인 필요.
     */
    @PostConstruct
    public void init() {
        String encoded = Base64.getEncoder()
                .encodeToString(jwtProperties.getSecret().getBytes());
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(encoded));
    }

    /**
     * AccessToken 발급 (유효기간: {@code jwt.access-expiry}).
     *
     * @param principal 인증 주체 (userId, email, role)
     * @return 서명된 JWT 문자열
     */
    public String generateAccessToken(UserPrincipal principal) {
        return buildToken(principal, jwtProperties.getAccessExpiry());
    }

    /**
     * RefreshToken 발급 (유효기간: {@code jwt.refresh-expiry}).
     * AccessToken 재발급 전용이며, Redis에 저장된다.
     */
    public String generateRefreshToken(UserPrincipal principal) {
        return buildToken(principal, jwtProperties.getRefreshExpiry());
    }

    /**
     * 토큰에서 {@link UserPrincipal} 파싱.
     *
     * @throws BusinessException INVALID_TOKEN — 서명 불일치 또는 형식 오류
     * @throws BusinessException EXPIRED_TOKEN — 토큰 만료
     */
    public UserPrincipal parseToken(String token) {
        Claims claims = getClaims(token);
        return UserPrincipal.builder()
                .userId(Long.parseLong(claims.getSubject()))
                .email(claims.get("email", String.class))
                .role(claims.get("role", String.class))
                .build();
    }

    /**
     * 토큰에서 JTI(JWT ID) 추출.
     * 로그아웃 시 Redis 블랙리스트 키로 사용된다.
     */
    public String extractJti(String token) {
        return getClaims(token).getId();
    }

    /**
     * 토큰 유효성 검증.
     *
     * @throws BusinessException EXPIRED_TOKEN — 만료된 토큰
     * @throws BusinessException INVALID_TOKEN — 서명 불일치, 형식 오류 등
     */
    public boolean validate(String token) {
        try {
            getClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    /** JWT 빌더 공통 로직 */
    private String buildToken(UserPrincipal principal, long expiry) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())       // jti: 블랙리스트 식별자
                .subject(String.valueOf(principal.getUserId()))
                .claim("email", principal.getEmail())
                .claim("role", principal.getRole())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiry))
                .signWith(secretKey)
                .compact();
    }

    /** 서명 검증 후 Claims 반환 */
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
