package com.ticketch.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * JWT 토큰에서 파싱된 인증 주체 정보.
 *
 * <p>Spring Security의 {@code Authentication#getPrincipal()} 로 사용되며,
 * API Gateway에서 검증 후 {@code X-User-Id} 헤더로 전파하는 값과 동일한 구조다.
 *
 * <p>컨트롤러에서는 {@code @AuthenticationPrincipal UserPrincipal principal}로 주입받는다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPrincipal {

    private Long userId;
    private String email;
    /** USER 또는 ADMIN */
    private String role;
}
