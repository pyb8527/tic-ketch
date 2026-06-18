package com.ticketch.userservice.application.service;

import com.ticketch.common.exception.BusinessException;
import com.ticketch.common.exception.ErrorCode;
import com.ticketch.userservice.application.port.in.RegisterUserUseCase.RegisterCommand;
import com.ticketch.userservice.application.port.out.EncodePasswordPort;
import com.ticketch.userservice.application.port.out.LoadUserPort;
import com.ticketch.userservice.application.port.out.SaveUserPort;
import com.ticketch.userservice.domain.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RegisterUserServiceTest {

    @InjectMocks RegisterUserService registerUserService;
    @Mock LoadUserPort loadUserPort;
    @Mock SaveUserPort saveUserPort;
    @Mock EncodePasswordPort encodePasswordPort;

    @Test
    @DisplayName("정상 회원가입 시 save 호출")
    void register_success() {
        given(loadUserPort.findByEmail("test@email.com")).willReturn(Optional.empty());
        given(encodePasswordPort.encode("password1!")).willReturn("encodedPw");
        given(saveUserPort.save(any(User.class))).willReturn(1L);

        registerUserService.register(new RegisterCommand("test@email.com", "password1!", "홍길동"));

        verify(saveUserPort).save(any(User.class));
    }

    @Test
    @DisplayName("중복 이메일이면 DUPLICATE_EMAIL 예외")
    void register_duplicateEmail() {
        User existing = User.builder().id(1L).email("test@email.com").build();
        given(loadUserPort.findByEmail("test@email.com")).willReturn(Optional.of(existing));

        assertThatThrownBy(() ->
                registerUserService.register(new RegisterCommand("test@email.com", "pw", "이름"))
        )
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_EMAIL);
    }
}
