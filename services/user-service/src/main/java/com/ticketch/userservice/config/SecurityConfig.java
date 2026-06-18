package com.ticketch.userservice.config;

import com.ticketch.security.JwtTokenProvider;
import com.ticketch.userservice.adapter.in.web.JwtAuthenticationFilter;
import com.ticketch.userservice.application.port.out.BlacklistTokenPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정.
 *
 * <p>Stateless REST API 기반으로 구성:
 * <ul>
 *   <li>CSRF 비활성화 (JWT 사용으로 불필요)</li>
 *   <li>세션 미사용 (STATELESS)</li>
 *   <li>{@code /api/auth/**}: 인증 불필요 (회원가입, 로그인, 토큰 재발급)</li>
 *   <li>그 외 모든 요청: JWT 인증 필요</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final BlacklistTokenPort blacklistTokenPort;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 인증 없이 접근 가능한 엔드포인트
                        .requestMatchers("/api/auth/**", "/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                // UsernamePasswordAuthenticationFilter 이전에 JWT 필터 삽입
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, blacklistTokenPort);
    }

    /** BCrypt 비밀번호 인코더 Bean — BCryptPasswordAdapter에 주입됨 */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
