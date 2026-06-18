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

/**
 * [Application Service] 회원가입 유스케이스 구현체.
 *
 * <p>헥사고날 원칙에 따라 포트 인터페이스만 의존하며, JPA·BCrypt 등 구현 기술을 직접 참조하지 않는다.
 *
 * <p>처리 흐름:
 * <ol>
 *   <li>이메일 중복 여부 확인 (LoadUserPort)</li>
 *   <li>비밀번호 인코딩 (EncodePasswordPort → BCryptPasswordAdapter)</li>
 *   <li>도메인 객체 생성 후 저장 (SaveUserPort → UserPersistenceAdapter)</li>
 * </ol>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class RegisterUserService implements RegisterUserUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final EncodePasswordPort encodePasswordPort;

    @Override
    public Long register(RegisterCommand command) {
        // 이메일 중복 검사 — 이미 가입된 경우 409 Conflict
        if (loadUserPort.findByEmail(command.email()).isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .email(command.email())
                .password(encodePasswordPort.encode(command.rawPassword())) // BCrypt 인코딩
                .name(command.name())
                .role(User.UserRole.USER)
                .createdAt(LocalDateTime.now())
                .build();

        return saveUserPort.save(user);
    }
}
