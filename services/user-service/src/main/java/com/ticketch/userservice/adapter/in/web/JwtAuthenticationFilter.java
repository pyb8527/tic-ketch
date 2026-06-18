package com.ticketch.userservice.adapter.in.web;

import com.ticketch.security.JwtTokenProvider;
import com.ticketch.security.UserPrincipal;
import com.ticketch.userservice.application.port.out.BlacklistTokenPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * [Web Adapter - Filter] JWT 인증 필터.
 *
 * <p>요청마다 한 번 실행되며(OncePerRequestFilter), Authorization 헤더의 Bearer 토큰을 검증한다.
 *
 * <p>처리 흐름:
 * <ol>
 *   <li>Authorization 헤더에서 Bearer 토큰 추출</li>
 *   <li>JwtTokenProvider로 서명/만료 검증</li>
 *   <li>Redis 블랙리스트 확인 (로그아웃 토큰 차단)</li>
 *   <li>SecurityContext에 UserPrincipal 설정</li>
 * </ol>
 *
 * <p>최종 MSA 구조에서는 API Gateway가 JWT 검증을 담당하고,
 * 이 필터는 User Service 단독 테스트 및 직접 호출 시 동작한다.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final BlacklistTokenPort blacklistTokenPort;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (StringUtils.hasText(token)) {
            try {
                jwtTokenProvider.validate(token);
                String jti = jwtTokenProvider.extractJti(token);

                // 로그아웃된 토큰(블랙리스트)은 인증 처리 skip
                if (!blacklistTokenPort.isBlacklisted(jti)) {
                    UserPrincipal principal = jwtTokenProvider.parseToken(token);
                    var auth = new UsernamePasswordAuthenticationToken(
                            principal, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + principal.getRole()))
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                // 유효하지 않은 토큰은 무시 — 인증 없이 다음 필터로 전달
                // 보호된 리소스 접근 시 Spring Security가 401을 반환
                log.debug("JWT validation failed: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    /** Authorization 헤더에서 "Bearer " 제거 후 토큰 추출 */
    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
