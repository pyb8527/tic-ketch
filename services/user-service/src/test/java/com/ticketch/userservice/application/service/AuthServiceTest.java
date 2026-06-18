package com.ticketch.userservice.application.service;

import com.ticketch.common.exception.BusinessException;
import com.ticketch.common.exception.ErrorCode;
import com.ticketch.security.JwtProperties;
import com.ticketch.security.JwtTokenProvider;
import com.ticketch.security.UserPrincipal;
import com.ticketch.userservice.application.port.in.LoginUseCase.LoginCommand;
import com.ticketch.userservice.application.port.in.TokenPair;
import com.ticketch.userservice.application.port.out.BlacklistTokenPort;
import com.ticketch.userservice.application.port.out.EncodePasswordPort;
import com.ticketch.userservice.application.port.out.LoadRefreshTokenPort;
import com.ticketch.userservice.application.port.out.LoadUserPort;
import com.ticketch.userservice.application.port.out.SaveRefreshTokenPort;
import com.ticketch.userservice.domain.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks AuthService authService;
    @Mock LoadUserPort loadUserPort;
    @Mock SaveRefreshTokenPort saveRefreshTokenPort;
    @Mock LoadRefreshTokenPort loadRefreshTokenPort;
    @Mock BlacklistTokenPort blacklistTokenPort;
    @Mock EncodePasswordPort encodePasswordPort;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock JwtProperties jwtProperties;

    private final User mockUser = User.builder()
            .id(1L).email("test@email.com").password("encodedPw")
            .name("홍길동").role(User.UserRole.USER).build();

    @Test
    @DisplayName("로그인 성공 시 TokenPair 반환")
    void login_success() {
        given(loadUserPort.findByEmail("test@email.com")).willReturn(Optional.of(mockUser));
        given(encodePasswordPort.matches("rawPw", "encodedPw")).willReturn(true);
        given(jwtTokenProvider.generateAccessToken(any(UserPrincipal.class))).willReturn("accessToken");
        given(jwtTokenProvider.generateRefreshToken(any(UserPrincipal.class))).willReturn("refreshToken");

        TokenPair result = authService.login(new LoginCommand("test@email.com", "rawPw"));

        assertThat(result.accessToken()).isEqualTo("accessToken");
        assertThat(result.refreshToken()).isEqualTo("refreshToken");
        verify(saveRefreshTokenPort).save(1L, "refreshToken");
    }

    @Test
    @DisplayName("존재하지 않는 이메일이면 USER_NOT_FOUND 예외")
    void login_userNotFound() {
        given(loadUserPort.findByEmail("none@email.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginCommand("none@email.com", "pw")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("비밀번호 불일치 시 INVALID_PASSWORD 예외")
    void login_wrongPassword() {
        given(loadUserPort.findByEmail("test@email.com")).willReturn(Optional.of(mockUser));
        given(encodePasswordPort.matches("wrongPw", "encodedPw")).willReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginCommand("test@email.com", "wrongPw")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PASSWORD);
    }

    @Test
    @DisplayName("로그아웃 시 블랙리스트 등록 + RefreshToken 삭제")
    void logout_success() {
        given(jwtTokenProvider.extractJti("accessToken")).willReturn("jti-uuid");
        given(jwtTokenProvider.parseToken("refreshToken"))
                .willReturn(UserPrincipal.builder().userId(1L).build());
        given(jwtProperties.getAccessExpiry()).willReturn(900000L);

        authService.logout("accessToken", "refreshToken");

        verify(blacklistTokenPort).blacklist("jti-uuid", 900000L);
        verify(saveRefreshTokenPort).revoke(1L);
    }
}
