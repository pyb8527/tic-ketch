package com.ticketch.userservice.application.service;

import com.ticketch.common.exception.BusinessException;
import com.ticketch.common.exception.ErrorCode;
import com.ticketch.userservice.application.port.in.RegisterUserUseCase;
import com.ticketch.userservice.application.port.out.EncodePasswordPort;
import com.ticketch.userservice.application.port.out.LoadUserPort;
import com.ticketch.userservice.application.port.out.SaveUserPort;
import com.ticketch.userservice.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class RegisterUserService implements RegisterUserUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final EncodePasswordPort encodePasswordPort;

    @Override
    public Long register(RegisterCommand command) {
        if (loadUserPort.findByEmail(command.email()).isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .email(command.email())
                .password(encodePasswordPort.encode(command.rawPassword()))
                .name(command.name())
                .role(User.UserRole.USER)
                .createdAt(LocalDateTime.now())
                .build();

        return saveUserPort.save(user);
    }
}
