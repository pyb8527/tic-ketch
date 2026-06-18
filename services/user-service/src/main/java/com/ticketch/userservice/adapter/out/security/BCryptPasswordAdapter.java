package com.ticketch.userservice.adapter.out.security;

import com.ticketch.userservice.application.port.out.EncodePasswordPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * [Security Adapter] BCrypt 비밀번호 인코딩 구현체.
 *
 * <p>구현 포트: {@link EncodePasswordPort}
 * <p>BCryptPasswordEncoder Bean은 {@link com.ticketch.userservice.config.SecurityConfig}에서 등록된다.
 * <p>헥사고날 원칙: 도메인/애플리케이션 레이어는 BCrypt를 직접 알지 못하며, 이 어댑터를 통해서만 접근한다.
 */
@Component
@RequiredArgsConstructor
public class BCryptPasswordAdapter implements EncodePasswordPort {

    private final PasswordEncoder passwordEncoder;

    @Override
    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
