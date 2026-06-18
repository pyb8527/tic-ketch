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

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        String encoded = Base64.getEncoder()
                .encodeToString(jwtProperties.getSecret().getBytes());
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(encoded));
    }

    public String generateAccessToken(UserPrincipal principal) {
        return buildToken(principal, jwtProperties.getAccessExpiry());
    }

    public String generateRefreshToken(UserPrincipal principal) {
        return buildToken(principal, jwtProperties.getRefreshExpiry());
    }

    public UserPrincipal parseToken(String token) {
        Claims claims = getClaims(token);
        return UserPrincipal.builder()
                .userId(Long.parseLong(claims.getSubject()))
                .email(claims.get("email", String.class))
                .role(claims.get("role", String.class))
                .build();
    }

    public String extractJti(String token) {
        return getClaims(token).getId();
    }

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

    private String buildToken(UserPrincipal principal, long expiry) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(principal.getUserId()))
                .claim("email", principal.getEmail())
                .claim("role", principal.getRole())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiry))
                .signWith(secretKey)
                .compact();
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
