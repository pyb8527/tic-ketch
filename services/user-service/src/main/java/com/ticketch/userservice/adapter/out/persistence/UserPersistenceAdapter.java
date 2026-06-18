package com.ticketch.userservice.adapter.out.persistence;

import com.ticketch.userservice.application.port.out.LoadUserPort;
import com.ticketch.userservice.application.port.out.SaveUserPort;
import com.ticketch.userservice.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements LoadUserPort, SaveUserPort {

    private final UserJpaRepository userJpaRepository;

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email).map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<User> findById(Long userId) {
        return userJpaRepository.findById(userId).map(UserJpaEntity::toDomain);
    }

    @Override
    public Long save(User user) {
        return userJpaRepository.save(UserJpaEntity.fromDomain(user)).getId();
    }
}
