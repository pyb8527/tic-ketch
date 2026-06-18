package com.ticketch.userservice.application.service;

import com.ticketch.common.exception.BusinessException;
import com.ticketch.common.exception.ErrorCode;
import com.ticketch.security.JwtProperties;
import com.ticketch.security.JwtTokenProvider;
import com.ticketch.security.UserPrincipal;
import com.ticketch.userservice.application.port.in.GetMyProfileUseCase;
import com.ticketch.userservice.application.port.in.LoginUseCase;
import com.ticketch.userservice.application.port.in.LogoutUseCase;
import com.ticketch.userservice.application.port.in.ReissueTokenUseCase;
import com.ticketch.userservice.application.port.in.TokenPair;
import com.ticketch.userservice.application.port.out.BlacklistTokenPort;
import com.ticketch.userservice.application.port.out.EncodePasswordPort;
import com.ticketch.userservice.application.port.out.LoadRefreshTokenPort;
import com.ticketch.userservice.application.port.out.LoadUserPort;
import com.ticketch.userservice.application.port.out.SaveRefreshTokenPort;
import com.ticketch.userservice.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [Application Service] 인증 관련 유스케이스 구현체.
 *
 * <p>로그인, 토큰 재발급, 로그아웃, 내 프로필 조회를 담당한다.
 *
 * <p>Refresh Token Rotation 전략:
 * 재발급 시 기존 RefreshToken을 새 것으로 교체하여, 토큰 탈취 시 피해를 최소화한다.
 */
@Service
@RequiredArgsConstructor
public class AuthService implements LoginUseCase, ReissueTokenUseCase, LogoutUseCase, GetMyProfileUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveRefreshTokenPort saveRefreshTokenPort;
    private final LoadRefreshTokenPort loadRefreshTokenPort;
    private final BlacklistTokenPort blacklistTokenPort;
    private final EncodePasswordPort encodePasswordPort;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    /**
     * 로그인 처리.
     * 이메일/비밀번호 검증 후 AccessToken + RefreshToken 발급.
     * RefreshToken은 Redis에 저장된다 (키: refresh:token:{userId}).
     */
    @Override
    @Transactional(readOnly = true)
    public TokenPair login(LoginCommand command) {
        User user = loadUserPort.findByEmail(command.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // BCrypt 비밀번호 검증
        if (!encodePasswordPort.matches(command.rawPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        UserPrincipal principal = toUserPrincipal(user);
        String accessToken  = jwtTokenProvider.generateAccessToken(principal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(principal);

        // RefreshToken Redis 저장 (TTL = jwt.refresh-expiry)
        saveRefreshTokenPort.save(user.getId(), refreshToken);

        return new TokenPair(accessToken, refreshToken);
    }

    /**
     * AccessToken 재발급 (Refresh Token Rotation).
     * 기존 RefreshToken 검증 → Redis 존재 확인 → 새 토큰 쌍 발급 → 기존 토큰 교체.
     */
    @Override
    public TokenPair reissue(String refreshToken) {
        // 서명/만료 검증
        jwtTokenProvider.validate(refreshToken);
        UserPrincipal principal = jwtTokenProvider.parseToken(refreshToken);

        // Redis에 저장된 토큰이 없으면 로그아웃 상태
        loadRefreshTokenPort.findByUserId(principal.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));

        String newAccessToken  = jwtTokenProvider.generateAccessToken(principal);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(principal);

        // 기존 RefreshToken 교체 (Rotation)
        saveRefreshTokenPort.save(principal.getUserId(), newRefreshToken);

        return new TokenPair(newAccessToken, newRefreshToken);
    }

    /**
     * 로그아웃 처리.
     * AccessToken의 JTI를 블랙리스트에 등록하고, RefreshToken을 Redis에서 삭제한다.
     * TTL은 AccessToken 만료시간으로 설정하여 만료 후 자동 제거된다.
     */
    @Override
    public void logout(String accessToken, String refreshToken) {
        String jti = jwtTokenProvider.extractJti(accessToken);
        // AccessToken 블랙리스트 등록 (TTL = accessExpiry)
        blacklistTokenPort.blacklist(jti, jwtProperties.getAccessExpiry());

        UserPrincipal principal = jwtTokenProvider.parseToken(refreshToken);
        // Redis에서 RefreshToken 삭제
        saveRefreshTokenPort.revoke(principal.getUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public User getMyProfile(Long userId) {
        return loadUserPort.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /** User 도메인 객체를 JWT 클레임용 UserPrincipal로 변환 */
    private UserPrincipal toUserPrincipal(User user) {
        return UserPrincipal.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
