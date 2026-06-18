package com.ticketch.userservice.adapter.out.persistence;

import com.ticketch.userservice.application.port.out.LoadUserPort;
import com.ticketch.userservice.application.port.out.SaveUserPort;
import com.ticketch.userservice.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * [Persistence Adapter] JPA를 이용한 사용자 저장소 구현체.
 *
 * <p>구현 포트: {@link LoadUserPort}, {@link SaveUserPort}
 * <p>DB: MySQL — users 테이블 (Flyway V1__create_users.sql)
 */
@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements LoadUserPort, SaveUserPort {

    private final UserJpaRepository userJpaRepository;

    @Override
    public Optional<User> findByEmail(String email) {
        // JPA Entity → 도메인 모델로 변환하여 반환
        return userJpaRepository.findByEmail(email).map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<User> findById(Long userId) {
        return userJpaRepository.findById(userId).map(UserJpaEntity::toDomain);
    }

    @Override
    public Long save(User user) {
        // 도메인 모델 → JPA Entity 변환 후 저장
        return userJpaRepository.save(UserJpaEntity.fromDomain(user)).getId();
    }
}
