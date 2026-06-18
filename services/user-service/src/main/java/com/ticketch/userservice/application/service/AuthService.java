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

    @Override
    @Transactional(readOnly = true)
    public TokenPair login(LoginCommand command) {
        User user = loadUserPort.findByEmail(command.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!encodePasswordPort.matches(command.rawPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        UserPrincipal principal = toUserPrincipal(user);
        String accessToken  = jwtTokenProvider.generateAccessToken(principal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(principal);

        saveRefreshTokenPort.save(user.getId(), refreshToken);

        return new TokenPair(accessToken, refreshToken);
    }

    @Override
    public TokenPair reissue(String refreshToken) {
        jwtTokenProvider.validate(refreshToken);
        UserPrincipal principal = jwtTokenProvider.parseToken(refreshToken);

        loadRefreshTokenPort.findByUserId(principal.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));

        String newAccessToken  = jwtTokenProvider.generateAccessToken(principal);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(principal);

        saveRefreshTokenPort.save(principal.getUserId(), newRefreshToken);

        return new TokenPair(newAccessToken, newRefreshToken);
    }

    @Override
    public void logout(String accessToken, String refreshToken) {
        String jti = jwtTokenProvider.extractJti(accessToken);
        blacklistTokenPort.blacklist(jti, jwtProperties.getAccessExpiry());

        UserPrincipal principal = jwtTokenProvider.parseToken(refreshToken);
        saveRefreshTokenPort.revoke(principal.getUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public User getMyProfile(Long userId) {
        return loadUserPort.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private UserPrincipal toUserPrincipal(User user) {
        return UserPrincipal.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
